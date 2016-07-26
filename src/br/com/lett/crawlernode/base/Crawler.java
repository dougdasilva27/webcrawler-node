package br.com.lett.crawlernode.base;

import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import br.com.lett.crawlernode.fetcher.DataFetcher;
import br.com.lett.crawlernode.models.CrawlerSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Crawler superclass. All crawler tasks must extend this class to override both
 * the shouldVisit and extract methods.
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
	 * the current crawling session
	 */
	protected CrawlerSession session;


	public Crawler(CrawlerSession session) {
		this.session = session;
	}	

	/**
	 * It defines wether the crawler must true to extract data or not
	 * 
	 * @param url
	 * @return
	 */
	public boolean shouldVisit() {
		return true;
	}

	public void extract() {
		if ( shouldVisit() ) {
			Document document = preProcessing();
			extractInformation(document);
		}
	}

	public void extractInformation(Document document) {
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

	private Document preProcessing() {

		// fetch data
		String html = DataFetcher.fetchString(DataFetcher.GET_REQUEST, session, null, null);

		return Jsoup.parse(html);		
	}

	@Override 
	public void run() {
		extract();
	}

}
