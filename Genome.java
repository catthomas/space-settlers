package stan5674;

import java.io.Serializable;
import java.lang.Math;
import java.util.*;

/**
 * Contains genes that are evolved over time. 
 */
public class Genome implements Serializable {

	float[] genes; 			//complete DNA sequence of agent as a parameterization of the PilotState class
	float fitness = 0;

	/**
	 * Unique ID of class in order to be written to a file
	 */
	private static final long serialVersionUID = -2069500657684221613L;

	/**
	 * Returns a new genomes with randomized genes. 
	 */
	public Genome(){
		genes = new float[4];

		for (int i = 0; i<4; i++){
			this.genes[i] = (float)Math.random();
		}
	}

	//new Genome based off given genes
	public Genome(float[] genes){
		this.genes = Arrays.copyOf(genes, genes.length);
		this.fitness = 0;
	}

	//copy constructor
	public Genome(Genome g){
		this.genes = Arrays.copyOf(g.getGenes(), g.getGenes().length);
		this.fitness = g.getFitness();
	}

	//Getter for fuel coef - the minimum amount of fuel before refuel search needed
	public float fuelCoefGene(){
		return this.genes[0];
	}

	//Setter for fuel coefficient
	public void setFuelCoef(float gene){
		this.genes[0] = gene;
	}
	
	//Gets the cargo capacity as a float - max resource to carry before heading to base
	public float cargoCapacityGene(){
		return this.genes[1];
	}

	//Setter for cargo capacity
	public void setCargoCapacity(float gene){
		this.genes[1] = gene;
	}

	//Gets max speed allowed by gene
	public float maxSpeedGene(){
		return this.genes[2];
	}

	//Setter for max speed
	public void setMaxSpeed(float gene){
		this.genes[2] = gene;
	}

	//Getter for frontier - the minimum distance between bases
	public float frontierGene(){
		return this.genes[3];
	}

	//Setter for frontier
	public void setFrontier(float gene){
		this.genes[3] = gene;
	}

	//Getter for genome's fitness
	public float getFitness(){
		return this.fitness;
	}

	//Setter for fitness
	public void setFitness(float fit){
		this.fitness = fit;
	}

	//Returns genome's genes in an array
	public float[] getGenes(){
		return this.genes;
	}

	//Setter for genes
	public void setGenes(float[] genes){
		this.genes = genes;
	}

} //end Genome
