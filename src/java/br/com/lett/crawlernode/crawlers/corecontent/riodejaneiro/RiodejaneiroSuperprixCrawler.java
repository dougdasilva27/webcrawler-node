package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
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
 * Date: 05/06/2017
 *
 * @author Gabriel Dornelas
 *
 */
public class RiodejaneiroSuperprixCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.superprix.com.br/";

  public RiodejaneiroSuperprixCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session,
          "Product page identified: " + this.session.getOriginalURL());

      // Pid
      String internalPid = crawlInternalPid(doc);

      // Categories
      CategoryCollection categories = crawlCategories(doc);

      // Description
      String description = crawlDescription(doc);

      // Primary image
      String primaryImage = crawlPrimaryImage(doc);

      // Secondary images
      String secondaryImages = crawlSecondaryImages(doc);

      // sku data in json
      JSONArray arraySkus = crawlSkuJsonArray(doc);

      // ean data in html
      JSONArray arrayEan = CrawlerUtils.scrapEanFromVTEX(doc);


      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        // Availability
        boolean available = crawlAvailability(jsonSku);

        // InternalId
        String internalId = crawlInternalId(jsonSku);

        // Price
        Float price = crawlMainPagePrice(jsonSku, available);

        // Name
        String name = crawlName(doc, jsonSku);

        // Prices
        Prices prices = crawlPrices(price);

        // Stock
        Integer stock = crawlStock(internalId, available);

        // Ean
        String ean = i < arrayEan.length() ? arrayEan.getString(i) : null;

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
            .setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
            .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
            .setDescription(description).setStock(stock).setMarketplace(new Marketplace()).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
    if (document.select(".productName").first() != null) {
      return true;
    }
    return false;
  }

  /*******************
   * General methods *
   *******************/

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("sku")) {
      internalId = Integer.toString(json.getInt("sku")).trim();
    }

    return internalId;
  }


  private String crawlInternalPid(Document document) {
    String internalPid = null;
    Element internalPidElement = document.select("#___rc-p-id").first();

    if (internalPidElement != null) {
      internalPid = internalPidElement.attr("value").toString().trim();
    }

    return internalPid;
  }

  private String crawlName(Document document, JSONObject jsonSku) {
    String name = null;
    Element nameElement = document.select(".productName").first();

    String nameVariation = jsonSku.getString("skuname");

    if (nameElement != null) {
      name = nameElement.text().toString().trim();

      if (name.length() > nameVariation.length()) {
        name += " " + nameVariation;
      } else {
        name = nameVariation;
      }
    }

    return name;
  }

  private Float crawlMainPagePrice(JSONObject json, boolean available) {
    Float price = null;

    if (json.has("bestPriceFormated") && available) {
      price = Float.parseFloat(json.getString("bestPriceFormated").replaceAll("[^0-9,]+", "")
          .replaceAll("\\.", "").replaceAll(",", "."));
    }

    return price;
  }

  private boolean crawlAvailability(JSONObject json) {
    if (json.has("available")) {
      return json.getBoolean("available");
    }
    return false;
  }

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;

    Element image = doc.select("#botaoZoom").first();

    if (image != null) {
      primaryImage = image.attr("zoom").trim();

      if (primaryImage == null || primaryImage.isEmpty()) {
        primaryImage = image.attr("rel").trim();
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements imageThumbs = doc.select("#botaoZoom");

    for (int i = 1; i < imageThumbs.size(); i++) { // starts with index 1, because the first image
                                                   // is the primary image
      String url = imageThumbs.get(i).attr("zoom");

      if (url == null || url.isEmpty()) {
        url = imageThumbs.get(i).attr("rel");
      }

      if (url != null && !url.isEmpty()) {
        secondaryImagesArray.put(url);
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".bread-crumb > ul li a");

    for (int i = 1; i < elementCategories.size(); i++) { // starting from index 1, because the first
                                                         // is the market name
      categories.add(elementCategories.get(i).text().trim());
    }

    return categories;
  }


  private String crawlDescription(Document doc) {
    StringBuilder str = new StringBuilder();

    Element desc = doc.select(".prod-info .prod-descricao").first();

    if (desc != null) {
      desc.select("h4.Conteudo-da-Pagina-de-Produto").remove();
      str.append(desc.html());
    }

    Elements desc2 = doc.select("#caracteristicas table");

    for (Element e : desc2) {
      e.select("h4.Conteudo-da-Pagina-de-Produto").remove();

      if (!e.select(".Tabela-Nutricional").isEmpty()) {
        str.append(e.html());
        break;
      }
    }

    return str.toString();
  }

  /**
   * There is no bank slip payment method Has no informations of installments
   * 
   * @param internalId
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> mapInstallments = new HashMap<>();
      mapInstallments.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), mapInstallments);
      prices.insertCardInstallment(Card.DINERS.toString(), mapInstallments);
      prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);
    }

    return prices;
  }


  private Integer crawlStock(String internalId, boolean available) {
    if (available) {
      String url = HOME_PAGE + "produto/sku/" + internalId;

      JSONArray jsonArray =
          DataFetcher.fetchJSONArray(DataFetcher.GET_REQUEST, session, url, null, cookies);

      if (jsonArray.length() > 0) {
        JSONObject skuInfo = jsonArray.getJSONObject(0);

        if (skuInfo.has("SkuSellersInformation")) {
          JSONObject sku = skuInfo.getJSONArray("SkuSellersInformation").getJSONObject(0);

          if (sku.has("AvailableQuantity")) {
            return sku.getInt("AvailableQuantity");
          }
        }
      }
    }

    return null;
  }

  /**
   * Get the script having a json with the availability information
   * 
   * @return
   */
  private JSONArray crawlSkuJsonArray(Document document) {
    Elements scriptTags = document.getElementsByTag("script");
    JSONObject skuJson = null;
    JSONArray skuJsonArray = null;

    for (Element tag : scriptTags) {
      for (DataNode node : tag.dataNodes()) {
        if (tag.html().trim().startsWith("var skuJson_0 = ")) {
          skuJson = new JSONObject(node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1]
              + node.getWholeData().split(Pattern.quote("var skuJson_0 = "))[1]
                  .split(Pattern.quote("}]};"))[0]);
        }
      }
    }

    if (skuJson != null && skuJson.has("skus")) {
      skuJsonArray = skuJson.getJSONArray("skus");
    }

    if (skuJsonArray == null) {
      skuJsonArray = new JSONArray();
    }

    return skuJsonArray;
  }
}
