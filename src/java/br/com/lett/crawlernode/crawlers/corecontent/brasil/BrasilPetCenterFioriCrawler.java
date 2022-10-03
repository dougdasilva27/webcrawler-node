package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class BrasilPetCenterFioriCrawler extends Crawler {

   public BrasilPetCenterFioriCrawler(Session session) {
      super(session);
   }
   private static final String HOME_PAGE="https://www.petcenterfiore.com.br/";
   private static final String SELLER_NAME = "PetCenterFiore";

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      Element product = doc.selectFirst(".js-has-new-shipping.js-product-detail.js-product-container.js-shipping-calculator-container");
      if( product != null) {
         String dataVariants = CrawlerUtils.scrapStringSimpleInfoByAttribute(product,".js-has-new-shipping.js-product-detail.js-product-container.js-shipping-calculator-container","data-variants");
         JSONObject dataJson = CrawlerUtils.stringToJSONObject(dataVariants.replace("[","").replace("]",""));
         String id = dataJson.getBigInteger("product_id").toString();
         String name = CrawlerUtils.scrapStringSimpleInfo(product,".js-product-name.h2.h1-md",false);
         String imgUrl = "https:"+dataJson.getString("image_url");
         String description = CrawlerUtils.scrapStringSimpleInfo(product,".product-description.user-content",false);
         Offers offers = checkAvailability( dataJson.get("available").toString()) ? scrapOffers(doc) : new Offers();
         List<String> imgsSecondaries = getSecondariesImgs(doc,imgUrl);

         Product newProduct = ProductBuilder.create()
            .setInternalId(id)
            .setInternalPid(id)
            .setUrl(session.getOriginalURL())
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(imgUrl)
            .setSecondaryImages(imgsSecondaries)
            .setDescription(description)
            .build();
         products.add(newProduct);
      }
      return products;
   }
   private Offers scrapOffers(Element data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(data);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }
   private Pricing scrapPricing(Element doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc,".js-price-display.text-primary",null,true,
         ',',session);
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc,".js-compare-price-display.price-compare.font-weight-normal",null,true,',',
         session);
      if (price != null && spotlightPrice != null && price.equals(spotlightPrice)) {
         price = null;
      }
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(price)
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

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AMEX.toString(),Card.DINERS.toString(), Card.AURA.toString(),
         Card.ELO.toString(), Card.HIPER.toString(), Card.HIPERCARD.toString(), Card.DISCOVER.toString());

      for (String card : cards) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }
   private boolean checkAvailability(String stock) {
      if (stock != null && !stock.isEmpty()) {
         boolean avaliable =stock.equals("true");
         return  avaliable;
      }
      return false;
   }
   private List<String> getSecondariesImgs(Document doc, String imgUrl ) {
      List<String> imgs=CrawlerUtils.scrapSecondaryImages(doc,".col-2.d-none.d-md-block a img", Arrays.asList("data-srcset"),"https","",imgUrl);
      List<String> returnImgs = new ArrayList<String>();
      if ( imgs!= null && !imgs.isEmpty()) {
         for (Integer i = 1; i < imgs.size(); i++) {
            returnImgs.add(getImage(imgs.get(i)));
         }
      }
      return returnImgs;
   }
   private String getImage(String values) {
      // String i[]= CommonMethods.getLast(values.split(" "));
      String imgs[] = values.split(",");
      Integer ult = imgs.length - 1;
      String pathImg[] = imgs[ult].split(" ");
      if (pathImg[1].contains("https://")) {
         return pathImg[1];
      }
      return "https:" + pathImg[1];
   }
}
