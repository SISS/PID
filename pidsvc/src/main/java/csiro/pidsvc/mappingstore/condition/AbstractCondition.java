package csiro.pidsvc.mappingstore.condition;

import javax.servlet.http.HttpServletRequest;

import csiro.pidsvc.helper.URI;

public abstract class AbstractCondition
{
	protected final URI _uri;
	protected final HttpServletRequest _request;
	public final int ID;
	public final String Match;
	public Object AuxiliaryData;
	
	public AbstractCondition(URI uri, HttpServletRequest request, int id, String match)
	{
		this._uri = uri;
		this._request = request;
		this.ID = id;
		this.Match = match;
	}
	
	public abstract boolean matches();
}
