package csiro.pidsvc.mappingstore.action;

import csiro.pidsvc.helper.Http;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public class Action303 extends AbstractAction
{
	public Action303(Runner controller, Descriptor descriptor, MappingMatchResults matchResult)
	{
		super(controller, descriptor, matchResult);
	}

	@Override
	public void run()
	{
		try
		{
			_controller.getResponse().setStatus(303);
			_controller.getResponse().addHeader("Location", getExpandedActionValue());
		}
		catch (Exception e)
		{
			Http.returnErrorCode(_controller.getResponse(), 500, e);
			e.printStackTrace();
		}
	}

	@Override
	public void trace()
	{
		try
		{
			trace("Set HTTP response status: 303; location: " + getExpandedActionValue());
		}
		catch (Exception e)
		{
			trace("Set HTTP response status: 500; exception: " + e.getCause().getMessage());
			e.printStackTrace();
		}
	}
}
