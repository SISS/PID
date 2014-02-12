/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 * 
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 * 
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

package csiro.pidsvc.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Basic Stream operations.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class Stream
{
	public static String readInputStream(InputStream inputStream) throws IOException
	{
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;
		try
		{
			if (inputStream != null)
			{
				bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
				char[] charBuffer = new char[128];
				int bytesRead = -1;
				while ((bytesRead = bufferedReader.read(charBuffer)) > 0)
				{
					stringBuilder.append(charBuffer, 0, bytesRead);
				}
			}
			else
				stringBuilder.append("");
		}
		finally
		{
			if (bufferedReader != null)
				bufferedReader.close();
		}
		return stringBuilder.toString();
	}
}
