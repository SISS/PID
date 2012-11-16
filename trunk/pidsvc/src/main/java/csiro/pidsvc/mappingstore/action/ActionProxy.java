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

import csiro.pidsvc.helper.Http;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public class ActionProxy extends AbstractAction
{
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
			if (isTraceMode())
				trace("Set response status: 500; exception: " + e.getCause().getMessage());
			else
				Http.returnErrorCode(_controller.getResponse(), 500, e);
			e.printStackTrace();
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
