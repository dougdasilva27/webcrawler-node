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
 * date: 29/03/2018
 * 
 * @author gabriel
 *
 */
public class BrasilLivrariaculturaCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.livrariacultura.com.br/";

  public BrasilLivrariaculturaCrawler(Session session) {
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
      String internalPid = internalId;
      String name = crawlName(doc);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price, doc);
      boolean available = crawlAvailability(doc);
      CategoryCollection categories = crawlCategories(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = crawlMarketplace();

      String ean = crawlEan(doc);
      List<String> eans = new ArrayList<>();
      eans.add(ean);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return doc.select("#product-highlights").first() != null;
  }

  private String crawlInternalId(Document document) {
    String internalId = null;

    Elements scripts = document.select("script");

    for (Element e : scripts) {
      String script = e.html().replace(" ", "").toLowerCase();

      if (script.startsWith("varproductid=")) {
        internalId = script.split("=")[1].replace("'", "").replace(";", "").trim();
        break;
      }
    }

    return internalId;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("h1.title").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;

    Element salePriceElement = document.select(".price").first();
    if (salePriceElement != null) {
      price = MathUtils.parseFloatWithComma(salePriceElement.ownText());
    }

    return price;
  }

  private boolean crawlAvailability(Document document) {
    return document.select(".not-sellable-txt").first() == null;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select(".gallery ul li a").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("href").trim();
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select(".gallery ul li a");

    for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
      String image = imagesElement.get(i).attr("href").trim();
      secondaryImagesArray.put(image);
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();

    Elements elementCategories = document.select(".breadcrumb a[title]");
    for (Element e : elementCategories) {
      String cat = e.ownText().trim();

      if (!cat.equalsIgnoreCase("home")) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();
    Element descriptionElement = document.select("#product-description").first();

    if (descriptionElement != null) {
      description.append(descriptionElement.html());
    }

    Element caracElement = document.select("#product-details").first();

    if (caracElement != null) {
      description.append(caracElement.html());
    }

    return description.toString();
  }

  /**
   * There is no bankSlip price.
   * 
   * Some cases has this: 6 x $259.83
   * 
   * Only card that was found in this market was the market's own
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

      prices.setBankTicketPrice(price);

      Element priceFrom = doc.select(".price-original").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.ownText()));
      }

      Element installments = doc.select(".installments").first();

      if (installments != null) {
        String text = installments.ownText().toLowerCase().trim();

        if (text.contains("x")) {
          int x = text.indexOf('x');

          String installment = text.substring(0, x).replaceAll("[^0-9]", "").trim();
          Float value = MathUtils.parseFloatWithComma(text.substring(x).split("sem")[0]);

          if (!installment.isEmpty() && value != null) {
            installmentPriceMap.put(Integer.parseInt(installment), value);
          }
        }
      }

      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }

    return prices;
  }

  private String crawlEan(Document doc) {
    String ean = null;
    Elements elmnts = doc.select("#product-details #product-list-detail .details-column li");

    for (Element e : elmnts) {
      String aux = e.text();

      if (aux.contains("Código de Barras")) {
        aux = aux.replaceAll("[^0-9]+", "");

        if (aux.length() == 13) {
          ean = aux;
        }
      }
    }

    return ean;
  }
}
