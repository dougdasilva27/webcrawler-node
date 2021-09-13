package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilThebeautyboxCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilThebeautyboxCrawler(Session session) {
      super(session);
   }

   private static final String MAIN_SELLER_NAME_LOWER = "the beauty box brasil";

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         String internalPid = scrapInternalPid(doc);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "ol.breadcrumb > li.breadcrumb-item");
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, "div.product-description-content.reset-allow-bold", true);
         RatingsReviews ratingsReviews = scrapRating(doc, internalPid);

         Elements items = doc.select("div.product-group-items.row > a");

         if (items != null && !items.isEmpty()) {
            for (Element e : items) {
               String url = e.attr("href");
               Document variantPage = url != null ? fetchPage(url) : null;

               String internalId = scrapInternalId(e);
               String name = e.attr("title");
               List<String> images = scrapImages(variantPage);
               String primaryImage = !images.isEmpty() ? images.remove(0) : null;

               boolean available = doc.selectFirst("button.isnt-marketable.js-notify-me") == null;
               Offers offers = available ? scrapOffers(variantPage) : new Offers();

               ratingsReviews.setInternalId(internalId);
               ratingsReviews.setDate(session.getDate());
               ratingsReviews.setUrl(session.getOriginalURL());

               // Creating the product
               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setRatingReviews(ratingsReviews.clone())
                  .setOffers(offers)
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(images)
                  .setDescription(description)
                  .build();

               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".container.nproduct-page") != null;
   }

   private String scrapInternalPid(Document doc) {
      String internalPid = "";
      String jsonStr = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".container.nproduct-page", "data-layer");

      if (jsonStr != null) {
         JSONObject json = CrawlerUtils.stringToJson(jsonStr);

         internalPid = json.optString("parentId");
      }

      return internalPid;
   }

   private String scrapInternalId(Element el) {
      String internalId = "";
      String jsonStr = el.attr("data-interaction");

      if (jsonStr != null) {
         JSONObject json = CrawlerUtils.stringToJson(jsonStr);

         String values = json.optString("values");

         if (values != null) {
            internalId = values.replace("sku;", "");
         }
      }

      return internalId;
   }

   private List<String> scrapImages(Document variantPage) {
      List<String> images = new ArrayList<>();

      if (variantPage != null) {
         String jsonStr = CrawlerUtils.scrapStringSimpleInfoByAttribute(variantPage, "div.js-carousel-simple.owl-carousel.product-images-carousel", "data-images");

         if (jsonStr != null) {
            JSONArray jsonArr = CrawlerUtils.stringToJsonArray(jsonStr);

            for (Object o : jsonArr) {
               JSONObject json = (JSONObject) o;
               images.add(json.optString("extraLarge"));
            }
         }
      }

      return images;
   }

   private Document fetchPage(String url) {
      Request requestPage = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      Response pageResponse = this.dataFetcher.get(session, requestPage);

      return Jsoup.parse(pageResponse.getBody());
   }

   private RatingsReviews scrapRating(Document doc, String internalPid) {
      RatingsReviews ratingReviews = new RatingsReviews();

         Integer totalRatings = CrawlerUtils.scrapIntegerFromHtml(doc, ".rating-count", true, 0);
         Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".rating-value-container", null, true, '.', session);

         ratingReviews.setAdvancedRatingReview(scrapAdvancedRating(internalPid));
         ratingReviews.setTotalRating(totalRatings);
         ratingReviews.setAverageOverallRating(avgRating);
         ratingReviews.setTotalWrittenReviews(totalRatings);


      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRating(String internalPid) {
      int page = 1;
      int star1 = 0;
      int star2 = 0;
      int star3 = 0;
      int star4 = 0;
      int star5 = 0;

      String url = "https://www.beautybox.com.br/api/htmls/reviews/" + internalPid + "?pagina=" + page + "&size=50&skus=undefined";
      boolean hasNextPage = true;

      while (hasNextPage) {
         Document ratingsDoc = fetchPage(url);

         if (ratingsDoc != null) {
            Elements reviews = ratingsDoc.select("article.review");

            for (Element review : reviews) {

               String stars = CrawlerUtils.scrapStringSimpleInfoByAttribute(review, "img.star", "alt");

               if (stars != null) {
                  Integer star = Integer.parseInt(stars.replace("review ", "").replace(".0", ""));

                  switch (star) {
                     case 5:
                        star5 += 1;
                        break;
                     case 4:
                        star4 += 1;
                        break;
                     case 3:
                        star3 += 1;
                        break;
                     case 2:
                        star2 += 1;
                        break;
                     case 1:
                        star1 += 1;
                        break;
                     default:
                        break;
                  }
               }
            }
            Element nextPage = ratingsDoc.selectFirst("a.js-load-more.reviews-load-more");

            if(nextPage != null){
               page++;
               url = "https://www.beautybox.com.br/api/htmls/reviews/" + internalPid + "?pagina=" + page + "&size=50&skus=undefined";
            }else{
               hasNextPage = false;
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

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(MAIN_SELLER_NAME_LOWER)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());
      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.nproduct-price-value", "content", true, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.nproduct-price-max", null, true, '.', session);

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Pair<Integer, Float> installment = CrawlerUtils.crawlSimpleInstallment(".nproduct-price-installments", doc, false);
      Integer installmentNumber = installment.getFirst();
      Double installmentPrice = installment.getSecond() != null ? installment.getSecond().doubleValue() : null;

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installmentNumber != null ? installmentNumber : 1)
            .setInstallmentPrice(installmentPrice != null ? installmentPrice : spotlightPrice)
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

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String saleDiscount = CrawlerUtils.calculateSales(pricing);
      if (saleDiscount != null) {
         sales.add(saleDiscount);
      }

      return sales;
   }
}
