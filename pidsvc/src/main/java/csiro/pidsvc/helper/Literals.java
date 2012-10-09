package csiro.pidsvc.helper;

public class Literals
{
	public static int toInt(String value, int defaultValue)
	{
		try
		{
			return Integer.parseInt(value);
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}

	public static boolean toBoolean(String value)
	{
		return value.equalsIgnoreCase("true") || toInt(value, 0) != 0;
	}
}
