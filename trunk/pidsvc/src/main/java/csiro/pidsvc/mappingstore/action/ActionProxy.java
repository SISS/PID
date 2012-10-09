package csiro.pidsvc.mappingstore.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import csiro.pidsvc.mappingstore.action.Runner;
import csiro.pidsvc.helper.Http;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public class ActionProxy extends AbstractAction
{
	@Override
	public void run(Runner controller, Descriptor actionDescriptor, MappingMatchResults matchResult)
	{
		HttpClient httpClient = new DefaultHttpClient();
		try
		{
			HttpServletRequest		originalHttpRequest = controller.getRequest();
			HttpServletResponse		originalHttpResponse = controller.getResponse();
			HttpGet					httpGet = new HttpGet(substrituteCaptureParameters(controller.getUri().getPathNoExtension(), actionDescriptor, matchResult));
			
			// Pass-through HTTP headers.
			for (String header : controller.getHttpHeaders().keySet())
			{
				httpGet.addHeader(header, controller.getHttpHeaders().get(header));
			}
			
			// Handle X-Original-URI and Referer HTTP headers.
			String originalUri = originalHttpRequest.getScheme() + "://" + originalHttpRequest.getServerName();
			if (originalHttpRequest.getServerPort() != 80)
				originalUri += ":" + originalHttpRequest.getServerPort();
			originalUri += controller.getUri().getOriginalUriAsString();
			httpGet.addHeader("Referer", originalUri);
			
			String xHttpHeaderOriginalUri = originalHttpRequest.getHeader("X-Original-URI");
			httpGet.addHeader("X-Original-URI", xHttpHeaderOriginalUri == null ? originalUri : xHttpHeaderOriginalUri);

			// Get the data.
			HttpResponse response  = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();

			// Pass HTTP headers through.
			originalHttpResponse.setStatus(response.getStatusLine().getStatusCode());
			if (entity.getContentType() != null)
				originalHttpResponse.setContentType(entity.getContentType().getValue());

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
				originalHttpResponse.addHeader(header.getName(), header.getValue());
			}

			// Pass content through.
			originalHttpResponse.getWriter().write(EntityUtils.toString(entity));
		}
		catch (Exception e)
		{
			Http.returnErrorCode(controller.getResponse(), 500, e.getCause().getMessage(), e);
			e.printStackTrace();
		}
		finally
		{
			httpClient.getConnectionManager().shutdown();
		}
	}
}
