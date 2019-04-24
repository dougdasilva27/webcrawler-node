package br.com.lett.crawlernode.crawlers.corecontent.ribeiraopreto;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

public class RibeiraopretoSavegnagoCrawler extends Crawler {

  /*
   * Ribeirão Preto - 1 Sertãozinho - 6 Jardinópolis - 11 Jaboticabal - 7 Franca - 3 Barretos - 10
   * Bebedouro - 9 Monte Alto - 12 Araraquara - 4 São carlos - 5 Matão - 8
   */

  private static final String HOME_PAGE = "https://www.savegnago.com.br/";

  public RibeiraopretoSavegnagoCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public String handleURLBeforeFetch(String curURL) {

    if (curURL.endsWith("/p")) {
      try {
        String url = curURL;
        List<NameValuePair> paramsOriginal = URLEncodedUtils.parse(new URI(url), "UTF-8");
        List<NameValuePair> paramsNew = new ArrayList<>();

        for (NameValuePair param : paramsOriginal) {
          if (!param.getName().equals("sc")) {
            paramsNew.add(param);
          }
        }

        paramsNew.add(new BasicNameValuePair("sc", "2"));
        URIBuilder builder = new URIBuilder(curURL.split("\\?")[0]);

        builder.clearParameters();
        builder.setParameters(paramsNew);

        return builder.build().toString();

      } catch (URISyntaxException e) {
        return curURL;
      }
    }

    return curURL;

  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (isProductPage(doc)) {
      Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

      // ID interno
      String internalId = null;
      Element elementInternalId = doc.select(".productReference").first();
      if (elementInternalId != null) {
        internalId = elementInternalId.text().trim();
      }

      // Pid
      String internalPid = internalId;

      // Nome
      String name = null;
      Element elementName = doc.select(".fn.productName").first();
      if (elementName != null) {
        name = elementName.text().trim();
      }

      // Price[DEBUG] -> [MSG]Crawled information:

      Float price = null;
      Element elementPrice = doc.select(".skuBestPrice").first();
      if (elementPrice != null) {
        price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
      }

      // Disponibilidade
      boolean available = true;
      if (price == null) {
        available = false;
      }

      // Categorias
      String category1 = "";
      String category2 = "";
      String category3 = "";
      Elements elementCategories = doc.select(".bread-crumb li a");

      if (elementCategories.size() > 1) {
        for (int i = 1; i < elementCategories.size(); i++) {// start with index 1 because the first
                                                            // item is the home page
          if (category1.isEmpty()) {
            category1 = elementCategories.get(i).text();
          } else if (category2.isEmpty()) {
            category2 = elementCategories.get(i).text();
          } else if (category3.isEmpty()) {
            category3 = elementCategories.get(i).text();
          }
        }
      }

      // Imagens
      String primaryImage = null;
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();
      Element elementPrimaryImage = doc.select("#image-main").first();

      if (elementPrimaryImage != null) {
        primaryImage = elementPrimaryImage.attr("src").trim();
      }
      if (secondaryImagesArray.length() > 0) {
        secondaryImages = secondaryImagesArray.toString();
      }

      // Descrição
      String description = "";
      Element elementDescription = doc.select(".productDescriptionWrap").first();
      Element elementSpecification = doc.select(".productSpecificationWrap").first();
      if (elementDescription != null) {
        description = description + elementDescription.html();
      }
      if (elementSpecification != null) {
        description = description + elementSpecification.html();
      }

      description += CrawlerUtils.scrapLettHtml(internalId, session, session.getMarket().getNumber());

      // Estoque
      Integer stock = null;

      // Marketplace
      Marketplace marketplace = new Marketplace();

      // Prices
      Prices prices = crawlPrices(price, doc);

      // Ean
      JSONArray arrayEan = CrawlerUtils.scrapEanFromVTEX(doc);
      String ean = 0 < arrayEan.length() ? arrayEan.getString(0) : null;

      List<String> eans = new ArrayList<>();
      eans.add(ean);

      Product product = new Product();

      product.setUrl(session.getOriginalURL());
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
      product.setEans(eans);

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }


  /*******************************
   * Product page identification *
   *******************************/

  private boolean isProductPage(Document document) {
    return document.select("#___rc-p-sku-ids").first() != null;
  }

  /**
   * In this market, installments not appear in product page
   * 
   * @param doc
   * @param price
   * @return
   */
  private Prices crawlPrices(Float price, Document doc) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      Element priceFrom = doc.select(".skuListPrice").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
      }

      installmentPriceMap.put(1, price);

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.CABAL.toString(), installmentPriceMap);
    }

    return prices;
  }
}
