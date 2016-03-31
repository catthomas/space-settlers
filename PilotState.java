package stan5674;

import java.util.*;

import spacesettlers.actions.*;
import spacesettlers.graphics.LineGraphics;
import spacesettlers.graphics.SpacewarGraphics;
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
	static int CARGO_CAPACITY = 1500; 	
	static int SPEED = 100;				//speed of travel coefficient
	static float FOV = 1000;			//Max distance to consider objects
	static int FRONTIER = 250;			//min distance between bases
	static int MIN_BASE_FUEL = 1000;	//Minimum base fuel to be considered a candidate for refueling
	static int EXE_TIME = 10;			//minimum time between planning
	static int CLOSE_ESC = 75;			//object is considered particularly close

	Ship vessel;
	HashMap<UUID, Set<Node>> graph = new HashMap<UUID, Set<Node>>();
	HashMap<UUID, Node> nodes = new HashMap<UUID, Node>();
	Stack<Node> path = new Stack<Node>();
	Set<SpacewarGraphics> graphics = new HashSet<SpacewarGraphics>(); //Holds markers for A* path
	int exe = this.EXE_TIME; 				//time spent executing current plan


	//Node structure
	public class Node{
		public AbstractObject object;
		public double g;
		public double h;
		public boolean isBypass;
		
		public Node(AbstractObject object){
			this.object = object;
			this.isBypass = false;
			this.g = Double.POSITIVE_INFINITY;
			this.h = Double.POSITIVE_INFINITY;
		}

		public Node(AbstractObject object, boolean isBypass){
			this.object = object;
			this.isBypass = isBypass;
			this.g = Double.POSITIVE_INFINITY;
			this.h = Double.POSITIVE_INFINITY;
		}
	}

	//return the current graphics
	 public Set<SpacewarGraphics> getPathGraphics(){
		 return graphics;
	 }
	 
	public PilotState(Toroidal2DPhysics space){
		this.setFOV(space);
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
	public void genGraph(Toroidal2DPhysics space, Ship vessel, AbstractObject goal){
		//System.out.println("~~~~~~Vessel: " + vessel.getPosition() + "~~~~~~~");
		Set<AbstractObject> objects = this.objectsInFov(space, vessel);
		objects.add(goal);		//make sure goal is in graph	
		Set<AbstractObject> other = new HashSet<AbstractObject>(objects);
		Set<AbstractObject> obs;
		Set<Node> children = new HashSet<Node>();
		this.graph.clear();
		this.nodes.clear();

		//System.out.println("~~~~~~Objects in FOV: " + objects.size() + "~~~~~~~");

		for (AbstractObject a : objects){		//initialize all nodes
			this.nodes.put(a.getId(), new Node(a));
		}

		for (AbstractObject a : objects){
			children.clear() ;
			other.remove(a);	//Don't consider paths to self...
			obs = new HashSet<AbstractObject>(other);


			for (AbstractObject b : other){
				obs.remove(b);	//b isn't in the way...

				if (space.isPathClearOfObstructions(a.getPosition(), b.getPosition(), obs, vessel.getRadius()*2)){
					children.add(this.nodes.get(b.getId()));
				}	/*else {
					Node bypass = this.generateBypassNode(space, a, obs, b);
					if (bypass != null)
						children.add(bypass);	//generates a node beside obstruction
					//children.add(this.nodes.get(b.getId()));
				}*/

				obs.add(b);
			}

			other.add(a);
			this.graph.put(a.getId(), children);
		}
		//System.out.println("~~~~~~Graph nodes generated: " + this.graph.size() + "~~~~~~");
	};
	
	//make new node beside any obstruction to end node
	public Node generateBypassNode(Toroidal2DPhysics space, AbstractObject start, Set<AbstractObject> obs, AbstractObject end){
		HashSet<AbstractObject> canOb = new HashSet<AbstractObject>();
		for (AbstractObject ob : obs){
			canOb.clear();
			canOb.add(ob);
			if (!space.isPathClearOfObstructions(start.getPosition(), end.getPosition(), canOb, vessel.getRadius()*2)){
				if (ob instanceof Beacon){		//never bypass a beacon
					return null;
				}
				Vector2D vec = space.findShortestDistanceVector(start.getPosition(), ob.getPosition());
				if (vec.getMagnitude() < this.CLOSE_ESC){
					vec = vec.getUnitVector().rotate(Math.PI/2); //rotate vector by 90 degrees 
					
					//Scale vector to be appropriate distance (this case, radius of end object + 1.5 radius of ship)
					vec = vec.multiply(ob.getRadius()+3*vessel.getRadius());
					
					//Find goal position based on vector
					Position goal = ob.getPosition().deepCopy();
					//System.out.println("The initial x position: " + goal.getX() + " The initial Y position: " + goal.getY());
					goal.setX(goal.getX() + vec.getXValue());
					goal.setY(goal.getY() + vec.getYValue());

					//if bypass locatoin is free, add it to graph
					if (space.isLocationFree(goal, this.vessel.getRadius()*2)){
						AbstractObject bypass = new Beacon(goal);		//some object

						//System.out.println("The final x position: " + bypass.getPosition().getX() + " The final Y position: " + bypass.getPosition().getY());
						
						this.nodes.put(bypass.getId(), new Node(bypass, true));
						HashSet<Node> temp = new HashSet<Node>();
						temp.add(this.nodes.get(end.getId()));
						this.graph.put(bypass.getId(), temp);	//add end node as child node of bypass
						return this.nodes.get(bypass.getId());
					}
				}
				return null;
			}
		}

		return null;
	}

	//Plans a path from one node on the hashmap to a goal node (both start and goal must be in the map), following A*
	public void planPath(Toroidal2DPhysics space, Node start, Node goal){
		Set<Node> closedList = new HashSet<Node>(); //list of nodes already evaluated
		Set<Node> openList = new HashSet<Node>(); //list of nodes to be evaluated
		HashMap<Node, Node> previousNode = new HashMap<Node, Node>();
		
		//Set start node values
		start.g = 0;
		start.h = findH(space, start, goal); 
		openList.add(start); //begin with start node
		
		//Plan path
		while(!openList.isEmpty()){
			//Set current node
			Node current = findLowestFNode(openList);

			//System.out.println("~~~~~~~Visiting nodeID: "+ current.object.getId() +"~~~~~~");
			
			if(current.equals(goal)){ //goal found - return path!
				this.setPath(space, previousNode, start, goal);
				//System.out.println("~~~~~Planning successful, size: " + this.path.size() +"~~~~~");
				return;
			}
			
			//Mark current as evaluated, add to closedList
			openList.remove(current);
			closedList.add(current);
			
			//expand current's neighbors
			Set<Node> neighbors = this.graph.get(current.object.getId());

			//System.out.println("~~~~~~~~Expanding " + neighbors.size() + " neighbors ~~~~~~~~");

			for(Node neighbor : neighbors){		
				if(!closedList.contains(neighbor)){
					double h = findH(space, neighbor, goal);
					double g = findG(space, current, neighbor);
					
					if(h < neighbor.h){
						neighbor.h = h;
						previousNode.put(neighbor, current);
					}
					if(g < neighbor.g){
						neighbor.g = g;
						previousNode.put(neighbor, current);
					}
					openList.add(neighbor); 
				}
			}
		}

		//System.out.println("~~~~~~~PLANNING FAILED~~~~~~~~");
	}
	
	public void setPath(Toroidal2DPhysics state, HashMap<Node, Node> previousNode, Node start, Node goal){
		this.path.clear();
		System.out.println("--------- SET PATH CALLED -----------");
		Node current = goal; //Start from end, assume goal was found
		graphics.clear(); //clear graphics
		Position prevPos;

		while(current != start){
			this.path.push(current); //add to path
			prevPos = current.object.getPosition(); //get position of node
			//graphics.add(new CircleGraphics(2, Color.RED, prevPos));	//mark location of node on map
			current = previousNode.get(current); //switch to next node
			//add a line between this node and last
			graphics.add(new LineGraphics(current.object.getPosition(), prevPos, 
					state.findShortestDistanceVector(current.object.getPosition(), prevPos))); 
		}

		this.exe = 0;
	}
	
	public Node findLowestFNode(Set<Node> nodes){
		Node best = null;		//pick one
		
		for(Node node : nodes){
		if(best == null || (node.g+best.h) < (best.g+best.h)){
				best = node;
			}
		}
		return best;
	}
	
	public double findH(Toroidal2DPhysics space, Node start, Node goal){
		AbstractObject nodeObject = start.object;
		if (nodeObject instanceof Asteroid && !((Asteroid)nodeObject).isMineable()){	//don't visit asteroids
			return Double.POSITIVE_INFINITY;
		} else if (nodeObject instanceof Base && !((Base)nodeObject).getTeam().getShips().contains(this.vessel)){
			return Double.POSITIVE_INFINITY;	//don't run into other team's bases
		}
	//System.out.println("~~~~~~~Getting H for: "+ nodeObject.getPosition() + " to: " + goal.object.getPosition() + "~~~~~~~~~~");
		return space.findShortestDistance(nodeObject.getPosition(), goal.object.getPosition());
	}
	
	public double findG(Toroidal2DPhysics space, Node start, Node end){
		return space.findShortestDistance(start.object.getPosition(), end.object.getPosition()) + start.g;
	}
	
	public double findF(Toroidal2DPhysics space, Node start, Node end, Node goal){
		return findH(space, start, goal) + findG(space, start, end);
	}

	//How do we know when we reach a subgoal?
	public void assessPlan(Toroidal2DPhysics space, Ship vessel){
		if (!this.path.empty())
			if (vessel.getRadius() <= space.findShortestDistance(vessel.getPosition(), this.path.peek().object.getPosition())){
				this.path.pop();			//subgoal achieved
			}
		if (this.path.empty()){				//plan complete
			this.exe = this.EXE_TIME;		//replan next timestep
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
	public AbstractObject findNearestRefuel(Toroidal2DPhysics space, Ship vessel){
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
			goal = findNearestRefuel(space, vessel);

			AbstractAction newAction = null;
			
			// if no object found, skip turn
			if(goal == null) {
				newAction = new DoNothingAction(); 
			} else {
				newAction = optimalApproach(space, currentPosition, goal.getPosition());
			};

			return newAction;
		}
		//return resources to base
		if (vessel.getResources().getTotal() > CARGO_CAPACITY){
			//System.out.println("~~~~~MOVING TO BASE~~~~~");
			goal = findNearestBase(space, vessel, false);

			return optimalApproach(space, currentPosition, goal.getPosition());
		}


		//System.out.println("~~~~~MOVING TO PROSPECT~~~~~");
		goal = findNearestProspect(space, vessel);

		//System.out.println(goal.getPosition().getTranslationalVelocity());
		return optimalApproach(space, currentPosition, goal.getPosition());
	};

	public void prePlan(Toroidal2DPhysics space, Ship vessel){
		Node start = this.nodes.get(vessel.getId());
		AbstractObject goal;

		if (vessel.getEnergy() < LOW_FUEL){
			System.out.println("~~~~~Planning TO REFUEL~~~~~");
			goal = findNearestRefuel(space, vessel);
		}
		//return resources to base
		else if (vessel.getResources().getTotal() > CARGO_CAPACITY){
			System.out.println("~~~~~Planning TO BASE~~~~~");
			goal = findNearestBase(space, vessel, false);
		} else {	//just get resources
			System.out.println("~~~~~Planning TO PROSPECT~~~~~");
			goal = findNearestProspect(space, vessel);
		}

		this.genGraph(space, vessel, goal);
		this.planPath(space, start, this.nodes.get(goal.getId()));	//find best path to goal
	}

	public AbstractAction executePlan(Toroidal2DPhysics space, Ship vessel){
		//System.out.println("~~~~~~~Starting plan Execution: "+ this.exe +"~~~~~~~");
		
		this.vessel = vessel;

		if (this.exe >= this.EXE_TIME || this.path.isEmpty()){		//Time to replan
			this.prePlan(space, vessel);
		}
		
		Position goal;

		if (!this.path.empty()){
			goal = this.path.peek().object.getPosition();		//get goal location
		}
		else {
			//System.out.println("~~~~~~~~FAIL SAFE~~~~~~~~~");
			return this.decideAction(space, vessel);			//failsafe heuristic
		}

		this.exe+=1;

		//System.out.println("~~~~~~Executing Plan: " + this.exe + "~~~~~~");

		return this.optimalApproach(space, vessel.getPosition(), goal);
	};

	//pilot decides what to buy from shop
	public PurchaseTypes shop(Toroidal2DPhysics space, Ship vessel, ResourcePile funds, PurchaseCosts prices){
		Position currentPosition = vessel.getPosition();
		if (prices.canAfford(PurchaseTypes.SHIP, funds)){
			return PurchaseTypes.SHIP;
		}
		if (prices.canAfford(PurchaseTypes.BASE, funds)) {	//buy bases when you are in the frontier
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

	public MoveAction optimalApproach(Toroidal2DPhysics space, Position current, Position goal){
		//Vector2D goalVelocity = goal.getTranslationalVelocity();  //movement vector of goal object--we can use this to take better paths
		MoveAction move;
		Vector2D distVec = space.findShortestDistanceVector(current, goal);
		if (Math.abs(distVec.angleBetween(this.vessel.getPosition().getTranslationalVelocity())) > Math.PI/4){
			distVec = distVec.getUnitVector().multiply(SPEED/2);	//slowdown for allignment
			move = new MoveAction(space, current, goal, distVec);
			move.setKpRotational(4.0);
			move.setKvRotational(4.0);

		} else {
			distVec = distVec.getUnitVector().multiply(SPEED);		//controls speed in orientation
			move = new MoveAction(space, current, goal, distVec);
		}

		//implement a function of mass fuel and closest refuel
		return move;
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