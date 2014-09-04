//	Extends Class object functionality allowing to create a new instance
//	of object passed as an argument and initializing it by calling its
//	constructor.
Object.extend(Class, {
	construct: function()
	{
		var args = $A(arguments);
		var obj = {};
		Object.extend(obj, args.shift());
		if (obj.ctor)
			obj.ctor.apply(obj, args);
		if (obj.init)
			this.__init_on_complete.apply(obj);
		if (obj.dtor)
			Event.observe(window, "unload", obj.dtor.bind(obj));
		return obj;
	},

	__init_on_complete: function()
	{
		if (/loaded|complete/.test(document.readyState))
		{
			this.init();
			return;
		}
		setTimeout(arguments.callee.bind(this), 10);
	}
});

Object.readInt = function(value, defaultValue)
{
	value = parseInt(value);
	return (arguments.length >= 2 && isNaN(value) ? defaultValue : value);
}
Object.readFloat = function(value, defaultValue)
{
	value = parseFloat(value);
	return (arguments.length >= 2 && isNaN(value) ? defaultValue : value);
}
Object.isNumber = function(value)
{
	return (typeof value == "string" || typeof value == "number") && !isNaN(value - 0) && value !== ""; 
}

Object.extend(Number.prototype, {
	addLeadingZeros: function(length)
	{
		var s = "";
		for (var i = length - this.toString().length; i > 0; --i) s += "0";
		return s += this;
	},

	groupDigits: function(sep)
	{
		var sValue = this.toString();
		var iLen = sValue.length;
		sep = (sep == null) ? "." : sep.substr(0, 1);
		for (var i = 3; i < sValue.length; i += 4)
			sValue = sValue.substr(0, sValue.length - i) + sep + sValue.substr(sValue.length - i);
		return sValue;
	},

	percentageOf: function(num, roundTo)
	{
		if (!this || !num)
			return 0;
		if (roundTo == null)
			roundTo = 0;
		return (roundTo ? Math.round(this * 100 / num * Math.pow(10, roundTo)) / Math.pow(10, roundTo) : this * 100 / num);
	},

	roundTo: function(roundTo)
	{
		if (roundTo == null)
			roundTo = 0;
		return Math.round(this * Math.pow(10, roundTo)) / Math.pow(10, roundTo);
	}
});

Object.extend(String.prototype, {
	toInt: function(defaultValue)
	{
		var iRet = parseInt(this);
		return (arguments.length >= 1 && isNaN(iRet) ? defaultValue : iRet);
	},

	trim: function()
	{
		return this.replace(/(^\s*)|(\s*$)/g, "");
	},

	escapeHtmlBrackets: function()
	{
		return this.replace(/</gm, "&lt;").replace(/>/gm, "&gt;");
	},

	addQueryParam: function(name, value)
	{
		return this + (this.indexOf('?') == -1 ? "?" : "&") + name + "=" + value;
	},
	removeQueryParam: function()
	{
		return arguments.length ? this.replace(new RegExp("(" + $A(arguments).join("|") + ")(=[^&]*)?&?", "ig"), "").replace(/[\?&]*$/, "") : this.replace(/\?.*/, "");
	},
	replaceQueryParam: function(name, value)
	{
		return this.removeQueryParam(name).addQueryParam(name, value);
	},
	getQueryParam: function(name)
	{
		var m = this.splitUrl().search.match(new RegExp("[\\?&]?" + name + "(?:=([^&#]*))?", "i"));
		if (!m)
			return false;
		return m[1] ? m[1] : true;
	},
	getQueryParams: function(name)
	{
//		var s = "http://www.a.com:80/img.axd?_res=image/26/fdddc8.jpg&w=75&_res&h=75&_res=123&ft=1#has&_res=3";
		var m = this.splitUrl().search.match(new RegExp("[\\?&]?" + name + "(=[^&#]*)?", "gi"));
		if (!m)
			return [];
		var ret = [];
		m.each(function(it) {
			ret.push(it.match(/^[^=]*=(.*)$/) ? RegExp.$1 : true);
		});
		return ret;
	},

	splitUrl: function()
	{
		var m = this.match(/^(\w+):\/{2}([^\/:]+)(?:\:(\d+))?(\/(?:[^?]+\/)?)?([^\?#]+)?(?:\?([^#]*))?(\#.*)?$/);
		return {
			protocol:	m[1],
			host:		m[2],
			port:		m[3],
			path:		m[4],
			name:		m[5],
			search:		m[6] == undefined ? "" : m[6],
			hash:		m[7] == undefined ? "" : m[7],

			join: function()
			{
				return this.protocol + "://" + this.host + (!this.port ? "" : ":" + this.port) + this.path + this.name + (!this.search ? "" : "?" + this.search) + this.hash;
			},
			joinPath: function()
			{
				return this.path + this.name + (!this.search ? "" : "?" + this.search) + this.hash;
			}
		};
	},

	htmlEscape: function()
	{ 
		return jQuery('<div/>').text(this.toString()).html(); 
	}
});

var WebUtils = {
	//
	//	Convert map of DOM object names from IDs to object references.
	//	As input parameter accepts object of following form:
	//		{ "<OriginalID>": "<ASP.NET generated name>" [, ...] }
	//	or in form of string:
	//		"<OriginalID> <ASP.NET generated name> [, ...]"
	//
	//	Returns a hash object.
	//
	LiteralObjectsToDom: function(val)
	{
		var obj = {};
		if (typeof(val) == "object")
			$H(val).keys().each(function(key) { obj[key] = $(val[key]); });
		else
		{
			var arr = $w(val.trim());
			for (var i = 0; i < arr.length; ++i)
				obj[arr[i]] = $(arr[++i]);
		}
		return obj;
	}
}