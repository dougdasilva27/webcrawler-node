package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
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

   public BrasilGimbaCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected Object fetch() {
      Request request = Request.RequestBuilder.create().setCookies(cookies).setUrl(session.getOriginalURL()).build();
      Response response = dataFetcher.get(session, request);
      cookies.addAll(response.getCookies());
      String html = response.getBody();
      return Jsoup.parse(html);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (doc.selectFirst("#produtoCadastro") != null) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "#codigoProduto p", true);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".produto-nome", false);
         boolean available = doc.selectFirst("#botao-comprar .btn-add-new") != null;
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#breadCrumb div a");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".main-thumb a", Arrays.asList("href"), "https:", "www.gimba.com.br");
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, "#thumb a", Arrays.asList("href"), "https:", "www.gimba.com.br", primaryImage);

         Offers offers = available ? scrapOffer(doc) : new Offers();

         String description = scrapDescription(doc);

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setOffers(offers)
            .setDescription(description)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private String scrapDescription(Document doc) {
      String description = "";
      String verificationToken = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "[name=__RequestVerificationToken]", "value");
      String pid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,"form#frm-produto  [id=hidProduto]","value");

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
      List<String> sales = new ArrayList<>();

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
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#valores-dados #valores-dados-preco-de", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#valores-dados-preco-por-valor", null, false, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      Double bankSlipValue = CrawlerUtils.scrapDoublePriceFromHtml(doc, "#boleto-estrutura strong", null, false, ',', session);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(bankSlipValue, null);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();


   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

      Installments installments = scrapInstallments(doc);
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

   public Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();

      Element installmentsElement = doc.selectFirst("#valores-dados-preco-parcelamento");

      if (installmentsElement != null) {
         String installmentTxt = installmentsElement.text();

         if (installmentTxt.contains("de")) {

            String installmentString = installmentTxt.split("de")[0];
            int installment = installmentString != null ? MathUtils.parseInt(installmentString) : null;

            String valueString = installmentTxt.split("de")[1];
            Double value = valueString != null ? MathUtils.parseDoubleWithComma(valueString) : null;

            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(installment)
               .setInstallmentPrice(value)
               .build());
         }
      }


      return installments;
   }

}
