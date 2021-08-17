package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BrasilMegamamuteCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.megamamute.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "megamamute";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public BrasilMegamamuteCrawler(Session session) {
      super(session);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      // Creating the product
      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, " small.sku", true).replaceAll("[^0-9]", "");
      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, " div[data-widget-pid]", "data-widget-pid");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.name", true);
      List<String> images = srapImages(doc);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".wd-descriptions-text"));
      Offers offers = scrapOffers(doc);

      Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
         .setInternalId(internalId)
         .setInternalPid(internalPid)
         .setName(name)
         .setPrimaryImage(images.remove(0))
         .setSecondaryImages(images)
         .setDescription(description)
         .setOffers(offers)
         .build();


      products.add(product);

      return products;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Element available = doc.selectFirst(".btn-buy[title=\"Comprar\"]");
      if (available != null) {
         Pricing pricing = scrapPricing(doc);

         offers.add(Offer.OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(MAIN_SELLER_NAME_LOWER)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }
      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Element available = doc.selectFirst(".btn-buy[title=\"Comprar\"]");


      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".sale-price span", null, true, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".list-price span", null, true, ',', session);

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();


   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      int installment = CrawlerUtils.scrapIntegerFromHtml(doc, ".buy .priceContainer  .parcels", true,0);
      Double installmentValue = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".parcel-value", null, true, ',', session);


      Installments installments = new Installments();
      if (installmentValue != null) {


         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(installment)
            .setInstallmentPrice(installmentValue)
            .build());
      }

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }


   private List<String> srapImages(Document doc) {
      List<String> images = new ArrayList<>();

      Elements imagesElements = doc.select(".wd-product-media-selector ul li");
      for (Element element : imagesElements) {
         String image = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, "img", "src");

         Pattern compile = Pattern.compile("(t)([0-9])");
         Matcher matcher = compile.matcher(image);
         if (matcher.find()) {
            images.add(new StringBuilder(image).replace(matcher.start(1), matcher.end(2), "m" + matcher.group(2)).toString());
         }


      }

      return images;
   }
}
