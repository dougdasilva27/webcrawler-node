package br.com.lett.crawlernode.crawlers.extractionutils.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;

public abstract class BrasilIfood extends Crawler {

   protected String region = getRegion();
   protected String store_name = getStore_name();
   protected String seller_full_name = getSellerFullName();

   protected abstract String getRegion();

   protected abstract String getStore_name();

   protected abstract String getSellerFullName();

   public BrasilIfood(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }


   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   @Override
   protected JSONObject fetch() {
      String storeCode = CommonMethods.getLast(session.getOriginalURL().split("/")).split("\\?")[0];

      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.ACCEPT, "application/json, text/plain, */*");
      headers.put("platform", "Desktop");
      headers.put("app_version", "8.31.0");
      headers.put("access_key", "69f181d5-0046-4221-b7b2-deef62bd60d5");
      headers.put("secret_key", "9ef4fb4f-7a1d-4e0d-a9b1-9b82873297d8");
      headers.put(HttpHeaders.USER_AGENT, "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36");

      Request request = RequestBuilder.create()
            .setUrl("https://wsloja.ifood.com.br/ifood-ws-v3/restaurants/" + storeCode + "/menu")
            .setCookies(this.cookies)
            .setHeaders(headers)
            .mustSendContentEncoding(false)
            .setFetcheroptions(
                  FetcherOptionsBuilder.create()
                        .mustUseMovingAverage(false)
                        .mustRetrieveStatistics(true)
                        .setForbiddenCssSelector("#px-captcha")
                        .build()
            ).setProxyservice(
                  Arrays.asList(
                        ProxyCollection.INFATICA_RESIDENTIAL_BR,
                        ProxyCollection.BUY,
                        ProxyCollection.NETNUT_RESIDENTIAL_BR
                  )
            ).build();

      String content = this.dataFetcher.get(session, request).getBody();

      if (content == null || content.isEmpty()) {
         content = new ApacheDataFetcher().get(session, request).getBody();
      }

      return JSONUtils.stringToJson(content);
   }


   @Override
   public List<Product> extractInformation(JSONObject apiJson) throws Exception {
      super.extractInformation(apiJson);
      List<Product> products = new ArrayList<>();

      if (!apiJson.isEmpty()) {

         JSONObject data = JSONUtils.getJSONValue(apiJson, "data");
         JSONArray menu = JSONUtils.getJSONArrayValue(data, "menu");

         if (menu != null && !menu.isEmpty() && session.getOriginalURL().contains("prato=")) {
            String productId = CommonMethods.getLast(session.getOriginalURL().split("prato="));

            for (Object menuArr : menu) {

               JSONObject menuObject = (JSONObject) menuArr;
               JSONArray itens = JSONUtils.getJSONArrayValue(menuObject, "itens");

               for (Object itensArr : itens) {

                  JSONObject itensObject = (JSONObject) itensArr;

                  String internalId = itensObject.optString("code");

                  if (internalId.equals(productId)) {

                     String name = itensObject.optString("description");
                     String available = itensObject.optString("availability");
                     boolean availableToBuy = available.equalsIgnoreCase("AVAILABLE");
                     String url = "https://static-images.ifood.com.br/image/upload/t_medium/pratos/";
                     String primaryImage = url + itensObject.optString("logoUrl");
                     String description = itensObject.optString("details");
                     Offers offers = availableToBuy ? scrapOffer(itensObject) : new Offers();

                     // Creating the product
                     Product product = ProductBuilder.create()
                           .setUrl(session.getOriginalURL())
                           .setInternalId(internalId)
                           .setInternalPid(internalId)
                           .setName(name)
                           .setPrimaryImage(primaryImage)
                           .setDescription(description)
                           .setOffers(offers)
                           .build();

                     products.add(product);

                     Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
                  }
               }
            }
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }



   private Offers scrapOffer(JSONObject jsonOffers) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonOffers);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(seller_full_name)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());

      return offers;

   }

   private Pricing scrapPricing(JSONObject jsonOffers) throws MalformedPricingException {

      Double priceFrom = jsonOffers.has("unitOriginalPrice") ? jsonOffers.optDouble("unitOriginalPrice") : null;
      Double spotlightPrice = jsonOffers.optDouble("unitPrice");
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .build();


   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

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
