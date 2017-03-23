package br.com.lett.crawlernode.core.models;

import java.util.List;

public class Market {
	
	private int id;
	private String city;
	private String name;
	private List<String> proxies;
	private List<String> imageProxies;
	
	/**
	 * Default constructor used for testing.
	 * @param marketId
	 * @param marketCity
	 * @param marketName
	 * @param proxies
	 */
	public Market(
			int marketId,
			String marketCity,
			String marketName,
			List<String> proxies,
			List<String> imageProxies) {
		
		this.id = marketId;
		this.city = marketCity;
		this.name = marketName;
		this.proxies = proxies;
		this.imageProxies = imageProxies;
		
	}

	public int getNumber() {
		return id;
	}
	
	public void setNumber(int number) {
		this.id = number;
	}

	public String getCity() {
		return city;
	}
	
	public void setCity(String city) {
		this.city = city;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "Market [id=" + this.id + 
				", city=" + this.city + 
				", name=" + this.name +
				", proxies=" + this.proxies.toString() +
				", image proxies=" + this.imageProxies.toString();
	}

	public List<String> getProxies() {
		return proxies;
	}

	public void setProxies(List<String> proxies) {
		this.proxies = proxies;
	}

	public List<String> getImageProxies() {
		return imageProxies;
	}

	public void setImageProxies(List<String> imageProxies) {
		this.imageProxies = imageProxies;
	}
}
