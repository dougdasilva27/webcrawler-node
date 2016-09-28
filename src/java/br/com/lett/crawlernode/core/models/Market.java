package br.com.lett.crawlernode.core.models;

import java.util.ArrayList;

public class Market {
	
	private int id;
	private String city;
	private String name;
	private ArrayList<String> proxies;
	private ArrayList<String> imageProxies;
	
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
			ArrayList<String> proxies,
			ArrayList<String> imageProxies) {
		
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

	public ArrayList<String> getProxies() {
		return proxies;
	}

	public void setProxies(ArrayList<String> proxies) {
		this.proxies = proxies;
	}

	public ArrayList<String> getImageProxies() {
		return imageProxies;
	}

	public void setImageProxies(ArrayList<String> imageProxies) {
		this.imageProxies = imageProxies;
	}
}
