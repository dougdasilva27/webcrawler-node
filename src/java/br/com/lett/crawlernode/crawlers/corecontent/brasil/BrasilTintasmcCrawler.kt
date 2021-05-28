package br.com.lett.crawlernode.crawlers.corecontent.brasil

import br.com.lett.crawlernode.core.session.Session
import br.com.lett.crawlernode.crawlers.extractionutils.core.Tintasmc

class BrasilTintasmcCrawler(session: Session) : Tintasmc(session) {

   override fun setJsonLocation(): String = locationJson

   companion object {
      val locationJson = """
              {
                "lat": -23.5514188,
                "lng": -46.7211741,
                "full_address": "Av. das Nações Unidas - Alto de Pinheiros, São Paulo - SP, 05466, Brasil",
                "address": {
                  "address": "Avenida das Nações Unidas",
                  "neighborhood": "Alto de Pinheiros",
                  "city": "São Paulo",
                  "state": "SP",
                  "_state": "São Paulo",
                  "country": "BR",
                  "_country": "Brasil",
                  "postal_code": "05466"
                },
                "place_id": "EktBdi4gZGFzIE5hw6fDtWVzIFVuaWRhcyAtIEFsdG8gZGUgUGluaGVpcm9zLCBTw6NvIFBhdWxvIC0gU1AsIDA1NDY2LCBCcmF6aWwiLiosChQKEgm_RA79OlbOlBHcB5b-x2OnrhIUChIJ1aA4ijBWzpQRBXaLKuLHye8"
              }
           """.trimIndent()
   }
}
