package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.models.Card;
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
import models.pricing.CreditCards;
import models.pricing.Pricing;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class ArgentinaPintureriasrexCrawler extends Crawler {
   protected Set<String> cards = Sets.newHashSet(Card.MASTERCARD.toString(), Card.VISA.toString(), Card.NARANJA.toString(), Card.AMEX.toString(), Card.COBAL.toString(), Card.CORDOBESA.toString());
   private static final String SELLER_NAME = "Pinturerias Rex";

   public ArgentinaPintureriasrexCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      super.extractInformation(document);
      List<Product> products = new ArrayList<>();
      if (!isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Not a product page" + session.getOriginalURL());
         return products;
      }
      // Get all product information
      String productName = CrawlerUtils.scrapStringSimpleInfo(document, ".base", false);
      String productInternalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".price-box.price-final_price", "data-product-id");
      String productInternalPid = productInternalId;
      String productDescription = CrawlerUtils.scrapStringSimpleInfo(document, ".product.attribute.description .value", false);
      String productPrimaryImage = CrawlerUtils.scrapSimplePrimaryImage(document, ".image-container img", Arrays.asList("src"), "", "");
      List<String> productSecondaryImages = ImageCapture(document);

      ProductBuilder builder = ProductBuilder.create().setUrl(session.getOriginalURL());
      Product product = ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(productInternalId)
         .setInternalPid(productInternalPid)
         .setName(productName)
         .setPrimaryImage(productPrimaryImage)
         .setSecondaryImages(productSecondaryImages)
         .setDescription(productDescription)
         .setOffers(scrapOffers(document, productInternalId))
         .build();
      products.add(product);
      return products;
   }

   private Offers scrapOffers(Document document, String productInternalId) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(document, productInternalId);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));
      offers.add(new Offer.OfferBuilder()
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .setSales(sales)
         .build()
      );
      return offers;
   }

   private Pricing scrapPricing(Document document, String id) throws MalformedPricingException {
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(document, "#product-price-" + id, "data-price-amount", true, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(document, "#old-price-" + id, "data-price-amount", true, '.', session);
      CreditCards creditCards = CrawlerUtils.scrapCreditCards(price, cards);
      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private List<String> ImageCapture(Document document) throws Exception {
      String script = CrawlerUtils.scrapScriptFromHtml(document, ".product.media [type]");
      JSONArray arr = JSONUtils.stringToJsonArray(script);
      JSONArray images = JSONUtils.getValueRecursive(arr, "0.[data-gallery-role=gallery-placeholder].mage/gallery/gallery.data", JSONArray.class);
      List<String> productSecondaryImagesList = new ArrayList<>();
      for (Object i : images) {
         JSONObject imageObj = (JSONObject) i;
         String url = JSONUtils.getStringValue(imageObj, "full");
         Boolean isMain = imageObj.optBoolean("isMain");
         if (!isMain) {
            productSecondaryImagesList.add(url);
         }

      }
      return productSecondaryImagesList;
   }

   private boolean isProductPage(Document document) {
      return document.selectFirst(".product.media") != null;
   }
}
