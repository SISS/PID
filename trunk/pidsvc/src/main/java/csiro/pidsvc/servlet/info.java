/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 *
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 *
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

package csiro.pidsvc.servlet;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import csiro.pidsvc.core.Settings;
import csiro.pidsvc.helper.Http;
import csiro.pidsvc.helper.Literals;
import csiro.pidsvc.mappingstore.ManagerJson;

/**
 * Info servlet is a read-only service API that provides informative data
 * for client applications (e.g. web-based user interface).
 *
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class info extends HttpServlet
{
	private static Logger _logger = LogManager.getLogger(info.class.getName());

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
			mgr = new ManagerJson(request);
			response.setContentType("application/json");

			_logger.info("Processing \"{}\" command -> {}?{}.", cmd, request.getRequestURL(), request.getQueryString());
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
				int mappingId = Literals.toInt(request.getParameter("mapping_id"), -1);
				String mappingPath = request.getParameter("mapping_path");

				response.getWriter().write(mappingId > 0 ? mgr.getPidConfig(mappingId) : (mappingId == 0 ? mgr.getPidConfig((String)null) : mgr.getPidConfig(mappingPath)));
			}
			else if (cmd.equalsIgnoreCase("check_mapping_path_exists"))
				response.getWriter().write(mgr.checkMappingPathExists(request.getParameter("mapping_path")));
			else if (cmd.equalsIgnoreCase("search_parent"))
			{
				int mappingId = Literals.toInt(request.getParameter("mapping_id"), -1);
				response.getWriter().write(mgr.searchParentMapping(mappingId, request.getParameter("q")));
			}
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
			else if (cmd.equalsIgnoreCase("global_js"))
			{
				response.setContentType("text/javascript");
				response.getWriter().write(mgr.getGlobalSettings(request));
			}
			else if (cmd.equalsIgnoreCase("chart"))
			{
				response.getWriter().write(mgr.getChart());
			}
			else if (cmd.equalsIgnoreCase("get_mapping_dependencies"))
			{
				int			mappingId = Literals.toInt(request.getParameter("mapping_id"), -1);
				String		mappingPath = request.getParameter("mapping_path");
				String		jsonThis = request.getParameter("json");

				if (mappingPath != null && mappingPath.isEmpty())
					mappingPath = null;
				if (jsonThis != null && jsonThis.isEmpty())
					jsonThis = null;
				response.getWriter().write(mappingId == -1 && jsonThis == null ? "{}" : mgr.getMappingDependencies((Object)(jsonThis == null ? mappingId : jsonThis), mappingPath));
			}
			else if (cmd.equalsIgnoreCase("echo"))
			{
				echo(request, response);
			}
		}
		catch (Exception e)
		{
			_logger.error(e);
			Http.returnErrorCode(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
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

	/**
	 * Echoes HTTP headers.
	 * 
	 * @param request
	 * @param response
	 * @throws IOException 
	 * @throws URISyntaxException 
	 */
	protected void echo(HttpServletRequest request, HttpServletResponse response) throws IOException, URISyntaxException
	{
		String ret = "HTTP " + request.getMethod() + " " + request.getRequestURL() + "?" + request.getQueryString() + "\n\n";

		// Retrieve HTTP headers.
		for (@SuppressWarnings("unchecked")
		Enumeration<String> header = request.getHeaderNames(); header.hasMoreElements();)
		{
		    String headerName = (String)header.nextElement();
		    ret += headerName + ": " + request.getHeader(headerName) + "\n";
		}

		response.setContentType("text/plain");
		response.getWriter().write(ret);
	}
}
