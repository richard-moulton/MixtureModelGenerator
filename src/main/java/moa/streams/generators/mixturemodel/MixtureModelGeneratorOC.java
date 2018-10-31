/*
 *    MixtureModelGeneratorOC.java
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

import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.core.Example;
import moa.core.FastVector;
import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.options.AbstractOptionHandler;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;

/**
* Generates a data stream representing one majority class and some number of minority classes
* based on an underlying mixture model. For use with the Imbalanced Stream generator.
* 
* Diagnostic print outs to the console are "commented out" along the process of generating
* new instances.
* 
* @author Richard Hugh Moulton
*/
public class MixtureModelGeneratorOC extends AbstractOptionHandler implements InstanceStream {

	private static final long serialVersionUID = 1L;

	public IntOption numAttsOption = new IntOption("numAtts", 'a',
            "The number of attributes to generate.", 10, 0, Integer.MAX_VALUE);

	// Not implemented
    //public IntOption distributionOption = new IntOption("distribution", 'd',
    //        "Distribution used for models. Uniform 0, Gaussian 1, etc.",
    //        1, 0, 4);
    
	public IntOption numMajClassesOption = new IntOption("numMajClasses", 'j',
			"The number of models that will comprise the majority class.", 1, 1, 10);
	
    public IntOption numMinClassesOption = new IntOption("numMinClasses", 'n',
            "The number of minority classes in the data stream and the number of models to include in the mixture model.",
            2, 1, 50);
    
    public FloatOption percentMajorityOption = new FloatOption("percentMajorityOption", 'p', "The percentage of instances "
    		+ "to draw from the majority class (must be greater than 50%).", 0.9, 0.51, 1.0);
    
    public FlagOption conceptMarkOption = new FlagOption("conceptMarked", 'c', "If true, each instance will have an "
    		+ "attribute describing the concept it is drawn from.");
    
    public IntOption modelRandomSeedOption = new IntOption("modelRandomSeed",
            'm', "Seed for random generation of model.", 1);

    public IntOption instanceRandomSeedOption = new IntOption("instanceRandomSeed", 
    		'r', "Seed for random generation of instances.", 1);
	
    protected InstancesHeader streamHeader, cmHeader;
    protected MixtureModel mixtureModel;
    private double[] conceptAssignments;
    private int numInstances;
    
    /**
	 * @see moa.options.AbstractOptionHandler#prepareForUseImpl(moa.tasks.TaskMonitor, moa.core.ObjectRepository)
	 * @see moa.streams.generators.mixturemodel.MixtureModel
	 */
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository)
	{
		generateHeader();
		this.numInstances = 0;
		this.mixtureModel = new MixtureModel(this.numMinClassesOption.getValue()+this.numMajClassesOption.getValue(), this.numAttsOption.getValue(),
				this.instanceRandomSeedOption.getValue(), this.modelRandomSeedOption.getValue());
		this.mixtureModel.setWeights(this.numMajClassesOption.getValue(), this.percentMajorityOption.getValue());
		if(this.conceptMarkOption.isSet())
		{
			this.conceptAssignments = this.mixtureModel.getConceptAssignments(this.numMajClassesOption.getValue());
			generateCMHeader();
		}
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
		Instance nextInst = this.mixtureModel.nextInstance(this.streamHeader).getData();
		
		//System.out.print("("+this.numInstances+") nextInst:");
		//for(int i = 0 ; i < nextInst.numAttributes() ; i++)
		//{
		//	System.out.print(" "+nextInst.value(i));
		//}
		
		if(this.conceptMarkOption.isSet())
		{
			double[] newAttributeValues = new double[nextInst.numAttributes()+1];
			
			newAttributeValues[0] = this.conceptAssignments[(int)nextInst.classValue()];
			for(int i = 0 ; i < nextInst.numAttributes() ; i++)
			{
				newAttributeValues[i+1] = nextInst.value(i);
			}
			
			nextInst = new DenseInstance(1.0, newAttributeValues);
			nextInst.setDataset(this.cmHeader);
			
			//System.out.print(" cmNextInst:");
			//for(int i = 0 ; i < nextInst.numAttributes() ; i++)
			//{
			//	System.out.print(" "+nextInst.value(i));
			//}
	    }
		
		if(nextInst.classValue() < this.numMajClassesOption.getValue())
			nextInst.setClassValue(0);
		else
			nextInst.setClassValue(1);
		
		//System.out.print(" newNextInst:");
		//for(int i = 0 ; i < nextInst.numAttributes() ; i++)
		//{
		//	System.out.print(" "+nextInst.value(i));
		//}
		//System.out.println();
		
		this.numInstances++;
		
		return new InstanceExample(nextInst);
	}
	
	/**
	 * Generates the stream's header.
	 */
	private void generateHeader()
	{
		FastVector<Attribute> attributes = new FastVector<Attribute>();
        for (int i = 0; i < this.numAttsOption.getValue(); i++) {
            attributes.addElement(new Attribute("att" + (i + 1)));
        }
        FastVector<String> classLabels = new FastVector<String>();
        classLabels.addElement("class0");
        classLabels.addElement("class1");

        attributes.addElement(new Attribute("class", classLabels));
        this.streamHeader = new InstancesHeader(new Instances(
                getCLICreationString(InstanceStream.class), attributes, 0));
        this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);
	}
	
	/**
	 * Generates the stream's header if conceptMark is set.
	 * 
	 * @see conceptMarkOption
	 */
	private void generateCMHeader()
	{
		FastVector<Attribute> attributes = new FastVector<Attribute>();
		attributes.addElement((new Attribute("concept")));
        for (int i = 0; i < this.numAttsOption.getValue(); i++) {
            attributes.addElement(new Attribute("att" + (i + 1)));
        }
        FastVector<String> classLabels = new FastVector<String>();
        classLabels.addElement("class0");
        classLabels.addElement("class1");

        attributes.addElement(new Attribute("class", classLabels));
        this.cmHeader = new InstancesHeader(new Instances(
                getCLICreationString(InstanceStream.class), attributes, 0));
        this.cmHeader.setClassIndex(this.cmHeader.numAttributes() - 1);
   	}
    
	/**
	 * @return the stream's header.
	 * @see moa.streams.ExampleStream#getHeader()
	 */
	@Override
	public InstancesHeader getHeader()
	{
		if(this.conceptMarkOption.isSet())
			return this.cmHeader;
		else
			return this.streamHeader;
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
		this.numInstances = 0;
		this.mixtureModel.restart(this.instanceRandomSeedOption.getValue(), this.modelRandomSeedOption.getValue());
		if(this.conceptMarkOption.isSet())
			this.conceptAssignments = this.mixtureModel.getConceptAssignments(this.numMajClassesOption.getValue());
	}

	 @Override
	 public String getPurposeString()
	 {
		 return "Generates a data stream with a mixture model based sub-concept structure.";
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
