package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 08/08/2017 - Remade date: 11/03/2019
 * 
 * @author Gabriel Dornelas
 *
 */
public class DrogariaMinasbrasilNetCrawler extends Crawler {

  protected String host;

  public DrogariaMinasbrasilNetCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith("https://" + host + "/"));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "input[name=product]", "value");
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-sku > span", true);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product-name  h1", true);
      Float price = crawlPrice(doc, internalId);
      Prices prices = crawlPrices(price, doc);
      boolean available = !doc.select("#productForm .buy-form .-buy").isEmpty();
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs-tree li:not(:first-child) > a");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".product-picture figure a", Arrays.asList("href"), "https", this.host);
      String secondaryImages =
          CrawlerUtils.scrapSimpleSecondaryImages(doc, ".product-picture figure a", Arrays.asList("href"), "https:", this.host, primaryImage);
      String description = crawlDescription(doc);
      String ean = CrawlerUtils.scrapStringSimpleInfo(doc, "[itemprop=gtin13]", true);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setMarketplace(new Marketplace()).setEans(Arrays.asList(ean)).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".product-section") != null;
  }

  private Float crawlPrice(Document document, String internalId) {
    Float price = null;

    String priceText = null;
    Element salePriceElement = document.select("#product-price-" + internalId).first();

    if (salePriceElement != null) {
      priceText = salePriceElement.text();
      price = MathUtils.parseFloatWithComma(priceText);
    }

    return price;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    description.append(CrawlerUtils.scrapElementsDescription(doc,
        Arrays.asList(".product-short-description", ".product-page .product-section:not(:first-child):not(.-related):not(.-send-review)")));

    String apiUrl = null;
    Elements scripts = doc.select("script");
    for (Element e : scripts) {
      String text = e.html().replace(" ", "");

      if (text.contains("functionwriteStandout")) {
        apiUrl = CrawlerUtils.completeUrl(CrawlerUtils.extractSpecificStringFromScript(text, "src=\"", "\"", false), "https", "www.standout.com.br");
        break;
      }
    }

    if (apiUrl != null) {
      Request request = RequestBuilder.create().setUrl(apiUrl).setCookies(cookies).build();
      description.append(this.dataFetcher.get(session, request).getBody());
    }

    return description.toString();
  }

  /**
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      prices.setBankTicketPrice(price);
      prices.setPriceFrom(
          CrawlerUtils.scrapDoublePriceFromHtml(doc, "#productForm .product-price-regular span[id^=old-price-]", null, true, ',', session));

      Elements cards = doc.select("#productForm .installment-options .card-option");

      if (!cards.isEmpty()) {
        for (Element card : cards) {
          Map<Integer, Float> installmentPriceMap = new HashMap<>();

          Elements table = card.select("table tr");

          for (Element e : table) {
            Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, false, "x", "total", true);

            if (!pair.isAnyValueNull()) {
              installmentPriceMap.put(pair.getFirst(), pair.getSecond());
            }
          }

          String cardOficialName = crawlCardName(card);
          if (cardOficialName != null) {
            prices.insertCardInstallment(cardOficialName, installmentPriceMap);
          }
        }
      } else {
        Map<Integer, Float> installmentPriceMap = new HashMap<>();
        installmentPriceMap.put(1, price);

        Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment("#productForm .product-installment", doc, false, "x", "", true);
        if (!pair.isAnyValueNull()) {
          installmentPriceMap.put(pair.getFirst(), pair.getSecond());
        }

        prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
        prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      }
    }

    return prices;
  }

  private String crawlCardName(Element card) {
    String cardOficialName = CommonMethods.getLast(card.attr("class").split(" ")).trim();

    switch (cardOficialName) {
      case "-visa":
        cardOficialName = Card.VISA.toString();
        break;

      case "-master-card":
        cardOficialName = Card.MASTERCARD.toString();
        break;

      case "-american-express":
        cardOficialName = Card.AMEX.toString();
        break;

      case "-diners-club":
        cardOficialName = Card.DINERS.toString();
        break;

      case "-elo":
        cardOficialName = Card.ELO.toString();
        break;

      case "-discover":
        cardOficialName = Card.DISCOVER.toString();
        break;

      case "-hiper":
        cardOficialName = Card.HIPER.toString();
        break;

      case "-hipercard":
        cardOficialName = Card.HIPERCARD.toString();
        break;

      case "-aura":
        cardOficialName = Card.AURA.toString();
        break;

      default:
        cardOficialName = null;
        break;
    }


    return cardOficialName;
  }
}
