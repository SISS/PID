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

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import csiro.pidsvc.core.Settings;
import csiro.pidsvc.helper.Http;
import csiro.pidsvc.helper.URI;
import csiro.pidsvc.mappingstore.Manager;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;
import csiro.pidsvc.mappingstore.action.Runner;
import csiro.pidsvc.tracing.ITracer;
import csiro.pidsvc.tracing.OutputStreamTracer;

/**
 * Dispatcher servlet handles incoming URI requests.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class dispatcher extends HttpServlet
{
	private static Logger _logger = LogManager.getLogger(dispatcher.class.getName());

	private static final long serialVersionUID = 1975810034384367312L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public dispatcher()
	{
		super();
	}

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException
	{
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *	  response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
		response.setHeader("Cache-Control", "no-cache, no-store, private, must-revalidate, max-stale=0, post-check=0, pre-check=0"); // HTTP 1.1.
		response.setDateHeader("Expires", 0); // Proxies.

		///////////////////////////////////////////////////////////////////////
		//	Get and decode input URI.

		URI			uri = null;
		Manager		mgr = null;
		ITracer		tracer = null;

		try
		{
			Settings.init(this);
			uri = new URI(URI.prepareURI(request.getQueryString()));
			mgr = new Manager();
		}
		catch (URISyntaxException e)
		{
			_logger.error("URI syntax exception has occurred.", e);
			Http.returnErrorCode(response, HttpServletResponse.SC_NOT_FOUND, e);
			return;
		}
		catch (Exception e)
		{
			_logger.error(e);
			Http.returnErrorCode(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
			if (mgr != null)
				mgr.close();
			return;
		}

		///////////////////////////////////////////////////////////////////////
		//	Get tracer settings.

		String tracingMode = mgr.getSetting("DispatcherTracingMode");
		if (!uri.isQrCodeRequest() && (tracingMode != null && tracingMode.equalsIgnoreCase("1") || uri.isTraceMode()))
		{
			tracer = new OutputStreamTracer(response.getOutputStream());
			tracer.trace("Dispatch " + uri.getOriginalUriAsString());
			tracer.trace("Case-" + (mgr.isCaseSensitive() ? "sensitive" : "insensitive") + " search");
		}

		///////////////////////////////////////////////////////////////////////
		//	Find a matching mapping and run action items.

		_logger.info("Resolving {}", uri.getOriginalUriAsString());
		_logger.info("Case-" + (mgr.isCaseSensitive() ? "sensitive" : "insensitive") + " search");
		try
		{
			MappingMatchResults matchResult;
			URI uriNoExtension = uri.getNoExtensionURI();

			// 1-to-1 mapping. Look for an exact match.
			if (tracer != null)
				tracer.trace("Find exact match: " + uri.getPathNoExtension() + (uri.getExtension() == null ? "" : " [." + uri.getExtension() + "]"));
			matchResult = mgr.findExactMatch(uri, request);
			if (!matchResult.success() && uri != uriNoExtension)
			{
				if (tracer != null)
					tracer.trace("Find exact match: " + uriNoExtension.getPathNoExtension());
				matchResult = mgr.findExactMatch(uriNoExtension, request);
				if (matchResult.success())
					uri = uriNoExtension;
			}

			// Regex-based mapping.
			if (!matchResult.success())
			{
				if (tracer != null)
					tracer.trace("Find regex match: " + uriNoExtension.getPathNoExtension());
				matchResult = mgr.findRegexMatch(uriNoExtension, request);
				if (matchResult.success())
					uri = uriNoExtension;
			}

			// Tracing information.
			if (tracer != null)
			{
				tracer.trace("Match found: " + matchResult.success());
				if (matchResult.success())
				{
					tracer.trace("\tDefault action ID: " + matchResult.DefaultActionId);
					tracer.trace("\tCondition: " + matchResult.Condition);
					tracer.trace("\tAuxiliaryData: " + matchResult.AuxiliaryData);
					tracer.trace("Run actions.");
				}
			}

			// Run actions.
			(new Runner(uri, request, response, tracer)).Run(matchResult);
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
}
