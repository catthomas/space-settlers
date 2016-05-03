package stan5674;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class ShipState {
		/** The ship the state is based on **/
		Ship vessel;
		
		/** Current target of the ship **/
		AbstractObject target;
		
		/** Default constructor for the class **/ 
		ShipState(Ship vessel){
			this.vessel = vessel;
			this.target = null;
		} //end ShipState
		
		/**Getter for target **/
		public AbstractObject getTarget(){
			return target;
		} //end getTarget
		
		/**Setter for target **/
		public void setTarget(AbstractObject target){
			this.target = target;
		} //end setTarget
		
		/** Getter for vessel **/
		public Ship getVessel(){
			return vessel;
		} //end getVessel
		
		/** Checks to see if vessel needs to refuel **/
		public boolean needsFuel(double fuelCap){
			if(vessel.getEnergy() < fuelCap){
				return true;
			}
			return false;
		} //end needsFuel
		
		/** Checks if vessel is at max cargo capacity **/
		public boolean atMaxCargo(double maxCargo){
			if(vessel.getResources().getTotal() >= maxCargo){
				return true;
			}
			return false;
		} //end atMaxCargo
		
		/** Returns the closest beacon to the ship, can avoid certain beacons if needed **/
		public Beacon getNearestBeacon(Toroidal2DPhysics space, Set<AbstractObject> avoid){
			Set<Beacon> beacons = space.getBeacons();
			beacons.removeAll(avoid);
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
		} //end getNearestBeacon
		
		/** Returns the closest base to the ship **/
		public Base getNearestBase(Toroidal2DPhysics space){
			Set<Base> bases = space.getBases();
			double shortest = Double.POSITIVE_INFINITY;
			Position location = vessel.getPosition();
			double dist;
			Base nearestBase = null;

			//find closest base
			for (Base base : bases) {
				if (base.getTeamName().equalsIgnoreCase(vessel.getTeamName())){
					dist = space.findShortestDistance(location, base.getPosition());

					if (dist < shortest) {		
						shortest = dist;
						nearestBase = base;
					}
				}
			}
			return nearestBase;
		} //end getNearestBase
		
		/** Returns the closest prospect to the ship, can avoid given objects if needed **/
		public Asteroid getNearestProspect(Toroidal2DPhysics space, Set<AbstractObject> avoid){
			List<Asteroid> prospects = getMinableAsteroids(space);
			prospects.removeAll(avoid);
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
		} //end getNearestProspect 
		
		/** Helper function to only return asteroids with resources **/
		public List<Asteroid> getMinableAsteroids(Toroidal2DPhysics space){
			List<Asteroid> asteroids = new ArrayList<Asteroid>(space.getAsteroids());
			List<Asteroid> minable = new ArrayList<Asteroid>();

			for (Asteroid ast : asteroids){
				if (ast.isMineable()){
					minable.add(ast);
				} 
			}
			return minable;
		} // end getMinableAsteroids
} //end ShipState class
