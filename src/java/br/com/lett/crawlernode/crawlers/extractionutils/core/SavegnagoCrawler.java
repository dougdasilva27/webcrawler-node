package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.JSONUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offers;
import models.RatingsReviews;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

   @Override
   protected Offers scrapOffer(Document doc, JSONObject jsonSku, String internalId, String internalPid) throws OfferException, MalformedPricingException {

      if(isAvailable()) {
         return super.scrapOffer(doc, jsonSku, internalId, internalPid);
      }
      else {
         return new Offers();
      }
   }

   private boolean isAvailable() {

      String url = "https://www.savegnago.com.br/api/checkout/pub/orderForms/simulation?sc="+CITY_CODE;

      String payload = "{\"items\":[{\"id\":6921,\"quantity\":1,\"seller\":\"1\"}],\"postalCode\":\""+CEP+"\",\"country\":\"BRA\"}";

      Map<String,String> headers = new HashMap<>();
      headers.put("Content-Type","application/json");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setPayload(payload)
         .setHeaders(headers)
         .build();

      Response response = new FetcherDataFetcher().post(session,request);
      JSONObject jsonObject = new JSONObject(response.getBody());

      String available = JSONUtils.getValueRecursive(jsonObject,"items.0.availability",String.class);

      return available.equals("available");

   }
}
