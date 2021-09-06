package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Date: 23/10/2019
 *
 * @author Gabriel Dornelas
 */
public class BrasilAbcdaconstrucaoCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.abcdaconstrucao.com.br/";

   public BrasilAbcdaconstrucaoCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
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

         String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#hdnProdutoId", "value");
         String internalPid = internalId;
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".prodTitle", false);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, "#fbits-breadcrumb li a span");
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#zoomImagemProduto", Arrays.asList("src"), "https:",
            "abcdaconstrucao.fbitsstatic.net/");
         List<String> secondaryImages = CrawlerUtils.scrapSecondaryImages(doc, "#galeria .fbits-produto-imagensMinicarrossel-item a", Arrays.asList("data-zoom-image", "data-image"),
            "https:", "abcdaconstrucao.fbitsstatic.net/", primaryImage);
         String description = CrawlerUtils.scrapStringSimpleInfo(doc, ".paddingbox", false);
         boolean available = doc.selectFirst(".fbits-preco") != null;
         Offers offers = available ? scrapOffer(doc) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setOffers(offers)
            .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".container.tpl-produto .produto-section-detalhes") != null;
   }

   private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = scrapSales(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("abc da constução")
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

      Element salesOneElement = doc.selectFirst(".fbits-preco-off");
      String firstSales = salesOneElement != null ? salesOneElement.text() : null;

      if (firstSales != null && !firstSales.isEmpty()) {
         sales.add(firstSales);
      }

      return sales;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {


      Double spotlightPrice = calculatePriceSquareMeter(doc);

      if (spotlightPrice == 0d) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fbits-preco .precoPor", null, false, ',', session);
      }

      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".fbits-preco .precoDe", null, false, ',', session);

      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
      Double bank = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".produtoPreco-boleto .precoParcela .fbits-parcela", null, false, ',', session);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(bank, null);

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .setBankSlip(bankSlip)
         .build();

   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();

      Installments installments = scrapInstallments(doc);
      if (installments.getInstallments().isEmpty()) {
         installments.add(Installment.InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());
      }

      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

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

      Elements installmentsCard = doc.select(".details-content p");

      for (Element e : installmentsCard) {

         if (e != null) {

            String installmentString = e.text();

            Integer installment = installmentString.contains("x") ? MathUtils.parseInt(installmentString.split("x")[0]) : null;

            //4 x sem juros de R$ 29,49 no Cartão
            String valueString = installmentString.contains("R$") ? installmentString.split("R")[1].replace("$ ", "") : null;
            String valueString2 = valueString != null && valueString.contains(" ") ? valueString.split(" ")[0] : null;

            double installmentValue = valueString2 != null ? MathUtils.parseDoubleWithComma(valueString2) : null;

            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(installment)
               .setInstallmentPrice(installmentValue)
               .build());
         }
      }

      return installments;
   }

   private double calculatePriceSquareMeter(Document doc) {
      double spotilightPrice = 0D;
      Double boxPriceSquareMeter;

      String squareMeter = scrapPriceBoxFromDescription(doc);
      if (squareMeter.contains(",")) {
         boxPriceSquareMeter = MathUtils.parseDoubleWithComma(squareMeter);

      } else {
         boxPriceSquareMeter = MathUtils.parseDoubleWithDot(squareMeter);
      }

      Double boxPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".precosDiv .precoPor", null, true, ',', session);

      if (boxPrice != null && boxPriceSquareMeter != null) {
         spotilightPrice = MathUtils.normalizeTwoDecimalPlaces(boxPrice / boxPriceSquareMeter);
         double fivePerCent = 0.05 * spotilightPrice;
         spotilightPrice = spotilightPrice - fivePerCent;
      }

      return spotilightPrice;
   }


   private String scrapPriceBoxFromDescription(Document doc) {

      String priceBox = "";

      Elements elements = doc.select("#conteudo-0 .paddingbox ul li");
      if (elements.isEmpty()) {
         elements = doc.select(".paddingbox p");
      }

      for (Element e : elements) {
         String element = e.toString().toLowerCase().replace(" ", "");
         if (!element.isEmpty() && element.contains("m2/caixa:") || element.contains("m2porcaixa:") || element.contains("m²porcaixa") || element.contains("m²/caixa")) {
            priceBox = CommonMethods.getLast(e.text().split(":"));

            break;
         }
      }
      return priceBox;
   }
}
