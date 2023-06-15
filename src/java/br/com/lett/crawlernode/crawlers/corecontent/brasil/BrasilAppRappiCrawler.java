package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import cdjd.com.google.common.net.HttpHeaders;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class BrasilAppRappiCrawler extends Crawler {
   public BrasilAppRappiCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.JSON);
   }

   private static final String sellerFullName = "rappiapp";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString(), Card.AMEX.toString());
   private final String lat = session.getOptions().optString("lat");
   private final String lng = session.getOptions().optString("lng");
   private final String storeId = session.getOptions().optString("storeId");
   private static final String imgUrl = "https://images.rappi.com.br/products/";

   Map<String, String> headers = new HashMap<>();

   @Override
   protected Response fetchResponse() {
      ;
      headers.put("content-type", "application/json; charset=UTF-8");
      headers.put("Host", "services.rappi.com.br");
      headers.put("app-version-name", "7.48.20230420-72418");
      headers.put(HttpHeaders.ACCEPT, "*/*");
      String getId = session.getOriginalURL().split("#").length > 1 ? session.getOriginalURL().split("#")[1] : session.getOriginalURL();
      String productId = getId.split("_").length > 1 ? getId.split("_")[1] : getId;

      String payload = "{\"context\":\"product_detail\",\"stores\":[" + storeId + "],\"offset\":0,\"limit\":1,\"state\":{\"parent_store_type\":\"market\",\"product_id\":\"" + productId + "\",\"sessions\":\"0\",\"is_prime\":\"false\",\"zone_ids\":\"[]\",\"unlimited_shipping\":\"false\",\"lat\":\"" + lat + "\",\"lng\":\"" + lng + "\"}}";

      String url = "https://services.rappi.com.br/api/dynamic/context/content";
      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setPayload(payload)
         .setProxyservice(Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.BUY_HAPROXY))
         .build();

      return CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), false);
   }


   private List<String> getSecondaryImages(JSONObject product) {
      List<String> secondaryImages = new ArrayList<>();
      JSONArray images = JSONUtils.getJSONArrayValue(product, "images");
      for (Integer i = 0; i < JSONUtils.getJSONArrayValue(product, "images").length(); i++) {
         secondaryImages.add(imgUrl + images.optString(i));
      }
      return secondaryImages;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      List<Product> products = new ArrayList<>();
      if (json != null && !json.isEmpty()) {
         JSONObject jsonProduct = JSONUtils.getValueRecursive(json, "data.components.0.resource.product", JSONObject.class, new JSONObject());
         String internalId = jsonProduct.optString("id");
         String name = jsonProduct.optString("name")+" "+jsonProduct.optString("quantity")+jsonProduct.optString("unit_type");
         String category = jsonProduct.optString("category_name");
         String primaryImage = imgUrl + jsonProduct.optString("image");
         List<String> secondaryImage = getSecondaryImages(jsonProduct);
         String description = jsonProduct.optString("description");
         boolean availableToBuy = jsonProduct.optBoolean("in_stock");
         Offers offers = availableToBuy ? scrapOffers(jsonProduct) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOptions().optString("preLink")+internalId)
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategory1(category)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }


   private Offers scrapOffers(JSONObject jsonProduct) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonProduct);
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

   private Pricing scrapPricing(JSONObject jsonProduct) throws MalformedPricingException {
      Double spotlightPrice = jsonProduct.optDouble("price");
      Double priceFrom = jsonProduct.optDouble("real_price");


      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }


   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }


}

