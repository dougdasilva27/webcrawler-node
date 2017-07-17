package br.com.lett.crawlernode.crawlers.ranking.keywords.riodejaneiro;

import java.io.BufferedWriter;
import java.io.FileWriter;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;
import br.com.lett.crawlernode.util.CommonMethods;

public class RiodejaneiroZonasulCrawler extends CrawlerRankingKeywords{

	public RiodejaneiroZonasulCrawler(Session session) {
		super(session);
	}
	
	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 15;
	
		// Neste market precisa de um código da busca e um código da keyword.
		if(this.currentPage == 1){
			//monta a url com a keyword e a página
			String url = "https://www.zonasul.com.br/WebForms/Lista-Facetada.aspx?BuscaPor="+this.keywordEncoded;
			
			//chama função de pegar a url
			this.currentDoc = fetchDocument(url);
		} else {
			//monta a url com o código da keyword e a página
			String url = session.getRedirectedToURL(session.getOriginalURL()) + "&Pagina="+this.currentPage;
			this.log("Link onde são feitos os crawlers: "+url);
			
			//chama função de pegar a url
			this.currentDoc = fetchDocument(url);
			
			try {
				BufferedWriter out = new BufferedWriter(new FileWriter("/home/gabriel/Desktop/zonasul.html"));
				
				out.write(currentDoc.toString());
				out.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	
		this.log("Página "+this.currentPage);
		
		Elements products =  this.currentDoc.select("div.prod_info a[href]");

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) {
				setTotalProducts();
			}
			
			for(Element e: products) {
				// Url do produto
				String productUrl = crawlProductUrl(e);

				// InternalPid
				String internalPid = crawlInternalPid(e);

				// InternalId
				String internalId = crawlInternalId(e);

				saveDataProduct(internalId, internalPid, productUrl);;

				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) {
					break;
				}
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}
	
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
	}

	@Override
	protected boolean hasNextPage() {
		if(this.arrayProducts.size() < this.totalProducts){
			//tem próxima página
			return true;
		} 
			
		return false;
	}
	
	@Override
	protected void setTotalProducts() {
		Element totalElement = this.currentDoc.select("div.result").first();
		
		if(totalElement != null) {
			try {
				String token = totalElement.ownText();
				int x = token.indexOf("de");
				
				this.totalProducts = Integer.parseInt(token.substring(x).replaceAll("[^0-9]", "").trim());
			} catch(Exception e) {
				this.logError(CommonMethods.getStackTrace(e));
			}
			
			this.log("Total da busca: "+this.totalProducts);
		}	
	}

	private String crawlInternalId(Element e){
		String internalId = null;

		String[] tokens = e.attr("href").split("--");
		internalId = tokens[tokens.length-1];

		return internalId;
	}

	private String crawlInternalPid(Element e){
		String internalPid = null;		
		
		String[] tokens = e.attr("href").split("--");
		internalPid = tokens[tokens.length-1];
		
		return internalPid;
	}

	private String crawlProductUrl(Element e){
		String urlProduct = e.attr("href");

		return urlProduct;
	}
}
