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

   public static Message build(SkuStatus skuStatus, String sessionId) {
      Message message = new Message();

      message.setTaskFinish(LocalDateTime.now().toString());
      message.setStatus("DONE");
      message.setProductStatus(skuStatus.toString());
      message.setSessionId(sessionId);
      return message;
   }

   public String serializeToKinesis() {
      Map<String, Object> linkedMap = new LinkedHashMap<>();

      linkedMap.put("status", getStatus());
      linkedMap.put("taskFinish", getTaskFinish());
      linkedMap.put("productStatus", getProductStatus());
      linkedMap.put("sessionId", getSessionId());

      return new org.bson.Document(linkedMap).toJson();
   }


}
