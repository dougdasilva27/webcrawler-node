package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.*;
import models.prices.Prices;
import models.pricing.BankSlip;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilFarmadeliveryCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.farmadelivery.com.br/";
   private Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.DINERS.toString());

   public BrasilFarmadeliveryCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {
      Request request = Request.RequestBuilder.create().setUrl(session.getOriginalURL()).setProxyservice(
         Arrays.asList(
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY
         )
      ).build();

      Response response = dataFetcher.get(session, request);
      String content = response.getBody();

      int statusCode = response.getLastStatusCode();

      if ((Integer.toString(statusCode).charAt(0) != '2' &&
         Integer.toString(statusCode).charAt(0) != '3'
         && statusCode != 404)) {
         request.setProxyServices(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY));

         content = new JsoupDataFetcher().get(session, request).getBody();
      }

      return Jsoup.parse(content);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(this.session.getOriginalURL(), doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=product]", "value");
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".title-new .sku_info", true);
         if (internalPid != null && !internalPid.isEmpty()) {
            internalPid = internalPid.replaceAll("[^0-9]", "");
         }

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "div.title-new h1", true);
         List<String> categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs ul li", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".inner-container-productimage img", List.of("data-zoom-image"), "https", "farmadelivery.com.br");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".showproductimage", List.of("data-zoom-image"), "https", "farmadelivery.com.br", primaryImage);
         String description = scrapDescription(doc, internalId);
         List<String> eans = crawlEan(doc);
         RatingsReviews ratingReviews = crawRating(doc);

         boolean available = doc.selectFirst("p.alert-stock") == null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .setEans(eans)
            .setRatingReviews(ratingReviews)
            .build();
         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing, doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("Farma Delivery Brasil")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private List<String> scrapSales(Pricing pricing, Document doc) {
      List<String> sales = new ArrayList<>();
      if (pricing != null) {
         String discount = CrawlerUtils.calculateSales(pricing);
         if (!discount.isEmpty()) {
            sales.add(discount);
         }
      }

      String kitSales = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-pricing .tier-price", false);
      if (kitSales != null && !kitSales.isEmpty()) {
         sales.add(kitSales);
      }
      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-shop .price-box .special-price .price", null, true, ',', session);
      if(spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-shop .price-box .regular-price .price", null, true, ',', session);
      }

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-shop .price-box .old-price span[id]", null, true, ',', session);
      Double bankSlipPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".pagamento .boleto > span", null, true, ',', session);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create().setFinalPrice(bankSlipPrice).build();
      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }


   private boolean isProductPage(String url, Document doc) {
      Element elementProduct = doc.select("div.product-view").first();
      return elementProduct != null && !url.contains("/review/");
   }

   private List<String> crawlEan(Document doc) {
      List<String> eans = new ArrayList<>();
      String ean = CrawlerUtils.scrapStringSimpleInfo(doc, ".box-collateral.box-additional .codigo_barras .data", true);

      if (ean != null && !ean.isEmpty()) {
         eans.add(ean);
      }

      return eans;
   }

   private String scrapDescription(Document doc, String internalId) {
      StringBuilder description = new StringBuilder();

      Element elementDescription = doc.selectFirst("div.product-collateral .box-description");
      if (elementDescription != null) {
         description.append(elementDescription.html());
      }

      Element elementAdditional = doc.selectFirst("div.product-collateral .box-additional");
      if (elementAdditional != null) {
         description.append(elementAdditional.html());
      }

      description.append(CrawlerUtils.scrapLettHtml(internalId, session, session.getMarket().getNumber()));

      return description.toString();
   }

   RatingsReviews crawRating(Document document) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalNumOfRatings(document);
      Double avgRating = getTotalAvgRating(document, totalNumOfEvaluations);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(document);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Double getTotalAvgRating(Document doc, Integer ratingCount) {
      Double avgRating = 0d;

      if (ratingCount > 0) {
         Element avg = doc.select(".rating-box-product .rating-box .rating").first();

         if (avg != null) {
            Double percentage = MathUtils.normalizeTwoDecimalPlaces(Double.parseDouble(avg.attr("style").replaceAll("[^0-9.]", "").trim()));

            if (percentage != null) {
               avgRating = MathUtils.normalizeTwoDecimalPlaces(5 * (percentage / 100d));
            }
         }
      }

      return avgRating;
   }

   private Integer getTotalNumOfRatings(Document docRating) {
      Integer totalRating = 0;
      Element totalRatingElement = docRating.select(".rating-box-product .amount a").first();

      if (totalRatingElement != null) {
         String text = totalRatingElement.toString();
         totalRating = MathUtils.parseInt(text);

      }

      return totalRating;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select("#customer-reviews .review-item .ratings-table .rating-box");

      for (Element review : reviews) {

         Element elementStarNumber = review.selectFirst(".rating");

         if (elementStarNumber != null) {

            String stringStarNumber = elementStarNumber.attr("style");
            Integer numberOfStars = MathUtils.parseInt(stringStarNumber);

            switch (numberOfStars) {
               case 100:
                  star5 += 1;
                  break;
               case 80:
                  star4 += 1;
                  break;
               case 60:
                  star3 += 1;
                  break;
               case 40:
                  star2 += 1;
                  break;
               case 20:
                  star1 += 1;
                  break;
               default:
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

}
