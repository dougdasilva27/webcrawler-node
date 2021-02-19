package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class SaopauloUltrafarmaCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.ultrafarma.com.br/";

   private static final String SELLER_FULL_NAME = "Ultrafarma Sao Paulo";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());


   public SaopauloUltrafarmaCrawler(Session session) {
      super(session);
      // super.config.setFetcher(FetchMode.WEBDRIVER);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   public void handleCookiesBeforeFetch() {
      Logging.printLogDebug(logger, session, "Adding cookie...");

      BasicClientCookie cookie = new BasicClientCookie("ultrafarma_uf", "SP");
      cookie.setDomain(".ultrafarma.com.br");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".skuReference", true);
         String internalPid = internalId;

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.product-name", true);
         boolean available = !doc.select(".product-stock").isEmpty();
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:first-child):not(.active) a", true);
         String ean = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#pdp-section-outras-informacoes > div > ul > li:nth-child(2) > span", "data-attr-value");
         List<String> eans = new ArrayList<>();
         eans.add(ean);
         String description = crawlDescription(doc, ean);
         JSONArray images = CrawlerUtils.selectJsonArrayFromHtml(doc, "script", "LeanEcommerce.PDP_PRODUTO_IMAGENS=JSON.parse('", "')", true, false);
         String primaryImage = scrapPrimaryImage(images);
         List<String> secondaryImages = scrapSecondaryImages(images);
         Offers offers = available ? scrapOffers(doc) : new Offers();

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
            .setDescription(description)
            .setEans(eans)
            .setRatingReviews(crawlRatingReviews(doc))
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Document crawlHtmlFromApi(String ean) {
      String url = "https://standout.com.br/ultrafarma/catchtagUltrafarma.php?sku=" + ean + "&url=" + session.getOriginalURL();
      Request request = Request.RequestBuilder.create().setUrl(url).build();
      String content = this.dataFetcher.get(session, request).getBody();

      return Jsoup.parse(content);

   }

   private String endUrl(Document document) {

      String category = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "body > div", "x");
      String product = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "body > div", "y");
      if (category != null && product != null) {
         String selector = category + "/" + product;
         return selector.replaceAll("\\\\\"", "");
      }

      return null;
   }

   private String middleUrl(Document document) {
      String brand = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, "body > div", "i");
      if (brand != null) {
         return brand.replaceAll("\\\\\"", "");
      }

      return null;
   }

   private Document crawlHtmlApiDescription(String ean) {

      Document document = crawlHtmlFromApi(ean);
      String categoryProduct = endUrl(document);
      String brand = middleUrl(document);
      if (categoryProduct != null && brand != null) {

         String url = "https://www.standout.com.br/" + brand + "/p/rojGklzisEQ,/" + categoryProduct;

         Map<String, String> headers = new HashMap<>();
         headers.put("Referer", session.getOriginalURL());

         Request request = Request.RequestBuilder.create().setHeaders(headers).setUrl(url).build();
         String content = this.dataFetcher.get(session, request).getBody();

         return Jsoup.parse(content);
      }

      return null;
   }

   private String crawlDescription(Document doc, String ean) {
      StringBuilder description = new StringBuilder();
      if (crawlHtmlApiDescription(ean) != null) {
         description.append(crawlHtmlApiDescription(ean));
      }

      String productDetails = null;
      description.append(CrawlerUtils.scrapSimpleDescription(doc,
         Arrays.asList(".product-references .product-seller-brand-name",
            "#pdp-section-outras-informacoes",
            ".product-details-section[id~=anvisa]", ".tab-content .box p")));


      Element productDetailsElement = doc.selectFirst(".product-details-container .product-details-section:not([ng-if]):not([id])");

      if (productDetailsElement != null) {
         productDetails = productDetailsElement.text();
         if (!productDetails.contains("avaliar")) {
            description.append(productDetails);
         }
      }

      System.out.println(description);

      return description.toString();
   }

   private String scrapPrimaryImage(JSONArray images) {
      String primaryImage = null;

      for (Object o : images) {
         JSONObject json = (JSONObject) o;

         if (json.has("Principal") && !json.isNull("Principal") && json.getBoolean("Principal")) {

            String key = null;
            if (json.has("Grande") && !json.isNull("Grande")) {
               key = "Grande";
            } else if (json.has("Media") && !json.isNull("Media")) {
               key = "Media";
            } else if (json.has("Pequena") && !json.isNull("Pequena")) {
               key = "Pequena";
            } else if (json.has("Mini") && !json.isNull("Mini")) {
               key = "Mini";
            }

            if (key != null) {
               primaryImage = CrawlerUtils.completeUrl(json.get(key).toString(), "https", "ultrafarma-storage.azureedge.net");
            }

            break;
         }
      }

      return primaryImage;
   }

   private List<String> scrapSecondaryImages(JSONArray images) {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      for (Object o : images) {
         JSONObject json = (JSONObject) o;

         if (json.has("Principal") && !json.isNull("Principal") && !json.getBoolean("Principal")) {

            String key = null;
            if (json.has("Grande") && !json.isNull("Grande")) {
               key = "Grande";
            } else if (json.has("Media") && !json.isNull("Media")) {
               key = "Media";
            } else if (json.has("Pequena") && !json.isNull("Pequena")) {
               key = "Pequena";
            } else if (json.has("Mini") && !json.isNull("Mini")) {
               key = "Mini";
            }

            if (key != null) {
               secondaryImagesArray.put(CrawlerUtils.completeUrl(json.get(key).toString(), "https", "ultrafarma-storage.azureedge.net"));
            }
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return Collections.singletonList(secondaryImages);
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      if (scrapSaleDiscount(pricing) != null) {
         sales.add(scrapSaleDiscount(pricing));

      }

      return sales;
   }

   private String scrapSaleDiscount(Pricing pricing) {

      return CrawlerUtils.calculateSales(pricing);
   }


   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);


      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price-new span[data-preco]", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price-old del", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
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


      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private RatingsReviews crawlRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      Integer totalReviews = computeTotalReviewsCount(doc);

      ratingReviews.setTotalRating(totalReviews);
      ratingReviews.setTotalWrittenReviews(totalReviews);
      ratingReviews.setAverageOverallRating(crawlAverageOverallRating(doc, totalReviews));
      ratingReviews.setAdvancedRatingReview(scrapAdvancedRatingReview(doc));

      return ratingReviews;
   }

   private Integer computeTotalReviewsCount(Document doc) {
      Elements votes = doc.select(".amount-reviews");
      int totalRatings = 0;

      for (Element e : votes) {
         totalRatings += CrawlerUtils.scrapIntegerFromHtml(e, null, true, 0);
      }
      return totalRatings;

   }

   private Double crawlAverageOverallRating(Document doc, int totalReviews) {
      Elements votes = doc.select(".amount-reviews");
      double totalvalue = 0;

      if (totalReviews == 0) {
         return 0.0;
      }

      for (Element e : votes) {

         totalvalue += CrawlerUtils.scrapIntegerFromHtml(e, null, true, 0) * (5 - votes.indexOf(e));
      }

      return totalvalue / totalReviews;
   }


   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      int star1 = 0;
      int star2 = 0;
      int star3 = 0;
      int star4 = 0;
      int star5 = 0;

      Elements stars = doc.select(".resume-label li span:first-child");
      Elements votes = doc.select(".amount-reviews");

      if (stars.size() == votes.size()) {

         for (int i = 0; i < stars.size(); i++) {

            Element starElement = stars.get(i);
            Element voteElement = votes.get(i);

            String starNumber = starElement.attr("class");
            int star = !starNumber.isEmpty() ? MathUtils.parseInt(starNumber) : 0;

            String voteNumber = CrawlerUtils.scrapStringSimpleInfo(voteElement, null, true);
            int vote = !voteNumber.isEmpty() ? MathUtils.parseInt(voteNumber) : 0;

            switch (star) {
               case 50:
                  star5 = vote;
                  break;
               case 40:
                  star4 = vote;
                  break;
               case 30:
                  star3 = vote;
                  break;
               case 20:
                  star2 = vote;
                  break;
               case 10:
                  star1 = vote;
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


   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product") != null;
   }
}
