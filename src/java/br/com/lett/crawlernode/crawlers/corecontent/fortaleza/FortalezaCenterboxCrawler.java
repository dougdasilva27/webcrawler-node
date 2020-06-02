package br.com.lett.crawlernode.crawlers.corecontent.fortaleza;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

public class FortalezaCenterboxCrawler extends Crawler {

   private static final String HOME_PAGE = "https://loja.centerbox.com.br/loja/58/";
   private static final String CEP = "60192-105";
   private static final String SELLER_FULL_NAME = "centerbox";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public FortalezaCenterboxCrawler(Session session) {
      super(session);

   }

   private String getToken() {
      String token = null;

      String apiAdress = "https://loja.centerbox.com.br/static/js/main.89e6b0f2.chunk.js";

      Request request = RequestBuilder.create().setUrl(apiAdress).setCookies(cookies).build();
      String content = this.dataFetcher.get(session, request).getBody();

      if (content != null) {

         Integer indexOf = content.indexOf("Auth-Token\":\"");

         if (indexOf != null) {
            Integer lastIndexOf = content.lastIndexOf("\"},$");
            if (lastIndexOf != null) {
               String buildJson = "{\"" + content.substring(indexOf, lastIndexOf) + "\"}";

               JSONObject jsonToken = CrawlerUtils.stringToJson(buildJson);

               token = jsonToken.optString("Auth-Token");

            }
         }
      }

      return token;
   }


   @Override
   protected Object fetch() {
      JSONObject api = new JSONObject();

      String token = getToken();

      Map<String, String> headers = new HashMap<>();
      headers.put("Auth-Token", token);
      headers.put("Connection", "keep-alive");

      String url = "https://www.merconnect.com.br/api/v4/markets?cep=" + CEP + "&market_codename=centerbox";

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
      String content = this.dataFetcher.get(session, request).getBody();

      api = CrawlerUtils.stringToJson(content);

      return api;
   }


   private String getMarketID(JSONObject apiResponse) {

      String marketId = null;

      JSONArray markets = JSONUtils.getJSONArrayValue(apiResponse, "markets");

      if (markets != null) {
         for (Object arr : markets) {

            JSONObject jsonM = (JSONObject) arr;

            marketId = jsonM.optString("id");

         }
      }

      return marketId;
   }


   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(JSONObject api) throws Exception {
      super.extractInformation(api);
      List<Product> products = new ArrayList<>();

      if (session.getOriginalURL().contains("id=")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String marketId = getMarketID(api);

         // Build the URL
         String[] tokens = session.getOriginalURL().split("id=");
         String url = "https://www.merconnect.com.br/api/v2/markets/" + marketId + "/items/search?query=" + tokens[tokens.length - 1];
         // make a request in a new URL
         Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
         String content = this.dataFetcher.get(session, request).getBody();
         // Get a JSONArray
         JSONObject rawJsonWithAllContent = CrawlerUtils.stringToJson(content);
         JSONArray arr = JSONUtils.getJSONArrayValue(rawJsonWithAllContent, "mixes");

         for (Object arrayOfArrays : arr) {

            if (arrayOfArrays != null) {

               JSONArray array = (JSONArray) arrayOfArrays;

               for (Object productJsonInfo : array) {

                  JSONObject jsonInfo = (JSONObject) productJsonInfo;

                  /*
                   * Now, we have to compare the product's bar_code with the bar_code that is passed as a parameter in
                   * the URL. The reason for this is that in some cases, JSONArray can return an array of arrays
                   * (here's an example:https://www.merconnect.com.br/api/v2/markets/58/items/search?query=10381) and
                   * all products have a bar_code, we have to make sure that we are getting the right product.
                   */

                  if (jsonInfo != null && jsonInfo.opt("bar_code").equals(tokens[tokens.length - 1])) {

                     String internalId = jsonInfo.optString("id");
                     String internalPid = jsonInfo.optString("mix_id");
                     String name = jsonInfo.optString("description");
                     Integer stock = jsonInfo.optInt("stock");
                     boolean available = stock != null && stock > 0;
                     String primaryImage = jsonInfo.optString("image");
                     String description = jsonInfo.optString("short_description");
                     Offers offers = available ? scrapOffers(jsonInfo) : new Offers();

                     // Creating the product
                     Product product = ProductBuilder.create()
                           .setUrl(session.getOriginalURL())
                           .setInternalId(internalId)
                           .setInternalPid(internalPid)
                           .setName(name)
                           .setPrimaryImage(primaryImage)
                           .setDescription(description)
                           .setStock(stock)
                           .setOffers(offers)
                           .build();

                     products.add(product);


                  }
               }
            }
         }
      } else

      {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }


   private Offers scrapOffers(JSONObject jsonInfo) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(jsonInfo);
      List<String> sales = new ArrayList<>();

      offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());
      return offers;
   }


   private Pricing scrapPricing(JSONObject jsonInfo) throws MalformedPricingException {
      Double spotlightPrice = jsonInfo.optDouble("price");
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();

   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }

}
