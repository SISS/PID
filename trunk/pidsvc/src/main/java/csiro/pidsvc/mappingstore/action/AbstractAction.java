package csiro.pidsvc.mappingstore.action;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import csiro.pidsvc.mappingstore.Manager;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;
import csiro.pidsvc.mappingstore.condition.AbstractCondition.NameValuePairSubstitutionGroup;

public abstract class AbstractAction
{
	protected final Runner _controller;
	protected final Descriptor _descriptor;
	protected final MappingMatchResults _matchResult;	

	public AbstractAction(Runner controller, Descriptor descriptor, MappingMatchResults matchResult)
	{
		_controller = controller;
		_descriptor = descriptor;
		_matchResult = matchResult;
	}

	public abstract void run();
	public abstract void trace();

	public boolean isTraceMode()
	{
		return _controller.isTraceMode();
	}

	protected void trace(String message)
	{
		_controller.trace(message);
	}

	protected String getExpandedActionValue() throws URISyntaxException, UnsupportedEncodingException
	{
		trace("Expand\t" + _descriptor.Value);

		// Bring all placeholders and function calls to the same syntax.
		String ret = _descriptor.Value.replaceAll("\\$(\\d+)", "\\${URI:$1}");
		if (ret != _descriptor.Value)
			trace("\t" + ret);

		// Process string functions.
		final Pattern reFunction = Pattern.compile("\\$\\{(\\w+):([^$]*?)\\}");
		for (Matcher m = reFunction.matcher(ret); m.find(); m = reFunction.matcher(ret))
		{
			if (m.group(1).equalsIgnoreCase("RAW"))
				// Raw function call just passes the argument through without URL encoding.
				ret = ret.substring(0, m.start()) + m.group(2) + ret.substring(m.end());
			else if (ret.lastIndexOf("${", m.start() - 1) == -1)
				// Non-nested function call.
				ret = ret.substring(0, m.start()) + URLEncoder.encode(callInternalFunction(m.group(1), m.group(2)), "UTF-8") + ret.substring(m.end());
			else
				// Nested function call.
				ret = ret.substring(0, m.start()) + callInternalFunction(m.group(1), m.group(2)) + ret.substring(m.end());
			trace("\t" + ret);
		}
		return ret;
	}

	protected String callInternalFunction(String name, String param)
	{
		Manager mgr = null;
		try
		{
			if (name.equalsIgnoreCase("URI"))
			{
				// Regex from URI matching.
				if (param.equals("0"))
					return _controller.getUri().getOriginalUriAsString();
				else if (_matchResult.AuxiliaryData instanceof Pattern)
				{
					int groupIndex = Integer.parseInt(param);
					Matcher m = ((Pattern)_matchResult.AuxiliaryData).matcher(_controller.getUri().getPathNoExtension());
					if (m.find() && groupIndex <= m.groupCount())
						return m.group(groupIndex);
				}
			}
			else if (name.equalsIgnoreCase("C"))
			{
				// Matches from condition regex matching.
				if (_matchResult.Condition != null)
				{
					if (_matchResult.Condition.AuxiliaryData instanceof Matcher)
					{
						Matcher m = (Matcher)_matchResult.Condition.AuxiliaryData;
						int groupIndex = Integer.parseInt(param);
						if (groupIndex <= m.groupCount())
							return m.group(groupIndex);
					}
					else if (_matchResult.Condition.AuxiliaryData instanceof NameValuePairSubstitutionGroup)
					{
						NameValuePairSubstitutionGroup aux = (NameValuePairSubstitutionGroup)_matchResult.Condition.AuxiliaryData;
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
					return _controller.getUri().getPathNoExtension();
				}
				else if (param.equalsIgnoreCase("REQUEST_URI_EXT"))
				{
					// E.g. /id/test.ext
					String ext = _controller.getUri().getExtension();
					return (ext == null || ext.equals("")) ? _controller.getUri().getPathNoExtension() : _controller.getUri().getPathNoExtension() + "." + ext;
				}
				else if (param.equalsIgnoreCase("REQUEST_URI_QS") || param.equalsIgnoreCase("ORIGINAL_URI"))
				{
					// E.g. /id/test.ext?arg=1
					return _controller.getUri().getOriginalUriAsString();
				}
				else if (param.equalsIgnoreCase("FULL_REQUEST_URI"))
				{
					// E.g. http://example.org:8080/id/test
					String val = _controller.getRequest().getScheme() + "://" + _controller.getRequest().getServerName();
					if (_controller.getRequest().getServerPort() != 80)
						val += ":" + _controller.getRequest().getServerPort();
					return val + _controller.getUri().getPathNoExtension();
				}
				else if (param.equalsIgnoreCase("FULL_REQUEST_URI_EXT"))
				{
					// E.g. http://example.org:8080/id/test.ext
					String val = _controller.getRequest().getScheme() + "://" + _controller.getRequest().getServerName();
					if (_controller.getRequest().getServerPort() != 80)
						val += ":" + _controller.getRequest().getServerPort();
					val += _controller.getUri().getPathNoExtension();

					// Check extension.
					String ext = _controller.getUri().getExtension();
					return (ext == null || ext.equals("")) ? val : val + "." + ext;
				}
				else if (param.equalsIgnoreCase("FULL_REQUEST_URI_QS"))
				{
					// E.g. http://example.org:8080/id/test.ext?arg=1
					String val = _controller.getRequest().getScheme() + "://" + _controller.getRequest().getServerName();
					if (_controller.getRequest().getServerPort() != 80)
						val += ":" + _controller.getRequest().getServerPort();
					return val + _controller.getUri().getOriginalUriAsString();
				}
				else if (param.equalsIgnoreCase("QUERY_STRING"))
				{
					// E.g. arg=1
					return _controller.getUri().getQueryString();
				}
				else if (param.equalsIgnoreCase("EXTENSION"))
				{
					// E.g. ext
					return _controller.getUri().getExtension();
				}
				else if (param.equalsIgnoreCase("SERVER_NAME"))
				{
					// E.g. example.org
					return _controller.getRequest().getServerName();
				}
				else if (param.equalsIgnoreCase("SERVER_ADDR"))
				{
					// E.g. http://example.org:8080
					String val = _controller.getRequest().getScheme() + "://" + _controller.getRequest().getServerName();
					if (_controller.getRequest().getServerPort() != 80)
						val += ":" + _controller.getRequest().getServerPort();
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

	public String toString()
	{
		return "Type=" + getClass().getSimpleName() + "; Name=" + _descriptor.Name + "; Value=" + _descriptor.Value + ";";		
	}
}
