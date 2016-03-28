package thom8296;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.*;

import spacesettlers.actions.*;
import spacesettlers.objects.*;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.*;

/**
 * Use a pilot's perspective to make decisions
 *
 */
public class PilotState {

	static int LOW_FUEL = 1000; 		//point of return
	static int CARGO_CAPACITY = 1000; 	
	static int SPEED = 100;				//speed of travel coefficient
	static float FOV = 1000;				//Max distance to consider objects
	static int FRONTIER = 250;			//min distance between bases
	static int MIN_BASE_FUEL = 1000;	//Minimum base fuel to be considered a candidate for refueling
	static int EXE_TIME = 10;			//minimum time between planning

	Ship vessel;
	HashMap<UUID, Set<Node>> graph = new HashMap<UUID, Set<Node>>();
	HashMap<UUID, Node> nodes = new HashMap<UUID, Node>();
	Stack<Node> path = new Stack<Node>();
	int exe = this.EXE_TIME; 				//time spent executing current plan

	public class Node{
		AbstractObject object;
		double g;
		double h;
		
		public Node(AbstractObject object){
			this.object = object;
			this.g = Double.POSITIVE_INFINITY;
			this.h = Double.POSITIVE_INFINITY;
		}
		
		public AbstractObject getObject(){
			return object;
		}
		
		public void setG(double g){
			this.g = g;
		}
		
		public void setH(double h){
			this.h = h;
		}
		
		public double getG(){
			return this.g;
		}
		
		public double getH(){
			return this.h;
		}
		
		public double getF(){
			return this.h + this.g;
		}
	}

	//call in agent init to set FOV radius of pilot
	public void setFOV(Toroidal2DPhysics space){
		this.FOV = Math.min(space.getHeight(), space.getWidth());
		this.FOV/=2;
	}


	/*
	* Maybe a hashmap connecting a UUID of an object to a set/array of connected objects
	* Path cost can be computed when traversing graph, or an adjacency matrix may be used
	*/
	public void genGraph(Toroidal2DPhysics space, Ship vessel){
		System.out.println("~~~~~~Vessel: " + vessel.getPosition() + "~~~~~~~");
		Set<AbstractObject> objects = this.objectsInFov(space, vessel);
		Set<AbstractObject> other;
		Set<AbstractObject> obs;
		Set<Node> children;
		this.graph.clear();
		this.nodes.clear();

		System.out.println("~~~~~~Objects in FOV: " + objects.size() + "~~~~~~~");

		for (AbstractObject a : objects){		//initialize all nodes
			this.nodes.put(a.getId(), new Node(a));
		}

		for (AbstractObject a : objects){
			children = new HashSet<Node>();
			other = new HashSet<AbstractObject>(objects);
			other.remove(a);	//Don't consider paths to self...

			for (AbstractObject b : other){
				obs = new HashSet<AbstractObject>(other);
				obs.remove(b);	//b isn't in the way...

				if (space.isPathClearOfObstructions(a.getPosition(), b.getPosition(), obs, vessel.getRadius()*2)){
					children.add(this.nodes.get(b.getId()));
				}
			}
			
			this.graph.put(a.getId(), children);
		}
		System.out.println("~~~~~~Graph nodes generated: " + this.graph.size() + "~~~~~~");
	};

	public boolean isObjectObstruction(AbstractObject obj){
		//if(obj.getClass() == Asteroid.class && !((Asteroid) obj).isMineable())
		if(obj.getClass() == Asteroid.class  || obj.getClass() == Ship.class
				|| obj.getClass() == Base.class) {
			return true;
		}	
		return false;
	}
	
	public AbstractObject generateGhostNode(Toroidal2DPhysics space, Ship vessel, AbstractObject start, AbstractObject end){
		Vector2D vec = space.findShortestDistanceVector(start.getPosition(), end.getPosition());
		vec = vec.rotate(Math.PI/2); //rotate vector by 90 degrees 
		
		//Scale vector to be appropriate distance (this case, radius of end object + 1.5 radius of ship)
		vec = vec.getUnitVector();
		vec = vec.multiply(end.getRadius()+1.5*vessel.getRadius());
		
		//Find goal position based on vector
		AbstractObject endCopy = end.deepClone();
		endCopy.resetId();
		Position goal = endCopy.getPosition().deepCopy();
		System.out.println("The initial x position: " + goal.getX() + " The initial Y position: " + goal.getY());
		goal.setX(goal.getX() + vec.getXValue());
		goal.setY(goal.getY() + vec.getYValue());
		endCopy.setPosition(goal);
		System.out.println("The final x position: " + endCopy.getPosition().getX() + " The final Y position: " + endCopy.getPosition().getY());
		
		return endCopy;
	}


	/*
	* Takes normalized heading from start to obj, radius of ship, and object to navigate around
	*/
	public Set<Node> getPassingNodes(Vector2D heading, int radius, AbstractObject obj){
		return null;
	};

	//Plans a path from one node on the hashmap to a goal node (both start and goal must be in the map), following A*
	public void planPath(Toroidal2DPhysics space, Node start, Node goal){
		Set<Node> closedList = new HashSet<Node>(); //list of nodes already evaluated
		Set<Node> openList = new HashSet<Node>(); //list of nodes to be evaluated
		HashMap<Node, Node> previousNode = new HashMap<Node, Node>();
		
		//Set start node values
		start.setG(0);
		start.setH(findH(space, start, goal)); 
		openList.add(start); //begin with start node
		
		//Plan path
		while(!openList.isEmpty()){
			//Set current node
			Node current = findLowestFNode(openList);

			System.out.println("~~~~~~~Visiting nodeID: "+ current.getObject().getId() +"~~~~~~");
			
			if(current.equals(goal)){ //goal found - return path!
				this.setPath(previousNode, start, goal);
				System.out.println("~~~~~Planning successful, size: " + this.path.size() +"~~~~~");
				return;
			}
			
			//Mark current as evaluated, add to closedList
			openList.remove(current); 
			closedList.add(current);
			
			//expand current's neighbors
			Set<Node> neighbors = this.graph.get(current.getObject().getId());

			System.out.println("~~~~~~~~Expanding " + neighbors.size() + " neighbors ~~~~~~~~");

			for(Node neighbor : neighbors){		
				if(!closedList.contains(neighbor)){
					double h = findH(space, neighbor, goal);
					double g = findG(space, current, neighbor);
					
					if(h < neighbor.getH()){
						neighbor.setH(h);
						previousNode.put(neighbor, current);
					}
					if(g < neighbor.getG()){
						neighbor.setG(g);
						previousNode.put(neighbor, current);
					}
					openList.add(neighbor); 
				}
			}
		}

		System.out.println("~~~~~~~PLANNING FAILED~~~~~~~~");
	}
	
	public void setPath(HashMap<Node, Node> previousNode, Node start, Node goal){
		this.path.clear();
		
		Node current = goal;

		while(current != start){
			this.path.push(current);
			current = previousNode.get(current);
		}

		this.exe = 0;
	}
	
	public Node findLowestFNode(Set<Node> nodes){
		Node best = null;		//pick one
		
		for(Node node : nodes){
		if(best == null || node.getF() < best.getF()){
				best = node;
			}
		}
		return best;
	}
	
	public double findH(Toroidal2DPhysics space, Node start, Node goal){
		AbstractObject nodeObject = start.getObject();
		if (nodeObject instanceof Asteroid && !((Asteroid)nodeObject).isMineable()){	//don't visit asteroids
			return Double.POSITIVE_INFINITY;
		}	else if (nodeObject instanceof Base && !((Base)nodeObject).getTeam().getShips().contains(this.vessel)){
			return Double.POSITIVE_INFINITY;	//don't run into other team's bases
		}

		return space.findShortestDistance(start.getObject().getPosition(), goal.getObject().getPosition());
	}
	
	public double findG(Toroidal2DPhysics space, Node start, Node end){
		return space.findShortestDistance(start.getObject().getPosition(), end.getObject().getPosition()) + start.getG();
	}
	
	public double findF(Toroidal2DPhysics space, Node start, Node end, Node goal){
		return findH(space, start, goal) + findG(space, start, end);
	}

	//How do we know when we reach a subgoal?
	public void assessPlan(Toroidal2DPhysics space, Ship vessel){
		if (!this.path.empty())
			if (vessel.getRadius() <= space.findShortestDistance(vessel.getPosition(), this.path.peek().getObject().getPosition())){
				this.path.pop();		//subgoal achieved
			}
		if (this.path.empty()){
			this.prePlan(space, vessel);	//out of plan, replan
		}
	};

	//finds the closest fuel beacon
	public Beacon findNearestBeacon(Toroidal2DPhysics space, Ship vessel){
		Set<Beacon> beacons = space.getBeacons();
		double shortest = Double.POSITIVE_INFINITY;
		Position location = vessel.getPosition();
		double dist;
		Beacon nearestBeacon = null;

		for (Beacon beacon : beacons) {
			dist = space.findShortestDistance(location, beacon.getPosition());

			if (dist < shortest) {
				shortest = dist;
				nearestBeacon = beacon;
			}
		}

		return nearestBeacon;
	}

	//finds the base nearest to the vessel. If we want to consider this for refueling, set refuel to true
	public Base findNearestBase(Toroidal2DPhysics space, Ship vessel, boolean refuel){
		Set<Base> bases = space.getBases();
		double shortest = Double.POSITIVE_INFINITY;
		Position location = vessel.getPosition();
		double dist;
		Base nearestBase = null;

		//find closest base
		for (Base base : bases) {
			if (base.getTeamName().equalsIgnoreCase(vessel.getTeamName())){
				dist = space.findShortestDistance(location, base.getPosition());

				if (dist < shortest) {		
					if (refuel){
						if (base.getEnergy() >= MIN_BASE_FUEL){
							shortest = dist;
							nearestBase = base;
						}
					} else {
						shortest = dist;
						nearestBase = base;
					}
				}
			}
		}
		//System.out.println("~~~~~~BASE IS: " + nearestBase.getId());

		return nearestBase;
	}

	//find nearest refueling station, either base or beacon
	public AbstractObject nearestRefuel(Toroidal2DPhysics space, Ship vessel){
		Position location = vessel.getPosition();

		Base nearestBase = findNearestBase(space, vessel, true);
		Beacon nearestBeacon = findNearestBeacon(space, vessel);

		if (space.findShortestDistance(location, nearestBase.getPosition()) <= space.findShortestDistance(location, nearestBeacon.getPosition())){
			return nearestBase;
		} else {
			return nearestBeacon;
		}
	}

	//Pilot's brains
	public AbstractAction decideAction(Toroidal2DPhysics space, Ship vessel){
		Position currentPosition = vessel.getPosition();
		AbstractObject goal;

		//System.out.println("Step: " + space.getCurrentTimestep());

		//get refueled
		if (vessel.getEnergy() < LOW_FUEL){
			//System.out.println("~~~~~MOVING TO REFUEL~~~~~");
			goal = nearestRefuel(space, vessel);

			AbstractAction newAction = null;
			
			// if no object found, skip turn
			if(goal == null) {
				newAction = new DoNothingAction(); 
			} else {
				newAction = new MoveAction(space, currentPosition, goal.getPosition(), optimalApproach(space, currentPosition, goal.getPosition()));
			};
			return newAction;
		}
		//return resources to base
		if (vessel.getResources().getTotal() > CARGO_CAPACITY){
			//System.out.println("~~~~~MOVING TO BASE~~~~~");
			goal = findNearestBase(space, vessel, false);

			MoveAction move = new MoveAction(space, currentPosition, goal.getPosition(), optimalApproach(space, currentPosition, goal.getPosition()));
			//move.setKvRotational(3.2);
			//move.setKpRotational(3.6);
			return move;
		}


		//System.out.println("~~~~~MOVING TO PROSPECT~~~~~");
		goal = findNearestProspect(space, vessel);

		//System.out.println(goal.getPosition().getTranslationalVelocity());
		MoveAction move = new MoveAction(space, currentPosition, goal.getPosition(), optimalApproach(space, currentPosition, goal.getPosition()));
		//move.setKvRotational(3.2);
		//move.setKpRotational(3.6);
		return move;
	};

	public void prePlan(Toroidal2DPhysics space, Ship vessel){
		this.genGraph(space, vessel);

		Node start = this.nodes.get(vessel.getId());
		Node goal;

		if (vessel.getEnergy() < LOW_FUEL){
			System.out.println("~~~~~Planning TO REFUEL~~~~~");
			goal = this.nodes.get(nearestRefuel(space, vessel).getId());
		}
		//return resources to base
		else if (vessel.getResources().getTotal() > CARGO_CAPACITY){
			System.out.println("~~~~~Planning TO BASE~~~~~");
			goal = this.nodes.get(findNearestBase(space, vessel, false).getId());
		} else {	//just get resources
			System.out.println("~~~~~Planning TO PROSPECT~~~~~");
			goal = this.nodes.get(findNearestProspect(space, vessel).getId());
		}

		this.planPath(space, start, goal);	//find best path to goal
	}

	public AbstractAction executePlan(Toroidal2DPhysics space, Ship vessel){
		System.out.println("~~~~~~~Starting plan Execution: "+ this.exe +"~~~~~~~");
		
		this.vessel = vessel;

		if (this.exe >= this.EXE_TIME || this.path.isEmpty()){		//Time to replan
			this.prePlan(space, vessel);
		}
		
		Position goal;

		if (!this.path.empty()){
			goal = this.path.peek().getObject().getPosition();		//get goal location
		}
		else {
			return this.decideAction(space, vessel);			//failsafe heuristic
		}

		this.exe+=1;

		System.out.println("~~~~~~Executing Plan: " + this.exe + "~~~~~~");
		
		MoveAction move = new MoveAction(space, vessel.getPosition(), goal, this.optimalApproach(space, vessel.getPosition(), goal));
		move.setKvRotational(4);
		move.setKpRotational(4);
		return move;
	};

	//pilot decides what to buy from shop
	public PurchaseTypes shop(Toroidal2DPhysics space, Ship vessel, ResourcePile funds, PurchaseCosts prices){
		Position currentPosition = vessel.getPosition();
		if (prices.canAfford(PurchaseTypes.BASE, funds)) {;	//buy bases when you are in the frontier
			for (Base base : space.getBases()) {
				if (base.getTeamName().equalsIgnoreCase(vessel.getTeamName())) {
					double distance = space.findShortestDistance(currentPosition, base.getPosition());
					if (distance < FRONTIER) { 		//do not buy a base if within minimum frontier distance
						return null;
					}
				}
			}

			return  PurchaseTypes.BASE;
		}

		return null;
	};

	public Asteroid findNearestProspect(Toroidal2DPhysics space, Ship vessel){
		List<Asteroid> prospects = getMinableAsteroids(space);
		double shortest = Double.POSITIVE_INFINITY;
		Position location = vessel.getPosition();
		double dist;
		Asteroid nearestProspect = null;

		for (Asteroid prospect : prospects){
			dist = space.findShortestDistance(location, prospect.getPosition());

			if (dist<shortest){
				shortest = dist;
				nearestProspect = prospect;
			}
		}

		return nearestProspect;
	}

	public Vector2D optimalApproach(Toroidal2DPhysics space, Position current, Position goal){
		//return fastest velocity vestor that the vessel can travel and still reach goal
		//Vector2D goalVelocity = goal.getTranslationalVelocity();  //movement vector of goal object--we can use this to take better paths
		Vector2D distVec = space.findShortestDistanceVector(current, goal).getUnitVector();		//orientation toward goal
		
		//consider slower approaches when low on energy
		
		if(space.findShortestDistance(current, goal) < 100){ //approaching target, slow down
			distVec = distVec.multiply(SPEED/2.0);
		} else {
			distVec = distVec.multiply(SPEED);		//controls speed in orientation
		}

		//Vector2D cruisingSpeed = new Vector2D(50, 50);

		return distVec;
	};		

	//helper function to only return asteroids with resources
	public List<Asteroid> getMinableAsteroids(Toroidal2DPhysics space){
		List<Asteroid> asteroids = new ArrayList<Asteroid>(space.getAsteroids());
		List<Asteroid> minable = new ArrayList<Asteroid>();

		for (Asteroid ast : asteroids){
			if (ast.isMineable()){
				minable.add(ast);
			} 
		}

		return minable;
	};
	

	/**
	 * Sorts the asteroid in space by total resource value.
	 * Can be used to target higher value asteroids by the agent. 
	 * 
	 * @return List of asteroids ordered by total resources
	 */
	public List<Asteroid> asteroidsByResources(Toroidal2DPhysics space) {
		//Comparator that utilizes total resource value 
		Comparator<Asteroid> value = new Comparator<Asteroid>() {
	        @Override
	        public int compare(Asteroid a1, Asteroid a2) {
	            if(a1.getResources().getTotal() < a2.getResources().getTotal()){
	            	return -1;
	            } else if (a1.getResources().getTotal() > a2.getResources().getTotal()){
	            	return 1;
	            } else{
	            	return 0;
	            }
	        }
	    };
	    
		List<Asteroid> asteroids = getMinableAsteroids(space);
		Collections.sort(asteroids, value); //sort asteroids by total resource value
		return asteroids;
	} //end asteroidsByValue
	
	/**
	 * Returns a list of all objects (asteroids, bases, other ships, beacons) within the pilot's field of view
	 * TODO: test this method
	 * @param space
	 * @param object
	 * @param radius
	 * @return Set of all objects within FOV
	 */
	public Set<AbstractObject> objectsInFov(Toroidal2DPhysics space, Ship ship){
		Set<AbstractObject> objects = new HashSet<AbstractObject>();
		Position currentPosition = ship.getPosition();

		for (AbstractObject obj : space.getAllObjects()) {
			double dist = space.findShortestDistance(currentPosition, obj.getPosition());

			if (dist < this.FOV) {
				objects.add(obj);
			}
		}
		return objects;
	}

	/**
	 * Checks to see if the given base is about to die. For this,
	 * it checks 1.) if the base is the home base and 2.) if it has
	 * less than 100 energy available. 
	 * 
	 * @param base
	 * @return Boolean saying whether the given base is about to die!
	 */
	public boolean isBaseNearDeath(Base base){
		if(base.isHomeBase() && base.getEnergy() <= 100){
			return true;
		}
		return false;
	} //end isBaseNearDeath
}