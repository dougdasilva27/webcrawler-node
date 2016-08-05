package br.com.lett.crawlernode.crawlers.brasil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.base.Crawler;
import br.com.lett.crawlernode.models.CrawlerSession;
import br.com.lett.crawlernode.models.Product;
import br.com.lett.crawlernode.util.Logging;

public class BrasilEletrozemaCrawler extends Crawler {

	private final String HOME_PAGE = "https://www.zema.com";

	public BrasilEletrozemaCrawler(CrawlerSession session) {
		super(session);
	}

	@Override
	public boolean shouldVisit() {
		String href = this.session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && (href.startsWith(HOME_PAGE));
	}


	@Override
	public List<Product> extractInformation(Document doc) {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();

		if( isProductPage(this.session.getUrl()) ) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// internalId
			String internalId = null;
			Element elementInternalId = doc.select("input#IdProduto").first();
			if (elementInternalId != null) {
				internalId = elementInternalId.attr("value").trim();
			}

			// internalPid
			String internalPid = null;
			Element elementPid = doc.select("p.codigo").first();
			if (elementPid != null) {
				String tmp = elementPid.text();
				internalPid = tmp.substring(tmp.indexOf(":")+1, tmp.length()).trim();
			}

			// name
			Element elementName = doc.select(".produtoPrincipal .nomeMarca h1.nome").first();
			String name = elementName.text().replace("'","").replace("’","").trim();

			// price
			Float price = null;
			Element elementPrice = doc.select(".valores .preco #PrecoProduto").first();
			if(elementPrice == null) {
				price = null;
			} 
			else {
				price = Float.parseFloat(elementPrice.text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// availability
			Element elementNotifyButton = doc.select(".flagEsgotado").first();
			boolean available = true;
			if(elementNotifyButton != null) {
				if( elementNotifyButton.attr("style").equals("display:block;") ) {
					available = false;
				}
			}

			// categories
			String category1 = "";
			String category2 = "";
			String category3 = "";
			String categories[] = doc.select(".breadCrumbs").text().split("/");

			for(int i=1; i<categories.length; i++) {
				if(category1.isEmpty()) {
					category1 = categories[i].trim();
				} 
				else if(category2.isEmpty()) {
					category2 = categories[i].trim();
				} 
				else if(category3.isEmpty()) {
					category3 = categories[i].trim();
				}
			}

			// images
			Elements elementSecondaryImages = doc.select("#ListarMultiFotos li a img.foto");
			String primaryImage = null;
			String secondaryImages = null;
			JSONArray secondaryImagesArray = new JSONArray();

			for(Element e: elementSecondaryImages) {

				// Tirando o 87x87 para pegar imagem original
				if(primaryImage == null) {
					primaryImage = e.attr("src");
				} else {
					secondaryImagesArray.put(e.attr("src"));
				}

			}

			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";
			Element elementDescription1 = doc.select(".aba1.descResumida").first();
			Element elementDescription2 = doc.select(".aba2.descricao").first();
			if(elementDescription1 != null) description = elementDescription1.text().replace(".", ".\n").replace("'","").replace("’","").trim();
			if(elementDescription2 != null) description = description + "\n\n" + elementDescription2.text().replace(".", ".\n").replace("'","").replace("’","").trim();

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;
			
			Product product = new Product();
			product.setSeedId(this.session.getSeedId());
			product.setUrl(this.session.getUrl());
			product.setInternalId(internalId);
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}

		return products;
	}


	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return (url.startsWith("https://www.zema.com/produto/") || url.startsWith("http://www.zema.com/produto/"));
	}

}
