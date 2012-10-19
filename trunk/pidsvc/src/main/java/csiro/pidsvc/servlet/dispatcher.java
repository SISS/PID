package csiro.pidsvc.servlet;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import csiro.pidsvc.mappingstore.action.Runner;
import csiro.pidsvc.helper.Http;
import csiro.pidsvc.helper.URI;
import csiro.pidsvc.mappingstore.Manager;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

/**
 * Servlet implementation class dispatcher
 */
public class dispatcher extends HttpServlet
{
	private static final long serialVersionUID = 1L;

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
		response.setDateHeader("Expires", 0); 
		response.addHeader("Cache-Control", "no-cache,no-store,private,must-revalidate,max-stale=0,post-check=0,pre-check=0");

		///////////////////////////////////////////////////////////////////////
		//	Get and decode input URI.

		URI uri = null;
		Manager mgr = null;

		try
		{
			uri = new URI(URLDecoder.decode(request.getQueryString(), "UTF-8").replaceAll("^([^&]+)&(.+)?$", "$1?$2"));
			mgr = new Manager();
		}
		catch (URISyntaxException e)
		{
			response.setStatus(404);
			return;
		}
		catch (Exception e)
		{
			Http.returnErrorCode(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), e);
			e.printStackTrace();
			if (mgr != null)
				mgr.close();
			return;
		}
		
		///////////////////////////////////////////////////////////////////////
		//	Find a matching mapping and run action items.
		
		try
		{
			MappingMatchResults matchResult;
			URI uriNoExtension = uri.getNoExtensionURI();

			// 1-to-1 mapping. Look for an exact match.
			matchResult = mgr.findExactMatch(uri, request);
			if (!matchResult.success())
			{
				matchResult = mgr.findExactMatch(uriNoExtension, request);
				if (matchResult.success())
					uri = uriNoExtension;
			}

			// Regex-based mapping.
			if (!matchResult.success())
			{
				matchResult = mgr.findRegexMatch(uri, request);
				if (!matchResult.success())
				{
					matchResult = mgr.findRegexMatch(uriNoExtension, request);
					if (matchResult.success())
						uri = uriNoExtension;
				}
			}

			// Run actions.
			(new Runner(uri, request, response)).Run(matchResult);
		}
		catch (Exception e)
		{
			Http.returnErrorCode(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), e);
			e.printStackTrace();
			return;
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
