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

import java.io.InputStream;
import java.io.Reader;

import org.w3c.dom.ls.LSInput;

/**
 * XML Schema resolver class for schema validation.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class XsdSchemaResolver implements LSInput
{
	protected String _systemId;

	public XsdSchemaResolver(String type, String namespaceURI, String publicId, String systemId, String baseURI)
	{
		_systemId = systemId;
	}

	@Override
	public Reader getCharacterStream()
	{
		return null;
	}

	@Override
	public void setCharacterStream(Reader characterStream)
	{
	}

	@Override
	public InputStream getByteStream()
	{
		return getClass().getResourceAsStream("xsd/" + _systemId);
	}

	@Override
	public void setByteStream(InputStream byteStream)
	{
	}

	@Override
	public String getStringData()
	{
		return null;
	}

	@Override
	public void setStringData(String stringData)
	{
	}

	@Override
	public String getSystemId()
	{
		return _systemId;
	}

	@Override
	public void setSystemId(String systemId)
	{
		_systemId = systemId;
	}

	@Override
	public String getPublicId()
	{
		return null;
	}

	@Override
	public void setPublicId(String publicId)
	{
	}

	@Override
	public String getBaseURI()
	{
		return null;
	}

	@Override
	public void setBaseURI(String baseURI)
	{
	}

	@Override
	public String getEncoding()
	{
		return "UTF-8";
	}

	@Override
	public void setEncoding(String encoding)
	{
	}

	@Override
	public boolean getCertifiedText()
	{
		return false;
	}

	@Override
	public void setCertifiedText(boolean certifiedText)
	{
	}
}
