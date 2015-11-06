package clustering;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.openqa.selenium.Point;

import util.Util;
import config.Constants;

public class DifferencePixelsClustering
{
	// DBSCAN attributes
	private double eps = 100.0;
	private int minPoints = 3;

	private List<Cluster<LocationWrapper>> clusterResults;
	private String differenceImageFullPath;

	private static final int INTERVAL = 10;
	
	public List<Cluster<LocationWrapper>> getClusterResults()
	{
		return clusterResults;
	}

	public DifferencePixelsClustering(String differenceImageFullPath)
	{
		this.differenceImageFullPath = differenceImageFullPath;

		/*
		 * ImageProcessing ip = new ImageProcessing(); String[]
		 * differenceImagePathAndName =
		 * Util.getPathAndFileNameFromFullPath(differenceImageFullPath); try {
		 * eps = Math.min(ip.getImageSize(differenceImagePathAndName[0],
		 * differenceImagePathAndName[1]).getWidth(),
		 * ip.getImageSize(differenceImagePathAndName[0],
		 * differenceImagePathAndName[1]).getHeight())/2; } catch (IOException
		 * e) { e.printStackTrace(); }
		 */
	}

	private List<Cluster<LocationWrapper>> clustering(List<Point> differencePixelsForClustering)
	{
		DBSCANClusterer<LocationWrapper> clusterer = new DBSCANClusterer<LocationWrapper>(eps, minPoints);

		// optimizing by uniformly selecting difference pixels
		/*List<Point> differencePixelsForClusteringOptimized = new ArrayList<Point>();
		for(int i = 0; i < differencePixelsForClustering.size(); i += INTERVAL)
		{
			differencePixelsForClusteringOptimized.add(differencePixelsForClustering.get(i));
		}*/
		
		List<Location> locations = new ArrayList<Location>();
		for (org.openqa.selenium.Point point : differencePixelsForClustering)
		{
			locations.add(new Location(point.x, point.y));
		}

		List<LocationWrapper> clusterInput = new ArrayList<LocationWrapper>(locations.size());
		for (Location location : locations)
		{
			clusterInput.add(new LocationWrapper(location));
		}

		List<Cluster<LocationWrapper>> clusterResults = clusterer.cluster(clusterInput);
		return clusterResults;
	}

	public Map<Integer, List<Point>> performClustering(List<Point> differencePixelsForClustering)
	{
		clusterResults = clustering(differencePixelsForClustering);
		Map<Integer, List<Point>> clusterDifferencePixelsMap = new HashMap<Integer, List<Point>>();

		for (int i = 0; i < clusterResults.size(); i++)
		{
			List<Point> dp = new ArrayList<Point>();
			for (LocationWrapper locationWrapper : clusterResults.get(i).getPoints())
			{
				dp.add(new Point((int) locationWrapper.getLocation().getX(), (int) locationWrapper.getLocation().getY()));
			}
			clusterDifferencePixelsMap.put(i, dp);
		}
		try
		{
			outputClusteringResults();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return clusterDifferencePixelsMap;
	}

	public void outputClusteringResults() throws IOException
	{
		Set<String> el = new HashSet<String>();
		String[] differenceImagePathAndName = Util.getPathAndFileNameFromFullPath(differenceImageFullPath);
		FileUtils.copyFile(new File(differenceImageFullPath), new File(differenceImagePathAndName[0] + File.separatorChar + Constants.CLUSTERING_DIFFERENCE_PIXELS_IMAGENAME));
		BufferedImage bi = ImageIO.read(new File(differenceImagePathAndName[0] + File.separatorChar + Constants.CLUSTERING_DIFFERENCE_PIXELS_IMAGENAME));
		Color myBlack = new Color(0, 0, 0); // Color black
		int rgb1 = myBlack.getRGB();
		for (int r = 0; r < bi.getWidth(); r++)
		{
			for (int c = 0; c < bi.getHeight(); c++)
			{
				bi.setRGB(r, c, rgb1);
			}
		}

		Random rand = new Random();
		for (int i = 0; i < clusterResults.size(); i++)
		{
			// System.out.println("Cluster " + i);
			// Java 'Color' class takes 3 floats, from 0 to 1.
			/*float r = rand.nextFloat();
			float g = rand.nextFloat();
			float b = rand.nextFloat();
			while (r == 0 && g == 0 && b == 0)
			{
				r = rand.nextFloat();
				g = rand.nextFloat();
				b = rand.nextFloat();
			}*/
			
			float r = 1;
			float g = 1;
			float b = 1;
			Color color = new Color(r, g, b);
			int rgb = color.getRGB();
			int firstX = (int)clusterResults.get(i).getPoints().get(0).getLocation().getX();
			int firstY = (int)clusterResults.get(i).getPoints().get(0).getLocation().getY();
			for (LocationWrapper locationWrapper : clusterResults.get(i).getPoints())
			{
				// System.out.println(locationWrapper.getLocation());
				int x = (int) locationWrapper.getLocation().getX();
				int y = (int) locationWrapper.getLocation().getY();

				// write filtered difference pixels
				bi.setRGB(x, y, rgb);
			}
			/*for (String s : el)
			{
				// System.out.println(s);
			}*/
			el = new HashSet<String>();
			// System.out.println();
			
			 Graphics graphics = bi.getGraphics();
			 graphics.setColor(Color.WHITE);
			 graphics.setFont(new Font("Arial Black", Font.BOLD, 20));
			 //graphics.drawString("Cluster " + i + ": " + Util.getHexFromDecimal(rgb), firstX, firstY);
			 graphics.drawString("Cluster " + (i+1), firstX, firstY-20);
			 
			 // find minX, minY and maxX, maxY
			 int minX = Integer.MAX_VALUE;
			 int minY = Integer.MAX_VALUE;
			 int maxX = Integer.MIN_VALUE;
			 int maxY = Integer.MIN_VALUE;
			 for(LocationWrapper lw : clusterResults.get(i).getPoints())
			 {
				 int x = (int) lw.getLocation().getX();
				 if(x < minX)
				 {
					 minX = x;
				 }
				 if(x > maxX)
				 {
					 maxX = x;
				 }
				 
				 int y = (int) lw.getLocation().getY();
				 if(y < minY)
				 {
					 minY = y;
				 }
				 if(y > maxY)
				 {
					 maxY = y;
				 }
			 }
			 Graphics2D g2D = (Graphics2D) graphics;
			 g2D.setStroke(new BasicStroke(3F));
			 g2D.drawOval(minX, minY, (maxX-minX), (maxY-minY));
		}
		ImageIO.write(bi, "png", new File(differenceImagePathAndName[0] + File.separatorChar + Constants.CLUSTERING_DIFFERENCE_PIXELS_IMAGENAME));
	}

	public static void main(String[] args)
	{
		// we have a list of our locations we want to cluster. create a
		List<Location> locations = new ArrayList<Location>();
		locations.add(new Location(150, 981));
		locations.add(new Location(136, 0));
		locations.add(new Location(158, 88));
		locations.add(new Location(330, 60));
		locations.add(new Location(0, 1001));
		locations.add(new Location(150, 0));
		locations.add(new Location(0, 0));
		locations.add(new Location(0, 0));
		locations.add(new Location(0, 0));
		locations.add(new Location(446, 88));
		locations.add(new Location(562, 88));
		locations.add(new Location(256, 88));
		locations.add(new Location(678, 88));
		locations.add(new Location(794, 88));
		locations.add(new Location(0, 1028));
		locations.add(new Location(136, 0));
		locations.add(new Location(150, 0));
		locations.add(new Location(150, 88));
		locations.add(new Location(150, 1028));
		locations.add(new Location(150, 88));
		locations.add(new Location(150, 88));
		locations.add(new Location(150, 88));
		locations.add(new Location(150, 88));
		locations.add(new Location(150, 88));
		locations.add(new Location(150, 88));
		locations.add(new Location(136, 103));
		locations.add(new Location(150, 0));
		List<LocationWrapper> clusterInput = new ArrayList<LocationWrapper>(locations.size());
		for (Location location : locations)
			clusterInput.add(new LocationWrapper(location));

		// initialize a new clustering algorithm.
		// we use KMeans++ with 10 clusters and 10000 iterations maximum.
		// we did not specify a distance measure; the default (euclidean
		// distance) is used.
		// org.apache.commons.math3.ml.clustering.FuzzyKMeansClusterer<LocationWrapper>
		// clusterer = new
		// org.apache.commons.math3.ml.clustering.FuzzyKMeansClusterer<LocationWrapper>(2,
		// 2);
		// KMeansPlusPlusClusterer<LocationWrapper> clusterer = new
		// KMeansPlusPlusClusterer<LocationWrapper>(2, 10);

		DBSCANClusterer<LocationWrapper> clusterer = new DBSCANClusterer<LocationWrapper>(1200.0, 5);
		List<Cluster<LocationWrapper>> clusterResults = clusterer.cluster(clusterInput);
		// List<CentroidCluster<LocationWrapper>> clusterResults =
		// clusterer.cluster(clusterInput);

		// output the clusters
		System.out.println("clusterResults.size() = " + clusterResults.size());
		for (int i = 0; i < clusterResults.size(); i++)
		{
			System.out.println("Cluster " + i);
			for (LocationWrapper locationWrapper : clusterResults.get(i).getPoints())
				System.out.println(locationWrapper.getLocation());
			System.out.println();
		}
	}
}