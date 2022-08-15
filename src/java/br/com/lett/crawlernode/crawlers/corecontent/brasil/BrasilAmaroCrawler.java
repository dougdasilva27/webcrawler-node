package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BrasilAmaroCrawler extends Crawler {

   private final String HOME_PAGE = "https://amaro.com/br/pt/";
   private static final String SELLER_FULL_NAME = "Amaro";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.AMEX.toString(), Card.DINERS.toString(), Card.DISCOVER.toString(),
      Card.JCB.toString(), Card.AURA.toString(), Card.HIPERCARD.toString());

   public BrasilAmaroCrawler(Session session) {
      super(session);
   }

   @Override
   protected Object fetch() {
      Document doc = new Document("");
      int attempt = 0;
      boolean sucess = false;
      List<String> proxies = List.of(ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY, ProxyCollection.BUY_HAPROXY, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY);
      do {
         try {
            Logging.printLogDebug(logger, session, "Fetching page with webdriver...");

            webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), proxies.get(attempt), session);
            doc = Jsoup.parse(webdriver.getCurrentPageSource());

            sucess = doc.selectFirst("div[class*=ProductView_container]") != null;
            attempt++;
            webdriver.waitLoad(10000);

         } catch (Exception e) {
            Logging.printLogDebug(logger, session, CommonMethods.getStackTrace(e));
            Logging.printLogWarn(logger, "Página não capturada");
         }

      } while (attempt < 3 && !sucess);

      return doc;
   }

   public List<Product> extractInformation(Document document) throws Exception {
      super.extractInformation(document);
      List<Product> products = new ArrayList<>();

      if (isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String productCode = CrawlerUtils.scrapStringSimpleInfo(document, "p[class*=Description_productCodeInformation]", true);
         Matcher matcher = getCrawlId(productCode);

         if (matcher != null) {
            String internalId = matcher.group(1);
            String internalPid = matcher.group(2);
            String name = CrawlerUtils.scrapStringSimpleInfo(document, "h1[class*=Heading_heading]", true);
            CategoryCollection categories = CrawlerUtils.crawlCategories(document, "a[class*=ProductBreadcrumb_link]", true);
            String description = CrawlerUtils.scrapSimpleDescription(document, Arrays.asList("div[class*=Description_apiMessage]"));
            RatingsReviews ratingsReviews = crawlRating(internalPid);
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(document, ".images-wrap > div:last-child img.gallery_image", Arrays.asList("src"), "https", "amarotech-res.cloudinary.com");
            List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(document, ".images-wrap > div:not([data-index = '-1']):not([data-index = '0']):not(:last-child) .gallery_image", Arrays.asList("src"), "https", "amarotech-res.cloudinary.com", primaryImage);
            boolean availableToBuy = !document.select("button[id*=add-to-cart-button]").isEmpty();
            Offers offers = availableToBuy ? scrapOffers(document) : new Offers();

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategories(categories)
               .setDescription(description)
               .setRatingReviews(ratingsReviews)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setOffers(offers)
               .build();

            products.add(product);

         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Matcher getCrawlId(String code) {
      String regex = "(([0-9]+)\\_[0-9]+)";
      final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(code);
      if (matcher.find()) {
         return matcher;
      }
      return null;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("div[class*=ProductView_container]") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setSales(sales)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "strong[class*=ProductOptions]", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div[class*=oldPrice]", null, true, ',', session);
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

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String saleDiscount = CrawlerUtils.calculateSales(pricing);

      if (saleDiscount != null) {
         sales.add(saleDiscount);
      }

      return sales;
   }

   private RatingsReviews crawlRating(String internalPid) {
      String url = "https://api-cdn.yotpo.com/v1/widget/C3WJEQUAevWXtzD53PwS4IFnSgbOtw3MkvQXWmJj/products/" + internalPid + "/reviews";
      RatingsReviews ratingsReviews = new RatingsReviews();

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setSendUserAgent(true)
         .build();
      Response response = new FetcherDataFetcher().get(session, request);
      JSONObject jsonObject = JSONUtils.stringToJson(response.getBody());
      JSONObject aggregationRating = (JSONObject) jsonObject.optQuery("/response/bottomline");

      if (aggregationRating != null) {
         AdvancedRatingReview advancedRatingReview = scrapAdvancedRatingReview(aggregationRating);

         ratingsReviews.setTotalRating(aggregationRating.optInt("total_review"));
         ratingsReviews.setAdvancedRatingReview(advancedRatingReview);
         ratingsReviews.setAverageOverallRating(aggregationRating.optDouble("average_score", 0d));
      }
      return ratingsReviews;
   }

   private AdvancedRatingReview scrapAdvancedRatingReview(JSONObject reviews) {

      JSONObject reviewValue = reviews.optJSONObject("star_distribution");

      if (reviewValue != null) {
         return new AdvancedRatingReview.Builder()
            .totalStar1(reviewValue.optInt("1"))
            .totalStar2(reviewValue.optInt("2"))
            .totalStar3(reviewValue.optInt("3"))
            .totalStar4(reviewValue.optInt("4"))
            .totalStar5(reviewValue.optInt("5"))
            .build();
      }

      return new AdvancedRatingReview();
   }
}
