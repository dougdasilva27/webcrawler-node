package br.com.lett.crawlernode.crawlers.corecontent.extractionutils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import com.google.common.net.HttpHeaders;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
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
import enums.OfferField;
import exceptions.OfferException;
import models.Marketplace;
import models.Offer;
import models.Offers;
import models.Seller;
import models.Util;
import models.prices.Prices;

public class B2WCrawler extends Crawler {
  protected Map<String, String> headers = new HashMap<>();
  private static final String MAIN_B2W_NAME_LOWER = "b2w";
  private static final Card DEFAULT_CARD = Card.VISA;
  protected String sellerNameLower;
  protected List<String> subSellers;
  protected String homePage;

  public B2WCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.FETCHER);
    this.setHeaders();
  }

  protected void setHeaders() {
    headers.put(HttpHeaders.REFERER, this.homePage);
    headers.put(
        HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3"
    );
    headers.put(HttpHeaders.CACHE_CONTROL, "max-age=0");
    headers.put(HttpHeaders.CONNECTION, "keep-alive");
    headers.put(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");
    headers.put(HttpHeaders.ACCEPT_ENCODING, "no");
    headers.put("Upgrade-Insecure-Requests", "1");
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(homePage));
  }

  @Override
  protected Document fetch() {
    return Jsoup.parse(fetchPage(session.getOriginalURL(), session));
  }

  public String fetchPage(String url, Session session) {
    Request request = RequestBuilder.create()
        .setUrl(url)
        .setCookies(this.cookies)
        .setHeaders(this.headers)
        .mustSendContentEncoding(false)
        .setFetcheroptions(
            FetcherOptionsBuilder.create()
                .mustUseMovingAverage(false)
                .mustRetrieveStatistics(true)
                .setForbiddenCssSelector("#px-captcha")
                .build()
        ).setProxyservice(
            Arrays.asList(
                ProxyCollection.STORM_RESIDENTIAL_EU,
                ProxyCollection.INFATICA_RESIDENTIAL_BR,
                ProxyCollection.STORM_RESIDENTIAL_US,
                ProxyCollection.BUY
            )
        ).build();

    String content = this.dataFetcher.get(session, request).getBody();

    if (content == null || content.isEmpty()) {
      content = new ApacheDataFetcher().get(session, request).getBody();
    }

    return content;
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    List<Product> products = new ArrayList<>();

    // Json da pagina principal
    JSONObject frontPageJson = SaopauloB2WCrawlersUtils.getDataLayer(doc);
    // Pega só o que interessa do json da api
    JSONObject infoProductJson = SaopauloB2WCrawlersUtils.assembleJsonProductWithNewWay(frontPageJson);

    // verifying if url starts with home page because on crawler seed,
    // some seeds can be of another store
    if (infoProductJson.has("skus") && session.getOriginalURL().startsWith(this.homePage)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = this.crawlInternalPid(infoProductJson);
      CategoryCollection categories = crawlCategories(infoProductJson);
      boolean hasImages = doc.select(".main-area .row > div > span > img:not([src])").first() == null && doc.select(".gallery-product") != null;
      String primaryImage = hasImages ? this.crawlPrimaryImage(infoProductJson) : null;
      String secondaryImages = hasImages ? this.crawlSecondaryImages(infoProductJson) : null;
      String description = this.crawlDescription(internalPid, doc);
      List<String> eans = crawlEan(infoProductJson);

      Map<String, String> skuOptions = this.crawlSkuOptions(infoProductJson, doc);

      for (Entry<String, String> entry : skuOptions.entrySet()) {
        String internalId = entry.getKey();
        String name = entry.getValue().trim();
        Map<String, Prices> marketplaceMap = this.crawlMarketplace(infoProductJson, internalId);
        Marketplace variationMarketplace = this.assembleMarketplaceFromMap(marketplaceMap);
        boolean available = this.crawlAvailability(marketplaceMap);
        Float variationPrice = this.crawlPrice(marketplaceMap);
        Prices prices = crawlPrices(marketplaceMap);
        List<JSONObject> buyBox = scrapBuyBox(doc, internalId, internalPid);
        Offers offers = assembleOffers(buyBox);
        // Creating the product
        Product product = ProductBuilder.create()
            .setUrl(session.getOriginalURL())
            .setInternalId(internalId)
            .setInternalPid(internalPid)
            .setName(name)
            .setPrice(variationPrice)
            .setPrices(prices)
            .setAvailable(available)
            .setCategory1(categories.getCategory(0))
            .setCategory2(categories.getCategory(1))
            .setCategory3(categories.getCategory(2))
            .setPrimaryImage(primaryImage)
            .setSecondaryImages(secondaryImages)
            .setDescription(description)
            .setMarketplace(variationMarketplace)
            .setOffers(offers)
            .setEans(eans)
            .build();

        products.add(product);
      }
    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private List<String> crawlEan(JSONObject infoProductJson) {
    List<String> eans = new ArrayList<>();
    if (infoProductJson.has("skus")) {
      JSONArray skusArray = infoProductJson.getJSONArray("skus");
      for (Object object : skusArray) {
        JSONObject skus = (JSONObject) object;

        if (skus.has("eans")) {
          JSONArray eansArray = skus.getJSONArray("eans");

          for (Object eansObject : eansArray) {
            String ean = (String) eansObject;
            eans.add(ean);
          }
        }
      }
    }

    return eans;
  }

  private Offers assembleOffers(List<JSONObject> buyBox) {
    Offers offers = new Offers();

    for (JSONObject jsonObject : buyBox) {
      try {
        Offer offer = new Offer(jsonObject);
        offers.add(offer);
      } catch (OfferException e) {
        Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
      }
    }

    return offers;
  }

  private List<JSONObject> scrapBuyBox(Document doc, String internalId, String internalPid) {
    List<JSONObject> listBuyBox = new ArrayList<>();

    JSONObject jsonSeller = CrawlerUtils.selectJsonFromHtml(doc, "script", "window.__PRELOADED_STATE__ =", ";", false, true);
    JSONObject offers = SaopauloB2WCrawlersUtils.extractJsonOffers(jsonSeller, internalPid);

    // Getting informations from sellers.
    if (offers.has(internalId)) {
      JSONArray sellerInfo = offers.getJSONArray(internalId);

      // The Business logic is: if we have more than 1 seller is buy box
      boolean isBuyBox = sellerInfo.length() > 1;

      for (int i = 0; i < sellerInfo.length(); i++) {
        JSONObject buyBoxJson = new JSONObject();

        JSONObject info = (JSONObject) sellerInfo.get(i);
        buyBoxJson.put(OfferField.IS_BUYBOX.toString(), isBuyBox);
        buyBoxJson.put(OfferField.SELLERS_PAGE_POSITION.toString(), JSONObject.NULL);

        Prices prices = crawlMarketplacePrices(info);
        Float price1x = !prices.isEmpty() ? prices.getCardInstallmentValue(DEFAULT_CARD.toString(), 1).floatValue() : null;
        Float bankTicket = CrawlerUtils.getFloatValueFromJSON(info, "bakTicket", true, false);
        Float defaultPrice = CrawlerUtils.getFloatValueFromJSON(info, "defaultPrice", true, false);

        if (i + 1 <= 3) {
          buyBoxJson.put(OfferField.MAIN_PAGE_POSITION.toString(), i + 1);
          Float featuredPrice = null;

          for (Float value : Arrays.asList(price1x, bankTicket, defaultPrice)) {
            if (featuredPrice == null || (value != null && value < featuredPrice)) {
              featuredPrice = value;
            }
          }

          buyBoxJson.put(OfferField.MAIN_PRICE.toString(), featuredPrice);

        } else {
          if (defaultPrice != null) {
            buyBoxJson.put(OfferField.MAIN_PRICE.toString(), defaultPrice);
          } else if (price1x != null) {
            buyBoxJson.put(OfferField.MAIN_PRICE.toString(), price1x);
          } else if (bankTicket != null) {
            buyBoxJson.put(OfferField.MAIN_PRICE.toString(), bankTicket);
          }

          buyBoxJson.put(OfferField.MAIN_PAGE_POSITION.toString(), JSONObject.NULL);
        }

        if (info.has("sellerName")) {
          String name = info.get("sellerName").toString();
          buyBoxJson.put(OfferField.SELLER_FULL_NAME.toString(), name);
          buyBoxJson.put(OfferField.SLUG_SELLER_NAME.toString(), CommonMethods.toSlug(name));
        }

        if (info.has("id")) {
          buyBoxJson.put(OfferField.INTERNAL_SELLER_ID.toString(), info.get("id").toString());
        }

        listBuyBox.add(buyBoxJson);
      }
    }

    if (listBuyBox.size() > 1) {
      this.headers.put(HttpHeaders.REFERER, session.getOriginalURL());
      Document docSellers = Jsoup.parse(fetchPage(this.homePage + "parceiros/" + internalPid + "?productSku=" + internalId, session));
      Elements moreOffers = docSellers.select(".more-offers-table tbody tr .seller-info-cell .seller-info a");

      for (int j = 0; j < moreOffers.size(); j++) {
        Element element = moreOffers.get(j);
        String href = element.attr("href");

        if (href.contains("/")) {
          String sellerId = CommonMethods.getLast(href.split("/")).trim();

          for (JSONObject buyBoxJson : listBuyBox) {
            if (buyBoxJson.has(OfferField.INTERNAL_SELLER_ID.toString())
                && buyBoxJson.get(OfferField.INTERNAL_SELLER_ID.toString()).toString().equals(sellerId)) {
              buyBoxJson.put(OfferField.SELLERS_PAGE_POSITION.toString(), j + 1);
            }
          }
        }
      }
    }

    return listBuyBox;

  }

  /*******************************
   * Product page identification *
   *******************************/

  private String crawlInternalPid(JSONObject assembleJsonProduct) {
    String internalPid = null;

    if (assembleJsonProduct.has("internalPid")) {
      internalPid = assembleJsonProduct.getString("internalPid").trim();
    }

    return internalPid;
  }

  private Map<String, String> crawlSkuOptions(JSONObject infoProductJson, Document doc) {
    Map<String, String> skuMap = new HashMap<>();

    boolean unnavailablePage = !doc.select("#title-stock").isEmpty();

    if (infoProductJson.has("skus")) {
      JSONArray skus = infoProductJson.getJSONArray("skus");

      for (int i = 0; i < skus.length(); i++) {
        JSONObject sku = skus.getJSONObject(i);

        if (sku.has("internalId")) {
          String internalId = sku.getString("internalId");
          StringBuilder name = new StringBuilder();

          String variationName = "";
          if (sku.has("variationName")) {
            variationName = sku.getString("variationName");
          }

          String varationNameWithoutVolts = variationName.replace("volts", "").trim();

          if (unnavailablePage || (variationName.isEmpty() && skus.length() < 2) && infoProductJson.has("name")) {
            name.append(infoProductJson.getString("name"));
          } else if (sku.has("name")) {
            name.append(sku.getString("name"));

            if (!name.toString().toLowerCase().contains(varationNameWithoutVolts.toLowerCase())) {
              name.append(" " + variationName);
            }
          }

          skuMap.put(internalId, name.toString().trim());
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

            if (marketArrays.length() == 1 && seller.has("priceFrom")) {
              String text = seller.get("priceFrom").toString();
              prices.setPriceFrom(MathUtils.parseDoubleWithComma(text));
            }

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
      if (!installmentMapPrice.isEmpty() && installmentMapPrice.containsKey(1) && seller.has("defaultPrice")) {
        Float defaultPrice = CrawlerUtils.getFloatValueFromJSON(seller, "defaultPrice");

        if (!defaultPrice.equals(installmentMapPrice.get(1))) {
          installmentMapPrice.put(2, MathUtils.normalizeTwoDecimalPlaces(defaultPrice / 2f));
        }
      }

      prices.insertCardInstallment(DEFAULT_CARD.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.VISA.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.AURA.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.HIPER.toString(), installmentMapPrice);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentMapPrice);
    }

    if (seller.has("installmentsShopCard")) {
      Map<Integer, Float> installmentMapPrice = new HashMap<>();

      JSONArray installments = seller.getJSONArray("installmentsShopCard");

      for (int i = 0; i < installments.length(); i++) {
        JSONObject installment = installments.getJSONObject(i);

        if (installment.has("quantity") && installment.has("value")) {
          Integer quantity = installment.getInt("quantity");
          Double value = installment.getDouble("value");

          installmentMapPrice.put(quantity, MathUtils.normalizeTwoDecimalPlaces(value.floatValue()));
        }
      }

      prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentMapPrice);
    }

    return prices;
  }

  /*******************
   * General methods *
   *******************/

  private Float crawlPrice(Map<String, Prices> marketplaces) {
    Float price = null;
    Prices prices = null;

    String sellerName = getPrincipalSellerName(marketplaces);
    if (sellerName != null) {
      prices = marketplaces.get(sellerName);
    }

    if (prices != null && prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
      Double priceDouble = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
      price = priceDouble.floatValue();
    }

    return price;
  }

  private boolean crawlAvailability(Map<String, Prices> marketplaces) {
    boolean available = false;

    String sellerName = getPrincipalSellerName(marketplaces);
    if (sellerName != null) {
      available = true;
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

  /**
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(JSONObject document) {
    CategoryCollection categories = new CategoryCollection();

    JSONArray categoryList = document.getJSONArray("categories");

    for (int i = categoryList.length() - 1; i >= 0; i--) { // Invert the Loop since the categorys in the JSONArray come reversed
      String cat = (categoryList.getJSONObject(i).get("name")).toString();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }


  private Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap) {
    Marketplace marketplace = new Marketplace();

    boolean hasPrincipalSeller = marketplaceMap.containsKey(MAIN_B2W_NAME_LOWER) || marketplaceMap.containsKey(sellerNameLower);

    for (String sellerName : marketplaceMap.keySet()) {
      if (!sellerName.equalsIgnoreCase(sellerNameLower) && !sellerName.equalsIgnoreCase(MAIN_B2W_NAME_LOWER)) {
        if (!hasPrincipalSeller && this.subSellers.contains(sellerName)) {
          continue;
        }

        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", sellerName);

        Prices prices = marketplaceMap.get(sellerName);

        if (prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
          // Pegando o preço de uma vez no cartão
          Double price = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
          Float priceFloat = MathUtils.normalizeTwoDecimalPlaces(price.floatValue());

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
    StringBuilder description = new StringBuilder();

    boolean alreadyCapturedHtmlSlide = false;

    Element datasheet = doc.selectFirst("#info-section");
    if (datasheet != null) {
      Element iframe = datasheet.selectFirst("iframe");

      if (iframe != null) {
        Document docDescriptionFrame = Jsoup.parse(fetchPage(iframe.attr("src"), session));
        if (docDescriptionFrame != null) {
          description.append(docDescriptionFrame.html());
        }
      }

      // https://www.shoptime.com.br/produto/8421276/mini-system-mx-hs6500-zd-bluetooth-e-funcao-karaoke-bivolt-preto-samsung
      // alreadyCapturedHtmlSlide as been moved here because of links like these.

      alreadyCapturedHtmlSlide = true;
      datasheet.select("iframe, h1.sc-hgHYgh").remove();
      description.append(datasheet.html().replace("hidden", ""));
    }

    if (internalPid != null) {
      Element desc2 = doc.select(".info-description-frame-inside").first();

      if (desc2 != null && !alreadyCapturedHtmlSlide) {
        String urlDesc2 = homePage + "product-description/acom/" + internalPid;
        Document docDescriptionFrame = Jsoup.parse(fetchPage(urlDesc2, session));
        if (docDescriptionFrame != null) {
          description.append(docDescriptionFrame.html());
        }
      }

      Element elementProductDetails = doc.select(".info-section").last();
      if (elementProductDetails != null) {
        elementProductDetails.select(".info-section-header.hidden-md.hidden-lg").remove();
        description.append(elementProductDetails.html());
      }
    }

    return Normalizer.normalize(description.toString(), Normalizer.Form.NFD).replaceAll("[^\n\t\r\\p{Print}]", "");
  }

  private Prices crawlPrices(Map<String, Prices> marketplaceMap) {
    Prices prices = new Prices();

    String sellerName = getPrincipalSellerName(marketplaceMap);
    if (sellerName != null) {
      prices = marketplaceMap.get(sellerName);
    }

    return prices;
  }

  /**
   * se retornar null o produto nao e vendido pela loja
   * 
   * @param marketplaceMap
   * @return
   */
  private String getPrincipalSellerName(Map<String, Prices> marketplaceMap) {
    String sellerName = null;

    if (marketplaceMap.containsKey(sellerNameLower)) {
      sellerName = sellerNameLower;
    } else if (marketplaceMap.containsKey(MAIN_B2W_NAME_LOWER)) {
      sellerName = MAIN_B2W_NAME_LOWER;
    } else {
      for (String seller : subSellers) {
        if (marketplaceMap.containsKey(seller)) {
          sellerName = seller;
          break;
        }
      }
    }

    return sellerName;
  }

}
