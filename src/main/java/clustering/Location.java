package clustering;

public class Location
{
	private double x;
	private double y;

	public Location(double x, double y)
	{
		super();
		this.x = x;
		this.y = y;
	}

	/**
	 * @return the x
	 */
	public double getX()
	{
		return x;
	}

	/**
	 * @param x
	 *            the x to set
	 */
	public void setX(double x)
	{
		this.x = x;
	}

	/**
	 * @return the y
	 */
	public double getY()
	{
		return y;
	}

	/**
	 * @param y
	 *            the y to set
	 */
	public void setY(double y)
	{
		this.y = y;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "Location [x=" + x + ", y=" + y + "]";
	}
}