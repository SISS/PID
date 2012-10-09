package csiro.pidsvc.mappingstore;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
	
	public String getMappings(int page, String mappingPath, String type, String creator, int includeDeprecated) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		String				ret = null;
		final int			pageSize = 4;
		final String		sourceView = (includeDeprecated == 2 ? "vw_deprecated_mapping" : (includeDeprecated == 1 ? "vw_latest_mapping" : "vw_active_mapping"));

		try
		{
			String query = "";
			if (mappingPath != null && !mappingPath.isEmpty())
				query += " AND \"mapping_path\" LIKE ?";
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
				// Bind parameters twise to two almost identical queries.
				if (!mappingPath.isEmpty())
					pst.setString(i++, "%" + mappingPath + "%");
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
					", \"pages\": " + (Math.ceil(rs.getFloat(1) / pageSize)) +
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
		catch (Exception e)
		{
			ret = "{}";
			e.printStackTrace();
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
			"SELECT m.mapping_id, m.mapping_path, m.description, m.creator, m.type, CASE WHEN m.date_end IS NULL THEN 0 ELSE 1 END AS ended, a.type AS action_type, a.action_name, a.action_value\n" +
			"FROM vw_latest_mapping m\n" +
			"	LEFT OUTER JOIN \"action\" a ON a.action_id = m.default_action_id\n" +
			"WHERE mapping_path = ?";
		return getPidConfigImpl(query, mappingPath);
	}

	public String getPidConfig(int mappingId) throws SQLException
	{
		String query =
			"SELECT m.mapping_id, m.mapping_path, m.description, m.creator, m.type, CASE WHEN m.date_end IS NULL THEN 0 ELSE 1 END AS ended, a.type AS action_type, a.action_name, a.action_value\n" +
			"FROM \"mapping\" m\n" +
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
					pst = _connection.prepareStatement("SELECT mapping_id, to_char(date_start, 'DD/MM/YYYY HH24:MI') AS date_start, to_char(date_end, 'DD/MM/YYYY HH24:MI') AS date_end FROM mapping WHERE mapping_path = ? ORDER BY mapping.date_start DESC");
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
									JSONObject.toString("mapping_id", rsHistory.getInt(1)) + ", " +
									JSONObject.toString("date_start", rsHistory.getString(2)) + ", " +
									JSONObject.toString("date_end", rsHistory.getString(3)) +
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
		catch (Exception e)
		{
			ret = "{}";
			e.printStackTrace();
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
}
