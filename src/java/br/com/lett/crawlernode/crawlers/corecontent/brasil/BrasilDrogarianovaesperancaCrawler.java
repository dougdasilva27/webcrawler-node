package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.fetcher.FetchMode;
import br.com.lett.crawlernode.core.fetcher.ProxyCollection;
import br.com.lett.crawlernode.core.fetcher.methods.ApacheDataFetcher;
import br.com.lett.crawlernode.core.fetcher.models.FetcherOptions.FetcherOptionsBuilder;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.fetcher.models.Request.RequestBuilder;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.models.ProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.Logging;
import br.com.lett.crawlernode.util.MathUtils;
import models.Marketplace;
import models.prices.Prices;

/**
 * Date: 08/08/2017
 * 
 * @author Gabriel Dornelas
 *
 */
public class BrasilDrogarianovaesperancaCrawler extends Crawler {

   private final String HOME_PAGE = "https://www.drogarianovaesperanca.com.br";

   public BrasilDrogarianovaesperancaCrawler(Session session) {
      super(session);
      super.config.setFetcher(FetchMode.FETCHER);
   }

   @Override
   protected Document fetch() {
      return Jsoup.parse(fetchPage(session.getOriginalURL(), session));
   }

   public String fetchPage(String url, Session session) {
      Request request = RequestBuilder.create()
            .setUrl(url)
            .setCookies(this.cookies)
            .setFetcheroptions(
                  FetcherOptionsBuilder.create()
                        .mustUseMovingAverage(false)
                        .mustRetrieveStatistics(true)
                        .build()
            ).setProxyservice(
                  Arrays.asList(
                        ProxyCollection.INFATICA_RESIDENTIAL_BR,
                        ProxyCollection.STORM_RESIDENTIAL_EU,
                        ProxyCollection.STORM_RESIDENTIAL_US
                  )
            ).build();

      String content = this.dataFetcher.get(session, request).getBody();

      if (content == null || content.isEmpty()) {
         content = new ApacheDataFetcher().get(session, request).getBody();
      }

      return content;
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

      if (isProductPage(doc)) {
         Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

         String internalId = crawlInternalId(doc);
         String internalPid = null;
         String name = crawlName(doc);
         Float price = CrawlerUtils.scrapFloatPriceFromHtml(doc, "span.preco-final span.valor, span.preco-por span.valor", null, true, ',', session);
         Prices prices = crawlPrices(price, internalId, doc);
         boolean available = crawlAvailability(doc);
         CategoryCollection categories = crawlCategories(doc);
         String primaryImage = crawlPrimaryImage(doc);
         String secondaryImages = CrawlerUtils.scrapSimpleSecondaryImages(doc,
               "#thumbs-produto div a",
               Arrays.asList("rel"), "https", "www.drogarianovaesperanca.com.br/", primaryImage);
         String description = crawlDescription(doc);
         Integer stock = null;
         Marketplace marketplace = crawlMarketplace();

         String ean = crawlEan(doc);
         List<String> eans = new ArrayList<>();
         eans.add(ean);

         // Creating the product
         Product product = ProductBuilder.create().setUrl(session.getOriginalURL()).setInternalId(internalId).setInternalPid(internalPid).setName(name)
               .setPrice(price).setPrices(prices).setAvailable(available).setCategory1(categories.getCategory(0)).setCategory2(categories.getCategory(1))
               .setCategory3(categories.getCategory(2)).setPrimaryImage(primaryImage).setSecondaryImages(secondaryImages).setDescription(description)
               .setStock(stock).setMarketplace(marketplace).setEans(eans).build();

         products.add(product);

      } else {
         Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
      }

      return products;

   }

   private boolean isProductPage(Document doc) {
      if (doc.select("#ID_SubProduto").first() != null) {
         return true;
      }
      return false;
   }

   private String crawlInternalId(Document doc) {
      String internalId = null;

      Element internalIdElement = doc.select("#ID_SubProduto").first();
      if (internalIdElement != null) {
         internalId = internalIdElement.val();
      }

      return internalId;
   }

   private String crawlName(Document document) {
      String name = null;
      Element nameElement = document.select(".produto-detalhes h1").first();

      if (nameElement != null) {
         name = nameElement.text().trim();
      }

      return name;
   }

   private Float crawlPrice(Document document) {
      Float price = null;

      String priceText = null;
      Element salePriceElement = document.selectFirst("span.preco-final span.valor");

      if (salePriceElement != null) {
         priceText = salePriceElement.text();
         price = MathUtils.parseFloatWithComma(priceText);
      }

      return price;
   }

   private Marketplace crawlMarketplace() {
      return new Marketplace();
   }

   private String crawlPrimaryImage(Document doc) {
      String primaryImage = null;
      Element elementPrimaryImage = doc.select("#imgProduto").first();

      if (elementPrimaryImage != null) {
         primaryImage = elementPrimaryImage.attr("src");
      }

      return primaryImage;
   }

   /**
    * In the time when this crawler was made, this market hasn't secondary Images
    * 
    * @param doc
    * @return
    */


   /**
    * @param document
    * @return
    */
   private CategoryCollection crawlCategories(Document document) {
      CategoryCollection categories = new CategoryCollection();
      Elements elementCategories = document.select("span[property=itemListElement] > a span");

      for (int i = 1; i < elementCategories.size(); i++) {
         String cat = elementCategories.get(i).ownText().replace("/", "").trim();

         if (!cat.isEmpty()) {
            categories.add(cat);
         }
      }

      return categories;
   }

   private String crawlDescription(Document doc) {
      StringBuilder description = new StringBuilder();

      Element elementDescription = doc.select(".ficha-produto").first();

      if (elementDescription != null) {
         description.append(elementDescription.html());
      }

      Element elementInfo = doc.select(".tabs-produto #tabs").first();

      if (elementInfo != null) {
         description.append(elementInfo.html());
      }

      Element aviso = doc.select(".aviso-medicamento").first();

      if (aviso != null) {
         description.append(aviso.html());
      }

      return description.toString();
   }

   private boolean crawlAvailability(Document doc) {
      return doc.select("#BtComprarProduto").first() != null;
   }

   /**
    * 
    * @param doc
    * @param price
    * @return
    */
   private Prices crawlPrices(Float price, String internalId, Document doc) {
      Prices prices = new Prices();

      if (price != null) {
         Element priceFrom = doc.select(".preco-de").first();
         if (priceFrom != null) {
            prices.setPriceFrom(MathUtils.parseDoubleWithComma(priceFrom.text()));
         }

         String pricesUrl = "https://www.drogarianovaesperanca.com.br/Funcoes_Ajax.aspx/CarregaFormaPagamento";
         String payload = "{\"ID_OpcaoPagamento\":0,\"ID_FormaPagamento\":0,\"ID_SubProduto\":\"" + internalId + "\"}";

         Map<String, String> headers = new HashMap<>();
         headers.put("content-type", "application/json");

         Request request = RequestBuilder.create()
               .setUrl(pricesUrl)
               .setCookies(this.cookies)
               .setHeaders(headers)
               .setPayload(payload)
               .setFetcheroptions(
                     FetcherOptionsBuilder.create()
                           .mustUseMovingAverage(false)
                           .mustRetrieveStatistics(true)
                           .build()
               ).setProxyservice(
                     Arrays.asList(
                           ProxyCollection.INFATICA_RESIDENTIAL_BR,
                           ProxyCollection.STORM_RESIDENTIAL_EU,
                           ProxyCollection.STORM_RESIDENTIAL_US
                     )
               ).build();

         JSONObject pricesJson = CrawlerUtils.stringToJson(this.dataFetcher.post(session, request).getBody());

         if (pricesJson.has("d")) {
            JSONArray cards = pricesJson.getJSONArray("d");

            for (int i = 0; i < cards.length() - 1; i++) {
               JSONObject card = cards.getJSONObject(i);
               String cardName = crawlCardName(card);

               if (cardName != null && card.has("Itens")) {
                  setInstallments(cardName, card, prices);
               }
            }

         }


      }

      return prices;
   }

   private String crawlCardName(JSONObject card) {
      String officalCardName = null;

      if (card.has("Icon")) {
         String cardName = card.getString("Icon").trim();

         switch (cardName) {
            case "visa":
               officalCardName = Card.VISA.toString();
               break;
            case "mastercard":
               officalCardName = Card.MASTERCARD.toString();
               break;
            case "diners":
               officalCardName = Card.DINERS.toString();
               break;
            case "american-express":
               officalCardName = Card.AMEX.toString();
               break;
            case "elo":
               officalCardName = Card.ELO.toString();
               break;
            case "boleto-bancario":
               officalCardName = "boleto";
               break;
            default:
               break;
         }
      }

      return officalCardName;
   }

   private void setInstallments(String cardName, JSONObject card, Prices prices) {
      JSONArray installments = card.getJSONArray("Itens");

      if (cardName.equals("boleto") && installments.length() > 0) {
         prices.setBankTicketPrice(installments.getJSONObject(0).getDouble("TotalGeral"));
      } else {
         Map<Integer, Float> installmentPriceMap = new HashMap<>();

         for (int i = 0; i < installments.length(); i++) {
            JSONObject installmentJson = installments.getJSONObject(i);

            if (installmentJson.has("Nparcel")) {
               Integer installment = installmentJson.getInt("Nparcel");

               if (installmentJson.has("TotalParcela")) {
                  String parcelText = installmentJson.getString("TotalParcela").trim();
                  Float value = parcelText.isEmpty() ? null : MathUtils.parseFloatWithComma(parcelText);

                  if (value != null) {
                     installmentPriceMap.put(installment, value);
                  }
               }
            }
         }

         prices.insertCardInstallment(cardName, installmentPriceMap);
      }
   }

   private String crawlEan(Document doc) {
      String ean = null;
      Elements elmnts = doc.select(".ficha-produto ul li div ul li");

      for (Element e : elmnts) {
         String aux = e.text();

         if (aux.contains("Código EAN")) {
            ean = aux.replaceAll("[^0-9]+", "");
            break;
         }
      }

      return ean;
   }
}
