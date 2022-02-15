package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.models.Card;
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
import models.pricing.CreditCards;
import models.pricing.Pricing;
import org.jsoup.nodes.Document;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class BrasilDrogaoSuperCrawler extends Crawler {
   protected Set<String> cards = Sets.newHashSet(Card.MASTERCARD.toString(), Card.VISA.toString(),Card.ELO.toString(),Card.DINERS.toString(), Card.AMEX.toString());
   private static final String SELLER_NAME = "Drogão Super";

   public BrasilDrogaoSuperCrawler(Session session) {
      super(session);
   }


   @Override
   public List<Product> extractInformation(Document document) throws Exception {
      super.extractInformation(document);
      List<Product> products = new ArrayList<>();
      if(!isProductPage(document)) {
         Logging.printLogDebug(logger, session, "Not a product page" + session.getOriginalURL());
         return products;
      }
      // Get all product information
      String productName = CrawlerUtils.scrapStringSimpleInfoByAttribute(document,"#NomeProdutoDetalhe", "value");
      String productInternalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(document,"#CodigoProduto", "value");
      String productInternalPid = productInternalId;
      String productDescription = CrawlerUtils.scrapStringSimpleInfo(document,".float-right.produto-nome-desc.is-hidden", false);
      String productPrimaryImage = CrawlerUtils.scrapSimplePrimaryImage(document, ".foto.table-center-element", Arrays.asList("src"), "", "");
      List<String> productSecondaryImages = CrawlerUtils.scrapSecondaryImages(document, "#ListarMultiFotos li a img .foto", Arrays.asList("src"), null, null, productPrimaryImage);

      Product product = ProductBuilder.create()
         .setUrl(session.getOriginalURL())
         .setInternalId(productInternalId)
         .setInternalPid(productInternalPid)
         .setName(productName)
         .setPrimaryImage(productPrimaryImage)
         .setSecondaryImages(productSecondaryImages)
         .setDescription(productDescription)
         .setOffers(scrapOffers(document))
         .build();
      products.add(product);
      return products;
   }
   private Offers scrapOffers(Document document) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      Pricing pricing = scrapPricing(document);
      List<String> sales = Collections.singletonList(CrawlerUtils.calculateSales(pricing));
      offers.add(new Offer.OfferBuilder()
         .setIsBuybox(false)
         .setPricing(pricing)
         .setSellerFullName(SELLER_NAME)
         .setIsMainRetailer(true)
         .setUseSlugNameAsInternalSellerId(true)
         .setSales(sales)
         .build()
      );
      return offers;
   }

   private Pricing scrapPricing(Document document) throws MalformedPricingException {
      Double price = CrawlerUtils.scrapDoublePriceFromHtml(document,".precoPor", null, false, ',', session);
      Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(document, ".precoDe #PrecoProdutoDetalhe", null, false,  ',', session );
      CreditCards creditCards = CrawlerUtils.scrapCreditCards(price, cards);
      return Pricing.PricingBuilder.create()
         .setSpotlightPrice(price)
         .setPriceFrom(priceFrom)
         .setCreditCards(creditCards)
         .build();
   }


   private boolean isProductPage(Document document) {
      return document.selectFirst(".conteudo.msg") == null;
   }
}
