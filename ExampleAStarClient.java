package stan5674;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import stan5674.astar.AStarSearch;
import stan5674.astar.FollowPathAction;
import stan5674.astar.Graph;
import stan5674.astar.Vertex;
import spacesettlers.actions.AbstractAction;
import spacesettlers.actions.DoNothingAction;
import spacesettlers.actions.MoveToObjectAction;
import spacesettlers.actions.PurchaseCosts;
import spacesettlers.actions.PurchaseTypes;
import spacesettlers.clients.TeamClient;
import spacesettlers.graphics.SpacewarGraphics;
import spacesettlers.objects.AbstractActionableObject;
import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.objects.powerups.SpaceSettlersPowerupEnum;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

/**
 * Just a beacon collector but it uses Astar (to show how to do it) and shows the Astar graphics
 * 
 * @author amy
 */
public class ExampleAStarClient extends TeamClient {
	FollowPathAction followPathAction;
	HashMap <UUID, Graph> graphByShip;
	int currentSteps;
	int REPLAN_STEPS = 20;

	/**
	 * Assigns ships to asteroids and beacons, as described above
	 */
	public Map<UUID, AbstractAction> getMovementStart(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		HashMap<UUID, AbstractAction> actions = new HashMap<UUID, AbstractAction>();

		// loop through each ship
		for (AbstractObject actionable :  actionableObjects) {
			if (actionable instanceof Ship) {
				Ship ship = (Ship) actionable;
				AbstractAction current = ship.getCurrentAction();

				if (current == null || currentSteps >= REPLAN_STEPS) {
					Beacon beacon = pickNearestBeacon(space, ship);
					AbstractAction action = getAStarPathToGoal(space, ship, beacon.getPosition());
					actions.put(ship.getId(), action);
					currentSteps = 0;
				}
			} else {
				// it is a base.  Do nothing
				actions.put(actionable.getId(), new DoNothingAction());
			}
		} 
		currentSteps++;
		return actions;
	}
	
	/**
	 * Follow an aStar path to the goal
	 * @param space
	 * @param ship
	 * @param goalPosition
	 * @return
	 */
	private AbstractAction getAStarPathToGoal(Toroidal2DPhysics space, Ship ship, Position goalPosition) {
		AbstractAction newAction;
		
		Graph graph = AStarSearch.createGraphToGoalWithBeacons(space, ship, goalPosition, new Random());
		Vertex[] path = graph.findAStarPath(space);
		followPathAction = new FollowPathAction(path);
		//followPathAction.followNewPath(path);
		newAction = followPathAction.followPath(space, ship);
		graphByShip.put(ship.getId(), graph);
		return newAction;
	}



	/**
	 * Find the nearest beacon to this ship
	 * @param space
	 * @param ship
	 * @return
	 */
	private Beacon pickNearestBeacon(Toroidal2DPhysics space, Ship ship) {
		// get the current beacons
		Set<Beacon> beacons = space.getBeacons();

		Beacon closestBeacon = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		for (Beacon beacon : beacons) {
			double dist = space.findShortestDistance(ship.getPosition(), beacon.getPosition());
			if (dist < bestDistance) {
				bestDistance = dist;
				closestBeacon = beacon;
			}
		}

		return closestBeacon;
	}



	@Override
	public void getMovementEnd(Toroidal2DPhysics space, Set<AbstractActionableObject> actionableObjects) {
		ArrayList<Asteroid> finishedAsteroids = new ArrayList<Asteroid>();


	}

	@Override
	public void initialize(Toroidal2DPhysics space) {
		graphByShip = new HashMap<UUID, Graph>();
		currentSteps = 0;
	}

	@Override
	public void shutDown(Toroidal2DPhysics space) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<SpacewarGraphics> getGraphics() {
		HashSet<SpacewarGraphics> graphics = new HashSet<SpacewarGraphics>();
		if (graphByShip != null) {
			for (Graph graph : graphByShip.values()) {
				// uncomment to see the full graph
				//graphics.addAll(graph.getAllGraphics());
				graphics.addAll(graph.getSolutionPathGraphics());
			}
		}
		HashSet<SpacewarGraphics> newGraphicsClone = (HashSet<SpacewarGraphics>) graphics.clone();
		graphics.clear();
		return newGraphicsClone;
	}

	@Override
	/**
	 * Never purchases
	 */
	public Map<UUID, PurchaseTypes> getTeamPurchases(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects, 
			ResourcePile resourcesAvailable, 
			PurchaseCosts purchaseCosts) {

		return null;
	}

	/**
	 * No shooting
	 * 
	 * @param space
	 * @param actionableObjects
	 * @return
	 */
	@Override
	public Map<UUID, SpaceSettlersPowerupEnum> getPowerups(Toroidal2DPhysics space,
			Set<AbstractActionableObject> actionableObjects) {
		return null;
	}

}
