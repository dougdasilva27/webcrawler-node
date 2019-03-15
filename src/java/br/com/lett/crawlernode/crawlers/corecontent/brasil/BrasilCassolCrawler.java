package br.com.lett.crawlernode.crawlers.corecontent.brasil;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
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
import br.com.lett.crawlernode.util.MathUtils;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

/**
 * date: 27/03/2018
 * 
 * @author gabriel
 *
 */

public class BrasilCassolCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.centauro.com.br/";

  public BrasilCassolCrawler(Session session) {
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

    JSONObject dataLayer = CrawlerUtils.selectJsonFromHtml(doc, "script", "dataLayer[0]['product'] =", ";", false, true);

    if (dataLayer.has("id")) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = crawlInternalPid(dataLayer);
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".bread-crumb > a:not(:first-child)");
      String description = CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".blocodescricao"));
      String mainProductId = getMainProductId(dataLayer);

      // sku data in json
      JSONArray arraySkus = dataLayer != null && dataLayer.has("variants") ? dataLayer.getJSONArray("variants") : new JSONArray();

      for (int i = 0; i < arraySkus.length(); i++) {
        JSONObject jsonSku = arraySkus.getJSONObject(i);

        String internalId = crawlInternalId(jsonSku);
        String name = crawlName(jsonSku);

        Document productAPI = captureImageAndPricesInfo(internalId, internalPid, mainProductId, doc);

        Float price = CrawlerUtils.scrapSimplePriceFloat(productAPI, "#divPrecoProduto > .product-adjustedPrice:not(.hide)", true);
        boolean available = crawlAvailability(price, jsonSku);
        Prices prices = available ? crawlPrices(productAPI, price) : new Prices();
        String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(productAPI, ".produto_foto #divImagemPrincipalZoom > a", Arrays.asList("href"),
            "https:", "www.cassol.com.br");
        String secondaryImages = crawlSecondaryImages(doc);

        String ean = crawlEan(jsonSku);
        List<String> eans = new ArrayList<>();
        eans.add(ean);

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
            .setMarketplace(new Marketplace()).setEans(eans).build();

        products.add(product);
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************
   * General methods *
   *******************/

  private String getMainProductId(JSONObject json) {
    String mainProductId = null;

    if (json.has("mainVariantId")) {
      mainProductId = json.get("mainVariantId").toString();
    }

    return mainProductId;
  }

  private String crawlInternalId(JSONObject json) {
    String internalId = null;

    if (json.has("id")) {
      internalId = json.get("id").toString();
    }

    return internalId;
  }

  private String crawlInternalPid(JSONObject json) {
    String internalPid = null;

    if (json.has("erpId")) {
      internalPid = json.get("erpId").toString();
    }

    return internalPid;
  }

  private String crawlName(JSONObject skuJson) {
    StringBuilder name = new StringBuilder();

    if (skuJson.has("name")) {
      name.append(skuJson.getString("name"));
    }

    return name.toString();
  }


  private boolean crawlAvailability(Float price, JSONObject jsonSku) {
    return jsonSku.has("available") && jsonSku.get("available") instanceof Boolean && jsonSku.getBoolean("available") && price != null;
  }

  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    Elements images = doc.select("#thumblist li:not(:first-child) > a");
    for (Element e : images) {
      JSONObject rel = CrawlerUtils.stringToJson(e.attr("rel"));

      if (rel.has("largeimage")) {
        secondaryImagesArray.put(rel.get("largeimage"));
      } else if (rel.has("smallimage")) {
        secondaryImagesArray.put(rel.get("smallimage"));
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  /**
   * 
   * Access prices and images api's
   * 
   * @param internalId
   * @param internalPid
   * @param mainProductId
   * @param mainPage
   * @return
   */
  private Document captureImageAndPricesInfo(String internalId, String internalPid, String mainProductId, Document mainPage) {
    Document doc = mainPage;

    if (mainProductId != null && !mainProductId.equals(internalId)) {
      doc = new Document("");
      String imagesUrl =
          "https://www.cassol.com.br/ImagensProduto/CodVariante/" + internalId + "/produto_id/" + internalPid + "/exibicao/produto/t/32";
      String pricesUrl = "https://www.cassol.com.br/ParcelamentoVariante/CodVariante/" + internalId + "/produto_id/" + internalPid + "/t/32";

      doc.append(DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, imagesUrl, null, cookies).toString());
      doc.append(DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, pricesUrl, null, cookies).toString());
    }

    return doc;
  }

  /**
   * 
   * @param internalId
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Element bankPrice = doc.selectFirst(".opcaoboleto .precoproduto .product-adjustedPrice");
      if (bankPrice != null) {
        prices.setBankTicketPrice(MathUtils.parseFloatWithComma(bankPrice.ownText()));
      } else {
        prices.setBankTicketPrice(price);
      }

      prices.setPriceFrom(CrawlerUtils.scrapSimplePriceDouble(doc, "#divPrecoProduto .product-price:not(.hide)", true));

      Map<Integer, Float> mapInstallments = new HashMap<>();
      mapInstallments.put(1, price);

      Elements installments = doc.select(".product-splitPrice-details-info .installments_iten:first-child > ul > li.installments div");
      for (Element e : installments) {
        Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, true);
        mapInstallments.put(pair.getFirst(), pair.getSecond());
      }

      prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
      prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);
      prices.insertCardInstallment(Card.DINERS.toString(), mapInstallments);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), mapInstallments);
      prices.insertCardInstallment(Card.ELO.toString(), mapInstallments);

      Map<Integer, Float> mapInstallmentsShopCard = new HashMap<>();
      Elements installmentsShopCard = doc.select(".product-splitPrice-details-info .installments_iten:last-child > ul > li.installments div");
      for (Element e : installmentsShopCard) {
        Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, true);
        mapInstallmentsShopCard.put(pair.getFirst(), pair.getSecond());
      }

      prices.insertCardInstallment(Card.SHOP_CARD.toString(), mapInstallmentsShopCard);

    }

    return prices;
  }

  private String crawlEan(JSONObject json) {
    String ean = null;

    if (json.has("ean")) {
      Object obj = json.get("ean");

      if (obj != null) {
        ean = obj.toString();
      }
    }

    return ean;
  }
}
