package stan5674;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import spacesettlers.actions.*;
import spacesettlers.objects.*;
import spacesettlers.objects.resources.ResourcePile;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.*;

/**
 * Use a pilot's perspective to make decisions
 *
 */
public class PilotState {

	static int LOW_FUEL = 2000; 		//point of return
	static int CARGO_CAPACITY = 1000; 	
	static int SPEED = 100;				//speed of travel coefficient
	static int FOV = 500;				//maybe go ahead and grab resources that are already in FOV area
	static int FRONTIER = 250;			//min distance between bases

	//finds the closest fuel beacon
	public Beacon findNearestBeacon(Toroidal2DPhysics space, Ship vessel){
		Set<Beacon> beacons = space.getBeacons();
		double shortest = Double.POSITIVE_INFINITY;
		Position location = vessel.getPosition();
		double dist;
		Beacon nearestBeacon = null;

		for (Beacon beacon : beacons) {
			dist = space.findShortestDistance(location, beacon.getPosition());

			if (dist < shortest) {
				shortest = dist;
				nearestBeacon = beacon;
			}
		}

		return nearestBeacon;
	}

	//finds the base nearest to the vessel
	public Base findNearestBase(Toroidal2DPhysics space, Ship vessel){
		Set<Base> bases = space.getBases();
		double shortest = Double.POSITIVE_INFINITY;
		Position location = vessel.getPosition();
		double dist;
		Base nearestBase = null;

		//find closest base
		for (Base base : bases) {
			if (base.getTeamName().equalsIgnoreCase(vessel.getTeamName())){
				dist = space.findShortestDistance(location, base.getPosition());

				if (dist < shortest && base.getEnergy() >= 500) {
					shortest = dist;
					nearestBase = base;
				}
			}
		}
		//System.out.println("~~~~~~BASE IS: " + nearestBase.getId());

		return nearestBase;
	}

	//find nearest refueling station, either base or beacon
	public AbstractObject nearestRefuel(Toroidal2DPhysics space, Ship vessel){
		Position location = vessel.getPosition();

		Base nearestBase = findNearestBase(space, vessel);
		Beacon nearestBeacon = findNearestBeacon(space, vessel);

		if (space.findShortestDistance(location, nearestBase.getPosition()) <= space.findShortestDistance(location, nearestBeacon.getPosition())){
			return nearestBase;
		} else {
			return nearestBeacon;
		}
	}

	//Pilot's brains
	public AbstractAction decideAction(Toroidal2DPhysics space, Ship vessel){
		Position currentPosition = vessel.getPosition();
		AbstractObject goal;

		//get refueled
		if (vessel.getEnergy() < LOW_FUEL){
			//System.out.println("~~~~~MOVING TO REFUEL~~~~~");
			goal = nearestRefuel(space, vessel);

			AbstractAction newAction = null;
			
			// if no object found, skip turn
			if(goal == null) {
				newAction = new DoNothingAction(); 
			} else {
				newAction = new MoveAction(space, currentPosition, goal.getPosition(), optimalApproach(space, currentPosition, goal.getPosition()));
			};

			return newAction;
		}
		//return resources to base
		if (vessel.getResources().getTotal() > CARGO_CAPACITY){
			//System.out.println("~~~~~MOVING TO BASE~~~~~");
			goal = findNearestBase(space, vessel);

			return new MoveAction(space, currentPosition, goal.getPosition(), optimalApproach(space, currentPosition, goal.getPosition()));
		}


		//System.out.println("~~~~~MOVING TO PROSPECT~~~~~");
		goal = findNearestProspect(space, vessel);

		//System.out.println(goal.getPosition().getTranslationalVelocity());
		return new MoveAction(space, currentPosition, goal.getPosition(), optimalApproach(space, currentPosition, goal.getPosition()));
	}

	//pilot decides what to buy from shop
	public PurchaseTypes shop(Toroidal2DPhysics space, Ship vessel, ResourcePile funds, PurchaseCosts prices){
		Position currentPosition = vessel.getPosition();
		if (prices.canAfford(PurchaseTypes.BASE, funds)) {;	//buy bases when you are in the frontier
			for (Base base : space.getBases()) {
				if (base.getTeamName().equalsIgnoreCase(vessel.getTeamName())) {
					double distance = space.findShortestDistance(currentPosition, base.getPosition());
					if (distance < FRONTIER) { 		//do not buy a base if within minimum frontier distance
						return null;
					}
				}
			}

			return  PurchaseTypes.BASE;
		}

		return null;
	};

	public Asteroid findNearestProspect(Toroidal2DPhysics space, Ship vessel){
		List<Asteroid> prospects = getMinableAsteroids(space);
		double shortest = Double.POSITIVE_INFINITY;
		Position location = vessel.getPosition();
		double dist;
		Asteroid nearestProspect = null;

		for (Asteroid prospect : prospects){
			dist = space.findShortestDistance(location, prospect.getPosition());

			if (dist<shortest){
				shortest = dist;
				nearestProspect = prospect;
			}
		}

		return nearestProspect;
	}

	public Vector2D optimalApproach(Toroidal2DPhysics space, Position current, Position goal){
		//return fastest velocity vestor that the vessel can travel and still reach goal
		//Vector2D goalVelocity = goal.getTranslationalVelocity();  //movement vector of goal object--we can use this to take better paths
		Vector2D distVec = space.findShortestDistanceVector(current, goal).getUnitVector();		//orientation toward goal
		
		//consider slower approaches when low on energy

		distVec = distVec.multiply(SPEED);		//controls speed in orientation

		//Vector2D cruisingSpeed = new Vector2D(50, 50);

		return distVec;
	};		

	//helper function to only return asteroids with resources
	public List<Asteroid> getMinableAsteroids(Toroidal2DPhysics space){
		List<Asteroid> asteroids = new ArrayList<Asteroid>(space.getAsteroids());
		List<Asteroid> minable = new ArrayList<Asteroid>();

		for (Asteroid ast : asteroids){
			if (ast.isMineable()){
				minable.add(ast);
			} 
		}

		return minable;
	};
	

	/**
	 * Sorts the asteroid in space by total resource value.
	 * Can be used to target higher value asteroids by the agent. 
	 * 
	 * @return List of asteroids ordered by total resources
	 */
	public List<Asteroid> asteroidsByResources(Toroidal2DPhysics space) {
		//Comparator that utilizes total resource value 
		Comparator<Asteroid> value = new Comparator<Asteroid>() {
	        @Override
	        public int compare(Asteroid a1, Asteroid a2) {
	            if(a1.getResources().getTotal() < a2.getResources().getTotal()){
	            	return -1;
	            } else if (a1.getResources().getTotal() > a2.getResources().getTotal()){
	            	return 1;
	            } else{
	            	return 0;
	            }
	        }
	    };
	    
		List<Asteroid> asteroids = getMinableAsteroids(space);
		Collections.sort(asteroids, value); //sort asteroids by total resource value
		return asteroids;
	} //end asteroidsByValue
	
	/**
	 * Returns a list of all objects (asteroids, bases, other ships, beacons) within the pilot's field of view
	 * TODO: test this method
	 * @param space
	 * @param object
	 * @param radius
	 * @return List of all objects within specified radius, sorted by distance
	 */
	public List<AbstractObject> objectsInFov(Toroidal2DPhysics space, Ship ship){
		LinkedList<AbstractObject> objects = new LinkedList<AbstractObject>();
		Position currentPosition = ship.getPosition();

		for (AbstractObject obj : space.getAllObjects()) {
			double dist = space.findShortestDistance(currentPosition, obj.getPosition());
			if (dist < FOV) {
				//Add to linkedlist of objects in order of closest object first
				objects.add(obj);
			}
		}
		return objects;
	}

	/**
	 * Checks to see if the given base is about to die. For this,
	 * it checks 1.) if the base is the home base and 2.) if it has
	 * less than 100 energy available. 
	 * 
	 * @param base
	 * @return Boolean saying whether the given base is about to die!
	 */
	public boolean isBaseNearDeath(Base base){
		if(base.isHomeBase() && base.getEnergy() <= 100){
			return true;
		}
		return false;
	} //end isBaseNearDeath
}