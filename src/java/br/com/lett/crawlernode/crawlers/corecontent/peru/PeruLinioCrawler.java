package br.com.lett.crawlernode.crawlers.corecontent.peru;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PeruLinioCrawler extends Crawler {
   private static final String SELLER_NAME = "Linio";

   public PeruLinioCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.WEBDRIVER);
   }

   @Override
   protected Object fetch() {
      List<String> proxies = List.of(ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY, ProxyCollection.SMART_PROXY_PE_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY);
      int attemp = 0;
      boolean succes = false;
      Document doc = new Document("");
      do {
         try {
            webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), proxies.get(attemp), session);
            if (webdriver != null) {
               doc = Jsoup.parse(webdriver.getCurrentPageSource());
               succes = !doc.select(".catalogue-product.row").isEmpty();
               webdriver.terminate();
            }
         } catch (Exception e) {
            Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
            Logging.printLogWarn(logger, "Page not captured");
         }
      } while (!succes && attemp++ < proxies.size());
      return doc;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();
      Element product = doc.selectFirst(".wrapper.container-fluid");
      if (product != null) {
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(product, ".feature [itemprop=\"sku\"]", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(product, ".product-name", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(product, ".image-modal", List.of("data-lazy"), "https", "");
         List<String> categories = getCategories(doc, name);
         String description = CrawlerUtils.scrapSimpleDescription(doc, List.of(".panel-body"));
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".product-image.thumb-image .image-wrapper .image", Arrays.asList("data-lazy"), "https", "", primaryImage);
         RatingsReviews ratingsReviews = scrapRatingReviews(product);
         Elements elementsVariations = doc.select(".select-dropdown__list-item");
         Offers offers = new Offers();

         if (elementsVariations.size() > 0) {
            for (int index = 0; index < elementsVariations.size(); index++) {
               Element elementVariation = elementsVariations.get(index);
               Integer stock = CrawlerUtils.scrapIntegerFromHtmlAttr(elementVariation, "option", "data-option-stock", 0);
               String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(elementVariation, "option", "value");
               String nameVariation = CrawlerUtils.scrapStringSimpleInfo(elementVariation, "option", true);
               if (nameVariation != null && !nameVariation.isEmpty() && !nameVariation.contains("-")) {
                  if (internalId == null || internalId.isEmpty()) {
                     internalId = internalPid;
                  }
                  if (stock > 0) {
                     offers = scrapOffers(product, index + 1);
                  }
                  nameVariation = name + " - " + nameVariation;
               } else {
                  nameVariation = name;
                  internalId = internalPid;
               }
               Product newProduct = ProductBuilder.create()
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setUrl(session.getOriginalURL())
                  .setCategories(categories)
                  .setName(nameVariation)
                  .setOffers(offers)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setRatingReviews(ratingsReviews)
                  .build();
               products.add(newProduct);
            }
         } else {
            String agotado = CrawlerUtils.scrapStringSimpleInfo(product, "#buy-now", true);
            if (agotado != null && !agotado.isEmpty() && !agotado.contains("Agotado")) {
               offers = scrapOffers(product, 1);
            }
            Product newProduct = ProductBuilder.create()
               .setInternalId(internalPid)
               .setInternalPid(internalPid)
               .setUrl(session.getOriginalURL())
               .setCategories(categories)
               .setName(name)
               .setOffers(offers)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setRatingReviews(ratingsReviews)
               .build();
            products.add(newProduct);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private List<String> getCategories(Document docProduct, String nameProduct) {
      List<String> categories = CrawlerUtils.crawlCategories(docProduct, ".breadcrumb li", true);
      if (categories.size() > 0) {
         String lastCategory = categories.get(categories.size() - 1);
         if (lastCategory.equals(nameProduct)) {
            categories.remove(lastCategory);
            return categories;
         }
      }
      return categories;
   }

   private RatingsReviews scrapRatingReviews(Element product) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      Integer totalWrittenReviews = getTotalWrittenReviews(CrawlerUtils.scrapStringSimpleInfo(product, ".chart-count", true));
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(product.select(".chart-progress.col-12"));
      Double average = CrawlerUtils.scrapDoublePriceFromHtml(product, ".review-subtitle-label.body-accent-lg.col-2", null, true, '.', session);

      ratingsReviews.setDate(session.getDate());
      ratingsReviews.setTotalRating(totalWrittenReviews);
      ratingsReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingsReviews.setTotalWrittenReviews(totalWrittenReviews);
      ratingsReviews.setAverageOverallRating(average);


      return ratingsReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Elements reviews) {
      if (reviews != null) {
         List<Integer> stars = new ArrayList<>(List.of(0, 0, 0, 0, 0));
         for (Element review : reviews) {
            String numberAvaliationString = CrawlerUtils.scrapStringSimpleInfo(review, ".number-of-reviews", true);
            String startString = CrawlerUtils.scrapStringSimpleInfo(review, "span", true);
            if (numberAvaliationString != null && !numberAvaliationString.isEmpty() && startString != null && !startString.isEmpty()) {
               Integer numberAvaliationInteger = Integer.parseInt(numberAvaliationString);
               Integer starInteger = Integer.parseInt(startString);
               stars.set(starInteger - 1, numberAvaliationInteger);
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

   private Integer getTotalWrittenReviews(String avaliation) {
      Integer totalAvaliations = 0;
      if (avaliation != null && !avaliation.isEmpty()) {
         List<String> numberAvaliation = List.of(avaliation.split(" "));
         if (numberAvaliation.size() > 0) {
            totalAvaliations = Integer.parseInt(numberAvaliation.get(0));
         }
      }
      return totalAvaliations;
   }

   private Offers scrapOffers(Element data, Integer optionIndex) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(data, optionIndex);
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

   private Pricing scrapPricing(Element doc, Integer optionIndex) throws MalformedPricingException {
      Element divVariation = doc.selectFirst(".product-price-container.option-container.option-" + optionIndex);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(divVariation, ".price-main-md", null, true, '.', session);
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(divVariation, ".original-price", null, true, '.', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(price)
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

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
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
