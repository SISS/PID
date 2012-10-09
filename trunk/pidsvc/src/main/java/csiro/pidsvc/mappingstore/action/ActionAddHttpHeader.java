package csiro.pidsvc.mappingstore.action;

import csiro.pidsvc.mappingstore.action.Runner;
import csiro.pidsvc.helper.Http;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public class ActionAddHttpHeader extends AbstractAction
{
	@Override
	public void run(Runner controller, Descriptor actionDescriptor, MappingMatchResults matchResult)
	{
		try
		{
			controller.getHttpHeaders().put(actionDescriptor.Name, substrituteCaptureParameters(controller.getUri().getPathNoExtension(), actionDescriptor, matchResult));
		}
		catch (Exception e)
		{
			Http.returnErrorCode(controller.getResponse(), 500, e.getCause().getMessage(), e);
			e.printStackTrace();
		}
	}
}
