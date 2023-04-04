package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Parser;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.extractionutils.generals.BrasilVilaNovaUtils;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;

public class BrasilVilanova extends Crawler {

   public static final String HOME_PAGE = "https://www.vilanova.com.br/";

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilVilanova(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
      super.config.setParser(Parser.HTML);
   }

   public String getSellerFullname() {
      return session.getOptions().optString("seller");
   }

   public String getMarket() {
      return session.getOptions().optString("market");
   }

   public String getToken() {
      return session.getOptions().optString("token");
   }

   private final BrasilVilaNovaUtils brasilVilaNovaUtils = new BrasilVilaNovaUtils(session);

   @Override
   public void handleCookiesBeforeFetch() {
      brasilVilaNovaUtils.login(this.dataFetcher, this.cookies);
   }

   @Override
   protected Response fetchResponse() {

      Response response = new Response();
      try {
         Request request = Request.RequestBuilder.create()
            .setUrl(session.getOriginalURL())
            .setProxy(
               brasilVilaNovaUtils.getFixedIp()
            )
            .setCookies(this.cookies)
            .build();
         response = this.dataFetcher.get(session, request);
      } catch (IOException e) {
         e.printStackTrace();
      }

      return response;
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String description = CrawlerUtils.scrapSimpleDescription(doc, singletonList(".product.attribute.description"));
         String baseName = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title", false);
         List<String> eans = singletonList(CrawlerUtils.scrapStringSimpleInfo(doc, ".ean", true));

         String jsonString = CrawlerUtils.scrapScriptFromHtml(doc, "#product-options-wrapper > div > script");
         JSONArray jsonArray = JSONUtils.stringToJsonArray(jsonString);
         JSONObject json = JSONUtils.getValueRecursive(jsonArray, "0.[data-role=swatch-options].Magento_Swatches/js/swatch-renderer", JSONObject.class);
         if (json != null && !json.isEmpty()) {
            JSONObject jsonProduct = json.optJSONObject("jsonConfig");
            String internalPid = jsonProduct.optString("productId");
            String primaryImage = "";
            List<String> secondaryImages = new ArrayList<>();
            JSONObject objectMarket = brasilVilaNovaUtils.getObjectMarket(jsonProduct);
            JSONArray variationsArray = brasilVilaNovaUtils.getAttributes(jsonProduct, "variant_embalagem");
            for (Object v : variationsArray) {
               JSONObject variation = (JSONObject) v;
               String label = variation.optString("label");
               String name = baseName + " " + label;
               String internalId = brasilVilaNovaUtils.getInternalId(internalPid, label);
               String idProductMarket = brasilVilaNovaUtils.findId(variation, objectMarket);
               JSONArray imagesArray = brasilVilaNovaUtils.getObjectImages(idProductMarket, jsonProduct);
               if (imagesArray.length() > 0) {
                  primaryImage = getPrimaryImageFromJson(imagesArray);
                  secondaryImages = getSecondaryImages(imagesArray, primaryImage);
               } else {
                  primaryImage = scrapImage(doc);
               }
               boolean available = objectMarket.optBoolean("status");
               Offers offers = available ? getOffersJson(idProductMarket, jsonProduct) : new Offers();
               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setOffers(offers)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setEans(eans)
                  .build();

               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String scrapImage(Document doc) {
      String imageFullUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".gallery-placeholder__image", "src");
      int indexPointer = imageFullUrl.indexOf('?');
      return imageFullUrl.substring(0, indexPointer);
   }

   private String getPrimaryImageFromJson(JSONArray images) {
      for (Object o : images) {
         JSONObject objImage = (JSONObject) o;
         if (objImage.optBoolean("isMain")) {
            return brasilVilaNovaUtils.getSanitizedUrl(objImage.optString("img"));
         }
      }
      return null;
   }

   private List<String> getSecondaryImages(JSONArray images, String primaryImage) {
      List<String> secondaryImages = new ArrayList<>();
      for (Object o : images) {
         JSONObject objImage = (JSONObject) o;
         String image = objImage.optString("img");
         if (image != null && !image.isEmpty()) {
            secondaryImages.add(brasilVilaNovaUtils.getSanitizedUrl(image));
         }
      }
      if (primaryImage != null && !primaryImage.isEmpty() && secondaryImages.size() > 0) {
         secondaryImages.remove(primaryImage);
      }
      return secondaryImages;
   }

   private Pricing scrapPricingJSON(JSONObject json) throws MalformedPricingException {
      JSONObject oldPrice = json.optJSONObject("oldPrice");
      JSONObject finalPrice = json.optJSONObject("finalPrice");
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(oldPrice, "amount", false);
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(finalPrice, "amount", false);

      if (priceFrom != null && priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private Offers getOffersJson(String id, JSONObject json) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      JSONObject objectPrices = JSONUtils.getValueRecursive(json, "optionPrices." + id, JSONObject.class, new JSONObject());
      Pricing pricing = scrapPricingJSON(objectPrices);
      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(getSellerFullname())
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());


      return offers;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".product-info-basic").isEmpty();
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
}
