var Main = Class.construct({
	init: function()
	{
		// Initialise UI elements.
		$J(document).keydown(this.globalDocumentOnKeyDown);

		$J("#TopMenu > DIV.MenuButton").click(this.openTab);
		this.openTab(0);

		// Set ExceptionHandler properties.
		ExceptionHandler.setPostHandler(Main.unblockUI);

		this.getSettings();
	},

	///////////////////////////////////////////////////////////////////////////
	//	Global event handlers.

	globalDocumentOnKeyDown: function(event)
	{
		switch (event.which)
		{
			case 8: // Backspace
				if (!$J(":focus").is(":input"))
				{
					Main.openTab(-1);
					event.preventDefault();
				}
				break;
		}
	},

	///////////////////////////////////////////////////////////////////////////
	//	UI handling.

	openTab: function(arg0)
	{
		var tabIndex = Object.isNumber(arg0) ? arg0 : $J(this).index();

		// Toggle tab.
		if (tabIndex == -1)
			tabIndex = $J("#TopMenu > DIV.MenuButtonActive").index() ? 0 : 1;

		$J("#TopMenu > DIV:eq(" + tabIndex + ")")
			.addClass("MenuButtonActive")
			.siblings()
				.removeClass("MenuButtonActive");

		if ($J("#Containers > DIV:eq(" + tabIndex + ")").is(":hidden"))
		{
			$J("#Containers > DIV:not(" + tabIndex + ")").fadeOut();
			$J("#Containers > DIV:eq(" + tabIndex + ")").fadeIn();
		}
	},

	blockUI: function(jq)
	{
		var settings = {
				message: "<img src='Images/loading319.gif' width='128' height='128'/>",
				overlayCSS: {
					opacity: .8,
					backgroundColor: "#fff"
				},
				css: {
					  border: "none",
					  backgroundColor: "",
					  color: "#fff"
				   }
			};
		if (jq)
			jq.block(settings);
		else
			$J.blockUI(settings);
	},

	unblockUI: function()
	{
		$J("#GeneralSection").unblock();
		$J.unblockUI();
	},

	///////////////////////////////////////////////////////////////////////////
	//	Page operations.

	getSettings: function()
	{
		Main.blockUI();
		$J.getJSON("info?cmd=get_settings", function(data) {
				var jq;
				for (var i = 0; i < data.length; ++i) 
				{
					jq = $J("#" + data[i].name);
					switch (jq.attr("type"))
					{
						case "checkbox":
							jq.attr("checked", data[i].value == "1");
							break;
						default:
							jq.val(data[i].value);
							break;
					}
				}
				Main.unblockUI();
			})
			.fail(this.renderResultsError);
	},

	save: function()
	{
		var postData = {};
		$J("input.__persistent_option")
			.each(function() {
				var jq = $J(this);
				var val;
				switch (jq.attr("type"))
				{
					case "checkbox":
						val = jq.is(":checked") ? 1 : 0;
						break;
					default:
						val = jq.val();
						break;
				}
				postData[jq.attr("id")] = val;
			});

		// Execute AJAX request.
		Main.blockUI();
		$J.ajax("controller?cmd=save_settings", {
				type: "POST",
				data: postData
			})
			.done(this.unblockUI)
			.fail(ExceptionHandler.displayGenericException);
	}
});
