package br.com.lett.crawlernode.crawlers.corecontent.brasil;

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

/**
 * date: 02/04/2019
 * 
 * @author gabriel
 *
 */
public class BrasilGazinCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.gazin.com.br/";

  public BrasilGazinCrawler(Session session) {
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

      JSONObject jsonInfo = CrawlerUtils.selectJsonFromHtml(doc, "script[type=\"application/ld+json\"]", "", null, false, false);

      String internalPid = jsonInfo.has("sku") ? jsonInfo.get("sku").toString() : null;
      String name = jsonInfo.has("name") ? jsonInfo.get("name").toString() : null;
      Float price = crawlPrice(jsonInfo);
      Prices prices = crawlPrices(price, doc);
      CategoryCollection categories = crawlCategories(doc);
      String description = crawlDescription(doc);
      Integer stock = null;
      Marketplace marketplace = crawlMarketplace();

      String internalId = crawlInternalId(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc);
      boolean available = crawlAvailability(doc);

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
    return doc.select("#lblCodProduto").first() != null;
  }

  private String crawlInternalId(Document document) {
    String internalId = null;

    Element id = document.select("#lblCodProduto").first();
    if (id != null) {
      internalId = id.ownText().trim();
    }

    return internalId;
  }

  private Float crawlPrice(JSONObject json) {
    Float price = null;

    if (json.has("offers")) {
      JSONArray prices = json.getJSONArray("offers");
      for (Object offer : prices) {
        if (offer instanceof JSONObject) {
          JSONObject value = (JSONObject) offer;
          if (value.has("@type") && value.getString("@type").equalsIgnoreCase("offer")) {
            price = value.getFloat("price");
          }
        }

      }
    }

    return price;
  }

  private boolean crawlAvailability(Document document) {
    return document.select("#lnkCarrinho").first() != null;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document document) {
    String primaryImage = null;
    Element primaryImageElement = document.select("#divGaleriaProdutos a").first();

    if (primaryImageElement != null) {
      primaryImage = primaryImageElement.attr("data-zoom-image").trim();
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document document) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imagesElement = document.select("#divGaleriaProdutos a");

    for (int i = 1; i < imagesElement.size(); i++) { // first index is the primary image
      String image = imagesElement.get(i).attr("data-zoom-image").trim();
      secondaryImagesArray.put(image);
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();

    Elements elementCategories = document.select(".spBreadcrumbs:not(.bold) a > span");
    for (Element e : elementCategories) {
      String cat = e.ownText().trim();

      if (!cat.contains("inicial")) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private String crawlDescription(Document document) {
    StringBuilder description = new StringBuilder();
    Element shortDescription = document.select("#lbl_DadosTecnicos").first();

    if (shortDescription != null) {
      description.append(shortDescription.html());
    }

    Element desc = document.select("#trDescr").first();

    if (desc != null) {
      description.append(desc.html());
    }

    Element caracElement = document.select("#trModelos").first();

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
      prices.setBankTicketPrice(CrawlerUtils.scrapFloatPriceFromHtml(doc, "#navpa .Menupa .bcaa b", null, true, ','));

      Elements parcels = doc.select("#navpa .Menupa .med > p.p1b");
      Elements parcelsValues = doc.select("#navpa .Menupa .med > p.p2b");

      if (parcels.size() == parcelsValues.size()) {
        for (int i = 0; i < parcels.size(); i++) {
          Integer parcel = CrawlerUtils.scrapIntegerFromHtml(parcels.get(i), null, true, null);
          Float value = CrawlerUtils.scrapFloatPriceFromHtml(parcelsValues.get(i), null, null, true, ',');

          if (parcel != null && value != null) {
            installmentPriceMap.put(parcel, value);
          }
        }
      }

      if (prices.getBankTicketPrice() == null) {
        prices.setBankTicketPrice(price);
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
