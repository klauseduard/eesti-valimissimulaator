package andmed

import mudel.Erakond
import mudel.Valimisringkond

/** Käesolevas failis on algandmed, mida tekstifailidesse viia ei raatsinud. */

/* konstant mis sisaldab hääletanute arvu ringkonna kaupa, aluseks 2019. aasta parlamendivalimised */
val hääletanud = mapOf(
  1 to 53136,
  2 to 71406,
  3 to 50823,
  4 to 87957,
  5 to 34291,
  6 to 28705,
  7 to 37975,
  8 to 41324,
  9 to 40793,
  10 to 46229,
  11 to 46976,
  12 to 40659,
)

// konstandid erakondade nimedega
val ISAMAA_ERAKOND = "ISAMAA Erakond"
val KONSERVATIIVID = "Eesti Konservatiivne Rahvaerakond"
val REFORMIERAKOND = "Eesti Reformierakond"
val KESKERAKOND = "Eesti Keskerakond"
val SDE = "Sotsiaaldemokraatlik Erakond"
val E200 = "Erakond Eesti 200"
val ROHELISED = "Erakond Eestimaa Rohelised"
val PAREMPOOLSED = "Erakond Parempoolsed"

/**
 * Konstant, mis sisaldab erakondade toetusi valimisringkondades ERRis avaldatud küsitlusandmete põhjal.
 * Siin ei ole ära toodud vea määrasid, mis teeksid andmed usaldusväärsemaks ja algoritmid keerulisemaks.
 */
val erakonnad = arrayOf(
  Erakond(
    ISAMAA_ERAKOND, mapOf(
      1 to 5.9,
      2 to 3.0,
      3 to 6.7,
      4 to 7.3,
      5 to 3.9,
      6 to 8.7,
      7 to 6.2,
      8 to 11.9,
      9 to 16.3,
      10 to 12.1,
      11 to 6.6,
      12 to 6.0,
    )
  ),
  Erakond(
    KONSERVATIIVID, mapOf(
      1 to 12.1,
      2 to 9.4,
      3 to 18.0,
      4 to 13.7,
      5 to 21.0,
      6 to 15.6,
      7 to 16.4,
      8 to 20.2,
      9 to 22.6,
      10 to 20.1,
      11 to 27.1,
      12 to 31.8,
    )
  ),
  Erakond(
    REFORMIERAKOND, mapOf(
      1 to 29.2,
      2 to 27.1,
      3 to 32.7,
      4 to 44.6,
      5 to 35.3,
      6 to 37.8,
      7 to 16.2,
      8 to 31.0,
      9 to 31.5,
      10 to 31.6,
      11 to 26.4,
      12 to 32.0,
    )
  ),
  Erakond(
    KESKERAKOND, mapOf(
      1 to 17.5,
      2 to 34.2,
      3 to 20.5,
      4 to 8.8,
      5 to 10.7,
      6 to 12.8,
      7 to 24.9,
      8 to 13.8,
      9 to 7.5,
      10 to 4.8,
      11 to 12.5,
      12 to 10.4,
    )
  ),
  Erakond(
    E200, mapOf(
      1 to 13.7,
      2 to 8.2,
      3 to 11.9,
      4 to 13.8,
      5 to 14.9,
      6 to 11.4,
      7 to 15.9,
      8 to 12.7,
      9 to 13.0,
      10 to 12.4,
      11 to 8.1,
      12 to 10.0,
    )
  ),
  Erakond(
    SDE, mapOf(
      1 to 11.9,
      2 to 10.9,
      3 to 5.2,
      4 to 4.8,
      5 to 10.6,
      6 to 6.7,
      7 to 9.5,
      8 to 5.6,
      9 to 4.0,
      10 to 9.1,
      11 to 11.9,
      12 to 5.6,
    )
  ),
)

/* konstant, mis sisaldab valimisringkondade andmestruktuuride massiivi */
val valimisringkonnad = arrayOf(
  Valimisringkond(1, "Tallinna Haabersti, Põhja-Tallinna ja Kristiine linnaosa", 10),
  Valimisringkond(2, "Tallinna Kesklinna, Lasnamäe ja Pirita linnaosa", 13),
  Valimisringkond(3, "Tallinna Mustamäe ja Nõmme linnaosa", 8),
  Valimisringkond(4, "Harju- ja Raplamaa", 16),
  Valimisringkond(5, "Hiiu-, Lääne- ja Saaremaa", 6),
  Valimisringkond(6, "Lääne-Virumaa", 5),
  Valimisringkond(7, "Ida-Virumaa", 6),
  Valimisringkond(8, "Järva- ja Viljandimaa", 7),
  Valimisringkond(9, "Jõgeva- ja Tartumaa", 7),
  Valimisringkond(10, "Tartu linn", 8),
  Valimisringkond(11, "Võru-, Valga- ja Põlvamaa", 8),
  Valimisringkond(12, "Pärnumaa", 7),
)