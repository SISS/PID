package csiro.pidsvc.mappingstore.condition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import csiro.pidsvc.helper.URI;

public class ConditionQueryString extends AbstractCondition
{
	public ConditionQueryString(URI uri, HttpServletRequest request, int id, String match)
	{
		super(uri, request, id, match);

	}

	@Override
	public boolean matches()
	{
		try
		{
			String[]	queryString = this.Match.split("=", 2);
			String		queryStringParam = _uri.getQuerystringParameter(queryString[0]);
			Pattern		re = Pattern.compile(queryString[1], Pattern.CASE_INSENSITIVE);
			Matcher		m = re.matcher(queryStringParam);

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
