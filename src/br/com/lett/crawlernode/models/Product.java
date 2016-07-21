package br.com.lett.crawlernode.models;

import org.json.JSONArray;

public class Product {
	
	private String url;
	private String seedId;
	private String internalId;
	private String internalPid;
	private String name;
	private Float price;
	private boolean available;
	private String category1;
	private String category2;
	private String category3;
	private String primaryImage;
	private String secondaryImages;
	private String description;
	private JSONArray marketplace;
	private Integer stock;
	
	public String getSeedId() {
		return seedId;
	}
	
	public void setSeedId(String seedId) {
		this.seedId = seedId;
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getInternalId() {
		return internalId;
	}
	
	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}
	
	public String getInternalPid() {
		return this.internalPid;
	}
	
	public void setInternalPid(String internalPid) {
		this.internalPid = internalPid;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Float getPrice() {
		return price;
	}
	
	public void setPrice(Float price) {
		this.price = price;
	}
	
	public boolean getAvailable() {
		return available;
	}
	
	public void setAvailable(boolean available) {
		this.available = available;
	}
	
	public String getCategory1() {
		return category1;
	}
	
	public void setCategory1(String category1) {
		this.category1 = category1;
	}
	public String getCategory2() {
		return category2;
	}
	
	public void setCategory2(String category2) {
		this.category2 = category2;
	}
	
	public String getCategory3() {
		return category3;
	}
	
	public void setCategory3(String category3) {
		this.category3 = category3;
	}
	
	public String getPrimaryImage() {
		return primaryImage;
	}
	
	public void setPrimaryImage(String primaryImage) {
		this.primaryImage = primaryImage;
	}
	
	public String getSecondaryImages() {
		return secondaryImages;
	}
	
	public void setSecondaryImages(String secondaryImages) {
		this.secondaryImages = secondaryImages;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public JSONArray getMarketplace() {
		return marketplace;
	}
	
	public void setMarketplace(JSONArray marketplace) {
		this.marketplace = marketplace;
	}
	
	public Integer getStock() {
		return stock;
	}
	
	public void setStock(Integer stock) {
		this.stock = stock;
	}

}
