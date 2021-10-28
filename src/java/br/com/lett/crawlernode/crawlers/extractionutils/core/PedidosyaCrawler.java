package br.com.lett.crawlernode.crawlers.extractionutils.core;

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
     headers.put("cookie", "_fbp=fb.2.1635281248370.496271014; _pxhd=6U5JYRKMSk6RzsQWMwANFlWmB1/kTV5DET3V6-AVNsCGjQiXiLmupcVbJfyNf2/R7k0iwDpKalZz68ibNJNDsg==:RCvqJeZHjASLONsiYhtUc1qYsQD9apAa-6RjaN/XPRt-4pj5RqcKvVfNEHm9AyxY-xnzUdFODUi4Mw-XE0tD1noVcoQNiwVpnfptUxZHTwI5q2bhWsK2W/EfSgPpdBcs; pxcts=f543cf30-369d-11ec-88df-8389a1d74c91; _pxvid=ef399119-369d-11ec-a6e1-49646e467a67; _hjid=05198fcf-eadc-4017-9886-d554d56eade8; _gcl_au=1.1.924052929.1635281260; _ga=GA1.3.293164481.1635281260; _gid=GA1.3.1074224560.1635281260; __Secure-peya.sid=s%3Afae4d9e4-7bd2-43f3-ad4f-c5c6b3282281.MioIplqrvnNAEu94JYbWreMN0ZcF6oZgR%2FIApTN8%2FX4; __Secure-peyas.sid=s%3A2d9ba471-0c9e-463d-977d-0060169c2aa5.Fbq6NPL9gxNQIpYiUviKQcdMwl3xcPBOVO2xbQy3uCY; __cf_bm=i6N6CXna3aXcYx0fpiJRXK_hZ9M06ZgSwlysEw9RTNE-1635361545-0-AY9LhaJXZm6LctAV2pLAX6fr88kVdd8CCU3Wt7jdNIc2xH0B/OFoAhxPPAI5Gk0GyGoY2zHQYhtDAkP1VxROiLA=; _hjIncludedInSessionSample=0; _hjAbsoluteSessionInProgress=1; AMP_TOKEN=%24NOT_FOUND; _px3=13cfe4458c93f119941e21ca559e3894dd2a0d6b621560a9979278d5cfc54161:k7FsqkIIWpvuBTLkbe+J6dHVQdHWhG21P3PYbLW4zTGKV4QgzhBMsXo3Gk55sbS9fpgc1QqA/XTW5KyeEKDXBw==:1000:wzIU4jhXy9177R4oB3RptpzUyZ8ODKser89dfyGKkbKMWQrQRd8pk1Af6dg6cfexs/PzErJ14W1BFLBkO8Udg4pew9how7tBWam5k3rK4MqGXiIB4QdyM+SZLZ0E5f8wUyPuV/wY+LMI1hFrtVAncQE38ch+FwUeQHNUpm9beVidkJrTP1XEq45DHU/eX8g1BTPM5ABDBR5IODmd9Y1zwg==; _gat_WD2_Tracker_PeYa_Prod=1; _tq_id.TV-81819090-1.20dc=6c224119bc0470f7.1635281248.0.1635362468..");

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
