/*! URI.js v1.7.2 http://medialize.github.com/URI.js/ */
/* build contains: IPv6.js, punycode.js, SecondLevelDomains.js, URI.js, URITemplate.js, jquery.URI.js */
(function(){("undefined"!==typeof module&&module.exports?module.exports:window).IPv6={best:function(d){var d=d.toLowerCase().split(":"),i=d.length,k=8;""===d[0]&&""===d[1]&&""===d[2]?(d.shift(),d.shift()):""===d[0]&&""===d[1]?d.shift():""===d[i-1]&&""===d[i-2]&&d.pop();i=d.length;-1!==d[i-1].indexOf(".")&&(k=7);var j;for(j=0;j<i&&""!==d[j];j++);if(j<k)for(d.splice(j,1,"0000");d.length<k;)d.splice(j,0,"0000");for(j=0;j<k;j++){for(var i=d[j].split(""),f=0;3>f;f++)if("0"===i[0]&&1<i.length)i.splice(0,
1);else break;d[j]=i.join("")}var i=-1,s=f=0,t=-1,h=!1;for(j=0;j<k;j++)h?"0"===d[j]?s+=1:(h=!1,s>f&&(i=t,f=s)):"0"==d[j]&&(h=!0,t=j,s=1);s>f&&(i=t,f=s);1<f&&d.splice(i,f,"");i=d.length;k="";""===d[0]&&(beststr=":");for(j=0;j<i;j++){k+=d[j];if(j===i-1)break;k+=":"}""===d[i-1]&&(k+=":");return k}}})();
(function(d){function i(a){throw RangeError(B[a]);}function k(a,c){for(var e=a.length;e--;)a[e]=c(a[e]);return a}function j(a){for(var c=[],e=0,b=a.length,l,d;e<b;)l=a.charCodeAt(e++),55296==(l&63488)&&(d=a.charCodeAt(e++),(55296!=(l&64512)||56320!=(d&64512))&&i("ucs2decode"),l=((l&1023)<<10)+(d&1023)+65536),c.push(l);return c}function f(a){return k(a,function(a){var c="";55296==(a&63488)&&i("ucs2encode");65535<a&&(a-=65536,c+=y(a>>>10&1023|55296),a=56320|a&1023);return c+=y(a)}).join("")}function s(e,
b,l){for(var d=0,e=l?v(e/c):e>>1,e=e+v(e/b);e>w*p>>1;d+=q)e=v(e/w);return v(d+(w+1)*e/(e+a))}function t(a){var c=[],b=a.length,d,g=0,n=l,h=e,r,j,k,m,u;r=a.lastIndexOf(A);0>r&&(r=0);for(j=0;j<r;++j)128<=a.charCodeAt(j)&&i("not-basic"),c.push(a.charCodeAt(j));for(r=0<r?r+1:0;r<b;){j=g;d=1;for(k=q;;k+=q){r>=b&&i("invalid-input");m=a.charCodeAt(r++);m=10>m-48?m-22:26>m-65?m-65:26>m-97?m-97:q;(m>=q||m>v((x-g)/d))&&i("overflow");g+=m*d;u=k<=h?o:k>=h+p?p:k-h;if(m<u)break;m=q-u;d>v(x/m)&&i("overflow");d*=
m}d=c.length+1;h=s(g-j,d,0==j);v(g/d)>x-n&&i("overflow");n+=v(g/d);g%=d;c.splice(g++,0,n)}return f(c)}function h(a){var c,b,d,g,n,h,f,r,m,k=[],u,t,w,a=j(a);u=a.length;c=l;b=0;n=e;for(h=0;h<u;++h)m=a[h],128>m&&k.push(y(m));for((d=g=k.length)&&k.push(A);d<u;){f=x;for(h=0;h<u;++h)m=a[h],m>=c&&m<f&&(f=m);t=d+1;f-c>v((x-b)/t)&&i("overflow");b+=(f-c)*t;c=f;for(h=0;h<u;++h)if(m=a[h],m<c&&++b>x&&i("overflow"),m==c){r=b;for(f=q;;f+=q){m=f<=n?o:f>=n+p?p:f-n;if(r<m)break;w=r-m;r=q-m;k.push(y(m+w%r+22+75*(26>
m+w%r)-0));r=v(w/r)}k.push(y(r+22+75*(26>r)-0));n=s(b,t,d==g);b=0;++d}++b;++c}return k.join("")}var b,g="function"==typeof define&&"object"==typeof define.amd&&define.amd&&define,n="object"==typeof exports&&exports,m="object"==typeof module&&module,x=2147483647,q=36,o=1,p=26,a=38,c=700,e=72,l=128,A="-",r=/[^ -~]/,u=/^xn--/,B={overflow:"Overflow: input needs wider integers to process.",ucs2decode:"UCS-2(decode): illegal sequence",ucs2encode:"UCS-2(encode): illegal value","not-basic":"Illegal input >= 0x80 (not a basic code point)",
"invalid-input":"Invalid input"},w=q-o,v=Math.floor,y=String.fromCharCode,z;b={version:"0.3.0",ucs2:{decode:j,encode:f},decode:t,encode:h,toASCII:function(a){return k(a.split("."),function(a){return r.test(a)?"xn--"+h(a):a}).join(".")},toUnicode:function(a){return k(a.split("."),function(a){return u.test(a)?t(a.slice(4).toLowerCase()):a}).join(".")}};if(n)if(m&&m.exports==n)m.exports=b;else for(z in b)b.hasOwnProperty(z)&&(n[z]=b[z]);else g?define("punycode",b):d.punycode=b})(this);
(function(){var d={list:{ac:"com|gov|mil|net|org",ae:"ac|co|gov|mil|name|net|org|pro|sch",af:"com|edu|gov|net|org",al:"com|edu|gov|mil|net|org",ao:"co|ed|gv|it|og|pb",ar:"com|edu|gob|gov|int|mil|net|org|tur",at:"ac|co|gv|or",au:"asn|com|csiro|edu|gov|id|net|org",ba:"co|com|edu|gov|mil|net|org|rs|unbi|unmo|unsa|untz|unze",bb:"biz|co|com|edu|gov|info|net|org|store|tv",bh:"biz|cc|com|edu|gov|info|net|org",bn:"com|edu|gov|net|org",bo:"com|edu|gob|gov|int|mil|net|org|tv",br:"adm|adv|agr|am|arq|art|ato|b|bio|blog|bmd|cim|cng|cnt|com|coop|ecn|edu|eng|esp|etc|eti|far|flog|fm|fnd|fot|fst|g12|ggf|gov|imb|ind|inf|jor|jus|lel|mat|med|mil|mus|net|nom|not|ntr|odo|org|ppg|pro|psc|psi|qsl|rec|slg|srv|tmp|trd|tur|tv|vet|vlog|wiki|zlg",
bs:"com|edu|gov|net|org",bz:"du|et|om|ov|rg",ca:"ab|bc|mb|nb|nf|nl|ns|nt|nu|on|pe|qc|sk|yk",ck:"biz|co|edu|gen|gov|info|net|org",cn:"ac|ah|bj|com|cq|edu|fj|gd|gov|gs|gx|gz|ha|hb|he|hi|hl|hn|jl|js|jx|ln|mil|net|nm|nx|org|qh|sc|sd|sh|sn|sx|tj|tw|xj|xz|yn|zj",co:"com|edu|gov|mil|net|nom|org",cr:"ac|c|co|ed|fi|go|or|sa",cy:"ac|biz|com|ekloges|gov|ltd|name|net|org|parliament|press|pro|tm","do":"art|com|edu|gob|gov|mil|net|org|sld|web",dz:"art|asso|com|edu|gov|net|org|pol",ec:"com|edu|fin|gov|info|med|mil|net|org|pro",
eg:"com|edu|eun|gov|mil|name|net|org|sci",er:"com|edu|gov|ind|mil|net|org|rochest|w",es:"com|edu|gob|nom|org",et:"biz|com|edu|gov|info|name|net|org",fj:"ac|biz|com|info|mil|name|net|org|pro",fk:"ac|co|gov|net|nom|org",fr:"asso|com|f|gouv|nom|prd|presse|tm",gg:"co|net|org",gh:"com|edu|gov|mil|org",gn:"ac|com|gov|net|org",gr:"com|edu|gov|mil|net|org",gt:"com|edu|gob|ind|mil|net|org",gu:"com|edu|gov|net|org",hk:"com|edu|gov|idv|net|org",id:"ac|co|go|mil|net|or|sch|web",il:"ac|co|gov|idf|k12|muni|net|org",
"in":"ac|co|edu|ernet|firm|gen|gov|i|ind|mil|net|nic|org|res",iq:"com|edu|gov|i|mil|net|org",ir:"ac|co|dnssec|gov|i|id|net|org|sch",it:"edu|gov",je:"co|net|org",jo:"com|edu|gov|mil|name|net|org|sch",jp:"ac|ad|co|ed|go|gr|lg|ne|or",ke:"ac|co|go|info|me|mobi|ne|or|sc",kh:"com|edu|gov|mil|net|org|per",ki:"biz|com|de|edu|gov|info|mob|net|org|tel",km:"asso|com|coop|edu|gouv|k|medecin|mil|nom|notaires|pharmaciens|presse|tm|veterinaire",kn:"edu|gov|net|org",kr:"ac|busan|chungbuk|chungnam|co|daegu|daejeon|es|gangwon|go|gwangju|gyeongbuk|gyeonggi|gyeongnam|hs|incheon|jeju|jeonbuk|jeonnam|k|kg|mil|ms|ne|or|pe|re|sc|seoul|ulsan",
kw:"com|edu|gov|net|org",ky:"com|edu|gov|net|org",kz:"com|edu|gov|mil|net|org",lb:"com|edu|gov|net|org",lk:"assn|com|edu|gov|grp|hotel|int|ltd|net|ngo|org|sch|soc|web",lr:"com|edu|gov|net|org",lv:"asn|com|conf|edu|gov|id|mil|net|org",ly:"com|edu|gov|id|med|net|org|plc|sch",ma:"ac|co|gov|m|net|org|press",mc:"asso|tm",me:"ac|co|edu|gov|its|net|org|priv",mg:"com|edu|gov|mil|nom|org|prd|tm",mk:"com|edu|gov|inf|name|net|org|pro",ml:"com|edu|gov|net|org|presse",mn:"edu|gov|org",mo:"com|edu|gov|net|org",
mt:"com|edu|gov|net|org",mv:"aero|biz|com|coop|edu|gov|info|int|mil|museum|name|net|org|pro",mw:"ac|co|com|coop|edu|gov|int|museum|net|org",mx:"com|edu|gob|net|org",my:"com|edu|gov|mil|name|net|org|sch",nf:"arts|com|firm|info|net|other|per|rec|store|web",ng:"biz|com|edu|gov|mil|mobi|name|net|org|sch",ni:"ac|co|com|edu|gob|mil|net|nom|org",np:"com|edu|gov|mil|net|org",nr:"biz|com|edu|gov|info|net|org",om:"ac|biz|co|com|edu|gov|med|mil|museum|net|org|pro|sch",pe:"com|edu|gob|mil|net|nom|org|sld",ph:"com|edu|gov|i|mil|net|ngo|org",
pk:"biz|com|edu|fam|gob|gok|gon|gop|gos|gov|net|org|web",pl:"art|bialystok|biz|com|edu|gda|gdansk|gorzow|gov|info|katowice|krakow|lodz|lublin|mil|net|ngo|olsztyn|org|poznan|pwr|radom|slupsk|szczecin|torun|warszawa|waw|wroc|wroclaw|zgora",pr:"ac|biz|com|edu|est|gov|info|isla|name|net|org|pro|prof",ps:"com|edu|gov|net|org|plo|sec",pw:"belau|co|ed|go|ne|or",ro:"arts|com|firm|info|nom|nt|org|rec|store|tm|www",rs:"ac|co|edu|gov|in|org",sb:"com|edu|gov|net|org",sc:"com|edu|gov|net|org",sh:"co|com|edu|gov|net|nom|org",
sl:"com|edu|gov|net|org",st:"co|com|consulado|edu|embaixada|gov|mil|net|org|principe|saotome|store",sv:"com|edu|gob|org|red",sz:"ac|co|org",tr:"av|bbs|bel|biz|com|dr|edu|gen|gov|info|k12|name|net|org|pol|tel|tsk|tv|web",tt:"aero|biz|cat|co|com|coop|edu|gov|info|int|jobs|mil|mobi|museum|name|net|org|pro|tel|travel",tw:"club|com|ebiz|edu|game|gov|idv|mil|net|org",mu:"ac|co|com|gov|net|or|org",mz:"ac|co|edu|gov|org",na:"co|com",nz:"ac|co|cri|geek|gen|govt|health|iwi|maori|mil|net|org|parliament|school",
pa:"abo|ac|com|edu|gob|ing|med|net|nom|org|sld",pt:"com|edu|gov|int|net|nome|org|publ",py:"com|edu|gov|mil|net|org",qa:"com|edu|gov|mil|net|org",re:"asso|com|nom",ru:"ac|adygeya|altai|amur|arkhangelsk|astrakhan|bashkiria|belgorod|bir|bryansk|buryatia|cbg|chel|chelyabinsk|chita|chukotka|chuvashia|com|dagestan|e-burg|edu|gov|grozny|int|irkutsk|ivanovo|izhevsk|jar|joshkar-ola|kalmykia|kaluga|kamchatka|karelia|kazan|kchr|kemerovo|khabarovsk|khakassia|khv|kirov|koenig|komi|kostroma|kranoyarsk|kuban|kurgan|kursk|lipetsk|magadan|mari|mari-el|marine|mil|mordovia|mosreg|msk|murmansk|nalchik|net|nnov|nov|novosibirsk|nsk|omsk|orenburg|org|oryol|penza|perm|pp|pskov|ptz|rnd|ryazan|sakhalin|samara|saratov|simbirsk|smolensk|spb|stavropol|stv|surgut|tambov|tatarstan|tom|tomsk|tsaritsyn|tsk|tula|tuva|tver|tyumen|udm|udmurtia|ulan-ude|vladikavkaz|vladimir|vladivostok|volgograd|vologda|voronezh|vrn|vyatka|yakutia|yamal|yekaterinburg|yuzhno-sakhalinsk",
rw:"ac|co|com|edu|gouv|gov|int|mil|net",sa:"com|edu|gov|med|net|org|pub|sch",sd:"com|edu|gov|info|med|net|org|tv",se:"a|ac|b|bd|c|d|e|f|g|h|i|k|l|m|n|o|org|p|parti|pp|press|r|s|t|tm|u|w|x|y|z",sg:"com|edu|gov|idn|net|org|per",sn:"art|com|edu|gouv|org|perso|univ",sy:"com|edu|gov|mil|net|news|org",th:"ac|co|go|in|mi|net|or",tj:"ac|biz|co|com|edu|go|gov|info|int|mil|name|net|nic|org|test|web",tn:"agrinet|com|defense|edunet|ens|fin|gov|ind|info|intl|mincom|nat|net|org|perso|rnrt|rns|rnu|tourism",tz:"ac|co|go|ne|or",
ua:"biz|cherkassy|chernigov|chernovtsy|ck|cn|co|com|crimea|cv|dn|dnepropetrovsk|donetsk|dp|edu|gov|if|in|ivano-frankivsk|kh|kharkov|kherson|khmelnitskiy|kiev|kirovograd|km|kr|ks|kv|lg|lugansk|lutsk|lviv|me|mk|net|nikolaev|od|odessa|org|pl|poltava|pp|rovno|rv|sebastopol|sumy|te|ternopil|uzhgorod|vinnica|vn|zaporizhzhe|zhitomir|zp|zt",ug:"ac|co|go|ne|or|org|sc",uk:"ac|bl|british-library|co|cym|gov|govt|icnet|jet|lea|ltd|me|mil|mod|national-library-scotland|nel|net|nhs|nic|nls|org|orgn|parliament|plc|police|sch|scot|soc",
us:"dni|fed|isa|kids|nsn",uy:"com|edu|gub|mil|net|org",ve:"co|com|edu|gob|info|mil|net|org|web",vi:"co|com|k12|net|org",vn:"ac|biz|com|edu|gov|health|info|int|name|net|org|pro",ye:"co|com|gov|ltd|me|net|org|plc",yu:"ac|co|edu|gov|org",za:"ac|agric|alt|bourse|city|co|cybernet|db|edu|gov|grondar|iaccess|imt|inca|landesign|law|mil|net|ngo|nis|nom|olivetti|org|pix|school|tm|web",zm:"ac|co|com|edu|gov|net|org|sch"},has_expression:null,is_expression:null,has:function(i){return!!i.match(d.has_expression)},
is:function(i){return!!i.match(d.is_expression)},get:function(i){return(i=i.match(d.has_expression))&&i[1]||null},init:function(){var i="",k;for(k in d.list)Object.prototype.hasOwnProperty.call(d.list,k)&&(i+="|("+("("+d.list[k]+")."+k)+")");d.has_expression=RegExp("\\.("+i.substr(1)+")$","i");d.is_expression=RegExp("^("+i.substr(1)+")$","i")}};d.init();"undefined"!==typeof module&&module.exports?module.exports=d:window.SecondLevelDomains=d})();
(function(d){function i(a){return a.replace(/([.*+?^=!:${}()|[\]\/\\])/g,"\\$1")}function k(a){return"[object Array]"===String(Object.prototype.toString.call(a))}function j(a){return encodeURIComponent(a).replace(/[!'()*]/g,escape)}var f="undefined"!==typeof module&&module.exports,s=f?require("./punycode"):window.punycode,t=f?require("./IPv6"):window.IPv6,h=f?require("./SecondLevelDomains"):window.SecondLevelDomains,b=function(a,c){if(!(this instanceof b))return new b(a,c);a===d&&(a="undefined"!==
typeof location?location.href+"":"");this.href(a);return c!==d?this.absoluteTo(c):this},f=b.prototype;b.idn_expression=/[^a-z0-9\.-]/i;b.punycode_expression=/(xn--)/i;b.ip4_expression=/^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$/;b.ip6_expression=/^\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:)))(%.+)?\s*$/;
b.find_uri_expression=/\b((?:[a-z][\w-]+:(?:\/{1,3}|[a-z0-9%])|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}\/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'".,<>?\u00ab\u00bb\u201c\u201d\u2018\u2019]))/ig;b.defaultPorts={http:"80",https:"443",ftp:"21"};b.invalid_hostname_characters=/[^a-zA-Z0-9\.-]/;b.encode=j;b.decode=decodeURIComponent;b.iso8859=function(){b.encode=escape;b.decode=unescape};b.unicode=function(){b.encode=j;b.decode=decodeURIComponent};
b.characters={pathname:{encode:{expression:/%(24|26|2B|2C|3B|3D|3A|40)/ig,map:{"%24":"$","%26":"&","%2B":"+","%2C":",","%3B":";","%3D":"=","%3A":":","%40":"@"}},decode:{expression:/[\/\?#]/g,map:{"/":"%2F","?":"%3F","#":"%23"}}},reserved:{encode:{expression:/%(21|23|24|26|27|28|29|2A|2B|2C|2F|3A|3B|3D|3F|40|5B|5D)/ig,map:{"%3A":":","%2F":"/","%3F":"?","%23":"#","%5B":"[","%5D":"]","%40":"@","%21":"!","%24":"$","%26":"&","%27":"'","%28":"(","%29":")","%2A":"*","%2B":"+","%2C":",","%3B":";","%3D":"="}}}};
b.encodeQuery=function(a){return b.encode(a+"").replace(/%20/g,"+")};b.decodeQuery=function(a){return b.decode((a+"").replace(/\+/g,"%20"))};b.recodePath=function(a){for(var a=(a+"").split("/"),c=0,e=a.length;c<e;c++)a[c]=b.encodePathSegment(b.decode(a[c]));return a.join("/")};b.decodePath=function(a){for(var a=(a+"").split("/"),c=0,e=a.length;c<e;c++)a[c]=b.decodePathSegment(a[c]);return a.join("/")};var g={encode:"encode",decode:"decode"},n,m=function(a,c){return function(e){return b[c](e+"").replace(b.characters[a][c].expression,
function(e){return b.characters[a][c].map[e]})}};for(n in g)b[n+"PathSegment"]=m("pathname",g[n]);b.encodeReserved=m("reserved","encode");b.parse=function(a){var c,e={};c=a.indexOf("#");-1<c&&(e.fragment=a.substring(c+1)||null,a=a.substring(0,c));c=a.indexOf("?");-1<c&&(e.query=a.substring(c+1)||null,a=a.substring(0,c));"//"===a.substring(0,2)?(e.protocol="",a=a.substring(2),a=b.parseAuthority(a,e)):(c=a.indexOf(":"),-1<c&&(e.protocol=a.substring(0,c),"//"===a.substring(c+1,c+3)?(a=a.substring(c+
3),a=b.parseAuthority(a,e)):(a=a.substring(c+1),e.urn=!0)));e.path=a;return e};b.parseHost=function(a,c){var e=a.indexOf("/"),b;-1===e&&(e=a.length);"["===a[0]?(b=a.indexOf("]"),c.hostname=a.substring(1,b)||null,c.port=a.substring(b+2,e)||null):a.indexOf(":")!==a.lastIndexOf(":")?(c.hostname=a.substring(0,e)||null,c.port=null):(b=a.substring(0,e).split(":"),c.hostname=b[0]||null,c.port=b[1]||null);c.hostname&&"/"!==a.substring(e)[0]&&(e++,a="/"+a);return a.substring(e)||"/"};b.parseAuthority=function(a,
c){a=b.parseUserinfo(a,c);return b.parseHost(a,c)};b.parseUserinfo=function(a,c){var e=a.indexOf("@"),l=a.indexOf("/");-1<e&&(-1===l||e<l)?(l=a.substring(0,e).split(":"),c.username=l[0]?b.decode(l[0]):null,c.password=l[1]?b.decode(l[1]):null,a=a.substring(e+1)):(c.username=null,c.password=null);return a};b.parseQuery=function(a){if(!a)return{};a=a.replace(/&+/g,"&").replace(/^\?*&*|&+$/g,"");if(!a)return{};for(var c={},a=a.split("&"),e=a.length,l=0;l<e;l++){var d=a[l].split("="),g=b.decodeQuery(d.shift()),
d=d.length?b.decodeQuery(d.join("=")):null;c[g]?("string"===typeof c[g]&&(c[g]=[c[g]]),c[g].push(d)):c[g]=d}return c};b.build=function(a){var c="";a.protocol&&(c+=a.protocol+":");if(!a.urn&&(c||a.hostname))c+="//";c+=b.buildAuthority(a)||"";"string"===typeof a.path&&("/"!==a.path[0]&&"string"===typeof a.hostname&&(c+="/"),c+=a.path);"string"===typeof a.query&&(c+="?"+a.query);"string"===typeof a.fragment&&(c+="#"+a.fragment);return c};b.buildHost=function(a){var c="";if(a.hostname)b.ip6_expression.test(a.hostname)?
c=a.port?c+("["+a.hostname+"]:"+a.port):c+a.hostname:(c+=a.hostname,a.port&&(c+=":"+a.port));else return"";return c};b.buildAuthority=function(a){return b.buildUserinfo(a)+b.buildHost(a)};b.buildUserinfo=function(a){var c="";a.username&&(c+=b.encode(a.username),a.password&&(c+=":"+b.encode(a.password)),c+="@");return c};b.buildQuery=function(a,c){var e="",l;for(l in a)if(Object.hasOwnProperty.call(a,l)&&l)if(k(a[l]))for(var g={},h=0,f=a[l].length;h<f;h++)a[l][h]!==d&&g[a[l][h]+""]===d&&(e+="&"+b.buildQueryParameter(l,
a[l][h]),!0!==c&&(g[a[l][h]+""]=!0));else a[l]!==d&&(e+="&"+b.buildQueryParameter(l,a[l]));return e.substring(1)};b.buildQueryParameter=function(a,c){return b.encodeQuery(a)+(null!==c?"="+b.encodeQuery(c):"")};b.addQuery=function(a,c,e){if("object"===typeof c)for(var l in c)Object.prototype.hasOwnProperty.call(c,l)&&b.addQuery(a,l,c[l]);else if("string"===typeof c)a[c]===d?a[c]=e:("string"===typeof a[c]&&(a[c]=[a[c]]),k(e)||(e=[e]),a[c]=a[c].concat(e));else throw new TypeError("URI.addQuery() accepts an object, string as the name parameter");
};b.removeQuery=function(a,c,e){if(k(c))for(var e=0,l=c.length;e<l;e++)a[c[e]]=d;else if("object"===typeof c)for(l in c)Object.prototype.hasOwnProperty.call(c,l)&&b.removeQuery(a,l,c[l]);else if("string"===typeof c)if(e!==d)if(a[c]===e)a[c]=d;else{if(k(a[c])){var l=a[c],g={},h,f;if(k(e)){h=0;for(f=e.length;h<f;h++)g[e[h]]=!0}else g[e]=!0;h=0;for(f=l.length;h<f;h++)g[l[h]]!==d&&(l.splice(h,1),f--,h--);a[c]=l}}else a[c]=d;else throw new TypeError("URI.addQuery() accepts an object, string as the first parameter");
};b.commonPath=function(a,c){var e=Math.min(a.length,c.length),b;for(b=0;b<e;b++)if(a[b]!==c[b]){b--;break}if(1>b)return a[0]===c[0]&&"/"===a[0]?"/":"";"/"!==a[b]&&(b=a.substring(0,b).lastIndexOf("/"));return a.substring(0,b+1)};b.withinString=function(a,c){return a.replace(b.find_uri_expression,c)};b.ensureValidHostname=function(a){if(a.match(b.invalid_hostname_characters)){if(!s)throw new TypeError("Hostname '"+a+"' contains characters other than [A-Z0-9.-] and Punycode.js is not available");if(s.toASCII(a).match(b.invalid_hostname_characters))throw new TypeError("Hostname '"+
a+"' contains characters other than [A-Z0-9.-]");}};f.build=function(a){if(!0===a)this._deferred_build=!0;else if(a===d||this._deferred_build)this._string=b.build(this._parts),this._deferred_build=!1;return this};f.clone=function(){return new b(this)};f.toString=function(){return this.build(!1)._string};f.valueOf=function(){return this.toString()};g={protocol:"protocol",username:"username",password:"password",hostname:"hostname",port:"port"};m=function(a){return function(c,b){if(c===d)return this._parts[a]||
"";this._parts[a]=c;this.build(!b);return this}};for(n in g)f[n]=m(g[n]);g={query:"?",fragment:"#"};m=function(a,c){return function(b,l){if(b===d)return this._parts[a]||"";null!==b&&(b+="",b[0]===c&&(b=b.substring(1)));this._parts[a]=b;this.build(!l);return this}};for(n in g)f[n]=m(n,g[n]);g={search:["?","query"],hash:["#","fragment"]};m=function(a,c){return function(b,d){var g=this[a](b,d);return"string"===typeof g&&g.length?c+g:g}};for(n in g)f[n]=m(g[n][1],g[n][0]);f.pathname=function(a,c){if(a===
d||!0===a){var e=this._parts.path||(this._parts.urn?"":"/");return a?b.decodePath(e):e}this._parts.path=a?b.recodePath(a):"/";this.build(!c);return this};f.path=f.pathname;f.href=function(a,c){if(a===d)return this.toString();this._string="";this._parts={protocol:null,username:null,password:null,hostname:null,urn:null,port:null,path:null,query:null,fragment:null};var e=a instanceof b,l="object"===typeof a&&(a.hostname||a.path),g;if("string"===typeof a)this._parts=b.parse(a);else if(e||l)for(g in e=
e?a._parts:a,e)Object.hasOwnProperty.call(this._parts,g)&&(this._parts[g]=e[g]);else throw new TypeError("invalid input");this.build(!c);return this};f.is=function(a){var c=!1,e=!1,d=!1,g=!1,f=!1,n=!1,m=!1,j=!this._parts.urn;this._parts.hostname&&(j=!1,e=b.ip4_expression.test(this._parts.hostname),d=b.ip6_expression.test(this._parts.hostname),c=e||d,f=(g=!c)&&h&&h.has(this._parts.hostname),n=g&&b.idn_expression.test(this._parts.hostname),m=g&&b.punycode_expression.test(this._parts.hostname));switch(a.toLowerCase()){case "relative":return j;
case "absolute":return!j;case "domain":case "name":return g;case "sld":return f;case "ip":return c;case "ip4":case "ipv4":case "inet4":return e;case "ip6":case "ipv6":case "inet6":return d;case "idn":return n;case "url":return!this._parts.urn;case "urn":return!!this._parts.urn;case "punycode":return m}return null};var x=f.protocol,q=f.port,o=f.hostname;f.protocol=function(a,c){if(a!==d&&a&&(a=a.replace(/:(\/\/)?$/,""),a.match(/[^a-zA-z0-9\.+-]/)))throw new TypeError("Protocol '"+a+"' contains characters other than [A-Z0-9.+-]");
return x.call(this,a,c)};f.scheme=f.protocol;f.port=function(a,c){if(this._parts.urn)return a===d?"":this;if(a!==d&&(0===a&&(a=null),a&&(a+="",":"===a[0]&&(a=a.substring(1)),a.match(/[^0-9]/))))throw new TypeError("Port '"+a+"' contains characters other than [0-9]");return q.call(this,a,c)};f.hostname=function(a,c){if(this._parts.urn)return a===d?"":this;if(a!==d){var e={};b.parseHost(a,e);a=e.hostname}return o.call(this,a,c)};f.host=function(a,c){if(this._parts.urn)return a===d?"":this;if(a===d)return this._parts.hostname?
b.buildHost(this._parts):"";b.parseHost(a,this._parts);this.build(!c);return this};f.authority=function(a,c){if(this._parts.urn)return a===d?"":this;if(a===d)return this._parts.hostname?b.buildAuthority(this._parts):"";b.parseAuthority(a,this._parts);this.build(!c);return this};f.userinfo=function(a,c){if(this._parts.urn)return a===d?"":this;if(a===d){if(!this._parts.username)return"";var e=b.buildUserinfo(this._parts);return e.substring(0,e.length-1)}"@"!==a[a.length-1]&&(a+="@");b.parseUserinfo(a,
this._parts);this.build(!c);return this};f.subdomain=function(a,c){if(this._parts.urn)return a===d?"":this;if(a===d){if(!this._parts.hostname||this.is("IP"))return"";var e=this._parts.hostname.length-this.domain().length-1;return this._parts.hostname.substring(0,e)||""}e=this._parts.hostname.length-this.domain().length;e=this._parts.hostname.substring(0,e);e=RegExp("^"+i(e));a&&"."!==a[a.length-1]&&(a+=".");a&&b.ensureValidHostname(a);this._parts.hostname=this._parts.hostname.replace(e,a);this.build(!c);
return this};f.domain=function(a,c){if(this._parts.urn)return a===d?"":this;"boolean"===typeof a&&(c=a,a=d);if(a===d){if(!this._parts.hostname||this.is("IP"))return"";var e=this._parts.hostname.match(/\./g);if(e&&2>e.length)return this._parts.hostname;e=this._parts.hostname.length-this.tld(c).length-1;e=this._parts.hostname.lastIndexOf(".",e-1)+1;return this._parts.hostname.substring(e)||""}if(!a)throw new TypeError("cannot set domain empty");b.ensureValidHostname(a);!this._parts.hostname||this.is("IP")?
this._parts.hostname=a:(e=RegExp(i(this.domain())+"$"),this._parts.hostname=this._parts.hostname.replace(e,a));this.build(!c);return this};f.tld=function(a,c){if(this._parts.urn)return a===d?"":this;"boolean"===typeof a&&(c=a,a=d);if(a===d){if(!this._parts.hostname||this.is("IP"))return"";var b=this._parts.hostname.lastIndexOf("."),b=this._parts.hostname.substring(b+1);return!0!==c&&h&&h.list[b.toLowerCase()]?h.get(this._parts.hostname)||b:b}if(a)if(a.match(/[^a-zA-Z0-9-]/))if(h&&h.is(a))b=RegExp(i(this.tld())+
"$"),this._parts.hostname=this._parts.hostname.replace(b,a);else throw new TypeError("TLD '"+a+"' contains characters other than [A-Z0-9]");else{if(!this._parts.hostname||this.is("IP"))throw new ReferenceError("cannot set TLD on non-domain host");b=RegExp(i(this.tld())+"$");this._parts.hostname=this._parts.hostname.replace(b,a)}else throw new TypeError("cannot set TLD empty");this.build(!c);return this};f.directory=function(a,c){if(this._parts.urn)return a===d?"":this;if(a===d||!0===a){if(!this._parts.path&&
!this._parts.hostname)return"";if("/"===this._parts.path)return"/";var e=this._parts.path.length-this.filename().length-1,e=this._parts.path.substring(0,e)||(this._parts.hostname?"/":"");return a?b.decodePath(e):e}e=this._parts.path.length-this.filename().length;e=this._parts.path.substring(0,e);e=RegExp("^"+i(e));this.is("relative")||(a||(a="/"),"/"!==a[0]&&(a="/"+a));a&&"/"!==a[a.length-1]&&(a+="/");a=b.recodePath(a);this._parts.path=this._parts.path.replace(e,a);this.build(!c);return this};f.filename=
function(a,c){if(this._parts.urn)return a===d?"":this;if(a===d||!0===a){if(!this._parts.path||"/"===this._parts.path)return"";var e=this._parts.path.lastIndexOf("/"),e=this._parts.path.substring(e+1);return a?b.decodePathSegment(e):e}e=!1;"/"===a[0]&&(a=a.substring(1));a.match(/\.?\//)&&(e=!0);var g=RegExp(i(this.filename())+"$"),a=b.recodePath(a);this._parts.path=this._parts.path.replace(g,a);e?this.normalizePath(c):this.build(!c);return this};f.suffix=function(a,c){if(this._parts.urn)return a===
d?"":this;if(a===d||!0===a){if(!this._parts.path||"/"===this._parts.path)return"";var e=this.filename(),g=e.lastIndexOf(".");if(-1===g)return"";e=e.substring(g+1);e=/^[a-z0-9%]+$/i.test(e)?e:"";return a?b.decodePathSegment(e):e}"."===a[0]&&(a=a.substring(1));if(e=this.suffix())g=a?RegExp(i(e)+"$"):RegExp(i("."+e)+"$");else{if(!a)return this;this._parts.path+="."+b.recodePath(a)}g&&(a=b.recodePath(a),this._parts.path=this._parts.path.replace(g,a));this.build(!c);return this};f.segment=function(a,c,
b){var g=this._parts.urn?":":"/",h=this.path(),f="/"===h.substring(0,1),h=h.split(g);"number"!==typeof a&&(b=c,c=a,a=d);if(a!==d&&"number"!==typeof a)throw Error("Bad segment '"+a+"', must be 0-based integer");f&&h.shift();0>a&&(a=Math.max(h.length+a,0));if(c===d)return a===d?h:h[a];if(null===a||h[a]===d)if(k(c))h=c;else{if(c||"string"===typeof c&&c.length)""===h[h.length-1]?h[h.length-1]=c:h.push(c)}else c||"string"===typeof c&&c.length?h[a]=c:h.splice(a,1);f&&h.unshift("");return this.path(h.join(g),
b)};var p=f.query;f.query=function(a,c){return!0===a?b.parseQuery(this._parts.query):a!==d&&"string"!==typeof a?(this._parts.query=b.buildQuery(a),this.build(!c),this):p.call(this,a,c)};f.addQuery=function(a,c,e){var d=b.parseQuery(this._parts.query);b.addQuery(d,a,c);this._parts.query=b.buildQuery(d);"string"!==typeof a&&(e=c);this.build(!e);return this};f.removeQuery=function(a,c,e){var d=b.parseQuery(this._parts.query);b.removeQuery(d,a,c);this._parts.query=b.buildQuery(d);"string"!==typeof a&&
(e=c);this.build(!e);return this};f.addSearch=f.addQuery;f.removeSearch=f.removeQuery;f.normalize=function(){return this._parts.urn?this.normalizeProtocol(!1).normalizeQuery(!1).normalizeFragment(!1).build():this.normalizeProtocol(!1).normalizeHostname(!1).normalizePort(!1).normalizePath(!1).normalizeQuery(!1).normalizeFragment(!1).build()};f.normalizeProtocol=function(a){"string"===typeof this._parts.protocol&&(this._parts.protocol=this._parts.protocol.toLowerCase(),this.build(!a));return this};
f.normalizeHostname=function(a){this._parts.hostname&&(this.is("IDN")&&s?this._parts.hostname=s.toASCII(this._parts.hostname):this.is("IPv6")&&t&&(this._parts.hostname=t.best(this._parts.hostname)),this._parts.hostname=this._parts.hostname.toLowerCase(),this.build(!a));return this};f.normalizePort=function(a){"string"===typeof this._parts.protocol&&this._parts.port===b.defaultPorts[this._parts.protocol]&&(this._parts.port=null,this.build(!a));return this};f.normalizePath=function(a){if(this._parts.urn||
!this._parts.path||"/"===this._parts.path)return this;var c,e,d=this._parts.path,g,h;"/"!==d[0]&&("."===d[0]&&(e=d.substring(0,d.indexOf("/"))),c=!0,d="/"+d);for(d=d.replace(/(\/(\.\/)+)|\/{2,}/g,"/");;){g=d.indexOf("/../");if(-1===g)break;else if(0===g){d=d.substring(3);break}h=d.substring(0,g).lastIndexOf("/");-1===h&&(h=g);d=d.substring(0,h)+d.substring(g+3)}c&&this.is("relative")&&(d=e?e+d:d.substring(1));d=b.recodePath(d);this._parts.path=d;this.build(!a);return this};f.normalizePathname=f.normalizePath;
f.normalizeQuery=function(a){"string"===typeof this._parts.query&&(this._parts.query.length?this.query(b.parseQuery(this._parts.query)):this._parts.query=null,this.build(!a));return this};f.normalizeFragment=function(a){this._parts.fragment||(this._parts.fragment=null,this.build(!a));return this};f.normalizeSearch=f.normalizeQuery;f.normalizeHash=f.normalizeFragment;f.iso8859=function(){var a=b.encode,c=b.decode;b.encode=escape;b.decode=decodeURIComponent;this.normalize();b.encode=a;b.decode=c;return this};
f.unicode=function(){var a=b.encode,c=b.decode;b.encode=j;b.decode=unescape;this.normalize();b.encode=a;b.decode=c;return this};f.readable=function(){var a=this.clone();a.username("").password("").normalize();var c="";a._parts.protocol&&(c+=a._parts.protocol+"://");a._parts.hostname&&(a.is("punycode")&&s?(c+=s.toUnicode(a._parts.hostname),a._parts.port&&(c+=":"+a._parts.port)):c+=a.host());a._parts.hostname&&(a._parts.path&&"/"!==a._parts.path[0])&&(c+="/");c+=a.path(!0);if(a._parts.query){for(var e=
"",g=0,h=a._parts.query.split("&"),f=h.length;g<f;g++){var n=(h[g]||"").split("="),e=e+("&"+b.decodeQuery(n[0]).replace(/&/g,"%26"));n[1]!==d&&(e+="="+b.decodeQuery(n[1]).replace(/&/g,"%26"))}c+="?"+e.substring(1)}return c+=a.hash()};f.absoluteTo=function(a){var c=this.clone(),e=["protocol","username","password","hostname","port"],d,g;if(this._parts.urn)throw Error("URNs do not have any generally defined hierachical components");if(this._parts.hostname)return c;a instanceof b||(a=new b(a));d=0;for(g;g=
e[d];d++)c._parts[g]=a._parts[g];e=["query","path"];d=0;for(g;g=e[d];d++)!c._parts[g]&&a._parts[g]&&(c._parts[g]=a._parts[g]);"/"!==c.path()[0]&&(a=a.directory(),c._parts.path=(a?a+"/":"")+c._parts.path,c.normalizePath());c.build();return c};f.relativeTo=function(a){var c=this.clone(),e=["protocol","username","password","hostname","port"],d;if(this._parts.urn)throw Error("URNs do not have any generally defined hierachical components");a instanceof b||(a=new b(a));if("/"!==this.path()[0]||"/"!==a.path()[0])throw Error("Cannot calculate common path from non-relative URLs");
d=b.commonPath(c.path(),a.path());for(var a=a.directory(),g=0,h;h=e[g];g++)c._parts[h]=null;if(!d||"/"===d)return c;if(a+"/"===d)c._parts.path="./"+c.filename();else{e="../";d=RegExp("^"+i(d));for(a=a.replace(d,"/").match(/\//g).length-1;a--;)e+="../";c._parts.path=c._parts.path.replace(d,e)}c.build();return c};f.equals=function(a){var c=this.clone(),d=new b(a),g={},h={},a={},f;c.normalize();d.normalize();if(c.toString()===d.toString())return!0;g=c.query();h=d.query();c.query("");d.query("");if(c.toString()!==
d.toString()||g.length!==h.length)return!1;g=b.parseQuery(g);h=b.parseQuery(h);for(f in g)if(Object.prototype.hasOwnProperty.call(g,f)){if(k(g[f])){if(!k(h[f])||g[f].length!==h[f].length)return!1;g[f].sort();h[f].sort();c=0;for(d=g[f].length;c<d;c++)if(g[f][c]!==h[f][c])return!1}else if(g[f]!==h[f])return!1;a[f]=!0}for(f in h)if(Object.prototype.hasOwnProperty.call(h,f)&&!a[f])return!1;return!0};"undefined"!==typeof module&&module.exports?module.exports=b:window.URI=b})();
(function(d){var i=Object.prototype.hasOwnProperty,k="undefined"!==typeof module&&module.exports?require("./URI"):window.URI,j=function(d){if(j._cache[d])return j._cache[d];if(!(this instanceof j))return new j(d);this.expression=d;j._cache[d]=this;return this},f=function(d){this.data=d;this.cache={}},s=j.prototype,t={"":{prefix:"",separator:",",named:!1,empty_name_separator:!1,encode:"encode"},"+":{prefix:"",separator:",",named:!1,empty_name_separator:!1,encode:"encodeReserved"},"#":{prefix:"#",separator:",",
named:!1,empty_name_separator:!1,encode:"encodeReserved"},".":{prefix:".",separator:".",named:!1,empty_name_separator:!1,encode:"encode"},"/":{prefix:"/",separator:"/",named:!1,empty_name_separator:!1,encode:"encode"},";":{prefix:";",separator:";",named:!0,empty_name_separator:!1,encode:"encode"},"?":{prefix:"?",separator:"&",named:!0,empty_name_separator:!0,encode:"encode"},"&":{prefix:"&",separator:"&",named:!0,empty_name_separator:!0,encode:"encode"}};j._cache={};j.EXPRESSION_PATTERN=/\{([^a-zA-Z0-9%_]?)([^\}]+)(\}|$)/g;
j.VARIABLE_PATTERN=/^([^*:]+)((\*)|:(\d+))?$/;j.VARIABLE_NAME_PATTERN=/[^a-zA-Z0-9%_]/;j.expand=function(d,b){var g=t[d.operator],f=g.named?"Named":"Unnamed",m=d.variables,k=[],i,o,p;for(p=0;o=m[p];p++)i=b.get(o.name),i.val.length?k.push(j["expand"+f](i,g,o.explode,o.explode&&g.separator||",",o.maxlength,o.name)):i.type&&k.push("");return k.length?g.prefix+k.join(g.separator):""};j.expandNamed=function(h,b,g,f,m,j){var i="",o=b.encode,b=b.empty_name_separator,p=!h[o].length,a=2===h.type?"":k[o](j),
c,e,l;e=0;for(l=h.val.length;e<l;e++)(m?(c=k[o](h.val[e][1].substring(0,m)),2===h.type&&(a=k[o](h.val[e][0].substring(0,m)))):p?(c=k[o](h.val[e][1]),2===h.type?(a=k[o](h.val[e][0]),h[o].push([a,c])):h[o].push([d,c])):(c=h[o][e][1],2===h.type&&(a=h[o][e][0])),i&&(i+=f),g)?i+=a+(b||c?"=":"")+c:(e||(i+=k[o](j)+(b||c?"=":"")),2===h.type&&(i+=a+","),i+=c);return i};j.expandUnnamed=function(h,b,g,f,m){var i="",j=b.encode,b=b.empty_name_separator,o=!h[j].length,p,a,c,e;c=0;for(e=h.val.length;c<e;c++)m?a=
k[j](h.val[c][1].substring(0,m)):o?(a=k[j](h.val[c][1]),h[j].push([2===h.type?k[j](h.val[c][0]):d,a])):a=h[j][c][1],i&&(i+=f),2===h.type&&(p=m?k[j](h.val[c][0].substring(0,m)):h[j][c][0],i+=p,i=g?i+(b||a?"=":""):i+","),i+=a;return i};s.expand=function(d){var b="";(!this.parts||!this.parts.length)&&this.parse();d instanceof f||(d=new f(d));for(var g=0,n=this.parts.length;g<n;g++)b+="string"===typeof this.parts[g]?this.parts[g]:j.expand(this.parts[g],d);return b};s.parse=function(){var d=this.expression,
b=j.EXPRESSION_PATTERN,g=j.VARIABLE_PATTERN,f=j.VARIABLE_NAME_PATTERN,i=[],k=0,q,o,p;for(b.lastIndex=0;;){o=b.exec(d);if(null===o){i.push(d.substring(k));break}else i.push(d.substring(k,o.index)),k=o.index+o[0].length;if(t[o[1]]){if(!o[3])throw Error('Unclosed Expression "'+o[0]+'"');}else throw Error('Unknown Operator "'+o[1]+'" in "'+o[0]+'"');q=o[2].split(",");for(var a=0,c=q.length;a<c;a++){p=q[a].match(g);if(null===p)throw Error('Invalid Variable "'+q[a]+'" in "'+o[0]+'"');if(p[1].match(f))throw Error('Invalid Variable Name "'+
p[1]+'" in "'+o[0]+'"');q[a]={name:p[1],explode:!!p[3],maxlength:p[4]&&parseInt(p[4],10)}}if(!q.length)throw Error('Expression Missing Variable(s) "'+o[0]+'"');i.push({expression:o[0],operator:o[1],variables:q})}i.length||i.push(d);this.parts=i;return this};f.prototype.get=function(f){var b=this.data,g={type:0,val:[],encode:[],encodeReserved:[]},n;if(this.cache[f]!==d)return this.cache[f];this.cache[f]=g;b="[object Function]"===String(Object.prototype.toString.call(b))?b(f):"[object Function]"===
String(Object.prototype.toString.call(b[f]))?b[f](f):b[f];if(b===d||null===b)return g;if("[object Array]"===String(Object.prototype.toString.call(b))){n=0;for(f=b.length;n<f;n++)b[n]!==d&&null!==b[n]&&g.val.push([d,String(b[n])]);g.val.length&&(g.type=3)}else if("[object Object]"===String(Object.prototype.toString.call(b))){for(n in b)i.call(b,n)&&(b[n]!==d&&null!==b[n])&&g.val.push([n,String(b[n])]);g.val.length&&(g.type=2)}else g.type=1,g.val.push([d,String(b)]);return g};k.expand=function(d,b){var g=
(new j(d)).expand(b);return new k(g)};"undefined"!==typeof module&&module.exports?module.exports=j:window.URITemplate=j})();
(function(d,i){function k(b){return b.replace(/([.*+?^=!:${}()|[\]\/\\])/g,"\\$1")}function j(b){var f;d.each(["href","src","action"],function(d,h){return h in b?(f=h,!1):!0});return"input"===b.nodeName.toLowerCase()&&"image"!==b.type?i:f}var f="undefined"!==typeof module&&module.exports?require("./URIjs"):window.URI,s=/^([a-zA-Z]+)\s*([\^\$*]?=|:)\s*(['"]?)(.+)\3|^\s*([a-zA-Z0-9]+)\s*$/,t={},h={"=":function(b,d){return b===d},"^=":function(b,d){return!!(b+"").match(RegExp("^"+k(d),"i"))},"$=":function(b,
d){return!!(b+"").match(RegExp(k(d)+"$","i"))},"*=":function(b,d,f){"directory"==f&&(b+="/");return!!(b+"").match(RegExp(k(d),"i"))},"equals:":function(b,d){return b.equals(d)},"is:":function(b,d){return b.is(d)}};d.each("authority directory domain filename fragment hash host hostname href password path pathname port protocol query scheme search subdomain suffix tld username".split(" "),function(b,f){t[f]=true;d.attrHooks["uri:"+f]={get:function(b){return d(b).uri()[f]()},set:function(b,g){d(b).uri()[f](g);
return g}}});d.fn.uri=function(b){var d=this.first(),h=d.get(0),k=j(h);if(!k)throw Error('Element "'+h.nodeName+'" does not have either property: href, src, action');if(b!==i){var q=d.data("uri");if(q)return q.href(b);b instanceof f||(b=f(b))}else{if(b=d.data("uri"))return b;b=f(d.attr(k))}b._dom_element=h;b._dom_attribute=k;b.normalize();d.data("uri",b);return b};f.prototype.build=function(b){if(this._dom_element)this._string=f.build(this._parts),this._deferred_build=!1,this._dom_element.setAttribute(this._dom_attribute,
this._string),this._dom_element[this._dom_attribute]=this._string;else if(!0===b)this._deferred_build=!0;else if(b===i||this._deferred_build)this._string=f.build(this._parts),this._deferred_build=!1;return this};d.expr.filters.uri=function(b,f,i){if(!j(b)||!i[3])return!1;f=i[3].match(s);if(!f||!f[5]&&":"!==f[2]&&!h[f[2]])return!1;i=d(b).uri();if(f[5])return i.is(f[5]);if(":"===f[2])return b=f[1].toLowerCase()+":",!h[b]?!1:h[b](i,f[4]);b=f[1].toLowerCase();return!t[b]?!1:h[f[2]](i[b](),f[4],b)};var b=
function(b,f){return d(b).uri().href(f).toString()};d.each(["src","href","action","uri"],function(f,h){d.attrHooks[h]={set:b}});d.attrHooks.uri.get=function(b){return d(b).uri()}})(jQuery);
