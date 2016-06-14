package burlap.behavior.singleagent.planning.deterministic.uninformed.dfs;

import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.SearchNode;
import burlap.mdp.auxiliary.stateconditiontest.StateConditionTest;
import burlap.mdp.core.Action;
import burlap.mdp.core.state.State;
import burlap.mdp.singleagent.SADomain;
import burlap.statehashing.HashableState;
import burlap.statehashing.HashableStateFactory;

import java.util.*;


/**
 * This is a modified version of DFS that maintains a memory of the last n states it has previously expanded.
 * Any potential path that will lead to a state that is in its memory will be pruned from the search tree. This valueFunction
 * gives some of the reduced searching power gained when using DFS with a closed with, but with a constant memory space cost
 * that is traded for less pruning power. If there is an imposed search depth and a potential path will explore
 * a state in the memory, but will explore it at an earlier depth, the valueFunction will explore it anyway, since it's
 * possible it will lead to a goal given that it can expand further than the previous exploration.
 * 
 * <p>
 * If a terminal function is provided via the setter method defined for OO-MDPs, then the search algorithm will not expand any nodes
 * that are terminal states, as if there were no actions that could be executed from that state. Note that terminal states
 * are not necessarily the same as goal states, since there could be a fail condition from which the agent cannot act, but
 * that is not explicitly represented in the transition dynamics.
 * 
 * @author James MacGlashan
 *
 */
public class LimitedMemoryDFS extends DFS {

	
	/**
	 * the size of the memory; that is, the number of recently expanded search nodes the valueFunction will remember.
	 */
	protected int									memorySize;
	
	/**
	 * A queue for storing the most recently expanded nodes.
	 */
	protected LinkedList<HashableState>			memoryQueue;
	
	
	/**
	 * Stores the depth at which each state in the memory was explored.
	 */
	protected Map <HashableState, Integer>			memoryStateDepth;
	
	
	
	/**
	 * Constructor for memory limited DFS
	 * @param domain the domain in which to plan
	 * @param gc indicates the goal states
	 * @param hashingFactory the state hashing factory to use
	 * @param maxDepth depth limit of DFS. -1 specifies no limit.
	 * @param maintainClosed whether to maintain a closed list or not
	 * @param optionsFirst whether to explore paths generated by options first.
	 * @param memorySize the number of most recently expanded nodes to remember.
	 */
	public LimitedMemoryDFS(SADomain domain, StateConditionTest gc, HashableStateFactory hashingFactory, int maxDepth,
							boolean maintainClosed, boolean optionsFirst, int memorySize) {
		super(domain, gc, hashingFactory, maxDepth, maintainClosed,
				optionsFirst);
		
		this.memorySize = memorySize;
		
	}



	/**
	 * Plans and returns a {@link burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy}. If
	 * a {@link State} is not in the solution path of this planner, then
	 * the {@link burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy} will throw
	 * a runtime exception. If you want a policy that will dynamically replan for unknown states,
	 * you should create your own {@link burlap.behavior.singleagent.planning.deterministic.DDPlannerPolicy}.
	 * @param initialState the initial state of the planning problem
	 * @return a {@link burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy}.
	 */

	@Override
	public SDPlannerPolicy planFromState(State initialState){
		
		memoryQueue = new LinkedList<HashableState>();
		memoryStateDepth = new HashMap<HashableState, Integer>();
		
		return super.planFromState(initialState);
		
	}
	
	
	
	/**
	 * Runs DFS from a given search node, keeping track of its current depth. This method is recursive. The memory
	 * of the memorySize most recently expanded nodes will be stored.
	 * @param n the current search node
	 * @param depth the current depth of the search
	 * @param statesOnPath the states that have bee explored on the current search path
	 * @return the SearchNode with a goal, or null if it cannot be found from this state.
	 */
	protected SearchNode dfs(SearchNode n, int depth, Set<HashableState> statesOnPath){
		
		numVisted++;
		
		if(gc.satisfies(n.s.s())){
			//found goal!
			return n;
		}
		
		if(maxDepth != -1 && depth > maxDepth){
			return null; //back track
		}
		
		if(this.model.terminal(n.s.s())){
			return null; //treat like dead end
		}
		
		//otherwise we need to generate successors and search them
		
		statesOnPath.add(n.s);
		
		if(memoryQueue.size() >= memorySize){
			HashableState mempop = memoryQueue.poll();
			memoryStateDepth.remove(mempop);
			
		}
		
		memoryQueue.offer(n.s);
		memoryStateDepth.put(n.s, depth);
		
		
		//shuffle actions for a random walk, but keep options as priority if set that way
		List<Action> gas = this.applicableActions(n.s.s());
		if(optionsFirst){
			int no = this.numOptionsInGAs(gas);
			this.shuffleGroundedActions(gas, 0, no);
			this.shuffleGroundedActions(gas, no, gas.size());
		}
		else{
			this.shuffleGroundedActions(gas, 0, gas.size());
		}
		
		//generate a search successors from the order of grounded actions
		for(Action ga : gas){
			HashableState shp = this.stateHash(this.model.sample(n.s.s(), ga).op);
			boolean notInMemory = true;
			Integer memoryDepth = memoryStateDepth.get(shp);
			if(memoryDepth != null){
				int md = memoryDepth;
				if(maxDepth == -1 || md <= depth+1){
					notInMemory = false;
				}
			}
			if(!statesOnPath.contains(shp) && notInMemory){
				SearchNode snp = new SearchNode(shp, ga, n);
				SearchNode result = this.dfs(snp, depth+1, statesOnPath);
				if(result != null){
					return result;
				}
			}
		}
		
		//no successors found a solution
		if(!maintainClosed){
			statesOnPath.remove(n.s);
		}
		return null;
	}

}
