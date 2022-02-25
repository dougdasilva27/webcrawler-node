package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXOldScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;
import models.pricing.Pricing;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

      if (descriptionArr != null && !descriptionArr.isEmpty()) {
         description = descriptionArr.toString();
      } else {
         description = productJson.optString("description");
      }

      description = decodeHtml(description);

      return description;
   }

   private String decodeHtml(String html) {
      return StringEscapeUtils.unescapeHtml4(html);
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

   @Override
   protected List<String> scrapSales(Document doc, JSONObject offerJson, String internalId, String internalPid, Pricing pricing) {
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      Object teasers = offerJson.optQuery("/commertialOffer/Teasers");
      if (teasers instanceof JSONArray) {
         Object quantity = ((JSONArray) teasers).optQuery("/0/<Conditions>k__BackingField/<MinimumQuantity>k__BackingField");
         if (quantity instanceof Integer) {
            int salesQuantity = (int) quantity;
            String sellerID = offerJson.optString("sellerId", "");
            Double salesPrice = getSalesPrice(quantity, sellerID, internalId);
            if (salesPrice != null && salesPrice > 1) {
               sales.add("Leve " + salesQuantity + " e pague R$ " + salesPrice + " cada");
            }
         }
      }
      return sales;
   }

   private Double getSalesPrice(Object quantity, String sellerID, String internalId) {
      String url = homePage + "api/checkout/pub/orderForms/simulation";
      String payload = "{\"items\":[{\"id\":\"" + internalId + "\",\"quantity\":" + quantity + ",\"seller\":\"" + sellerID + "\"}],\"postalCode\":\"\",\"country\":\"BRA\"}";
      HashMap<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");

      Request request = Request.RequestBuilder.create().setUrl(url).setPayload(payload).setHeaders(headers).setCookies(cookies).build();
      JSONObject response = CrawlerUtils.stringToJSONObject(new FetcherDataFetcher().post(session, request).getBody());
      if (response != null) {
         Object priceInCents = response.optQuery("/items/0/sellingPrice");
         if (priceInCents instanceof Integer) {
            return ((Integer) priceInCents) / 100.0;
         }
      }
      return null;
   }
}
