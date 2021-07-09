package br.com.lett.crawlernode.crawlers.corecontent.guatemala;

import br.com.lett.crawlernode.core.fetcher.methods.JsoupDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewImpl;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GuatemalaCemacoCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   private static final String SELLER_NAME = "cemaco";

   public GuatemalaCemacoCrawler(Session session) {
      super(session);
   }

   @Override
   protected JSONObject fetch() {
      String id = getProductId();
      Map<String, String> headers = getHeaders();
      String API = "https://www.cemaco.com/produto/sku/" + id;

      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setHeaders(headers)
         .build();
      String content = this.dataFetcher
         .get(session, request)
         .getBody();

      JSONArray jsonArray = CrawlerUtils.stringToJsonArray(content);

      return (JSONObject) jsonArray.get(0);
   }


   private String getProductId() {
      String url = null;
      Pattern pattern = Pattern.compile("\\-([0-9]*)\\/p");
      Matcher matcher = pattern.matcher(session.getOriginalURL());
      if (matcher.find()) {
         url = matcher.group(1);
      }
      return url;
   }

   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (!json.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = getProductId();
         String internalPid = json.optString("IdProduct");
         String name = json.optString("Name");
         JSONArray imageArray = (JSONArray) json.optJSONArray("Images");
         List<String> images = getImages(imageArray);
         String primaryImage = images != null && !images.isEmpty() ? images.remove(0) : null;
         String description = JSONUtils.getValueRecursive(json, "0.x_caracteristicasHtml", String.class);
         boolean available = json.optBoolean("Availability");
         Offers offers = available ? scrapOffers(json) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> getImages(JSONArray imageArray) {
      List<String> listImages = new ArrayList<>();
      if (imageArray != null) {
         for (Object objArray : imageArray) {
            if (objArray instanceof JSONArray){
                  JSONObject jsonObject = (JSONObject) ((JSONArray) objArray).get(0);
                  listImages.add(jsonObject.optString("Path"));
               }

         }
      }

      return listImages;
   }


   private Offers scrapOffers(JSONObject productInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productInfo);
      List<String> sales = scrapSales(pricing);

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

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String salesOnJson = CrawlerUtils.calculateSales(pricing);
      sales.add(salesOnJson);
      return sales;
   }

   private Map<String, String> getHeaders() {
      Map<String, String> headers = new HashMap<>();
      headers.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
      headers.put("Referer", session.getOriginalURL());
      headers.put("content-type", "application/json; charset=UTF-8");

      return headers;
   }


   private Pricing scrapPricing(JSONObject productInfo) throws MalformedPricingException {

      Double priceFrom = JSONUtils.getValueRecursive(productInfo, "ListPrice", Double.class);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(productInfo, "Price", true);
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
