package br.com.lett.crawlernode.crawlers.extractionutils.core;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
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
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class FerreiracostaCrawler extends Crawler {

   private static final String SELLER_FULL_NAME = "ferreira costa";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


   public FerreiracostaCrawler(Session session) {
      super(session);
   }

   @Override
   public Object fetch(){


      String location = session.getOptions().optString("location");

      Map<String,String> headers = new HashMap<>();
      headers.put("cookie", "ASP.NET_SessionId=afdhrtmd1chvych4b1y1zqmv;" +
         " osVisitor=b44b973b-4257-4e0b-a2ca-47e37d439251;" +
         " osVisit=f29d817f-51ed-4082-8dd0-7ae9b309c837;" +
         " eco_ce=; ecoFC=FCB02C35F253FCE56D9696DC7757BC6E;" +
         " _gid=GA1.2.1245936171.1631798355;" +
         " _gat_UA-48783684-2=1;" +
         " _gat_gtag_UA_48783684_2=1;" +
         " G_ENABLED_IDPS=google;" +
         " _fbp=fb.1.1631798354801.1640176812;" +
         " _pin_unauth=dWlkPU9XVTBZMlExTlRRdE5UZ3pNeTAwTWprd0xXSTNNMkV0TVRWaE5XVTJZVE0yTkRZMw;" +
         " _hjid=fdb9c70b-3929-430b-8698-0a11e9020213;" +
         " _hjIncludedInSessionSample=1;" +
         " _hjAbsoluteSessionInProgress=0;" +
         " eco_lo=" + location + ";" +
         " _ga=GA1.2.1447234012.1631798354;" +
         " _ga_DD7Y69210P=GS1.1.1631798009.3.1.1631798369.0;" +
         " cto_bundle=QnJvIl9STm1tMVN2ZVhydXYxSXhnSGJFbEV1ak1kT1VRYXlGRnIyUldQbm4lMkZhM0hFVWNCYXM4JTJCUDRJUFklMkIzJTJGblglMkJaSCUyQkU5QkUlMkYweWVVNUU5bkYxWkV0bXdzYnZEOWxxV2xEdjFZMDIlMkZvSTVWTnRueGVKZDZxT3dRQ05SbnQlMkJ0cWdvYnZac1pBSSUyRkZtenpzVzA0RFRQSiUyQmZBJTNEJTNE;" +
         " AWSALB=AWQren+oPEAFGXDRiJutL5+sy0hpn5zZBAoiHwI5wCsthQh1UcN4sz5hYfT2hEfrlKuY45Vz5J0qEsHDS9JBbMDsPqDb7l12m63zMvEokIrgKyyLHS5mFcV8YyT+;" +
         " AWSALBCORS=AWQren+oPEAFGXDRiJutL5+sy0hpn5zZBAoiHwI5wCsthQh1UcN4sz5hYfT2hEfrlKuY45Vz5J0qEsHDS9JBbMDsPqDb7l12m63zMvEokIrgKyyLHS5mFcV8YyT+;" +
         " RT=s=1631798388140&r=https%3A%2F%2Fwww.ferreiracosta.com%2FProduto%2F408846%2Flavadora-de-roupa-brastemp-12kg-branca-127v-bwk12abana");

      Request request = Request.RequestBuilder.create().setUrl(session.getOriginalURL()).setHeaders(headers).setCookies(this.cookies).build();
      Response resp = this.dataFetcher.get(session,request);

      return Jsoup.parse(resp.getBody());

   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".detalhe-produto") != null) {
         Logging
            .printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());

         JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "span script[type=\"application/ld+json\"]","", null, false, false);
         JSONObject offersInfo = jsonInfo.optJSONObject("offers");

         String name = jsonInfo.optString("name");
         String internalId = jsonInfo.optString("productID");
         String description = jsonInfo.optString("description");
         String primaryImage = jsonInfo.optString("image");
         boolean available = doc.selectFirst(".btn.btn--full.btn--light-gray") == null;

         Offers offers = available ? scrapOffers(offersInfo): new Offers();

               Product product = ProductBuilder.create()
                  .setUrl(session.getOriginalURL())
                  .setInternalId(internalId)
                  .setInternalPid(internalId)
                  .setOffers(offers)
                  .setName(name)
                  .setPrimaryImage(primaryImage)
                  .setDescription(description)
                  .build();

               products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }


   private Offers scrapOffers(JSONObject offersInfo) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(offersInfo);
      List<String> sales = new ArrayList<>();

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


   private Pricing scrapPricing(JSONObject offersInfo) throws MalformedPricingException {
      Double spotlightPrice = offersInfo.optDouble("price");
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .build();
   }


   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
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
}
