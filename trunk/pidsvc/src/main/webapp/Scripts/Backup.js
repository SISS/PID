var Main = Class.construct({
	init: function()
	{
		// Initialise UI elements.
		$J(document).keydown(this.globalDocumentOnKeyDown);

		$J("#TopMenu > DIV.MenuButton").click(this.openTab);
		this.openTab(0);
		
		$J("#PurgeDataStore").click(function() {
			if ($J(this).is(":checked") && !confirm("This operation is irreversible and will cause complete loss of mapping configuration.\nDo you want to proceed?"))
				$J(this).removeAttr("checked");
		});

		if ($J.browser.msie)
		{
			// For smarties.
			$J("#BackupLoader")
				.bind("readystatechange", function() {
					if (this.readyState.match(/interactive|complete/gi))
						Main.unblockUI();
				});
		}

		this.initializeUploadControl();
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
	//	Error handling.

	displayGenericError: function(jqXHR, textStatus, errorThrown)
	{
		if (jqXHR.status != 200)
			alert(jqXHR.status + " " + jqXHR.statusText);
		else
			alert(errorThrown.name + " (" + textStatus + ")\n" + errorThrown.message);
		Main.unblockUI();
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
		$J("#BackupSection").unblock();
		$J("#RestoreSection").unblock();
		$J.unblockUI();
	},

	///////////////////////////////////////////////////////////////////////////
	//	Backup.

	backup: function()
	{
		if ($J.browser.msie)
			Main.blockUI($J("#BackupSection"));
		$J("#BackupLoader").attr("src", "controller?cmd=" + ($J("#BackupTypeFull").is(":checked") ? "full" : "partial") + "_backup&deprecated=" + $J("#IncludeDeprecated").is(":checked").toString().toLowerCase());
	},

	///////////////////////////////////////////////////////////////////////////
	//	Restore.

	initializeUploadControl: function()
	{
		$J("#swfuploadControl").swfupload({
				upload_url:				"controller?cmd=import",
				file_size_limit:		"1GB",
//				file_types:				"*.*",
//				file_types_description:	"All Files",
				file_types:				"*.psb",
				file_types_description:	"PID Service Backup Files",
				file_upload_limit:		"0",
				flash_url:				"Scripts/jQuery/swfupload/vendor/swfupload.swf",
				button_image_url:		"Scripts/jQuery/swfupload/vendor/XPButtonUploadText_61x22.png",
				button_width:			61,
				button_height:			22,
				button_placeholder:		$J("#UploadButton")[0],
				assume_success_timeout:	0,
				debug:					false
			})
			.bind("fileQueued", function(event, file) {
				$J("#UploadQueue")
					.append(
						"<tr id='trfile" + file.id + "' valign='top'>" +
						"	<td>" + file.name + "</td>" +
						"	<td><div class='ProgressBar' style='width: 130px;'><div style='width: 0; height: 7px;'/></div></td>" +
						"	<td align='center'></td>" +
						"</tr>"
					)
					.find("tr:gt(0) > td").hover(
						function() { $J(this).siblings().andSelf().addClass("ResultRowHighlight"); },
						function() { $J(this).siblings().andSelf().removeClass("ResultRowHighlight"); }
					)
					.parents("DIV:first")
						.show();

				// Purge data store option.
				var jqPurgeDataStore = $J("#PurgeDataStore");
				if (!jqPurgeDataStore.is(":disabled") && jqPurgeDataStore.is(":checked"))
					Main.purgeDataStore();
				else
					Main.purgeDataStoreDone();
			})
			.bind("fileQueueError", function(event, file, errorCode, message) {
				$J("#UploadQueue")
					.append(
						"<tr valign='top'>" +
						"	<td><strike>" + file.name + "</strike><br/><span class='tip'>Error: " + message + "</span></td>" +
						"	<td></td>" +
						"	<td></td>" +
						"</tr>"
					)
					.find("tr:gt(0) > td").hover(
						function() { $J(this).siblings().andSelf().addClass("ResultRowHighlight"); },
						function() { $J(this).siblings().andSelf().removeClass("ResultRowHighlight"); }
					)
					.parents("DIV:first")
						.show();
			})
			.bind("uploadStart", function(event, file) {
				$J("#trfile" + file.id + " > td:last")
					.html("<img src='Images/hourglass.png' width='16' height='16'/>");
			})
			.bind("uploadProgress", function(event, file, bytesLoaded) {
				var progress = Math.ceil(bytesLoaded * 100 / file.size);
				if (progress > 100)
					progress = 100; // Chrome calculates percentage incorrectly (especially for small files).
				$J("#trfile" + file.id + " > td:eq(1) > div > div").css("width", progress + "%");
			})
			.bind("uploadSuccess", function(event, file, serverData) {
				if (serverData.match(/ERROR:\s*(.*)/i))
					Main.uploadError(event, file, SWFUpload.UPLOAD_ERROR.HTTP_ERROR, RegExp.$1);
				else
				{
					if (serverData.match(/OK:\s*(.*)/i))
						serverData = RegExp.$1.replace(/\[\[\[(.+?)\]\]\]/gi, "<a>$1</a>");
					$J("#trfile" + file.id)
						.find("> td:last > img")
							.attr("src", "Images/tick.png")
						.end()
						.find("> td:first")
							.append("<br/><span class='tip'>" + serverData + "</span>")
							.find("a")
								.each(function() {
									var jq = $J(this);
									jq.attr("href", "mappings.html?mapping_path=" + encodeURIComponent(jq.text()));
								});
				}
			})
			.bind("uploadComplete", function(event, file) {
				// Upload has completed, lets try the next one in the queue.
				$J(this).swfupload("startUpload");
			})
			.bind("uploadError", Main.uploadError);		
	},

	uploadError: function(event, file, errorCode, message)
	{
		$J("#trfile" + file.id)
			.find("> td:last > img")
				.attr("src", "Images/messagebox_warning.png")
			.end()
			.find("> td:first")
				.wrapInner("<strike/>")
				.append("<br/><span class='tip'>Error (" + errorCode + "): " + message + "</span>");
	},

	///////////////////////////////////////////////////////////////////////////
	//	Purge.

	purgeDataStore: function()
	{
		Main.blockUI($J("#RestoreSection"));
		$J.ajax("controller?cmd=purge_data_store", {
				type: "POST",
				cache: false
			})
			.done(this.purgeDataStoreDone)
			.fail(this.displayGenericError);
	},

	purgeDataStoreDone: function()
	{
		$J("#RestoreOptions").hide().find("input").attr("disabled", true);
		Main.unblockUI();
		$J("#swfuploadControl").swfupload("startUpload");
	}
});
