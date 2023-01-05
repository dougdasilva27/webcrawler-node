package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.amazonaws.services.dynamodbv2.xspec.S;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.AdvancedRatingReview;
import models.Offer;
import models.Offers;
import models.RatingsReviews;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.io.UnsupportedEncodingException;
import java.util.*;

public class BrasilNiveaCrawler extends Crawler {

   public BrasilNiveaCrawler(Session session) {
      super(session);
   }

   private static final String SELLER_NAME = "nivea";
   protected Set<String> cards = Sets.newHashSet(Card.MASTERCARD.toString(), Card.VISA.toString(), Card.AMEX.toString(), Card.ELO.toString());

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      super.extractInformation(document);
      List<Product> products = new ArrayList<>();
      if (!isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Not a product page" + session.getOriginalURL());
         return products;
      }
      // Get all product information
      String productName = CrawlerUtils.scrapStringSimpleInfo(document, ".page-title-wrapper.product h1 span", false);
      String productInternalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".price-box.price-final_price", "data-product-id");
      String productDescription = CrawlerUtils.scrapStringSimpleInfo(document, ".product.info.detailed", false);
      String productPrimaryImage = CrawlerUtils.scrapSimplePrimaryImage(document, "img.gallery-placeholder__image", Arrays.asList("src"), "", "");
      List<String> productSecondaryImages = ImageCapture(document, productInternalId, productPrimaryImage);
      Offers offers = CrawlerUtils.scrapStringSimpleInfo(document, "#contact_form_notify > p", false) == null ? scrapOffers(document) : new Offers();
      Product product = ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(productInternalId)
         .setName(productName)
         .setPrimaryImage(productPrimaryImage)
         .setSecondaryImages(productSecondaryImages)
         .setDescription(productDescription)
         .setOffers(offers)
         .build();
      products.add(product);
      return products;
   }

   private List<String> ImageCapture(Document document, String internalId, String primaryImage) throws Exception {
      String script = CrawlerUtils.scrapScriptFromHtml(document, "[type=\"text/x-magento-init\"]:containsData(Magento_Catalog/js/product/view/provider)");
      JSONArray arr = JSONUtils.stringToJsonArray(script);
      JSONArray images = JSONUtils.getValueRecursive(arr, "0.*.Magento_Catalog/js/product/view/provider.data.items." + internalId + ".images", JSONArray.class);
      List<String> productSecondaryImagesList = new ArrayList<>();
      images.remove(0);
      if (images != null && !images.isEmpty()) {
         for (Object i : images) {
            JSONObject imageObj = (JSONObject) i;
            String url = JSONUtils.getStringValue(imageObj, "url");
            if (!url.equals(primaryImage)) {
               productSecondaryImagesList.add(url);
            }
         }
      } else {
         return null;
      }
      return productSecondaryImagesList;
   }
   private boolean isProductPage(Document document) {
      return document.selectFirst(".product.media") != null;
   }
   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(doc);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }
   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlight = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price", null, true, ',', session);
      // site não possui promoção

      CreditCards creditCards = scrapCreditCards(spotlight);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlight)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlight)
         .setPriceFrom(null)
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

