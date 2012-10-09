package csiro.pidsvc.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import csiro.pidsvc.helper.Http;
import csiro.pidsvc.mappingstore.Manager;

/**
 * Servlet implementation class controller
 */
public class controller extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public controller()
	{
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String cmd = request.getParameter("cmd");
		if (cmd == null || cmd.isEmpty())
			return;
		
		Manager mgr = null;
		try
		{
			// Create mapping store manager object.
			mgr = new Manager();

			if (cmd.equalsIgnoreCase("create_mapping"))
				mgr.createMapping(request.getInputStream());
			else if (cmd.equalsIgnoreCase("delete_mapping"))
				mgr.deleteMapping(request.getParameter("mapping_path"));
		}
		catch (Exception e)
		{
			Http.returnErrorCode(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage(), e);
			e.printStackTrace();
			return;
		}
		finally
		{
			if (mgr != null)
				mgr.close();
		}
	}
}
