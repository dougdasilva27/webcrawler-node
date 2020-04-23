
package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import com.google.common.collect.Sets;
import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.CategoryCollection;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.TrustvoxRatingCrawler;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXScraper;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VtexConfig;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VtexConfig.CardsInfo;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VtexConfig.VtexConfigBuilder;
import br.com.lett.crawlernode.util.CrawlerUtils;
import models.RatingsReviews;

public class BrasilConsulCrawler extends VTEXScraper {

   private static final String HOME_PAGE = "https://loja.consul.com.br/";
   private static final List<String> SELLERS = Arrays.asList("Whirlpool", "Consul", "Brastemp");
   private Set<String> cards = Sets.newHashSet(Card.VISA.toString(), Card.MASTERCARD.toString(), Card.AMEX.toString());

   public BrasilConsulCrawler(Session session) {
      super(session);
   }

   @Override
   public boolean shouldVisit() {
      String href = session.getOriginalURL().toLowerCase();
      return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
   }

   @Override
   public List<Product> extractInformation(Document doc) throws Exception {
      super.extractInformation(doc);

      Integer aVistaDiscount = CrawlerUtils.scrapIntegerFromHtml(doc, ".prod-info .prod-selos p[class^=\"flag btp--desconto\"]", true, 0);
      List<CardsInfo> cardsInfo = setListOfCards(cards, new HashMap<Integer, Integer>(1, aVistaDiscount));
      VtexConfig vtexConfig = VtexConfigBuilder.create()
            .setBankDiscount(aVistaDiscount)
            .setMainSellerNames(SELLERS)
            .setHomePage(HOME_PAGE)
            .setUsePriceAPI(true)
            .setCards(cardsInfo)
            .setSalesIsCalculated(true)
            .build();

      return extractVtexInformation(doc, vtexConfig);
   }

   @Override
   protected CategoryCollection scrapCategories(Document doc, String internalId) {
      CategoryCollection categories = new CategoryCollection();

      Element category = doc.selectFirst(".bread-crumb .last a");
      if (category != null) {
         categories.add(category.ownText().trim());
      }

      return categories;
   }

   /**
    * @param document
    * @param apiJSON
    * @return
    */
   protected String scrapDescription(Document document, JSONObject apiJSON, JSONObject skuJson, JSONObject productJson, String internalId) {
      StringBuilder description = new StringBuilder();

      JSONObject descriptionJson = crawlCatalogAPI(internalId, "skuId", HOME_PAGE);

      if (descriptionJson.has("description")) {
         description.append("<div>");
         description.append(sanitizeDescription(descriptionJson.get("description")));
         description.append("</div>");
      }

      List<String> specs = new ArrayList<>();

      if (descriptionJson.has("Caracteristícas Técnicas")) {
         JSONArray keys = descriptionJson.getJSONArray("Caracteristícas Técnicas");
         for (Object o : keys) {
            if (!o.toString().equalsIgnoreCase("Informações para Instalação") && !o.toString().equalsIgnoreCase("Portfólio")) {
               specs.add(o.toString());
            }
         }
      }

      for (String spec : specs) {
         if (descriptionJson.has(spec)) {

            String label = spec;

            if (spec.equals("Tipo do produto")) {
               label = "Tipo";
            } else if (spec.equalsIgnoreCase("Garantia do Fornecedor (mês)")) {
               label = "Garantia";
            } else if (spec.equalsIgnoreCase("Mais Informações")) {
               label = "Informações";
            }

            description.append("<div>");
            description.append("<h4>").append(label).append("</h4>");
            description.append(VTEXCrawlersUtils.sanitizeDescription(descriptionJson.get(spec)) + (label.equals("Garantia") ? " meses" : ""));
            description.append("</div>");
         }
      }

      Element manual = document.selectFirst(".value-field.Manual-do-Produto");
      if (manual != null) {

         description
               .append("<a href=\"" + manual.ownText() + "\" title=\"Baixar manual\" class=\"details__manual\" target=\"_blank\">Baixar manual</a>");
      }

      if (apiJSON.has("RealHeight")) {
         description.append("<table cellspacing=\"0\" class=\"Height\">\n").append("<tbody>").append("<tr>").append("<th>Largura").append("</th>")
               .append("<td>").append("\n" + apiJSON.get("RealHeight").toString().replace(".0", "") + " cm").append("</td>").append("</tbody>")
               .append("</table>");
      }

      if (apiJSON.has("RealWidth")) {
         description.append("<table cellspacing=\"0\" class=\"Width\">\n").append("<tbody>").append("<tr>").append("<th>Altura").append("</th>")
               .append("<td>").append("\n" + apiJSON.get("RealWidth").toString().replace(".0", "") + " cm").append("</td>").append("</tbody>")
               .append("</table>");
      }

      if (apiJSON.has("RealLength")) {
         description.append("<table cellspacing=\"0\" class=\"Length\">\n").append("<tbody>").append("<tr>").append("<th>Profundidade").append("</th>")
               .append("<td>").append("\n" + apiJSON.get("RealLength").toString().replace(".0", "") + " cm").append("</td>").append("</tbody>")
               .append("</table>");
      }

      if (apiJSON.has("RealWeightKg")) {
         description.append("<table cellspacing=\"0\" class=\"WeightKg\">\n").append("<tbody>").append("<tr>").append("<th>Peso").append("</th>")
               .append("<td>").append("\n" + apiJSON.get("RealWeightKg").toString().replace(".0", "") + " kg").append("</td>").append("</tbody>")
               .append("</table>");
      }

      return description.toString();
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject apiJson) {
      TrustvoxRatingCrawler trustVox = new TrustvoxRatingCrawler(session, "3531", logger);
      return trustVox.extractRatingAndReviewsForVtex(doc, dataFetcher).getRatingReviews(internalId);
   }
}
