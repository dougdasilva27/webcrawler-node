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
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 08/06/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilDrogarianisseiCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.farmaciasnissei.com.br/";

  public BrasilDrogarianisseiCrawler(Session session) {
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
      JSONObject images = crawlArrayImages(doc);
      String primaryImage = crawlPrimaryImage(images);
      String secondaryImages = crawlSecondaryImages(images);
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
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".product-view").isEmpty();
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element internalIdElement = doc.select("input[name=product]").first();

    if (internalIdElement != null) {
      internalId = internalIdElement.val();
    } else {
      internalIdElement = doc.select(".price-box[product-id]").first();

      if (internalIdElement != null) {
        internalId = internalIdElement.attr("data-compra");
      }
    }

    return internalId;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;
    Element pid = doc.select(".value[itemprop=sku]").first();

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
    Element nameElement = document.select("h1.page-title").first();

    if (nameElement != null) {
      name = nameElement.text().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;
    Element salePriceElement = document.select(".price-container span[data-price-type=finalPrice] .price").first();

    if (salePriceElement != null) {
      price = MathUtils.parseFloatWithComma(salePriceElement.text().trim());
    }

    return price;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }

  private JSONObject crawlArrayImages(Document doc) {
    JSONObject imagesJson = new JSONObject();
    JSONArray images = new JSONArray();

    JSONObject scriptJson = CrawlerUtils.selectJsonFromHtml(doc, ".product.media script[type=\"text/x-magento-init\"]", null, null, true, false);

    if (scriptJson.has("[data-gallery-role=gallery-placeholder]")) {
      JSONObject mediaJson = scriptJson.getJSONObject("[data-gallery-role=gallery-placeholder]");

      if (mediaJson.has("mage/gallery/gallery")) {
        JSONObject gallery = mediaJson.getJSONObject("mage/gallery/gallery");

        if (gallery.has("data")) {
          JSONArray arrayImages = gallery.getJSONArray("data");

          for (Object o : arrayImages) {
            JSONObject imageJson = (JSONObject) o;

            String image = null;

            if (imageJson.has("full")) {
              image = imageJson.get("full").toString();
            } else if (imageJson.has("img")) {
              image = imageJson.get("img").toString();
            } else if (imageJson.has("thumb")) {
              image = imageJson.get("thumb").toString();
            }

            if (imageJson.has("isMain") && imageJson.getBoolean("isMain")) {
              imagesJson.put("primary", image);
            } else {
              images.put(image);
            }
          }
        }
      }
    }

    if (images.length() > 0) {
      imagesJson.put("secondary", images);
    }

    return imagesJson;
  }

  private String crawlPrimaryImage(JSONObject images) {
    String primaryImage = null;

    if (images.has("primary")) {
      primaryImage = images.get("primary").toString();
    }

    return primaryImage;
  }

  /**
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(JSONObject images) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    if (images.has("secondary")) {
      secondaryImagesArray = images.getJSONArray("secondary");
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
    Elements elementCategories = document.select(".items .item:not(.home):not(.product)");

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

    Element elementDescription = doc.select(".product-infos").first();

    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    return description.toString();
  }

  private boolean crawlAvailability(Document doc) {
    return doc.select(".stock.available").first() != null;
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

      Element priceFrom = doc.select(".old-price .price").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
      }

      Element installmentsElement = doc.select(".parcelamento").first();

      if (installmentsElement != null) {
        String textInstallment = installmentsElement.ownText().replaceAll("[^0-9]", "");

        if (!textInstallment.isEmpty() && !textInstallment.contains("de")) {
          Integer installment = Integer.parseInt(textInstallment);
          installmentPriceMap.put(installment, MathUtils.normalizeTwoDecimalPlaces(price / installment));
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
