package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.Seller;
import models.Util;
import models.prices.Prices;

public class BrasilKanuiCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.kanui.com.br/";
  private static final String MAIN_SELLER_NAME_LOWER = "kanui";

  public BrasilKanuiCrawler(Session session) {
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
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // Nome
      Elements elementPreName = doc.select("h1.product-name");
      String preName = elementPreName.text().replace("'", "").replace("’", "").trim();

      // Categorias
      Elements elementCategories = doc.select("ul.breadcrumb2").first().select("li");
      String category1 = null;
      String category2 = null;
      String category3 = null;

      if (elementCategories.size() > 1) {
        category1 = elementCategories.get(1).text();
      }
      if (elementCategories.size() > 2) {
        category2 = elementCategories.get(2).text();
      }
      if (elementCategories.size() > 3) {
        category3 = elementCategories.get(3).text();
      }

      // Imagem primária e imagens secundárias
      Elements elementPrimaryImage = doc.select(".gallery-thumbs ul.carousel-items").select("a");
      String primaryImage = null;
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      for (Element e : elementPrimaryImage) {

        if (primaryImage == null) {
          primaryImage = e.attr("data-img-zoom");
        } else {
          secondaryImagesArray.put(e.attr("data-img-zoom"));
        }

      }

      if (secondaryImagesArray.length() > 0) {
        secondaryImages = secondaryImagesArray.toString();
      }

      // Descrição
      String description = "";
      Elements elementDescription = doc.select(".product-information-content");
      description = elementDescription.first().text().replace(".", ".\n").replace("'", "").replace("’", "").trim();

      Element elementSku = doc.select("#add-to-cart input[name=p]").first();

      try {
        String sku = elementSku.attr("value");

        // Pegando os produtos usando o endpoint da Dafiti

        String url = "https://www.kanui.com.br/catalog/detailJson?sku=" + sku + "&_=1522244165198";
        Request request = RequestBuilder.create().setUrl(url).setCookies(cookies).build();
        JSONObject json = CrawlerUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

        JSONArray sizes = json.has("sizes") ? json.getJSONArray("sizes") : new JSONArray();

        /*
         * Pegar o restante das informações usando os objetos JSON vindos do endpoint da dafit
         */
        for (int i = 0; i < sizes.length(); i++) {

          // ID interno
          String internalId = sizes.getJSONObject(i).getString("sku");

          // Pid
          String internalPid = internalId.split("-")[0];

          // Nome - pré-nome pego anteriormente, acrescido do tamanho do sapato
          String name = preName + " (tamanho " + sizes.getJSONObject(i).getString("name") + ")";

          Map<String, Prices> marketplaceMap = crawlMarketplace(doc, json);
          Integer stock = Integer.parseInt(sizes.getJSONObject(i).get("stock").toString());
          boolean available = marketplaceMap.containsKey(MAIN_SELLER_NAME_LOWER) && stock > 0;

          Marketplace marketplace = assembleMarketplaceFromMap(marketplaceMap);
          Prices prices = available ? marketplaceMap.get(MAIN_SELLER_NAME_LOWER) : new Prices();
          stock = available ? stock : 0;
          Float price = crawlPrice(prices);

          Product product = new Product();
          product.setUrl(this.session.getOriginalURL());
          product.setInternalId(internalId);
          product.setInternalPid(internalPid);
          product.setName(name);
          product.setPrice(price);
          product.setCategory1(category1);
          product.setCategory2(category2);
          product.setCategory3(category3);
          product.setPrimaryImage(primaryImage);
          product.setSecondaryImages(secondaryImages);
          product.setDescription(description);
          product.setStock(stock);
          product.setMarketplace(marketplace);
          product.setAvailable(available);
          product.setPrices(prices);

          products.add(product);

        }
      } catch (Exception e1) {
        e1.printStackTrace();
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
    return (document.select(".container.product-page").first() != null);
  }


  private Map<String, Prices> crawlMarketplace(Document doc, JSONObject json) {
    Map<String, Prices> marketplace = new HashMap<>();

    String sellerName = MAIN_SELLER_NAME_LOWER;
    Element sellerNameElement = doc.select(".product-seller-name strong").first();

    if (sellerNameElement != null) {
      sellerName = sellerNameElement.ownText().toLowerCase();
    }

    marketplace.put(sellerName, crawlPrices(doc, json));

    return marketplace;
  }

  private Prices crawlPrices(Document doc, JSONObject skuJson) {
    Prices prices = new Prices();

    Element priceElement = doc.select(".catalog-detail-price-value").first();

    if (priceElement != null) {
      Float price = MathUtils.parseFloatWithComma(priceElement.ownText());
      prices.setBankTicketPrice(price);

      Map<Integer, Float> mapInstallments = new HashMap<>();
      mapInstallments.put(1, price);


      if (skuJson.has("installments")) {
        JSONObject installments = skuJson.getJSONObject("installments");

        if (installments.has("count") && installments.has("value")) {
          String installment = installments.get("count").toString().replaceAll("[^0-9]", "").trim();
          Float priceInstallment = MathUtils.parseFloatWithComma(installments.get("value").toString());

          if (!installment.isEmpty() && priceInstallment != null) {
            mapInstallments.put(Integer.parseInt(installment), priceInstallment);
          }
        }

      }

      prices.insertCardInstallment(Card.VISA.toString(), mapInstallments);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), mapInstallments);
      prices.insertCardInstallment(Card.AMEX.toString(), mapInstallments);
      prices.insertCardInstallment(Card.DINERS.toString(), mapInstallments);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), mapInstallments);
      prices.insertCardInstallment(Card.ELO.toString(), mapInstallments);
      prices.insertCardInstallment(Card.SHOP_CARD.toString(), mapInstallments);
    }

    return prices;
  }

  private Marketplace assembleMarketplaceFromMap(Map<String, Prices> marketplaceMap) {
    Marketplace marketplace = new Marketplace();

    for (String seller : marketplaceMap.keySet()) {
      if (!seller.equalsIgnoreCase(MAIN_SELLER_NAME_LOWER)) {
        Prices prices = marketplaceMap.get(seller);

        JSONObject sellerJSON = new JSONObject();
        sellerJSON.put("name", seller);
        sellerJSON.put("price", crawlPrice(prices));
        sellerJSON.put("prices", prices.toJSON());

        try {
          Seller s = new Seller(sellerJSON);
          marketplace.add(s);
        } catch (Exception e) {
          Logging.printLogError(logger, session, Util.getStackTraceString(e));
        }
      }
    }

    return marketplace;
  }

  private Float crawlPrice(Prices prices) {
    Float price = null;

    if (!prices.isEmpty() && prices.getCardPaymentOptions(Card.VISA.toString()).containsKey(1)) {
      Double priceDouble = prices.getCardPaymentOptions(Card.VISA.toString()).get(1);
      price = priceDouble.floatValue();
    }

    return price;
  }

}
