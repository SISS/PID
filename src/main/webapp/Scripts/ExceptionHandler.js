/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 * 
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 * 
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

var ExceptionHandler = Class.construct({
	_postHandler: null,

	setPostHandler: function(fnPtr)
	{
		this._postHandler = fnPtr;
	},

	displayGenericException: function(jqXHR, textStatus, errorThrown)
	{
		var msg;
		if (jqXHR.status != 200)
			msg = jqXHR.status + " " + jqXHR.statusText;
		else
			msg = errorThrown.name + " (" + textStatus + ")\n" + errorThrown.message;

		// Get extended error message.
		var errorHeader = jqXHR.getResponseHeader("X-PID-Service-Exception");
		if (errorHeader)
			msg += "\n\n" + errorHeader;

		alert(msg);
		ExceptionHandler._postHandler && ExceptionHandler._postHandler();
	},

	renderGenericException: function(jqXHR, textStatus, errorThrown)
	{
		var msg;
		if (jqXHR.status != 200)
			msg = jqXHR.status + " " + jqXHR.statusText;
		else
			msg = errorThrown.name + " (" + textStatus + ")<br/>" + errorThrown.message;

		// Get extended error message.
		var errorHeader = jqXHR.getResponseHeader("X-PID-Service-Exception");
		if (errorHeader)
			msg += "<br/><br/>" + errorHeader;

		$J("#SearchResultsTable")
			.find("tr:gt(0)")
				.remove()
			.end()
			.append(
				"<tr valign='top' class='__error'>" +
				"	<td colspan='" + $J("#SearchResultsTable TR:first > TD").size() + "'>" + msg + "</td>" +
				"</tr>"
			);
		$J("#Pager").hide();
		$J("#SearchResults").show();
		ExceptionHandler._postHandler && ExceptionHandler._postHandler();
	}
});