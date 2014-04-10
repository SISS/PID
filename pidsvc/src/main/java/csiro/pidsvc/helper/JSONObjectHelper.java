package csiro.pidsvc.helper;

import java.util.HashMap;

import org.json.simple.JSONObject;

public class JSONObjectHelper
{
	public static JSONObject create(Object... args)
	{
		int nPairs = (int)Math.floor(args.length / 2);
		HashMap<String, Object> map = new HashMap<String, Object>(nPairs);
		for (int i = 0; i < nPairs * 2; i += 2)
			map.put(args[i].toString(), args[i + 1]);
		return new JSONObject(map);
	}
}
