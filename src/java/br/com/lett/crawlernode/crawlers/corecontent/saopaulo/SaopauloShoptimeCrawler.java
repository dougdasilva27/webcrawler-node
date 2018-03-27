package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.SaopauloB2WCrawlersUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

/************************************************************************************************************************************************************************************
 * Crawling notes (30/10/2016):
 * 
 * A b2w etá com uma lógica complexa de bloqueio, para os produtos não ficarem void, quando acesso
 * sua pagina principal e nao consigo as informacoes, entro em uma api que possui os dados basicos
 * de preço nome e marketplaces.
 * 
 * Optimizations notes: No optimizations.
 *
 ************************************************************************************************************************************************************************************/

public class SaopauloShoptimeCrawler extends Crawler {
  private static final String HOME_PAGE = "https://www.shoptime.com.br/";

  private static final String MAIN_SELLER_NAME_LOWER = "shoptime";
  private static final String MAIN_B2W_NAME_LOWER = "b2w";

  public SaopauloShoptimeCrawler(Session session) {
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

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // Json da pagina principal
      JSONObject frontPageJson = SaopauloB2WCrawlersUtils.getDataLayer(doc);

      // Pega só o que interessa do json da api
      JSONObject infoProductJson = SaopauloB2WCrawlersUtils.assembleJsonProductWithNewWay(frontPageJson);

      String internalPid = this.crawlInternalPid(infoProductJson);
      CategoryCollection categories = crawlCategories(doc);
      boolean hasImages = doc.select(".main-area .row > div > span > img:not([src])").first() == null && doc.select(".gallery-product") != null;
      String primaryImage = hasImages ? this.crawlPrimaryImage(infoProductJson) : null;
      String secondaryImages = hasImages ? this.crawlSecondaryImages(infoProductJson) : null;
      String description = this.crawlDescription(internalPid, doc);
      Map<String, String> skuOptions = this.crawlSkuOptions(infoProductJson);

      for (Entry<String, String> entry : skuOptions.entrySet()) {
        String internalId = entry.getKey();

        String name = this.crawlMainPageName(infoProductJson);
        String variationName = entry.getValue().trim();

        if (name != null && !name.toLowerCase().contains(variationName.toLowerCase())) {
          name += " " + variationName;
        }

        Map<String, Prices> marketplaceMap = this.crawlMarketplace(infoProductJson, internalId);
        Marketplace variationMarketplace = this.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = this.crawlAvailability(marketplaceMap);
        Float variationPrice = this.crawlPrice(marketplaceMap);
        Prices prices = crawlPrices(marketplaceMap);
        Integer stock = null; // stock só tem na api

        // Creating the product
        Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
            .setPrice(variationPrice).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages).setDescription(description).setStock(stock).setMarketplace(variationMarketplace).build();

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

  private boolean isProductPage(String url) {

    if (url.startsWith("https://www.shoptime.com.br/produto/") || url.startsWith("http://www.shoptime.com.br/produto/")) {
      return true;
    }
    return false;
  }

  private String crawlInternalPid(JSONObject assembleJsonProduct) {
    String internalPid = null;

    if (assembleJsonProduct.has("internalPid")) {
      internalPid = assembleJsonProduct.getString("internalPid").trim();
    }

    return internalPid;
  }

  private Map<String, String> crawlSkuOptions(JSONObject infoProductJson) {
    Map<String, String> skuMap = new HashMap<>();

    if (infoProductJson.has("skus")) {
      JSONArray skus = infoProductJson.getJSONArray("skus");

      for (int i = 0; i < skus.length(); i++) {
        JSONObject sku = skus.getJSONObject(i);

        if (sku.has("internalId")) {
          String internalId = sku.getString("internalId");
          String name = "";

          if (sku.has("variationName")) {
            name = sku.getString("variationName");
          }

          skuMap.put(internalId, name);
        }
      }
    }

    return skuMap;
  }

  private Map<String, Prices> crawlMarketplace(JSONObject skus, String internalId) {
    Map<String, Prices> marketplaces = new HashMap<>();

    if (skus.has("prices")) {
      JSONObject pricesJson = skus.getJSONObject("prices");

      if (pricesJson.has(internalId)) {
        JSONArray marketArrays = pricesJson.getJSONArray(internalId);

        for (int i = 0; i < marketArrays.length(); i++) {
          JSONObject seller = marketArrays.getJSONObject(i);

          if (seller.has("sellerName")) {
            String sellerName = seller.getString("sellerName");
            Prices prices = crawlMarketplacePrices(seller);

            marketplaces.put(sellerName, prices);
          }
        }
      }
    }

    return marketplaces;
  }

  private Prices crawlMarketplacePrices(JSONObject seller) {
    Prices prices = new Prices();

    if (seller.has("bankTicket")) {
      prices.setBankTicketPrice(seller.getDouble("bankTicket"));
    }

    if (seller.has("installments")) {
      Map<Integer, Float> installmentMapPrice = new HashMap<>();

      JSONArray installments = seller.getJSONArray("installments");

      for (int i = 0; i < installments.length(); i++) {
        JSONObject installment = installments.getJSONObject(i);

        if (installment.has("quantity") && installment.has("value")) {
          Integer quantity = installment.getInt("quantity");
          Double value = installment.getDouble("value");

          installmentMapPrice.put(quantity, MathUtils.normalizeTwoDecimalPlaces(value.floatValue()));
        }
      }

      // Isso acontece quando o seller principal não é a b2w, com isso não aparecem as parcelas
      // Na maioria dos casos a primeira parcela tem desconto e as demais não
      // O preço default seria o preço sem desconto.
      // Para pegar esse preço, dividimos ele por 2 e adicionamos nas parcelas como 2x esse preço
      if (installments.length() == 1 && seller.has("defaultPrice")) {
        Double priceD = seller.getDouble("defaultPrice") / 2d;
        installmentMapPrice.put(2, MathUtils.normalizeTwoDecimalPlaces(priceD.floatValue()));
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.AURA.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.HIPER.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentMapPrice);
    }

    return prices;
  }

  /*******************
   * General methods *
   *******************/

  private Float crawlPrice(Map<String, Prices> marketplaces) {
    Float price = null;

    Prices prices = null;

    if (marketplaces.containsKey(MAIN_SELLER_NAME_LOWER)) {
      prices = marketplaces.get(MAIN_SELLER_NAME_LOWER);
    } else if (marketplaces.containsKey(MAIN_B2W_NAME_LOWER)) {
      prices = marketplaces.get(MAIN_B2W_NAME_LOWER);
    }

    if (prices != null && prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
      Double priceDouble = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
      price = priceDouble.floatValue();
    }

    return price;
  }

  private boolean crawlAvailability(Map<String, Prices> marketplaces) {
    boolean available = false;

    for (String seller : marketplaces.keySet()) {
      if (seller.equalsIgnoreCase(MAIN_SELLER_NAME_LOWER) || seller.equalsIgnoreCase(MAIN_B2W_NAME_LOWER)) {
        available = true;
        break;
      }
    }

    return available;
  }

  private String crawlPrimaryImage(JSONObject infoProductJson) {
    String primaryImage = null;

    if (infoProductJson.has("images")) {
      JSONObject images = infoProductJson.getJSONObject("images");

      if (images.has("primaryImage")) {
        primaryImage = images.getString("primaryImage");
      }
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(JSONObject infoProductJson) {
    String secondaryImages = null;

    JSONArray secondaryImagesArray = new JSONArray();

    if (infoProductJson.has("images")) {
      JSONObject images = infoProductJson.getJSONObject("images");

      if (images.has("secondaryImages")) {
        secondaryImagesArray = images.getJSONArray("secondaryImages");
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String crawlMainPageName(JSONObject json) {
    String name = null;

    if (json.has("name")) {
      name = json.getString("name");
    }

    return name;
  }

  /**
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".breadcrumb > li > a");

    for (int i = 1; i < elementCategories.size(); i++) { // primeiro item é a home
      String cat = elementCategories.get(i).attr("name").trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap) {
    Marketplace marketplace = new Marketplace();

    for (String sellerName : marketplaceMap.keySet()) {
      if (!sellerName.equalsIgnoreCase(MAIN_SELLER_NAME_LOWER) && !sellerName.equalsIgnoreCase(MAIN_B2W_NAME_LOWER)) {
        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", sellerName);

        Prices prices = marketplaceMap.get(sellerName);

        if (prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
          // Pegando o preço de uma vez no cartão
          Double price = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
          Float priceFloat = price.floatValue();

          sellerJSON.put("price", priceFloat); // preço de boleto é o mesmo de preço uma vez.
        }
        sellerJSON.put("prices", marketplaceMap.get(sellerName).toJSON());

        try {
          Seller seller = new Seller(sellerJSON);
          marketplace.add(seller);
        } catch (Exception e) {
          Logging.printLogError(logger, session, Util.getStackTraceString(e));
        }
      }
    }

    return marketplace;
  }

  private String crawlDescription(String internalPid, Document doc) {
    String description = "";

    if (internalPid != null) {
      // String url = HOME_PAGE + "product-description/shop/" + internalPid;
      // Document docDescription = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null,
      // cookies);
      // if(docDescription != null){
      // description = description + docDescription.html();
      // }

      Element desc2 = doc.select(".info-description-frame-inside").first();

      if (desc2 != null) {
        String urlDesc2 = HOME_PAGE + "product-description/acom/" + internalPid;
        Document docDescriptionFrame = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, urlDesc2, null, cookies);
        if (docDescriptionFrame != null) {
          description = description + docDescriptionFrame.html();
        }
      }

      Element elementProductDetails = doc.select(".info-section").last();
      if (elementProductDetails != null) {
        elementProductDetails.select(".info-section-header.hidden-md.hidden-lg").remove();
        description = description + elementProductDetails.html();
      }
    }

    return description;
  }
  //
  // private Integer crawlStock(String internalId, JSONObject jsonProduct){
  // Integer stock = null;
  //
  // if(jsonProduct.has("prices")){
  // if(jsonProduct.getJSONObject("prices").has(internalId)){
  // JSONArray offers = jsonProduct.getJSONObject("prices").getJSONArray(internalId);
  //
  // for(int i = 0; i < offers.length(); i++) {
  // JSONObject seller = offers.getJSONObject(i);
  //
  // if(seller.has("sellerName") && seller.has("stock")) {
  // String sellerName = seller.getString("sellerName");
  //
  // if(sellerName.equalsIgnoreCase(MAIN_SELLER_NAME_LOWER) ||
  // sellerName.equalsIgnoreCase(MAIN_B2W_NAME_LOWER)) {
  // stock = seller.getInt("stock");
  // break;
  // }
  // }
  // }
  // }
  // }
  //
  // return stock;
  // }

  private Prices crawlPrices(Map<String, Prices> marketplaceMap) {
    Prices prices = new Prices();

    if (marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER)) {
      prices = marketplaceMap.get(MAIN_SELLER_NAME_LOWER);
    } else if (marketplaceMap.containsKey(MAIN_B2W_NAME_LOWER)) {
      prices = marketplaceMap.get(MAIN_B2W_NAME_LOWER);
    }

    return prices;
  }
}
