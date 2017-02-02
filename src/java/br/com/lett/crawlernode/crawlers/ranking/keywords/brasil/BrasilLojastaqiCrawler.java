package br.com.lett.crawlernode.crawlers.ranking.keywords.brasil;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.CrawlerRankingKeywords;

public class BrasilLojastaqiCrawler extends CrawlerRankingKeywords{

	public BrasilLojastaqiCrawler(Session session) {
		super(session);
	}

	@Override
	protected void extractProductsFromCurrentPage() {
		//número de produtos por página do market
		this.pageSize = 15;
			
		this.log("Página "+ this.currentPage);
		
		//monta a url com a keyword e a página
		String url = "http://www.taqi.com.br/pesquisa?q_question="+ this.keywordEncoded +"&q_pageSize=120&q_pageNum="+ this.currentPage;
		this.log("Link onde são feitos os crawlers: "+url);	
			
		//chama função de pegar a url
		this.currentDoc = fetchDocument(url);
		
		Elements products =  this.currentDoc.select("div.box_default_produto div.box_item_default");

		//se obter 1 ou mais links de produtos e essa página tiver resultado faça:
		if(products.size() >= 1) {
			//se o total de busca não foi setado ainda, chama a função para setar
			if(this.totalBusca == 0) setTotalBusca();
			
			for(Element e: products) {
				//seta o id com o seletor
				Element inid 		= e.select("a.bt_comprar_default").first();
				String internalPid 	= null;
				String internalId 	= makeInternalid(inid);
				
				//monta a url
				Element eUrl = e.select(".foto_produto > a").first();
				String productUrl  = "http://www.taqi.com.br"+ eUrl.attr("href");
				
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
		if(this.arrayProducts.size() < this.totalBusca){
			//tem próxima página
			return true;
		} else {
			//não tem próxima página
			return false;
		}
	}
	
	@Override
	protected void setTotalBusca()
	{
		Element totalElement = this.currentDoc.select("div.line_form_filtro > label > span").first();
		
		if(totalElement != null)
		{ 	
			try
			{				
				this.totalBusca = Integer.parseInt(totalElement.text().replaceAll("[^0-9]", "").trim());
			}
			catch(Exception e)
			{
				this.logError(e.getMessage());
			}
			
			this.log("Total da busca: "+this.totalBusca);
		}
	}
	
	private String makeInternalid(Element e){
		
		String[] tokens = e.attr("onclick").split(";");
		
		String temp = tokens[tokens.length-1];
		
		int x = temp.indexOf("(");
		int y = temp.indexOf(")", x+1);
		
		String[] tokens2 = ((temp.substring(x+1, y).replaceAll("'", "")).trim()).split(",");
		
		String inidFinal = tokens2[tokens2.length-1];
		
		return inidFinal;
	}
}
