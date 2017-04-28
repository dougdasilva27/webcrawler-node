package br.com.lett.crawlernode.core.imgprocessing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
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

import br.com.lett.crawlernode.core.session.ImageCrawlerSession;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

public class ImageConverter {

	private static final Logger logger = LoggerFactory.getLogger(ImageConverter.class);
	
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
	public static File createTransformedImageFile(
			File localOriginalFile,
			Session session) throws IOException {
		
		ImageCrawlerSession imageCrawlerSession = (ImageCrawlerSession)session;
		
		// create a buffered image from the downloaded image
		// which is the original image
		Logging.printLogDebug(logger, session, "Creating a buffered image...");
		BufferedImage bufferedImage = createBufferedImage(localOriginalFile, session);
		
		if (bufferedImage == null) {
			Logging.printLogError(logger, session, "Image downloaded is null...returning...");
			return null;
		}

		boolean converted = false;
		if( imageType(localOriginalFile.getAbsolutePath()).equals("png") || bufferedImage.getType() == 0) {
			Logging.printLogDebug(logger, session, "Image is png...converting to jpg...");

			bufferedImage = convertFromPNGtoJPG(bufferedImage);
			converted = true;
		}
		
		// compute dimensions
		int widthOriginal = bufferedImage.getWidth();
		int heightOriginal = bufferedImage.getHeight();
		Dimension originalDimension = new Dimension(widthOriginal, heightOriginal);
		
		BufferedImage transformedBufferedImage = new BufferedImage(originalDimension.width, originalDimension.height, bufferedImage.getType());
		
		Graphics2D graphics2dOriginal = transformedBufferedImage.createGraphics();
		if (!converted) {
			graphics2dOriginal.setColor(Color.WHITE);
		}
		graphics2dOriginal.fillRect(0, 0, originalDimension.width, originalDimension.height);
		graphics2dOriginal.drawImage(
				bufferedImage, 
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
	
	/**
	 * 
	 * @param imageFile
	 * @return
	 * @throws IOException
	 */
	private static BufferedImage createBufferedImage(File imageFile, Session session) throws IOException {
		if (imageFile == null) {
			Logging.printLogDebug(logger, session, "Image file is null!");
			return null;
		}

		return ImageIO.read(imageFile);
	}

}