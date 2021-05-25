package br.com.lett.crawlernode.crawlers.corecontent.fortaleza;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;

import java.util.*;

public class FortalezaCompremaxCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());
   private static final String SELLER_FULL_NAME = "compremax";

   public FortalezaCompremaxCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }


   public String getAuthorization() {
      String apiURL = "https://www.merconnect.com.br/oauth/token";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json;charset=UTF-8");

      JSONObject payload = new JSONObject();
      payload.put("client_id", "dcdbcf6fdb36412bf96d4b1b4ca8275de57c2076cb9b88e27dc7901e8752cdff");
      payload.put("client_secret", "27c92c098d3f4b91b8cb1a0d98138b43668c89d677b70bed397e6a5e0971257c");
      payload.put("grant_type", "client_credentials");

      Request request = Request.RequestBuilder.create().setUrl(apiURL).setHeaders(headers).setPayload(payload.toString()).build();
      String response = this.dataFetcher.post(session, request).getBody();

      JSONObject reponseJson = CrawlerUtils.stringToJson(response);

      String acessToken = reponseJson.optString("access_token");
      String tokenType = reponseJson.optString("token_type");


      return tokenType + " " + acessToken;
   }


   @Override
   protected JSONObject fetch() {
      JSONObject itemInfo = new JSONObject();

      String url = session.getOriginalURL();

      if (url != null && session.getOriginalURL().contains("produto/")) {
         String productId = CommonMethods.getLast(url.split("produto/")).replaceAll("[^0-9]", "");
         String apiUrl = "https://www.merconnect.com.br/mapp/v2/markets/36/items/" + productId;

         Map<String, String> headers = new HashMap<>();
         headers.put("Authorization", getAuthorization());

         Request request = Request.RequestBuilder.create().setUrl(apiUrl).setHeaders(headers).build();
         String response = this.dataFetcher.get(session, request).getBody();

         JSONObject productInfo = JSONUtils.stringToJson(response);
         itemInfo = JSONUtils.getJSONValue(productInfo, "item");
      }

      return itemInfo;
   }

   @Override
   public List<Product> extractInformation(JSONObject productInfo) throws Exception {
      List<Product> products = new ArrayList<>();

      if (productInfo != null) {
         String internalId = productInfo.optString("id");
         String internalPid = productInfo.optString("mix_id");
         String primaryImage = productInfo.optString("image");
         String name = productInfo.optString("short_description");
         int stock = productInfo.optInt("stock");
         Offers offers = scrapOffer(productInfo);
         Product product =
            ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setStock(stock)
               .setOffers(offers)
               .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());

      }

      return products;
   }


   private Offers scrapOffer(JSONObject productInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productInfo);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
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

   private Pricing scrapPricing(JSONObject productInfo) throws MalformedPricingException {
      Double spotlightPrice = productInfo.optDouble("price");
      Double priceFrom = productInfo.optDouble("original_price");

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
