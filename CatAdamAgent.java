package stan5674;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
//import spacesettlers.clients.ImmutableTeamInfo;
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
	/** Central commander for the team */
	SpaceCommand spaceCommand;
	
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				if (!spaceCommand.getShips().containsKey(ship.getId())){ //new ship - add
					spaceCommand.addShip(ship.getId(), new ShipState(ship.getId()));
				} 
			}
			if (actionable instanceof Base) {
				Base base = (Base) actionable;
				if (!spaceCommand.getBases().contains(base)){ //new base- add
					spaceCommand.addBase(base);
				} 
			}
		} 
		Map<UUID, AbstractAction> actions = spaceCommand.getTeamCommands(space);
		spaceCommand.updateGraphics(space);

		return actions;
	}

	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				if (!spaceCommand.getShips().containsKey(ship.getId())){ //new ship - add
					spaceCommand.addShip(ship.getId(), new ShipState(ship.getId()));
				} 
			}
			if (actionable instanceof Base) {
				Base base = (Base) actionable;
				if (!spaceCommand.getBases().contains(base)){ //new base- add
					spaceCommand.addBase(base);
				} 
			}
		} 
	}

	/**
	 * Required by parent class
	 */
	@Override
	public void initialize(Toroidal2DPhysics space) {
		//do nothing
		this.spaceCommand = new SpaceCommand();
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		//do nothing
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
		return spaceCommand.getTeamPurchases(space, resourcesAvailable, purchaseCosts);
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
		HashSet<SpacewarGraphics> graphics = spaceCommand.getGraphics();
		HashSet<SpacewarGraphics> newGraphicsClone = ((HashSet<SpacewarGraphics>) graphics.clone());
		graphics.clear();
		return newGraphicsClone;
	}
}
