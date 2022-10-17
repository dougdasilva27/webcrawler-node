package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.BankSlip;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;
import org.apache.kafka.common.security.JaasUtils;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilPetcenterexpressCrawler extends Crawler {

   private static final String SELLER_NAME = "Petcenter Express Brasil";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public BrasilPetcenterexpressCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {
      // We append this parameter for load all coments to capture rating and reviews
      String url = session.getOriginalURL().contains("?") ? session.getOriginalURL() + "&comtodos=s" : session.getOriginalURL() + "?comtodos=s";

      Request request = RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .build();

      return Jsoup.parse(this.dataFetcher.get(session, request).getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "[itemprop=\"sku\"]", true);
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-product__name", true);
         boolean available = scrapAvailable(internalId);
         //hasn't category
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".container_thumb.cloud-zoom", Arrays.asList("href"), "https", "static.petcenterexpress.com.br");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".jcarousel-skin-tango li span.produto-imagem-miniatura a", Arrays.asList("href"), "https", "static.petcenterexpress.com.br", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".additional-message"));
         RatingsReviews rating = scrapRatingReviews(doc);
         Offers offers = available ? scrapOffers(doc) : new Offers();


         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .setRatingReviews(rating)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean scrapAvailable(String internalId) {
      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.petcenterexpress.com.br/pricing/1059813/1/" + internalId + "/?snippet=snippets/pricing")
         .build();

      Response response = this.dataFetcher.get(session, request);
      Document doc = Jsoup.parse(response.getBody());
      if (doc != null) {
         String available = CrawlerUtils.scrapStringSimpleInfo(doc, ".botao-nao_indisponivel", false);
         if (available == null) {
            return true;
         }

      }
      return false;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      //site hasn't sale

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());


      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double spotlightPrice = scrapPriceInJson(doc);
      //site hasn't old price
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private Double scrapPriceInJson(Document doc) {
      Elements listScript = doc.select("script");
      for (int i = 0; i < listScript.size(); i++) {
         String script = String.valueOf(listScript.get(i));
         if (script.contains("dataLayer ")) {
            script = script.replaceAll("dataLayer =", "").replaceAll("<script>", "").replaceAll("</script>", "");
            if (script != null && !script.isEmpty()) {
               JSONArray jsonArray = CrawlerUtils.stringToJsonArray(script);
               String priceStr = JSONUtils.getValueRecursive(jsonArray, "0.price", String.class);
               if (priceStr != null && !priceStr.isEmpty()) {
                  return Double.parseDouble(priceStr);
               }
            }

         }

      }
      return null;
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


   private boolean isProductPage(Document doc) {
      return !doc.select(".page-product__essential").isEmpty();
   }

   private RatingsReviews scrapRatingReviews(Document doc) {
      RatingsReviews ratingReviews = new RatingsReviews();
      ratingReviews.setDate(session.getDate());

      Integer totalNumOfEvaluations = doc.select(".customer-comment .comentarios-score-lista").size();
      Integer totalWrittenReviews = totalNumOfEvaluations;
      AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(doc);
      Double avgRating = CrawlerUtils.extractRatingAverageFromAdvancedRatingReview(advancedRatingReview);

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalWrittenReviews);
      ratingReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(Document doc) {
      Integer star1 = 0;
      Integer star2 = 0;
      Integer star3 = 0;
      Integer star4 = 0;
      Integer star5 = 0;

      Elements reviews = doc.select(".customer-comment .comentarios-score-lista");

      for (Element review : reviews) {
         Integer val = review.select(".cheia").size();

         switch (val) {
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
