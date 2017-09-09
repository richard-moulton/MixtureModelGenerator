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
		// Initialize Mixture Model Variables and set seeds for random number generators
		this.models = numClasses;
		this.dimensions = numAttributes;
		weights = new double[models];
		modelArray = new MultivariateNormalDistribution[models];
		modelRandom.setSeed(modelRandomSeed);
		instanceRandom.setSeed(instanceRandomSeed);
		
		double weightSum = 0.0;
		double[] means = new double[dimensions];
		double[][] covariances = new double[dimensions][dimensions];
		
		// initialize arrays
		for(int i = 0 ; i < models ; i++)
		{
			weights[i] = modelRandom.nextDouble();
			weightSum += weights[i];
			
			// Generate "centroid" and covariance matrix for the Multivariate Normal Distribution
			for(int j = 0 ; j < dimensions ; j++)
			{
				means[j] = modelRandom.nextDouble();
				for(int k = 0 ; k < dimensions ; k++)
				{
					covariances[j][k] = modelRandom.nextDouble();
				}
			}
			
			modelArray[i] = new MultivariateNormalDistribution(means, covariances);
		}
		
		// Normalize weights array
		for(int i = 0 ; i < models ; i++)
		{
			weights[i] = weights[i]/weightSum;
		}
	}

	public InstanceExample nextInstance()
	{
		int index = instanceRandom.nextInt(models-1);
		double[] point = modelArray[index].sample();
		double[] instance = new double[dimensions+1];
		
		// Add the class label to the sampled point as the last attribute
		for(int i = 0 ; i < dimensions ; i++)
		{
			instance[i] = point[i];
		}
		
		instance[dimensions] = (double)index;
		
		return new InstanceExample(new DenseInstance(1.0, instance));
	}

}
