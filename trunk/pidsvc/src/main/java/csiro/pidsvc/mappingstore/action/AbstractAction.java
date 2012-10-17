package csiro.pidsvc.mappingstore.action;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import csiro.pidsvc.mappingstore.action.Runner;
import csiro.pidsvc.mappingstore.condition.AbstractCondition.NameValuePairSubstitutionGroup;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public abstract class AbstractAction
{
	public abstract void run(Runner controller, Descriptor actionDescriptor, MappingMatchResults matchResult);
	
	protected String substrituteCaptureParameters(String input, Descriptor actionDescriptor, MappingMatchResults matchResult) throws URISyntaxException, UnsupportedEncodingException
	{
		String actionValue = actionDescriptor.Value.replaceAll("\\$\\{C\\:([^\\}]+)\\}", "%[[$1]]");
		String ret;

		// Regex from URI matching.
		if (matchResult.AuxiliaryData instanceof Pattern)
			ret = input.replaceAll(((Pattern)matchResult.AuxiliaryData).pattern(), actionValue);
		else
			ret = actionValue;

		// Matches from condition regex matching.
		if (matchResult.Condition != null)
		{
			String q, val;
			Matcher m;
			if (matchResult.Condition.AuxiliaryData instanceof Matcher)
			{
				m = (Matcher)matchResult.Condition.AuxiliaryData;
				for (int i = 0; i <= m.groupCount(); ++i)
				{
					ret = ret.replace("%[[" + i + "]]", m.group(i));
				}
			}
			else if (matchResult.Condition.AuxiliaryData instanceof NameValuePairSubstitutionGroup)
			{
				NameValuePairSubstitutionGroup aux = (NameValuePairSubstitutionGroup)matchResult.Condition.AuxiliaryData;
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
}
