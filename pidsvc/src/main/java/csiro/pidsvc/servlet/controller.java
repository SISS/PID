package csiro.pidsvc.servlet;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import csiro.pidsvc.helper.Http;
import csiro.pidsvc.helper.Literals;
import csiro.pidsvc.mappingstore.Manager;
import csiro.pidsvc.mappingstore.ManagerJson;

/**
 * Servlet implementation class controller
 */
public class controller extends HttpServlet
{
	private static final long serialVersionUID = -6453299989235903216L;

	protected final SimpleDateFormat _sdfBackupStamp = new SimpleDateFormat("yyyy-MM-dd.HHmmss");

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public controller()
	{
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *	  response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		response.setDateHeader("Expires", 0); 
		response.addHeader("Cache-Control", "no-cache,no-store,private,must-revalidate,max-stale=0,post-check=0,pre-check=0");

		String cmd = request.getParameter("cmd");
		if (cmd == null || cmd.isEmpty())
			return;

		ManagerJson mgr = null;
		try
		{
			mgr = new ManagerJson();
			
			if (cmd.matches("(?i)^(?:full|partial)_export$"))
			{
				int			mappingId = Literals.toInt(request.getParameter("mapping_id"), 0);
				String		mappingPath = request.getParameter("mapping_path");
				String		serializedConfig = mappingId > 0 ? mgr.exportMapping(mappingId) : mgr.exportMapping(mappingPath, cmd.startsWith("full"));

				returnAttachment(response, "mapping." + (cmd.startsWith("full") ? "full" : "partial") + "." + _sdfBackupStamp.format(new Date()) + ".psb", serializedConfig);
			}
			else if (cmd.matches("(?i)^(?:full|partial)_backup$"))
			{
				String includeDeprecated = request.getParameter("deprecated");
				String serializedConfig = mgr.backupDataStore(cmd.startsWith("full"), includeDeprecated != null && includeDeprecated.equalsIgnoreCase("true"));
				returnAttachment(response, "backup." + (cmd.startsWith("full") ? "full" : "partial") + "." + _sdfBackupStamp.format(new Date()) + ".psb", serializedConfig);
			}
			else if (cmd.matches("(?i)export_lookup$"))
			{
				String ns = request.getParameter("ns");
				String serializedConfig = mgr.exportLookup(ns);
				returnAttachment(response, "lookup." + _sdfBackupStamp.format(new Date()) + ".psl", serializedConfig);
			}
		}
		catch (Exception e)
		{
			Http.returnErrorCode(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
			e.printStackTrace();
		}
		finally
		{
			if (mgr != null)
				mgr.close();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *	  response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String cmd = request.getParameter("cmd");
		if (cmd == null || cmd.isEmpty())
			return;
		
		Manager mgr = null;
		try
		{
			// Create mapping store manager object.
			mgr = new Manager();

			if (cmd.equalsIgnoreCase("create_mapping"))
				mgr.createMapping(request.getInputStream(), false);
			else if (cmd.equalsIgnoreCase("delete_mapping"))
				mgr.deleteMapping(request.getParameter("mapping_path"));
			else if (cmd.equalsIgnoreCase("import"))
				response.getWriter().write(mgr.importMappings(request)); 
			else if (cmd.equalsIgnoreCase("purge_data_store"))
				mgr.purgeDataStore();
			else if (cmd.equalsIgnoreCase("save_settings"))
				mgr.saveSettings(request.getParameterMap());
			else if (cmd.equalsIgnoreCase("create_lookup"))
				mgr.createLookup(request.getInputStream());
			else if (cmd.equalsIgnoreCase("delete_lookup"))
				mgr.deleteLookup(request.getParameter("ns"));
			else if (cmd.equalsIgnoreCase("import_lookup"))
				response.getWriter().write(mgr.importLookup(request)); 
			else
				response.setStatus(404);
		}
		catch (Exception e)
		{
			Http.returnErrorCode(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
			e.printStackTrace();
		}
		finally
		{
			if (mgr != null)
				mgr.close();
		}
	}

	protected void returnAttachment(HttpServletResponse response, String filename, String content) throws IOException
	{
//		response.setContentType("application/xml"); response.getWriter().write(content);

		response.setContentType("application/zip");
		response.setHeader("Content-Disposition", "attachment; filename=" + filename);
		GZIPOutputStream gos = new GZIPOutputStream(response.getOutputStream());
		gos.write(content.getBytes());
		gos.close();
	}
}
