package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import cdjd.com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MexicoFarmaciasanpabloCrawler extends Crawler {
   private static final String SELLER_NAME = "Farmacia San Pablo";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString());


   public MexicoFarmaciasanpabloCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
      super.config.setParser(Parser.JSON);
   }

   @Override
   protected Response fetchResponse() {
      String url = scrapUrlApi();
      Request request = Request.RequestBuilder.create()
         .setUrl(this.session.getOriginalURL())
         .setProxyservice
            (List.of(ProxyCollection.SMART_PROXY_MX,
               ProxyCollection.SMART_PROXY_MX_HAPROXY,
               ProxyCollection.NETNUT_RESIDENTIAL_MX,
               ProxyCollection.NETNUT_RESIDENTIAL_MX_HAPROXY))
         .setUrl(url)
         .build();

      Response response = CrawlerUtils.retryRequestWithListDataFetcher(request, List.of(dataFetcher, new FetcherDataFetcher(), new JsoupDataFetcher()), session, "get");
      return response;
   }


   @Override
   public List<Product> extractInformation(JSONObject object) throws Exception {
      List<Product> products = new ArrayList<>();
      if (object != null && !object.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = getInternalId();
         String name = getName(object);
         String primaryImage = getPrimaryImage(object);
         List<String> secondaryImages = getSecondaryImages(object);
         List<String> categories = getCategories(object);
         String description = getDescripton(object);
         boolean available = object.optBoolean("purchasable");
         Offers offers = available ? scrapOffers(object) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private String getInternalId() {
      String internalId = CommonMethods.getLast(this.session.getOriginalURL().split("/"));
      if (internalId != null && !internalId.isEmpty()) {
         return internalId;
      }
      return null;
   }

   private String scrapUrlApi() {
      String urlApi = CommonMethods.getLast(this.session.getOriginalURL().split("/"));
      if (urlApi != null && !urlApi.isEmpty()) {
         urlApi = "https://api.farmaciasanpablo.com.mx/rest/v2/fsp/products/" + this.getInternalId() + "?lang=es_MX&curr=MXN";
      }
      return urlApi;
   }

   private String getName(JSONObject object) {
      StringBuilder fullName = new StringBuilder();
      String name = object.optString("name");
      String additionalDescription = object.optString("additionalDescription");
      if (name != null && !name.isEmpty()) {
         fullName.append(name);
      }
      if (additionalDescription != null && !additionalDescription.isEmpty()) {
         fullName.append(" " + additionalDescription);
      }
      return fullName.toString();
   }

   private String getDescripton(JSONObject object) {
      StringBuilder fullDescription = new StringBuilder();
      String name = object.optString("name");
      String description = object.optString("description");
      String additionalDescription = object.optString("additionalDescription");

      if (name != null && !name.isEmpty()) {
         fullDescription.append(name);
      }
      if (description != null && !description.isEmpty()) {
         fullDescription.append(description);
      }
      if (additionalDescription != null && !additionalDescription.isEmpty()) {
         fullDescription.append(additionalDescription);
      }
      return fullDescription.toString();
   }

   private String getPrimaryImage(JSONObject object) {
      String imageUrl = null;
      JSONArray imageArray = JSONUtils.getValueRecursive(object, "images", JSONArray.class);
      if (imageArray != null && !imageArray.isEmpty()) {
         JSONObject objArray = JSONUtils.getValueRecursive(imageArray, "0", JSONObject.class);
         imageUrl = JSONUtils.getValueRecursive(objArray, "url", String.class);
      }
      return imageUrl;
   }

   private List<String> getCategories(JSONObject object) {
      List<String> categories = new ArrayList<>();
      JSONArray arrayCategories = JSONUtils.getValueRecursive(object, "categories", JSONArray.class);
      if (categories != null && categories.isEmpty()) {
         for (Object e : arrayCategories) {
            JSONObject objectCategory = (JSONObject) e;
            String category = objectCategory.optString("name");
            categories.add(category);
         }
      }
      return categories;
   }

   private List<String> getSecondaryImages(JSONObject object) {
      List<String> secondaryImages = new ArrayList<>();
      JSONObject galleryImages = JSONUtils.getValueRecursive(object, "galleryImages", JSONObject.class);
      if (galleryImages != null && !galleryImages.isEmpty()) {
         JSONObject imageObject = galleryImages.optJSONObject("frontView");
         if (imageObject != null && !imageObject.isEmpty()) {
            JSONObject superZoom = JSONUtils.getValueRecursive(imageObject, "superZoom", JSONObject.class);
            if (superZoom != null && !superZoom.isEmpty()) {
               String url = superZoom.optString("url");
               secondaryImages.add(url);
            }

         }
         imageObject = galleryImages.optJSONObject("reverseView");
         if (imageObject != null && !imageObject.isEmpty()) {
            JSONObject superZoom = JSONUtils.getValueRecursive(imageObject, "superZoom", JSONObject.class);
            if (superZoom != null && !superZoom.isEmpty()) {
               String url = superZoom.optString("url");
               secondaryImages.add(url);
            }

         }
         imageObject = galleryImages.optJSONObject("sideView1");
         if (imageObject != null && !imageObject.isEmpty()) {
            JSONObject superZoom = JSONUtils.getValueRecursive(imageObject, "superZoom", JSONObject.class);
            if (superZoom != null && !superZoom.isEmpty()) {
               String url = superZoom.optString("url");
               secondaryImages.add(url);
            }

         }
      }
      return secondaryImages;
   }

   private Offers scrapOffers(JSONObject object) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(object);
      List<String> sales = new ArrayList<>();
      String sale = CrawlerUtils.calculateSales(pricing);
      sales.add(sale);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Double getSpotLightPrice(JSONObject object) {
      Double spotlightPrice = null;
      JSONObject scrapSpotlightPrice = JSONUtils.getValueRecursive(object, "basePrice", JSONObject.class);
      if (scrapSpotlightPrice != null && !scrapSpotlightPrice.isEmpty()) {
         spotlightPrice = scrapSpotlightPrice.optDouble("value");
      }
      return spotlightPrice;
   }

   private Double getPriceFrom(JSONObject object) {
      Double priceFrom = null;
      JSONObject scrapPriceFrom = JSONUtils.getValueRecursive(object, "price", JSONObject.class);
      if (scrapPriceFrom != null && !scrapPriceFrom.isEmpty()) {
         priceFrom = scrapPriceFrom.optDouble("value");
      }
      return priceFrom;
   }

   private Pricing scrapPricing(JSONObject object) throws MalformedPricingException {
      Double spotlightPrice = getSpotLightPrice(object);
      Double priceFrom = getPriceFrom(object);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom != null && !priceFrom.equals(spotlightPrice) ? priceFrom : null)
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

      for (String brand : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }
}
