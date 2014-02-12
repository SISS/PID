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

import java.util.HashMap;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;

import csiro.pidsvc.helper.URI;

/**
 * Abstract base class for URI matching logic.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public abstract class AbstractCondition
{
	public class NameValuePairSubstitutionGroup extends HashMap<String, Matcher>
	{
		private static final long serialVersionUID = -5092377711359150527L;

		public NameValuePairSubstitutionGroup()
		{
			super();
		}

		public NameValuePairSubstitutionGroup(int initialCapacity)
		{
			super(initialCapacity);
		}

		@Override
		public Matcher put(String key, Matcher value)
		{
			// Makes NameValuePairSubstitutionGroup class key case-insensitive.
			return super.put(key.toLowerCase(), value);
		}
	}

	protected final URI					_uri;
	protected final HttpServletRequest	_request;
	protected final Object				_matchAuxiliaryData;

	public final int					ID;
	public final String					Match;
	public Object						AuxiliaryData = null;
	
	public AbstractCondition(URI uri, HttpServletRequest request, int id, String match, Object matchAuxiliaryData)
	{
		this._uri = uri;
		this._request = request;
		this._matchAuxiliaryData = matchAuxiliaryData;
		this.ID = id;
		this.Match = match;
	}
	
	public abstract boolean matches();

	public String toString()
	{
		return "Type=" + getClass().getSimpleName() + "; ID=" + ID + "; Match=" + Match + "; Aux=" + AuxiliaryData + ";";
	}
}
