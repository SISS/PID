var G_APPLICATION_PATH = "/pidsvc/"; 
$J(function ()
{
	if (top.location.href == this.location.href)
		location.href = G_APPLICATION_PATH + "?page=" + location.pathname;
});
