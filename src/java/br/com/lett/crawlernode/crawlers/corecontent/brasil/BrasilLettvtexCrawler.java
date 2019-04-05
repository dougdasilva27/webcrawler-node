package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class BrasilLettvtexCrawler extends Crawler {

  public BrasilLettvtexCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE_HTTPS = "https://www.lett.digital/";

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE_HTTPS));
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    doc = Jsoup.parse(CommonMethods.readFile("/home/gabriel/htmls/LETTVTEX.html"));

    if (isProductPage(doc)) {
      VTEXCrawlersUtils vtexUtil = new VTEXCrawlersUtils(session, "lett", HOME_PAGE_HTTPS, cookies, dataFetcher);

      JSONObject skuJson = CrawlerUtils.crawlSkuJsonVTEX(doc, session);

      String internalPid = vtexUtil.crawlInternalPid(skuJson);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb li > a");
      String description = crawlDescription(doc);

      // sku data in json
      JSONArray arraySkus = skuJson != null && skuJson.has("skus") ? skuJson.getJSONArray("skus") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = vtexUtil.crawlInternalId(jsonSku);
        String name = vtexUtil.crawlName(jsonSku, skuJson);
        boolean available = false;
        String primaryImage = crawlPrimaryImage(doc);
        String secondaryImages = crawlSecondaryImages(doc);
        Prices prices = new Prices();
        Float price = vtexUtil.crawlMainPagePrice(prices);
        Integer stock = null;

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setStock(stock).setMarketplace(new Marketplace()).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }


  private boolean isProductPage(Document document) {
    return document.select(".productName").first() != null;
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select("#image .image-zoom").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("href").trim();
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".thumbs #botaoZoom");

    for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
      String image = imagesElement.get(i).attr("zoom").trim();

      if (image.isEmpty()) {
        image = imagesElement.get(i).attr("rel").trim();
      }

      secondaryImagesArray.put(image);
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String crawlDescription(Document document) {
    String description = "";

    Element shortDesc = document.select(".product-description").first();

    if (shortDesc != null) {
      description = description + shortDesc.html();
    }

    return description;
  }

}
