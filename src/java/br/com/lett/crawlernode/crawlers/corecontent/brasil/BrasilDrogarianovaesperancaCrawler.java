package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offers;
import models.prices.Prices;
import models.pricing.*;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Date: 08/08/2017
 *
 * @author Gabriel Dornelas
 */
public class BrasilDrogarianovaesperancaCrawler extends Crawler {

   private final String HOME_PAGE = "https://www.drogarianovaesperanca.com.br/";
   private static final List<Card> CARDS = Arrays.asList(Card.VISA, Card.MASTERCARD, Card.AMEX, Card.ELO);

   public BrasilDrogarianovaesperancaCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.APACHE);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String internalPid = null;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-head h1", true);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#imgProduto", List.of("src"), "https", "drogarianovaesperanca.com.br");
         List<String> secondaryImages = scrapSecondaryImages(doc, primaryImage);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "span[property=itemListElement]:not(:last-child)", true);
         String description = CrawlerUtils.scrapSimpleDescription(doc, List.of(".ficha-produto", ".tabs-produto #tabs", ".aviso-medicamento"));
         List<String> eans = crawlEan(doc);
         boolean availableToBuy = doc.selectFirst(".produto-esgotado-avise") == null;

         Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setCategories(categories)
            .setDescription(description)
            .setOffers(offers)
            .setEans(eans)
            .build();
         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private List<String> scrapSecondaryImages(Document doc, String primaryImage) {
      List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, "#thumbs-produto img", List.of("src"), "https", "drogarianovaesperanca.com.br", primaryImage);
      secondaryImages = secondaryImages.stream().map(image -> image.replace("100x100", "1000x1000")).collect(Collectors.toList());
      return secondaryImages;
   }

   private List<String> crawlEan(Document doc) {
      List<String> eans = new ArrayList<>();
      Elements elements = doc.select(".ficha-produto ul li div ul li");

      for (Element e : elements) {
         String aux = e.text();

         if (aux.contains("Código EAN")) {
            eans.add(aux.replaceAll("[^0-9]+", ""));
            break;
         }
      }

      return eans;
   }

   private boolean isProductPage(Document doc) {
      return doc.select("#ID_SubProduto").first() != null;
   }

   private String crawlInternalId(Document doc) {
      String internalId = null;

      Element internalIdElement = doc.select("#ID_SubProduto").first();
      if (internalIdElement != null) {
         internalId = internalIdElement.val();
      }

      return internalId;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc, pricing);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("Drogaria Nova Esperanca")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setSales(sales)
         .setPricing(pricing)
         .build());

      return offers;
   }

   private List<String> scrapSales(Document doc, Pricing pricing) {
      List<String> sales = new ArrayList<>();
      sales.add(CrawlerUtils.calculateSales(pricing));

      String buyPromotion = CrawlerUtils.scrapStringSimpleInfo(doc, ".BtComprarProdutoPromocao", false);
      if(buyPromotion != null && !buyPromotion.isEmpty()) {
         sales.add(buyPromotion);
      }
      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".valor-preco-por", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".preco-de", null, true, ',', session);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      BankSlip bankSlip =  BankSlip.BankSlipBuilder.create().setFinalPrice(spotlightPrice).build();

      if(priceFrom == spotlightPrice) priceFrom = null;

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(Installment.InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      Integer maxInstallments = CrawlerUtils.scrapIntegerFromHtml(doc, ".preco-parcelado-produto .parcelas", true, null);
      Double installmentPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".preco-parcelado-produto > b  > span:not(:first-child)", null, true, ',', session);
      if (maxInstallments != null && installmentPrice != null) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(maxInstallments)
            .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(installmentPrice))
            .build());
      }

      for (Card card : CARDS) {
         creditCards.add(CreditCard.CreditCardBuilder.create()
            .setBrand(card.name())
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return creditCards;
   }

}
