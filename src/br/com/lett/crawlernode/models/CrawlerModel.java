
package br.com.lett.crawlernode.models;

import java.util.List;

/**
 * Processor model - Crawler
 * 
 * @author samirleao
 *
 */
public class CrawlerModel {

	// Table fields
	private Integer id;
	private String 	internalId;
	private String 	name;
	private Float 	price;
	private Integer market;
	private String 	pic;
	private String 	cat1;
	private String 	cat2;
	private String 	cat3;
	private String 	url;
	private String 	description;

	// Temporary
	private List<String> preprocessedName;

	public CrawlerModel(Integer id, String internalId, String name, Float price, String pic, String url, String cat1, String cat2, String cat3, Integer market, String description) {
		super();
		this.id = id;
		this.internalId = internalId;
		this.name = name;
		this.price = price;
		this.market = market;
		this.pic = pic;
		this.cat1 = cat1;
		this.cat2 = cat2;
		this.cat3 = cat3;
		this.url = url;
		this.description = description;
	}


	@Override
	public String toString() {
		return "CrawlerModel [id=" + id + ", internalId=" + internalId
				+ ", name=" + name + ", price=" + price + ", market=" + market + ", pic=" + pic
				+ ", cat1=" + cat1 + ", cat2=" + cat2 + ", cat3=" + cat3
				+ ", url=" + url + ", description=" + description
				+ ", preprocessedName=" + preprocessedName + "]";
	}

	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getInternalId() {
		return this.internalId;
	}
	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}

	public String getName() {
		return this.name;
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

	public Integer getMarket() {
		return this.market;
	}

	public void setMarket(Integer market) {
		this.market = market;
	}

	public List<String> getPreprocessedName() {
		return this.preprocessedName;
	}
	
	public void setPreprocessedName(List<String> preprocessedName) {
		this.preprocessedName = preprocessedName;
	}

	public String getPic() {
		return this.pic;
	}

	public void setPic(String pic) {
		this.pic = pic;
	}

	public String getCat1() {
		return this.cat1;
	}

	public void setCat1(String cat1) {
		this.cat1 = cat1;
	}

	public String getCat2() {
		return this.cat2;
	}
	public void setCat2(String cat2) {
		this.cat2 = cat2;
	}

	public String getCat3() {
		return this.cat3;
	}

	public void setCat3(String cat3) {
		this.cat3 = cat3;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
