package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.aws.s3.S3Service;
import br.com.lett.crawlernode.core.fetcher.DataFetcherNO;
import br.com.lett.crawlernode.core.fetcher.DynamicDataFetcher;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
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
import br.com.lett.crawlernode.util.Pair;
import models.Marketplace;
import models.prices.Prices;

public class SaopauloUltrafarmaCrawler extends Crawler {

  private static final String HOME_PAGE = "http://www.ultrafarma.com.br/";

  public SaopauloUltrafarmaCrawler(Session session) {
    super(session);
    super.config.setFetcher(FetchMode.WEBDRIVER);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  protected Document fetch() {
    Document doc = new Document("");
    this.webdriver = DynamicDataFetcher.fetchPageWebdriver(session.getOriginalURL(), session);

    if (this.webdriver != null) {
      doc = Jsoup.parse(this.webdriver.getCurrentPageSource());

      Element script = doc.select("head script").last();
      Element robots = doc.select("meta[name=robots]").first();

      if (script != null && robots != null) {
        String eval = script.html().trim();

        if (!eval.isEmpty()) {
          Logging.printLogDebug(logger, session, "Execution of incapsula js script...");
          this.webdriver.executeJavascript(eval);
        }
      }

      String requestHash = DataFetcherNO.generateRequestHash(session);
      this.webdriver.waitLoad(12000);

      doc = Jsoup.parse(this.webdriver.getCurrentPageSource());
      Logging.printLogDebug(logger, session, "Terminating PhantomJS instance ...");
      this.webdriver.terminate();

      // saving request content result on Amazon
      S3Service.uploadCrawlerSessionContentToAmazon(session, requestHash, doc.toString());
    }

    return doc;
  }

  @Override
  public void handleCookiesBeforeFetch() {
    Logging.printLogDebug(logger, session, "Adding cookie...");

    BasicClientCookie cookie = new BasicClientCookie("ultrafarma_uf", "SP");
    cookie.setDomain(".ultrafarma.com.br");
    cookie.setPath("/");
    this.cookies.add(cookie);
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // ID interno
      String internalID = crawlInternalId(doc);

      // Pid
      String internalPid = crawlInternalPid(doc);

      // Disponibilidade
      boolean available = doc.select(".txt_pag_pedido_dados").isEmpty();

      // Nome
      String name = null;
      Element elementName = doc.select(".bloco-produto-detalhe h1").first();
      if (elementName != null) {
        name = elementName.text().trim();
      }

      // Preço
      Float price = null;
      Element elementPrice = doc.select(".preco-por-deta").first();
      if (elementPrice != null) {
        price = MathUtils.parseFloatWithComma(elementPrice.text());
      }

      // Categorias
      CategoryCollection categories = crawlCategories(doc);

      // Imagem primária
      Elements elementPrimaryImage = doc.select("#imagem-grande");
      String primaryImage = elementPrimaryImage.attr("src");

      // Imagens secundárias
      String secondaryImages = null;

      JSONArray secondaryImagesArray = new JSONArray();
      Elements images = doc.select(".conteudo-chama-foto img");

      for (int i = 1; i < images.size(); i++) {
        Element e = images.get(i);
        secondaryImagesArray.put(e.attr("src"));
      }

      if (secondaryImagesArray.length() > 0) {
        secondaryImages = secondaryImagesArray.toString();
      }

      // Descrição
      StringBuilder description = new StringBuilder();

      Elements infos = doc.select(".tabcontent:not(#Comet), .div_anvisa");
      for (Element e : infos) {
        String text = e.text().toLowerCase().trim();

        if (!text.contains("comentários")) {
          description.append(e.outerHtml());
        }
      }

      // Estoque
      Integer stock = null;

      // Marketplace
      Marketplace marketplace = new Marketplace();

      // Prices
      Prices prices = crawlPrices(doc, price);

      // Creating the product
      Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalID).setInternalPid(internalPid).setName(name)
          .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
          .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages)
          .setDescription(description.toString()).setStock(stock).setMarketplace(marketplace).build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, "Not a product page.");
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document doc) {
    return doc.selectFirst(".div_prod_qualidade > span") != null;
  }

  private String crawlInternalId(Document doc) {
    String internalId = null;

    Element id = doc.selectFirst(".div_prod_qualidade > span");
    if (id != null) {
      String text = id.ownText();

      if (text.contains(":")) {
        internalId = CommonMethods.getLast(text.split(":"));

        if (internalId.contains("-")) {
          internalId = internalId.split("-")[0].trim();
        }
      }
    }

    return internalId;
  }

  private String crawlInternalPid(Document doc) {
    String internalPid = null;

    Element id = doc.selectFirst(".div_prod_qualidade > span");
    if (id != null) {
      String text = id.ownText();

      if (text.contains(":")) {
        String ids = CommonMethods.getLast(text.split(":"));

        if (ids.contains("-")) {
          internalPid = CommonMethods.getLast(ids.split("-")).replace("[", "").replace("]", "").trim();
        }
      }
    }

    return internalPid;
  }

  /**
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new TreeMap<>();

      installmentPriceMap.put(1, price);

      Element priceFrom = doc.select(".preco-de-deta").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
      }

      Pair<Integer, Float> pair = CrawlerUtils.crawlSimpleInstallment(".bloco-produto-detalhe [class^=parcele]", doc, false);
      if (!pair.isAnyValueNull()) {
        installmentPriceMap.put(pair.getFirst(), pair.getSecond());
      }

      prices.setBankTicketPrice(price);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPER.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }

    return prices;
  }

  private CategoryCollection crawlCategories(Document doc) {
    CategoryCollection categories = new CategoryCollection();

    Elements cats = doc.select(".alBreadCrumbs li a");
    int i = 0;

    for (Element e : cats) {
      if (i != 0) {
        String text = e.ownText().trim();
        categories.add(text);
      }
      i++;
    }

    return categories;
  }
}
