package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
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

import java.util.*;

import models.Offer;
import models.Offers;
import models.pricing.BankSlip;
import models.pricing.CreditCard;
import models.pricing.CreditCards;
import models.pricing.Installment;
import models.pricing.Installments;
import models.pricing.Pricing;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class BrasilExtrabomCrawler extends Crawler {

   private static final String API = "https://www.extrabom.com.br/carrinho/verificarCepDepositoType/";
   private static final String SELLER_FULL_NAME = "Extrabom";
   private String cep = this.session.getOptions().optString("cep");

   protected Set<String> cards = Sets.newHashSet(Card.ELO.toString(), Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString(), Card.HIPERCARD.toString(),
      Card.DINERS.toString());

   public BrasilExtrabomCrawler(Session session) {
      super(session);
      config.setFetcher(FetchMode.FETCHER);
      config.setParser(Parser.HTML);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Map<String, String> headers = new HashMap<>();
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36");
      String payload = "cep=" + cep;

      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setHeaders(headers)
         .setProxyservice(Collections.singletonList(ProxyCollection.NO_PROXY))
         .setPayload(payload)
         .build();

      Response response = this.dataFetcher.post(session, request);
      this.cookies = response.getCookies();
   }

   @Override
   protected Response fetchResponse() {
      Map<String, String> headers = new HashMap<>();
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36");

      Request request = Request.RequestBuilder.create()
         .setUrl(API)
         .setHeaders(headers)
         .setProxyservice(Collections.singletonList(ProxyCollection.NO_PROXY))
         .build();

     return this.dataFetcher.get(session, request);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());


         String[] internalIdArray = CrawlerUtils.scrapStringSimpleInfo(doc, ".dados-produto .cod", true).split(":");
         if (internalIdArray.length > 1) {
            String internalId = internalIdArray[1].replace(" ", "");
            String internalPid = internalId;
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".nome-produto", true);

            CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb.breadcrumb-section a");
            String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".descricao"));
            String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".detalhe-produto__img", Arrays.asList("src"), "https:",
               "www.extrabom.com.br/");
            List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, ".detalhe-produto__thumbs-table tbody tr td a", Arrays.asList("data-img"), "https", "www.extrabom.com.br/", primaryImage);
            boolean available = !doc.select(".dados-produto__buttons .carousel__buttons .bt-item.bt-comprar").isEmpty();
            Offers offers = available ? scrapOffers(doc) : new Offers();

            // Creating the product
            Product product = ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategories(categories)
               .setDescription(description)
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setOffers(offers)
               .build();

            products.add(product);

         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".dados-produto .cod") != null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);

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

   private List<String> scrapSales(Document doc) {
      List<String> sales = new ArrayList<>();

      Element discount = doc.selectFirst(".tag-desconto-produto");
      Element promotion = doc.selectFirst(".promocao");
      String discountStr = discount != null ? discount.text() : null;
      String promotionStr = promotion != null ? promotion.text() : null;

      if (discountStr != null && !discountStr.isEmpty()) {
         sales.add(discountStr);
      }
      if (promotionStr != null && !promotionStr.isEmpty()) {
         sales.add(promotionStr);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      String interger = CrawlerUtils.scrapStringSimpleInfo(doc, ".valor .dec", true);
      String cents = CrawlerUtils.scrapStringSimpleInfo(doc, ".valor .cent", true);
      Double spotlightPrice = interger != null && cents != null ? MathUtils.parseDoubleWithComma(interger + cents) : null;
      String priceFromStr = CrawlerUtils.scrapStringSimpleInfo(doc, ".item-de__line", false);
      String[] priceFromSplit = priceFromStr != null ? priceFromStr.split("\\$") : null;
      Double priceFrom = priceFromSplit != null && priceFromSplit.length > 1 ? MathUtils.parseDoubleWithDot(priceFromSplit[1]) : null;
      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
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
