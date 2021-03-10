package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.session.Session;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.List;

public abstract class SavegnagoCrawler extends VTEXOldScraper {
   private final String HOME_PAGE  = "https://www.savegnago.com.br/";
   private final String SELLER_NAME  = "Savegnago Supermercados";
   private final String CITY_CODE = getCityCode();
   private final String CEP = getCEP();

   protected abstract String getCEP();
   protected abstract String getCityCode();

   public SavegnagoCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList(SELLER_NAME);
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

   @Override
   protected JSONObject crawlProductApi(String internalPid, String parameters) {
      return super.crawlProductApi(internalPid, "&sc=" + CITY_CODE);
   }

   public String handleURLBeforeFetch(String url) {
      return super.handleURLBeforeFetch(url.split("\\?")[0] + "?sc=" + CITY_CODE);
   }



}
