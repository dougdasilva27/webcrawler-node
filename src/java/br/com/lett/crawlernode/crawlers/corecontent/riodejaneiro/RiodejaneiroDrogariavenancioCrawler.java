package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RiodejaneiroDrogariavenancioCrawler extends VTEXNewScraper {
   public RiodejaneiroDrogariavenancioCrawler(Session session) {
      super(session);
   }

   @Override
   protected String getHomePage() {
      return session.getOptions().optString("homePage");
   }

   @Override
   protected List<String> getMainSellersNames() {
      return List.of("venancio produtos farmaceuticos ltda");
   }

   @Override
   protected List<String> scrapSales(Document doc, JSONObject offerJson, String internalId, String internalPid, Pricing pricing) {
      List<String> sales = new ArrayList<>();
      if (pricing != null) sales.add(CrawlerUtils.calculateSales(pricing));

      Object teasers = offerJson.optQuery("/commertialOffer/Teasers");

      if (teasers instanceof JSONArray) {
         Object teaser = ((JSONArray) teasers).optQuery("/0/<Name>k__BackingField");
         if (teaser instanceof String) {
            sales.add((String) teaser);
         }
      }

      return sales;
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) throws UnsupportedEncodingException {
      JSONArray descriptions = productJson.optJSONArray("Descrição");
      if (descriptions != null) {
         StringBuilder description = new StringBuilder();
         IntStream.range(0, descriptions.length()).forEach(i -> description.append(descriptions.optString(i)).append("\n"));
         return description.toString();
      }
      return null;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

}
