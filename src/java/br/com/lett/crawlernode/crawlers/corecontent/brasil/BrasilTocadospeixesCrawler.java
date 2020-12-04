package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer;
import models.Offers;
import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BrasilTocadospeixesCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.tocadospeixes.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "balassa e bonfatti magazine ltda epp";

   public BrasilTocadospeixesCrawler(Session session) {
      super(session);
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      List<Product> products = new ArrayList<>();

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         Elements elements = doc.select(".principal .acoes-produto.disponivel, .principal .acoes-produto.indisponivel");

         for (Element element : elements) {
            String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(element, ".acoes-produto", "data-produto-id");
            String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".codigo-produto span span", false);
            String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".nome-produto.titulo", false);
            CategoryCollection category = CrawlerUtils.crawlCategories(doc, ".breadcrumbs ul");
            String description = CrawlerUtils.scrapSimpleDescription(doc, Collections.singletonList("div #descricao"));
            String primaryImage = scraplPrimaryImage(doc);
            boolean available = crawlAvailability(element);
            Offers offers = available ? scrapOffer(doc, internalId) : new Offers();


            products.add(ProductBuilder.create()
               .setUrl(session.getOriginalURL())
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategories(category)
               .setPrimaryImage(primaryImage)
               .setDescription(description)
               .setOffers(offers)
               .build());

         }
      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }
      return products;
   }

   private String scraplPrimaryImage(Document doc) {

      return CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".conteiner-imagem a", "href");

   }


   private Offers scrapOffer(Document doc, String internalId) throws OfferException, MalformedPricingException {
      Offers offers = new Offers();

      Pricing pricing = scrapPricing(doc, internalId);

      offers.add(Offer.OfferBuilder.create()
         .setUseSlugNameAsInternalSellerId(true)
         .setSellerFullName(MAIN_SELLER_NAME_LOWER)
         .setMainPagePosition(1)
         .setIsBuybox(false)
         .setIsMainRetailer(true)
         .setPricing(pricing)
         .build());


      return offers;
   }

   private CreditCards scrapCreditCard(Document doc, String internalId) throws MalformedPricingException {
      CreditCards card = new CreditCards();

      Installments installments = scrapInstallments(doc, internalId);
      if (!installments.getInstallments().isEmpty()) {
         card.add(CreditCard.CreditCardBuilder.create()
            .setBrand("Mercado pago")
            .setInstallments(installments)
            .setIsShopCard(false)
            .build());
      }

      return card;
   }

   private Installments scrapInstallments(Document doc, String internalId) throws MalformedPricingException {
      Installments installments = new Installments();
      Elements elements = doc.select(".parcelas-produto");
      String attr = "[" + "data-produto-id=" + internalId + "]";
      elements = elements.select(attr);
      elements = elements.select(".accordion-inner .parcela");

      for (Element element : elements) {
         Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, element, false, "x", "", true, ',');


         if (!pair.isAnyValueNull()) {
            installments.add(Installment.InstallmentBuilder.create()
               .setInstallmentNumber(pair.getFirst())
               .setInstallmentPrice(pair.getSecond().doubleValue())
               .build());
         }
      }
      return installments;
   }

   private Pricing scrapPricing(Document doc, String internalId) throws MalformedPricingException {
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".preco-produto", null, false, ',', session);
      CreditCards cards = scrapCreditCard(doc, internalId);
      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setCreditCards(cards)
         .build();
   }

   private boolean crawlAvailability(Element document) {
      Elements elements = document.select(".comprar");
      return !elements.isEmpty();
   }

   private boolean isProductPage(Document doc) {
      return doc.selectFirst(".nome-produto") != null;
   }
}
