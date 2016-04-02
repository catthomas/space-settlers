package stan5674;

import java.util.*;

import spacesettlers.actions.*;
import spacesettlers.graphics.LineGraphics;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.*;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.objects.weapons.Missile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.*;
import stan5674.Genome;

/**
 * Use a pilot's perspective to make decisions
 *
 */
public class PilotState {
	private int FUEL_COEF = 2000; 		//point of return
	private int CARGO_CAPACITY = 3000; 	//Max resources to carry
	private int MAX_SPEED = 200;				//speed of travel coefficient
	private int FRONTIER = 500;			//min distance between bases
	
	private float FOV = 1000;			//Max distance to consider objects
	private int MIN_BASE_FUEL = 1000;	//Minimum base fuel to be considered a candidate for refueling
	private int EXE_TIME = 30;			//max time between planning
	private int CLOSE_ESC = 125;			//object is considered particularly close
	private int TRAJ_ANGLE = 50;			//when determining best prospect, prioritize objects within this angle

	private Ship vessel;
	
	// Variables for A* and path planning
	private boolean usePlanning = false; //turn A* on and off - runs more light weight if off
	private Node goal; //Goal location of vessel
	private WeakHashMap<UUID, Set<Node>> graph = new WeakHashMap<UUID, Set<Node>>(); //Holds graph used for A*
	private WeakHashMap<UUID, Node> nodes = new WeakHashMap<UUID, Node>();	//Holds nodes used in A* graph
	private Stack<Node> path = new Stack<Node>(); //The path the vessel is following
	private Set<SpacewarGraphics> graphics = new HashSet<SpacewarGraphics>(); //Holds markers for A* path
	private int exe = this.EXE_TIME; 				//time spent executing current plan

	
	//return the current graphics
	public Set<SpacewarGraphics> getPathGraphics(){
		return graphics;
	}

	//call in agent init to set FOV radius of pilot
	public void setFOV(Toroidal2DPhysics space){
		this.FOV = Math.min(space.getHeight(), space.getWidth());
		this.FOV/=2;
	}
	
	/**
	 * Class used to create graph for A*. 
	 */
	public class Node{
		AbstractObject object;
		double g; //Cumulative path cost
		double h; //Heuristic variable
		public boolean isBypass; //Was node created to pass another object?
		
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
	
	//constructor method
	public PilotState(Toroidal2DPhysics space){
		setFOV(space);
		goal = null;
		this.FUEL_COEF *= .5;
		this.CARGO_CAPACITY *= .5;
		this.MAX_SPEED *= .5;
		this.FRONTIER *= .5;
	}

	//learning constructor method
	public PilotState(Toroidal2DPhysics space, Genome genome){
		setFOV(space);
		goal = null;
		this.FUEL_COEF *= genome.fuelCoefGene();
		this.CARGO_CAPACITY *= genome.cargoCapacityGene();
		this.MAX_SPEED *= genome.maxSpeedGene();
		this.FRONTIER *= genome.frontierGene();
	}

	/**
	 * Generates a graph of connected nodes to be used for pilot path planning.
	 * @param space Environment of vessel
	 * @param vessel Ship whose path will be planned
	 */
	public void genGraph(Toroidal2DPhysics space, Ship vessel){
		//System.out.println("~~~~~~Vessel: " + vessel.getPosition() + "~~~~~~~");
		Set<AbstractObject> objects = this.objectsInFov(space, vessel);
		AbstractObject base = this.findNearestBase(space, vessel, false);		//no place like home
		AbstractObject beacon = this.findNearestBeacon(space, vessel); //or a gas station
		Set<AbstractObject> other;
		Set<AbstractObject> obs;
		Set<Node> children;
		this.graph.clear();
		this.nodes.clear();
		
		if(base != null){ //add base if not null
			objects.add(base);
		}
		if(beacon != null){ //add beacon if not null
			objects.add(beacon);
		}

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
				}	else {
//					Node bypass = this.generateBypassNode(space, a, obs, b);
//					if (bypass != null)
//						children.add(bypass);	//generates a node beside obstruction
					//children.add(this.nodes.get(b.getId()));
					
				}

			}
			
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
				Vector2D posEsc;
				Vector2D negEsc;
				if (vec.getMagnitude() < this.CLOSE_ESC){
					vec = vec.getUnitVector();
					posEsc = vec.rotate(2*Math.PI/3); //
					negEsc = vec.rotate(-2*Math.PI/3);
					
					//Scale vector to be appropriate distance (this case, radius of end object + 2 radius of ship)
					posEsc = posEsc.multiply(ob.getRadius()+2*vessel.getRadius());
					negEsc = negEsc.multiply(ob.getRadius()+2*vessel.getRadius());

					Vector2D[] escs = {posEsc, negEsc};
					Position goal;

					for (Vector2D esc : escs){
						//Find goal position based on vector
						goal = ob.getPosition().deepCopy();
						//System.out.println("The initial x position: " + goal.getX() + " The initial Y position: " + goal.getY());
						goal.setX(goal.getX() + esc.getXValue());
						goal.setY(goal.getY() + esc.getYValue());

						//if bypass location is free, add it to graph
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
				}
				return null;
			}
		}

		return null;
	}

	//Plans a path from one node on the hashmap to a goal node (both start and goal must be in the map), following A*
	public void planPath(Toroidal2DPhysics space, Node start, Node goal){
		if(goal == null || start == null || !this.graph.containsKey(goal.getObject().getId()) 
				|| !this.graph.containsKey(start.getObject().getId())){
			return; //no goal or start available - don't plan
		}
		
		Set<UUID> closedList = new HashSet<UUID>(); //list of nodes already evaluated
		Set<Node> openList = new HashSet<Node>(); //list of nodes to be evaluated
		WeakHashMap<UUID, UUID> previousNode = new WeakHashMap<UUID, UUID>();

		//Set start node values
		start.setG(0);
		start.setH(findH(space, start, goal)); 
		openList.add(start); //begin with start node
		System.out.println("~~~~~~~A STAR STARTED~~~~~~~~~~~~~");
		//Plan path
		while(!openList.isEmpty()){
			//Set current node
			Node current = findLowestFNode(openList);

			//System.out.println("~~~~~~~Visiting nodeID: "+ current.getObject().getId() +"~~~~~~");
			
			if(current.getObject().getId().equals(goal.getObject().getId())){ //goal found - return path!
				this.setPath(space, previousNode, start, goal);
				//System.out.println("~~~~~Planning successful, size: " + this.path.size()~~~~~");
				return;
			}
			
			//Mark current as evaluated, add to closedList
			openList.remove(current); 
			closedList.add(current.getObject().getId());
			
			//expand current's neighbors
			Set<Node> neighbors = this.graph.get(current.getObject().getId());

			//System.out.println("~~~~~~~~Expanding " + neighbors.size() + " neighbors ~~~~~~~~");

			for(Node neighbor : neighbors){		
				if(!closedList.contains(neighbor.getObject().getId())){
					double h = findH(space, neighbor, goal);
					double g = findG(space, current, neighbor);
					
					if(h < neighbor.getH()){
						neighbor.setH(h);
						previousNode.put(neighbor.getObject().getId(), current.getObject().getId());
					}
					if(g < neighbor.getG()){
						neighbor.setG(g);
						previousNode.put(neighbor.getObject().getId(), current.getObject().getId());
					}
					openList.add(neighbor); 
				}
			}
		}

		//System.out.println("~~~~~~~PLANNING FAILED~~~~~~~~");
	}
	
	/**
	 * Iterates through nodes backwards and draws the A* path. 
	 * @param previousNode
	 * @param start
	 * @param goal
	 */
	public void setPath(Toroidal2DPhysics state, WeakHashMap<UUID, UUID> previousNode, Node start, Node goal){
		//System.out.println("~~~~~~~SET PATH STARTED~~~~~~~~~~~~~");
		this.path.clear();
		this.graphics.clear();
		
		Node current = goal; //Start from end, assume goal was found
		Position prevPos;

		while(current != start){
			this.path.push(current); //add to path
			prevPos = current.getObject().getPosition(); //get position of node
			//graphics.add(new CircleGraphics(2, Color.RED, prevPos));	//mark location of node on map
			current = nodes.get(previousNode.get(current.getObject().getId())); //switch to next node
			//add a line between this node and last
			graphics.add(new LineGraphics(current.getObject().getPosition(), prevPos, 
					state.findShortestDistanceVector(current.getObject().getPosition(), prevPos))); 
		}
		//System.out.println("~~~~~~~SET PATH ENDED~~~~~~~~~~~~~");
		this.exe = 0;
	}
	
	/**
	 * Calculates A* efficiency for a node.
	 */
	public Node findLowestFNode(Set<Node> nodes){
		Node best = null;		//pick one
		
		for(Node node : nodes){
		if(best == null || node.getF() < best.getF()){
				best = node;
			}
		}
		return best;
	}
	
	/**
	 * Finds heuristic cost for a node, based on euclidean distance.
	 */
	public double findH(Toroidal2DPhysics space, Node start, Node goal){
		AbstractObject nodeObject = start.getObject();
		if (nodeObject instanceof Asteroid && !((Asteroid)nodeObject).isMineable()){	//don't visit unmineable asteroids
			return Double.MAX_VALUE;
		} else if (nodeObject instanceof Base){
			if (!(((Base)nodeObject).getTeam().getShips().contains(this.vessel))){
				return Double.MAX_VALUE;	//don't run into other team's bases
			} else if (!(goal.getObject() instanceof Base)){
				return Double.MAX_VALUE; //don't run into our base if not a goal
			}
		} else if (nodeObject instanceof Ship || nodeObject instanceof Missile){
			return Double.MAX_VALUE;	//or their ships & bullets
		}
	//System.out.println("~~~~~~~Getting H for: "+ nodeObject.getPosition() + " to: " + goal.getObject().getPosition() + "~~~~~~~~~~");
		return space.findShortestDistance(nodeObject.getPosition(), goal.getObject().getPosition());
	}
	
	/**
	 * Finds Total path cost for a node. 
	 */
	public double findG(Toroidal2DPhysics space, Node start, Node end){
		return space.findShortestDistance(start.getObject().getPosition(), end.getObject().getPosition()) + start.getG();
	}
	
	/**
	 * Calculates A* efficiency for a node (start).
	 */
	public double findF(Toroidal2DPhysics space, Node start, Node end, Node goal){
		return findH(space, start, goal) + findG(space, start, end);
	}

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
		//System.out.println("~~~~~FIND NEAREST REFUEL~~~~~");
		Position location = vessel.getPosition();

		Base nearestBase = findNearestBase(space, vessel, true);
		Beacon nearestBeacon = findNearestBeacon(space, vessel);
		
		if(nearestBeacon != null && nearestBase == null){
			return nearestBeacon;
		} else if (nearestBeacon != null && nearestBase != null && space.findShortestDistance(location, nearestBase.getPosition()) >= space.findShortestDistance(location, nearestBeacon.getPosition())){
			//System.out.println("Shortest distance to base found: " + nearestBase.toString());
			return nearestBeacon;
		} else {
			//System.out.println("Shortest distance to Beacon found: " + nearestBeacon.toString());
			return nearestBase;
		}
	}

	//Pilot's instincts (failsafe actions)
	public AbstractAction decideAction(Toroidal2DPhysics space, Ship vessel){
		Position currentPosition = vessel.getPosition();
		AbstractObject goal = null;

		//get refueled
		if (vessel.getEnergy() < FUEL_COEF){
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
		//goal = getProspectWithinFOVAndTrajectory(space, vessel, TRAJ_ANGLE); //favor within certain angle
		//if(goal == null){
			goal = findNearestProspect(space, vessel);
		//}

		return optimalApproach(space, currentPosition, goal.getPosition());
	};

	public void prePlan(Toroidal2DPhysics space, Ship vessel){
		this.genGraph(space, vessel);
	
		Node start = this.nodes.get(vessel.getId());
		AbstractObject temp = null;
		if (vessel.getEnergy() < FUEL_COEF){
			//System.out.println("~~~~~Planning TO REFUEL~~~~~");
			temp = findNearestRefuel(space, vessel);
			if(temp != null){
				goal = this.nodes.get(temp.getId());
			}
		}
		//return resources to base
		if (temp == null && vessel.getResources().getTotal() > CARGO_CAPACITY){
			//System.out.println("~~~~~Planning TO BASE~~~~~");
			temp = findNearestBase(space, vessel, false);
			if(temp != null){
				goal = this.nodes.get(temp.getId());
			}
		} else {	//just get resources
			//System.out.println("~~~~~Planning TO PROSPECT~~~~~");
			temp = getProspectWithinFOVAndTrajectory(space, vessel, TRAJ_ANGLE); //favor within certain angle
			if(temp == null){
				temp = findNearestProspect(space, vessel);
			}
			if(temp != null){
				goal = this.nodes.get(temp.getId());
			}
		}
		if(goal != null){
			this.planPath(space, start, goal);	//find best path to goal
		}
	}
	
	/**
	 * Tracks the ship's status as it navigates A* path.
	 */
	public AbstractAction executePlan(Toroidal2DPhysics space, Ship vessel){
		//System.out.println("~~~~~~~Starting plan Execution: "+ this.exe +"~~~~~~~");
		this.vessel = vessel;
		
		if(this.usePlanning == false){ // do not plan - run failsale
			return this.decideAction(space, vessel);			//failsafe heuristic
		}

		if (this.exe >= this.EXE_TIME || this.path.isEmpty()){		//Time to replan
			this.prePlan(space, vessel);
		}
		
		Position target;

		if (!this.path.empty()){
			target = this.path.peek().getObject().getPosition();		//get goal location
		}
		else {
			//System.out.println("~~~~~~~~FAIL SAFE~~~~~~~~~");
			return this.decideAction(space, vessel);			//failsafe heuristic
		}

		this.exe+=1;

		//System.out.println("~~~~~~Executing Plan: " + this.exe + "~~~~~~");

		return this.optimalApproach(space, vessel.getPosition(), target);
	};

	//How do we know when we reach a subgoal?
	public void assessPlan(Toroidal2DPhysics space, Ship vessel){
		if(this.usePlanning == false) return;
		if (!this.path.empty())
			if (2*vessel.getRadius() >= space.findShortestDistance(vessel.getPosition(), this.path.peek().getObject().getPosition())){
				this.path.pop();			//subgoal achieved
				//System.out.println("~~~~~~Popping node~~~~~~~");
			}
		if (this.path.empty()){				//plan complete
			this.exe = this.EXE_TIME;		//replan next timestep
		}
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
		//return fastest velocity vestor that the vessel can travel and still reach goal
		//Vector2D goalVelocity = goal.getTranslationalVelocity();  //movement vector of goal object--we can use this to take better paths
		MoveAction move;
		Vector2D distVec = space.findShortestDistanceVector(current, goal);

		if (Math.abs(distVec.angleBetween(this.vessel.getPosition().getTranslationalVelocity())) > 30){
			distVec = distVec.getUnitVector().multiply(MAX_SPEED/2);	//slowdown for allignment
			move = new MoveAction(space, current, goal, distVec);
			// move.setKpRotational(36);
			// move.setKvRotational(12);

		} else {
			distVec = distVec.getUnitVector().multiply(MAX_SPEED);		//controls MAX_SPEED in orientation
			move = new MoveAction(space, current, goal, distVec);
			// move.setKpRotational(36);
			// move.setKvRotational(12);
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
	 * Returns the closest mineable asteroid or beacon that is within a 
	 * specified range and angle.
	 */
	public AbstractObject getProspectWithinFOVAndTrajectory(Toroidal2DPhysics space, Ship vessel, double angle){
		AbstractObject target = null;
		double shortest = Double.MAX_VALUE;
		Position currentPosition = vessel.getPosition();
	
	
		for (AbstractObject obj : space.getAllObjects()) {
			Vector2D dist = space.findShortestDistanceVector(currentPosition, obj.getPosition());

			if (dist.getMagnitude() < this.FOV && dist.getMagnitude() < shortest && (obj instanceof Beacon || (obj instanceof Asteroid && ((Asteroid) obj).isMineable() == true))
					&& Math.abs(dist.angleBetween(currentPosition.getTranslationalVelocity())) <= angle) {
				target = obj;
			}
		}
		return target;
	}
	
	//pilot decides what to buy from shop
	public PurchaseTypes shop(Toroidal2DPhysics space, Ship vessel, ResourcePile funds, PurchaseCosts prices){
		Position currentPosition = vessel.getPosition();
		if (prices.canAfford(PurchaseTypes.SHIP, funds)){
			//System.out.println("-----------------BUY A SHIP");
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
}