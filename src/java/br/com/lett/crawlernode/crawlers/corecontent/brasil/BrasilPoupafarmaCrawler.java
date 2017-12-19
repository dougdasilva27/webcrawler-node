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
import br.com.lett.crawlernode.util.MathCommonsMethods;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 21/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilPoupafarmaCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.poupafarma.com.br/";

  public BrasilPoupafarmaCrawler(Session session) {
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
      Logging.printLogDebug(logger, session,
          "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = crawlInternalPid(doc);
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

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
          .setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
          .setDescription(description).setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    if (doc.select(".prod-index").first() != null) {
      return true;
    }
    return false;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select("input[name=Id_Produto_SKU]").first();

    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    } else {
      internalIdElement = doc.select(".button-comprar[data-compra]").first();

      if (internalIdElement != null) {
        internalId = internalIdElement.attr("data-compra");
      }
    }

    return internalId;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;
    Element pid = doc.select(".prod-index h4 small[class]").first();

    if (pid != null) {
      String text = pid.ownText().replaceAll("[^0-9]", "").trim();

      if (!text.isEmpty()) {
        internalPid = text;
      }
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(".prod-index h2").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;
    Element salePriceElement = document.select("#side-prod .preco-por strong").first();

    if (salePriceElement != null) {
      price = MathCommonsMethods.parseFloat(salePriceElement.text().trim());
    }

    return price;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element elementPrimaryImage = doc.select(".zoom-img-index > img").first();

    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("data-zoom-image").trim();

      if (primaryImage.isEmpty()) {
        primaryImage = elementPrimaryImage.attr("src").trim();
      }

      if (!primaryImage.startsWith(HOME_PAGE)) {
        primaryImage = HOME_PAGE + primaryImage;
      }
    }

    return primaryImage;
  }

  /**
   * Quando este crawler foi feito, nao tinha imagens secundarias
   * 
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select("#gallery li > a");

    for (int i = 1; i < images.size(); i++) {
      Element e = images.get(i);

      String image = e.attr("data-zoom-image").trim();

      if (image.isEmpty()) {
        image = e.attr("data-image").trim();
      }

      if (!image.startsWith(HOME_PAGE)) {
        image = HOME_PAGE + image;
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
    Elements elementCategories =
        document.select(".breadcrumbs li.show-for-large-up:not(.current) a");

    for (Element e : elementCategories) {
      String cat = e.ownText().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element elementDescription = doc.select("#corpo-detalhe .prod-descr .descricao").first();

    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    Element info = doc.select(".prod-descr .prod-content-left").first();

    if (info != null) {
      description.append(info.html());
    }


    return description.toString();
  }

  private boolean crawlAvailability(Document doc) {
    return doc.select(".button-comprar").first() != null;
  }

  /**
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

      Elements installmentsElement = doc.select("#formas-de-pagamento .show-for-medium-up li");

      for (Element e : installmentsElement) {
        Element parcelElement = e.select(".left-content strong").first();
        Element valueElement = e.select(".right-content").first();

        if (parcelElement != null && valueElement != null) {
          String parcel = parcelElement.ownText().replaceAll("[^0-9]", "").trim();
          Float value = MathCommonsMethods.parseFloat(valueElement.ownText());

          if (!parcel.isEmpty() && value != null) {
            installmentPriceMap.put(Integer.parseInt(parcel), value);
          }
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }

    return prices;
  }

}
