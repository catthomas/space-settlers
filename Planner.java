package stan5674;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
//import java.util.UUID;

import stan5674.SpaceCommand.Strategy;

/** Implements graph building and BFS
 * for PDDL high level strategy planning
 */
public class Planner {
	/** Graph variables **/
	private HashMap<Node, List<Node>> graph;
	private Node rootNode;
	private Node goalNode;
	
	/** Goal values for Planner **/
	private int goalBases;
	private int goalShips;
	private int minShips;
	
	/** SpaceCommand it is making plans for **/
	private SpaceCommand spaceCommand;
	
	/** Node class for the graph **/
	private class Node {
		Strategy strat;
		int currentShips;
		int currentBases;
		Node parent;
		
		/** Constructor for a node - state based on strategy, ships, and bases **/
		Node(Strategy strat, int currentShips, int currentBases, Node parent){
			this.strat = strat;
			this.currentShips = currentShips;
			this.currentBases = currentBases;
			this.parent = parent;
		}
		
		/** Checks if node is in goal state **/
		public boolean inGoalState(){
			if(currentShips >= goalShips && currentBases >= goalBases) return true;
			return false;
		}
	}
	
	/** Initialize Planner with goal values **/
	Planner(int goalShips, int goalBases, int minShips, SpaceCommand commander){
		this.goalShips = goalShips;
		this.goalBases = goalBases;
		this.minShips = minShips;
		this.graph = new HashMap<Node, List<Node>>();
		this.spaceCommand = commander;
	}
	
	/** Generates a graph based on given start node until it finds the goal state
	 * In essence, doing a Breadth First Search to the goal **/
	public void generateGraph(Strategy strat, int ships, int bases){
		this.rootNode = new Node(strat, ships, bases, null);
		if(rootNode.inGoalState()) return;
		Node startNode = rootNode;
		Queue<Node> builders = new LinkedList<Node>();
		
		do {
			List<Node> children = new ArrayList<Node>();
			
			//Add child node if preconditions satisfied
			if(spaceCommand.canBuildFleet(startNode.currentShips, startNode.currentBases)){
				//Set effects of build fleet strategy
				if(startNode.currentShips < minShips){
					children.add(new Node(Strategy.BUILD_FLEET, minShips, startNode.currentBases, startNode));
				} else {
					children.add(new Node(Strategy.BUILD_FLEET, startNode.currentBases + 1, startNode.currentBases, startNode));
				}
			}
			if(spaceCommand.canExpandEmpire(startNode.currentShips, startNode.currentBases)){
				//Set effects of expand empire strategy
				children.add(new Node(Strategy.EXPAND_EMPIRE, startNode.currentShips, startNode.currentShips, startNode));
			}
			if(spaceCommand.canFreeMine(startNode.currentShips)){
				//Set effects of free mine strategy
				children.add(new Node(Strategy.FREE_MINE, startNode.currentShips, startNode.currentBases, startNode));
			}
			//Add to graph Hashmap
			this.graph.put(startNode, children);
			
			//Add all child nodes to the queue
			builders.addAll(children);
			
			//Go to next node
			startNode = builders.poll();
			
			if(startNode.inGoalState()){
				this.goalNode = startNode;
			}
		} while(!builders.isEmpty() && !startNode.inGoalState());
	} //end genGraph
	
	/** Starts from the goal node, traverses to start node and returns appropriate strategy **/
	public Strategy findBestStrategy(){
		if(rootNode.inGoalState()){
			return Strategy.FREE_MINE; //default to free mine if root node is already at goal
		} else {
			//Backwards traversal from goal node to determine next action
			Node currentNode = goalNode;
			Strategy currentStrat = currentNode.strat;
			while(currentNode != rootNode){
				currentStrat = currentNode.strat;
				currentNode = currentNode.parent;
			}
			return currentStrat;
		}
	}
} //end Planner class
