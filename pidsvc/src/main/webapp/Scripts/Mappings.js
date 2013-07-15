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
	_actionTypesDescriptions: {
		"301": "Moved permanently to a target URL.<br/>Value points to resource location.",
		"302": "Simple redirection to a target URL.<br/>Value points to resource location.",
		"303": "See other URLs.<br/>Value points to resource location.",
		"307": "Temporary redirect to a target URL.<br/>Value points to resource location.",
		"404": "Temporarily gone.<br/>No action parameters are required.",
		"410": "Permanently gone.<br/>No action parameters are required.",
		"415": "Unsupported Media Type.<br/>No action parameters are required.",
		"AddHttpHeader": "Add HTTP response header.<br/>Action name specifies a new HTTP header name and value defines its value.",
		"RemoveHttpHeader": "Remove HTTP response header.<br/>Action name specifies HTTP header name that is to be removed from HTTP header collection. No value is required.",
		"ClearHttpHeaders": "Clear HTTP response headers.<br/>No action parameters are required.",
		"Proxy": "Proxy request.<br/>Value points to resource location."
	},
	_defaultOverlaySettings: {
		overlayCSS: {
			opacity:				.8,
			backgroundColor:		"#fff",
			cursor:					""
		},
		css: {
			width:					"705px",
			top:					"80px",
			left:					"10px",
			border:					"solid 2px #bed600",
			backgroundColor:		"#fff",
			color:					"#000",
			padding:				"15px",
			cursor:					"",
			textAlign:				"left"
		},
		onOverlayClick: $J.unblockUI
	},

	JSON_SELF_IDENTIFIER:			encodeURIComponent("{\"id\": \"__this\", \"name\": \"&ltNew mapping&gt;\", \"data\": { \"css\": \"chart_label_current\" }}"),

	_tabActivationPostHandlers:		new Array(2),
	_forceMappingPathCheck:			false,

	_focusPager:					false,
	_timerCollapseChangeHistory:	null,
	_timerLoadQrCodeImg:			null,
	_timerResetDependencyChart:		null,

	_stInheritanceGraph:			null,
	_timerParentMappingEditing:		null,
	_newParent:						null,	// NULL - no change, 0 - change to Catch-all.

	init: function()
	{
		// Initialise global event handlers.
		$J("#UriSearchSection")
			.find("input:not(#PagerInput)")
				.keypress(this.searchOnKeyPress)
			.end()
			.find("select")
				.keypress(this.searchOnKeyPress);
		$J("#Pager input:first").keypress(this.pagerOnKeyPress);
		$J(document).keydown(this.globalDocumentOnKeyDown);
		$J("#MappingPath").change(this.mappingPathOnChange);

		// Initialise UI elements.
		this.appendAction($J("#DefaultAction"), null);
		$J("#TopMenu > DIV.MenuButton").click(this.openTab);
		this.openTab(0);

		// Set ExceptionHandler properties.
		ExceptionHandler.setPostHandler(Main.unblockUI);

		// Initialise context menus.
		$J.contextMenu({
			selector: "#cmdExport",
			trigger: "left",
			callback: Main.export,
			items: {
				"partial_export": { name: "Partial export (current only)", icon: "export", accesskey: "p" },
				"full_export": { name: "<nobr>Full export (preserves history) &nbsp;</nobr>", icon: "export", accesskey: "f" },
			}
		});
		$J.contextMenu({
			selector: "#cmdQrCode",
			trigger: "left",
			callback: Main.publishQrCode,
			items: {
				"1": { name: "Nano", icon: "barcode" },
				"100": { name: "100 px", icon: "barcode" },
				"120": { name: "120 px", icon: "barcode" },
				"150": { name: "150 px", icon: "barcode" },
				"200": { name: "200 px", icon: "barcode" },
				"300": { name: "300 px", icon: "barcode" },
				"custom_size": { name: "Custom size", icon: "barcode" }
			}
		});

		// Set change history style.
		$J("#ChangeHistory DIV")
			.slimscroll({
				height:			387,
				size:			5,
				distance:		0,
				railVisible:	true,
				railOpacity:	.15
			})
			.css("height", 195)
			.hover(
				function() {
					var scrollHeight = $J(this).get(0).scrollHeight;
					if (scrollHeight <= 195)
						return;
					clearTimeout(Main._timerCollapseChangeHistory);
					Main._timerCollapseChangeHistory = null;
					$J(this).animate({ height: (scrollHeight < 387 ? scrollHeight : 387) + "px" }, "fast", function() { $J(this).slimscroll(); });
				},
				function()
				{
					clearTimeout(Main._timerCollapseChangeHistory)
					Main._timerCollapseChangeHistory = setTimeout(function() {
						$J("#ChangeHistory").css("border", "none");
						$J(this).animate({ height: "195px" }, "slow");
					}.bind(this), 1000);
				}
			);

		// Set commit note styles.
		$J("#CommitNoteRO").dblclick(this.addCommitNote);
		$J("#CommitNote").attr("configurationOnChange", "1"); // Disable change monitoring for this field (this is not an actual change).

		$J("#DefaultAction").hover(this.conditionHoverOn, this.conditionHoverOff);

		// Inheritance handling.
		$J("#MappingParentName").click(Main.openSearchParentMapping);
		$J("#ParentEdit A, #MappingParent")
			.focus(function() {
				if (!Main._timerParentMappingEditing)
					return;
				clearTimeout(Main._timerParentMappingEditing);
				Main._timerParentMappingEditing = null;				
			})
			.blur(function() {
				if (Main._timerParentMappingEditing)
				{
					clearTimeout(Main._timerParentMappingEditing);
					Main._timerParentMappingEditing = null;
				}
				Main._timerParentMappingEditing = setTimeout(Main.cancelParentMappingEditing, 10);
			});
		$J("#MappingParent")
			.keyup(this.parentMappingOnKeyUp)
			.autocomplete({
				minLength:	2,
				source:		this.searchParentMapping,
				select:		this.selectParentMapping
			})
			.data("ui-autocomplete")
				._renderItem = function(ul, item)
				{
					if (!item.mapping_path)
						return $J("<li>").append("<a class=\"i\">" + item.label + "</a>").appendTo(ul);
					return $J("<li>").append("<a>" + item.label + (item.label == item.mapping_path ? "" : "<br/><span class=\"tip\" style=\"padding-left: 15px;\">" + item.mapping_path + "</span>") + "</a>").appendTo(ul);
				};

		// Regex tester.
		$J("#MappingPath, #txtUriTesting").keyup(this.testUriChanged);
		$J("#MappingType").change(this.mappingTypeOnChange);

		// Allow either MappingDeprecatedInclude or MappingDeprecatedOnly to be checked.
		$J("#UriSearchSection input:checkbox[id^='MappingDeprecated']")
			.removeAttr("checked")
			.click(function() {
				if (!$J(this).is(":checked"))
					return;
				$J("#UriSearchSection input:checkbox[id^='MappingDeprecated']")
					.not(":eq(" + $J(this).index("#UriSearchSection input:checkbox[id^='MappingDeprecated']") + ")")
						.removeAttr("checked");
			});

		// Set default parameters.
		var type = location.href.getQueryParam("type");
		if (type == "Deprecated")
		{
			$J("#MappingDeprecatedOnly").click();
			this.search();
		}
		else if (type)
		{
			$J("#SearchMappingType").val(type);
			this.search();
		}
		this.blockSaving(true);

		// Automatically retrieve mapping configuration.
		var qsMappingId = location.href.getQueryParam("mapping_id");
		var qsMappingPath = location.href.getQueryParam("mapping_path");
		if (qsMappingId === "-1")
		{
			// Create new mapping.
			this.openTab(1);
			this.create();
		}
		else if (qsMappingId !== false)
			this.getConfigByMappingId(qsMappingId);
		else if (qsMappingPath !== false)
			this.getConfigByMappingPath(decodeURIComponent(qsMappingPath));
		else
		{
			// Reset mapping configuration.
			this.create();
		}

		// Tip about Author field behaviour.
		if (GlobalSettings.AuthorizationName)
			$J("#MappingCreator").val(GlobalSettings.AuthorizationName).attr("title", "The author is automatically taken from your access credentials.");
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
			tabIndex = Main.getCurrentTabIndex() ? 0 : 1;

		$J("#TopMenu > DIV:eq(" + tabIndex + ")")
			.addClass("MenuButtonActive")
			.siblings()
				.removeClass("MenuButtonActive");

		if ($J("#Containers > DIV:eq(" + tabIndex + ")").is(":hidden"))
		{
			$J("#Containers > DIV:not(" + tabIndex + ")").fadeOut();
			$J("#Containers > DIV:eq(" + tabIndex + ")").fadeIn();
		}

		// Run tab post activation handlers.
		if (Main._tabActivationPostHandlers[tabIndex])
		{
			eval(Main._tabActivationPostHandlers[tabIndex]);
			Main._tabActivationPostHandlers[tabIndex] = null;
		}
	},

	getCurrentTabIndex: function()
	{
		return $J("#TopMenu > DIV.MenuButtonActive").index();
	},

	setTabPostActivationHandler: function(tabIndex, handler)
	{
		Main._tabActivationPostHandlers[tabIndex] = handler;
	},

	blockUI: function(jq)
	{
		var settings = {
				message: "<img src='Images/loading319.gif' width='128' height='128'/>",
				overlayCSS: {
					opacity:			.8,
					backgroundColor:	"#fff"
				},
				css: {
					border:				"none",
					backgroundColor:	"",
					color:				"#fff"
				}
			};
		if (jq)
			jq.block(settings);
		else
			$J.blockUI(settings);
	},

	unblockUI: function()
	{
		$J("#ConfigSection").unblock();
		$J("#UriSearchSection").unblock();
		$J.unblockUI();
	},

	isEditingBlocked: function()
	{
		return $J("#ConfigSupersededWarning").is(":visible");
	},

	blockEditing: function(state)
	{
		if (state)
		{
			$J("#ConfigSection input, #ConfigSection select, #ConfigSection textarea").attr("disabled", "disabled");
			$J("#ConfigSupersededWarning").show();
			$J("*.__supersededLock, #ConfigSaveWarning").hide();
			Main.displayQrCodeUI(false);

			$J("#CommitNoteSection A").hide();
			$J("#CommitNoteRO").removeAttr("title").css("cursor", "default");

			// Remove section title if there're no conditions defined.
			if (!$J("#ConditionSection").children().size())
				$J("#ConditionSection").prev().hide();
		}
		else
		{
			var config			= $J("#ConfigSection").data("config");
			var isCatchAll		= config != null && (config.mapping_id == 0 || config.mapping_path == null);
			var reinstated		= $J("#ConfigSaveWarning").is(":visible");

			if (!isCatchAll)
				$J(".__catchAllHide").show();
			$J("#ConfigSection input, #ConfigSection select, #ConfigSection textarea").removeAttr("disabled");

			// Unlock certain control only if reinstating deprecated mapping, otherwise unlock all controls.
			$J(reinstated ? "*.__supersededReinstate" : "*.__supersededLock").show();

			$J("#CommitNoteSection").show();
			if (config && config.commit_note)
				$J("#CommitNoteSection A.__preserveCommitNote").show();
			$J("#CommitNoteRO").attr("title", "Double click to add note").css("cursor", "pointer");
			
			// Open URI link is only visible for 1-to-1 mappings.
			if ($J("#MappingType").val() != "1:1")
				$J("#cmdQrCode").hide();
			else if (!config || $J("#ChangeHistory").attr("isDeprecated") == "1" || reinstated)
				Main.displayQrCodeUI(false);

			// Catch-all mapping.
			if (isCatchAll)
			{
				$J(".__catchAllHide").hide();
				$J("#MappingTitle").attr("disabled", "disabled");
			}
//			else
//				$J("#MappingType").change();

			// Ensure condition section title is visible.
			$J("#ConditionSection").prev().show();

			// Re-initialise selectors.
			$J("#DefaultAction td.__actionType > select, #ConditionSection td.__actionType > select").change();

			// Disable author/creator input for authenticate requests.
			if (GlobalSettings.AuthorizationName)
				$J("#MappingCreator").attr("disabled", "disabled");

			// Set visibility for controls affected by multiple parameters.
			$J("#cmdClone, #cmdDelete")[!isCatchAll && !reinstated ? "show" : "hide"]();
			$J("#cmdOpenChart")[!reinstated ? "show" : "hide"]();

			Main.monitorChanges();
		}
	},

	getOverlaySettings: function(jqMessage)
	{
		var obj = { message: jqMessage };
		jQuery.extend(obj, this._defaultOverlaySettings);
		return obj;
	},

	isOneToOneMapping: function()
	{
		return $J("#MappingType").val() == "1:1";
	},

	addCommitNote: function()
	{
		if (Main.isEditingBlocked())
			return;
		$J("#CommitNoteRO").hide();
		$J("#CommitNote").show().val("").select();
	},

	preserveCommitNote: function()
	{
		if (Main.isEditingBlocked())
			return;
		var config = $J("#ConfigSection").data("config");
		$J("#CommitNoteRO").hide();
		$J("#CommitNote").show().val(config && config.commit_note ? config.commit_note : "").select().change();
	},

	resetCommitNote: function()
	{
		$J("#CommitNoteSection")
			.show()
			.find("A.__preserveCommitNote")
				.hide();
		$J("#CommitNoteRO").text("").hide();
		$J("#CommitNote").val("").hide();
	},

	///////////////////////////////////////////////////////////////////////////
	//	Change monitoring.

	monitorChanges: function()
	{
		$J("#ConfigSection")
			.find("input[configurationOnChange != '1'], select[configurationOnChange != '1'], textarea[configurationOnChange != '1']")
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
		{
			$J("#cmdSave").removeAttr("disabled");
			if (GlobalSettings.AuthorizationName)
				$J("#MappingCreator").val(GlobalSettings.AuthorizationName);
		}
	},

	isSavingBlocked: function()
	{
		return $J("#cmdSave").is(":disabled");
	},

	mappingTypeOnChange: function(event)
	{
		var showRegexTester = $J("#MappingType").val() != "1:1";
		if (showRegexTester)
		{
			$J("#RegexTester").show();
			Main.testUriChanged();
		}
		else
		{
			$J("#RegexTester").hide();

			// Reset parent if 1:1 maping type is choosen.
			if (Main.getCurrentTabIndex() == 1 &&				// Mapping configuration view is active.
				Main._timerResetDependencyChart == null &&		// No parent reset timer is set.
				Main._newParent !== 0)							// New parent is not already set to Catch-all.
			{
				// Forcefully reset parent to Catch-all.
				Main._timerResetDependencyChart = setTimeout(Main.resetParentToCatchAll, 150);
			}
		}
	},

	///////////////////////////////////////////////////////////////////////////
	//	URI and regex testing.

	testUriChanged: function(event)
	{
		if (Main.isOneToOneMapping())
			return;

		var mappingPath		= $J("#MappingPath").val().trim();
		var val				= $J("#txtUriTesting").val().trim();

		// Get rid of hostname and querystring in URI if present.
		val = val.replace(new RegExp("^(?:\\w+://[^/]+)?([^\\?]+).*$", "i"), "$1");

		// Validate regular expression.
		var re;
		try
		{
			re = new RegExp(mappingPath, "i");
		}
		catch (ex)
		{
			// Display regular expression error message.
			Main.showRegexTester(true);
			$J("#imgUriTestingStatus").attr("src", "Images/messagebox_warning.png").attr("title", "Regex exception occurred");
			$J("#phMatchingGroupInfo").html("<div class=\"ellipsis\" style=\"width: 717px; font-family: Courier New; font-size: 12px;\">" + ex + "</div>");

			// Reset QR code image.
			Main.setQrCode(null);

			return;
		}

		// Value must begin with / and have at least on more character following it.
		var m = val.match(/^\/.+/) ? val.match(re) : null;
		var html = "";

		if (!m)
		{
			// Count non capturing groups.
			var count = mappingPath.match(/\((?!\?\:)/g);
			for (var i = 0; i <= (count ? count.length : 0); ++i)
				html += "<div class=\"ellipsis\" style=\"width: 717px; font-family: Courier New; font-size: 12px;\">$" + i + " = </div>";

			if (!mappingPath || !val)
				$J("#imgUriTestingStatus").attr("src", "Images/help-faq.png").attr("title", "Start typing URI...");
			else
				$J("#imgUriTestingStatus").attr("src", "Images/messagebox_warning.png").attr("title", "URI is NOT matching this mapping rule");

			// Reset QR code image.
			Main.setQrCode(null);
		}
		else
		{
			// Matching.
			var i = 0;
			m.each(function(it) {
				html += "<div class=\"ellipsis\" style=\"width: 717px; font-family: Courier New; font-size: 12px;\">$" + (i++) + " = " + (it ? it : "") + "</div>";
			});
			$J("#imgUriTestingStatus").attr("src", "Images/tick.png").attr("title", "URI is matching this mapping rule");

			// Set QR code image (only if mapping path has not been changed).
			var config			= $J("#ConfigSection").data("config");
			var originalPath	= config ? config.mapping_path : null;

			if (originalPath == null || originalPath == "" || originalPath != mappingPath)
				Main.setQrCode(null);
			else
			{
				clearTimeout(Main._timerLoadQrCodeImg);
				Main._timerLoadQrCodeImg = null;
				$J("#RegexTesterImg")
					.unbind("load")
					.load(Main.setQrCodeFromRegexTester.bind(Main, val))
					.attr("src", val + (val.indexOf("?") == -1 ? "?" : "&") + "_pidsvcqr=1");
				Main._timerLoadQrCodeImg = setTimeout(Main.setQrCode, 500);
			}
		}
		$J("#phMatchingGroupInfo").html(html);
	},

	setQrCodeFromRegexTester: function(uri)
	{
		clearTimeout(this._timerLoadQrCodeImg);
		Main._timerLoadQrCodeImg = null;
		this.setQrCode(uri);
	},
	
	showRegexTester: function(state)
	{
		var jq = $J("#URITesting");
		if (state === true)
			jq.show();
		else if (state === false)
			jq.hide();
		else
			jq.toggle();
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
		var request = "info?cmd=search&page=" + page +
			"&type=" + $J("#SearchMappingType").val() +
			"&mapping=" + encodeURIComponent($J("#SearchUriPattern").val()) +
			"&creator=" + encodeURIComponent($J("#SearchCreator").val()) +
			"&deprecated=" + ($J("#MappingDeprecatedInclude").is(":checked") ? 1 : ($J("#MappingDeprecatedOnly").is(":checked") ? 2 : 0));

		Main.blockUI($J("#UriSearchSection"));

		// Call service.
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
			if (data.results.length)
			{
				$J(data.results).each(function() {
					$J("#SearchResultsTable")
						.append(
							"<tr valign='top'>" +
							"	<td>" +
								(
									this.date_end == null ? (
										this.type == "Regex" ? (
											// Non-navigable link for Regex-based mappings.
											"<img src='Images/earth-icon.png' width='16' height='16' style='position: relative; top: 4px; margin-right: 2px;'/>"
										) : (
											// Navigable link for 1:1-based mappings.
											"<a href='" + this.mapping_path + "' target='_blank'><img src='Images/earth-icon.png' title='Open in a new window' width='16' height='16' border='0' style='position: relative; top: 4px; margin-right: 2px;'/></a>"
										)
									) : (
										// Disabled link for deprecated mappings.
										"<img src='Images/earth-icon-gray.png' title='Deprecated mapping' width='16' height='16' border='0' style='position: relative; top: 4px; margin-right: 2px;'/>"
									)
								) +
							"		<a href='#' mapping_id='" + this.mapping_id + "' class='__link' style='font-weight: bold;'" + (this.title ? " title='" + this.description + "'" : "") + ">" + this.mapping_path + "</a>" +
							"		<br/>" +
							"		<span class='tip' style='margin-left: 21px;'>" + (this.title ? this.title : (this.description ? this.description : "")) + "</span>" +
							"	</td>" +
							"	<td>" + (this.creator ? this.creator : "") + "</td>" +
							"	<td nowrap='nowrap'>" + this.date_start + " - " + (this.date_end == null ? "present" : this.date_end) + "</td>" +
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
				$J("#SearchResultsTable")
					.append(
						"<tr valign='top'>" +
						"	<td colspan='3'>No mappings found...</td>" +
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

	create: function()
	{
		this.renderConfig(null);
	},

	clone: function()
	{
		$J("#ConfigSection").data("config", null);
		Main._forceMappingPathCheck = true;

		// Preserve parent.
		var parent = Main.getParent();
		Main.setParentMapping(parent.title, parent.mappingPath);

		// Clear change history.
		$J("#ChangeHistory")
			.hide()
			.attr("isDeprecated", 0)
			.find("ul")
				.empty();

		Main.resetCommitNote();
		Main.displayQrCodeUI(false);
		Main.blockSaving(false);
		Main.blockEditing(false);

		// Place cursor into mapping path field.
		$J("#MappingPath").select();
	},

	delete: function()
	{
		if (Main.isEditingBlocked())
			return;

		var config		= $J("#ConfigSection").data("config");
		var mappingPath	= $J("#MappingPath").val().trim();

		if ((!config && !mappingPath) || $J("#ChangeHistory").attr("isDeprecated") == "1")
			return this.create();
		if (!confirm("Are you sure wish to delete \"" + (config ? config.mapping_path : mappingPath) + "\" mapping?"))
			return;

		if (config)
		{
			// Delete existing record.
			this.blockUI();
			$J.ajax("controller?cmd=delete_mapping&mapping_path=" + encodeURIComponent(config.mapping_path), {
					type: "POST",
					cache: false
				})
				.done(this.getConfig.bind(this, -1))
				.fail(ExceptionHandler.displayGenericException);
		}
		else
		{
			// Clear unsaved mapping.
			this.create();
		}
	},

	openChart: function()
	{
		var config = $J("#ConfigSection").data("config");
		if (!Main.isSavingBlocked() || !config)
		{
			alert("The mapping must be saved first!");
			return;
		}
		location.href = "chart.html" + (config.mapping_path == null ? "" : "?mapping_path=" + encodeURIComponent(config.mapping_path.trim()));
	},

	getConfig: function(id)
	{
		var internalCall	= Object.isNumber(id);
		var mappingId		= internalCall ? id : $J(this).attr("mapping_id");

		// Deactivate any post activation handlers.
		Main._tabActivationPostHandlers[1] = null;

		Main.openTab(1);
		if (!internalCall)
			Main.blockUI($J("#ConfigSection"));

		// If mappingId === -1 then get the latest configuration for the current mapping.
		if (mappingId === -1)
		{
			var path = $J("#MappingPath").val().trim();
			$J("#MappingPath").val(path);
			if (path == "")
			{
				// Catch-all mapping.
				$J.getJSON("info?cmd=get_pid_config&mapping_id=0", Main.renderConfig).fail(ExceptionHandler.displayGenericException);
			}
			else
				$J.getJSON("info?cmd=get_pid_config&mapping_path=" + encodeURIComponent(path), Main.renderConfig).fail(ExceptionHandler.displayGenericException);
		}
		else
			$J.getJSON("info?cmd=get_pid_config&mapping_id=" + mappingId, Main.renderConfig).fail(ExceptionHandler.displayGenericException);
	},

	getConfigByMappingId: function(mappingId, passive)
	{
		if (passive === true)
		{
			// In passive mode when mapping is not available it gives an alert and stays are the previous mapping.
			Main.blockUI($J("#ConfigSection"));
			$J.getJSON("info?cmd=get_pid_config&mapping_id=" + mappingId,
				function(data) {
					if (data == null || $J.isEmptyObject(data))
					{
						alert("Mapping is not found!");
						Main.unblockUI();
					}
					else
						Main.renderConfig(data);
				})
				.fail(ExceptionHandler.displayGenericException);
			return;
		}
		Main.openTab(1);
		Main.blockUI($J("#ConfigSection"));
		$J.getJSON("info?cmd=get_pid_config&mapping_id=" + mappingId, Main.renderConfig).fail(ExceptionHandler.displayGenericException);
	},

	getConfigByMappingPath: function(mappingPath, passive)
	{
		if (passive === true)
		{
			// In passive mode when mapping is not available it gives an alert and stays are the previous mapping.
			Main.blockUI($J("#ConfigSection"));
			$J.getJSON("info?cmd=get_pid_config&mapping_path=" + encodeURIComponent(mappingPath),
				function(data) {
					if (data == null || $J.isEmptyObject(data))
					{
						alert("Mapping is not found!");
						Main.unblockUI();
					}
					else
						Main.renderConfig(data);
				})
				.fail(ExceptionHandler.displayGenericException);
			return;
		}
		Main.openTab(1);
		Main.blockUI($J("#ConfigSection"));
		$J.getJSON("info?cmd=get_pid_config&mapping_path=" + encodeURIComponent(mappingPath), Main.renderConfig).fail(ExceptionHandler.displayGenericException);
	},

	setQrCode: function(mapping_path)
	{
		if (mapping_path)
		{
			var qruri = mapping_path + (mapping_path.indexOf("?") == -1 ? "?" : "&") + "_pidsvcqr";
			$J("#QRCode")
				.data("uri", qruri)
				.attr("src", qruri + "=120")
				.attr("title", "Open in a new window\n" + location.href.replace(/^(https?:\/\/.+?)\/.*$/gi, "$1" + mapping_path))
				.parent()
					.attr("href", mapping_path);
		}
		else
		{
			$J("#QRCode")
				.data("uri", null)
				.attr("src", "Images/emptyqr.png")
				.attr("title", "No QR Code is generated for Regex-based mappings.")
				.parent()
					.removeAttr("href");
		}
	},

	renderConfig: function(data)
	{
		// Save last loaded configuration in memory.
		$J("#ConfigSection").data("config", data == null || $J.isEmptyObject(data) ? null : data);

		// Reset configuration.
		if (data == null || $J.isEmptyObject(data))
		{
			if (data != null && $J.isEmptyObject(data))
				alert("Mapping is not found!");

			$J("#MappingPath").val("");
			$J("#MappingType").val("1:1").change();
			$J("#MappingTitle").val("");
			$J("#MappingDescription").val("");
			$J("#MappingCreator").val(GlobalSettings.AuthorizationName ? GlobalSettings.AuthorizationName : "");

			$J("#DefaultAction")
				.find("td.__actionType > select")
					.val("")
				.end()
				.find("input.__actionName")
					.val("")
				.end()
				.find("input.__actionValue")
					.val("")
				.end()
				.find("textarea.__description")
					.val("");
			$J("#ConditionSection").empty();
			$J("#ChangeHistory").hide();

			// Commit note section.
			Main.resetCommitNote();

			// Inheritance section.
			if (Main.getCurrentTabIndex() == 1)
				Main.resetParentMapping();
			else
				Main.setTabPostActivationHandler(1, "Main.resetParentMapping()");

			Main.displayQrCodeUI(false);
			Main.blockSaving(true);
			Main.blockEditing(false);
			Main.unblockUI();
			return;
		}

		var isCatchAll = data.mapping_id === 0;
		
		// Show configuration.
		$J("#MappingPath").val(data.ended ? data.original_path : data.mapping_path);
		$J("#MappingType").val(data.type).change();
		$J("#MappingTitle").val(data.title);
		$J("#MappingDescription").val(data.description);
		$J("#MappingCreator").val(data.creator);

		// Inheritance section.
		if (!isCatchAll)
		{
			Main.resetParentMapping(true);
			if (data.parent && data.parent.mapping_path)
			{
				Main.setParentMappingLink(data.parent.title ? data.parent.title : data.parent.mapping_path, data.parent.mapping_path);
				if (data.parent.active && data.parent.cyclic)
				{
					$J("#ParentWarningIcon")
						.attr("src", "Images/messagebox_warning.png")
						.attr("title", "Cyclic inheritance encountered!\nPlease inspect the inheritance chain and rectify the problem.")
						.show();
				}
				else if (!data.parent.active)
				{
					$J("#ParentWarningIcon")
						.attr("src", "Images/messagebox_warning.png")
						.attr("title", "The parent mapping \"" + data.parent.mapping_path + "\" doesn't exist or has been deprecated. The resolution will fall back to Catch-All mapping.")
						.show();
				}
			}
			Main.renderInheritanceGraph(data.parent && data.parent.graph ? data.parent.graph : {});
		}

		// QR Code.
		if (data.type == "1:1")
		{
			Main.setQrCode(data.mapping_path);
		}
		else
		{
			Main.setQrCode(null);
			Main.testUriChanged(null);
		}
		$J("#QRCodeHits").text(data.qr_hits);
		Main.displayQrCodeUI(true);

		// Default action.
		$J("#DefaultAction")
			.find("td.__actionType > select")
				.val(data.action ? data.action.type : "")
			.end()
			.find("input.__actionName")
				.val(data.action ? data.action.name : "")
			.end()
			.find("input.__actionValue")
				.val(data.action ? data.action.value : "")
			.end()
			.find("textarea.__description")
				.val(data.action ? data.action.description : "");

		// Add conditions.
		if (data.conditions)
		{
			$J("#ConditionSection").empty();
			$J(data.conditions).each(function() {
				Main.appendCondition($J("#ConditionSection"), this);

				// Add actions for each conditions.
				if (this.actions.length)
				{
					$J(this.actions).each(function() {
						Main.appendAction($J("#ConditionSection table.__actions:last"), this);
					});
				}
				else
					Main.appendAction($J("#ConditionSection table.__actions:last"), { type: "404" });
			});
		}

		// Commit note.
		$J("#CommitNoteSection").show();
		if (data.commit_note)
		{
			$J("#CommitNoteSection A.__preserveCommitNote").show();
			$J("#CommitNote").hide();
			$J("#CommitNoteRO").html(data.commit_note.htmlEscape().replace(/\n/g, "<br/>")).show();
		}
		else
		{
			if (data.ended)
				$J("#CommitNoteSection").hide();
			$J("#CommitNoteSection A.__preserveCommitNote").hide();
			$J("#CommitNote, #CommitNoteRO").hide();
		}

		// Change history.
		$J("#ChangeHistory")[data.history ? "show" : "hide"]()
			.attr("isDeprecated", data.history && data.history[0].date_end != null ? 1 : 0)
			.find("ul")
				.empty();
		$J(data.history).each(function() {
			$J("#ChangeHistory ul")
				.append(
					"<li><a href='#' mapping_id='" + this.mapping_id + "' title='" + (this.commit_note ? this.commit_note : "") + "'" + (this.mapping_id == data.mapping_id ? " style='font-weight: bold;'" : "") + ">" + this.date_start + " - " + (this.date_end == null ? "present" : this.date_end) + "</a>" +
						(this.creator ? "<br/><span class=\"tip\">by " + this.creator + "</span>" : "") +
					"</li>"
				);
		});
		$J("#ChangeHistory a").click(Main.getConfig);

		// Block editing for deprecated/superseded mappings.
		$J("#ConfigSupersededWarning, #ConfigSaveWarning").hide();
		Main.blockEditing(data.ended);

		Main.blockSaving(true);
		Main.unblockUI();
	},

	mappingPathOnChange: function()
	{
		var jq				= $J("#MappingPath");
		var config			= $J("#ConfigSection").data("config");
		var originalPath	= config ? config.mapping_path : null;
		var newPath			= jq.val().trim();

		if (originalPath == null)
			return;
		originalPath = originalPath.trim();
		jq.val(newPath);
		if (originalPath != "" && originalPath != newPath && !confirm("You have changed the URI pattern. It may override another mapping or cause some URIs to work incorrectly.\n\nDo you wish to continue?"))
		{
			// Revert changes back.
			jq.val(originalPath).keyup();
			return;
		}

		// Check that mapping path is still consistent with the parent (for one-to-one mappings only).
		if (Main.isOneToOneMapping() && Main._newParent !== 0)
		{
			var parent = Main._newParent ? Main._newParent : (config ? config.parent.mapping_path : null);
			if (parent && !Main.isPathParentPatternConformant(newPath, parent))
				Main.resetParentToCatchAll();
		}
	},

	//
	//	Inheritance handling.
	//

	initializeInheritanceGraph: function()
	{
		if (Main._stInheritanceGraph)
			return;
		Main._stInheritanceGraph = new $jit.ST({
			injectInto:			"infovis",
			levelDistance:		10,
			duration:			100,
			transition:			$jit.Trans.Quart.easeInOut,
			levelsToShow:		10,
			align:				"left",

			// Set Node and Edge styles.
			Node: {
				color:			"#fff",
				width:			132
			},
			Edge: {
				type:			"bezier",
				color:			"#20B4E6",
				lineWidth:		1.5
			},

			Tips: {
				enable: true,
				onShow: function(tip, node)
				{
					$J(tip).removeClass("tip").addClass("inheritance_graph_tip");
					if (node.id == -1)
						tip.innerHTML = "Some parents were collapsed for readability.<br/>Inspect the whole dependency tree in the Mapping Chart.";
					else if (node.id == -2)
						tip.innerHTML = (node.data.inheritors == 1 ? "There's " : "There're ") + node.data.inheritors + " " + (node.data.inheritors == 1 ? "mapping that depends" : "mappings that depend") + " on this mapping.";
					else
						tip.innerHTML = "<div class=\"ellipsis b\">" + (node.id === 0 ? "Built-in &lt;Catch-all&gt; mapping" : node.name) + "</div>" +
							(node.data.title ? "<div>" + node.data.mapping_path + "</div>" : "") +
							(node.data.description ? "<div style=\"margin-top: 10px; height: 50px; overflow: hidden; text-overflow: ellipsis;\">" + node.data.description + "</div>" : "") +
							(node.data.inheritors ? "<div style=\"margin-top: 10px; border-top: 1px solid #eee; padding-top: 7px;\"><b>Inheritors:</b> " + node.data.inheritors + "</div>" : "");
				}
			},

			onCreateLabel: function(label, node)
			{
				label.id = node.id;
				$J(label)
					.html(node.name)
					.addClass("chart_label")
					.addClass(node.data.css);
				label.onclick = Main.inheritanceGraphLabelOnClick.bind(this, node);
			}
		});
		Main._stInheritanceGraph.canvas.translate(-292, -30);
	},

	renderInheritanceGraph: function(data)
	{
		if (Main._timerResetDependencyChart)
		{
			clearTimeout(Main._timerResetDependencyChart);
			Main._timerResetDependencyChart = null;
		}
		Main.initializeInheritanceGraph();
		if (data == null || $J.isEmptyObject(data))
			return;
		Main._stInheritanceGraph.loadJSON(data);
		Main._stInheritanceGraph.compute();
		Main._stInheritanceGraph.onClick(Main._stInheritanceGraph.root);
	},

	inheritanceGraphLabelOnClick: function(node)
	{
		if (node.id === 0)
			Main.getConfigByMappingId(0);
		else if (node.id == node.data.mapping_path)
			Main.getConfigByMappingPath(node.data.mapping_path);
	},

	searchParentMapping: function(request, response)
	{
		var config = $J("#ConfigSection").data("config");
		$J.ajax({
			url:		"info?cmd=search_parent",
			dataType:	"json",
			data:		{ mapping_id: config && config.mapping_id, q: request.term },

			success: function(data)
			{
				data = $J.merge(data, [{ mapping_path: 0, title: "Catch-all" }]);
				response(
					$J.map(data, function(item) {
						var label = item.title ? item.title : item.mapping_path;
						return $J.extend(item, { label: label, value: label });
					})
				);
			}
		});
	},

	openSearchParentMapping: function(event)
	{
		// Ctrl + Click follows the link.
		if (event.ctrlKey)
		{
			var config		= $J("#ConfigSection").data("config");
			var parent		= Main._newParent ? Main._newParent : (config && config.parent.mapping_path ? config.parent.mapping_path : 0);

			event.preventDefault();
			if (parent === 0)
				Main.getConfigByMappingId(0, true);
			else
				Main.getConfigByMappingPath(parent, true);
			return;
		}

		if (Main.isEditingBlocked())
			return;
		$J("#ParentView").hide();
		$J("#ParentEdit").show();
		$J("#MappingParent").val("").select();
	},

	isPathParentPatternConformant: function(path, pattern)
	{
		return (new RegExp(pattern, "i")).match(path);
	},

	selectParentMapping: function(event, ui)
	{
		if (ui.item)
			Main.setParentMapping(ui.item.value, ui.item.mapping_path);
	},

	setParentMapping: function(title, mappingPath)
	{
		if (mappingPath && Main.isOneToOneMapping())
		{
			// Check that mapping URI matches pattern of the parent mapping.
			if (!Main.isPathParentPatternConformant($J("#MappingPath").val(), mappingPath))
			{
				alert("Selected parent mapping doesn't match the mapping URI.");
				Main.cancelParentMappingEditing();
				return;
			}
		}

		// Redraw inheritance chart.
		var config = $J("#ConfigSection").data("config");
		$J.getJSON(
				"info?cmd=get_mapping_dependencies" +
					(config ? "&mapping_id=" + config.mapping_id : "&json=" + Main.JSON_SELF_IDENTIFIER) +
					(mappingPath ? "&mapping_path=" + encodeURIComponent(mappingPath) : ""),
				Main.renderInheritanceGraph
			)
			.fail(ExceptionHandler.displayGenericException);

		// Save selection.
		$J("#ParentWarningIcon").hide();
		Main._newParent = mappingPath;
		Main.setParentMappingLink(title, mappingPath);
		Main.cancelParentMappingEditing();
		Main.configurationOnChange();
	},

	setParentMappingLink: function(title, mappingPath)
	{
		if (!title || !mappingPath)
			title = mappingPath = null;
		$J("#MappingParentName")
			.data("path", mappingPath)
			.text(title ? title : "Catch-all")
			.attr("title", (mappingPath ? mappingPath + "\n\n" : "") + "\"Ctrl + Click\" to follow the link...");
	},

	getParent: function()
	{
		var jq = $J("#MappingParentName");
		return {
			title:			jq.text(),
			mappingPath:	jq.data("path")
		};
	},

	resetParentToCatchAll: function()
	{
		Main.resetParentMapping(false, 0);
	},

	resetParentMapping: function(noRedraw, newParent)
	{
		if (noRedraw !== true)
		{
			var config = $J("#ConfigSection").data("config");
			$J.getJSON("info?cmd=get_mapping_dependencies" + (config ? "&mapping_id=" + config.mapping_id : "&json=" + Main.JSON_SELF_IDENTIFIER), Main.renderInheritanceGraph)
				.fail(ExceptionHandler.displayGenericException);
		}
		$J("#ParentWarningIcon").hide();
		Main._newParent = newParent === 0 ? 0 : null; 
		Main.setParentMappingLink(null, null);
		Main.cancelParentMappingEditing();
		Main.configurationOnChange();
	},

	cancelParentMappingEditing: function()
	{
		$J("#ParentEdit").hide();
		$J("#ParentView").show();
	},

	parentMappingOnKeyUp: function(event)
	{
		if (event.which == 27) // Esc
		{
			event.preventDefault();
			Main.cancelParentMappingEditing();
		}
	},

	//
	//	Conditions handling.
	//

	addCondition: function()
	{
		var jq = Main.appendCondition($J("#ConditionSection"), null).find("table.__actions:last");
		this.appendAction(jq, { type: "", name: "", value: "" });
		Main.blockSaving(false);
	},

	removeCondition: function()
	{
		if (Main.isEditingBlocked())
			return;
		if (confirm("Are you sure wish to delete this condition?"))
		{
			$J(this).parents("table:first").remove();
			Main.blockSaving(false);
		}
	},

	conditionMoveUp: function()
	{
		if (Main.isEditingBlocked())
			return;
		var jqThisItem = $J(this).parents("table:first");
		var jqOtherItem = jqThisItem.prev();
		if (jqOtherItem.size())
		{
			jqThisItem.insertBefore(jqOtherItem);
			Main.blockSaving(false);
		}
	},

	conditionMoveDown: function()
	{
		if (Main.isEditingBlocked())
			return;
		var jqThisItem = $J(this).parents("table:first");
		var jqOtherItem = jqThisItem.next();
		if (jqOtherItem.size())
		{
			jqThisItem.insertAfter(jqOtherItem);
			Main.blockSaving(false);
		}
	},

	appendCondition: function(jq, json)
	{
		if (json == null)
			json = { type: null, match: "", description: "" };

		return jq
			.append("<table border='0' cellpadding='5' cellspacing='0' width='100%' style='border: 1px solid #fff;'>" +
				"	<tr valign='top'>" +
				"		<td>" +
				"<a href='#' class='__conditionMoveUp'><img src='Images/arrow_up_small.gif' title='Move Up' width='12' height='6' border='0' style='position: relative; top: 2px;'/></a><br/>" +
				"<a href='#' class='__conditionMoveDown'><img src='Images/arrow_down_small.gif' title='Move Down' width='12' height='6' border='0' style='position: relative; top: 9px;'/></a>" +
				"</td>" +
				"		<td class='__conditionType'>" +
				"			<select class='input' style='width: 200px;'>" +
				"				<option value='Comparator'>Comparator</option>" +
				"				<option value='ComparatorI'>Comparator (case-insensitive)</option>" +
				"				<option value='ContentType'>ContentType</option>" +
				"				<option value='Extension'>Extension</option>" +
				"				<option value='HttpHeader'>HttpHeader</option>" +
				"				<option value='QueryString'>QueryString</option>" +
				"			</select>" +
				"		</td>" +
				"		<td>" +
				"			<input type='text' value='" + json.match + "' class='__conditionMatch' maxlength='255' style='width: 483px;' />" +
				"		</td>" +
				"	</tr>" +
				"	<tr valign='top'>" +
				"		<td></td>" +
				"		<td align='right' class='tip' style='padding-top: 8px;'><span class='__commentHeading' " + (json.description ? "" : "style='display: none;'") + ">Comment:</span></td>" +
				"		<td>" +
				"			<textarea class='__conditionDescription' rows='2' style='width: 483px; margin-bottom: 5px;" + (json.description ? "" : "display: none;") + "'>" + (json.description ? json.description : "") + "</textarea>" +
				"			<div align='right'>" +
								(json.description ? "" : "<span class='__supersededLock __supersededReinstate'><a href='#' class='__addConditionComment tip'><img src='Images/arrow_137.gif' width='9' height='9' border='0' style='margin-right: 5px; position: relative; top: 2px;'/>Add comment</a> &nbsp;</span>") +
				"				<a href='#' class='__toggleActions tip'><img src='Images/arrow_137.gif' width='9' height='9' border='0' style='margin-right: 5px; position: relative; top: 2px;'/>Actions</a>" +
				"				<span class='__supersededLock __supersededReinstate'>&nbsp; <a href='#' class='__removeCondition tip'><img src='Images/cross10.png' width='10' height='10' border='0' style='margin-right: 5px; position: relative; top: 2px;'/>Remove</a></span>" +
				"			</div>" +
				"		</td>" +
				"	</tr>" +
				"	<tr " + (json.type == null ? "" : "style='display: none;'") + ">" +
				"		<td colspan='3'>" +
				"			<div style='margin-left: 21px'>" +
				"				<div class='caps' style='background: #f2f2f2; border: 2px solid #fff; padding-left: 5px;'>Actions:</div>" +
				"				<table class='__actions' border='0' cellpadding='5' cellspacing='1' width='100%' style='position: relative; right: -6px;'>" +
				"				</table>" +
				"				<div class='__supersededLock __supersededReinstate' align='right'><a href='#' class='__addAction'><img src='Images/plus_16.png' title='Add action' width='16' height='16' border='0'/></a></div>" +
				"			</div>" +
				"		</td>" +
				"	</tr>" +
				"</table>"
			)
			.find("td.__conditionType > select:last")
				.val(json.type)
			.end()
			.find("a.__conditionMoveUp:last")
				.click(Main.conditionMoveUp)
			.end()
			.find("a.__conditionMoveDown:last")
				.click(Main.conditionMoveDown)
			.end()
			.find("a.__toggleActions:last")
				.click(Main.toggleActions)
			.end()
			.find("a.__removeCondition:last")
				.click(Main.removeCondition)
			.end()
			.find("a.__addAction:last")
				.click(Main.addAction)
			.end()
			.find("a.__addConditionComment:last")
				.click(Main.addConditionComment)
			.end()
			.find("> TABLE")
				.hover(Main.conditionHoverOn, Main.conditionHoverOff)
			.end();
	},

	conditionHoverOn: function()
	{
		$J(this).css({ "border": "1px solid #e2e2e2", "background": "#fafafa" });
	},

	conditionHoverOff: function()
	{
		$J(this).css({ "border": "1px solid #ffffff", "background": "none" });
	},

	addConditionComment: function()
	{
		$J(this)
			.parents("tr:first")
				.find("SPAN.__commentHeading")
					.show()
				.end()
				.find("TEXTAREA.__conditionDescription")
					.show()
					.select()
				.end()
			.end()
			.remove();
	},

	//
	//	Actions handling.
	//

	addAction: function()
	{
		Main.appendAction($J(this).parent().prev(), { type: "", name: "", value: "" });
		Main.blockSaving(false);
	},

	removeAction: function()
	{
		if (Main.isEditingBlocked())
			return;

		// Prevent the last action from removal.
		if (!$J(this).parents("tr:first").siblings().size())
			return;

		if (confirm("Are you sure wish to delete this action?"))
		{
			$J(this).parents("tr:first").remove();
			Main.blockSaving(false);
		}
	},

	toggleActions: function()
	{
		$J(this).parents("tr:first").next().toggle();
	},

	actionMoveUp: function()
	{
		if (Main.isEditingBlocked())
			return;
		var jqThisItem = $J(this).parents("tr:first");
		var jqOtherItem = jqThisItem.prev();
		if (jqOtherItem.size())
		{
			jqThisItem.insertBefore(jqOtherItem);
			Main.blockSaving(false);
		}
	},

	actionMoveDown: function()
	{
		if (Main.isEditingBlocked())
			return;
		var jqThisItem = $J(this).parents("tr:first");
		var jqOtherItem = jqThisItem.next();
		if (jqOtherItem.size())
		{
			jqThisItem.insertAfter(jqOtherItem);
			Main.blockSaving(false);
		}
	},

	appendAction: function(jq, json)
	{
		if (json == null || json.type == null)
			json = this;
		if (json == null || json.type == null)
			json = { type: null, name: "", value: "" };

		var isDefaultAction = (json.type == null);
		var elementWidth = null;

		if (isDefaultAction)
		{
			// Default action.
			elementWidth = {
				type: 	"184px",
				name:	"134px",
				value:	"332px"
			};
		}
		else
		{
			// Condition actions.
			elementWidth = {
				type: 	"166px",
				name:	"120px",
				value:	"283px"
			};
		}

		var scrollTop = $J(window).scrollTop();
		var ret = jq
			.append("<tr>" +
				(
						!isDefaultAction ?
						"<td>" +
						"<a href='#' class='__actionMoveUp'><img src='Images/arrow_up_small.gif' title='Move Up' width='12' height='6' border='0' style='position: relative; top: -4px;'/></a><br/>" +
						"<a href='#' class='__actionMoveDown'><img src='Images/arrow_down_small.gif' title='Move Down' width='12' height='6' border='0' style='position: relative; top: 3px;'/></a>" +
						"</td>"
					: ""
				) +
				"	<td class='__actionType'>" +
				"		<select class='input' style='width: " + elementWidth.type +";'>" +
				(!isDefaultAction ? "" : "<option></option>") +
				"			<option value='301'>301 Moved permanently</option>" +
				"			<option value='302'>302 Simple redirection</option>" +
				"			<option value='303'>303 See other</option>" +
				"			<option value='307'>307 Temporary redirect</option>" +
				"			<option value='404'>404 Temporarily gone</option>" +
				"			<option value='410'>410 Permanently gone</option>" +
				"			<option value='415'>415 Unsupported media type</option>" +
				"			<option value='Proxy'>Proxy</option>" +
				(
						!isDefaultAction ?
						"<option value='AddHttpHeader'>AddHttpHeader</option>" +
						"<option value='RemoveHttpHeader'>RemoveHttpHeader</option>" +
						"<option value='ClearHttpHeaders'>ClearHttpHeaders</option>"
						: ""
				) +
				"		</select>" +
				"	</td>" +
				"	<td>" +
				"		<td><a href='#' class='__actionTip'><img src='Images/help-faq.png' title='Tips' width='16' height='16' border='0' style='position: relative; left: -14px;'/></a></td>" +
				"	</td>" +
				"	<td>" +
				"		<input type='text' value='" + (json.name ? json.name : "") + "' class='__actionName' maxlength='50' style='width: " + elementWidth.name +";' />" +
				"	</td>" +
				"	<td align='right'>" +
				"		<input type='text' value='" + (json.value ? json.value : "") + "' class='__actionValue' maxlength='4096' style='width: " + elementWidth.value +";' />" +
				"	</td>" +
				(
						!isDefaultAction ? "<td><a href='#' class='__removeAction'><img src='Images/delete.png' title='Remove' width='16' height='16' border='0' style='position: relative; top: 1px;'/></a></td>" : ""
				) +
				"</tr>" +
				(
					isDefaultAction ?
						"<tr valign='top'>" +
						"	<td colspan='3' align='right' class='tip' style='padding-top: 8px;'>Comment:</td>" +
						"	<td colspan='2'>" +
						"		<textarea class='__description' rows='2' style='width: 483px; margin-bottom: 5px;'></textarea>" +
						"	</td>" +
						"</tr>"
					: ""
				)
			)
			.find("td.__actionType > select:last")
				.val(json.type)
				.change(Main.actionTypeOnChange)
				.change()
			.end()
			.find("a.__actionTip:last")
				.click(Main.actionTip)
			.end()
			.find("a.__actionMoveUp:last")
				.click(Main.actionMoveUp)
			.end()
			.find("a.__actionMoveDown:last")
				.click(Main.actionMoveDown)
			.end()
			.find("a.__removeAction:last")
				.click(Main.removeAction)
			.end();
		setTimeout("$J(window).scrollTop(" + scrollTop + ")", 0);
		return ret;
	},

	actionTypeOnChange: function()
	{
		var jq = $J(this).parents("tr:first");

		jq.find("input.__actionName").removeAttr("disabled");
		jq.find("input.__actionValue").removeAttr("disabled");
		$J("#DefaultAction TEXTAREA.__description").removeAttr("disabled");

		switch ($J(this).val())
		{
			case "301":
			case "302":
			case "303":
			case "307":
			case "Proxy":
				jq.find("input.__actionName").val("location").attr("disabled", "disabled");
				break;
			case "":
				$J("#DefaultAction TEXTAREA.__description").attr("disabled", "disabled");
			case "404":
			case "410":
			case "415":
			case "ClearHttpHeaders":
				jq.find("input.__actionName").val("").attr("disabled", "disabled");
				jq.find("input.__actionValue").val("").attr("disabled", "disabled");
				break;
			case "RemoveHttpHeader":
				jq.find("input.__actionValue").val("").attr("disabled", "disabled");
				break;
		}
	},

	actionTip: function()
	{
		var val				= $J(this).parents("tr:first").find("td.__actionType > select").val();
		var description		= Main._actionTypesDescriptions[val];

		if (!description)
			return;

		$J("#Tip > div").html(description);
		$J.blockUI(Main.getOverlaySettings($J("#Tip")));
	},

	///////////////////////////////////////////////////////////////////////////
	//	Saving.

	presaveCheck: function(data)
	{
		Main.unblockUI();
		if (data && data.exists && !confirm("The mapping \"" + data.mapping_path + "\" already exists.\n\nDo you wish to continue and overwrite existing mapping?"))
			return;
		Main.save(true);
	},

	save: function(prechecked)
	{
		if (Main.isSavingBlocked())
			return;

		var config		= $J("#ConfigSection").data("config");
		var oldpath		= config ? config.mapping_path : null;
		var path		= $J("#MappingPath").val();
		var isCatchAll	= config && config.mapping_path == null;

		if (!path && !isCatchAll)
			return;

		// Check and warn the user if necessary if he's just about to rewrite existing mapping.
		if (prechecked === true)
			Main._forceMappingPathCheck = false;
		if (Main._forceMappingPathCheck || !prechecked && oldpath && oldpath != path)
		{
			Main.blockUI();
			$J.getJSON("info?cmd=check_mapping_path_exists&mapping_path=" + encodeURIComponent(path.htmlEscape().trim()), Main.presaveCheck).fail(ExceptionHandler.displayGenericException);
			return;
		}

		var title			= isCatchAll ? null : $J("#MappingTitle").val().trim();
		var description		= $J("#MappingDescription").val().trim();
		var creator			= $J("#MappingCreator").val().trim();

		var jqCommitNote	= $J("#CommitNote");
		var commitNote		= jqCommitNote.is(":visible") ? jqCommitNote.val().trim() : "";

		// Basic data.
		var cmdxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		cmdxml += "<mapping xmlns=\"urn:csiro:xmlns:pidsvc:backup:1.0\">";
		if (isCatchAll)
		{
			cmdxml += "<path/>";
			cmdxml += "<type>Regex</type>";
		}
		else
		{
			cmdxml += "<path" + (oldpath && oldpath != path ? " rename=\"" + oldpath.htmlEscape() + "\"" : "") + ">" + path.htmlEscape().trim() + "</path>";
			if (Main._newParent === 0)
			{
				// Do nothing. Catch-all mapping.
			}
			else if (Main._newParent)
				cmdxml += "<parent>" + Main._newParent + "</parent>";
			else if (config && config.parent.mapping_path)
				cmdxml += "<parent>" + config.parent.mapping_path + "</parent>";
			cmdxml += "<type>" + $J("#MappingType").val() + "</type>";
			if (title)
				cmdxml += "<title>" + title.htmlEscape() + "</title>";
		}
		if (description)
			cmdxml += "<description>" + description.htmlEscape() + "</description>";
		if (creator)
			cmdxml += "<creator>" + creator.htmlEscape() + "</creator>";
		if (commitNote)
			cmdxml += "<commitNote>" + commitNote.htmlEscape() + "</commitNote>";

		// Default action.
		var jqDefault = $J("#DefaultAction");
		var defaultActionType = jqDefault.find("td.__actionType select").val();
		if (defaultActionType)
		{
			var defaultActionDescription = jqDefault.find("textarea.__description").val().trim();

			cmdxml += "<action>";
			cmdxml += "<type>" + defaultActionType + "</type>";
			cmdxml += "<name>" + jqDefault.find("input.__actionName").val().trim().htmlEscape() + "</name>";
			cmdxml += "<value>" + jqDefault.find("input.__actionValue").val().htmlEscape() + "</value>";
			if (defaultActionDescription)
				cmdxml += "<description>" + defaultActionDescription.htmlEscape() + "</description>";
			cmdxml += "</action>";
		}

		// Conditions.
		var jqConditions = $J("#ConditionSection > table");
		if (jqConditions.size() > 0)
		{
			cmdxml += "<conditions>";
			jqConditions.each(function() {
				var jqCondition = $J(this);
				var match = jqCondition.find("input.__conditionMatch").val().trim();
				if (!match)
					return;
				var conditionComment = jqCondition.find("textarea.__conditionDescription").val().trim();
	
				cmdxml += "<condition>";
				cmdxml += "<type>" + jqCondition.find("td.__conditionType select").val() + "</type>";
				cmdxml += "<match>" + match.htmlEscape() + "</match>";
				if (conditionComment)
					cmdxml += "<description>" + conditionComment.htmlEscape() + "</description>";

				var jqActions = jqCondition.find("table.__actions tr");
				if (jqActions.size() > 0)
				{
					cmdxml += "<actions>";
					jqActions.each(function() {
						// Serialize actions.
						var jqAction = $J(this);
						var actionName = jqAction.find("input.__actionName").val().trim().htmlEscape();
						var actionValue = jqAction.find("input.__actionValue").val().htmlEscape()
		
						if (!jqAction.find("input.__actionName").attr("disabled") && !actionName ||
							!jqAction.find("input.__actionValue").attr("disabled") && !actionValue)
							return;
						cmdxml += "<action>";
						cmdxml += "<type>" + jqAction.find("td.__actionType select").val() + "</type>";
						cmdxml += "<name>" + actionName + "</name>";
						cmdxml += "<value>" + actionValue + "</value>";
						cmdxml += "</action>";
					});
					cmdxml += "</actions>";
				}
				cmdxml += "</condition>";
			});
			cmdxml += "</conditions>";
		}
		cmdxml += "</mapping>";

//		alert(cmdxml); return;
//		$J(window.open().document.body).html(cmdxml.htmlEscape());

		// Submit request.
		Main.blockUI();
		$J.ajax("controller?cmd=create_mapping", {
				type: "POST",
				cache: false,
				contentType: "text/xml",
				data: cmdxml
			})
			.done(Main.getConfig.bind(Main, -1))
			.fail(ExceptionHandler.displayGenericException);
	},

	reinstateMapping: function()
	{
		if (!confirm("Are you sure wish to reinstate this mapping configuration?\n\nYou will be given a chance to make changes before saving new configuration."))
			return;
		$J("#ConfigSupersededWarning, #ConfigSaveWarning").toggle();
		this.blockEditing(false);
	},

	export: function(key, options)
	{
		var config = $J("#ConfigSection").data("config");
		if (!Main.isSavingBlocked() || !config)
		{
			alert("You must save the mapping before exporting!");
			return;
		}
		if (key == "full_export" && config.mapping_path == null)
			location.href = "controller?cmd=full_export&mapping_id=0";
		else if (key == "full_export")
			location.href = "controller?cmd=full_export&mapping_path=" + encodeURIComponent(config.mapping_path.trim());
		else
			location.href = "controller?cmd=partial_export&mapping_id=" + config.mapping_id;
	},

	///////////////////////////////////////////////////////////////////////////
	//	QR Codes.

	displayQrCodeUI: function(show)
	{
		$J("#QRCodeSection, #cmdQrCode")[show ? "show" : "hide"]();
	},

	publishQrCode: function(key, options)
	{
		var size = key.toInt(0);
		if (size === 0)
		{
			size = prompt("Enter the size of QR Code in pixels:", 100);
			if (!size)
				return;
			if ((size = size.toInt(0)) === 0)
			{
				alert("You entered a wrong number!");
				return;
			}
		}
		location.href = $J("#QRCode").data("uri") + "=" + size;
	},

	publishQrCodeInstructions: function()
	{
		$J.blockUI(this.getOverlaySettings($J("#QRCodeInstructions")));
	}
});