package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BrasilB2WLojistasCrawler extends CrawlerRankingKeywords {

   private String store = getStore();
   private String homePage = getHomePage();


   public String getStore() {
      return session.getOptions().optString("store");
   }

   public String getHomePage() {
      return "www." + store + ".com.br";
   }

   public BrasilB2WLojistasCrawler(Session session) {
      super(session);
   }

   @Override
   protected Document fetchDocument(String url) {

      this.log("Link onde são feitos os crawlers: " + url);

      Map<String, String> headers = new HashMap<>();

      headers.put("authority", "www.americanas.com.br");
      headers.put("sec-ch-ua", " \" Not A;Brand\";v=\"99\", \"Chromium\";v=\"90\", \"Google Chrome\";v=\"90\"");
      headers.put("sec-ch-ua-mobile", "?0");
      headers.put("upgrade-insecure-requests", "1");
      headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,/;q=0.8,application/signed-exchange;v=b3;q=0.9");
      headers.put("sec-fetch-site", "none");
      headers.put("sec-fetch-mode", "navigate");
      headers.put("sec-fetch-user", "?1");
      headers.put("sec-fetch-dest", "document");
      headers.put("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.212 Safari/537.36");
      headers.put("accept-language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6");

      return Jsoup.parse(br.com.lett.crawlernode.crawlers.corecontent.saopaulo.SaopauloAmericanasCrawler.fetchPage(url, this.dataFetcher, this.cookies, headers, session));
   }

   @Override
   protected void extractProductsFromCurrentPage() throws MalformedProductException {
      this.pageSize = 24;
      this.log("Página " + this.currentPage);

      String url = getUrl();

      this.log("Link onde são feitos os crawlers: " + url);
      this.currentDoc = fetchDocument(url);

      Elements products = this.currentDoc.select(".product-grid-item");

      if (!products.isEmpty()) {
         for (Element e : products) {
            String internalPid = scraperInternalPid(e);
            String productUrl = CrawlerUtils.completeUrl("produto/" + internalPid, "https", homePage);
            String name = CrawlerUtils.scrapStringSimpleInfo(e, "[class^=\"TitleUI\"]", true);
            Integer price = CrawlerUtils.scrapIntegerFromHtml(e, "[class^=\"PriceUI\"]", true, 0);
            String imageUrl = scraperImage(internalPid, name);
            boolean isAvailable = price != null;

            RankingProduct productRanking = RankingProductBuilder.create()
               .setUrl(productUrl)
               .setInternalId(null)
               .setInternalPid(internalPid)
               .setImageUrl(imageUrl)
               .setName(name)
               .setPriceInCents(price)
               .setAvailability(isAvailable)
               .build();

            saveDataProduct(productRanking);

            if (this.arrayProducts.size() == productsLimit) {
               break;
            }
         }

      } else {
         this.result = false;
         this.log("Keyword sem resultado!");
      }

      this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora "
         + this.arrayProducts.size() + " produtos crawleados");
   }

   protected String getUrl() {
      String url = "";
      String keyword = this.keywordWithoutAccents.replace(" ", "-");

      if (this.currentPage == 1) {
         url = "https://www." + store + ".com.br/lojista/" + session.getOptions().optString("lojista") + "?ordenacao=relevance&conteudo=" + keyword;
      } else {
         url = "https://www." + store + ".com.br/lojista/" + session.getOptions().optString("lojista") + "/pagina-" + this.currentPage + "?conteudo=" + keyword + "&ordenacao=relevance";
      }
      return url;
   }


   @Override
   protected boolean hasNextPage() {
      return !this.currentDoc.select(".pagination-product-grid.pagination li span .svg-icon.svg-icon-right.svg-icon-sm").isEmpty();
   }

   private String scraperImage(String internalPid, String name) {
      String slug = name.toLowerCase(Locale.ROOT).replace(" ", "-");
      return "https://images-" + store + ".b2w.io/produtos/" + internalPid + "/imagens/" + slug + "/" + internalPid + "_1_large.jpg";
   }

   private String scraperInternalPid(Element e) {
      String internalPid = "";
      String pid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "[class^=\"RippleContainer\"] a", "href");

      if (pid != null) {
         String regex = "produto\\/([0-9]+)";
         Pattern pattern = Pattern.compile(regex);
         Matcher matcher = pattern.matcher(pid);

         if (matcher.find()) {
            internalPid = matcher.group(1);
         }
      }
      return internalPid;
   }
}
