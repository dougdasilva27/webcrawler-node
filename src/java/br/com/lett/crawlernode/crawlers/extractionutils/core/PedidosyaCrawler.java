package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.json.JSONObject;
import org.jsoup.nodes.Document;

import java.util.*;

public class PedidosyaCrawler extends Crawler {
   public PedidosyaCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.FETCHER);
   }

   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.HIPERCARD.toString(), Card.ELO.toString());
   private static final String MAINSELLER = "Pedidos Ya";

   public void handleCookiesBeforeFetch() {
      Request request = Request.RequestBuilder.create().setUrl("https://www.pedidosya.com.ar").build();
      this.cookies = this.dataFetcher.get(session,request).getCookies();
   }

   private JSONObject getinfoFromApi(){
      String internalId = getInternalIdFromUrl();

     String storeId =  session.getOptions().optString("store_id");

      String url = "https://www.pedidosya.com.ar/mobile/v1/products/" + internalId + "?restaurantId="+storeId+"&businessType=GROCERIES";

      Map<String,String> headers = new HashMap<>();
     headers.put("cookie", "_gcl_au=1.1.1011751451.1637583178; _pxvid=866bbcf0-4b8d-11ec-b905-6f634349694f; _fbp=fb.2.1637583178210.170594016; _ga=GA1.3.774476031.1637583179; _gid=GA1.3.996295816.1637583179; _hjSessionUser_350054=eyJpZCI6Ijk2N2M2ODcyLWZmYmMtNWIxNC1iZjI3LTk0MTUxMWYzNWE5NiIsImNyZWF0ZWQiOjE2Mzc1ODMxNzc5MTMsImV4aXN0aW5nIjp0cnVlfQ==; _pxhd=Q/jSWxeJcG5bPGZvno91ZeOE5/qmvOZdERbzu/S84kSWQdGREa4OFYjsWsJ8uoHO/N-NUt0KPDxd7WAjfdg70A==:3zyyELqg4n3ARRY3ndaPBNJzsyOKtFv5QRopUngk6zaJFpbAkewzAg6A0jqnMrJhW1S-vgJUz/OoTDvPoaOcXHvmxqsyqryWTQCAIdP9H5pjhecPw-FXdJtIxtXqRXvFADSjdUWMV-KxcrU4T010DQ==; pxcts=90549120-4b9e-11ec-8817-4f0b938e9d55; dhhPerseusGuestId=1637601706139.173991779938371460.9g0bmi95tsp; dhhPerseusSessionId=1637601706139.254603884442502900.dw4yjheabkf; __Secure-peya.sid=s%3A1c3f5a7a-c34a-452b-af94-02edc619e933.1V6q0xhX1BZXR1c1sWIuHp5vfBchYf01rNWdWdMERQo; __Secure-peyas.sid=s%3A8a1c5ae7-ed41-414c-9156-109449554894.ZWPiSl%2BxEPjKHDeWC5HM1WkPNcom0bHJxC0m0zbfg28; __cf_bm=QGM0Zv6vfaMsbiaBBUJ45zQKG4ZSpoKEz2z_rvV3kgc-1637601706-0-AcrjjGTA1yOngwVBYwckLzROhaU9tTASoOPLTSdnnZiz7rCLfGJ5oYqf9qSLt2Fd7+fM0zIrb60zar34E9IhWO8=; _hjSession_350054=eyJpZCI6ImExMWYxYTZhLTFmOTctNGEzZi05ZmI1LTg0ODA1YjRiYTAwMCIsImNyZWF0ZWQiOjE2Mzc2MDE3MDc3NTZ9; _hjIncludedInPageviewSample=1; _hjAbsoluteSessionInProgress=0; _hjIncludedInSessionSample=0; AMP_TOKEN=%24NOT_FOUND; _gat_WD2_Tracker_PeYa_Prod=1; _tq_id.TV-81819090-1.20dc=875790c825561a67.1637583179.0.1637601764..; _px3=8c20f2798a26a95f32931157b56de8ff377c9b9e94ef1411f02bd8373db0efa8:PXqjpGw7T/Cu2XDDN5HJ0kpwMTkaUmTHCQDuC+DwH+bcjB4wJvvehQzvEyH0fxx4t5q0B0qAUWE6M7mXGUddLQ==:1000:I0cMlPx96TnpSPg7jeBcf2doRvPrsjDATssKQyj1n6hL8Bczxjdj54NJXroQjNQghhYEJYjuHz/0nQYW7MdpuVtwGcGgm2lG86fT2injnOuLA3sJbsS37JQ8NO7yNtPfHihaOX3IzV227aW7rFdrupY38A8zwaCRcPus4es5KzF8lpoNUYfiJ6DMgHMGm7wQn1X96AxZp2frFO++40cnkA==; dhhPerseusHitId=1637601765900.372113030709838800.eruqgjb5v");

     Request request = Request.RequestBuilder.create().setUrl(url).setHeaders(headers).build();
      String resp = this.dataFetcher.get(session,request).getBody();
      return CrawlerUtils.stringToJson(resp);

   }
   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      JSONObject productInfo = getinfoFromApi();

      if (productInfo.has("name")) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = productInfo.optString("id");
         String internalPid = internalId;
         String name = productInfo.optString("name");
         String primaryImage = "https://images.deliveryhero.io/image/pedidosya/products/" + productInfo.optString("image");
         boolean available = productInfo.optBoolean("enabled");
         Offers offers = available ? scrapOffers(productInfo) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setOffers(offers)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private String getInternalIdFromUrl() {
      String originalUrl = CommonMethods.getLast(session.getOriginalURL().split("p="));
      originalUrl = originalUrl.split("&menu")[0];
      return originalUrl;
   }

   private Offers scrapOffers(JSONObject productInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(productInfo);
      List<String> sales = scrapSales(pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setMainPagePosition(1)
         .setSellerFullName(MAINSELLER)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setSales(sales)
         .setPricing(pricing)
         .build());


      return offers;

   }

   private List<String> scrapSales(Pricing pricing) {
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      return sales;
   }


   private Pricing scrapPricing(JSONObject productInfo) throws MalformedPricingException {

      Double spotlightPrice = productInfo.optDouble("price");

      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
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
