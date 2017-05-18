package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilLojasredeCrawler extends CrawlerRankingKeywords{

	public BrasilLojasredeCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		this.log("Página "+ this.currentPage);
		
		//número de produtos por página do market
		this.pageSize = 24;
		
		//monta a url com a keyword e a página
		String url = "http://www.lojasrede.com.br/busca?busca="+this.keywordEncoded+"&pagina="+this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
		
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select("div.spot");
		
		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalProducts == 0) setTotalProducts();
			
			for(Element e: products) {
				//seta e monta os ids
				String[] tokens 	= e.attr("id").split("-");
				String internalPid 	= tokens[tokens.length-1].trim();
				
				//monta o InternalId
				Element inid 	 = e.select("> div > a > div > img").first();
				String[] tokens2 = inid.attr("data-original").split("/");
				String temp 	 = tokens2[tokens2.length-2].trim();
				
				if(temp.contains("-")) {
					int x = temp.indexOf("-");
					temp  = temp.substring(0, x).trim();
				}
				
				String internalId = temp;
				
				//monta url
				Element eUrl = e.select("> div > a").first();
				String productUrl  = "http://www.lojasrede.com.br"+eUrl.attr("href");
				
				saveDataProduct(internalId, internalPid, productUrl);
				
				this.log("Position: " + this.position + " - InternalId: " + internalId + " - InternalPid: " + internalPid + " - Url: " + productUrl);
				if(this.arrayProducts.size() == productsLimit) break;
				
			}
		} else {
			this.result = false;
			this.log("Keyword sem resultado!");
		}
		
		this.log("Finalizando Crawler de produtos da página "+this.currentPage+" - até agora "+this.arrayProducts.size()+" produtos crawleados");
		
	}

	@Override
	protected boolean hasNextPage() {
		Elements page = this.currentDoc.select("div.spot");
		//se  elemeno page obtiver algum resultado
		if(page.size() >= this.pageSize)
		{
			//tem próxima página
			return true;
		}
		else
		{
			//não tem próxima página
			return false;
		}
	}
	
	@Override
	protected void setTotalProducts()
	{
		Element totalElement = this.currentDoc.select("ul.filtroFilho.Assinatura-Recorrente label.item span span.qtde").first();
		
		try
		{
			if(totalElement != null) this.totalProducts = Integer.parseInt(totalElement.text().replaceAll("\\(", "").replaceAll("\\)", "").trim());
		}
		catch(Exception e)
		{
			this.logError(e.getMessage());
		}
		
		this.log("Total da busca: "+this.totalProducts);
	}

}
