package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.DataFetcher;
import br.com.lett.crawlernode.core.fetcher.LettProxy;
import br.com.lett.crawlernode.core.fetcher.methods.GETFetcher;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class BrasilIbyteCrawler extends Crawler {

  private final String HOME_PAGE = "http://www.ibyte.com.br/";

  public BrasilIbyteCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  private static final String USER_AGENT = DataFetcher.randUserAgent();
  private LettProxy proxyToBeUsed = null;

  @Override
  protected Object fetch() {
    return Jsoup.parse(GETFetcher.fetchPageGET(session, session.getOriginalURL(), cookies, USER_AGENT, this.proxyToBeUsed, 1));
  }

  /**
   * Esse market possui um verificador de javascript Esse verificador é uma função que monta o cookie
   * e da um reload na página Com isso pegamos este script, trocamos o reload da página pelo retorno
   * do cookie Para isso usamos o ScriptEngineManager para rodar o código javascript.
   * 
   * Para acessar o site deve se usar o cookie pego aqui e usar o mesmo user agent.
   */
  @Override
  public void handleCookiesBeforeFetch() {
    Document doc = Jsoup.parse(GETFetcher.fetchPageGET(session, session.getOriginalURL(), cookies, USER_AGENT, null, 1));

    this.proxyToBeUsed = session.getRequestProxy(session.getOriginalURL());

    if (!isProductPage(doc)) {
      Element script = doc.select("script").first();

      if (script != null) {
        String eval = script.html().trim();

        if (eval.endsWith(";")) {
          int y = eval.indexOf(";}}") + 3;
          int x = eval.indexOf(';', y) + 1;

          String b = eval.substring(y, x);

          if (b.contains("(")) {
            int z = b.indexOf('(') + 1;
            int u = b.indexOf(')', z);

            String result = b.substring(z, u);

            eval = "var document = {};" + eval.replace(b, "") + " " + result + " = " + result + ".replace(\"location.reload();\", \"\"); " + b;
          }
        }

        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("js");
        try {
          String cookieString = engine.eval(eval).toString();
          if (cookieString != null && cookieString.contains("=")) {
            String cookieValues = cookieString.contains(";") ? cookieString.split(";")[0] : cookieString;

            String[] tokens = cookieValues.split("=");

            if (tokens.length > 1) {
              BasicClientCookie cookie = new BasicClientCookie(tokens[0], tokens[1]);
              cookie.setDomain("www.ibyte.com.br");
              cookie.setPath("/");
              this.cookies.add(cookie);
            }
          }

        } catch (ScriptException e) {
          Logging.printLogWarn(logger, session, CommonMethods.getStackTrace(e));
        }
      }
    }
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // ID interno
      String internalId = null;
      Element elementId = doc.select(".view-sku").first();
      if (elementId != null && elementId.text().contains(":")) {
        internalId = elementId.text().split(":")[1].replace(")", "").trim();
      }

      // Pid
      String internalPid = null;
      Element elementPid = doc.select("input[name=product]").first();
      if (elementPid != null) {
        internalPid = elementPid.attr("value").trim();
      }

      // Nome
      String name = null;
      Element elementName = doc.select(".product-name h1").first();
      if (elementName != null) {
        name = elementName.text().trim();
      }

      // Disponibilidade
      boolean available = false;
      Element elementBuyButton = doc.select(".back-in-stock-form.avise_me").first();
      if (elementBuyButton == null) {
        available = true;
      }

      // Preço
      Float price = null;
      if (available) {
        Element elementPrice = doc.select(".regular-price .price").first();
        Element specialPrice = doc.select(".special-price .price").first();

        if (specialPrice != null) {
          price = Float.parseFloat(specialPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
        } else if (elementPrice != null) {
          price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
        }
      }

      // Categoria
      String category1 = "";
      String category2 = "";
      String category3 = "";
      Elements elementCategories = doc.select(".breadcrumbs li a");
      for (int i = 1; i < elementCategories.size() - 1; i++) {
        String c = elementCategories.get(i).text().trim();
        if (category1.isEmpty()) {
          category1 = c;
        } else if (category2.isEmpty()) {
          category2 = c;
        } else if (category3.isEmpty()) {
          category3 = c;
        }
      }

      // Imagem primária
      String primaryImage = null;
      Element elementPrimaryImage = doc.select(".product-image .cloud-zoom").first();
      if (elementPrimaryImage != null) {
        primaryImage = elementPrimaryImage.attr("href");
      }

      // Imagens secundárias
      Elements elementImages = doc.select("#galeria div a");
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      for (Element image : elementImages) {
        if (!image.attr("href").equals(primaryImage)) {
          secondaryImagesArray.put(image.attr("href"));
        }
      }
      if (secondaryImagesArray.length() > 0) {
        secondaryImages = secondaryImagesArray.toString();
      }

      // Descrição
      String description = "";
      Element elementDescription = doc.select("#descricao").first();
      Element elementAdditionalContents = doc.select("#atributos").first();
      if (elementDescription != null) {
        description = description + elementDescription.html();
      }
      if (elementAdditionalContents != null) {
        description = description + elementAdditionalContents.html();
      }

      // Estoque
      Integer stock = null;

      // Marketplace
      Marketplace marketplace = new Marketplace();

      // Prices
      Prices prices = crawlPrices(doc, price);

      Product product = new Product();
      product.setUrl(this.session.getOriginalURL());
      product.setInternalId(internalId);
      product.setInternalPid(internalPid);
      product.setName(name);
      product.setPrice(price);
      product.setPrices(prices);
      product.setCategory1(category1);
      product.setCategory2(category2);
      product.setCategory3(category3);
      product.setPrimaryImage(primaryImage);
      product.setSecondaryImages(secondaryImages);
      product.setDescription(description);
      product.setStock(stock);
      product.setMarketplace(marketplace);
      product.setAvailable(available);

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
    }

    return products;
  }


  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();
    Map<Integer, Float> installmentPriceMap = new HashMap<>();

    if (price != null) {
      installmentPriceMap.put(1, price);

      Element boleto = doc.select(".boletoBox .price").first();

      if (boleto != null) {
        Float value = MathUtils.parseFloatWithComma(boleto.ownText());

        if (value != null) {
          prices.setBankTicketPrice(value);
        } else {
          prices.setBankTicketPrice(price);
        }
      } else {
        prices.setBankTicketPrice(price);
      }

      Elements installments = doc.select(".formas li #simularParcelamento tr");

      if (!installments.isEmpty()) {
        for (Element e : installments) {
          Elements values = e.select("td");

          if (values.size() > 1) {
            Integer installment = Integer.parseInt(values.first().ownText().replaceAll("[^0-9]", ""));
            Float value = Float.parseFloat(values.get(1).ownText().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", ".").trim());

            if (installment == 1) {
              value = MathUtils.normalizeTwoDecimalPlaces(value - (value * 0.07f));
            }

            installmentPriceMap.put(installment, value);
          }
        }
      } else {
        installments = doc.select(".product-shop .parcelaBloco div[class^=parcela-]");

        for (Element e : installments) {
          Element installmentElement = e.select(".parcela").last();
          Element valueElement = e.select(".price").last();

          if (valueElement != null && installmentElement != null) {
            String installment = installmentElement.ownText().replaceAll("[^0-9]", "").trim();
            Float value = MathUtils.parseFloatWithComma(valueElement.ownText());

            if (!installment.isEmpty() && value != null) {
              int installmentNumber = Integer.parseInt(installment);

              if (installmentNumber == 1) {
                value = MathUtils.normalizeTwoDecimalPlaces(value - (value * 0.07f));
              }

              installmentPriceMap.put(installmentNumber, value);
            }
          }
        }
      }

      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AURA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
    }

    return prices;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
    Element elementProduct = document.select(".view-sku").first();
    return (elementProduct != null);
  }
}
