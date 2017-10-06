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
		weights = new double[numModels];
		modelArray = new MultivariateNormalDistribution[numModels];
		range = (double)this.numModels;
		
		// Initialize random number generators
		modelRandom = new Random();
		modelRandom.setSeed(modelRandomSeed);
		instanceRandom = new Random();
		instanceRandom.setSeed(instanceRandomSeed);
		
		double weightSum = 0.0;
		double[] means = new double[dimensions];
		
		// initialize arrays
		for(int i = 0 ; i < numModels ; i++)
		{
			weights[i] = modelRandom.nextDouble();
			weightSum += weights[i];
			
			// Generate "centroids" for the Multivariate Normal Distribution
			for(int j = 0 ; j < this.dimensions ; j++)
			{
				means[j] = (modelRandom.nextDouble()*range)-(range/2.0);
			}
			
			double[][] covariances = generateCovariance(this.dimensions);		
			
			modelArray[i] = new MultivariateNormalDistribution(means, covariances);
		}
		
		// Normalize weights array
		for(int i = 0 ; i < numModels ; i++)
		{
			weights[i] = weights[i]/weightSum;
		}
		
		//System.out.println(this.toString());
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
	 * Restarts the mixture model by reinitializing the pseudo random number generators' seeds.
	 * @param instanceRandomSeed the seed for the instances' pseudo random number generator.
	 * @param modelRandomSeed the see for the models' pseudo random number generator.
	 */
	public void restart(int instanceRandomSeed, int modelRandomSeed)
	{
		this.instanceRandom.setSeed(instanceRandomSeed);
		this.modelRandom.setSeed(modelRandomSeed);
	}

	/**
	 * Calculates the density of the mixture model at the argument point.
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
	 * @param i the index of the model
	 * @return the weight of the ith model
	 */
	public double getWeight(int i)
	{
		return this.weights[i];
	}
	
	/**
	 * 
	 * @return the weights of the mixture model
	 */
	public double[] getWeights()
	{
		return this.weights;
	}
	
	/**
	 * 
	 * @param i the index of the model
	 * @return the means of the ith model
	 */
	public double[] getMeans(int i)
	{
		return this.modelArray[i].getMeans();
	}
	
	public double[][] getCovariance(int i)
	{
		return this.modelArray[i].getCovariances().getData();
	}
	
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
	
	private double[][] generateCovariance(int d)
	{
		double[][] x = new double[d][d];
		double[][] covariances = new double[d][d];
		double matrixSum = 0.0;

		for(int j = 0 ; j < d ; j++)
		{
			for(int k = 0 ; k < d ; k++)
			{
				x[j][k] = (((modelRandom.nextDouble()*2.0)-1.0)+((modelRandom.nextDouble()*2.0)-1.0))/2.0;
			}
		}

		for(int j = 0 ; j < d ; j++)
		{
			for(int k = 0 ; k <= j ; k++)
			{
				for(int l = 0 ; l < d ; l++)
				{
					matrixSum += x[j][l]*x[k][l];
				}

				covariances[j][k] = matrixSum;
				covariances[k][j] = matrixSum;
				matrixSum = 0.0;
			}
		}

		return covariances;
	}
	
}
