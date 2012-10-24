package csiro.pidsvc.mappingstore.action;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Enumeration;
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
		String actionValue = _descriptor.Value.replaceAll("\\$\\{C\\:([^\\}]+)\\}", "%[[$1]]");
		String ret;

		// Regex from URI matching.
		if (_matchResult.AuxiliaryData instanceof Pattern)
			ret = input.replaceAll(((Pattern)_matchResult.AuxiliaryData).pattern(), actionValue);
		else
			ret = actionValue;

		// Matches from condition regex matching.
		if (_matchResult.Condition != null)
		{
			String q, val;
			Matcher m;
			if (_matchResult.Condition.AuxiliaryData instanceof Matcher)
			{
				m = (Matcher)_matchResult.Condition.AuxiliaryData;
				for (int i = 0; i <= m.groupCount(); ++i)
				{
					ret = ret.replace("%[[" + i + "]]", m.group(i));
				}
			}
			else if (_matchResult.Condition.AuxiliaryData instanceof NameValuePairSubstitutionGroup)
			{
				NameValuePairSubstitutionGroup aux = (NameValuePairSubstitutionGroup)_matchResult.Condition.AuxiliaryData;
				Enumeration<String> keys = aux.keys();

				while (keys.hasMoreElements())
				{
					q = (String)keys.nextElement();
					m = aux.get(q);

					ret = ret.replace("%[[" + q + "]]", m.group(0));
					for (int i = 0; i <= m.groupCount(); ++i)
					{
						val = m.group(i);
						ret = ret.replace("%[[" + q + ":" + i + "]]", val == null ? "" : val);
					}
				}
			}
		}

		// Remove any non-expanded place holders.
		return ret.replaceAll("%\\[\\[.+?\\]\\]|\\$\\d+", "");
	}

	public String toString()
	{
		return "Type=" + getClass().getSimpleName() + "; Name=" + _descriptor.Name + "; Value=" + _descriptor.Value + ";";		
	}
}
