package stan5674;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Ship;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;


public class Planner {
	/** Singleton class, will only ever have one instance in the program */
	private static Planner singleton = new Planner();
	
	/** The ultimate goal of the planner - receive a team score of one million */
	private final double goalScore = 1000000;
	private final int goalShips = 5;
	
	/** Tracks the pilot states of all instantiated ships */
	WeakHashMap<UUID, PilotState> pilots = new WeakHashMap<UUID, PilotState>();	
	
	/** Default constructor, creates a planner class */ 
	Planner() {
		
	} //end Planner constructor
	
	/** Static 'instance' method to get singleton */
	public static Planner getInstance( ) {
	   return singleton;
	} //end getInstance
	
	/** Getter for hashmap of pilots **/
	public WeakHashMap<UUID, PilotState> getPilots(){
		return pilots;
	} //end getPilots
	
	/** Adds a new pilot **/
	public void addPilot(UUID shipId, PilotState pilot){
		if(pilots.isEmpty()){
			pilot.setChaser(true); //give first ship added the 'chaser' role
		} else {
			pilot.setChaser(false);
		}
		pilots.put(shipId, pilot);
	} //end addPilot
	
	/** Returns an action for the ship based on the team's current actions */
	public AbstractAction getPilotAction(Toroidal2DPhysics space, Ship vessel){
		Position currentPosition = vessel.getPosition();
		PilotState state = this.pilots.get(vessel.getId());
		AbstractObject target = null;

		//get refueled
		if (vessel.getEnergy() < state.FUEL_COEF){
			target = state.findNearestRefuel(space, vessel, getTargets(vessel.getId()));

			AbstractAction newAction = null;
			
			// if no object found, skip turn
			if(target == null) {
				newAction = new DoNothingAction(); 
			} else {
				state.setTarget(target);
				newAction = state.optimalApproach(space, currentPosition, target.getPosition());
			};

			return newAction;
		}
		//return resources to base
		if (vessel.getResources().getTotal() > state.CARGO_CAPACITY){
			target = state.findNearestBase(space, vessel, false);
			state.setTarget(target);
			return state.optimalApproach(space, currentPosition, target.getPosition());
		}

		//Default action is to gather resources
		if(state.getChaser() == true){
			
		} else {
			target = state.findNearestProspect(space, vessel, getTargets(vessel.getId()));
		}
		state.setTarget(target);
		return state.optimalApproach(space, currentPosition, target.getPosition());
	} //end getPilotMove
	
	public PurchaseTypes getPilotPurchase(Toroidal2DPhysics space, Ship vessel, ResourcePile funds, PurchaseCosts prices){
		return null;
	} //end getPilotPurchase
	
	/** Get targets of all other pilots - used for avoidance precondition **/
	public Set<AbstractObject> getTargets(UUID shipId){
		Set<AbstractObject> targets = new HashSet<AbstractObject>();
		for(PilotState pilot : pilots.values()){
			if(!pilot.getVessel().getId().equals(shipId)){ //don't add if current ship's target
				targets.add(pilot.getTarget());
			}
		}
		return targets;
	} //end getTargets
} //end Planner class
