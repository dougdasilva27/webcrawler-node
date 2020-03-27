package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.http.HttpHeaders;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.fetcher.FetchUtilities;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
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
 * Date: 08/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class MercadolivreCrawler extends Crawler {

   private String homePage;
   private String mainSellerNameLower;
   private char separator;
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());

   protected MercadolivreCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   public void setSeparator(char separator) {
      this.separator = separator;
   }

   public void setHomePage(String homePage) {
      this.homePage = homePage;
   }

   public void setMainSellerNameLower(String mainSellerNameLower) {
      this.mainSellerNameLower = mainSellerNameLower;
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   @Override
   protected Object fetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put(HttpHeaders.USER_AGENT, FetchUtilities.randUserAgentWithoutChrome());

      Request request = RequestBuilder.create()
            .setUrl(session.getOriginalURL())
            .setCookies(cookies)
            .setHeaders(headers)
            .build();

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }



   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());



         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=itemId], #productInfo input[name=\"item_id\"]", "value");

         Map<String, Document> variations = getVariationsHtmls(doc);
         for (Entry<String, Document> entry : variations.entrySet()) {
            Document docVariation = entry.getValue();

            String variationId = CrawlerUtils.scrapStringSimpleInfoByAttribute(docVariation, "input[name=variation]", "value");

            if (variations.size() > 1 && (variationId == null || variationId.trim().isEmpty())) {
               continue;
            }

            String internalId = variationId == null || variations.size() < 2 ? internalPid : internalPid + "-" + variationId;

            String name = crawlName(docVariation);
            CategoryCollection categories = CrawlerUtils.crawlCategories(docVariation, "a.breadcrumb:not(.shortened)");
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(docVariation, "figure.gallery-image-container a", Arrays.asList("href"), "https:",
                  "http2.mlstatic.com");
            String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(docVariation, "figure.gallery-image-container a", Arrays.asList("href"),
                  "https:", "http2.mlstatic.com", primaryImage);
            String description =
                  CrawlerUtils.scrapSimpleDescription(docVariation, Arrays.asList(".vip-section-specs", ".section-specs", ".item-description"));



            RatingReviewsCollection ratingReviewsCollection = new RatingReviewsCollection();
            ratingReviewsCollection.addRatingReviews(crawlRating(doc, internalId));
            RatingsReviews ratingReviews = ratingReviewsCollection.getRatingReviews(internalId);
            boolean availableToBuy = !docVariation.select(".item-actions [value=\"Comprar agora\"]").isEmpty()
                  || !docVariation.select(".item-actions [value=\"Comprar ahora\"]").isEmpty()
                  || !docVariation.select(".item-actions [value~=Comprar]").isEmpty();
            Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

            // Creating the product
            Product product = ProductBuilder.create()
                  .setUrl(entry.getKey())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setCategory1(categories.getCategory(0))
                  .setCategory2(categories.getCategory(1))
                  .setCategory3(categories.getCategory(2))
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setRatingReviews(ratingReviews)
                  .setOffers(offers)
                  .build();

            products.add(product);

         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private RatingsReviews crawlRating(Document doc, String internalId) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = getTotalNumOfRatings(doc);
      Double avgRating = getTotalAvgRating(doc);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc, internalId);

      ratingReviews.setInternalId(internalId);
      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   /**
    * 
    * @param document
    * @return
    */
   private Double getTotalAvgRating(Document doc) {
      Double avgRating = 0d;

      Element avg = doc.selectFirst(".review-summary-average");
      if (avg != null) {
         String text = avg.ownText().replaceAll("[^0-9.]", "");

         if (!text.isEmpty()) {
            avgRating = Double.parseDouble(text);
         }
      }

      return avgRating;
   }

   /**
    * Number of ratings appear in html
    * 
    * @param docRating
    * @return
    */
   private Integer getTotalNumOfRatings(Document docRating) {
      Integer totalRating = 0;
      Element totalRatingElement = docRating.selectFirst(".core-review .average-legend");

      if (totalRatingElement != null) {
         String text = totalRatingElement.text().replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            totalRating = Integer.parseInt(text);
         }
      }

      return totalRating;
   }


   private Document acessHtmlWithAdvanedRating(String internalId) {

      Document docRating = new Document("");

      String url =
            "https://produto.mercadolivre.com.br/noindex/catalog/reviews/" + internalId + "?noIndex=true&itemId=" + internalId + "&modal=true&modalWidth=840&modalHeight=400&access=stars&parent_url=" + this.homePage;

      Map<String, String> headers = new HashMap<>();
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.87 Safari/537.36");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");

      Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      String response = dataFetcher.get(session, request).getBody().trim();

      docRating = Jsoup.parse(response);

      return docRating;

   }


   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc, String internalId) {
      Document docRating;

      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      docRating = acessHtmlWithAdvanedRating(internalId);

      Elements reviews = docRating.select(".reviews-rating .review-rating-row.is-rated");

      for (Element review : reviews) {

         Element elementStarNumber = review.selectFirst(".review-rating-label");

         if (elementStarNumber != null) {


            String stringStarNumber = elementStarNumber.text().replaceAll("[^0-9]", "").trim();
            Integer numberOfStars = !stringStarNumber.isEmpty() ? Integer.parseInt(stringStarNumber) : 0;

            Element elementVoteNumber = review.selectFirst(".review-rating-total");

            if (elementVoteNumber != null) {

               String vN = elementVoteNumber.text().replaceAll("[^0-9]", "").trim();
               Integer numberOfVotes = !vN.isEmpty() ? Integer.parseInt(vN) : 0;

               switch (numberOfStars) {
                  case 5:
                     star5 = numberOfVotes;
                     break;
                  case 4:
                     star4 = numberOfVotes;
                     break;
                  case 3:
                     star3 = numberOfVotes;
                     break;
                  case 2:
                     star2 = numberOfVotes;
                     break;
                  case 1:
                     star1 = numberOfVotes;
                     break;
                  default:
                     break;
               }
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


   private boolean isProductPage(Document doc) {
      return !doc.select(".vip-nav-bounds .layout-main").isEmpty();
   }

   private Map<String, Document> getVariationsHtmls(Document doc) {
      Map<String, Document> variations = new HashMap<>();

      String originalUrl = session.getOriginalURL();
      variations.putAll(getSizeVariationsHmtls(doc, originalUrl));

      Elements colors = doc.select(".variation-list--full li:not(.variations-selected)");
      for (Element e : colors) {
         String dataValue = e.attr("data-value");
         String url =
               originalUrl + (originalUrl.contains("?") ? "&" : "?") + "attribute=COLOR_SECONDARY_COLOR%7C" + dataValue + "&quantity=1&noIndex=true";
         Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
         Document docColor = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
         variations.putAll(getSizeVariationsHmtls(docColor, url));
      }

      return variations;
   }

   private Map<String, Document> getSizeVariationsHmtls(Document doc, String urlColor) {
      Map<String, Document> variations = new HashMap<>();
      variations.put(urlColor, doc);

      Elements sizes = doc.select(".variation-list li:not(.variations-selected) a.ui-list__item-option");
      for (Element e : sizes) {
         String attribute = null;
         String[] parameters = e.attr("href").split("&");
         for (String p : parameters) {
            if (p.startsWith("attribute=")) {
               attribute = p;
               break;
            }
         }

         String url = urlColor + (urlColor.contains("?") ? "&" : "?") + attribute;
         Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
         Document docSize = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

         variations.put(url, docSize);
      }

      return variations;
   }

   private static String crawlName(Document doc) {
      StringBuilder name = new StringBuilder();
      name.append(CrawlerUtils.scrapStringSimpleInfo(doc, "h1.item-title__primary", true));

      Element sizeElement = doc.selectFirst(".variation-list li.variations-selected");
      if (sizeElement != null) {
         name.append(" ").append(sizeElement.attr("data-title"));
      }

      Element colorElement = doc.selectFirst(".variation-list--full li.variations-selected");
      if (colorElement != null) {
         name.append(" ").append(colorElement.attr("data-title"));
      }

      return name.toString();
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);
      String sellerFullName = scrapSellerFullName(doc);

      offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(sellerFullName)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());

      return offers;
   }

   private String scrapSellerFullName(Document doc) {

      String sellerName = mainSellerNameLower;
      String sellerNameElement = doc.selectFirst(".official-store-info .title").toString();

      if (sellerName.equalsIgnoreCase(sellerNameElement)) {
         sellerName = mainSellerNameLower;
      } else {
         sellerName = sellerNameElement;
      }

      return sellerName;
   }


   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element salesOneElement = doc.selectFirst(".price-tag.discount-arrow p");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-tag.price-tag__del del .price-tag-symbol", "content", false, '.', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".item-price span.price-tag:not(.price-tag__del) .price-tag-symbol", "content", false, '.', session);
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
      if (installments.getInstallments().isEmpty()) {
         installments.add(InstallmentBuilder.create()
               .setInstallmentNumber(1)
               .setInstallmentPrice(spotlightPrice)
               .build());
      }

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

      Element installmentsElement = doc.selectFirst(".payment-installments .highlight-info span");
      String installmentString = installmentsElement != null ? installmentsElement.text().replaceAll("[^0-9]", "").trim() : null;
      int installment = installmentString != null ? Integer.parseInt(installmentString) : null;

      Element valueElement = doc.selectFirst(".payment-installments .ch-price");
      String valueString = valueElement != null ? valueElement.ownText().replaceAll("[^0-9]", "").trim() : null;
      Double value = valueString != null ? MathUtils.parseDoubleWithComma(valueString) : null;


      installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(installment)
            .setInstallmentPrice(value)
            .build());

      return installments;
   }

}
