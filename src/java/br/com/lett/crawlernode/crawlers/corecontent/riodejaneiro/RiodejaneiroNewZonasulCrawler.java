package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;


/**
 * Date: 20/08/2018
 * 
 * @author victor
 *
 */
public class RiodejaneiroNewZonasulCrawler {

  public static final String HOME_PAGE = "https://convidado.zonasul.com.br/";


  public List<Product> extractInformation(Document doc, Session session, Logger logger) {
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String name = crawlName(doc);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb .hide_mobile a", false);
      String primaryImage = crawlPrimaryImage(doc);
      // NO SECONDARY IMAGES
      String description = crawlDescription(doc);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(doc, price);
      boolean available = price != null;

      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setDescription(description).setMarketplace(new Marketplace())
          .build();

      products.add(product);
    } else {
      Logging.printLogDebug(logger, session, "Not a product page: " + session.getOriginalURL());
    }

    return products;
  }

  /**
   * Checks if the page acessed is product page or not.
   * 
   * @param doc - contais html from the page to be scrapped
   * @return true if its a skupage.
   */
  private boolean isProductPage(Document doc) {
    return doc.selectFirst("body[id=produto]") != null;
  }

  /**
   * 
   * @param doc - contais html from the page to be scrapped
   * @return the extracted pid from the URL.
   */
  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element metaProducts = doc.selectFirst(".header_info .code");
    if (metaProducts != null) {
      internalId = CommonMethods.getLast(metaProducts.ownText().split(":")).trim();
    }

    return internalId;
  }

  /**
   * Gets the sku Name
   * 
   * @param doc - contais html from the page to be scrapped
   * @return the sku Name
   */
  private String crawlName(Document doc) {
    String name = null;
    Element nameElement = doc.selectFirst("h2.hide_mobile");

    if (nameElement != null) {
      name = nameElement.text();
    }
    return name;
  }

  /**
   * Get the primary image from the sku.
   * 
   * @param doc - the html data to be scrapped
   * @return - the primary sku image
   */
  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element primaryImageElement = doc.selectFirst(".bg_branco div.fotorama img");

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("src");
    }
    if (primaryImage != null && !primaryImage.startsWith("http")) {
      primaryImage = "https:" + primaryImage;
    }

    return primaryImage;
  }

  /**
   * Gets the description of the SKU
   * 
   * @param doc - html to be scrapped
   * @return a html formated into a string
   */
  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element descHead = doc.selectFirst(".div_line:eq(2)");

    if (descHead != null) {
      description.append(descHead.text());
    }

    Element descBody = doc.selectFirst(".div_line:eq(3)");

    if (descBody != null) {
      description.append(descBody.outerHtml());
    }

    return description.toString();
  }

  /**
   * Gets the SKU prices
   * 
   * @param doc - html to be scrapped
   * @return the price scrapped from the sku page
   */
  private Float crawlPrice(Document doc) {
    Float price = null;

    Element priceElement = doc.selectFirst(".content_price > div.price_desconto");

    if (priceElement != null) {
      price = MathUtils.parseFloatWithComma(priceElement.ownText());
    } else {
      Element normalPrice = doc.selectFirst(".content_price > div.price");

      if (normalPrice != null) {
        price = MathUtils.parseFloatWithComma(normalPrice.ownText());
      }
    }

    return price;
  }

  /**
   * Get the SKU Old price if it exists
   * 
   * @param doc - html to be scrapped
   * @return
   */
  private Double crawlOldPrice(Document doc) {
    Double price = null;

    Element priceDiv = doc.selectFirst(".oferta .content_price > div.price");
    if (priceDiv != null) {
      price = MathUtils.parseDoubleWithComma(priceDiv.ownText());
    }

    return price;
  }

  /**
   * Create a map of Prices and payment way
   * 
   * @param doc - html to be scrapped
   * @param price - price of the SKU
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> paymentPriceMap = new TreeMap<>();

      paymentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);
      prices.setPriceFrom(crawlOldPrice(doc));

      prices.insertCardInstallment(Card.MASTERCARD.toString(), paymentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), paymentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), paymentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), paymentPriceMap);

    }
    return prices;
  }

}
