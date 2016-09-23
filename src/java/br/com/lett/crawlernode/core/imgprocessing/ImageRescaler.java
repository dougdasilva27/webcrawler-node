package br.com.lett.crawlernode.core.imgprocessing;

import java.awt.Color;
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

import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class ImageRescaler {
	
	private static final Logger logger = LoggerFactory.getLogger(ImageRescaler.class);
	
	public ImageRescaler() {
		super();
	}
	
	public void rescale(
			CrawlerSession session,
			BufferedImage image, 
			File localFile, 
			String localOriginalFileDir, 
			String localSmallFileDir, 
			String localRegularFileDir) throws FileNotFoundException, IOException {
		
		boolean converted = false;
		
		if( imageType(localFile.getAbsolutePath()).equals("png") || image.getType() == 0) {
			Logging.printLogDebug(logger, session, "Image is png...converting to jpg...");
			
			image = convertFromPNGtoJPG(image);
			converted = true;
		}
		
		int widthOriginal = image.getWidth();
		int heightOriginal = image.getHeight();
		boolean wide = widthOriginal >= heightOriginal;
		int widthSmall = 0;
		int heightSmall = 0;
		int widthRegular = 0;
		int heightRegular = 0;
		
		if(wide) {
			
			widthSmall = 200;
			heightSmall = Math.round(200*((float)heightOriginal/(float)widthOriginal));
			
			heightRegular = 800;
			widthRegular = Math.round(800*((float)heightOriginal/(float)widthOriginal));
			
		} else {
			
			widthSmall = Math.round(200*((float)widthOriginal/(float)heightOriginal));
			heightSmall = 200;
			
			widthRegular = Math.round(800*((float)widthOriginal/(float)heightOriginal));
			heightRegular = 800;
			
		}
		
		ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        
        BufferedImage outputImageOriginal = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        BufferedImage outputImageSmall = new BufferedImage(widthSmall, heightSmall, image.getType());
        BufferedImage outputImageRegular = new BufferedImage(widthRegular, heightRegular, image.getType());
        
        Graphics2D g2d_original = outputImageOriginal.createGraphics();
        if(!converted) g2d_original.setColor(Color.WHITE);
        g2d_original.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2d_original.drawImage(image, 0, 0, widthOriginal, heightOriginal, null);
        g2d_original.dispose();
        
        Graphics2D g2d_small = outputImageSmall.createGraphics();
        if(!converted) g2d_small.setColor(Color.WHITE);
        g2d_small.fillRect(0, 0, widthSmall, heightSmall);
        g2d_small.drawImage(image, 0, 0, (widthOriginal < widthSmall ? widthOriginal : widthSmall), (heightOriginal < heightSmall ? heightOriginal : heightSmall), null);
        g2d_small.dispose();
        
        Graphics2D g2d_regular = outputImageRegular.createGraphics();
        if(!converted) g2d_regular.setColor(Color.WHITE);
        g2d_regular.fillRect(0, 0, widthRegular, heightRegular);        
        g2d_regular.drawImage(image, 0, 0, (widthOriginal < widthRegular ? widthOriginal : widthRegular), (heightOriginal < heightRegular ? heightOriginal : heightRegular), null);
        g2d_regular.dispose();
        
        // Escrevendo original
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(1.0F);
        writer.setOutput(new FileImageOutputStream(new File(localOriginalFileDir)));
        writer.write(null, new IIOImage(outputImageOriginal, null, null), param);
        
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality((float) 0.8);
        writer.setOutput(new FileImageOutputStream(new File(localSmallFileDir)));
        writer.write(null, new IIOImage(outputImageSmall, null, null), param);
        
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality((float) 0.5);
        writer.setOutput(new FileImageOutputStream(new File(localRegularFileDir)));
        writer.write(null, new IIOImage(outputImageRegular, null, null), param);
        
        writer.dispose();
	}
	
	private BufferedImage convertFromPNGtoJPG(BufferedImage image) {
		BufferedImage jpgImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		jpgImage.createGraphics().drawImage(image, 0, 0, Color.white, null);
		
		return jpgImage;
	}
	
	private String imageType(String absolutePath) {
		String[] tokens = absolutePath.split("/");
		String fileName = tokens[tokens.length-1];
		
		return fileName.split("\\.")[1];
	}

}
