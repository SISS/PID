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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import csiro.pidsvc.helper.URI;

/**
 * Abstract base class implements generic collection search logic.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public abstract class AbstractConditionCollectionSearch extends AbstractCondition
	implements ICollectionSearch
{
	public AbstractConditionCollectionSearch(URI uri, HttpServletRequest request, int id, String match, Object matchAuxiliaryData)
	{
		super(uri, request, id, match, matchAuxiliaryData);
	}

	@Override
	public boolean matches()
	{
		try
		{
			String[]	nameValuePairs = this.Match.split("(?<!\\\\)&");
			String[]	nameValuePair;
			String		paramName, paramValue, paramPattern;
			Boolean		optionalParam;
			Pattern		re;
			Matcher		m;

			NameValuePairSubstitutionGroup aux = new NameValuePairSubstitutionGroup(nameValuePairs.length); 

			for (String pair : nameValuePairs)
			{
				nameValuePair	= pair.replace("\\&", "&").split("=", 2);
				paramName		= nameValuePair[0];
				paramPattern	= nameValuePair.length > 1 ? nameValuePair[1] : ".*"; // If parameter pattern isn't present then check for parameter presence.

				// Check if parameter is optional.
				if (optionalParam = paramName.endsWith("?"))
					paramName = paramName.substring(0, paramName.length() - 1);

				paramValue	= this.getValue(paramName);
				re			= Pattern.compile(paramPattern, Pattern.CASE_INSENSITIVE);

				if (paramValue == null)
				{
					if (optionalParam)
						continue;
					else
						return false;
				}
				m = re.matcher(paramValue);

				// If at least one parameter doesn't match then return false.
				if (!m.find())
					return false;

				// Save the Matcher for each parameter.
				aux.put(paramName, m);
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
