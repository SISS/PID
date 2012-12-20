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
