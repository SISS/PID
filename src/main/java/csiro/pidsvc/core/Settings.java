/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 *
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 *
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

package csiro.pidsvc.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;

import csiro.pidsvc.helper.Http;
import csiro.pidsvc.helper.JSONObjectHelper;

/**
 * Application settings handling.
 *
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class Settings
{
	final static String SETTINGS_OPT = "pidsvc.settings";

	private static Logger _logger = LogManager.getLogger(Settings.class.getName());

	private static Settings			_instance = null;
	protected HttpServlet			_servlet = null;
	protected ResourceBundle		_properties = null;
	protected Manifest				_manifest = null;
	protected Map<String, String>	_serverProperties = new HashMap<String, String>(6);

	private Settings(HttpServlet servlet) throws NullPointerException, IOException
	{
		// Retrieve manifest.
		if ((_servlet = servlet) != null)
		{
			ServletConfig config = _servlet.getServletConfig();
			if (config != null)
			{
				ServletContext application = config.getServletContext();
				_manifest = new Manifest(application.getResourceAsStream("/META-INF/MANIFEST.MF"));
			}
		}

		// Retrieve settings.
		FileInputStream fis = null;
		try
		{
			InitialContext context = new InitialContext();
			String settingsFile = (String)context.lookup("java:comp/env/" + SETTINGS_OPT);
			fis = new FileInputStream(settingsFile);
			_properties = new PropertyResourceBundle(fis);
		}
		catch (NamingException ex)
		{
			_logger.debug("Using default pidsvc.properties file.");
			_properties = ResourceBundle.getBundle("pidsvc");
		}
		finally
		{
			if (fis != null)
				fis.close();
		}

		// Get additional system properties.
		_serverProperties.put("serverJavaVersion", System.getProperty("java.version"));
		_serverProperties.put("serverJavaVendor", System.getProperty("java.vendor"));
        _serverProperties.put("javaHome", System.getProperty("java.home"));
        _serverProperties.put("serverOsArch", System.getProperty("os.arch"));
        _serverProperties.put("serverOsName", System.getProperty("os.name"));
        _serverProperties.put("serverOsVersion", System.getProperty("os.version"));
	}

	protected Object clone() throws CloneNotSupportedException
	{
		throw new CloneNotSupportedException();
	}

	public static void init(HttpServlet servlet) throws NullPointerException, IOException
	{
		getInstance(servlet);
	}

	public static Settings getInstance() throws NullPointerException, IOException
	{
		return getInstance(null);
	}

	public static Settings getInstance(HttpServlet servlet) throws NullPointerException, IOException
	{
		if (_instance == null)
		{
			synchronized (Settings.class)
			{
				if (_instance == null)
					_instance = new Settings(servlet);
			}
		}
		return _instance;
	}

	public String getProperty(String name)
	{
		return _properties == null ? null : _properties.getString(name);
	}

	public Manifest getManifest()
	{
		return _manifest;
	}

	public String getManifestProperty(String name)
	{
		return _manifest == null ? null : _manifest.getMainAttributes().getValue(name);
	}

	public Map<String, String> getServerProperties()
	{
		return _serverProperties;
	}

	@SuppressWarnings("unchecked")
	public JSONObject getManifestJson()
	{
		JSONObject json = new JSONObject();

		// Build repository.
		json.put("repository", getProperty("buildRepository"));

		// Build manifest.
		if (_manifest != null)
		{
			JSONObject manifest = new JSONObject();
			for (Entry<Object, Object> entry : _manifest.getMainAttributes().entrySet())
				manifest.put(entry.getKey(), entry.getValue());
			json.put("manifest", manifest);
		}

		// Server environment.
		JSONObject server = new JSONObject();
		for (Entry<String, String> entry : _serverProperties.entrySet())
			server.put(entry.getKey(), entry.getValue());
		json.put("server", server);

		return json;
	}

	public boolean isNewVersionAvailable()
	{
		if (_manifest == null)
			return false;

		String      buildRepository = getProperty("buildRepository");
		if (buildRepository == null || buildRepository.trim().isEmpty()){
			_logger.debug("buildRepository property is not set: skipping new version check.");
		}
		else {
			String		content = Http.simpleGetRequest(getProperty("buildRepository"));
			Pattern		re = Pattern.compile("href=\"pidsvc-(\\d+\\.\\d+)(?:-SNAPSHOT)?\\.(.+?)\\.war\"", Pattern.CASE_INSENSITIVE);
			Matcher		m = re.matcher(content);
			try
			{
				if (m.find())
				{
					String currentVersion = _manifest.getMainAttributes().getValue("Implementation-Build");
					String newVersion = m.group(2);

					if (!currentVersion.isEmpty() && !newVersion.equalsIgnoreCase(currentVersion)){
						return true;
					}
				}
			}
			catch (Exception e)
			{
				_logger.debug(e);
			}
		}
		return false;
	}

	public JSONObject isNewVersionAvailableJson()
	{
		return JSONObjectHelper.create(
				"isAvailable", isNewVersionAvailable(),
				"repository", getProperty("buildRepository")
			);
	}
}
