/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 * 
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 * 
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

package csiro.pidsvc.mappingstore.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import csiro.pidsvc.helper.Http;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

/**
 * Returns 301 HTTP response code.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class Action301 extends AbstractAction
{
	private static Logger _logger = LogManager.getLogger(Action301.class.getName());

	public Action301(Runner controller, Descriptor descriptor, MappingMatchResults matchResult)
	{
		super(controller, descriptor, matchResult);
	}

	@Override
	public void run()
	{
		try
		{
			_controller.getResponse().setStatus(301);
			_controller.getResponse().addHeader("Location", getExpandedActionValue());
		}
		catch (Exception e)
		{
			_logger.error(e);
			Http.returnErrorCode(_controller.getResponse(), 500, e);
		}
	}

	@Override
	public void trace()
	{
		try
		{
			trace("Set HTTP response status: 301; location: " + getExpandedActionValue());
		}
		catch (Exception e)
		{
			_logger.error(e);
			trace("Set HTTP response status: 500; exception: " + e.getCause().getMessage());
		}
	}
}
