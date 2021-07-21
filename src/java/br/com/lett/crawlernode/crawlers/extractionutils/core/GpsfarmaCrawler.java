package br.com.lett.crawlernode.crawlers.extractionutils.core;

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
import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Date: 22/06/2018
 *
 * @author Gabriel Dornelas
 */
public abstract class GpsfarmaCrawler extends Crawler {

   private final String SELLER_FULLNAME = getSellerFullName();

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString(),
      Card.ELO.toString(), Card.JCB.toString(), Card.DISCOVER.toString());

   public GpsfarmaCrawler(Session session) {
      super(session);
   }

   protected abstract String getSellerFullName();

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=product]", "value");
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, "tr td.data", true);
         String name = getName(doc);
         CategoryCollection categories = crawlCategories(doc);
         List<String> images = crawlImages(doc);
         String primaryImage = !images.isEmpty() ? images.remove(0) : "";
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("div.product.data.items"));

         boolean available = crawlAvailability(doc);
         Offers offers = available ? crawlOffers(doc) : new Offers();

         String ean = CrawlerUtils.scrapStringSimpleInfo(doc, "td[data-th=Código de barra]", true);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setEans(Collections.singletonList(ean))
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.select("input[name=\"product\"]").first() != null;
   }

   private String getName(Document doc) {
      StringBuilder buildName = new StringBuilder();
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.page-title", false);
      String brand = CrawlerUtils.scrapStringSimpleInfo(doc, ".product.product-item-brand", true);
      if (name != null) {
         buildName.append(name);
         if (brand != null) {
            buildName.append(" - ").append(brand);
         }
      }

      return buildName.toString();
   }

   private CategoryCollection crawlCategories(Document doc) {
      CategoryCollection categories = new CategoryCollection();

      Elements jsonList = doc.select("[type=text/x-magento-init]");

      for (Element elementJson : jsonList) {
         JSONObject json = CrawlerUtils.stringToJson(elementJson.html());

         String result = JSONUtils.getValueRecursive(json, "*.Magento_GoogleTagManager/js/actions/product-detail.category", String.class);

         if (result != null) {
            categories.add(StringEscapeUtils.unescapeHtml(result));
         }
      }

      return categories;
   }

   private boolean crawlAvailability(Document doc) {
      return !doc.select("div.box-tocart").isEmpty();
   }

   private List<String> crawlImages(Document doc) {
      List<String> imgList = new ArrayList<>();

      JSONArray images = CrawlerUtils.crawlArrayImagesFromScriptMagento(doc);

      for (Object img : images) {
         imgList.add(img.toString());
      }

      return imgList;
   }

   private Offers crawlOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(doc);
      sales.add(CrawlerUtils.scrapStringSimpleInfo(doc, "ul.prices-tier.items", false));
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULLNAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotLightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span[data-price-type=finalPrice] span", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span[data-price-type=oldPrice] span", null, false, ',', session);

      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotLightPrice, null);
      CreditCards creditCards = scrapCreditCards(spotLightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotLightPrice)
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
