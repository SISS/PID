package csiro.pidsvc.mappingstore.action;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import csiro.pidsvc.mappingstore.action.Runner;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public abstract class AbstractAction
{
	public abstract void run(Runner controller, Descriptor actionDescriptor, MappingMatchResults matchResult);
	
	protected String substrituteCaptureParameters(String input, Descriptor actionDescriptor, MappingMatchResults matchResult) throws URISyntaxException, UnsupportedEncodingException
	{
		String actionValue = actionDescriptor.Value.replaceAll("\\$\\{C\\:(\\d+)\\}", "%[[$1]]");
		String ret;

		// Regex from URI matching.
		if (matchResult.AuxiliaryData instanceof Pattern)
			ret = input.replaceAll(((Pattern)matchResult.AuxiliaryData).pattern(), actionValue);
		else
			ret = actionValue;

		// Matches from condition regex matching.
		if (matchResult.Condition != null && matchResult.Condition.AuxiliaryData instanceof Matcher)
		{
			Matcher m = (Matcher)matchResult.Condition.AuxiliaryData;
			for (int i = 1; i <= m.groupCount(); ++i)
			{
				ret = ret.replace("%[[" + i + "]]", m.group(i));
			}
		}
		
		return ret;
	}
}
