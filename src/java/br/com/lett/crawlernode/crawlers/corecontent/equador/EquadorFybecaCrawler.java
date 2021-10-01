package br.com.lett.crawlernode.crawlers.corecontent.equador;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.Card;
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
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EquadorFybecaCrawler extends Crawler {

   private static final String SELLER_NAME = "fybeca";
   private static final Set<String> cards = Sets.newHashSet(Card.DINERS.toString(), Card.VISA.toString(),
      Card.MASTERCARD.toString(), Card.ELO.toString());

   public EquadorFybecaCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst("form#product-detail") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=id]", "value");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("div#product-info"));
         String name = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "meta[property=og:title]", "content");
         List<String> images = scrapImages(doc);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         boolean available = doc.selectFirst("button[data-method=addToCart]") != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalPid)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);


      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> scrapImages(Document doc) {
      List<String> imgList = new ArrayList<>();

      Elements imgsEl = doc.select("div.carousel.carousel-navigation li[data-id] img.productImage");

      for (Element img : imgsEl) {
         String imgUrl = img.attr("src");

         if (imgUrl != null) {
            imgUrl = imgUrl.replace("thumbnail/", "")
               .replace("../..", "");
            imgList.add("https://www.fybeca.com" + imgUrl);
         }
      }

      return imgList;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(doc);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      String priceElement = doc.selectFirst("div#price2") != null ? doc.selectFirst("div#price2").attr("data-bind") : "";

      Double spotlightPrice = formatPrice(priceElement);

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      creditCards.add(scrapShopCard(doc));

      return creditCards;
   }

   private CreditCard scrapShopCard(Document doc) throws MalformedPricingException {
      CreditCard creditCard;
      Installments installments = new Installments();

      String priceElement = doc.selectFirst("div#price2") != null ? doc.selectFirst("div#price2").attr("data-bind") : "";
      Double shopCardPrice = formatPrice(priceElement);

      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(shopCardPrice)
            .build());
      }

      creditCard = CreditCard.CreditCardBuilder.create()
         .setBrand(Card.SHOP_CARD.toString())
         .setInstallments(installments)
         .setIsShopCard(true)
         .build();

      return creditCard;
   }

   private Double formatPrice(String priceElement){
      String priceStr = "";
      String regex = "\\(([0-9]*?\\.[0-9]*?)\\)";

      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(priceElement);
      if (matcher.find()) {
         priceStr = matcher.group(1);
      }

      return MathUtils.parseDoubleWithDot(priceStr);
   }
}
