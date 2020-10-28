package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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
import models.Marketplace;
import models.Offer;
import models.Offers;
import models.prices.Prices;
import models.pricing.*;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

/**
 * Date: 27/10/2020
 *
 * @author Marcos Moura
 *
 */
public class BrasilNanacareCrawler extends Crawler {

   private final String HOME_PAGE = "https://www.nanacare.com.br/";

   public BrasilNanacareCrawler(Session session) {
      super(session);
      super.config.setMustSendRatingToKinesis(true);
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

         String internalId = CrawlerUtils.scrapStringSimpleInfo(doc, "[itemprop =\"sku\"]", false);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".info-principal-produto .nome-produto", false);
         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".conteiner-imagem a", Arrays.asList("href"),"https:","cdn.awsli.com.br");
         //When this scraper was made I couldn't find any product that had secondary images
         String description = CrawlerUtils.scrapStringSimpleInfo(doc ,"#descricao p", true);
         boolean available = doc.selectFirst(".acoes-produto.disponivel .comprar .botao-comprar") != null;
         Offers offers = available ? scrapOffer(doc): new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalId)
            .setName(name)
            .setPrimaryImage(primaryImage)
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
      return doc.selectFirst(".info-principal-produto") != null;
   }

   private Offers scrapOffer(Document doc ) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);
      List<String> sales = new ArrayList<>();

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName("Nana Care")
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .setSales(sales)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".principal .preco-venda ", null, false, ',', session);
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".principal .preco-produto .preco-promocional", null, false, ',', session);
      Double bank = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".principal .text-parcelas.pull-right.cor-principal", null, false, ',', session);
      BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(bank, null);
      CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);

      return Pricing.PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setBankSlip(bankSlip)
         .setCreditCards(creditCards)
         .build();
   }

   private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


      Installments installments = scrapInstallments(doc);
      if (installments == null || installments.getInstallments().isEmpty()) {
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

      Elements installmentsAndValues = doc.select(".accordion-body.collapse.in .accordion-inner li");

      if(installmentsAndValues != null && !installmentsAndValues.isEmpty()) {

         for(Element eachInstallmentAndValue: installmentsAndValues) {
            String eachInstallmentAndValueString = eachInstallmentAndValue != null? eachInstallmentAndValue.text():null;

            String installmentString = eachInstallmentAndValueString.contains("x")? eachInstallmentAndValueString.split("x")[0]: null;
            int installment = installmentString != null? MathUtils.parseInt(installmentString):0;

            String valueString = eachInstallmentAndValueString.contains("R$")? eachInstallmentAndValueString.split("R")[1]: null;
            Double value = valueString != null? MathUtils.parseDoubleWithComma(valueString): 0D;

            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(installment)
               .setInstallmentPrice(value)
               .build());
         }
      }
      return installments;
   }

}
