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
import br.com.lett.crawlernode.util.Pair;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Paula
 */

public class BrasilCasadoprodutorCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.casadoprodutor.com.br/";
   private static final String FULL_SELLER_NAME = "Casa do produtor";
   private Set<String> cards = Sets.newHashSet(Card.MASTERCARD.toString(), Card.VISA.toString(), Card.ELO.toString(), Card.AMEX.toString());

   public BrasilCasadoprodutorCrawler(Session session) {
      super(session);
   }


   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=product]", "value");

      if (internalId != null) {
         Logging.printLogDebug(logger, session,
            "Product page identified: " + this.session.getOriginalURL());


         String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "meta[itemprop=sku]", "content");
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name  h1", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs ul li:not(:first-child)");
         String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList(".__descricao"));

         Offers offers = doc.selectFirst(".out-of-stock") == null ? scrapOffers(doc) : new Offers();

         String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-image img", Collections.singletonList("src"), "https:", "www.casadoprodutor.com.br");

         // Creating the product
         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setOffers(offers)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setDescription(description)
            .build();

         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;
   }

   /*******************
    * General methods *
    *******************/


   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();


      Pricing pricing = scrapPricing(doc);

      offers.add(OfferBuilder.create()
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSellerFullName(FULL_SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .build());

      return offers;
   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".regular-price .price", null, false, ',', session);

      CreditCards creditCards = new CreditCards();
      Installments installments = scrapInstallments(doc);

      for (String brand : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(brand)
            .setIsShopCard(false)
            .setInstallments(installments)
            .build());
      }

      return PricingBuilder.create()
         .setSpotlightPrice(price)
         .setBankSlip(BankSlipBuilder.create()
            .setFinalPrice(price)
            .build())
         .setCreditCards(creditCards)
         .build();
   }

   private Installments scrapInstallments(Document doc) throws MalformedPricingException {
      Installments installments = new Installments();

      Elements elements = doc.select(".parcelas-list tbody tr");
      for (Element element : elements) {
         Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallmentFromString(element.text(), "x", "", true);
         installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(pair.getFirst())
            .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
            .build());
      }
      return installments;
   }

}
