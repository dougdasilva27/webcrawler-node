package br.com.lett.crawlernode.core.imgprocessing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.ImageCrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class ImageRescaler {

	private static final Logger logger = LoggerFactory.getLogger(ImageRescaler.class);
	
	private static final String ORIGINAL_IMAGE = "original";
	private static final String REGULAR_IMAGE = "regular";
	private static final String SMALL_IMAGE = "small";
	
	private static final int SMALL_WIDTH = 200;
	private static final int SMALL_HEIGHT = 200;
	
	private static final int REGULAR_WIDTH = 800;
	private static final int REGULAR_HEIGHT = 800;

	private static final float SMALL_IMAGE_COMPRESSION_QUALITY = 0.8f;
	private static final float REGULAR_IMAGE_COMPRESSION_QUALITY = 0.5f;
	private static final float ORIGINAL_IMAGE_COMPRESSION_QUALITY = 1.0f;

	public static void rescale(
			Session session,
			BufferedImage image, 
			File localFile) throws FileNotFoundException, IOException {

		String localOriginalFileDir = ((ImageCrawlerSession)session).getLocalOriginalFileDir(); 
		String localSmallFileDir = ((ImageCrawlerSession)session).getLocalSmallFileDir(); 
		String localRegularFileDir = ((ImageCrawlerSession)session).getLocalRegularFileDir();

		boolean converted = false;

		if( imageType(localFile.getAbsolutePath()).equals("png") || image.getType() == 0 || image.getType() == 6) {
			Logging.printLogDebug(logger, session, "Image is png...converting to jpg...");

			image = convertFromPNGtoJPG(image);
			converted = true;
		}

		int widthOriginal = image.getWidth();
		int heightOriginal = image.getHeight();

		// compute dimensions
		Dimension originalDimension = new Dimension(widthOriginal, heightOriginal);
		Dimension smallDimension = computeSmallDimension(originalDimension);
		Dimension regularDimension = computeRegularDimension(originalDimension);

		// compute the new image location after image translation
		Point regularLocation = computeLocation(originalDimension, regularDimension);
		Point smallLocation = computeLocation(originalDimension, smallDimension);

		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		ImageWriteParam param = writer.getDefaultWriteParam();

		BufferedImage outputImageOriginal = new BufferedImage(originalDimension.width, originalDimension.height, image.getType());
		BufferedImage outputImageSmall = new BufferedImage(smallDimension.width, smallDimension.height, image.getType());
		BufferedImage outputImageRegular = new BufferedImage(regularDimension.width, regularDimension.height, image.getType());

		// original
		Graphics2D g2d_original = outputImageOriginal.createGraphics();
		if(!converted) g2d_original.setColor(Color.WHITE);
		g2d_original.fillRect(0, 0, originalDimension.width, originalDimension.height);
		g2d_original.drawImage(
				image, 
				0, 
				0, 
				originalDimension.width, 
				originalDimension.height, 
				null);
		g2d_original.dispose();

		// small
		Graphics2D g2d_small = outputImageSmall.createGraphics();
		if(!converted) g2d_small.setColor(Color.WHITE);
		g2d_small.fillRect(0, 0, smallDimension.width, smallDimension.height);
		g2d_small.drawImage(
				image, 
				smallLocation.x, 
				smallLocation.y, 
				(widthOriginal < smallDimension.width ? widthOriginal : smallDimension.width), 
				(heightOriginal < smallDimension.height ? heightOriginal : smallDimension.height), 
				null);
		g2d_small.dispose();

		// regular
		Graphics2D g2d_regular = outputImageRegular.createGraphics();
		if(!converted) g2d_regular.setColor(Color.WHITE);
		g2d_regular.fillRect(0, 0, regularDimension.width, regularDimension.height);        
		g2d_regular.drawImage(
				image, 
				regularLocation.x, 
				regularLocation.y, 
				(widthOriginal < regularDimension.width ? widthOriginal : regularDimension.width), 
				(heightOriginal < regularDimension.height ? heightOriginal : regularDimension.height), 
				null);
		g2d_regular.dispose();

		// write final images to file
		writeImage(param, writer, ORIGINAL_IMAGE, localOriginalFileDir, outputImageOriginal);
		writeImage(param, writer, SMALL_IMAGE, localSmallFileDir, outputImageSmall);
		writeImage(param, writer, REGULAR_IMAGE, localRegularFileDir, outputImageRegular);

		writer.dispose();
	}
	
	/**
	 * Write the image to a file.
	 * 
	 * @param param
	 * @param writer
	 * @param imageDimensionType
	 * @param fileDir
	 * @param outputImage
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void writeImage(
			ImageWriteParam param,
			ImageWriter writer,
			String imageDimensionType, 
			String fileDir, 
			BufferedImage outputImage) throws FileNotFoundException, IOException {
		
		if (imageDimensionType.equals(ORIGINAL_IMAGE)) {
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(ORIGINAL_IMAGE_COMPRESSION_QUALITY);
		}
		else if (imageDimensionType.equals(SMALL_IMAGE)) {
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(SMALL_IMAGE_COMPRESSION_QUALITY);
		}
		else if (imageDimensionType.equals(REGULAR_IMAGE)) {
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(REGULAR_IMAGE_COMPRESSION_QUALITY);
		}
		
		writer.setOutput(new FileImageOutputStream(new File(fileDir)));
		writer.write(null, new IIOImage(outputImage, null, null), param);
		
	}

	/**
	 * Computes the new location (leftmost and uppermost point) for the new image
	 * relative to the original.
	 * 
	 * @param originalDimension
	 * @param newDimension
	 * @return the point with the new location
	 */
	private static Point computeLocation(Dimension originalDimension, Dimension newDimension) {		
		double originalWidth = originalDimension.getSize().getWidth();
		double originalHeight = originalDimension.getSize().getHeight();
		double newWidth = newDimension.getSize().getWidth();
		double newHeight = newDimension.getSize().getHeight();

		double x;
		double y;

		if (originalWidth <= newWidth) {
			x = (newWidth - originalWidth)/2;
		} else {
			x = 0;
		}

		if (originalHeight <= newHeight) {
			y = (newHeight - originalHeight)/2;
		} else {
			y = 0;
		}

		return new Point(Math.round((float)x), Math.round((float)y));
	}

	private static Dimension computeSmallDimension(Dimension originalDimension) {
		int width;
		int height;

		if(isWide(originalDimension.width, originalDimension.height)) {
			width = SMALL_WIDTH;
			height = Math.round(SMALL_HEIGHT*((float)originalDimension.height/(float)originalDimension.width));

		} else {
			width = Math.round(SMALL_WIDTH*((float)originalDimension.width/(float)originalDimension.height));
			height = SMALL_HEIGHT;
		}

		return new Dimension(width, height);
	}

	private static Dimension computeRegularDimension(Dimension originalDimension) {
		int width;
		int height;

		if(isWide(originalDimension.width, originalDimension.height)) {
			width = REGULAR_WIDTH;
			height = Math.round(REGULAR_HEIGHT*((float)originalDimension.height/(float)originalDimension.width));

		} else {
			width = Math.round(REGULAR_WIDTH*((float)originalDimension.width/(float)originalDimension.height));
			height = REGULAR_HEIGHT;
		}

		return new Dimension(width, height);
	}

	private static boolean isWide(int width, int height) {
		return width >= height;
	}

	private static BufferedImage convertFromPNGtoJPG(BufferedImage image) {
		BufferedImage jpgImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		jpgImage.createGraphics().drawImage(image, 0, 0, Color.white, null);
		return jpgImage;
	}

	private static String imageType(String absolutePath) {
		String[] tokens = absolutePath.split("/");
		String fileName = tokens[tokens.length-1];
		return fileName.split("\\.")[1];
	}

}
