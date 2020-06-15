package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilCasaevideoCrawler extends CrawlerRankingKeywords {

    public BrasilCasaevideoCrawler(Session session) {
        super(session);
    }

    @Override
    protected void extractProductsFromCurrentPage() {
        this.pageSize = 24;
        this.log("Página " + this.currentPage);

        String url = "https://busca.casaevideo.com.br/busca?q=" + this.keywordEncoded + "&page=" + this.currentPage;
        this.log("Link onde são feitos os crawlers: " + url);

        this.currentDoc = fetchDocument(url);

        Elements products = this.currentDoc.select("#nm-product-");

        if (!products.isEmpty()) {
            if (this.totalProducts == 0) {
                setTotalProducts();
            }

            for (Element product : products) {
                String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "div[data-trustvox-product-code]", "data-trustvox-product-code");
                String productUrl = scrapProductUrl(product);

                saveDataProduct(null, internalPid, productUrl);

                this.log("Position: " + this.position +
                        " - InternalId: " + null +
                        " - InternalPid: " + internalPid +
                        " - Url: " + productUrl);

                if (this.arrayProducts.size() == productsLimit)
                    break;
            }
        } else {
            this.result = false;
            this.log("Keyword sem resultado!");
        }
        this.log("Finalizando Crawler de produtos da página " + this.currentPage +
                " - até agora " + this.arrayProducts.size() + " produtos crawleados");
    }

    @Override
    protected void setTotalProducts() {
        this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".neemu-total-products-container", true, 0);
        this.log("Total da busca: " + this.totalProducts);
    }

    private String scrapProductUrl(Element product) {
        String protocol = "https";
        String baseUrl = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".nm-product-img-link", "href");
        return (baseUrl != null) ? protocol + ":" + baseUrl : null;
    }
}
