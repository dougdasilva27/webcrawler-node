package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BrasilMadridSupermercados extends Crawler {
   public BrasilMadridSupermercados(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.madrid.com.br/";
   private static final String SELLER_NAME = "madridsupermercados";

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();
      super.extractInformation(doc);
      Element product = doc.selectFirst(".pdp");
      if (product != null) {
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#ctl00_ContentPlaceHolder1_h_pfid", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(product, ".detalheProduto > h1", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(product, "#ImgProdDesk > a img", List.of("src"), "", "");
         List<String> categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb a", true);
         String description = CrawlerUtils.scrapSimpleDescription(doc, List.of(".desc p"));
         Offers offers = scrapOffers(product);
         RatingsReviews ratingsReviews = scrapRatingReviews(product);
         Product newProduct = ProductBuilder.create()
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setUrl(session.getOriginalURL())
            .setName(name)
            .setOffers(new Offers())
            .setPrimaryImage(primaryImage)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .setRatingReviews(ratingsReviews)
            .build();
         products.add(newProduct);
      } else {

         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private RatingsReviews scrapRatingReviews(Element product) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      String avaliation = CrawlerUtils.scrapStringSimpleInfo(product, ".flex.aic.avaliacao", true);
      String imageStar = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "#ctl00_ContentPlaceHolder1_img_avaliacao", "src");
      Elements comments = product.select("#bboxComentarios > ul li");
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(comments);
      Double average = getStar(imageStar) * 1.0;

      ratingsReviews.setDate(session.getDate());
      ratingsReviews.setTotalRating(getTotalAvaliations(avaliation));
      ratingsReviews.setDate(session.getDate());
      ratingsReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingsReviews.setTotalWrittenReviews(comments.size());
      ratingsReviews.setAverageOverallRating(average);


      return ratingsReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Elements comments) {
      if (comments != null) {
         List<Integer> stars = new ArrayList<>(List.of(0, 0, 0, 0, 0));
         for (Element comment : comments) {
            String imageAvaliation = CrawlerUtils.scrapStringSimpleInfoByAttribute(comment, "img", "src");
            if (imageAvaliation != null && !imageAvaliation.isEmpty()) {
               Integer star = getStar(imageAvaliation);
               Integer indexStar = star - 1;
               if (star > 0) {
                  Integer avaliations = stars.get(indexStar);
                  avaliations++;
                  stars.set(indexStar, avaliations);
               }
            }
         }

         return new AdvancedRatingReview.Builder()
            .totalStar1(stars.get(0))
            .totalStar2(stars.get(1))
            .totalStar3(stars.get(2))
            .totalStar4(stars.get(3))
            .totalStar5(stars.get(4))
            .build();
      }
      return null;
   }

   private Integer getStar(String imageStar) {
      if (imageStar != null && !imageStar.isEmpty()) {
         String avaliation = CommonMethods.getLast(imageStar.split("/"));
         if (!avaliation.isEmpty()) {
            for (Integer i = 0; i < 6; i++) {
               String numberString = String.valueOf(i);
               if (avaliation.contains(numberString)) {
                  return Integer.parseInt(numberString);
               }
            }
         }
      }
      return 0;
   }

   private Integer getTotalAvaliations(String avaliation) {
      Integer totalAvaliations = 0;
      if (avaliation != null && !avaliation.isEmpty()) {
         String numberAvaliation = avaliation.substring(1, 2);
         totalAvaliations = Integer.parseInt(numberAvaliation);
      }
      return totalAvaliations;
   }

   private Offers scrapOffers(Element product) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();
      Pricing pricing = scrapPricing(product);
      sales.add(CrawlerUtils.calculateSales(pricing));
      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());
      return offers;
   }

   private Pricing scrapPricing(Element product) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(product, ".valor", null, true, ',', session);
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(product, ".de s", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();
      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(price)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.MASTERCARD.toString(), Card.VISA.toString(),
         Card.AMEX.toString(), Card.DINERS.toString());

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
