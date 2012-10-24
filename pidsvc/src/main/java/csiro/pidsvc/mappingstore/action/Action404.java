package csiro.pidsvc.mappingstore.action;

import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public class Action404 extends AbstractAction
{
	public Action404(Runner controller, Descriptor descriptor, MappingMatchResults matchResult)
	{
		super(controller, descriptor, matchResult);
	}

	@Override
	public void run()
	{
		_controller.getResponse().setStatus(404);
	}

	@Override
	public void trace()
	{
		trace("Set HTTP response status: 404");
	}
}
