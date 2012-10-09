package csiro.pidsvc.mappingstore.action;

import csiro.pidsvc.mappingstore.action.Runner;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public class ActionRemoveHttpHeader extends AbstractAction
{
	@Override
	public void run(Runner controller, Descriptor actionDescriptor, MappingMatchResults matchResult)
	{
		controller.getHttpHeaders().remove(actionDescriptor.Name);
	}
}
