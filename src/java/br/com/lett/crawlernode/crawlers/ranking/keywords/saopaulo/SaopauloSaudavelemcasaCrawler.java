package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SaopauloSaudavelemcasaCrawler extends CrawlerRankingKeywords {

    private static final String BASE_URL = "www.saudavelemcasa.com.br";
    public SaopauloSaudavelemcasaCrawler(Session session) {
        super(session);
    }

    @Override
    protected void extractProductsFromCurrentPage() {
        this.log("Página " + this.currentPage);

        this.pageSize = 12;
        String url = "https://www.saudavelemcasa.com.br/product/getproducts/?path=%2Fbusca&viewList=g&pageNumber=" + this.currentPage + "&pageSize=12&keyWord=" + this.keywordEncoded;

        this.log("Link onde são feitos os crawlers: " + url);
        this.currentDoc = fetchDocument(url, cookies);

        Elements products = this.currentDoc.select(".cards > div");

        if (!products.isEmpty()) {
            if (this.totalProducts == 0)
                setTotalProducts();

            for (Element product : products) {
                String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "meta", "content");
                String internalPid = internalId;
                String productUrl = CrawlerUtils.scrapUrl(product, ".content > a", "href", "https", BASE_URL);

                saveDataProduct(internalId, internalPid, productUrl);

                this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
                if (this.arrayProducts.size() == productsLimit)
                    break;
            }
        } else {
            this.result = false;
            this.log("Keyword sem resultado!");
        }

        this.log("Finalizando Crawler de produtos da página " + this.currentPage + " - até agora " + this.arrayProducts.size() + " produtos crawleados");
    }

    @Override
    protected boolean hasNextPage() {
        int totalNumberOfPages = this.currentDoc.select(".btnPageNumber").size();
        return this.currentPage < totalNumberOfPages;
    }
}
