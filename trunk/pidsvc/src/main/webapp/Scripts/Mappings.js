var Main = Class.construct({
	_actionTypesDescriptions: {
		"301": "Moved permanently to a target URL<br/><br/>Value points to resource location.",
		"302": "Simple redirection to a target URL<br/><br/>Value points to resource location.",
		"303": "See other URLs<br/><br/>Value points to resource location.",
		"307": "Temporary redirect to a target URL<br/><br/>Value points to resource location.",
		"404": "Temporarily gone<br/><br/>No action parameters are required.",
		"410": "Permanently gone<br/><br/>No action parameters are required.",
		"415": "Unsupported Media Type<br/><br/>No action parameters are required.",
		"AddHttpHeader": "Add HTTP response header<br/><br/>Action name specifies a new HTTP header name and value defines its value.",
		"RemoveHttpHeader": "Remove HTTP response header<br/><br/>Action name specifies HTTP header name that is to be removed from HTTP header collection. No value is required.",
		"ClearHttpHeaders": "Clear HTTP response headers<br/><br/>No action parameters are required.",
		"Proxy": "Proxy request<br/><br/>Value points to resource location."
	},

	_focusPager: false,

	init: function()
	{
		// Initialise UI elements.
		$J("#UriSearchSection")
			.find("input:not(#PagerInput)")
				.keypress(this.searchOnKeyPress)
			.end()
			.find("select")
				.keypress(this.searchOnKeyPress);
		$J("#Pager input:first").keypress(this.pagerOnKeyPress);
		$J(document).keydown(this.globalDocumentOnKeyDown);
		$J("#MappingPath").change(this.mappingPathOnChange);

		$J("#Tip > div").css("opacity", .7);
		this.appendAction($J("#DefaultAction"), null);

		$J("#TopMenu > DIV.MenuButton").click(this.openTab);
		this.openTab(0);

		// Set ExceptionHandler properties.
		ExceptionHandler.setPostHandler(Main.unblockUI);

		// Initialise context menus.
		$J.contextMenu({
			selector: '#cmdExport',
			trigger: 'left',
			callback: Main.export,
			items: {
				"partial_export": { name: "Partial export (current only)", icon: "export", accesskey: "p" },
				"full_export": { name: "<nobr>Full export (preserves history) &nbsp;</nobr>", icon: "export", accesskey: "f" },
			}
		});
		$J.contextMenu({
			selector: '#cmdQrCode',
			trigger: 'left',
			callback: Main.publishQrCode,
			items: {
				"100": { name: "100 px", icon: "barcode" },
				"120": { name: "120 px", icon: "barcode" },
				"150": { name: "150 px", icon: "barcode" },
				"200": { name: "200 px", icon: "barcode" },
				"300": { name: "300 px", icon: "barcode" },
				"custom_size": { name: "Custom size", icon: "barcode" }
			}
		});

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
			$J("#MappingType").val(type);
			this.search();
		}
		this.blockSaving(true);

		// Reset mapping configuration.
		this.create(true);

		// Automatically retrieve mapping configuration.
		var qsMappingPath = location.href.getQueryParam("mapping_path");
		if (qsMappingPath !== false)
			this.getConfigByMappingPath(decodeURIComponent(qsMappingPath));
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
			$J("#ConfigSection input, #ConfigSection select").attr("disabled", "disabled");
			$J("#ConfigSupersededWarning").show();
			$J("*.__supersededLock, #ConfigSaveWarning").hide();

			// Remove section title if there're no conditions defined.
			if (!$J("#ConditionSection").children().size())
				$J("#ConditionSection").prev().hide();
		}
		else
		{
			$J("#ConfigSection input, #ConfigSection select").removeAttr("disabled");
			$J("#ConfigSupersededWarning, #ConfigSaveWarning").hide();
			$J("*.__supersededLock").show();

			// Open URI link is only visible for 1-to-1 mappings.
			if ($J("#MappingType").val() != "1:1" || !$J("#ConfigSection").data("config") || $J("#ChangeHistory").attr("isDeprecated") == "1")
				$J("#QRCode").hide();
			
			// Ensure condition section title is visible.
			$J("#ConditionSection").prev().show();

			// Reinitialise action selectors.
			$J("#DefaultAction td.__actionType > select, #ConditionSection td.__actionType > select").change();

			Main.monitorChanges();
		}
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
							"		<a href='#' mapping_id='" + this.mapping_id + "' class='__link' style='font-weight: bold;'>" + this.mapping_path + "</a>" +
							"		<br/>" +
							"		<span class='tip' style='margin-left: 21px;'>" + (this.description ? this.description : "") + "</span>" +
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

	create: function(initialize)
	{
		if (initialize !== true)
			$J("#Tip").hide();
		this.renderConfig(null);
	},

	delete: function()
	{
		if (Main.isEditingBlocked())
			return;

		var config		= $J("#ConfigSection").data("config");
		var mappingPath	= $J("#MappingPath").val().trim();

		if ((!config && !mappingPath) || $J("#ChangeHistory").attr("isDeprecated") == "1")
			return this.create();
		if (!confirm("Are you sure wish to delete \"" + config.mapping_path + "\" mapping?"))
			return;

		if (config)
		{
			// Delete existing record.
			this.blockUI();
			$J.ajax("controller?cmd=delete_mapping&mapping_path=" + encodeURIComponent(config.mapping_path), {
					type: "POST",
					cache: false
				})
				.done(this.getConfig.bind(this, 0))
				.fail(ExceptionHandler.displayGenericException);
		}
		else
		{
			// Clear unsaved mapping.
			this.create();
		}
	},

	getConfig: function(id)
	{
		var internalCall = Object.isNumber(id);
		var mappingId = internalCall ? id : $J(this).attr("mapping_id");

		$J("#Tip").hide();
		Main.openTab(1);

		if (!internalCall)
			Main.blockUI($J("#ConfigSection"));

		// If mappingId === 0 then get the latest configuration for the current mapping.
		if (mappingId === 0)
		{
			var path = $J("#MappingPath").val().trim();
			$J("#MappingPath").val(path);
			$J.getJSON("info?cmd=get_pid_config&mapping_path=" + encodeURIComponent(path), Main.renderConfig).fail(ExceptionHandler.displayGenericException);
		}
		else
			$J.getJSON("info?cmd=get_pid_config&mapping_id=" + mappingId, Main.renderConfig).fail(ExceptionHandler.displayGenericException);
	},

	getConfigByMappingPath: function(mappingPath)
	{
		$J("#Tip").hide();
		Main.openTab(1);
		Main.blockUI($J("#ConfigSection"));
		$J.getJSON("info?cmd=get_pid_config&mapping_path=" + encodeURIComponent(mappingPath), Main.renderConfig).fail(ExceptionHandler.displayGenericException);
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

			$J("#MappingPath").val("");
			$J("#MappingType").val("1:1");
			$J("#MappingDescription").val("");
			$J("#MappingCreator").val("");
			$J("#QRCode").hide();

			$J("#DefaultAction")
				.find("td.__actionType > select")
					.val("")
				.end()
				.find("input.__actionName")
					.val("")
				.end()
				.find("input.__actionValue")
					.val("");
			$J("#ConditionSection").empty();
			$J("#ChangeHistory").hide();

			Main.blockSaving(true);
			Main.blockEditing(false);
			Main.unblockUI();
			return;
		}

		// Show configuration.
		$J("#MappingPath").val(data.ended ? data.original_path : data.mapping_path);
		$J("#MappingType").val(data.type);
		$J("#MappingDescription").val(data.description);
		$J("#MappingCreator").val(data.creator);

		if (data.type == "1:1")
		{
			var qruri = location.href.replace(/^(https?:\/\/.+?)\/.*$/gi, "$1" + data.mapping_path);
			$J("#QRCode")
				.data("uri", qruri)
				.attr("src", "qrcode?uri=" + encodeURIComponent(qruri) + "&size=120")
				.attr("title", "Open in a new window\n" + qruri)
				.show()
				.parent()
					.attr("href", qruri);
		}

		$J("#DefaultAction")
			.find("td.__actionType > select")
				.val(data.action ? data.action.type : "")
			.end()
			.find("input.__actionName")
				.val(data.action ? data.action.name : "")
			.end()
			.find("input.__actionValue")
				.val(data.action ? data.action.value : "");

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

		// Change history.
		$J("#ChangeHistory").attr("isDeprecated", data.history[0].date_end != null ? 1 : 0).show().find("ul").empty();
		$J(data.history).each(function() {
			$J("#ChangeHistory ul")
				.append(
					"<li><a href='#' mapping_id='" + this.mapping_id + "'>" + this.date_start + " - " + (this.date_end == null ? "present" : this.date_end) + "</a>" +
						(this.creator ? "<br/><span class=\"tip\">by " + this.creator + "</span>" : "") +
					"</li>"
				);
		});
		$J("#ChangeHistory a").click(Main.getConfig);

		// Block editing for deprecated/superseded mappings.
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
			jq.val(originalPath);
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
			json = { type: null, match: "" };

		return jq
			.append("<table border='0' cellpadding='5' cellspacing='0' width='100%'>" +
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
				"			<div align='right'>" +
				"				<a href='#' class='__toggleActions tip'><img src='Images/arrow_137.gif' width='9' height='9' border='0' style='margin-right: 5px; position: relative; top: 2px;'/>Actions</a>" +
				"				<span class='__supersededLock'>&nbsp; <a href='#' class='__removeCondition tip'><img src='Images/arrow_137.gif' width='9' height='9' border='0' style='margin-right: 5px; position: relative; top: 2px;'/>Remove</a></span>" +
				"			</div>" +
				"		</td>" +
				"	</tr>" +
				"	<tr " + (json.type == null ? "" : "style='display: none;'") + ">" +
				"		<td colspan='3'>" +
				"			<div style='margin-left: 70px'>" +
				"				<div class='caps' style='background: #f2f2f2;'>Actions:</div>" +
				"				<table class='__actions' border='0' cellpadding='5' cellspacing='1' width='100%' style='position: relative; right: -6px;'>" +
				"				</table>" +
				"				<div class='__supersededLock' align='right'><a href='#' class='__addAction'><img src='Images/plus_16.png' title='Add action' width='16' height='16' border='0'/></a></div>" +
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
			.end();
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

		var elementWidth = null;
		if (json.type == null)
		{
			elementWidth = {
				type: 	"184px",
				name:	"134px",
				value:	"332px"
			};
		}
		else
		{
			elementWidth = {
				type: 	"150px",
				name:	"120px",
				value:	"250px"
			};
		}

		var scrollTop = $J(window).scrollTop();
		var ret = jq
			.append("<tr>" +
				(
						json.type != null ?
						"<td>" +
						"<a href='#' class='__actionMoveUp'><img src='Images/arrow_up_small.gif' title='Move Up' width='12' height='6' border='0' style='position: relative; top: -4px;'/></a><br/>" +
						"<a href='#' class='__actionMoveDown'><img src='Images/arrow_down_small.gif' title='Move Down' width='12' height='6' border='0' style='position: relative; top: 3px;'/></a>" +
						"</td>"
					: ""
				) +
				"	<td class='__actionType'>" +
				"		<select class='input' style='width: " + elementWidth.type +";'>" +
				(json.type != null ? "" : "<option></option>") +
				"			<option value='301'>301 Moved permanently</option>" +
				"			<option value='302'>302 Simple redirection</option>" +
				"			<option value='303'>303 See other</option>" +
				"			<option value='307'>307 Temporary redirect</option>" +
				"			<option value='404'>404 Temporarily gone</option>" +
				"			<option value='410'>410 Permanently gone</option>" +
				"			<option value='415'>415 Unsupported media type</option>" +
				"			<option value='Proxy'>Proxy</option>" +
				(
						json.type != null ?
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
				"		<input type='text' value='" + (json.value ? json.value : "") + "' class='__actionValue' maxlength='512' style='width: " + elementWidth.value +";' />" +
				"	</td>" +
				(
						json.type != null ? "<td><a href='#' class='__removeAction'><img src='Images/delete.png' title='Remove' width='16' height='16' border='0' style='position: relative; top: 1px;'/></a></td>" : ""
				) +
				"</tr>"
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
		{
			$J("#Tip").hide();
			return;
		}
		$J("#Tip")
			.show()
			.find("div")
				.html(description);
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
		var path = $J("#MappingPath").val();
		if (!path || Main.isSavingBlocked())
			return;

		var config		= $J("#ConfigSection").data("config");
		var oldpath		= config ? config.mapping_path : null;

		// Check and warn the user if necessary if he's just about to rewrite existing mapping.
		if (!prechecked && oldpath && oldpath != path)
		{
			Main.blockUI();
			$J.getJSON("info?cmd=check_mapping_path_exists&mapping_path=" + encodeURIComponent(path.htmlEscape().trim()), Main.presaveCheck).fail(ExceptionHandler.displayGenericException);
			return;
		}

		var description	= $J("#MappingDescription").val();
		var creator		= $J("#MappingCreator").val();

		// Basic data.
		var cmdxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		cmdxml += "<mapping xmlns=\"urn:csiro:xmlns:pidsvc:mapping:1.0\">";
		cmdxml += "<path" + (oldpath && oldpath != path ? " rename=\"" + oldpath.htmlEscape() + "\"" : "") + ">" + path.htmlEscape().trim() + "</path>";
		cmdxml += "<type>" + $J("#MappingType").val() + "</type>";
		if (description)
			cmdxml += "<description>" + description.htmlEscape() + "</description>";
		if (creator)
			cmdxml += "<creator>" + creator.htmlEscape() + "</creator>";

		// Default action.
		var jqDefault = $J("#DefaultAction");
		var defaultActionType = jqDefault.find("td.__actionType select").val();
		if (defaultActionType)
		{
			cmdxml += "<action>";
			cmdxml += "<type>" + defaultActionType + "</type>";
			cmdxml += "<name>" + jqDefault.find("input.__actionName").val().trim().htmlEscape() + "</name>";
			cmdxml += "<value>" + jqDefault.find("input.__actionValue").val().htmlEscape() + "</value>";
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
	
				cmdxml += "<condition>";
				cmdxml += "<type>" + jqCondition.find("td.__conditionType select").val() + "</type>";
				cmdxml += "<match>" + match.htmlEscape() + "</match>";

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
			.done(Main.getConfig.bind(Main, 0))
			.fail(ExceptionHandler.displayGenericException);
	},

	reinstateMapping: function()
	{
		if (!confirm("Are you sure wish to reinstate this mapping configuration?\n\nYou will be given a chance to make changes before saving new configuration."))
			return;
		this.blockEditing(false);
		$J("#ConfigSaveWarning").show();
	},

	export: function(key, options)
	{
		var config = $J("#ConfigSection").data("config");
		if (!Main.isSavingBlocked() || !config)
		{
			alert("You must save the mapping before exporting!");
			return;
		}
		if (key == "full_export")
			location.href = "controller?cmd=full_export&mapping_path=" + encodeURIComponent(config.mapping_path.trim());
		else
			location.href = "controller?cmd=partial_export&mapping_id=" + config.mapping_id;
	},

	///////////////////////////////////////////////////////////////////////////
	//	QR Codes.

	publishQrCode: function(key, options)
	{
		var size = key.toInt(0);
		if (size === 0)
		{
			size = prompt("Enter the size of QR Code you wish to get (in pixels):", 100);
			if (!size)
				return;
			if ((size = size.toInt(0)) === 0)
			{
				alert("You entered a wrong number!");
				return;
			}
		}
		location.href = "qrcode?uri=" + encodeURIComponent($J("#QRCode").data("uri")) + "&size=" + size;
	}
});