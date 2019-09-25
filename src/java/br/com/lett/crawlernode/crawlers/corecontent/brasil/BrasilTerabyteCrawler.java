package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Response;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.test.Test;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 16/10/2018
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilTerabyteCrawler extends Crawler {

  private static final String HOME_PAGE = "https://www.terabyteshop.com.br/";

  public BrasilTerabyteCrawler(Session session) {
    super(session);
  }

  @Override
  protected Object fetch() {
    Document doc = null;

    try {
      URL url = new URL(session.getOriginalURL());
      String pathUrl = url.getPath();
      String apiUrl = session.getOriginalURL() + "?app=true&" + "&url=" + pathUrl;
      Request request = RequestBuilder.create().setCookies(cookies).setUrl(apiUrl).build();
      Response response = dataFetcher.get(session, request);
      // A resposta vem com um caractere ascii que não aparece em arquivo, por isso é usado essa expressão
      // para remover.
      String resultString = response.getBody().replaceAll("[^\\x00-\\x7F]", "");
      JSONObject bodyJson = JSONUtils.stringToJson(resultString);
      doc = Jsoup.parse(JSONUtils.getStringValue(bodyJson, "body"));

    } catch (MalformedURLException e) {
      Logging.printLogError(logger, session, e.getMessage());
    }

    return doc;
  }

  @Override
  public boolean shouldVisit() {
    String href = session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    CommonMethods.saveDataToAFile(doc, Test.pathWrite + "e.html");
    List<Product> products = new ArrayList<>();
    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "span[data-produto]", "data-produto");
      String internalPid = crawlInternalPid(doc);
      String name = CrawlerUtils.scrapStringSimpleInfo(doc, "h1.tit-prod", false);
      Float price = crawlPrice(doc);
      Prices prices = crawlPrices(doc, price);
      Element availableElement = doc.selectFirst("#indisponivel");
      boolean available = availableElement == null && price != null;
      CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li a strong");
      String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, ".fotorama img", Arrays.asList("src"), "https:", "www.terabyteshop.com.br");
      String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc, ".fotorama img", Arrays.asList("src"), "https:",
          "www.terabyteshop.com.br", primaryImage);
      String description = crawlDescription(doc);

      // Creating the product
      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setPrice(price)
          .setPrices(prices)
          .setAvailable(available)
          .setCategory1(categories.getCategory(0))
          .setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2))
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setMarketplace(new Marketplace())
          .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;

  }

  private Float crawlPrice(Document doc) {
    Float price = CrawlerUtils.scrapSimplePriceFloat(doc, ".val-prod", true);

    if (price == null) {
      price = CrawlerUtils.scrapSimplePriceFloat(doc, ".p3", false);
    }

    return price;
  }

  private boolean isProductPage(Document doc) {
    return !doc.select(".produtos-home").isEmpty();
  }

  private static String crawlInternalId(Document doc) {
    String internalId = null;

    Element infoElement = doc.selectFirst("input#idproduto");
    if (infoElement != null) {
      internalId = infoElement.val();
    }

    return internalId;
  }

  private static String crawlInternalPid(Document doc) {
    String inernalPid = null;

    Element infoElement = doc.selectFirst("#partnumber");
    if (infoElement != null) {
      String text = infoElement.ownText();

      if (text.contains(":")) {
        inernalPid = CommonMethods.getLast(text.split(":")).trim();
      } else {
        inernalPid = CommonMethods.getLast(text.trim().split(" ")).trim();
      }
    }

    return inernalPid;
  }

  private String crawlDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Elements selos = doc.select(".boxs .box-selo");
    for (Element e : selos) {
      if (e.select("> a").isEmpty()) {
        description.append(e.outerHtml());
      }
    }

    description.append(CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".especificacoes .descricao", ".especificacoes .tecnicas")));

    return description.toString();
  }

  /**
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();
      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);



      Elements parcels = doc.select("#verparcelamento ul li");
      for (Element e : parcels) {
        Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(null, e, false, "x");
        if (!pair.isAnyValueNull()) {
          installmentPriceMap.put(pair.getFirst(), pair.getSecond());
        }
      }

      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }

}
