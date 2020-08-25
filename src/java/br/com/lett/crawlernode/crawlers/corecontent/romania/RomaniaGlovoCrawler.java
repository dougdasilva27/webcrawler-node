package br.com.lett.crawlernode.crawlers.corecontent.romania;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RomaniaGlovoCrawler extends Crawler {

   public RomaniaGlovoCrawler(Session session) {
      super(session);
   }

   private final String HOME_PAGE = "https://www.emag.ro/";
   private static final String SELLER_FULL_NAME = "Emag";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   private JSONArray extractAllProductsFromApi() {
      JSONArray products = new JSONArray();

      String urlApi = "https://api.glovoapp.com/v3/stores/52287/addresses/152639/collections/78058245";

      Request request = Request.RequestBuilder.create().setUrl(urlApi).build();
      JSONObject productsJson = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      JSONArray sections = JSONUtils.getJSONArrayValue(productsJson, "sections");

      products = sections;
      return products;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (session.getOriginalURL().contains("product_id=")) {

         JSONArray sections = extractAllProductsFromApi();

         for (Object object : sections) {
            JSONObject sectionsObject = (JSONObject) object;

            if (sectionsObject != null && !sectionsObject.isEmpty()) {

               JSONArray productsArr = JSONUtils.getJSONArrayValue(sectionsObject, "products");

               for (Object p : productsArr) {
                  JSONObject prod = (JSONObject) p;
                  String idFromUrl = session.getOriginalURL().split("product_id=")[1];

                  String internalId = prod.optString("id");

                  if (idFromUrl.equals(internalId)) {

                     String name = prod.optString("name");
                     String primaryImage = prod.optString("imageUrl");
                     Offers offers = scrapOffer(prod);

                     // Creating the product
                     Product product = ProductBuilder.create()
                        .setUrl(session.getOriginalURL())
                        .setInternalId(internalId)
                        .setInternalPid(internalId)
                        .setName(name)
                        .setPrimaryImage(primaryImage)
                        .setOffers(offers)
                        .build();

                     products.add(product);


                  }
               }
            }
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Offers scrapOffer(JSONObject product) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(product);
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

   private Pricing scrapPricing(JSONObject product) throws MalformedPricingException {

      Double spotlightPrice = product.optDouble("price");
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
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
