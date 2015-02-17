package prioritization;

public class VisualProperty {
	private String type;
	private String name;

	public String getType()
	{
		return type;
	}

	public String getName()
	{
		return name;
	}

	public VisualProperty(String type, String name) {
		this.type = type;
		this.name = name;
	}

	public String toString() {
		return this.type + ":" + this.name;
	}

	public int hashCode() {
		return this.type.hashCode() & this.name.hashCode();
	}

	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!(obj instanceof VisualProperty))
			return false;
		VisualProperty vp = (VisualProperty) obj;
		return vp.name.equals(this.name) && vp.type.equals(this.type);
	}
}
