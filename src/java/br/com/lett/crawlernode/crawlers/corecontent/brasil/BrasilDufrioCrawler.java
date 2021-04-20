package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/*****************************************************************************************************************************
 * Crawling notes (12/07/2016):
 *
 * 1) For this crawler, we have one url per each sku. 2) There is no stock information for skus in
 * this ecommerce by the time this crawler was made. 3) There is no marketplace in this ecommerce by
 * the time this crawler was made. 4) The sku page identification is done simply looking for an
 * specific html element. 5) if the sku is unavailable, it's price is not displayed. Yet the crawler
 * tries to crawl the price. 6) There is no internalPid for skus in this ecommerce. The internalPid
 * must be a number that is the same for all the variations of a given sku. 7) We have one method
 * for each type of information for a sku (please carry on with this pattern).
 *
 * Examples: ex1 (available):
 * https://www.dufrio.com.br/ar-condicionado-frio-split-cassete-inverter-35000-btus-220v-lg.html ex2
 * (unavailable):
 * https://www.dufrio.com.br/ar-condicionado-frio-split-piso-teto-36000-btus-220v-1-lg.html
 *
 ******************************************************************************************************************************/

public class BrasilDufrioCrawler extends Crawler {

   private static final String HOME_PAGE_HTTPS = "https://www.dufrio.com.br/";
   private static final String SELLER_FULL_NAME = "Dufrio Brasil";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());

   public BrasilDufrioCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.WEBDRIVER);
   }

   @Override
   protected Object fetch() {
      Document doc = new Document("");
      this.webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), ProxyCollection.BUY_HAPROXY, session);

      if (this.webdriver != null) {
         doc = Jsoup.parse(this.webdriver.getCurrentPageSource());
         String requestHash = FetchUtilities.generateRequestHash(session);

         if (!isProductPage(doc)) {
            this.webdriver.waitLoad(10000);
            doc = Jsoup.parse(this.webdriver.getCurrentPageSource());
         }
         // saving request content result on Amazon
         S3Service.saveResponseContent(session, requestHash, doc.toString());

      }

      return doc;
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && ((href.startsWith(HOME_PAGE_HTTPS)));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".detalheProduto", "sku");
         String internalPid = crawlInternalPid(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".topoNome h1", false);
         boolean available = crawlAvailability(doc);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "div > figure.slick-slide.slick-current img", Arrays.asList("src"), "https", "www.dufrio.com.br");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".item.slick-slide.slick-active img", Arrays.asList("src"), "https", "www.dufrio.com.br", primaryImage);
         String description = crawlDescription(doc);
         RatingsReviews ratingsReviews = scrapRatingAndReviews(doc, internalId);
         Offers offers = available ? scrapOffers(doc) : new Offers();

         String productUrl = session.getOriginalURL();
         if (internalId != null && session.getRedirectedToURL(productUrl) != null) {
            productUrl = session.getRedirectedToURL(productUrl);
         }

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(productUrl)
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setRatingReviews(ratingsReviews)
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)

            .build();
         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return doc.select(".detalheProduto").first() != null;
   }


   private String crawlInternalPid(Document doc) {
      String internalPid = null;
      Elements scripts = doc.select("script");

      for (Element e : scripts) {
         String script = e.outerHtml();

         if (script.contains("dataLayer =")) {
            int x = script.indexOf('[') + 1;
            int y = script.indexOf("];", x) + 1;

            try {
               JSONObject datalayer = new JSONObject(script.substring(x, y));

               if (datalayer.has("google_tag_params")) {
                  JSONObject google = datalayer.getJSONObject("google_tag_params");

                  if (google.has("ecomm_prodid")) {
                     internalPid = google.getString("ecomm_prodid").trim();
                  }
               }
            } catch (Exception e1) {
               Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e1));
            }

            break;
         }
      }

      return internalPid;
   }


   private boolean crawlAvailability(Document document) {
      return document.select("#aviseme, #sobconsulta").isEmpty();
   }

   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();

      Elements elementCategories = document.select(".breadcrumb li > a");
      for (int i = 1; i < elementCategories.size(); i++) { // first index is the home page
         categories.add(elementCategories.get(i).ownText().trim());
      }

      return categories;
   }

   private String crawlDescription(Document document) {
      StringBuilder description = new StringBuilder();
      description.append(CrawlerUtils.scrapSimpleDescription(document, Arrays.asList("section.linha-01",
         "main > section:not([class]):not([id]) .small-12.columns", ".linha-bigLG", ".caracteristicasDetalhe", ".dimensoes")));

      Elements specialDescriptions = document.select("section[class^=linha-], div[class^=row box], .font-ubuntu-l");
      for (Element e : specialDescriptions) {
         description.append(e.html());
      }

      return description.toString();
   }


   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      //Site hasn't any sale

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".boxComprarNew-t2 .t2", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".boxComprarNew-t1", null, false, ',', session);

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(scrapBankSlip(doc))
         .setCreditCards(creditCards)
         .build();
   }

   private BankSlip scrapBankSlip(Document doc) throws MalformedPricingException {
      Double bankSlip = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".precos1x-t2", null, false, ',', session);

      return BankSlip.BankSlipBuilder.create()
         .setFinalPrice(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".boxComprarNew-t3", doc, false, "x", "juros", true, ',');

      if (!pair.isAnyValueNull()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(pair.getFirst())
            .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
            .build());

      } else {
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


   private RatingsReviews scrapRatingAndReviews(Document document, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      Integer totalNumOfEvaluations = getTotalNumOfRatings(document);
      Double avgRating = getTotalAvgRating(document, totalNumOfEvaluations);

      ratingReviews.setDate(session.getDate());
      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      return ratingReviews;
   }

   /**
    * Number of ratings appear in html element
    *
    * @param doc
    * @return
    */
   private Integer getTotalNumOfRatings(Document doc) {
      int ratingNumber = 0;
      Elements evaluations = doc.select(".boxBarras .p3");

      for (Element e : evaluations) {
         String text = e.ownText().replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            ratingNumber += Integer.parseInt(text);
         }
      }

      return ratingNumber;
   }

   private Double getTotalAvgRating(Document doc, Integer totalRatings) {
      Double avgRating = 0D;

      if (totalRatings != null && totalRatings > 0) {
         Elements ratings = doc.select(".boxBarras .item");

         int values = 0;

         for (Element e : ratings) {
            Element stars = e.select(".p1").first();
            Element value = e.select(".p3").first();

            if (stars != null && value != null) {
               Integer star = Integer.parseInt(stars.ownText().replaceAll("[^0-9]", "").trim());
               Integer countStars = Integer.parseInt(value.ownText().replaceAll("[^0-9]", "").trim());

               values += star * countStars;
            }
         }

         avgRating = MathUtils.normalizeTwoDecimalPlaces(((double) values) / totalRatings);
      }

      return avgRating;
   }
}
