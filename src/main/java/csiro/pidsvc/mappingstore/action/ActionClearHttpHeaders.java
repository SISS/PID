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

import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

/**
 * Clear HTTP header collection.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class ActionClearHttpHeaders extends AbstractAction
{
	public ActionClearHttpHeaders(Runner controller, Descriptor descriptor, MappingMatchResults matchResult)
	{
		super(controller, descriptor, matchResult);
	}

	@Override
	public void run()
	{
		_controller.getHttpHeaders().clear();
	}

	@Override
	public void trace()
	{
		run();
		trace("Clear HTTP headers");
	}
}
