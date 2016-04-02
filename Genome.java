package stan5674;

import java.io.Serializable;
import java.lang.Math;
import java.util.*;

/**
 * Class used for learning!
 * @author catherinethomas
 *
 */
public class Genome implements Serializable {

	float[] genes; 			//complete DNA sequence of agent as a parameterization of the PilotState class
	float fitness = 0;

	/**
	 * Unique ID of class in order to be written to a file
	 */
	private static final long serialVersionUID = -2069500657684221613L;

	public Genome(){
		genes = new float[4];

		for (int i = 0; i<4; i++){
			this.genes[i] = (float)Math.random();
		}
	}

	//copy constructor
	public Genome(Genome g){
		this.genes = Arrays.copyOf(g.getGenes(), g.getGenes().length);
		this.fitness = g.getFitness();
	}

	public float fuelCoefGene(){
		return this.genes[0];
	}

	public void setFuelCoef(float gene){
		this.genes[0] = gene;
	}

	public float cargoCapacityGene(){
		return this.genes[1];
	}

	public void setCargoCapacity(float gene){
		this.genes[1] = gene;
	}

	public float maxSpeedGene(){
		return this.genes[2];
	}

	public void setMaxSpeed(float gene){
		this.genes[2] = gene;
	}

	public float frontierGene(){
		return this.genes[3];
	}

	public void setFrontier(float gene){
		this.genes[3] = gene;
	}

	public float getFitness(){
		return this.fitness;
	}

	public void setFitness(float fit){
		this.fitness = fit;
	}

	public float[] getGenes(){
		return this.genes;
	}

	public void setGenes(float[] genes){
		this.genes = genes;
	}

} //end Genome
