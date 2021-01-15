package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.*;

public class BrasilDimedCrawler extends Crawler {

   private static final String HOST = "www.dimed.com.br";

   private static final String USER = "tstpfo";
   private static final String PASSWORD = "dimed987@Poa";

   public BrasilDimedCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.DINERS.toString(), Card.AMEX.toString());

   @Override
   protected Object fetch() {

      String payload = "j_username=" + USER + "&j_password=" + PASSWORD + "";
      HashMap<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");

      Request requestValidate = Request.RequestBuilder.create()
         .setUrl("https://dimed.com.br/clientes/j_spring_security_check")
         .setHeaders(headers)
         .setPayload(payload)
         .build();

      String urltoken = dataFetcher.post(session, requestValidate).getRedirectUrl();

      String cookieName = "jsessionid=";
      Integer index = urltoken != null ? urltoken.indexOf(cookieName) + cookieName.length() : null;
      String token = urltoken != null ? urltoken.substring(index) : null;

      List<Cookie> cookies = new ArrayList<>();
      cookies.add(new BasicClientCookie("JSESSIONID", token));

      String cookie = "JSESSIONID=\"" + token + "\";";

      headers.put("Cookie", cookie);

      Request request = Request.RequestBuilder.create()
         .setUrl(session.getOriginalURL())
         .setHeaders(headers)
         .build();

      Response response = dataFetcher.get(session, request);

      return Jsoup.parse(response.getBody());
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=codigoProduto]", "value");

      if (internalId != null && !internalId.isEmpty()) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".detalhes .descricaoproduto", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".imagem-principal img", Arrays.asList("src"), "https", HOST);

         if (primaryImage != null) {
            primaryImage = primaryImage.replace("235x", "500x");
         }

         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".image-gallery__image .lazyOwl", Arrays.asList("data-zoom-image", "src"), "https", HOST, primaryImage);
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".fieldcontain:not(.descricaoproduto):not(.precoproduto)"));
         String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".fieldcontain.ean span", true);
         boolean available = doc.selectFirst(".btaviseme") == null;
         Offers offers = available ? scrapOffers(doc) : new Offers();
         List<String> eans = Arrays.asList(internalPid);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setOffers(offers)
            .setDescription(description)
            .setEans(eans)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      if (pricing != null) {
         offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName("Dimed")
            .setSellersPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .build());
      }

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fieldcontain.precoproduto", null, false, ',', session);

      if (spotlightPrice != null) {
         Double priceFrom = null;
         CreditCards creditCards = scrapCreditCards(spotlightPrice);

         return PricingBuilder.create()
            .setSpotlightPrice(spotlightPrice)
            .setPriceFrom(priceFrom)
            .setCreditCards(creditCards)
            .build();
      }

      return null;
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = new Installments();
      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return creditCards;
   }
}
