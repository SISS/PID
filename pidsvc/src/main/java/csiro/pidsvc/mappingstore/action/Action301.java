package csiro.pidsvc.mappingstore.action;

import csiro.pidsvc.mappingstore.action.Runner;
import csiro.pidsvc.helper.Http;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public class Action301 extends AbstractAction
{
	@Override
	public void run(Runner controller, Descriptor actionDescriptor, MappingMatchResults matchResult)
	{
		try
		{
			controller.getResponse().setStatus(301);
			controller.getResponse().addHeader("Location", substrituteCaptureParameters(controller.getUri().getPathNoExtension(), actionDescriptor, matchResult));
		}
		catch (Exception e)
		{
			Http.returnErrorCode(controller.getResponse(), 500, e.getCause().getMessage(), e);
			e.printStackTrace();
		}
	}
}
