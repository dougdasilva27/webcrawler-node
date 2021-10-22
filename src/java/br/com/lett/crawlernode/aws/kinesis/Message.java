package br.com.lett.crawlernode.aws.kinesis;

import br.com.lett.crawlernode.core.models.SkuStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

public class Message implements Serializable {

   private String taskFinish;
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

   public String getTaskFinish() {
      return taskFinish;
   }

   public void setTaskFinish(String scheduled) {
      this.taskFinish = scheduled;
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

      message.setTaskFinish(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).toString());
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
      linkedMap.put("productStatus", getProductStatus());
      linkedMap.put("taskFinish", getTaskFinish());

      return new org.bson.Document(linkedMap).toJson();
   }


}
