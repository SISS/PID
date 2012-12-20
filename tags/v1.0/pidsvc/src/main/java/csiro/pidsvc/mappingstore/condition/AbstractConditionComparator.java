package csiro.pidsvc.mappingstore.condition;

import javax.servlet.http.HttpServletRequest;

import csiro.pidsvc.helper.URI;
import csiro.pidsvc.mappingstore.FormalGrammar;

public abstract class AbstractConditionComparator extends AbstractCondition
	implements IComparator
{
	public AbstractConditionComparator(URI uri, HttpServletRequest request, int id, String match, Object matchAuxiliaryData)
	{
		super(uri, request, id, match, matchAuxiliaryData);
	}

	@Override
	public boolean matches()
	{
		try
		{
			FormalGrammar	grammar = new FormalGrammar(_uri, _request, _matchAuxiliaryData, null);
			String[]		nameValuePairs = this.Match.split("(?<!\\\\)&");
			String[]		operands;
			String			leftOperand, rightOperand;

			for (String pair : nameValuePairs)
			{
				operands		= pair.replace("\\&", "&").split("(?<!\\\\)=", 2);
				leftOperand		= grammar.parse(operands[0].replace("\\=", "="), false);
				rightOperand	= grammar.parse(operands[1].replace("\\=", "="), false);

				if (leftOperand.isEmpty() || rightOperand.isEmpty() || !this.compare(leftOperand, rightOperand))
					return false;
			}
			return true;
		}
		catch (Exception e)
		{
		}
		return false;
	}
}
