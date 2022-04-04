package br.com.lett.crawlernode.crawlers.extractionutils.ranking;

import br.com.lett.crawlernode.core.models.RankingProduct;
import br.com.lett.crawlernode.core.models.RankingProductBuilder;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.exceptions.MalformedProductException;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class RipleyCrawlerRanking extends CrawlerRankingKeywords {

    private final String homePage = session.getOptions().optString("homePage");

    public RipleyCrawlerRanking(Session session) {
        super(session);
    }

    @Override
    protected void extractProductsFromCurrentPage() throws MalformedProductException {
        this.pageSize = 48;

        this.log("Página " + this.currentPage);

        String url = homePage + "/search/" + this.keywordWithoutAccents.replace(" ", "%20") + "?page=" + this.currentPage;
        this.log("Link onde são feitos os crawlers: " + url);

        this.currentDoc = fetchDocument(url);

        Elements products = this.currentDoc.select("div.catalog-product-item");

        if (!products.isEmpty()) {
            if (this.totalProducts == 0) {
                setTotalProducts();
            }
            for (Element e : products) {
                String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a.catalog-product-item", "id");
                String productUrl = homePage + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "a.catalog-product-item", "href");
                String imageUrl = "https:" + CrawlerUtils.scrapStringSimpleInfoByAttribute(e, "img[alt=Image]", "data-src");
                String name = CrawlerUtils.scrapStringSimpleInfo(e, "div.catalog-product-details__name", true);
                Integer price = CrawlerUtils.scrapPriceInCentsFromHtml(e, "li.catalog-prices__offer-price.catalog-prices__highest", null, true, '.', session, 0);
                boolean isAvailable = price != 0;

                RankingProduct objProducts = RankingProductBuilder.create()
                        .setUrl(productUrl)
                        .setImageUrl(imageUrl)
                        .setName(name)
                        .setPriceInCents(price)
                        .setAvailability(isAvailable)
                        .build();

                saveDataProduct(objProducts);

                if (this.arrayProducts.size() == productsLimit) {
                    break;
                }

            }
        } else {
            this.result = false;
            this.log("Keyword sem resultado!");
        }

        this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
    }

    @Override
    protected void setTotalProducts() {
        Element total = this.currentDoc.selectFirst("div.catalog-page__results-text div.col-xs-12.col-md-9.col-lg-9");

        if (total != null) {
            String text = total.ownText().replaceAll("[^0-9]", "");

            if (!text.isEmpty()) {
                this.totalProducts = Integer.parseInt(text);
                this.log("Total da busca: " + this.totalProducts);
            }
        }
    }
}
