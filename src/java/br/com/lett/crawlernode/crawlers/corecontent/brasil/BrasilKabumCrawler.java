package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.Fetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.Logging;
import models.Marketplace;
import models.prices.Prices;

public class BrasilKabumCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.kabum.com.br";

  public BrasilKabumCrawler(Session session) {
    super(session);
    super.config.setFetcher(Fetcher.WEBDRIVER);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  protected Object fetch() {
    this.webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), session);
    Document doc = Jsoup.parse(this.webdriver.getCurrentPageSource());

    Element script = doc.select("script").first();
    Element robots = doc.select("meta[name=robots]").first();

    if (script != null && robots != null) {
      String eval = script.html().trim();
      if (!eval.isEmpty()) {
        Logging.printLogDebug(logger, session, "Escution of incapsula js script...");
        this.webdriver.executeJavascript(eval);
      }

      this.webdriver.waitLoad(9000);
      return Jsoup.parse(this.webdriver.getCurrentPageSource());
    }

    return doc;
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(this.session.getOriginalURL())) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      /**
       * Caso fixo do blackfriday onde aparentemente a página do produto dá um refresh para a página do
       * produto na blackfriday.
       */
      Element blackFriday = doc.select("meta[http-equiv=refresh]").first();

      if (blackFriday != null) {
        String url = blackFriday.attr("content");

        if (url.contains("url=")) {
          int x = url.indexOf("url=") + 4;

          url = url.substring(x);
        }

        doc = DataFetcher.fetchDocument(DataFetcher.GET_REQUEST, session, url, null, cookies);
      }

      // internalId
      String internalID = null;
      Element elementInternalID = doc.select(".boxs .links_det").first();
      if (elementInternalID != null) {
        String text = elementInternalID.ownText();
        internalID = text.substring(text.indexOf(':') + 1).trim();

        if (internalID.isEmpty()) {
          Element e = elementInternalID.select("span[itemprop=sku]").first();

          if (e != null) {
            internalID = e.ownText().trim();
          }
        }
      }

      Element elementProduct = doc.select("#pag-detalhes").first();

      // internalPid
      String internalPid = null;

      // price
      Float price = null;

      // availability
      boolean available = false;

      // categories
      CategoryCollection categories = crawlCategories(doc);

      // Images
      String primaryImage = null;
      String secondaryImages = null;

      // name
      String name = null;

      // Prices
      Prices prices = new Prices();

      if (elementProduct != null) {

        // Prices
        prices = crawlPrices(elementProduct, doc);

        // Name
        Element elementName = elementProduct.select("#titulo_det h1").first();
        if (elementName != null) {
          name = elementName.text().replace("'", "").replace("’", "").trim();
        }

        // Price
        price = crawlPrice(prices);

        // Available
        Element elementAvailability = elementProduct.select(".disponibilidade img").first();
        Element availablePromo = doc.select(".box_comprar-cm .textocomprar").first();
        if ((elementAvailability == null || elementAvailability.attr("alt").equalsIgnoreCase("produto_disponivel")) || availablePromo != null) {
          available = true;
        }

        // images
        Elements elementImages = elementProduct.select("#imagens-carrossel li img");
        JSONArray secondaryImagesArray = new JSONArray();
        for (Element e : elementImages) {
          if (primaryImage == null) {
            primaryImage = e.attr("src").replace("_p.", "_g.");
          } else {
            secondaryImagesArray.put(e.attr("src").replace("_p.", "_g."));
          }

        }

        if (secondaryImagesArray.length() > 0) {
          secondaryImages = secondaryImagesArray.toString();
        }
      }

      // description
      String description = crawlDescription(doc);

      // stock
      Integer stock = null;

      // marketplace
      Marketplace marketplace = new Marketplace();

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalID).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
          .setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }


  private Float crawlPrice(Prices prices) {
    Float price = null;

    if (prices != null && prices.getCardPaymentOptions(Card.MASTERCARD.toString()).containsKey(1)) {
      Double priceDouble = prices.getCardPaymentOptions(Card.MASTERCARD.toString()).get(1);
      price = priceDouble.floatValue();
    }

    return price;
  }

  private String crawlDescription(Document doc) {
    StringBuilder str = new StringBuilder();
    Elements tabs = doc.select("#pag-detalhes .tab_");

    for (Element e : tabs) {
      if (e.select(".opiniao_box").isEmpty()) {
        str.append(e.html());
      }
    }

    Element specialDescription = doc.select(".MsoNormal").first();

    if (specialDescription != null) {
      str.append(specialDescription.html());
    }

    return str.toString().replace("’", "").trim();
  }


  /**
   * Crawl categories
   * 
   * @param document
   * @return
   */
  private CategoryCollection crawlCategories(Document document) {
    CategoryCollection categories = new CategoryCollection();
    Elements elementCategories = document.select(".links_det ol li > a");

    for (int i = 0; i < elementCategories.size(); i++) {
      categories.add(elementCategories.get(i).text().replace(">", "").trim());
    }

    return categories;
  }

  private Prices crawlPrices(Element product, Document doc) {
    Prices prices = new Prices();
    Map<Integer, Float> installmentPriceMap = new HashMap<>();

    Element priceBoleto = product.select(".preco_desconto strong").first();

    if (priceBoleto == null) {
      priceBoleto = doc.select(".preco_desconto_avista-cm").first();
    }

    if (priceBoleto != null) {
      Float bankTicket = Float.parseFloat(priceBoleto.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());
      prices.setBankTicketPrice(bankTicket);
    }

    Elements installmentsPrices = product.select(".ParcelamentoCartao li");

    if (installmentsPrices.size() < 1) {
      installmentsPrices = doc.select(".ParcelamentoCartao-cm li");
    }

    for (Element e : installmentsPrices) {
      String text = e.text().toLowerCase();

      int x = text.indexOf("x");

      Integer installment = Integer.parseInt(text.substring(0, x).trim());
      Float value = Float.parseFloat(text.substring(x).replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));

      installmentPriceMap.put(installment, value);
    }

    if (installmentPriceMap.size() > 0) {
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(String url) {
    return true;
  }
}
