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
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 11/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class SaopauloLeroymerlinCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.leroymerlin.com.br";

  public SaopauloLeroymerlinCrawler(Session session) {
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
      String internalPid = null;
      String name = crawlName(doc);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price, doc);
      boolean available = crawlAvailability(doc);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb-item:not(:first-child) a .name", false);
      List<String> images = crawlImages(doc);
      String primaryImage = images.isEmpty() ? null : images.get(0);
      String secondaryImages = crawlSecondaryImages(images);
      String description = crawlDescription(doc);
      Integer stock = null;

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(new Marketplace()).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-code").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.selectFirst(".product-code");
    if (internalIdElement != null) {
      String text = internalIdElement.text();

      if (text.contains(".")) {
        internalId = CommonMethods.getLast(text.split("\\.")).trim();
      } else if (text.contains("digo")) {
        internalId = CommonMethods.getLast(text.split("digo")).trim();
      } else {
        internalId = text.trim();
      }
    }

    return internalId;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.selectFirst(".product-title");

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;

    Elements salePriceElement = document.select(".product-price-tag .to-price .price-integer, .product-price-tag .to-price .price-decimal");
    if (!salePriceElement.isEmpty()) {
      price = MathUtils.parseFloat(salePriceElement.text());
    }

    return price;
  }

  private List<String> crawlImages(Document doc) {
    List<String> images = new ArrayList<>();

    Element divImages = doc.selectFirst(".product-carousel .carousel[data-items]");
    if (divImages != null) {
      JSONArray imagesArray = new JSONArray(divImages.attr("data-items"));

      for (Object o : imagesArray) {
        JSONObject imageJson = (JSONObject) o;

        if (imageJson.has("shouldLoadImageZoom") && imageJson.getBoolean("shouldLoadImageZoom") && imageJson.has("zoomUrl")
            && !imageJson.get("zoomUrl").toString().trim().isEmpty()) {
          images.add(imageJson.get("zoomUrl").toString());
        } else if (imageJson.has("url") && !imageJson.get("url").toString().trim().isEmpty()) {
          images.add(imageJson.get("url").toString());
        } else if (imageJson.has("thumb") && !imageJson.get("thumb").toString().trim().isEmpty()) {
          images.add(imageJson.get("thumb").toString());
        }
      }
    }

    return images;
  }

  private String crawlSecondaryImages(List<String> images) {
    String secondaryImages = null;

    if (!images.isEmpty()) {
      JSONArray secondaryImagesArray = new JSONArray(images);
      secondaryImagesArray.remove(0);

      if (secondaryImagesArray.length() > 0) {
        secondaryImages = secondaryImagesArray.toString();
      }
    }

    return secondaryImages;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Elements rows = doc.select(".exposicao-produto-info");

    for (Element e : rows) {
      description.append(e.html());
    }


    return description.toString();
  }

  private boolean crawlAvailability(Document doc) {
    return doc.select("button[rel=gravaProdu]").first() != null;
  }

  /**
   * In the time when this crawler was made, this market hasn't installments informations
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);

      Element priceFrom = doc.select("#preco span").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDouble(priceFrom.text()));
      }

      prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }

}
