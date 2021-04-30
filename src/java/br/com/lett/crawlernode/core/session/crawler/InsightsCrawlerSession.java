package br.com.lett.crawlernode.core.session.crawler;

import br.com.lett.crawlernode.core.models.Market;
import br.com.lett.crawlernode.core.server.request.Request;
import br.com.lett.crawlernode.core.session.Session;

public class InsightsCrawlerSession extends Session {

   /**
    * Processed id associated with the sku being crawled
    */
   private Long processedId;

   /**
    * Internal id associated with the sku being crawled
    */
   private String internalId;

   /**
    * Number of readings to prevent a void status
    */
   private int voidAttemptsCounter;


   public InsightsCrawlerSession(Request request, String queueName, Market market) {
      super(request, queueName, market);

      processedId = request.getProcessedId();
      internalId = request.getInternalId();

      this.voidAttemptsCounter = 0;
   }

   @Override
   public Long getProcessedId() {
      return processedId;
   }

   public void setProcessedId(Long processedId) {
      this.processedId = processedId;
   }

   @Override
   public void incrementVoidAttemptsCounter() {
      this.voidAttemptsCounter++;
   }

   @Override
   public int getVoidAttempts() {
      return voidAttemptsCounter;
   }

   @Override
   public String getInternalId() {
      return internalId;
   }

   public void setInternalId(String internalId) {
      this.internalId = internalId;
   }

   public void setVoidAttempts(int voidAttempts) {
      this.voidAttemptsCounter = voidAttempts;
   }

}
