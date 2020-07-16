package br.com.lett.crawlernode.crawlers.ranking.keywords.saopaulo;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SaopauloEmporioquatroestrelasCrawler extends CrawlerRankingKeywords {

    private static final String BASE_URL = "www.emporioquatroestrelas.com.br";

    public SaopauloEmporioquatroestrelasCrawler(Session session) {
        super(session);
    }

    @Override
    protected void extractProductsFromCurrentPage() {
        this.pageSize = 40;

        this.log("Página " + this.currentPage);

        String url = "https://www.emporioquatroestrelas.com.br/buscapagina?ft=" + this.keywordEncoded + "&PS=40&sl=9d655531-1c4a-4ead-98ab-dcbd746907bf&cc=40&sm=0&PageNumber=" + this.currentPage;
        this.log("Link onde são feitos os crawlers: " + url);

        this.currentDoc = fetchDocument(url);

        Elements products = this.currentDoc.select("div.data");

        if (!products.isEmpty()) {
            if (this.totalProducts == 0) {
                setTotalProducts();
            }

            for (Element product : products) {
                String internalId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".data","data-sku-id");
                String internalPid = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".data","data-id");
                String productUrl = CrawlerUtils.scrapUrl(product, ".productName a", "href", "https:", BASE_URL);

                saveDataProduct(internalId, internalPid, productUrl);

                this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);

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
        String url = "https://www.emporioquatroestrelas.com.br/"+this.keywordEncoded;
        Document document = fetchDocument(url);
        this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(document, "span.value", true, 0);
        this.log("Total: " + this.totalProducts);
    }
}
