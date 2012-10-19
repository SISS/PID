package csiro.pidsvc.helper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Http
{
	private static final String PID_SERVICE_MSG_HTTP_HEADER = "X-PID-Service-Message";

	/**
	 * Return specific HTTP response code and add a message to HTTP header.
	 * 
	 * @param response HttpServletResponse object.
	 * @param httpResponseCode HTTP response code.
	 * @param message HTTP response message.
	 * @param exception Exception object.
	 */
	public static void returnErrorCode(HttpServletResponse response, int httpResponseCode, String message, Exception exception)
	{
		try
		{
			StringWriter sw = new StringWriter();
			exception.printStackTrace(new PrintWriter(sw));

			response.addHeader(PID_SERVICE_MSG_HTTP_HEADER, sw.toString());
			response.sendError(httpResponseCode, message);
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
}
