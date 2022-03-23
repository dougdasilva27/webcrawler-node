package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.eclipse.jetty.util.ajax.JSON;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilShopeeHariboCrawler extends Crawler {
   public BrasilShopeeHariboCrawler(Session session) {
      super(session);
   }

   public List<Product> extractInformation(Document document) throws Exception {
      JSONObject productObj = getProduct(this.session.getOriginalURL());
      List<Product> products = new ArrayList<>();
      if (productObj != null) {
         JSONObject data = productObj.optJSONObject("data");
         String internalId = data.optString("itemid");
         String name = data.optString("name");
         String primaryImage = "https://cf.shopee.com.br/file/" + data.optString("image");
         String description = data.getString("description");
         CategoryCollection categories = scrapCategories(data);
         RatingsReviews ratingsReviews = scrapRatingsAlternativeWay(data);
         Integer stock = data.optInt("other_stock");
         List<String> secondaryImages = scrapSecondaryImages(data, primaryImage);
         Offers offers = stock > 0 ? scrapOffers(data) : new Offers();
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setOffers(offers)
            .setRatingReviews(ratingsReviews)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setCategories(categories)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private RatingsReviews scrapRatingsAlternativeWay(JSONObject data) {
      RatingsReviews ratingReviews = new RatingsReviews();
      JSONObject itemRating = data.optJSONObject("item_rating");
      ratingReviews.setDate(session.getDate());
      JSONArray ratingCount = itemRating.optJSONArray("rating_count");
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(ratingCount);
      //por padrão a posição 0 é o total de avaliações
      ratingReviews.setTotalRating(ratingCount.getInt(0));
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setAverageOverallRating(itemRating.getDouble("rating_star"));


      return ratingReviews;
   }


   private AdvancedRatingReview scrapAdvancedRatingReview(JSONArray data) {
      Integer star1 = (Integer) data.get(1);
      Integer star2 = (Integer) data.get(2);
      Integer star3 = (Integer) data.get(3);
      Integer star4 = (Integer) data.get(4);
      Integer star5 = (Integer) data.get(5);

      return new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();
   }


   private CategoryCollection scrapCategories(JSONObject data) {
      JSONArray arr = data.optJSONArray("categories");
      CategoryCollection categories = new CategoryCollection();
      int cont = 0;
      for (Object obj : arr) {
         JSONObject category = (JSONObject) obj;
         categories.add(category.optString("display_name"));
         cont++;
         if (cont == 3) {
            return categories;
         }
      }

      return null;
   }

   private List<String> scrapSecondaryImages(JSONObject data, String primaryImage) {
      List<String> list = new ArrayList<>();
      JSONArray arr = data.optJSONArray("images");
      for (Integer i = 0; i < arr.length(); i++) {
         String image = "https://cf.shopee.com.br/file/" + arr.get(i);
         if (!image.equals(primaryImage)) {
            list.add(image);
         }
      }

      return list;
   }

   private JSONObject getProduct(String url) {
      JSONObject data = null;
      String[] arr = url.split("\\.");
      String itemId = arr[arr.length - 1];
      String shopId = arr[arr.length - 2];
      String newUrl = "https://shopee.com.br/api/v4/item/get?itemid=" + itemId + "&shopid=" + shopId;
      Request request = Request.RequestBuilder.create()
         .setUrl(newUrl)
         .build();

      Response response = this.dataFetcher.get(session, request);
      if (!response.getBody().isEmpty()) {
         return CrawlerUtils.stringToJson(response.getBody());
      }
      return data;

   }

   private Offers scrapOffers(JSONObject data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(data);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName("shopee-haribo")
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {
      Integer spotlightPriceInt = data.optInt("price");
      Integer priceFromInt = data.getInt("price_min_before_discount");
      Double spotlightPrice = spotlightPriceInt / 100000.0;
      Double priceFrom;
      if (priceFromInt != -1) {
         priceFrom = priceFromInt / 100000.0;
      } else {
         priceFrom = null;
      }

      if (spotlightPrice == null) {
         spotlightPrice = priceFrom;
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
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

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

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
