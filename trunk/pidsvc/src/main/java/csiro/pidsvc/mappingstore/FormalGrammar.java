package csiro.pidsvc.mappingstore;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import csiro.pidsvc.helper.URI;
import csiro.pidsvc.mappingstore.condition.AbstractCondition.NameValuePairSubstitutionGroup;

public class FormalGrammar
{
	protected final URI					_uri;
	protected final HttpServletRequest	_request;
	protected final Object				_matchAuxiliaryData;
	protected final Object				_conditionAuxiliaryData;
	private ArrayList<String>			_log = new ArrayList<String>();
	
	public FormalGrammar(URI uri, HttpServletRequest request, Object matchAuxiliaryData, Object conditionAuxiliaryData)
	{
		_uri = uri;
		_request = request;
		_matchAuxiliaryData = matchAuxiliaryData;
		_conditionAuxiliaryData = conditionAuxiliaryData;
	}

	public ArrayList<String> getLog()
	{
		return _log;
	}

	public String parse(String expression, boolean urlSafe) throws UnsupportedEncodingException
	{
		_log.clear();
		_log.add(expression);

		// Bring all place-holders and function calls to the same syntax.
		String ret = expression.replaceAll("\\$(\\d+)", "\\${URI:$1}");
		if (ret != expression)
			_log.add(ret);

		// Process string functions.
		final Pattern reFunction = Pattern.compile("\\$\\{(\\w+)(?::([^$]*?))?\\}");
		for (Matcher m = reFunction.matcher(ret); m.find(); m = reFunction.matcher(ret))
		{
			if (m.group(1).equalsIgnoreCase("RAW"))
				// Raw function call just passes the argument through without URL encoding.
				ret = ret.substring(0, m.start()) + m.group(2) + ret.substring(m.end());
			else if (urlSafe && ret.lastIndexOf("${", m.start() - 1) == -1)
				// Non-nested function call.
				ret = ret.substring(0, m.start()) + URLEncoder.encode(invokeFunction(m.group(1), m.group(2)), "UTF-8") + ret.substring(m.end());
			else
				// Nested function call.
				ret = ret.substring(0, m.start()) + invokeFunction(m.group(1), m.group(2)) + ret.substring(m.end());
			_log.add(ret);
		}
		return ret;
	}

	public String invokeFunction(String name, String param)
	{
		Manager mgr = null;
		try
		{
			if (name.equalsIgnoreCase("URI"))
			{
				// Regex from URI matching.
				if (param == null || param.isEmpty() || param.equals("0"))
					return _uri.getOriginalUriAsString();
				else if (_matchAuxiliaryData instanceof Pattern)
				{
					int groupIndex = Integer.parseInt(param);
					Matcher m = ((Pattern)_matchAuxiliaryData).matcher(_uri.getPathNoExtension());
					if (m.find() && groupIndex <= m.groupCount())
						return m.group(groupIndex);
				}
			}
			else if (name.equalsIgnoreCase("C"))
			{
				// Matches from condition regex matching.
				if (_conditionAuxiliaryData != null)
				{
					if (param == null || param.isEmpty())
						param = "0";
					if (_conditionAuxiliaryData instanceof Matcher)
					{
						Matcher m = (Matcher)_conditionAuxiliaryData;
						int groupIndex = Integer.parseInt(param);
						if (groupIndex <= m.groupCount())
							return m.group(groupIndex);
					}
					else if (_conditionAuxiliaryData instanceof NameValuePairSubstitutionGroup)
					{
						NameValuePairSubstitutionGroup aux = (NameValuePairSubstitutionGroup)_conditionAuxiliaryData;
						Matcher m = Pattern.compile("^(.+?)(?::(.+))?$").matcher(param);
						if (m.find())
						{
							String val = m.group(2);
							if (val == null)
								return aux.get(m.group(1)).group(0);
							else
							{
								m = aux.get(m.group(1));
								int groupIndex = Integer.parseInt(val);
								if (groupIndex <= m.groupCount())
									return m.group(groupIndex);
							}
						}
					}
				}
			}
			else if (name.equalsIgnoreCase("ENV"))
			{
				// Environment variables.
				if (param.equalsIgnoreCase("REQUEST_URI"))
				{
					// E.g. /id/test
					return _uri.getPathNoExtension();
				}
				else if (param.equalsIgnoreCase("REQUEST_URI_EXT"))
				{
					// E.g. /id/test.ext
					String ext = _uri.getExtension();
					return (ext == null || ext.equals("")) ? _uri.getPathNoExtension() : _uri.getPathNoExtension() + "." + ext;
				}
				else if (param.equalsIgnoreCase("REQUEST_URI_QS") || param.equalsIgnoreCase("ORIGINAL_URI"))
				{
					// E.g. /id/test.ext?arg=1
					return _uri.getOriginalUriAsString();
				}
				else if (param.equalsIgnoreCase("FULL_REQUEST_URI"))
				{
					// E.g. http://example.org:8080/id/test
					String val = _request.getScheme() + "://" + _request.getServerName();
					if (_request.getServerPort() != 80)
						val += ":" + _request.getServerPort();
					return val + _uri.getPathNoExtension();
				}
				else if (param.equalsIgnoreCase("FULL_REQUEST_URI_EXT"))
				{
					// E.g. http://example.org:8080/id/test.ext
					String val = _request.getScheme() + "://" + _request.getServerName();
					if (_request.getServerPort() != 80)
						val += ":" + _request.getServerPort();
					val += _uri.getPathNoExtension();

					// Check extension.
					String ext = _uri.getExtension();
					return (ext == null || ext.equals("")) ? val : val + "." + ext;
				}
				else if (param.equalsIgnoreCase("FULL_REQUEST_URI_QS"))
				{
					// E.g. http://example.org:8080/id/test.ext?arg=1
					String val = _request.getScheme() + "://" + _request.getServerName();
					if (_request.getServerPort() != 80)
						val += ":" + _request.getServerPort();
					return val + _uri.getOriginalUriAsString();
				}
				else if (param.equalsIgnoreCase("QUERY_STRING"))
				{
					// E.g. arg=1
					return _uri.getQueryString();
				}
				else if (param.equalsIgnoreCase("EXTENSION") || param.equalsIgnoreCase("EXT"))
				{
					// E.g. ext
					return _uri.getExtension();
				}
				else if (param.equalsIgnoreCase("DOT_EXTENSION") || param.equalsIgnoreCase("DOT_EXT"))
				{
					// E.g. .ext
					String ext = _uri.getExtension();
					return (ext == null || ext.equals("")) ? "" : "." + ext;
				}
				else if (param.equalsIgnoreCase("SERVER_NAME"))
				{
					// E.g. example.org
					return _request.getServerName();
				}
				else if (param.equalsIgnoreCase("SERVER_ADDR"))
				{
					// E.g. http://example.org:8080
					String val = _request.getScheme() + "://" + _request.getServerName();
					if (_request.getServerPort() != 80)
						val += ":" + _request.getServerPort();
					return val;
				}
			}
			else if (name.equalsIgnoreCase("LOOKUP"))
			{
				// Extracts [(namespace)]:(value).
				final Pattern reNsValue = Pattern.compile("^\\[(.+?)\\]:(.+)$");
				Matcher m = reNsValue.matcher(param);

				if (m.find())
				{
					String value = (mgr = new Manager()).resolveLookupValue(m.group(1), m.group(2));
					return value == null ? "" : value;
				}
			}
			else if (name.equalsIgnoreCase("QS"))
			{
				// Returns query string parameter from original request URI.
				return URLDecoder.decode(_uri.getQuerystringParameter(param), "UTF-8");
			}
		}
		catch (Exception ex)
		{
		}
		finally
		{
			if (mgr != null)
				mgr.close();
		}
		return "";
	}
}
