package csiro.pidsvc.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;

public class Settings
{
	private static Settings			_instance = null;
	protected Properties			_properties = new Properties();
	protected Manifest				_manifest = new Manifest();
	protected Map<String, String>	_serverProperties = new HashMap<String, String>(6);

	private Settings()
	{
		try
		{
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			_properties.load(classLoader.getResourceAsStream("../pidsvc.properties"));
			_manifest.read(classLoader.getResourceAsStream("../../META-INF/MANIFEST.MF"));

			_serverProperties.put("serverJavaVersion", System.getProperty("java.version"));
			_serverProperties.put("serverJavaVendor", System.getProperty("java.vendor"));
            _serverProperties.put("javaHome", System.getProperty("java.home"));
            _serverProperties.put("serverOsArch", System.getProperty("os.arch"));
            _serverProperties.put("serverOsName", System.getProperty("os.name"));
            _serverProperties.put("serverOsVersion", System.getProperty("os.version"));
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

	public Manifest getManifest()
	{
		return _manifest;
	}

	public String getManifestProperty(String name)
	{
		return _manifest.getMainAttributes().getValue(name);
	}

	public Map<String, String> getServerProperties()
	{
		return _serverProperties;
	}
}
