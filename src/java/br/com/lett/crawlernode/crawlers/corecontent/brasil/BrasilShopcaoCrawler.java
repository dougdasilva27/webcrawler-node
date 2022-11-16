package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
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
import br.com.lett.crawlernode.util.Pair;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class BrasilShopcaoCrawler extends Crawler {

   private static String SELLER_NAME = "Shopcao";
   public Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilShopcaoCrawler(Session session) {
      super(session);
   }

   @Override
   protected Response fetchResponse() {
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setProxyservice(List.of(
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_MX))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(this.dataFetcher, new ApacheDataFetcher(), new FetcherDataFetcher()), session, "get");

      return response;
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      super.extractInformation(document);
      List<Product> products = new ArrayList<>();

      if (isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "#shopify-ali-review", "product-id");
         String description = CrawlerUtils.scrapStringSimpleInfo(document, "#tab_pr_deskl > div.sp-tab-content", false);
         String productName = CrawlerUtils.scrapStringSimpleInfo(document, "#shopify-section-pr_summary > h1", true);
         String primaryImage = "https:" + CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".img_ptw", "data-src");
         List<String> secondaryImages = getSecondaryImages(document);
         RatingsReviews ratingsReviews = ratingsReviews(document);


         Elements variations = document.select("#product-select_ppr > option");

         if (variations.isEmpty()) {

            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "#cart-form_ppr > input[name=id]", "value");
            boolean isAvailable = checkIfIsAvailable(document);
            Offers offers = isAvailable ? scrapOffers(document) : new Offers();

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(productName)
               .setDescription(description)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setRatingReviews(ratingsReviews)
               .setOffers(offers)
               .build();
            products.add(product);

         } else {
            for (Element element : variations) {
               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, null, "value");
               String variationName = element.hasText() ? productName + " " + element.text().split(" ")[0].trim() : productName;
               boolean isAvailable = checkIfIsAvailable(document);
               Offers offers = isAvailable ? scrapOffersVariations(document, internalId) : new Offers();

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(variationName)
                  .setDescription(description)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setRatingReviews(ratingsReviews)
                  .setOffers(offers)
                  .build();
               products.add(product);

            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private boolean checkIfIsAvailable(Document document) {
      String stock = CrawlerUtils.scrapStringSimpleInfo(document, "div.variations_button.in_flex.column.w__100.buy_qv_false > div > button", true);
      return stock != null;
   }

   private boolean isProductPage(Document document) {
      return document.selectFirst("#shopify-section-pr_summary") != null;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Offers scrapOffersVariations(Document doc, String internalId) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricingVariations(doc, internalId);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(new Offer.OfferBuilder()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(this.SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricingVariations(Document doc, String internalId) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".js_sticky_sl [value=\"" + internalId + "\"]", "data-price", false, ',', session);
      if (spotlightPrice != null) {
         spotlightPrice = spotlightPrice / 100;
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#price_ppr", null, false, ',', session);
      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#price_ppr ins", null, false, ',', session);
      }
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#price_ppr del", null, false, ',', session);

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private CreditCards scrapCreditCards(Document doc, Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = scrapInstallments(doc);

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
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

   public Installments scrapInstallments(Document doc, String selector) throws MalformedPricingException {
      Installments installments = new Installments();

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(selector, doc, false);
      if (!pair.isAnyValueNull()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(pair.getFirst())
            .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(((Float) pair.getSecond()).doubleValue()))
            .build());
      }

      return installments;
   }

   public Installments scrapInstallments(Document doc) throws MalformedPricingException {

      Installments installments = scrapInstallments(doc, "#shopify-section-pr_summary > span");
      if (installments != null || installments.getInstallments().isEmpty()) {
         return installments;
      }
      return null;
   }

   private List<String> getSecondaryImages(Document doc) {
      List<String> secondaryImages = new ArrayList<>();

      Elements imagesLi = doc.select("div.p-thumb.p-thumb_ppr.images.sp-pr-gallery.equal_nt.nt_contain.ratio_imgtrue.position_8.nt_slider.pr_carousel > div");
      for (Element imageLi : imagesLi) {
         secondaryImages.add(imageLi.attr("src"));
      }
      if (secondaryImages.size() > 0) {
         secondaryImages.remove(0);
      }
      return secondaryImages;
   }

   private RatingsReviews ratingsReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();

      Integer totalNumOfEvaluations = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, ".jdgm-prev-badge", "data-number-of-reviews", null);
      Double avgRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".jdgm-prev-badge", "data-average-rating", false, '.', session);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

      return ratingReviews;
   }
}
