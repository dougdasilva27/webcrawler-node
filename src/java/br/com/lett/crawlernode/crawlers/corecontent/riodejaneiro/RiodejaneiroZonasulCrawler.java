package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.crawler.Crawler;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.util.Logging;

public class RiodejaneiroZonasulCrawler extends Crawler {
	
	private final String HOME_PAGE = "http://www.zonasulatende.com.br/";

	public RiodejaneiroZonasulCrawler(Session session) {
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
			String internalId = null;
			Element elementID = doc.select(".codigo").first();
			if (elementID != null) {
				if(!elementID.text().trim().isEmpty()){
					internalId = Integer.toString(Integer.parseInt(elementID.text().replaceAll("[^0-9,]+", ""))).trim();
				}
			}

			// Pid
			String internalPid = internalId;

			// Nome
			String name = null;
			Element elementName = doc.select(".top h3").first();
			if (elementName != null) {
				name = elementName.text().trim();
			}

			// Disponibilidade
			boolean available = true;
			Element elementUnavailable = doc.select("#ctl00_cphMasterPage1_pnlProdutoIndisponivel").first();
			if (elementUnavailable != null) {
				available = false;
			}

			// Preço
			Float price = null;
			if (available) {
				Elements elementPrice = doc.select("#ctl00_cphMasterPage1_pnlPreco p.preco");
				if(elementPrice.size() == 0) {
					elementPrice = doc.select("#ctl00_cphMasterPage1_pnlPrecoRebaixa p.preco_por");
				}

				try {
					price = Float.parseFloat(elementPrice.first().text().replaceAll("[^0-9,]+", "").replaceAll("\\.", "").replaceAll(",", "."));
				} catch (Exception e) {
					Logging.printLogError(logger, session, "Preço não encontrado para o produto: " + this.session.getOriginalURL());
				}
			}

			// Categorias
			String category1; 
			String category2; 
			String category3;

			Elements elementCategories = doc.select("span.migalha a"); 

			String[] category = new String[4];
			category[0] = "";
			category[1] = "";
			category[2] = "";
			category[3] = "";
			int j=0;

			for(int i=0; i < elementCategories.size(); i++) {
				Element e = elementCategories.get(i);
				category[j] = e.text().toString();
				category[j] = category[j].replace(">", "");
				j++;
			}
			category1 = category[1];
			category2 = category[2];
			category3 = null;

			// Imagem primária
			String primaryImage = null;
			Element elementPrimaryImage = doc.select("#img_zoom img").first();
			if (elementPrimaryImage != null) {
				primaryImage = elementPrimaryImage.attr("src").trim();
			}

			if(primaryImage.contains("produto_sem_foto")) primaryImage = "";

			// Imagens secundárias
			String secondaryImages = null;

			JSONArray SecondaryImagesArray = new JSONArray();
			Elements elementSecondaryImage = doc.select("#ctl00_cphMasterPage1_dlFotos td img");

			if(elementSecondaryImage.size() > 1){
				for(int i = 0; i < elementSecondaryImage.size(); i++) {
					Element e = elementSecondaryImage.get(i);
					if( !e.attr("src").replace("/60_60/", "/430_430/").equals(primaryImage) ) {
						SecondaryImagesArray.put(e.attr("src").replace("/60_60/", "/430_430/"));
					}
				}
			}
			if(SecondaryImagesArray.length() > 0) {
				secondaryImages = SecondaryImagesArray.toString();
			}

			// Descrição
			String description = "";

			Element elementInfoUL = doc.select(".info ul").first();
			Element elementInfoDET = doc.select(".info .det").first();
			if(elementInfoUL != null) 	description = description + elementInfoUL.html();
			if(elementInfoDET != null) 	description = description + elementInfoDET.html();

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			Product product = new Product();
			product.setUrl(this.session.getOriginalURL());
			
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setAvailable(available);
			product.setPrice(price);
			product.setCategory1(category1);
			product.setCategory2(category2);
			product.setCategory3(category3);
			product.setPrimaryImage(primaryImage);
			product.setSecondaryImages(secondaryImages);
			product.setDescription(description);
			product.setStock(stock);
			product.setMarketplace(marketplace);

			products.add(product);

		} else {
			Logging.printLogDebug(logger, session, "Not a product page" + this.session.getOriginalURL());
		}
		
		return products;
	}
	
	
	/*******************************
	 * Product page identification *
	 *******************************/

	private boolean isProductPage(String url) {
		return url.startsWith("http://www.zonasulatende.com.br/Produto/");
	}
}