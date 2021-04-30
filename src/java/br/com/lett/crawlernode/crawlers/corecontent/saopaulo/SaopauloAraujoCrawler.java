package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.util.JSONUtils;
import models.RatingsReviews;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

public class SaopauloAraujoCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.araujo.com.br/";
   private static final List<String> SELLERS = Arrays.asList("araujo");

   public SaopauloAraujoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) throws UnsupportedEncodingException {
      String description = "";
      JSONArray descriptionArr = productJson.optJSONArray("Saiba Mais");

      if(descriptionArr != null && !descriptionArr.isEmpty()){
         description = descriptionArr.toString();
      }else{
         description = productJson.optString("description");
      }

      return description;
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return SELLERS;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "78444", logger);
      return trustVox.extractRatingAndReviews(internalPid, doc, dataFetcher);
   }

}
