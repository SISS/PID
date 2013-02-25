/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 * 
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 * 
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

var G_APPLICATION_PATH = "/pidsvc/"; 
$J(function() {
	if (location.href.match(/main_frameset.html$/gi))
	{
		var page = top.location.href.getQueryParam("page");
		$J("#fraContent").attr("src", (page && page !== true ? decodeURIComponent(page) : G_APPLICATION_PATH + "mappings.html"));
	}
	else if (top.location.href == this.location.href)
		location.href = G_APPLICATION_PATH + "?page=" + encodeURIComponent(location.href);
});
