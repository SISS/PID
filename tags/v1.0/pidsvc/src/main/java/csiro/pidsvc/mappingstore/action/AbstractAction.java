package csiro.pidsvc.mappingstore.action;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import csiro.pidsvc.mappingstore.FormalGrammar;
import csiro.pidsvc.mappingstore.Manager.MappingMatchResults;

public abstract class AbstractAction
{
	protected final Runner				_controller;
	protected final Descriptor			_descriptor;
	protected final MappingMatchResults	_matchResult;	

	public AbstractAction(Runner controller, Descriptor descriptor, MappingMatchResults matchResult)
	{
		_controller = controller;
		_descriptor = descriptor;
		_matchResult = matchResult;
	}

	public abstract void run();
	public abstract void trace();

	public boolean isTraceMode()
	{
		return _controller.isTraceMode();
	}

	protected void trace(String message)
	{
		_controller.trace(message);
	}

	protected String getExpandedActionValue() throws URISyntaxException, UnsupportedEncodingException
	{
		FormalGrammar grammar = new FormalGrammar(_controller.getUri(), _controller.getRequest(), _matchResult.AuxiliaryData, _matchResult.Condition == null ? null : _matchResult.Condition.AuxiliaryData);
		String ret = grammar.parse(_descriptor.Value, true);

		// Trace log messages.
		if (isTraceMode())
		{
			ArrayList<String> log = grammar.getLog();
			boolean isFirst = true;
			for (String it : log)
			{
				trace((isFirst ? "Expand" : "") + "\t" + it);
				isFirst = false;
			}
		}
		return ret;
	}

	public String toString()
	{
		return "Type=" + getClass().getSimpleName() + "; Name=" + _descriptor.Name + "; Value=" + _descriptor.Value + ";";		
	}
}
