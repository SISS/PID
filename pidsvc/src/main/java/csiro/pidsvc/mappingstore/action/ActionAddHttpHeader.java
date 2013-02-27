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
 * Add additional HTTP header.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class ActionAddHttpHeader extends AbstractAction
{
	private static Logger _logger = LogManager.getLogger(ActionAddHttpHeader.class.getName());

	public ActionAddHttpHeader(Runner controller, Descriptor descriptor, MappingMatchResults matchResult)
	{
		super(controller, descriptor, matchResult);
	}

	@Override
	public void run()
	{
		try
		{
			String val = getExpandedActionValue();
			_controller.getHttpHeaders().put(_descriptor.Name, val);
			if (isTraceMode())
				trace("Add HTTP header; name: " + _descriptor.Name + "; value: " + val);
		}
		catch (Exception e)
		{
			_logger.error(e);
			Http.returnErrorCode(_controller.getResponse(), 500, e);
			if (isTraceMode())
				trace("Set HTTP response status 500; exception: " + e.getCause().getMessage());
		}
	}

	@Override
	public void trace()
	{
		run();
	}
}
