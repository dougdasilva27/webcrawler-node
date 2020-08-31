package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

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
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


/**
 * Date: 20/08/2018
 *
 * @author victor
 */
public class RiodejaneiroZonasulCrawler extends Crawler {

   public static final String HOME_PAGE = "https://www.zonasul.com.br/";
   private static final String SELLER_FULL_NAME = "Zona Sul";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.HIPER.toString(), Card.AMEX.toString());

   // Site não possui rating

   public RiodejaneiroZonasulCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {

      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h2.hide_mobile", true);
         CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb .hide_mobile a");
         String primaryImage = null;
         List<String> images = doc.select(".bg_branco div.fotorama img").eachAttr("src");
         if (!images.isEmpty()) {
            primaryImage = images.remove(0);
         }
         String description = CrawlerUtils.scrapElementsDescription(doc, Arrays.asList(".div_line:nth-last-child(2)", ".div_line:nth-last-child(1)"));
         boolean available = doc.select(".miolo_info .content-produto-indisponivel").isEmpty();
         Offers offers = available ? scrapOffers(doc) : new Offers();

         Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setOffers(offers)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(images)
            .setDescription(description)
            .build();
         products.add(product);
      } else {
         Logging.printLogDebug(logger, session, "Not a product page: " + session.getOriginalURL());
      }

      return products;
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst("#produto") != null;
   }

   private String crawlInternalId(Document doc) {
      String idText = CrawlerUtils.scrapStringSimpleInfo(doc, ".header_info .code", true);
      return idText != null ? idText.replaceAll("^.*?(?<=:\\s)", "") : null;
   }

   private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(doc);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(SELLER_FULL_NAME)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());

      return offers;

   }

   private Pricing scrapPricing(Document doc) throws MalformedPricingException {

      Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".div_line_priceInfo .price_desconto",
         null, false, ',', session);
      Double priceFrom = null;

      if (spotlightPrice == null) {
         spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".div_line_priceInfo .price",
            null, false, ',', session);
      } else {
         priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".div_line_priceInfo .price",
            null, false, ',', session);
      }

      return PricingBuilder.create()
         .setPriceFrom(priceFrom)
         .setSpotlightPrice(spotlightPrice)
         .setCreditCards(scrapCreditCards(spotlightPrice))
         .setBankSlip(BankSlipBuilder.create().setFinalPrice(spotlightPrice).build())
         .build();

   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = new Installments();

      installments.add(InstallmentBuilder.create()
         .setInstallmentNumber(1)
         .setInstallmentPrice(spotlightPrice)
         .build());

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
            .setBrand(card)
            .setInstallments(installments)
            .setIsShopCard(false).build());
      }

      return creditCards;
   }

}
