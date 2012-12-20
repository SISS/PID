package csiro.pidsvc.mappingstore.action;

import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public class ActionClearHttpHeaders extends AbstractAction
{
	public ActionClearHttpHeaders(Runner controller, Descriptor descriptor, MappingMatchResults matchResult)
	{
		super(controller, descriptor, matchResult);
	}

	@Override
	public void run()
	{
		_controller.getHttpHeaders().clear();
	}

	@Override
	public void trace()
	{
		run();
		trace("Clear HTTP headers");
	}
}
