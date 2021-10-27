package br.com.lett.crawlernode.aws.kinesis;

import br.com.lett.crawlernode.core.models.SkuStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

public class Message implements Serializable {

   private String timestamp;
   private String status;
   private String productStatus;
   private String sessionId;
   private String supplierId;
   private String internalId;
   private String marketId;

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public String getProductStatus() {
      return productStatus;
   }

   public void setProductStatus(String productStatus) {
      this.productStatus = productStatus;
   }

   public String getTimestamp() {
      return timestamp;
   }

   public void setTimestamp(String timestamp) {
      this.timestamp = timestamp;
   }

   public String getSessionId() {
      return sessionId;
   }

   public void setSessionId(String sessionId) {
      this.sessionId = sessionId;
   }

   public String getSupplierId() {
      return supplierId;
   }

   public void setSupplierId(String supplierId) {
      this.supplierId = supplierId;
   }

   public String getInternalId() {
      return internalId;
   }

   public void setInternalId(String internalId) {
      this.internalId = internalId;
   }

   public String getMarketId() {
      return marketId;
   }

   public void setMarketId(String marketId) {
      this.marketId = marketId;
   }

   public static Message build(SkuStatus skuStatus, String sessionId, String internalId, Integer marketId, Long supplierId) {
      Message message = new Message();

      message.setTimestamp(LocalDateTime.now().toString());
      message.setStatus("DONE");
      message.setProductStatus(skuStatus.toString());
      message.setSessionId(sessionId);
      message.setMarketId(marketId.toString());
      message.setInternalId(internalId);
      message.setSupplierId(supplierId != null ? supplierId.toString() : null);
      return message;
   }

   public String serializeToKinesis() {
      Map<String, Object> linkedMap = new LinkedHashMap<>();
      linkedMap.put("sessionId", getSessionId());
      linkedMap.put("status", getStatus());
      linkedMap.put("supplier_id", getSupplierId());
      linkedMap.put("market_id", getMarketId());
      linkedMap.put("internal_id", getInternalId());
      linkedMap.put("productStatus", getProductStatus());
      linkedMap.put("timestamp", getTimestamp());

      return new org.bson.Document(linkedMap).toJson();
   }


}
