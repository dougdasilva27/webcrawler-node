package br.com.lett.crawlernode.core.models;

import java.util.ArrayList;

public class CategoryCollection extends ArrayList<String> {

  public String getCategory(int n) {
    if (n < this.size()) {
      return this.get(n);
    }
    return "";
  }
}
