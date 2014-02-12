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
		$J.getJSON("info?cmd=get_manifest", Main.renderVersionTag);
	},

	renderVersionTag: function(data)
	{
		var jq = $J("#VersionTag");
		jq.html(jq.html() + " v" + data.manifest["version"].replace(/^(\d+\.\d+)(?:-.*)?$/gi, "$1") + "." + data.manifest["Implementation-Build"]);
	}
});
