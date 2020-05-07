package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.Arrays;
import java.util.List;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import br.com.lett.crawlernode.core.models.RatingReviewsCollection;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXOldScraper;
import models.RatingsReviews;

public class BrasilCompracertaCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://www.compracerta.com.br/";
   private static final List<String> SELLERS = Arrays.asList("Compra certa", "Compracerta", "Whirlpool", "Consul", "Brastemp");

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
      return !doc.select(".productName").isEmpty() && !producReference.endsWith("_out");
   }

   protected String crawlProductReference(Document doc) {
      String producReference = "";
      Element prod = doc.select(".skuReference").first();

      if (prod != null) {
         producReference = prod.ownText().trim();
      }

      return producReference;
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      RatingReviewsCollection ratingCollection = new TrustvoxRatingCrawler(session, "1756", null).extractRatingAndReviewsForVtex(doc, dataFetcher);
      return ratingCollection.getRatingReviews(internalId);
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) {
      StringBuilder description = new StringBuilder();

      Element especificDescriptionTitle = doc.selectFirst("#especificacoes > h2");
      if (especificDescriptionTitle != null) {
         description.append(especificDescriptionTitle.html());
      }

      if (productJson.has("RealHeight")) {
         description.append("<table cellspacing=\"0\" class=\"Height\">\n").append("<tbody>").append("<tr>").append("<th>Largura").append("</th>")
               .append("<td>").append("\n" + productJson.get("RealHeight").toString().replace(".0", "") + " cm").append("</td>").append("</tbody>")
               .append("</table>");
      }

      if (productJson.has("RealWidth")) {
         description.append("<table cellspacing=\"0\" class=\"Width\">\n").append("<tbody>").append("<tr>").append("<th>Altura").append("</th>")
               .append("<td>").append("\n" + productJson.get("RealWidth").toString().replace(".0", "") + " cm").append("</td>").append("</tbody>")
               .append("</table>");
      }

      if (productJson.has("RealLength")) {
         description.append("<table cellspacing=\"0\" class=\"Length\">\n").append("<tbody>").append("<tr>").append("<th>Profundidade").append("</th>")
               .append("<td>").append("\n" + productJson.get("RealLength").toString().replace(".0", "") + " cm").append("</td>").append("</tbody>")
               .append("</table>");
      }

      if (productJson.has("RealWeightKg")) {
         description.append("<table cellspacing=\"0\" class=\"WeightKg\">\n").append("<tbody>").append("<tr>").append("<th>Peso").append("</th>")
               .append("<td>").append("\n" + productJson.get("RealWeightKg").toString().replace(".0", "") + " kg").append("</td>").append("</tbody>")
               .append("</table>");
      }


      Element caracteristicas = doc.select("#caracteristicas").first();

      if (caracteristicas != null) {
         Element caracTemp = caracteristicas.clone();
         caracTemp.select(".group.Prateleira").remove();

         Elements nameFields = caracteristicas.select(".name-field, h4");
         for (Element e : nameFields) {
            String classString = e.attr("class");

            if (classString.toLowerCase().contains("modulo") || classString.toLowerCase().contains("foto")) {
               caracTemp.select("th." + classString.trim().replace(" ", ".")).remove();
            }
         }

         caracTemp.select("h4.group, .Galeria, .Video, .Manual-do-Produto, h4.Arquivos").remove();
         description.append(caracTemp.html());

      }

      return description.toString();
   }
}
