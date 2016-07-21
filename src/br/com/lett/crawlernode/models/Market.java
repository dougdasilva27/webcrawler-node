package br.com.lett.crawlernode.models;

public class Market {
	
	private int number;
	private String city;
	private String name;
	private Boolean production;
	
	
	public Market(int number, String city, String name, Boolean production) {
		super();
		this.number = number;
		this.city = city;
		this.name = name;
		this.production = production;
	}
	
	@Override
	public String toString() {
		return "Market [number=" + number + ", city=" + city + ", name=" + name
				+ ", production=" + production + "]";
	}

	public int getNumber() {
		return number;
	}
	
	public void setNumber(int number) {
		this.number = number;
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

	public Boolean getProduction() {
		return production;
	}
	
	public void setProduction(Boolean production) {
		this.production = production;
	}
}
