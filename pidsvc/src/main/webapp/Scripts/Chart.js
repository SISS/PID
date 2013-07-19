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
	LEVEL_DISTANCE:		150,

	_rgraph:			null,
	_parents:			[],

	init: function()
	{
		// Set ExceptionHandler properties.
		ExceptionHandler.setPostHandler(Main.unblockUI);

		// Initialise graph.
		this._rgraph = new $jit.RGraph({
			injectInto:		"infovis",
			levelDistance:	this.LEVEL_DISTANCE,

			// Optional: create a background canvas that plots concentric circles.
			background: {
				CanvasStyles: {
					strokeStyle:	"#f0f0f0"
				},
				levelDistance:		this.LEVEL_DISTANCE,
				numberOfCircles:	5
			},

			// Add navigation capabilities: zooming by scrolling and panning.
			Navigation: {
				enable:			true,
				panning:		true,
				zooming:		100
			},

			// Set Node and Edge styles.
			Node: {
				overridable:	true,
				type:			"circle",
				color:			"#A5CFFA",
				dim:			3
			},
			Edge: {
				overridable:	true,
				type:			"line",
				color:			"#20B4E6",
				lineWidth:		.5
			},

			Tips: {
				enable: true,
				onShow: function(tip, node)
				{
					tip.innerHTML = "<div class=\"ellipsis\"><b>" + (node.id === 0 ? "Built-in &lt;Catch-all&gt; mapping" : node.name) + "</b></div>" +
						(node.data.title ? "<div>" + node.data.mapping_path + "</div>" : "") +
						(node.data.inheritors ? "<div style=\"margin-top: 10px; border-top: 1px solid #eee; padding-top: 7px;\"><b>Inheritors:</b> " + node.data.inheritors + "</div>" : "");
				}
			},

			onBeforeCompute: function(node)
			{
				Main.renderNodeInfo(node);
			},

			// Add the name of the node in the corresponding label and a click handler to move the graph.
			// This method is called once, on label creation.
			onCreateLabel: function(domElement, node)
			{
				domElement.onclick = Main.labelOnClick.bind(this, node);
			},

			// This method is triggered right before plotting each Graph.Node.
			onBeforePlotNode: function(node)
			{
				if (!node.data.parent && node.data.parent !== 0)
				{
					// Compute parent nodes once when root node is in the centre.
					var p = node.getParents()[0];
					node.data.parent = p ? p.id : 0;

					// Save the original node depth.
					node.data.depth = node._depth;

					// Compute the number of inheritors.
					var count = 0;
					node.eachAdjacency(function(adj) { ++count; });
					if (node.id !== 0)
						--count;
					node.data.inheritors = count;
				}
			},

			// This method is called each time a label is plotted.
			onPlaceLabel: function(domElement, node)
			{
				var style = domElement.style;
				style.display = "";
				style.cursor = "pointer";

				var jq = $J(domElement).html("<div class=\"chart_label\">" + node.name + "</div>").find("DIV");

				// Clear selected node styling.
				node.removeData("dim");

				// Set styling for edges highlighting the ones that go to the root node.
				node.eachAdjacency(function(adj) {
					if (Main.isAncestor(adj.nodeTo.id) && Main.isAncestor(adj.nodeFrom.id))
					{
						adj.setDataset("current", {
							lineWidth: 2,
							color: "#09c"
						});
					}
					else
					{
						adj.removeData("color");
						adj.removeData("lineWidth");
					}
				});

				// Set styling depending on the node type and position.
				if (node.id === 0)
				{
					// Root node.
					node.setData("dim", 12);
					jq.addClass("chart_label_root");
				}
				else if (node._depth == 0)
				{
					// Current node.
					node.setData("dim", 10);
					jq.addClass("chart_label_0");
				}
				else if (Main.isAncestor(node.id))
				{
					// Parent nodes.
					node.setData("dim", 7);
					jq.addClass("chart_label_parents");
				}
				else if (node._depth <= 3)
				{
					// First three levels.
					jq.addClass("chart_label_" + node._depth);
				}
				else if (node.data.depth <= 1)
				{
					// Nodes initially close to root.
					jq.addClass("chart_label_3");
				}
				else
				{
					// Hide other nodes.
					style.display = "none";
				}

				// Position label.
				style.left = (parseInt(style.left) - domElement.offsetWidth / 2) + "px";
				style.top = (parseInt(style.top) + 5) + "px";
			}
		});
		this._rgraph.canvas.translate(-$J("#info-panel-container").width() / 2, 0); 

		// Load data.
		this.blockUI();
		$J.getJSON("info?cmd=chart", Main.renderData).fail(ExceptionHandler.displayGenericException);
	},

	///////////////////////////////////////////////////////////////////////////
	//	UI handling.

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
		$J.unblockUI();
	},

	///////////////////////////////////////////////////////////////////////////
	//	Data handling.

	renderData: function(data)
	{
		Main.unblockUI();
		Main._rgraph.loadJSON(data);

		var qsMappingPath	= location.href.getQueryParam("mapping_path");
		var node			= null;

		if (qsMappingPath !== false && qsMappingPath != "0")
			node = Main._rgraph.graph.getNode(decodeURIComponent(qsMappingPath));

		if (node)
		{
			// Select a node.
			Main._rgraph.compute("end");
			Main._rgraph.refresh();
			Main.labelOnClick(node);
		}
		else
		{
			// Trigger short animation.
			var scatter = 300;
			Main._rgraph.graph.eachNode(function(n) {
				n.getPos().setc(
					(Math.random() - .5 < 0 ? -1 : 1) * Math.floor(Math.random() * scatter),
					(Math.random() - .5 < 0 ? -1 : 1) * Math.floor(Math.random() * scatter)
				);
			});
			Main._rgraph.compute("end");
			Main._rgraph.fx.animate({
				modes:		[ "polar" ],
				duration:	1500,
				hideLabels:	false
			});

			// Append additional information to the info panel.
			Main.renderNodeInfo(Main._rgraph.graph.getNode(Main._rgraph.root));
		}
	},

	renderNodeInfo: function(node)
	{
		var title = node.id === 0 ? "Catch-all" : (node.data.title ? node.data.title : node.data.mapping_path);
		var html =
			"<div class=\"b black ellipsis\" title=\"" + title + "\">" + title + "</div>" +
			(node.id === 0 ?
				"<div>Built-in mapping</div>" :
				(node.data.title ? "<div class=\"ellipsis\" title=\"" + node.data.mapping_path + "\">" + node.data.mapping_path + "</div>" : "<div>&nbsp;</div>")
			) +
			"<br/><div class=\"ellipsis\"><b>Author:</b> " + (node.data.author ? node.data.author : "not set") + "</div>" +
			(node.data.description ? "<br/><br/><div class=\"__description\">" + node.data.description.htmlEscape().replace(/\n/g, "<br/>") + "</div>" : "") +
			"<div style=\"border-top: 1px solid #eee; padding-top: 7px; margin-top: 7px;\">" +
				"<b>Inheritors:</b> " + (node.data.inheritors ? node.data.inheritors : "none") + "<br/><br/>" +
				"<a href='mappings.html?" + (node.data.mapping_path == null ? "mapping_id=0" : "mapping_path=" + encodeURIComponent(node.data.mapping_path)) + "'><img src='Images/arrow_137.gif' width='9' height='9' border='0' align='absmiddle' style='margin-right: 5px;'/>View/edit</a>" +
			"</div>";
		$J("#info-panel")
			.html(html)
			.find("DIV.__description")
				.slimscroll({
					height:			335,
					size:			5,
					distance:		0,
					railVisible:	false,
					railOpacity:	.15
				});
	},

	isAncestor: function(nodeId)
	{
		return $J.inArray(nodeId, Main._parents) !== -1;
	},

	labelOnClick: function(node)
	{
		// Build a list of parents for clicked node.
		Main._parents = [node.id, node.data.parent];
		var gr = Main._rgraph.graph;
		var p = node.data.parent;
		while (p)
		{
			p = gr.getNode(p).data.parent;
			Main._parents.push(p);
		}

		// Trigger graph click event.
		Main._rgraph.onClick(node.id, { hideLabels: false });
	}
});

//var Log = {
//	write: function(text)
//	{
//		var jq = $J("#log").append("<li>" + text + "</li>");
//		if (jq.find("LI").size() > 50)
//			jq.find("LI:eq(0)").remove();
//	}
//};
