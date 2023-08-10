package br.com.lett.crawlernode.crawlers.corecontent.chile;


import br.com.lett.crawlernode.core.fetcher.FetchMode;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChileLidersuperCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.lider.cl/supermercado/";
   private static final String SELLER_NAME_LOWER = "lider";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString());

   public ChileLidersuperCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.MIRANHA);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String internalPidString = CrawlerUtils.scrapStringSimpleInfo(doc, ".pdp-desktop-item-number", true);
         String internalPid = internalPidString != null ? internalPidString.replace("item ", "") : null;
         String name = scrapNameAndBrand(doc);
         CategoryCollection categories = new CategoryCollection();
         String primaryImage = crawlPrimaryImage(doc);
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, "div[class*=ShowProductImages] > img", List.of("src"), "https", "images.lider.cl", primaryImage);
         String description = CrawlerUtils.scrapSimpleDescription(doc, List.of(".product-detail__card-section:contains(Descripción)"));
         boolean available = doc.selectFirst(".tags__attribute-tag-container--opaque") == null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String scrapNameAndBrand(Document doc) {

      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-detail-display-name", true);
      String brand = CrawlerUtils.scrapStringSimpleInfo(doc, ".prduct-detail-cart__brand-link", true);
      return name != null && brand != null ? brand + " - " + name : name;
   }

   private String crawlInternalId(Document doc) {
      String imgUrl = crawlPreviewImage(doc);
      String regex = "productos\\/(\\d+)[a-z].";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(imgUrl != null ? imgUrl : "");

      return matcher.find() ? matcher.group(1) : null;
   }

   private String crawlPreviewImage(Document doc) {
      Element figure = doc.selectFirst("div.image-preview__figure-wrapper > figure");
      String styleValue = figure != null ? figure.attr("style") : "";
      String regex = "background-image: url\\(\"(.+)\"\\).";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(styleValue);

      return matcher.find() ? matcher.group(1) : null;
   }

   private String crawlPrimaryImage(Document doc) {
      String previewImage = crawlPreviewImage(doc);

      // checking if preview image it's the primary image
      // sometimes Lider don't loads the primary image, only the secondary images
      // primary image url example: https://images.lider.cl/wmtcl?source=url[file:/productos/500244a.jpg]&sink
      // secondary image url example: https://images.lider.cl/wmtcl?source=url[file:/productos/500244b.jpg]&sink
      String regex = "\\/\\d+a\\.jpg|\\/\\d+a-1\\.jpg";
      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(previewImage != null ? previewImage : "");

      return matcher.find() ? previewImage : null;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_NAME_LOWER)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());


      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div[class*=product-prices] .saving__price__pdp", null, true, ',', session);
      Double spotlightPrice = extractPrice(doc);

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

   private Double extractPrice(Document doc) {
      String defaultSelector = "div.d-flex > span.pdp-mobile-sales-price";
      String alternativeSelector = ".regular-unit-price__price-default > span";

      String text = CrawlerUtils.scrapStringSimpleInfo(doc, defaultSelector, true);

      String priceSelector = (text != null && text.contains("x")) ? alternativeSelector : defaultSelector;

      return CrawlerUtils.scrapDoublePriceFromHtml(doc, priceSelector, null, true, ',', session);
   }

   private boolean isProductPage(Document doc) {
      return doc.select(".product-info") != null && doc.selectFirst(".no-available") == null;
   }
}
