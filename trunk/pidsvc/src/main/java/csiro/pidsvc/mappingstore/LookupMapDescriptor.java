package csiro.pidsvc.mappingstore;

public class LookupMapDescriptor
{
	private final String _type;
	private final String _defaultBehaviourType;
	private final String _defaultBehaviourValue;
	
	public LookupMapDescriptor(String type, String defaultBehaviourType, String defaultBehaviourValue)
	{
		_type = type;
		_defaultBehaviourType = defaultBehaviourType;
		_defaultBehaviourValue = defaultBehaviourValue;
	}

	public String getType()
	{
		return _type;
	}

	public boolean isStatic()
	{
		return _type.equalsIgnoreCase("Static");
	}
	
	public boolean isHttpResolver()
	{
		return _type.equalsIgnoreCase("HttpResolver");
	}
	
	public String getDefaultBehaviourType()
	{
		return _defaultBehaviourType;
	}

	public String getDefaultBehaviourValue()
	{
		return _defaultBehaviourValue;
	}

	public String getDefaultValue(String key)
	{
		return _defaultBehaviourType.equalsIgnoreCase("Constant") ? _defaultBehaviourValue : key;
	}
}
