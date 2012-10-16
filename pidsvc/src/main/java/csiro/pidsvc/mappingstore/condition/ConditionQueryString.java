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
			String[]	qsPairs = this.Match.split("&");
			String[]	queryString;
			String		queryStringParam;
			Boolean		optionalParam;
			Pattern		re;
			Matcher		m;

			NameValuePairSubstitutionGroup aux = new NameValuePairSubstitutionGroup(qsPairs.length); 
			
			for (String pair : qsPairs)
			{
				queryString = pair.split("=", 2);

				// Check if query string parameter is optional.
				if (optionalParam = queryString[0].endsWith("?"))
					queryString[0] = queryString[0].substring(0, queryString[0].length() - 1);
				
				queryStringParam	= _uri.getQuerystringParameter(queryString[0]);
				re					= Pattern.compile(queryString[1], Pattern.CASE_INSENSITIVE);

				if (queryStringParam == null)
				{
					if (optionalParam)
						continue;
					else
						return false;
				}
				m = re.matcher(queryStringParam);

				// If at least one query string parameter doesn't match then return false.
				if (!m.find())
					return false;

				// Save the Matcher for each query string parameters.
				aux.put(queryString[0], m);
			}

			this.AuxiliaryData = aux;
			return true;
		}
		catch (Exception e)
		{
		}
		return false;
	}
}
