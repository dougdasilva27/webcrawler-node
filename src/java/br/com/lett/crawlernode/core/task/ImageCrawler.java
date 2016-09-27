package br.com.lett.crawlernode.core.task;


import br.com.lett.crawlernode.core.session.CrawlerSession;

public class ImageCrawler implements Runnable {
	
	protected CrawlerSession session;
	
	public ImageCrawler(CrawlerSession session) {
		this.session = session;
	}
	
	@Override
	public void run() {
		System.out.println("Inside run method of Image Crawler.");
	}

}
