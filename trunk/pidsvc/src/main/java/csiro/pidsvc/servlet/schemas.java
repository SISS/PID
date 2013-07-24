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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import csiro.pidsvc.helper.Stream;

/**
 * Schemas servlet provides access to internal XML schemas used by the service.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class schemas extends HttpServlet
{
	private static Logger _logger = LogManager.getLogger(schemas.class.getName());

	private static final long serialVersionUID = -1816378109366416815L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public schemas()
	{
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *	  response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		try
		{
			String schemaName = request.getRequestURI().replaceAll("^/[^/]+/schemas/(.+)$", "$1");
			String schemaContent = Stream.readInputStream(getClass().getResourceAsStream("../mappingstore/xsd/" + schemaName));

			if (schemaContent == null || schemaContent.isEmpty())
				response.setStatus(404);
			else
			{
				response.setContentType("text/xml");
				response.getWriter().write(schemaContent);
			}
		}
		catch (Exception e)
		{
			_logger.error(e);
			response.setStatus(404);
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
