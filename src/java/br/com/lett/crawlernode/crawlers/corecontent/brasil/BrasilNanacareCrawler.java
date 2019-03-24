package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
 * Date: 10/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilNanacareCrawler extends Crawler {

  private final String HOME_PAGE = "https://www.nanacare.com.br/";

  public BrasilNanacareCrawler(Session session) {
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
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price, doc);
      Integer stock = crawlStock(doc);
      boolean available = stock != null && stock > 0;
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc, primaryImage);
      String description = crawlDescription(doc);
      Marketplace marketplace = crawlMarketplace();

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    if (doc.select("input[name=id]").first() != null) {
      return true;
    }
    return false;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select("input[name=id]").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    }

    return internalId;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;

    Element internalIdElement = doc.select("span[itemprop=sku]").first();
    if (internalIdElement != null) {
      internalPid = internalIdElement.ownText().trim();
    }

    return internalPid;
  }


  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("h1.prod-title").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;
    Element salePriceElement = document.select(".sale.product_price").first();

    if (salePriceElement != null) {
      price = MathUtils.parseFloatWithComma(salePriceElement.text());
    }

    return price;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element elementPrimaryImage = doc.select(".prod-image img").first();

    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("data-zoom-image");

      if (primaryImage.isEmpty() || !primaryImage.contains("uploads/images")) {
        primaryImage = elementPrimaryImage.attr("src");
      }
    }

    return primaryImage;
  }

  /**
   * In the time when this crawler was made, this market hasn't secondary Images
   * 
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(Document doc, String primaryImage) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select(".prod-image-thumbs a");

    for (Element e : images) {
      String image = e.attr("data-zoom-image");

      if (image.isEmpty() || !image.contains("uploads/images")) {
        image = e.attr("src");
      }

      if (!image.equals(primaryImage)) {
        secondaryImagesArray.put(e.attr("href"));
      }
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
    Elements elementCategories = document.select(".breadcrumb li:not([class=active]) a");

    for (int i = 1; i < elementCategories.size(); i++) { // first page is home
      String cat = elementCategories.get(i).ownText().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element elementShortdescription = doc.select(".prod-excerpt").first();

    if (elementShortdescription != null) {
      description.append(elementShortdescription.html());
    }

    Element elementDescription = doc.select(".prod-description").first();

    if (elementDescription != null) {
      elementDescription.select("#tab-testimonials").remove();
      description.append(elementDescription.html().replace(">Depoimentos</a>", "></a>"));
    }

    return description.toString();
  }

  private Integer crawlStock(Document doc) {
    Integer stock = null;
    Elements scripts = doc.select("script[type=text/javascript]");

    for (Element e : scripts) {
      String script = e.outerHtml().replace("<script type=\"text/javascript\">", "").replaceAll(" ", "");

      if (script.contains("overall_quantity:")) {
        String[] tokens = script.split(",");

        for (String token : tokens) {
          if (token.trim().contains("overall_quantity:")) {
            String stockText = token.split(":")[1].replaceAll("[^0-9]", "").trim();
            stock = stockText.isEmpty() ? 0 : Integer.parseInt(stockText);

            break;
          }
        }

        break;
      }
    }

    return stock;
  }

  /**
   * 
   * Quando esse crawler foi feito, não achei as parcelas
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Element priceFrom = doc.select(".slash.product_price").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
      }

      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
    }

    return prices;
  }

}
