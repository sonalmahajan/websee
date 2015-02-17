package rca;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.jgap.BaseGene;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.InvalidConfigurationException;
import org.jgap.RandomGenerator;
import org.jgap.UnsupportedRepresentationException;

public class CustomGene extends BaseGene implements Gene, java.io.Serializable
{
	private static final long serialVersionUID = 1L;

	private static final String TOKEN_SEPARATOR = ":";

	private List<String> possibleValues;
	private String myValue;

	public CustomGene(Configuration a_conf, List<String> possibleValues) throws InvalidConfigurationException
	{
		super(a_conf);
		this.possibleValues = possibleValues; 
	}

	public Gene newGeneInternal()
	{
		try
		{
			return new CustomGene(getConfiguration(), possibleValues);
		}
		catch (InvalidConfigurationException ex)
		{
			throw new IllegalStateException(ex.getMessage());
		}
	}

	public void setAllele(Object a_newValue)
	{
		if(a_newValue instanceof Integer)
		{
			myValue = possibleValues.get((Integer)a_newValue);
		}
		else if(a_newValue instanceof String)
		{
			myValue = (String) a_newValue;
		}
	}

	public Object getAllele()
	{
		return myValue;
	}

	public void setToRandomValue(RandomGenerator a_numberGenerator)
	{
		myValue = possibleValues.get(a_numberGenerator.nextInt(possibleValues.size()));
	}

	public String getPersistentRepresentation() throws UnsupportedOperationException
	{
		return possibleValues + TOKEN_SEPARATOR + myValue;
	}

	public void setValueFromPersistentRepresentation(String a_representation) throws UnsupportedOperationException, UnsupportedRepresentationException
	{
		StringTokenizer tokenizer = new StringTokenizer(a_representation, TOKEN_SEPARATOR);
		if (tokenizer.countTokens() != 2)
		{
			throw new UnsupportedRepresentationException("Unknown representation format: Two tokens expected.");
		}

		try
		{
			possibleValues = new ArrayList<String>();
			myValue = tokenizer.nextToken();
		}
		catch (NumberFormatException e)
		{
			throw new UnsupportedRepresentationException("Unknown representation format: Expecting integer values.");
		}
	}

	public void cleanup()
	{
	}

	public int compareTo(Object a_otherQuarterGene)
	{
		if (a_otherQuarterGene == null)
		{
			return 1;
		}

		if (myValue == null)
		{
			if (((CustomGene) a_otherQuarterGene).myValue == null)
			{
				return 0;
			}
			else
			{
				return -1;
			}
		}

		return myValue.compareTo(((CustomGene) a_otherQuarterGene).myValue);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((myValue == null) ? 0 : myValue.hashCode());
		result = prime * result + ((possibleValues == null) ? 0 : possibleValues.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomGene other = (CustomGene) obj;
		if (myValue == null)
		{
			if (other.myValue != null)
				return false;
		}
		else if (!myValue.equals(other.myValue))
			return false;
		if (possibleValues == null)
		{
			if (other.possibleValues != null)
				return false;
		}
		else if (!possibleValues.equals(other.possibleValues))
			return false;
		return true;
	}

	public Object getInternalValue()
	{
		return myValue;
	}

	public void applyMutation(int a_index, double a_percentage)
	{
		setAllele(getConfiguration().getRandomGenerator().nextInt(possibleValues.size()));
	}
}