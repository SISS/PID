package csiro.pidsvc.mappingstore.action;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import csiro.pidsvc.core.Settings;
import csiro.pidsvc.helper.URI;
import csiro.pidsvc.mappingstore.Manager;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;
import csiro.pidsvc.tracing.ITracer;

public class Runner
{
	protected final URI _uri;
	protected final HttpServletRequest _request;
	protected final HttpServletResponse _response;
	protected final ITracer _tracer;
	
	protected HashMap<String, String> _httpHeaders = new HashMap<String, String>();
	
	public Runner(URI uri, HttpServletRequest request, HttpServletResponse response, ITracer tracer)
	{
		_uri = uri;
		_request = request;
		_response = response;
		_tracer = tracer;
	}
	
	public URI getUri()
	{
		return _uri;
	}

	public HttpServletRequest getRequest()
	{
		return _request;
	}

	public HttpServletResponse getResponse()
	{
		return _response;
	}
	
	public HashMap<String, String> getHttpHeaders()
	{
		return _httpHeaders;
	}

	public boolean isTraceMode()
	{
		return _tracer != null;
	}
	
	public void trace(String message)
	{
		if (_tracer != null)
			_tracer.trace(message);
	}

	public void Run(MappingMatchResults matchResult) throws NamingException, SQLException, IOException
	{
		Manager				mgr = null;
		AbstractAction		action;

		try
		{
			mgr = new Manager();

			// Update QR Code hits counter for resolved URIs.
			if (matchResult.MappingId != MappingMatchResults.NULL && _uri.isQrCodeHit())
				mgr.increaseQrCodeHitCounter(matchResult.MappingId);

			// Get request HTTP headers.
			for (@SuppressWarnings("unchecked")
				Enumeration<String> enumeration = (Enumeration<String>)_request.getHeaderNames(); enumeration.hasMoreElements(); )
			{
				String header = (String)enumeration.nextElement();
				_httpHeaders.put(header, _request.getHeader(header));
			}
			_httpHeaders.remove("host");
			_httpHeaders.remove("content-length");
			_httpHeaders.remove("accept-encoding");

			// Process actions.
			if (matchResult.Condition != null)
			{
				// Condition-specific actions.
				List actionList = mgr.getActionsByConditionId(matchResult.Condition.ID);
				for (Descriptor descriptor : actionList)
				{
					action = instantiateActionObject(descriptor, matchResult);
					if (isTraceMode())
					{
						_tracer.trace(action.toString());
						action.trace();
					}
					else
						action.run();
				}
			}
			else if (matchResult.DefaultActionId != MappingMatchResults.NULL)
			{
				// User-defined fall back action.
				Descriptor descriptor = mgr.getAction(matchResult.DefaultActionId);
				action = instantiateActionObject(descriptor, matchResult);
				if (isTraceMode())
				{
					_tracer.trace(action.toString());
					action.trace();
				}
				else
					action.run();
			}
			else if (matchResult.AuxiliaryData != null)
			{
				// Matching URI mapping has been found with no matching condition
				// and no fall back action defined by the user.
				if (isTraceMode())
				{
					_tracer.trace("No matching condition found.");
					(new Action415(this, null, null)).trace();
				}
				else
					(new Action415(this, null, null)).run();
			}
			else
			{
				// Last resort fall back action.
				if (isTraceMode())
					(new Action404(this, null, null)).trace();
				else
					(new Action404(this, null, null)).run();
			}

			if (isTraceMode())
				_response.setContentType("text/plain");
		}
		finally
		{
			if (mgr != null)
				mgr.close();
		}
	}

	protected AbstractAction instantiateActionObject(Descriptor descriptor, MappingMatchResults matchResult)
	{
		try
		{
			String actionType = descriptor.Type;
			if (actionType.equals("Proxy") && !Settings.getInstance().getProperty("allowProxyAction").equalsIgnoreCase("true"))
			{
				trace("Proxy action is disallowed for this PID Service instance. Invoking 302 Simple Redirection action instead...");
				actionType = "302";
			}

			Class<?> impl = Class.forName("csiro.pidsvc.mappingstore.action.Action" + actionType);
			Constructor<?> ctor = impl.getDeclaredConstructor(Runner.class, Descriptor.class, MappingMatchResults.class);
			return (AbstractAction)ctor.newInstance(this, descriptor, matchResult);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
}
