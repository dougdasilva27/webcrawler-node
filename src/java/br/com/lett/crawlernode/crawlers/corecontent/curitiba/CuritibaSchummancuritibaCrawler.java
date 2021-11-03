package br.com.lett.crawlernode.crawlers.corecontent.curitiba;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jooq.util.derby.sys.Sys;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class CuritibaSchummancuritibaCrawler extends Crawler {

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString());

   private static final String SELLER_NAME = "Schumman Curitiba";

   public CuritibaSchummancuritibaCrawler(Session session) {
      super(session);
   }

   private String getProductPid() {
      String[] urlWords = session.getOriginalURL().split("-");

      return urlWords[urlWords.length - 1].replaceAll("[^0-9]", "");
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".product-detail") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());
         String internalPid = getProductPid();
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".long-description"));
         CategoryCollection categories = getCategories(doc, "[name=keywords]", "content");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".zoom > img", Arrays.asList("src"), "https", "");
         String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[property=\"og:title\"]", "content");
         boolean available = doc.selectFirst("[title=\"Adicionar ao carrinho\"]") != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();
         Elements colors = doc.select(".variation-group .options label");

         for (Element e : colors) {
            String colorName = CrawlerUtils.scrapStringSimpleInfo(e, "span b", false);
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "input", "value");

            if(internalId.equals("")){
               internalId = internalPid;
            }

            colorName = getName(name, colorName);

            // Creating the productInfo
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(colorName)
               .setPrimaryImage(primaryImage)
               .setDescription(description)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setOffers(offers)
               .build();

            products.add(product);
         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

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

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".list-price > span", null, true, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".instant-price", null, true, ',', session);
      Double bankSlipValue = spotlightPrice;

      CreditCards creditCards = scrapCreditCards(doc);

      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(bankSlipValue)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      String numberInstallmentStr = CrawlerUtils.scrapStringSimpleInfo(doc, ".parcels", false);
      Integer numberInstallment = numberInstallmentStr != null ? Integer.valueOf(numberInstallmentStr.replace("x", "")) : null;
      String valueStr =  CrawlerUtils.scrapStringSimpleInfo(doc, ".sale-price > span", false);
      String valueInstallmentStr =  CrawlerUtils.scrapStringSimpleInfo(doc, ".parcel-value", false);
      Double valueInstallment = MathUtils.parseDoubleWithComma(valueInstallmentStr);
      Double value = valueStr != null ? MathUtils.parseDoubleWithComma(valueStr) : null;
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(numberInstallment)
         .setFinalPrice(value)
         .setInstallmentPrice(valueInstallment)
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

   public static CategoryCollection getCategories(Document doc, String selector, String attr) {
      CategoryCollection categories = new CategoryCollection();

      Elements elementCategories = doc.select(selector);

      String selectorAttribute = elementCategories.attr(attr);

      ArrayList<String> categoryList = new ArrayList<>(Arrays.asList(selectorAttribute.split(",")));

      for (String category: categoryList) {
         categories.add(category.trim());
      }
      return categories;
   }

   private String getName(String name, String color) {
      StringBuilder stringBuilder = new StringBuilder();

      if (name != null && !name.isEmpty()) {
         stringBuilder.append(name);
         if (color != null && !color.isEmpty()) {
            stringBuilder.append(" - ").append(color);
         }
      }
      return stringBuilder.toString();
   }
}
