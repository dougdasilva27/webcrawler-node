package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * @author gabriel date: 2018-05-25
 */
public class BrasilDrogalCrawler extends Crawler {

   public BrasilDrogalCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.drogal.com.br/";
   private static final String SELLER = "Drogal";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#content_product [data-sku]", "data-sku");
         String name = crawlName(doc);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".big-image a img", Arrays.asList("src"), "https", "io2.convertiez.com.br");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, "#sly_carousel li:not(.active) a", Arrays.asList("big_img"), "https", "io2.convertiez.com.br", primaryImage);
         String description = crawlDescription(doc);
         RatingsReviews ratingsReviews = scrapRatingAndReviews(doc, internalId);
         boolean available = doc.selectFirst(".box-buttons .bt-big.bt-checkout") != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setRatingReviews(ratingsReviews)
            .setName(name)
            .setOffers(offers)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }


   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(Document doc) {
      return doc.select(".code span[itemprop=sku]").first() != null;
   }


   /**
    * We extract internalId on script like this:
    * <p>
    * Key: productId
    * <p>
    * In that case below, internalId will be "kityenzahshleave"
    * <p>
    * ;var dataLayer=dataLayer||[];dataLayer.push({device:"d"})
    * dataLayer.push({pageName:"product",productId:"kityenzahshleave",productName:"Kit Yenzah Sou+
    * Cachos Shampoo Lowpoo 240ml + Leave In Suave
    * 365ml",productPrice:"63.24",productDepartment:"CUIDADOS COM CABELOS",productCategory:"CUIDADOS
    * COM CABELOS",productSubCategory:"KIT CABELOS",productBrand:"Yenzah"});
    *
    * @return
    */
   private String crawlInternalId(Document document) {
      String internalId = null;
      JSONObject dataLayer = new JSONObject();

      String firstIndexString = "dataLayer.push(";
      String lastIndexString = ");";
      Elements scripts = document.select("script[type=\"text/javascript\"]");

      for (Element e : scripts) {
         String html = e.html().replace(" ", "");

         if (html.contains(firstIndexString) && html.contains(lastIndexString)) {
            String script = CrawlerUtils.extractSpecificStringFromScript(html, firstIndexString, true, lastIndexString, false);

            if (script.startsWith("{") && script.endsWith("}")) {
               try {
                  dataLayer = new JSONObject(script);
               } catch (JSONException e1) {
                  Logging.printLogError(logger, session, CommonMethods.getStackTrace(e1));
               }
            }

            break;
         }
      }

      if (dataLayer.has("productId") && !dataLayer.isNull("productId")) {
         internalId = dataLayer.get("productId").toString();
      }

      return internalId;
   }

   private String crawlName(Document document) {
      String name = null;
      Element nameElement = document.select("h1.name").first();

      if (nameElement != null) {
         name = nameElement.ownText().trim();
      }

      return name;
   }


   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select("#breadcrumb li:not(.home) > a");

      for (Element e : elementCategories) {
         String cat = e.text().trim();

         if (!cat.isEmpty()) {
            categories.add(cat);
         }
      }

      return categories;
   }

   private String crawlDescription(Document document) {
      StringBuilder description = new StringBuilder();
      Element descriptionElement = document.select(".container .float > .center:not(.product) > .row").first();

      if (descriptionElement != null) {
         description.append(descriptionElement.html());
      }

      Element modal = document.select(".modal-content .modal-body").last();
      if (modal != null) {
         description.append(modal.html());
      }

      return description.toString();
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing, doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER)
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
      String sale = CrawlerUtils.calculateSales(pricing);
      if (sale != null) {
         sales.add(sale);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price_off span", null, false, ',', session);
      Double priceCard = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".sale .sale_price", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(priceCard, doc);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-boleto.get_price_boleto", null, false, ',', session);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }


   private CreditCards scrapCreditCards(Double priceCard, Document doc) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      Integer installment = CrawlerUtils.scrapIntegerFromHtml(doc, ".get_min_installments", true, 0);
      Double value = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".get_card_price", null, true, ',', session);

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(priceCard)
         .build());

      if (installment != 0 && value != null) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installment)
            .setInstallmentPrice(value)
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

   private RatingsReviews scrapRatingAndReviews(Document document, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      Integer totalNumOfEvaluations = getTotalNumOfRatings(document);
      Double avgRating = getTotalAvgRating(document, totalNumOfEvaluations);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(document);


      ratingReviews.setDate(session.getDate());
      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      // For this website the rating is always belong with a comment
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      return ratingReviews;
   }


   private Double getTotalAvgRating(Document doc, Integer ratingCount) {
      double avgRating = 0D;

      if (ratingCount > 0) {
         Element avg = doc.select("[itemprop=aggregateRating] .rating-star [itemprop=ratingValue]").first();

         if (avg != null) {
            String text = avg.ownText().replaceAll("[^0-9.]", "").replace(",", ".").trim();

            if (!text.isEmpty()) {
               avgRating = Double.parseDouble(text);
            }
         }
      }

      return avgRating;
   }

   private Document fetchAdvancedRating(int page) {
      StringBuilder url = new StringBuilder(this.session.getOriginalURL()).append("?p=").append(page);
      Request request = Request.RequestBuilder.create().setUrl(url.toString()).build();
      return Jsoup.parse(dataFetcher.get(session, request).getBody());
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document document) {

      // Select all 'li' with no class, this selector get all elements page (1, 2, 3...) of rating
      // pagination
      Element paginationExists = document.select(".pagination").first();

      Logging.printLogDebug(logger, session, "Will run rating");
      // The size of ratingElementsPage is the number of pages
      int totalPages = 1;
      int pageIterator = 1;

      if (paginationExists != null) {
         Elements ratingElementsPage = document.select(".pagination>ul>li:not([class])");
         totalPages = ratingElementsPage.size();
      }
      ;


      Map<Integer, Integer> starsCount = new HashMap<>();

      Document currentDocument = document;
      while (pageIterator <= totalPages) {
         Logging.printLogDebug(logger, session, "Will run rating " + totalPages);
         if (pageIterator > 1) {
            currentDocument = fetchAdvancedRating(pageIterator);
         }

         Elements ratingComments = currentDocument.select("div#ratings div.float span.rating-star>span");

         ratingComments.forEach(element -> {
            Integer ratingValue = Integer.parseInt(element.html().trim());
            if (ratingValue > 0 && ratingValue <= 5) {
               Integer count = starsCount.getOrDefault(ratingValue, 0) + 1;
               starsCount.put(ratingValue, count);
            } else {
               Logging.printLogError(logger, session, "rating error: rating star error");
            }
         });

         pageIterator++;
      }

      return new AdvancedRatingReview.Builder()
         .allStars(starsCount)
         .build();
   }

   /**
    * Number of ratings appear in html
    */
   private Integer getTotalNumOfRatings(Document docRating) {
      int totalRating = 0;
      Element totalRatingElement = docRating.select("[itemprop=aggregateRating] [itemprop=reviewCount]").first();

      if (totalRatingElement != null) {
         String text = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            totalRating = Integer.parseInt(text);
         }
      }

      return totalRating;
   }
}
