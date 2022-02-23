package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.*;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BrasilMartinsCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Martins";
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString());


   public BrasilMartinsCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
      super.config.setParser(Parser.HTML);
   }

   private final String password = getPassword();
   private final String login = getLogin();

   protected String getPassword() {
      return session.getOptions().optString("pass");
   }

   protected String getLogin() {
      return session.getOptions().optString("login");
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      String homePage = "https://www.martinsatacado.com.br";
      return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
   }

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      headers.put("content-type", "application/x-www-form-urlencoded");
      headers.put("referer", session.getOriginalURL());
      headers.put("authority", "www.martinsatacado.com.br");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");

      String payload = "j_username=" + login.replace("@", "%40") + "&j_password=" + password;

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.martinsatacado.com.br/j_spring_security_check")
         .setPayload(payload)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ANY_HAPROXY))

         .setHeaders(headers)
         .build();

      Response response = this.dataFetcher.post(session, request);
      int statusCode = response.getLastStatusCode();

      if (statusCode == 0) {
         try {
            TimeUnit.SECONDS.sleep(2);
         } catch (InterruptedException e) {
            e.printStackTrace();
         }
         Request requestNextTry = Request.RequestBuilder.create()
            .setUrl("https://www.martinsatacado.com.br/j_spring_security_check")
            .setPayload(payload)
            .setProxyservice(Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY
            ))
            .setHeaders(headers)
            .build();
         response = this.dataFetcher.post(session, requestNextTry);
      }
      return response;
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogInfo(
            logger, session, "Product page identified: " + session.getOriginalURL());

         List<String> variations = scrapVariations(doc);

         if (!variations.isEmpty()) {
            for (int i = 0; i < variations.size(); i++) {
               String variation = i == 0 ? "" : variations.get(i);
               products.add(extractProductFromHtml(doc, variation));
            }
         } else {
            products.add(extractProductFromHtml(doc, ""));
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + session.getOriginalURL());
      }

      return products;
   }

   private Product extractProductFromHtml(Document doc, String variation) throws OfferException, MalformedPricingException, MalformedProductException {
      String internalId = CommonMethods.getLast(CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input#id", "value").split("_"));
      String variationInternalId = internalId;
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".qdDetails .title", true);
      Double variationPrice = null;

      if (!variation.isEmpty()) {
         String variationName = StringUtils.substringBetween(variation, "(", ")");
         String variationSlug = CrawlerUtils.toSlug(variationName);
         variationPrice = convertPrice(variation);
         if (!"1-unid".equals(variationSlug)) {
            variationInternalId += "-" + variationSlug;
            name += " " + variationName;
         }
      }

      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:first-child) > a", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".imagePrincipal img", Collections.singletonList("src"), "https", "imgprd.martins.com.br");
      List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".galeryImages img", Collections.singletonList("src"), "https", "imgprd.martins.com.br", primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".qdDetails .cods", ".details", "#especfication", ".body #details"));
      List<String> eans = Collections.singletonList(CrawlerUtils.scrapStringSimpleInfo(doc, ".cods .col-2 p", true));
      RatingsReviews ratingsReviews = scrapRating(doc, internalId);
      boolean available = !doc.select(".js-add-to-cart").isEmpty();
      Offers offers = available ? scrapOffers(doc, variationPrice) : new Offers();

      return ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(variationInternalId)
         .setInternalPid(internalId)
         .setName(name)
         .setCategory1(categories.getCategory(0))
         .setCategory2(categories.getCategory(1))
         .setCategory3(categories.getCategory(2))
         .setPrimaryImage(primaryImage)
         .setRatingReviews(ratingsReviews)
         .setSecondaryImages(secondaryImages)
         .setOffers(offers)
         .setDescription(description)
         .setEans(eans)
         .build();
   }

   private Double convertPrice(String variation) {
      Double price = null;
      String priceString = StringUtils.substringAfter(variation, "R$");
      try {
         price = MathUtils.normalizeTwoDecimalPlaces(MathUtils.parseDoubleWithComma(priceString));
      } catch (Exception e) {
         e.printStackTrace();
      }
      return price;
   }

   private List<String> scrapVariations(Document doc) {
      List<Element> variations = doc.select(".popover .gr-line:not(:first-child) b");
      return variations.stream().map(Element::text).collect(Collectors.toList());
   }

   private RatingsReviews scrapRating(Document doc, String internalId) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      String ratingString =
         CrawlerUtils.scrapStringSimpleInfoByAttribute(
            doc, ".hidden-sm .rating .rating-stars", "data-rating");
      JSONObject jsonRating = JSONUtils.stringToJson(ratingString);

      double avgRating = jsonRating.opt("rating") != null ? jsonRating.optInt("rating") : 0;
      int totalReviews = CrawlerUtils.scrapIntegerFromHtml(doc, ".box-review > div:nth-child(3) u", true, 0);

      ratingsReviews.setTotalRating(totalReviews);
      ratingsReviews.setTotalWrittenReviews(totalReviews);
      ratingsReviews.setAverageOverallRating(avgRating);
      ratingsReviews.setDate(session.getDate());
      ratingsReviews.setInternalId(internalId);
      return ratingsReviews;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select("input#id").isEmpty();
   }

   private Offers scrapOffers(Document doc, Double variationPrice) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc, variationPrice);
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

   private Pricing scrapPricing(Document doc, Double variationPrice) throws MalformedPricingException {
      Double spotlightPrice;

      if (variationPrice != null) {
         spotlightPrice = variationPrice;
      } else {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".qdValue .value", null, true, ',', session);
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      //Site hasn't any product with old price

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
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


}
