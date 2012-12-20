package csiro.pidsvc.mappingstore.action;

import csiro.pidsvc.helper.Http;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public class ActionAddHttpHeader extends AbstractAction
{
	public ActionAddHttpHeader(Runner controller, Descriptor descriptor, MappingMatchResults matchResult)
	{
		super(controller, descriptor, matchResult);
	}

	@Override
	public void run()
	{
		try
		{
			String val = getExpandedActionValue();
			_controller.getHttpHeaders().put(_descriptor.Name, val);
			if (isTraceMode())
				trace("Add HTTP header; name: " + _descriptor.Name + "; value: " + val);
		}
		catch (Exception e)
		{
			Http.returnErrorCode(_controller.getResponse(), 500, e);
			if (isTraceMode())
				trace("Set HTTP response status 500; exception: " + e.getCause().getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void trace()
	{
		run();
	}
}
