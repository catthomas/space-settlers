package stan5674;

import stan5674.Genetic;
import stan5674.Genome;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.clients.ImmutableTeamInfo;
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
	int generation;
	String outputFile = "learning.txt";
	int evalTime = 5000;			//time steps to evaluate a genome
	Genome currentGenome = null;	//the currently evaluated Genome
	double totalScore = 0;			//used to calculate fitnesses
	int popSizeToEvolve = 40; 		//evolve once this amount of genomes sampled

	
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
	
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();

		// loop through each ship
		for (AbstractObject actionable :  actionableObjects) {
			//System.out.println(actionable);
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;

				//System.out.println(space.getCurrentTimestep());

				//handle fitness evaluations 
				if (space.getCurrentTimestep() % evalTime == 0 && (ship != null || pilots != null)){ //avoid null pointer at game start
					//System.out.println("Evaluating genome @:" + space.getCurrentTimestep());

					if (currentGenome == null){		//first initialization
						currentGenome = Genetic.getInstance().getNextCandidate();
						pilots.put(ship.getId(), new PilotState(space, currentGenome));
						//System.out.println("Init genome");
					}
					else if (space.getCurrentTimestep() != 0) {
						double fitness= 0;

						for(ImmutableTeamInfo info : space.getTeamInfo()){
							if(info.getTeamName().equals(this.getTeamName())){
								fitness = info.getScore() - this.totalScore; //difference in score is genome's fitness
								this.totalScore = info.getScore();
							}
						}
						//System.out.println("Evaluation finished for " + this.getTeamName()+ " with fitness: "+ fitness);
						currentGenome.setFitness((float)fitness);		//genome uses float for fitness...
						this.testForEvolve(); //See if time to evolve, evolve if ready


						if(space.getCurrentTimestep() < 20000){ // dont add genomes at game end
							currentGenome = Genetic.getInstance().getNextCandidate();
							pilots.put(ship.getId(), new PilotState(space, currentGenome));
						}		
					}
				}

				AbstractAction action;
				action = pilots.get(ship.getId()).executePlan(space, ship);

				if (action == null) action = new DoNothingAction();
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
					pilots.put(ship.getId(), new PilotState(space, Genetic.getInstance().getNextCandidate()));
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
		//save what current generation number is
		generation = Genetic.getInstance().generation;
	}

	/**
	 * Demonstrates saving out to the xstream file
	 * You can save out other ways too.  This is a human-readable way to examine
	 * the knowledge you have learned.
	 */
	@Override
	public void shutDown(Toroidal2DPhysics space) {
		//System.out.println("SHUTTING DOWN");
	      try {
	          // find file
	          FileOutputStream out = new FileOutputStream("stan5674/"+Genetic.getInstance().fileName);
	          ObjectOutputStream oout = new ObjectOutputStream(out);
	          
	          //Write knowledge to the file
	          oout.writeObject(Genetic.getInstance());
	          oout.flush();

	          //Close object write stream
	          oout.close();
	          out.close();  
	       } catch (Exception ex) {
	          ex.printStackTrace();
	       }
	}

	public void testForEvolve(){
		try{	          //Evolve knowledge base if appropriate pop size
	          if(Genetic.getInstance().testedCount >= popSizeToEvolve){
	          	//set tested count here, just in case
	          	Genetic.getInstance().testedCount = 0;
	          //Print fitness to file
	        	  PrintWriter print = new PrintWriter(new FileOutputStream(new File("stan5674/"+outputFile), true));
	  	  		  print.append(""+Genetic.getInstance().generation+","+ Genetic.getInstance().trackFitness() +"\n"); //write generation number, score
	  	  		  print.close();
	        	  Genetic.getInstance().evolve(); //evolve
	        	  System.out.println("~~~~~~~~~~~~~~EVOLVED~~~~~~~~~~~~~~~");
	          }
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
