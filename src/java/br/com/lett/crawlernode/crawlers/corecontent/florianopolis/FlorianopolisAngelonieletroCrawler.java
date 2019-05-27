package br.com.lett.crawlernode.crawlers.corecontent.florianopolis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.http.HttpHeaders;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.ratingandreviews.extractionutils.AngelonieletroUtils;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * 
 * This crawler uses WebDriver to detect when we have sku variations on the same page.
 * 
 * Sku price crawling notes: 1) The payment options can change between different card brands. 2) The
 * ecommerce share the same cad payment options between sku variations. 3) The cash price (preço a
 * vista) can change between sku variations, and it's crawled from the main page. 4) To get card
 * payments, first we perform a POST request to get the list of all card brands, then we perform one
 * POST request for each card brand.
 * 
 * org.openqa.selenium.StaleElementReferenceException:
 * http://www.angeloni.com.br/eletro/p/lavadora-de-roupas-brastemp-11kg-ative-bwl11a-branco-2344440
 * normal: http://www.angeloni.com.br/eletro/p/lava-e-seca-lg-85kg-wd1485at-aco-escovado-3868980
 * 
 * @author Samir Leao
 *
 */

public class FlorianopolisAngelonieletroCrawler extends Crawler {

  public FlorianopolisAngelonieletroCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    boolean shouldVisit = false;
    shouldVisit = !FILTERS.matcher(href).matches()
        && (href.startsWith("http://www.angeloni.com.br/eletro/") || href.startsWith("https://www.angeloni.com.br/eletro/"));

    return shouldVisit;
  }


  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, "Product page identified: " + this.session.getOriginalURL());

      String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#titulo[data-productid]", "data-productid");
      String mainId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#titulo[data-sku]", "data-sku");

      Document voltageAPi = AngelonieletroUtils.fetchVoltageApi(internalPid, mainId, session, cookies, dataFetcher);
      Elements voltageVariation = voltageAPi.select("#formGroupVoltage input[name=voltagem]");

      if (!voltageVariation.isEmpty()) {
        for (Element e : voltageVariation) {
          Product p = crawlProduct(AngelonieletroUtils.fetchSkuHtml(doc, e, mainId, session, cookies, dataFetcher));
          String variationName = CrawlerUtils.scrapStringSimpleInfo(voltageAPi, "label[for=" + e.attr("id") + "]", true);

          if (variationName != null && !p.getName().toLowerCase().contains(variationName.toLowerCase())) {
            p.setName(p.getName() + " " + variationName);
          }
          products.add(p);
        }
      } else {
        products.add(crawlProduct(doc));
      }

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document doc) {
    return !doc.select("#titulo").isEmpty();
  }

  private Product crawlProduct(Document doc) {
    String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#titulo[data-productid]", "data-productid");
    String internalId = AngelonieletroUtils.crawlInternalId(doc);
    String name = crawlName(doc);
    Float price = crawlPrice(doc);
    Prices prices = crawlPrices(doc, internalPid);
    boolean available = crawlAvailability(doc);
    String primaryImage = CrawlerUtils.scrapSimplePrimaryImage(doc, "#imagem-grande > div[data-zoom-image]", Arrays.asList("data-zoom-image"),
        "https:", "dy3cxdqdg9dx0.cloudfront.net");
    String secondaryImages = crawlSecondaryImages(doc, primaryImage);
    Integer stock = null;
    String description = crawlDescription(doc);
    CategoryCollection categories = CrawlerUtils.crawlCategories(doc, ".breadcrumb li:not(:first-child) a span");

    List<String> eans = crawlEan(doc);

    return ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
        .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
        .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
        .setStock(stock).setMarketplace(new Marketplace()).setEans(eans).build();
  }

  /**
   * The payment options are the same across variations. So the same prices object e set to all the
   * crawled products.
   * 
   * To crawl the card payment options we must request for the list of all card brands that can be
   * used. The, for each card brand we must request for the payment options. They can change between
   * card brands.
   *
   * @param document
   * @return
   */
  private Prices crawlPrices(Document document, String internalPid) {
    Prices prices = new Prices();

    boolean isAvailable = crawlAvailability(document);

    if (isAvailable) {

      Float price = crawlPrice(document);
      prices.setBankTicketPrice(price);

      Map<String, Map<Integer, Float>> cardsInstallments = crawlCardInstallmentsMap(document, internalPid);
      for (Entry<String, Map<Integer, Float>> entry : cardsInstallments.entrySet()) {
        prices.insertCardInstallment(entry.getKey(), entry.getValue());
      }
    }

    return prices;
  }

  /**
   * 
   * @param document
   */
  private Map<String, Map<Integer, Float>> crawlCardInstallmentsMap(Document document, String internalPid) {
    Map<String, Map<Integer, Float>> cardInstallmentsMap = new HashMap<>();

    Set<Card> cards = crawlSetOfCards(internalPid);
    Float price = crawlPrice(document);

    for (Card card : cards) {
      String compatibleCardName = createCompatibleName(card);
      if (compatibleCardName != null) {

        StringBuilder url = new StringBuilder();
        url.append("https://www.angeloni.com.br/eletro/modais/installmentsRender.jsp").append("?");
        url.append("cardTypeKey=").append(compatibleCardName);
        url.append("&totalValue=").append(price);
        url.append("&useTheBestInstallment=false");

        Request request = RequestBuilder.create().setUrl(url.toString()).setCookies(cookies).build();
        Document response = Jsoup.parse(this.dataFetcher.get(session, request).getBody());
        Map<Integer, Float> installments = crawlInstallmentsFromPaymentRequestResponse(response);
        cardInstallmentsMap.put(card.toString(), installments);
      }
    }

    return cardInstallmentsMap;
  }

  /**
   * 
   * Get all the installments numbers and values from the content of the POST request to payment
   * methods on a certain card brand.
   * 
   * e.g:
   * 
   * Nº de parcelas á vista 2 vezes sem juros 3 vezes sem juros 4 vezes sem juros 5 vezes sem juros
   *
   * Valor de cada parcela R$ 439,90 R$ 219,95 R$ 146,63 R$ 109,98 R$ 87,98
   *
   * @param document
   * @return
   */
  private Map<Integer, Float> crawlInstallmentsFromPaymentRequestResponse(Document document) {
    Map<Integer, Float> installments = new HashMap<>();

    Elements installmentNumberTextElements = document.select("div.numero-parcelas ul li");
    Elements installmentPriceTextElements = document.select("div.valor-parcelas ul li");

    if (installmentNumberTextElements.size() == installmentPriceTextElements.size()) {
      for (int i = 0; i < installmentNumberTextElements.size(); i++) {
        String installmentNumberText = installmentNumberTextElements.get(i).text();
        String installmentPriceText = installmentPriceTextElements.get(i).text();

        List<String> parsedNumbers = MathUtils.parseNumbers(installmentNumberText);
        if (parsedNumbers.size() == 0) {
          installments.put(1, MathUtils.parseFloatWithComma(installmentPriceText));
        } else {
          installments.put(Integer.parseInt(parsedNumbers.get(0)), MathUtils.parseFloatWithComma(installmentPriceText));
        }
      }
    }

    return installments;
  }

  private String createCompatibleName(Card card) {
    String compatibleName = null;

    if (card == Card.AMEX) {
      compatibleName = "americanExpress";
    } else if (card == Card.MASTERCARD) {
      compatibleName = "masterCard";
    } else if (card == Card.DINERS) {
      compatibleName = "dinersClub";
    } else
      compatibleName = card.toString();

    return compatibleName;
  }

  private Set<Card> crawlSetOfCards(String internalId) {
    Set<Card> cards = new HashSet<>();

    Map<String, String> headers = new HashMap<>();
    headers.put(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded");

    Request request = RequestBuilder.create().setUrl("https://www.angeloni.com.br/eletro/modais/paymentMethods.jsp").setCookies(cookies)
        .setHeaders(headers).setPayload("productId=" + internalId).build();
    Document response = Jsoup.parse(this.dataFetcher.post(session, request).getBody());

    Elements cardsElements = response.select("div.box-cartao h2");

    for (Element card : cardsElements) {
      String text = card.text().trim().toLowerCase();
      if (text.contains(Card.DINERS.toString())) {
        cards.add(Card.DINERS);
      } else if (text.contains(Card.MASTERCARD.toString())) {
        cards.add(Card.MASTERCARD);
      } else if (text.contains(Card.VISA.toString())) {
        cards.add(Card.VISA);
      } else if (text.contains("americanexpress")) {
        cards.add(Card.AMEX);
      } else if (text.contains(Card.HIPERCARD.toString())) {
        cards.add(Card.HIPERCARD);
      } else if (text.contains("angeloni")) {
        cards.add(Card.SHOP_CARD);
      }
    }

    return cards;
  }

  private String crawlName(Document document) {
    String name = null;

    Element elementName = document.select("#titulo [itemprop=name]").first();
    if (elementName != null) {
      name = elementName.text();
    }

    return name;
  }

  private Float crawlPrice(Document document) {
    Float price = null;

    // Element elementPrice = document.select("div#descricao .esquerda .valores .preco-por
    // .microFormatoProduto").first();
    Element elementPrice = document.select("div#descricao .esquerda .valores .parcelamento span:not(:first-child)").first();

    if (elementPrice != null) {
      price = MathUtils.parseFloatWithComma(elementPrice.text());
    }

    return price;
  }

  private boolean crawlAvailability(Document document) {
    boolean available = true;

    Element elementAvailable = document.select("div#descricao .esquerda .produto-esgotado").first();
    if (elementAvailable != null) {
      available = false;
    }

    return available;
  }

  private String crawlSecondaryImages(Document document, String primaryImage) {
    String secondaryImages = null;

    Elements elementsSecondaryImages = document.select("#galeria .thumbImage:not(.active) div[onclick]");
    JSONArray secondaryImagesArray = new JSONArray();

    for (Element e : elementsSecondaryImages) {
      String onclick = e.attr("onclick");

      if (onclick.contains("'//")) {
        int x = onclick.indexOf("('") + 2;
        int y = onclick.indexOf("',", x);

        String image = CrawlerUtils.completeUrl(onclick.substring(x, y), "https:", "dy3cxdqdg9dx0.cloudfront.net");
        if (!image.equalsIgnoreCase(primaryImage)) {
          secondaryImagesArray.put(image);
        }
      }
    }


    if (secondaryImagesArray.length() > 0) {
      secondaryImages = secondaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private String crawlDescription(Document document) {
    String description = null;
    Elements elementsDescription = document.select("section#abas .tab-content div[role=tabpanel]:not([id=tab-avaliacoes-clientes])");
    description = elementsDescription.html();

    return description;
  }

  private List<String> crawlEan(Document doc) {
    String ean = null;
    Elements elmnts = doc.select(".tab-content .tab-pane .caracteristicas tbody tr");

    for (Element e : elmnts) {
      String aux = e.text();

      if (aux.contains("EAN")) {
        aux = aux.replaceAll("[^0-9]+", "");

        if (!aux.isEmpty()) {
          ean = aux;
        }
      }
    }

    return Arrays.asList(ean);
  }
}


