/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 * 
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 * 
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

package csiro.pidsvc.mappingstore;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import csiro.pidsvc.helper.URI;
import csiro.pidsvc.mappingstore.condition.AbstractCondition.NameValuePairSubstitutionGroup;
import csiro.pidsvc.mappingstore.condition.ConditionComparator;

/**
 * Implementation class of a formal grammar used in URI rewrite actions.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class FormalGrammar
{
	protected class FunctionArguments
	{
		final private String _arguments[];

		public FunctionArguments(String arguments)
		{
			_arguments = (arguments == null || arguments.isEmpty() ? new String[] {} : arguments.split("(?<!\\\\):"));
		}

		public String get(int index)
		{
			return index < _arguments.length ? unescape(_arguments[index]) : "";
		}

		public String getUnescaped(int index)
		{
			return index < _arguments.length ? _arguments[index] : "";
		}

		public int getCount()
		{
			return _arguments.length;
		}
	}

	private static Logger _logger = LogManager.getLogger(FormalGrammar.class.getName());

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
		_logger.trace("Parsing formal grammar expression: {}", expression);
		_log.clear();
		String lastState = expression;

		// Bring all place-holders and function calls to the same syntax.
		String ret = expression.replaceAll("\\$(\\d+)", "\\${URI:$1}");
		if (ret != lastState)
		{
			_logger.trace(">>> {}", ret);
			_log.add(lastState = ret);
		}

		/*
		 * Process function calls.
		 * 		Expand function calls going inside out (from innermost to outermost calls).
		 * 		The result of outermost function calls is URL encoded.
		 * 		The result of nested function calls is escaped.
		 */
		final Pattern reFunction = Pattern.compile("(?<!\\\\)\\$(?<!\\\\)\\{(\\w+)(?::([^$]*?))?(?<!\\\\)\\}", Pattern.CASE_INSENSITIVE);
		for (Matcher m = reFunction.matcher(ret); m.find(); m = reFunction.matcher(ret))
		{
			String fnName		= m.group(1);
			String fnRet		= invokeFunction(fnName, m.group(2));
			boolean isNested	= ret.lastIndexOf("${", m.start() - 1) != -1;

			fnRet = (fnRet == null ? "" : URLDecoder.decode(fnRet, "UTF-8"));

			if (isNested)
			{
				// Nested function call.
				ret = ret.substring(0, m.start()) + escape(fnRet) + ret.substring(m.end());
			}
			else if (urlSafe && !fnName.equalsIgnoreCase("RAW"))
			{
				// Non-nested function call.
				ret = ret.substring(0, m.start()) + URLEncoder.encode(fnRet, "UTF-8") + ret.substring(m.end());
			}
			else
			{
				// Non-nested function call.
				ret = ret.substring(0, m.start()) + fnRet + ret.substring(m.end());
			}
			_logger.trace(">>> {}", ret);
			_log.add(ret);
		}

		if (_log.size() > 0)
			_log.add(0, expression);
		_logger.trace("Result: {}", ret);
		return ret;
	}

	public static String escape(String str)
	{
		return str.replaceAll("[:\\\\\\$\\{\\}=&]", "\\\\$0");
	}
	
	public static String unescape(String str)
	{
		return str.replaceAll("\\\\([:\\\\\\$\\{\\}=&])", "$1");
	}
	
	protected String invokeFunction(String name, String param)
	{
		_logger.trace("Invoking internal function ${{}:{}}", name, param);
		Manager mgr = null;
		try
		{
			FunctionArguments args = new FunctionArguments(param);

			if (name.equalsIgnoreCase("RAW"))
			{
				return unescape(param);
			}
			else if (name.equalsIgnoreCase("URI"))
			{
				// Regex from URI matching.
				if (param == null || param.isEmpty() || param.equals("0"))
					return URLDecoder.decode(_uri.getOriginalUriAsString(), "UTF-8");
				else if (_matchAuxiliaryData instanceof Pattern)
				{
					int groupIndex = Integer.parseInt(args.get(0));
					Matcher m = ((Pattern)_matchAuxiliaryData).matcher(_uri.getPathNoExtension());
					if (m.find() && groupIndex <= m.groupCount())
						return URLDecoder.decode(m.group(groupIndex), "UTF-8");
				}
			}
			else if (name.equalsIgnoreCase("C"))
			{
				// Matches from condition regex matching.
				if (_conditionAuxiliaryData != null)
				{
					if (_conditionAuxiliaryData instanceof Matcher)
					{
						Matcher m = (Matcher)_conditionAuxiliaryData;
						int groupIndex = args.get(0).isEmpty() ? 0 : Integer.parseInt(args.get(0));
						if (groupIndex <= m.groupCount())
							return m.group(groupIndex);
					}
					else if (_conditionAuxiliaryData instanceof NameValuePairSubstitutionGroup)
					{
						NameValuePairSubstitutionGroup aux = (NameValuePairSubstitutionGroup)_conditionAuxiliaryData;
						Matcher m = Pattern.compile("^(.+?)(?::(.+))?$").matcher(args.get(0));
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
				if (args.get(0).equalsIgnoreCase("REQUEST_URI"))
				{
					// E.g. /id/test
					return _uri.getPathNoExtension();
				}
				else if (args.get(0).equalsIgnoreCase("REQUEST_URI_EXT"))
				{
					// E.g. /id/test.ext
					String ext = _uri.getExtension();
					return (ext == null || ext.equals("")) ? _uri.getPathNoExtension() : _uri.getPathNoExtension() + "." + ext;
				}
				else if (args.get(0).equalsIgnoreCase("REQUEST_URI_QS") || args.get(0).equalsIgnoreCase("ORIGINAL_URI"))
				{
					// E.g. /id/test.ext?arg=1
					return _uri.getOriginalUriAsString();
				}
				else if (args.get(0).equalsIgnoreCase("FULL_REQUEST_URI"))
				{
					// E.g. http://example.org:8080/id/test
					return getFullRequestUri();
				}
				else if (args.get(0).equalsIgnoreCase("FULL_REQUEST_URI_BASE"))
				{
					// E.g. Returns http://example.org:8080/id/ for http://example.org:8080/id/test
					return getFullRequestUri().replaceAll("[^/]*$", "");
				}
				else if (args.get(0).equalsIgnoreCase("URI_REGISTER"))
				{
					// E.g. Returns http://example.org:8080/id for http://example.org:8080/id/test
					// Same as FULL_REQUEST_URI_BASE without trailing slash.
					return getFullRequestUri().replaceAll("/[^/]*$", "");
				}
				else if (args.get(0).equalsIgnoreCase("FULL_REQUEST_URI_EXT"))
				{
					// E.g. http://example.org:8080/id/test.ext
					String ext = _uri.getExtension();
					return (ext == null || ext.equals("")) ? getFullRequestUri() : getFullRequestUri() + "." + ext;
				}
				else if (args.get(0).equalsIgnoreCase("FULL_REQUEST_URI_QS"))
				{
					// E.g. http://example.org:8080/id/test.ext?arg=1
					String val = _request.getScheme() + "://" + _request.getServerName();
					if (_request.getServerPort() != 80)
						val += ":" + _request.getServerPort();
					return val + _uri.getOriginalUriAsString();
				}
				else if (args.get(0).equalsIgnoreCase("QUERY_STRING"))
				{
					// E.g. arg=1
					return _uri.getQueryString();
				}
				else if (args.get(0).equalsIgnoreCase("FILENAME") || args.get(0).equalsIgnoreCase("RESOURCE_NAME"))
				{
					// E.g. returns test for /id/test
					return _uri.getPathNoExtension().replaceFirst("^.+/", "");
				}
				else if (args.get(0).equalsIgnoreCase("FILENAME_EXT") || args.get(0).equalsIgnoreCase("RESOURCE_NAME_EXT"))
				{
					// E.g. returns test.ext for /id/test.ext
					String ext = _uri.getExtension();
					String val = _uri.getPathNoExtension().replaceFirst("^.+/", "");
					return (ext == null || ext.equals("")) ? val : val + "." + ext;
				}
				else if (args.get(0).equalsIgnoreCase("EXTENSION") || args.get(0).equalsIgnoreCase("EXT"))
				{
					// E.g. ext
					return _uri.getExtension();
				}
				else if (args.get(0).equalsIgnoreCase("DOT_EXTENSION") || args.get(0).equalsIgnoreCase("DOT_EXT"))
				{
					// E.g. .ext
					String ext = _uri.getExtension();
					return (ext == null || ext.equals("")) ? "" : "." + ext;
				}
				else if (args.get(0).equalsIgnoreCase("SERVER_NAME"))
				{
					// E.g. example.org
					return _request.getServerName();
				}
				else if (args.get(0).equalsIgnoreCase("SERVER_ADDR"))
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
				return (mgr = new Manager()).resolveLookupValue(args.get(0), args.get(1));
			}
			else if (name.equalsIgnoreCase("QS"))
			{
				// Returns query string parameter from original request URI.
				if (args.get(0).isEmpty())
					return _uri.getQueryString();
				else
				{
					String val = _uri.getQuerystringParameter(args.get(0));
					return val == null ? "" : URLDecoder.decode(val, "UTF-8");
				}
			}
			else if (name.equalsIgnoreCase("HTTP_HEADER"))
			{
				// Returns HTTP header from original HTTP request.
				return args.get(0).isEmpty() ? "" : _request.getHeader(args.get(0));
			}
			else if (name.equalsIgnoreCase("IF_THEN_ELSE"))
			{
				ConditionComparator cmp = new ConditionComparator(_uri, _request, 0, args.getUnescaped(0), _matchAuxiliaryData);
				return cmp.matches() ? args.get(1) : args.get(2);
			}
			else if (name.equalsIgnoreCase("ISNULL"))
			{
				return args.get(args.get(0).isEmpty() ? 1 : 0);
			}
			else if (name.equalsIgnoreCase("NULLIF"))
			{
				return args.get(0).equals(args.get(1)) ? null : args.get(0);
			}
			else if (name.equalsIgnoreCase("COALESCE"))
			{
				for (int i = 0; i < args.getCount(); ++i)
				{
					String val = args.get(i);
					if (!val.isEmpty())
						return val;
				}
			}
			else if (name.equalsIgnoreCase("LOWERCASE"))
			{
				return args.get(0).toLowerCase();
			}
			else if (name.equalsIgnoreCase("UPPERCASE"))
			{
				return args.get(0).toUpperCase();
			}
		}
		catch (Exception e)
		{
			_logger.debug("Internal function invocation exception.", e);
		}
		finally
		{
			if (mgr != null)
				mgr.close();
		}
		return null;
	}

	private String getFullRequestUri()
	{
		// E.g. http://example.org:8080/id/test
		String val = _request.getScheme() + "://" + _request.getServerName();
		if (_request.getServerPort() != 80)
			val += ":" + _request.getServerPort();
		return val + _uri.getPathNoExtension();
	}
}
