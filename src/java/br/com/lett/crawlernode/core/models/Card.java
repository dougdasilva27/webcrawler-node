package br.com.lett.crawlernode.core.models;

public enum Card {

   VISA, MASTERCARD, MAESTRO, AMEX, DINERS, CREDICARD, ELO, HIPERCARD, HIPER, AURA, BNDES, DISCOVER, HSCARD, CABAL, SENFF, CREDISHOP, NARANJA, NATIVA, JCB, MULTICASH, MULTIEMPRESARIAL, MULTICHEQUE, MULTIBENEFICIOS, SOROCRED,

   SHOP_CARD, UNKNOWN_CARD;

   @Override
   public String toString() {
      switch (this) {
         case VISA:
            return "Visa";
         case MASTERCARD:
            return "Mastercard";
         case MAESTRO:
            return "Maestro";
         case AMEX:
            return "Amex";
         case DINERS:
            return "Diners";
         case CREDICARD:
            return "Credicard";
         case ELO:
            return "Elo";
         case HIPERCARD:
            return "Hipercard";
         case HIPER:
            return "Hiper";
         case AURA:
            return "Aura";
         case BNDES:
            return "Bndes";
         case DISCOVER:
            return "Discover";
         case SHOP_CARD:
            return "Shop_Card";
         case HSCARD:
            return "HSCard";
         case CABAL:
            return "Cabal";
         case SENFF:
            return "Senff";
         case CREDISHOP:
            return "Credishop";
         case NARANJA:
            return "Naranja";
         case NATIVA:
            return "Nativa";
         case MULTICASH:
            return "Multicash";
         case JCB:
            return "JCB";
         case MULTIEMPRESARIAL:
            return "Multiempresarial";
         case MULTICHEQUE:
            return "Multicheque";
         case MULTIBENEFICIOS:
            return "Multibeneficios";
         case SOROCRED:
            return "Sorocred";
         default:
            return "unkown_card";
      }
   }

}
