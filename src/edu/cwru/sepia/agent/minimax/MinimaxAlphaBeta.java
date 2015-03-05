package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MinimaxAlphaBeta extends Agent {

    private final int numPlys;
    private boolean isMax = true;

    public MinimaxAlphaBeta(int playernum, String[] args)
    {
        super(playernum);

        if(args.length < 1)
        {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }

        numPlys = Integer.parseInt(args[0]);
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
                numPlys,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);

        return bestChild.action;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this.
     *
     * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
     * and book for more information.
     *
     * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
     * code into other functions and methods)
     *
     * @param node The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta The current best value for the minimizing node from this node to the root
     * @return The best child of this node with updated values
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta)
    { 
    	List<GameStateChild> children = orderChildrenWithHeuristics(node.state.getChildren());
    	if (depth == 0 || children==null || children.size() == 0) {
            //return node; unnecessary, happens at end of method
    	} else if (isMax()) {
    		double v = Double.MIN_VALUE;
    		  for(GameStateChild child: children) {
    			  
    			  setMax(false);
    			  GameStateChild bestNode = alphaBetaSearch(child, depth - 1, alpha, beta);
	              v = Math.max(v, bestNode.state.getUtility());
	              if (beta <= v) {
	                  break; // alpha pruning
	              }
	              if(alpha < v) {
	            	  alpha = v;
	            	  node = bestNode;
	              }

    		  }
    	} else {
	          double v = Double.MAX_VALUE;
	          for(GameStateChild child: children) {
	        	  setMax(true);
    			  GameStateChild bestNode = alphaBetaSearch(child, depth - 1, alpha, beta);
	              v = Math.min(v, bestNode.state.getUtility());
	              if (v <= alpha) {
	                  break; // alpha pruning
	              }
	              if(beta > v) {
	            	  beta = v;
	            	  node = bestNode;
	              }

	          }
    	}
        return node;
    }

    private void setMax(boolean b) {
		isMax = b;		
	}

	private boolean isMax() {
		return isMax;
	}
    


	/**
     * You will implement this.
     *
     * Given a list of children you will order them according to heuristics you make up.
     * See the assignment description for suggestions on heuristics to use when sorting.
     *
     * Use this function inside of your alphaBetaSearch method.
     *
     * Include a good comment about what your heuristics are and why you chose them.
     *
     * @param children
     * @return The list of children sorted by your heuristic.
     */
    public List<GameStateChild> orderChildrenWithHeuristics(List<GameStateChild> children)
    {
         if(children==null) {
              return null;
         }
         ArrayList<GameStateChild> children2=new ArrayList<GameStateChild>();
         children2.addAll(children);
         Collections.sort(children2, new GameStateComparator());
         System.out.println(children2);
         return children2;
    }
    
    /* A comparator for sorting GameStateChild in orderChildrenWithHeuristics
     * Simply compares the utility (from GameState,getUtility) of state1 and state2.
     * Returns 1 if state1 has greater utility, 0 if the utilities are equal, -1 if state2 has the greater utility
     */
    private class GameStateComparator implements Comparator<GameStateChild>
    {
          @Override
          public int compare(GameStateChild state1, GameStateChild state2) {
               double relativeUtility=state1.state.getUtility()-state2.state.getUtility();
               if(relativeUtility>0) {
                    return 1;
               } else if(relativeUtility==0) {
                    return 0;
               } else {
                    return -1;
               }
          }
    }
}
