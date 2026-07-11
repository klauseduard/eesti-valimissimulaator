package mudel

data class Erakond(val nimetus: String, var toetusprotsentValimisringkonnas: Map<Int, Double>)

data class Valimisringkond(val nr: Int, val nimetus: String, val mandaatideArv: Int)

data class Kandidaat(val nimi: String, val erakond: String, val valimisringkond: Int, var hääli: Int = 0, var jrkNr: Int = 0,
                     /** "ringkond" või "kompensatsioon", kui kandidaat on mandaadi saanud; väli on toString'is viimane, et runs/ failide senine veerujärjestus säiliks */
                     var mandaadiliik: String = "")