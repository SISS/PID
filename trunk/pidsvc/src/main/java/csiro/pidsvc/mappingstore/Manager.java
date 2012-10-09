package csiro.pidsvc.mappingstore;

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
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	
	public void createMapping(InputStream inputStream) throws SaxonApiException, SQLException
	{
		// Generate SQL query.
		Processor			processor = new Processor(false);
		XsltCompiler		xsltCompiler = processor.newXsltCompiler();
		InputStream			inputSqlGen = getClass().getResourceAsStream("xslt/execute_cmd_postgresql.xslt");
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
	        st = _connection.createStatement();
	        st.execute(swSqlQuery.toString());
        }
        finally
        {
            if (st != null)
                st.close();
        }
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
	
	protected AbstractCondition getCondition(int mappingId, URI uri, HttpServletRequest request) throws SQLException
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
			rs.close();
			rs = null;

			// Test conditions.
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
		finally
		{
            if (rs != null)
                rs.close();
            if (pst != null)
            	pst.close();
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
		List			actions = new List();
		
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
}
