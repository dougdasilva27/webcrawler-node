package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
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
import org.jsoup.select.Elements;

import java.util.*;

import static br.com.lett.crawlernode.core.models.Card.*;

public class BrasilEmporioecoCrawler extends Crawler {
   protected Set<String> cards = Sets.newHashSet(MASTERCARD.toString(), VISA.toString(), ELO.toString(), AMEX.toString(), HIPERCARD.toString(), HIPER.toString(), SOROCRED.toString());

   public BrasilEmporioecoCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      super.extractInformation(document);
      List<Product> products = new ArrayList<>();


      if(!isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Not a product page" + session.getOriginalURL());
         return products;
      }

      JSONObject json = CrawlerUtils.stringToJson(
         document.selectFirst("script[type=\"application/ld+json\"]").html()
      );

      // Get all product information
      String productName = json.optString("name");
      String productInternalId = json.optString("sku");
      String productInternalPid = json.optString("sku");
      String productDescription = CrawlerUtils.scrapSimpleDescription(document, Collections.singletonList("#tab-description"));
      CategoryCollection productCategories = CrawlerUtils.crawlCategories(document, ".posted_in > a");
      String productPrimaryImage = json.optString("image");
      List<String> productSecondaryImages = CrawlerUtils.scrapSecondaryImages(document, ".woocommerce-product-gallery__wrapper a", Arrays.asList("href"), "https", "lojaemporioeco.com.br" , productPrimaryImage);


      ProductBuilder builder = ProductBuilder.create().setUrl(session.getOriginalURL());
      builder.setName(productName)
         .setInternalId(productInternalId)
         .setInternalPid(productInternalPid)
         .setDescription(productDescription)
         .setCategories(productCategories)
         .setPrimaryImage(productPrimaryImage)
         .setSecondaryImages(productSecondaryImages);


      // Scrap all variations
      for(Object jsonOffer : json.optJSONArray("offers")) {
         if(jsonOffer instanceof JSONObject) {

            if(document.selectFirst(".variations_form.cart") != null) {
               List<Product> productWithVariations = scrapVariations(document, builder, (JSONObject) jsonOffer);
               products.addAll(productWithVariations);
            } else {
               boolean isAvailable = document.selectFirst(".in-stock") != null;
               builder.setOffers(isAvailable ? scrapOffers(document, (JSONObject) jsonOffer) : new Offers());
               products.add(builder.build());
            }
         }
      }
      return products;

   }

   private List<Product> scrapVariations(Document document, ProductBuilder productBuilder, JSONObject jsonOffer) throws MalformedProductException, MalformedPricingException, OfferException {
      List<Product> products = new ArrayList<>();

      JSONArray jsonArray = CrawlerUtils.stringToJsonArray(document.selectFirst(".variations_form.cart").attr("data-product_variations"));
      String name = document.selectFirst(".product_title.entry-title").text();

      Elements variationsElements = document.select("li.variable-item");

      for(int i = 0; i < variationsElements.size(); i++) {
         JSONObject elementJSON = jsonArray.optJSONObject(i);
         boolean isAvailable = elementJSON.optBoolean("is_in_stock");

         Product variationProduct = productBuilder
            .setOffers(isAvailable ? scrapOffers(document, jsonOffer) : new Offers())
            .setInternalId(elementJSON.optString("variation_id"))
            .setName(name + " " + variationsElements.get(i).selectFirst("span").text())
            .build();

         products.add(variationProduct);
      }

      return products;
   }

   private Offers scrapOffers(Document document, JSONObject json) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(document, json);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));
      String sellerName = json.optJSONObject("seller").optString("name", null);
      offers.add(new Offer.OfferBuilder()
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSellerFullName(sellerName)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
            .setSales(sales)
         .build()
      );

      return offers;
   }

   private Pricing scrapPricing(Document document, JSONObject json) throws MalformedPricingException {
      Double price = json.optDouble("price");
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(document, "del > .woocommerce-Price-amount", null, false,  ',', session );
      CreditCards creditCards = CrawlerUtils.scrapCreditCards(price, cards);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }


   private boolean isProductPage(Document document) {
      return document.selectFirst(".row.la-single-product-page") != null;
   }
}
