package br.com.lett.crawlernode.core.models;

import org.bson.Document;



public class RankingDiscoverUrls {

	private int marketId;
	private String url;
	private String internalPid;
	private String internalId;
	private String ect;
	private String lmt;	
	
	public Document getDocument(){
		
		return new Document()
			.append("market", this.marketId)
			.append("ect", this.ect)
			.append("url", this.url)
			.append("lmt", this.lmt)
			.append("internal_id", this.internalId)
			.append("internal_pid", this.internalPid);
		
	}
	
	
	public int getMarketId() {
		return marketId;
	}
	public void setMarketId(int marketId) {
		this.marketId = marketId;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getInternalPid() {
		return internalPid;
	}
	public void setInternalPid(String internalPid) {
		this.internalPid = internalPid;
	}
	public String getInternalId() {
		return internalId;
	}
	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}
	
	
	public String getEct() {
		return ect;
	}


	public void setEct(String ect) {
		this.ect = ect;
	}


	public String getLmt() {
		return lmt;
	}


	public void setLmt(String lmt) {
		this.lmt = lmt;
	}


	@Override
	public boolean equals(Object obj)
	{
		if (getClass() != obj.getClass())
	      return false;
	 
		RankingDiscoverUrls other = (RankingDiscoverUrls) obj;
		
		if (getUrl().equals(other.getUrl())) return true;
		else						   			   return false;
	}
	
	@Override
	public int hashCode()
	{
		return Integer.valueOf(getUrl().charAt(0));
	}
}
