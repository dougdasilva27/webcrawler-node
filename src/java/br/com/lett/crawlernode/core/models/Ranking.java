package br.com.lett.crawlernode.core.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Ranking {

   protected int marketId;
   protected List<RankingProducts> products = new ArrayList<>();
   protected String location;
   protected String rankType;
   protected Timestamp date;
   protected String lmt;
   protected RankingStatistics statistics = new RankingStatistics();

   public int getMarketId() {
      return marketId;
   }

   public void setMarketId(int marketId) {
      this.marketId = marketId;
   }


   public String getLocation() {
      return location;
   }


   public void setLocation(String location) {
      this.location = location;
   }


   public String getRankType() {
      return rankType;
   }


   public void setRankType(String rankType) {
      this.rankType = rankType;
   }

   public List<RankingProducts> getProducts() {
      return products;
   }

   public void setProducts(List<RankingProducts> products) {
      this.products = products;
   }

   public Timestamp getDate() {
      return date;
   }

   public void setDate(Timestamp date) {
      this.date = date;
   }

   public String getLmt() {
      return lmt;
   }

   public void setLmt(String lmt) {
      this.lmt = lmt;
   }

   public RankingStatistics getStatistics() {
      return statistics;
   }

   public void setStatistics(RankingStatistics statistics) {
      this.statistics = statistics;
   }

}
