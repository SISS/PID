/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 * 
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 * 
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

package csiro.pidsvc.mappingstore.action;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import csiro.pidsvc.helper.Http;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

/**
 * Proxy HTTP request.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class ActionProxy extends AbstractAction
{
	private static Logger _logger = LogManager.getLogger(ActionProxy.class.getName());

	public ActionProxy(Runner controller, Descriptor descriptor, MappingMatchResults matchResult)
	{
		super(controller, descriptor, matchResult);
	}

	@Override
	public void run()
	{
		HttpClient httpClient = new DefaultHttpClient();
		try
		{
			HttpServletRequest		originalHttpRequest = _controller.getRequest();
			HttpServletResponse		originalHttpResponse = _controller.getResponse();
			HttpGet					httpGet = new HttpGet(getExpandedActionValue());

			if (isTraceMode())
				trace(httpGet.getRequestLine().toString());

			// Pass-through HTTP headers.
			HashMap<String, String> hmHeaders = _controller.getHttpHeaders();
			for (String header : hmHeaders.keySet())
			{
				httpGet.addHeader(header, hmHeaders.get(header));
				if (isTraceMode())
					trace("\t" + header + ": " + hmHeaders.get(header));
			}

			// Handle X-Original-URI HTTP header.
			if (!hmHeaders.containsKey("X-Original-URI"))
			{
				String originalUri = originalHttpRequest.getScheme() + "://" + originalHttpRequest.getServerName();
				if (originalHttpRequest.getServerPort() != 80)
					originalUri += ":" + originalHttpRequest.getServerPort();
				originalUri += _controller.getUri().getOriginalUriAsString();

				httpGet.addHeader("X-Original-URI", originalUri);
				if (isTraceMode())
					trace("\tX-Original-URI: " + originalUri);
			}

			// Get the data.
			HttpResponse response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			if (isTraceMode())
				trace(response.getStatusLine().toString());

			// Pass HTTP headers through.
			if (!isTraceMode())
				originalHttpResponse.setStatus(response.getStatusLine().getStatusCode());
			if (entity.getContentType() != null)
			{
				if (isTraceMode())
				{
					trace("\tContent-Type: " + entity.getContentType().getValue());
					trace("\tContent-Length: " + EntityUtils.toString(entity).getBytes().length);
				}
				else
					originalHttpResponse.setContentType(entity.getContentType().getValue());
			}

			String headerName;
			for (Header header : response.getAllHeaders())
			{
				headerName = header.getName(); 
				if (headerName.equalsIgnoreCase("Expires") ||
					headerName.equalsIgnoreCase("Cache-Control") ||
					headerName.equalsIgnoreCase("Content-Type") ||
					headerName.equalsIgnoreCase("Set-Cookie") ||
					headerName.equalsIgnoreCase("Transfer-Encoding"))
					continue;
				if (isTraceMode())
					trace("\t" + header.getName() + ": " + header.getValue());
				else
					originalHttpResponse.addHeader(header.getName(), header.getValue());
			}

			// Pass content through.
			if (!isTraceMode())
				originalHttpResponse.getWriter().write(EntityUtils.toString(entity));
		}
		catch (Exception e)
		{
			_logger.trace("Exception occurred while proxying HTTP request.", e);
			if (isTraceMode())
			{
				Throwable cause = e.getCause();
				trace("Set response status: 500; exception: " + (cause == null ? e.getMessage() : cause.getMessage()));
			}
			else
				Http.returnErrorCode(_controller.getResponse(), 500, e);
		}
		finally
		{
			httpClient.getConnectionManager().shutdown();
		}
	}

	@Override
	public void trace()
	{
		run();
	}
}
