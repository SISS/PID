package csiro.pidsvc.mappingstore.condition;

import javax.servlet.http.HttpServletRequest;

import csiro.pidsvc.helper.URI;

public class ConditionQueryString extends AbstractConditionCollectionSearch
{
	public ConditionQueryString(URI uri, HttpServletRequest request, int id, String match)
	{
		super(uri, request, id, match);
	}

	@Override
	public String getValue(String name)
	{
		return _uri.getQuerystringParameter(name);
	}
}
