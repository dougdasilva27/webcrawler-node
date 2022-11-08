package br.com.lett.crawlernode.aws.kinesis;

import br.com.lett.crawlernode.core.models.Ranking;
import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.session.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.List;

public class RankingModel {
   private Timestamp timestamp;
   private Long keyWordId;
   private Integer marketId;
   private Integer pageSize;
   private Integer productFound;
   private String keyWord;
   private Integer productCaptured;

   JSONArray productGrid;

   private RankingModel() {
   }

   public RankingModel(Ranking ranking) {
      this.timestamp = ranking.getDate();
      this.keyWord = ranking.getLocation();
      this.keyWordId = ranking.getLocationId();
      this.marketId = ranking.getMarketId();
      this.pageSize = ranking.getStatistics().getPageSize();
      this.productFound = ranking.getStatistics().getTotalSearch();
      this.productCaptured = ranking.getStatistics().getTotalFetched();
      this.productGrid = createProductGrid(ranking.getProducts());
   }

   private JSONArray createProductGrid(List<RankingProduct> products) {
      JSONArray productGrid = new JSONArray();
      for (RankingProduct product : products) {
         productGrid.put(product.toJson());
      }
      return productGrid;
   }

   public String serializeToKinesis() {
      return this.toJson().toString();
   }

   public String serializeToKinesis(Session session) {
      JSONObject json = this.toJson();
      json.put("session_id", session.getSessionId());
      return json.toString();
   }

   //generate a json object from the object
   public JSONObject toJson() {
      JSONObject json = new JSONObject();
      json.put("timestamp", timestamp);
      json.put("search_keyword_value", keyWord);
      json.put("search_keyword_id", keyWordId);
      json.put("market_id", marketId);
      json.put("page_size", pageSize);
      json.put("total_products_found", productFound);
      json.put("total_products_captured", productCaptured);
      json.put("products_grid", productGrid);
      return json;
   }

   public Timestamp getTimestamp() {
      return timestamp;
   }

   public Long getKeyWordId() {
      return keyWordId;
   }

   public Integer getMarketId() {
      return marketId;
   }

   public Integer getPageSize() {
      return pageSize;
   }

   public Integer getProductFound() {
      return productFound;
   }

   public String getKeyWord() {
      return keyWord;
   }

   public Integer getProductCaptured() {
      return productCaptured;
   }

   public JSONArray getProductGrid() {
      return productGrid;
   }
}
