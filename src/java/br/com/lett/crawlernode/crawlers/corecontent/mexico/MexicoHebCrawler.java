package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
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
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Date: 30/10/2018
 *
 * @author Gabriel Dornelas
 */
public class MexicoHebCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.heb.com.mx/";
   private static final String SELLER_NAME_LOWER = "heb";

   public MexicoHebCrawler(Session session) {
      super(session);
   }

   private String getStore() {
      return this.session.getOptions().optString("store");
   }

   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("store", getStore());
      cookie.setDomain("www.heb.com.mx");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(
            logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = crawlInternalPid(doc);
         String internalId = internalPid;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title-wrapper .page-title", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs li:not(.home):not(.product)");
         boolean available = !doc.select(".action.tocart.primary").isEmpty();
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".gallery-placeholder img", Arrays.asList("src"), "https", "www.heb.com.mx/");
         List<String> secondaryImages = getSecondaryImage(doc);
         String description = crawlDescription(doc);
         String ean = scrapEan(doc, ".extra-info span span[data-upc]");
         List<String> eans = ean != null ? List.of(ean) : new ArrayList<>();
         Offers offers = available ? crawlOffers(doc) : null;

         Product product =
            ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setOffers(offers)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setEans(eans)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".product-sku").isEmpty();
   }

   private Offers crawlOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(pricing);

      if(pricing != null){
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_NAME_LOWER)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-final_price h2[data-price-type=finalPrice] .price", null, true, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".price-final_price h2[data-price-type=oldPrice] .price", null, true, '.', session);

      if (spotlightPrice != null) {
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

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(), Card.DINERS.toString());

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

   private String crawlInternalPid(Document doc) {
      String internalPid = null;

      Element pid = doc.selectFirst(".price-box.price-final_price");
      if (pid != null) {
         internalPid = pid.attr("data-product-id");
      }

      return internalPid;
   }

   private List<String> getSecondaryImage(Document doc) {
      List<String> imageList = new ArrayList<>();
      JSONObject jsonObject = CrawlerUtils.selectJsonFromHtml(doc, ".product.media script[type='text/x-magento-init']", null, null, false, false);
      JSONArray jsonArray = jsonObject != null ? JSONUtils.getValueRecursive(jsonObject, "[data-gallery-role=gallery-placeholder].mage/gallery/gallery.data", JSONArray.class) : null;
      if (jsonArray != null) {
         for (Object obj : jsonArray) {
            if (obj instanceof JSONObject) {
               JSONObject jsonImages = (JSONObject) obj;
               String imageLink = jsonImages.optString("full");
               Boolean mainImg= jsonImages.optBoolean("isMain");
               if (imageLink != null && !imageLink.isEmpty() && !imageLink.contains("image") && !mainImg ) {
                  imageList.add(imageLink.replace("\\", ""));
               }
            }
         }
      }
      return imageList;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();
      String ingredients = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".additional-info .tab-content .tab-pane.fade.in.active"));
      if(!ingredients.isEmpty()){
         return ingredients;
      }
      Elements descriptions = doc.select(".product-collateral dd.tab-container");
      for (Element e : descriptions) {
         if (e.select("#customer-reviews").isEmpty()) {
            description.append(e.html());
         }
      }

      return description.toString();
   }

   private String scrapEan(Document doc, String selector) {
      String ean = null;
      Element e = doc.selectFirst(selector);

      if (e != null) {
         String aux = e.attr("data-upc");
         ean = aux.length() == 12 ? "0" + aux : aux;
      }

      return ean;
   }
}
