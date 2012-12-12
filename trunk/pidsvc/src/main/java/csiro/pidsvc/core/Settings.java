package csiro.pidsvc.core;

import java.io.IOException;
import java.util.Properties;

public class Settings
{
	private static Settings _instance = null;
	private Properties _properties = new Properties();

	private Settings()
	{
		try
		{
			_properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("../pidsvc.properties"));
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	protected Object clone() throws CloneNotSupportedException
	{
		throw new CloneNotSupportedException();
	}

	public static Settings getInstance()
	{
		if (_instance == null)
		{
			synchronized (Settings.class)
			{
				if (_instance == null)
					_instance = new Settings();
			}
		}
		return _instance;
	}

	public String getProperty(String name)
	{
		return _properties.getProperty(name);
	}
}
