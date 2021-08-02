package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.models.Card;
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
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ElsalvadorSuperselectosCrawler extends Crawler {


   protected String sucursalSelectos = session.getOptions().getString("sucursalSelectos");
   protected String sellerFullname = session.getOptions().getString("sellerFullname");

   public ElsalvadorSuperselectosCrawler(Session session) {
      super(session);
   }

   protected Set<String> cards = Sets.newHashSet(
      Card.VISA.toString(),
      Card.MASTERCARD.toString(),
      Card.AURA.toString(),
      Card.DINERS.toString(),
      Card.HIPER.toString(),
      Card.AMEX.toString());

   @Override
   public void handleCookiesBeforeFetch() {
      BasicClientCookie cookie = new BasicClientCookie("sucursalSelectos", sucursalSelectos);
      cookie.setDomain("www.superselectos.com");
      cookie.setPath("/");
      this.cookies.add(cookie);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (Boolean.TRUE.equals(isProductPage(doc))) {

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div.productFrame.full button.add-producto", "data-cod");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "div.productFrame.full p.desc", true);

         List<String> images = scrapImages(doc);
         String primaryImage = !images.isEmpty() ? images.remove(0) : "";
         List<String> secondaryImages = images;

         String description = CrawlerUtils.scrapStringSimpleInfo(doc, "p.fulldesc", true);

         //No unavailable products were found
         boolean available = true;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
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

   protected Boolean isProductPage(Document doc) {
      return doc.selectFirst("div.productFrame.full") != null;
   }

   protected List<String> scrapImages(Document doc) {
      List<String> imgs = new ArrayList<>();

      Elements imagesEl = doc.select("div.productFrame.full div.big div.img");

      if (!imagesEl.isEmpty()) {
         for (Element el : imagesEl) {
            Element img = el.selectFirst("img.ig-elevz");

            if (img != null) {
               imgs.add(img.attr("src"));
            }
         }
      }

      return imgs;
   }

   protected Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(sellerFullname)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.productFrame.full p.precio", null, false, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "div.productFrame.full p.ahorro span", null, false, '.', session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Double spotLightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotLightPrice)
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
