/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 * 
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 * 
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

package csiro.pidsvc.mappingstore.condition;

import javax.servlet.http.HttpServletRequest;

import csiro.pidsvc.helper.URI;
import csiro.pidsvc.mappingstore.FormalGrammar;

/**
 * Abstract base class that implements basic comparator logic.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
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
				leftOperand		= grammar.parse(FormalGrammar.unescape(operands[0]), false);

				if (operands.length == 1)
				{
					// If only one operand is present and is not empty then expression equates to true.
					if (leftOperand.isEmpty())
						return false;
				}
				else
				{
					rightOperand = grammar.parse(FormalGrammar.unescape(operands[1]), false);
					if (leftOperand.isEmpty() || rightOperand.isEmpty() || !this.compare(leftOperand, rightOperand))
						return false;
				}
			}
			return true;
		}
		catch (Exception e)
		{
		}
		return false;
	}
}
