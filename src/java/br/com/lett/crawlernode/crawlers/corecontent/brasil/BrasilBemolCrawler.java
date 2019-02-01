package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
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
import models.Marketplace;
import models.prices.Prices;


public class BrasilBemolCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.bemol.com.br/";

  public BrasilBemolCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".reference [itemprop=productID]", true);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h2.name", true);
      Float price = CrawlerUtils.scrapSimplePriceFloat(doc, ".buy-box .sale-price", false);
      Prices prices = crawlPrices(price, internalId, doc);
      boolean available = doc.selectFirst(".wd-buy-button  > div:not([style$=none])") != null;
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrum-product li:not(.first) a span");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".wd-product-media-selector .image.selected img",
          Arrays.asList("data-image-large", "data-small", "data-image-big", "src"), "https:", "d3ddx6b2p2pevg.cloudfront.net");

      if (primaryImage.endsWith("/Custom/Content")) {
        primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".wd-product-media-selector .image.selected img",
            Arrays.asList("data-image-big", "data-small", "src"), "https:", "d3ddx6b2p2pevg.cloudfront.net");
      }

      String secondaryImages = scrapSimpleSecondaryImages(doc, ".wd-product-media-selector .image:not(.selected) img",
          Arrays.asList("data-image-large", "data-image-big", "data-small", "src"), "https:", "d3ddx6b2p2pevg.cloudfront.net",
          primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc,
          Arrays.asList(".wrapper-detalhe-produto .descriptions", ".wrapper-detalhe-produto .caracteristicas"));

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select("input[name=ProductID]").isEmpty();
  }

  private static String crawlInternalId(Document doc) {
    String internalId = null;

    Element infoElement = doc.selectFirst("input[name=ProductID]");
    if (infoElement != null) {
      internalId = infoElement.val();
    }

    return internalId;
  }
  
  public static String scrapSimpleSecondaryImages(Document doc, String cssSelector,
	      List<String> attributes, String protocol, String host, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select(cssSelector);
    for (Element e : images) {
      String image = sanitizeUrl(e, attributes, protocol, host);

      if ((primaryImage == null || !primaryImage.equals(image)) && image != null) {
        secondaryImagesArray.put(image);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  public static String sanitizeUrl(Element element, List<String> attributes, String protocol, String host) {
    String sanitizedUrl = null;

    for (String att : attributes) {
      String url = element.attr(att).trim();

      if (!url.isEmpty() && !url.equalsIgnoreCase("https://d8xabijtzlaac.cloudfront.net/Custom/Content")) {
        sanitizedUrl = CrawlerUtils.completeUrl(url, protocol, host);
        break;
      }
    }

    return sanitizedUrl;
  }
  
  /**
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, String internalId, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Document docPrices = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session,
          "https://www.bemol.com.br/widget/product_payment_options?SkuID=" + internalId + "&ProductID=" + internalId
              + "&Template=wd.product.payment.options.result.template&ForceWidgetToRender=true&nocache=1108472214",
          null, cookies);

      Elements cards = docPrices.select(".modal-wd-product-payment-options .grid table");
      for (Element e : cards) {
        Element cardElement = e.selectFirst(".payment-description");
        if (cardElement != null) {
          Map<Integer, Float> installmentPriceMap = new TreeMap<>();
          installmentPriceMap.put(1, price);

          Card card = null;
          String text = cardElement.ownText().toLowerCase();

          if (text.contains("visa")) {
            card = Card.VISA;
          } else if (text.contains("amex")) {
            card = Card.AMEX;
          } else if (text.contains("elo")) {
            card = Card.ELO;
          } else if (text.contains("master")) {
            card = Card.MASTERCARD;
          } else if (text.contains("diners")) {
            card = Card.DINERS;
          } else if (text.contains("bemol")) {
            card = Card.SHOP_CARD;

            Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".cartao-bemol strong", doc, true, "de");

            if (!pair.isAnyValueNull()) {
              installmentPriceMap.put(pair.getFirst(), pair.getSecond());
            }
          }

          if (card != null) {

            Elements installments = e.select("tbody tr td");
            for (Element i : installments) {
              Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, i, false, "de", "juros", true);

              if (!pair.isAnyValueNull()) {
                installmentPriceMap.put(pair.getFirst(), pair.getSecond());
              }
            }

            prices.insertCardInstallment(card.toString(), installmentPriceMap);
          } else if (text.contains("boleto")) {
            Element bank = e.selectFirst("tbody tr td");

            if (bank != null) {
              prices.setBankTicketPrice(MathUtils.parseFloatWithComma(bank.ownText()));
            }
          }
        }
      }
    }

    return prices;
  }
}
