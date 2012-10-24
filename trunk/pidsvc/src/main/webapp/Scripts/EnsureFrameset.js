var G_APPLICATION_PATH = "/pidsvc/"; 
$J(function() {
	if (location.href.match(/main_frameset.html$/gi))
	{
		var page = top.location.href.getQueryParam("page");
		$J("#fraContent").attr("src", (page && page !== true ? decodeURIComponent(page) : G_APPLICATION_PATH + "mappings.html"));
	}
	else if (top.location.href == this.location.href)
		location.href = G_APPLICATION_PATH + "?page=" + encodeURIComponent(location.pathname);
});
