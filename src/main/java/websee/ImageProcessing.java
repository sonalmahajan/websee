package websee;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import util.Util;
import config.Constants;

public class ImageProcessing
{
	private Logger logger = Logger.getLogger(this.getClass());
	private String fileEnclosingCharacter;
	
	public ImageProcessing()
	{
		if (System.getProperty("os.name").startsWith("Windows"))
		{
			this.fileEnclosingCharacter = Constants.WINDOWS_FILE_ENCLOSING_CHARACTER_IMAGE_PROCESSING;
		}
		else
		{
			this.fileEnclosingCharacter = "";
		}
		
		// load opencv java library
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	
	public void compareImages(String referenceImagePath, String referenceImageName, String comparisonImagePath, String comparisonImageName, String differenceText, String function, boolean doNotOverwrite) throws IOException
	{
		String img1 = referenceImagePath + File.separatorChar + referenceImageName;
		String img2 = comparisonImagePath + File.separatorChar + comparisonImageName;
		String diff = comparisonImagePath + File.separatorChar + differenceText;

		if(doNotOverwrite && new File(diff).exists() && new File(diff).length() > 0)
		{
			return;
		}
		
		// for imagemagick composite function
		BufferedImage bimg = ImageIO.read(new File(img1));
		int img1Width = bimg.getWidth();
		int img1Height = bimg.getHeight();
		bimg = ImageIO.read(new File(img2));
		int img2Width = bimg.getWidth();
		int img2Height = bimg.getHeight();
		
		//swap if img2 size is less than img1 size
		if(img1Width*img1Height > img2Width*img2Height)
		{
			String temp = img2;
			img2 = img1;
			img1 = temp;
		}
		
		Runtime runtime = Runtime.getRuntime();
		Process p = runtime.exec("composite -compose " + function + " " + fileEnclosingCharacter + img1  + fileEnclosingCharacter + " " + 
						fileEnclosingCharacter + img2 + fileEnclosingCharacter + " " + 
						fileEnclosingCharacter + diff + fileEnclosingCharacter);
		try
		{
			p.waitFor();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		p.destroy();
		p = runtime.exec("composite -compose " + function + " "  + fileEnclosingCharacter + img1  + fileEnclosingCharacter + " " + 
						fileEnclosingCharacter + img2 + fileEnclosingCharacter + " " + 
						fileEnclosingCharacter + comparisonImagePath + File.separatorChar + Util.getFileNameAndExtension(differenceText)[0] + Constants.SCREENSHOT_FILE_EXTENSION + fileEnclosingCharacter);
		try
		{
			p.waitFor();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		p.destroy();
	}
	
	public List<org.openqa.selenium.Point> compareImages(String referenceImagePath, String referenceImageName, String comparisonImagePath, String comparisonImageName)
	{
		List<org.openqa.selenium.Point> differencePixels = new ArrayList<org.openqa.selenium.Point>();
		
		String img1 = referenceImagePath + File.separatorChar + referenceImageName;
		String img2 = comparisonImagePath + File.separatorChar + comparisonImageName;

		Mat img1Mat = Highgui.imread(img1);
		Mat img2Mat = Highgui.imread(img2);
		
		Mat outDiff = new Mat(Math.max(img1Mat.rows(), img2Mat.rows()), Math.max(img1Mat.cols(), img2Mat.cols()), img1Mat.type());
		
		for(int col = 0; col < img1Mat.cols() || col < img2Mat.cols(); col += Constants.IMAGE_COMPARISON_PIXEL_SUB_SAMPLING_RATE)
		{
			for(int row = 0; row < img1Mat.rows() || row < img2Mat.rows(); row += Constants.IMAGE_COMPARISON_PIXEL_SUB_SAMPLING_RATE)
			{
				double[] img1RGB = img1Mat.get(row, col);
				double[] img2RGB = img2Mat.get(row, col);
				
				// point is present in one of the images but not both (different sized images)
				if(img1RGB == null && img2RGB != null || img2RGB == null && img1RGB != null)
				{
					differencePixels.add(new org.openqa.selenium.Point(col, row));
					if(img1RGB != null)
						outDiff.put(row, col, img1RGB);
					else
						outDiff.put(row, col, img2RGB);
				}
				else if(img1RGB != null & img2RGB != null )
				{
					boolean isSame = true;
					/*String img1Hex = Util.getHexFromRGB((int)img1RGB[0], (int)img1RGB[1], (int)img1RGB[2]);
					String img2Hex = Util.getHexFromRGB((int)img2RGB[0], (int)img2RGB[1], (int)img2RGB[2]);
					if(!img1Hex.equalsIgnoreCase(img2Hex))
					{
						differencePixels.add(new org.openqa.selenium.Point(col, row));
					}*/
					for(int i = 0; i < img1RGB.length; i++)
					{
						if(img1RGB[i] != img2RGB[i])
						{
							isSame = false;
							break;
						}
					}
					if(!isSame)
					{
						differencePixels.add(new org.openqa.selenium.Point(col, row));
						outDiff.put(row, col, img1RGB);
					}
				}
			}
		}
		Highgui.imwrite(comparisonImagePath + File.separatorChar + "diff" + (Util.getNumbersFromString(comparisonImageName).size() > 0?Util.getNumbersFromString(comparisonImageName).get(0):"") + ".png", outDiff);
		return differencePixels;
	}
	
	public String cropImage(int x, int y, int width, int height, String imageFullPath) throws IOException
	{
		String[] image = Util.getPathAndFileNameFromFullPath(imageFullPath);
		String croppedImagesDirectoryString = image[0] + File.separatorChar + Constants.DIRECTORY_TO_STORE_CROP_IMAGES;
		File croppedImagesDirectory = new File(croppedImagesDirectoryString);
		if(!croppedImagesDirectory.exists())
		{
			croppedImagesDirectory.mkdir();
		}
		
		String croppedImage = croppedImagesDirectoryString + File.separatorChar + Util.getNextAvailableFileName(croppedImagesDirectoryString, Constants.CROPPED_IMAGE_NAME);
		
		Mat uncropped = Highgui.imread(imageFullPath);
		
		// check if the dimensions are out of the uncropped image size
		if(x < 0)
			x = 0;
		if(y < 0)
			y = 0;
		if((x + width) > uncropped.width())
			width = uncropped.width() - x;
		if((y + height) > uncropped.height())
			height = uncropped.height() - y;
		
		if(width > 0 && height > 0)
		{
			Rect roi = new Rect(x, y, width, height);
			Mat cropped = new Mat(uncropped, roi);
			Highgui.imwrite(croppedImage, cropped);
		}
		return croppedImage;
	}
	
	public Rectangle findSubImage(String originalImageFullPath, String templateImageFileNameFullPath) throws IOException
	{
		// source image
		Mat img = Highgui.imread(originalImageFullPath, Highgui.CV_LOAD_IMAGE_ANYDEPTH);
		// template image
		Mat templ = Highgui.imread(templateImageFileNameFullPath, Highgui.CV_LOAD_IMAGE_ANYDEPTH);
		
		Mat img_display = new Mat();
		img.copyTo(img_display);
		
		Mat result = new Mat();
		int match_method = Imgproc.TM_SQDIFF;
		
		try
		{
			Imgproc.matchTemplate(img, templ, result, match_method);
		}
		catch(CvException e)
		{
			return null;
		}
		
		MinMaxLocResult minMaxResult = Core.minMaxLoc(result);
		Point minLoc = minMaxResult.minLoc; 
		Point maxLoc = minMaxResult.maxLoc;

		Point matchLoc;
		if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED)
		{
			matchLoc = minLoc;
		}
		else
		{
			matchLoc = maxLoc;
		}
		
		// check if the sub-image is same as the template
		String originalCroppedImage = cropImage((int)matchLoc.x, (int)matchLoc.y, templ.cols(), templ.rows(), originalImageFullPath);
		double histResult = compareImagesByHistogram(templateImageFileNameFullPath, originalCroppedImage);
		if(histResult == 1)
		{
			Rectangle rect = new Rectangle((int)matchLoc.x, (int)matchLoc.y, templ.cols(), templ.rows());
			return rect;
		}
		return null;
	}
	
	public double compareImagesByHistogram(String img1FullPath, String img2FullPath)
	{
		List<Mat> images = new ArrayList<Mat>();
		images.add(Highgui.imread(img1FullPath));
		Mat h1 = new Mat();
		
		Imgproc.calcHist(images, new MatOfInt(0, 1), new Mat(), h1, new MatOfInt(256, 256), new MatOfFloat(0.0f, 255.0f, 0.0f, 255.0f));
		images = new ArrayList<Mat>();
		images.add(Highgui.imread(img2FullPath));
		Mat h2 = new Mat();
		Imgproc.calcHist(images, new MatOfInt(0, 1), new Mat(), h2, new MatOfInt(256, 256), new MatOfFloat(0.0f, 255.0f, 0.0f, 255.0f));
		
        double result = Imgproc.compareHist(h1, h2, Imgproc.CV_COMP_CORREL);
        
        return result;
	}
	
	public Rectangle getImageSize(String imagePath, String imageName) throws IOException
	{
		BufferedImage bimg = ImageIO.read(new File(imagePath + File.separatorChar + imageName));
		return new Rectangle(0, 0, bimg.getWidth(), bimg.getHeight());
	}
	
	public boolean isImageBlank(String imageFullPath)
	{
		Mat img = Highgui.imread(imageFullPath);
		boolean isAllSameColor = true;
		double[] imgRGBFirstPixel = img.get(0, 0);
		String colorFirstPixel = Util.getHexFromRGB((int)imgRGBFirstPixel[0], (int)imgRGBFirstPixel[1], (int)imgRGBFirstPixel[2]);
		for(int col = 1; col < img.cols(); col += 10)
		{
			for(int row = 1; row < img.rows(); row += 10)
			{
				double[] imgRGB = img.get(row, col);
				String color = Util.getHexFromRGB((int)imgRGB[0], (int)imgRGB[1], (int)imgRGB[2]);
				if(!color.equalsIgnoreCase(colorFirstPixel))
				{
					isAllSameColor = false;
					break;
				}
			}
		}
		return isAllSameColor;
	}
	
	public void compareImagesForSimilarity(String img1FullPath, String img2FullPath)
	{
		Mat img1 = Highgui.imread(img1FullPath);
		Mat img2 = Highgui.imread(img2FullPath);
		
		MatOfKeyPoint keypoints1 = new MatOfKeyPoint();
		MatOfKeyPoint keypoints2 = new MatOfKeyPoint();
		Mat outImage = new Mat();
		Scalar keypointColor = new Scalar(255.0, 0.0, 0.0);
		Scalar singlePointColor = new Scalar(0.0, 255.0, 0.0);
		MatOfDMatch matches1to2 = new MatOfDMatch();
		MatOfByte matchesMask = new MatOfByte();
		
		FeatureDetector detector = FeatureDetector.create(FeatureDetector.SIFT);
		detector.detect(img1, keypoints1);
		detector.detect(img2, keypoints2);
		
//		Features2d.drawKeypoints(img1, keypoints, outImage);		
//		Highgui.imwrite("C:\\USC\\rwd\\test_mob_desktop_keypoints.png", outImage);
		
		DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SIFT);
		DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);
		
		Mat descriptors1 = new Mat();
		Mat descriptors2 = new Mat();
		descriptorExtractor.compute(img1, keypoints1, descriptors1);
		descriptorExtractor.compute(img2, keypoints2, descriptors2);
		
		descriptorMatcher.match(descriptors1, descriptors2, matches1to2);
		
		System.out.println("matches1to2: " + matches1to2.total());
		
		System.out.println("keypoints1 = " + keypoints1.total());
		System.out.println("keypoints2 = " + keypoints2.total());
		System.out.println("descriptors1 = " + descriptors1.total());
		System.out.println("descriptors2 = " + descriptors2.total());
		
		outImage = new Mat();
		Features2d.drawMatches(img1, keypoints1, img2, keypoints2, matches1to2, outImage);
		Highgui.imwrite("/Users/sonal/USC/rwd/test_mob_desktop_keypoints_match.png", outImage);
		
		double maxDistance = 0; double minDistance = 100;
		List<DMatch> matchesList = matches1to2.toList();
		//-- Quick calculation of max and min distances between keypoints
		int rowCount = matchesList.size();
		for (int r = 0; r < rowCount; r++)
		{
			double dist = matchesList.get(r).distance;
			if (dist < minDistance)
				minDistance = dist;
			if (dist > maxDistance)
				maxDistance = dist;
		}
		
		System.out.println("Matches list: " + matchesList.size());
		
		List<DMatch> goodMatchesList = new ArrayList<DMatch>();
		double upperBound = 6 * minDistance;
		for (int i = 0; i < rowCount; i++)
		{
			//if (matchesList.get(i).distance < upperBound)
			if (matchesList.get(i).distance <= Math.max(6*minDistance, 0.02))
			{
				goodMatchesList.add(matchesList.get(i));
			}
		}
		MatOfDMatch goodMatches = new MatOfDMatch();
		goodMatches.fromList(goodMatchesList);
		
		outImage = new Mat();
		Features2d.drawMatches(img1, keypoints1, img2, keypoints2, goodMatches, outImage);
		Highgui.imwrite("/Users/sonal/USC/rwd/test_mob_desktop_keypoints_good_matches.png", outImage);

		System.out.println("Good matches list: " + goodMatchesList.size());
	}
	
	public static void main(String[] args)
	{
		ImageProcessing ip = new ImageProcessing();
		//ip.compareImagesForSimilarity("/Users/sonal/USC/rwd/mobify_selenium_browser_size_set_to_360_640.png", "/Users/sonal/USC/rwd/mobify_selenium_browser_size_set_to_391_640.png");
		
		ip.compareImagesByHistogram("/Users/sonal/USC/visual_checking/evaluation/test/output/ff/screenshot1.png", "/Users/sonal/USC/visual_checking/evaluation/test/output/ff1/screenshot2.png");
	}
}