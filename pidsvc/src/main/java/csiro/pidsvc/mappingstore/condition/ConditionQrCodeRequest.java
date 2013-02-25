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
 * Special condition type that indicates user QR code request.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class ConditionQrCodeRequest extends AbstractCondition
{
	public ConditionQrCodeRequest(URI uri, HttpServletRequest request)
	{
		super(uri, request, SpecialConditionType.QR_CODE_REQUEST, null, null);
	}

	@Override
	public boolean matches()
	{
		return false;
	}
}
