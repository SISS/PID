package csiro.pidsvc.helper;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URI
{
	protected static final Pattern		RE_URI_COMPONENTS = Pattern.compile("^(.+?)(?:\\.([^\\.]*?))?(?:\\?(.*))?$", Pattern.CASE_INSENSITIVE); // matches path, file extension, querystring.
	protected static final Pattern		RE_URI_QRCODE_REQUEST = Pattern.compile("^(.+?)(?:\\?_pidsvcqr(?:=([^&]*))?$|_pidsvcqr(?:=([^&]*))?&|&_pidsvcqr(?:=([^&]*))?$)(.*)$", Pattern.CASE_INSENSITIVE);
	protected static final Pattern		RE_URI_QRCODE_HIT = Pattern.compile("^(.+?)(?:\\?_pidsvcqrhit(?:=[^&]*)?$|_pidsvcqrhit(?:=[^&]*)?&|&_pidsvcqrhit(?:=[^&]*)?$)(.*)$", Pattern.CASE_INSENSITIVE);

	protected String					_originalUri;
	protected java.net.URI				_uri;
	protected String					_uriQueryString = null;
	protected HashMap<String, String>	_uriQueryStringMap = new HashMap<String, String>();
	protected String					_pathNoExtension = null;
	protected String					_extension = null;
	protected boolean					_qrCodeRequest = false, _qrCodeHit = false;
	protected int						_qrCodeSize = 100;

	public URI(java.net.URI uri) throws URISyntaxException
	{
		_originalUri = uri.toString();
		_uri = uri;
		parse();
	}
	
	public URI(String str) throws URISyntaxException
	{
		_uri = new java.net.URI(_originalUri = str);
		parse();
	}

	private URI(URI src)
	{
		this._originalUri = src._originalUri;
		this._uri = src._uri;
		this._uriQueryString = src._uriQueryString;
		this._uriQueryStringMap = src._uriQueryStringMap;
		this._pathNoExtension = src._pathNoExtension;
		this._extension = src._extension;

		this._qrCodeRequest = src._qrCodeRequest;
		this._qrCodeHit = src._qrCodeHit;
		this._qrCodeSize = src._qrCodeSize;
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

	public String getQueryString()
	{
		return _uriQueryString;
	}

	public String getQuerystringParameter(String parameter)
	{
		return _uriQueryStringMap.get(parameter);
	}

	public boolean isQrCodeRequest()
	{
		return _qrCodeRequest;
	}

	public boolean isQrCodeHit()
	{
		return _qrCodeHit;
	}

	public int getQrCodeSize()
	{
		return _qrCodeSize;
	}

	private void parse() throws URISyntaxException
	{
		Matcher m;

		// Detect QR Code request flag.
		for (m = RE_URI_QRCODE_REQUEST.matcher(_uri.toString()); m.matches(); m = RE_URI_QRCODE_REQUEST.matcher(_uri.toString())) 
		{
			for (int i = 2; i < 5; ++i)
			{
				int size = Literals.toInt(m.group(i), -1);
				if (size == -1)
					continue;
				if (size > 1200)
					size = 1200;
				_qrCodeSize = size;
				break;
			}
			_qrCodeRequest = true;
			_uri = new java.net.URI(_originalUri = m.replaceAll("$1$5"));
		}

		// Detect QR Code hit flag.
		for (m = RE_URI_QRCODE_HIT.matcher(_uri.toString()); m.matches(); m = RE_URI_QRCODE_REQUEST.matcher(_uri.toString())) 
		{
			_qrCodeHit = true;
			_uri = new java.net.URI(_originalUri = m.replaceAll("$1$2"));
		}

		// Parse URI components.
		m = RE_URI_COMPONENTS.matcher(_uri.toString());
		if (m.matches())
		{
			_pathNoExtension = m.group(1);
			_extension = m.group(2);
			
			if (m.group(3) != null)
			{
				String queryStringArgs[];
				for (String pair : (_uriQueryString = m.group(3)).split("&"))
				{
					queryStringArgs = pair.split("=", 2);
					if (queryStringArgs.length == 0)
						continue;
					_uriQueryStringMap.put(queryStringArgs[0], queryStringArgs.length == 1 ? "" : queryStringArgs[1]);
				}
			}
		}
	}
}
