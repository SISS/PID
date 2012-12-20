package csiro.pidsvc.mappingstore.condition;

import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import csiro.pidsvc.helper.URI;
import csiro.pidsvc.mappingstore.condition.helper.PrioritizedQueue;

public class ConditionContentType extends AbstractCondition
{
	public ConditionContentType(URI uri, HttpServletRequest request, int id, String match, Object matchAuxiliaryData)
	{
		super(uri, request, id, match, matchAuxiliaryData);
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

	public static ConditionContentType getMatchingCondition(Vector<ConditionContentType> prioritizedConditions)
	{
		if (prioritizedConditions.size() == 0)
			return null;

		String acceptHeader = prioritizedConditions.get(0)._request.getHeader("Accept");
		if (acceptHeader == null || acceptHeader.isEmpty())
			return null;

		List<String> acceptList = (new PrioritizedQueue(acceptHeader)).getQueue();
		for (String accept : acceptList)
		{
			if (accept.equalsIgnoreCase("*/*") || accept.equalsIgnoreCase("*"))
				return prioritizedConditions.get(0);
			for (ConditionContentType condition : prioritizedConditions)
			{
				condition._acceptHeader = accept;
				if (condition.matches())
					return condition;
			}
		}
		return null;
	}
}
