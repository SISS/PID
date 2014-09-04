/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 * 
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 * 
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

package csiro.pidsvc.helper;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service-specific URI parsing and handling.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class URI
{
	protected static final Pattern		RE_URI_COMPONENTS = Pattern.compile("^(.+?)(?:\\.([^\\.]*?))?(?:\\?(.*))?$", Pattern.CASE_INSENSITIVE); // matches path, file extension, querystring.
	protected static final Pattern		RE_URI_QRCODE_REQUEST = Pattern.compile("^(.+?)(?:\\?_pidsvcqr(?:=([^&]*))?$|_pidsvcqr(?:=([^&]*))?&|&_pidsvcqr(?:=([^&]*))?$)(.*)$", Pattern.CASE_INSENSITIVE);
	protected static final Pattern		RE_URI_QRCODE_HIT = Pattern.compile("^(.+?)(?:\\?_pidsvcqrhit(?:=[^&]*)?$|_pidsvcqrhit(?:=[^&]*)?&|&_pidsvcqrhit(?:=[^&]*)?$)(.*)$", Pattern.CASE_INSENSITIVE);
	protected static final Pattern		RE_URI_TRACE = Pattern.compile("^(.+?)(?:\\?_pidsvctrace(?:=[^&]*)?$|_pidsvctrace(?:=[^&]*)?&|&_pidsvctrace(?:=[^&]*)?$)(.*)$", Pattern.CASE_INSENSITIVE);

	protected String					_originalUri;
	protected java.net.URI				_uri;
	protected String					_uriQueryString = null;
	protected HashMap<String, String>	_uriQueryStringMap = new HashMap<String, String>();
	protected String					_pathNoExtension = null;
	protected String					_extension = null;
	protected boolean					_cmdQrCodeRequest = false, _cmdQrCodeHit = false;
	protected int						_cmdQrCodeSize = 100;
	protected boolean					_cmdTrace = false;

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

	public static String prepareURI(String str) throws UnsupportedEncodingException
	{
		str = str.replace("%26", "%2526"); // Double escape &.
		str = URLDecoder.decode(str, "UTF-8");
		str = str.replace(" ", "+"); // Required to avoid java.net.URISyntaxException: Illegal character in path.
		str = str.replaceAll("^([^&]+)&(.+)?$", "$1?$2"); // Replace first & with ? marking the start of the query string.
		return str;
	}

	public static URI create(String str) throws UnsupportedEncodingException, URISyntaxException
	{
		return new URI(prepareURI(URLEncoder.encode(str, "UTF-8")));
	}
	
	private URI(URI src)
	{
		this._originalUri = src._originalUri;
		this._uri = src._uri;
		this._uriQueryString = src._uriQueryString;
		this._uriQueryStringMap = src._uriQueryStringMap;
		this._pathNoExtension = src._pathNoExtension;
		this._extension = src._extension;

		this._cmdQrCodeRequest = src._cmdQrCodeRequest;
		this._cmdQrCodeHit = src._cmdQrCodeHit;
		this._cmdQrCodeSize = src._cmdQrCodeSize;
		this._cmdTrace = src._cmdTrace;
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
		return _cmdQrCodeRequest;
	}

	public boolean isQrCodeHit()
	{
		return _cmdQrCodeHit;
	}

	public int getQrCodeSize()
	{
		return _cmdQrCodeSize;
	}

	public boolean isTraceMode()
	{
		return _cmdTrace;
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
				_cmdQrCodeSize = size;
				break;
			}
			_cmdQrCodeRequest = true;
			_uri = new java.net.URI(_originalUri = m.replaceAll("$1$5"));
		}

		// Detect QR Code hit flag.
		for (m = RE_URI_QRCODE_HIT.matcher(_uri.toString()); m.matches(); m = RE_URI_QRCODE_HIT.matcher(_uri.toString())) 
		{
			_cmdQrCodeHit = true;
			_uri = new java.net.URI(_originalUri = m.replaceAll("$1$2"));
		}

		// Detect trace mode activation flag.
		for (m = RE_URI_TRACE.matcher(_uri.toString()); m.matches(); m = RE_URI_TRACE.matcher(_uri.toString())) 
		{
			_cmdTrace = true;
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
