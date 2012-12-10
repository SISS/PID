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

import csiro.pidsvc.helper.Http;
import csiro.pidsvc.helper.URI;
import csiro.pidsvc.mappingstore.Manager;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;
import csiro.pidsvc.mappingstore.action.Runner;
import csiro.pidsvc.tracing.ITracer;
import csiro.pidsvc.tracing.OutputStreamTracer;

/**
 * Servlet implementation class dispatcher
 */
public class dispatcher extends HttpServlet
{
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
		response.setDateHeader("Expires", 0); 
		response.addHeader("Cache-Control", "no-cache,no-store,private,must-revalidate,max-stale=0,post-check=0,pre-check=0");

		///////////////////////////////////////////////////////////////////////
		//	Get and decode input URI.

		URI			uri = null;
		Manager		mgr = null;
		ITracer		tracer = null;

		try
		{
			String preparedUri = request.getQueryString();
			preparedUri = preparedUri.replace("%26", "%2526"); // Double escape &.
			preparedUri = URLDecoder.decode(preparedUri, "UTF-8");
			preparedUri = preparedUri.replace(" ", "+");
			preparedUri = preparedUri.replaceAll("^([^&]+)&(.+)?$", "$1?$2"); // Replace first & with ? marking the start of the query string.

			uri = new URI(preparedUri);
			mgr = new Manager();
		}
		catch (URISyntaxException ex)
		{
			Http.returnErrorCode(response, HttpServletResponse.SC_NOT_FOUND, ex);
			return;
		}
		catch (Exception ex)
		{
			Http.returnErrorCode(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
			ex.printStackTrace();
			if (mgr != null)
				mgr.close();
			return;
		}

		///////////////////////////////////////////////////////////////////////
		//	Get tracer settings.

		if (!uri.isQrCodeRequest() && mgr.getSetting("DispatcherTracingMode").equalsIgnoreCase("1"))
		{
			tracer = new OutputStreamTracer(response.getOutputStream());
			tracer.trace("Dispatch " + uri.getOriginalUriAsString());
		}

		///////////////////////////////////////////////////////////////////////
		//	Find a matching mapping and run action items.

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
		catch (Exception ex)
		{
			Http.returnErrorCode(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
			ex.printStackTrace();
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
