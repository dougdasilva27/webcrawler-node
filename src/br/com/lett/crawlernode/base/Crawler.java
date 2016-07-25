package br.com.lett.crawlernode.base;

import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import br.com.lett.crawlernode.fetcher.DataFetcher;
import br.com.lett.crawlernode.models.CrawlerSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	

	protected CrawlerSession session;
	
	public Crawler(CrawlerSession session) {
		this.session = session;
	}	
	
	public boolean shouldVisit(String url) {
		return true;
	}
	
	public void extract(String url) {
		Document document = preProcessing(url);
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
		extract(session.getUrl());
	}

}
