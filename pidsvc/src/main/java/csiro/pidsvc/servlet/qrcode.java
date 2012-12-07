package csiro.pidsvc.servlet;

import java.io.IOException;
import java.util.Hashtable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import csiro.pidsvc.helper.Literals;

/**
 * Servlet implementation class qrcode
 */
public class qrcode extends HttpServlet
{
	private static final long serialVersionUID = 1469982996689969299L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public qrcode()
	{
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		response.setDateHeader("Expires", 0);
		response.addHeader("Cache-Control", "no-cache,no-store,private,must-revalidate,max-stale=0,post-check=0,pre-check=0");
		response.addHeader("Content-Disposition", "attachment; filename=qrcode.png");
		response.setContentType("image/png");

		String uri = request.getParameter("uri");
		if (uri == null || uri.isEmpty())
			return;

		int size = Literals.toInt(request.getParameter("size"), 100);
		if (size > 1200)
			size = 1200;

		// Get a byte matrix for the data.
		BitMatrix matrix = null;
		com.google.zxing.Writer writer = new MultiFormatWriter();
		try
		{
			Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>(1);
			hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
			matrix = writer.encode(uri, com.google.zxing.BarcodeFormat.QR_CODE, size, size, hints);
			MatrixToImageWriter.writeToStream(matrix, "PNG", response.getOutputStream());
		}
		catch (com.google.zxing.WriterException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}
}
