package br.com.lett.crawlernode.core.imgprocessing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.session.crawler.ImageCrawlerSession;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class ImageConverter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ImageConverter.class);

	private static final float ORIGINAL_IMAGE_COMPRESSION_QUALITY = 1.0f;

	private ImageConverter() {
		super();
	}

	/**
	 * Convert an image to jpg, if it is png, and also apply
	 * a transformation on the original jpg image.
	 * 
	 * @param session
	 * @param localOriginalFile
	 * @return  <br>the transformed image as a file
	 * 			<br>null if no file was created
	 * @throws IOException
	 */
	public static File createTransformedImageFile(File localOriginalFile, Session session) throws IOException {
		if (localOriginalFile == null) {
			Logging.printLogError(LOGGER, session, "Image downloaded is null...returning...");
			return null;
		}

		ImageCrawlerSession imageCrawlerSession = (ImageCrawlerSession)session;

		String imageFormatName = getImageFormatName(localOriginalFile, session);

		Logging.printLogDebug(LOGGER, imageCrawlerSession, "Image format: " + imageFormatName);

		File convertedToJPEG = null;

		boolean converted = false;

		if ( ("png").equals(imageFormatName) ) {
			Logging.printLogWarn(LOGGER, imageCrawlerSession, "Image is png .. converting to jpeg ...");
			convertedToJPEG = pngToJPEG(localOriginalFile, session);
			converted = true;
			Logging.printLogWarn(LOGGER, imageCrawlerSession, "Done converting");
		}
		
		BufferedImage originalBufferedImage;
		if (convertedToJPEG == null) {
			originalBufferedImage = ImageIO.read(localOriginalFile);
		} else {
			originalBufferedImage = ImageIO.read(convertedToJPEG);
		}
		
		if ( originalBufferedImage == null ) {
			Logging.printLogWarn(LOGGER, imageCrawlerSession, "Buffered image of the downloaded image is null. Probably it's not an image.");
			return null;
		}
		
		// compute dimensions
		Dimension originalDimension = new Dimension(originalBufferedImage.getWidth(), originalBufferedImage.getHeight());
		
		BufferedImage transformedBufferedImage = new BufferedImage(originalDimension.width, originalDimension.height, originalBufferedImage.getType());
		
		Graphics2D graphics2dOriginal = transformedBufferedImage.createGraphics();

		if (!converted) {
			graphics2dOriginal.setColor(Color.WHITE);
		}

		graphics2dOriginal.fillRect(
				0, 
				0, 
				originalDimension.width, 
				originalDimension.height);

		graphics2dOriginal.drawImage(
				originalBufferedImage, 
				0, 
				0, 
				originalDimension.width, 
				originalDimension.height, 
				null);

		graphics2dOriginal.dispose();

		// write final image to the tmp converted image file
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
		ImageWriteParam param = writer.getDefaultWriteParam();

		writeImage(param, writer, imageCrawlerSession.getLocalTransformedFileDir(), transformedBufferedImage);

		writer.dispose();

		return new File(imageCrawlerSession.getLocalTransformedFileDir());	
	}

	/**
	 * Write the image to a file.
	 * 
	 * @param param
	 * @param writer
	 * @param fileDir
	 * @param outputImage
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void writeImage(
			ImageWriteParam param,
			ImageWriter writer,
			String fileDir, 
			BufferedImage outputImage) throws IOException {

		param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
		param.setCompressionQuality(ORIGINAL_IMAGE_COMPRESSION_QUALITY);

		writer.setOutput(new FileImageOutputStream(new File(fileDir)));
		writer.write(null, new IIOImage(outputImage, null, null), param);
	}

	/**
	 * Converts a png image file to jpeg. The original file
	 * will not be overrided, because it is the original image downloaded
	 * from the market. This one we have to save as it is. 
	 * <br>Remember that in our application, even
	 * if the original file is'n a jpeg file, after the product
	 * image on the market is downloaded, we consider it is jpeg and
	 * automatically use extension .jpg on the file. TODO this will be changed. 
	 * <br>This method will create the transformed file, which wil be the
	 * final file.
	 * 
	 * @param pngImageFile
	 * @param Session
	 * @return 	<br>the converted jpg file
	 * 			<br>null if any error occurred during conversion
	 */
	public static File pngToJPEG(File pngImageFile, Session session) {
		try {

			ImageCrawlerSession imageCrawlerSession = (ImageCrawlerSession)session;

			File jpgImageFile = new File(imageCrawlerSession.getLocalTransformedFileDir());

			//read image file
			BufferedImage pngBufferedImage = ImageIO.read(pngImageFile);

			// create a blank, RGB, same width and height, and a white background
			BufferedImage newBufferedImage = new BufferedImage(
					pngBufferedImage.getWidth(),
					pngBufferedImage.getHeight(), 
					BufferedImage.TYPE_INT_RGB);

			newBufferedImage.createGraphics().drawImage(pngBufferedImage, 0, 0, Color.WHITE, null);

			// write to jpeg file
			ImageIO.write(newBufferedImage, "jpg", jpgImageFile);

			return jpgImageFile;

		} catch (IOException e) {

			e.printStackTrace();

			return null;
		}

	}

	/**
	 * Get the image file format name.
	 * 
	 * @param imageFile
	 * @return 	<br>the image file format name in lower case.
	 * 			<br>an empty string if it isn't an image.
	 */
	public static String getImageFormatName(File imageFile, Session session) {
		ImageInputStream iis = null;
		try {
			iis = ImageIO.createImageInputStream(imageFile);
			Iterator<ImageReader> iter = ImageIO.getImageReaders(iis);

			if (iter.hasNext()) {
				ImageReader reader = iter.next();
				String formatName = reader.getFormatName().toLowerCase();
				iis.close();
				return formatName;
			} else {
				return "";
			}

		} catch (IOException e) {
			Logging.printLogDebug(LOGGER, session, CommonMethods.getStackTraceString(e));
		} catch (IllegalArgumentException e) {
			Logging.printLogDebug(LOGGER, session, "Image file is null.");
			Logging.printLogDebug(LOGGER, session, CommonMethods.getStackTraceString(e));
		}

		return "";
	}

}