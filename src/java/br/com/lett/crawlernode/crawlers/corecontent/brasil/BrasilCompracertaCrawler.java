package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.fetcher.models.Request;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.extractionutils.core.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.crawlers.extractionutils.core.VTEXNewScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.JSONUtils;
import models.RatingsReviews;

public class BrasilCompracertaCrawler extends VTEXNewScraper {

   private static final String HOME_PAGE = "https://www.compracerta.com.br/";
   private static final List<String> SELLERS = Arrays.asList("Compra certa", "Compracerta", "Whirlpool", "Consul", "Brastemp");
   private static final String API_TOKEN = "c3073c1616a463d2149576e841169c9ee031c9a76a7eb41723427097bd10ae3a";

   public BrasilCompracertaCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = this.session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   protected String getHomePage() {
      return HOME_PAGE;
   }

   @Override
   protected List<String> getMainSellersNames() {
      return SELLERS;
   }

   @Override
   protected boolean isProductPage(Document doc) {
      String producReference = crawlProductReference(doc).toLowerCase();
      return !producReference.isEmpty() && !producReference.endsWith("_out");
   }

   protected String crawlProductReference(Document doc) {
      String producReference = "";
      Element prod = doc.select(".vtex-product-identifier-0-x-product-identifier__value").first();

      if (prod != null) {
         producReference = prod.ownText().trim();
      }

      return producReference;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return new TrustvoxRatingCrawler(session, "1756", null).extractRatingAndReviews(internalId, doc, this.dataFetcher);
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) throws UnsupportedEncodingException {
      String description = "";

      JSONArray items = productJson.optJSONArray("items");
      String id = null;
      if (items != null) {
         id = "{\"sku\":\"" + ((JSONObject) items.get(0)).optString("itemId") + "\"}";
      }

      assert id != null;
      String encodedString = Base64.getEncoder().encodeToString(id.getBytes());

      String api = "https://www.compracerta.com.br/_v/public/graphql/v1?workspace=master&maxAge=long&appsEtag=remove&domain=store&locale=pt-BR&__bindingId=dcb4b5dd-4083-47f9-b2de-da54656f88b0&operationName=ProductSku&extensions=";

      String query = "{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"" + API_TOKEN + "\",\"sender\":\"compracerta.product-details@0.x\",\"provider\":\"compracerta.store-graphql@0.x\"},\"variables\":\"" + encodedString + "\"}";

      String encodedQuery = URLEncoder.encode(query, "UTF-8");

      Request request = Request.RequestBuilder.create().setUrl(api + encodedQuery)
            .build();
      JSONObject response = JSONUtils.stringToJson(this.dataFetcher.get(session, request).getBody());

      if (response.has("data")) {
         JSONArray productArray = response.optJSONObject("data").optJSONArray("productSku");
         JSONObject productDetail = null;

         if (!productArray.isEmpty()) {
            productDetail = ((JSONObject) productArray.get(0));
         }

         if (productDetail != null) {
            description += productDetail.optString("RealHeight") + "\nAltura\n";
            description += productDetail.optString("RealWidth") + "\nLargura\n";
            description += productDetail.optString("RealLength") + "\nComprimento\n";
            description += productDetail.optString("RealWeightKg") + "\nPeso\n";
         }
      }

      description += CrawlerUtils.scrapSimpleDescription(doc, Arrays.asList(".product-dimensions-box", ".product-infos-tabs"));
      return description;

   }
}
