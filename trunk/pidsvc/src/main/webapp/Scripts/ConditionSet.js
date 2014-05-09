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
	_conditionTypesPlaceholders: {
		"Comparator":	"Comparison equation, e.g. $1=string&$2=escaped\&characters",
		"ComparatorI":	"Comparison equation, e.g. $1=string&$2=escaped\&characters",
		"ContentType":	"A regular expression to match an Accept HTTP header, e.g. ^application/html?$",
		"Extension":	"A regular expression to match an extension, e.g. ^html?$",
		"HttpHeader":	"Regular expression condition, e.g. accept=^application/(xml|rdf)$",
		"QueryString":	"Regular expression condition, e.g. param=(one)&number=(\d+)"
	},

	// Holds the last loaded configuration.
	_config:						null,

	_focusPager:					false,

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
		$J("#SetName").change(this.nameOnChange);

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
				"export_psb": { name: "Binary", icon: "doc-bin" },
				"export_xml": { name: "XML", icon: "doc-xml" }
			}
		});
		$J.contextMenu({
			selector: "#cmdExportAll",
			trigger: "left",
			callback: Main.exportAll,
			items: {
				"export_all_psb": { name: "Binary", icon: "doc-bin" },
				"export_all_xml": { name: "XML", icon: "doc-xml" }
			}
		});

		// Set default parameters.
		this.search();
		this.blockSaving(true);

		// Reset mapping configuration.
		this.create(true);

		// Automatically retrieve configuration.
		var name = location.href.getQueryParam("name");
		if (name !== false)
			this.getConfigByName(decodeURIComponent(name));

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

	getCurrentTabIndex: function()
	{
		return $J("#TopMenu > DIV.MenuButtonActive").index();
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
		var request = "info?cmd=search_condition_set&page=" + page + "&q=" + encodeURIComponent($J("#SearchText").val());
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
							"		<a href='#' set_name='" + this.name + "' class='__link' style='font-weight: bold;'>" + this.name + "</a>" +
								(
									this.description ? "<br/><span class=\"tip\">" + this.description + "</span>" : ""
								) +
							"	</td>" +
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
		{
			if (this.getCurrentTabIndex() != 1)
				this.openTab(1);
		}
		this.renderConfig(null);
	},

	delete: function()
	{
		if (!Main._config || !Main._config.name)
			return this.create();

		if (!confirm("Are you sure wish to delete \"" + Main._config.name + "\" condition set?"))
			return;

		// Delete existing record.
		this.blockUI();
		$J.ajax("controller?cmd=delete_condition_set&name=" + encodeURIComponent(Main._config.name), {
				type: "POST",
				cache: false
			})
			.done(this.renderConfig.bind(this, null))
			.fail(ExceptionHandler.displayGenericException);
	},

	getConfig: function(internalCall)
	{
		internalCall = (internalCall === true);

		var name = $J(this).attr("set_name");
		if (!name)
			name = $J("#SetName").val();

		Main.openTab(1);

		if (!internalCall)
			Main.blockUI($J("#ConfigSection"));
		$J.getJSON("info?cmd=get_condition_set_config&name=" + encodeURIComponent(name), Main.renderConfig).fail(ExceptionHandler.renderGenericException);
	},

	getConfigByName: function(name)
	{
		Main.openTab(1);
		Main.blockUI($J("#ConfigSection"));
		$J.getJSON("info?cmd=get_condition_set_config&name=" + encodeURIComponent(name), Main.renderConfig).fail(ExceptionHandler.renderGenericException);
	},

	getConfigByLink: function(e)
	{
		Main.getConfigByName($J(this).text());
	},

	renderConfig: function(data)
	{
		// Save last loaded configuration in memory.
		Main._config = data;

		// Reset configuration.
		if (!data || $J.isEmptyObject(data))
		{
			if (data != null && $J.isEmptyObject(data))
				alert("Configuration is not found!");

			$J("#SetName").val("");
			$J("#SetDescription").val("");
			$J("#ConditionSection").empty();

			Main.blockSaving(true);
			Main.monitorChanges();
			Main.unblockUI();
			return;
		}

		// Show configuration.
		$J("#SetName").val(data.name);
		$J("#SetDescription").val(data.description);

		if (data.conditions)
		{
			$J("#ConditionSection").empty();
			$J(data.conditions).each(function() {
				Main.appendCondition($J("#ConditionSection"), this);

				// Add actions for each conditions.
				if (this.actions.length)
				{
					$J(this.actions).each(function() {
						Main.appendAction($J("#ConditionSection TABLE.__actions:last"), this);
					});
				}
				else
					Main.appendAction($J("#ConditionSection TABLE.__actions:last"), { type: "404" });
			});
		}

		Main.monitorChanges();
		Main.blockSaving(true);
		Main.unblockUI();
	},

	nameOnChange: function()
	{
		var jq				= $J("#SetName");
		var originalName	= Main._config ? Main._config.name : null;
		var newName			= jq.val().trim();

		if (originalName == null)
			return;
		originalName = originalName.trim();
		jq.val(newName);
		if (originalName != "" && originalName != newName && !confirm("You have changed the name for the set. It may override another set or cause some URI mappings to work incorrectly.\n\nDo you wish to continue?"))
			jq.val(originalName);
	},

	//
	//	Conditions handling.
	//

	addCondition: function()
	{
		var jq = Main.appendCondition($J("#ConditionSection"), null).find("table.__actions:last");
		if (jq != null)
			this.appendAction(jq, { type: "", name: "", value: "" });
		Main.blockSaving(false);
	},

	removeCondition: function()
	{
		if (confirm("Are you sure wish to delete this condition?"))
		{
			$J(this).parents("table:first").remove();
			Main.blockSaving(false);
		}
	},

	conditionMoveUp: function()
	{
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
		var jqThisItem = $J(this).parents("table:first");
		var jqOtherItem = jqThisItem.next();
		if (jqOtherItem.size())
		{
			jqThisItem.insertAfter(jqOtherItem);
			Main.blockSaving(false);
		}
	},

	conditionOnChange: function()
	{
		var placeholder = Main._conditionTypesPlaceholders[$J(this).val()];
		$J(this).parents("table:first").find("INPUT.__conditionMatch").attr("placeholder", placeholder ? placeholder : "");
	},

	getConditionTypesHtml: function()
	{
		return "<option value='Comparator'>Comparator</option>" +
			"<option value='ComparatorI'>Comparator (case-insensitive)</option>" +
			"<option value='ContentType'>ContentType</option>" +
			"<option value='Extension'>Extension</option>" +
			"<option value='HttpHeader'>HttpHeader</option>" +
			"<option value='QueryString'>QueryString</option>";
	},

	appendConditionTypes: function(jq)
	{
		return jq.append(Main.getConditionTypesHtml());
	},

	appendCondition: function(jq, json)
	{
		if (json == null)
			json = { type: null, match: "", description: "" };

		return jq
			.append("<table class='__conditionTable' border='0' cellpadding='5' cellspacing='0' width='100%' style='border: 1px solid #fff;'>" +
				"	<tr valign='top'>" +
				"		<td style=\"position: relative;\">" +
				"<div class=\"__marker\" style=\"background: #d42626; position: absolute; top: 0; left: -5px; width: 5px; height: 33px; display: none;\"></div>" +
				"<a href='#' class='__conditionMoveUp'><img src='Images/arrow_up_small.gif' title='Move Up' width='12' height='6' border='0' style='position: relative; top: 2px;'/></a><br/>" +
				"<a href='#' class='__conditionMoveDown'><img src='Images/arrow_down_small.gif' title='Move Down' width='12' height='6' border='0' style='position: relative; top: 9px;'/></a>" +
				"</td>" +
				"		<td class='__conditionType'>" +
				"			<select class='input' style='width: 200px;'>" + Main.getConditionTypesHtml() + "</select>" +
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
				"	<tr class='__actionsSection' " + (json.type == null ? "" : "style='display: none;'") + ">" +
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
				.change(Main.conditionOnChange)
				.val(json.type)
				.change()
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
		$J(this).css({ "border": "1px solid #CCE6FF", "background": "#F2FBFF" });
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
				.change(Main.actionTypeOnChange)
				.val(json.type)
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

	save: function()
	{
		var name = $J("#SetName").val().trim();
		if (!name || Main.isSavingBlocked())
			return;

		var oldname		= Main._config ? Main._config.name : null;
		var description	= $J("#SetDescription").val().trim();

		// Basic data.
		var cmdxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		cmdxml += "<conditionSet xmlns=\"urn:csiro:xmlns:pidsvc:backup:1.0\">";
		cmdxml += "<name" + (oldname && oldname != name ? " rename=\"" + oldname.htmlEscape() + "\"" : "") + ">" + name.htmlEscape() + "</name>";
		if (description)
			cmdxml += "<description>" + description.htmlEscape() + "</description>";

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

		cmdxml += "</conditionSet>";

		// Submit request.
		Main.blockUI();
		$J.ajax("controller?cmd=create_condition_set", {
				type: "POST",
				cache: false,
				contentType: "text/xml",
				data: cmdxml
			})
			.done(Main.getConfig.bind(Main, true))
			.fail(ExceptionHandler.displayGenericException);
	},

	export: function(key, options)
	{
		if (!Main.isSavingBlocked() || !Main._config)
		{
			alert("You must save condition set before exporting!");
			return;
		}

		var outputFormat = key.replace(/^.*_([^_]+)$/, "$1");
		location.href = "controller?cmd=export_condition_set&name=" + encodeURIComponent($J("#SetName").val()) + "&format=" + outputFormat;
	},

	exportAll: function(key, options)
	{
		var outputFormat = key.replace(/^.*_([^_]+)$/, "$1");
		location.href = "controller?cmd=export_condition_set&format=" + outputFormat;
	}
});