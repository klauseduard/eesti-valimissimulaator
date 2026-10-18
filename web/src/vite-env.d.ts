/// <reference types="svelte" />
/// <reference types="vite/client" />

declare module '@andmed/*?raw' {
  const sisu: string;
  export default sisu;
}

declare module '@veebiandmed/toetused.json' {
  const küsitlused: Record<
    string,
    { küsitleja: string; kuupäev: string; viga: [number, number]; erakonnad: string[]; protsendid: number[][] }
  >;
  export default küsitlused;
}

/** ./gradlew kandidaaditest */
declare module '@andmed/kandidaadimudel.json' {
  const mudel: {
    vaikimisi: boolean;
    b: number;
    c: number;
    d: number;
    kappa: number;
    testitulemus: { kohti: number; lihtne: number; kalibreeritud: number };
  };
  export default mudel;
}

/** ./gradlew tagasitest; vead on dispersioonid logaritmilisel skaalal */
declare module '@andmed/kusitlused/koondmudel.json' {
  interface Horisont {
    ühine: number;
    majaefekt: number;
    küsitlejad: Record<string, number>;
  }
  const mudel: { meetod: string; veamudelKehtib: boolean; korrelatsioonidKehtivad: boolean; horisondid: Record<string, Horisont> };
  export default mudel;
}
