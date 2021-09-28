package br.com.lett.crawlernode.core.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

public class RankingProduct {

	protected String internalPid;
	protected int position;
	protected String url;

   //New fields that must captured in ranking
   protected Boolean isSponsored = false;
   protected Boolean isAvailable;
   protected Integer priceInCents;
   protected int marketId;
   protected int pageNumber = 0;
   protected String internalId;
   protected String name;
   protected String imageUrl;
   protected String keyword;
   protected Timestamp timestamp;

	protected List<Long> processedIds = new ArrayList<>();
	protected String screenshot;
	
	
	public Document getDocument(){
		
		return new Document()
			.append("position", this.position)
			//.append("url", this.url)
			.append("processed_ids", this.processedIds);
	}
	
	public String getInteranlPid() {
		return internalPid;
	}
	
	public void setInteranlPid(String interanlPid) {
		this.internalPid = interanlPid;
	}
	
	public int getPosition() {
		return position;
	}
	
	public void setPosition(int position) {
		this.position = position;
	}
	
	public List<Long> getProcessedIds() {
		return processedIds;
	}
	
	public void addProcessedId(String x){
		
	}
	
	public void setProcessedIds(List<Long> processedIds) {
		this.processedIds = processedIds;
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getScreenshot() {
		return screenshot;
	}

	public void setScreenshot(String screenshot) {
		this.screenshot = screenshot;
	}

   public Boolean getIsSponsored() {
      return isSponsored;
   }

   public void setIsSponsored(Boolean isSponsored) {
      this.isSponsored = isSponsored;
   }

   public int getPriceInCents() {
      return priceInCents;
   }

   public void setPriceInCents(Integer priceInCents) {
      this.priceInCents = priceInCents;
   }

   public Boolean getIsAvailable() {
      return isAvailable;
   }

   public void setIsAvailable(Boolean isAvailable) {
      this.isAvailable = isAvailable;
   }

   public int getPageNumber() {
      return pageNumber;
   }

   public void setPageNumber(int pageNumber) {
      this.pageNumber = pageNumber;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public int getMarketId() {
      return marketId;
   }

   public void setMarketId(int marketId) {
      this.marketId = marketId;
   }

   public String getImageUrl() {
      return imageUrl;
   }

   public void setImageUrl(String imageUrl) {
      this.imageUrl = imageUrl;
   }

   public String getKeyword() {
      return keyword;
   }

   public void setKeyword(String keyword) {
      this.keyword = keyword;
   }

   public String getInternalId() {
      return internalId;
   }

   public void setInternalId(String internalId) {
      this.internalId = internalId;
   }

   public Timestamp getTimestamp() {
      return timestamp;
   }

   public void setTimestamp(Timestamp timestamp) {
      this.timestamp = timestamp;
   }

   @Override
   public String toString() {
      return "Position " + position + " - " +
         "internalPid=" + internalPid +
         ", internalId=" + internalId +
         ", name=" + name +
         ", url=" + url +
         ", isSponsored=" + isSponsored +
         ", priceInCents=" + priceInCents +
         ", isAvailable=" + isAvailable +
         ", pageNumber=" + pageNumber +
         ", imageUrl=" + imageUrl;
   }
}
