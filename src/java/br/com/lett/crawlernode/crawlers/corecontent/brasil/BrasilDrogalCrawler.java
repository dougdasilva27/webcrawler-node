package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * 
 * @author gabriel date: 2018-05-25
 */
public class BrasilDrogalCrawler extends Crawler {

  public BrasilDrogalCrawler(Session session) {
    super(session);
  }

  private static final String HOME_PAGE = "https://www.drogal.com.br/";

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && href.startsWith(HOME_PAGE);
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = null;
      Integer stock = null;
      Marketplace marketplace = new Marketplace();
      String name = crawlName(doc);
      Float price = crawlMainPagePrice(doc);
      boolean available = price != null;
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
      Prices prices = crawlPrices(price, doc);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }



  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document doc) {
    return doc.select(".code span[itemprop=sku]").first() != null;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(Document document) {
    String internalId = null;

    Element id = document.select(".code span[itemprop=sku]").first();
    if (id != null) {
      internalId = id.ownText().replaceAll("[^0-9]", "");
    }

    return internalId;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("h1.name").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlMainPagePrice(Document document) {
    Float price = null;
    Element mainPagePriceElement = document.select(".sale_price").first();

    if (mainPagePriceElement != null) {
      price = MathUtils.parseFloat(mainPagePriceElement.text());
    }

    return price;
  }

  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".big-image a").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("href");

      if (!primaryImage.startsWith("http")) {
        primaryImage = "https:" + primaryImage;
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = document.select("#sly_carousel li:not(.active) a");
    for (Element e : images) {
      String image = e.attr("big_img");

      if (!image.startsWith("http")) {
        image = "https:" + image;
      }

      secondaryImagesArray.put(image);
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  /**
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select("#breadcrumb li:not(.home) > a");

    for (Element e : elementCategories) {
      String cat = e.text().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();
    Element descriptionElement = document.select(".container .float > .center:not(.product) > .row").first();

    if (descriptionElement != null) {
      description.append(descriptionElement.html());
    }

    Element modal = document.select(".modal-content").last();
    if (modal != null) {
      description.append(modal.html());
    }

    return description.toString();
  }

  /**
   * 
   * @param price
   * @param doc
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      Element priceFrom = doc.select(".unit_price").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDouble(priceFrom.ownText()));
      }

      Elements installments = doc.select(".get_installments > span strong");

      if (installments.size() > 1) {
        String number = installments.get(0).ownText().replaceAll("[^0-9]", "").trim();
        Float value = MathUtils.parseFloat(installments.get(1).ownText());

        if (!number.isEmpty() && value != null) {
          installmentPriceMap.put(Integer.parseInt(number), value);
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);

    }

    return prices;
  }
}
