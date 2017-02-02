package br.com.lett.crawlernode.core.models;

public class CategoriesRanking {

	private String cat1;
	private String cat2;
	private String cat3;
	private String url;
	private String cat;
	
	public CategoriesRanking(){
	}

	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getCat1() {
		return cat1;
	}

	public void setCat1(String cat1) {
		this.cat1 = cat1;
	}

	public String getCat2() {
		return cat2;
	}

	public void setCat2(String cat2) {
		this.cat2 = cat2;
	}

	public String getCat3() {
		return cat3;
	}

	public void setCat3(String cat3) {
		this.cat3 = cat3;
	}

	public String getCat() {
		return cat;
	}

	public void setCat() {
		if(this.cat2 == null)
		{
			this.cat = this.cat1;
		}
		else
		{
			if(this.cat3 == null)
			{
				this.cat = this.cat1 + "_" + this.cat2;
			}
			else
			{
				this.cat = this.cat1 + "_" + this.cat2 + "_" + this.cat3;
			}
		}
	}
	
	@Override
    public String toString() {
    	return "Cats: " + this.cat1 + " - " + this.cat2 + " - " + this.cat3;
    }
	
}
