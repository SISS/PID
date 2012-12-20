package csiro.pidsvc.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import csiro.pidsvc.core.Settings;
import csiro.pidsvc.helper.Http;
import csiro.pidsvc.helper.Literals;
import csiro.pidsvc.mappingstore.ManagerJson;

/**
 * Servlet implementation class info
 */
public class info extends HttpServlet
{
	private static final long serialVersionUID = -2660354665193002690L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public info()
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
//			Thread.sleep(1500);
			mgr = new ManagerJson();

			if (cmd.equalsIgnoreCase("search"))
			{
				int page = 1;
				String sPage = request.getParameter("page");
				if (sPage != null && sPage.matches("\\d+"))
					page = Integer.parseInt(sPage);

				response.getWriter().write(mgr.getMappings(page, request.getParameter("mapping"), request.getParameter("type"), request.getParameter("creator"), Literals.toInt(request.getParameter("deprecated"), 0)));
			}
			else if (cmd.equalsIgnoreCase("get_pid_config"))
			{
				int mappingId = Literals.toInt(request.getParameter("mapping_id"), 0);
				String mappingPath = request.getParameter("mapping_path");

				response.getWriter().write(mappingId > 0 ? mgr.getPidConfig(mappingId) : mgr.getPidConfig(mappingPath));
			}
			else if (cmd.equalsIgnoreCase("check_mapping_path_exists"))
				response.getWriter().write(mgr.checkMappingPathExists(request.getParameter("mapping_path")));
			else if (cmd.equalsIgnoreCase("get_settings"))
				response.getWriter().write(mgr.getSettings());
			else if (cmd.equalsIgnoreCase("search_lookup"))
			{
				int page = 1;
				String sPage = request.getParameter("page");
				if (sPage != null && sPage.matches("\\d+"))
					page = Integer.parseInt(sPage);

				response.getWriter().write(mgr.getLookups(page, request.getParameter("ns")));
			}
			else if (cmd.equalsIgnoreCase("get_lookup_config"))
			{
				String ns = request.getParameter("ns");
				response.getWriter().write(mgr.getLookupConfig(ns));
			}
			else if (cmd.equalsIgnoreCase("get_manifest"))
				response.getWriter().write(Settings.getInstance().getManifestJson());
			else if (cmd.equalsIgnoreCase("is_new_version_available"))
				response.getWriter().write(Settings.getInstance().isNewVersionAvailableJson());
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
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}
}
