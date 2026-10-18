/** Lihtkvoot: kehtivate häälte arv ringkonnas jagatud ringkonna mandaatide arvuga. */
fun lihtkvoot(kehtivaidHääli: Int, mandaatideArv: Int): Double {
  return kehtivaidHääli / mandaatideArv.toDouble()
}

/** Arvutab erakondade ringkonnamandaatide arvu ühes ringkonnas.
 * <p>
 *   Erakond saab nii mitu mandaati, mitu korda tema häälte arv ületab lihtkvooti. Kui jääk on vähemalt
 *   0,75 lihtkvoodist, saab erakond ühe mandaadi lisaks.
 * </p>
 *
 * @param erakondadeHääled - erakonna nimi -> erakonna kandidaatide häälte summa ringkonnas
 * @param kehtivaidHääli - kehtivate häälte arv ringkonnas
 * @param mandaatideArv - ringkonna mandaatide arv
 * @return erakonna nimi -> ringkonnamandaatide arv
 */
fun arvutaRingkonnaMandaadid(
  erakondadeHääled: Map<String, Int>,
  kehtivaidHääli: Int,
  mandaatideArv: Int
): Map<String, Int> {
  val kvoot = lihtkvoot(kehtivaidHääli, mandaatideArv)
  return erakondadeHääled.mapValues { (_, hääled) ->
    var mandaate = (hääled / kvoot).toInt()
    if (((hääled % kvoot) / kvoot) >= 0.75) {
      mandaate++
    }
    mandaate
  }
}
