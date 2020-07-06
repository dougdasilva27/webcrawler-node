package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.models.Product
import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.core.task.impl.Crawler
import org.jsoup.nodes.Document

class BrasilPontonaturalCrawler(session: Session) : Crawler(session) {

  override fun extractInformation(document: Document): MutableList<Product> {
    return super.extractInformation(document)
  }
}
