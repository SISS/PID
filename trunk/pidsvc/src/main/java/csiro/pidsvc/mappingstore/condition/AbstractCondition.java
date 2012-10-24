package csiro.pidsvc.mappingstore.condition;

import java.util.Hashtable;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;

import csiro.pidsvc.helper.URI;

public abstract class AbstractCondition
{
	public class NameValuePairSubstitutionGroup extends Hashtable<String, Matcher>
	{
		private static final long serialVersionUID = -5092377711359150527L;

		public NameValuePairSubstitutionGroup()
		{
			super();
		}

		public NameValuePairSubstitutionGroup(int initialCapacity)
		{
			super(initialCapacity);
		}
	}

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

	public String toString()
	{
		return "Type=" + getClass().getSimpleName() + "; ID=" + ID + "; Match=" + Match + "; Aux=" + AuxiliaryData + ";";
	}
}
