package stan5674;

import java.io.Serializable;
import java.lang.Math;

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
	
	//...testing...
	//public static void main(String[] args){
	// 	Genome g = new Genome();
	// 	System.out.println(g.fuelCoefGene());
	// 	g.setFuelCoef(8.8f);
	// 	System.out.println(g.fuelCoefGene());

	// 	g.getGenes()[0] = 16.16f;

	// 	System.out.println(g.fuelCoefGene());

	// 	float [] b = g.getGenes();

	// 	b[0] = 32.32f;

	// 	System.out.println(g.fuelCoefGene());
	// }

	public Genome(){
		//Generate DNA
		genes = new float[4];

		for (int i = 0; i<4; i++){
			this.genes[i] = (float)Math.random();
		}
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

	public void setFitness(float fit){
		this.fitness = fit;
	}

	public float getFitness(){
		return this.fitness;
	}

	public float[] getGenes(){
		return this.genes;
	}

} //end Genome
