package br.com.lett.crawlernode.core.task;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.imgprocessing.ImageRescaler;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.session.CrawlerSessionError;
import br.com.lett.crawlernode.core.session.ImageCrawlerSession;
import br.com.lett.crawlernode.database.Persistence;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;

public class ImageCrawler implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(ImageCrawler.class);
	
	protected CrawlerSession session;
	
	public ImageCrawler(CrawlerSession session) {
		this.session = session;
	}
	
	@Override
	public void run() {
		
		try {
			
			// download the image
			Logging.printLogDebug(logger, session, "Downloading image from market...");
			File imageFile = DataFetcher.fetchImage(session);
			
			// create a buffered image from the downloaded image
			Logging.printLogDebug(logger, session, "Creating a buffered image...");
			BufferedImage bufferedImage = createImage(imageFile);
			
			// apply rescaling on the image
			Logging.printLogDebug(logger, session, "Rescaling the image...");
			rescale(bufferedImage, imageFile);			
			
		}
		catch (IOException e) {
			session.registerError( new CrawlerSessionError(CrawlerSessionError.EXCEPTION, CommonMethods.getStackTraceString(e)) );
			Logging.printLogError(logger, session, CommonMethods.getStackTraceString(e));
		}		
		
	}
	
	private BufferedImage createImage(File imageFile) throws IOException {
		if(imageFile != null) return ImageIO.read(imageFile);
		return null;
	}
	
	private void rescale(BufferedImage bufferedImage, File imageFile) throws FileNotFoundException, IOException {
		if (bufferedImage != null) {
			ImageRescaler.rescale(session, bufferedImage, imageFile);
		} else {
			Logging.printLogError(logger, session, "Image downloaded is null.");
		}
	}

}
