package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedUrlException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ColombiaMerqueolicoresCrawler extends Crawler {

   private final String zoneId = session.getOptions().optString("zoneId");
   private final String store = session.getOptions().optString("store");
   private final String city = session.getOptions().optString("city");
   private final String store_name = session.getOptions().optString("store_name");


   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AMEX.toString());
   private static final String SELLER_FULL_NAME = "Merqueo Colombia";

   public ColombiaMerqueolicoresCrawler(Session session) {
      super(session);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (session.getOriginalURL().contains(store_name)) {
         JSONObject apiJson = scrapApiJson(session.getOriginalURL());
         JSONObject data = new JSONObject();

         if (apiJson.has("data") && !apiJson.isNull("data")) {
            data = apiJson.getJSONObject("data");
         }

         if (isProductPage(data)) {
            Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

            JSONObject attributes = data.optJSONObject("attributes");
            String internalId = String.valueOf(data.optInt("id"));
            String name = attributes.optString("name");
            Offers offers = crawlOffers(attributes);
            String primaryImage = attributes.optString("image_large_url");
            String description = data.optString("description");

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setName(name)
               .setOffers(offers)
               .setPrimaryImage(primaryImage)
               .setDescription(description)
               .build();

            products.add(product);

         } else {
            Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
         }
      } else {
         throw new MalformedUrlException("URL não corresponde ao market");
      }
      return products;

   }

   private Offers crawlOffers(JSONObject attributes) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = crawlpricing(attributes);
      List<String> sales = scrapSales(attributes);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(!attributes.optBoolean("is_marketplace", true))
         .setPricing(pricing)
         .setSales(sales)
         .build());


      return offers;
   }

   private List<String> scrapSales(JSONObject attributes) {
      List<String> sales = new ArrayList<>();
      String sale = attributes.optString("discount_percentage");
      if (sale != null && !sale.isEmpty()) {
         sales.add(sale);
      }
      return sales;
   }


   private JSONObject scrapApiJson(String originalURL) {
      List<String> slugs = scrapSlugs(originalURL);

      StringBuilder apiUrl = new StringBuilder();
      apiUrl.append("https://merqueo.com/api/3.1/stores/" + store + "/department/");

      apiUrl.append(slugs.get(0));
      apiUrl.append("/shelf/").append(slugs.get(1));
      apiUrl.append("/products/").append(slugs.get(2));


      apiUrl.append("?zoneId=");
      apiUrl.append(zoneId);

      Request request = Request.RequestBuilder
         .create()
         .setUrl(apiUrl.toString())
         .mustSendContentEncoding(false)
         .build();

      return CrawlerUtils.stringToJson(new FetcherDataFetcher().get(session, request).getBody());
   }

   /*
    * Url example:
    * https://merqueo.com/cali/nueva-botella-cali/cervezas/cerveza-nacional/cerveza-coronita-botella-210-ml-210-ml
    */
   private List<String> scrapSlugs(String originalURL) {
      List<String> slugs = new ArrayList<>();
      String slugString = CommonMethods.getLast(originalURL.split(city + "/"));
      String[] slug = slugString.contains("/") ? slugString.split("/") : null;

      if (slug != null) {
         Collections.addAll(slugs, slug);
      }
      return slugs;
   }

   private boolean isProductPage(JSONObject data) {
      return data.has("id");
   }


   private Pricing crawlpricing(JSONObject attributes) throws MalformedPricingException {

      Double spotLightprice = (double) attributes.optInt("special_price", 0);
      spotLightprice = spotLightprice == 0d ? null : spotLightprice;
      Double priceFrom = (double) attributes.optInt("price", 0);
      priceFrom = priceFrom == 0d ? null : priceFrom;

      if (spotLightprice == null && priceFrom != null) {
         spotLightprice = priceFrom;
         priceFrom = null;
      }


      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotLightprice)
         .build());


      CreditCards creditCards = new CreditCards();
      for (String s : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setInstallments(installments)
            .setBrand(s)
            .setIsShopCard(false)
            .build());
      }


      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotLightprice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotLightprice)
            .build())
         .build();

   }
}
