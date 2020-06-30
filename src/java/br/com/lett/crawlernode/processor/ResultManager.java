package br.com.lett.crawlernode.processor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.database.DatabaseManager;
import models.Processed;


public class ResultManager {

   private static final Logger LOGGER = LoggerFactory.getLogger(ResultManager.class);

   private DateFormat isoDateFormat;

   /**
    * Called on crawler main method.
    * 
    * @param activateLogging
    * @param mongo
    * @param db
    * @throws NullPointerException
    */
   public ResultManager(DatabaseManager db) throws NullPointerException {
      this.init();
   }

   /**
    * Result manager initialization.
    * 
    * @param activateLogging
    * @throws NullPointerException
    */
   private void init() throws NullPointerException {
      this.isoDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
      isoDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      // createMarketInfo();
   }

   /**
    * Extract informations from all originals fields on ProcessedModel and then saves the data gattered
    * from the ProcessedModel to be returned.
    * 
    * @author Fabricio
    * @param cm Recebe valores do Crawler e os transfere para o ProcessModel
    * @return pm Retorna processModel com valores do Crawler
    */
   public Processed processProduct(Processed pm, Session session) {
      // preventing extra field to be null
      if (pm.getExtra() == null) {
         pm.setExtra("");
      }
      // update digital content
      return pm;
   }


}
