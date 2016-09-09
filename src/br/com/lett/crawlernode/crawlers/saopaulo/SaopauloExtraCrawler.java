package br.com.lett.crawlernode.crawlers.saopaulo;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.kernel.models.Product;
import br.com.lett.crawlernode.kernel.task.Crawler;
import br.com.lett.crawlernode.kernel.task.CrawlerSession;
import br.com.lett.crawlernode.util.Logging;

public class SaopauloExtraCrawler extends Crawler {

	public SaopauloExtraCrawler(CrawlerSession session) {
		super(session);
	}


	@Override
	public boolean shouldVisit() {
		String href = session.getUrl().toLowerCase();
		return !FILTERS.matcher(href).matches() && href.startsWith("http://www.deliveryextra.com.br/");
	}


	@Override
	public List<Product> extractInformation(Document doc) throws Exception {
		super.extractInformation(doc);
		List<Product> products = new ArrayList<Product>();
		
		if(session.getUrl().contains("/produto/")) {
			Logging.printLogDebug(logger, session, "Product page identified: " + this.session.getUrl());

			// ID interno
			String internalID = null;
			try {
				internalID = Integer.toString(Integer.parseInt(session.getUrl().split("/")[4]));
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Pid
			String internalPid = internalID;

			// Nome
			String name = null;
			Element elementName = doc.select("article.hproduct h1").first();
			if (elementName != null) {
				name = elementName.text().replace("'", "").trim();
			}

			// Preço
			Float price = null;
			Elements elementPrice = doc.select("article.hproduct > .hproductLeft .price-off");
			if(elementPrice.size() == 0) {
				elementPrice = doc.select(".price-detail.sale-detail > .for > .sale-price");
				if (elementPrice.size() == 0) {
					elementPrice = doc.select(".box-price .progressiveDiscount-baseValue");
				}
			}
			if (elementPrice.last() != null) {
				price = Float.parseFloat(elementPrice.last().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
			}

			// Disponibilidade
			boolean available = false;
			Element elementAvailable = doc.select(".btnComprarProd.productElement").first();
			
			available = (elementAvailable != null);

			// Categorias
			String category1 = "";
			String category2 = "";
			String category3 = "";

			Element elementCategory1 = doc.select("article.hproduct h2").first();
			if (elementCategory1 != null) {
				category1 = elementCategory1.text().trim();
			}

			Element elementCategory2 = doc.select("article.hproduct h3").first();
			if (elementCategory2 != null) {
				category2 = elementCategory2.text().trim();
			}

			// Imagem primária
			String primaryImage = null;
			Elements elementPrimaryImage = doc.select(".box-image .image img");
			if (elementPrimaryImage.size() > 0) {
				primaryImage = "http://www.deliveryextra.com.br" + elementPrimaryImage.attr("src");
			}
			
			if(primaryImage.contains("nome_da_imagem_do_sem_foto.gif")) {
				primaryImage = ""; //TODO: Verificar o nome da foto genérica
			}

			// Imagens secundárias
			Elements elementSecondaryImages = doc.select(".more-views dl.thumb-list dd a");
			JSONArray secondaryImagesArray = new JSONArray();
			String secondaryImages = "";
			if(elementSecondaryImages.size() > 1) {
				for(int i = 1; i < elementSecondaryImages.size(); i++) { // primeira imagem é a primária
					String tmp = elementSecondaryImages.attr("href");
					if(tmp.equals("#")) {
						Elements imgElement = elementSecondaryImages.select("img");
						if(imgElement.size() > 0) {
							tmp = "http://www.deliveryextra.com.br" + imgElement.attr("src");
							secondaryImagesArray.put(tmp);
						}
					}
				}
			}
			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";   
			Element element_moreinfo = doc.select("#more-info").first();
			if(element_moreinfo != null) {
				description = description + element_moreinfo.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			product.setSeedId(session.getSeedId());
			product.setUrl(session.getUrl());
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
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getUrl());
		}

		return products;
	}

}