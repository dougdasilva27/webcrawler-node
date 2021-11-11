package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

import java.util.*;

import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.prices.Prices;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

//the first crawler make with github copilot
public class BrasilCobasiCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.cobasi.com.br/";
   private static final String SELLER_FULL_NAME = "cobasi";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public BrasilCobasiCrawler(Session session) {
      super(session);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {

      List<Product> products = new ArrayList<>();
      JSONObject pageJson = CrawlerUtils.selectJsonFromHtml(doc, "#__NEXT_DATA__", null, null, true, false);


      if ((pageJson != null && !pageJson.isEmpty()) && pageJson.query("/props/pageProps/productDetail") != null) {


         JSONObject productsObj = (JSONObject) pageJson.query("/props/pageProps/productDetail");
         JSONArray variants = productsObj.optJSONArray("activeSkus");
         CategoryCollection categoryCollection = scrapeCategory(productsObj);
         List<String> images = scrapeImages(productsObj);

         for (Object o : variants) {

            JSONObject variant = (JSONObject) o;
            Offers offers = scrapOffer(productsObj, variant);


            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(productsObj.optString("id"))
               .setInternalPid(productsObj.optString("id"))
               .setName(CommonMethods.camelcaseToText(productsObj.optString("name")) + variant.optString("name"))
               .setCategories(categoryCollection)
               .setPrimaryImage(images.remove(0))
               .setSecondaryImages(images)
               .setDescription(productsObj.optString("description"))
               .setOffers(offers)
               .setEans(Collections.singletonList(variant.optString("ean")))
               .build();

            products.add(product);

         }
      }
      else {
         Logging.printLogDebug(logger, "No products page");
      }
      return products;
   }
   private Offers scrapOffer(JSONObject jsonObject, JSONObject variation) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      if (variation.optBoolean("available")) {

         Pricing pricing = scrapPricing(variation);

         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());

      }
      return offers;

   }


   private Pricing scrapPricing(JSONObject jsonObject) throws MalformedPricingException {
      Double spotlightPrice = jsonObject.optDouble("bestPrice");
      Double priceFrom = jsonObject.optDouble("price");

      if (spotlightPrice == null && priceFrom != null) {
         priceFrom = priceFrom / 100;
         spotlightPrice = priceFrom;
         priceFrom = null;
      } else if (spotlightPrice.equals(priceFrom)) {
         priceFrom = null;
         spotlightPrice = spotlightPrice / 100;
      }

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

   private List<String> scrapeImages(JSONObject productsObj) {
      List<String> images = new ArrayList<>();
      JSONArray imagesArray = productsObj.optJSONArray("imagesAndVideos");
      if (imagesArray != null) {
         for (int i = 0; i < imagesArray.length(); i++) {
            images.add(imagesArray.optJSONObject(i).optString("imageUrl"));
         }
      }
      return images;
   }

   private CategoryCollection scrapeCategory(JSONObject jsonObject) {
      CategoryCollection categoryCollection = new CategoryCollection();
      String categories = (String) jsonObject.query("/categories/0");
      categoryCollection.addAll(Arrays.asList(categories));
      return categoryCollection;
   }


}
