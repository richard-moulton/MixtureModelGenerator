/*
 *    MixtureModelGenerator.java
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
 * @author Richard Hugh Moulton
 *
 */
public class MixtureModelGenerator extends AbstractOptionHandler implements InstanceStream
{
	 @Override
	 public String getPurposeString()
	 {
		 return "Generates a data stream based on a mixture model.";
	 }
	 
	private static final long serialVersionUID = 1L;

	public IntOption modelRandomSeedOption = new IntOption("modelRandomSeed",
            'r', "Seed for random generation of model.", 1);

    public IntOption instanceRandomSeedOption = new IntOption(
            "instanceRandomSeed", 'i',
            "Seed for random generation of instances.", 1);

    public IntOption numClassesOption = new IntOption("numClasses", 's',
            "The number of classes in the data stream and the number of models to include in the mixture model.",
            2, 2, Integer.MAX_VALUE);

    public IntOption numAttsOption = new IntOption("numAtts", 'a',
            "The number of attributes to generate.", 10, 0, Integer.MAX_VALUE);

    public IntOption distributionOption = new IntOption("distribution", 'd',
            "Distribution used for models. Uniform 0, Gaussian 1, etc.",
            1, 0, 4);
	
    protected InstancesHeader streamHeader;
    protected MixtureModel mixtureModel;
    
	/* (non-Javadoc)
	 * @see moa.streams.ExampleStream#getHeader()
	 */
	@Override
	public InstancesHeader getHeader()
	{
		return this.streamHeader;
	}

	/* (non-Javadoc)
	 * @see moa.streams.ExampleStream#estimatedRemainingInstances()
	 */
	@Override
	public long estimatedRemainingInstances()
	{
		return -1;
	}

	/* (non-Javadoc)
	 * @see moa.streams.ExampleStream#hasMoreInstances()
	 */
	@Override
	public boolean hasMoreInstances()
	{
		return true;
	}

	/* (non-Javadoc)
	 * @see moa.streams.ExampleStream#nextInstance()
	 */
	@Override
	public Example<Instance> nextInstance()
	{
		return this.mixtureModel.nextInstance();
	}

	/* (non-Javadoc)
	 * @see moa.streams.ExampleStream#isRestartable()
	 */
	@Override
	public boolean isRestartable()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see moa.streams.ExampleStream#restart()
	 */
	@Override
	public void restart()
	{
		// Nothing required to be restarted.
	}

	/* (non-Javadoc)
	 * @see moa.MOAObject#getDescription(java.lang.StringBuilder, int)
	 */
	@Override
	public void getDescription(StringBuilder sb, int indent)
	{
		// Not implemented.
	}

	/* (non-Javadoc)
	 * @see moa.options.AbstractOptionHandler#prepareForUseImpl(moa.tasks.TaskMonitor, moa.core.ObjectRepository)
	 */
	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository)
	{
		generateHeader();
		
		// Initialize mixture model
		this.mixtureModel = new MixtureModel(this.numClassesOption.getValue(), this.numAttsOption.getValue(),
				this.instanceRandomSeedOption.getValue(), this.modelRandomSeedOption.getValue());
	}

	private void generateHeader()
	{
		FastVector<Attribute> attributes = new FastVector<Attribute>();
        for (int i = 0; i < this.numAttsOption.getValue(); i++) {
            attributes.addElement(new Attribute("att" + (i + 1)));
        }
        FastVector<String> classLabels = new FastVector<String>();
        for (int i = 0; i < this.numClassesOption.getValue(); i++) {
            classLabels.addElement("class" + (i + 1));
        }
        attributes.addElement(new Attribute("class", classLabels));
        this.streamHeader = new InstancesHeader(new Instances(
                getCLICreationString(InstanceStream.class), attributes, 0));
        this.streamHeader.setClassIndex(this.streamHeader.numAttributes() - 1);
	}

}
