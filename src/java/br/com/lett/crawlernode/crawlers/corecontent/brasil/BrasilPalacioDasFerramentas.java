package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class BrasilPalacioDasFerramentas extends Crawler {
   private static String SELLER_FULL_NAME = "Palácio das ferramentas";
   private static String HOST = "www.palaciodasferramentas.com.br/";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.ELO.toString(), Card.DINERS.toString());
   private Integer pageRating = 1;
   private Integer star1 = 0;
   private Integer star2 = 0;
   private Integer star3 = 0;
   private Integer star4 = 0;
   private Integer star5 = 0;

   public BrasilPalacioDasFerramentas(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
   }

   @Override
   protected Response fetchResponse() {
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setSendUserAgent(false)
         .setProxyservice(Arrays.asList(
            ProxyCollection.BUY,
            ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_BR))
         .build();
      return CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      Product product = null;

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title span", true);
         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".product-add-form input[name='product']", "value");
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".product.attribute.sku div.value", true);

         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product.media img.gallery-placeholder__image", Arrays.asList("src"), "https", HOST);
         primaryImage = cleanUrlImage(primaryImage);
         List<String> images = scrapImages(doc);

         CategoryCollection categoryCollection = CrawlerUtils.crawlCategories(doc, "div.breadcrumbs ul.items li a", true);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product.attribute.description div"));
         Boolean available = isAvailable(doc);

         Offers offers = available ? scrapOffers(doc) : new Offers(); // I did not found any product having price or avaibility differente because volts or model
         RatingsReviews ratings = crawlRating(doc, internalPid);

         JSONArray variations = getVariations(doc);
         if (variations != null && variations.length() > 1) {
            for (Object variation : variations) {
               JSONObject jsonObj = (JSONObject) variation;
               String voltsOrModel = jsonObj.optString("label");
               String nameVariation = scrapName(name, voltsOrModel);
               String internalIdVariation = (internalId != null) ? (internalId + voltsOrModel) : null;

               product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalIdVariation)
                  .setInternalPid(internalPid)
                  .setName(nameVariation)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(images)
                  .setDescription(description)
                  .setRatingReviews(ratings)
                  .setCategories(categoryCollection)
                  .setOffers(offers)
                  .build();

               products.add(product);
            }
         } else {
            product = ProductBuilder.create().setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setCategories(categoryCollection)
               .setRatingReviews(ratings)
               .setOffers(offers)
               .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> scrapImages(Document doc) {
      Element imageScript = doc.selectFirst("script:containsData(mage/gallery/gallery)");
      if (imageScript != null) {
         JSONObject imageToJson = CrawlerUtils.stringToJson(imageScript.html());
         JSONArray imageArray = JSONUtils.getValueRecursive(imageToJson, "[data-gallery-role=gallery-placeholder].mage/gallery/gallery.data", JSONArray.class, new JSONArray());
         List<String> imagesList = new ArrayList<>();
         for (int i = 1; i < imageArray.length(); i++) {
            String imageUrl = JSONUtils.getValueRecursive(imageArray, i + ".img", String.class);
            imageUrl = cleanUrlImage(imageUrl);
            if (imageUrl != null) {
               imagesList.add(imageUrl);
            }
         }
         return imagesList;
      }
      return null;
   }

   private String cleanUrlImage(String imageUrl) {
      try {
         URL url = new URL(imageUrl);
         String path = url.getPath();
         return url.getProtocol() + "://" + url.getHost() + path;
      } catch (MalformedURLException e) {
         e.printStackTrace();
      }
      return null;
   }


   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".loading-mask") != null;
   }

   private String scrapName(String name, String voltsOrModel) {
      StringBuilder stringBuilder = new StringBuilder();

      if (name != null) {
         stringBuilder.append(name);
         if (voltsOrModel != null && !name.contains(voltsOrModel)) {
            stringBuilder.append(" - ").append(voltsOrModel);
         }
      }

      return stringBuilder.toString();
   }

   private Boolean isAvailable(Document doc) {
      return doc.select(".stock.unavailable").isEmpty();
   }

   private JSONArray getVariations(Document doc) {

      String variationsString = CrawlerUtils.scrapScriptFromHtml(doc, ".fieldset script");
      if (variationsString != null && JSONUtils.stringToJsonArray(variationsString) != null) {
         JSONArray variationsArr = JSONUtils.stringToJsonArray(variationsString);
         JSONObject variationObj = JSONUtils.getValueRecursive(variationsArr, "0.#product_addtocart_form.configurable.spConfig.attributes", JSONObject.class);
         if (variationObj != null) {
            Iterator<String> it = variationObj.keys();
            String key = it.next();
            variationObj = variationObj.optJSONObject(key);
            return variationObj.optJSONArray("options");
         }
         return null;
      } else {
         return null;
      }
   }

   private Offers scrapOffers(Document doc) {
      Offers offers = new Offers();
      try {
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

      } catch (Exception e) {
         Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
      }
      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".pricewithdiscount.final_price .price", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price", null, false, ',', session);
      if (spotlightPrice == null) {
         spotlightPrice = priceFrom;
         priceFrom = null;
      }

      Double priceBankSlip = spotlightPrice;

      if (Objects.equals(priceFrom, spotlightPrice)) priceFrom = null;

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(priceBankSlip)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.DINERS.toString(), Card.ELO.toString(), Card.AMEX.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

   private RatingsReviews crawlRating(Document doc, String internalId) {
      RatingsReviews ratingsReviews = new RatingsReviews();
      Number avgReviews = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, "div.rating [itemprop=\"ratingValue\"]", "content", 0);
      Integer totalReviews = CrawlerUtils.scrapIntegerFromHtmlAttr(doc, "[itemprop=\"reviewCount\"]", "content", 0);

      Document ratingResponse = fetchRatings(internalId);
      Elements comments = ratingResponse.select("body > li");
      Integer currentRating = 0;

      while (comments != null && !comments.isEmpty() && currentRating < totalReviews) {
         for (Element comment : comments) {
            scrapAdvancedRatingReview(comment);
            currentRating++;
         }
         pageRating++;
         ratingResponse = fetchRatings(internalId);
         comments = ratingResponse.select("body > li");
      }

      AdvancedRatingReview advancedRatingReview = new AdvancedRatingReview.Builder()
         .totalStar1(star1)
         .totalStar2(star2)
         .totalStar3(star3)
         .totalStar4(star4)
         .totalStar5(star5)
         .build();

      ratingsReviews.setTotalRating(totalReviews);
      ratingsReviews.setTotalWrittenReviews(totalReviews);
      ratingsReviews.setAverageOverallRating(Objects.isNull(avgReviews) ? null : avgReviews.doubleValue());
      ratingsReviews.setAdvancedRatingReview(advancedRatingReview);

      return ratingsReviews;
   }

   private void scrapAdvancedRatingReview(Element doc) {
      Integer star = CrawlerUtils.scrapIntegerFromHtml(doc, "li.rating option[selected=\"selected\"]", false, 0);

      if (star != null) {
         switch (star) {
            case 1:
               star1++;
               break;
            case 2:
               star2++;
               break;
            case 3:
               star3++;
               break;
            case 4:
               star4++;
               break;
            case 5:
               star5++;
               break;
         }
      }
   }

   protected Document fetchRatings(String internalId) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Accept", "*/*");
      headers.put("Accept-Encoding", "gzip, deflate, br");
      headers.put("Connection", "keep-alive");

      Request request = Request.RequestBuilder.create()
         .setUrl("https://www.palaciodasferramentas.com.br/load/comentarios/" + internalId + "?page=" + pageRating)
         .setHeaders(headers)
         .setProxyservice(Arrays.asList(ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
            ProxyCollection.BUY_HAPROXY))
         .build();

      Response response = this.dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String saleDiscount = CrawlerUtils.calculateSales(pricing);

      if (saleDiscount != null) {
         sales.add(saleDiscount);
      }
      return sales;
   }
}
