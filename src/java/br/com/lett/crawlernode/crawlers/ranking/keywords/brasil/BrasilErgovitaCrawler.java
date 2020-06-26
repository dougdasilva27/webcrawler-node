package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CrawlerUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class BrasilErgovitaCrawler extends CrawlerRankingKeywords {
    public BrasilErgovitaCrawler(Session session) {
        super(session);
    }

    private static final String HOME_PAGE = "www.ergovita.com.br";

    @Override
    protected void extractProductsFromCurrentPage() {
        pageSize = 24;
        log("Página " + currentPage);

        String url = "https://www.ergovita.com.br/listaprodutos.asp?digitada=true&texto=" + this.keywordEncoded + "&Pag=" + this.currentPage;
        log("Link onde são feitos os crawlers: " + url);

        currentDoc = fetchDocument(url);

        Elements products = currentDoc.select(".DivListProd");

        if (!products.isEmpty()) {
            if (totalProducts == 0) {
                setTotalProducts();
            }
            for (Element product : products) {
                String internalId = scrapId(product);
                String internalPid = internalId;
                String productUrl = CrawlerUtils.scrapUrl(product, ".DivProdListDetails > a", "href", "https://", HOME_PAGE);
                saveDataProduct(null, null, productUrl);

                log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
                if (arrayProducts.size() == productsLimit) {
                    break;
                }
            }
        } else {
            result = false;
            log("Keyword sem resultado!");
        }
        log("Finalizando Crawler de produtos da página $currentPage até agora ${arrayProducts.size} produtos crawleados");
    }

    private String scrapId(Element product) {
        String fullTextWithId = CrawlerUtils.scrapStringSimpleInfoByAttribute(product, ".DivListProd", "id");
        return fullTextWithId.replace("DivProd", "");
    }

    @Override
    protected void setTotalProducts() {
        this.totalProducts = CrawlerUtils.scrapIntegerFromHtml(currentDoc, "#idFoundFC b", true, 0);
        this.log("Total de produtos: " + this.totalProducts);
    }
}
