package csiro.pidsvc.mappingstore;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.json.simple.JSONObject;

public class ManagerJson extends Manager
{
//	protected final SimpleDateFormat _sdfdb = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
//	protected final SimpleDateFormat _sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	public ManagerJson() throws NamingException, SQLException, IOException
	{
		super();
	}

	public String getSettings() throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		String				ret = null;

		try
		{
			pst = _connection.prepareStatement("SELECT * FROM configuration");
			if (pst.execute())
			{
				ret = "[";
				int i = 0;
				for (rs = pst.getResultSet(); rs.next(); ++i)
				{
					if (i > 0)
						ret += ",";

					ret += "{" +
							JSONObject.toString("name", rs.getString(1)) + ", " +
							JSONObject.toString("value", rs.getString(2)) +
						"}";
				}
				ret += "]";
			}
		}
		finally
		{
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
		}
		return ret;
	}

	public String getMappings(int page, String mappingPath, String type, String creator, int includeDeprecated) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		String				ret = null;
		final int			pageSize = 10;
		final String		sourceView = (includeDeprecated == 2 ? "vw_deprecated_mapping" : (includeDeprecated == 1 ? "vw_latest_mapping" : "vw_active_mapping"));

		try
		{
			String query = "";
			if (mappingPath != null && !mappingPath.isEmpty())
				query += " AND \"mapping_path\" ILIKE ?";
			if (type != null && !type.isEmpty())
				query += " AND \"type\" = ?";
			if (creator != null && !creator.isEmpty())
				query += " AND \"creator\" = ?";

			query =
				"SELECT COUNT(*) FROM " + sourceView + (query.isEmpty() ? "" : " WHERE " + query.substring(5)) + ";\n" +
				"SELECT mapping_id, mapping_path, description, creator, type, to_char(date_start, 'DD/MM/YYYY HH24:MI') AS date_start, to_char(date_end, 'DD/MM/YYYY HH24:MI') AS date_end FROM " + sourceView + (query.isEmpty() ? "" : " WHERE " + query.substring(5)) + " ORDER BY mapping_path LIMIT " + pageSize + " OFFSET " + ((page - 1) * pageSize) + ";";

			int i = 1;
			pst = _connection.prepareStatement(query);
			for (int j = 0; j < 2; ++j)
			{
				// Bind parameters twice to two almost identical queries.
				if (!mappingPath.isEmpty())
					pst.setString(i++, "%" + mappingPath.replace("\\", "\\\\") + "%");
				if (!type.isEmpty())
					pst.setString(i++, type);
				if (!creator.isEmpty())
					pst.setString(i++, creator);
			}
			
			if (pst.execute())
			{
				rs = pst.getResultSet();
				rs.next();
				ret = "{ \"count\": " + rs.getInt(1) +
					", \"page\": " + page +
					", \"pages\": " + ((int)Math.ceil(rs.getFloat(1) / pageSize)) +
					", \"results\": [";
				
				for (pst.getMoreResults(), rs = pst.getResultSet(), i = 0; rs.next(); ++i)
				{
					if (i > 0)
						ret += ",";

//					String dateStart = sdf.format(sdfdb.parse(rs.getString("date_start")));
//					String dateEnd = rs.getString("date_end");
//					if (dateEnd != null)
//						dateEnd = sdf.format(sdfdb.parse(dateEnd));

					ret += "{" +
							JSONObject.toString("mapping_id", rs.getString("mapping_id")) + ", " +
							JSONObject.toString("mapping_path", rs.getString("mapping_path")) + ", " +
							JSONObject.toString("description", rs.getString("description")) + ", " +
							JSONObject.toString("creator", rs.getString("creator")) + ", " +
							JSONObject.toString("type", rs.getString("type")) + ", " +
							JSONObject.toString("date_start", rs.getString("date_start")) + ", " +
							JSONObject.toString("date_end", rs.getString("date_end")) + ", " +
						"	\"date\": \"\"" +
						"}";
				}
				ret += "]}";
			}
		}
		finally
		{
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
		}
		return ret;
	}

	public String getPidConfig(String mappingPath) throws SQLException
	{
		String query =
			"SELECT m.mapping_id, m.mapping_path, m.original_path, m.description, m.creator, m.type, CASE WHEN m.date_end IS NULL THEN 0 ELSE 1 END AS ended, a.type AS action_type, a.action_name, a.action_value\n" +
			"FROM vw_latest_mapping m\n" +
			"	LEFT OUTER JOIN \"action\" a ON a.action_id = m.default_action_id\n" +
			"WHERE mapping_path = ?";
		return getPidConfigImpl(query, mappingPath);
	}

	public String getPidConfig(int mappingId) throws SQLException
	{
		String query =
			"SELECT m.mapping_id, m.mapping_path, m.original_path, m.description, m.creator, m.type, CASE WHEN m.date_end IS NULL THEN 0 ELSE 1 END AS ended, a.type AS action_type, a.action_name, a.action_value\n" +
			"FROM mapping m\n" +
			"	LEFT OUTER JOIN \"action\" a ON a.action_id = m.default_action_id\n" +
			"WHERE mapping_id = ?";
		return getPidConfigImpl(query, mappingId);
	}

	protected String getPidConfigImpl(String query, Object value) throws SQLException
	{
//		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("../pid.json");
//		byte[] bytes=new byte[inputStream.available()]; 
//		inputStream.read(bytes); 
//		String s = new String(bytes); 
//		return s;

		PreparedStatement	pst = null;
		ResultSet			rsMapping = null, rsCondition = null, rsAction = null, rsHistory = null;
		String				ret = null;
		int					i, j;

		try
		{
			pst = _connection.prepareStatement(query);
			if (value instanceof Integer)
				pst.setInt(1, (Integer)value);
			else
				pst.setString(1, (String)value);
			
			if (pst.execute())
			{
				ret = "{";
				for (rsMapping = pst.getResultSet(); rsMapping.next();)
				{
					String actionType = rsMapping.getString("action_type");
					if (rsMapping.wasNull())
						actionType = null;
					ret +=
						JSONObject.toString("mapping_id", rsMapping.getInt("mapping_id")) + ", " +
						JSONObject.toString("mapping_path", rsMapping.getString("mapping_path")) + ", " +
						JSONObject.toString("original_path", rsMapping.getString("original_path")) + ", " +
						JSONObject.toString("type", rsMapping.getString("type")) + ", " +
						JSONObject.toString("description", rsMapping.getString("description")) + ", " +
						JSONObject.toString("creator", rsMapping.getString("creator")) + ", " +
						JSONObject.toString("ended", rsMapping.getBoolean("ended")) + ", " +
						"\"action\": ";
					if (actionType == null)
						ret += "null";
					else
					{
						ret +=
							"{" +
								JSONObject.toString("type", actionType) + ", " +
								JSONObject.toString("name", rsMapping.getString("action_name")) + ", " +
								JSONObject.toString("value", rsMapping.getString("action_value")) +
							"}";
					}
					ret += ",";

					// Serialise change history.
					pst = _connection.prepareStatement("SELECT mapping_id, creator, to_char(date_start, 'DD/MM/YYYY HH24:MI') AS date_start, to_char(date_end, 'DD/MM/YYYY HH24:MI') AS date_end FROM mapping WHERE mapping_path = ? ORDER BY mapping.date_start DESC");
					pst.setString(1, rsMapping.getString("mapping_path"));

					ret += "\"history\": [";
					if (pst.execute())
					{
						for (rsHistory = pst.getResultSet(), i = 0; rsHistory.next(); ++i)
						{
							if (i > 0)
								ret += ",";
							ret +=
								"{" +
									JSONObject.toString("mapping_id", rsHistory.getInt("mapping_id")) + ", " +
									JSONObject.toString("creator", rsHistory.getString("creator")) + ", " +
									JSONObject.toString("date_start", rsHistory.getString("date_start")) + ", " +
									JSONObject.toString("date_end", rsHistory.getString("date_end")) +
								"}";
						}
					}
					ret += "],"; // history					
					
					// Serialise conditions.
					pst = _connection.prepareStatement("SELECT * FROM \"condition\" WHERE mapping_id = ? ORDER BY condition_id");
					pst.setInt(1, rsMapping.getInt("mapping_id"));

					ret += "\"conditions\": [";
					if (pst.execute())
					{
						for (rsCondition = pst.getResultSet(), i = 0; rsCondition.next(); ++i)
						{
							if (i > 0)
								ret += ",";
							ret +=
								"{" +
									JSONObject.toString("type", rsCondition.getString("type")) + ", " +
									JSONObject.toString("match", rsCondition.getString("match")) + ", " +
									"\"actions\": [";

							// Serialise actions.
							pst = _connection.prepareStatement("SELECT * FROM \"action\" WHERE condition_id = ? ORDER BY action_id");
							pst.setInt(1, rsCondition.getInt("condition_id"));
							if (pst.execute())
							{
								for (rsAction = pst.getResultSet(), j = 0; rsAction.next(); ++j)
								{
									if (j > 0)
										ret += ",";
									ret +=
										"{" +
											JSONObject.toString("type", rsAction.getString("type")) + ", " +
											JSONObject.toString("name", rsAction.getString("action_name")) + ", " +
											JSONObject.toString("value", rsAction.getString("action_value")) +
										"}";
								}
							}
							
							ret += "]"; // actions
							ret += "}"; // condition
						}
					}
					ret += "]"; // conditions
				}
				ret += "}";
			}
		}
		finally
		{
			if (rsMapping != null)
				rsMapping.close();
			if (rsCondition != null)
				rsCondition.close();
			if (rsAction != null)
				rsAction.close();
			if (rsHistory != null)
				rsHistory.close();
			if (pst != null)
				pst.close();
		}
		return ret;
	}

	public String checkMappingPathExists(String mappingPath) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		boolean				exists = false;

		try
		{
			pst = _connection.prepareStatement("SELECT 1 FROM mapping WHERE mapping_path = ?");
			pst.setString(1, mappingPath);

			if (pst.execute())
			{
				rs = pst.getResultSet();
				exists = rs.next();
			}
		}
		catch (Exception e)
		{
		}
		finally
		{
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
		}
		return "{" +
				JSONObject.toString("exists", exists) + ", " +
				JSONObject.toString("mapping_path", mappingPath) +
			"}";
	}
	
	public String getLookups(int page, String namespace) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		String				ret = null;
		final int			pageSize = 10;

		try
		{
			String query = "";
			if (namespace != null && !namespace.isEmpty())
				query += " AND ns ILIKE ?";

			query =
				"SELECT COUNT(*) FROM lookup_ns" + (query.isEmpty() ? "" : " WHERE " + query.substring(5)) + ";\n" +
				"SELECT * FROM lookup_ns" + (query.isEmpty() ? "" : " WHERE " + query.substring(5)) + " ORDER BY ns LIMIT " + pageSize + " OFFSET " + ((page - 1) * pageSize) + ";";

			int i = 1;
			pst = _connection.prepareStatement(query);
			for (int j = 0; j < 2; ++j)
			{
				// Bind parameters twice to two almost identical queries.
				if (namespace != null && !namespace.isEmpty())
					pst.setString(i++, "%" + namespace.replace("\\", "\\\\") + "%");
			}
			
			if (pst.execute())
			{
				rs = pst.getResultSet();
				rs.next();
				ret = "{ \"count\": " + rs.getInt(1) +
					", \"page\": " + page +
					", \"pages\": " + ((int)Math.ceil(rs.getFloat(1) / pageSize)) +
					", \"results\": [";
				
				for (pst.getMoreResults(), rs = pst.getResultSet(), i = 0; rs.next(); ++i)
				{
					if (i > 0)
						ret += ",";

					ret += "{" +
							JSONObject.toString("ns", rs.getString("ns")) + ", " +
							JSONObject.toString("type", rs.getString("type")) +
						"}";
				}
				ret += "]}";
			}
		}
		finally
		{
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
		}
		return ret;
	}

	public String getLookupConfig(String ns) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		String				ret = null;
		String				lookupType;

		try
		{
			pst = _connection.prepareStatement("SELECT ns, type, behaviour_type, behaviour_value FROM lookup_ns WHERE ns = ?;SELECT key, value FROM lookup WHERE ns = ?;");
			pst.setString(1, ns);
			pst.setString(2, ns);

			if (pst.execute())
			{
				ret = "{";
				rs = pst.getResultSet();
				if (rs.next())
				{
					lookupType = rs.getString("type");
					ret +=
						JSONObject.toString("ns", rs.getString("ns")) + ", " +
						JSONObject.toString("type", lookupType) + ", " +
						"\"default\":{" +
							JSONObject.toString("type", rs.getString("behaviour_type")) + ", " + 
							JSONObject.toString("value", rs.getString("behaviour_value")) + 
						"}," +
						"\"lookup\":";

					pst.getMoreResults();
					rs = pst.getResultSet();
					if (lookupType.equalsIgnoreCase("Static"))
					{
						ret += "[";
						for (int i = 0; rs.next(); ++i)
						{
							if (i > 0)
								ret += ",";
							ret +=
								"{" +
									JSONObject.toString("key", rs.getString(1)) + ", " +
									JSONObject.toString("value", rs.getString(2)) +
								"}";
						}
						ret += "]";
					}
					else if (lookupType.equalsIgnoreCase("HttpResolver"))
					{
						if (rs.next())
						{
							final Pattern		reType = Pattern.compile("^T:(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
							final Pattern		reExtract = Pattern.compile("^E:(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
							final Pattern		reNamespace = Pattern.compile("^NS:(.+?):(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
							Matcher				m;
							String				buf = rs.getString(2);

							try
							{
								String jsonPart = "{" + JSONObject.toString("endpoint", rs.getString(1)) + ", ";

								// Type.
								m = reType.matcher(buf);
								m.find();
								jsonPart += JSONObject.toString("type", m.group(1)) + ", ";

								// Extractor.
								m = reExtract.matcher(buf);
								m.find();
								jsonPart += JSONObject.toString("extractor", m.group(1)) + ", ";

								// Namespaces.
								m = reNamespace.matcher(buf);
								jsonPart += "\"namespaces\":[";
								for (int i = 0; m.find(); ++i)
								{
									if (i > 0)
										jsonPart += ",";
									jsonPart +=
										"{" +
											JSONObject.toString("prefix", m.group(1)) + ", " +
											JSONObject.toString("uri", m.group(2)) +
										"}";
								}
								jsonPart += "]";

								jsonPart += "}";
								ret += jsonPart;
							}
							catch (Exception e)
							{
								ret += "{}";
							}
						}
					}
				}
				ret += "}";
			}
		}
		finally
		{
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
		}
		return ret;
	}
}
