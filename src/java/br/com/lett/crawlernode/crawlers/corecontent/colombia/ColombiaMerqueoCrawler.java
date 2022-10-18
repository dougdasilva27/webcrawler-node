package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
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
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class ColombiaMerqueoCrawler extends Crawler {

   public ColombiaMerqueoCrawler(Session session) {
      super(session);
   }

   private final String zoneId = session.getOptions().optString("zoneId");

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AMEX.toString());
   private static final String SELLER_FULL_NAME = "Merqueo Colombia";

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      JSONObject apiJson = scrapApiJson(session.getOriginalURL());
      JSONObject data = new JSONObject();

      if (apiJson.has("data") && !apiJson.isNull("data")) {
         data = apiJson.getJSONObject("data");
      }

      if (isProductPage(data)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = String.valueOf(data.optInt("id"));
         String name = crawlName(data);
         boolean available = JSONUtils.getValueRecursive(data, "attributes.status", Boolean.class);

         Offers offers = available ? crawlOffers(data) : new Offers();

         String primaryImage = crawlPrimaryImage(data);
         List<String> secondaryImages = crawlSecondaryImage(apiJson);
         String description = JSONUtils.getValueRecursive(data, "attributes.description", String.class);
         String url = assembleProductUrl(session.getOriginalURL());

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(url)
            .setInternalId(internalId)
            .setName(name)
            .setOffers(offers)
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

   private String assembleProductUrl(String originalURL) {
      String slug = getSlug(originalURL);
      return "https://merqueo.com/bogota/" + slug;
   }

   private Offers crawlOffers(JSONObject data) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      Pricing pricing = crawlpricing(data);

      offers.add(Offer.OfferBuilder.create()
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSellerFullName(SELLER_FULL_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());
      return offers;
   }

   private String crawlPrimaryImage(JSONObject data) {
      String primaryImage;

      JSONArray jsonArrImg = JSONUtils.getValueRecursive(data, "attributes.images", JSONArray.class);
      if (jsonArrImg != null && jsonArrImg.length() > 0) {
         primaryImage = getLargestImage(jsonArrImg.get(0) instanceof JSONObject ? jsonArrImg.getJSONObject(0) : new JSONObject());
      } else {
         primaryImage = getLargestImage(data);
      }

      return primaryImage;
   }

   private List<String> crawlSecondaryImage(JSONObject apiJson) {
      JSONArray included = JSONUtils.getValueRecursive(apiJson, "included", JSONArray.class);
      Map<String, JSONObject> attributesId = new HashMap<>();
      for (Object obj : included) {
         JSONObject jsonObject = (JSONObject) obj;
         String code = jsonObject.optString("id");
         JSONObject attributes = jsonObject.optJSONObject("attributes");
         attributesId.put(code, attributes);
      }

      JSONArray jsonImgCode = JSONUtils.getValueRecursive(apiJson, "data.relationships.images.data", JSONArray.class);
      List<String> imagesCode = new ArrayList<>();
      if (jsonImgCode != null) {
         for (int i = 1; i < jsonImgCode.length(); i++) {
            String code = JSONUtils.getValueRecursive(jsonImgCode, i + ".id", String.class);
            imagesCode.add(code);
         }
      }

      List<String> secondaryImages = new ArrayList<>();
      for (String code : imagesCode) {
         JSONObject attributes = attributesId.get(code);
         String imageUrl = attributes.optString("image_large_url");
         secondaryImages.add(imageUrl);
      }

      return secondaryImages;
   }

   private String getLargestImage(JSONObject jsonObjImg) {
      String image = null;
      jsonObjImg = jsonObjImg.optJSONObject("attributes");

      if (jsonObjImg.has("image_large_url") && !jsonObjImg.isNull("image_large_url")) {
         image = jsonObjImg.optString("image_large_url");

      } else if (jsonObjImg.has("image_medium_url") && !jsonObjImg.isNull("image_medium_url")) {
         image = jsonObjImg.optString("image_medium_url");

      } else if (jsonObjImg.has("image_small_url") && !jsonObjImg.isNull("image_small_url")) {
         image = jsonObjImg.optString("image_small_url");
      }
      return image;
   }

   private String crawlName(JSONObject data) {
      String name = JSONUtils.getValueRecursive(data, "attributes.name", String.class);
      if (name != null) {
         String quantity = JSONUtils.getValueRecursive(data, "attributes.quantity", String.class);
         String unit = JSONUtils.getValueRecursive(data, "attributes.unit", String.class);
         if (quantity != null && unit != null) {
            name += " " + quantity + " " + unit;
         }
      }
      return name;
   }

   //https://merqueo.com/api/3.1/stores/63/department/mascotas/shelf/higiene-de-la-mascota/products/shampoo-iki-pets-perros-botella-240-ml?zoneId=40
   private JSONObject scrapApiJson(String originalURL) {
      List<String> slugs = scrapSlugs(originalURL);

      StringBuilder apiUrl = new StringBuilder();
      apiUrl.append("https://merqueo.com/api/3.1/stores/63/");

      if (slugs.size() == 3) {
         apiUrl.append("department/").append(slugs.get(0));
         apiUrl.append("/shelf/").append(slugs.get(1));
         apiUrl.append(slugs.get(2));
      } else {
         apiUrl.append("department/").append(slugs.get(1));
         apiUrl.append("/shelf/").append(slugs.get(2));
         apiUrl.append("/products/").append(slugs.get(3));
      }

      apiUrl.append("?zoneId=");
      apiUrl.append(zoneId);

      Request request = RequestBuilder.create()
         .setUrl(apiUrl.toString())
         .setProxyservice(List.of(ProxyCollection.NETNUT_RESIDENTIAL_BR, ProxyCollection.NETNUT_RESIDENTIAL_CO_HAPROXY))
         .mustSendContentEncoding(false)
         .build();

      return CrawlerUtils.stringToJson(CrawlerUtils.retryRequestString(request, List.of(new ApacheDataFetcher(), new JsoupDataFetcher(), new FetcherDataFetcher()), session));
   }

   /*
    * Url exemple:
    * https://merqueo.com/bogota/aseo-del-hogar/detergentes/ariel-concentrado-doble-poder-detergente-la
    * -quido-2-lt
    */
   private List<String> scrapSlugs(String originalURL) {
      List<String> slugs = new ArrayList<>();
      String slugString = getSlug(originalURL);
      String[] slug = slugString.contains("/") ? slugString.split("/") : null;

      if (slug != null) {
         Collections.addAll(slugs, slug);
      }
      return slugs;
   }

   private String getSlug(String originalURL) {
      String slugString = CommonMethods.getLast(originalURL.split("bogota/"));

      if (slugString.contains("?")) {
         slugString = slugString.split("\\?")[0];
      }

      return slugString;
   }

   private boolean isProductPage(JSONObject data) {
      return data.has("id");
   }

   private Pricing crawlpricing(JSONObject data) throws MalformedPricingException {
      Integer spotLightPriceInt = JSONUtils.getValueRecursive(data, "attributes.special_price", Integer.class);
      Double spotLightprice = null;
      if (spotLightPriceInt != null) {
         spotLightprice = (double) spotLightPriceInt;
         spotLightprice = spotLightprice == 0d ? null : spotLightprice;
      }
      Integer priceFromInt = JSONUtils.getValueRecursive(data, "attributes.price", Integer.class);
      Double priceFrom = null;
      if (priceFromInt != null) {
         priceFrom = (double) priceFromInt;
      }

      priceFrom = priceFrom == 0d ? null : priceFrom;

      if (spotLightPriceInt == null && priceFrom != null) {
         spotLightprice = priceFrom;
         priceFrom = null;
      }

      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotLightprice)
         .build());

      CreditCards creditCards = new CreditCards();
      for (String s : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setInstallments(installments)
            .setBrand(s)
            .setIsShopCard(false)
            .build());
      }

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotLightprice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotLightprice)
            .build())
         .build();

   }
}
