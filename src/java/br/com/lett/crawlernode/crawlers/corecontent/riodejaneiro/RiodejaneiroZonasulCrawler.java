package br.com.lett.crawlernode.crawlers.corecontent.riodejaneiro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import br.com.lett.crawlernode.core.models.Card;
import br.com.lett.crawlernode.core.models.Prices;
import br.com.lett.crawlernode.core.models.Product;
import br.com.lett.crawlernode.core.session.Session;
import br.com.lett.crawlernode.core.task.impl.Crawler;
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
			Element elementPrimaryImage = doc.select(".img_principal").first();
			if (elementPrimaryImage != null) {
				primaryImage = crawlImage(elementPrimaryImage);
			}

			// Imagens secundárias
			String secondaryImages = null;

			JSONArray secondaryImagesArray = new JSONArray();
			Elements elementSecondaryImage = doc.select("#ctl00_cphMasterPage1_dlFotos td > a");

			if(elementSecondaryImage.size() > 1){
				for(Element e : elementSecondaryImage) {
					String image = crawlImage(e);
					
					if( image != null && !image.equals(primaryImage)) {
						secondaryImagesArray.put(image);
					}
				}
			}
			
			if(secondaryImagesArray.length() > 0) {
				secondaryImages = secondaryImagesArray.toString();
			}

			// Descrição
			String description = "";

			Element elementInfoUL = doc.select(".info ul").first();
			if(elementInfoUL != null) 	{
				description = description + elementInfoUL.html();
			}
			
			Element elementInfoDET = doc.select(".info .det").first();
			if(elementInfoDET != null) 	{
				description = description + elementInfoDET.html();
			}

			// Estoque
			Integer stock = null;

			// Marketplace
			JSONArray marketplace = null;

			// Prices
			Prices prices = crawlPrices(doc, price);
			
			Product product = new Product();
			
			product.setUrl(this.session.getOriginalURL());
			product.setInternalId(internalId);
			product.setInternalPid(internalPid);
			product.setName(name);
			product.setAvailable(available);
			product.setPrice(price);
			product.setPrices(prices);
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
	
	/**
	 * In this market, installments not appear in product page
	 * 
	 * @param doc
	 * @param price
	 * @return
	 */
	private Prices crawlPrices(Document doc, Float price){
		Prices prices = new Prices();

		if(price != null){
			Map<Integer,Float> installmentPriceMap = new HashMap<>();

			installmentPriceMap.put(1, price);
			prices.insertBankTicket(price);

			prices.insertCardInstallment(Card.VISA.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.MASTERCARD.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.AMEX.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.DINERS.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.ELO.toString(), installmentPriceMap);
			prices.insertCardInstallment(Card.SHOP_CARD.toString(), installmentPriceMap);
		}

		return prices;
	}
	
	/**
	 * Crawl any image for attribute "onClick"
	 * @param onClick
	 * @return
	 */
	private String crawlImage(Element e) {
		String image;
		
		if(e.hasAttr("onClick")) {
			String onClick = e.attr("onClick");
			if(onClick.contains("'") && !onClick.toLowerCase().contains("video")) {
				String[] tokens = onClick.split("'");
				
				image = tokens[tokens.length-2].trim();
				
				if(image.contains("280_280")) {
					image = image.replace("280-280", "430_430");
				} else if(image.contains("60_60")) {
					image = image.replace("60_60", "430_430");
				}
				
			} else {
				return null;
			}
		} else {
			image = e.attr("src").trim();
			
			if(image.contains("video")) {
				return null;
			}
		}
		
		if(image.contains("produto_sem_foto")) {
			return null;
		}
		
		return image;
	}
}