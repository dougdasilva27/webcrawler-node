package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.*;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.cookie.Cookie;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class BrasilGimbaCrawler extends Crawler {

   private final String HOME_PAGE = "https://www.gimba.com.br/";
   private final Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.ELO.toString(), Card.DINERS.toString(), Card.AMEX.toString());

   public BrasilGimbaCrawler(Session session) {
      super(session);
      super.config.setParser(Parser.HTML);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Response fetchResponse() {
      Request request = Request.RequestBuilder.create().setCookies(cookies).setUrl(session.getOriginalURL()).build();
      Response response = dataFetcher.get(session, request);
      cookies.addAll(response.getCookies());
      return response;
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst(".pdp-product-title") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = scrapInternalId(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".pdp-product-title", true);
         boolean available = doc.selectFirst(".pdp-produto-indisponivel-form") == null;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb-item");
         List<String> images = CrawlerUtils.scrapSecondaryImages(doc, ".pdp-galeria img", List.of("src"), "https", "images.gimba.com.br", null);
         String primaryImage = images.remove(0);
         Offers offers = available ? scrapOffer(doc) : new Offers();

         String description = scrapDescription(doc);

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategories(categories)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setOffers(offers)
            .setDescription(description)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private String scrapInternalId(Document doc) {
      String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, ".pdp-product-infos-list > li:first-child", false);
      if (internalId != null && !internalId.isEmpty()) {
         return internalId.replaceAll("[^0-9]", "");
      }
      return null;
   }

   private String scrapDescription(Document doc) {
      String description = "";
      String verificationToken = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[name=__RequestVerificationToken]", "value");
      String pid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "form#frm-produto  [id=hidProduto]", "value");

      if (verificationToken != null && pid != null) {
         String url = "https://www.gimba.com.br/produtos/JsonRetornaProdutoDetalhe?id=" + pid + "&kit=false";
         String payload = "__RequestVerificationToken=" + verificationToken;
         String cookietoken = "";

         for (Cookie cookie : cookies) {
            if (cookie.getName().equals("__RequestVerificationToken")) {
               cookietoken = cookie.getValue();
            }
         }

         String requestCookieValue = "__RequestVerificationToken=" + cookietoken + ";";
         Map<String, String> headres = new HashMap<>();
         headres.put("cookie", requestCookieValue);
         headres.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.183 Safari/537.36");
         headres.put("Content-Type", "application/x-www-form-urlencoded");
         headres.put("Host", "www.gimba.com.br");

         Request request = Request.RequestBuilder
            .create()
            .setUrl(url)
            .setHeaders(headres)
            .setPayload(payload)
            .build();

         String response = dataFetcher.post(session, request).getBody();
         if (response != null) {
            Document document = Jsoup.parse(StringEscapeUtils.unescapeJava(response));
            description = CrawlerUtils.scrapStringSimpleInfo(document, ".fonte-descricao-prod dd", false);
         }
      }

      return description;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("Gimba")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".pdp-valores .pdp-valor-antigo", null, true, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".pdp-valores .pdp-valor-atual", null, true, ',', session);
      Double bankSlipValue = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".pdp-valores > small > strong", null, true, ',', session);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(bankSlipValue, null);

      CreditCards creditCards = CrawlerUtils.scrapCreditCards(spotlightPrice, cards);

      if (priceFrom == spotlightPrice) priceFrom = null;

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }
}
