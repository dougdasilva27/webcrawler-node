package br.com.lett.crawlernode.base;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import br.com.lett.crawlernode.fetcher.DataFetcher;
import br.com.lett.crawlernode.models.Market;
import uk.org.lidalia.slf4jext.Logger;
import uk.org.lidalia.slf4jext.LoggerFactory;

/**
 * 
 * @author Samir Leão
 *
 */

public class Crawler implements Runnable {
	
	protected static final Logger logger = LoggerFactory.getLogger(Crawler.class);
	
	protected final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|bmp|gif|jpe?g"
			+ "|png|ico|tiff?|mid|mp2|mp3|mp4"
			+ "|wav|avi|mov|mpeg|ram|m4v|pdf" 
			+ "|rm|smil|wmv|swf|wma|zip|rar|gz))(\\?.*)?$");
	
	/**
	 * The market associated with this crawler
	 */
	public Market market;
	
	public String taskURL;
	
	
	
	public Crawler(String url) {
		this.taskURL = url;
	}	
	
	
	public boolean shouldVisit(String url) {
		return true;
	}
	
	public void extract(String url) {
		
//		Document document = preProcessing(url);
		
		Document document = null;
		
		extractInformation(document, url);
		
	}
	
	public void extractInformation(Document document, String url) {
		beforeExtraction();
		
		/*
		 * Other functionalities will be implemented on subclasses.
		 */
	}
	
	public void beforeExtraction() {
		/*
		 * Do nothing by default. Subclasses will implement the desired functionality.
		 */
	}
	
	private Document preProcessing(String url) {
		
		// fetch data
		String html = DataFetcher.fetchString(DataFetcher.GET_REQUEST, url, null, null);
		
		return Jsoup.parse(html);		
	}
	
	@Override 
	public void run() {
		extract(this.taskURL);
	}

}
