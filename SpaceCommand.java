package stan5674;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;


public class SpaceCommand {
	/** Singleton class, will only ever have one instance in the program */
	private static SpaceCommand singleton = new SpaceCommand();
	
	/** The ultimate goal of the planner - receive a team score of one million */
//	private final double goalScore = 1000000;
//	private final int goalShips = 5;
	
	/** Tracks the pilot states of all instantiated ships */
	private HashMap<UUID, ShipState> pilots = new HashMap<UUID, ShipState>();
	private HashMap<UUID, BaseState> bases = new HashMap<UUID, BaseState>();
	
	/** Default constructor, creates a planner class */ 
	SpaceCommand() {
		
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
	public void decidePlan(){
		
	} //end decidePlan
	
	/** Returns a Map of ships and their assigned actions, based on the current high-level strategy **/	
	public Map<UUID, AbstractAction> getPilotCommands(Toroidal2DPhysics space){
		decidePlan();
		return null;
	} //end getPilotCommands
	
	/** Returns list of purchases for all objects, purchase ability based on high level strategy **/
	public PurchaseTypes getTeamPurchases(Toroidal2DPhysics space, ResourcePile funds, PurchaseCosts prices){
		return null;
	} //end getPilotPurchase
	
} //end Planner class
