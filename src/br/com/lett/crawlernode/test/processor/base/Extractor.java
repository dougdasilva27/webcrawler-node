package br.com.lett.crawlernode.test.processor.base;

import java.text.Normalizer;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.lett.crawlernode.test.Logging;
import br.com.lett.crawlernode.test.processor.models.BrandModel;
import br.com.lett.crawlernode.test.processor.models.ClassModel;
import br.com.lett.crawlernode.test.processor.models.ProcessedModel;

/**
 * Classe Extratora pai de todas as classes extratoras de todos os supermercados
 * @author Doug
 */
public abstract class Extractor {
	
	private static final Logger logger = LoggerFactory.getLogger(Extractor.class);
	
	protected Map<String, String> unitsReplaceMap;
	protected Map<String, String> recipientsReplaceMap;
	
	protected List<String> recipientsList;
	protected List<String> unitsList;
	protected List<ClassModel> classModelList;
	protected List<BrandModel> brandModelList;
	
	protected boolean logActivated;
	
	/**
	 * Método responsável pela definição e extração de atributos
	 * @author Doug
	 * @param logActivated - Definição de status do log
	 * @param unitsReplaceMap - Mapa de redefinição de unidades
	 * @param recipientsReplaceMap - Mapa de redefinição de recipientes
	 * @param brandsReplaceMap - Mapa de redefinição de marcas
	 * @param recipientsList - Mapa de identificação de recipientes
	 * @param unitsList - Mapa de identificação de unidades
	 */
	public void setAttrs(
			boolean logActivated,
			List<BrandModel> brandModelList,
			Map<String, String> unitsReplaceMap, 
			Map<String, String> recipientsReplaceMap, 
			List<String> recipientsList, 
			List<String> unitsList,
			List<ClassModel> classModelList) {
		
		this.logActivated = logActivated;
		this.brandModelList = brandModelList;
		this.unitsReplaceMap = unitsReplaceMap;
		this.recipientsReplaceMap = recipientsReplaceMap;
		this.recipientsList = recipientsList;
		this.unitsList = unitsList;
		this.classModelList = classModelList;
	}
	
	/**
	 * Método alegórico de extração que será definido de acordo com cada supermercado
	 * @param processedModel
	 * @return processedModel
	 */
	public ProcessedModel extract(ProcessedModel processedModel) {
		
		// Colocando o original_name no extra.
		processedModel.setExtra(processedModel.getOriginalName());

		return processedModel;
	}
	
	/** 
	 * Método responsável por uma pré hizienização e padronização do conteúdo de ProcessModel
	 * @category Manipulação
	 * @author Doug
	 * @param pm - ProcessModel à ser padronizado/hizienizado
	 */
	public void preSanitize(ProcessedModel pm) {
		
		// Higieniza o nome do CrawlerModel
		String sanitizedName = pm.getOriginalName();
		
		// Essas palavras ou caracteres devem ser substituídas em qualquer situação, não apenas na lista negra
		sanitizedName = sanitizedName.replace("'", "");
		sanitizedName = sanitizedName.replace("`", "");
		sanitizedName = sanitizedName.replace("+", " ");
		sanitizedName = sanitizedName.replaceAll("\\u00a0"," ");
		sanitizedName = sanitizedName.replaceAll("\\u2007"," ");
		sanitizedName = sanitizedName.replaceAll("\\u202F"," ");
		sanitizedName = sanitizedName.replaceAll("\\u3000"," ");
		sanitizedName = sanitizedName.replaceAll("\\u1680"," ");
		sanitizedName = sanitizedName.replaceAll("\\u180e"," ");
		sanitizedName = sanitizedName.replaceAll("\\u200a"," ");
		sanitizedName = sanitizedName.replaceAll("\\xA0"," ");
		sanitizedName = sanitizedName.replaceAll("\\u205f"," ");
		
		// Básico - Remover caixa alta e espaços vazios no começo e fim
		sanitizedName = sanitizedName.toLowerCase();
		sanitizedName = sanitizedName.trim();
		
		// Remoção de espaços duplos
		while (sanitizedName.contains("  ")) sanitizedName = sanitizedName.replace("  ", " ");
		
		sanitizedName = sanitizedName.trim();
		
		// Substituição de marcas de acordo com o 'brandsReplaceMap'
		for (BrandModel bm : brandModelList) {
			if(!bm.getMistake().isEmpty()){
				for(String mistake : bm.getMistake()){
					// Se encontrar a marca no começo...
					Pattern beginning = Pattern.compile("^" + Pattern.quote(mistake + " "));
					Matcher matcherBeginning = beginning.matcher(sanitizedName);
					while (matcherBeginning.find()) {
						if (logActivated) Logging.printLogDebug(logger, "-- Found BRAND ON BEGINNING: " + mistake + "\n    '" + sanitizedName + "' -> '");
						sanitizedName = bm.getBrand() + " " + sanitizedName.substring(matcherBeginning.end());
						if (logActivated) Logging.printLogDebug(logger, sanitizedName + "'");
		//						matcherBeginning = beginning.matcher(sanitizedName);
					}
					
					// Se encontrar a marca no final...
					Pattern ending = Pattern.compile(Pattern.quote(mistake) + "$");
					Matcher matcherEnding = ending.matcher(sanitizedName);
					while (matcherEnding.find()) {
						if (logActivated) Logging.printLogDebug(logger, "-- Found BRAND ON END: " + mistake + "\n    '" + sanitizedName + "' -> '");
						sanitizedName = sanitizedName.substring(0, matcherEnding.start()) + " " + bm.getBrand();
						if (logActivated) Logging.printLogDebug(logger, sanitizedName + "'");
		//						matcherEnding = ending.matcher(sanitizedName);
					}
					
					// Se encontrar a marca no meio...
					if (sanitizedName.contains(" " + mistake + " ")) {
				    	if (logActivated) Logging.printLogDebug(logger, "-- Found BRAND MIDDLE: " + mistake + "\n    '" + sanitizedName + "' -> '");
				    	sanitizedName = sanitizedName.replace(" " + mistake + " ", " " + bm.getBrand() + " ");
				    	if (logActivated) Logging.printLogDebug(logger, sanitizedName + "'");
				    }
				}
			}
		}
		
		// Remarcação de unidades de acordo com o mapa 'unitsReplaceMap'
		for (Map.Entry<String, String> entry : this.unitsReplaceMap.entrySet()) {
			Pattern pattern = Pattern.compile("([0-9]+)([,.][0-9]{1,2})?([ ])?" + Pattern.quote(entry.getKey()) + "(\\s|$)");
		    Matcher matcher = pattern.matcher(sanitizedName);
		    while (matcher.find()) {
		        String newName = "";
		        // Caso haja a ocorrência do padrão fora da primeira posição
		        if (matcher.start() != 0) {
		        	newName = sanitizedName.substring(0, matcher.start()) + matcher.group(1) + (matcher.group(2)!=null ? matcher.group(2) : "") + " " + entry.getValue();
		        }
		        // Caso haja a ocorrência no final da sentença
		        if (matcher.end() != sanitizedName.length()) {
		        	newName = newName + " " + sanitizedName.substring(matcher.end(), sanitizedName.length());
		        }
		        
		        if (logActivated) Logging.printLogDebug(logger, "-- Found UNIT WITH PATTERN: " + entry.getKey() + "\n    '" + sanitizedName + "' -> '" + newName + "'");
		        sanitizedName = newName;
		        matcher = pattern.matcher(sanitizedName);
		    }
		    
		}
		
		// Recipiente entre meio ou final da sentença
		for (Map.Entry<String, String> entry : this.recipientsReplaceMap.entrySet()) {
			// Se esta no fim da String
			Pattern ending = Pattern.compile(Pattern.quote(entry.getKey()) + "$");
			Matcher matcherEnding = ending.matcher(sanitizedName);
			while (matcherEnding.find()) {
				if (logActivated) Logging.printLogDebug(logger, "-- Found RECIPIENT ON END: " + entry.getKey() + "\n    '" + sanitizedName + "' -> '");
				sanitizedName = sanitizedName.substring(0, matcherEnding.start()) + " " + entry.getValue();
				if (logActivated) Logging.printLogDebug(logger, sanitizedName + "'");
				matcherEnding = ending.matcher(sanitizedName);
			}
			
			// Se esta no meio da String
			if (sanitizedName.contains(" " + entry.getKey() + " ")) {
	        	if (logActivated) Logging.printLogDebug(logger, "-- Found RECIPIENT: " + entry.getKey() + "\n    '" + sanitizedName + "'");
	        	sanitizedName = sanitizedName.replace(" " + entry.getKey() + " ", " " + entry.getValue() + " ");
	        	if (logActivated) Logging.printLogDebug(logger, "--> '" + sanitizedName + "'");
	        }
			
			
		}
		
		// Remoção de espaços duplos
		while (sanitizedName.contains("  "))  sanitizedName = sanitizedName.replace("  ", " ");
		
		
		
		// Classe passo 1 - Classes erradas do início do nome do produto
		//
		// Exemplos: beb. nestle pronta 300ml     ->   bebida nestle pronta 300ml
		//           beb.nestle pronta 300ml      ->   bebida nestle pronta 300ml
		//           beb nestle pronta 300ml      ->   bebida nestle pronta 300ml
		//           alimento para cão FOX 1kg    ->   ração FOX cão 1kg
		
		for (ClassModel classModel : this.classModelList) {
			Pattern pattern;
			String normalizedName = Normalizer.normalize(sanitizedName, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");

			for(Map.Entry<String, String> entry : classModel.getMistakes().entrySet()){

				String normalizedKey = Normalizer.normalize(entry.getKey(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
				
				if(normalizedKey.endsWith(".")) {
					pattern = Pattern.compile("^" + Pattern.quote(normalizedKey)); // Se tiver ponto final depois da abreviação ok
				} else {
					pattern = Pattern.compile("^" + Pattern.quote(normalizedKey) + "(\\s|$)"); // Se não, colocar espaço ou marcador de final de frase.
				}
				
				Matcher matcher = pattern.matcher(normalizedName);  // Padroniza o nome no caso de ocorrência do padrão
				
				while(matcher.find()){
					if (logActivated) Logging.printLogDebug(logger, "-- Found CLASS STEP 1 ON BEGINNING: " + entry.getKey() + "\n    '" + sanitizedName + "' -> '");
			    	sanitizedName = classModel.getLettName() + " " + sanitizedName.substring(matcher.end()) + " " + entry.getValue();
			    	if (logActivated) Logging.printLogDebug(logger, pm.getSanitizedName() + "'");
				}
			}			
		}
		   
		// Remoção de espaços duplos
		while (sanitizedName.contains("  "))  sanitizedName = sanitizedName.replace("  ", " ");
				
		sanitizedName = sanitizedName.trim();
		
		//Salvando no objeto nome padronizado
	    pm.setSanitizedName(sanitizedName);
		
	}
	
	/**
	 * Método responsável pela extração da marca do produto
	 * @author Doug
	 * @param pm -ProcessModel que contém a marca à ser extraída
	 * @category Manipulação
	 */
	public void extractBrand(ProcessedModel pm) {

		try {
			if (logActivated) Logging.printLogInfo(logger, "\n---> Extracting brand...");
			
			// Definindo padrão
			pm.setBrand(null);
			
			String currentBrand = null;
			String currentNormalizedBrand = null;
			int currentBrandPosition = 9999999;
			int currentBrandLength = 0;
			
			for (BrandModel bm : this.brandModelList) {
				// Padroniza o nome e a marca sem caracteres especias
				String normalizedName = Normalizer.normalize(pm.getSanitizedName(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
				String normalizedBrand = Normalizer.normalize(bm.getBrand(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
				
				// No final
				if (normalizedName.endsWith(" " + normalizedBrand)) {
					
					// Procurando marca que está mais para a esquerda que a já encontrada atualmente
					// Exemplo: Refrigerante COCA-COLA Tradicional leve de brinde uma Pepsi 350ml
					// COCA-COLA é uma marca. Pepsi é outra. Queremos pegar a primeira, COCA-COLA.
					if(currentBrand == null || normalizedName.indexOf(" " + normalizedBrand) < currentBrandPosition) {
						currentBrand = bm.getBrand().trim();
						currentNormalizedBrand = Normalizer.normalize(currentBrand, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
						currentBrandPosition = normalizedName.indexOf(" " + currentNormalizedBrand);
						currentBrandLength = currentBrand.trim().length();
					} else if(normalizedName.indexOf(" " + normalizedBrand) == currentBrandPosition) {
						// Está na mesma posição, então vamos chegar o size. Pegar a com o maior size, pois pode ser marca composta.
						// Exemplo: Acelerador de bronzeado AUSTRALIAN GOLD Dark frasco 237ml
						// AUSTRALIAN é uma marca. GOLD é outra. AUSTRALIAN GOLD outra diferente. Queremos pegar a AUSTRALIAN GOLD.
						if(currentBrandLength < bm.getBrand().trim().length()){
							currentBrand = bm.getBrand().trim();
							currentNormalizedBrand = Normalizer.normalize(currentBrand, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
							currentBrandPosition = normalizedName.indexOf(" " + currentNormalizedBrand);
							currentBrandLength = currentBrand.trim().length();
						}
					}
				}
				
				// No meio
				if (normalizedName.contains(" " + normalizedBrand + " ")) {
					
					// Procurando marca que está mais para a esquerda que a já encontrada atualmente
					// Exemplo: Refrigerante COCA-COLA Tradicional leve de brinde uma Pepsi 350ml
					// COCA-COLA é uma marca. Pepsi é outra. Queremos pegar a primeira, COCA-COLA.
					if(currentBrand == null || normalizedName.indexOf(" " + normalizedBrand + " ") < currentBrandPosition) {
						currentBrand = bm.getBrand().trim();
						currentNormalizedBrand = Normalizer.normalize(currentBrand, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
						currentBrandPosition = normalizedName.indexOf(" " + currentNormalizedBrand + " ");
						currentBrandLength = currentBrand.trim().length();
					} else if(normalizedName.indexOf(" " + normalizedBrand + " ") == currentBrandPosition) {
						// Está na mesma posição, então vamos chegar o size. Pegar a com o maior size, pois pode ser marca composta.
						// Exemplo: Acelerador de bronzeado AUSTRALIAN GOLD Dark frasco 237ml
						// AUSTRALIAN é uma marca. GOLD é outra. AUSTRALIAN GOLD outra diferente. Queremos pegar a AUSTRALIAN GOLD.
						if(currentBrandLength < bm.getBrand().trim().length()){
							currentBrand = bm.getBrand().trim();
							currentNormalizedBrand = Normalizer.normalize(currentBrand, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
							currentBrandPosition = normalizedName.indexOf(" " + currentNormalizedBrand + " ");
							currentBrandLength = currentBrand.trim().length();
						}
					}
				}
			}
			
			// Define marca encontrada no ProcessedModel
			pm.setBrand(currentBrand);
			if (logActivated) Logging.printLogInfo(logger, "Brand " + currentBrand + " found on: " + pm.getOriginalName() + " / " + pm.getSanitizedName());
			
		} catch (Exception e) {
			if (logActivated)Logging.printLogError(logger, "Error!");
		}
	}

	/**
	 * Método responsável pela extração da classe do produto (Classe é tudo aquilo antes de marca)
	 * @author Doug
	 * @param pm - ProcessModel que contém a classe à ser extraída
	 * @category Manipulação
	 */
	public void extractClass(ProcessedModel pm) {
		try {
			if (logActivated) System.out.println("\n---> Extracting class...");
			
			if (pm.getBrand() != null) {
				
				// Padroniza o nome e a marca sem caracteres especias
				String normalizedName = Normalizer.normalize(pm.getSanitizedName(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
				String normalizedBrand = Normalizer.normalize(pm.getBrand(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
				
				
				int i = normalizedName.indexOf(normalizedBrand);
				if (i > 0) {
					// Classe do produto é tudo antes da marca
					int indexOfBrand = normalizedName.indexOf(normalizedBrand);
					pm.set_class(pm.getSanitizedName().substring(0, indexOfBrand).trim());
					
					// No momento extra é tudo além de marca e classe
					pm.setExtra(pm.getSanitizedName().substring(indexOfBrand + pm.getBrand().length(), pm.getSanitizedName().length()).trim());
				}
				
				
			} else {
				// Se não encontrar a marca, colocaremos temporariamente no class
				pm.set_class(pm.getSanitizedName().trim());
				
				// Classe ainda em construção para alertar a falta de marca 
				
				if (logActivated) Logging.printLogDebug(logger, pm.getOriginalName());
				if (logActivated) Logging.printLogError(logger, "Problem extracting class. Brand not found on: " + pm.getOriginalName() + " / " + pm.getSanitizedName());
			
			
			}	
			
			// Variável para verificação se encontrou a classe
			boolean found = false;
			
			String newExtra = new String();
		    String newClass = new String();
		    int currentSizeClass = 0;
		    
		    // Sanitize -> Classe passo 2 - Abreviações no meio ou final da classe
		 	// Exemplos: vinho ale BACKER gelada 600ml             ->   vinho alemão BACKER gelado 600ml
		 	//           vinho ale.puro BACKER gelada 600ml        ->   vinho alemão puro BACKER gelado 600ml
		 	//           vinho s/ alcool ale BACKER gelada 600ml   ->   vinho sem alcool alemão BACKER gelado 600ml
			for (ClassModel classModel : this.classModelList) {	
				// Início
				Pattern pattern = Pattern.compile("^" + Pattern.quote(classModel.getLettName().trim()) + "(\\s|$)"); // Espaço depois ou no final
				
			    Matcher matcher = pattern.matcher(pm.get_class());
			    // System.err.println(pm.get_class() + " -> Compare with -> " + classModel.getLettName().trim());
			    
			    // Corrige as ocorrências do segundo caso de abrevição de classe
			    
			    if(classModel.getLettName().length() > currentSizeClass){
			    
				    while (matcher.find()) {
				    	
				    	currentSizeClass = classModel.getLettName().length();
				    	
				    	if (logActivated) Logging.printLogDebug(logger, "-- Found CLASS: " + classModel.getLettName() + "\n    '" + pm.get_class() + "' -> '");
				    	
				    	String prependToExtra = pm.get_class().substring(matcher.end(), pm.get_class().length());
				    	
				    	// Atualizando class...
				    	newClass = classModel.getLettName();
				    	newExtra = prependToExtra + " " + pm.getExtra();
				    	
				    	if (logActivated) Logging.printLogDebug(logger, pm.get_class() + "'");
				    	
				    	found = true;
					    
				    }
			    }
			}
			// Caso não encontre classe todo conteúdo irá para o extra
			if(!found) {
				newExtra = pm.get_class() + " " + pm.getExtra();
				newClass = "";
			}
			
			// Eliminação de espaços duplos
		    while (newExtra.contains("  "))  newExtra = newExtra.replace("  ", " ");
		    while (newClass.contains("  "))  newClass = newClass.replace("  ", " ");
		    newExtra = newExtra.trim();
		    newClass = newClass.trim();
		    
		    // Determina o extra e a classe
		    pm.set_class(newClass);
		    pm.setExtra(newExtra);
				
		} catch (Exception e) {
			if (logActivated) Logging.printLogError(logger, "Error!");
		}
	}
	
	/**
	 * Método responsável pela extração do recipiente do produto
	 * @param pm - ProcessModel que contém o recipiente à ser extraída
	 * @author Doug
	 * @category Manipulação
	 */
	public void extractRecipient(ProcessedModel pm) {
		try {
			if (logActivated) Logging.printLogDebug(logger, "\n---> Extracting recipient...");
			
			// Definindo padrão
			pm.setRecipient(null);
			
			// Identificação dos recipientes através da lista de recipientes
			for (String rec: this.recipientsList) {
				
				//Tratamento de espaços ao encontrar recipiente
				if (pm.getSanitizedName().contains(" " + rec + " ") || pm.getSanitizedName().endsWith(" " + rec)) {
					
					// Definindo recipiente
					pm.setRecipient(rec.trim());
					
					// Remoção do recipiente do conteúdo de extra
					pm.setExtra(removeExtraFromString(pm.getRecipient(), pm.getExtra()));
					
					break;
				}
			}
			
		} catch (Exception e) {
			if (logActivated) Logging.printLogError(logger, "Error!");
		}
		
	}
	
	/**
	 * Método responsável pela extração de conteúdo que define a quantidade de produtos que serão vendidos.<br>
	 * Esse método é apenas para casos típicos, casos especiais serão tratados nos extractor de cada supermercado <br>
	 * Ex.: Leve 3 pague 2
	 * @author Doug
	 * @param pm - ProcessModel que contém o fator de multiplicação à ser extraída
	 * @category Manipulação
	 */
	public void extractMultiplier(ProcessedModel pm) {
		try {
			if (logActivated) Logging.printLogInfo(logger, "\n---> Extracting multiplier...");
			
			// Definindo padrão
			pm.setMultiplier(1);
			
			Pattern patternMultiplier;
			Matcher matcherMultiplier;
			String multiplierString;

			
			// Atenção, apenas casos típicos serão tratados aqui. Para casos especiais de cada supermercado,
			// devem ser tratados dentro do extractor de cada supermercado, dando override nesta função,
			// e completando com os casos especiais.
			
			// Caso típico 1 -> leve 12 pague 10 unidades
			patternMultiplier = Pattern.compile("\\b(leve)(\\s)(\\d+)(\\s)(pague)(\\s)(\\d+)(\\s)(unidades)\\b");
			matcherMultiplier = patternMultiplier.matcher(pm.getExtra());
		    
			while (matcherMultiplier.find()) {
				multiplierString = matcherMultiplier.group(3).trim();
				
		    	if (logActivated) Logging.printLogDebug(logger, "-- Found MULTIPLIER: " + multiplierString + "\n    ");
		        try {
					pm.setMultiplier(Integer.parseInt(multiplierString));
					
					// Atualizando extra...
					if (logActivated)Logging.printLogDebug(logger, pm.getExtra() + "' -> '");
			        String newExtra = pm.getExtra().substring(0, matcherMultiplier.start()) + pm.getExtra().substring(matcherMultiplier.end(), pm.getExtra().length());
			        pm.setExtra(newExtra.trim());
			    	if (logActivated) Logging.printLogDebug(logger, pm.getExtra() + "'");
			    	
			    } catch (Exception e) {
			    	if (logActivated) Logging.printLogError(logger, "-- Error trying to convert multipler to integer: " + multiplierString + "\n' -> '");
				}
			}
			
			
			// Caso típico 2 -> leve 12 pague 10
			patternMultiplier = Pattern.compile("\\b(leve)(\\s)(\\d+)(\\s)(pague)(\\s)(\\d+)\\b");
			matcherMultiplier = patternMultiplier.matcher(pm.getExtra());
		    
			while (matcherMultiplier.find()) {
				multiplierString = matcherMultiplier.group(3).trim();

		    	if (logActivated) Logging.printLogDebug(logger, "-- Found MULTIPLIER: " + multiplierString + "\n    ");
		        try {
					pm.setMultiplier(Integer.parseInt(multiplierString));
					
					// Atualizando extra...
					if (logActivated) Logging.printLogDebug(logger, pm.getExtra() + "' -> '");
			        String newExtra = pm.getExtra().substring(0, matcherMultiplier.start()) + pm.getExtra().substring(matcherMultiplier.end(), pm.getExtra().length());
			        pm.setExtra(newExtra.trim());
			    	if (logActivated) Logging.printLogDebug(logger, pm.getExtra() + "'");
			    	
			    } catch (Exception e) {
			    	if (logActivated) Logging.printLogError(logger, "-- Error trying to convert multipler to integer: " + multiplierString + "\n' -> '");
				}
			}
			
			
			// Caso típico 3 -> com 8 unidades
			patternMultiplier = Pattern.compile("\\b(com)(\\s)(\\d+)(\\s)(unidades)\\b");
			matcherMultiplier = patternMultiplier.matcher(pm.getExtra());
		    
			while (matcherMultiplier.find()) {
				multiplierString = matcherMultiplier.group(3).trim();

		    	if (logActivated) Logging.printLogDebug(logger, "-- Found MULTIPLIER: " + multiplierString + "\n    ");
		        try {
					pm.setMultiplier(Integer.parseInt(multiplierString));
					
					// Atualizando extra...
					if (logActivated) Logging.printLogDebug(logger, pm.getExtra() + "' -> '");
			        String newExtra = pm.getExtra().substring(0, matcherMultiplier.start()) + pm.getExtra().substring(matcherMultiplier.end(), pm.getExtra().length());
			        pm.setExtra(newExtra.trim());
			    	if (logActivated) Logging.printLogDebug(logger, pm.getExtra() + "'");
			    	
			    } catch (Exception e) {
			    	if (logActivated) Logging.printLogError(logger, "-- Error trying to convert multipler to integer: " + multiplierString + "\n' -> '");
				}
			}
			
			// Caso típico 4 -> (10 unidades)
			patternMultiplier = Pattern.compile("\\((\\d+)(\\s)(unidades)\\)");
			matcherMultiplier = patternMultiplier.matcher(pm.getExtra());
		    
			while (matcherMultiplier.find()) {
				multiplierString = pm.getExtra().substring(matcherMultiplier.start(), matcherMultiplier.end()).trim();
				multiplierString = multiplierString.replace("(", "");
				multiplierString = multiplierString.replace(")", "");
				
		    	if (logActivated) Logging.printLogDebug(logger, "-- Found MULTIPLIER: " + multiplierString + "\n    ");
		        try {
					pm.setMultiplier(Integer.parseInt(multiplierString.substring(0, multiplierString.indexOf(" "))));
					
					// Atualizando extra...
					if (logActivated) Logging.printLogDebug(logger, pm.getExtra() + "' -> '");
			        String newExtra = pm.getExtra().substring(0, matcherMultiplier.start()) + pm.getExtra().substring(matcherMultiplier.end(), pm.getExtra().length());
			        pm.setExtra(newExtra.trim());
			    	if (logActivated) Logging.printLogDebug(logger, pm.getExtra() + "'");
			    	
			    } catch (Exception e) {
			    	if (logActivated) Logging.printLogError(logger, "-- Error trying to convert multipler to integer: " + multiplierString + "\n' -> '");
				}
			}
			
			// Caso típico 5 -> kit com 10
			patternMultiplier = Pattern.compile("(kit)(\\s)(com)(\\s)(\\d+)");
			matcherMultiplier = patternMultiplier.matcher(pm.getExtra());
			    
			while (matcherMultiplier.find()) {
				multiplierString = pm.getExtra().substring(matcherMultiplier.start(), matcherMultiplier.end()).trim();
							
			   	if (logActivated) Logging.printLogDebug(logger, "-- Found MULTIPLIER: " + multiplierString + "\n    ");
		        try {
					pm.setMultiplier(Integer.parseInt(multiplierString.substring(multiplierString.lastIndexOf(" "), multiplierString.length()).trim()));
								
					// Atualizando extra...
					if (logActivated) Logging.printLogDebug(logger, pm.getExtra() + "' -> '");
			        String newExtra = pm.getExtra().substring(0, matcherMultiplier.start()) + pm.getExtra().substring(matcherMultiplier.end(), pm.getExtra().length());
			        pm.setExtra(newExtra.trim());
			    	if (logActivated) Logging.printLogDebug(logger, pm.getExtra() + "'");
				    	
			    } catch (Exception e) {
			    	if (logActivated) Logging.printLogError(logger, "-- Error trying to convert multipler to integer: " + multiplierString + "\n' -> '");
				}
			}
			
		} catch (Exception e) {
			if (logActivated) Logging.printLogError(logger, "Error! ");
		}
	}
	
	/**
	 * Método responsável pela extração da quantidade e da unidade do produtos
	 * @author Doug
	 * @param pm - ProcessModel que contém a unidade e quantidade à ser extraída
	 * @category Manipulação
	 */	
	public void extractUnitAndQuantity(ProcessedModel pm) {
		try {
			if (logActivated) Logging.printLogInfo(logger, "\n---> Extracting unit and quantity...");
			
			// Definindo padrão
			pm.setUnit(null);
			pm.setQuantity(null);
			
			int regexStartPosition = 99999999;
			
			for (String unit: this.unitsList) {
				// Sentença de verificação de ocorrência de casos como '4,4 ml', etc...
				Pattern pattern = Pattern.compile("([0-9]+)([,.][0-9]{1,2})?([ ])" + Pattern.quote(unit) + "(\\s|$)");

			    Matcher matcher = pattern.matcher(pm.getExtra());
			    while (matcher.find()) {
			    	if (logActivated) Logging.printLogDebug(logger, "-- Found QUANTITY/UNIT: " + unit + "\n' -> '");
			    	
			        String quantityString;
			        Double quantity;
			        
			        // Caso a quandtidade seja nula, a String de quantidade será vazia
			        quantityString = matcher.group(1) + (matcher.group(2)!=null ? matcher.group(2) : "");
			        // Substituição de de vírgula por ponto
			        quantityString = quantityString.replace(",", ".").trim();
			        // Conversão da String para Double, por isso a necessidade da substituição acima
			        quantity = Double.parseDouble(quantityString);
			        
			        // Se está mais a esquerda de uma eventual regex encontrada antes
			        // Exemplo: refrigerante coca-cola 2,0lt compre e ganhe uma de 350ml gelada
			        // Temos que pegar o 2,0lt ...
			        if(matcher.start() < regexStartPosition) {
			        	// Salva a primeira ocorrência
			        	regexStartPosition = matcher.start();
			        	// Define quantidade e unidade
			        	pm.setQuantity(quantity);
				        pm.setUnit(unit);
				        
				        // Atualizando extra...
				        String newExtra = pm.getExtra().substring(0, matcher.start()) + pm.getExtra().substring(matcher.end(), pm.getExtra().length());
				        pm.setExtra(newExtra.trim());
			        }
			    }
			}
			
			// Unificando unidades e quantidades
			if(pm.getUnit() != null && pm.getQuantity() != null) {
				// Se os mililitros ou gramas ultrapassarem 1000, mensura-se com a unidade superior (litros ou kilos)
				if(pm.getUnit().equals("mililitros") && pm.getQuantity() > 1000) {
					pm.setUnit("litros");
					pm.setQuantity(pm.getQuantity() / 1000);
				} else if(pm.getUnit().equals("gramas") && pm.getQuantity() > 1000) {
					pm.setUnit("kilogramas");
					pm.setQuantity(pm.getQuantity() / 1000);
				}
				
			}
			
		} catch (Exception e) {
			if (logActivated) Logging.printLogError(logger, "Error! ");
		}
	}
	
	/**
	 * Método responsável pela higienização ou padronização do extra
	 * @author Doug
	 * @param pm - ProcessModel que contém o extra
	 * @category Manipulação
	 */	
	public void sanitizeExtra(ProcessedModel pm) {
		try {
			if (logActivated) Logging.printLogInfo(logger, "\n---> Sanitizing extra...");
			
			String newExtra = pm.getExtra();
			// Retirando palavras que não tem nenhum significado semântico
			newExtra = newExtra.replace("leve mais pague menos", " ");
			newExtra = newExtra.replace("leve mais e pague menos", " ");
			newExtra = newExtra.replace("l+p-", "");
			newExtra = newExtra.replace("l p-", ""); //l+p- sem o +
			newExtra = newExtra.replace("promoção", " ");
			newExtra = newExtra.replace("promocao", " ");
			newExtra = newExtra.replace("promoçao", " ");
			newExtra = newExtra.replace("desconto", " ");
			
			// Define o extra
			pm.setExtra(newExtra);
			
			
		} catch (Exception e) {
			if (logActivated) Logging.printLogError(logger, "Error! ");
		}
	}
	
	/**
	 * Método responsável pela automatização de remoção de termos do extra 
	 * @param stringToBeRemoved Termo a ser removido do extra
	 * @param extra
	 * @return String com extra livre do termo não desejado
	 */	
	protected static String removeExtraFromString(String stringToBeRemoved, String extra) {
		String newExtra = extra;
		
		// Se encontrar o termo no começo... Remove o começo de extra
		Pattern beginning = Pattern.compile("^" + Pattern.quote(stringToBeRemoved));
		Matcher matcherBeginning = beginning.matcher(newExtra);
		while (matcherBeginning.find()) {
			newExtra = newExtra.substring(matcherBeginning.end());
		}
		
		// Se encontrar o termo no final... Remove o final da String
		Pattern ending = Pattern.compile(Pattern.quote(stringToBeRemoved) + "$");
		Matcher matcherEnding = ending.matcher(newExtra);
		while (matcherEnding.find()) {
			newExtra = newExtra.substring(0, matcherEnding.start());
		}
		
		// Se encontrar no meio da sentença...
		if (newExtra.contains(" " + stringToBeRemoved + " ")) {
			newExtra = newExtra.replace(" " + stringToBeRemoved + " ", " ");
	    }
		
		// Remoção de espaços duplos
		while (newExtra.contains("  "))  newExtra = newExtra.replace("  ", " ");
		
		// remoçaõ de espaços vazios nas extramidades
		newExtra = newExtra.trim();

		return newExtra;
	}
	
	
}
