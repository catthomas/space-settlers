package stan5674;

import java.io.Serializable;
import java.lang.Math;

/**
 * Class used for learning!
 * @author catherinethomas
 *
 */
public class Genome implements Serializable {

	float FUEL_COEF;
	float CARGO_CAPACITY;
	float MAX_SPEED;
	float FRONTIER;
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

	public float fuelCoefGene(){
		return this.FUEL_COEF;
	}

	public float cargoCapacityGene(){
		return this.CARGO_CAPACITY;
	}

	public float maxSpeedGene(){
		return this.MAX_SPEED;
	}

	public float frontierGene(){
		return this.FRONTIER;
	}

	public void setFitness(float fit){
		this.fitness = fit;
	}

	public float getFitness(){
		return this.fitness;
	}

} //end Genome
