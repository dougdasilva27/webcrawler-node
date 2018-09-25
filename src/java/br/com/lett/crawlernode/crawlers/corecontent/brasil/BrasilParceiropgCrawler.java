package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 07/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilParceiropgCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.parceiropg.com.br/";

  public BrasilParceiropgCrawler(Session session) {
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
      String internalPid = crawlInternalPid(doc);
      String name = crawlName(doc);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".items li[class^=item category]");

      JSONArray images = crawlArrayImages(doc);
      String primaryImage = crawlPrimaryImage(images);
      String secondaryImages = crawlSecondaryImages(images);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = crawlMarketplace();

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrices(new Prices()).setAvailable(false).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-info-main").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select(".product.sku .value").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.ownText();
    }

    return internalId;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;

    Element pid = doc.selectFirst("input[name=\"product\"]");
    if (pid != null) {
      internalPid = pid.val();
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("h1.page-title").first();

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private JSONArray crawlArrayImages(Document doc) {
    JSONArray array = new JSONArray();

    JSONObject imagesJson = CrawlerUtils.selectJsonFromHtml(doc, ".product.media script[type=\"text/x-magento-init\"]", null, null, true, true);
    if (imagesJson.has("[data-gallery-role=gallery-placeholder]")) {
      JSONObject firstJson = imagesJson.getJSONObject("[data-gallery-role=gallery-placeholder]");

      if (firstJson.has("mage/gallery/gallery")) {
        JSONObject secondJson = firstJson.getJSONObject("mage/gallery/gallery");

        if (secondJson.has("data")) {
          array = secondJson.getJSONArray("data");
        }
      }
    }

    return array;
  }

  private String crawlPrimaryImage(JSONArray images) {
    String primaryImage = null;

    for (int i = 0; i < images.length(); i++) {
      JSONObject jsonImage = images.getJSONObject(i);

      if (jsonImage.has("isMain") && jsonImage.getBoolean("isMain") && jsonImage.has("full")) {
        primaryImage = jsonImage.getString("full");
        break;
      }
    }

    return primaryImage;
  }

  /**
   * 
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(JSONArray images) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    for (int i = 0; i < images.length(); i++) {
      JSONObject jsonImage = images.getJSONObject(i);

      // jump primary image
      if (jsonImage.has("isMain") && jsonImage.getBoolean("isMain")) {
        continue;
      }

      if (jsonImage.has("full")) {
        secondaryImagesArray.put(jsonImage.getString("full"));
      }

    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Elements rows = doc.select(".value[itemprop=description], .product.data.items");
    for (Element e : rows) {
      description.append(e.html());
    }

    return description.toString();
  }
}
