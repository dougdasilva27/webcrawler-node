package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class SaopauloPanvelCrawler extends Crawler {

  private final String HOME_PAGE_HTTP = "http://www.panvel.com/";
  private final String HOME_PAGE_HTTPS = "https://www.panvel.com/";

  public SaopauloPanvelCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE_HTTP) || href.startsWith(HOME_PAGE_HTTPS));
  }



  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      JSONObject chaordic = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.chaordic_meta =", ";", false);
      JSONObject productJson = chaordic.has("product") ? chaordic.getJSONObject("product") : new JSONObject();

      String internalId = crawlInternalId(productJson);
      String internalPid = internalId;
      String name = crawlName(productJson);
      Float price = crawlPrice(productJson);
      Prices prices = crawlPrices(price, productJson);
      boolean available = crawlAvailability(productJson);
      CategoryCollection categories = crawlCategories(productJson);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = crawlMarketplace();

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

  private boolean isProductPage(Document doc) {
    return doc.select(".item-detalhe").first() != null;
  }

  private String crawlInternalId(JSONObject product) {
    String internalId = null;

    if (product.has("id")) {
      internalId = product.get("id").toString();
    }

    return internalId;
  }

  private String crawlName(JSONObject product) {
    String name = null;

    if (product.has("name")) {
      name = product.getString("name");
    }

    return name;
  }

  private Float crawlPrice(JSONObject product) {
    Float price = null;

    if (product.has("price")) {
      String text = product.get("price").toString().replaceAll("[^0-9.]", "");

      if (!text.isEmpty()) {
        price = Float.parseFloat(text);
      }
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject product) {
    return product.has("status") && product.get("status").toString().equalsIgnoreCase("available");
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".slideshow__slides div > img").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("src").trim();
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".slideshow__slides div > img");

    for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
      String image = imagesElement.get(i).attr("src").trim();
      secondaryImagesArray.put(image);
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(JSONObject product) {
    CategoryCollection categories = new CategoryCollection();

    if (product.has("categories")) {
      JSONArray categoriesArray = product.getJSONArray("categories");

      for (int i = 0; i < categoriesArray.length(); i++) {
        JSONObject catJson = categoriesArray.getJSONObject(i);

        if (catJson.has("name")) {
          categories.add(catJson.getString("name"));
        }
      }
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();
    Element desc = document.select(".item-description").first();

    if (desc != null) {
      description.append(desc.html());
    }

    return description.toString();
  }

  private Prices crawlPrices(Float price, JSONObject product) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      if (product.has("old_price")) {
        String text = product.get("old_price").toString().replaceAll("[^0-9.]", "");

        if (!text.isEmpty()) {
          prices.setPriceFrom(Double.parseDouble(text));
        }
      }

      if (product.has("installment")) {
        JSONObject installment = product.getJSONObject("installment");

        if (installment.has("count") && installment.has("price")) {
          String textCount = installment.get("count").toString().replaceAll("[^0-9]", "");
          String textPrice = installment.get("price").toString().replaceAll("[^0-9.]", "");

          if (!textCount.isEmpty() && !textPrice.isEmpty()) {
            installmentPriceMap.put(Integer.parseInt(textCount), Float.parseFloat(textPrice));
          }
        }
      }

      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }

    return prices;
  }
}
