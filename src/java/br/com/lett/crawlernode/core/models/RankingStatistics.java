package br.com.lett.crawlernode.core.models;

public class RankingStatistics {

   private int pageSize;
   private int totalSearch;
   private int totalFetched;

   public int getPageSize() {
      return pageSize;
   }

   public void setPageSize(int pageSize) {
      this.pageSize = pageSize;
   }

   public int getTotalSearch() {
      return totalSearch;
   }

   public void setTotalSearch(int totalSearch) {
      this.totalSearch = totalSearch;
   }

   public int getTotalFetched() {
      return totalFetched;
   }

   public void setTotalFetched(int totalFetched) {
      this.totalFetched = totalFetched;
   }
}
