package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilAmarosbichosCrawler extends Crawler {
   private static final String MAIN_SELLER_NAME = "Amaro's Bichos";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.DINERS.toString(), Card.AMEX.toString());

   public BrasilAmarosbichosCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[id=hdnProdutoId]", "value");
      JSONObject productObject = getFromApi(internalPid);

      if (isProductPage(doc) && productObject != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".informacao-abas"));
         List<String> categories = CrawlerUtils.crawlCategories(doc, "ol > li > a > span");
         RatingsReviews ratingsReviews = scrapRatingReviews(doc);

         JSONArray variants = JSONUtils.getValueRecursive(productObject, "Variantes", JSONArray.class);
         if (variants != null && !variants.isEmpty()) {
            for (Object o : variants) {

               JSONObject jsonObject = (JSONObject) o;
               String internalId = jsonObject.optString("SKU");
               String name = jsonObject.optString("Nome");
               String primaryImage = JSONUtils.getValueRecursive(jsonObject, "Fotos.0.FotoId", String.class);
               List<String> secondaryImages = getImageListFromScript(jsonObject.optJSONArray("Fotos"));
               boolean available = jsonObject.optBoolean("Disponivel");
               Offers offers = available ? scrapOffers(jsonObject) : new Offers();

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setCategories(categories)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setOffers(offers)
                  .setRatingReviews(ratingsReviews)
                  .build();

               products.add(product);

            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(JSONObject doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      if (pricing != null) {
         offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(MAIN_SELLER_NAME)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject variation) throws MalformedPricingException {
      Double spotlightPrice;
      Double priceFrom;
      JSONObject priceObject = scrapPriceObject(variation.optJSONArray("TabelasPreco"));
      if (priceObject != null && !priceObject.isEmpty()) {
         spotlightPrice = priceObject.optDouble("PrecoPor");
         priceFrom = priceObject.optDouble("PrecoDe");
         if (spotlightPrice != null && priceFrom != null && spotlightPrice.equals(priceFrom)) {
            priceFrom = null;
         }
      } else {
         spotlightPrice = variation.optDouble("PrecoPor");
         priceFrom = variation.optDouble("PrecoDe");
         if (spotlightPrice != null && priceFrom != null && spotlightPrice.equals(priceFrom)) {
            priceFrom = null;
         }
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlipBuilder.create().setFinalPrice(spotlightPrice).setOnPageDiscount(0d).build())
         .build();

   }

   private JSONObject scrapPriceObject(JSONArray tabelasPreco) {
      if (tabelasPreco != null && !tabelasPreco.isEmpty()) {
         for (Object o : tabelasPreco) {
            JSONObject price = (JSONObject) o;
            if (price.optBoolean("Ativo")) {
               return price;
            }
         }
      }
      return null;
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".detalhe-produto") != null;
   }

   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, ".comments-wrapper [itemprop=\"ratingCount\"]", "content", 0);
      Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".comments-wrapper [itemprop=\"ratingValue\"]", "content", false, '.', session);
      Integer totalWrittenReviews = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, ".comments-wrapper [itemprop=\"reviewCount\"]", "content", 0);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".product-comment-list > .customer-comments .stars > .rating-stars");

      for (Element review : reviews) {
         if (review.hasAttr("data-score")) {
            Integer val = Integer.parseInt(review.attr("data-score").replaceAll("[^0-9]+", ""));

            switch (val) {
               case 1:
                  star1 += 1;
                  break;
               case 2:
                  star2 += 1;
                  break;
               case 3:
                  star3 += 1;
                  break;
               case 4:
                  star4 += 1;
                  break;
               case 5:
                  star5 += 1;
                  break;
            }
         }
      }

      return new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();
   }

   private List<String> getSecondaryImages(Document doc) {
      List<String> secondaryImages = new ArrayList<>();

      Elements imagesLi = doc.select(".zoom-thumbnail.elevatezoom-gallery");
      for (Element imageLi : imagesLi) {
         secondaryImages.add(imageLi.attr("data-image"));
      }
      if (secondaryImages.size() > 0) {
         secondaryImages.remove(0);
      }
      return secondaryImages;
   }

   private JSONObject getFromApi(String internalPid) {
      if (internalPid != null && !internalPid.isEmpty()) {
         String url = "https://www.petshopamarosbichos.com.br/produto/api/" + internalPid;
         Request request = Request.RequestBuilder.create()
            .setUrl(url)
            .build();

         Response response = this.dataFetcher.get(session, request);
         String body = response.getBody();
         if (body != null && !body.isEmpty()) {
            return JSONUtils.stringToJson(body);
         }
      }
      return null;
   }

   private List<String> getImageListFromScript(JSONArray secondaryImages) {
      List<String> imagesList = new ArrayList<>();
      if (secondaryImages != null && !secondaryImages.isEmpty()) {
         for (int i = 1; i < secondaryImages.length(); i++) {
            String imageList = JSONUtils.getValueRecursive(secondaryImages, i + ".FotoId", String.class);
            imagesList.add(imageList);
         }
         return imagesList;
      }
      return null;
   }
}

