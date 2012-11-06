package csiro.pidsvc.mappingstore.action;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	protected String substrituteCaptureParameters(String input) throws URISyntaxException, UnsupportedEncodingException
	{
		// Bring all placeholders and function calls to the same syntax.
		int actionValueLength = -1;
		String ret = _descriptor.Value.replaceAll("\\$(\\d+)", "%[[URI:$1]]");
		while (actionValueLength != ret.length())
		{
			actionValueLength = ret.length();
			ret = ret.replaceAll("(?i)\\$\\{(\\w+)\\:([^\\$\\}]+)\\}", "%[[$1:$2]]");
		}

		// Process string functions.
		Pattern reFunction = Pattern.compile("%\\[\\[(\\w+):([^%]*?)\\]\\]");
		for (Matcher m = reFunction.matcher(ret); m.find(); m = reFunction.matcher(ret))
		{
			if (ret.lastIndexOf("%[[", m.start() - 1) == -1)
				// Non-nested function call.
				ret = ret.substring(0, m.start()) + URLEncoder.encode(callInternalFunction(input, m.group(1), m.group(2)), "UTF-8") + ret.substring(m.end());
			else
				// Nested function call.
				ret = ret.substring(0, m.start()) + callInternalFunction(input, m.group(1), m.group(2)) + ret.substring(m.end());
		}
		return ret;
	}

	protected String callInternalFunction(String input, String name, String param)
	{
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
					Matcher m = ((Pattern)_matchResult.AuxiliaryData).matcher(input);
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
					return _controller.getUri().getPathNoExtension();
				else if (param.equalsIgnoreCase("ORIGINAL_URI"))
					return _controller.getUri().getOriginalUriAsString();
				else if (param.equalsIgnoreCase("QUERY_STRING"))
					return _controller.getUri().getQueryString();
				else if (param.equalsIgnoreCase("EXTENSION"))
					return _controller.getUri().getExtension();
				else if (param.equalsIgnoreCase("SERVER_NAME"))
					return _controller.getRequest().getServerName();
				else if (param.equalsIgnoreCase("SERVER_ADDR"))
				{
					String val = _controller.getRequest().getScheme() + "://" + _controller.getRequest().getServerName();
					if (_controller.getRequest().getServerPort() != 80)
						val += ":" + _controller.getRequest().getServerPort();
					return val;
				}
			}
		}
		catch (Exception ex)
		{
		}
		return "";
	}

	public String toString()
	{
		return "Type=" + getClass().getSimpleName() + "; Name=" + _descriptor.Name + "; Value=" + _descriptor.Value + ";";		
	}
}
