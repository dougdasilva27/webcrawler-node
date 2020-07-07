package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilBiomundoCrawler extends CrawlerRankingKeywords {

    private static final String HOST_PAGE = "www.lojabiomundo.com.br";

    public BrasilBiomundoCrawler(Session session) {
        super(session);
    }

    @Override
    protected void extractProductsFromCurrentPage() {
        this.pageSize = 24;
        this.log("Página " + this.currentPage);

        String url = "https://www.lojabiomundo.com.br/pesquisa/?p=" + this.keywordEncoded + "&pagina=" + this.currentPage;

        this.log("Link onde são feitos os crawlers: " + url);
        this.currentDoc = fetchDocument(url);
        Elements products = this.currentDoc.select(".list-standart-products > li");

        if (!products.isEmpty()) {
            if (this.totalProducts == 0) {
                setTotalProducts();
            }

            for (Element product : products) {
                String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, "input[name=\"ProdutoId\"]", "value");
                String internalPId = internalId;
                String productUrl = CrawlerUtils.scrapUrl(product, ".product > a", "href", "https", HOST_PAGE);

                saveDataProduct(internalId, internalPId, productUrl);

                this.log(
                        "Position: " + this.position +
                                " - InternalId: " + internalId +
                                " - InternalPid: " + internalPId +
                                " - Url: " + productUrl
                );

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
        this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(this.currentDoc, ".results > span:last-child", true, 0);
        this.log("Total: " + this.totalProducts);
    }
}
