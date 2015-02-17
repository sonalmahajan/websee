package websee;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import javax.imageio.ImageIO;

import org.openqa.selenium.Point;

import util.Util;
import websee.pDiff.PerceptualImageDifferencing;
import config.Constants;

public class Detection
{
	// perceptual image differencing constants
	// parameter values for getting pixel-perfect match like pixel-to-pixel difference
 	private double COLOR_FACTOR = 1.0;
	private double FOV = 89.9;
	private double GAMMA = 4.2;
	private double LUMINANCE = 800.0;
	private boolean LUMINANCE_ONLY = false;
	private int THRESHOLD_PIXELS = 100;
	private int DOWNSAMPLE = 0;
	private int PERCENTAGE_OF_TOTAL_IMAGE_SIZE = 0;

/*	// default values from pdiff page by Hector Yee
 	private double COLOR_FACTOR = 1.0;
	private double FOV = 27.0;
	private double GAMMA = 2.2;
	private double LUMINANCE = 100.0;
	private boolean LUMINANCE_ONLY = false;
	private int THRESHOLD_PIXELS = 100;
	private int DOWNSAMPLE = 0;
	private int PERCENTAGE_OF_TOTAL_IMAGE_SIZE = 0;
*/
	
/*	private double COLOR_FACTOR = 0.0;
	private double FOV = 27;
	private double GAMMA = 4.2;
	private double LUMINANCE = 20.0;
	private boolean LUMINANCE_ONLY = false;
	private int THRESHOLD_PIXELS = 100;
	private int DOWNSAMPLE = 0;
	private int PERCENTAGE_OF_TOTAL_IMAGE_SIZE = 0;
*/	
	private String differenceColor = "#ff0000";

	private static final ForkJoinPool pool = new ForkJoinPool();

	// pixel-by-pixel comparison
	public List<Point> detectionByPixelByPixelImageDifferencing(String oracleFullPath, String testPageScreenshotFullPath)
	{
		ImageProcessing ip = new ImageProcessing();
		String[] oracleImagePathAndName = Util.getPathAndFileNameFromFullPath(oracleFullPath);
		String[] testImagePathAndName = Util.getPathAndFileNameFromFullPath(testPageScreenshotFullPath);
		List<Point> lDifferencePixels = ip.compareImages(oracleImagePathAndName[0], 
				oracleImagePathAndName[1], testImagePathAndName[0], testImagePathAndName[1]);
		return lDifferencePixels;
	}

	// perceptual image differencing
	public List<Point> detectionByPerceptualImageDifferencing(String oracleFullPath, String testPageScreenshotFullPath, String differenceImageFullPath) throws IOException
	{
		String[] testScreenshotPathAndName = Util.getPathAndFileNameFromFullPath(testPageScreenshotFullPath);

		ImageProcessing ip = new ImageProcessing();
		
		THRESHOLD_PIXELS = (int) (ip.getImageSize(testScreenshotPathAndName[0], testScreenshotPathAndName[1]).width * 
				ip.getImageSize(testScreenshotPathAndName[0], testScreenshotPathAndName[1]).height * ((double)PERCENTAGE_OF_TOTAL_IMAGE_SIZE/100));
		
		List<Point> differencePixels = new ArrayList<Point>();
		
		BufferedImage imgA = ImageIO.read(new File(oracleFullPath));
		BufferedImage imgB = ImageIO.read(new File(testPageScreenshotFullPath));

		if (DOWNSAMPLE > 0)
		{
			double scale = 1.0 / (1 << DOWNSAMPLE);
			imgA = resize(imgA, scale);
			imgB = resize(imgB, scale);
		}

		int width = Math.max(imgA.getWidth(), imgB.getWidth());
		int height = Math.max(imgA.getHeight(), imgB.getHeight());
		
		BufferedImage imgDiff = (differenceImageFullPath != null) ? new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) : null;

		PerceptualImageDifferencing.Builder builder = new PerceptualImageDifferencing.Builder();
		builder.setColorFactor(COLOR_FACTOR);
		builder.setFieldOfView(FOV);
		builder.setGamma(GAMMA);
		builder.setLuminance(LUMINANCE);
		builder.setLuminanceOnly(LUMINANCE_ONLY);
		builder.setThresholdPixels(THRESHOLD_PIXELS);

		PerceptualImageDifferencing pd = builder.build();
		boolean passed = pd.compare(pool, imgA, imgB, imgDiff);
		pd.dump();

		for(int r = 0; r < imgDiff.getWidth(); r++)
        {
        	for(int c = 0; c < imgDiff.getHeight(); c++)
        	{
        		if(Util.getHexFromDecimal(imgDiff.getRGB(r, c)).equalsIgnoreCase(differenceColor))
        		{
        			differencePixels.add(new Point(r, c));
        		}
        	}
        }
		
		// Always output image difference if requested.
		if (differenceImageFullPath != null)
		{
			int extIndex = differenceImageFullPath.lastIndexOf('.');
			String formatName = (extIndex != -1) ? differenceImageFullPath.substring(extIndex + 1) : Constants.SCREENSHOT_FILE_EXTENSION;
			ImageIO.write(imgDiff, formatName, new File(differenceImageFullPath));
		}
		
		System.out.println("Difference pixels = " + differencePixels.size());
		
		//TODO: deal with passed maybe to segregate as warnings and errors.
		if(passed)
		{
			// set the number of difference pixels as zero, implying there is no perceptual difference
			System.out.println("Less than threshold, hence oracle and test web page are perceptually same");
			differencePixels = new ArrayList<Point>();
		}
		
		return differencePixels;
	}

	private static BufferedImage resize(BufferedImage src, double scale)
	{
		AffineTransform at = new AffineTransform();
		at.scale(scale, scale);
		AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		return scaleOp.filter(src, null);
	}

	public static void main(String[] args) throws IOException
	{
		Detection d = new Detection();
		String path = "/Users/sonal/USC/visual_checking/evaluation/real_data/crawller_designs/test3";
		d.detectionByPerceptualImageDifferencing(path + "/oracle.png", path + "/test.png", path + "/diff1.png");
	}
}
