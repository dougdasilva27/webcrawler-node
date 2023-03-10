package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilPeixotoCrawler extends Crawler {

   public BrasilPeixotoCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.MIRANHA);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "div.value", true);
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div.price-final_price", "data-product-id");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "span[itemprop=name]", true);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("div.product.pricing"));
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "ul.items a", false);
         List<String> imagesList = getImageList(doc);
         String primaryImage = !imagesList.isEmpty() ? imagesList.remove(0) : CrawlerUtils.scrapSimplePrimaryImage(doc, ".fotorama__stage__shaft img", Arrays.asList("src"), "https", "www.peixoto.com.br");

         boolean availableToBuy = doc.select("button[id=button-out]").isEmpty();
         Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(imagesList)
            .setOffers(offers)
            .setDescription(description)

            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> getImageList(Document doc) {
      List<String> imageList = new ArrayList<>();
      Elements images = doc.select(".fotorama__nav__shaft img");
      if (images != null) {
         for (Element image : images) {
            String miniImage = CrawlerUtils.scrapSimplePrimaryImage(image, "img", Arrays.asList("src"), "https", "www.peixoto.com.br");
            miniImage = miniImage != null ? miniImage.replace("20acf7ec804333a88ca98c30b5782c8f", "a02926593082cf51ab15a190f9ad0c12") : null;
            imageList.add(miniImage);
         }
      }
      return imageList;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("div.product-main-content") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();
      Pricing pricing = scrapPricing(doc);

      offers.add(Offer.OfferBuilder.create()
         .setSellerFullName("peixoto")
         .setMainPagePosition(1)
         .setPricing(pricing)
         .setSales(sales)
         .setUseSlugNameAsInternalSellerId(true)
         .setIsMainRetailer(true)
         .setIsBuybox(false)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "span.price-wrapper  .price", null, true, ',', session);
      Double priceFrom = spotlightPrice;

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

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();
      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString());

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
