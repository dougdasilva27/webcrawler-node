package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

import java.util.*;

import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.*;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ColombiaLarebajaCrawler extends Crawler {

   private static final String MAIN_SELLER_NAME = "la-rebaja-colombia";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public ColombiaLarebajaCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalPid = crawlInternalId(doc);
         String name = crawlName(doc);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#gallery img", Arrays.asList("src"), "https:", "www.larebajavirtual.com");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".ad-thumb-list li a img", Arrays.asList("src"), "https:", "www.larebajavirtual.com", primaryImage);
         RatingsReviews ratingReviews = crawRating(doc);

         Elements offersElement = doc.select("div.descripciones");
         boolean hasMultipleOffers = false;

         if (offersElement.size() > 1 && offersElement.hasClass("fraccionado_columns")) {
            offersElement = offersElement.select("tr > td");
            hasMultipleOffers = true;
         }

         for (Element sku : offersElement) {
            String internalId = internalPid;

            if (hasMultipleOffers) {
               internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(sku, "input.increment", "id").replace("cantidad-", "");
               String variationName = CrawlerUtils.scrapStringSimpleInfo(sku, "div.sep-dashed label span", true);

               if (variationName != null) {
                  name += name.contains(variationName) ? "" : " - " + variationName;
               }
            }

            Offers offers = scrapOffers(sku, hasMultipleOffers);

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setOffers(offers)
               .setRatingReviews(ratingReviews)
               .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String crawlName(Document doc) {
      String name = null;
      Element nameEl = doc.selectFirst(".descripciones h1");

      if (nameEl != null) {
         name = nameEl.ownText().trim();
      }
      return name;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".product_detail").isEmpty();
   }

   private String crawlInternalId(Document doc) {
      String internalId = null;

      Element serchedId = doc.selectFirst(".control_cant_detalle input[data-producto], .detPproduct input[data-producto]");
      if (serchedId != null) {
         internalId = serchedId.attr("data-producto").trim();
      }

      // I have to do this on below, because in this url:
      // "https://larebajavirtual.com/catalogo/producto/producto/125967/GATSY-PESCADO-ARROZ-Y-ESPINACA.html"
      // there is no save place to extract internalId, unless on head description "Código: 2216515"
      if (internalId == null) {
         Elements descripciones = doc.select(".descripciones > div > div > span");

         for (Element element : descripciones) {
            String text = element.ownText().toLowerCase().trim();

            // The text appear like this: "Código: 58461"
            // i used text "digo" to identify because if the site remove accent
            // this condition will work
            if (text.contains("digo:")) {
               internalId = CommonMethods.getLast(text.split(":")).trim();

               break;
            }
         }
      }

      return internalId;
   }

   public static CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select(".breadcrumb li + li");

      for (Element e : elementCategories) {
         categories.add(e.text().replace(">", "").trim());
      }

      Element lastCategory = document.selectFirst(".breadcrumb active");
      if (lastCategory != null) {
         categories.add(lastCategory.ownText().trim());
      }

      return categories;
   }

   private RatingsReviews crawRating(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      if (doc.selectFirst(".content-resena p") == null) {
         return null;
      }

      Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);
      Double avgRating = getTotalAvgRating(doc);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);


      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Integer getTotalNumOfRatings(Document doc) {
      int totalNumOfRatings = 0;
      Element pResenha = doc.selectFirst(".content-resena p");

      if (pResenha != null) {
         String resenha = pResenha.text().replaceAll("[^0-9]", "").trim();
         totalNumOfRatings = Integer.parseInt(resenha);
      }

      return totalNumOfRatings;
   }

   private Double getTotalAvgRating(Document doc) {
      Double avgRating = 0.0;
      Element rating = doc.selectFirst("div[data-score]");

      if (rating != null) {
         String score = rating.attr("data-score");
         avgRating = !score.isEmpty() ? Double.parseDouble(score.trim()) : null;
      }

      return avgRating;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".col-md-12 .col-md-2 .col-md-12 .row:first-child div");

      for (Element review : reviews) {
         if (review != null && review.hasAttr("data-score")) {

            String percentageString = review.attr("data-score").replaceAll("[^0-9]+", "");

            Integer val = !percentageString.isEmpty() ? Integer.parseInt(percentageString) : 0;
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

   private Offers scrapOffers(Element doc, boolean hasMultipleOffers) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = hasMultipleOffers ? scrapPricingMultipleOffers(doc) : scrapPricingDefault(doc);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(MAIN_SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(true)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricingMultipleOffers(Element doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.ahora", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.antes.strike", null, false, ',', session);

      if (priceFrom != null && priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
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

   private Pricing scrapPricingDefault(Element doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.pricened", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.strike2", null, false, ',', session);

      if (spotlightPrice == null || spotlightPrice.equals(0d)) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.subtotal", null, false, ',', session);
         priceFrom = null;
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
