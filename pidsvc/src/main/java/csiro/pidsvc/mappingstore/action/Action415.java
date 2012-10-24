package csiro.pidsvc.mappingstore.action;

import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public class Action415 extends AbstractAction
{
	public Action415(Runner controller, Descriptor descriptor, MappingMatchResults matchResult)
	{
		super(controller, descriptor, matchResult);
	}

	@Override
	public void run()
	{
		_controller.getResponse().setStatus(415);
	}

	@Override
	public void trace()
	{
		trace("Set HTTP response status: 415");
	}
}
