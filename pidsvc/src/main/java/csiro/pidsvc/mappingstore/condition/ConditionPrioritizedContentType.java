package csiro.pidsvc.mappingstore.condition;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import csiro.pidsvc.helper.URI;

public class ConditionPrioritizedContentType extends AbstractCondition
{
	public ConditionPrioritizedContentType(URI uri, HttpServletRequest request, int id, String match)
	{
		super(uri, request, id, match);
	}
	
	protected String _acceptHeader = null;

	@Override
	public boolean matches()
	{
		if (_acceptHeader == null)
			return false;

		try
		{
			Pattern re = Pattern.compile(this.Match, Pattern.CASE_INSENSITIVE);
			Matcher m = re.matcher(_acceptHeader);

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
	
	public static ConditionPrioritizedContentType getMatchingCondition(Vector<ConditionPrioritizedContentType> prioritizedConditions)
	{
		if (prioritizedConditions.size() == 0)
			return null;
		
		String acceptHeader = prioritizedConditions.get(0)._request.getHeader("Accept");
		if (acceptHeader == null || acceptHeader.isEmpty())
			return null;

		String acceptList[] = acceptHeader.split(",\\s+");
		for (String accept : acceptList)
		{
			if (accept.equalsIgnoreCase("*/*"))
				return prioritizedConditions.get(0);
			for (ConditionPrioritizedContentType condition : prioritizedConditions)
			{
				condition._acceptHeader = accept;
				if (condition.matches())
					return condition;
			}
		}
		
		return null;
	}
}
