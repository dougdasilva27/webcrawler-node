package br.com.lett.crawlernode.core.models;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class Categories {

	private int market;
	private String cat1;
	private String cat2;
	private String cat3;
	private String cat1Name;
	private String cat2Name;
	private String cat3Name;
//	private String chave;
	private String url;
	private String dataCreated;
	private String dataUpdated;
	
	public Categories(int market, String cat1, String cat1name, String cat2, String cat2name, String cat3, String cat3name, String url)
	{
		this.setMarket(market);
		this.setCat1(cat1);
		this.setCat1Name(cat1name);
		this.setCat2(cat2);
		this.setCat2Name(cat2name);
		this.setCat3(cat3);
		this.setCat3Name(cat3name);
		this.setUrl(url);
		this.setDataCreated();
		this.setDataUpdated();
//		this.setChave(this.cat1+this.cat2+this.cat3);
	}

	public int getMarket() {
		return market;
	}

	public void setMarket(int market) {
		this.market = market;
	}

	public String getCat2() {
		return cat2;
	}

	public void setCat2(String cat2) {
		this.cat2 = cat2;
	}

	public String getCat1() {
		return cat1;
	}

	public void setCat1(String cat1) {
		this.cat1 = cat1;
	}

	public String getCat3() {
		return cat3;
	}

	public void setCat3(String cat3) {
		this.cat3 = cat3;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getCat1Name() {
		return cat1Name;
	}

	public void setCat1Name(String cat1Name) {
		this.cat1Name = cat1Name;
	}

	public String getCat2Name() {
		return cat2Name;
	}

	public void setCat2Name(String cat2Name) {
		this.cat2Name = cat2Name;
	}

	public String getCat3Name() {
		return cat3Name;
	}

	public void setCat3Name(String cat3Name) {
		this.cat3Name = cat3Name;
	}

	public String getDataCreated() {
		return dataCreated;
	}

	public void setDataCreated() {
		this.dataCreated = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss.SSS");
	}

	public String getDataUpdated() {
		return dataUpdated;
	}

	public void setDataUpdated() {
		this.dataUpdated = new DateTime(DateTimeZone.forID("America/Sao_Paulo")).toString("yyyy-MM-dd HH:mm:ss.SSS");
	}
	
	@Override
	public boolean equals(Object obj) {
		if (getClass() != obj.getClass())
			return false;

		Categories other = (Categories) obj;

		return getChave().equals(other.getChave());
	}
	
	@Override
	public int hashCode()
	{
		return Integer.valueOf(getChave().charAt(0));
	}
	
	@Override
    public String toString() {
    	return "Cats: " + this.cat1 + " - " + this.cat2 + " - " + this.cat3;
    }

	public String getChave() {
		return this.cat1+this.cat2+this.cat3;
	}

}
