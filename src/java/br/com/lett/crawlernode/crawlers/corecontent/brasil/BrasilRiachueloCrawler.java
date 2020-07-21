package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
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
import models.pricing.BankSlip;
import models.pricing.BankSlip.BankSlipBuilder;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;

public class BrasilRiachueloCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.riachuelo.com.br/";
  private static final String SELLER_FULL_NAME = "Riachuelo";
  protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
      Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());


  public BrasilRiachueloCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  protected Document fetch() {
    return Jsoup.parse(fetchPage(session.getOriginalURL(), session));
  }

  public String fetchPage(String url, Session session) {
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "*/*");
    headers.put("accept-encoding", "no");
    headers.put("connection", "keep-alive");
    Request request = RequestBuilder.create()
        .setUrl(url)
        .setIgnoreStatusCode(false)
        .mustSendContentEncoding(false)
        .setHeaders(headers)
        .setFetcheroptions(
            FetcherOptionsBuilder.create()
                .mustUseMovingAverage(false)
                .mustRetrieveStatistics(true)
                .setForbiddenCssSelector("#px-captcha")
                .build()
        ).setProxyservice(
            Arrays.asList(
                ProxyCollection.INFATICA_RESIDENTIAL_BR,
                ProxyCollection.STORM_RESIDENTIAL_US,
                ProxyCollection.NETNUT_RESIDENTIAL_BR,
                ProxyCollection.NO_PROXY
            )
        ).build();

    return this.dataFetcher.get(session, request).getBody();
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "form[data-product-sku]", "data-product-sku");
      String internalId = internalPid;
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".page-title", false);
      JSONArray imagesArray = CrawlerUtils.crawlArrayImagesFromScriptMagento(doc);
      String primaryImage = CrawlerUtils.scrapPrimaryImageMagento(imagesArray);
      String secondaryImages = CrawlerUtils.scrapSecondaryImagesMagento(imagesArray, primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList("#jq-product-info-accordion"));
      boolean availableToBuy = doc.select(".product-info-main .out-of-stock").isEmpty();

      Offers offers = availableToBuy ? scrapOffers(doc) : new Offers();

      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setOffers(offers)
          .build();

      Elements variations = doc.select("#product-options-wrapper a[option-id]");
      if (!variations.isEmpty()) {
        for (Element e : variations) {
          Product clone = product.clone();
          String variationInternalId = clone.getInternalId() + "-" + e.attr("option-id");
          String variationName = clone.getName() + " " + e.attr("option-label");

          if (e.hasAttr("disabled")) {
            clone.setOffers(new Offers());
          }

          clone.setName(variationName);
          clone.setInternalId(variationInternalId);

          products.add(product);
        }
      } else {
        products.add(product);
      }


    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }


  private boolean isProductPage(Document doc) {
    return doc.selectFirst("form[data-product-sku]") != null;
  }


  private Offers scrapOffers(Document doc) throws MalformedPricingException, OfferException {
    Offers offers = new Offers();
    Pricing pricing = scrapPricing(doc);
    List<String> sales = new ArrayList<>();

    offers.add(OfferBuilder.create()
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

  private Pricing scrapPricing(Document doc) throws MalformedPricingException {
    Double priceFrom = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-main span.old-price .price", null, true, ',', session);
    Double spotlightPrice = CrawlerUtils.scrapDoublePriceFromHtml(doc, ".product-info-main span[data-price-type=finalPrice] .price", null, true, ',', session);
    CreditCards creditCards = scrapCreditCards(doc, spotlightPrice);
    BankSlip bankSlip = BankSlipBuilder.create()
        .setFinalPrice(spotlightPrice)
        .build();

    return PricingBuilder.create()
        .setPriceFrom(priceFrom)
        .setSpotlightPrice(spotlightPrice)
        .setCreditCards(creditCards)
        .setBankSlip(bankSlip)
        .build();

  }

  private CreditCards scrapCreditCards(Document doc, Double spotlightPrice) throws MalformedPricingException {
    CreditCards creditCards = new CreditCards();

    Installments installments = new Installments();
    Installments installmentsShopCard = new Installments();

    if (doc.selectFirst(".product-info-main .installement-info p") != null) {
      scrapInstallments(doc, false, installments);
      scrapInstallments(doc, true, installmentsShopCard);

      creditCards.add(CreditCardBuilder.create()
          .setBrand(Card.SHOP_CARD.toString())
          .setInstallments(installmentsShopCard)
          .setIsShopCard(true)
          .build());
    }

    installments.add(InstallmentBuilder.create()
        .setInstallmentNumber(1)
        .setInstallmentPrice(spotlightPrice)
        .build());

    for (String card : cards) {
      creditCards.add(CreditCardBuilder.create()
          .setBrand(card)
          .setInstallments(installments)
          .setIsShopCard(false)
          .build());
    }

    installments.add(InstallmentBuilder.create()
        .setInstallmentNumber(1)
        .setInstallmentPrice(spotlightPrice)
        .build());

    return creditCards;
  }

  public void scrapInstallments(Document doc, boolean storeCard, Installments installments) throws MalformedPricingException {
    String tagPosition = storeCard ? ":last-child" : "first-child";

    Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".product-info-main .installement-info p" + tagPosition, doc, true);
    if (!pair.isAnyValueNull()) {
      installments.add(InstallmentBuilder.create()
          .setInstallmentNumber(pair.getFirst())
          .setInstallmentPrice(MathUtils.normalizeTwoDecimalPlaces(pair.getSecond().doubleValue()))
          .build());
    }
  }

}
