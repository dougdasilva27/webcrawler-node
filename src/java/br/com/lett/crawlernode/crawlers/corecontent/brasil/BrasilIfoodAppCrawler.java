package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class BrasilIfoodAppCrawler extends Crawler {
   public BrasilIfoodAppCrawler(Session session) {
      super(session);
      config.setParser(Parser.JSON);
   }

   private static final String baseImageUrl = "https://static.ifood-static.com.br/image/upload/t_medium/pratos/";

   private final String merchant_id = session.getOptions().optString("merchant_id");
   private final String access_key = session.getOptions().optString("access_key");
   private final String secret_key = session.getOptions().optString("secret_key");

   private final String state = session.getOptions().optString("state");

   private static final String sellerFullName = "ifood app";

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      headers.put("host", "wsloja.ifood.com.br");
      headers.put("channel", "IFOOD");
      headers.put("user-agent", "okhttp/4.10.0");
      headers.put("app_package_name", "br.com.brainweb.ifood");
      headers.put("authority", "marketplace.ifood.com.br");
      headers.put("sec-fetch-dest", "empty");
      headers.put("sec-fetch-mode", "cors");
      headers.put("state", state);
      headers.put("access_key", access_key);
      headers.put("secret_key", secret_key);
      String url = "https://wsloja.ifood.com.br/ifood-ws-v3/restaurants/" + merchant_id + "/menuitem/" + session.getOriginalURL();
      Request request = Request.RequestBuilder.create()
         .setHeaders(headers)
         .setUrl(url)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.SMART_PROXY_BR,
            ProxyCollection.SMART_PROXY_BR_HAPROXY
         ))
         .build();
      return CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), true);
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();
      if (json != null && !json.isEmpty() && json.has("data")) {
         Logging.printLogDebug(logger, session, "Item app identified: " + this.session.getOriginalURL());
         JSONObject item = JSONUtils.getValueRecursive(json, "data.menu.0.itens.0", JSONObject.class, new JSONObject());
         if (!item.isEmpty()) {
            String internalId = item.optString("id");
            String name = item.optString("description");
            String description = item.optString("additionalInfo");
            String ean = item.optString("ean");
            List<String> images = scrapImages(item);
            String primaryImage = images.size() > 0 ? images.remove(0) : null;
            String availability = item.optString("availability", "");
            Offers offers = availability.equals("AVAILABLE") ? scrapOffers(item) : new Offers();

            Product product = ProductBuilder.create()
               .setUrl(internalId)
               .setInternalId(internalId)
               .setInternalPid(internalId)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setEans(List.of(ean))
               .setDescription(description)
               .setOffers(offers)
               .build();
            products.add(product);

         }
      } else {
         String msg = json != null && !json.isEmpty() ? json.optString("message") : null;
         String messageError = msg != null ? "Not a item app " + this.session.getOriginalURL() + msg : "Not a item app " + this.session.getOriginalURL();
         Logging.printLogDebug(logger, session, messageError);
      }
      return products;
   }

   private List<String> scrapImages(JSONObject json) {
      JSONArray suffixesImages = json.optJSONArray("logosUrls");
      List<String> images = new ArrayList<>();
      if (suffixesImages != null && !suffixesImages.isEmpty()) {
         for (Object s : suffixesImages) {
            String suffixImage = (String) s;
            images.add(baseImageUrl + suffixImage);
         }
      }
      return images;
   }

   private Offers scrapOffers(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(sellerFullName)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double priceFrom = json.optDouble("unitPrice");
      Double spotlightPrice = json.optDouble("unitMinPrice");
      if (spotlightPrice.isNaN()) {
         spotlightPrice = priceFrom;
         priceFrom = null;
      }
      if (priceFrom != null && priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }
      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .build();
   }
}
