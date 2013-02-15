var Main = Class.construct({
	_focusPager: false,

	init: function()
	{
		// Initialise UI elements.
		$J("#SearchSection")
			.find("input:not(#PagerInput)")
				.keypress(this.searchOnKeyPress)
			.end()
			.find("select")
				.keypress(this.searchOnKeyPress);
		$J("#Pager input:first").keypress(this.pagerOnKeyPress);
		$J(document).keydown(this.globalDocumentOnKeyDown);
		$J("#Namespace").change(this.namespaceOnChange);
		$J("#LookupType").change(this.lookupTypeOnChange);
		$J("#DefaultBehaviourConstant, #DefaultBehaviourPassThrough").change(this.defaultBehaviourOnChange);

		$J("#Tip > div").css("opacity", .7);

		$J("#TopMenu > DIV.MenuButton").click(this.openTab);
		this.openTab(0);

		// Set ExceptionHandler properties.
		ExceptionHandler.setPostHandler(Main.unblockUI);

		// Set default parameters.
		this.search();
		this.blockSaving(true);

		// Reset mapping configuration.
		this.create(true);

		// Automatically retrieve configuration.
		var ns = location.href.getQueryParam("ns");
		if (ns !== false)
			this.getConfigByNamespace(decodeURIComponent(ns));

		this.initializeUploadControl();
	},

	///////////////////////////////////////////////////////////////////////////
	//	Global event handlers.

	globalDocumentOnKeyDown: function(event)
	{
		switch (event.which)
		{
			case 37: // Left arrow
				if (!$J(":focus").is(":input") && event.ctrlKey)
				{
					Main.pagerPrev();
					event.preventDefault();
				}
				break;
			case 39: // Right arrow
				if (!$J(":focus").is(":input") && event.ctrlKey)
				{
					Main.pagerNext();
					event.preventDefault();
				}
				break;
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
		$J("#SearchSection").unblock();
		$J("#ConfigSection").unblock();
		$J.unblockUI();
	},

	///////////////////////////////////////////////////////////////////////////
	//	Change monitoring.

	monitorChanges: function()
	{
		$J("#ConfigSection")
			.find("input[configurationOnChange != '1'], select[configurationOnChange != '1']")
				.attr("configurationOnChange", "1")
				.change(Main.configurationOnChange);
	},

	configurationOnChange: function()
	{
		if (Main.isSavingBlocked())
			Main.blockSaving(false);
	},

	blockSaving: function(state)
	{
		if (state)
			$J("#cmdSave").attr("disabled", "disabled");
		else
			$J("#cmdSave").removeAttr("disabled");
	},

	isSavingBlocked: function()
	{
		return $J("#cmdSave").is(":disabled");
	},

	///////////////////////////////////////////////////////////////////////////
	//	Handling search results.

	search: function(page)
	{
		if (!page)
		{
			Main._focusPager = false;
			page = 1;
		}
		Main.blockUI($J("#SearchSection"));

		// Call service.
		var request = "info?cmd=search_lookup&page=" + page + "&ns=" + encodeURIComponent($J("#SearchNsText").val());
		$J.getJSON(request, this.renderResults).fail(ExceptionHandler.renderGenericException);
	},

	renderResults: function(data)
	{
		Main.unblockUI();

		// Check for errors.
		if ($J("#SearchResultsTable .__error").size() > 0)
			return;

		// Render results.
		if (data != null)
		{
			$J("#SearchResultsTable tr:gt(0)").remove();
			$J("#SearchResults").show();
			if (data.results && data.results.length)
			{
				$J(data.results).each(function() {
					$J("#SearchResultsTable")
						.append(
							"<tr valign='top'>" +
							"	<td>" +
							"		<a href='#' ns='" + this.ns + "' class='__link' style='font-weight: bold;'>" + this.ns + "</a>" +
							"	</td>" +
							"	<td nowrap='nowrap'>" + this.type + "</td>" +
							"</tr>"
						);
				});
				$J("#SearchResultsTable tr:gt(0) > td").hover(
					function() { $J(this).siblings().andSelf().addClass("ResultRowHighlight"); },
					function() { $J(this).siblings().andSelf().removeClass("ResultRowHighlight"); }
				);
				$J("#SearchResultsTable .__link").click(Main.getConfig);

				// Initialise pager control.
				$J("#MenuResultsCount").text(data.count ? "(" + data.count + ")" : "");
				if (data.pages > 1)
				{
					$J("#Pager")
						.show()
						.find("input:first")
							.val(data.page)
						.end()
						.find("span:first")
							.text(data.pages);
					if (Main._focusPager)
						$J("#Pager input:first").select();
				}
				else
					$J("#Pager").hide();
			}
			else
			{
				$J("#MenuResultsCount").text("");
				$J("#SearchResultsTable")
					.append(
						"<tr valign='top'>" +
						"	<td colspan='3'>No records found...</td>" +
						"</tr>"
					);
				$J("#Pager").hide();
			}
		}
		Main._focusPager = false;
	},

	searchOnKeyPress: function(event)
	{
		if (event.which == 13)
		{
			event.preventDefault();
			Main.search();
		}
	},

	pagerOnKeyPress: function(event)
	{
		if (event.which == 13)
		{
			event.preventDefault();
			Main._focusPager = true;
			Main.pagerGoto($J("#Pager input:first").val().toInt(-1));
		}
	},

	pagerPrev: function()
	{
		Main.pagerGoto($J("#Pager input:first").val().toInt(-2) - 1);
	},

	pagerNext: function()
	{
		var page = $J("#Pager input:first").val().toInt(-2) + 1;
		Main.pagerGoto(page < 0 ? parseInt($J("#Pager span:first").text()) : page);
	},

	pagerGoto: function(page)
	{
		if ($J("#Pager").is(":hidden"))
			return;
		if (page <= 0)
			page = 1;

		var totalPages = parseInt($J("#Pager span:first").text());
		if (page > totalPages)
			page = totalPages;

		$J("#Pager input:first").val(page);
		Main.search(page);
	},

	///////////////////////////////////////////////////////////////////////////
	//	Configuration UI.

	create: function(initialize)
	{
		if (initialize !== true)
			$J("#Tip").hide();
		this.renderConfig(null);
	},

	delete: function()
	{
		var config = $J("#ConfigSection").data("config");
		if (!config || !config.ns)
			return this.create();

		if (!confirm("Are you sure wish to delete \"" + config.ns + "\" lookup map?"))
			return;

		// Delete existing record.
		this.blockUI();
		$J.ajax("controller?cmd=delete_lookup&ns=" + encodeURIComponent(config.ns), {
				type: "POST",
				cache: false
			})
			.done(this.renderConfig.bind(this, null))
			.fail(ExceptionHandler.displayGenericException);
	},

	getConfig: function(internalCall)
	{
		internalCall = (internalCall === true);

		var ns = $J(this).attr("ns");
		if (!ns)
			ns = $J("#Namespace").val();

		$J("#Tip").hide();
		Main.openTab(1);

		if (!internalCall)
			Main.blockUI($J("#ConfigSection"));

		$J.getJSON("info?cmd=get_lookup_config&ns=" + encodeURIComponent(ns), Main.renderConfig).fail(ExceptionHandler.renderGenericException);
	},

	getConfigByNamespace: function(ns)
	{
		$J("#Tip").hide();
		Main.openTab(1);
		Main.blockUI($J("#ConfigSection"));
		$J.getJSON("info?cmd=get_lookup_config&ns=" + encodeURIComponent(ns), Main.renderConfig).fail(ExceptionHandler.renderGenericException);
	},

	getConfigByLink: function(e)
	{
		Main.getConfigByNamespace($J(this).text());
	},

	renderConfig: function(data)
	{
		// Save last loaded configuration in memory.
		$J("#ConfigSection").data("config", data);

		// Reset configuration.
		if (!data || $J.isEmptyObject(data))
		{
			if (data != null && $J.isEmptyObject(data))
				alert("Mapping is not found!");

			$J("#Namespace").val("");
			$J("#LookupType").val("Static").attr("__current", "Static");
			$J("#LookupMap").empty();
			$J("#DefaultBehaviourConstantValue").val("");
			$J("#DefaultBehaviourConstant").click();
			Main.appendLookupRecord(null);

			Main.blockSaving(true);
			Main.monitorChanges();
			Main.unblockUI();
			return;
		}

		// Show configuration.
		$J("#Namespace").val(data.ns);
		$J("#LookupType").val(data.type).attr("__current", data.type);
		if (data.default.type == "Constant")
		{
			$J("#DefaultBehaviourConstant").click();
			$J("#DefaultBehaviourConstantValue").val(data.default.value);
		}
		else
		{
			$J("#DefaultBehaviourPassThrough").click();
			$J("#DefaultBehaviourConstantValue").val("");
		}

		// Initialize lookup map.
		if (data.lookup)
		{
			$J("#LookupMap").empty();
			switch (data.type)
			{
				case "Static":
					if (!data.lookup.length)
						Main.appendLookupRecord(null);
					else
						$J(data.lookup).each(Main.appendLookupRecord);
					Main.checkLookupRecordUniqueness(true);
					break;
				case "HttpResolver":
					Main.appendHttpLookupRecord(data.lookup);
					break;
			}
		}

		Main.monitorChanges();
		Main.blockSaving(true);
		Main.unblockUI();
	},

	namespaceOnChange: function()
	{
		var jq			= $J("#Namespace");
		var config		= $J("#ConfigSection").data("config");
		var originalNs	= config ? config.ns : null;
		var newNs		= jq.val().trim();

		if (originalNs == null)
			return;
		originalNs = originalNs.trim();
		jq.val(newNs);
		if (originalNs != "" && originalNs != newNs && !confirm("You have changed the namespace for the lookup map. It may override another map or cause some URI mappings to work incorrectly.\n\nDo you wish to continue?"))
			jq.val(originalNs);
	},

	lookupTypeOnChange: function(e)
	{
		var jqType = $J("#LookupType");
		var lookupType = jqType.val();

		// Check is selection is reverted to original.
		var config = $J("#ConfigSection").data("config");
		if (config && config.type == lookupType)
		{
			Main.renderConfig(config);
			return;
		}

		// Change to a new type.
		var currentType = jqType.attr("__current");
		var warnUser = (
				currentType == "Static" && $J("#LookupMap").find("INPUT[value != '']").size() > 0 ||
				currentType == "HttpResolver" && ($J("#LookupMap INPUT.__lookupEndpoint").val() != "http://" || $J("#LookupMap INPUT.__lookupExtractor").val() != "")
			);
		if (warnUser && !confirm("Changing lookup map type will erase your current settings!\n\nDo you want to proceed?"))
		{
			jqType.val(currentType);
			e.stopImmediatePropagation();
			return;
		}
		jqType.attr("__current", lookupType);

		// Show an appropriate empty form.
		var jqMap = $J("#LookupMap").empty();
		switch (lookupType)
		{
			case "Static":
			{
				Main.appendLookupRecord(null);
				break;
			}
			case "HttpResolver":
			{
				Main.appendHttpLookupRecord(null);
				break;
			}
		}
	},

	defaultBehaviourOnChange: function(e)
	{
		if ($J(this).val() == "Constant")
			$J("#DefaultBehaviourConstantValue").removeAttr("disabled");
		else
			$J("#DefaultBehaviourConstantValue").attr("disabled", "disabled");
	},

	//
	//	Lookup map handling.
	//

	addLookupRecord: function()
	{
		Main.appendLookupRecord(null);
		Main.blockSaving(false);
	},

	removeGenericLookupRecord: function()
	{
		var jq = $J(this).parents("tr:first");
		var isEmpty = jq.find("input[value != '']").size() == 0;

		// Confirm action only if values aren't empty.
		if (!isEmpty && !confirm("Are you sure wish to delete this record?"))
			return;

		if (jq.parents("table:first").find("a.__remove").size() > 1)
			jq.remove();
		else
		{
			// Do not remove the last record.
			jq.find("input").val("");
		}
		Main.blockSaving(false);
	},

	appendLookupRecord: function(json)
	{
		if (json == null || !json.key)
			json = this;
		if (json == null || !json.key)
			json = { key: null, value: null };

		$J("#AddLookupRecordCmd").show();
		return $J("#LookupMap")
			.append(
				"<tr>" +
				"	<td><input class=\"__lookupKey\" type=\"text\" style=\"width: 315px;\" maxlength=\"255\" /></td>" +
				"	<td><img src=\"Images/arrow_right.png\" width=\"16\" height=\"16\" /></td>" +
				"	<td><input class=\"__lookupValue\" type=\"text\" style=\"width: 315px;\" maxlength=\"255\" /></td>" +
				"	<td><a href='#' class='__remove'><img src='Images/delete.png' title='Remove' width='16' height='16' border='0' style='position: relative; top: 1px;'/></a></td>" +
				"</tr>"
			)
			.find("input.__lookupKey:last")
				.val(json.key)
				.change(Main.checkLookupRecordUniqueness)
			.end()
			.find("input.__lookupValue:last")
				.val(json.value)
			.end()
			.find("a.__remove:last")
				.click(Main.removeGenericLookupRecord);
	},

	checkLookupRecordUniqueness: function(silent)
	{
		var jq = $J("#LookupMap INPUT.__lookupKey").css("background", "");
		var totalNonUnique = jq.filter("INPUT[nonunique = '1']").size();

		// Find and highlight duplicates.
		jq.removeAttr("nonunique");
		jq.each(function() {
			var jqThis = $J(this);
			if (jqThis.val().trim() == "" || jqThis.attr("nonunique") == "1")
				return;
			var jqDuplicates = jq.filter("INPUT[value = '" + jqThis.val() + "']");
			if (jqDuplicates.size() > 1)
				jqDuplicates.attr("nonunique", "1").css("background", "#FFE6E6");
		});

		// Give a warning message for non-silent calls.
		if (silent !== true && jq.filter("INPUT[nonunique = '1']").size() > totalNonUnique)
			alert("Please note that some lookup keys aren't unique.\nLookup behaviour is undefined for such lookup maps!");
	},

	appendHttpLookupRecord: function(json)
	{
		if (!json || !json.endpoint)
			json = { endpoint: null, type: null, extractor: null, namespaces: null };

		$J("#AddLookupRecordCmd").hide();
		var jqRet = $J("#LookupMap")
			.append(
				"<tr>" +
				"	<td nowrap=\"nowrap\">HTTP Endpoint:</td>" +
				"	<td align=\"right\"><input class=\"__lookupEndpoint\" type=\"text\" style=\"width: 604px;\"/></td>" +
				"</tr>" +
				"<tr>" +
				"	<td></td>" +
				"	<td class=\"tip\">Set the HTTP endpoint that will be called to resolve lookup key. Lookup key will be appended to the end of this URL. Alternatively use $0 placeholder to indicate the place where lookup key needs to be placed.</td>" +
				"</tr>" +
				"<tr>" +
				"	<td>Extractor:</td>" +
				"	<td align=\"right\">" +
				"		<select class=\"input __lookupExtractorType\" style=\"width: 100px;\">" +
				"			<option value=\"Regex\">Regex</option>" +
				"			<option value=\"XPath\">XPath</option>" +
				"		</select>" +
				"		<input class=\"__lookupExtractor\" type=\"text\" style=\"width: 500px;\"/></td>" +
				"</tr>" +
				"<tr>" +
				"	<td></td>" +
				"	<td class=\"tip\">Use either Regex or XPath-based extractor to retrive the value for resolved lookup key or leave empty to use the whole response.</td>" +
				"</tr>" +
				"<tr id=\"NamespaceDeclarationSection\">" +
				"	<td></td>" +
				"	<td>Namespace declarations:<br/>" +
				"		<table id=\"NamespaceDeclaration\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\"></table>" +
				"		<table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">" +
				"			<tr>" +
				"				<td align=\"right\"><a href='#' class='__addNamespaceDeclaration'><img src='Images/plus_16.png' title='Add' width='16' height='16' border='0' style='position: relative; top: 1px; margin: 5px 0 5px 0;'/></a></td>" +
				"			</tr>" +
				"		</table>" +
				"	</td>" +
				"</tr>"
			)
			.find("input.__lookupEndpoint")
				.val(json.endpoint ? json.endpoint : "http://")
			.end()
			.find("select.__lookupExtractorType")
				.val(json.type)
				.change(Main.extractorTypeOnChange)
				.change()
			.end()
			.find("input.__lookupExtractor")
				.val(json.extractor)
			.end()
			.find("a.__addNamespaceDeclaration")
				.click(Main.appendNamespaceDeclaration)
			;

		// Append namespace declarations.
		if (!json.namespaces || !json.namespaces.length)
			Main.appendNamespaceDeclaration(null);
		else
			$J(json.namespaces).each(Main.appendNamespaceDeclaration);
		return jqRet;
	},

	extractorTypeOnChange: function()
	{
		if ($J("#LookupMap SELECT.__lookupExtractorType").val() == "XPath")
			$J("#NamespaceDeclarationSection").show();
		else
			$J("#NamespaceDeclarationSection").hide();
	},

	appendNamespaceDeclaration: function(json)
	{
		if (json == null || !json.prefix)
			json = this;
		if (json == null || !json.prefix)
			json = { prefix: null, uri: null };

		$J("#NamespaceDeclaration")
			.append(
				"<tr>" +
				"	<td>" +
				"		<input class=\"__lookupNsPrefix\" type=\"text\" title=\"Namespace prefix\" style=\"width: 97px;\"/>" +
				"		<input class=\"__lookupNsUri\" type=\"text\" title=\"Namespace URI\" style=\"width: 477px;\"/>" +
				"	</td>" +
				"	<td align=\"right\"><a href='#' class='__remove'><img src='Images/delete.png' title='Remove' width='16' height='16' border='0' style='position: relative; top: 1px; margin: 5px 0 5px 0;'/></a></td>" +
				"</tr>"
			)
			.find("input.__lookupNsPrefix:last")
				.val(json.prefix)
			.end()
			.find("input.__lookupNsUri:last")
				.val(json.uri)
			.end()
			.find("a.__remove:last")
				.click(Main.removeGenericLookupRecord)
			;
		Main.blockSaving(false);
	},

	///////////////////////////////////////////////////////////////////////////
	//	Saving.

	save: function()
	{
		var ns = $J("#Namespace").val().trim();
		if (!ns || Main.isSavingBlocked())
			return;

		var config		= $J("#ConfigSection").data("config");
		var oldns		= config ? config.ns : null;
		var type		= $J("#LookupType").val();
		var defaultType	= $J("INPUT:radio[name = 'DefaultBehaviour']:checked").val();

		// Basic data.
		var cmdxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		cmdxml += "<lookup xmlns=\"urn:csiro:xmlns:pidsvc:lookup:1.0\">";
		cmdxml += "<ns" + (oldns && oldns != ns ? " rename=\"" + oldns.htmlEscape() + "\"" : "") + ">" + ns.htmlEscape() + "</ns>";
		cmdxml += "<default type=\"" + defaultType + "\">" + (defaultType == "Constant" ? $J("#DefaultBehaviourConstantValue").val().htmlEscape() : "") + "</default>";
		cmdxml += "<" + type + ">";

		// Lookup configuration.
		switch (type)
		{
			case "Static":
			{
				$J("#LookupMap TR").each(function() {
					var jqThis = $J(this);
					var key = jqThis.find("INPUT.__lookupKey").val();
					if (key.trim() == "")
						return;
					cmdxml += "<pair>";
					cmdxml += "<key>" + jqThis.find("INPUT.__lookupKey").val().htmlEscape() + "</key>";
					cmdxml += "<value>" + jqThis.find("INPUT.__lookupValue").val().htmlEscape() + "</value>";
					cmdxml += "</pair>";
				});
				break;
			}
			case "HttpResolver":
			{
				var jq = $J("#LookupMap");
				var endpoint = jq.find("INPUT.__lookupEndpoint").val().trim();
				var extractorType = jq.find("SELECT.__lookupExtractorType").val();
				var extractor = jq.find("INPUT.__lookupExtractor").val().trim();

				if (endpoint == "" || endpoint == "http://")
				{
					alert("Configuration is incomplete!\nHTTP endpoint is empty.");
					return;
				}
				cmdxml += "<endpoint>" + endpoint.htmlEscape() + "</endpoint>";
				cmdxml += "<type>" + extractorType + "</type>";
				cmdxml += "<extractor>" + extractor.htmlEscape() + "</extractor>";

				// Namespaces.
				if ($J("#NamespaceDeclarationSection").is(":visible"))
				{
					var namespaces = "";
					$J("#NamespaceDeclaration tr").each(function() {
						var jqThis = $J(this);
						var prefix = jqThis.find("input.__lookupNsPrefix").val().trim();
						var uri = jqThis.find("input.__lookupNsUri").val().trim();
	
						if (!!prefix && !!uri)
							namespaces += "<ns prefix=\"" + prefix + "\">" + uri + "</ns>";
					});
					if (namespaces != "")
						cmdxml += "<namespaces>" + namespaces + "</namespaces>";
				}
				break;
			}
		}

		cmdxml += "</" + type + ">";
		cmdxml += "</lookup>";

//		alert(cmdxml);

		// Submit request.
		Main.blockUI();
		$J.ajax("controller?cmd=create_lookup", {
				type: "POST",
				cache: false,
				contentType: "text/xml",
				data: cmdxml
			})
			.done(Main.getConfig.bind(Main, true))
			.fail(ExceptionHandler.displayGenericException);
	},

	export: function()
	{
		if (Main.isSavingBlocked() && $J("#ConfigSection").data("config"))
			location.href = "controller?cmd=export_lookup&ns=" + encodeURIComponent($J("#Namespace").val());
		else
			alert("You must save the mapping before exporting!");
	},

	exportAll: function()
	{
		location.href = "controller?cmd=export_lookup";
	},

	///////////////////////////////////////////////////////////////////////////
	//	Import.

	initializeUploadControl: function()
	{
		$J("#swfuploadControl").swfupload({
				upload_url:				"controller?cmd=import_lookup",
				file_size_limit:		"1GB",
//				file_types:				"*.*",
//				file_types_description:	"All Files",
				file_types:				"*.psl",
				file_types_description:	"PID Service Lookup Map Files",
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

				// Start upload.
				$J("#swfuploadControl").swfupload("startUpload");
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
						serverData = RegExp.$1.replace(/\[\[\[(.+?)\]\]\]/gi, "<a href=\"#\">$1</a>");
					$J("#trfile" + file.id)
						.find("> td:last > img")
							.attr("src", "Images/tick.png")
						.end()
						.find("> td:first")
							.append("<br/><span class='tip'>" + serverData + "</span>")
							.find("a")
								.click(Main.getConfigByLink);
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
	}
});