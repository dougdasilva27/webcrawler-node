package br.com.lett.crawlernode.processor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.database.DatabaseManager;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import models.DateConstants;
import models.Processed;


public class ResultManager {

   private static final Logger LOGGER = LoggerFactory.getLogger(ResultManager.class);
   public static final String NOT_VERIFIED = "not-verified";

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
      updateDigitalContent(pm, session);
      return pm;
   }

   /**
    * Evaluates the processed product main and secondary images. Main image evaluation algorithm: <br>
    * Fetch the md5 of the most recent downloaded main image by the image crawler <br>
    * If the md5 is null, we set the image status to no_image <br>
    * If the md5 exists, we get the previous verified md5 from the digital content and compare to the
    * new one <br>
    * If they are equals, there is nothing to be done <br>
    * If they are different, we update the md5 field of the digital content and set the image status to
    * no_image
    * 
    * @param pm
    * @param session
    */
   private void updateDigitalContent(Processed pm, Session session) {
      Logging.printLogDebug(LOGGER, session, "Updating digital content ...");

      // if the processed model doesn't have a digital content
      // we must create an empty one, to be populated
      if (pm.getDigitalContent() == null) {
         pm.setDigitalContent(new JSONObject());
      }

      // analysing images
      // count pics
      // evaluate primary image
      // evaluate secondary images
      JSONObject processedModelDigitalContentPic = new JSONObject();
      try {
         JSONObject processedModelDigitalContent = pm.getDigitalContent();
         if (processedModelDigitalContent != null && processedModelDigitalContent.has("pic")) {
            processedModelDigitalContentPic = pm.getDigitalContent().getJSONObject("pic");
         }
      } catch (Exception e) {
         Logging.printLogError(LOGGER, session, CommonMethods.getStackTraceString(e));
      }

      // count images
      processedModelDigitalContentPic.put("count", DigitalContentAnalyser.imageCount(pm));

      if (pm.getDigitalContent() == null) { // if md5 is null, means that there is no image in Amazon, let's see the previous status
         String nowISO = new DateTime(DateConstants.timeZone).toString("yyyy-MM-dd HH:mm:ss.SSS");

         JSONObject processedModelDigitalContentPicPrimary = new JSONObject();
         processedModelDigitalContentPicPrimary.put("status", NOT_VERIFIED);
         processedModelDigitalContentPicPrimary.put("verified_by", "crawler_" + nowISO);

         processedModelDigitalContentPic.put("primary", processedModelDigitalContentPicPrimary);
      }

      // set pic on digital content
      pm.getDigitalContent().put("pic", processedModelDigitalContentPic);
   }
}
