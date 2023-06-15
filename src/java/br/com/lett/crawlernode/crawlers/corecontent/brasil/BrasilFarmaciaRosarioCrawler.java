package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.jsoup.nodes.Document;

import java.util.*;

public class BrasilFarmaciaRosarioCrawler extends Crawler {
   protected Set<String> cards = Sets.newHashSet(Card.MASTERCARD.toString(), Card.VISA.toString(), Card.ELO.toString(), Card.HSCARD.toString(), Card.HIPER.toString());

   public BrasilFarmaciaRosarioCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      super.extractInformation(document);

      List<Product> products = new ArrayList<>();
      if (!isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Not a product page" + session.getOriginalURL());
         return products;
      }

      String productName = CrawlerUtils.scrapStringSimpleInfo(document, ".name.pb-1", false);
      String productInternalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document, ".d-flex.flex-wrap.mb-4 > span > meta:nth-child(1)", "content");
      String productInternalPid = CrawlerUtils.scrapStringSimpleInfo(document, ".mr-3.text-muted.font-size-12.font-weight-bold", false);
      if (productInternalPid != null) {
         String[] arrProduct = productInternalPid.split(" ");
         if (arrProduct.length == 2) {
            productInternalPid = String.valueOf(Integer.parseInt(arrProduct[1]));
         }
      }
      String productDescription = CrawlerUtils.scrapStringSimpleInfo(document, "#main-wrapper > div.content > div > div:nth-child(2)", false);
      List<String> categories = CrawlerUtils.crawlCategories(document, "#breadcrumb > ul > li > a");
      List<String> productSecondaryImages = scrapImagesLarge(document);
      String productPrimaryImage = CrawlerUtils.scrapSimplePrimaryImage(document, "#content-product > div > div > div > div.col-12.col-md-7.product-image > div > div > figure > img", Arrays.asList("data-src"), "https", "");
      Offers offers = isValid(document) ? scrapOffers(document) : new Offers();

      Product product = ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(productInternalId)
         .setInternalPid(productInternalPid)
         .setName(productName)
         .setCategories(categories)
         .setPrimaryImage(productPrimaryImage)
         .setSecondaryImages(productSecondaryImages)
         .setDescription(productDescription)
         .setOffers(offers)
         .build();
      products.add(product);
      return products;
   }

   private List<String> scrapImagesLarge(Document document) {
      List<String> imagesMini = CrawlerUtils.scrapSecondaryImages(document, ".thumbs li img", Arrays.asList("data-src"), "https", "io.convertiez.com.br", null);
      if( imagesMini != null){
         for (String str : imagesMini){

         }

      }
   }

   private Offers scrapOffers(Document document) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(document);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));
      offers.add(new Offer.OfferBuilder()
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSellerFullName("farmaciarosario")
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .setSales(sales)
         .build()
      );
      return offers;
   }

   private Pricing scrapPricing(Document document) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(document, ".product-form .sale-price strong", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(document, ".product-form .unit-price:not([style='display:none;'])", null, false, ',', session);

      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);
      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private boolean isProductPage(Document document) {
      return document.selectFirst("#content-product") != null;
   }

   private boolean isValid(Document document) {
      return document.selectFirst(".font-size-20.font-weight-bold.text-dark") == null;
   }
}
