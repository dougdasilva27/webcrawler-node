package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
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
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilFerramentasKennedyCrawler extends Crawler {
   public BrasilFerramentasKennedyCrawler(Session session) {
      super(session);
   }
   private static final String SELLER_FULL_NAME = "Ferramentas Kennedy";

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);
      List<Product> products = new ArrayList<>();
      if (doc.selectFirst("span.sku") != null) {
         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div.top input[id*=produto]", "value");
         String internalId = crawlInternalId(doc);

         String primaryImage = crawlImage(doc);

         CategoryCollection categoryCollection = CrawlerUtils.crawlCategories(doc, "li[itemprop=itemListElement]");
//       VARIAÇÔES _ ACREDITO SER NECESSARIO LOGAR NAS PAGINAS
//         https://www.ferramentaskennedy.com.br/100083974/esmerilhadeira-angular-4-12-840w-9557hng-127v-makita
//         Testar substitui o pid 100083973

         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.title-product", true);
         String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("div.descricao-prod"));
         boolean available = doc.selectFirst("div.product-config button.btn-success") != null;
         Offers offers = available ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalPid(internalPid)
            .setInternalId(internalId)
            .setName(name)
            .setOffers(offers)
            .setPrimaryImage(primaryImage)
            .setCategories(categoryCollection)
           // .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .build();
         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page:   " + this.session.getOriginalURL());
      }
      return products;
   }

   private String crawlImage(Document doc) {
      String img = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "div[id=custom-dots] .item", "style");
      String regex = "'(.*)'";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(img);
      if(matcher.find()){
         return matcher.group(1).replace("90","1200");
      }
      return null;
   }

   private String crawlInternalId(Document doc) {
      String id = CrawlerUtils.scrapStringSimpleInfo(doc, "span.sku", true);
      String regex = ": ([0-9]*)";

      Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(id);
      if(matcher.find()){
         return matcher.group(1);
      }
      return null;
   }

   private Offers scrapOffers(Document doc) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();
      List<String> sales = new ArrayList<>();

      Pricing pricing = scrapPricing(doc);
      sales.add(CrawlerUtils.calculateSales(pricing));

      offers.add(Offer.OfferBuilder.create()
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSales(sales)
         .setSellerFullName(SELLER_FULL_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {
      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc,"div.product-price p",null,false,',' ,session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc,"div.product-price del",null,false,',' ,session);

      CreditCards creditCards = scrapCreditCards(spotlightPrice);
      BankSlip bankSlip = BankSlip.BankSlipBuilder.create()
         .setFinalPrice(spotlightPrice)
         .build();

      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(spotlightPrice)
         .setPriceFrom(priceFrom)
         .setBankSlip(bankSlip)
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

      Set<String> cards = Sets.newHashSet(Card.AMEX.toString(), Card.AURA.toString(), Card.DINERS.toString(), Card.ELO.toString(),
         Card.HIPERCARD.toString(),Card.HIPERCARD.toString(), Card.VISA.toString());

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
