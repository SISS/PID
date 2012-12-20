var Main = Class.construct({
	init: function()
	{
		$J.getJSON("info?cmd=get_manifest", Main.renderVersionTag);
	},

	renderVersionTag: function(data)
	{
		var jq = $J("#VersionTag");
		jq.text(jq.text() + " v" + data.manifest["version"].replace(/^(\d+\.\d+)(?:-.*)?$/gi, "$1") + "." + data.manifest["Implementation-Build"]);
	}
});
