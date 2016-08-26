package br.com.lett.crawlernode.processor.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Processor model - Brand
 * 
 * @author Samir Leão
 *
 */

public class BrandModel {
	
	private String brand;
	private String supplier;
	private List<String> mistake;
	
	public BrandModel(String brand, String supplier) {
		this.brand = brand;
		
		// Evita a insersão de fabricante nulo
		if(supplier == null) {
			this.supplier = "";
		}
		else {
			this.supplier = supplier;
		}
		
		this.mistake = new ArrayList<String>();
	}
	
	/**
	 * Padronização de exibição de dados do modelo
	 */
	public String toString() {
		if(mistake.isEmpty() && supplier.isEmpty()) { 
			return "Class: " + brand;		
		}
		else if(mistake.isEmpty()) {
			return "Class: " + brand + "\nSupplier: " + supplier;
		}
		else if(supplier.isEmpty()) {
			return "Class: " + brand +
					"\nList mistakes: " + mistake;
		}
		else {
			return "Class: " + brand + "\nSupplier: " + supplier + "\nList mistakes: " + mistake;
		}
	}
	
	/**
	 * Método de insersão de valor na lista de erros comuns.
	 * @param mistake = erro a inserir
	 * @return = retorna se foi inserido ou não
	 */
	public boolean putOnList(String mistake) {
		// Evita a insersão de um valor nulo
		if(mistake == null || mistake.isEmpty()) {
			return false;
		}
		else {
			this.mistake.add(mistake);
			return true;
		}
	}
	
	public String getBrand() {
		return brand;
	}
	
	public void setBrand(String brand) {
		this.brand = brand;
	}
	
	public String getSupplier() {
		return supplier;
	}
	
	public void setSupplier(String supplier) {
		this.supplier = supplier;
	}
	
	public List<String> getMistake() {
		return mistake;
	}
	
	public void setMistake(List<String> mistake) {
		this.mistake = mistake;
	}
	
	public void putOnList(List<String> addMistake) {
		mistake.addAll(addMistake);
	}
}
