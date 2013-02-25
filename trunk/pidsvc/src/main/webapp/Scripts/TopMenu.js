/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 * 
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 * 
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

var Main = Class.construct({
	init: function()
	{
		$J.getJSON("info?cmd=is_new_version_available", Main.isNewVersionAvailableOnLoad);
	},

	isNewVersionAvailableOnLoad: function(data)
	{
		if (data.isAvailable)
		{
			$J("#NewVersionAvailable")
				.find("a")
					.attr("href", data.repository)
				.end()
				.show();
		}
	}
});
