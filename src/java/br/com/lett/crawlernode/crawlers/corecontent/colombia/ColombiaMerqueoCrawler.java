package br.com.lett.crawlernode.crawlers.corecontent.colombia;

import br.com.lett.crawlernode.core.fetcher.methods.FetcherDataFetcher;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ColombiaMerqueoCrawler extends Crawler {

   public ColombiaMerqueoCrawler(Session session) {
      super(session);
   }

   private final String zoneId = session.getOptions().optString("zoneId");

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AMEX.toString());

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

         String internalId = data.getString("id");
         String name = crawlName(data);
         boolean available = data.optBoolean("availability");

         CategoryCollection categories = crawlCategories(data);

         Offers offers = available ? crawlOffers(data) : new Offers();

         String primaryImage = crawlPrimaryImage(data);
         List<String> secondaryImages = crawlSecondaryImage(data);
         String description = data.optString("description");
         Integer stock = crawlStock(data);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setOffers(offers)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setStock(stock)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private Offers crawlOffers(JSONObject data) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      Pricing pricing = crawlpricing(data);


      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("Mer")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(data.optBoolean("isMarketplace", false))
         .setPricing(pricing)
         .build());


      return offers;
   }

   private Integer crawlStock(JSONObject data) {
      return data.optInt("quantity", 0);
   }

   private CategoryCollection crawlCategories(JSONObject data) {
      CategoryCollection categories = new CategoryCollection();
      JSONObject shelf = data.getJSONObject("shelf");

      if (shelf != null && shelf.has("name")) {
         categories.add(shelf.optString("name"));
      }
      shelf = data.getJSONObject("department");

      if (shelf != null && shelf.has("name")) {
         categories.add(shelf.optString("name"));
      }

      return categories;
   }


   private String crawlPrimaryImage(JSONObject data) {
      String primaryImage;

      JSONArray jsonArrImg = JSONUtils.getJSONArrayValue(data, "images");
      if (jsonArrImg.length() > 0) {
         primaryImage = getLargestImage(jsonArrImg.get(0) instanceof JSONObject ? jsonArrImg.getJSONObject(0) : new JSONObject());
      } else {
         primaryImage = getLargestImage(data);
      }

      return primaryImage;
   }

   private List<String> crawlSecondaryImage(JSONObject data) {
      JSONArray jsonArrImg = JSONUtils.getJSONArrayValue(data, "images");

      List<String> secondaryImages = new ArrayList<>();

      for (int i = 1; i < jsonArrImg.length(); i++) {
         JSONObject jsonObjImg = jsonArrImg.get(i) instanceof JSONObject ? jsonArrImg.getJSONObject(i) : new JSONObject();
         secondaryImages.add(getLargestImage(jsonObjImg));
      }


      return secondaryImages;
   }

   private String getLargestImage(JSONObject jsonObjImg) {
      String image = null;

      if (jsonObjImg.has("imageLargeUrl") && !jsonObjImg.isNull("imageLargeUrl")) {
         image =  jsonObjImg.getString("imageLargeUrl");

      } else if (jsonObjImg.has("imageMediumUrl") && !jsonObjImg.isNull("imageMediumUrl")) {
         image = jsonObjImg.getString("imageMediumUrl");

      } else if (jsonObjImg.has("imageSmallUrl") && !jsonObjImg.isNull("imageSmallUrl")) {
         image = jsonObjImg.getString("imageSmallUrl");
      }
      return image;
   }



   private String crawlName(JSONObject data) {
      StringBuilder name = new StringBuilder();
      name.append(data.optString("name")).append(" ");
      name.append(data.optString("quantity")).append(" ");
      name.append(data.optString("unit"));

      return name.toString();
   }

   private JSONObject scrapApiJson(String originalURL) {
      List<String> slugs = scrapSlugs(originalURL);

      StringBuilder apiUrl = new StringBuilder();
      apiUrl.append("https://merqueo.com/api/2.0/stores/63/find?");

      if (slugs.size() == 3) {
         apiUrl.append("department_slug=").append(slugs.get(0));
         apiUrl.append("&shelf_slug=").append(slugs.get(1));
         apiUrl.append("&product_slug=").append(slugs.get(2));
      } else {
         apiUrl.append("department_slug=").append(slugs.get(1));
         apiUrl.append("&shelf_slug=").append(slugs.get(2));
         apiUrl.append("&product_slug=").append(slugs.get(3));
      }

      apiUrl.append("&limit=7&zoneId=");
      apiUrl.append(zoneId);
      apiUrl.append("&adq=1");

      Request request = RequestBuilder
         .create()
         .setUrl(apiUrl.toString())
         .mustSendContentEncoding(false)
         .build();


      return CrawlerUtils.stringToJson(new FetcherDataFetcher().get(session, request).getBody());
   }

   /*
    * Url exemple:
    * https://merqueo.com/bogota/aseo-del-hogar/detergentes/ariel-concentrado-doble-poder-detergente-la
    * -quido-2-lt
    */
   private List<String> scrapSlugs(String originalURL) {
      List<String> slugs = new ArrayList<>();
      String slugString = CommonMethods.getLast(originalURL.split("bogota/"));
      String[] slug = slugString.contains("/") ? slugString.split("/") : null;

      if (slug != null) {
         Collections.addAll(slugs, slug);
      }
      return slugs;
   }

   private boolean isProductPage(JSONObject data) {
      return data.has("id");
   }


   private Pricing crawlpricing(JSONObject data) throws MalformedPricingException {

      Double spotLightprice = (double) data.optInt("specialPrice",0);
      Double priceFrom = (double) data.optInt("specialPrice",0);



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
