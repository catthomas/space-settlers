package stan5674;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
	 * Knowledge used for learning.
	 */
	Genetic evoKnowledge;
	
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
	
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();

		// loop through each ship
		for (AbstractObject actionable :  actionableObjects) {
			//System.out.println(actionable);
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;

				if (!pilots.containsKey(ship.getId())){
					pilots.put(ship.getId(), new PilotState(space, evoKnowledge.getNextCandidate()));
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
					pilots.put(ship.getId(), new PilotState(space, evoKnowledge.getNextCandidate()));
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
		File f = new File("knowledge.ser");
		if(f.exists()) { //learning has occurred previously, set up on past knowledge
			System.out.println("LEARNING HAS OCCURED BEFORE! :)");
			try {
				FileInputStream fis;
				ObjectInputStream ois;
				fis = new FileInputStream(f);
				ois = new ObjectInputStream(fis);
				this.evoKnowledge = (Genetic) ois.readObject();
				ois.close();
				fis.close();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				//error occurred, start from new knowledge..
				this.evoKnowledge = new Genetic();
			}
		} else { //learning has not occurred before
			System.out.println("LEARNING HAS NOT OCCURED BEFORE! :(");
			this.evoKnowledge = new Genetic();
		}
	}

	/**
	 * Demonstrates saving out to the xstream file
	 * You can save out other ways too.  This is a human-readable way to examine
	 * the knowledge you have learned.
	 */
	@Override
	public void shutDown(Toroidal2DPhysics space) {
	      try {
	          // create a new file with an ObjectOutputStream
	          FileOutputStream out = new FileOutputStream("knowledge.ser");
	          ObjectOutputStream oout = new ObjectOutputStream(out);

	          //Run evolution
	          evoKnowledge.evolve();
	          
	          //Write knowledge to the file
	          oout.writeObject(evoKnowledge);
	          oout.flush();

	          //Close write streams
	          oout.close();
	          out.close();
	       } catch (Exception ex) {
	          ex.printStackTrace();
	       }
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
		HashMap<UUID, PurchaseTypes> purchases = new HashMap<UUID, PurchaseTypes>();
		PurchaseTypes purchase=null;
		
		// can I buy a ship?
		if (purchaseCosts.canAfford(PurchaseTypes.SHIP, resourcesAvailable)) {
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Base) {
					Base base = (Base) actionableObject;
					purchases.put(base.getId(), PurchaseTypes.SHIP);
					purchase = PurchaseTypes.SHIP;
					break;
				}

			}

		}

		if(purchase == null){
			for (AbstractActionableObject actionableObject : actionableObjects) {
				if (actionableObject instanceof Ship) {
					Ship ship = (Ship) actionableObject;
	
					purchase = pilots.get(ship.getId()).shop(space, ship, resourcesAvailable, purchaseCosts);	//ship gets a single purchase
					
					if (purchase != null) {
						purchases.put(ship.getId(), purchase);
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
