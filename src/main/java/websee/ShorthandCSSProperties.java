package websee;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import config.Constants;

public class ShorthandCSSProperties
{
	// singleton pattern
	private static final ShorthandCSSProperties instance = new ShorthandCSSProperties();
	private Map<String, List<String>> shorthandPropertyMap;

	private ShorthandCSSProperties()
	{
		shorthandPropertyMap = new HashMap<String, List<String>>();
		Properties prop = readPropertiesFile(new File(Constants.SHORTHAND_CSS_PROPERTIES_FILE_PATH));
		processProperties(prop);
	}

	public Map<String, List<String>> getShorthandPropertyMap()
	{
		return shorthandPropertyMap;
	}

	public static ShorthandCSSProperties getInstance()
	{
		return instance;
	}
	
	private Properties readPropertiesFile(File file)
	{
		// Read properties file.
		Properties properties = new Properties();
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
	
	private void processProperties(Properties prop)
	{
		for(Object key : prop.keySet())
		{
			String propertyValue = prop.getProperty((String) key);
			shorthandPropertyMap.put((String) key, Arrays.asList(propertyValue.split("\\|")));
		}
	}
}
