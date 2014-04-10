/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 * 
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 * 
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

package csiro.pidsvc.mappingstore;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import csiro.pidsvc.helper.JSONObjectHelper;

/**
 * ManagerJson class derived from the base Manager class encapsulates JSON
 * objects generation logic.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class ManagerJson extends Manager
{
	private static Logger _logger = LogManager.getLogger(ManagerJson.class.getName());

	private String _authorizationName = null;

//	protected final SimpleDateFormat _sdfdb = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
//	protected final SimpleDateFormat _sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	public ManagerJson() throws NamingException, SQLException, IOException
	{
		super();
	}

	public ManagerJson(HttpServletRequest request) throws NamingException, SQLException, IOException
	{
		super();

        _authorizationName = request.getRemoteUser();
	}

	public String getAuthorizationName()
	{
		return _authorizationName;
	}

	public JSONObject getGlobalSettings(HttpServletRequest request)
	{
		return JSONObjectHelper.create(
				"BaseURI",				getBaseURI(),
				"CaseSensitiveURI",		isCaseSensitive(),
				"AuthorizationName",	_authorizationName
			);
	}

	@SuppressWarnings("unchecked")
	public JSONArray getSettings() throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		JSONArray			ret = new JSONArray();

		try
		{
			pst = _connection.prepareStatement("SELECT * FROM configuration");
			if (pst.execute())
			{
				for (rs = pst.getResultSet(); rs.next();)
					ret.add(JSONObjectHelper.create("name", rs.getString(1), "value", rs.getString(2)));
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

	@SuppressWarnings("unchecked")
	public JSONObject getMappings(int page, String mappingPath, String type, String creator, int includeDeprecated) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		JSONObject			ret = new JSONObject();
		final int			pageSize = 10;
		final String		sourceView = (includeDeprecated == 2 ? "vw_deprecated_mapping" : (includeDeprecated == 1 ? "vw_latest_mapping" : "vw_active_mapping"));

		try
		{
			String query = "mapping_path IS NOT NULL";
			if (mappingPath != null && !mappingPath.isEmpty())
				query += " AND (title ILIKE ? OR mapping_path ILIKE ?)";
			if (type != null && !type.isEmpty())
				query += " AND type = ?";
			if (creator != null && !creator.isEmpty())
				query += " AND creator = ?";

			query =
				"SELECT COUNT(*) FROM " + sourceView + (query.isEmpty() ? "" : " WHERE " + query) + ";\n" +
				"SELECT mapping_id, mapping_path, title, description, creator, type, to_char(date_start, 'DD/MM/YYYY HH24:MI') AS date_start, to_char(date_end, 'DD/MM/YYYY HH24:MI') AS date_end FROM " + sourceView + (query.isEmpty() ? "" : " WHERE " + query) + " ORDER BY COALESCE(title, mapping_path) LIMIT " + pageSize + " OFFSET " + ((page - 1) * pageSize) + ";";

			pst = _connection.prepareStatement(query);

			// Bind parameters twice to two almost identical queries.
			for (int i = 1, j = 0; j < 2; ++j)
			{
				if (!mappingPath.isEmpty())
				{
					pst.setString(i++, "%" + mappingPath.replace("\\", "\\\\") + "%");
					pst.setString(i++, "%" + mappingPath.replace("\\", "\\\\") + "%");
				}
				if (!type.isEmpty())
					pst.setString(i++, type);
				if (!creator.isEmpty())
					pst.setString(i++, creator);
			}

			if (pst.execute())
			{
				rs = pst.getResultSet();
				rs.next();
				ret.put("count", rs.getInt(1));
				ret.put("page", page);
				ret.put("pages", (int)Math.ceil(rs.getFloat(1) / pageSize));

				JSONArray jsonArr = new JSONArray();
				for (pst.getMoreResults(), rs = pst.getResultSet(); rs.next();)
				{
//					String dateStart = sdf.format(sdfdb.parse(rs.getString("date_start")));
//					String dateEnd = rs.getString("date_end");
//					if (dateEnd != null)
//						dateEnd = sdf.format(sdfdb.parse(dateEnd));

					jsonArr.add(JSONObjectHelper.create(
							"mapping_id",	rs.getString("mapping_id"),
							"mapping_path",	rs.getString("mapping_path"),
							"title",		rs.getString("title"),
							"description",	rs.getString("description"),
							"creator",		rs.getString("creator"),
							"type",			rs.getString("type"),
							"date_start",	rs.getString("date_start"),
							"date_end",		rs.getString("date_end"),
							"date",			""
						));
				}
				ret.put("results", jsonArr);
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

	@SuppressWarnings("unchecked")
	public JSONArray searchParentMapping(int mappingId, String searchTerm) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		JSONArray			ret = new JSONArray();

		try
		{
			String query =
				"SELECT mapping_path, title " +
				"FROM vw_active_mapping " +
				"WHERE type = 'Regex' AND (mapping_path ILIKE ? OR title ILIKE ?) " + (mappingId > 0 ? "AND mapping_id != " + mappingId + " " : "") +
				"ORDER BY title, mapping_path " +
				"LIMIT 10;";
			searchTerm = "%" + searchTerm + "%";

			pst = _connection.prepareStatement(query);
			pst.setString(1, searchTerm);
			pst.setString(2, searchTerm);

			if (pst.execute())
			{
				for (rs = pst.getResultSet(); rs.next();)
					ret.add(JSONObjectHelper.create("mapping_path", rs.getString("mapping_path"), "title", rs.getString("title")));
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
			"SELECT m.mapping_id, m.mapping_path, m.original_path, m.title, m.description, m.creator, m.commit_note, m.type, m.default_action_description, CASE WHEN m.date_end IS NULL THEN 0 ELSE 1 END AS ended, a.type AS action_type, a.action_name, a.action_value,\n" +
			"	m.parent, vwa.title AS parent_title, CASE WHEN vwa.mapping_id IS NULL THEN 0 ELSE 1 END AS parent_is_active\n" +
			"FROM vw_latest_mapping m\n" +
			"	LEFT OUTER JOIN action a ON a.action_id = m.default_action_id\n" +
			"	LEFT OUTER JOIN vw_active_mapping vwa ON vwa.mapping_path = m.parent\n" +
			"WHERE m.mapping_path " + (mappingPath == null ? "IS NULL" : "= ?");
		return getPidConfigImpl(query, mappingPath);
	}

	public String getPidConfig(int mappingId) throws SQLException
	{
		String query =
			"SELECT m.mapping_id, m.mapping_path, m.original_path, m.title, m.description, m.creator, m.commit_note, m.type, m.default_action_description, CASE WHEN m.date_end IS NULL THEN 0 ELSE 1 END AS ended, a.type AS action_type, a.action_name, a.action_value,\n" +
			"	m.parent, vwa.title AS parent_title, CASE WHEN vwa.mapping_id IS NULL THEN 0 ELSE 1 END AS parent_is_active\n" +
			"FROM mapping m\n" +
			"	LEFT OUTER JOIN action a ON a.action_id = m.default_action_id\n" +
			"	LEFT OUTER JOIN vw_active_mapping vwa ON vwa.mapping_path = m.parent\n" +
			"WHERE m.mapping_id = ?";
		return getPidConfigImpl(query, mappingId);
	}

	protected String getPidConfigImpl(String query, Object value) throws SQLException
	{
//		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("../pid.json");
//		byte[] bytes = new byte[inputStream.available()]; 
//		inputStream.read(bytes); 
//		String s = new String(bytes); 
//		return s;

		PreparedStatement	pst = null;
		ResultSet			rsMapping = null, rsCondition = null, rsAction = null, rsHistory = null;
		String				ret = null;
		int					mappingId, i, j;
		String				mappingPath, parentPath;
		boolean				isParentActive;

		try
		{
			pst = _connection.prepareStatement(query);
			if (value != null)
			{
				if (value instanceof Integer)
					pst.setInt(1, (Integer)value);
				else
					pst.setString(1, (String)value);
			}

			if (pst.execute())
			{
				ret = "{";
				for (rsMapping = pst.getResultSet(); rsMapping.next();)
				{
					String actionType = rsMapping.getString("action_type");
					if (rsMapping.wasNull())
						actionType = null;

					mappingId		= rsMapping.getInt("mapping_id");
					mappingPath		= rsMapping.getString("mapping_path");
					parentPath		= rsMapping.getString("parent");
					isParentActive	= rsMapping.getBoolean("parent_is_active");

					ret +=
						JSONObject.toString("mapping_id", mappingId) + ", " +
						JSONObject.toString("mapping_path", mappingPath) + ", " +
						JSONObject.toString("original_path", rsMapping.getString("original_path")) + ", " +
						JSONObject.toString("type", mappingPath == null ? "Regex" : rsMapping.getString("type")) + ", " +
						JSONObject.toString("title", mappingPath == null ? "Catch-all" : rsMapping.getString("title")) + ", " +
						JSONObject.toString("description", rsMapping.getString("description")) + ", " +
						JSONObject.toString("creator", rsMapping.getString("creator")) + ", " +
						JSONObject.toString("commit_note", rsMapping.getString("commit_note")) + ", " +
						JSONObject.toString("ended", rsMapping.getBoolean("ended")) + ", " +
						JSONObject.toString("qr_hits", this.getTotalQrCodeHits(mappingPath)) + ", " +
						"\"parent\": {" +
							JSONObject.toString("mapping_path", parentPath) + ", " +
							JSONObject.toString("title", rsMapping.getString("parent_title")) + ", " +
							(isParentActive ? JSONObject.toString("cyclic", !this.checkNonCyclicInheritance(mappingId)) + ", " : "") +
							JSONObject.toString("active", isParentActive) + ", " +
							"\"graph\": " + getMappingDependencies(mappingId, parentPath) +
						"}," +
						"\"action\": ";
					if (actionType == null)
						ret += "null";
					else
					{
						ret +=
							"{" +
								JSONObject.toString("type", actionType) + ", " +
								JSONObject.toString("name", rsMapping.getString("action_name")) + ", " +
								JSONObject.toString("value", rsMapping.getString("action_value")) + ", " +
								JSONObject.toString("description", rsMapping.getString("default_action_description")) +
							"}";
					}
					ret += ",";

					// Serialise change history.
					pst = _connection.prepareStatement(
						"SELECT mapping_id, creator, commit_note, " +
						"	to_char(date_start, 'DD/MM/YYYY HH24:MI') AS date_start, " +
						"	to_char(date_end, 'DD/MM/YYYY HH24:MI') AS date_end " +
						"FROM mapping " +
						"WHERE mapping_path " + (mappingPath == null ? "IS NULL " : "= ? ") +
						"ORDER BY mapping.date_start DESC");
					if (mappingPath != null)
						pst.setString(1, mappingPath);

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
									JSONObject.toString("commit_note", rsHistory.getString("commit_note")) + ", " +
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
									JSONObject.toString("description", rsCondition.getString("description")) + ", " +
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

				if (value == null && ret.equals("{}"))
				{
					// Catch-all mapping is not defined yet. Return default.
					ret = "{" +
						JSONObject.toString("mapping_id", 0) + ", " +
						JSONObject.toString("mapping_path", null) + ", " +
						JSONObject.toString("original_path", null) + ", " +
						JSONObject.toString("type", "Regex") + ", " +
						JSONObject.toString("title", "Catch-all") + ", " +
						JSONObject.toString("description", null) + ", " +
						JSONObject.toString("creator", null) + ", " +
						JSONObject.toString("commit_note", null) + ", " +
						JSONObject.toString("ended", false) + ", " +
						JSONObject.toString("qr_hits", 0) + ", " +
						JSONObject.toString("parent", null) + ", " +
						"\"action\": {" +
							JSONObject.toString("type", "404") + ", " +
							JSONObject.toString("name", null) + ", " +
							JSONObject.toString("value", null) + ", " +
							JSONObject.toString("description", null) +
						"}," +
						"\"history\": null," + 
						"\"conditions\": []" +
					"}";
				}
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

	public JSONObject checkMappingPathExists(String mappingPath) throws SQLException
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
			_logger.debug(e);
		}
		finally
		{
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
		}
		return JSONObjectHelper.create("exists", exists, "mapping_path", mappingPath);
	}

	@SuppressWarnings("unchecked")
	public JSONObject getLookups(int page, String namespace) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		JSONObject			ret = new JSONObject();
		final int			pageSize = 20;

		try
		{
			String query = "";
			if (namespace != null && !namespace.isEmpty())
				query += " AND ns ILIKE ?";

			query =
				"SELECT COUNT(*) FROM lookup_ns" + (query.isEmpty() ? "" : " WHERE " + query.substring(5)) + ";\n" +
				"SELECT * FROM lookup_ns" + (query.isEmpty() ? "" : " WHERE " + query.substring(5)) + " ORDER BY ns LIMIT " + pageSize + " OFFSET " + ((page - 1) * pageSize) + ";";

			pst = _connection.prepareStatement(query);
			for (int i = 1, j = 0; j < 2; ++j)
			{
				// Bind parameters twice to two almost identical queries.
				if (namespace != null && !namespace.isEmpty())
					pst.setString(i++, "%" + namespace.replace("\\", "\\\\") + "%");
			}

			if (pst.execute())
			{
				rs = pst.getResultSet();
				rs.next();
				ret.put("count", rs.getInt(1));
				ret.put("page", page);
				ret.put("pages", (int)Math.ceil(rs.getFloat(1) / pageSize));

				JSONArray jsonArr = new JSONArray();
				for (pst.getMoreResults(), rs = pst.getResultSet(); rs.next();)
				{
					jsonArr.add(JSONObjectHelper.create(
							"ns",		rs.getString("ns"),
							"type",		rs.getString("type")
						));
				}
				ret.put("results", jsonArr);
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

	@SuppressWarnings("unchecked")
	public JSONObject getLookupConfig(String ns) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		JSONObject			ret = new JSONObject();
		JSONArray			jsonArr;
		String				lookupType;

		try
		{
			pst = _connection.prepareStatement("SELECT ns, type, behaviour_type, behaviour_value FROM lookup_ns WHERE ns = ?;SELECT key, value FROM lookup WHERE ns = ?;");
			pst.setString(1, ns);
			pst.setString(2, ns);

			if (pst.execute())
			{
				rs = pst.getResultSet();
				if (rs.next())
				{
					lookupType = rs.getString("type");
					ret.put("ns", rs.getString("ns"));
					ret.put("type", lookupType);
					ret.put("default", JSONObjectHelper.create("type", rs.getString("behaviour_type"), "value", rs.getString("behaviour_value")));

					pst.getMoreResults();
					rs = pst.getResultSet();
					if (lookupType.equalsIgnoreCase("Static"))
					{
						jsonArr = new JSONArray();
						while (rs.next())
						{
							jsonArr.add(JSONObjectHelper.create(
									"key",		rs.getString(1),
									"value",	rs.getString(2)
								));
						}
						ret.put("lookup", jsonArr);
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
								JSONObject jsonPart = new JSONObject();
								jsonPart.put("endpoint", rs.getString(1));

								// Type.
								m = reType.matcher(buf);
								m.find();
								jsonPart.put("type", m.group(1));

								// Extractor.
								m = reExtract.matcher(buf);
								m.find();
								jsonPart.put("extractor", m.group(1));

								// Namespaces.
								m = reNamespace.matcher(buf);
								jsonArr = new JSONArray();
								while (m.find())
								{
									jsonArr.add(JSONObjectHelper.create(
											"prefix",	m.group(1),
											"uri",		m.group(2)
										));
								}
								jsonPart.put("namespaces", jsonArr);

								ret.put("lookup", jsonPart);
							}
							catch (Exception e)
							{
								_logger.debug(e);
								return null;
							}
						}
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
		return ret;
	}

	@SuppressWarnings("unchecked")
	public JSONObject getChart() throws IOException, SQLException
	{
//		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("../chart_data.js");
//		byte[] bytes = new byte[inputStream.available()]; 
//		inputStream.read(bytes); 
//		String s = new String(bytes); 
//		return s;

		PreparedStatement	pst = null;
		ResultSet			rs = null;
		JSONObject			ret = null;

		try
		{
			pst = _connection.prepareStatement("SELECT description, creator FROM vw_active_mapping WHERE mapping_path IS NULL");
			if (pst.execute())
			{
				rs = pst.getResultSet();
				if (rs.next())
				{
					ret = new JSONObject();
					ret.put("id", 0);
					ret.put("name", "&lt;Catch-all&gt;");
					ret.put("data", JSONObjectHelper.create(
							"$type",		"star",
							"$color",		"#C72240",
							"mapping_path",	null,
							"author",		rs.getString("creator"),
							"description",	rs.getString("description")
						));
					ret.put("children", encodeChartChildren(null));
				}
			}

			// Catch-all mapping is not defined yet.
			if (ret == null)
			{
				ret = new JSONObject();
				ret.put("id", 0);
				ret.put("name", "&lt;Catch-all&gt;");
				ret.put("data", JSONObjectHelper.create(
						"$type",		"star",
						"$color",		"#C72240",
						"mapping_path",	null
					));
				ret.put("children", encodeChartChildren(null));
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

	@SuppressWarnings("unchecked")
	private JSONArray encodeChartChildren(String parent) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		JSONArray			ret = new JSONArray();
		JSONObject			jsonData;
		String				mappingPath, title;
		boolean				isOneToOne;

		try
		{
			pst = _connection.prepareStatement("SELECT mapping_path, parent, title, description, creator, type FROM vw_active_mapping WHERE mapping_path IS NOT NULL AND parent " + (parent == null ? "IS NULL" : "= ?"));
			if (parent != null)
				pst.setString(1, parent);

			if (pst.execute())
			{
				rs = pst.getResultSet();
				while (rs.next())
				{
					mappingPath		= rs.getString("mapping_path");
					title			= rs.getString("title");
					isOneToOne		= rs.getString("type").equalsIgnoreCase("1:1");

					// Construct data object.
					jsonData = JSONObjectHelper.create(
							"mapping_path",	mappingPath,
							"title",		title,
							"author",		rs.getString("creator"),
							"description",	rs.getString("description")
						);
					if (isOneToOne)
					{
						jsonData.put("$type", "square");
						jsonData.put("$color", "#bed600");
					}

					JSONObject json = new JSONObject();
					json.put("id", mappingPath);
					json.put("name", title == null ? mappingPath : title);
					json.put("data", jsonData);
					json.put("children", encodeChartChildren(mappingPath));
					ret.add(json);
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
		return ret;
	}

	public String getMappingDependencies(Object thisMapping, String parentPath) throws SQLException
	{
		Vector<String>		parentsList = new Vector<String>();
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		String				ret = null, jsonThis = "", jsonParent = null, jsonParent2 = null;
		String				mappingPath, title;
		int					mappingId, inheritors;

		try
		{
			// Get current mapping descriptor.
			if (thisMapping instanceof String)
			{
				jsonThis = (String)thisMapping;
				parentsList.add("__this");
			}
			else
			{
				mappingId = (Integer)thisMapping;
				pst = _connection.prepareStatement(
					"SELECT a.mapping_path, a.title, a.description, a.creator, " +
					"	(SELECT COUNT(1) FROM vw_active_mapping aa WHERE aa.parent = a.mapping_path) AS inheritors " +
					"FROM mapping a " +
					"WHERE a.mapping_id = ?");
				pst.setInt(1, mappingId);

				if (pst.execute())
				{
					jsonThis = "{";
					rs = pst.getResultSet();
					if (rs.next())
					{
						mappingPath		= rs.getString("mapping_path");
						title			= rs.getString("title");
						inheritors		= rs.getInt("inheritors");

						// Initial mapping ID is required for detection of cyclic inheritance.
						parentsList.add(mappingPath);

						jsonThis +=
							JSONObject.toString("id", "__this") + ", " +
							JSONObject.toString("name", title == null ? mappingPath : title) + ", " +
							"\"data\":{" +
								JSONObject.toString("mapping_path", mappingPath) + ", " + 
								JSONObject.toString("title", title) + ", " + 
								JSONObject.toString("description", rs.getString("description")) + ", " +
								JSONObject.toString("author", rs.getString("creator")) + ", " + 
								JSONObject.toString("css", "chart_label_current") + ", " + 
								JSONObject.toString("inheritors", inheritors) + 
							"}" +
							(
								inheritors == 0 ?
									""
								:
									",\"children\":[{" +
									JSONObject.toString("id", -2) + ", " + 
									JSONObject.toString("name", inheritors + " inheritor" + (inheritors == 1 ? "" : "s") + "...") + ", " + 
									"\"data\":{" +
										JSONObject.toString("css", "chart_label_hidden") + ", " + 
										JSONObject.toString("inheritors", inheritors) + 
									"}" +
								"}]"
							);
					}
					jsonThis += "}";
				}
			}

			// Get parents.
			while (parentPath != null)
			{
				pst = _connection.prepareStatement(
					"SELECT mapping_path, title, description, creator, parent " +
					"FROM vw_active_mapping " +
					"WHERE mapping_path = ? AND type = 'Regex'"
				);
				pst.setString(1, parentPath);

				parentPath = null;
				if (pst.execute())
				{
					rs = pst.getResultSet();
					if (rs.next())
					{
						mappingPath		= rs.getString("mapping_path");
						title			= rs.getString("title");
						parentPath		= rs.getString("parent");

						// Prevent cyclic inheritance syndrome.
						if (parentsList.contains(mappingPath))
						{
							jsonParent =
								"{" +
									JSONObject.toString("id", -3) + ", " +
									JSONObject.toString("name", "ERROR") + ", " +
									"\"data\":{" +
										JSONObject.toString("description", "Cyclic inheritance encountered!<br/><br/>Please inspect the inheritance chain and rectify the problem. Mappings with detected cyclic inheritance will fall back to Catch-all mapping automatically.") + ", " +
										JSONObject.toString("css", "chart_label_error") + 
									"}," +
									"\"children\":[" + jsonThis + "]" +
								"}";
							return jsonParent;
						}

						// Construct JSON for the first parent.
						if (jsonParent2 == null)
						{
							String buf =
								JSONObject.toString("id", mappingPath) + ", " +
								JSONObject.toString("name", title == null ? mappingPath : title) + ", " +
								"\"data\":{" +
									JSONObject.toString("mapping_path", mappingPath) + ", " + 
									JSONObject.toString("title", title) + ", " + 
									JSONObject.toString("description", rs.getString("description")) + ", " +
									JSONObject.toString("author", rs.getString("creator")) + 
								"}";
							if (jsonParent == null)
								jsonParent = "{" + buf + ", \"children\":[" + jsonThis + "]}";
							else if (jsonParent2 == null)
								jsonParent2 = "{" + buf + ", \"children\":[" + jsonParent + "]}";
						}

						// Add new parent to the list.
						parentsList.add(mappingPath);
					}
				}
			}
			if (jsonParent == null)
				jsonParent = jsonThis;

			// Get catch-all mapping descriptor.
			String author = null, description = null;
			int hiddenParents = parentsList.size() - 2;

			pst = _connection.prepareStatement(
				"SELECT description, creator " +
				"FROM vw_active_mapping " +
				"WHERE mapping_path IS NULL AND type = 'Regex'");
			if (pst.execute())
			{
				if ((rs = pst.getResultSet()).next())
				{
					author = rs.getString("creator");
					description = rs.getString("description");
				}
			}
			if (hiddenParents == 1)
				jsonParent = jsonParent2;
			else if (hiddenParents > 1)
			{
				jsonParent =
					"{" +
						JSONObject.toString("id", -1) + ", " +
						JSONObject.toString("name", hiddenParents + " more parents...") + ", " +
						"\"data\":{" +
							JSONObject.toString("css", "chart_label_hidden") + 
						"}," +
						"\"children\":[" + jsonParent + "]" +
					"}";
			}
			ret =
				"{" +
					JSONObject.toString("id", 0) + ", " +
					JSONObject.toString("name", "&lt;Catch-all&gt;") + ", " +
					"\"data\":{" +
						JSONObject.toString("author", author) + ", " +
						JSONObject.toString("description", description) + ", " +
						JSONObject.toString("css", "chart_label_root") + 
					"}," +
					"\"children\":[" + jsonParent + "]" +
				"}";
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
