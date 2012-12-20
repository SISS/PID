package csiro.pidsvc.mappingstore.action;

import java.io.IOException;
import java.util.Hashtable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public class ActionQrCode extends AbstractAction
{
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
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void trace()
	{
		trace("Return QR Code for: " + _controller.getUri().getOriginalUriAsString());
	}
}
