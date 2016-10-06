package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.CrawlerSession;
import br.com.lett.crawlernode.core.task.Crawler;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloNetfarmaCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.netfarma.com.br/";

	public SaopauloNetfarmaCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getOriginalURL().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}

	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if ( isProductPage(this.session.getOriginalURL()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getOriginalURL());

			// ID interno
			String id = this.session.getOriginalURL().split("/")[4];
			String internalID = Integer.toString(Integer.parseInt(id));

			// Pid
			String internalPid = null;
			Element elementInternalPid = doc.select(".codigo").first();
			if (elementInternalPid != null) {
				internalPid = elementInternalPid.text().split(":")[1].trim();
			}

			// Nome
			Elements elementName = doc.select(".prodInfo h1.nome");
			String name = elementName.text().trim();

			// Preço
			Float price = null;
			Element elementPrice = doc.select(".compra-unica .precoPor #PrecoPromocaoProduto").first();
			if(elementPrice == null) {
				elementPrice = doc.select(".compra-unica .precoPor").first();
				if (elementPrice != null) {
					price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				}
			}

			// Categorias
			String category1 = null; 
			String category2 = null; 
			String category3 = null;

			// Imagem primária
			Elements elementPrimaryImage = doc.select("#lupaZoom a");
			String primaryImage = elementPrimaryImage.attr("href");

			// Imagens secudnárias
			String secondaryImages = null;

			JSONArray secondaryImagesArray = new JSONArray();
			Elements element_fotosecundaria = doc.select("#ListarMultiFotos li img");
			if(element_fotosecundaria.size()>1){
				for(int i=1; i<element_fotosecundaria.size();i++){
					Element e = element_fotosecundaria.get(i);
					if(e.attr("src").contains("/imagens/icon_video.png")){

					}else{
						secondaryImagesArray.put(e.attr("src"));
					}
				}

			}
			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";

			try {
				Element[] sections = new Element[]{
						doc.select("div[name=infoProduto]").first(),
				};
				for(Element e: sections) {
					if(e != null) {
						description = description + e.html(); 
					}
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}


			// Disponibilidade
			boolean available = true;
			Elements elementOutOfStock = doc.select("#SemEstoque");
			if(elementOutOfStock != null && elementOutOfStock.attr("style").contains("display:block")) {
				available = false;
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			
			product.setUrl(session.getOriginalURL());
			product.setInternalId(internalID);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setPrice(price);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);
			product.setAvailable(available);

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page " + this.session.getOriginalURL());
		}
		
		return products;
	}
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.startsWith("http://www.netfarma.com.br/produto/");
	}
}
