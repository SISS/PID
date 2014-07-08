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
		$J("#frmMerge").submit(this.onSubmitHandler);

		// Read JSON response from the server.
		var json = location.href.getQueryParam("json");
		if (json !== false)
		{
			json = $J.parseJSON(decodeURIComponent(json));
			if (json.error)
				$J("#Error").text(json.error).show();
			else
				parent.Main.mergeOnComplete(json.conditionChangeFlags);
		}		
	},

	onSubmitHandler: function()
	{
		var jq			= $J("#frmMerge");
		var action		= jq.attr("action");
		var mappingPath	= parent.Main.getMappingPath();
		var replace		= $J("#ReplaceConditions").is(":checked");

		if (mappingPath == null)
		{
			alert("Mapping must be saved before merging with another mapping configuration!");
			return false;
		}
		jq.attr("action", action + "&replace=" + (replace ? 1 : 0) + "&mapping_path=" + encodeURIComponent(mappingPath));
		return true;
	}
});