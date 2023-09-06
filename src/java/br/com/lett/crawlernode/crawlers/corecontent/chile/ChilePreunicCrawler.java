package br.com.lett.crawlernode.crawlers.corecontent.chile;

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
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChilePreunicCrawler extends Crawler {

   public ChilePreunicCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      JSONObject jsonScript = getJsonFromScript(doc);

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = jsonScript.optString("groupId");
         JSONObject productsSkus = jsonScript.optJSONObject("products");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("div.description-tabs"));
         CategoryCollection categories = scrapCategories(doc);
         for (Iterator<String> it = productsSkus.keys(); it.hasNext(); ) {
            String internalId = it.next();
            Object valueKey = productsSkus.opt(internalId);
            if (valueKey instanceof JSONObject) {
               JSONObject variation = (JSONObject) valueKey;
               String url = variation.optString("url");
               String name = scrapNameWithBrand(variation, jsonScript);
               String primaryImage = crawlPrimaryImage(variation);
               List<String> secondaryImages = crawlSecondaryImages(variation, doc);
               boolean available = jsonScript.optBoolean("isAvailable");
               Offers offers = available ? scrapOffers(variation) : new Offers();

               Product product = ProductBuilder.create()
                  .setUrl(url)
                  .setInternalId(internalId)
                  .setInternalPid(internalPid)
                  .setName(name)
                  .setOffers(offers)
                  .setPrimaryImage(primaryImage)
                  .setSecondaryImages(secondaryImages)
                  .setDescription(description)
                  .setCategories(categories)
                  .build();

               products.add(product);
            }
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private String crawlPrimaryImage(JSONObject variation) {
      String primaryImage = variation.optString("pictureUrl");
      if (primaryImage != null && !primaryImage.isEmpty()) {
        primaryImage = primaryImage.replace("product/", "large/");
      }

      return primaryImage;
   }

   private JSONObject getJsonFromScript(Document doc) {
      Element script = doc.selectFirst(".col-sm-12 script");
      String matcherToConvert = null;

      if (script != null) {
         Pattern pattern = Pattern.compile("\\((.+?)\\);");
         Matcher matcher = pattern.matcher(script.toString());
         if (matcher.find()) {
            matcherToConvert = matcher.group(1);
         }
      }

      return CrawlerUtils.stringToJson(matcherToConvert);
   }

   private List<String> crawlSecondaryImages(JSONObject variation, Document doc) {
      Integer variantId = JSONUtils.getValueRecursive(variation, "params.variant_id", Integer.class);
      List<String> secondaryImages = new ArrayList<>();
      Elements images = doc.select(".products-vertical.slider div a[data-id='" + variantId.toString() + "'] img");
      if (!images.isEmpty()) {
         for (Element e : images) {
            secondaryImages.add(e.attr("src"));
         }
      }

      return secondaryImages;
   }

   private String scrapNameWithBrand(JSONObject variation, JSONObject jsonScript) {
      StringBuilder nameComplete = new StringBuilder();
      String name = variation.optString("name");
      String brand = jsonScript.optString("vendor");
      String params = JSONUtils.getValueRecursive(variation, "params.option_text", String.class);

      if (name != null) {
         nameComplete.append(name).append(" ");
         if (brand != null) {
            nameComplete.append(brand).append(" ");
         }
         if (params != null) {
            nameComplete.append(params);
         }

      }
      return nameComplete.toString();

   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("div#product-info") != null;
   }

   private CategoryCollection scrapCategories(Document doc) {
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "ol.breadcrumb li span[itemprop=name]", true);

      if (categories.getCategory(0).equals("Productos")) {
         categories.remove(0);
      }

      return categories;
   }

   private Offers scrapOffers(JSONObject variation) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(variation);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName("preunic")
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject variation) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(variation, "price", true);
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(variation, "oldPrice", true);

      if (spotlightPrice == null) {
         spotlightPrice = priceFrom;
         priceFrom = null;
      }

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
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

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

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

