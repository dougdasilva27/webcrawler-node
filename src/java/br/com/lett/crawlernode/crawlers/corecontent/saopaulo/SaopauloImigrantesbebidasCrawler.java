package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SaopauloImigrantesbebidasCrawler extends Crawler {

   private static final String BASE_URL = "www.imigrantesbebidas.com.br";
   private static final String MAIN_SELLER_NAME = "Imigrantes Bebidas";
   private final Set<String> cards = Sets.newHashSet(Card.VISA.toString(),
      Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.ELO.toString(), Card.DINERS.toString());

   public SaopauloImigrantesbebidasCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         String name = scrapName(doc);
         List<String> images = scrapImages(doc);
         String primaryImage = !images.isEmpty() ? images.remove(0) : null;
         String internalId = scrapInternalId(doc, primaryImage);

         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".productPage__tabs__content__text", ".productPage__tabs__content__text br", ".personalization__description p"));
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs__wrapper a:not(:last-child)", true);
         List<String> eans = scrapEans(doc);

         boolean availability = doc.selectFirst(".out-of-stock") == null;
         Offers offers = availability ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .setEans(eans)
            .build();

         products.add(product);

      }

      return products;
   }

   private String scrapName(Document doc) {
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".productPage__name", true);
      if (name == null || name.isEmpty()) {
         name = CrawlerUtils.scrapStringSimpleInfo(doc, "div.productName", true);
      }

      return name;
   }

   private String scrapInternalId(Document doc, String primaryImage) {
      String id = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".productPage__info  [name=\"products_id\"]", "value");

      if (id == null) {
         id = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".personalization--desktop [name=\"products_id\"]", "value");

         if (id == null && primaryImage != null) {
            String regex = "full\\/([^\\/\\-]+)\\-";
            Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(primaryImage);
            if (matcher.find()) {
               return matcher.group(1);
            }
         }
      }

      return id;
   }

   private boolean isProductPage(Document doc) {
      boolean personalizedPage = doc.selectFirst("main[class*='personalized']") != null;
      boolean isProductPage = doc.selectFirst(".productPage") != null;

      return personalizedPage || isProductPage;
   }

   private List<String> scrapImages(Document doc) {
      List<String> imgList = new ArrayList<>();
      Elements elementImages =  doc.select("div.slider.productGallery__slider > picture");

      if (elementImages != null) {
         elementImages.forEach(img -> imgList.add("https://www.imigrantesbebidas.com.br" + img.attr("rel")));
      }

      String personalizedImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "div[class*=\"personalized\"] img", Arrays.asList("src"), "https", "www.imigrantesbebidas.com.br");
      if (imgList.isEmpty() && personalizedImage != null) {
         imgList.add(personalizedImage);
      }

      return imgList;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      if (pricing != null) {
         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(MAIN_SELLER_NAME)
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }
      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".productPage__priceFrom", null, true, ',', this.session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".productPage__price", null, true, ',', this.session);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(scrapCreditCards(spotlightPrice))
         .setBankSlip(scrapBankSlip(spotlightPrice))
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

   private BankSlip scrapBankSlip(Double spotlightPrice) throws MalformedPricingException {
      return BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();
   }

   private List<String> scrapEans(Document document) {
      List<String> listEans = new ArrayList<>();
      String element = CrawlerUtils.scrapStringSimpleInfo(document, "productPage__tabs__content__text", false);

      if (element != null) {
         listEans.add(CommonMethods.getLast(element.split("EAN:")));
      }
      return listEans;
   }
}
