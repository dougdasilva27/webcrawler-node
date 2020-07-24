package br.com.lett.crawlernode.crawlers.corecontent.saopaulo;

import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.RatingsReviews;
import models.prices.Prices;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaopauloDrogaraiaCrawler extends Crawler {

  public SaopauloDrogaraiaCrawler(Session session) {
    super(session);
  }

  @Override
  public boolean shouldVisit() {
    String href = this.session.getOriginalURL().toLowerCase();
    String HOME_PAGE = "http://www.drogaraia.com.br/";
    return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
  }

  @Override
  public List<Product> extractInformation(Document doc) throws Exception {
    super.extractInformation(doc);
    List<Product> products = new ArrayList<>();

    if (doc.selectFirst(".product-view") != null) {
      Logging.printLogDebug(
          logger, session, "Product page identified: " + this.session.getOriginalURL());

      String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(doc, "#sku", "value");

      String internalPid = null;
      Element elementInternalPid = doc.select("input[name=product]").first();
      if (elementInternalPid != null) {
        internalPid = elementInternalPid.attr("value").trim();
      }

      boolean available = doc.selectFirst(".add-to-cart-buttons") != null;

      String name = crawlName(doc, available);

      Float price = null;
      Element elementPrice = doc.select(".product-shop .regular-price").first();
      if (elementPrice == null) {
        elementPrice = doc.select(".product-shop .price-box .special-price .price").first();
      }
      if (elementPrice != null) {
        price = Float.parseFloat(elementPrice
            .text()
            .replaceAll("[^0-9,]+", "")
            .replaceAll("\\.", "")
            .replaceAll(",", "."));
      }

      List<String> cat = doc.select(".breadcrumbs ul li:not(.home):not(.product) a").eachText();
      String description = scrapDescription(doc);
      String primaryImage = crawlPrimaryImage(doc);
      String secondaryImages = crawlSecondaryImages(doc, primaryImage);
      Integer stock = null;
      Marketplace marketplace = new Marketplace();
      Prices prices = crawlPrices(doc, price);
      String ean = scrapEan(doc);
      RatingsReviews ratingReviews = crawRating(doc, internalId);
      List<String> eans = new ArrayList<>();
      eans.add(ean);

      Product product = ProductBuilder.create()
          .setUrl(session.getOriginalURL())
          .setInternalId(internalId)
          .setInternalPid(internalPid)
          .setName(name)
          .setPrice(price)
          .setPrices(prices)
          .setCategories(cat)
          .setPrimaryImage(primaryImage)
          .setSecondaryImages(secondaryImages)
          .setDescription(description)
          .setStock(stock)
          .setMarketplace(marketplace)
          .setAvailable(available)
          .setRatingReviews(ratingReviews)
          .setEans(eans)
          .build();

      products.add(product);

    } else {
      Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
    }

    return products;
  }

  private String crawlName(Document doc, boolean available) {
    StringBuilder name = new StringBuilder();

    if (available) {
      Element firstName = doc.selectFirst(".product-name h1");
      if (firstName != null) {
        name.append(firstName.text());

        Elements attributes = doc.select(".product-attributes .show-hover");
        for (Element e : attributes) {
          name.append(" ").append(e.ownText().trim());
        }
      }
      return name.toString().replace("  ", " ").trim();
    } else {
      return doc.selectFirst(".product-image-gallery > img").attr("title");
    }
  }

  private String crawlPrimaryImage(Document doc) {
    String primaryImage = null;

    Element elementPrimaryImage = doc.select(".product-image-gallery img.gallery-image").first();
    if (elementPrimaryImage != null) {
      primaryImage = elementPrimaryImage.attr("data-zoom-image");
    }

    return primaryImage;
  }

  private String crawlSecondaryImages(Document doc, String primaryImage) {
    String secondaryImages = null;
    JSONArray secundaryImagesArray = new JSONArray();
    Elements elementImages = doc.select(".product-image-gallery img.gallery-image");

    for (Element e : elementImages) {
      String image = e.attr("data-zoom-image").trim();

      if (!isPrimaryImage(image, primaryImage)) {
        secundaryImagesArray.put(image);
      }
    }

    if (secundaryImagesArray.length() > 0) {
      secondaryImages = secundaryImagesArray.toString();
    }

    return secondaryImages;
  }

  private boolean isPrimaryImage(String image, String primaryImage) {
    if (primaryImage == null) {
      return false;
    }

    String x = CommonMethods.getLast(image.split("/"));
    String y = CommonMethods.getLast(primaryImage.split("/"));

    return x.equals(y);
  }

  private Prices crawlPrices(Document doc, Float price) {
    Prices prices = new Prices();

    if (price != null) {
      Map<Integer, Float> installmentPriceMap = new HashMap<>();

      installmentPriceMap.put(1, price);
      prices.setBankTicketPrice(price);

      Element priceFrom = doc.select(".old-price span[id]").first();
      if (priceFrom != null) {
        prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
      }

      prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.HIPERCARD.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
      prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
    }

    return prices;
  }

  private String scrapEan(Element e) {
    String ean = null;
    Elements trElements = e.select(".farmaBox .data-table tr");

    if (trElements != null && !trElements.isEmpty()) {
      for (Element tr : trElements) {
        if (tr.text().contains("EAN")) {
          Element td = tr.selectFirst("td");
          ean = td != null ? td.text().trim() : null;
        }
      }
    }

    return ean;
  }

  private String scrapDescription(Document doc) {
    StringBuilder description = new StringBuilder();

    Element shortDescription = doc.selectFirst(".product-short-description");
    if (shortDescription != null) {
      description.append(shortDescription.html().trim());
    }

    Element elementDescription = doc.selectFirst("#details");
    if (elementDescription != null) {
      description.append(elementDescription.html().trim());
    }

    Elements iframes = doc.select(".product-essential iframe");
    for (Element iframe : iframes) {
      String url = iframe.attr("src");
      if (!url.contains("youtube")) {
        description.append(
            Jsoup.parse(
                this.dataFetcher
                    .get(
                        session,
                        RequestBuilder.create().setUrl(url).setCookies(cookies).build())
                    .getBody())
                .html());
      }
    }

    return description.toString();
  }

  private RatingsReviews crawRating(Document doc, String internalId) {
    TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "71450", logger);
    return trustVox.extractRatingAndReviews(internalId, doc, dataFetcher);
  }
}
