package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import exceptions.MalformedPricingException;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Iterator;

public class BrasilRappiCrawler extends RappiCrawler {

   private static final String HOME_PAGE = "https://www.rappi.com";

   public BrasilRappiCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   protected String getHomeDomain() {
      return "rappi.com.br";
   }

   @Override
   protected String getImagePrefix() {
      return "images.rappi.com.br/products";
   }

   @Override
   protected String getUrlPrefix() {
      return "produto/";
   }

   @Override
   protected String getHomeCountry() {
      return "https://www.rappi.com.br/";
   }

   @Override
   protected String getMarketBaseUrl() {
      return "https://www.rappi.com.br/lojas/";
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   protected JSONObject getProductRankingJson(JSONObject productJson) {
      String productName = productJson.optString("name");
      String productNameEncoded = productName != null ? StringUtils.stripAccents(productName.replace("%", "")).replace(" ", "%20") : null;
      String url = getMarketBaseUrl() + getStoreId() + "/s?term=" + productNameEncoded;
      Request request = Request.RequestBuilder.create()
         .setCookies(this.cookies)
         .setUrl(url)
         .setFollowRedirects(true)
         .build();

      Response response = CrawlerUtils.retryRequest(request, session, new FetcherDataFetcher(), true);

      Document docRanking = Jsoup.parse(response.getBody());
      JSONObject rankingPageJson = CrawlerUtils.selectJsonFromHtml(docRanking, "#__NEXT_DATA__", null, null, false, false);
      JSONArray searchProducts = JSONUtils.getValueRecursive(rankingPageJson, "props.pageProps.products", JSONArray.class, new JSONArray());

      if (searchProducts.isEmpty()) {
         JSONObject fallback = JSONUtils.getValueRecursive(rankingPageJson, "props.pageProps.fallback", JSONObject.class, new JSONObject());
         if (!fallback.isEmpty()) {
            Iterator<String> keys = fallback.keys();
            while(keys.hasNext()) {
               String key = keys.next();
               if (fallback.get(key) instanceof JSONObject) {
                  searchProducts = JSONUtils.getValueRecursive(fallback.get(key), "products", JSONArray.class, new JSONArray());
               }
            }
         }
      }

      if (searchProducts.length() > 0) {
         for (int i = 0; i < searchProducts.length(); i++) {
            JSONObject searchProduct = searchProducts.getJSONObject(i);
            boolean productEquals = checkProductEquals(productJson, searchProduct);
            if (productEquals) {
               return searchProduct;
            }
         }
      }

      return null;
   }

   @Override
   public Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      JSONObject productRankingJson = getProductRankingJson(productJson);
      Double priceFrom = productRankingJson != null ? JSONUtils.getDoubleValueFromJSON(productRankingJson, "real_price", true) : null;
      Double price = productRankingJson != null ? JSONUtils.getDoubleValueFromJSON(productRankingJson, "balance_price", true) : null;

      if (price == null || price.equals(priceFrom)) {
         price = priceFrom;
         priceFrom = null;
      }
      CreditCards creditCards = scrapCreditCards(price);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }
}
