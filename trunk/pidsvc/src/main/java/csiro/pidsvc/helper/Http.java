package csiro.pidsvc.helper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class Http
{
	private static final String PID_SERVICE_ERROR_HTTP_HEADER = "X-PID-Service-Exception";
	private static final String PID_SERVICE_MSG_HTTP_HEADER = "X-PID-Service-Message";

	/**
	 * Return specific HTTP response code and add a message to HTTP header.
	 * 
	 * @param response HttpServletResponse object.
	 * @param httpResponseCode HTTP response code.
	 * @param message HTTP response message.
	 * @param exception Exception object.
	 */
	public static void returnErrorCode(HttpServletResponse response, int httpResponseCode, Exception exception)
	{
		try
		{
			String message = (exception.getMessage() == null ? "" : exception.getMessage()) +
				(exception.getCause() == null ? "" : "\n" + exception.getCause().getMessage());

			StringWriter sw = new StringWriter();
			exception.printStackTrace(new PrintWriter(sw));

			response.addHeader(PID_SERVICE_ERROR_HTTP_HEADER, message);
			response.addHeader(PID_SERVICE_MSG_HTTP_HEADER, sw.toString());
			response.sendError(httpResponseCode, exception.getMessage());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static String readInputStream(HttpServletRequest request) throws IOException
	{
		return Stream.readInputStream(request.getInputStream());
	}
	
	public static String simpleGetRequest(String uri)
	{
		HttpClient httpClient = new DefaultHttpClient();
		try
		{
			HttpGet httpGet = new HttpGet(uri);

			// Get the data.
			HttpResponse response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();

			// Return content.
			return EntityUtils.toString(entity);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		finally
		{
			httpClient.getConnectionManager().shutdown();
		}
	}
}
