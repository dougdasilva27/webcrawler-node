package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offers;
import models.prices.Prices;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public abstract class SupernicoliniCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "Super Nicolini";
   protected abstract String getHomepage();

   public SupernicoliniCrawler(Session session){
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && href.startsWith(getHomepage());
   }

   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-title.product_title", false);
         String internalId = crawlInternalId(doc);
         boolean available = CrawlerUtils.scrapStringSimpleInfo(doc, ".stock", false).equals("Em estoque");
         CategoryCollection categories = crawlCategories(doc, ".woocommerce-breadcrumb.breadcrumbs.uppercase a");
         String primaryImage = crawlPrimaryImage(doc);
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".woocommerce-Tabs-panel--description p", false);
         Offers offers = available ? scrapOffer(doc, internalId) : new Offers();
         String ean = CrawlerUtils.scrapStringSimpleInfo(doc,"div.product_meta > span.sku_wrapper > span", false);

         List<String> eans = new ArrayList<>();
         eans.add(ean);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc){ return !doc.select(".product-main").isEmpty();   }

   private String crawlInternalId(Document doc){

      String internalId = null;
      String shortUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "link[rel=shortLink]", "href");
      if(shortUrl != null){
         internalId = shortUrl.split("p=")[1];
      }

      return internalId;
   }

   private CategoryCollection crawlCategories(Document doc, String selector){

      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = doc.select(selector);

      for(Element e: elementCategories){
         categories.add(e.text());
      }

      return categories;

   }

   private String crawlPrimaryImage(Document document) {
      String primaryImage = null;
      Elements primaryImageElement = document.select(".woocommerce-product-gallery__image img");

      if (primaryImageElement != null) {
         primaryImage = primaryImageElement.attr("src").trim();
      }

      return primaryImage;
   }

   private CreditCards scrapCreditCards(Document doc, String internalId, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      return creditCards;
   }

   private Pricing scrapPricing(String internalId, Document doc) throws MalformedPricingException {
      Double priceFrom = null;
      Double spotlightPrice;
      Elements productPrices = doc.select(".product-info > .price-wrapper .product-page-price span.woocommerce-Price-amount");
      if(productPrices.size() > 1){
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.price-wrapper > p > del > span", null, false, ',', session);
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.price-wrapper > p > ins > span", null, false, ',', session);
      } else{
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.price-wrapper > p > span", null, false, ',', session);
      }

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .build();
   }

   private Offers scrapOffer(Document doc, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(internalId, doc);

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

}
