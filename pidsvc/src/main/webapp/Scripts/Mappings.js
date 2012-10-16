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
				.keypress(this.searchMappingOnKeyPress)
			.end()
			.find("select")
				.keypress(this.searchMappingOnKeyPress);
		$J("#Pager input:first").keypress(this.pagerOnKeyPress);
		$J(document).keydown(this.globalDocumentOnKeyDown);

		$J("#Tip > div").css("opacity", .7);
		this.appendAction($J("#DefaultAction"), null);

		$J("#TopMenu > DIV.MenuButton").click(this.openTab);
		this.openTab(0);

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
			this.searchMapping();
		}
		else if (type)
		{
			$J("#SearchMappingType").val(type);
			$J("#MappingType").val(type);
			this.searchMapping();
		}
		this.blockSaving(true);

		// Reset mapping configuration.
		this.createMapping(true);
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
				if (!$J(":focus").is(":input") && event.ctrlKey)
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
		$J.unblockUI();
	},

	renderGenericError: function(jqEl, jqXHR, textStatus, errorThrown)
	{
		if (jqXHR.status != 200)
		{
			jqEl
				.append(
					"<tr valign='top' class='__error'>" +
					"	<td colspan='3'>" + jqXHR.status + " " + jqXHR.statusText + "</td>" +
					"</tr>"
				);
		}
		else
		{
			jqEl
				.append(
					"<tr valign='top' class='__error'>" +
					"	<td colspan='3'>" + errorThrown.name + " (" + textStatus + ")<br/>" + errorThrown.message + "</td>" +
					"</tr>"
				);
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
					backgroundColor: '#fff'
				},
				css: {
				      border: 'none',
				      backgroundColor: '',
				      color: '#fff'
				   }
			};
		if (jq)
			jq.block(settings);
		else
			$J.blockUI(settings);
	},

	unblockUI: function()
	{
		$J("#MappingConfigSection").unblock();
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
			$J("#MappingConfigSection input, #MappingConfigSection select").attr("disabled", "disabled");
			$J("#ConfigSupersededWarning").show();
			$J("*.__supersededLock, #ConfigSaveWarning").hide();

			// Remove section title if there're no conditions defined.
			if (!$J("#ConditionSection").children().size())
				$J("#ConditionSection").prev().hide();
		}
		else
		{
			$J("#MappingConfigSection input:not(#MappingPath), #MappingConfigSection select:not(#MappingType)").removeAttr("disabled");
			$J("#ConfigSupersededWarning, #ConfigSaveWarning").hide();
			$J("*.__supersededLock").show();

			// Open URI link is only visible for 1-to-1 mappings.
			if ($J("#MappingType").val() != "1:1" || $J("#MappingPath").is(":enabled"))
				$J("#OpenUriLink").hide();
			
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
		$J("#MappingConfigSection input[configurationOnChange!='1'], #MappingConfigSection select[configurationOnChange!='1']")
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

	searchMapping: function(page)
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
		$J.getJSON(request, this.renderResults).fail(this.renderResultsError);
	},

	renderResults: function(data)
	{
		Main.unblockUI();

		// Check for errors.
		if ($J("#MappingSearchResultsTable .__error").size() > 0)
			return;

		// Render results.
		if (data != null)
		{
			$J("#MappingSearchResultsTable tr:gt(0)").remove();
			$J("#MappingSearchResults").show();
			if (data.results.length)
			{
				$J(data.results).each(function() {
					$J("#MappingSearchResultsTable")
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
				$J("#MappingSearchResultsTable tr:gt(0) > td").hover(
					function() { $J(this).siblings().andSelf().addClass("ResultRowHighlight"); },
					function() { $J(this).siblings().andSelf().removeClass("ResultRowHighlight"); }
				);
				$J("#MappingSearchResultsTable .__link").click(Main.getPidConfig);

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
				$J("#MappingSearchResultsTable")
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

	searchMappingOnKeyPress: function(event)
	{
		if (event.which == 13)
		{
			event.preventDefault();
			Main.searchMapping();
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
		Main.searchMapping(page);
	},

	renderResultsError: function(jqXHR, textStatus, errorThrown)
	{
		renderGenericError($J("#MappingSearchResultsTable"), jqXHR, textStatus, errorThrown);
	},

	///////////////////////////////////////////////////////////////////////////
	//	PID configuration UI.

	createMapping: function(initialize)
	{
		if (initialize !== true)
			$J("#Tip").hide();
		this.renderPidConfig(null);
	},

	deleteMapping: function()
	{
		if (Main.isEditingBlocked())
			return;
		var mappingPath = $J("#MappingPath").val().trim();
		if (!mappingPath || $J("#ChangeHistory").attr("isDeprecated") == "1")
			return this.createMapping();

		if (!confirm("Are you sure wish to delete this mapping?"))
			return;

		if ($J("#MappingPath").attr("disabled"))
		{
			// Delete existing mapping.
			this.blockUI();
			$J.ajax("controller?cmd=delete_mapping&mapping_path=" + encodeURIComponent(mappingPath), {
					type: "POST",
					cache: false
				})
				.done(this.getPidConfig.bind(this, 0))
				.fail(this.displayGenericError);
		}
		else
		{
			// Clear unsaved mapping.
			this.createMapping();
		}
	},

	getPidConfig: function(id)
	{
		var internalCall = Object.isNumber(id);
		var mappingId = internalCall ? id : $J(this).attr("mapping_id");

		$J("#Tip").hide();
		Main.openTab(1);

		if (!internalCall)
			Main.blockUI($J("#MappingConfigSection"));

		// If mappingId === 0 then get the latest configuration for the current mapping.
		if (mappingId === 0)
			$J.getJSON("info?cmd=get_pid_config&mapping_path=" + encodeURIComponent($J("#MappingPath").val()), Main.renderPidConfig).fail(Main.renderResultsError);
		else
			$J.getJSON("info?cmd=get_pid_config&mapping_id=" + mappingId, Main.renderPidConfig).fail(Main.renderResultsError);
	},

	renderPidConfig: function(data)
	{
		// Reset configuration.
		if (!data || $J.isEmptyObject(data))
		{
			if (data != null && $J.isEmptyObject(data))
				alert("Mapping is not found!");

			$J("#MappingPath").val("").removeAttr("disabled");
			$J("#MappingType").val("1:1").removeAttr("disabled");
			$J("#MappingDescription").val("");
			$J("#MappingCreator").val("");
			$J("#OpenUriLink").hide();

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
		$J("#MappingPath").val(data.mapping_path).attr("disabled", "disabled");
		$J("#MappingType").val(data.type).attr("disabled", "disabled");
		$J("#MappingDescription").val(data.description);
		$J("#MappingCreator").val(data.creator);

		if (data.type == "1:1")
			$J("#OpenUriLink").attr("href", data.mapping_path).show();

		$J("#DefaultAction")
			.find("td.__actionType > select")
				.val(data.action ? data.action.type : "")
			.end()
			.find("input.__actionName")
				.val(data.action ? data.action.name : "")
			.end()
			.find("input.__actionValue")
				.val(data.action ? data.action.value : "");

		if (data.conditions)
		{
			$J("#ConditionSection").empty();
			$J(data.conditions).each(function() {
				// Add conditions.
				Main.appendCondition($J("#ConditionSection"), this)
					.find("td.__conditionType:last select")
						.val(this.type);

				// Add actions for each conditions.
				if (this.actions.length)
				{
					$J(this.actions).each(function() {
						Main.appendAction($J("#ConditionSection table.__actions:last"), this)
							.find("td.__actionType:last > select")
								.val(this.type);
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
				.append("<li><a href='#' mapping_id='" + this.mapping_id + "'>" + this.date_start + " - " + (this.date_end == null ? "present" : this.date_end) + "</a></li>");
		});
		$J("#ChangeHistory a").click(Main.getPidConfig);

		// Block editing for deprecated/superseded mappings.
		Main.blockEditing(data.ended);

		Main.blockSaving(true);
		Main.unblockUI();
	},

	renderPidConfigError: function(jqXHR, textStatus, errorThrown)
	{
		renderGenericError($J("#MappingSearchResultsTable"), jqXHR, textStatus, errorThrown);
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

	appendCondition: function(jq, jsonAction)
	{
		if (!jsonAction)
			jsonAction = { type: null, match: "" };

		return jq
			.append("<table border='0' cellpadding='5' cellspacing='0' width='100%'>" +
				"	<tr valign='top'>" +
				"		<td>" +
				"<a href='#' class='__conditionMoveUp'><img src='Images/arrow_up_small.gif' title='Move Up' width='12' height='6' border='0' style='position: relative; top: 2px;'/></a><br/>" +
				"<a href='#' class='__conditionMoveDown'><img src='Images/arrow_down_small.gif' title='Move Down' width='12' height='6' border='0' style='position: relative; top: 9px;'/></a>" +
				"</td>" +
				"		<td class='__conditionType'>" +
				"			<select class='input' style='width: 200px;'>" +
				"				<option value='ContentType'>ContentType</option>" +
				"				<option value='PrioritizedContentType'>PrioritizedContentType</option>" +
				"				<option value='Extension'>Extension</option>" +
				"				<option value='QueryString'>QueryString</option>" +
				"			</select>" +
				"		</td>" +
				"		<td>" +
				"			<input type='text' value='" + jsonAction.match + "' class='__conditionMatch' maxlength='255' style='width: 483px;' />" +
				"			<div align='right'>" +
				"				<a href='#' class='__toggleActions tip'><img src='Images/arrow_137.gif' width='9' height='9' border='0' style='margin-right: 5px; position: relative; top: 2px;'/>Actions</a>" +
				"				<span class='__supersededLock'>&nbsp; <a href='#' class='__removeCondition tip'><img src='Images/arrow_137.gif' width='9' height='9' border='0' style='margin-right: 5px; position: relative; top: 2px;'/>Remove</a></span>" +
				"			</div>" +
				"		</td>" +
				"	</tr>" +
				"	<tr " + (jsonAction.type == null ? "" : "style='display: none;'") + ">" +
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

	appendAction: function(jq, jsonAction)
	{
		if (!jsonAction)
			jsonAction = { type: null, name: "", value: "" };

		var elementWidth = null;
		if (jsonAction.type == null)
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
					jsonAction.type != null ?
						"<td>" +
						"<a href='#' class='__actionMoveUp'><img src='Images/arrow_up_small.gif' title='Move Up' width='12' height='6' border='0' style='position: relative; top: -4px;'/></a><br/>" +
						"<a href='#' class='__actionMoveDown'><img src='Images/arrow_down_small.gif' title='Move Down' width='12' height='6' border='0' style='position: relative; top: 3px;'/></a>" +
						"</td>"
					: ""
				) +
				"	<td class='__actionType'>" +
				"		<select class='input' style='width: " + elementWidth.type +";'>" +
				(jsonAction.type != null ? "" : "<option></option>") +
				"			<option value='301'>301 Moved permanently</option>" +
				"			<option value='302'>302 Simple redirection</option>" +
				"			<option value='303'>303 See other</option>" +
				"			<option value='307'>307 Temporary redirect</option>" +
				"			<option value='404'>404 Temporarily gone</option>" +
				"			<option value='410'>410 Permanently gone</option>" +
				"			<option value='415'>415 Unsupported media type</option>" +
				"			<option value='Proxy'>Proxy</option>" +
				(
					jsonAction.type != null ?
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
				"		<input type='text' value='" + (jsonAction.name ? jsonAction.name : "") + "' class='__actionName' maxlength='50' style='width: " + elementWidth.name +";' />" +
				"	</td>" +
				"	<td align='right'>" +
				"		<input type='text' value='" + (jsonAction.value ? jsonAction.value : "") + "' class='__actionValue' maxlength='512' style='width: " + elementWidth.value +";' />" +
				"	</td>" +
				(
					jsonAction.type != null ? "<td><a href='#' class='__removeAction'><img src='Images/delete.png' title='Remove' width='16' height='16' border='0' style='position: relative; top: 1px;'/></a></td>" : ""
				) +
				"</tr>"
			)
			.find("td.__actionType select:last")
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

	saveMapping: function()
	{
		var path = $J("#MappingPath").val();
		if (!path || Main.isSavingBlocked())
			return;

		var description = $J("#MappingDescription").val();
		var creator = $J("#MappingCreator").val();

		// Basic data.
		var cmdxml = "<mapping>";
		cmdxml += "<path>" + path.htmlEscape() + "</path>";
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
		cmdxml += "<conditions>";
		$J("#ConditionSection > table").each(function() {
			var jqCondition = $J(this);
			var match = jqCondition.find("input.__conditionMatch").val().trim();
			if (!match)
				return;

			cmdxml += "<condition>";
			cmdxml += "<type>" + jqCondition.find("td.__conditionType select").val() + "</type>";
			cmdxml += "<match>" + match.htmlEscape() + "</match>";
			cmdxml += "<actions>";
			jqCondition.find("table.__actions tr").each(function() {
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
			cmdxml += "</condition>";
		});
		cmdxml += "</conditions>";
		cmdxml += "</mapping>";

//		alert(cmdxml);
//		$J(window.open().document.body).html(cmdxml.htmlEscape());

		// Submit request.
		Main.blockUI();
		$J.ajax("controller?cmd=create_mapping", {
				type: "POST",
				cache: false,
				contentType: "text/xml",
				data: cmdxml
			})
			.done(Main.getPidConfig.bind(Main, 0))
			.fail(Main.displayGenericError);
	},

	reinstateMapping: function()
	{
		if (!confirm("Are you sure wish to reinstate this mapping configuration?\n\nYou will be given a chance to make changes before saving new configuration."))
			return;
		this.blockEditing(false);
		$J("#ConfigSaveWarning").show();
	}
});