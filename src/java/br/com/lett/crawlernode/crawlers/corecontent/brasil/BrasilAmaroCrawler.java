package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BrasilAmaroCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Amaro";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString(), Card.AMEX.toString(),
      Card.DINERS.toString(), Card.DISCOVER.toString(), Card.JCB.toString(), Card.AURA.toString(), Card.HIPERCARD.toString());

   public BrasilAmaroCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   public List<Product> extractInformation(Document document) throws Exception {
      super.extractInformation(document);
      List<Product> products = new ArrayList<>();

      if (isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = scrapInternal("/([0-9]*_[0-9]*)/");
         String internalPid = scrapInternal("/(\\d+)_");

         String name = crawlName(document);

         List<String> imagesList = crawlImages(document);
         String primaryImage = !imagesList.isEmpty() ? imagesList.remove(0) : null;

         CategoryCollection categories = CrawlerUtils.crawlCategories(document, "a[class*=Breadcrumb_link]", true);
         String description = CrawlerUtils.scrapElementsDescription(document, Arrays.asList("[class*=Description_apiMessage] p", "[class*=Description_apiMessage] ul li"));
         RatingsReviews ratingsReviews = crawlRating(internalPid);

         boolean availableToBuy = document.selectFirst("[class*=OutOfStockDesktop_titleWrapper]") == null;
         Offers offers = availableToBuy ? scrapOffers(document) : new Offers();

         Elements variants = document.select("[class*=SizeSelectFormGroup_sizeOptionsList] div");

         if (variants.size() > 1) {
            for (Element variant : variants) {
               String size = CrawlerUtils.scrapStringSimpleInfo(variant, "label", true);
               String variantName = name + " - " + size;
               String variantInternalId = internalId + "_" + size;
               availableToBuy = variant.selectFirst("label[class*=RadioButton_unavailable]") == null;
               offers = availableToBuy ? offers : null;

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(variantInternalId)
                  .setInternalPid(internalPid)
                  .setName(variantName)
                  .setCategories(categories)
                  .setDescription(description)
                  .setRatingReviews(ratingsReviews)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(imagesList)
                  .setOffers(offers)
                  .build();

               products.add(product);
            }

         } else {

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategories(categories)
               .setDescription(description)
               .setRatingReviews(ratingsReviews)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(imagesList)
               .setOffers(offers)
               .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String crawlName(Document document) {
      String name = CrawlerUtils.scrapStringSimpleInfo(document, "h1[class*=Heading_heading]", true);

      if (name != null) {
         String color = CrawlerUtils.scrapStringSimpleInfo(document, "[class*=selectedColor]", true);
         if (color != null) {
            return name + " - " + color;
         }

         return name;
      }

      String outStockName = CrawlerUtils.scrapStringSimpleInfo(document, "[class*=OutOfStockDesktop] p[class*=Paragraph_paragraph] strong", true);
      return outStockName;
   }

   private String scrapInternal(String pattern) {
      Pattern regexPattern = Pattern.compile(pattern);
      Matcher matcher = regexPattern.matcher(session.getOriginalURL());

      if (matcher.find()) {
         return matcher.group(1);
      }

      return null;
   }

   private List<String> crawlImages(Document document) {
      List<String> images = new ArrayList<>();
      JSONObject json = CrawlerUtils.selectJsonFromHtml(document, "#__NEXT_DATA__", null, " ", false, false);
      JSONObject data = findData(json);
      if (data != null) {
         JSONArray imagesList = data.optJSONArray("images");
         for (int i = 0; i < imagesList.length(); i++) {
            JSONObject image = imagesList.optJSONObject(i);
            String imageUrl = JSONUtils.getStringValue(image, "url");
            images.add(imageUrl);
         }
      }
      return images;
   }

   private JSONObject findData(JSONObject json) {
      JSONArray arr = JSONUtils.getValueRecursive(json, "props.pageProps.dehydratedState.queries", JSONArray.class);
      for (Object obj : arr) {
         JSONObject jObj = (JSONObject) obj;
         if (JSONUtils.getValueRecursive(jObj, "state.data.baseProduct", String.class) != null) {
            return JSONUtils.getValueRecursive(jObj, "state.data", JSONObject.class);
         }
      }
      return null;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("div[class*=ProductView_container]") != null || doc.selectFirst("div[class*=OutOfStockDesktop_wrapperDesktop]") != null;
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
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "strong[class*=Prices_value]", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div[class*=Prices_oldPrice]", null, true, ',', session);
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
      RatingsReviews ratingsReviews = new RatingsReviews();
      String url = "https://api-cdn.yotpo.com/v1/widget/0eU0TYNuFzWbeD60wD2lB7UWnCbAkVtX3vwtaEH0/products/" + internalPid + "/reviews";

      Map<String, String> headers = new HashMap<>();
      headers.put("Connection", "keep-alive");
      headers.put("authority", "api-cdn.yotpo.com");
      headers.put("content-type", "application/json");
      headers.put("origin", "https://amaro.com");

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setHeaders(headers)
         .setSendUserAgent(true)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.LUMINATI_SERVER_BR))
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
