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
 * Date: 11/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilNutriiCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.nutrii.com.br/";

  public BrasilNutriiCrawler(Session session) {
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
      boolean available = crawlAvailability(doc);
      CategoryCollection categories = crawlCategories(doc);
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
    if (doc.select("input[name=product]").first() != null) {
      return true;
    }
    return false;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select("input[name=product]").first();
    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    }

    return internalId;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;
    Element pdi = doc.select(".product-info p[itemprop=mpn]").first();

    if (pdi != null) {
      internalPid = pdi.ownText().replaceAll("[^0-9]", "").trim();
    }

    return internalPid;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select(".product-info h1").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;
    Element discountPrice = document.select(".display-preco-com-desconto").first();
    Element salePriceElement = document.select(".price-box .price").first();

    if (discountPrice != null) {
      price = MathCommonsMethods.parseFloat(discountPrice.text());
    } else if (salePriceElement != null) {
      price = MathCommonsMethods.parseFloat(salePriceElement.text());
    }

    return price;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element elementPrimaryImage = doc.select(".product-image >a").first();

    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("href");
    }

    return primaryImage;
  }

  /**
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select(".more-views li a");

    for (int i = 1; i < images.size(); i++) { // primeira imagem é a imagem primaria
      secondaryImagesArray.put(images.get(i).attr("href"));
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
    Elements elementCategories = document.select(".breadcrumbs li[class^=category] a");

    for (int i = 0; i < elementCategories.size(); i++) {
      String cat = elementCategories.get(i).ownText().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element elementDescription = doc.select(".description-new-prod").first();

    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    return description.toString();
  }

  private boolean crawlAvailability(Document doc) {
    return doc.select(".bt-buy-nova").first() != null;
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

      Element bank = doc.select("p[class=fs-14] span.bold").first();
      Element specialPrice = doc.select(".bg-price.ico-boleto").first();

      if (bank != null) {
        Float bankTicket = MathCommonsMethods.parseFloat(bank.ownText());

        if (bankTicket != null) {
          prices.setBankTicketPrice(bankTicket);
        } else {
          prices.setBankTicketPrice(price);
        }
      } else if (specialPrice != null) {
        Float bankTicket = MathCommonsMethods.parseFloat(specialPrice.ownText());

        if (bankTicket != null) {
          prices.setBankTicketPrice(bankTicket);
        } else {
          prices.setBankTicketPrice(price);
        }

      } else {
        prices.setBankTicketPrice(price);
      }

      Element installmentsElement = doc.select(".parcelamento-view-prod-page span.bold").first();

      if (installmentsElement != null) {
        String text = installmentsElement.ownText().toLowerCase().trim();

        if (text.contains("x")) {
          int x = text.indexOf('x');

          String installmentText = text.substring(0, x).replaceAll("[^0-9]", "");
          Float value = MathCommonsMethods.parseFloat(text.substring(x).trim());

          if (!installmentText.isEmpty() && value != null) {
            installmentPriceMap.put(Integer.parseInt(installmentText), value);
          }
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }

}
