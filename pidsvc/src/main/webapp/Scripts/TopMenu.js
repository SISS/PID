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
	_svcInstanceLabelWidth:		null,
	_isNewVersionAvailable:		false,

	init: function()
	{
		$J.getJSON("info?cmd=is_new_version_available", Main.isNewVersionAvailableOnLoad.bind(this));

		$J(window).resize(this.onWindowResize.bind(this)).resize();

		this.setInstanceBaseURI(GlobalSettings.BaseURI);
	},

	isNewVersionAvailableOnLoad: function(data)
	{
		if (this._isNewVersionAvailable = data.isAvailable)
		{
			$J("#NewVersionAvailable")
				.find("a")
					.attr("href", data.repository)
				.end()
				.show();
			this.onWindowResize();
		}
	},

	setInstanceBaseURI: function(baseURI)
	{
		GlobalSettings.BaseURI = baseURI;
		this._svcInstanceLabelWidth = parseInt($J("#BaseURI").text(baseURI).width());
	},

	onWindowResize: function(ev)
	{
		if (this._isNewVersionAvailable)
			$J("#NewVersionAvailable")[this._svcInstanceLabelWidth && $J(window).width() - 500 > this._svcInstanceLabelWidth ? "show" : "hide"]();
	}
});
