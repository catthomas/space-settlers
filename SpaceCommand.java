package stan5674;
import java.util.HashSet;
import java.util.Map;
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
	
	/** Tracks the pilot states of all instantiated ships */
	private HashMap<UUID, ShipState> pilots;
	private HashMap<UUID, BaseState> bases;
	
	/** Default constructor, creates a space command class */ 
	SpaceCommand() {
		this.bases = new HashMap<UUID, BaseState>();
		this.pilots = new HashMap<UUID, ShipState>();
		this.strategy = Strategy.FREE_MINE; //default to free mine at beginning
		this.planner = new Planner(GOAL_SHIPS, GOAL_BASES, MIN_SHIPS, this);
	} //end Planner constructor
	
	/** Getter for hashmap of pilots **/
	public HashMap<UUID, ShipState> getShips(){
		return pilots;
	} //end getPilots
	
	/** Adds a new pilot **/
	public void addShip(UUID shipId, ShipState pilot){
		pilots.put(shipId, pilot);
	} //end addPilot
	
	/** Getter for hashmap of bases **/
	public HashMap<UUID, BaseState> getBases(){
		return bases;
	} //end getBases

	/** Adds a new base **/
	public void addBase(UUID baseId, BaseState base){
		bases.put(baseId, base);
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
		
		switch (strategy){
			case FREE_MINE:
				return freeMine(space);
			case EXPAND_EMPIRE:
				return expandEmpire(space);
			case BUILD_FLEET:
				return buildFleet(space);
		}
		return null;
	} //end getPilotCommands
	
	/** Returns list of purchases for all objects, purchase ability based on high level strategy **/
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space, ResourcePile funds, PurchaseCosts prices){
		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
		
		switch (strategy){
			case FREE_MINE:
				return purchases; //Free mine does not make purchases
			case EXPAND_EMPIRE:
				//Buy base at appropriate distance
				if(prices.canAfford(PurchaseTypes.BASE, funds)){
					for(ShipState ship : pilots.values()){
						Ship vessel = (Ship)space.getObjectById(ship.getVessel());
						if(space.findShortestDistance(vessel.getPosition(), ship.getNearestBase(space, vessel, false, MIN_BASE_FUEL).getPosition()) >= FRONTIER){
							purchases.put(vessel.getId(), PurchaseTypes.BASE);
							return purchases;
						}
					}
				}
				
			case BUILD_FLEET:
				//Buy a ship if you can afford it
				 if (prices.canAfford(PurchaseTypes.SHIP, funds)) {
				 	for(BaseState base : bases.values()){
				 		purchases.put(base.getBase().getId(), PurchaseTypes.SHIP);
				 		return purchases;
				 	}
				 }
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
	
	public HashMap<UUID, AbstractAction> getBasicActions(Toroidal2DPhysics space){
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();
		
		//Simplest strategy - prioritize mining for ships
		for(ShipState state : pilots.values()){
			AbstractAction action = goToProspect(space, state);
			
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
		for (BaseState base : bases.values()){
			actions.put(base.getBase().getId(), new DoNothingAction());
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
				&& prospect != null && !isTargeted(prospect, ship.getVessel())){
			//Set effect
			ship.setTarget(prospect);
			return optimalApproach(space, vessel, vessel.getPosition(), prospect.getPosition());
		}
		return null;
	} //end goToDiamond
	
	/** Action for a ship to go to a less populated point in the frontier **/ 
	public MoveAction goToFrontier(ShipState ship){
		// precondition - does not need fuel, not at max capacity, locations exists
		//TODO - i dont even know
		return null;
	} //end goToFrontier


	
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
} //end SpaceCommand class
