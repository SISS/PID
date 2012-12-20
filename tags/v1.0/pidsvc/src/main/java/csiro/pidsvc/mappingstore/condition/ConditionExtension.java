package csiro.pidsvc.mappingstore.condition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import csiro.pidsvc.helper.URI;

public class ConditionExtension extends AbstractCondition
{
	public ConditionExtension(URI uri, HttpServletRequest request, int id, String match, Object matchAuxiliaryData)
	{
		super(uri, request, id, match, matchAuxiliaryData);
	}

	@Override
	public boolean matches()
	{
		try
		{
			Pattern re = Pattern.compile(this.Match, Pattern.CASE_INSENSITIVE);
			Matcher m = re.matcher(_uri.getExtension());

			if (m.find())
			{
				this.AuxiliaryData = m;
				return true;
			}
		}
		catch (Exception e)
		{
		}
		return false;
	}
}
