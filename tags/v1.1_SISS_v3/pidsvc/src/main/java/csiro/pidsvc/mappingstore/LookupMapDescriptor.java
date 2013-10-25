/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 * 
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 * 
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

package csiro.pidsvc.mappingstore;

/**
 * Lookup map entry descriptor class.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
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
