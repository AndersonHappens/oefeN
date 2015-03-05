package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
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
 */
public class GameState {
     
     private static final Integer myPlayerNum=new Integer(0);
     
     private boolean myTurnNext;

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
    public GameState(State.StateView state) {
         xSize=state.getXExtent();
         ySize=state.getYExtent();
         
         Integer[] resourceIds=state.getAllResourceIds().toArray(new Integer[0]);
         obstaclesXPositions=new int[resourceIds.length];
         obstaclesYPositions=new int[resourceIds.length];
         for(int i=0;i<resourceIds.length;i++) {
              obstaclesXPositions[i]=state.getResourceNode(resourceIds[i]).getXPosition();
              obstaclesYPositions[i]=state.getResourceNode(resourceIds[i]).getYPosition();
         }
         
         friendlyUnitIds=state.getUnitIds(myPlayerNum).toArray(new Integer[0]);
         List<UnitView> myUnits = state.getUnits(myPlayerNum);             // support for arbitrary amount of friendly units
         friendlyUnitXPositions = new int[myUnits.size()];
         friendlyUnitYPositions = new int[myUnits.size()];
         friendlyUnitRange = new int[myUnits.size()];
         for(int i=0;i<myUnits.size();i++) {
              friendlyUnitXPositions[i]=myUnits.get(i).getXPosition();
              friendlyUnitYPositions[i]=myUnits.get(i).getYPosition();
              friendlyUnitRange[i]=myUnits.get(i).getTemplateView().getRange();
         }
         
         Integer[] players=state.getPlayerNumbers();                       //support for arbitrary amount of enemies and enemy units
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
     * You will implement this function.
     *
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     *
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     *
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() {
    	double util= 0;
    	Pair<Integer, Double> closestEnemy;
		for(int j = 0; j< friendlyUnitXPositions.length; j++) {
			closestEnemy = getClosestEnemy(friendlyUnitXPositions[j], friendlyUnitYPositions[j]);
     	    	for(int i = 0; i< enemyUnitXPositions.length; i++) {
     	    		if(closestEnemy.a == i) {
     	    		     util += 100-(DistanceMetrics.chebyshevDistance(friendlyUnitXPositions[j], friendlyUnitYPositions[j], enemyUnitXPositions[i], enemyUnitYPositions[i])+1);
     	    		} //else {
     	    			//util -= xSize*ySize/100/(DistanceMetrics.chebyshevDistance(friendlyUnitXPositions[j], friendlyUnitYPositions[j], enemyUnitXPositions[i], enemyUnitYPositions[i])+1);
     	    		//}
         		}
         	}
        return -util;
    }
    /**
     * 
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
    public List<GameStateChild> getChildren() {
         ArrayList<GameStateChild> children = new ArrayList<GameStateChild>();
         GameState copy=copy(this);
         copy.myTurnNext=!this.myTurnNext;
         children.add(new GameStateChild(new HashMap<Integer, Action>(), copy));

         ArrayList<GameStateChild> newChildren = new ArrayList<GameStateChild>();
         if(myTurnNext) {
              //our turn
              for(int i=0;i<friendlyUnitIds.length;i++) {
                   while(!children.isEmpty()) {
                        GameStateChild current=children.remove(0);
                        Integer enemyToAttack=canAttack(i, true);
                        if(enemyToAttack!=null) {
                             GameStateChild newChild=new GameStateChild(new HashMap<Integer,Action>(),GameState.copy(current.state));
                             newChild.action.putAll(current.action);
                             newChild.action.put(new Integer(i),Action.createPrimitiveAttack(friendlyUnitIds[i], enemyToAttack));
                             newChildren.add(newChild);
                        } else {
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
    
    private static boolean isValidMove(GameState state, int xPos, int yPos) {
         System.out.println(xPos+"  "+yPos);
         if(xPos<0 || xPos>=state.xSize || yPos<0 || yPos>=state.ySize) {
              return false;
         }
         System.out.println("passed in bounds check");
         for(int i=0;i<state.obstaclesXPositions.length;i++) {
              if(xPos==state.obstaclesXPositions[i] && yPos==state.obstaclesYPositions[i]) {
                   return false;
              }
         }
         for(int i=0;i<state.enemyUnitXPositions.length;i++) {
              if(xPos==state.enemyUnitXPositions[i] && yPos==state.enemyUnitYPositions[i]) {
                   return false;
              }
         }
         for(int i=0;i<state.friendlyUnitXPositions.length;i++) {
              if(xPos==state.friendlyUnitXPositions[i] && yPos==state.friendlyUnitYPositions[i]) {
                   return false;
              }
         }
         return true;
    }
    
    private Integer canAttack(int i, boolean whoseAttack) {      // whose attack true if my units attacking, false if enemy units attacking
         if(whoseAttack) {                                       // i refers to the index of the unit in the arrays
              for(int j=0;j<enemyUnitIds.length;j++) {
                   if(Math.abs(enemyUnitXPositions[j]-friendlyUnitXPositions[i])+Math.abs(enemyUnitYPositions[j]-friendlyUnitYPositions[i])<friendlyUnitRange[i]) {
                        return enemyUnitIds[j];
                   }
              }
         } else {
              for(int j=0;j<friendlyUnitIds.length;j++) {
                   if(Math.abs(enemyUnitXPositions[i]-friendlyUnitXPositions[j])+Math.abs(enemyUnitYPositions[i]-friendlyUnitYPositions[j])<enemyUnitRange[i]) {
                        return friendlyUnitIds[j];
                   }
              }
         }
         return null;
    }
    
    public String toString() {
         String s="";
         s+="Friendlies at: ";
         for(int i=0;i<friendlyUnitXPositions.length;i++) {
              s+="("+friendlyUnitXPositions[i]+", "+friendlyUnitYPositions[i]+"), ";
         }
         s+="Enemies at: ";
         for(int i=0;i<enemyUnitXPositions.length;i++) {
              s+="("+enemyUnitXPositions[i]+", "+enemyUnitYPositions[i]+"), ";
         }
         return s;
    }
}
