package csiro.pidsvc.mappingstore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringEscapeUtils;

import csiro.pidsvc.helper.URI;
import csiro.pidsvc.mappingstore.action.Descriptor;
import csiro.pidsvc.mappingstore.action.List;
import csiro.pidsvc.mappingstore.condition.AbstractCondition;
import csiro.pidsvc.mappingstore.condition.ConditionPrioritizedContentType;

public class Manager
{
	protected Connection _connection = null;
	
	public class MappingMatchResults
	{
		public static final int NULL = -1;

		public final int DefaultActionId;
		public final AbstractCondition Condition;
		public final Object AuxiliaryData;
		
		public MappingMatchResults(int defaultActionId, AbstractCondition condition, Object auxiliaryData)
		{
			DefaultActionId = defaultActionId;
			Condition = condition;
			AuxiliaryData = auxiliaryData;
		}
		
		public boolean success()
		{
			return DefaultActionId != MappingMatchResults.NULL || Condition != null || AuxiliaryData != null;
		}
	}
	
	protected class ConditionDescriptor
	{
		public final int ID;
		public final String Type;
		public final String Match;
		
		public ConditionDescriptor(int id, String type, String match)
		{
			this.ID = id;
			this.Type = type;
			this.Match = match;
		}
	}
	
	public Manager() throws NamingException, SQLException, IOException
	{
		Properties properties = new Properties(); 
		properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("../mappingstore.properties"));

		InitialContext initCtx = new InitialContext(); 
		Context envCtx = (Context)initCtx.lookup("java:comp/env"); 
		DataSource ds = (DataSource)envCtx.lookup(properties.getProperty("jndiReferenceName"));
		
		_connection = ds.getConnection();
	}

	public void close()
	{
		try
		{
			_connection.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			_connection = null;
		}
	}
	
	public String createMapping(InputStream inputStream) throws SaxonApiException, SQLException
	{
		// Generate SQL query.
		Processor			processor = new Processor(false);
		XsltCompiler		xsltCompiler = processor.newXsltCompiler();
		InputStream			inputSqlGen = getClass().getResourceAsStream("xslt/import_mapping_postgresql.xslt");
		XsltExecutable		xsltExec = xsltCompiler.compile(new StreamSource(inputSqlGen));
		XsltTransformer		transformer = xsltExec.load();
		XdmNode				source = processor.newDocumentBuilder().build(new StreamSource(inputStream));
		StringWriter		swSqlQuery = new StringWriter();
		
		// TODO: Add validation of the input request.

		transformer.setInitialContextNode(source);
		transformer.setDestination(new Serializer(swSqlQuery));
		transformer.transform();

		// Update mappings in the database.
		Statement st = null;
		try
		{
			String sqlQuery = swSqlQuery.toString();
			st = _connection.createStatement();
			st.execute(sqlQuery);

			Pattern re = Pattern.compile("^--(OK: .*)", Pattern.CASE_INSENSITIVE);
			Matcher m = re.matcher(sqlQuery);

			if (m.find())
				return m.group(1);
		}
		finally
		{
			if (st != null)
			st.close();
		}
		return null;
	}
	
	public boolean deleteMapping(String mappingPath) throws SQLException
	{
		PreparedStatement pst = null;
		try
		{
			pst = _connection.prepareStatement("UPDATE \"mapping\" SET date_end = now() WHERE mapping_path = ? AND date_end IS NULL;");
			pst.setString(1, mappingPath);
			return pst.execute();
		}
		finally
		{
			if (pst != null)
				pst.close();
		}
	}
	
	public MappingMatchResults findExactMatch(URI uri, HttpServletRequest request) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		int					defaultActionId = MappingMatchResults.NULL;
		AbstractCondition	retCondition = null;
		Object				retAuxiliaryData = null;

		try
		{
			pst = _connection.prepareStatement("SELECT mapping_id, default_action_id FROM vw_active_mapping WHERE mapping_path = ? AND \"type\" = '1:1'");
			pst.setString(1, uri.getPathNoExtension());
			
			if (pst.execute())
			{
				// First result set.
				rs = pst.getResultSet();
				if (rs.next())
				{
					// Save default action id.
					defaultActionId = rs.getInt(2);
					if (rs.wasNull())
						defaultActionId = MappingMatchResults.NULL;

					// Set auxiliary data to "true" to flag that the URI has been found.
					retAuxiliaryData = true;

					// Find matching condition.
					retCondition = getCondition(rs.getInt(1), uri, request);
				}
			}
		}
		finally
		{
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
		}
		return new MappingMatchResults(defaultActionId, retCondition, retAuxiliaryData);		
	}

	public MappingMatchResults findRegexMatch(URI uri, HttpServletRequest request) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException
	{
		PreparedStatement	pst = null;
		ResultSet 			rs = null;
		int					defaultActionId = MappingMatchResults.NULL;
		AbstractCondition	retCondition = null;
		Object				retAuxiliaryData = null;

		try
		{
			pst = _connection.prepareStatement("SELECT mapping_id, mapping_path, default_action_id FROM vw_active_mapping WHERE \"type\" = 'Regex'");
			if (pst.execute())
			{
				rs = pst.getResultSet();
				while (rs.next())
				{
					Pattern re = Pattern.compile(rs.getString(2), Pattern.CASE_INSENSITIVE);
					Matcher m = re.matcher(uri.getPathNoExtension());

					if (m.find())
					{
						// Save default action id.
						defaultActionId = rs.getInt(3);
						if (rs.wasNull())
							defaultActionId = MappingMatchResults.NULL;

						// Save auxiliary data.
						retAuxiliaryData = re;
						
						// Find matching condition.
						retCondition = getCondition(rs.getInt(1), uri, request);
						break;
					}
				}
			}
		}
		finally
		{
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
		}
		
		return new MappingMatchResults(defaultActionId, retCondition, retAuxiliaryData);
	}
	
	protected Vector<ConditionDescriptor> getConditions(int mappingId) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet 			rs = null;

		try
		{
			pst = _connection.prepareStatement("SELECT * FROM \"condition\" WHERE mapping_id = ? ORDER BY condition_id");
			pst.setInt(1, mappingId);
			
			if (!pst.execute())
				return null;

			// Get list of conditions.
			Vector<ConditionDescriptor> conditions = new Vector<ConditionDescriptor>();
			for (rs = pst.getResultSet(); rs.next(); conditions.add(new ConditionDescriptor(rs.getInt("condition_id"), rs.getString("type"), rs.getString("match"))));
			return conditions;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
		}
		return null;
	}
	
	protected AbstractCondition getCondition(int mappingId, URI uri, HttpServletRequest request) throws SQLException
	{
		// Get list of conditions.
		Vector<ConditionDescriptor> conditions = getConditions(mappingId);
		if (conditions == null)
			return null;

		// Test conditions.
		try
		{
			Vector<ConditionPrioritizedContentType> prioritizedConditions = null;
			for (ConditionDescriptor descriptor : conditions)
			{
				/*
				 * Once PrioritizedContentType condition is encountered process all
				 * PrioritizedContentType conditions in one go as a group.
				 */
				if (descriptor.Type.equalsIgnoreCase("PrioritizedContentType"))
				{
					// Skip if PrioritizedContentType conditions have already been processed.
					if (prioritizedConditions != null)
						continue;
	
					// Extract all PrioritizedContentType conditions.
					prioritizedConditions = new Vector<ConditionPrioritizedContentType>();
					for (ConditionDescriptor dctr : conditions)
					{
						if (dctr.Type.equalsIgnoreCase("PrioritizedContentType"))
						{
							Class<?> impl = Class.forName("csiro.pidsvc.mappingstore.condition.Condition" + dctr.Type);
							Constructor<?> ctor = impl.getDeclaredConstructor(URI.class, HttpServletRequest.class, int.class, String.class);
			
							prioritizedConditions.add((ConditionPrioritizedContentType)ctor.newInstance(uri, request, dctr.ID, dctr.Match));
						}
					}
	
					// Find matching conditions.
					AbstractCondition matchingCondition = ConditionPrioritizedContentType.getMatchingCondition(prioritizedConditions);
					if (matchingCondition != null)
						return matchingCondition;
	
					// Continue if no matching conditions were found.
					continue;
				}
				
				// Process all other conditions.
				Class<?> impl = Class.forName("csiro.pidsvc.mappingstore.condition.Condition" + descriptor.Type);
				Constructor<?> ctor = impl.getDeclaredConstructor(URI.class, HttpServletRequest.class, int.class, String.class);
	
				AbstractCondition condition = (AbstractCondition)ctor.newInstance(uri, request, descriptor.ID, descriptor.Match);
				if (condition.matches())
					return condition;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public Descriptor getActionsByActionId(int actionId) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		
		try
		{
			pst = _connection.prepareStatement("SELECT * FROM \"action\" WHERE action_id = ?");
			pst.setInt(1, actionId);
			
			rs = pst.executeQuery();
			rs.next();
			return new Descriptor(rs.getString("type"), rs.getString("action_name"), rs.getString("action_value"));
		}
		finally
		{
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
		}
	}
	
	public List getActionsByConditionId(int conditionId) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		List				actions = new List();
		
		try
		{
			pst = _connection.prepareStatement("SELECT * FROM \"action\" WHERE condition_id = ?");
			pst.setInt(1, conditionId);
			
			rs = pst.executeQuery();
			while (rs.next())
			{
				actions.add(new Descriptor(rs.getString("type"), rs.getString("action_name"), rs.getString("action_value")));
			}
		}
		finally
		{
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
		}
		return actions;
	}

	/**************************************************************************
	 *  Export/import.
	 */
	
	public String backupDataStore(boolean fullBackup, boolean includeDeprecated) throws SQLException
	{
		String source;
		if (includeDeprecated)
			source = fullBackup ? "mapping" : "vw_latest_mapping";
		else
			source = fullBackup ? "vw_full_mapping_activeonly" : "vw_active_mapping";
		return exportMappingsImpl(null, "db", fullBackup, source, true);
	}

	public String exportMapping(int mappingId) throws SQLException
	{
		return exportMappingsImpl(mappingId, "record", false, "mapping", false);
	}

	public String exportMapping(String mappingPath, boolean fullBackup) throws SQLException
	{
		return exportMappingsImpl(mappingPath, "record", fullBackup, fullBackup ? "mapping" : "vw_latest_mapping", false);
	}

	protected String exportMappingsImpl(Object mappingIdentifier, String scope, boolean fullBackup, String source, boolean preserveDatesForDeprecatedMappings) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		String				ret = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<backup type=\"" + (fullBackup ? "full" : "partial") + "\" scope=\"" + scope + "\">";
		int					defaultActionId;
		List				actions;
		Timestamp			timeStamp;
		String				buf;

		try
		{
			if (mappingIdentifier instanceof Integer)
			{
				pst = _connection.prepareStatement("SELECT * FROM " + source + " WHERE mapping_id = ? ORDER BY mapping_id");
				pst.setInt(1, (Integer)mappingIdentifier);
			}
			else
			{
				pst = _connection.prepareStatement("SELECT * FROM " + source + (mappingIdentifier == null ? "" : " WHERE mapping_path = ?") + " ORDER BY mapping_id");
				if (mappingIdentifier != null)
					pst.setString(1, (String)mappingIdentifier);
			}
			
			if (pst.execute())
			{
				for (rs = pst.getResultSet(); rs.next();)
				{
					ret += "<mapping>";
					ret += "<path>" + StringEscapeUtils.escapeXml(rs.getString("mapping_path")) + "</path>";
					ret += "<type>" + rs.getString("type") + "</type>";

					buf = rs.getString("description");
					if (buf != null)
						ret += "<description>" + StringEscapeUtils.escapeXml(buf) + "</description>";
					buf = rs.getString("creator");
					if (buf != null)
						ret += "<creator>" + StringEscapeUtils.escapeXml(buf) + "</creator>";

					// Time stamps are only applicable for full backups and deprecated records.
					if (fullBackup || !fullBackup && preserveDatesForDeprecatedMappings && rs.getTimestamp("date_end") != null)
					{
						timeStamp = rs.getTimestamp("date_start");
						if (timeStamp != null)
							ret += "<date_start>" + timeStamp + "</date_start>";
						timeStamp = rs.getTimestamp("date_end");
						if (timeStamp != null)
							ret += "<date_end>" + timeStamp + "</date_end>";
					}

					// Default action.
					defaultActionId = rs.getInt("default_action_id");
					if (!rs.wasNull())
					{
						Descriptor action = getActionsByActionId(defaultActionId);
						ret += "<action>";
						ret += "<type>" + action.Type + "</type>";
						if (action.Name != null)
							ret += "<name>" + StringEscapeUtils.escapeXml(action.Name) + "</name>";
						if (action.Value != null)
							ret += "<value>" + StringEscapeUtils.escapeXml(action.Value) + "</value>";
						ret += "</action>";
					}
					
					// Conditions.
					Vector<ConditionDescriptor> conditions = getConditions(rs.getInt("mapping_id"));
					if (conditions != null && conditions.size() > 0)
					{
						ret += "<conditions>";
						for (ConditionDescriptor condition : conditions)
						{
							ret += "<condition>";
							ret += "<type>" + condition.Type + "</type>";
							ret += "<match>" + StringEscapeUtils.escapeXml(condition.Match) + "</match>";
							

							actions = getActionsByConditionId(condition.ID);
							if (actions != null && actions.size() > 0)
							{
								ret += "<actions>";
								for (Descriptor action : actions)
								{
									ret += "<action>";
									ret += "<type>" + action.Type + "</type>";
									if (action.Name != null)
										ret += "<name>" + StringEscapeUtils.escapeXml(action.Name) + "</name>";
									if (action.Value != null)
										ret += "<value>" + StringEscapeUtils.escapeXml(action.Value) + "</value>";
									ret += "</action>";
								};
								ret += "</actions>";
							}
							ret += "</condition>";
						}
						ret += "</conditions>";
					}
					ret += "</mapping>";
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
		}
		ret += "</backup>";
		return ret;
	}
	
	public String importMappings(HttpServletRequest request)
	{
		java.util.List<FileItem> fileList = null;
		GZIPInputStream gis = null;
		try
		{
			DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();

			// Set the size threshold, above which content will be stored on disk.
			fileItemFactory.setSizeThreshold(1 * 1024 * 1024); // 1 MB
//			fileItemFactory.setSizeThreshold(100 * 1024); // 100 KB

			// Set the temporary directory to store the uploaded files of size above threshold.
			fileItemFactory.setRepository(new File(System.getProperty("java.io.tmpdir")));

			ServletFileUpload uploadHandler = new ServletFileUpload(fileItemFactory);

			@SuppressWarnings("unchecked")
			java.util.List<FileItem> items = uploadHandler.parseRequest(request);
			fileList = items;

			for (FileItem item : items)
			{
				if (item.isFormField())
					continue;

				gis = new GZIPInputStream(item.getInputStream());
//				String fileContent = Stream.readInputStream(gis);
				String ret = createMapping(gis);
				gis.close();
				
				// Process the first uploaded file only.
				return ret;
			}
		}
		catch (Exception ex)
		{
			if (ex.getMessage().equalsIgnoreCase("Not in GZIP format"))
				return "ERROR: Unknown file format.";
			else
				return "ERROR: " + ex.getMessage();
		}
		finally
		{
			try
			{
				// Close the stream;
				gis.close();
			}
			catch (Exception ex)
			{
			}
			if (fileList != null)
			{
				// Delete all uploaded files.
				for (FileItem item : fileList)
				{
					if (!item.isFormField() && !item.isInMemory())
						((DiskFileItem)item).delete();
				}
			}
		}
		return "ERROR: No file.";
	}

	public boolean purgeDataStore() throws SQLException
	{
		PreparedStatement pst = null;
		try
		{
			pst = _connection.prepareStatement("TRUNCATE TABLE \"mapping\" CASCADE;");
			return pst.execute();
		}
		finally
		{
			if (pst != null)
				pst.close();
		}
	}
}
