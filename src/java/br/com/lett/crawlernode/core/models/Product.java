package br.com.lett.crawlernode.core.models;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.SerializationUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.common.base.CharMatcher;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.DateUtils;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class Product implements Serializable {

	private static final long serialVersionUID = 4971005612828002546L;
	
	private String url;
	private String internalId;
	private String internalPid;
	private String name;
	private Float price;
	private Prices prices;
	private boolean available;
	private String category1;
	private String category2;
	private String category3;
	private String primaryImage;
	private String secondaryImages;
	private String description;
	private Marketplace marketplace;
	private Integer stock;
	private String ean;
	private List<String> eans;
	private String timestamp;
	private Integer marketId;
	private String status;

	public Product() {
		this.description = "";
		this.timestamp = DateUtils.newTimestamp();
	}
	
	public Product clone() {
		return SerializationUtils.clone(this);
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
		if (price != null) {
			this.price = MathUtils.normalizeTwoDecimalPlaces(price);
		} else {
			this.price = price;
		}
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
		// It was identified that the urls of images has some special characters,
		// the characters are the ones that are not in ASCII, tab and space
		CharMatcher desired = CharMatcher.ASCII // match character in ASCII
				.and(CharMatcher.noneOf(" 	")) // no match of space and tab
				.precomputed();

		if (primaryImage != null && !primaryImage.isEmpty() && !desired.matchesAllOf(primaryImage)) {
			this.primaryImage = CommonMethods.sanitizeUrl(primaryImage);
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
	 * Check if the product instance is void. Cases in which it's considered a void product:
	 * <ul>
	 * <li>1. The price is null or 0.0 and at the same time is available.</li>
	 * <li>2. The internal id is null or is an empty string</li>
	 * <li>3. The name is null or is an empty string</li>
	 * </ul>
	 * 
	 * @return true if product is void or false otherwise
	 */
	public boolean isVoid() {
		if ((price == null || price.equals(0f)) && available) {
			return true;
		}
		if (internalId == null || internalId.isEmpty()) {
			return true;
		}
		if (name == null || name.isEmpty()) {
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

	public String getEan() {
		return ean;
	}

	public void setEan(String ean) {
		this.ean = ean;
	}

	public List<String> getEans() {
		return this.eans;
	}

	public void setEans(List<String> eans) {
		this.eans = eans;
	}
	
	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public Integer getMarketId() {
		return marketId;
	}

	public void setMarketId(Integer marketId) {
		this.marketId = marketId;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		String images = this.secondaryImages != null ? this.secondaryImages.replace("[", "").replace("]", "").trim() : "";
		int secondaryImagesNumber = 0;

		if (images.contains(",")) {
			secondaryImagesNumber = images.split("\",").length;
		} else if (!images.isEmpty()) {
			secondaryImagesNumber = 1;
		}

		sb.append("\n" + "url: " + this.url + "\n");
		sb.append("internalId: " + this.internalId + "\n");
		sb.append("internalPid: " + this.internalPid + "\n");
		sb.append("name: " + this.name + "\n");
		sb.append("price: " + this.price + "\n");
		sb.append("prices: " + (this.prices != null ? this.prices.toString() : null) + "\n");
		sb.append("available: " + this.available + "\n");
		sb.append("marketplace: " + (this.marketplace != null ? this.marketplace.size() : null) + "\n");
		sb.append("category1: " + this.category1 + "\n");
		sb.append("category2: " + this.category2 + "\n");
		sb.append("category3: " + this.category3 + "\n");
		sb.append("primary image: " + this.primaryImage + "\n");
		sb.append("secondary images: " + secondaryImagesNumber + "\n");
		sb.append("description: " + "html code with " + this.description.length() + " characters" + "\n");
		sb.append("stock: " + this.stock + "\n");
		sb.append("ean: " + this.ean + "\n");
		sb.append("eans: " + (this.eans == null ? this.eans : this.eans.toString()) + "\n");

		return sb.toString();
	}

	public String toJson() {
		return new JSONObject()
				.put("url", (url != null ? url : JSONObject.NULL))
				.put("internalId", (internalId != null ? internalId : JSONObject.NULL))
				.put("internalPid", (internalPid != null ? internalPid : JSONObject.NULL))
				.put("name", (name != null ? name : JSONObject.NULL))
				.put("price", (price != null ? price : JSONObject.NULL))
				.put("prices", (prices != null ? prices.toString() : JSONObject.NULL))
				.put("available", available)
				.put("category1", (category1 != null ? category1 : JSONObject.NULL))
				.put("category2", (category2 != null ? category2 : JSONObject.NULL))
				.put("category3", (category3 != null ? category3 : JSONObject.NULL))
				.put("primaryImage", (primaryImage != null ? primaryImage : JSONObject.NULL))
				.put("secondaryImages", (secondaryImages != null ? secondaryImages : JSONObject.NULL))
				.put("marketplace", (marketplace != null ? marketplace.toString() : JSONObject.NULL))
				.put("stock", (stock != null ? stock : JSONObject.NULL))
				.put("description", (description != null ? description : JSONObject.NULL))
				.put("eans", (eans != null ? eans : Collections.EMPTY_LIST))
				.put("timestamp", timestamp)
				.toString();
	}
	
	public String serializeToKinesis() {
		
		// Data transformation rule for marketplace
		// String that must be interpreted as JSONArray
		// Nullable
		// We never have empty array
		JSONArray marketplaceArray = null;
		if (marketplace != null) {
			marketplaceArray = new JSONArray(marketplace);
			if (marketplaceArray.length() == 0) marketplaceArray = null;
		}
		
		
		
		return 
				new JSONObject()
				.put("url", (url != null ? url : JSONObject.NULL))
				.put("internalId", (internalId != null ? internalId : JSONObject.NULL))
				.put("internalPid", (internalPid != null ? internalPid : JSONObject.NULL))
				.put("marketId", marketId)
				.put("name", (name != null ? name : JSONObject.NULL))
				.put("price", (price != null ? price.toString() : JSONObject.NULL))
				.put("prices", (prices != null ? prices.toString() : JSONObject.NULL))
				.put("status", status)
				.put("available", available)
				.put("category1", (category1 != null ? category1 : JSONObject.NULL))
				.put("category2", (category2 != null ? category2 : JSONObject.NULL))
				.put("category3", (category3 != null ? category3 : JSONObject.NULL))
				.put("primaryImage", (primaryImage != null ? primaryImage : JSONObject.NULL))
				.put("secondaryImages", (secondaryImages != null ? secondaryImages : JSONObject.NULL))
				.put("marketplace", (marketplaceArray != null ? marketplaceArray.toString() : JSONObject.NULL))
				.put("stock", (stock != null ? stock : JSONObject.NULL))
				.put("description", ((description != null && !description.isEmpty()) ? description : JSONObject.NULL))
				.put("eans", ((eans != null && !eans.isEmpty()) ? eans : JSONObject.NULL))
				.put("timestamp", timestamp)
				.toString();
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
