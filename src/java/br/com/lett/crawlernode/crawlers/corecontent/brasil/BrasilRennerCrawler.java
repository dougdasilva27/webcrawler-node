package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
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
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

;


/**
 * Date: 14/07/20
 *
 * @author Fellype Layunne
 */
public class BrasilRennerCrawler extends Crawler {

   private static String SELLER_NAME = "Renner";

   public Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilRennerCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
      super.config.setFetcher(FetchMode.JSOUP);
   }

   @Override
   protected Response fetchResponse() {
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setProxyservice(List.of(ProxyCollection.LUMINATI_SERVER_BR, ProxyCollection.BUY))
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(new ApacheDataFetcher(), new FetcherDataFetcher(), new JsoupDataFetcher()), session, "get");

      return response;
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      List<Product> products = new ArrayList<>();

      JSONObject pageJson = CrawlerUtils.selectJsonFromHtml(document, "#__NEXT_DATA__", null, null, false, false);
      JSONObject productJson = JSONUtils.getValueRecursive(pageJson, "props.pageProps.product", JSONObject.class);

      if (productJson != null && !productJson.isEmpty()) {
         String internalId = productJson.optString("skuId");
         String internalPid = productJson.optString("productId");
         String description = getDescription(pageJson);
         String displayName = productJson.optString("displayName");
         List<String> categories = CrawlerUtils.crawlCategories(document, "ul[aria-label='breadcrumb'] li:not(:first-child):not(:last-child) a", true);
         boolean available = productJson.optBoolean("purchasable");
         RatingsReviews ratingsReviews = ratingsReviews(productJson);
         JSONObject productResponse = fetchProductResponse(internalId, internalPid);
         String primaryImage = getPrimaryImage(productResponse);
         List<String> secondaryImages = getSecondaryImages(productResponse);
         Offers offers = available ? scrapOffers(productResponse, internalId, internalPid) : new Offers();

         Elements variations = document.select(".ProductAttributes_ProductAttributeWrapper__aRm_h");

         if (variations.size() > 0) {
            for (Element e : variations) {
               internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".ProductAttributes_inputCheckbox__5Y371", "data-sku");
               String variationName = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, ".ProductAttributes_inputCheckbox__5Y371", "data-name");
               String productName = displayName + " " + variationName;

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(productName)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setCategories(categories)
                  .setRatingReviews(ratingsReviews)
                  .setOffers(offers)
                  .build();
               products.add(product);
            }
         } else {
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(displayName)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setCategories(categories)
               .setRatingReviews(ratingsReviews)
               .setOffers(offers)
               .build();
            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private String getPrimaryImage(JSONObject productResponse) {
      String primaryImage = null;
      JSONArray mediaSets = JSONUtils.getValueRecursive(productResponse, "mediaSets", JSONArray.class);
      if (mediaSets != null && !mediaSets.isEmpty()) {
         JSONObject images = (JSONObject) mediaSets.opt(0);
         if (images != null && !images.isEmpty()) {
            primaryImage = CrawlerUtils.completeUrl(images.optString("largeImageUrl"), "https", "");
         }
      }
      return primaryImage;
   }

   private List<String> getSecondaryImages(JSONObject object) {
      JSONArray mediaSets = JSONUtils.getValueRecursive(object, "mediaSets", JSONArray.class);
      JSONObject images;
      List<String> secondaryImages = new ArrayList<>();
      if (mediaSets != null && !mediaSets.isEmpty()) {
         for (int i = 0; i < mediaSets.length(); i++) {
            images = (JSONObject) mediaSets.opt(i);
            secondaryImages.add(CrawlerUtils.completeUrl(images.optString("largeImageUrl"), "https", ""));
         }
      }
      if (secondaryImages.size() > 0) {
         secondaryImages.remove(0);
      }
      return secondaryImages;
   }

   private String getDescription(JSONObject object) {
      String description = null;
      if (object != null && !object.isEmpty()) {
         JSONObject descriptionObj = JSONUtils.getValueRecursive(object, "props.pageProps.content", JSONObject.class);
         if (descriptionObj != null && !descriptionObj.isEmpty()) {
            JSONArray mainContent = JSONUtils.getValueRecursive(descriptionObj, "mainContent", JSONArray.class);
            if (mainContent != null && !mainContent.isEmpty()) {
               JSONObject metaDescription = JSONUtils.getValueRecursive(mainContent, "0.record.attributes", JSONObject.class);
               if (metaDescription != null && !metaDescription.isEmpty()) {
                  description = metaDescription.optString("prop.product.metaDescription");
               }
            }
         }
      }
      return description;
   }

   private JSONObject fetchProductResponse(String internalId, String internalPid) {
      String urlVariation = "https://www.lojasrenner.com.br/rest/model/lrsa/api/CatalogActor/refreshProductPage?skuId=" + internalId + "&productId=" + internalPid;
      Request request = Request.RequestBuilder.create()
         .setUrl(urlVariation)
         .setProxyservice(List.of(
            ProxyCollection.LUMINATI_SERVER_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR,
            ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY))
         .build();
      Response response = CrawlerUtils.retryRequest(request, session, new FetcherDataFetcher(), true);
      return CrawlerUtils.stringToJson(response.getBody());
   }

   private Offers scrapOffers(JSONObject object, String internalId, String internalPid) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      if (internalId != null && internalPid != null) {
         Pricing pricing = scrapPricing(object);
         List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

         offers.add(new Offer.OfferBuilder()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(this.SELLER_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());
      }
      return offers;
   }

   private Pricing scrapPricing(JSONObject object) throws MalformedPricingException {
      Double spotlightPrice = object.optDouble("salePrice");
      Double priceFrom = object.optDouble("listPrice");
      if (spotlightPrice.isNaN()) {
         spotlightPrice = priceFrom;
         priceFrom = null;
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private CreditCards scrapCreditCards(Double price) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(price)
         .setFinalPrice(price)
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

   private RatingsReviews ratingsReviews(JSONObject object) {
      RatingsReviews ratingReviews = new RatingsReviews();
      JSONObject review = JSONUtils.getValueRecursive(object, "review", JSONObject.class);
      Integer totalNumOfEvaluations = review.optInt("count");
      Double avgRating = review.optDouble("average");

      ratingReviews.setTotalRating(totalNumOfEvaluations);
      ratingReviews.setAverageOverallRating(avgRating);
      ratingReviews.setTotalWrittenReviews(totalNumOfEvaluations);

      return ratingReviews;
   }
}
