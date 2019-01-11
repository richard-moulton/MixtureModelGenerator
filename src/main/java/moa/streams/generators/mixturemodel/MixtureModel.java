/*
 *    MixedModel.java
 *    
 *    Copyright 2017 Richard Hugh Moulton
 *   
 *    @author Richard Hugh Moulton
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.streams.generators.mixturemodel;

import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.core.InstanceExample;
import moa.core.MiscUtils;

import java.util.Random;

import org.apache.commons.math3.distribution.*;
import org.apache.commons.math3.linear.*;

/**
 * Makes use of the Apache Commons Math 3 package to represent a mixture model made up of individual multivariate distributions.
 * Currently limited to using multivariate normal distributions.
 * 
 * @see org.apache.commons.math3.distribution
 * 
 * @author Richard Hugh Moulton
 */
public class MixtureModel
{	
	private int numModels, dimensions;
	private double[] weights;
	private MultivariateNormalDistribution[] modelArray;
	private double[][][] lArray;
	private Random modelRandom;
	private Random instanceRandom;
	private double range;
	
	/**
	 * Constructor method for a new MixtureModel that uses basic parameters.
	 * 
	 * @param numClasses the number of classes/number of models to include in the mixture model.
	 * @param numAttributes the dimensionality if the distributions.
	 * @param instanceRandomSeed the seed for the instances' pseudo random number generator.
	 * @param modelRandomSeed the see for the models' pseudo random number generator.
	 */
	public MixtureModel(int numClasses, int numAttributes, int instanceRandomSeed, int modelRandomSeed)
	{
		// Initialize Mixture Model Variables
		this.numModels = numClasses;
		this.dimensions = numAttributes;
		this.weights = new double[this.numModels];
		this.modelArray = new MultivariateNormalDistribution[this.numModels];
		this.lArray = new double[this.numModels][this.dimensions][this.dimensions];
		this.range = (double) this.numModels;
		
		// Initialize random number generators
		this.modelRandom = new Random();
		this.modelRandom.setSeed(modelRandomSeed);
		this.instanceRandom = new Random();
		this.instanceRandom.setSeed(instanceRandomSeed);
		
		double weightSum = 0.0;
		double[] means = new double[this.dimensions];
		
		// initialize arrays
		for(int i = 0 ; i < this.numModels ; i++)
		{
			this.weights[i] = this.modelRandom.nextDouble();
			weightSum += this.weights[i];
			
			// Generate "centroids" for the Multivariate Normal Distribution
			for(int j = 0 ; j < this.dimensions ; j++)
			{
				means[j] = (this.modelRandom.nextDouble()*range)-(range/2.0);
			}
			
			this.lArray[i] = this.generateL();
			
			this.modelArray[i] = new MultivariateNormalDistribution(means, generateCovariance(this.lArray[i]));
		}
		
		// Normalize weights array
		for(int i = 0 ; i < this.numModels ; i++)
		{
			this.weights[i] = this.weights[i]/weightSum;
		}
		
		//System.out.println(this.toString());
	}

	/**
	 * Sets a given probability in the model weight probability vector to a specified weight.
	 * 
	 * @param index the index of the model whose weight to set
	 * @param weight the weight to set the chosen model's probability to
	 */
	public void setWeight(int index, double weight)
	{
		double weightSum = 0.0;
		
		// Ensure that "weight" is a valid probability (between 0 and 1)
		if(weight > 1.0)
		{
			System.out.println("Invalid weight for a probability vector; greater than 1.0!");
			weight = 1.0;
		}
		else if (weight < 0.0)
		{
			System.out.println("Invalid weight for a probability vector; less than 0.0!");
			weight = 0.0;
		}
		
		// Sum the probabilities for the non setting class...
		for(int i = 0 ; i < this.numModels ; i++)
		{
			if (i != index)
				weightSum += this.weights[i];
		}
		
		// ...and scale them appropriately. Set the index probability to the argument weight.
		for(int i = 0 ; i < this.numModels ; i++)
		{
			if (i == index)
				this.weights[i] = weight;
			else
				this.weights[i] = (this.weights[i]*(1.0-weight))/weightSum;
		}
	}
	
	/**
	 * Sets the model weights so that, in a one-class classification scenario, the majority classes'
	 * weights sum to the argument weight.
	 * 
	 * @param numMajClasses the number of majority classes
	 * @param weight the total of the majority classes' weights
	 */
	public void setWeights(int numMajClasses, double majWeight)
	{
		double weightSum = 0.0;
		double minWeight = 1.0 - majWeight;
		
		// MAJORITY CLASSES
		for(int i = 0 ; i < numMajClasses ; i++)
		{
			weights[i] = modelRandom.nextDouble();
			weightSum += weights[i];
		}
		
		// Normalize weights array
		for(int i = 0 ; i < numMajClasses ; i++)
		{
			weights[i] = (majWeight*weights[i])/weightSum;
		}
		
		// MINORITY CLASSES
		weightSum = 0.0;
		
		for(int i = numMajClasses ; i < this.numModels ; i++)
		{
			weights[i] = modelRandom.nextDouble();
			weightSum += weights[i];
		}
		
		// Normalize weights array
		for(int i = numMajClasses ; i < this.numModels ; i++)
		{
			weights[i] = (minWeight*weights[i])/weightSum;
		}
		
	}
	
	
	/**
	 * Generates the next instance in the data stream by selecting a model (via the weights array) and then sampling that model.
	 * 
	 * @see moa.streams.generators.mixturemodel.MixtureModel#weights
	 * 
	 * @param instHead the header for instances in the data stream
	 * @return the next instance in the data stream
	 */
	public InstanceExample nextInstance(InstancesHeader instHeader)
	{
		int index = MiscUtils.chooseRandomIndexBasedOnWeights(this.weights,
                this.instanceRandom);
		//System.out.println("MMnI: index "+index+" is chosen.\n"+modelArray[index].toString());
		double[] point = modelArray[index].sample();
		double[] attVals = new double[dimensions+1];
		
		// Add the class label to the sampled point as the last attribute
		//System.out.println("Instance:");
		for(int i = 0 ; i < dimensions ; i++)
		{
			attVals[i] = point[i];
			//System.out.print(attVals[i]+" ");
		}
		
		Instance inst = new DenseInstance(1.0, attVals);
        inst.setDataset(instHeader);
        inst.setClassValue(index);
        return new InstanceExample(inst);
	}
	
	/**
	 * Calculates the concept assignment map. Each majority class is its own concept, each minority class is assigned to its closest concept.
	 * @param numMajClasses the number of majority classes
	 * 
	 * @return an array mapping classes to concepts
	 */
	public double[] getConceptAssignments(int numMajClasses)
	{
		double[] conceptAssignments = new double[this.numModels];
		
		//System.out.print("ConceptAssignments:");
		for(int i = 0 ; i < numMajClasses ; i++)
		{
			conceptAssignments[i] = i;
			//System.out.print(" "+i+":"+conceptAssignments[i]+":0 /");
		}
		
		for(int i = numMajClasses ; i < this.numModels ; i++)
		{
			conceptAssignments[i] = getIndexOfClosestMajorityClass(i, numMajClasses);
			//System.out.print(" "+i+":"+conceptAssignments[i]+":1 /");
		}
		
		//System.out.println();
		
		return conceptAssignments;
	}
	
	/**
	 * Determines to which majority class/concept the argument minority class is closest.
	 * 
	 * @param index the index of the minority class
	 * @param numMajClasses the number of majority classes
	 * 
	 * @return the index of the majority class to which the minority class is closest
	 */
	private double getIndexOfClosestMajorityClass(int index, int numMajClasses)
	{
		double concept = -1;
		double minDistance = Double.MAX_VALUE;
		double currDistance;
		
		for(int i = 0 ; i < numMajClasses ; i++)
		{
			currDistance = hellingerDistance(index, i);
			
			if(currDistance < minDistance)
			{
				minDistance = currDistance;
				concept = i;
			}
		}		
		
		return concept;
	}
	
	/**
	 * Calculates the Hellinger distance between two multivariate normal distributions.
	 * 
	 * @param modelA the first MVND
	 * @param modelB the second MVND
	 * @return the Hellinger distance bewteen modelA and modelB
	 */
	private double hellingerDistance(int modelA, int modelB)
	{
		Array2DRowRealMatrix meansA = new Array2DRowRealMatrix(getMeans(modelA));
		Array2DRowRealMatrix covarianceA = new Array2DRowRealMatrix(getCovariance(modelA));
		Array2DRowRealMatrix meansB = new Array2DRowRealMatrix(getMeans(modelB));
		Array2DRowRealMatrix covarianceB = new Array2DRowRealMatrix(getCovariance(modelB));
		
		Array2DRowRealMatrix covarianceCombined = (Array2DRowRealMatrix)(covarianceA.add(covarianceB)).scalarMultiply(0.5);
		Array2DRowRealMatrix covarianceCombinedInverse = (Array2DRowRealMatrix)new LUDecomposition(covarianceCombined).getSolver().getInverse();
		Array2DRowRealMatrix meansDifference = (Array2DRowRealMatrix)(meansA.subtract(meansB));
		
		double detA = new LUDecomposition(covarianceA).getDeterminant();
		double detB = new LUDecomposition(covarianceB).getDeterminant();
		double detCmbnd = new LUDecomposition(covarianceCombined).getDeterminant();

		
		double partA = (Math.pow(detA, 0.25)*Math.pow(detB, 0.25))/Math.pow(detCmbnd, 0.5);
		Array2DRowRealMatrix partBMatrix = (Array2DRowRealMatrix)((meansDifference.transpose()).multiply(covarianceCombinedInverse)).multiply(meansDifference);
		double partB = Math.exp(-0.125 * partBMatrix.getEntry(0, 0));
		
		double distance = 1.0 - (partA*partB);

		return Math.sqrt(distance);
	}
	
	/**
	 * Restarts the mixture model by reinitializing the pseudo random number generators' seeds.
	 * 
	 * @param instanceRandomSeed the seed for the instances' pseudo random number generator.
	 * @param modelRandomSeed the see for the models' pseudo random number generator.
	 */
	public void restart(int instanceRandomSeed, int modelRandomSeed)
	{
		this.instanceRandom.setSeed(instanceRandomSeed);
		this.modelRandom.setSeed(modelRandomSeed);
	}

	/**
	 * Calculates the density of the mixture model at the argument point. This is done
	 * via a weighted sum of the density of each multivariate normal distribution in the
	 * mixture model  evaluated at the argument point.
	 * 
	 * @param point the point at which to calculate the mixture model's density.
	 * @return the mixture model's density at the argument point.
	 */
	public double densityAt(double[] point)
	{
		double density = 0.0;
		
		for(int i = 0 ; i < this.numModels ; i++)
		{
			density += weights[i]*modelArray[i].density(point);
		}
		
		return density;
	}
	
	/**
	 * Generates L, a lower triangular matrix with all real entries and with non-negative
	 * entries on the diagonal. This matrix will be used as the Cholesky decomposition of
	 * a covariance matrix (which must itself be positive semi-definite).
	 * 
	 * @return L, lower triangular matrix with non-negative entries on the diagonal.
	 */
	private double[][] generateL()
	{
		double[][] l = new double[this.dimensions][this.dimensions];
		
		for(int j = 0 ; j < this.dimensions ; j++)
		{
			for(int k = 0 ; k < j ; k++)
			{
				l[j][k] = (modelRandom.nextDouble()*2.0)-1.0;
			}
			
			l[j][j] = modelRandom.nextDouble();
		}

		return l;
	}
	
	/**
	 * Using the argument matrix l* as a Cholesky decomposition, this method generates
	 * and returns a covariance matrix (which must be positive semi-definite).
	 * 
	 * * - the argument matrix l must be a lower triangular matrix with all real entries
	 * and with non-negative entries on the diagonal. Such a matrix is generated by the
	 * generateL() method in this class.
	 * 
	 * @param l the Cholesky decomposition for the eventual covariance matrix.
	 * @return the covariance matrix recovered from its Cholesky decomposition, l.
	 */
	private double[][] generateCovariance(double[][] l)
	{
		double[][] covariances = new double[this.dimensions][this.dimensions];
		double matrixSum;

		for(int j = 0 ; j < this.dimensions ; j++)
		{
			for(int k = 0 ; k <= j ; k++)
			{
				matrixSum = 0.0;

				for(int m = 0 ; m < this.dimensions ; m++)
				{
					matrixSum += l[j][m]*l[k][m];
				}

				covariances[j][k] = matrixSum;
				covariances[k][j] = matrixSum;
			}
		}
		
		return covariances;
	}

	/**
	 * Adjust this mixture model towards or away from the argument mixture model
	 * as dictated by the argument miss distance.
	 * 
	 * @param targetMM the MixtureModel towards/away from which to adjust this MixtureModel
	 * @param distMiss the required correction between the respective MixtureModels
	 */
	public void adjustMixtureModel(MixtureModel targetMM, double distMiss)
	{
		// Adjust the weights
		for(int i = 0 ; i < this.getNumModels() ; i++)
		{
			double weightMiss = targetMM.getWeight((i%targetMM.getDimensions())) - this.weights[i];
			this.weights[i] = this.weights[i] + (weightMiss*distMiss) + (this.modelRandom.nextDouble()/100.0);
		}
		
		// Adjust the MVNDs
		int numTargetModels = targetMM.getNumModels();
		for(int i = 0 ; i < this.getNumModels() ; i++)
		{
			MultivariateNormalDistribution mvndi = this.modelArray[i];
			double[] oldMeans = mvndi.getMeans();
			double[][] oldX = this.getL(i);
			
			double[] targetMeans = targetMM.getMeans(i%numTargetModels);
			double[][] targetX = targetMM.getL(i%numTargetModels);
			
			double[] newMeans = new double[this.dimensions];
			double[][] newX = new double[this.dimensions][this.dimensions];
			
			for(int j = 0 ; j < this.dimensions ; j++)
			{
				// Update the means
				double meanDist = targetMeans[j] - oldMeans[j];
				newMeans[j] = oldMeans[j] + (meanDist*distMiss) + (this.modelRandom.nextDouble()/100.0);
				
				// Update the X matrix
				for(int k = 0 ; k < this.dimensions ; k++)
				{
					double xMiss = targetX[j][k] - oldX[j][k];
					newX[j][k] = oldX[j][k] + (xMiss*distMiss) + (this.modelRandom.nextDouble()/100.0);
				}				
			}
			
			this.lArray[i] = newX;

			MultivariateNormalDistribution mvndNew = new MultivariateNormalDistribution(newMeans, this.generateCovariance(newX));
			this.modelArray[i] = mvndNew;
		}
	}	
	
	/**
	 * @return the number of dimensions for the multivariate distribution
	 */
	public int getDimensions()
	{
		return dimensions;
	}


	/**
	 * @return the number of models for the multivariate distribution
	 */
	public int getNumModels()
	{
		return numModels;
	}
	
	/**
	 * Returns the weight of the ith multivariate normal distribution (MVND)
	 * in the mixture model.
	 * 
	 * @param i the index of the MVND
	 * @return the weight of the ith MVND
	 */
	public double getWeight(int i)
	{
		return this.weights[i];
	}
	
	/**
	 * Returns the vector of weights for the mixture model.
	 * 
	 * @return the weights of the mixture model
	 */
	public double[] getWeights()
	{
		return this.weights;
	}
	
	/**
	 * Returns the means of the ith multivariate normal distribution (MVND)
	 * in the mixture model.
	 * 
	 * @param i the index of the MVND
	 * @return the means of the ith MVND
	 */
	public double[] getMeans(int i)
	{
		return this.modelArray[i].getMeans();
	}
	
	/**
	 * Returns the covariance matrix belonging to the ith multivariate normal
	 * distribution (MVND).
	 * 
	 * @param i the index of the MVND
	 * @return the covariance matrix of the ith MVND
	 */
	public double[][] getCovariance(int i)
	{
		return this.modelArray[i].getCovariances().getData();
	}
	
	/**
	 * Returns the Cholesky decomposition of the covariance matrix belonging
	 * to the ith multivariate normal distribution (MVND).
	 * 
	 * @param i the index of the MVND
	 * @return the Cholesky decomposition of the ith MVND's covariance matrix
	 */
	public double[][] getL(int i)
	{
		return this.lArray[i];
	}
	
	/**
	 * Constructs and returns a String representation of the MixtureModel object.
	 * This includes the means and covariance matrix for each of the multivariate
	 * normal distributions (MVNDs) as well as the weight vector weighting each
	 * of the MVNDs in the mixture model.
	 * 
	 * @return a String representation of the MixtureModel object
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0 ; i < this.numModels ; i++)
		{
			double[] means = this.getMeans(i);
			double[][] covariance = this.getCovariance(i);
			sb.append("Mean:\n");
			for(int j = 0 ; j < this.dimensions ; j++)
			{
				sb.append(means[j]+" ");
			}
			sb.append("\n");
			
			sb.append("\nCovariance:\n");
			for(int j = 0 ; j < this.dimensions ; j++)
			{
				for(int k = 0 ; k < this.dimensions ; k++)
				{
					sb.append(covariance[j][k]+" ");
				}
				sb.append("\n");
			}
			sb.append("\n");
		}
		
		sb.append("Weights:\n");
		for(int i = 0 ; i < this.numModels ; i++)
		{
			sb.append(this.weights[i]+" ");
		}
		sb.append("\n");
		
		return sb.toString();
	}
}
