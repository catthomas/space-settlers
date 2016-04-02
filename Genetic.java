package stan5674;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.Math;
import java.util.*;

import stan5674.Genome;

public class Genetic implements Serializable{

	/*
		Genetic class handles generational evolution stuff on Genomes
	*/
	float MUT_RATE = .3f;
	float MUT_VAR = .1f;
	int TOURN_SIZE = 5;			//Expected number of greatest fit = (1 - ((popSize-1)/popSize)^tournSize))*popSize (5 tourn ~ 5/100)
	int ELITE_CLONES = 0;

	ArrayList<Genome> lastPop;
	ArrayList<Genome> pop;
	int nextCand;
	int generation;

	Random rand = new Random();
	
	/** Singleton class - in case multiple cat adam agents running on ladder at once **/
	private static Genetic singleton = new Genetic();
	String fileName = "knowledge.ser";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4070454895004720955L;

	private Genetic(){
		File f = new File("stan5674/"+fileName);
		if(f.exists()) { //learning has occurred previously, set up on past knowledge
			System.out.println("LEARNING HAS OCCURRED BEFORE! :)");
			try {
				FileInputStream fis;
				ObjectInputStream ois;
				fis = new FileInputStream(f);
				ois = new ObjectInputStream(fis);
				Genetic knowledge = (Genetic) ois.readObject(); //remove object from file when read?
				
				//Initialize values from file read
				this.lastPop = knowledge.lastPop;
				this.pop = knowledge.pop;
				this.nextCand = knowledge.nextCand;
				this.generation = knowledge.generation;
				ois.close();
				fis.close();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				System.out.println("Error occurred reading in Genetic file... starting from scratch.");
				//error occurred, start from new knowledge..?
				this.lastPop = null;
				this.pop = new ArrayList<Genome>();
				this.nextCand = 0;
				this.generation = 0;
			}
		} else { //learning has not occurred before
			System.out.println("LEARNING HAS NOT OCCURRED BEFORE! :(");
			this.lastPop = null;
			this.pop = new ArrayList<Genome>();
			this.nextCand = 0;
			this.generation = 0;
		}
	}
	
	/** Static 'instance' method to get singleton */
	public static Genetic getInstance( ) {
	   return singleton;
	}

	/*
		Performs uniform crossover on two genomes
	*/
	public void cross(Genome a, Genome b){
		float temp;
		for (int i =0; i < a.getGenes().length; i++){
			if (rand.nextFloat() < .5){
				temp = a.getGenes()[i];
				a.getGenes()[i] = b.getGenes()[i];
				b.getGenes()[i] = temp;
			}
		}
	}

	/*
		Performs uniform mutation on Genome
	*/
	public void mutate(Genome gen){
		float[] genes = gen.getGenes();

		for (int i = 0; i< genes.length; i++){
			if (rand.nextFloat() < this.MUT_RATE){
				genes[i] += this.normal();
				genes[i] = (genes[i] > 1)? 1 : genes[i];
				genes[i] = (genes[i] < 0)? 0 : genes[i];
			}
		}
	}

	public Genome tourn(){
		Genome winner = null;
		Genome contestant;
		float best = Float.NEGATIVE_INFINITY;

		for (int i = 0; i< this.TOURN_SIZE; i++){
			contestant = this.pop.get(rand.nextInt(this.pop.size()));
			if (contestant.getFitness() > best){
				winner = contestant;
				best = contestant.getFitness();
			}
		}
		
		return new Genome(winner.getGenes());
	}

	public void evolve(){
		//perform selection and mutation for next generation
		ArrayList<Genome> selection = new ArrayList<Genome>();
		ArrayList<Genome> nextGen = new ArrayList<Genome>();

		for (int i =0; i < this.pop.size(); i++){
			selection.add(this.tourn());
		}

		Collections.sort(this.pop, (a, b) -> (int)(b.getFitness() - a.getFitness()));
		Collections.sort(selection, (a, b) -> (int)(b.getFitness() - a.getFitness()));		//sort selection in descending order of fitness

		for (int i = 0; i < this.ELITE_CLONES; i++){	//clone best genomes to next generation without change (resetting fitness)
			if (i < this.pop.size())
				nextGen.add(new Genome(this.pop.get(i).getGenes()));
		}

		Genome a;
		Genome b;

		while(nextGen.size() < this.pop.size()){		//make next generation at least as large
			a = selection.remove(0);
			if (selection.size() > 0){ b = selection.remove(0);}
			else {b = new Genome(this.pop.get(0).getGenes());}		//If out of selected Genomes, clone the best one

			this.cross(a, b);

			this.mutate(a);
			this.mutate(b);

			nextGen.add(a);
			nextGen.add(b);
		}

		this.lastPop = this.pop;
		this.pop = nextGen;

		this.nextCand = 0;
		this.generation++;
	}

	public float normal(){
		return (float)rand.nextGaussian()*this.MUT_VAR;
	}

	public Genome getNextCandidate(){
		if (this.pop.size() <= this.nextCand){
			this.pop.add(new Genome());
		}

		Genome cand = this.pop.get(this.nextCand);
		this.nextCand++;

		return cand;
	}

	public ArrayList<Genome> getPop(){
		return this.pop;
	}

	public int generation(){
		return this.generation;
	}
}