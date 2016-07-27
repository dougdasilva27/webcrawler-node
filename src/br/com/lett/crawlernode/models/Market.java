package br.com.lett.crawlernode.models;

public class Market {
	
	private int id;
	private String city;
	private String name;
	
	public Market(int number, String city, String name) {
		super();
		this.id = number;
		this.city = city;
		this.name = name;
	}
	
	@Override
	public String toString() {
		return "Market [number=" + id + ", city=" + city + ", name=" + name
				+ ", production=" + "]";
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
}
