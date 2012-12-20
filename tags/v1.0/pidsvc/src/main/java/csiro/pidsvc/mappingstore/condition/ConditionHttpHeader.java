package csiro.pidsvc.mappingstore.condition;

import javax.servlet.http.HttpServletRequest;

import csiro.pidsvc.helper.URI;

public class ConditionHttpHeader extends AbstractConditionCollectionSearch
{
	public ConditionHttpHeader(URI uri, HttpServletRequest request, int id, String match, Object matchAuxiliaryData)
	{
		super(uri, request, id, match, matchAuxiliaryData);
	}

	@Override
	public String getValue(String name)
	{
		return _request.getHeader(name);
	}
}
