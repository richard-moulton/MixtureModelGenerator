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

import java.util.Random;

import org.apache.commons.math3.distribution.*;

public class MixtureModel
{
	private int models, dimensions;
	private double[] weights;
	private MultivariateNormalDistribution[] modelArray;
	private Random modelRandom;
	private Random instanceRandom;
	
	public MixtureModel(int numClasses, int numAttributes, int instanceRandomSeed, int modelRandomSeed)
	{
		// Initialize Mixture Model Variables
		this.models = numClasses;
		this.dimensions = numAttributes;
		weights = new double[models];
		modelArray = new MultivariateNormalDistribution[models];
		
		// Initialize random number generators
		modelRandom = new Random();
		modelRandom.setSeed(modelRandomSeed);
		instanceRandom = new Random();
		instanceRandom.setSeed(instanceRandomSeed);
		
		double weightSum = 0.0;
		double[] means = new double[dimensions];
		double[][] x = new double[dimensions][dimensions];
		double[][] covariances = new double[dimensions][dimensions];
		double matrixSum = 0.0;
		
		// initialize arrays
		for(int i = 0 ; i < models ; i++)
		{
			weights[i] = modelRandom.nextDouble();
			weightSum += weights[i];
			
			// Generate "centroid" and covariance matrix for the Multivariate Normal Distribution
			System.out.println("\nMeans:");
			for(int j = 0 ; j < dimensions ; j++)
			{
				means[j] = modelRandom.nextDouble();
				System.out.print(means[j]+" ");
				for(int k = 0 ; k < dimensions ; k++)
				{
					x[j][k] = modelRandom.nextDouble();
				}
			}
			
			System.out.println("\nCovariance matrix:");
			for(int j = 0 ; j < dimensions ; j++)
			{
				for(int k = 0 ; k < dimensions ; k++)
				{
					for(int l = 0 ; l < dimensions ; l++)
					{
						matrixSum += x[j][l]*x[k][l];
					}
					
					covariances[j][k] = matrixSum;
					matrixSum = 0.0;
					System.out.print(covariances[j][k]+" ");
				}
				System.out.println();
			}
			
			modelArray[i] = new MultivariateNormalDistribution(means, covariances);
		}
		
		// Normalize weights array
		System.out.println("\nWeights array:");
		for(int i = 0 ; i < models ; i++)
		{
			weights[i] = weights[i]/weightSum;
			System.out.print(weights[i]+" ");
		}
	}

	public InstanceExample nextInstance(InstancesHeader instHead)
	{
		int index = instanceRandom.nextInt(models);
		System.out.println("MMnI: index "+index+" is chosen.");
		double[] point = modelArray[index].sample();
		double[] attVals = new double[dimensions+1];
		
		// Add the class label to the sampled point as the last attribute
		System.out.println("Instance:");
		for(int i = 0 ; i < dimensions ; i++)
		{
			attVals[i] = point[i];
			System.out.print(attVals[i]+" ");
		}
		
		Instance inst = new DenseInstance(1.0, attVals);
        inst.setDataset(instHead);
        inst.setClassValue(index);
        return new InstanceExample(inst);
	}

}
