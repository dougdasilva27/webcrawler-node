package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions;
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
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class WalmartSuperCrawler extends Crawler {

   private static final String HOME_PAGE = "https://super.walmart.com.mx";
   private static final String SELLER_FULL_NAME = "Walmart Super Mexico";
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString());

   public WalmartSuperCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.JSOUP);
      super.config.setParser(Parser.HTML);
   }

   String store_id = session.getOptions().optString("store_id");

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Response fetchResponse() {
      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setProxyservice(
            Arrays.asList(
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_BR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_ES_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_AR_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_DE_HAPROXY))
         .build();
      return CrawlerUtils.retryRequest(request, session, new JsoupDataFetcher(), true);
   }

   JSONObject getJsonFromHtml(Document doc) {
      String script = CrawlerUtils.scrapScriptFromHtml(doc, "#__NEXT_DATA__");
      JSONArray scriptArray = JSONUtils.stringToJsonArray(script);
      Object json = scriptArray.get(0);
      JSONObject jsonObject = (JSONObject) json;
      return JSONUtils.getValueRecursive(jsonObject, "props.pageProps.initialData.data", JSONObject.class, new JSONObject());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();
      JSONObject json = getJsonFromHtml(doc);
      JSONObject productJson = json.optJSONObject("product");
      if (productJson.has("usItemId")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalId = productJson.optString("usItemId");
         String name = productJson.optString("name");
         String primaryImage = JSONUtils.getValueRecursive(productJson, "imageInfo.thumbnailUrl", String.class, null);
         List<String> secondaryImages = crawlSecondaryImages(productJson, primaryImage);
         CategoryCollection categories = crawlCategories(productJson);
         String description = crawlDescription(json);
         List<String> eans = new ArrayList<>();
         eans.add(internalId);
         boolean available = crawlAvailability(productJson);
         Offers offers = available ? crawlOffers(productJson) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setEans(eans)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Offers crawlOffers(JSONObject json) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(json);

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

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      JSONObject spotlightPriceObject = JSONUtils.getValueRecursive(json, "priceInfo.currentPrice", JSONObject.class, new JSONObject());
      JSONObject priceFromObject = JSONUtils.getValueRecursive(json, "priceInfo.wasPrice", JSONObject.class, new JSONObject());
      Double spotlightPrice = null;
      Double priceFrom = null;

      if (!spotlightPriceObject.isEmpty()) {
         int priceInt = spotlightPriceObject.optInt("price", 0);
         spotlightPrice = priceInt != 0 ? priceInt * 1.0 : null;
      }
      if (!priceFromObject.isEmpty()) {
         int priceInt = priceFromObject.optInt("price", 0);
         priceFrom = priceInt != 0 ? priceInt * 1.0 : null;
      }

      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }


   private boolean crawlAvailability(JSONObject apiJson) {
      return apiJson.optString("availabilityStatus").equals("IN_STOCK");
   }

   private List<String> crawlSecondaryImages(JSONObject json, String primaryImage) {
      List<String> secondaryImages = new ArrayList<>();
      JSONArray images = JSONUtils.getValueRecursive(json, "imageInfo.allImages", JSONArray.class, new JSONArray());
      for (Object objImage : images) {
         JSONObject image = (JSONObject) objImage;
         secondaryImages.add(image.optString("url"));
      }
      if (primaryImage != null) {
         secondaryImages.remove(primaryImage);
      }
      return secondaryImages;
   }

   private CategoryCollection crawlCategories(JSONObject json) {
      CategoryCollection categories = new CategoryCollection();
      JSONArray pathsCategories = JSONUtils.getValueRecursive(json, "category.path", JSONArray.class, new JSONArray());
      for (Object objCategory : pathsCategories) {
         JSONObject catrgory = (JSONObject) objCategory;
         categories.add(catrgory.optString("name"));
      }
      return categories;
   }

   private String crawlDescription(JSONObject json) {
      StringBuilder description = new StringBuilder();
//      String longDescription = json.getString("longDescription");
//      longDescription = JSONUtils.
//      if (json.has("longDescription")) {
//         description.append("<div id=\"desc\"> <h3> Descripción </h3>");
//         description.append(json.get("longDescription") + "</div>");
//      }
//      if (json.has("shortDescription")) {
//         description.append("<div id=\"desc\"> <h4> Descripción </h4>");
//         description.append(json.get("longDescription") + "</div>");
//      }

      return description.toString();
   }

   private void setAPIDescription(JSONObject attributesMap, StringBuilder desc) {
      if (attributesMap.has("attrDesc") && attributesMap.has("value")) {
         desc.append("<div>");
         desc.append("<span float=\"left\">" + attributesMap.get("attrDesc") + "&nbsp </span>");
         desc.append("<span float=\"right\">" + attributesMap.get("value") + " </span>");
         desc.append("</div>");
      }
   }

}

