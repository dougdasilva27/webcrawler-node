package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.*;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static models.pricing.BankSlip.BankSlipBuilder;

public class BrasilRicardoeletroCrawler extends Crawler {

   public BrasilRicardoeletroCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      JSONArray jsonArray = CrawlerUtils.selectJsonArrayFromHtml(doc, "script", "dataLayer = ", ";", false, true);
      JSONObject productJSON = jsonArray.length() > 0 && jsonArray.get(0) instanceof JSONObject ? jsonArray.getJSONObject(0) : new JSONObject();

      if (productJSON.has("productSKUList")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = scrapInternalPid(productJSON);
         String mainId = scrapMainId(productJSON);
         String ean = JSONUtils.getStringValue(productJSON, "productEAN13");
         List<String> eans = ean != null && !ean.isEmpty() ? Arrays.asList(ean) : null;

         JSONArray skusArray = JSONUtils.getJSONArrayValue(productJSON, "productSKUList");

         for (Object obj : skusArray) {
            JSONObject skuJson = obj instanceof JSONObject ? (JSONObject) obj : new JSONObject();
            String internalId = scrapInternalId(skuJson);

            if (internalId != null) {
               String name = scrapName(skuJson);
               String newUrl = JSONUtils.getStringValue(skuJson, "url");
               String url = newUrl != null ? newUrl : session.getOriginalURL();

               Document docVariation = !internalId.equalsIgnoreCase(mainId) ? scrapVariationHTML(url) : doc;

               String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(docVariation, ".product-info-images .swiper-slide img", Arrays.asList("src"),
                       "https", "www.imgeletro.com.br");
               String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(docVariation, ".product-info-images .swiper-slide img", Arrays.asList("src"),
                       "https", "www.imgeletro.com.br", primaryImage);
               CategoryCollection categories = CrawlerUtils.crawlCategories(docVariation, "#Breadcrumbs a");
               String description = scrapDescription(docVariation);

               List<Document> sellersHtmls = scrapProductPagePerSeller(docVariation);
               Offers offers = scrapOffers(sellersHtmls);
               Integer stock = JSONUtils.getIntegerValueFromJSON(skuJson, "stock", 0);
               RatingsReviews rating = scrapRating(doc);

               Product product = ProductBuilder.create()
                       .setUrl(url)
                       .setInternalId(internalId)
                       .setInternalPid(internalPid)
                       .setName(name)
                       .setCategory1(categories.getCategory(0))
                       .setCategory2(categories.getCategory(1))
                       .setCategory3(categories.getCategory(2))
                       .setPrimaryImage(primaryImage)
                       .setSecondaryImages(secondaryImages)
                       .setDescription(description)
                       .setStock(stock)
                       .setEans(eans)
                       .setOffers(offers)
                       .setRatingReviews(rating)
                       .build();

               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String scrapInternalPid(JSONObject productJSON) {
      String internalPid = null;

      if (productJSON.has("productID") && !productJSON.isNull("productID")) {
         internalPid = productJSON.get("productID").toString();
      }

      return internalPid;
   }

   private String scrapMainId(JSONObject productJSON) {
      String mainId = null;

      if (productJSON.has("productSKU") && !productJSON.isNull("productSKU")) {
         mainId = productJSON.get("productSKU").toString();
      }

      return mainId;
   }

   private Document scrapVariationHTML(String url) {
      Request request = RequestBuilder.create()
              .setUrl(url)
              .setCookies(cookies)
              .build();

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   private String scrapName(JSONObject obj) {
      StringBuilder name = new StringBuilder();
      name.append(JSONUtils.getStringValue(obj, "name"));

      JSONObject specs = JSONUtils.getJSONValue(obj, "specs");
      for (String key : specs.keySet()) {
         name.append(" ").append(specs.get(key));
      }

      return name.toString();
   }

   private String scrapInternalId(JSONObject obj) {
      String internalId = null;

      if (obj.has("sku") && !obj.isNull("sku")) {
         internalId = obj.get("sku").toString();
      }

      return internalId;
   }

   private String scrapDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element principalDescription = doc.selectFirst("#product-description");
      if (principalDescription != null) {
         String iframeUrl = CrawlerUtils.scrapUrl(principalDescription, "iframe", Arrays.asList("src"), "https", "conteudo.maquinadevendas.com.br");

         if (iframeUrl != null) {
            Request request = RequestBuilder.create().setUrl(iframeUrl).build();
            description.append(Jsoup.parse(this.dataFetcher.get(session, request).getBody()));

            principalDescription.select("iframe").remove();
         }

         description.append(principalDescription.html());
      }

      description.append(CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#product-features", "#product-list", "#product-provider")));

      return description.toString();
   }

   private List<Document> scrapProductPagePerSeller(Document doc) {
      List<Document> htmls = new ArrayList<>();
      htmls.add(doc);

      Elements sellers = doc.select(".product-price-marketplace .product-price-card a[href]");
      for (Element seller : sellers) {
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(seller, ".product-price-card-price", null, true, ',', session);

         // if price is not null this seller have a offer and we need to access another url to scrap
         // installments
         if (price != null) {
            String sellerUrl = CrawlerUtils.completeUrl(seller.attr("href"), "https", "www.ricardoeletro.com.br");
            Request request = RequestBuilder.create().setUrl(sellerUrl).build();

            htmls.add(Jsoup.parse(this.dataFetcher.get(session, request).getBody()));
         }
      }

      return htmls;
   }

   private Offers scrapOffers(List<Document> htmls) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      int position = 1;
      for (Document doc : htmls) {
         String sellerName = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-price-details .product-price-details-sold-by b", true);
         String sellerId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#ExibeFormasPagamento[data-siteid]", "data-siteid");

         Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price-details .product-price-details-new-price", null, false, ',', session);
         Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-price-details .product-price-details-old-price", null, false, ',', session);
         if (price != null) {
            Pricing pricing = PricingBuilder.create().setSpotlightPrice(price).setCreditCards(scrapCreditCards(doc))
                    .setPriceFrom(priceFrom)
                    .setBankSlip(BankSlipBuilder.create().setFinalPrice(price).build())
                    .build();
            offers.add(new OfferBuilder()
                    .setSellerFullName(sellerName)
                    .setInternalSellerId(sellerId)
                    .setIsMainRetailer(Pattern.matches("(?i)ricardoeletro\\s?|ricardo?[\\s]?eletro\\s?", sellerName))
                    .setMainPagePosition(position)
                    .setPricing(pricing)
                    .setIsBuybox(htmls.size() > 1)
                    .build());
         }
         position++;
      }

      return offers;
   }

   private CreditCards scrapCreditCards(Document doc) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = scrapInstallment(doc);
      Set<String> allCards = Arrays.stream(Card.values()).map(Card::toString).collect(Collectors.toSet());
      Set<String> cards = new HashSet<>();
      for (Element child : doc.selectFirst(".footer-infos-flags").children()) {
         allCards.stream().filter(card -> child.className().contains(card)).forEach(cards::add);
      }
      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
                 .setBrand(card)
                 .setInstallments(installments)
                 .setIsShopCard(true)
                 .build());
      }

      return creditCards;

   }

   private Installments scrapInstallment(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();
      Elements installmentsElements = doc.select(".product-price-details  .product-price-details-installments:not(:last-child) b");
      for (Element e : installmentsElements) {
         Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, true);
         if (!pair.isAnyValueNull()) {
            installments.add(InstallmentBuilder.create()
                    .setInstallmentNumber(pair.getFirst())
                    .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
                    .build());
         }
      }
      return installments;
   }

   /**
    * Crawl rating and reviews stats using the bazaar voice endpoint. To get only the stats summary we
    * need at first, we only have to do one request. If we want to get detailed information about each
    * review, we must perform pagination.
    * 
    * The RatingReviews crawled in this method, is the same across all skus variations in a page.
    *
    * @param document
    * @return
    */
   private RatingsReviews scrapRating(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();

      ratingReviews.setDate(session.getDate());

      Integer totalReviews = scrapReviewsCount(doc);
      ratingReviews.setTotalRating(totalReviews);
      ratingReviews.setTotalWrittenReviews(totalReviews);
      ratingReviews.setAverageOverallRating(scrapAverageOverallRating(doc));
      ratingReviews.setAdvancedRatingReview(scrapTotalOfRewviesPerEachStar(doc));

      return ratingReviews;
   }

   private Integer scrapReviewsCount(Document doc) {
      return doc.select(".product-comments-body-comment-stars").size();
   }

   private Double scrapAverageOverallRating(Document doc) {
      Double avgOverallRating = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-comments-body-grade", null, true, '.', session);

      if (avgOverallRating == null) {
         avgOverallRating = 0d;
      }
      return avgOverallRating;
   }

   private AdvancedRatingReview scrapTotalOfRewviesPerEachStar(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".product-comments-body-comment-stars");
      for (Element e : reviews) {
         int numberOfStars = e.select("img[src=/public/img/star-full.svg]").size();

         if (numberOfStars == 1) {
            star1++;
         } else if (numberOfStars == 2) {
            star2++;
         } else if (numberOfStars == 3) {
            star3++;
         } else if (numberOfStars == 4) {
            star4++;
         } else if (numberOfStars == 5) {
            star5++;
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
