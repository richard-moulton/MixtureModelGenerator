/*
 *    MixtureModelGeneratorDrift.java
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

import com.github.javacliparser.IntOption;

import java.util.Random;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.core.Example;
import moa.core.FastVector;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;

/**
 * Generates a data stream with concept drift. The concepts before and
 * after the drift period are each based on an underlying mixture model.
 * 
 * @author Richard Hugh Moulton
 */
public class MixtureModelGeneratorDrift extends AbstractOptionHandler implements InstanceStream
{

	private static final long serialVersionUID = 1L;

	public IntOption numAttsOption = new IntOption("numAtts", 'a',
			"The number of attributes to generate.", 10, 0, Integer.MAX_VALUE);

	//public IntOption distributionOption = new IntOption("distribution", 'z',
	//      "Distribution used for models. Uniform 0, Gaussian 1, etc.",
	//      1, 0, 4);

	public IntOption numClassesPreOption = new IntOption("numClassesPre", 'p',
			"The number of classes in the data stream and the number of models to include in the mixture model pre-concept drift.",
			2, 2, Integer.MAX_VALUE);

	public IntOption burnInInstances = new IntOption("burnInInstances", 'b',
			"The number of instances to draw from the pre-concept drift mixture model.", 10000, 1, Integer.MAX_VALUE);

	public IntOption driftDuration = new IntOption("driftDuration", 'd',
			"The number of instances between the stable pre-drift mixture model and the stable post-drift mixture model.",
			0, 0, Integer.MAX_VALUE);

	public FloatOption driftMagnitude = new FloatOption("driftMagnitude",
			'D', "Magnitude of the drift between the starting probability and the one after the drift."
					+ " Magnitude is expressed as the Hellinger distance [0,1]", 0.5, 1e-20, 0.9);

	public FloatOption precisionDriftMagnitude = new FloatOption("epsilon", 'e',
			"Precision of the drift magnitude for p(x) (how far from the set magnitude is acceptable)",
			0.01, 1e-20, 1.0);

	public IntOption numClassesPostOption = new IntOption("numClassesPost", 'P',
			"The number of classes in the data stream and the number of models to include in the mixture model post-concept drift.",
			2, 2, Integer.MAX_VALUE);

	public IntOption modelRandomSeedOption = new IntOption("modelRandomSeed",
			'm', "Seed for random generation of model.", 1);

	public IntOption instanceRandomSeedOption = new IntOption(
			"instanceRandomSeed", 'i',
			"Seed for random generation of instances.", 1);

	protected InstancesHeader streamHeader;
	protected MixtureModel mixtureModelPre, mixtureModelPost;
	protected int numInstances, lastInstancePre, firstInstancePost;
	protected Random monteCarloRandom;
	protected double integrateRange;

	/**
	 * @see moa.options.AbstractOptionHandler#prepareForUseImpl(moa.tasks.TaskMonitor, moa.core.ObjectRepository)
	 * @see moa.streams.generators.mixturemodel.MixtureModel
	 */
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository)
	{
		System.out.println("Initialized...");
		this.numInstances = 0;
		this.lastInstancePre = this.burnInInstances.getValue();
		this.firstInstancePost = lastInstancePre+this.driftDuration.getValue()+1;
		this.monteCarloRandom = new Random();
		this.integrateRange = Math.max(this.numClassesPreOption.getValue(),this.numClassesPreOption.getValue())+4.0;
		int y = 0;

		generateHeader(this.numClassesPreOption.getValue());

		double hDist; //= hellingerDistance(this.mixtureModelPre, this.mixtureModelPost, this.driftMagnitude.getValue());
		double distMiss; //= hDist - this.driftMagnitude.getValue();
		do
		{
			// Initialize pre-concept drift mixture model
			this.mixtureModelPre = new MixtureModel(this.numClassesPreOption.getValue(), this.numAttsOption.getValue(),
					this.instanceRandomSeedOption.getValue()+y, this.modelRandomSeedOption.getValue()+y++);
			int z = y;

			//System.out.println("PRE:\n"+this.mixtureModelPre.toString());

			// Initialize post-concept drift mixture model
			//this.mixtureModelPost = new MixtureModel(this.mixtureModelPre, this.numClassesPostOption.getValue(), this.driftMagnitude.getValue(),
			//this.instanceRandomSeedOption.getValue()+1, this.modelRandomSeedOption.getValue()+1);
			//this.mixtureModelPost = new MixtureModel(this.numClassesPostOption.getValue(), this.numAttsOption.getValue(),
			//		this.instanceRandomSeedOption.getValue()+z++, this.modelRandomSeedOption.getValue()+z++, 
			//		this.mixtureModelPre, this.driftMagnitude.getValue());

			do
			{
				this.mixtureModelPost = new MixtureModel(this.numClassesPostOption.getValue(), this.numAttsOption.getValue(),
						this.instanceRandomSeedOption.getValue()+z, this.modelRandomSeedOption.getValue()+z++, 
						this.mixtureModelPre, this.driftMagnitude.getValue());
				hDist = hellingerDistance(this.mixtureModelPre, this.mixtureModelPost, this.driftMagnitude.getValue());
				distMiss = hDist - this.driftMagnitude.getValue();
				System.out.println((y-1)+"."+(z-1)+": The Hellinger distance was evaluated as "+hDist+", compared against the desired range "+this.driftMagnitude.getValue()+
						" +/- "+this.precisionDriftMagnitude.getValue());
			}while((z <= 100) && (Math.abs(distMiss) > this.precisionDriftMagnitude.getValue()));

			//System.out.println("POST:\n"+this.mixtureModelPost.toString());
		}while(Math.abs(distMiss) > this.precisionDriftMagnitude.getValue());	

		//System.out.println("The Hellinger distance was evaluated as "+hDist+", compared against the desired range "+this.driftMagnitude.getValue()+
		//		" +/- "+this.precisionDriftMagnitude.getValue());
		//System.out.println("The distance was off by "+distMiss);
	}

	/**
	 * @return the next instance in the data stream by calling the MixtureModel object's nextInstance method.
	 * 
	 * @see moa.streams.ExampleStream#nextInstance()
	 * @see moa.streams.generators.mixturemodel.MixtureModel#nextInstance(InstancesHeader)
	 */
	@Override
	public Example<Instance> nextInstance()
	{
		this.numInstances++;

		// Post concept drift model
		if(this.numInstances > firstInstancePost)
		{			
			return this.mixtureModelPost.nextInstance(this.getHeader());
		}
		if (this.numInstances == firstInstancePost)
		{
			generateHeader(this.numClassesPostOption.getValue());
			return this.mixtureModelPost.nextInstance(this.getHeader());
		}
		// Pre concept drift model
		else if (this.numInstances <= lastInstancePre)
		{
			return this.mixtureModelPre.nextInstance(this.getHeader());
		}
		// During concept drift mix of models
		else
		{

			if(this.numInstances == (lastInstancePre+1) &&
					this.numClassesPostOption.getValue() > this.numClassesPreOption.getValue())
			{
				generateHeader(this.numClassesPostOption.getValue());
			}

			if (this.monteCarloRandom.nextDouble() < ((double)this.numInstances-(double)this.burnInInstances.getValue())/(double)this.driftDuration.getValue())
				return this.mixtureModelPost.nextInstance(this.getHeader());
			else
				return this.mixtureModelPre.nextInstance(this.getHeader());
		}

	}

	/**
	 * Generates the stream's header.
	 */
	private void generateHeader(int numClasses)
	{
		FastVector<Attribute> attributes = new FastVector<Attribute>();
		for (int i = 0; i < this.numAttsOption.getValue(); i++) {
			attributes.addElement(new Attribute("att" + (i + 1)));
		}
		FastVector<String> classLabels = new FastVector<String>();
		for (int i = 0; i < numClasses; i++) {
			classLabels.addElement("class" + (i + 1));
		}
		attributes.addElement(new Attribute("class", classLabels));
		this.streamHeader = new InstancesHeader(new Instances(
				getCLICreationString(InstanceStream.class), attributes, 0));
		this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);
	}

	/**
	 * @return the stream's header.
	 * @see moa.streams.ExampleStream#getHeader()
	 */
	@Override
	public InstancesHeader getHeader()
	{
		return this.streamHeader;
	}

	/**
	 * Uses Monte Carlo integration to calculate the Hellinger distance between the two argument mixture models.
	 * 
	 * @param mm1 the first mixture model
	 * @param mm2 the second mixture model
	 * @return the Hellinger distance between mm1 and mm2
	 */
	private double hellingerDistance(MixtureModel mm1, MixtureModel mm2, double targetDist)
	{
		//System.out.println("Monte Carlo Integration:");

		double monteCarlo = 0.0;
		double runningSum = 0.0;
		double hellingerDistance = -1.0;

		double volume = Math.pow(this.integrateRange,(double)this.numAttsOption.getValue());
		double error = Double.MAX_VALUE;
		double N = 0.0;
		double sampleVar = 0.0;
		double mean = 0.0;
		double M2 = 0.0;
		double delta1 = 0.0;
		double delta2 = 0.0;
		double x = 0.0;
		double[] point = new double[this.numAttsOption.getValue()];
		this.monteCarloRandom.setSeed(this.instanceRandomSeedOption.getValue()+this.modelRandomSeedOption.getValue());


		// Monte Carlo integration
		while(error > 0.001)
		{
			// Randomly generate the point at which to evaluate the function
			for(int i = 0 ; i < this.numAttsOption.getValue() ; i++)
			{
				point[i] = (this.monteCarloRandom.nextDouble()*this.integrateRange) - (this.integrateRange/2.0);
				//System.out.print(point[i]+" ");
			}

			// Evaluate the function at point and add the result to the running sum
			x = Math.sqrt(mm1.densityAt(point)*mm2.densityAt(point));
			runningSum += x;
			N++;

			// Adjust other values of interest
			delta1 = x - mean;
			mean += delta1/N;
			delta2 = x - mean;
			M2 += delta1*delta2;
			monteCarlo = volume*runningSum/N;

			// Once a sufficient base of samples has been built, calculate the sample variance and estimate the error
			if (N > 1000000)
			{
				sampleVar = M2/(N-1);
				error = volume*Math.sqrt(sampleVar)/Math.sqrt(N);
				hellingerDistance = Math.sqrt(1.0 - monteCarlo);

				// If the target distance is no longer within the error margin around the estimated distance
				// then break from the WHILE loop
				if(Math.abs(targetDist - hellingerDistance) > (Math.sqrt(error)))
				{
					break;
				}

				//if (N % 1000000 == 0)
				//{
				//System.out.println("N: "+N+", monteCarlo: "+monteCarlo+", 1.0 - monteCarlo: "+(1.0-monteCarlo)+", and error: "+error);
				//}
			}
		}

		//System.out.println("N: "+N+", monteCarlo: "+monteCarlo+", 1.0 - monteCarlo: "+(1.0-monteCarlo)+", and error: "+error);
		//System.out.println("Hellinger distance is estimated as ("+hellingerDistance+" +/- "+Math.sqrt(error)+"); (target distance was "+targetDist+")");
		return hellingerDistance;
	}

	/**
	 * The MixtureModelGenerator can generate an infinite number of instances.
	 * 
	 * @see moa.streams.ExampleStream#estimatedRemainingInstances()
	 */
	@Override
	public long estimatedRemainingInstances()
	{
		return -1;
	}

	/**
	 * The MixtureModelGenerator can generate an infinite number of instances, therefore this method always returns TRUE.
	 * 
	 * @see moa.streams.ExampleStream#hasMoreInstances()
	 */
	@Override
	public boolean hasMoreInstances()
	{
		return true;
	}

	/**
	 * @see moa.streams.ExampleStream#isRestartable()
	 */
	@Override
	public boolean isRestartable()
	{
		return true;
	}

	/**
	 * Restarts the MixtureModel by repassing the pseudo random number generators' original seed values.
	 * 
	 * @see moa.streams.ExampleStream#restart()
	 * @see moa.streams.generators.mixturemodel.MixtureModel#restart(int, int)
	 */
	@Override
	public void restart()
	{
		this.mixtureModelPre.restart(this.instanceRandomSeedOption.getValue(), this.modelRandomSeedOption.getValue());
		this.mixtureModelPost.restart(this.instanceRandomSeedOption.getValue(), this.modelRandomSeedOption.getValue());
	}

	@Override
	public String getPurposeString()
	{
		return "Generates a data stream which features concept drift and is based on mixture models.";
	}

	/**
	 * @see moa.MOAObject#getDescription(java.lang.StringBuilder, int)
	 */
	@Override
	public void getDescription(StringBuilder sb, int indent)
	{
		// Not implemented.
	}
}
