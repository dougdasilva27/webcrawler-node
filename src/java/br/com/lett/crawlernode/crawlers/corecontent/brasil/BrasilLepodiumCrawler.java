package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXOldScraper;
import models.RatingsReviews;

public class BrasilLepodiumCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.lepodium.com.br/";
   private static final String MAIN_SELLER_NAME = "Lepodium";


   public BrasilLepodiumCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return Arrays.asList(MAIN_SELLER_NAME);
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) {
      Element descriptionElement = doc.selectFirst(".productDescription");
      Element infoDescriptionElement = doc.selectFirst(".box.specification");

      StringBuilder description = new StringBuilder();

      if (descriptionElement != null) {
         description.append(descriptionElement.text());
      }

      if (infoDescriptionElement != null) {
         description.append(infoDescriptionElement.text());
      }

      return description.toString();
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}

