package br.com.lett.crawlernode.crawlers.corecontent.brasil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXCrawlersUtils;
import br.com.lett.crawlernode.crawlers.corecontent.extractionutils.VTEXOldScraper;
import br.com.lett.crawlernode.util.CrawlerUtils;
import br.com.lett.crawlernode.util.MathUtils;
import models.RatingsReviews;
import models.pricing.Pricing;

public class BrasilBrastempCrawler extends VTEXOldScraper {

   private static final String HOME_PAGE = "https://loja.brastemp.com.br/";
   private static final String MAIN_SELLER_NAME_LOWER = "brastemp";
   private static final String MAIN_SELLER_NAME_LOWER_2 = "consul";
   private static final String MAIN_SELLER_NAME_LOWER_3 = "whirlpool";
   private static final List<String> SELLERS = Arrays.asList(MAIN_SELLER_NAME_LOWER, MAIN_SELLER_NAME_LOWER_2, MAIN_SELLER_NAME_LOWER_3);


   public BrasilBrastempCrawler(Session session) {
      super(session);
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
   protected List<String> scrapSales(Document doc, JSONObject offerJson, String internalId, String internalPid, Pricing pricing) {
      String sale = CrawlerUtils.scrapStringSimpleInfo(doc, ".product__flags .price-flag", true);
      return sale != null && !sale.isEmpty() ? Arrays.asList(sale) : new ArrayList<>();
   }


   @Override
   protected Double scrapSpotlightPrice(Document doc, String internalId, Double principalPrice, JSONObject comertial, JSONObject discountsJson) {
      Double spotlightPrice = super.scrapSpotlightPrice(doc, internalId, principalPrice, comertial, discountsJson);
      Double maxDiscount = 0d;
      if (discountsJson != null && discountsJson.length() > 0) {
         for (String key : discountsJson.keySet()) {
            JSONObject paymentEffect = discountsJson.optJSONObject(key);
            Double discount = paymentEffect.optDouble("discount");

            if (discount > maxDiscount) {
               maxDiscount = discount;
            }
         }
      }

      if (maxDiscount > 0d) {
         spotlightPrice = MathUtils.normalizeTwoDecimalPlaces(spotlightPrice - (spotlightPrice * maxDiscount));
      }

      return spotlightPrice;
   }

   @Override
   protected String scrapDescription(Document doc, JSONObject productJson) {
      StringBuilder description = new StringBuilder();

      if (productJson.has("description")) {
         description.append("<div>");
         description.append(sanitizeDescription(productJson.get("description")));
         description.append("</div>");
      }

      List<String> specs = new ArrayList<>();

      if (productJson.has("Caracteristícas Técnicas")) {
         JSONArray keys = productJson.getJSONArray("Caracteristícas Técnicas");
         for (Object o : keys) {
            if (!o.toString().equalsIgnoreCase("Informações para Instalação") && !o.toString().equalsIgnoreCase("Portfólio")) {
               specs.add(o.toString());
            }
         }
      }

      List<Integer> modules = Arrays.asList(1, 2, 3, 4);


      for (int i : modules) {
         if (productJson.has("Texto modulo 0" + i)) {
            description.append("<div>");

            if (productJson.has("Título modulo 0" + i)) {
               description.append("<h4>");
               description.append(productJson.get("Título modulo 0" + i).toString().replace("[\"", "").replace("\"]", ""));
               description.append("</h4>");
            }

            description.append(sanitizeDescription(productJson.get("Texto modulo 0" + i)));
            description.append("</div>");
         }
      }

      for (String spec : specs) {
         if (productJson.has(spec)) {

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
            description.append(VTEXCrawlersUtils.sanitizeDescription(productJson.get(spec)) + (label.equals("Garantia") ? " meses" : ""));
            description.append("</div>");
         }
      }

      Element manual = doc.selectFirst(".value-field.Manual-do-Produto");
      if (manual != null) {

         description
               .append("<a href=\"" + manual.ownText() + "\" title=\"Baixar manual\" class=\"details__manual\" target=\"_blank\">Baixar manual</a>");
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

      return description.toString();
   }

   @Override
   protected RatingsReviews scrapRating(String internalId, String internalPid, Document doc, JSONObject jsonSku) {
      return null;
   }
}
