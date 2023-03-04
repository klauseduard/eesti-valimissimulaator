package andmed


import mudel.Erakond
import mudel.Valimisringkond


val MANDAATE_KOKKU = 101

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
      1 to 3.7,
      2 to 3.0,
      3 to 5.2,
      4 to 7.4,
      5 to 9.2,
      6 to 7.4,
      7 to 3.5,
      8 to 19.4,
      9 to 11.8,
      10 to 7.3,
      11 to 4.4,
      12 to 7.6,
    )
  ),
  Erakond(
    KONSERVATIIVID, mapOf(
      1 to 7.5,
      2 to 8.6,
      3 to 6.9,
      4 to 9.0,
      5 to 18.4,
      6 to 20.8,
      7 to 3.5,
      8 to 25.4,
      9 to 19.8,
      10 to 8.9,
      11 to 27.3,
      12 to 20.2,
    )
  ),
  Erakond(
    REFORMIERAKOND, mapOf(
      1 to 21.5,
      2 to 21.1,
      3 to 32.9,
      4 to 43.4,
      5 to 30.6,
      6 to 36.7,
      7 to 15.4,
      8 to 25.4,
      9 to 33.0,
      10 to 40.8,
      11 to 21.0,
      12 to 37.7,
    )
  ),
  Erakond(
    KESKERAKOND, mapOf(
      1 to 19.3,
      2 to 38.2,
      3 to 18.9,
      4 to 11.2,
      5 to 5.0,
      6 to 11.1,
      7 to 30.4,
      8 to 3.8,
      9 to 9.2,
      10 to 6.6,
      11 to 12.8,
      12 to 6.4,
    )
  ),
  Erakond(
    E200, mapOf(
      1 to 22.2,
      2 to 12.1,
      3 to 15.5,
      4 to 16.5,
      5 to 21.0,
      6 to 11.0,
      7 to 16.0,
      8 to 13.4,
      9 to 12.1,
      10 to 11.3,
      11 to 14.7,
      12 to 14.8,
    )
  ),
  Erakond(
    SDE, mapOf(
      1 to 15.9,
      2 to 9.9,
      3 to 13.7,
      4 to 6.6,
      5 to 12.2,
      6 to 10.9,
      7 to 8.7,
      8 to 11.1,
      9 to 5.3,
      10 to 19.8,
      11 to 17.1,
      12 to 9.8,
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