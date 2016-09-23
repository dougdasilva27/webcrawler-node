package br.com.lett.crawlernode.core.task;

public class ImageCrawler implements Runnable {
	
	private String market;
	private String city;
	
	public ImageCrawler(String market, String city) {
		this.market = market;
		this.city = city;
	}
	
	@Override
	public void run() {
		System.out.println("Inside run method of Image Crawler.");
	}

	public String getMarket() {
		return market;
	}

	public void setMarket(String market) {
		this.market = market;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

}
