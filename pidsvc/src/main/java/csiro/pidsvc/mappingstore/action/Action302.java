package csiro.pidsvc.mappingstore.action;

import csiro.pidsvc.helper.Http;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public class Action302 extends AbstractAction
{
	public Action302(Runner controller, Descriptor descriptor, MappingMatchResults matchResult)
	{
		super(controller, descriptor, matchResult);
	}

	@Override
	public void run()
	{
		try
		{
			_controller.getResponse().setStatus(302);
			_controller.getResponse().addHeader("Location", substrituteCaptureParameters(_controller.getUri().getPathNoExtension()));
		}
		catch (Exception e)
		{
			Http.returnErrorCode(_controller.getResponse(), 500, e.getCause().getMessage(), e);
			e.printStackTrace();
		}
	}

	@Override
	public void trace()
	{
		try
		{
			trace("Set HTTP response status: 302; location: " + substrituteCaptureParameters(_controller.getUri().getPathNoExtension()));
		}
		catch (Exception e)
		{
			trace("Set HTTP response status: 500; exception: " + e.getCause().getMessage());
			e.printStackTrace();
		}
	}
}
