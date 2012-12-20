package csiro.pidsvc.mappingstore.action;

import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public class ActionRemoveHttpHeader extends AbstractAction
{
	public ActionRemoveHttpHeader(Runner controller, Descriptor descriptor, MappingMatchResults matchResult)
	{
		super(controller, descriptor, matchResult);
	}

	@Override
	public void run()
	{
		_controller.getHttpHeaders().remove(_descriptor.Name);
	}

	@Override
	public void trace()
	{
		run();
		trace("Remove HTTP header; name: " + _descriptor.Name);
	}
}
