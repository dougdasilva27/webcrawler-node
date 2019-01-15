package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
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
import br.com.lett.crawlernode.util.Pair;
import models.prices.Prices;

public class BrasilNutrineCrawler extends Crawler {
  private static final String HOME_PAGE = "https://www.nutrine.com.br/";

  public BrasilNutrineCrawler(Session session) {
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

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc,
          ".halfRight > input#produtoId", "value");
      String internalPid = crawlInternalPid(doc, "#product .halfRight .prodName .code");
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, ".halfRight > .prodName > h1", true);
      CategoryCollection categories =
          CrawlerUtils.crawlCategories(doc, ".detalheHeader a.nivel1:not(:first-child)");
      String primaryImage = scrapPrimaryImage(doc, "#product .halfLeft .thumbs .cloudzoom-gallery");
      String secondaryImages =
          scrapSecondaryImage(doc, "#product .halfLeft .thumbs .cloudzoom-gallery", primaryImage);
      String description =
          CrawlerUtils.scrapElementsDescription(doc, Arrays.asList("li.item_aba", ".conteudo"));
      Integer stock = null;
      Float price = null;
      Prices prices = null;
      boolean available = checkAvaliability(doc, "#product .preco .btnOrcamento");

      if (available) {
        price = CrawlerUtils.scrapSimplePriceFloat(doc, "#product .halfRight .preco .por b", true);
        prices = crawlPrices(price, doc);
      }

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL())
          .setInternalId(internalId).setInternalPid(internalPid).setName(name).setPrice(price)
          .setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1)).setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
          .setDescription(description).setStock(stock).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".produtoDetalhes") != null;
  }

  private String crawlInternalPid(Document doc, String selector) {
    String internalPid = null;
    String aux = CrawlerUtils.scrapStringSimpleInfo(doc, selector, true);

    if (aux.startsWith("Ref: ")) {
      internalPid = aux.substring(5);
    }

    return internalPid;
  }

  private String scrapPrimaryImage(Document doc, String selector) {
    Element e = doc.selectFirst(selector);
    String imageUrl = null;

    if (e != null) {
      JSONObject json = CrawlerUtils.stringToJson(e.attr("data-cloudzoom"));

      if (json.has("zoomImage")) {
        imageUrl =
            CrawlerUtils.completeUrl(json.getString("zoomImage"), "https", "cdn.nutrine.com.br");
      }
    }


    return imageUrl;
  }

  private String scrapSecondaryImage(Document doc, String selector, String primaryImage) {
    Elements elmnts = doc.select(selector);
    String secondaryImages = null;
    JSONArray secondaryImagesArray = new JSONArray();

    for (Element e : elmnts) {
      if (e != null) {
        JSONObject json = CrawlerUtils.stringToJson(e.attr("data-cloudzoom"));

        if (json.has("zoomImage")) {
          String img =
              CrawlerUtils.completeUrl(json.getString("zoomImage"), "https", "cdn.nutrine.com.br");

          if ((primaryImage == null || !primaryImage.equals(img)) && img != null) {
            secondaryImagesArray.put(img);
          }
        }
      }
    }

    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();
    JSONObject json = CrawlerUtils.selectJsonFromHtml(doc, ".floatLeft.ten-column script[type]",
        "porcentagemDescontoVista=", ",\nusuario", true, false);
    String key = "valorComDesconto";

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      Elements elmnts = doc.select("#product .preco");

      prices.setPriceFrom(price.doubleValue());

      if (!json.isNull(key)) {
        prices.setBankTicketPrice(CrawlerUtils.getFloatValueFromJSON(json, key));
      }

      for (Element e : elmnts) {
        Pair<Integer, Float> aux =
            CrawlerUtils.crawlSimpleInstallment(".formas", e, true, "x", "sem", true);

        installmentPriceMap.put(aux.getFirst(), aux.getSecond());
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }

  private boolean checkAvaliability(Document doc, String selector) {
    return doc.selectFirst(selector) == null;
  }
}
