package br.com.lett.crawlernode.core.models;

import br.com.lett.crawlernode.core.session.Session;

import java.time.LocalDateTime;

public class RedisMessage {

   private Long marketId;
   private LocalDateTime taskFinish;
   private String sessionId;
   private String internalId;
   private String status;

   private String productStatus;


   public String getProductStatus() {
      return productStatus;
   }

   public void setProductStatus(String productStatus) {
      this.productStatus = productStatus;
   }



   public Long getMarketId() {
      return marketId;
   }

   public void setMarketId(Long marketId) {
      this.marketId = marketId;
   }

   public LocalDateTime getTaskFinish() {
      return taskFinish;
   }

   public void setTaskFinish(LocalDateTime taskFinish) {
      this.taskFinish = taskFinish;
   }

   public String getSessionId() {
      return sessionId;
   }

   public void setSessionId(String sessionId) {
      this.sessionId = sessionId;
   }

   public String getInternalId() {
      return internalId;
   }

   public void setInternalId(String internalId) {
      this.internalId = internalId;
   }

   public String getStatus() {
      return status;
   }

   public void setStatus(String status) {
      this.status = status;
   }

   public static RedisMessage build(Session session,Object obj) {
      RedisMessage message = new RedisMessage();

      message.setTaskFinish(LocalDateTime.now());
      message.setInternalId(session.getInternalId());
      message.setMarketId(session.getMarket().getId());
      message.setStatus("DONE");
      message.setSessionId(session.getSessionId());
      return message;
   }
}
