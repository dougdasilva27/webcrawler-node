package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilFarmaciasuniprecoCrawler extends Crawler {

   public BrasilFarmaciasuniprecoCrawler(Session session) {
      super(session);
   }

   private static final String HOME_PAGE = "https://www.farmaciasunipreco.com.br/";

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
         Element productInfo = doc.selectFirst(".product-detail");


         String internalId = crawlInternalId(session.getOriginalURL());
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(productInfo,".product-detail .information .name", false);
         boolean isAvailable = productInfo.selectFirst(".information .buy .wd-product-notifywhenavailable .content[style=\"display:none\"]") != null;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".wd-browsing-breadcrumbs > ul > li > a > span", true);
         List<String> images = crawlImages(productInfo);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-detail .descriptions #LongDescription .wd-descriptions-text", false);
         Offers offers = isAvailable ? crawlOffers(productInfo) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".product-detail") != null;
   }

   private String crawlInternalId(String url) {
      String internalId = null;
      String regex = "p([0-9]+)(#|$)";

      final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(url);

      if (matcher.find()) {
         internalId = matcher.group(1);
      }

      return internalId;
   }

   private List<String> crawlImages(Element productInfo) {
      List<String> images = new ArrayList<>();
      Elements imagesElements = productInfo.select(".wd-product-media-selector #scroll-media > .image");

      for (Element e : imagesElements) {
         String imageUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "img", "data-image-big");
         images.add(imageUrl);
      }

      return images;
   }

   private Offers crawlOffers(Element productInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productInfo);
      List<String> sales = scrapSales(pricing);

      if(pricing != null){
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("farmaciaunipreco")
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Element productInfo) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(productInfo, ".information .wd-product-price-description .priceContainer .sale-price span", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(productInfo, ".information .wd-product-price-description .priceContainer .list-price span", null, true, ',', session);

      if(spotlightPrice != null){
         CreditCards creditCards = scrapCreditCards(productInfo, spotlightPrice);
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

   private CreditCards scrapCreditCards(Element productInfo, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());


      Integer parcels = CrawlerUtils.scrapIntegerFromHtml(productInfo, ".information .wd-product-price-description .priceContainer .condition .parcels", true, 0);
      Double parcelValue = CrawlerUtils.scrapDoublePriceFromHtml(productInfo, ".information .wd-product-price-description .priceContainer .condition .parcel-value", null, true, ',', session);

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(parcels)
         .setInstallmentPrice(parcelValue != null ? parcelValue : 0.0)
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
}
