package stan5674;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.MoveAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.graphics.LineGraphics;
import spacesettlers.graphics.SpacewarGraphics;


public class SpaceCommand {

	/** Enum for high level strategies **/
	public enum Strategy {
		FREE_MINE, EXPAND_EMPIRE, BUILD_FLEET
	}
	
	/** Action constants **/
	private final double MAX_SPEED = 88; //speed of travel coefficient
	private final double CARGO_CAPACITY = 500; //max amount of resources to carry
	private final double FUEL_COEF = 1000; //min amount of fuel to seek refuel
	private final double FRONTIER = 200;
	private final double MIN_BASE_FUEL = 1000;
	
	
	/** PDDL goals and high level strategy tracker */
	private final int GOAL_BASES = 10;
	private final int GOAL_SHIPS = 10;
	private final int MIN_SHIPS = 5;
	private Strategy strategy;
	private Planner planner;
	private Position goldmine;
	
	/** Tracks the pilot states of all instantiated ships, and current roles */
	private HashMap<UUID, ShipState> pilots;
	private List<Base> bases;
	
	/** Default constructor, creates a space command class */ 
	SpaceCommand() {
		this.bases = new LinkedList<Base>();
		this.pilots = new HashMap<UUID, ShipState>();
		this.strategy = Strategy.BUILD_FLEET; //default to free mine at beginning
		this.planner = new Planner(GOAL_SHIPS, GOAL_BASES, MIN_SHIPS, this);
	} //end Planner constructor
	
	/** Getter for hashmap of pilots **/
	public HashMap<UUID, ShipState> getShips(){
		return pilots;
	} //end getPilots
	
	/** Adds a new pilot **/
	public void addShip(UUID shipId, ShipState pilot){
		pilots.put(shipId, pilot);
		
		if(pilots.size() == 1){
			//Set first ships to be the diamond chaser!
			pilots.get(shipId).setDiamondChaser(true);		
		} else if(pilots.size() == 2){
			//Set second ship to be the gold digger!
			pilots.get(shipId).setGoldDigger(true);
		}
	} //end addPilot
	
	/** Getter for hashmap of bases **/
	public List<Base> getBases(){
		return bases;
	} //end getBases

	/** Adds a new base **/
	public void addBase(Base base){
		bases.add(base);
	} //end addBase
	
	/** Uses PDDL searching to decide which high level strategy to follow **/ 
	public void decideStrategy(){
		planner.generateGraph(strategy, pilots.size(), bases.size());
		this.strategy = planner.findBestStrategy();
	} //end decidePlan
	
	/** Checks if the strategy needs to be replanned based on strat preconditions **/
	public boolean replanNeeded(){
		switch (strategy){
			case FREE_MINE:
				if(!canFreeMine(pilots.size())) return true;
			case EXPAND_EMPIRE:
				if(!canExpandEmpire(pilots.size(), bases.size())) return true;
			case BUILD_FLEET:
				if(!canBuildFleet(pilots.size(), bases.size())) return true;
		}
		return false;
	} //end replanNeeded
	
	/** Returns a Map of objects and their assigned actions, based on the current high-level strategy **/	
	public Map<UUID, AbstractAction> getTeamCommands(Toroidal2DPhysics space){
		if(replanNeeded() == true){
			decideStrategy();
		}
		
		Map<UUID, AbstractAction> actions = null;
		switch (strategy){
			case FREE_MINE:
				actions = freeMine(space);
				break;
			case EXPAND_EMPIRE:
				actions = expandEmpire(space);
				break;
			case BUILD_FLEET:
				actions = buildFleet(space);
				break;
		}
		
		if(actions == null){
			actions = getBasicActions(space); //fail safe
		}
		
		return actions;
	} //end getPilotCommands
	
	/** Returns list of purchases for all objects, purchase ability based on high level strategy **/
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space, ResourcePile funds, PurchaseCosts prices){
		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
		
		switch (strategy){
			case FREE_MINE:
				return purchases; //Free mine does not make purchases
			case EXPAND_EMPIRE:
				//Buy base at appropriate frontier distance
				if(prices.canAfford(PurchaseTypes.BASE, funds)){
					for(ShipState ship : pilots.values()){
						Ship vessel = (Ship)space.getObjectById(ship.getVessel());
						
						//Prioritize purchasing a base at a gold mine
						if(vessel != null && ship.isGoldDigger() && this.goldmine != null
								&& space.findShortestDistance(vessel.getPosition(), goldmine) <= 100){
							purchases.clear();
							purchases.put(vessel.getId(), PurchaseTypes.BASE);
							this.goldmine = null;
							System.out.println("BOUGHT A GOLDMINE BASE");
							return purchases;
						}
						
						//Find place to purchase on frontier
						if(vessel != null && purchases.size() < 1 && space.findShortestDistance(vessel.getPosition(), ship.getNearestBase(space, vessel, false, MIN_BASE_FUEL).getPosition()) >= FRONTIER){
							purchases.put(vessel.getId(), PurchaseTypes.BASE);
							System.out.println("BOUGHT A REGULAR BASE");
						}
					}
					return purchases;
				}
				break;
			case BUILD_FLEET:
				//Buy a ship if you can afford it
				 if (prices.canAfford(PurchaseTypes.SHIP, funds)) {
				 	Base base = bases.get(0);
				 	if(base != null){
				 		purchases.put(base.getId(), PurchaseTypes.SHIP);
				 		return purchases;
				 	}
				 }
				 break;
		}

		return purchases;
	} //end getTeamPurchase

	/** Checks preconditions for the free min strategy **/
	public boolean canFreeMine(int numShips){
		if(pilots.size() >= MIN_SHIPS) return true;
		return false;
	} //end canFreeMine
	
	/** Returns team actions for the mine freely strategy **/
	public Map<UUID, AbstractAction> freeMine(Toroidal2DPhysics space){
		//Check preconditions of strategy
		if(canFreeMine(pilots.size())){
			return getBasicActions(space);
		} 
		return null;
	} //end freeMine
	
	/** Checks preconditions for the expand empire strategy **/
	public boolean canExpandEmpire(int numShips, int numBases){
		if(numBases < GOAL_BASES && numBases < numShips) return true;
		return false;
	}

	/** Returns team actions for the expand empire strategy **/
	public Map<UUID, AbstractAction> expandEmpire(Toroidal2DPhysics space){
		if(canExpandEmpire(pilots.size(), bases.size())){
			return getBasicActions(space);
		}
		return null;
	} //end expandEmpire

	/** Checks preconditions for the build fleet strategy **/
	public boolean canBuildFleet(int numShips, int numBases){
		if(pilots.size() < MIN_SHIPS || numShips < GOAL_SHIPS && numShips <= numBases) return true;
		return false;
	}
	
	/** Returns team actions for the build fleet strategy **/
	public Map<UUID, AbstractAction> buildFleet(Toroidal2DPhysics space){
		//Check preconditions of strategy
		if(canBuildFleet(pilots.size(), bases.size())){
			return getBasicActions(space); //Basic actions - but has differences in shopping!
		}
		return null;
	} //end buildFleet
	
	/** Returns the basic mine actions for every ship and base **/
	public HashMap<UUID, AbstractAction> getBasicActions(Toroidal2DPhysics space){
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();
		
		//Simplest strategy - prioritize mining for ships
		for(ShipState state : pilots.values()){
			AbstractAction action = null;
			
			if(this.strategy == Strategy.EXPAND_EMPIRE && state.isGoldDigger()){
				action = goToGoldmine(space, state);
			}
			
			if( action == null && state.isDiamondChaser()){
				action = goToDiamond(space, state);
			} else if(action == null) {
			  action = goToProspect(space, state);
			}
			
			if(action == null){
				action = goToBase(space, state);
			}

			if(action == null){
				action = goToRefuel(space, state);
			}

			if(action == null){
				action = new DoNothingAction(); //don't leave null
			}
			actions.put(state.getVessel(), action);
		}


		//All bases do nothing
		for (Base base : bases){
			actions.put(base.getId(), new DoNothingAction());
		}

		return actions;
	} //end getBasicActions
	
	/** Action for a ship to collect a beacon **/
	public AbstractAction goToBeacon(Toroidal2DPhysics space, ShipState ship){
		Ship vessel = (Ship)space.getObjectById(ship.getVessel());
		
		//Target nearest beacon
		Beacon beacon = ship.getNearestBeacon(space, vessel, getTargets(ship.getVessel()));
		
		//Check preconditions of action
		if(ship.needsFuel(FUEL_COEF, vessel) && !isTargeted(beacon, ship.getVessel()) && beacon != null){
			//Set effect
			ship.setTarget(beacon);
			return optimalApproach(space, vessel, vessel.getPosition(), beacon.getPosition());
		}
		return null;
	} //end goToBeacon

	/** Action for a ship to collect refuel at beacon or base **/
	public AbstractAction goToRefuel(Toroidal2DPhysics space, ShipState ship){
		Ship vessel = (Ship)space.getObjectById(ship.getVessel());
		
		//Target nearest beacon or base
		AbstractObject refuel = ship.getNearestRefuel(space, vessel,MIN_BASE_FUEL);
		
		//Check preconditions of action
		if(ship.needsFuel(FUEL_COEF, vessel) && !isTargeted(refuel, ship.getVessel()) && refuel != null){
			//Set effect
			ship.setTarget(refuel);
			return optimalApproach(space, vessel, vessel.getPosition(), refuel.getPosition());
		}
		return null;
	} //end goToBeacon
	
	/** Action for a ship to head to a base **/
	public MoveAction goToBase(Toroidal2DPhysics space, ShipState ship){
		Ship vessel = (Ship)space.getObjectById(ship.getVessel());
		
		//Target nearest base
		Base base = ship.getNearestBase(space, vessel, false, MIN_BASE_FUEL);
		
		//Check preconditions of action
		if(ship.atMaxCargo(CARGO_CAPACITY, vessel) && base != null){
			//Set effect
			ship.setTarget(base);
			return optimalApproach(space, vessel, vessel.getPosition(), base.getPosition());	
		}
		return null;
	} //end goToBase
	
	/** Action for a ship to head to a minable asteroid **/
	public MoveAction goToProspect(Toroidal2DPhysics space, ShipState ship){
		Ship vessel = (Ship)space.getObjectById(ship.getVessel());
		
		//Target nearest prospect
		Asteroid prospect = ship.getNearestProspect(space, vessel, getTargets(ship.getVessel()));
		
		// precondition - does not need fuel, not at max capacity, asteroid exists
		if(!ship.needsFuel(FUEL_COEF, vessel) && !ship.atMaxCargo(CARGO_CAPACITY, vessel) 
				&& prospect != null){ // && !isTargeted(prospect, ship.getVessel().getId()
			//Set effect
			ship.setTarget(prospect);
			return optimalApproach(space, vessel, vessel.getPosition(), prospect.getPosition());
		}
		return null;
	} //end goToProspect
	
	/** Action for a ship to head to the HIGHEST value asteroid **/
	public MoveAction goToDiamond(Toroidal2DPhysics space, ShipState ship){
		Ship vessel = (Ship)space.getObjectById(ship.getVessel());
		
		//Target nearest prospect
		Asteroid prospect = ship.getDiamond(space);
		
		// precondition - does not need fuel, not at max capacity, asteroid exists
		if(!ship.needsFuel(FUEL_COEF, vessel) && !ship.atMaxCargo(CARGO_CAPACITY, vessel) 
				&& prospect != null){
			//Set effect
			ship.setTarget(prospect);
			return optimalApproach(space, vessel, vessel.getPosition(), prospect.getPosition());
		}
		return null;
	} //end goToDiamond
	
	/** Action for a ship to go to a less populated point in the frontier **/ 
	public MoveAction goToGoldmine(Toroidal2DPhysics space, ShipState ship){
		Ship vessel = (Ship)space.getObjectById(ship.getVessel());
		Position goldmine = null;
		if(this.goldmine == null){
			goldmine = findGoldmine(space, ship);
		} else {
			return optimalApproach(space, vessel, vessel.getPosition(), this.goldmine);
		}
		
		
		// precondition - does not need fuel, not at max capacity, locations exists
		if(goldmine != null && ship.isGoldDigger() && !ship.needsFuel(FUEL_COEF, vessel)){
			this.goldmine = goldmine;
			ship.setTarget(new Beacon(goldmine)); //pseudo object for graphics
			return optimalApproach(space, vessel, vessel.getPosition(), goldmine);	
		}
		this.goldmine = null; //reset gold mind to null if not found
		return null;
	} //end goToGoldmine


	
	/** Returns a move which should be both speedy and at an appropriate angle between given positions.**/ 
	public MoveAction optimalApproach(Toroidal2DPhysics space, Ship vessel, Position current, Position goal){
		//return fastest velocity vector that the vessel can travel and still reach goal
		MoveAction move;
		Vector2D distVec = space.findShortestDistanceVector(current, goal);

		if (Math.abs(distVec.angleBetween(vessel.getPosition().getTranslationalVelocity())) > 15){
			distVec = distVec.getUnitVector().multiply(MAX_SPEED/2);	//slowdown for allignment
			distVec = distVec.rotate(distVec.angleBetween(vessel.getPosition().getTranslationalVelocity()));
			move = new MoveAction(space, current, goal, distVec);
		} else {
			distVec = distVec.getUnitVector().multiply(MAX_SPEED);		//controls MAX_SPEED in orientation
			move = new MoveAction(space, current, goal, distVec);
		}
		return move;
	}; //end optimalApproach
	
	/** Determines if the object is targeted by any of the ships BESIDES the given ship **/
	public boolean isTargeted(AbstractObject object, UUID shipId){
		return getTargets(shipId).contains(object);
	} //end isTargeted
	
	/** Get targets of all ships besides given ship ID **/
	public Set<AbstractObject> getTargets(UUID shipId){
		Set<AbstractObject> targets = new HashSet<AbstractObject>();
		for(ShipState pilot : pilots.values()){
			if(pilot.getTarget() != null && !pilot.getVessel().equals(shipId)){ 
				targets.add(pilot.getTarget());
			}
		}
		return targets;
	} //end getTargets

	/** Returns graphics for all the ships **/
	public HashSet<SpacewarGraphics> getGraphics(){
		HashSet<SpacewarGraphics> graphics = new HashSet<SpacewarGraphics>();
		for(ShipState pilot : pilots.values()){
			LineGraphics line = pilot.getGraphics();
			if(line != null){
				graphics.add(line);
			}
		}
		return graphics;
	} //end getGraphics
	
	/** Updates the graphics on the ships **/
	public void updateGraphics(Toroidal2DPhysics space){
		for(ShipState ship : pilots.values()){
			ship.setGraphics(space, (Ship)space.getObjectById(ship.getVessel()));
		}
	} //end updateGraphics
	
	/** Uses K-means clustering to find a location with high resource density **/
	public Position findGoldmine(Toroidal2DPhysics space, ShipState vessel){
		Random rand = new Random();
		List<Asteroid> prospects = vessel.getMinableAsteroids(space); //maybe only consider stationary asteroids?
		int[] ptok = new int[prospects.size()];	//links prospects to their closest centroid (k)
		
		
		double minDist;
		double dist;
		int count;
		Vector2D dxy;
		boolean changed = true;
		int MAX_ITER = 100;
		int iter = 0;
		Position bestest = null;
		double mostest = Double.NEGATIVE_INFINITY;

		
		int k = 9;
			iter = 0;
			changed = true;
			List<Position> K = new ArrayList<Position>(k);				//K mean positions
			List<ArrayList<Asteroid>> clusters = new ArrayList<ArrayList<Asteroid>>(k);		//keep track of cluster assignments K index : [prospect indexes]

			for (int i =0; i<k; i++){
				K.add(space.getRandomFreeLocation(rand, 0));			
			}

			for (int i = 0; i < k; i ++){
				clusters.add(new ArrayList<Asteroid>());
			}

			while (changed && iter < MAX_ITER){
				iter++;
				changed = false;			//reset change flag

				for (int j = 0; j < prospects.size(); j++){
					minDist = Double.POSITIVE_INFINITY;

					for (int i = 0; i < K.size(); i++){
						dist = space.findShortestDistance(prospects.get(j).getPosition(), K.get(i));

						if (dist < minDist){
							minDist = dist;
							ptok[j] = i;
						}
					}
				}
				
				for (ArrayList<Asteroid> cluster : clusters){
					cluster.clear();
				}

				for (int i = 0; i < ptok.length; i++){
					clusters.get(ptok[i]).add(prospects.get(i));
				}

				for (int i = 0; i < clusters.size(); i++){
					count = clusters.get(i).size();

					if (count != 0){
						dxy = new Vector2D();

						for (Asteroid j : clusters.get(i)){
							dxy = dxy.add(space.findShortestDistanceVector(K.get(i), j.getPosition()));
						}

						dxy = dxy.divide(count);			//average displacement from centroid

						//System.out.println("Average displacement: " + dxy.getMagnitude() + " @ iter: " + iter);

						if (dxy.getMagnitude() > .0000001){		//if centroid is not close enough to center, recluster
							changed = true;				
							K.get(i).setX(K.get(i).getX() + dxy.getXValue());
							K.get(i).setY(K.get(i).getY() + dxy.getYValue());
						}
					}
				}
			
			//return centroid of cluster with highest resource density
			double total = 0;
			double most = Double.NEGATIVE_INFINITY;
			double extent = 0;
			double fromCenter;
			Position best = K.get(0);		//just init

			for (ArrayList<Asteroid> cluster : clusters){
				total = 0; 
				extent = 1;

				for (Asteroid ast : cluster){
					total+=ast.getResources().getTotal();
					fromCenter = space.findShortestDistance(ast.getPosition(), K.get(clusters.indexOf(cluster)));
					if (extent < fromCenter){
						extent = fromCenter;
					}
				}

				total/= extent;			//resource density of cluster using the farthest node (think of the area of an encompassing circle defined by extent as the radius, without the extra math)

				if (total > most){
					most = total;
					best = K.get(clusters.indexOf(cluster));
				}
			}
			if (mostest < most){
				bestest = best;
				mostest = total;
			}
		}

		return bestest;
	} //end findGoldMine
} //end SpaceCommand class
