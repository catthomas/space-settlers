package stan5674;

import java.io.Serializable;
import java.lang.Math;
import java.util.Random;
import stan5674.Genome;

public class Genetic implements Serializable{

	/*
		Genetic class handles generational evolution stuff on Genomes
	*/
	Genome[] pop;
	Random rand = new Random();

	public Genetic(int popSize){
		pop = new Genome[popSize];

		for (int i = 0; i < popSize; i++){
			pop[i] = new Genome();
		}
         

	}

	public void mutate(Genome gen){
		float[] genes = gen.getGenes();


	}

	public void evolve(){

	}

	public float normal(float mean, float dev){
		return (float)rand.nextGaussian()*dev + mean;
	}

	public float normal(float dev){
		return this.normal(0f, dev);
	}

	public float normal(){
		return this.normal(0f, .1f);
	}

}