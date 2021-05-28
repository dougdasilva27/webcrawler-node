package br.com.lett.crawlernode.crawlers.corecontent.saopaulo

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.Tintasmc

class SaopauloTintasmcviladasaudeCrawler(session: Session) : Tintasmc(session) {

   override fun setJsonLocation(): String = locationJson

   companion object {
      val locationJson = """
            {
              "lat": -23.618162,
              "lng": -46.6278979,
              "full_address": "Av. Prof. Abraão de Morais - Vila da Saúde, São Paulo - SP, Brasil",
              "address": {
                "address": "Avenida Professor Abraão de Morais",
                "neighborhood": "Vila da Saúde",
                "city": "São Paulo",
                "state": "SP",
                "_state": "São Paulo",
                "country": "BR",
                "_country": "Brasil"
              },
              "place_id": "EkVBdi4gUHJvZi4gQWJyYcOjbyBkZSBNb3JhaXMgLSBWaWxhIGRhIFNhw7pkZSwgU8OjbyBQYXVsbyAtIFNQLCBCcmF6aWwiLiosChQKEgln_Q4LAVvOlBER2R-K_eNztRIUChIJKccVtlNazpQRPtrD0ItR-z4"
            }
         """.trimIndent()
   }
}
