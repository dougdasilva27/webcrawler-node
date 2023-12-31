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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
         String internalPid = crawlInternalPid(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.name", true);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".big-image img", List.of("data-src"), "https", "io.convertiez.com.br");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".product-image li:not(.active) img", List.of("data-src"), "https", "io.convertiez.com.br", primaryImage);
         String description = scrapDescription(doc);
         RatingsReviews ratingsReviews = scrapRatingAndReviews(doc, internalId);
         boolean available = doc.selectFirst("#content-product .purchase > button:contains(Comprar)") != null;
         Offers offers = available ? scrapOffers(doc, internalPid) : new Offers();

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

   private String scrapDescription(Document doc) {

      StringBuilder description = new StringBuilder();
      doc.select(".container .my-4").forEach(el -> {
         String element = CrawlerUtils.scrapStringSimpleInfo(el, ".title", true);
         String productDescription = CrawlerUtils.scrapStringSimpleInfo(el, "p", true);
         if (element != null && productDescription != null) {
            if (element.contains("Modo de Usar")) {

               description.append("Modo de Usar").append("\n");
               description.append(productDescription);

            } else if (element.contains("Descrição")) {
               description.append("Descrição").append("\n");
               description.append(productDescription);
            }
         }
      });

      String information = CrawlerUtils.scrapStringSimpleInfo(doc, ".box-style-3.mt-2 p", true);

      if (information != null) {
         description.append(information).append("\n");
      }

      return description.toString();

   }

   private String crawlInternalPid(Document doc) {
      List<Element> scripts = doc.select("script[type=text/javascript]").stream().filter(e -> e.html().contains("window.dataProduct")).collect(Collectors.toList());
      if (!scripts.isEmpty()) {
         String script = scripts.get(0).html();
         try {
            JSONObject productJSON = new JSONObject(CrawlerUtils.extractSpecificStringFromScript(script, "=", true, "", true));
            return productJSON.optString("sku", null);
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
      return null;
   }


   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-form") != null;
   }

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


   private Offers scrapOffers(Document doc, String internalPid) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing, internalPid);

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

   private List<String> scrapSales(Pricing pricing, String internalPid) {
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      String url = HOME_PAGE + "api/1.0/public/offers/search/?skus=" + internalPid;

      Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).build();
      JSONObject response = CrawlerUtils.stringToJSONObject(this.dataFetcher.get(session, request).getBody());

      if (response.has("results")) {
         JSONArray results = response.optJSONArray("results");
         if (results != null) {
            IntStream.range(0, results.length()).forEach(i -> {
               JSONObject result = results.optJSONObject(i);
               if (!result.isNull("seal")) {
                  Object sale = result.optQuery("/seal/title");
                  if (sale instanceof String) {
                     sales.add(sale.toString());
                  }
               }
            });
         }
      }
      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-form .sale-price", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-form .unit-price", null, false, ',', session);
      Double priceCard = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-form .get_card_price", null, false, ',', session);

      if (Objects.equals(priceFrom, spotlightPrice)) priceFrom = null;

      CreditCards creditCards = scrapCreditCards(priceCard, doc);

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
         Element avg = doc.select(".rating-star.ml-auto span").first();

         if (avg != null) {
            String text = avg.className().replaceAll("[^0-9.]", "").replace(",", ".").trim();

            if (!text.isEmpty()) {
               avgRating = Double.parseDouble(text);
            }
         }
      }

      return avgRating;
   }


   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {

      int stars5 = 0;
      int stars4 = 0;
      int stars3 = 0;
      int stars2 = 0;
      int stars1 = 0;

      Elements elements = doc.select("#ratings-tab-list .box-ratings .rating-star span");

      if (!elements.isEmpty()) {
         for (Element element : elements) {

            String star = element.className().replaceAll("[^0-9.]", "").replace(",", ".").trim();

            switch (star) {
               case "5":
                  stars5 += 1;
                  break;
               case "4":
                  stars4 += 1;
                  break;
               case "3":
                  stars3 += 1;
                  break;
               case "2":
                  stars2 += 1;
                  break;
               case "1":
                  stars1 += 1;
                  break;
               default:
                  break;
            }
         }
      }

      return new AdvancedRatingReview.Builder()
         .totalStar1(stars1)
         .totalStar2(stars2)
         .totalStar3(stars3)
         .totalStar4(stars4)
         .totalStar5(stars5)
         .build();

   }


   private Integer getTotalNumOfRatings(Document docRating) {
      int totalRating = 0;
      Element totalRatingElement = docRating.select(".rating-star.ml-auto span").first();

      if (totalRatingElement != null) {
         String text = totalRatingElement.ownText().replaceAll("[^0-9]", "").trim();

         if (!text.isEmpty()) {
            totalRating = Integer.parseInt(text);
         }
      }

      return totalRating;
   }
}
