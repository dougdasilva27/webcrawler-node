package br.com.lett.crawlernode.core.models;

import java.util.ArrayList;
import java.util.List;

public class CategoryCollection {
	
	private List<String> categories;
	
	public CategoryCollection() {
		this.categories = new ArrayList<String>();
	}
	
	public void add(String category) {
		if (!this.categories.contains(category)) {
			this.categories.add(category);
		}
	}
	
	public String getCategory(int n) {
		if (n < this.categories.size()) {
			return this.categories.get(n);
		}
		return "";
	}
	
}
