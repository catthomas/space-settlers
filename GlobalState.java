package thom8296;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;
import spacesettlers.utilities.Vector2D;

/**
 * Uses global knowledge representation to develop heuristics
 * about the Spacewar environment. 
 * @author catherinethomas
 *
 */
public class GlobalState {

	/**
	 * Sorts the asteroid in space by total resource value.
	 * Can be used to target higher value asteroids by the agent. 
	 * 
	 * @return List of asteroids
	 */
	public List<Asteroid> asteroidsByHighestValue(Toroidal2DPhysics space) {
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
	    
		List<Asteroid> asteroids = new ArrayList<Asteroid>(space.getAsteroids());
		Collections.sort(asteroids, value); //sort asteroids by total resource value
		return asteroids;
	} //end asteroidsByValue
	
	/**
	 * Returns a list of all objects (asteroids, bases, other ships, beacons) within a certain radius of the object, SORTED
	 * by distance to the object. 
	 * TODO: test this method
	 * @param space
	 * @param object
	 * @param radius
	 * @return List of all objects within specified radius, sorted by distance
	 */
	public List<AbstractObject> objectsWithinRadius(Toroidal2DPhysics space, AbstractObject object, double radius){
		LinkedList<AbstractObject> objects = new LinkedList<AbstractObject>();

		for (AbstractObject obj2 : space.getAllObjects()) {
			double dist = space.findShortestDistance(object.getPosition(), obj2.getPosition());
			if (dist < radius) {
				//Add to linkedlist of objects in order of closest object first
				if (objects.size() == 0) { //list is empty, add
		            objects.add(obj2);
		        } else {
		            int i = 0;
		            while (space.findShortestDistance(object.getPosition(), objects.get(i).getPosition()) < dist) {
		                i++;
		            }
		            
		            if(i == objects.size()){ //add to end of list
		            	objects.addLast(obj2);
		            } else { //add at index 
		            	objects.add(i, obj2);
		            }
		        }
			}
		}
		return objects;
	}
	
	/**
	 * Returns the closest object (asteroid, base, ship, or beacon) to the one given. 
	 * @param space
	 * @param object
	 * @return Closest other object in space to the given one. 
	 */
	public AbstractObject getClosestOtherObject(Toroidal2DPhysics space, AbstractObject object){
		AbstractObject closest = null;
		double minDistance = Double.MAX_VALUE;
		
		for(AbstractObject obj2 : space.getAllObjects()){
			if(space.findShortestDistance(object.getPosition(), obj2.getPosition()) < minDistance){
				closest = obj2;
			}
		}	
		return closest;
	} //end getClosestOtherObject
	
	/**
	 * Takes in two objects in space, and returns true or false
	 * depending on whether the two objects are experiencing a collision. 
	 *  
	 * @param space
	 * @param object1
	 * @param object2
	 * @return True if objects are colliding, false otherwise 
	 */
	public boolean areObjectsColliding(Toroidal2DPhysics space, AbstractObject obj1, AbstractObject obj2){
		//If experiencing a collision, distance between them will be less than or equal to their radi combined
		if(space.findShortestDistance(obj1.getPosition(), obj2.getPosition()) <= (obj1.getRadius() + obj2.getRadius())){
			return true;
		}
		return false;
	} //end areTwoObjectsColliding
	
	/**
	 * Test to see if the given object is experiencing a collision with a base
	 * @param object
	 * @return
	 */
	public boolean isCollidingWithBase(Toroidal2DPhysics space, AbstractObject object){
		AbstractObject possibleCollision = getClosestOtherObject(space, object); //What about simultaneous collisions?
		if(possibleCollision instanceof Base && areObjectsColliding(space, object, possibleCollision)){
			return true;
		}
		return false;
	} //end isCollidingWithBase
	
	/**
	 * Test to see if the given object is experiencing a collision with an unmineable asteroid
	 * @param object
	 * @return
	 */
	public boolean isCollidingWithUnmineableAsteroid(Toroidal2DPhysics space, AbstractObject object){
		AbstractObject possibleCollision = getClosestOtherObject(space, object);
		if(possibleCollision instanceof Asteroid && areObjectsColliding(space, object, possibleCollision) && !((Asteroid) possibleCollision).isMineable()){
			return true;
		}
		return false;
	} //end isCollidingWithBase
	
	/**
	 * Returns the highest value mineable asteroid within a certain radius of a ship - MIGHT BE BETTER TO PUT IN EGOCENTRIC??
	 * @param space
	 * @param ship
	 * @param radius
	 * @return Highest value mineable asteroid within a given radius
	 */
	public Asteroid getHighestValueAsteroidWithinRadius(Toroidal2DPhysics space, Ship ship, double radius){
		double highestValue = Double.MIN_VALUE;
		Asteroid highestAsteroid = null;
		
		for (Asteroid asteroid : space.getAsteroids()){
			if(asteroid.isMineable() && space.findShortestDistance(ship.getPosition(), asteroid.getPosition()) <= radius 
					&& asteroid.getResources().getTotal() > highestValue){
				highestAsteroid = asteroid;
			}
		}
		return highestAsteroid;
	} //end getHighestValueAsteroidWithinRadius
	
//	/**
//	 * Estimate energy cost starting from an object to a point.
//	 */
//	public double energyCostFromObjectToPoint(Toroidal2DPhysics space, AbstractObject object, Position point){
//		//TODO: stub
//		return 0.0;
//	} //end energyCostFromObjectToPoint
	
	/**
	 * Compares the velocity of two objects in space. Takes in the unique identifiers
	 * of the two objects to be compared. These objects can be asteroids, bases, beacons, or ships. 
	 * 
	 * @param object1 UUID of the first object to be compared
	 * @param object2 UUID of the second object to be compared
	 * @return -1 if the velocity of the first object is less than that of the second,
	 * 0 if equal, and 1 if the velocity of the first object is greater than that of the second. 
	 */
	public int compareVelocity(Toroidal2DPhysics space, AbstractObject obj1, AbstractObject obj2){		
		if(obj1.getPosition().getTotalTranslationalVelocity() > obj2.getPosition().getTotalTranslationalVelocity()){
			return 1;
		} else if(obj1.getPosition().getTotalTranslationalVelocity() < obj2.getPosition().getTotalTranslationalVelocity()){
			return -1;
		} else {
			return 0;
		}
	} //end compareVelocity
	
	/**
	 * This method takes in a start position and goal object and returns a
	 * FARTHER version of the goal object. 
	 * The format mimics the 'MoveToObjectAction' in the space class, which is 
	 * used for targeting.
	 * The purpose of this method is to effectively create a goal that can allow an agent to 'speed up'
	 * @param space
	 * @param startPosition Location of start
	 * @param AbstractObject The original target 
	 * @return A farther version of the goal object.  
	 */
	public AbstractObject scaleDistanceVector(Toroidal2DPhysics space, Position startPosition, AbstractObject goalObject, double scalar){
		Vector2D distVec = space.findShortestDistanceVector(startPosition, goalObject.getPosition());
		distVec = distVec.multiply(scalar);
		
		//Calculate X,Y coordinate of new distance vector endpoint
		Position newPosition = goalObject.getPosition().deepCopy();
		double x = newPosition.getX() + distVec.getXValue();
		if(x > space.getWidth()){ //don't exceed bounds of map - adjust coordinates accordingly 
			x = space.getWidth();
		} else if ( x < 0){
			x = 0;
		}
		double y = newPosition.getY() + distVec.getYValue();
		if(y > space.getHeight()){
			y = space.getHeight();
		} else if ( y < 0){
			y = 0;
		}
		newPosition.setX(x);
		newPosition.setY(y);
		
		//Create an abstract object that mimics the goal object, but at a farther distance
		AbstractObject newGoal = goalObject.deepClone();
		System.out.println("original coordinate: " + newGoal.getPosition());
		newGoal.setPosition(newPosition);
		System.out.println("new coordinate: " + newGoal.getPosition());
		return newGoal;
	} //end scaleDistanceVector
	
	/**
	 * Method takes in a start location and a target object, and returns
	 * a target object with a position at a different angel as well as distance.
	 * Uses radians to rotate and a scalar multiple for strength
	 * @param space
	 * @param startPosition
	 * @param goalObject
	 * @param radians
	 * @param scalar
	 * @return
	 */
	public AbstractObject rotateTrajectoryVector(Toroidal2DPhysics space, Position startPosition, AbstractObject goalObject, double radians, double scalar){
		Vector2D distVec = space.findShortestDistanceVector(startPosition, goalObject.getPosition());
		distVec = distVec.rotate(radians);
		distVec = distVec.multiply(scalar);
		
		//Calculate X,Y coordinate of new distance vector endpoint
		Position newPosition = goalObject.getPosition().deepCopy();
		double x = newPosition.getX() + distVec.getXValue();
		if(x > space.getWidth()){ //don't exceed bounds of map - adjust coordinates accordingly 
			x = space.getWidth();
		} else if ( x < 0){
			x = 0;
		}
		double y = newPosition.getY() + distVec.getYValue();
		if(y > space.getHeight()){
			y = space.getHeight();
		} else if ( y < 0){
			y = 0;
		}
		newPosition.setX(x);
		newPosition.setY(y);
		
		//Create an abstract object that mimics the goal object, but at an angle
		AbstractObject newGoal = goalObject.deepClone();
		System.out.println("original collision avoidance coordinate: " + newGoal.getPosition());
		newGoal.setPosition(newPosition);
		System.out.println("new collision avoidance coordinate: " + newGoal.getPosition());
		return newGoal;
	}
	
	/**
	 * Useful for determining where a ship should head to refuel.
	 * MAKE SPECIFICALLY FOR SHIP IN EGOCENTRIC MODEL?? OR MOVE TO EGOCENTRIC? 
	 * Automatically returns a beacon if base with sufficient energy not found 
	 * @param object
	 * @param ship
	 * @param fuelMin Minimum amount of fuel that should be available at the beacon/base
	 * @return 
	 */
	public AbstractObject closestBeaconOrBaseToObject(Toroidal2DPhysics space, Ship ship, double fuelMin){
		// get the current beacons - refactor with add all into one set? 
		Set<Beacon> beacons = space.getBeacons();
		// get the current bases
		Set<Base> bases = space.getBases();

		AbstractObject closestFuel = null;
		double bestDistance = Double.POSITIVE_INFINITY;

		//find minimal beacon 
		for (Beacon beacon : beacons) {
			double dist = space.findShortestDistance(ship.getPosition(), beacon.getPosition());
			if (dist < bestDistance) {
				bestDistance = dist;
				closestFuel = beacon;
			}
		}
		
		//find minimal base
		for (Base base : bases) {
			double dist = space.findShortestDistance(ship.getPosition(), base.getPosition());
			if (base.getTeamName().equalsIgnoreCase(ship.getTeamName()) && dist < bestDistance && base.getEnergy() >= fuelMin) {
				bestDistance = dist;
				closestFuel = base;
			}
		}

		return closestFuel;
	} //end closestBeaconOrBaseToObject
	
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