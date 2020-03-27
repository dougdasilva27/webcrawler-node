package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JavanetDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.RequestsStatistics;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;

/**
 * 02/02/2018
 * 
 * @author gabriel
 *
 */
public class BrasilCarrefourCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.carrefour.com.br/";
   private static final String SELLER_NAME_LOWER = "carrefour";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilCarrefourCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   protected Object fetch() {
      return Jsoup.parse(fetchPage(session.getOriginalURL()));
   }

   protected String fetchPage(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
      headers.put("referer", HOME_PAGE);

      Request request = RequestBuilder.create()
            .setUrl(url)
            .setCookies(cookies)
            .setHeaders(headers)
            .setFetcheroptions(
                  FetcherOptionsBuilder.create()
                        .mustUseMovingAverage(false)
                        .mustRetrieveStatistics(true)
                        .build())
            .setProxyservice(Arrays.asList(
                  ProxyCollection.INFATICA_RESIDENTIAL_BR,
                  ProxyCollection.STORM_RESIDENTIAL_EU))
            .build();

      int attempts = 0;
      Response response = this.dataFetcher.get(session, request);
      String body = response.getBody();

      Integer statusCode = 0;
      List<RequestsStatistics> requestsStatistics = response.getRequests();
      if (!requestsStatistics.isEmpty()) {
         statusCode = requestsStatistics.get(requestsStatistics.size() - 1).getStatusCode();
      }

      boolean retry = statusCode == null ||
            (Integer.toString(statusCode).charAt(0) != '2'
                  && Integer.toString(statusCode).charAt(0) != '3'
                  && statusCode != 404);

      // If fetcher don't return the expected response we try with apache
      // If apache do the same, we try with javanet
      if (retry) {
         do {
            if (attempts == 0) {
               body = new ApacheDataFetcher().get(session, request).getBody();
            } else if (attempts == 1) {
               body = new JavanetDataFetcher().get(session, request).getBody();
            }

            attempts++;
         } while (attempts < 2 && (body == null || body.isEmpty()));
      }

      return body;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(session.getOriginalURL());
         String name = crawlName(doc);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = crawlSecondaryImages(doc);
         String description = crawlDescription(doc);
         String internalPid = crawlInternalPid(doc);
         RatingsReviews rating = crawlRatingReviews(doc);
         boolean available = doc.selectFirst("#buyProductButtonBottom") != null;
         Offers offers = available ? scrapNewOffers(doc) : new Offers();

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
               .setDescription(description)
               .setRatingReviews(rating)
               .setOffers(offers)
               .build();

         products.add(product);

      } else

      {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   /*******************************
    * Product page identification *
    *******************************/

   private boolean isProductPage(Document doc) {
      return doc.select(".product-details-panel").first() != null;
   }

   /*******************
    * General methods *
    *******************/

   private String crawlInternalPid(Document document) {
      String internalId = null;
      Element internalIdElement = document.select("#productCod").first();

      if (internalIdElement != null) {
         internalId = internalIdElement.val().trim();
      }

      return internalId;
   }

   private String crawlInternalId(String url) {
      String internalPid = null;

      if (url.contains("?")) {
         url = url.split("\\?")[0];
      }

      if (url.contains("/p/")) {
         String[] tokens = url.split("/p/");

         if (tokens.length > 1 && tokens[1].contains("/")) {
            internalPid = tokens[1].split("/")[0];
         } else if (tokens.length > 1) {
            internalPid = tokens[1];
         }
      }
      return internalPid;
   }

   private String crawlName(Document document) {
      String name = null;
      Element nameElement = document.select("h1[itemprop=name]").first();

      if (nameElement != null) {
         name = nameElement.text().trim();
      }

      return name;
   }

   private String crawlPrimaryImage(Document document) {
      String primaryImage = null;
      Element primaryImageElement = document.select(".sust-gallery div.item .thumb img").first();

      if (primaryImageElement != null) {
         String image = primaryImageElement.attr("data-zoom-image");
         if (image != null) {
            primaryImage = image.trim();
         }

      }

      return primaryImage;
   }

   private String crawlSecondaryImages(Document document) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements imagesElement = document.select(".sust-gallery div.item .thumb img");

      for (int i = 1; i < imagesElement.size(); i++) { // start with index 1 because the first image
                                                       // is the primary image
         Element e = imagesElement.get(i);

         if (e.attr("data-zoom-image") != null && !e.attr("data-zoom-image").isEmpty()) {
            secondaryImagesArray.put(e.attr("data-zoom-image").trim());
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   /**
    * @param document
    * @return
    */
   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select(".breadcrumb > li > a");

      for (int i = 1; i < elementCategories.size() - 1; i++) { // starting from index 1, because the
         categories.add(elementCategories.get(i).text().trim());
      }

      return categories;
   }

   private String crawlDescription(Document document) {
      StringBuilder description = new StringBuilder();

      Element desc2 = document.select(".productDetailsPageShortDescription").first();
      if (desc2 != null) {
         description.append(desc2.outerHtml());
      }

      Elements descriptionElements = document.select("#accordionFichaTecnica");
      if (descriptionElements != null) {
         description.append(descriptionElements.html());
      }

      Element desc = document.select(".productDetailsPageDescription").first();
      if (desc != null) {
         description.append(desc.outerHtml());
      }

      return description.toString();
   }


   private RatingsReviews crawlRatingReviews(Document document) {
      RatingsReviews ratingReviews = new RatingsReviews();

      Map<String, Integer> ratingDistribution = crawlRatingDistribution(document);
      AdvancedRatingReview advancedRatingReview = getTotalStarsFromEachValueWithRate(ratingDistribution);

      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setDate(session.getDate());
      ratingReviews.setTotalRating(computeTotalReviewsCount(ratingDistribution));
      ratingReviews.setAverageOverallRating(crawlAverageOverallRating(document));

      return ratingReviews;
   }

   private Integer computeTotalReviewsCount(Map<String, Integer> ratingDistribution) {
      Integer totalReviewsCount = 0;

      for (String rating : ratingDistribution.keySet()) {
         if (ratingDistribution.get(rating) != null)
            totalReviewsCount += ratingDistribution.get(rating);
      }

      return totalReviewsCount;
   }

   private Double crawlAverageOverallRating(Document document) {
      Double avgOverallRating = null;

      Element avgOverallRatingElement =
            document.select(".sust-review-container .block-review-pagination-bar div.block-rating div.rating.js-ratingCalc").first();
      if (avgOverallRatingElement != null) {
         String dataRatingText = avgOverallRatingElement.attr("data-rating").trim();
         try {
            JSONObject dataRating = new JSONObject(dataRatingText);
            if (dataRating.has("rating")) {
               avgOverallRating = dataRating.getDouble("rating");
            }
         } catch (JSONException e) {
            Logging.printLogWarn(logger, session, "Error converting String to JSONObject");
         }
      }

      return avgOverallRating;
   }

   private Map<String, Integer> crawlRatingDistribution(Document document) {
      Map<String, Integer> ratingDistributionMap = new HashMap<String, Integer>();

      Elements ratingLineElements = document.select("div.tab-review ul.block-list-starbar li");
      for (Element ratingLine : ratingLineElements) {
         Element ratingStarElement = ratingLine.select("div").first();
         Element ratingStarCount = ratingLine.select("div").last();

         if (ratingStarElement != null && ratingStarCount != null) {
            String ratingStarText = ratingStarElement.text();
            String ratingCountText = ratingStarCount.attr("data-star");

            List<String> parsedNumbers = MathUtils.parseNumbers(ratingStarText);
            if (parsedNumbers.size() > 0 && !ratingCountText.isEmpty()) {
               ratingDistributionMap.put(parsedNumbers.get(0), Integer.parseInt(ratingCountText));
            }
         }
      }

      return ratingDistributionMap;
   }

   public static AdvancedRatingReview getTotalStarsFromEachValueWithRate(Map<String, Integer> ratingDistribution) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      for (Map.Entry<String, Integer> entry : ratingDistribution.entrySet()) {

         if (entry.getKey().equals("1")) {
            star1 = entry.getValue();
         }

         if (entry.getKey().equals("2")) {
            star2 = entry.getValue();
         }

         if (entry.getKey().equals("3")) {
            star3 = entry.getValue();
         }

         if (entry.getKey().equals("4")) {
            star4 = entry.getValue();
         }

         if (entry.getKey().equals("5")) {
            star5 = entry.getValue();
         }

      }

      return new AdvancedRatingReview.Builder().totalStar1(star1).totalStar2(star2).totalStar3(star3).totalStar4(star4).totalStar5(star5).build();
   }

   // NEW MODEL OF OFFERS


   private Offers scrapNewOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      Map<String, Integer> mainSellers = new HashMap<>();
      if (doc.selectFirst(".product-details") != null) {

         Offer principalOffer = captureOffersWhenDontHaveMoreThanOne(doc);
         mainSellers.put(principalOffer.getInternalSellerId(), 1);

         if (principalOffer != null) {
            offers.add(principalOffer);
         }


      } else {

         JSONObject jsonSellers = getJSON(doc);

         boolean isBuyBoxPage = jsonSellers.has("buyBoxOffers") ? true : false;

         String sellerFullName = null;
         String internalSellerId = null;
         Double spotlightPriceJson = 0d;

         if (jsonSellers.has("buyBoxOffers") && !jsonSellers.isNull("buyBoxOffers")) {
            JSONArray buyBoxOffers = jsonSellers.getJSONArray("buyBoxOffers");
            int position = 1;
            for (Object o : buyBoxOffers) {

               JSONObject offerJson = (JSONObject) o;

               if (offerJson.has("miraklVendor") && !offerJson.isNull("miraklVendor") && offerJson.has("price") && !offerJson.isNull("price")) {
                  JSONObject miraklVendor = offerJson.getJSONObject("miraklVendor");
                  JSONObject price = offerJson.getJSONObject("price");

                  if (miraklVendor.has("name") && miraklVendor.has("code")) {
                     sellerFullName = miraklVendor.get("name").toString();
                     internalSellerId = miraklVendor.get("code").toString();

                  }

                  if (price.has("value")) {
                     spotlightPriceJson = price.getDouble("value");
                  }

               }

               boolean isMainRetailer = sellerFullName.equalsIgnoreCase(SELLER_NAME_LOWER);
               Pricing pricing = scrapPricingOfSellers(doc, spotlightPriceJson);
               List<String> sales = new ArrayList<>();

               Offer offer = new OfferBuilder()
                     .setInternalSellerId(internalSellerId)
                     .setSellerFullName(sellerFullName)
                     .setMainPagePosition(position)
                     .setIsBuybox(isBuyBoxPage)
                     .setIsMainRetailer(isMainRetailer)
                     .setPricing(pricing)
                     .setSales(sales)
                     .build();
               offers.add(offer);

               position++;
            }
         }
      }

      return offers;
   }

   private JSONObject getJSON(Document doc) {
      JSONObject productInfo = null;

      JSONArray dataLayer = CrawlerUtils.selectJsonArrayFromHtml(doc, "script[type=\"text/javascript\"]", "dataLayer = ", ";", false, false);

      if (dataLayer.length() > 0) {
         productInfo = dataLayer.getJSONObject(0);

      }

      return productInfo;
   }

   // Oferta principal

   private Offer captureOffersWhenDontHaveMoreThanOne(Document doc) throws OfferException, MalformedPricingException {
      boolean isBuyBoxPage = doc.selectFirst(".list-group.send-results.list-offer-by-box") != null;
      String sellerNameInMainPage = CrawlerUtils.scrapStringSimpleInfo(doc, ".sellerLink.vendaPorSeller", false);
      String sellerFullName = sellerNameInMainPage != null ? sellerNameInMainPage : SELLER_NAME_LOWER;
      Integer sellersPagePosition = null;
      Pricing pricing = scrapPricingForProductPage(doc);
      String sale = CrawlerUtils.scrapStringSimpleInfo(doc, ".percentual", false);
      List<String> sales = sale != null ? Arrays.asList(sale) : new ArrayList<>();

      return OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(sellerFullName)
            .setMainPagePosition(1)
            .setIsBuybox(isBuyBoxPage)
            .setIsMainRetailer(true)
            .setSellersPagePosition(sellersPagePosition)
            .setPricing(pricing)
            .setSales(sales)
            .build();
   }


   private Pricing scrapPricingForProductPage(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".row .prince-product-default .priceBig", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".row .prince-product-default .priceBig", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);


      return PricingBuilder.create()
            .setPriceFrom(priceFrom)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(doc);

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;
   }

   public Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();

      Element installmentsCard = doc.selectFirst(".prince-product-blue");

      if (installmentsCard != null) {

         String installmentCard = installmentsCard.ownText();
         String installmentString = installmentCard != null ? installmentCard.split("x")[0] : null;
         int installment = installmentString != null ? Integer.parseInt(installmentString.replaceAll("[^0-9]", "").trim()) : null;

         String valueCard = installmentsCard.ownText();
         int de = valueCard.contains("R$") ? valueCard.indexOf("R$") : null;
         Double value = valueCard != null ? MathUtils.parseDoubleWithComma(valueCard.substring(de)) : null;

         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(installment)
               .setInstallmentPrice(value)
               .build());
      }

      return installments;
   }

   // Fim da oferta principal

   // Inicio da captura das lojas externas

   private Pricing scrapPricingOfSellers(Document doc, Double spotlightPriceJson) throws MalformedPricingException {

      Double spotlightPrice = spotlightPriceJson;
      CreditCards creditCards = scrapCreditCardsForSellers(doc);

      return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .build();
   }


   private CreditCards scrapCreditCardsForSellers(Document doc) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments regularCard = scrapInstallmentsForSellers(doc, ".installment .credit-card-installments");
      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(brand)
               .setIsShopCard(false)
               .setInstallments(regularCard)
               .build());
      }

      Installments shopCard = scrapInstallmentsForSellers(doc, ".installment-payment .big-price");
      creditCards.add(CreditCardBuilder.create()
            .setBrand(Card.SHOP_CARD.toString())
            .setIsShopCard(true)
            .setInstallments(shopCard)
            .build());

      return creditCards;
   }

   private Installments scrapInstallmentsForSellers(Document doc, String selector) throws MalformedPricingException {
      Installments installments = new Installments();

      Elements installmentsElements = doc.select(selector);

      for (Element e : installmentsElements) {
         String id = e.toString();

         String installmentCard = id;
         String installmentString = installmentCard.contains("x") ? installmentCard.split("x")[0] : null;
         int installment = installmentString != null ? Integer.parseInt(installmentString.replaceAll("[^0-9]", "").trim()) : null;

         String valueCard = id;
         int rs = valueCard.contains("R$") ? valueCard.indexOf("R$") : null;
         Double value = valueCard != null ? MathUtils.parseDoubleWithComma(valueCard.substring(rs)) : null;


         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(installment)
               .setInstallmentPrice(value)
               .build());

      }

      return installments;
   }


}
