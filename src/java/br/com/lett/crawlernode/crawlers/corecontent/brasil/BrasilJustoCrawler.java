package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BrasilJustoCrawler extends Crawler {
   private static final String HOME_PAGE = "https://soujusto.com.br/";
   private static final String SELLER_FULL_NAME = "Justo";
   private final String POSTAL_CODE = getPostalCode();

   public BrasilJustoCrawler(Session session) {
      super(session);
   }

   private String getPostalCode() { return session.getOptions().getString("postal_code"); }

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(), Card.DINERS.toString());

   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("postal_code", POSTAL_CODE);
      cookie.setDomain("soujusto.com.br");
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
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject contextSchema = CrawlerUtils.selectJsonFromHtml(doc, ".row.product.product__container > script", null, ";", true, true);
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".shopping-list__content", "data-id");
         String internalId = internalPid;
         List<String> eans = new ArrayList<>();
         if (contextSchema != null) {
            String sku = JSONUtils.getValueRecursive(contextSchema, "offers.0.sku", String.class, internalPid);
            internalId = !contextSchema.isEmpty() ? sku : internalPid;
            if (sku != null) {
               eans.add(sku);
            }
         }
         String name = CrawlerUtils.scrapStringSimpleInfo(doc,".product-details__name", false);
         boolean isAvailable = crawlAvailability(doc);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image > img", List.of("data-src"), "https", "media.soujusto.com.br");
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".product__info-description-text", false);
         Offers offers = isAvailable ? crawlOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            // this site currently has no secondary images
            .setDescription(description)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);
      }else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return !doc.select(".product__container").isEmpty();
   }

   private Offers crawlOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
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

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double[] prices = scrapPrices(doc);
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
      }else{
         return null;
      }
   }

   private Double[] scrapPrices(Document doc){
      Double spotlightPrice;
      Double priceFrom;

      if (doc.select(".product-details__discount").isEmpty()){
         priceFrom = null;
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-details__price > span > span", null, true, ',', session);
      }else{
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-details__discount", null, true, ',', session);
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-details__price > span > span", null, true, ',', session);
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

   private boolean crawlAvailability(Document doc){
      return !doc.select(".product-details__price").isEmpty();
   }

   private CategoryCollection crawlCategories(Document doc){

      CategoryCollection categories = new CategoryCollection();
      JSONArray jsonCategories = CrawlerUtils.selectJsonArrayFromHtml(doc, "script#legacy-breadcrumb", null, ";", true, true);

      for (int i = 0; i < jsonCategories.length() - 1; i++) {
         categories.add(jsonCategories.getJSONObject(i).optString("category"));
      }

      categories.removeIf(c -> c.equals(""));

      return categories;
   }

}
