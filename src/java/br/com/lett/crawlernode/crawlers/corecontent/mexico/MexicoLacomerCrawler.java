package br.com.lett.crawlernode.crawlers.corecontent.mexico;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import java.util.*;

/**
 * Date: 28/11/2016
 * <p>
 * 1) Only one sku per page.
 * <p>
 * Price crawling notes: 1) We couldn't find any sku with status available when writing this
 * crawler. 2) There is no bank slip (boleto bancario) payment option. 3) There is no installments
 * for card payment. So we only have 1x payment, and for this value we use the cash price crawled
 * from the sku page. (nao existe divisao no cartao de credito). 4) In this market has two others
 * possibles markets, City Market = 305 and Fresko = 14 5) In page of product, has all physicals
 * stores when it is available.
 * <p>
 * Url example:
 * http://www.lacomer.com.mx/lacomer/doHome.action?succId=14&pasId=63&artEan=7501055901401&ver=detallearticulo&opcion=detarticulo
 * <p>
 * pasId -> Lacomer succId -> Tienda Lomas Anahuac (Mondelez choose)
 *
 * @author Gabriel Dornelas
 */
public class MexicoLacomerCrawler extends Crawler {

   private static final String HOME_PAGE = "http://www.lacomer.com.mx/";
   private static final String SELLER_FULL_NAME = "Lacomer";
   private final String succId = session.getOptions().optString("succId");

   public MexicoLacomerCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.APACHE);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }


   @Override
   protected Object fetch() {
      String ean = scrapEan();
      String url = "https://www.lacomer.com.mx/lacomer-api/api/v1/public/articulopasillo/detalleArticulo?artEan=" + ean + "&noPagina=1&succId="+succId;

      Request request = Request.RequestBuilder.create()
         .setUrl(url)
         .setCookies(cookies)
         .setProxyservice(List.of(
            ProxyCollection.BUY_HAPROXY,
            ProxyCollection.NETNUT_RESIDENTIAL_ROTATE_MX,
            ProxyCollection.NETNUT_RESIDENTIAL_MX
         ))
         .build();

      String content = this.dataFetcher
         .get(session, request)
         .getBody();


      return CrawlerUtils.stringToJson(content);
   }


   @Override
   public List<Product> extractInformation(JSONObject json) throws Exception {
      super.extractInformation(json);
      List<Product> products = new ArrayList<>();

      if (!json.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         JSONObject data = JSONUtils.getJSONValue(json, "estrucArti");
         if (!data.isEmpty()) {

            String internalId = JSONUtils.getIntegerValueFromJSON(data, "artCod", 0).toString();
            String internalPid = scrapEan();
            String name = scrapProductName(data);
            CategoryCollection categories = crawlCategories(data);
            List<String> images = scrapImages(json);
            String primaryImage = scrapPrimaryImage(images);
            images.remove(primaryImage);
            String description = JSONUtils.getStringValue(data, "artTexto");
            List<String> eans = new ArrayList<>();
            eans.add(scrapEan());

            Offers offers = scrapOffers(data);

            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategories(categories)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(images)
               .setDescription(description)
               .setEans(eans)
               .setOffers(offers)
               .build();

            products.add(product);
         }

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String scrapProductName(JSONObject data) {
      String completeName = JSONUtils.getStringValue(data, "art_des_com");
      if(completeName != null && !completeName.isEmpty()) {
         return completeName;
      }

      String productName = "";
      String name = JSONUtils.getStringValue(data, "artDes");
      String brand = JSONUtils.getStringValue(data, "marDes");
      int quantity = JSONUtils.getIntegerValueFromJSON(data, "artUco", 0);
      String unity = JSONUtils.getStringValue(data, "artTun");

      if(name != null && !name.isEmpty()) productName += name;
      if(brand != null && !brand.isEmpty()) productName += " " + brand;
      if(quantity != 0) productName += " " + quantity;
      if(unity != null && !unity.isEmpty()) productName += " " + unity;

      return productName;
   }

   private String scrapEan() {
      String[] eanSplit = session.getOriginalURL().split("detarticulo/");
      if (eanSplit.length > 1) {
         return eanSplit[1].split("/")[0];
      }
      return null;
   }

   private CategoryCollection crawlCategories(JSONObject data) {
      CategoryCollection categories = new CategoryCollection();
      categories.add(data.optString("departamento"));
      categories.add(data.optString("agruDes"));

      return categories;
   }

   private List<String> scrapImages(JSONObject json) {
      List<String> listImages = new ArrayList<>();

      JSONObject images = JSONUtils.getJSONValue(json, "estrucArtiImg");
      if (images != null) {
         for (Iterator<String> iter = images.keys(); iter.hasNext(); ) {
            String key = iter.next();
            listImages.add(images.optString(key));
         }
      }
      return listImages;
   }

   private String scrapPrimaryImage(List<String> imgList) {
      String primaryImage = "";

      //The primary image always ends with 3
      for (String img : imgList) {
         if(img.endsWith("_3.jpg")){
            primaryImage = img;
         }
      }

      return primaryImage;
   }

   private Offers scrapOffers(JSONObject data) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(data);
      List<String> sales = scrapSales(data, pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private List<String> scrapSales(JSONObject data, Pricing pricing) {
      List<String> sales = new ArrayList<>();

      String firstSales = data.optString("promoCartulina");

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      sales.add(CrawlerUtils.calculateSales(pricing));

      return sales;
   }


   private Pricing scrapPricing(JSONObject data) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(data, "artPrven", true);
      Double priceFrom = priceFrom(spotlightPrice, data);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }

   private Double priceFrom(Double spotlightPrice, JSONObject data) {
      Double priceFrom = JSONUtils.getDoubleValueFromJSON(data, "artPrlin", true);
      if (priceFrom != null && priceFrom.equals(spotlightPrice)) {
         priceFrom = null;
      }
      return priceFrom;
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

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


}
