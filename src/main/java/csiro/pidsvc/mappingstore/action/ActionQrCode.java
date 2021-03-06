/*
 * CSIRO Open Source Software License Agreement (variation of the BSD / MIT License)
 * 
 * Copyright (c) 2013, Commonwealth Scientific and Industrial Research Organisation (CSIRO)
 * ABN 41 687 119 230.
 * 
 * All rights reserved. This code is licensed under CSIRO Open Source Software
 * License Agreement license, available at the root application directory.
 */

package csiro.pidsvc.mappingstore.action;

import java.io.IOException;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

/**
 * Generate and return QR code for requested URI.
 * 
 * @author Pavel Golodoniuc, CSIRO Earth Science and Resource Engineering
 */
public class ActionQrCode extends AbstractAction
{
	private static Logger _logger = LogManager.getLogger(ActionQrCode.class.getName());

	public ActionQrCode(Runner controller, Descriptor descriptor, MappingMatchResults matchResult)
	{
		super(controller, descriptor, matchResult);
	}

	@Override
	public void run()
	{
		HttpServletResponse response = _controller.getResponse();
		HttpServletRequest request = _controller.getRequest();

		// Set HTTP response parameters.
		response.setHeader("Pragma", "");
		response.setHeader("Cache-Control", "");
		response.setDateHeader("Expires", System.currentTimeMillis() + 604800000L); // 1 week in future.
		response.setHeader("Content-Disposition", "attachment; filename=qrcode.png");
		response.setContentType("image/png");

		// Construct fully-qualified URI with tracing parameter.
		String fullUri = request.getScheme() + "://" + request.getServerName();
		if (request.getServerPort() != 80)
			fullUri += ":" + request.getServerPort();
		fullUri += _controller.getUri().getOriginalUriAsString();
		fullUri += (fullUri.contains("?") ? "&" : "?") + "_pidsvcqrhit";

		// Get QR Code size.
		int size = _controller.getUri().getQrCodeSize();

		// Get a byte matrix for the data.
		BitMatrix matrix = null;
		com.google.zxing.Writer writer = new MultiFormatWriter();
		try
		{
			Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>(1);
			hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
			matrix = writer.encode(fullUri, com.google.zxing.BarcodeFormat.QR_CODE, size, size, hints);
			MatrixToImageWriter.writeToStream(matrix, "PNG", response.getOutputStream());
		}
		catch (com.google.zxing.WriterException e)
		{
			_logger.error("Exception occurred while generating QR code.", e);
		}
		catch (IOException e)
		{
			_logger.error(e);
		}
	}

	@Override
	public void trace()
	{
		trace("Return QR Code for: " + _controller.getUri().getOriginalUriAsString());
	}
}
