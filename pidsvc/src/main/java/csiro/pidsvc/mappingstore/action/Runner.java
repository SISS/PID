package csiro.pidsvc.mappingstore.action;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;

import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import csiro.pidsvc.helper.URI;
import csiro.pidsvc.mappingstore.Manager;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;
import csiro.pidsvc.mappingstore.action.AbstractAction;
import csiro.pidsvc.mappingstore.action.Action404;
import csiro.pidsvc.mappingstore.action.Descriptor;
import csiro.pidsvc.mappingstore.action.List;

public class Runner
{
	protected final URI _uri;
	protected final HttpServletRequest _request;
	protected final HttpServletResponse _response;
	
	protected HashMap<String, String> _httpHeaders = new HashMap<String, String>();
	
	public Runner(URI uri, HttpServletRequest request, HttpServletResponse response)
	{
		_uri = uri;
		_request = request;
		_response = response;
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

	public void Run(MappingMatchResults matchResult) throws InstantiationException, IllegalAccessException, ClassNotFoundException, NamingException, SQLException, IOException
	{
		Manager				mgr = null;
		AbstractAction		action;

		try
		{
			mgr = new Manager();
		
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
					action = (AbstractAction)(Class.forName("csiro.pidsvc.mappingstore.action.Action" + descriptor.Type).newInstance());
					action.run(this, descriptor, matchResult);
				}
			}
			else if (matchResult.DefaultActionId != MappingMatchResults.NULL)
			{
				// User-defined fall back action.
				Descriptor descriptor = mgr.getActionsByActionId(matchResult.DefaultActionId);
				action = (AbstractAction)(Class.forName("csiro.pidsvc.mappingstore.action.Action" + descriptor.Type).newInstance());
				action.run(this, descriptor, matchResult);
			}
			else if (matchResult.AuxiliaryData != null)
			{
				// Matching URI mapping has been found with no matching condition
				// and no fall back action defined by the user.
				(new Action415()).run(this, null, null);
			}
			else
			{
				// Last resort fall back action.
				(new Action404()).run(this, null, null);
			}
		}
		finally
		{
			if (mgr != null)
				mgr.close();
		}
	}
}
