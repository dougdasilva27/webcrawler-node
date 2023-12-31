package br.com.lett.crawlernode.crawlers.corecontent.argentina;

import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CommonMethods;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import br.com.lett.crawlernode.util.Logging;
import com.google.common.collect.Sets;
import exceptions.MalformedPricingException;
import exceptions.OfferException;
import models.Offer.OfferBuilder;
import models.Offers;
import models.pricing.CreditCard.CreditCardBuilder;
import models.pricing.CreditCards;
import models.pricing.Installment.InstallmentBuilder;
import models.pricing.Installments;
import models.pricing.Pricing;
import models.pricing.Pricing.PricingBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.*;

public class ArgentinaDiscoCrawler extends Crawler {

   private static final String HOME_PAGE = "https://www.disco.com.ar/";
   private static final String IMAGE_FIRST_PART = HOME_PAGE + "DiscoComprasArchivos/Archivos/ArchivosMxM/";
   private static final String SELLER_FULL_NAME = "Disco";
   protected Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(),
         Card.AURA.toString(), Card.DINERS.toString(), Card.HIPER.toString(), Card.AMEX.toString());

   public ArgentinaDiscoCrawler(Session session) {
      super(session);
   }

   @Override
   public void handleCookiesBeforeFetch() {
      Logging.printLogDebug(logger, session, "Adding cookie...");
      this.cookies = CrawlerUtils.fetchCookiesFromAPage(HOME_PAGE + "Login/PreHome.aspx", Collections.singletonList("ASP.NET_SessionId"), "www.disco.com.ar", "/",
            cookies, session, new HashMap<>(), dataFetcher);

      BasicClientCookie cookie = new BasicClientCookie("noLocalizar", "true");
      cookie.setDomain("www.disco.com.ar");
      cookie.setPath("/");
      this.cookies.add(cookie);
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

      JSONObject apiJson = crawlProductApi(doc);

      if (isProductPage(apiJson)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalPid = crawlInternalPid(apiJson);
         String internalId = crawlInternalId(apiJson);
         String name = crawlName(apiJson);
         Integer stock = crawlStock(apiJson);
         CategoryCollection categories = new CategoryCollection();
         String primaryImage = crawlPrimaryImage(apiJson);
         String secondaryImages = crawlSecondaryImages();
         String description = crawlDescription(internalId);
         boolean availableToBuy = apiJson.opt("Precio") != null;
         Offers offers = availableToBuy ? scrapOffer(apiJson) : new Offers();

         // Creating the product
         Product product = ProductBuilder.create()
               .setUrl(crawlNewUrl(internalId))
               .setInternalId(internalId)
               .setInternalPid(internalPid)
               .setName(name)
               .setCategory1(categories.getCategory(0))
               .setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2))
               .setPrimaryImage(primaryImage)
               .setSecondaryImages(secondaryImages)
               .setDescription(description)
               .setStock(stock)
               .setOffers(offers)
               .build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(JSONObject jsonSku) {
      return jsonSku.has("IdArticulo");
   }

   private String crawlInternalPid(JSONObject json) {
      String internalPid = null;

      if (json.has("Codigo")) {
         internalPid = json.get("Codigo").toString();
      }

      return internalPid;
   }

   private String crawlInternalId(JSONObject json) {
      String internalId = null;

      if (json.has("IdArticulo")) {
         internalId = json.get("IdArticulo").toString();
      }

      return internalId;
   }

   private String crawlName(JSONObject json) {
      String name = null;

      if (json.has("DescripcionArticulo")) {
         name = json.getString("DescripcionArticulo");
      }

      return name;
   }

   private Integer crawlStock(JSONObject json) {
      int stock = 0;

      if (json.has("Stock")) {
         String text = json.get("Stock").toString().replaceAll("[^0-9.]", "");

         if (!text.isEmpty()) {
            stock = ((Double) Double.parseDouble(text)).intValue();
         }
      }

      return stock;
   }

   private String crawlPrimaryImage(JSONObject json) {
      String primaryImage = null;

      if (json.has("IdArchivoBig")) {
         String image = json.getString("IdArchivoBig").trim();

         if (!image.isEmpty()) {
            primaryImage = IMAGE_FIRST_PART + image;
         }
      }

      if ((primaryImage == null || primaryImage.isEmpty()) && json.has("IdArchivoSmall")) {
         String image = json.getString("IdArchivoSmall").trim();

         if (!image.isEmpty()) {
            primaryImage = IMAGE_FIRST_PART + image;
         }
      }

      if (primaryImage != null && primaryImage.isEmpty()) {
         primaryImage = null;
      }

      return primaryImage;
   }

   /**
    * There is no secondary Images in this market.
    *
    * @param
    * @return
    */
   private String crawlSecondaryImages() {
      String secondaryImages = null;
      JSONArray secondaryImagesArray = new JSONArray();

      if (secondaryImagesArray.length() > 0) {
         secondaryImages = secondaryImagesArray.toString();
      }

      return secondaryImages;
   }

   private String crawlDescription(String pid) {
      StringBuilder description = new StringBuilder();
      String url = HOME_PAGE + "Comprar/HomeService.aspx/ObtenerDetalleDelArticuloLevex";
      String payload = "{code:'" + pid + "'}";

      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");

      Request request = RequestBuilder.create().setUrl(url).setPayload(payload).setCookies(cookies).setHeaders(headers).build();
      String response = this.dataFetcher.post(session, request).getBody();

      if (response != null && response.contains("descr")) {
         JSONObject json = CrawlerUtils.stringToJson(response);

         if (json.has("d")) {
            JSONObject jsonD = CrawlerUtils.stringToJson(json.get("d").toString());
            JSONObject especificaciones = jsonD.optJSONObject("especificaciones");

            if (especificaciones != null ){
               description.append("<div id=\"especificaciones\">");
               for (String key : especificaciones.keySet()) {
                  description.append(
                          "<div class=\"columna\">"
                                  + "<div class=\"caracteristica  text-left\">" + key + "</div> "
                                  + "<div class=\"valor-caracteristica  text-right\">" + especificaciones.get(key) + "</div>"
                                  + "</div>"
                  );
               }
            }
         }
      }

      return description.toString();
   }

   private String crawlNewUrl(String internalId) {
      String url = session.getOriginalURL();

      if (!url.contains("prod/")) {
         url = HOME_PAGE + "prod/" + internalId;
      }

      return url;
   }

   private JSONObject crawlProductApi(Document doc) {
      JSONObject json = new JSONObject();

      Element e = doc.selectFirst("#hfProductData");
      if (e != null) {
         json = CrawlerUtils.stringToJson(e.val());
      } else if (session.getOriginalURL().contains("_query=")) {
         json = crawlProductOldApi(session.getOriginalURL());
      }
      return json;
   }

   /**
    * Crawl api of search when probably has only one product
    *
    * @param url
    * @return
    */
   private JSONObject crawlProductOldApi(String url) {
      Map<String, String> headers = new HashMap<>();
      headers.put("Content-Type", "application/json");

      // Nome do produto na busca
      String[] tokens = url.split("=");

      String urlSearch = HOME_PAGE + "Comprar/HomeService.aspx/ObtenerArticulosPorDescripcionMarcaFamiliaLevex";
      String payload = "{IdMenu:\"\",textoBusqueda:\"" + CommonMethods.removeAccents(tokens[tokens.length - 1]) + "\","
            + " producto:\"\", marca:\"\", pager:\"\", ordenamiento:0, precioDesde:\"\", precioHasta:\"\"}";

      Request request = RequestBuilder.create().setUrl(urlSearch).setPayload(payload).setCookies(cookies).setHeaders(headers).build();
      JSONObject json = CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());
      if (json.has("d")) {
         JSONObject jsonD = CrawlerUtils.stringToJson(json.get("d").toString());
         if (jsonD.has("ResultadosBusquedaLevex")) {
            JSONArray products = jsonD.getJSONArray("ResultadosBusquedaLevex");

            if (products.length() > 0) {
               json = products.getJSONObject(0);
            }
         }
      }
      return json;
   }

   private Offers scrapOffer(JSONObject json) throws MalformedPricingException, OfferException {
      Offers offers = new Offers();
      List<String> sales = scrapSales(json);
      Pricing pricing = scrapPricing(json);

      offers.add(OfferBuilder.create()
            .setUseSlugNameAsInternalSellerId(true)
            .setSellerFullName(SELLER_FULL_NAME)
            .setMainPagePosition(1)
            .setIsBuybox(false)
            .setIsMainRetailer(true)
            .setPricing(pricing)
            .setSales(sales)
            .build());

      return offers;
   }

   private Pricing scrapPricing(JSONObject json) throws MalformedPricingException {
      Double spotlightPrice = JSONUtils.getDoubleValueFromJSON(json, "Precio", true);
      CreditCards creditCards = scrapCreditCards(spotlightPrice);

      return PricingBuilder.create()
            .setPriceFrom(null)
            .setSpotlightPrice(spotlightPrice)
            .setCreditCards(creditCards)
            .build();
   }

   private List<String> scrapSales(JSONObject json) {
      List<String> sales = new ArrayList<>();

      JSONArray descuentos = JSONUtils.getJSONArrayValue(json, "Descuentos");
      if (descuentos.optJSONObject(0) != null) {
         String firstSales = descuentos.getJSONObject(0).optString("Subtipo");

         /*
          * We have to getJSONObject(0) because the JSONArray descuentos count a list of promotions but we
          * only need the first one which is the promotion that appears on the website
          */

         if (firstSales != null && !firstSales.isEmpty()) {
            sales.add(firstSales);
         }
      }

      return sales;
   }

   private CreditCards scrapCreditCards(Double spotlightPrice) throws MalformedPricingException {
      CreditCards creditCards = new CreditCards();
      Installments installments = scrapInstallment(spotlightPrice);

      for (String card : cards) {
         creditCards.add(CreditCardBuilder.create()
               .setBrand(card)
               .setInstallments(installments)
               .setIsShopCard(false)
               .build());
      }

      return creditCards;

   }

   private Installments scrapInstallment(Double spotlightPrice) throws MalformedPricingException {

      Installments installments = new Installments();

      installments.add(InstallmentBuilder.create()
            .setInstallmentNumber(1)
            .setInstallmentPrice(spotlightPrice)
            .build());

      return installments;
   }
}
