package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
//import edu.cwru.sepia.agent.AstarAgent.LocationComparator;
//import edu.cwru.sepia.agent.AstarAgent.MapLocation;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.util.DistanceMetrics;
import edu.cwru.sepia.util.Pair;

import java.util.*;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 * 
 * @author: Jake Anderson
 * @author: Joseph Tate 
 */
public class GameState {
     //The player number.  Change this if the player number is not 0.
     private static final Integer myPlayerNum=new Integer(0);
     
     //keeps track of whose turn is next, to know whether to move the friendly units or the enemies when finding children
     public boolean myTurnNext;

     private int xSize;  //map size along x-axis
     private int ySize;  //map size along y-axis
     
     private int[] obstaclesXPositions;
     private int[] obstaclesYPositions;
     
     private Integer[] enemyUnitIds;
     public int[] enemyUnitXPositions;
     public int[] enemyUnitYPositions;
     private int[] enemyUnitRange;
     
     private Integer[] friendlyUnitIds;
     public int[] friendlyUnitXPositions;
     public int[] friendlyUnitYPositions;
     private int[] friendlyUnitRange;
     
    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     *
     * You may find the following state methods useful:
     *
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns all of the obstacles in the map
     * state.getResourceNode(Integer resourceID): Return a ResourceView for the given ID
     *
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     *
     * For a given unit you will need to find the attack damage, range and max HP
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit deals
     * unitView.getTemplateView().getBaseHealth(): The maximum amount of health of this unit
     *
     * @param state Current state of the episode
     */
     /* The constructor for the parent GameState.
      * Extracts relevant information from the State.StateView
      */
    public GameState(State.StateView state) {
         xSize=state.getXExtent();
         ySize=state.getYExtent();
         
         //get resource (obstacle) locations
         Integer[] resourceIds=state.getAllResourceIds().toArray(new Integer[0]);
         obstaclesXPositions=new int[resourceIds.length];
         obstaclesYPositions=new int[resourceIds.length];
         for(int i=0;i<resourceIds.length;i++) {
              obstaclesXPositions[i]=state.getResourceNode(resourceIds[i]).getXPosition();
              obstaclesYPositions[i]=state.getResourceNode(resourceIds[i]).getYPosition();
         }
         
         // get the ids, locations, and ranges of the friendly units (assumption:playerNum=0)
         // support for arbitrary amount of friendly units
         // theoretically supports arbitrary friendly units (untested)
         friendlyUnitIds=state.getUnitIds(myPlayerNum).toArray(new Integer[0]);
         List<UnitView> myUnits = state.getUnits(myPlayerNum);
         friendlyUnitXPositions = new int[myUnits.size()];
         friendlyUnitYPositions = new int[myUnits.size()];
         friendlyUnitRange = new int[myUnits.size()];
         for(int i=0;i<myUnits.size();i++) {
              friendlyUnitXPositions[i]=myUnits.get(i).getXPosition();
              friendlyUnitYPositions[i]=myUnits.get(i).getYPosition();
              friendlyUnitRange[i]=myUnits.get(i).getTemplateView().getRange();
         }
         
         // get the ids, locations, and ranges of the enemy units (assumption:playerNum=0)
         // support for arbitrary amount of enemies and enemy units
         // theoretically supports arbitrary enemy units (untested)
         Integer[] players=state.getPlayerNumbers();
         ArrayList<Integer> enemies=new ArrayList<Integer>();
         for(int i=0;i<players.length;i++) {
              if(!players[i].equals(myPlayerNum)) {
                   enemies.add(players[i]);
              }
         }
         List<Integer> enemyIds=null;
         List<UnitView> enemyUnits=null;
         if(enemies.size()>0) {
              enemyIds=state.getUnitIds(enemies.get(0));
              enemyUnits = state.getUnits(enemies.get(0));
              if(enemies.size()>1) {
                   for(int i=1;i<enemies.size();i++) {
                        enemyIds=state.getUnitIds(enemies.get(i));
                        enemyUnits.addAll(state.getUnits(enemies.get(i)));
                   }
              }
         }
         enemyUnitIds=enemyIds.toArray(new Integer[0]);
         enemyUnitXPositions=new int[0];
         enemyUnitYPositions=new int[0];
         enemyUnitRange=new int[0];
         if(enemyUnits!=null) {
              enemyUnitXPositions = new int[enemyUnits.size()];
              enemyUnitYPositions = new int[enemyUnits.size()];
              enemyUnitRange=new int[enemyUnits.size()];
              for(int i=0;i<enemyUnits.size();i++) {
                   enemyUnitXPositions[i]=enemyUnits.get(i).getXPosition();
                   enemyUnitYPositions[i]=enemyUnits.get(i).getYPosition();
                   enemyUnitRange[i]=enemyUnits.get(i).getTemplateView().getRange();
              }
         }
         
         myTurnNext=true;
    }

    private GameState() {
     // for creating deep copies.
    }

    /**
     * Finds the utility of the current state.
     * The utility is defined in the following manner:
     * if you can see the enemy and no obstacles are blocking the path, add the inverse of the distance to the utility times a constant.
     * that way, if there is a straight (or diagonal) line to the archer, you head straight for the archer.
     * otherwise, subtract the inverse of the distance times the constant.
     * that way, if you can't get to the archer, you try to get out of range of his arrows until you can.
     * also, add 10000 to the utility of the function if the  footman gets within 2 of the archer.
     * That way, hitting the archer is supremely favored (as the point of the game is to kill the archer).
     * @return The weighted linear combination of the features 
     */
    public double getUtility() {
    	double util= 0;
    	Pair<Integer, Double> closestEnemy;
		for(int j = 0; j< friendlyUnitXPositions.length; j++) {
			if(enemyUnitXPositions != null && enemyUnitXPositions.length > 0) {
				closestEnemy = getClosestEnemy(friendlyUnitXPositions[j], friendlyUnitYPositions[j]);
				if(canSeeEnemy(friendlyUnitXPositions[j], friendlyUnitYPositions[j], enemyUnitXPositions[closestEnemy.a], enemyUnitYPositions[closestEnemy.a])) {
	    		     util += xSize*ySize/(DistanceMetrics.chebyshevDistance(friendlyUnitXPositions[j], friendlyUnitYPositions[j], enemyUnitXPositions[closestEnemy.a], enemyUnitYPositions[closestEnemy.a])+1);
				} else {
					util -= xSize*ySize/(DistanceMetrics.chebyshevDistance(friendlyUnitXPositions[j], friendlyUnitYPositions[j], enemyUnitXPositions[closestEnemy.a], enemyUnitYPositions[closestEnemy.a])+1);
				}
	 	    	if(closestEnemy.b <2 ) {
	 	    		util +=10000;
	 	    	}
			}
     	}
        return util;
    }
    /**
     * iterates through every combination of x's and y's to see if there is an obstacle within the line of sight from the
     * footman to the agent. returns false if something is blocking his way, otherwise, returns true.
     * @param herox
     * @param heroy
     * @param x
     * @param y
     * @return
     */
    private boolean canSeeEnemy(int herox, int heroy, int x, int y) {
    	if(x < herox) {
    		//swap
    		int temp = x;
    		x = herox;
    		herox = temp;
    		 
    	}
    	if(y< heroy) {
    		//swap
    		int temp = y;
    		y = heroy;
    		heroy = temp;
    		 
    	}
    	for(int i = herox; i <= x; i++ ) {
    		for(int j = heroy; j <= y; j++) {
    			for(int ox: obstaclesXPositions) {
    				for(int oy: obstaclesYPositions) {
    					if(ox == i && oy == j) {
    						return false;
    					}
    				}
    			}
    		}
    	}
    	return true;
    }
    /**
     * get's the closest enemy to a given position.
     * @param x
     * @param y
     * @return the index of the closest enemy in the array of enemy positions and the distance
     */
    private Pair<Integer, Double> getClosestEnemy(int x, int y) {
    	int index = enemyUnitXPositions.length;
    	double dist = Double.MAX_VALUE;
    	int temp;
    	for(int i = 0; i< enemyUnitXPositions.length; i++) {
    		temp =  DistanceMetrics.chebyshevDistance(x, y, enemyUnitXPositions[i], enemyUnitYPositions[i]);
    		if( dist > temp) {
    			dist = temp;
    			index = i;
    		}
    	}
    	return new Pair<Integer, Double>(index, dist);
    }

    /**
     * You will implement this function.
     *
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     *
     * You may find it useful to iterate over all the different directions in SEPIA.
     *
     * for(Direction direction : Directions.values())
     *
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     *
     * @return All possible actions and their associated resulting game state
     */
    //This should work for arbitrary numbers and types of friendly and enemy units.
    public List<GameStateChild> getChildren() {
         ArrayList<GameStateChild> children = new ArrayList<GameStateChild>();
         GameState copy=copy(this);
         copy.myTurnNext=!this.myTurnNext;
         children.add(new GameStateChild(new HashMap<Integer, Action>(), copy));

         ArrayList<GameStateChild> newChildren = new ArrayList<GameStateChild>();
         if(myTurnNext) {     //calculate children if our turn
              //our turn
              for(int i=0;i<friendlyUnitIds.length;i++) {
                   // for each friendly unit, compute possible states from states already in children.
                   // then add the resulting states back to children, and repeat for the next unit
                   while(!children.isEmpty()) {
                        GameStateChild current=children.remove(0);
                        Integer enemyToAttack=canAttack(i, true);
                        // if a unit can attack an enemy, it does
                        if(enemyToAttack!=null) {
                             GameStateChild newChild=new GameStateChild(new HashMap<Integer,Action>(),GameState.copy(current.state));
                             newChild.action.putAll(current.action);
                             newChild.action.put(new Integer(i),Action.createPrimitiveAttack(friendlyUnitIds[i], enemyToAttack));
                             newChildren.add(newChild);
                        } else {
                             //otherwise, explore all legal moves
                             for(Direction direction: Direction.values()) {
                                  if((direction.xComponent()==0 || direction.yComponent()==0) && isValidMove(current.state,current.state.friendlyUnitXPositions[i]+direction.xComponent(),current.state.friendlyUnitYPositions[i]+direction.yComponent())) {
                                       GameStateChild newChild=new GameStateChild(new HashMap<Integer,Action>(),GameState.copy(current.state));
                                       newChild.action.putAll(current.action);
                                       newChild.action.put(new Integer(i),Action.createPrimitiveMove(friendlyUnitIds[i], direction));
                                       newChild.state.friendlyUnitXPositions[i]+=direction.xComponent();
                                       newChild.state.friendlyUnitYPositions[i]+=direction.yComponent();
                                       newChildren.add(newChild);
                                  }
                             }
                        }
                   }
                   children=newChildren;
                   newChildren=new ArrayList<GameStateChild>();
              }
         } else {
              //their turn
              //same as above, except the role of enemy and friendly units is reversed
              for(int i=0;i<enemyUnitIds.length;i++) {
                   while(!children.isEmpty()) {
                        GameStateChild current=children.remove(0);
                        Integer friendlyToAttack=canAttack(i, false);
                        if(friendlyToAttack!=null) {
                             GameStateChild newChild=new GameStateChild(new HashMap<Integer,Action>(),GameState.copy(current.state));
                             newChild.action.putAll(current.action);
                             newChild.action.put(new Integer(i),Action.createPrimitiveAttack(enemyUnitIds[i], friendlyToAttack));
                             newChildren.add(newChild);
                        } else {
                             for(Direction direction: Direction.values()) {
                                  if((direction.xComponent()==0 || direction.yComponent()==0) && isValidMove(current.state,current.state.enemyUnitXPositions[i]+direction.xComponent(),current.state.enemyUnitYPositions[i]+direction.yComponent())) {
                                       GameStateChild newChild=new GameStateChild(new HashMap<Integer,Action>(),GameState.copy(current.state));
                                       newChild.action.putAll(current.action);
                                       newChild.action.put(new Integer(i),Action.createPrimitiveMove(enemyUnitIds[i], direction));
                                       newChild.state.enemyUnitXPositions[i]+=direction.xComponent();
                                       newChild.state.enemyUnitYPositions[i]+=direction.yComponent();
                                       newChildren.add(newChild);
                                  }
                             }
                        }
                   }
                   children=newChildren;
                   newChildren=new ArrayList<GameStateChild>();
              }
         }
         return children;
    }
    
    /*
     * Creates a copy of a GameState for finding children.
     * Specific values are later changed as needed (in getChildren)to reflect the new state.
     */
    private static GameState copy(GameState state) {
         GameState copy=new GameState();
         copy.myTurnNext=state.myTurnNext;
         copy.xSize=state.xSize;
         copy.ySize=state.ySize;
         
         copy.obstaclesXPositions=Arrays.copyOf(state.obstaclesXPositions, state.obstaclesXPositions.length);
         copy.obstaclesYPositions=Arrays.copyOf(state.obstaclesYPositions, state.obstaclesYPositions.length);
         
         copy.enemyUnitIds=Arrays.copyOf(state.enemyUnitIds, state.enemyUnitIds.length);
         copy.enemyUnitXPositions=Arrays.copyOf(state.enemyUnitXPositions, state.enemyUnitXPositions.length);
         copy.enemyUnitYPositions=Arrays.copyOf(state.enemyUnitYPositions, state.enemyUnitYPositions.length);
         copy.enemyUnitRange=Arrays.copyOf(state.enemyUnitRange,state.enemyUnitRange.length);
         
         copy.friendlyUnitIds=Arrays.copyOf(state.friendlyUnitIds, state.friendlyUnitIds.length);
         copy.friendlyUnitXPositions=Arrays.copyOf(state.friendlyUnitXPositions, state.friendlyUnitXPositions.length);
         copy.friendlyUnitYPositions=Arrays.copyOf(state.friendlyUnitYPositions, state.friendlyUnitYPositions.length);
         copy.friendlyUnitRange=Arrays.copyOf(state.friendlyUnitRange,state.friendlyUnitRange.length);
         return copy;
    }
    
    //Checks whether a location is valid to move to
    private static boolean isValidMove(GameState state, int xPos, int yPos) {
         // in bounds check
         if(xPos<0 || xPos>=state.xSize || yPos<0 || yPos>=state.ySize) {
              return false;
         }
         // checks that there is no obstacle there
         for(int i=0;i<state.obstaclesXPositions.length;i++) {
              if(xPos==state.obstaclesXPositions[i] && yPos==state.obstaclesYPositions[i]) {
                   return false;
              }
         }
         // checks that there is no enemy there
         for(int i=0;i<state.enemyUnitXPositions.length;i++) {
              if(xPos==state.enemyUnitXPositions[i] && yPos==state.enemyUnitYPositions[i]) {
                   return false;
              }
         }
         // checks that there is no friendly unit there
         for(int i=0;i<state.friendlyUnitXPositions.length;i++) {
              if(xPos==state.friendlyUnitXPositions[i] && yPos==state.friendlyUnitYPositions[i]) {
                   return false;
              }
         }
         return true;
    }
    
    // checks whether there is a unit that the unit in the ith entry of the corresponding array can attack
    // if so, returns that unit's id, otherwise, return null
    private Integer canAttack(int i, boolean whoseAttack) {      // whoseAttack true if my units attacking, false if enemy units attacking
         if(whoseAttack) {                                       // i refers to the index of the unit in the arrays
              for(int j=0;j<enemyUnitIds.length;j++) {
                   if(Math.abs(enemyUnitXPositions[j]-friendlyUnitXPositions[i])+Math.abs(enemyUnitYPositions[j]-friendlyUnitYPositions[i])<=friendlyUnitRange[i]) {
                        return enemyUnitIds[j];
                   }
              }
         } else {
              for(int j=0;j<friendlyUnitIds.length;j++) {
                   if(Math.abs(enemyUnitXPositions[i]-friendlyUnitXPositions[j])+Math.abs(enemyUnitYPositions[i]-friendlyUnitYPositions[j])<=enemyUnitRange[i]) {
                        return friendlyUnitIds[j];
                   }
              }
         }
         return null;
    }
}
