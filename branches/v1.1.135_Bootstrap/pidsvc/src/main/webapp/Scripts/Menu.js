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
		var jq = $J("#VersionTag")
		var version, build;
		if (data.manifest)
		{
			version = data.manifest["version"]
			build = data.manifest["Implementation-Build"];
		}
		if (jq && version && build)
			jq.html(jq.html() + " v" + version.replace(/^(\d+\.\d+)(?:-.*)?$/gi, "$1") + "." + build);
	}
});
