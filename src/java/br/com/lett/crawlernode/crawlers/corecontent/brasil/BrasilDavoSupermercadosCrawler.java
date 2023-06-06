package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

public class BrasilDavoSupermercadosCrawler extends Crawler {
   public BrasilDavoSupermercadosCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.MIRANHA);
   }

   private static final String SELLER_FULL_NAME = "Davo Supermercados";
   private static final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.ELO.toString(),
      Card.AMEX.toString(), Card.DINERS.toString(), Card.HIPERCARD.toString(), Card.JCB.toString(), Card.CABAL.toString(), Card.DISCOVER.toString(), Card.SOROCRED.toString());

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      super.extractInformation(document);
      List<Product> products = new ArrayList<>();
      String script = CrawlerUtils.scrapScriptFromHtml(document, "[data-name]");
      if (script != null && !script.isEmpty()) {
         JSONArray arrayJson = CrawlerUtils.stringToJsonArray(script);
         JSONObject structureProduct = JSONUtils.getValueRecursive(arrayJson, "0.0", JSONObject.class, new JSONObject());
         String internalId = structureProduct.optString("productId");
         String name = structureProduct.optString("name");
         String primaryImage = sanitizedUrl(structureProduct.optString("image"));
         JSONObject scriptObject = getObjectProductScript(document, internalId);
         boolean isAvailable = Available(structureProduct, scriptObject);
         String description = scriptObject.optString("longDescription");
         Offers offers = isAvailable ? scrapOffers(scriptObject) : new Offers();
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private boolean Available(JSONObject scriptObject,JSONObject structureProduct) {
      Double price = scriptObject.optDouble("listPrice");
     return JSONUtils.getValueRecursive(structureProduct, "offers.availability", String.class, "").equals("https://schema.org/InStock") && price != null ;
   }

   private JSONObject getObjectProductScript(Document document, String internalId) throws UnsupportedEncodingException {
      Element catalogJSON = document.selectFirst("script:containsData(window.state)");
      String aux = catalogJSON.html().replace(" ", "").replace("\n", "");
      String jsonString = CommonMethods.substring(aux, "window.state=JSON.parse(decodeURI(\"", "\"))", true);
      String decodedString = URLDecoder.decode(jsonString, "UTF-8");
      JSONObject productJson = CrawlerUtils.stringToJson(decodedString);
      return JSONUtils.getValueRecursive(productJson, "catalogRepository.products." + internalId, JSONObject.class, new JSONObject());
   }

   private String sanitizedUrl(String urlImage) {
      if (urlImage != null && !urlImage.isEmpty()) {
         return CommonMethods.substring(urlImage, "", "&", true);
      }
      return "";
   }

   private Offers scrapOffers(JSONObject json) throws OfferException, MalformedPricingException {
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
      Double spotlightPrice = json.optDouble("listPrice");


      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
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
