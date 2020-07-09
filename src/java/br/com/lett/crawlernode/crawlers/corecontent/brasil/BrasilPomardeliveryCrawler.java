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
import models.Offer;
import models.Offers;
import models.RatingsReviews;

import models.pricing.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


public class BrasilPomardeliveryCrawler extends Crawler {

  private final String HOME_PAGE = "https://www.pomardelivery.com.br";
  private static final String SELLER_FULL_NAME = "Pomar Delivery";
  protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
        Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

  public BrasilPomardeliveryCrawler(Session session) {
    super(session);
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

    if (doc.selectFirst("#formProduto") != null) {

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, ".acoes_produto .celula.btn_comprar", "data-id");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".tabela.faixa_secao.sitewide .celula span", true);
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".slide img", Arrays.asList("src"), "http://", "www.pomardelivery.com.br");
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb.sitewide li a");
      boolean available = doc.selectFirst(".acoes_produto.celula .celula.btn_comprar") != null;
      Offers offers = available ? scrapOffer(doc) : new Offers();

      // Creating the product
      Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setName(name)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setOffers(offers)
            .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }



  private Offers scrapOffer(Document doc) throws OfferException, MalformedPricingException {
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

    Element salesOneElement = doc.selectFirst(".desconto");
    String firstSales = salesOneElement != null ? salesOneElement.text() : null;

    if (firstSales != null && !firstSales.isEmpty()) {
      sales.add(firstSales);
    }

    return sales;
  }


  private Pricing scrapPricing(Document doc) throws MalformedPricingException {
    Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".precos .preco_de span", null, false, ',', session);
    Double spotlightPrice =
          priceFrom != null ? CrawlerUtils.scrapDoublePriceFromHtml(doc, ".acoes_produto.celula .precos", null, true, ',', session) : CrawlerUtils.scrapDoublePriceFromHtml(doc, ".acoes_produto.celula .precos", null, false, ',', session);
    ;
    CreditCards creditCards = scrapCreditCards(spotlightPrice);
    BankSlip bankSlip = CrawlerUtils.setBankSlipOffers(spotlightPrice, null);

    return Pricing.PricingBuilder.create()
          .setPriceFrom(priceFrom)
          .setSpotlightPrice(spotlightPrice)
          .setCreditCards(creditCards)
          .setBankSlip(bankSlip)
          .build();
  }

  private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
    CreditCards creditCards = new CreditCards();
    if (spotlightPrice != null) {
      Installments installments = new Installments();
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
    }
    return creditCards;
  }

}
