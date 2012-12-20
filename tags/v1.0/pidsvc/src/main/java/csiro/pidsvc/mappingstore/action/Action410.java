package csiro.pidsvc.mappingstore.action;

import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public class Action410 extends AbstractAction
{
	public Action410(Runner controller, Descriptor descriptor, MappingMatchResults matchResult)
	{
		super(controller, descriptor, matchResult);
	}

	@Override
	public void run()
	{
		_controller.getResponse().setStatus(410);
	}

	@Override
	public void trace()
	{
		trace("Set HTTP response status: 410");
	}
}
