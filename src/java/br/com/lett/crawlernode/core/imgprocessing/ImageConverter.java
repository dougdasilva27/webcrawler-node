package br.com.lett.crawlernode.core.imgprocessing;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

public class ImageConverter {

	private static final Logger logger = LoggerFactory.getLogger(ImageConverter.class);
	
	private ImageConverter() {
		super();
	}

	/**
	 * If image is PNG, it will be converted to JPG.
	 * 
	 * @param session
	 * @param image
	 * @param localFile
	 * @throws IOException
	 */
	public static void convertToJPG(
			Session session,
			BufferedImage image, 
			File localFile) throws IOException {

		if( imageType(localFile.getAbsolutePath()).equals("png") || image.getType() == 0 || image.getType() == 6) {
			Logging.printLogDebug(logger, session, "Image is png...converting to jpg...");
			image = convertFromPNGtoJPG(image);
		}
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
