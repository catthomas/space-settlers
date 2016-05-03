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


public class SpaceCommand {
	/** Singleton class, will only ever have one instance in the program */
	private static SpaceCommand singleton = new SpaceCommand();
	
	/** Heuristic constants - obtained from learning **/
	private final double MAX_SPEED = 88; //speed of travel coefficient
	private final double CARGO_CAPACITY = 1500; //max amount of resources to carry
	private final double FUEL_COEF = 45; //min amount of fuel to seek refuel
	
	
	/** The ultimate goal of the planner - receive a team score of one million */
//	private final double goalScore = 1000000;
//	private final int goalShips = 5;
	
	/** Tracks the pilot states of all instantiated ships */
	private HashMap<UUID, ShipState> pilots;
	private HashMap<UUID, BaseState> bases;
	
	/** Default constructor, creates a space command class */ 
	SpaceCommand() {
		this.bases = new HashMap<UUID, BaseState>();
		this.pilots = new HashMap<UUID, ShipState>();
	} //end Planner constructor
	
	/** Static 'instance' method to get singleton */
	public static SpaceCommand getInstance( ) {
	   return singleton;
	} //end getInstance
	
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
	
	/** Decides which high level plan to follow **/ 
	public void decideStrategy(){
		
	} //end decidePlan
	
	/** Returns a Map of ships and their assigned actions, based on the current high-level strategy **/	
	public Map<UUID, AbstractAction> getPilotCommands(Toroidal2DPhysics space){
		decideStrategy();
		return null;
	} //end getPilotCommands
	
	/** Returns list of purchases for all objects, purchase ability based on high level strategy **/
	public PurchaseTypes getTeamPurchases(Toroidal2DPhysics space, ResourcePile funds, PurchaseCosts prices){
		return null;
	} //end getPilotPurchase
	
	/** Action for a ship to collect a beacon **/
	public AbstractAction goToBeacon(Toroidal2DPhysics space, ShipState ship){
		//Target nearest beacon
		Beacon beacon = ship.getNearestBeacon(space, null); //TODO: call get targets here?
		
		//Check preconditions of action
		if(ship.needsFuel(FUEL_COEF) && !isTargeted(beacon, ship.getVessel().getId()) && beacon != null){
			//Set effect
			ship.setTarget(beacon);
			return optimalApproach(space, ship.getVessel(), ship.getVessel().getPosition(), beacon.getPosition());
		}
		return null;
	} //end goToBeacon
	
	/** Action for a ship to head to a base **/
	public MoveAction goToBase(Toroidal2DPhysics space, ShipState ship){
		//Target nearest base
		Base base = ship.getNearestBase(space);
		
		//Check preconditions of action
		if(ship.atMaxCargo(CARGO_CAPACITY) && base != null){
			//Set effect
			ship.setTarget(base);
			return optimalApproach(space, ship.getVessel(), ship.getVessel().getPosition(), base.getPosition());	
		}
		return null;
	} //end goToBase
	
	/** Action for a ship to head to a minable asteroid **/
	public MoveAction goToProspect(Toroidal2DPhysics space, ShipState ship){
		//Target nearest prospect
		Asteroid prospect = ship.getNearestProspect(space, null); //TODO: change to get Targets?
		
		// precondition - does not need fuel, not at max capacity
		return null;
	} //end goToProspect
	
	public MoveAction goToDiamond(ShipState ship){
		return null;
	} //end goToHighValueProspect
	
	/** Action for a ship to go to a less populated point in the frontier **/ 
	public MoveAction goToFrontier(ShipState ship){
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
			if(pilot.getTarget() != null && !pilot.getVessel().getId().equals(shipId)){ 
				targets.add(pilot.getTarget());
			}
		}
		return targets;
	} //end getTargets
		
} //end SpaceCommand class
