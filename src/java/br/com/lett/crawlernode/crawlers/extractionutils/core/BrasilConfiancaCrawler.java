package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
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

import java.util.*;

public class BrasilConfiancaCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.confianca.com.br/";
   private final String SELLER_FULL_NAME = session.getOptions().optString("seller");
   final private String cityName = session.getOptions().optString("cityName");
   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(), Card.DINERS.toString());

   public BrasilConfiancaCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      JSONObject sites = crawlSites();
      String locationId = crawlLocationId(sites);

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         JSONObject json = crawlProductApi(internalId);
         JSONObject productJson = JSONUtils.getValueRecursive(json, "items/0", "/", JSONObject.class, new JSONObject());

         String internalPid = productJson.optString("barcode");
         String name = JSONUtils.getValueRecursive(productJson, "parentProducts.0.displayName", String.class);
         Integer stock = crawlStock(internalId, locationId);
         boolean isAvailable = stock != null && stock > 0;
         Offers offers = isAvailable ? crawlOffers(productJson) : null;
         String imagePath = JSONUtils.getValueRecursive(productJson, "parentProducts/0/primaryFullImageURL", "/", String.class, null);
         String primaryImage = imagePath != null ? "https://www.confianca.com.br" + imagePath : null;
         List<String> secondaryImages = CrawlerUtils.scrapImagesListFromJSONArray(JSONUtils.getValueRecursive(productJson, "parentProducts/0/smallImageURLs", "/", JSONArray.class, new JSONArray()), null, null, "https", "www.confianca.com.br", session);
         String description = JSONUtils.getValueRecursive(productJson, "parentProducts/0/description", "/", String.class, null);
         CategoryCollection categories = crawlCategories(productJson);
         List<String> eans = List.of(productJson.optString("barcode"));

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setStock(stock)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      return this.session.getOriginalURL().contains(this.cityName) && doc.selectFirst("meta[content=product]") != null;
   }

   private JSONObject crawlProductApi(String internalId) {
      JSONObject json = new JSONObject();

      if (internalId != null) {
         Map<String, String> headers = new HashMap<>();
         headers.put("Referer", session.getOriginalURL());

         String url = "https://www.confianca.com.br/ccstore/v1/skus?skuIds=" + internalId;

         Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
         json = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
      }

      return json;
   }

   private String crawlInternalId(Document doc) {
      String contextString = CrawlerUtils.scrapScriptFromHtml(doc, "script[data-name=occ-structured-data]");
      JSONArray contextJson = CrawlerUtils.stringToJsonArray(contextString);

      return JSONUtils.getValueRecursive(contextJson, "0/0/sku", "/", String.class, null);
   }

   private Integer crawlStock(String internalId, String locationId) {
      Integer stock = null;

      if (internalId != null) {
         Map<String, String> headers = new HashMap<>();
         headers.put("Referer", session.getOriginalURL());

         String url = "https://www.confianca.com.br/ccstore/v1/stockStatus?actualStockStatus=true&locationIds=" + locationId + "&products=" + internalId + ":" + internalId;

         Request request = Request.RequestBuilder.create().setUrl(url).setCookies(cookies).setHeaders(headers).build();
         JSONObject json = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
         if (json != null) {
            stock = JSONUtils.getValueRecursive(json, "items/0/inStockQuantity", "/", Integer.class, null);
         }
      }

      return stock;
   }

   private JSONObject crawlSites() {
      Map<String, String> headers = new HashMap<>();
      headers.put("Referer", session.getOriginalURL());

      String url = "https://www.confianca.com.br/ccstore/v1/sites";

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setHeaders(headers)
         .build();

      return CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());
   }

   private String crawlLocationId(JSONObject sites) {
      String locationId = null;

      if (sites != null) {
         JSONArray items = JSONUtils.getValueRecursive(sites, "items", "/", JSONArray.class, new JSONArray());
         for (int i = 0; i < items.length(); i++) {
            String productionURL = items.optJSONObject(i).optString("productionURL");
            String location = productionURL != null ? CommonMethods.getLast(productionURL.split("/")) : null;
            if (location != null && location.equals(this.cityName)) {
               locationId = items.optJSONObject(i).optString("id");
               break;
            }
         }
      }

      return locationId;
   }

   private Offers crawlOffers(JSONObject productJson) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productJson);
      List<String> sales = scrapSales(pricing);

      if(pricing != null){
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(JSONObject productJson) throws MalformedPricingException {
      Double[] prices = scrapPrices(productJson);
      Double priceFrom = null;
      Double spotlightPrice = null;
      if (prices.length >= 2) {
         priceFrom = prices[0];
         spotlightPrice = prices[1];
      }

      if(spotlightPrice != null){
         CreditCards creditCards = scrapCreditCards(spotlightPrice);
         BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
            .setFinalPrice(spotlightPrice)
            .build();

         return Pricing.PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .setBankSlip(bankSlip)
            .build();
      } else {
         return null;
      }
   }

   private Double[] scrapPrices(JSONObject productJson){
      double spotlightPrice = productJson.optDouble("salePrice", 0.0);
      double priceFrom = productJson.optDouble("listPrice", 0.0);

      if (spotlightPrice == 0.0) {
         spotlightPrice = priceFrom;
      }

      return new Double[]{priceFrom, spotlightPrice};
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
      sales.add(saleDiscount);

      return sales;
   }

   private CategoryCollection crawlCategories(JSONObject productJson) {
      CategoryCollection categories = new CategoryCollection();


      JSONArray parentCategories = JSONUtils.getValueRecursive(productJson, "parentProducts/0/parentCategories", "/", JSONArray.class, new JSONArray());
      for (int i = 0; i < parentCategories.length(); i++) {
         categories.add(parentCategories.optJSONObject(i).optString("repositoryId"));
      }

      return categories;
   }

}
