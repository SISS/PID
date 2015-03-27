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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.xml.XMLConstants;
import javax.xml.bind.ValidationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XdmAtomicValue;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import net.sf.saxon.s9api.XsltCompiler;
import net.sf.saxon.s9api.XsltExecutable;
import net.sf.saxon.s9api.XsltTransformer;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

import csiro.pidsvc.core.Settings;
import csiro.pidsvc.helper.Http;
import csiro.pidsvc.helper.JSONObjectHelper;
import csiro.pidsvc.helper.Stream;
import csiro.pidsvc.helper.URI;
import csiro.pidsvc.mappingstore.action.List;
import csiro.pidsvc.mappingstore.condition.AbstractCondition;
import csiro.pidsvc.mappingstore.condition.ConditionContentType;
import csiro.pidsvc.mappingstore.condition.ConditionQrCodeRequest;
import csiro.pidsvc.mappingstore.condition.SpecialConditionType;

/**
 * Manager class encapsulates application/database interaction logic.
 * 
 * @author Pavel Golodoniuc, CSIRO Mineral Resources Flagship
 */
public class Manager
{
	private static Logger	_logger = LogManager.getLogger(Manager.class.getName());

	protected Connection	_connection = null;
	private String 			_authorizationName = null;

	public class MappingMatchResults
	{
		public static final int NULL = -1;

		public final int MappingId;
		public final int DefaultActionId;
		public final AbstractCondition Condition;
		public final Object AuxiliaryData;

		public MappingMatchResults(int mappingId, int defaultActionId, AbstractCondition condition, Object auxiliaryData)
		{
			MappingId = mappingId;
			DefaultActionId = defaultActionId;
			Condition = condition;
			AuxiliaryData = auxiliaryData;
		}

		public boolean success()
		{
			return MappingId != MappingMatchResults.NULL || DefaultActionId != MappingMatchResults.NULL || Condition != null || AuxiliaryData != null;
		}
	}

	public class MappingParentDescriptor
	{
		public final int MappingId;
		public final String MappingPath;
		public final int DefaultActionId;
		public final Object Aux;

		public MappingParentDescriptor(int mappingId, String mappingPath, int defaultActionId, Object aux)
		{
			MappingId = mappingId;
			MappingPath = mappingPath;
			DefaultActionId = defaultActionId;
			Aux = aux;
		}
	}

	public class MappingParentDescriptorList extends Vector<MappingParentDescriptor>
	{
		private static final long serialVersionUID = 8137846998774342633L;

	    public MappingParentDescriptorList(int initialCapacity)
	    {
	    	super(initialCapacity);
	    }

		public boolean contains(int mappingId)
		{
			for (MappingParentDescriptor it : this)
			{
				if (it.MappingId == mappingId)
					return true;
			}
			return false;
		}

		public MappingParentDescriptor getFirstDefaultActionMapping()
		{
			for (MappingParentDescriptor it : this)
			{
				if (it.DefaultActionId != MappingMatchResults.NULL)
					return it;
			}
			return null;
		}
	}

	protected interface ICallback
	{
		public String process(InputStream inputStream) throws Exception;
	}

	/**************************************************************************
	 *  Case-sensitivity 'structure' class and properties.
	 */

	protected class CaseSensitivity
	{
		public boolean IsCaseSensitive = true;
		public int RegularExpressionFlags = 0;
	}

	protected CaseSensitivity _caseSensitivity = new CaseSensitivity();

	protected CaseSensitivity getCaseSensitivity()
	{
		return _caseSensitivity;
	}

	protected void refreshCaseSensitivity()
	{
		String caseSensitive = getSetting("CaseSensitiveURI");
		_caseSensitivity.IsCaseSensitive = caseSensitive != null && caseSensitive.equalsIgnoreCase("1");
		_caseSensitivity.RegularExpressionFlags = _caseSensitivity.IsCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
	}

	/**************************************************************************
	 *  Construction/destruction.
	 */

	public Manager() throws NamingException, SQLException, IOException
	{
		InitialContext initCtx = new InitialContext();
		Context envCtx = (Context)initCtx.lookup("java:comp/env");
		DataSource ds = (DataSource)envCtx.lookup(Settings.getInstance().getProperty("jndiReferenceName"));
		_connection = ds.getConnection();
		refreshCaseSensitivity();
	}

	public Manager(HttpServletRequest request) throws NamingException, SQLException, IOException
	{
		this();

		// Try to retrieve authentication details using Java API.
        _authorizationName = request.getRemoteUser();

        // If it fails try to read 'authorization' HTTP header directly.
        if (_authorizationName == null)
        {
	        String authHeader = request.getHeader("authorization");
	        if (authHeader != null && !authHeader.isEmpty() && authHeader.startsWith("Basic"))
	        {
	        	// Extract user name from basic authentication HTTP header.
	        	authHeader = authHeader.substring(authHeader.indexOf(' '));
	        	authHeader = StringUtils.newStringUtf8(Base64.decodeBase64(authHeader));
	        	_authorizationName = authHeader.substring(0, authHeader.indexOf(':'));
	        }
        }
	}

	public void close()
	{
		try
		{
			_connection.close();
		}
		catch (SQLException e)
		{
			_logger.error("Closing connection failed.", e);
		}
		finally
		{
			_connection = null;
		}
	}

	/**************************************************************************
	 *  Authentication/authorisation methods.
	 */

	public String getAuthorizationName()
	{
		return _authorizationName;
	}

	/**************************************************************************
	 *  Generic processing methods.
	 */

	protected void validateRequest(String inputData, String xmlSchemaResourcePath) throws IOException, ValidationException
	{
		try
		{
			SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			schemaFactory.setResourceResolver(new LSResourceResolver() {
				@Override
				public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI)
				{
					return new XsdSchemaResolver(type, namespaceURI, publicId, systemId, baseURI);
				}
			});

			Schema schema = schemaFactory.newSchema(new StreamSource(getClass().getResourceAsStream(xmlSchemaResourcePath)));
			Validator validator = schema.newValidator();
			_logger.trace("Validating XML Schema.");
			validator.validate(new StreamSource(new StringReader(inputData))); 
		}
		catch (SAXException ex)
		{
			_logger.debug("Unknown format.", ex);
			throw new ValidationException("Unknown format.", ex);
		}
	}

	protected String processGenericXmlCommand(InputStream inputStream, String xmlSchemaResourcePath, String xsltResourcePath) throws SaxonApiException, SQLException, IOException, ValidationException
	{
		return processGenericXmlCommand(Stream.readInputStream(inputStream), xmlSchemaResourcePath, xsltResourcePath);
	}

	protected String processGenericXmlCommand(String inputData, String xmlSchemaResourcePath, String xsltResourcePath) throws SaxonApiException, SQLException, IOException, ValidationException
	{
		// Validate request.
		if (xmlSchemaResourcePath != null)
			validateRequest(inputData, xmlSchemaResourcePath);

		// Generate SQL query.
		Processor			processor = new Processor(false);
		XsltCompiler		xsltCompiler = processor.newXsltCompiler();
		InputStream			inputSqlGen = getClass().getResourceAsStream(xsltResourcePath);
		XsltExecutable		xsltExec = xsltCompiler.compile(new StreamSource(inputSqlGen));
		XsltTransformer		transformer = xsltExec.load();

		StringWriter swSqlQuery = new StringWriter();
		transformer.setInitialContextNode(processor.newDocumentBuilder().build(new StreamSource(new StringReader(inputData))));
		transformer.setDestination(new Serializer(swSqlQuery));
		transformer.setParameter(new QName("AuthorizationName"), new XdmAtomicValue(getAuthorizationName()));
		_logger.trace("Generating SQL query.");
		transformer.transform();

		// Update mappings in the database.
		Statement st = null;
		try
		{
			String sqlQuery = swSqlQuery.toString();
			st = _connection.createStatement();
			st.execute(sqlQuery);

			Pattern re = Pattern.compile("^--((?:OK|ERROR): .*)", Pattern.CASE_INSENSITIVE);
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

	@SuppressWarnings("unchecked")
	protected String unwrapCompressedBackupFile(HttpServletRequest request, ICallback callback)
	{
		java.util.List<FileItem>	fileList = null;
		GZIPInputStream				gis = null;
		String						ret = null;

		try
		{
			DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();

			// Set the size threshold, above which content will be stored on disk.
			fileItemFactory.setSizeThreshold(1 * 1024 * 1024); // 1 MB
//			fileItemFactory.setSizeThreshold(100 * 1024); // 100 KB

			// Set the temporary directory to store the uploaded files of size above threshold.
			fileItemFactory.setRepository(new File(System.getProperty("java.io.tmpdir")));

			ServletFileUpload uploadHandler = new ServletFileUpload(fileItemFactory);

			fileList = uploadHandler.parseRequest(request);
			for (FileItem item : fileList)
			{
				if (item.isFormField())
					continue;

				try
				{
					// Try to restore the backup file as it was in binary format.
					gis = new GZIPInputStream(item.getInputStream());
					ret = callback.process(gis);
					gis.close();
				}
				catch (IOException ex)
				{
					String msg = ex.getMessage();
					if (msg != null && msg.equalsIgnoreCase("Not in GZIP format"))
					{
						// Try to restore the backup file as it was unzipped.
						ret = callback.process(item.getInputStream());
					}
					else
						throw ex; 
				}

				// Process the first uploaded file only.
				return ret;
			}
		}
		catch (Exception ex)
		{
			String msg = ex.getMessage();
			Throwable linkedException = ex.getCause();
			_logger.warn(msg);
			if (linkedException != null)
				_logger.warn(linkedException.getMessage());
			if (msg != null && msg.equalsIgnoreCase("Not in GZIP format"))
				return "ERROR: Unknown file format.";
			else
				return "ERROR: " + (msg == null ? "Something went wrong." : msg + (linkedException == null ? "" : " " + linkedException.getMessage()));
		}
		finally
		{
			try
			{
				// Close the stream.
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
		_logger.trace("No file found.");
		return "ERROR: No file.";
	}

	/**************************************************************************
	 *  URI Mappings.
	 */

	public String createMapping(InputStream inputStream, boolean isBackup) throws SaxonApiException, SQLException, IOException, ValidationException
	{
		return createMapping(Stream.readInputStream(inputStream), isBackup);
	}

	public String createMapping(String inputData, boolean isBackup) throws SaxonApiException, SQLException, IOException, ValidationException
	{
		String ret = processGenericXmlCommand(inputData, "xsd/" + (isBackup ? "backup" : "mapping") + ".xsd", "xslt/import_mapping_postgresql.xslt");
		if (isBackup)
		{
			boolean noMappings = ret.equalsIgnoreCase("OK: No mapping rules found in the backup.");
			String otherRestoreStatus = "";
			
			// Restore condition sets.
			String retSubset = createConditionSet(inputData);
			if (retSubset.startsWith("OK: Success"))
				otherRestoreStatus += "\n" + retSubset;

			// Restore lookup maps.
			retSubset = createLookup(inputData);
			if (retSubset.startsWith("OK: Success"))
				otherRestoreStatus += "\n" + retSubset;

			if (noMappings)
				ret = otherRestoreStatus.substring(1);
			else
				ret += otherRestoreStatus;
		}
		return ret;
	}

	public JSONObject mergeMappingByPath(String mappingPath, String inputData, boolean replace)
	{
		try
		{
			String targetMapping = exportMapping(mappingPath, false).replaceAll("<\\?xml.*?\\?>", "");
			return mergeMappingImpl(targetMapping, inputData, replace);
		}
		catch (Exception e)
		{
			return JSONObjectHelper.create("error", "ERROR: " + e.getMessage());
		}
	}

	public JSONObject mergeMappingImpl(String targetMapping, String inputData, boolean replace)
	{
		try
		{
			// Validate request.
			validateRequest(targetMapping, "xsd/backup.xsd");
			validateRequest(inputData, "xsd/backup.xsd");

			// Generate merged mapping.
			String 				xsltInput = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><merge><target>" + targetMapping + "</target><source>" + inputData.replaceAll("<\\?xml.*?\\?>", "") + "</source></merge>";
			Processor			processor = new Processor(false);
			XsltCompiler		xsltCompiler = processor.newXsltCompiler();
			InputStream			inputSqlGen = getClass().getResourceAsStream("xslt/merge.xslt");
			XsltExecutable		xsltExec = xsltCompiler.compile(new StreamSource(inputSqlGen));
			XsltTransformer		transformer = xsltExec.load();

			StringWriter swMergedMapping = new StringWriter();
			transformer.setInitialContextNode(processor.newDocumentBuilder().build(new StreamSource(new StringReader(xsltInput))));
			transformer.setDestination(new Serializer(swMergedMapping));
			transformer.setParameter(new QName("", "replace"), new XdmAtomicValue(replace ? 1 : 0));
			_logger.trace("Generating merged mapping.");
			transformer.transform();

			// Process merged mapping.
			String				mergedMapping = swMergedMapping.toString();
			XPathCompiler		xpathCompiler = processor.newXPathCompiler();
			XdmNode				mergedXml = processor.newDocumentBuilder().build(new StreamSource(new StringReader(mergedMapping)));
			XdmItem 			node = xpathCompiler.evaluateSingle("/response/merged/*[1]", mergedXml);

			if (node != null)
			{
				String retCreateMapping = createMapping(node.toString(), false);
				_logger.trace(retCreateMapping);
				if (retCreateMapping.startsWith("OK:"))
				{
					XdmValue log = xpathCompiler.evaluate("/response/log/condition", mergedXml);
					String conditionFlags = "";

					// Get the list of conditions that have changed.
					for (XdmSequenceIterator iter = log.iterator(); iter.hasNext(); conditionFlags += iter.next().getStringValue());

					// Return JSON object.
					return JSONObjectHelper.create("status", retCreateMapping, "conditionChangeFlags", conditionFlags);
				}
				else
				{
					// Error has occurred.
					return JSONObjectHelper.create("error", retCreateMapping);
				}
			}

			node = xpathCompiler.evaluateSingle("/error", mergedXml);
			if (node != null)
				return JSONObjectHelper.create("error", node.getStringValue());
			return JSONObjectHelper.create("error", "ERROR: Unexpected exception has occurred. No data has been changed.");
		}
		catch (Exception e)
		{
			return JSONObjectHelper.create("error", "ERROR: " + e.getMessage());
		}
	}

	public boolean deleteMapping(String mappingPath) throws SQLException
	{
		PreparedStatement pst = null;
		try
		{
			pst = _connection.prepareStatement("UPDATE mapping SET date_end = now() WHERE mapping_path = ? AND date_end IS NULL;");
			pst.setString(1, mappingPath);
			return pst.execute();
		}
		finally
		{
			if (pst != null)
				pst.close();
		}
	}

	public MappingParentDescriptor getCatchAllDescriptor() throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		int					defaultActionId = MappingMatchResults.NULL;

		try
		{
			pst = _connection.prepareStatement("SELECT mapping_id, default_action_id FROM vw_active_mapping WHERE mapping_path IS NULL AND type = 'Regex'");
			if (pst.execute())
			{
				rs = pst.getResultSet();
				if (rs.next())
				{
					defaultActionId = rs.getInt(2);
					if (rs.wasNull())
						defaultActionId = MappingMatchResults.NULL;
					return new MappingParentDescriptor(rs.getInt(1), null, defaultActionId, true);
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
		return null;
	}

	public MappingParentDescriptorList getParents(int mappingId, URI uri) throws SQLException
	{
		MappingParentDescriptorList	ret = new MappingParentDescriptorList(1);
		PreparedStatement			pst = null;
		ResultSet					rs = null;
		int							parentId;
		int							defaultActionId = MappingMatchResults.NULL;
		String 						mappingPath, parentPath = null, mappingType = null;
		Object						aux = null;

		try
		{
			pst = _connection.prepareStatement("SELECT mapping_path, parent, type, default_action_id FROM vw_active_mapping WHERE mapping_id = ? AND mapping_path IS NOT NULL");
			pst.setInt(1, mappingId);

			// Get initial mapping.
			if (pst.execute())
			{
				rs = pst.getResultSet();
				if (rs.next())
				{
					mappingPath		= rs.getString(1);
					parentPath		= rs.getString(2);
					mappingType		= rs.getString(3);

					defaultActionId = rs.getInt(4);
					if (rs.wasNull())
						defaultActionId = MappingMatchResults.NULL;

					aux = mappingType.equalsIgnoreCase("1:1") ? true : Pattern.compile(mappingPath, getCaseSensitivity().RegularExpressionFlags);
					ret.add(new MappingParentDescriptor(mappingId, mappingPath, defaultActionId, aux));
				}
			}
			rs.close();
			pst.close();
			rs = null;
			pst = null;

			// Get parents.
			while (parentPath != null)
			{
				pst = _connection.prepareStatement("SELECT mapping_id, mapping_path, parent, default_action_id FROM vw_active_mapping WHERE mapping_path = ? AND type = 'Regex'");
				pst.setString(1, parentPath);

				parentPath = null;
				if (pst.execute())
				{
					rs = pst.getResultSet();
					if (rs.next())
					{
						parentId		= rs.getInt(1);
						mappingPath		= rs.getString(2);
						parentPath		= rs.getString(3);

						defaultActionId = rs.getInt(4);
						if (rs.wasNull())
							defaultActionId = MappingMatchResults.NULL;

						// Prevent cyclic inheritance syndrome.
						if (ret.contains(parentId))
							break;

						// Check that parent pattern matches URI.
						Pattern re = Pattern.compile(mappingPath, getCaseSensitivity().RegularExpressionFlags);
						Matcher m = re.matcher(uri.getPathNoExtension());
						if (!m.find())
							break;

						// Add new parent to the list.
						ret.add(new MappingParentDescriptor(parentId, rs.getString(2), defaultActionId, re));
					}
				}
			}

			// Get catch-all mapping descriptor.
			MappingParentDescriptor catchAll = getCatchAllDescriptor();
			if (catchAll != null)
				ret.add(catchAll);
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

	public boolean checkNonCyclicInheritance(int mappingId) throws SQLException
	{
		Vector<Integer>				ret = new Vector<Integer>(1);
		PreparedStatement			pst = null;
		ResultSet					rs = null;
		int							parentId;
		String 						parentPath = null;

		try
		{
			pst = _connection.prepareStatement("SELECT parent FROM vw_active_mapping WHERE mapping_id = ? AND mapping_path IS NOT NULL");
			pst.setInt(1, mappingId);

			// Get initial mapping.
			if (pst.execute())
			{
				rs = pst.getResultSet();
				if (rs.next())
				{
					parentPath = rs.getString(1);
					ret.add(mappingId);
				}
			}
			rs.close();
			pst.close();
			rs = null;
			pst = null;

			// Get parents.
			while (parentPath != null)
			{
				pst = _connection.prepareStatement("SELECT mapping_id, parent FROM vw_active_mapping WHERE mapping_path = ? AND type = 'Regex'");
				pst.setString(1, parentPath);

				parentPath = null;
				if (pst.execute())
				{
					rs = pst.getResultSet();
					if (rs.next())
					{
						parentId		= rs.getInt(1);
						parentPath		= rs.getString(2);

						// Cyclic inheritance encountered.
						if (ret.contains(parentId))
							return false;

						// Add new parent to the list.
						ret.add(parentId);
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
		return true;
	}

	public MappingMatchResults findExactMatch(URI uri, HttpServletRequest request) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException
	{
		PreparedStatement pst = null;
		try
		{
			if (isCaseSensitive())
			{
				pst = _connection.prepareStatement("SELECT mapping_id, mapping_path, type FROM vw_active_mapping WHERE mapping_path = ? AND type = '1:1'");
				pst.setString(1, uri.getPathNoExtension());
			}
			else
			{
				pst = _connection.prepareStatement("SELECT mapping_id, mapping_path, type FROM vw_active_mapping WHERE LOWER(mapping_path) = ? AND type = '1:1'");
				pst.setString(1, uri.getPathNoExtension().toLowerCase());
			}
			return findMatchImpl(pst, uri, request, false);
		}
		finally
		{
			if (pst != null)
				pst.close();
		}
	}

	public MappingMatchResults findRegexMatch(URI uri, HttpServletRequest request) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException
	{
		PreparedStatement pst = null;
		try
		{
			/* Recursive SQL query ensures the pattern based matching starts from
			 * the deepest level up to Catch-All mapping.
			 */
			pst = _connection.prepareStatement(
				"WITH RECURSIVE F (mapping_id, mapping_path, type, level)\n" +
				"AS\n" +
				"(\n" +
				"	SELECT a.mapping_id, a.mapping_path, a.type, 0\n" +
				"	FROM vw_active_mapping a\n" +
				"	WHERE a.type = 'Regex' AND a.mapping_path IS NOT NULL AND a.parent IS NULL\n" +
				"	UNION ALL\n" +
				"	SELECT a.mapping_id, a.mapping_path, a.type, level + 1\n" +
				"	FROM vw_active_mapping a\n" +
				"		INNER JOIN F ON F.mapping_path = a.parent\n" +
				"	WHERE a.type = 'Regex' AND a.mapping_path IS NOT NULL\n" +
				")\n" +
				"SELECT mapping_id, mapping_path, type FROM F ORDER BY level DESC"
			);
			return findMatchImpl(pst, uri, request, true);
		}
		finally
		{
			if (pst != null)
				pst.close();
		}		
	}

	private MappingMatchResults findMatchImpl(PreparedStatement pst, URI uri, HttpServletRequest request, boolean patternBased) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException
	{
		ResultSet 					rs = null;
		int							mappingId = MappingMatchResults.NULL;
		int							defaultActionId = MappingMatchResults.NULL;
		AbstractCondition			retCondition = null;
		Object						matchAuxiliaryData = null;
		Pattern						re;
		Matcher						m;

		MappingParentDescriptorList	parents = null;
		boolean						isOneToOne = false;

		try
		{
			if (pst.execute())
			{
				rs = pst.getResultSet();
				while (rs.next())
				{
					// Check pattern match for pattern based mappings.
					if (!(isOneToOne = rs.getString("type").equalsIgnoreCase("1:1")))
					{
						re = Pattern.compile(rs.getString("mapping_path"), getCaseSensitivity().RegularExpressionFlags);
						m = re.matcher(uri.getPathNoExtension());
						if (!m.find())
							continue;
					}

					// Get mapping hierarchy.
					parents = getParents(rs.getInt("mapping_id"), uri);

					// Break the loop once first matching pattern is found.
					break;
				}

				// If there're no matching pattern based mappings were found then resort to Catch-all.
				if (patternBased && parents == null)
				{
					MappingParentDescriptor catchAll = getCatchAllDescriptor();
					parents = new MappingParentDescriptorList(1);
					if (catchAll != null)
						parents.add(catchAll);
				}

				// Iterate through the inheritance tree up to Catch-All mapping to find a matching condition.
				if (parents != null)
				{
					for (MappingParentDescriptor parent : parents)
					{
						// Find matching condition.
						retCondition = getCondition(parent.MappingId, uri, request, parent.Aux);
						if (retCondition != null)
						{
							mappingId = parent.MappingId;
							matchAuxiliaryData = parent.Aux;
							break;
						}
					}

					// Iterate through the inheritance tree up to Catch-All mapping to find a default action.
					if (retCondition == null)
					{
						MappingParentDescriptor defaultActionMapping = parents.getFirstDefaultActionMapping();
						if (defaultActionMapping != null)
						{
							mappingId			= defaultActionMapping.MappingId;
							defaultActionId		= defaultActionMapping.DefaultActionId;
							matchAuxiliaryData	= defaultActionMapping.Aux;
						}
						else if (isOneToOne)
						{
							// Set a flag that one-to-one mapping has been found but neither matching conditions nor
							// default actions defined.
							matchAuxiliaryData = true; 
						}
					}
				}
			} //- pst.execute()
		}
		finally
		{
			if (rs != null)
				rs.close();
		}
		return new MappingMatchResults(mappingId, defaultActionId, retCondition, matchAuxiliaryData);
	}

	private Vector<csiro.pidsvc.mappingstore.condition.Descriptor> getConditionsByMappingId(int mappingId) throws SQLException
	{
		return getConditionsImpl("mapping_id", mappingId);
	}

	private Vector<csiro.pidsvc.mappingstore.condition.Descriptor> getConditionsBySetId(int conditionSetId) throws SQLException
	{
		return getConditionsImpl("condition_set_id", conditionSetId);
	}

	private Vector<csiro.pidsvc.mappingstore.condition.Descriptor> getConditionsBySetName(String conditionSetName) throws SQLException
	{
		return getConditionsImpl("condition_set_id", getConditionSetId(conditionSetName));
	}

	private Vector<csiro.pidsvc.mappingstore.condition.Descriptor> getConditionsImpl(String searchField, int id) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet 			rs = null;

		try
		{
			pst = _connection.prepareStatement("SELECT * FROM condition WHERE " + searchField + " = ? ORDER BY condition_id");
			pst.setInt(1, id);

			if (!pst.execute())
				return null;

			// Get list of conditions.
			Vector<csiro.pidsvc.mappingstore.condition.Descriptor> conditions = new Vector<csiro.pidsvc.mappingstore.condition.Descriptor>();
			for (rs = pst.getResultSet(); rs.next(); conditions.add(new csiro.pidsvc.mappingstore.condition.Descriptor(rs.getInt("condition_id"), rs.getString("type"), rs.getString("match"), rs.getString("description"))));
			return conditions;
		}
		catch (Exception e)
		{
			_logger.error(e);
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

	protected AbstractCondition getCondition(int mappingId, URI uri, HttpServletRequest request, Object matchAuxiliaryData) throws SQLException
	{
		// QR Code request.
		if (uri.isQrCodeRequest())
			return new ConditionQrCodeRequest(uri, request);

		// Get matching condition.
		return getMatchingCondition(getConditionsByMappingId(mappingId), uri, request, matchAuxiliaryData);
	}

	protected AbstractCondition getMatchingCondition(Vector<csiro.pidsvc.mappingstore.condition.Descriptor> conditions, URI uri, HttpServletRequest request, Object matchAuxiliaryData) throws SQLException
	{
		if (conditions == null)
			return null;

		try
		{
			Vector<ConditionContentType> prioritizedConditions = null;
			for (csiro.pidsvc.mappingstore.condition.Descriptor descriptor : conditions)
			{
				/*
				 * Once ContentType condition is encountered process all
				 * ContentType conditions in one go as a group.
				 * 
				 * Skip if ContentType conditions have already been processed.
				 */
				if (prioritizedConditions == null && descriptor.Type.equalsIgnoreCase("ContentType"))
				{
					// Extract all ContentType conditions.
					prioritizedConditions = new Vector<ConditionContentType>();
					for (csiro.pidsvc.mappingstore.condition.Descriptor dctr : conditions)
					{
						if (dctr.Type.equalsIgnoreCase("ContentType"))
						{
							Class<?> impl = Class.forName("csiro.pidsvc.mappingstore.condition.Condition" + dctr.Type);
							Constructor<?> ctor = impl.getDeclaredConstructor(URI.class, HttpServletRequest.class, int.class, String.class, Object.class);
							prioritizedConditions.add((ConditionContentType)ctor.newInstance(uri, request, dctr.ID, dctr.Match, matchAuxiliaryData));
						}
					}

					// Find matching conditions.
					AbstractCondition matchingCondition = ConditionContentType.getMatchingCondition(prioritizedConditions);
					if (matchingCondition != null)
						return matchingCondition;

					// Continue if no matching conditions were found.
					continue;
				}

				/*
				 * Special handling for ConditionSet type.
				 */
				if (descriptor.Type.equalsIgnoreCase("ConditionSet"))
				{
					// Block using a ConditionSet within another ConditionSet.
					if (Thread.currentThread().getStackTrace()[2].getMethodName().equalsIgnoreCase("getMatchingCondition"))
						continue;

					// Get conditions from the set.
					AbstractCondition matchingCondition = getMatchingCondition(getConditionsBySetName(descriptor.Match), uri, request, matchAuxiliaryData);
					if (matchingCondition != null)
					{
						matchingCondition.Set = descriptor.Match;
						return matchingCondition;
					}

					// Continue if no matching conditions in the subset were found.
					continue;
				}

				// Process all other conditions.
				Class<?> impl = Class.forName("csiro.pidsvc.mappingstore.condition.Condition" + descriptor.Type);
				Constructor<?> ctor = impl.getDeclaredConstructor(URI.class, HttpServletRequest.class, int.class, String.class, Object.class);

				AbstractCondition condition = (AbstractCondition)ctor.newInstance(uri, request, descriptor.ID, descriptor.Match, matchAuxiliaryData);
				if (condition.matches())
					return condition;
			}
		}
		catch (Exception e)
		{
			_logger.error(e);
		}
		return null;
	}

	public csiro.pidsvc.mappingstore.action.Descriptor getAction(int actionId) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;

		try
		{
			pst = _connection.prepareStatement("SELECT * FROM action WHERE action_id = ?");
			pst.setInt(1, actionId);

			rs = pst.executeQuery();
			rs.next();
			return new csiro.pidsvc.mappingstore.action.Descriptor(rs.getString("type"), rs.getString("action_name"), rs.getString("action_value"));
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

		switch (conditionId)
		{
			case SpecialConditionType.QR_CODE_REQUEST:
				actions.add(new csiro.pidsvc.mappingstore.action.Descriptor("QrCode", null, null));
				break;
			default:
			{
				try
				{
					pst = _connection.prepareStatement("SELECT * FROM action WHERE condition_id = ?");
					pst.setInt(1, conditionId);

					rs = pst.executeQuery();
					while (rs.next())
					{
						actions.add(new csiro.pidsvc.mappingstore.action.Descriptor(rs.getString("type"), rs.getString("action_name"), rs.getString("action_value")));
					}
				}
				finally
				{
					if (rs != null)
						rs.close();
					if (pst != null)
						pst.close();
				}
			}
		}
		return actions;
	}

	/**************************************************************************
	 *  Export/import.
	 */

	public String backupDataStore(boolean fullBackup, boolean includeDeprecated, boolean includeConditionSets, boolean includeLookupMaps) throws SQLException
	{
		String source;
		if (includeDeprecated)
			source = fullBackup ? "mapping" : "vw_latest_mapping";
		else
			source = fullBackup ? "vw_full_mapping_activeonly" : "vw_active_mapping";
		return exportMappingsImpl(null, "db", fullBackup, source, true, includeConditionSets, includeLookupMaps);
	}

	public String exportMapping(int mappingId) throws SQLException
	{
		return exportMappingsImpl(mappingId, "record", false, "mapping", false, false, false);
	}

	public String exportMapping(String mappingPath, boolean fullBackup) throws SQLException
	{
		if (mappingPath == null || mappingPath.isEmpty())
			return exportCatchAllMapping(fullBackup);
		return exportMappingsImpl(mappingPath, "record", fullBackup, fullBackup ? "mapping" : "vw_latest_mapping", false, false, false);
	}

	public String exportCatchAllMapping(boolean fullBackup) throws SQLException
	{
		return exportMappingsImpl(0, "record", fullBackup, fullBackup ? "mapping" : "vw_latest_mapping", false, false, false);
	}

	protected String exportMappingsImpl(Object mappingIdentifier, String scope, boolean fullBackup, String source, boolean preserveDatesForDeprecatedMappings, boolean includeConditionSets, boolean includeLookupMaps) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;
		String				ret = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<backup type=\"" + (fullBackup ? "full" : "partial") + "\" scope=\"" + scope + "\" xmlns=\"urn:csiro:xmlns:pidsvc:backup:1.0\">";
		int					defaultActionId;
		Timestamp			timeStamp;
		String				buf, path;

		try
		{
			if (mappingIdentifier instanceof Integer)
			{
				if ((Integer)mappingIdentifier == 0)
				{
					// Catch all mapping.
					pst = _connection.prepareStatement("SELECT * FROM " + source + " WHERE mapping_path IS NULL ORDER BY mapping_id");
				}
				else
				{
					pst = _connection.prepareStatement("SELECT * FROM " + source + " WHERE mapping_id = ? ORDER BY mapping_id");
					pst.setInt(1, (Integer)mappingIdentifier);
				}
			}
			else
			{
				// Export mapping by path or all mappings.
				pst = _connection.prepareStatement("SELECT * FROM " + source + (mappingIdentifier == null ? "" : " WHERE mapping_path = ?") + " ORDER BY mapping_id");
				if (mappingIdentifier != null)
					pst.setString(1, (String)mappingIdentifier);
			}

			if (pst.execute())
			{
				for (rs = pst.getResultSet(); rs.next();)
				{
					path = rs.getString(mappingIdentifier instanceof Integer || !fullBackup ? "original_path" : "mapping_path");

					ret += "<mapping";

					// Time stamps are only applicable for full backups and deprecated records.
					if (fullBackup || !fullBackup && preserveDatesForDeprecatedMappings && rs.getTimestamp("date_end") != null)
					{
						timeStamp = rs.getTimestamp("date_start");
						if (timeStamp != null)
							ret += " date_start=\"" + timeStamp.toString().replace(" ", "T") + "Z\"";
						timeStamp = rs.getTimestamp("date_end");
						if (timeStamp != null)
							ret += " date_end=\"" + timeStamp.toString().replace(" ", "T") + "Z\"";
					}

					// Preserve original mapping path for full backups.
					if (fullBackup && path != null)
						ret += " original_path=\"" + rs.getString("original_path") + "\"";

					ret += ">";	// mapping

					ret += (path == null ? "<path/>" : "<path>" + StringEscapeUtils.escapeXml(path) + "</path>");
					buf = rs.getString("parent");
					if (buf != null)
						ret += "<parent>" + StringEscapeUtils.escapeXml(buf) + "</parent>";
					ret += "<type>" + rs.getString("type") + "</type>";
					buf = rs.getString("title");
					if (buf != null)
						ret += "<title>" + StringEscapeUtils.escapeXml(buf) + "</title>";
					buf = rs.getString("description");
					if (buf != null)
						ret += "<description>" + StringEscapeUtils.escapeXml(buf) + "</description>";
					buf = rs.getString("creator");
					if (buf != null)
						ret += "<creator>" + StringEscapeUtils.escapeXml(buf) + "</creator>";
					buf = rs.getString("commit_note");
					if (buf != null)
						ret += "<commitNote>" + StringEscapeUtils.escapeXml(buf) + "</commitNote>";

					// Default action.
					defaultActionId = rs.getInt("default_action_id");
					if (!rs.wasNull())
					{
						csiro.pidsvc.mappingstore.action.Descriptor action = getAction(defaultActionId);
						ret += "<action>";
						ret += "<type>" + action.Type + "</type>";
						if (action.Name != null)
							ret += "<name>" + StringEscapeUtils.escapeXml(action.Name) + "</name>";
						if (action.Value != null)
							ret += "<value>" + StringEscapeUtils.escapeXml(action.Value) + "</value>";
						buf = rs.getString("default_action_description");
						if (buf != null)
							ret += "<description>" + StringEscapeUtils.escapeXml(buf) + "</description>";
						ret += "</action>";
					}

					// Conditions.
					ret += exportConditionsByMappingId(rs.getInt("mapping_id"));

					ret += "</mapping>";
				}
			}

			// Condition sets.
			if (includeConditionSets)
				ret += exportConditionSetImpl(null);

			// Lookup maps.
			if (includeLookupMaps)
				ret += exportLookupImpl(null);
		}
		catch (Exception e)
		{
			_logger.error(e);
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

	protected String exportConditionsByMappingId(int mappingId) throws SQLException
	{
		return exportConditions(getConditionsByMappingId(mappingId));
	}

	protected String exportConditionsBySetId(int conditionSetId) throws SQLException
	{
		return exportConditions(getConditionsBySetId(conditionSetId));
	}

	protected String exportConditions(Vector<csiro.pidsvc.mappingstore.condition.Descriptor> conditions) throws SQLException
	{
		String		ret = "";
		List		actions;

		if (conditions != null && conditions.size() > 0)
		{
			ret += "<conditions>";
			for (csiro.pidsvc.mappingstore.condition.Descriptor condition : conditions)
			{
				ret += "<condition>";
				ret += "<type>" + condition.Type + "</type>";
				ret += "<match>" + StringEscapeUtils.escapeXml(condition.Match) + "</match>";
				if (condition.Description != null)
					ret += "<description>" + StringEscapeUtils.escapeXml(condition.Description) + "</description>";

				actions = getActionsByConditionId(condition.ID);
				if (actions != null && actions.size() > 0)
				{
					ret += "<actions>";
					for (csiro.pidsvc.mappingstore.action.Descriptor action : actions)
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
		return ret;
	}

	public String importMappings(HttpServletRequest request)
	{
		return unwrapCompressedBackupFile(request, new ICallback() {
			@Override
			public String process(InputStream inputStream) throws Exception
			{
				return createMapping(inputStream, true);
			}
		});
	}

	public String mergeMappingUpload(HttpServletRequest request)
	{
		final String mappingPath	= request.getParameter("mapping_path");
		final String replace		= request.getParameter("replace");

		return unwrapCompressedBackupFile(request, new ICallback() {
			@Override
			public String process(InputStream inputStream) throws Exception
			{
				String inputData = Stream.readInputStream(inputStream);
				return mergeMappingByPath(mappingPath, inputData, "1".equals(replace)).toString();
			}
		});
	}

	public boolean purgeDataStore() throws SQLException
	{
		PreparedStatement pst = null;
		try
		{
			pst = _connection.prepareStatement("TRUNCATE TABLE mapping RESTART IDENTITY CASCADE;");
			return pst.execute();
		}
		finally
		{
			if (pst != null)
				pst.close();
		}
	}

	/**************************************************************************
	 *  Settings.
	 */

	public boolean saveSettings(Map<?, ?> settings) throws SQLException
	{
		String sqlQuery = "BEGIN;\n", value;
		HashMap<String, String> params = new HashMap<String, String>(); 
		for (Object key : settings.keySet())
		{
			if (key.equals("cmd"))
				continue;
			value = ((String[])settings.get(key))[0];
			if (key.equals("BaseURI"))
			{
				try
				{
					// Leave base URI only.
					java.net.URI uri = new java.net.URI(value);
					if (uri.getScheme() == null || uri.getHost() == null)
						value = "";
					else
						value = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == 80 || uri.getPort() == -1 ? "" : ":" + uri.getPort());
				}
				catch (URISyntaxException e)
				{
					continue;
				}
			}
			params.put((String)key, value);
			sqlQuery += "UPDATE configuration SET value = ? WHERE name = ?;\n";
		}
		sqlQuery += "COMMIT;";

		// Execute SQL query.
		PreparedStatement pst = null;
		try
		{
			pst = _connection.prepareStatement(sqlQuery);

			// Set query parameters.
			int i = 1;
			for (String key : params.keySet())
			{
				pst.setString(i++, params.get(key));
				pst.setString(i++, key);
			}

			return pst.execute();
		}
		finally
		{
			if (pst != null)
				pst.close();
		}
	}

	public String getSetting(String name)
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;

		try
		{
			pst = _connection.prepareStatement("SELECT value FROM configuration WHERE name = ?");
			pst.setString(1, name);

			if (pst.execute())
			{
				rs = pst.getResultSet();
				if (rs.next())
					return rs.getString(1);
			}
		}
		catch (Exception e)
		{
			_logger.error(e);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (pst != null)
					pst.close();
			}
			catch (SQLException e)
			{
				_logger.error(e);
			}
		}
		return null;
	}

	public String getBaseURI()
	{
		String baseURI = getSetting("BaseURI");
		if (baseURI == null)
		{
			try
			{
				// Execute SQL query.
				PreparedStatement pst = null;
				try
				{
					pst = _connection.prepareStatement(
							"BEGIN;\n" +
							"DELETE FROM configuration WHERE name = 'BaseURI';\n" +
							"INSERT INTO configuration (name, value) VALUES ('BaseURI', '');\n" +
							"COMMIT;"
						);
					pst.execute();
					return "";
				}
				finally
				{
					if (pst != null)
						pst.close();
				}
			}
			catch (SQLException e)
			{
				_logger.error("Saving BaseURI setting failed.", e);
			}
		}
		return baseURI;
	}

	public boolean isCaseSensitive()
	{
		return _caseSensitivity.IsCaseSensitive;
	}

	/**************************************************************************
	 *  Condition sets.
	 */

	public String createConditionSet(InputStream inputStream) throws SaxonApiException, SQLException, IOException, ValidationException
	{
		return createConditionSet(Stream.readInputStream(inputStream));
	}

	public String createConditionSet(String inputData) throws SaxonApiException, SQLException, IOException, ValidationException
	{
		return processGenericXmlCommand(inputData, "xsd/backup.xsd", "xslt/import_conditionSet_postgresql.xslt");
	}

	public boolean deleteConditionSet(String name) throws SQLException
	{
		PreparedStatement pst = null;
		try
		{
			pst = _connection.prepareStatement("DELETE FROM condition_set WHERE name = ?;");
			pst.setString(1, name);
			return pst.execute();
		}
		finally
		{
			if (pst != null)
				pst.close();
		}
	}

	public int getConditionSetId(String name) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;

		try
		{
			pst = _connection.prepareStatement("SELECT condition_set_id FROM condition_set WHERE name = ?;");
			pst.setString(1, name);

			if (pst.execute())
			{
				rs = pst.getResultSet();
				if (rs.next())
					return rs.getInt("condition_set_id");
			}
			return -1;
		}
		finally
		{
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
		}
	}

	public String importConditionSet(HttpServletRequest request)
	{
		return unwrapCompressedBackupFile(request, new ICallback() {
			@Override
			public String process(InputStream inputStream) throws Exception
			{
				return createConditionSet(inputStream);
			}
		});
	}

	public String exportConditionSet(String name) throws SQLException
	{
		String ret = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		if (name == null)
			ret += "<backup xmlns=\"urn:csiro:xmlns:pidsvc:backup:1.0\">" + exportConditionSetImpl(null) + "</backup>";
		else
			ret += exportConditionSetImpl(name);
		return ret;
	}

	protected String exportConditionSetImpl(String name) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null, rsMap = null;
		String				ret = "";

		try
		{
			if (name == null)
			{
				// Export all condition sets.
				pst = _connection.prepareStatement("SELECT * FROM condition_set;");
			}
			else
			{
				// Export a particular condition set.
				pst = _connection.prepareStatement("SELECT * FROM condition_set WHERE name = ?;");
				pst.setString(1, name);
			}

			if (pst.execute())
			{
				rs = pst.getResultSet();
				boolean dataAvailable = rs.next();

				// Backups may be empty. Otherwise throw an exception.
				if (name != null && !dataAvailable)
					throw new SQLException("Condition set cannot be exported. Data may be corrupted.");

				if (dataAvailable)
				{
					do
					{
						String setName = rs.getString("name");
						String setDesc = rs.getString("description");

						ret += "<conditionSet xmlns=\"urn:csiro:xmlns:pidsvc:backup:1.0\">";
						ret += "<name>" + StringEscapeUtils.escapeXml(setName) + "</name>";

						if (setDesc != null && !setDesc.isEmpty())
							ret += "<description>" + StringEscapeUtils.escapeXml(setDesc) + "</description>";

						ret += exportConditionsBySetId(rs.getInt("condition_set_id"));
						ret += "</conditionSet>";
					}
					while (rs.next());
				}
			}
		}
		finally
		{
			if (rsMap != null)
				rsMap.close();
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
		}
		return ret;
	}

	/**************************************************************************
	 *  Lookup maps.
	 */

	public String createLookup(InputStream inputStream) throws SaxonApiException, SQLException, IOException, ValidationException
	{
		return createLookup(Stream.readInputStream(inputStream));
	}

	public String createLookup(String inputData) throws SaxonApiException, SQLException, IOException, ValidationException
	{
		return processGenericXmlCommand(inputData, "xsd/backup.xsd", "xslt/import_lookup_postgresql.xslt");
	}

	public boolean deleteLookup(String ns) throws SQLException
	{
		PreparedStatement pst = null;
		try
		{
			pst = _connection.prepareStatement("DELETE FROM lookup_ns WHERE ns = ?;");
			pst.setString(1, ns);
			return pst.execute();
		}
		finally
		{
			if (pst != null)
				pst.close();
		}
	}

	public LookupMapDescriptor getLookupMapType(String ns)
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;

		try
		{
			pst = _connection.prepareStatement("SELECT type, behaviour_type, behaviour_value FROM lookup_ns WHERE ns = ?");
			pst.setString(1, ns);

			if (pst.execute())
			{
				rs = pst.getResultSet();
				if (rs.next())
					return new LookupMapDescriptor(rs.getString(1), rs.getString(2), rs.getString(3));
			}
		}
		catch (Exception e)
		{
			_logger.error(e);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (pst != null)
					pst.close();
			}
			catch (SQLException e)
			{
				_logger.error(e);
			}
		}
		return null;
	}

	public String getLookupValue(String ns, String key)
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;

		try
		{
			pst = _connection.prepareStatement("SELECT value FROM lookup WHERE ns = ? AND key = ? LIMIT 1");
			pst.setString(1, ns);
			pst.setString(2, key);

			if (pst.execute())
			{
				rs = pst.getResultSet();
				if (rs.next())
					return rs.getString(1);
			}
		}
		catch (Exception e)
		{
			_logger.error(e);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (pst != null)
					pst.close();
			}
			catch (SQLException e)
			{
				_logger.error(e);
			}
		}
		return null;
	}

	protected String[] getLookupKeyValue(String ns)
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null;

		try
		{
			pst = _connection.prepareStatement("SELECT key, value FROM lookup WHERE ns = ? LIMIT 1");
			pst.setString(1, ns);

			if (pst.execute())
			{
				rs = pst.getResultSet();
				if (rs.next())
					return new String[] { rs.getString(1), rs.getString(2) };
			}
		}
		catch (Exception e)
		{
			_logger.error(e);
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
				if (pst != null)
					pst.close();
			}
			catch (SQLException e)
			{
				_logger.error(e);
			}
		}
		return null;
	}

	public String resolveLookupValue(String ns, String key)
	{
		LookupMapDescriptor lookupDescriptor = getLookupMapType(ns);
		if (lookupDescriptor == null)
			return null;

		try
		{
			if (lookupDescriptor.isStatic())
			{
				String ret = getLookupValue(ns, key);
				return ret == null ? lookupDescriptor.getDefaultValue(key) : ret;
			}
			else if (lookupDescriptor.isHttpResolver())
			{
				final Pattern		reType = Pattern.compile("^T:(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
				final Pattern		reExtract = Pattern.compile("^E:(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
				final Pattern		reNamespace = Pattern.compile("^NS:(.+?):(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
				Matcher				m;
				String				endpoint, extractorType, extractor, content;
				String[]			dynKeyValue = getLookupKeyValue(ns);

				if (dynKeyValue == null)
					return null;

				// Endpoint.
				endpoint = dynKeyValue[0];
				if (endpoint.contains("$0"))
					endpoint = endpoint.replace("$0", key);
				else
					endpoint += key;

				// Type.
				m = reType.matcher(dynKeyValue[1]);
				m.find();
				extractorType = m.group(1);

				// Extractor.
				m = reExtract.matcher(dynKeyValue[1]);
				m.find();
				extractor = m.group(1);

				// Execute HTTP GET request.
				content = Http.simpleGetRequestStrict(endpoint);
				if (content == null)
					return lookupDescriptor.getDefaultValue(key);

				// Retrieve data.
				if (extractor.equals(""))
					return content;
				if (extractorType.equalsIgnoreCase("Regex"))
				{
					Pattern re = Pattern.compile(extractor);
					m = re.matcher(content);

					if (m.find())
						return m.groupCount() > 0 ? m.group(1) : m.group();
				}
				else if (extractorType.equalsIgnoreCase("XPath"))
				{
					Processor			processor = new Processor(false);
					XPathCompiler		xpathCompiler = processor.newXPathCompiler();

					// Declare XML namespaces.
					m = reNamespace.matcher(dynKeyValue[1]);
					while (m.find())
						xpathCompiler.declareNamespace(m.group(1), m.group(2));

					// Evaluate XPath expression.
					XdmItem node = xpathCompiler.evaluateSingle(extractor, processor.newDocumentBuilder().build(new StreamSource(new StringReader(content))));
					return node == null ? lookupDescriptor.getDefaultValue(key) : node.getStringValue();
				}
			}
		}
		catch (Exception e)
		{
			_logger.error(e);
		}
		return lookupDescriptor.getDefaultValue(key);
	}

	public String exportLookup(String ns) throws SQLException
	{
		String ret = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		if (ns == null)
			ret += "<backup xmlns=\"urn:csiro:xmlns:pidsvc:backup:1.0\">" + exportLookupImpl(null) + "</backup>";
		else
			ret += exportLookupImpl(ns);
		return ret;
	}

	protected String exportLookupImpl(String ns) throws SQLException
	{
		PreparedStatement	pst = null;
		ResultSet			rs = null, rsMap = null;
		String				ret = "";

		try
		{
			if (ns == null)
			{
				// Export all lookup maps.
				pst = _connection.prepareStatement("SELECT ns, type, behaviour_type, behaviour_value FROM lookup_ns;");
			}
			else
			{
				// Export a particular lookup map.
				pst = _connection.prepareStatement("SELECT ns, type, behaviour_type, behaviour_value FROM lookup_ns WHERE ns = ?;");
				pst.setString(1, ns);
			}

			if (pst.execute())
			{
				rs = pst.getResultSet();
				boolean dataAvailable = rs.next();

				// Backups may be empty. Otherwise throw an exception.
				if (ns != null && !dataAvailable)
					throw new SQLException("Lookup map configuration cannot be exported. Data may be corrupted.");

				if (dataAvailable)
				{
					do
					{
						String lookupNamespace = rs.getString("ns");
						String lookupType = rs.getString("type");

						ret += "<lookup xmlns=\"urn:csiro:xmlns:pidsvc:backup:1.0\">";
						ret += "<ns>" + StringEscapeUtils.escapeXml(lookupNamespace) + "</ns>";

						String behaviourValue = rs.getString("behaviour_value");
						ret += "<default type=\"" + StringEscapeUtils.escapeXml(rs.getString("behaviour_type")) + "\">" + (behaviourValue == null ? "" : StringEscapeUtils.escapeXml(behaviourValue)) + "</default>";

						pst = _connection.prepareStatement("SELECT key, value FROM lookup WHERE ns = ?;");
						pst.setString(1, lookupNamespace);
						if (!pst.execute())
							throw new SQLException("Lookup map configuration cannot be exported. Data may be corrupted.");
						rsMap = pst.getResultSet();
						if (lookupType.equalsIgnoreCase("Static"))
						{
							ret += "<Static>";
							while (rsMap.next())
							{
								ret += "<pair>";
								ret += "<key>" + StringEscapeUtils.escapeXml(rsMap.getString(1)) + "</key>";
								ret += "<value>" + StringEscapeUtils.escapeXml(rsMap.getString(2)) + "</value>";
								ret += "</pair>";
							}
							ret += "</Static>";
						}
						else if (lookupType.equalsIgnoreCase("HttpResolver"))
						{
							ret += "<HttpResolver>";
							if (!rsMap.next())
								throw new SQLException("Lookup map configuration cannot be exported. Data is corrupted.");

							final Pattern		reType = Pattern.compile("^T:(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
							final Pattern		reExtract = Pattern.compile("^E:(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
							final Pattern		reNamespace = Pattern.compile("^NS:(.+?):(.+)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
							Matcher				m;
							String				namespaces = "";
							String				buf = rsMap.getString(2);

							ret += "<endpoint>" + StringEscapeUtils.escapeXml(rsMap.getString(1)) + "</endpoint>";

							// Type.
							m = reType.matcher(buf);
							m.find();
							ret += "<type>" + m.group(1) + "</type>";

							// Extractor.
							m = reExtract.matcher(buf);
							m.find();
							ret += "<extractor>" + StringEscapeUtils.escapeXml(m.group(1)) + "</extractor>";

							// Namespaces.
							m = reNamespace.matcher(buf);
							while (m.find())
								namespaces += "<ns prefix=\"" + StringEscapeUtils.escapeXml(m.group(1)) + "\">" + StringEscapeUtils.escapeXml(m.group(2)) + "</ns>";
							if (!namespaces.isEmpty())
								ret += "<namespaces>" + namespaces + "</namespaces>";

							ret += "</HttpResolver>";
						}
						ret += "</lookup>";
					}
					while (rs.next());
				}
			}
		}
		finally
		{
			if (rsMap != null)
				rsMap.close();
			if (rs != null)
				rs.close();
			if (pst != null)
				pst.close();
		}
		return ret;
	}

	public String importLookup(HttpServletRequest request)
	{
		return unwrapCompressedBackupFile(request, new ICallback() {
			@Override
			public String process(InputStream inputStream) throws Exception
			{
				return createLookup(inputStream);
			}
		});
	}

	/**************************************************************************
	 *  QR Codes.
	 */

	public boolean increaseQrCodeHitCounter(int mappingId) throws SQLException
	{
		PreparedStatement pst = null;
		try
		{
			pst = _connection.prepareStatement("UPDATE mapping SET qr_hits = qr_hits + 1 WHERE mapping_id = ?;");
			pst.setInt(1, mappingId);
			return pst.execute();
		}
		finally
		{
			if (pst != null)
				pst.close();
		}
	}

	public int getTotalQrCodeHits(String mappingPath) throws SQLException
	{
		PreparedStatement pst = null;
		try
		{
			pst = _connection.prepareStatement("SELECT SUM(qr_hits) FROM mapping WHERE mapping_path = ?;");
			pst.setString(1, mappingPath);
			if (pst.execute())
			{
				ResultSet rs = pst.getResultSet();
				if (rs.next())
					return rs.getInt(1);
			}
		}
		finally
		{
			if (pst != null)
				pst.close();
		}
		return 0;
	}
}
