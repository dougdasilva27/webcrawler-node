package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 15/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilPichauCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.pichau.com.br/";

  public BrasilPichauCrawler(Session session) {
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

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = CrawlerUtils.scrapStringSimpleInfo(doc, ".sku .value", true);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".product.title h1", true);
      Float price = CrawlerUtils.scrapSimplePriceFloat(doc, "#product-price-" + internalId, false);
      Prices prices = crawlPrices(doc, price);
      boolean available = !doc.select(".stock.available").isEmpty();
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumbs .item:not(.home):not(.product) a");
      JSONArray images = CrawlerUtils.crawlArrayImagesFromScriptMagento(doc);
      String primaryImage = crawlPrimaryImage(images, doc);
      String secondaryImages = crawlSecondaryImages(images);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".form-pichau-product .product.info")).replace("hidemobile", "");

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
    return !doc.select("input[name=product]").isEmpty();
  }

  private static String crawlInternalId(Document doc) {
    String internalId = null;

    Element infoElement = doc.selectFirst("input[name=product]");
    if (infoElement != null) {
      internalId = infoElement.val();
    }

    return internalId;
  }

  private String crawlPrimaryImage(JSONArray images, Document doc) {
    Element metaImage = doc.selectFirst("meta[property=\"og:image\"]");
    String token = null;
    String primaryImage = null;

    if (metaImage != null) {
      String attr = metaImage.attr("content");
      token = attr.substring(attr.lastIndexOf("/"), attr.length());
    }
    for (Object object : images) {
      String image = (String) object;

      if (image.endsWith(token)) {
        primaryImage = image;
      }
    }

    return primaryImage;
  }

  /**
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(JSONArray images) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (images.length() > 1) {
      images.remove(0);
      secondaryImagesArray = images;
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  /**
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    Map<Integer, Float> installmentPriceMap = new TreeMap<>();
    if (price != null) {
      installmentPriceMap.put(1, price);

      Float bankTicket = CrawlerUtils.scrapSimplePriceFloat(doc, ".price-boleto > span", true);
      if (bankTicket != null) {
        prices.setBankTicketPrice(bankTicket);
      } else {
        prices.setBankTicketPrice(price);
      }

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".price-installments > span", doc, true, "x");
      if (!pair.isAnyValueNull()) {
        installmentPriceMap.put(pair.getFirst(), pair.getSecond());
      }

      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }

}
