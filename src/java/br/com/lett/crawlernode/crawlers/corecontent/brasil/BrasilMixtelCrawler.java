package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;

public class BrasilMixtelCrawler extends Crawler {

  public BrasilMixtelCrawler(Session session) {
    super(session);
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {

      boolean available = false;
      String internalId = scrapInternalId(doc);
      String name = scrapName(doc);
      String primaryImage = scrapPrimaryImage(doc);
      String secondaryImages = scrapSecondaryImage(doc, primaryImage);
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".textwidget",
          ".widget_custom_html.panel-last-child"));

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setName(name)
          .setAvailable(available).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private String scrapSecondaryImage(Document doc, String primaryImage) {
    Elements secondaryImagesElements = doc.select(".sow-image-container img");
    JSONArray secondaryImages = new JSONArray();

    for (int i = 0; i < secondaryImagesElements.size(); i++) {
      String href = secondaryImagesElements.get(i).attr("src");

      if (!href.equals(primaryImage)) {
        secondaryImages.put(href);

      }
    }

    return secondaryImages.toString();
  }


  private String scrapPrimaryImage(Document doc) {
    String primaryImage = null;
    Element primaryImageElement = doc.selectFirst(".sow-image-container img");

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("src");
    }

    return primaryImage;
  }

  private String scrapName(Document doc) {
    Element nameElement = doc.selectFirst(".product_title");
    String name = null;

    if (nameElement != null) {
      name = nameElement.text();
    }

    return name;
  }


  private String scrapInternalId(Document doc) {
    Element internalIdElement = doc.selectFirst(".product.type-product");
    String internalId = null;

    if (internalIdElement != null) {
      internalId = internalIdElement.attr("id");
      internalId = internalId.replaceAll("[^0-9]", "");
    }

    return internalId;
  }


  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".product_title") != null;
  }

}
