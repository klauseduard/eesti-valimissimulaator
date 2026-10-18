// Taustalõim: kordab simulatsiooni partiide kaupa ja saadab pärast iga partiid vahetulemuse.
import { Simulaator, ToetusJs, type KorduvSimulatsioonJs } from '@simulaator';
import { partiid, type SõnumTöölisele, type SõnumTöölselt } from './protokoll';
import { tavaliseks } from './tavaline';

let simulaator: Simulaator | null = null;
let praeguneId = -1;

function saada(sõnum: SõnumTöölselt) {
  postMessage(sõnum);
}

/** lubab enne järgmist partiid töödelda uusi sõnumeid (nt uus algus või peatamine) */
const oota = () => new Promise<void>((lahenda) => setTimeout(lahenda, 0));

async function kordaPartiidena(id: number, korduv: KorduvSimulatsioonJs, kordi: number, partii: number) {
  for (const suurus of partiid(kordi, partii)) {
    if (id !== praeguneId) return;
    korduv.korda(suurus);
    saada({ tüüp: 'vahetulemus', id, kokkuvõte: tavaliseks(korduv.kokkuvõte()), valmis: korduv.kordi >= kordi });
    await oota();
  }
}

onmessage = async (sündmus: MessageEvent<SõnumTöölisele>) => {
  const sõnum = sündmus.data;
  if (sõnum.tüüp === 'andmed') {
    simulaator = new Simulaator(sõnum.andmed.ringkondadeTekstid, sõnum.andmed.üleriigilisedNimekirjad, sõnum.andmed.rk2023);
  } else if (sõnum.tüüp === 'peata') {
    if (sõnum.id === praeguneId) praeguneId = -1;
  } else if (sõnum.tüüp === 'alusta') {
    praeguneId = sõnum.id;
    try {
      if (simulaator === null) throw new Error('Andmed puuduvad');
      const korduv = simulaator.korduv(new ToetusJs(sõnum.toetus.erakonnad, sõnum.toetus.protsendid), sõnum.seeme);
      await kordaPartiidena(sõnum.id, korduv, sõnum.kordi, sõnum.partii);
    } catch (viga) {
      saada({ tüüp: 'viga', id: sõnum.id, teade: viga instanceof Error ? viga.message : String(viga) });
    }
  }
};
