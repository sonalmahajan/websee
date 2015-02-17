package websee;

import java.util.Set;

public class TextRegion extends SpecialRegion
{
	private Set<String> styleProperties;

	public Set<String> getStyleProperties()
	{
		return styleProperties;
	}

	public void setStyleProperties(Set<String> styleProperties)
	{
		this.styleProperties = styleProperties;
	}

	@Override
	public String toString()
	{
		return "TextRegion [styleProperties=" + styleProperties + "]";
	}
}
