package br.com.lett.crawlernode.core.server.request;

import br.com.lett.crawlernode.core.models.Market;
import org.json.JSONObject;

public class Request {

   private String requestMethod;

   // common to all tasks
   private String messageId;
   private String parameter;
   private String scraperType;
   private Market market;
   private Long supplierId;
   private String className;
   private JSONObject options;
   private String sessionId;

   private String internalId;
   private Long processedId;
   private String queueName;

   private boolean useBrowser;

   private boolean isVoid;

   private String fileS3Miranha;
   public Request() {
      super();
   }

   public String getMessageId() {
      return messageId;
   }

   public void setMessageId(String messageId) {
      this.messageId = messageId;
   }


   public String getFileS3Miranha() {
      return fileS3Miranha;
   }

   public void setFileS3Miranha(String fileS3Miranha) {
      this.fileS3Miranha = fileS3Miranha;
   }

   public String getParameter() {
      return parameter;
   }

   public void setParameter(String parameter) {
      this.parameter = parameter.trim();
   }

   public String getInternalId() {
      return internalId;
   }

   public void setInternalId(String internalId) {
      this.internalId = internalId;
   }

   public Long getProcessedId() {
      return processedId;
   }

   public void setProcessedId(Long processedId) {
      this.processedId = processedId;
   }

   public String getRequestMethod() {
      return requestMethod;
   }

   public void setRequestMethod(String requestMethod) {
      this.requestMethod = requestMethod;
   }

   public Market getMarket() {
      return market;
   }

   public void setMarket(Market market) {
      this.market = market;
   }

   public String getScraperType() {
      return scraperType;
   }

   public void setScraperType(String scraperType) {
      this.scraperType = scraperType;
   }

   public String getQueueName() {
      return queueName;
   }

   public void setQueueName(String queueName) {
      this.queueName = queueName;
   }

   public Long getSupplierId() {
      return supplierId;
   }

   public void setSupplierId(Long supplierId) {
      this.supplierId = supplierId;
   }

   public String getSessionId() {
      return sessionId;
   }

   public void setSessionId(String sessionId) {
      this.sessionId = sessionId;
   }

   public boolean isUseBrowser() {
      return useBrowser;
   }

   public void setUseBrowser(boolean useBrowser) {
      this.useBrowser = useBrowser;
   }

   @Override
   public String toString() {
      return "Request[sessionId= " + sessionId + ", messageId=" + messageId + ", fileS3Miranha=" + fileS3Miranha + ",messageBody=" + parameter + ", scraperType=" + scraperType + ", requestMethod=" + requestMethod
            + ", internalId=" + internalId + ", processedId=" + processedId + ", marketId=" + market + ", queueName=" + queueName
            + ", supplierId=" + supplierId + ", useBrowser=" + useBrowser + ", isVoid=" + isVoid + "]";
   }

   public String getClassName() {
      return className;
   }

   public void setClassName(String className) {
      this.className = className;
   }

   public JSONObject getOptions() {
      return options;
   }

   public void setOptions(JSONObject options) {
      this.options = options;
   }

   public boolean isVoid() {
      return isVoid;
   }

   public void setVoid(boolean aVoid) {
      isVoid = aVoid;
   }
}
