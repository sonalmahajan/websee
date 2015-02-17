package clustering;

import org.apache.commons.math3.ml.clustering.Clusterable;

public class LocationWrapper implements Clusterable
{
	private double[] points;
	private Location location;

	public LocationWrapper(Location location)
	{
		this.location = location;
		this.points = new double[] { location.getX(), location.getY() };
	}

	public Location getLocation()
	{
		return location;
	}

	public double[] getPoint()
	{
		return points;
	}
}
