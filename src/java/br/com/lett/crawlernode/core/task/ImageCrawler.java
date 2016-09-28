package br.com.lett.crawlernode.core.task;


import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.session.ImageCrawlerSession;

public class ImageCrawler implements Runnable {
	
	protected CrawlerSession session;
	
	public ImageCrawler(CrawlerSession session) {
		this.session = session;
	}
	
	@Override
	public void run() {
		System.out.println("Inside the run method...");
		System.out.println("Image URL: " + session.getUrl());
		
		if (session instanceof ImageCrawlerSession) {
			System.out.println("Image type: " + ((ImageCrawlerSession)session).getType());
		}
		
		
	}

}
