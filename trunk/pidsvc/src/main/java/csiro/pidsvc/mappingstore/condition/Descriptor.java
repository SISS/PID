package csiro.pidsvc.mappingstore.condition;

public class Descriptor
{
	public final int ID;
	public final String Type;
	public final String Match;
	
	public Descriptor(int id, String type, String match)
	{
		this.ID = id;
		this.Type = type;
		this.Match = match;
	}
}
