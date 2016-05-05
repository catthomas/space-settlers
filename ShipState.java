package stan5674;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.awt.Color;

import spacesettlers.objects.AbstractObject;
import spacesettlers.objects.Asteroid;
import spacesettlers.objects.Base;
import spacesettlers.objects.Beacon;
import spacesettlers.graphics.*;
import spacesettlers.objects.Ship;
import spacesettlers.simulator.Toroidal2DPhysics;
import spacesettlers.utilities.Position;

public class ShipState {
		/** The ID ship the state is based on **/
		UUID vessel;
		
		/** Current target of the ship **/
		AbstractObject target;
		LineGraphics graphics;

		/** Role of ship **/
		boolean diamondChaser;
		
		/** Default constructor for the class **/ 
		ShipState(UUID vessel){
			this.vessel = vessel;
			this.target = null;
			this.graphics = null;
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
		public UUID getVessel(){
			return vessel;
		} //end getVessel
		
		/** Checks to see if vessel needs to refuel **/
		public boolean needsFuel(double fuelCap, Ship vessel){
			if(vessel.getEnergy() < fuelCap){
				return true;
			}
			return false;
		} //end needsFuel
		
		/** Checks if vessel is at max cargo capacity **/
		public boolean atMaxCargo(double maxCargo, Ship vessel){
			if(vessel.getResources().getTotal() >= maxCargo){
				return true;
			}
			return false;
		} //end atMaxCargo
		
		/** Returns the closest beacon to the ship, can avoid certain beacons if needed **/
		public Beacon getNearestBeacon(Toroidal2DPhysics space, Ship vessel, Set<AbstractObject> avoid){
			if(target != null && avoid != null && avoid.contains(target)){
				avoid.remove(target); //don't avoid your own target
			}
			
			Set<Beacon> beacons = space.getBeacons();
			if(avoid != null){
				beacons.removeAll(avoid);
			}
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
		public Base getNearestBase(Toroidal2DPhysics space, Ship vessel, boolean refuel, double refuelMin){
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
						if (refuel){
							if (base.getEnergy() >= refuelMin){
								shortest = dist;
								nearestBase = base;
							}
						} else {
							shortest = dist;
							nearestBase = base;
						}
					}
				}
			}
			return nearestBase;
		} //end getNearestBase
		
		/** Returns the closest prospect to the ship, can avoid given objects if needed **/
		public Asteroid getNearestProspect(Toroidal2DPhysics space, Ship vessel,  Set<AbstractObject> avoid){
			if(target != null && avoid != null && avoid.contains(target)){
				avoid.remove(target); //don't avoid your own target
			}

			List<Asteroid> prospects = getMinableAsteroids(space);
			if(avoid != null){
				prospects.removeAll(avoid);
			}
			double shortest = Double.POSITIVE_INFINITY;
			Position location = vessel.getPosition();
			double dist;
			Asteroid nearestProspect = null;

			for (Asteroid prospect : prospects){
				dist = space.findShortestDistance(location, prospect.getPosition());

				if (dist<shortest && space.isPathClearOfObstructions(vessel.getPosition(), prospect.getPosition(), getUnminableAsteroids(space), vessel.getRadius())){ //only target unobstructed prospects
					shortest = dist;
					nearestProspect = prospect;
				}
			}

			return nearestProspect;
		} //end getNearestProspect 

		/** Returns the closest beacon or base to the ship **/
		public AbstractObject getNearestRefuel(Toroidal2DPhysics space, Ship vessel, double refuelMin){
			Position location = vessel.getPosition();

			Base nearestBase = getNearestBase(space, vessel, true, refuelMin);
			Beacon nearestBeacon = getNearestBeacon(space, vessel, null);
		
			if(nearestBeacon != null && nearestBase == null){
				return nearestBeacon;
			} else if (nearestBeacon == null && nearestBase != null){
				return nearestBase;
			} else if (nearestBeacon != null && nearestBase != null){
				if (space.findShortestDistance(location, 
					nearestBase.getPosition()) > space.findShortestDistance(location, nearestBeacon.getPosition())){
					return nearestBeacon;
				} else {
					return nearestBase;
				}
			}
			return getNearestBase(space, vessel, false, refuelMin); //failsafe
		} //end getNearestProspect
		
		/** Returns the highest value asteroid in the game - global data **/
		public Asteroid getDiamond(Toroidal2DPhysics space){
			List<Asteroid> prospects = getMinableAsteroids(space);
			Asteroid diamond = prospects.get(0);
			
			for (Asteroid prospect : prospects){
				if(prospect.getResources().getTotal() > diamond.getResources().getTotal()){
					diamond = prospect;
				}
			}
			return diamond;
		} //end getDiamond
		
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
		
		/** Helper function to only return asteroids without resources **/
		public Set<AbstractObject> getUnminableAsteroids(Toroidal2DPhysics space){
			List<Asteroid> asteroids = new ArrayList<Asteroid>(space.getAsteroids());
			List<AbstractObject> unminable = new ArrayList<AbstractObject>();

			for (Asteroid ast : asteroids){
				if (!ast.isMineable()){
					unminable.add(ast);
				} 
			}
			return new HashSet<AbstractObject>(unminable);
		} // end getMinableAsteroids

		/** Updates the line between ship and target **/
		public void setGraphics(Toroidal2DPhysics space, Ship vessel){
			if(this.target != null){
				LineGraphics line = new LineGraphics(vessel.getPosition(), target.getPosition(), 
				space.findShortestDistanceVector(vessel.getPosition(), target.getPosition()));
				line.setLineColor(Color.RED);
				this.graphics = line;
			}
		} //end setGraphics	

		/** Returns ships trajectory **/
		public LineGraphics getGraphics(){
			return this.graphics;
		} //end getGraphics
} //end ShipState class
