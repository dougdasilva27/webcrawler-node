package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.methods.POSTFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 29/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilDrogariapovaoCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.drogariaspovao.com.br/";

  public BrasilDrogariapovaoCrawler(Session session) {
    super(session);
  }

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    // performing request to get cookie
    String cookieValue = DataFetcher.fetchCookie(session, HOME_PAGE, "PHPSESSID", null, 1);

    BasicClientCookie cookie = new BasicClientCookie("PHPSESSID", cookieValue);
    cookie.setDomain("www.drogariaspovao.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }

  @Override
  protected Document fetch() {
    StringBuilder str = new StringBuilder();
    str.append("<html><body>");
    String url = session.getOriginalURL();

    if (url.contains("detalhes_produto/")) {
      String id = url.split("detalhes_produto/")[1].split("/")[0];

      String payload = "arrapara=[\"Carregar_Detalhes_Produto\",\"" + id + "\",\"\",\"detalhe_pagina\"]&origem=site&controle=navegacao";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/x-www-form-urlencoded");

      String page =
          POSTFetcher.fetchPagePOSTWithHeaders("http://www.drogariaspovao.com.br/ct/atende_geral.php", session, payload, cookies, 1, headers).trim();

      if (page != null && page.startsWith("[") && page.endsWith("]")) {
        try {
          JSONArray infos = new JSONArray(page);

          if (infos.length() > 6) {
            JSONArray productInfo = infos.getJSONArray(6); // 6º position of array has product info

            if (productInfo.length() >= 3) {
              str.append("<h1 class=\"name\">" + productInfo.get(2) + "</h1>");
              str.append("<h2 class=\"cod\">" + productInfo.get(1) + "</h2>");
              str.append("<div class=\"info\">" + productInfo.get(0) + "</div>");
            }
          }
        } catch (JSONException e) {
          Logging.printLogError(logger, session, CommonMethods.getStackTrace(e));
        }
      }
    }

    str.append("</body></html>");

    return Jsoup.parse(str.toString());
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

    if (isProductPage(session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = crawlInternalId(doc);
      String internalPid = null;
      String name = crawlName(doc);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(price, doc);
      boolean available = price != null;
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

  private boolean isProductPage(String url) {
    return url.contains("detalhes_produto/");
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;
    Element internalIdElement = doc.select("h2.cod").first();

    if (internalIdElement != null) {
      internalId = internalIdElement.ownText();
    }

    return internalId;
  }

  private String crawlName(Document document) {
    String name = null;
    Element nameElement = document.select("h1.name").first();

    if (nameElement != null) {
      name = nameElement.ownText().trim();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;
    Element salePriceElement = document.select(".product_price b font").first();

    if (salePriceElement != null) {
      price = MathUtils.parseFloat(salePriceElement.ownText());
    }

    return price;
  }

  private Marketplace crawlMarketplace() {
    return new Marketplace();
  }


  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;
    Element elementPrimaryImage = doc.select("#img_zoom").first();

    if (elementPrimaryImage != null) {
      String image = elementPrimaryImage.attr("data-zoom-image").trim();

      if (image.isEmpty()) {
        image = elementPrimaryImage.attr("src").trim();
      }

      if (image.contains("../")) {
        primaryImage = HOME_PAGE + image.replace("../", "").trim();
      } else if (image.startsWith(HOME_PAGE)) {
        primaryImage = image;
      } else {
        primaryImage = HOME_PAGE + image;
      }
    }

    return primaryImage;
  }

  /**
   * No momento que o crawler foi feito não achei produto com imagens secundárias
   * 
   * @param doc
   * @return
   */
  private String crawlSecondaryImages(Document doc) {
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

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
    Elements elementCategories = document.select(".breadcrumbCustomizado ul li > a");

    for (int i = 1; i < elementCategories.size(); i++) {
      String cat = elementCategories.get(i).ownText().trim();

      if (!cat.isEmpty()) {
        categories.add(cat);
      }
    }

    return categories;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element elementDescription = doc.select(".description_section").last();

    if (elementDescription != null) {
      description.append(elementDescription.html());
    }

    return description.toString();
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

      Element priceFrom = doc.select(".product_price s font").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDouble(priceFrom.text()));
      }

      Element installments = doc.select(".product_price > font").first();

      if (installments != null) {
        String text = installments.ownText().toLowerCase();

        if (text.contains("x")) {
          int x = text.indexOf('x');

          String parcel = text.substring(0, x).replaceAll("[^0-9]", "").trim();
          Float value = MathUtils.parseFloat(text.substring(x));

          if (!parcel.isEmpty() && value != null) {
            installmentPriceMap.put(Integer.parseInt(parcel), value);
          }
        }
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.SOROCRED.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);
    }

    return prices;
  }


}
