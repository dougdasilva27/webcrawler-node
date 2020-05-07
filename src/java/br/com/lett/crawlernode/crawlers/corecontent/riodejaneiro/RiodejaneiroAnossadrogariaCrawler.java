package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXOldScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;
import models.pricing.Pricing;

public class RiodejaneiroAnossadrogariaCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.anossadrogaria.com.br/";
   private static final List<String> SELLERS = Arrays.asList("A Nossa Drogaria");

   public RiodejaneiroAnossadrogariaCrawler(Session session) {
      super(session);
   }

   @Override
   protected List<String> scrapImages(Document doc, JSONObject skuJson, String internalPid, String internalId) {
      return scrapImagesOldWay(internalId);
   }

   @Override
   protected List<String> scrapSales(Document doc, JSONObject offerJson, String internalId, String internalPid, Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String sale = CrawlerUtils.scrapStringSimpleInfo(doc, ".produto__info--tag > p[class*=leve]", true);
      if (sale != null) {
         sales.add(sale);
      }

      String calculatedSale = CrawlerUtils.calculateSales(pricing);
      if (calculatedSale != null) {
         sales.add("-" + calculatedSale + "%");
      }

      return sales;
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
   protected String scrapDescription(Document doc, JSONObject productJson) {
      return CrawlerUtils.scrapStringSimpleInfo(doc, ".description-wrapper", false);
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }

}
