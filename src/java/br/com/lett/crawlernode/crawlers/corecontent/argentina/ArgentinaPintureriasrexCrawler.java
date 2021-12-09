package br.com.lett.crawlernode.crawlers.corecontent.argentina;

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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class ArgentinaPintureriasrexCrawler extends Crawler {
   protected Set<String> cards = Sets.newHashSet(Card.MASTERCARD.toString(), Card.VISA.toString(), Card.NARANJA.toString(), Card.AMEX.toString(), Card.COBAL.toString(), Card.CORDOBESA.toString());
   private static final String SELLER_NAME = "Pinturerias Rex";

   public ArgentinaPintureriasrexCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      super.extractInformation(document);
      List<Product> products = new ArrayList<>();


      if(!isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Not a product page" + session.getOriginalURL());
         return products;
      }

      // Get all product information
      String productName = CrawlerUtils.scrapStringSimpleInfo(document,".base", false);
      String productInternalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document,".price-box.price-final_price", "data-product-id");
      String productInternalPid = productInternalId;
      String productDescription = CrawlerUtils.scrapStringSimpleInfo(document,".product.attribute.description .value", false);
      String productPrimaryImage = CrawlerUtils.scrapSimplePrimaryImage(document, ".image-container img", Arrays.asList("src"), "", "");
      List<String> productSecondaryImages = ImageCapture(document, productPrimaryImage);

      ProductBuilder builder = ProductBuilder.create().setUrl(session.getOriginalURL());
      builder.setName(productName)
         .setInternalId(productInternalId)
         .setInternalPid(productInternalPid)
         .setDescription(productDescription)
         .setPrimaryImage(productPrimaryImage)
         .setSecondaryImages(productSecondaryImages);


      builder.setOffers(scrapOffers(document));
      products.add(builder.build());


      return products;

   }


   private Offers scrapOffers(Document document) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(document);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));
      offers.add(new Offer.OfferBuilder()
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .setSales(sales)
         .build()
      );

      return offers;
   }

   private Pricing scrapPricing(Document document) throws MalformedPricingException {
      Integer id = CrawlerUtils.scrapIntegerFromHtmlAttr(document, ".price-box.price-final_price", "data-product-id", 0);
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(document,"#product-price-"+id, "data-price-amount", true, '.', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(document, "#old-price-"+id, "data-price-amount", true,  '.', session );
      CreditCards creditCards = CrawlerUtils.scrapCreditCards(price, cards);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }

   private List<String> ImageCapture (Document document, String productPrimaryImage) throws Exception {
      int count = 0;
      int statusCode;
      int number = 2;
      List<String> productSecondaryImagesList = new ArrayList<>();
      do{
         String newUrlImage = productPrimaryImage.replaceAll(".jpg", "");
         statusCode = ImageRequest(newUrlImage+"-0"+number+".jpg");
         if(statusCode == 200){
            productSecondaryImagesList.add(newUrlImage+"-0"+number+".jpg");
            count++;
            number++;
         }
      }while (statusCode == 200);

      return productSecondaryImagesList;
   }
   private int ImageRequest (String imageUrl) throws Exception{
      int statusCode = 200;

      try {
         URL url = new URL(imageUrl);
         HttpURLConnection connection = (HttpURLConnection) url.openConnection();

         if (connection.getResponseCode() != statusCode){
            return 404;
         }


         return statusCode;
      } catch (Exception e) {
         return 404;
      }
   }


   private boolean isProductPage(Document document) {
      return document.selectFirst(".product.media") != null;
   }
}
