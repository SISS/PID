package csiro.pidsvc.helper;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URI
{
	protected final String _originalUri;
	protected final java.net.URI _uri;
	protected HashMap<String, String> _uriQueryString = new HashMap<String, String>();;
	protected String _pathNoExtension = null;
	protected String _extension = null;

	public URI(java.net.URI uri)
	{
		_originalUri = uri.toString();
		_uri = uri;
		parse();
	}
	
	public URI(String str) throws URISyntaxException
	{
		_originalUri = str;
		_uri = new java.net.URI(str);
		parse();
	}

	private URI(URI src)
	{
		this._originalUri = src._originalUri;
		this._uri = src._uri;
		this._uriQueryString = src._uriQueryString;
		this._pathNoExtension = src._pathNoExtension;
		this._extension = src._extension;
	}
	
	public String getOriginalUriAsString()
	{
		return _originalUri;
	}
	
	public java.net.URI getSubclass()
	{
		return _uri;
	}
	
	public String getPath()
	{
		return _uri.getPath();
	}
	
	public String getPathNoExtension()
	{
		return _pathNoExtension;
	}
	
	public String getExtension()
	{
		return _extension;
	}

	public URI getNoExtensionURI()
	{
		if (this._extension == null)
			return this;
		URI ret = new URI(this);
		ret._pathNoExtension += "." + ret._extension;
		ret._extension = null;
		return ret;
	}
	
	public String getQuerystringParameter(String parameter)
	{
		return _uriQueryString.get(parameter);
	}
	
	private void parse()
	{
		Pattern reUriParts = Pattern.compile("^(.+?)(?:\\.([^\\.]*?))?(?:\\?(.*))?$", Pattern.CASE_INSENSITIVE); // matches path, file extension, querystring.
		Matcher m = reUriParts.matcher(_uri.toString());
		
		if (m.matches())
		{
			_pathNoExtension = m.group(1);
			_extension = m.group(2);
			
			if (m.group(3) != null)
			{
				String queryStringArgs[];
				for (String pair : m.group(3).split("&"))
				{
					queryStringArgs = pair.split("=");
					_uriQueryString.put(queryStringArgs[0], queryStringArgs.length == 1 ? "" : queryStringArgs[1]);
				}
			}
		}
	}
}
