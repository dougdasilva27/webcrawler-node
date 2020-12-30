package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer.OfferBuilder;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BrasilAgrosoloCrawler extends Crawler {

   private static final String HOME_PAGE = "www.agrosolo.com.br";

   public BrasilAgrosoloCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {

      String urlToFetch = session.getOriginalURL();

      try {
         URL u = new URL(urlToFetch);
         String query = u.getQuery();

         //Setting parameter to show all comments on the page
         if (query == null) {
            urlToFetch += "?comtodos=s";
         } else if (!query.contains("comtodos")) {
            urlToFetch += "&comtodos=s";
         }
      } catch (Exception e) {
         Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }

      Request request = RequestBuilder.create()
         .setUrl(urlToFetch)
         .setCookies(cookies)
         .build();

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      Elements elements = doc.select(".product-variation > ul > li");

      if (elements.size() > 1) {
         products.addAll(extractVariations(elements));
      } else {
         Product product = extractProduct(doc,
            CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#ctrIdGrade", "value"), null);

         if (product != null) {
            products.add(product);
         }
      }

      return products;
   }

   private List<Product> extractVariations(Elements elements) throws Exception {
      List<Product> products = new ArrayList<>();

      for (Element e : elements) {
         String internalId = CrawlerUtils
            .scrapStringSimpleInfoByAttribute(e, "a[href][idgrade]", "idgrade");
         String variationName = CrawlerUtils
            .scrapStringSimpleInfoByAttribute(e, "a[href][idgrade]", "title");
         boolean availability = !e.hasClass("indisponivel");

         if (internalId != null) {
            Request request = RequestBuilder.create()
               .setUrl(session.getOriginalURL() + "&idgrade=" + internalId)
               .setCookies(cookies)
               .build();

            Document productDoc = Jsoup.parse(this.dataFetcher.get(session, request).getBody());

            Product product = extractProduct(productDoc, internalId, variationName);
            if (product != null) {
               product.setAvailable(availability);
               products.add(product);
            }
         }
      }

      return products;
   }

   private Product extractProduct(Document doc, String internalId, String variationName)
      throws Exception {
      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session,
            "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils
            .scrapStringSimpleInfoByAttribute(doc, "#ctrIdProduto", "value");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-info > h1", true);
         CategoryCollection categories = CrawlerUtils
            .crawlCategories(doc, ".breadcrumb > ul > li", true);
         String primaryImage = CrawlerUtils
            .scrapSimplePrimaryImage(doc, ".product-image .ctrFotoPrincipalZoomNew",
               Collections.singletonList("href"), "https", HOME_PAGE);
         List<String> secondaryImages = scrapSecondaryImages(doc, primaryImage);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays
            .asList("[tab=caracteristicas]", "#caracteristicas", "[tab=especificacoes]",
               "#especificacoes"));
         Integer stock = null;
         String ean = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[ean]", "ean");
         List<String> eans = ean != null && !ean.isEmpty() ? Collections.singletonList(ean) : null;
         RatingsReviews ratingsReviews = scrapRatingReviews(doc);

         Element availabilityElement = doc.selectFirst(".product-buy-area");
         boolean available = availabilityElement != null && !availabilityElement.hasClass("hidden");
         Offers offers = available ? scrapOffers(doc) : new Offers();

         if (variationName != null && !variationName.isEmpty()) {
            name += " - " + variationName;
         }

         // Creating the product
         return ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setStock(stock)
            .setEans(eans)
            .setRatingReviews(ratingsReviews)
            .build();

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return null;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".content-product") != null;
   }

   private List<String> scrapSecondaryImages(Document doc, String primaryImage) {
      List<String> secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      Elements images = doc.select(".images li.ctrFotosVideosFoto > a");
      for (Element e : images) {
         String image = CrawlerUtils
            .sanitizeUrl(e, Collections.singletonList("urlfoto"), "https", HOME_PAGE);

         if (image != null) {

            // Replacing part of the image url to get the larger image
            //This operation is done by the website via javascript
            image = image.replace("/det/", "/original/");

            if ((primaryImage == null || !primaryImage.equals(image))) {
               secondaryImagesArray.put(image);
            }
         }
      }

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = Collections.singletonList(secondaryImagesArray.toString());
      }

      return secondaryImages;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Double price = CrawlerUtils
         .scrapDoublePriceFromHtml(doc, ".new-value .ctrValorMoeda", null, true, ',', session);

      CreditCards creditCards = new CreditCards(
         Stream.of(Card.MASTERCARD, Card.VISA, Card.AMEX, Card.DINERS, Card.ELO,
            Card.HIPERCARD).map(card -> {
            Pair<Integer, Float> installment = CrawlerUtils
               .crawlSimpleInstallment(".parcel", doc, false, "x", "juros", false, ',');
            try {
               return CreditCardBuilder.create()
                  .setBrand(card.toString())
                  .setIsShopCard(false)
                  .setInstallments(new Installments(Collections.singleton(InstallmentBuilder.create()
                     .setInstallmentPrice(installment.getSecond().doubleValue())
                     .setInstallmentNumber(installment.getFirst())
                     .build())))
                  .build();
            } catch (MalformedPricingException e) {
               throw new RuntimeException(e);
            }
         }).collect(Collectors.toList()));

      offers.add(OfferBuilder.create()
         .setIsBuybox(false)
         .setPricing(PricingBuilder.create()
            .setSpotlightPrice(price)
            .setPriceFrom(CrawlerUtils
               .scrapDoublePriceFromHtml(doc, ".old-value .ctrValorDeMoeda", null, true, ',',
                  session))
            .setBankSlip(BankSlipBuilder.create()
               .setFinalPrice(CrawlerUtils
                  .scrapDoublePriceFromHtml(doc, ".ctrValorVistaArea .ctrValorVistaMoeda", null,
                     true, ',',
                     session))
               .build())
            .setCreditCards(creditCards)
            .build())
         .setSellerFullName("Agrosolo Brasil")
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = doc.select(".comments-area > .customer-comment").size();
      Double avgRating = scrapAvgRating(doc);
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private Double scrapAvgRating(Document doc) {
      double avgRating = 0.0;
      Elements elements = doc.select(".detalhe-produto-avaliacao-estrelas > li");

      for (Element e : elements) {
         if (e.hasClass("cheia")) {
            avgRating += 1.0;
         }
      }

      return avgRating;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      int star1 = 0;
      int star2 = 0;
      int star3 = 0;
      int star4 = 0;
      int star5 = 0;

      Elements reviews = doc.select(".comments-area > .customer-comment");

      for (Element review : reviews) {
         Elements stars = review.select(".rating > ul > li.cheia");

         switch (stars.size()) {
            case 1:
               star1 += 1;
               break;
            case 2:
               star2 += 1;
               break;
            case 3:
               star3 += 1;
               break;
            case 4:
               star4 += 1;
               break;
            case 5:
               star5 += 1;
               break;
            default:
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
