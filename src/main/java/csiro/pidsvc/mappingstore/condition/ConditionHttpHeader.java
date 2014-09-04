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

/**
 * Match HTTP request header.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class ConditionHttpHeader extends AbstractConditionCollectionSearch
{
	public ConditionHttpHeader(URI uri, HttpServletRequest request, int id, String match, Object matchAuxiliaryData)
	{
		super(uri, request, id, match, matchAuxiliaryData);
	}

	@Override
	public String getValue(String name)
	{
		return _request.getHeader(name);
	}
}
