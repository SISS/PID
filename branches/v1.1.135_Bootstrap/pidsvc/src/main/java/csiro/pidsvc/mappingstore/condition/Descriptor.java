/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 * 
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 * 
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

package csiro.pidsvc.mappingstore.condition;

/**
 * Condition type descriptor.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class Descriptor
{
	public final int ID;
	public final String Type;
	public final String Match;
	public final String Description;
	
	public Descriptor(int id, String type, String match, String description)
	{
		this.ID = id;
		this.Type = type;
		this.Match = match;
		this.Description = description;
	}
}
