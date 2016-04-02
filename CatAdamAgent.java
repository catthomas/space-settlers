package stan5674;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.clients.ExampleKnowledge;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Base;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;

/**
 * Collects nearby asteroids and brings them to the base, picks up beacons as needed for energy.
 * 
 * If there is more than one ship, this version happily collects asteroids with as many ships as it
 * has.  it never shoots (it is a pacifist)
 * 
 * @author amy
 */
public class CatAdamAgent extends TeamClient {
	WeakHashMap<UUID, PilotState> pilots = new WeakHashMap<UUID, PilotState>();
	//PilotState pilot = new PilotState();
	/**
	 * Example knowledge used to show how to load in/save out to files for learning
	 */
	ExampleKnowledge myKnowledge;
	
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
	
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();

		// loop through each ship
		for (AbstractObject actionable :  actionableObjects) {
			//System.out.println(actionable);
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;

				if (!pilots.containsKey(ship.getId())){
					pilots.put(ship.getId(), new PilotState(space));
				}

				AbstractAction action = new DoNothingAction();
				action = pilots.get(ship.getId()).executePlan(space, ship);
				actions.put(ship.getId(), action);
				
			} else {
				// it is a base.  Heuristically decide when to use the shield (TODO)
				actions.put(actionable.getId(), new DoNothingAction());
			}
		} 
		//System.out.println("There are " + pilots.size() + " ships");
		return actions;
	}


	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
		//do goal arrival checking here?
		for (AbstractObject actionable :  actionableObjects) {
			//System.out.println(actionable);
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;


				if (!pilots.containsKey(ship.getId())){
					pilots.put(ship.getId(), new PilotState(space));
				} else {
					pilots.get(ship.getId()).assessPlan(space, ship);
				}
				//actions.put(ship.getId(), action);
				
			}
		} 
	}

	/**
	 * Demonstrates one way to read in knowledge from a file
	 */
	@Override
	public void initialize(Toroidal2DPhysics space) {
		//pilot.setFOV(space);
	}

	/**
	 * Demonstrates saving out to the xstream file
	 * You can save out other ways too.  This is a human-readable way to examine
	 * the knowledge you have learned.
	 */
	@Override
	public void shutDown(Toroidal2DPhysics space) {
//		XStream xstream = new XStream();
//		xstream.alias("ExampleKnowledge", ExampleKnowledge.class);
//
//		try { 
//			// if you want to compress the file, change FileOuputStream to a GZIPOutputStream
//			xstream.toXML(myKnowledge, new FileOutputStream(new File(getKnowledgeFile())));
//		} catch (XStreamException e) {
//			// if you get an error, handle it somehow as it means your knowledge didn't save
//			// the error will happen the first time you run
//			myKnowledge = new ExampleKnowledge();
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			myKnowledge = new ExampleKnowledge();
//		}
	}

	@Override
	/**
	 * If there is enough resourcesAvailable, buy a base.  Place it by finding a ship that is sufficiently
	 * far away from the existing bases
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {
//		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
//		PurchaseTypes purchase=null;
//
//		for (AbstractActionableObject actionableObject : actionableObjects) {
//			if (actionableObject instanceof Ship) {
//				Ship ship = (Ship) actionableObject;
//
//				purchase = pilots.get(ship.getId()).shop(space, ship, resourcesAvailable, purchaseCosts);	//ship gets a single purchase
//				
//
//
//				if (purchase != null) {
//					purchases.put(ship.getId(), purchase);
//				}
//			}		
//		}
		
		
		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
		double BASE_BUYING_DISTANCE = 200;
		boolean bought_ship = false;

		// can I buy a ship?
		if (purchaseCosts.canAfford(PurchaseTypes.SHIP, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Base) {
					Base base = (Base) actionableObject;
					purchases.put(base.getId(), PurchaseTypes.SHIP);
					bought_ship = true;
					break;
				}

			}

		}
		if (purchaseCosts.canAfford(PurchaseTypes.BASE, resourcesAvailable) && bought_ship == false) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
					Set<Base> bases = space.getBases();

					// how far away is this ship to a base of my team?
					boolean buyBase = true;
					for (Base base : bases) {
						if (base.getTeamName().equalsIgnoreCase(getTeamName())) {
							double distance = space.findShortestDistance(ship.getPosition(), base.getPosition());
							if (distance < BASE_BUYING_DISTANCE) {
								buyBase = false;
							}
						}
					}
					if (buyBase) {
						purchases.put(ship.getId(), PurchaseTypes.BASE);
						break;
					}
				}
			}		
		} 

		return purchases;
	}

	/**
	 * The pacifist asteroid collector doesn't use power ups 
	 * @param space
	 * @param actionableObjects
	 * @return
	 */
	@Override
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, SpaceSettlersPowerupEnum> powerUps = new HashMap<UUID, SpaceSettlersPowerupEnum>();

		
		return powerUps;
	}
	
	
	@Override
	public Set<SpacewarGraphics> getGraphics() {
		HashSet<SpacewarGraphics> graphics = new HashSet<SpacewarGraphics>();
		for (PilotState state : pilots.values()) {
			// uncomment to see the full graph
			//graphics.addAll(graph.getAllGraphics());
		 	graphics.addAll(state.getPathGraphics());
		 }

		HashSet<SpacewarGraphics> newGraphicsClone = (HashSet<SpacewarGraphics>) graphics.clone();
		graphics.clear();
		return newGraphicsClone;
	}
}
