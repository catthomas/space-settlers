package stan5674;

import java.io.Serializable;
import java.lang.Math;

/**
 * Class used for learning!
 * @author catherinethomas
 *
 */
public class Genome implements Serializable {

	float FUEL_COEF; 			//determine when ship needs to return for fueling
	float CARGO_CAPACITY;
	float MAX_SPEED;			//speed of travel coefficient
	float FRONTIER;				//min distance between bases
	float fitness = 0;

	/**
	 * Unique ID of class in order to be written to a file
	 */
	private static final long serialVersionUID = -2069500657684221613L;

	public Genome(){
		//Generate DNA
		this.FUEL_COEF = (float)Math.random();
		this.CARGO_CAPACITY = (float)Math.random();
		this.MAX_SPEED = (float)Math.random();
		this.FRONTIER = (float)Math.random();
	}

	float fuelCoefGene(){
		return this.FUEL_COEF;
	}

	float cargoCapacityGene(){
		return this.CARGO_CAPACITY;
	}

	float maxSpeedGene(){
		return this.MAX_SPEED;
	}

	float frontierGene(){
		return FRONTIER;
	}

	void setFitness(float fit){
		this.fitness = fit;
	}

	float getFitness(){
		return this.fitness;
	}

} //end Genome
