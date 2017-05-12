package br.com.lett.crawlernode.core.models;

import org.json.JSONObject;

import br.com.lett.crawlernode.util.CommonMethods;
import models.Marketplace;
import models.Prices;

public class Product {
	
	private String 		url;
	private String 		internalId;
	private String 		internalPid;
	private String 		name;
	private Float 		price;
	private Prices 		prices;
	private boolean 	available;
	private String 		category1;
	private String 		category2;
	private String 		category3;
	private String 		primaryImage;
	private String 		secondaryImages;
	private String 		description;
	private Marketplace 	marketplace;
	private Integer 	stock;
	
	public Product() {
		this.description = "";
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
		if(primaryImage != null) {
			this.primaryImage = CommonMethods.removeIllegalParameters(primaryImage);
		} else {
			this.primaryImage = primaryImage;
		}
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
		if (description == null) {
			this.description = "";
		} else {
			this.description = description;
		}
	}
	
	public Marketplace getMarketplace() {
		return marketplace;
	}
	
	public void setMarketplace(Marketplace marketplace) {
		this.marketplace = marketplace;
	}
	
	public Integer getStock() {
		return stock;
	}
	
	public void setStock(Integer stock) {
		this.stock = stock;
	}
	
	/**
	 * Check if the product instance is void. Cases in which it's considered
	 * a void product:
	 * <ul>
	 * <li>1. The price is null or 0.0 and at the same time is available.</li>
	 * <li>2. The internal id is null or is an empty string</li>
	 * <li>3. The name is null or is an empty string</li>
	 * </ul>
	 * @return true if product is void or false otherwise
	 */
	public boolean isVoid() {
		if((price == null || price.equals(0f)) && available) {
			return true;
		}
		if(internalId == null || internalId.isEmpty()) {
			return true;
		}
		if(name == null || name.isEmpty()) {
			return true;
		}
		
		return false;
	}

	public Prices getPrices() {
		return prices;
	}

	public void setPrices(Prices prices) {
		this.prices = prices;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("\n" + "url: " + this.url + "\n");
		sb.append("internalId: " + this.internalId + "\n");
		sb.append("internalPid: " + this.internalPid + "\n");
		sb.append("name: " + this.name + "\n");
		sb.append("price: " + this.price + "\n");
		sb.append("prices: " + (this.prices != null ? this.prices.toString() : null) + "\n");
		sb.append("available: " + this.available + "\n");
		sb.append("marketplace: " +  (this.marketplace != null ? this.marketplace.size() : null) + "\n");
		sb.append("category1: " + this.category1 + "\n");
		sb.append("category2: " + this.category2 + "\n");
		sb.append("category3: " + this.category3 + "\n");
		sb.append("primary image: " + this.primaryImage + "\n");
		sb.append("secondary images: " + (this.secondaryImages != null ? "yes" : "no")  + "\n");
		sb.append("description: " + "html code with " + this.description.length() + " characters" + "\n");
		sb.append("stock: " + this.stock + "\n");

		return sb.toString();
	}
	
	public JSONObject toJSON() {
		JSONObject obj = new JSONObject();
		
		obj.put("url", (url != null ? url : JSONObject.NULL));
		obj.put("internalId", (internalId != null ? internalId : JSONObject.NULL));
		obj.put("internalPid", (internalPid != null ? internalPid : JSONObject.NULL));
		obj.put("name", (name != null ? name : JSONObject.NULL));
		obj.put("price", (price != null ? price : JSONObject.NULL));
		obj.put("prices", (prices != null ? prices.toString() : JSONObject.NULL));
		obj.put("available", available);
		obj.put("category1", (category1 != null ? category1 : JSONObject.NULL));
		obj.put("category2", (category2 != null ? category2 : JSONObject.NULL));
		obj.put("category3", (category3 != null ? category3 : JSONObject.NULL));
		obj.put("primaryImage", (primaryImage != null ? primaryImage : JSONObject.NULL));
		obj.put("secondaryImages", (secondaryImages != null ? secondaryImages : JSONObject.NULL));
		obj.put("marketplace", (marketplace != null ? marketplace.toString() : JSONObject.NULL));
		obj.put("stock", (stock != null ? stock : JSONObject.NULL));
		obj.put("description", (description != null ? description : JSONObject.NULL));
		
		return obj;
	}

}
