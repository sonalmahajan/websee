package evalframework;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.RandomStringUtils;

public class AttributeHelperValuesMaster
{
	// singleton pattern
	private static final AttributeHelperValuesMaster instance = new AttributeHelperValuesMaster();
	
	private HashMap<String, ArrayList<String>> helperValues;
	
	private AttributeHelperValuesMaster()
	{
		helperValues = new HashMap<String, ArrayList<String>>();
		LinkedProperties prop = (LinkedProperties) readPropertiesFile(new File(EvaluationFrameworkConstants.helperValuesFilePath));
		processProperties(prop);
	}
	
	public static AttributeHelperValuesMaster getInstance()
	{
		return instance;
	}
	
	public HashMap<String, ArrayList<String>> getHelperValues()
	{
		return helperValues;
	}

	public void setHelperValues(HashMap<String, ArrayList<String>> helperValues)
	{
		this.helperValues = helperValues;
	}

	private Properties readPropertiesFile(File file)
	{
		// Read properties file.
		LinkedProperties properties = new LinkedProperties();
		try
		{
			properties.load(new FileInputStream(file));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return properties;
	}
	
	private void processProperties(LinkedProperties properties)
	{
		for(Object key : properties.keySet())
		{
			String propertyValue = properties.getProperty((String) key);
			ArrayList<String> values = new ArrayList<String>();
			
			//number or float
			if(propertyValue.equals(HelperValuesContants.NON_ZERO_POSITIVE_INTEGER) || propertyValue.equals(HelperValuesContants.NON_ZERO_POSITIVE_DECIMAL))
			{
				//add 2 values
				values.add(String.valueOf(getRandomInteger(HelperValuesContants.MIN_NON_ZERO_POSITIVE_INTEGER, HelperValuesContants.MAX_INTEGER)));
				values.add(String.valueOf(getRandomInteger(HelperValuesContants.MIN_NON_ZERO_POSITIVE_INTEGER, HelperValuesContants.MAX_INTEGER)));
			}
			//nnumber or nfloat
			else if(propertyValue.equals(HelperValuesContants.INTEGER) || propertyValue.equals(HelperValuesContants.DECIMAL))
			{
				values.add(String.valueOf(getRandomInteger(HelperValuesContants.MIN_INTEGER, HelperValuesContants.MAX_INTEGER)));
				values.add(String.valueOf(getRandomInteger(HelperValuesContants.MIN_INTEGER, HelperValuesContants.MAX_INTEGER)));
			}
			
			//big number
			if(propertyValue.equals(HelperValuesContants.BIG_INTEGER))
			{
				//add 2 values
				values.add(String.valueOf(getRandomInteger(HelperValuesContants.MIN_BIG_INTEGER, HelperValuesContants.MAX_BIG_INTEGER)));
				values.add(String.valueOf(getRandomInteger(HelperValuesContants.MIN_BIG_INTEGER, HelperValuesContants.MAX_BIG_INTEGER)));
			}
			
			//urlstring
			else if(propertyValue.equals(HelperValuesContants.URL))
			{
				values.add("file://" + HelperValuesContants.URL_IMAGE_PATH);
			}
			
			//urlstring small image
			else if(propertyValue.equals(HelperValuesContants.URL_SMALL_IMAGE))
			{
				values.add("file://" + HelperValuesContants.URL_SMALL_IMAGE_PATH);
			}
			
			//urlstring border image
			else if(propertyValue.equals(HelperValuesContants.URL_BORDER_IMAGE))
			{
				values.add("file://" + HelperValuesContants.URL_BORDER_IMAGE_PATH);
			}
			//string
			else if(propertyValue.equals(HelperValuesContants.TEXT))
			{
				values.add(RandomStringUtils.random(HelperValuesContants.STRING_LENGTH, HelperValuesContants.STRING_RANDOM));
			}
			
			//predefined values
			else if(propertyValue.contains("|"))
			{
				String val[] = propertyValue.split("\\|");
				int index = getRandomInteger(0, val.length);
				
				if(val[index].matches(HelperValuesContants.DERIVED_VALUE_CHECK))
				{
					ArrayList<String> derivedValues = getDerivedValues(val[index]);
					values.add(getDerivedValueString(val[index], derivedValues, 0));
				}
				else
				{
					values.add(val[index]);
				}
				
				if(val.length > 1)
				{
					int index2;
					//avoid same random value being generated
					while((index2 = getRandomInteger(0, val.length)) == index);
					if(val[index2].matches(HelperValuesContants.DERIVED_VALUE_CHECK))
					{
						ArrayList<String> derivedValues = getDerivedValues(val[index2]);
						values.add(getDerivedValueString(val[index2], derivedValues, 0));
					}
					else
					{
						values.add(val[index2]);
					}
				}
			}
			//derived values
			else if(propertyValue.matches(HelperValuesContants.DERIVED_VALUE_CHECK))
			{
				ArrayList<String> derivedValues = getDerivedValues(propertyValue);
				for(int i = 0 ; i < 2 ; i++)
				{
					values.add(getDerivedValueString(propertyValue, derivedValues, i));
				}
			}
			//normalized decimal
			else if(propertyValue.matches(HelperValuesContants.NORMALIZED_DECIMAL))
			{
				Random generator = new Random(); 
				values.add(generator.nextFloat() + "");
				values.add(generator.nextFloat() + "");
			}
			
			helperValues.put((String) key, values);
		}
	}
	
	private String getDerivedValueString(String propertyValue, ArrayList<String> derivedValues, int index)
	{
		String combinedValue = propertyValue;
		for (String derivedValue : derivedValues)
		{
			//strip off the brackets 
			derivedValue = derivedValue.replaceAll("<", "").replaceAll(">", "");
			ArrayList<String> vals = helperValues.get(derivedValue);
			combinedValue = combinedValue.replace("<" + derivedValue + ">", vals.get(index));
		}
		return combinedValue;
	}
	
	private int getRandomInteger(int min, int max)
	{
		Random generator = new Random(); 
		int number = generator.nextInt(max - min) + min;
		return number;
	}
	
	private ArrayList<String> getDerivedValues(String propertyValue)
	{
		ArrayList<String> derivedValues = new ArrayList<String>();
		Pattern p = Pattern.compile(HelperValuesContants.DERIVED_VALUE, Pattern.DOTALL);
		Matcher m = p.matcher(propertyValue);
		while (m.find())
		{
			derivedValues.add(m.group());		
		}
		return derivedValues;
	}
	
	public static void main(String args[])
	{
		AttributeHelperValuesMaster ahvm = new AttributeHelperValuesMaster();
		LinkedProperties prop = (LinkedProperties) ahvm.readPropertiesFile(new File("C:\\Dev\\VisualInvariants\\evaluationframework\\com\\attributes\\master\\properties\\attribute_helper_values.properties"));
		ahvm.processProperties(prop);
	}		
}

/**
 * preserve order of the properties file
 */
class LinkedProperties extends Properties 
{
	private static final long serialVersionUID = 1L;
	private final LinkedHashSet<Object> keys = new LinkedHashSet<Object>();

    public Enumeration<Object> keys() {
        return Collections.<Object>enumeration(keys);
    }

    @Override
    public Set<Object> keySet()
    {
    	return new LinkedHashSet<Object>(Collections.list(keys()));
    }
    
    public Object put(Object key, Object value) {
        keys.add(key);
        return super.put(key, value);
    }
}