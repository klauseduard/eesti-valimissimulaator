#!/usr/bin/env python3
# > **Märkus tehisintellekti kohta.** Selle faili teksti kirjutas tehisintellekt (Claude, Anthropic, tööriistas Claude Code) 2026. aasta oktoobris.
"""Laadib Vikipeediast alla Riigikogu valimiste arvamusküsitluste tabelid ja
ametlikud tulemused ning kirjutab need CSV-failidesse (vt LOEMIND.md).

Kasutamine (ainult Python 3 standardteek):

    python3 tooma.py                 # kasutab LOEMIND.md-s nimetatud versioone (oldid)
    python3 tooma.py --uusim         # kasutab lehtede uusimaid versioone
    python3 tooma.py --vahemalu DIR  # salvestab/loeb vikiteksti kataloogist DIR
    python3 tooma.py --valjund DIR   # kirjutab CSV-failid kataloogi DIR (vaikimisi skripti kataloog)

Parsimine on automaatne, kuid käsitsi kontrollitud. Leitud vead ja
allikate erinevused on parandatud selgesõnaliselt loendis PARANDUSED;
iga parandus kirjutatakse ka vastava rea veergu `markus`.
Uusima versiooni kasutamisel võivad parandused enam mitte kehtida -
skript teatab, kui mõni parandus ei leidnud oma rida.
"""
import argparse
import ast
import csv
import datetime as dt
import json
import operator
import os
import re
import sys
import urllib.parse
import urllib.request
from collections import Counter

# ---------------------------------------------------------------------------
# Allikad: võti -> (viki, lehe pealkiri, kasutatud versioon)
LEHED = {
    'en_rk2011': ('en', '2011 Estonian parliamentary election', 1366534953),
    'en_rk2015': ('en', '2015 Estonian parliamentary election', 1366534818),
    'en_rk2019': ('en', '2019 Estonian parliamentary election', 1374695601),
    'en_rk2023': ('en', '2023 Estonian parliamentary election', 1374857369),
    'en_op2019': ('en', 'Opinion polling for the 2019 Estonian parliamentary election', 1306346084),
    'en_op2023': ('en', 'Opinion polling for the 2023 Estonian parliamentary election', 1375859751),
    'en_opnext': ('en', 'Opinion polling for the next Estonian parliamentary election', 1378822267),
    'et_rk2019': ('et', '2019. aasta Riigikogu valimised', 7205073),
    'et_vahe2023': ('et', 'Valimistevaheliste arvamusküsitluste tulemused aastatel 2019–2023', 6670803),
    'et_vahe2027': ('et', 'Valimistevaheliste arvamusküsitluste tulemused aastatel 2023–2027', 7251364),
}

# Küsitlustabelid: võti -> (leht, tabeli pealkirjas olev lõik või None, kas aasta tuleb lõigu pealkirjast)
# Tabelid valitakse lõigu järgi, kuhu nad kuuluvad (ainult üleriigilised tabelid).
TABELID = {
    'en2011': ('en_rk2011', r'^==\s*Opinion polls', False),
    'en2015': ('en_rk2015', r'^==\s*Opinion polls', False),
    'en2019': ('en_op2019', r'^===\s*\d{4}', True),
    'et2019': ('et_rk2019', r'^===\s*Erakondlikud eelistused', False),
    'en2023': ('en_op2023', r'^===\s*\d{4}', True),
    'et2023': ('et_vahe2023', r'^==\s*Erakondlikud eelistused', False),
    'ennext': ('en_opnext', r'^===\s*\d{4}', True),
    'etnext': ('et_vahe2027', r'^==\s*Erakonnaeelistused', False),
}

# Väljundfail -> (põhiallikas, ristkontrolli allikas, eelmise valimise päev, valimispäev,
#                kas lisada read, mis on ainult ristkontrolli allikas)
VALJUNDID = {
    'rk2011.csv': ('en2011', None, '2007-03-04', '2011-03-06', False),
    'rk2015.csv': ('en2015', None, '2011-03-06', '2015-03-01', False),
    'rk2019.csv': ('et2019', 'en2019', '2015-03-01', '2019-03-03', True),
    'rk2023.csv': ('en2023', 'et2023', '2019-03-03', '2023-03-05', True),
    'rk2027.csv': ('ennext', 'etnext', '2023-03-05', '2027-12-31', True),
}

# Ülesande põhiveerud on failis esimesed ja samas järjekorras; lisaveerud on lõpus.
ERAKONNAD = ['REF', 'KESK', 'EKRE', 'ISAMAA', 'SDE', 'E200', 'VABA', 'ROH', 'PP',
             'EUVP', 'RAHVALIIT', 'MUUD', 'KOOS', 'ERK']
VEERUD = ['kusitleja', 'algus', 'lopp', 'valim'] + ERAKONNAD + \
         ['alus', 'tyyp', 'lopp_liik', 'valim_tekst', 'allikas', 'markus']

# Tabeli päise silt -> veerukood. None = veerg jäetakse välja.
SILDID = [
    (r'^(ref|reform)$', 'REF'), (r'^kesk$', 'KESK'), (r'^ekre$', 'EKRE'),
    (r'^(isamaa|i|irl)$', 'ISAMAA'), (r'^sde$', 'SDE'), (r'^(e200|eesti 200)$', 'E200'),
    (r'^(eer|green|greens|rohelised|rohe)$', 'ROH'), (r'^(parem|parempoolsed)$', 'PP'),
    (r'^(eva|vaba)$', 'VABA'),
    (r'^tule$', 'VABA'),  # Eesti Tulevikuerakond, Vabaerakonna ja Elurikkuse Erakonna ühinemisel 2020
    (r'^(eüvp|vl|vasakliit)$', 'EUVP'),  # Eesti Vasakliit = endine EÜVP
    (r'^erl$', 'RAHVALIIT'), (r'^koos$', 'KOOS'), (r'^erk$', 'ERK'),
    (r'^(others|muud|teised)$', 'MUUD'),
    (r'^(polling firm|allikas|uuringufirma|küsitleja)$', 'FIRMA'),
    (r'^(date|fieldwork date|kuupäev|küsitlusperiood)$', 'KUUPAEV'),
    (r'^(sample size|valim)$', 'VALIM'),
    (r'^(lead|gov\.|opp\.|valitsus|valitsus – opositsioon)$', None),
    (r'cabinet', None),
]

# Käsitsi kontrollitud parandused: (tabel, küsitleja, lõpp, väli, uus väärtus, põhjendus)
PARANDUSED = [
    ('en2023', 'Norstat', '2021-12-07', 'REF', '25.8',
     'en-viki REF 22.3; et-viki 25.8; 4 nädala libisev keskmine (reitingud.ee) ja ridade summa (96.5) toetavad et-vikit'),
    ('en2023', 'Norstat', '2022-03-14', 'E200', '16.2',
     'en-viki E200 18.8; et-viki 16.2; 4 nädala libisev keskmine (reitingud.ee) ja ridade summa (102.7) toetavad et-vikit'),
    ('en2023', 'Faktum & Ariko', '2023-02-27', 'algus', '2023-02-10',
     'en-viki algus 20.02; ERR 02.03.2023 ja et-viki: 10.-27.02.2023'),
    ('ennext', 'Norstat', '2024-08-04', 'algus', '2024-07-29',
     'en-viki "23-4 Aug" (13 päeva); et-viki 29.07.-04.08.2024'),
    ('ennext', 'Norstat', '2026-03-01', 'algus', '2026-02-23',
     'en-viki "13-1 March" (17 päeva); et-viki 23.02.-01.03.2026'),
]

# Ridade väljajätmine: (tabel, küsitleja, algus, lõpp, tekst küsitleja lahtris, põhjendus)
VALJAJAETUD = [
    ('et2019', 'Turu-uuringute AS', '2019-02-07', '2019-02-20', 'veebiküsitlus',
     'sama küsitluse alamtulemus; alles jäetud koondtulemus'),
    ('et2019', 'Turu-uuringute AS', '2019-02-07', '2019-02-20', 'näost-näkku',
     'sama küsitluse alamtulemus; alles jäetud koondtulemus'),
    ('en2023', 'Norstat', '2019-03-18', '2019-04-15', 'Norstat',
     '4 nädala libisev keskmine (valim 4014); samad vastajad on nädala ridades'),
]

# Read, mis on ainult ristkontrolli allikas ja on käsitsi üle vaadatud (tabel, küsitleja, lõpp).
# Teised sellised read jäetakse välja: näiteks et-viki 2019-2023 tabelis on kaks 2022. aasta
# Turu-uuringute küsitlust märgitud aastasse 2021 ning need on en-vikis juba olemas.
LISATUD = {
    ('en2019', 'Turu-uuringute AS', '2017-05-22'),
    ('et2023', 'Turu-uuringute AS', '2023-02-10'),
    ('et2023', 'Turu-uuringute AS', '2019-04-15'),
    ('etnext', 'Kantar Emor', '2024-06-02'),
    ('etnext', 'Kantar Emor', '2026-08-19'),
    ('etnext', 'Norstat', '2026-09-05'),
    ('etnext', 'Turu-uuringute AS', '2026-09-07'),
    ('etnext', 'Norstat', '2026-09-13'),
    ('etnext', 'Norstat', '2026-09-19'),
    ('etnext', 'Kantar Emor', '2026-09-23'),
    ('etnext', 'Norstat', '2026-10-03'),
    ('etnext', 'Turu-uuringute AS', '2026-10-07'),
}

# Märkused konkreetsetele ridadele (tabel, küsitleja, lõpp, tekst)
MARKUSED = [
    ('en2023', 'Norstat', '2023-03-03',
     'et-viki samal nädalal (27.02.-03.03.) REF 32.5 KESK 15.4 EKRE 16.2 ISAMAA 7.3 SDE 7.8 E200 14.5'),
    ('en2023', 'Kantar Emor', '2023-03-02',
     'erakondade reiting (ERR 03.03.2023); kandidaatidega küsitlus: REF 30.3 KESK 15.7 EKRE 13.6 E200 15.1'),
    ('en2023', 'Kantar Emor', '2023-02-16',
     'erakondade reiting; ERR avaldas ka kandidaatidega küsitluse (et-viki: REF 31.4 KESK 16.4 EKRE 16.9)'),
    ('en2023', 'Faktum & Ariko', '2023-02-27',
     'valim 959 valimisvalmis vastajat, neist 846 nimetas eelistuse (ERR); MUUD = üksikkandidaadid 2 + EÜVP 1'),
    ('en2011', 'Turu-uuringute AS', '2011-01',
     'osakaalud kõigist vastajatest (summa 78); otsustamata vastajad ei ole tabelis'),
]

# ---------------------------------------------------------------------------
# Allalaadimine


def lae(key, uusim, vahemalu):
    viki, pealkiri, oldid = LEHED[key]
    fail = os.path.join(vahemalu, key + '.json') if vahemalu else None
    if fail and os.path.exists(fail) and not uusim:
        with open(fail, encoding='utf-8') as f:
            d = json.load(f)
    else:
        param = {'action': 'parse', 'prop': 'wikitext|revid', 'format': 'json', 'redirects': '1'}
        if uusim:
            param['page'] = pealkiri
        else:
            param['oldid'] = str(oldid)
        url = 'https://%s.wikipedia.org/w/api.php?%s' % (viki, urllib.parse.urlencode(param))
        req = urllib.request.Request(url, headers={'User-Agent': 'eesti-valimissimulaator/1.0 (kusitluste kogumine)'})
        with urllib.request.urlopen(req, timeout=60) as r:
            d = json.loads(r.read().decode('utf-8'))
        if 'error' in d:
            raise SystemExit('%s: %s' % (key, d['error']))
        if fail:
            os.makedirs(vahemalu, exist_ok=True)
            with open(fail, 'w', encoding='utf-8') as f:
                json.dump(d, f, ensure_ascii=False)
    return d['parse']['revid'], d['parse']['wikitext']['*']

# ---------------------------------------------------------------------------
# Vikiteksti tabelite parsimine


def jaota(s, sep):
    """Jagab s eraldaja sep kohalt, mis ei ole {{ }} ega [[ ]] sees."""
    out, sk, nk, buf, i = [], 0, 0, [], 0
    while i < len(s):
        if s.startswith('{{', i):
            sk += 1; buf.append('{{'); i += 2; continue
        if s.startswith('}}', i) and sk:
            sk -= 1; buf.append('}}'); i += 2; continue
        if s.startswith('[[', i):
            nk += 1; buf.append('[['); i += 2; continue
        if s.startswith(']]', i) and nk:
            nk -= 1; buf.append(']]'); i += 2; continue
        if sk == 0 and nk == 0 and s.startswith(sep, i):
            out.append(''.join(buf)); buf = []; i += len(sep); continue
        buf.append(s[i]); i += 1
    out.append(''.join(buf))
    return out


def tabelid(tekst):
    """Tagastab [(algusrea indeks, tabeli tekst)]."""
    res, read, cur, algus, sugavus = [], tekst.split('\n'), None, 0, 0
    for nr, rida in enumerate(read):
        st = rida.lstrip()
        if st.startswith('{|'):
            if sugavus == 0:
                cur, algus = [], nr
            sugavus += 1
        if sugavus:
            cur.append(rida)
        if st.startswith('|}'):
            sugavus -= 1
            if sugavus == 0:
                res.append((algus, '\n'.join(cur)))
    return res


def eemalda_viited(s):
    s = re.sub(r'<ref[^>/]*/>', '', s)
    s = re.sub(r'<ref[^>]*>.*?</ref>', '', s, flags=re.S)
    return re.sub(r'<!--.*?-->', '', s, flags=re.S)


def lahtrid(tabel):
    """Tabeli read lahtrite loendina [(päis?, atribuudid, väärtus)]."""
    read = eemalda_viited(tabel).split('\n')[1:]
    osad = []
    for rida in read:
        st = rida.strip()
        if st.startswith('|}'):
            break
        if st.startswith('|-'):
            osad.append(('RIDA', st))
        elif st.startswith('|+'):
            continue
        elif st.startswith('|') or st.startswith('!'):
            osad.append(('LAHTER', st))
        elif osad and osad[-1][0] == 'LAHTER':
            osad[-1] = ('LAHTER', osad[-1][1] + '\n' + st)
    tulemus, cur = [], []
    for liik, st in osad:
        if liik == 'RIDA':
            tulemus.append(cur); cur = []
            continue
        pais = st[0] == '!'
        sisu = st[1:]
        tykid = jaota(sisu, '!!' if pais else '||')
        if pais and len(tykid) == 1:
            tykid = jaota(sisu, '||')
        for t in tykid:
            alam = jaota(t, '|')
            atr, v = (alam[0], '|'.join(alam[1:])) if len(alam) >= 2 else ('', alam[0])
            cur.append((pais, atr.strip(), v.strip()))
    tulemus.append(cur)
    return [r for r in tulemus if r]


def laienda(read):
    """Rakendab rowspan/colspan atribuudid."""
    vork, ootel = [], {}
    for r in read:
        out, col, ci = [], 0, 0
        while ci < len(r) or col in ootel:
            if col in ootel:
                jaanud, lahter = ootel[col]
                out.append(lahter)
                if jaanud > 1:
                    ootel[col] = (jaanud - 1, lahter)
                else:
                    del ootel[col]
                col += 1
                continue
            lahter = r[ci]; ci += 1
            m = re.search(r'colspan\s*=\s*"?(\d+)', lahter[1]); cs = int(m.group(1)) if m else 1
            m = re.search(r'rowspan\s*=\s*"?(\d+)', lahter[1]); rs = int(m.group(1)) if m else 1
            for _ in range(cs):
                out.append(lahter)
                if rs > 1:
                    ootel[col] = (rs - 1, lahter)
                col += 1
        vork.append(out)
    return vork


_OP = {ast.Add: operator.add, ast.Sub: operator.sub, ast.Mult: operator.mul,
       ast.Div: operator.truediv, ast.USub: operator.neg, ast.UAdd: operator.pos}


def _arvuta(n):
    if isinstance(n, ast.Expression):
        return _arvuta(n.body)
    if isinstance(n, ast.Constant):
        return n.value
    if isinstance(n, ast.BinOp):
        return _OP[type(n.op)](_arvuta(n.left), _arvuta(n.right))
    if isinstance(n, ast.UnaryOp):
        return _OP[type(n.op)](_arvuta(n.operand))
    raise ValueError(n)


def expr(avaldis):
    """MediaWiki {{#expr: ... round N}} (ümardamine nullist eemale)."""
    m = re.match(r'(.*?)\s+round\s+(\d+)\s*$', avaldis.strip())
    kohti = None
    if m:
        avaldis, kohti = m.group(1), int(m.group(2))
    v = _arvuta(ast.parse(avaldis.strip(), mode='eval'))
    if kohti is None:
        return repr(v)
    q = 10 ** kohti
    v = int(abs(v) * q + 0.5 + 1e-9) / q * (1 if v >= 0 else -1)
    return ('%.' + str(kohti) + 'f') % v


def puhasta(v):
    """Vikimärgendus -> tavatekst."""
    s = re.sub(r'\{\{(?:snd|ndash|spaced ndash|spnd)\}\}', '–', v)

    def _op(m):
        p = ([x.strip() for x in m.group(1).split('|')] + ['', '', '', ''])[:4]
        return ('%s-%s %s %s' % tuple(p)) if p[0] else ('%s %s %s' % (p[1], p[2], p[3]))
    s = re.sub(r'\{\{opdrts\|([^{}]*)\}\}', _op, s)
    s = re.sub(r'\{\{#expr:\s*(.*?)\}\}', lambda m: expr(m.group(1)), s)
    s = re.sub(r'\{\{(?:efn|refn|efn-ua|efn-lr|NoteTag)\b(?:[^{}]|\{\{[^{}]*\}\})*\}\}', '', s, flags=re.I)
    for _ in range(3):
        s = re.sub(r'\{\{(?:small|nowrap|yes|no|nobr|big|abbr|tooltip|sort)\|([^{}|]*)(?:\|[^{}]*)?\}\}', r'\1', s, flags=re.I)
    s = re.sub(r'\{\{[^{}]*\}\}', '', s)
    s = re.sub(r'\[\[(?:[^|\]]*\|)?([^\]]*)\]\]', r'\1', s)
    s = re.sub(r'\[(?:https?:)?//\S+\s+([^\]]*)\]', r'\1', s)
    s = re.sub(r'\[(?:https?:)?//\S+\]', '', s)
    s = re.sub(r"'''?", '', s)
    s = re.sub(r'<br\s*/?>', ' ', s)
    s = re.sub(r'<[^>]+>', '', s)
    s = s.replace('&nbsp;', ' ').replace(' ', ' ')
    return re.sub(r'\s+', ' ', s).strip()

# ---------------------------------------------------------------------------
# Väljade tõlgendamine


EN_KUU = {m: i + 1 for i, m in enumerate(
    ['jan', 'feb', 'mar', 'apr', 'may', 'jun', 'jul', 'aug', 'sep', 'oct', 'nov', 'dec'])}
ET_KUU = (('jaan', 1), ('veeb', 2), ('mär', 3), ('apr', 4), ('mai', 5), ('juuni', 6), ('juuli', 7),
          ('aug', 8), ('sept', 9), ('okt', 10), ('nov', 11), ('dets', 12))


def kuu(t):
    t = t.lower().rstrip('.,')
    if t[:3] in EN_KUU:
        return EN_KUU[t[:3]]
    for k, v in ET_KUU:
        if t.startswith(k):
            return v
    return None


def kuupaevad(s, aasta):
    """Tekst -> (algus, lopp, tõlgendatud?). Kuu täpsusega kuupäev: lopp = 'YYYY-MM'."""
    algne = s
    s = re.sub(r'\s*[-–—−]\s*', '-', s).replace('.', ' ').replace(',', ' ')
    s = re.sub(r'\s+', ' ', s).strip()
    osad = s.split('-')
    if len(osad) > 2:
        raise ValueError('kuupäev: ' + algne)

    def tykk(p):
        toks = p.split()
        d = m = y = None
        for t in toks:
            if re.fullmatch(r'\d{4}', t):
                y = int(t)
            elif re.fullmatch(r'\d{1,2}', t):
                if m is not None:  # "Jan 11", "25 Feb 11": kahekohaline aasta
                    y = 2000 + int(t)
                else:
                    d = int(t)
            else:
                m = kuu(t)
                if m is None:
                    raise ValueError('kuu %r: %s' % (t, algne))
        return d, m, y
    t = [tykk(p) for p in osad]
    if len(t) == 1:
        d, m, y = t[0]
        y = y or aasta
        if m is None or y is None:
            raise ValueError('kuupäev: ' + algne)
        if d is None:
            return '', '%04d-%02d' % (y, m), False
        return '', dt.date(y, m, d).isoformat(), False
    (d1, m1, y1), (d2, m2, y2) = t
    y2 = y2 or aasta
    if d1 is None and d2 is None and m1 and m2 and y2:  # "nov-dets 2022"
        y1 = y1 or (y2 if m1 <= m2 else y2 - 1)
        return '%04d-%02d' % (y1, m1), '%04d-%02d' % (y2, m2), False
    if m2 is None or d1 is None or d2 is None or y2 is None:
        raise ValueError('kuupäev: ' + algne)
    tolgendatud = False
    if m1 is None:
        m1 = m2
        if d1 > d2:  # "31-6 Apr" = 31. märts - 6. aprill
            m1 -= 1
            tolgendatud = True
            if m1 == 0:
                m1, y1 = 12, y2 - 1
    if y1 is None:
        y1 = y2 if (m1, d1) <= (m2, d2) else y2 - 1
    return dt.date(y1, m1, d1).isoformat(), dt.date(y2, m2, d2).isoformat(), tolgendatud


def kusitleja(s):
    l = s.lower()
    for voti, nimi in (('norstat', 'Norstat'), ('emor', 'Kantar Emor'), ('turu', 'Turu-uuringute AS'),
                       ('faktum', 'Faktum & Ariko'), ('saar', 'Saar Poll'), ('ariko', 'Ariko')):
        if voti in l:
            return nimi
    if 'election' in l or 'valimistulemus' in l:
        return 'VALIMISED'
    return None


def protsent(s):
    s = s.replace(',', '.').replace('%', '').strip()
    if s in ('', '-', '–', '—', '−', 'N/A', 'n/a', '?') or s.lower() == 'did not exist':
        return ''
    if not re.fullmatch(r'\d+(\.\d+)?', s):
        raise ValueError('protsent: %r' % s)
    return s


def valim(s):
    t = s.replace(',', '').replace(' ', '').replace(' ', '')
    if not t or t.upper() == 'N/A' or t in ('–', '-', '—'):
        return '', ''
    if re.fullmatch(r'\d+', t):
        return t, ''
    m = re.fullmatch(r'(?:≥|>|~|ca|u|umbes|üle|vähemalt)?\.?(\d+)(?:[–-]\d+)?', t)
    if m:  # alampiir või ligikaudne arv; algne tekst veergu valim_tekst
        return m.group(1), s
    raise ValueError('valim: %r' % s)


def silt(s):
    l = s.strip().lower()
    for muster, kood in SILDID:
        if re.search(muster, l):
            return kood
    raise ValueError('tundmatu päise silt: %r' % s)


def loe_tabel(tekst, loigu_muster, aasta_loigust):
    read_tekstis = tekst.split('\n')
    kirjed = []
    for algus, t in tabelid(tekst):
        pealkirjad = [l for l in read_tekstis[:algus] if re.match(r'^=+[^=]', l)]
        if not pealkirjad or not re.search(loigu_muster, pealkirjad[-1]):
            continue
        aasta = None
        if aasta_loigust:
            aasta = int(re.search(r'(\d{4})', pealkirjad[-1]).group(1))
        vork = laienda(lahtrid(t))
        pais = [silt(puhasta(v)) for _, _, v in vork[0]]
        for r in vork:
            if all(h for h, _, _ in r):
                continue
            if len(r) != len(pais):  # sündmuste read (valitsuse vahetus jms)
                continue
            raw = {k: v for k, (_, _, v) in zip(pais, r) if k}
            firma = kusitleja(puhasta(raw.get('FIRMA', '')))
            if firma is None:
                tekst_ = puhasta(raw.get('FIRMA', ''))
                arvud = [puhasta(raw.get(p, '')) for p in ERAKONNAD if p in raw]
                if tekst_ and any(re.fullmatch(r'[\d.,]+', a) for a in arvud):
                    raise ValueError('tundmatu küsitleja: %r' % tekst_)
                continue  # sündmuse rida
            if firma == 'VALIMISED':
                continue  # valimistulemuse ja mandaatide read
            m = re.search(r'\[(https?://[^\s\]]+)', raw['FIRMA'])
            raw_veerud = set(raw)
            k = {'firma': firma, 'firma_tekst': puhasta(raw['FIRMA']), 'raw': raw, 'url': m.group(1) if m else '', 'veerud': raw_veerud,
                 'tuletatud': any('#expr' in v or 'calculated numbers' in v for v in raw.values()),
                 'avaldamine': bool(re.search(r'publication date', raw.get('KUUPAEV', ''), re.I))}
            k['kuupaev_tekst'] = puhasta(raw['KUUPAEV'])
            k['algus'], k['lopp'], k['tolgendatud'] = kuupaevad(k['kuupaev_tekst'], aasta)
            k['kuu_vahemik'] = False
            if len(k['algus']) == 10 and k['algus'][8:] == '01' and k['algus'][:7] == k['lopp'][:7] \
                    and (paev(k['lopp']) + dt.timedelta(days=1)).day == 1:
                # "1-31 Jan": tõenäoliselt kuu, mitte välitöö täpsed päevad
                k['algus'], k['lopp'], k['kuu_vahemik'] = '', k['lopp'][:7], True
            k['valim'], k['valim_tekst'] = valim(puhasta(raw.get('VALIM', '')))
            for p in ERAKONNAD:
                try:
                    k[p] = protsent(puhasta(raw[p])) if p in raw else ''
                except ValueError:
                    if p != 'MUUD':
                        raise
                    k[p] = ''  # et-vikis on "Muud" mõnikord erakondade kaupa lahti kirjutatud
            kirjed.append(k)
    return kirjed

# ---------------------------------------------------------------------------
# Koostamine


def paev(s):
    if len(s) == 10:
        return dt.date.fromisoformat(s)
    if len(s) == 7:  # kuu: võrdluseks kuu keskpaik
        return dt.date(int(s[:4]), int(s[5:7]), 15)
    return None


def summa(k):
    return sum(float(k[p]) for p in ERAKONNAD if k[p] != '')


def sama_arvud(a, b):
    """Kas kaks rida on sama küsitlus? Täisarvuks ümardatud väärtuste puhul lubatakse 0.5."""
    yhised = [p for p in ERAKONNAD if p != 'MUUD' and a[p] != '' and b[p] != '']
    if len(yhised) < 5:
        return False
    for p in yhised:
        tol = 0.051 if ('.' in a[p] and '.' in b[p]) else 0.51
        if abs(float(a[p]) - float(b[p])) > tol:
            return False
    return True


def lahedal(x, y, paevi):
    return x is not None and y is not None and abs((x - y).days) <= paevi


def libisev(k):
    return (k['firma'] == 'Norstat' and len(k['algus']) == 10
            and (paev(k['lopp']) - paev(k['algus'])).days >= 20)


def leia_paar(k, teised, kasutatud, unikaalsed):
    """Leiab ristkontrolli allikast sama küsitluse."""
    x = paev(k['lopp'])
    pikk = libisev(k)
    kand = [(j, s) for j, s in enumerate(teised) if j not in kasutatud and s['firma'] == k['firma']
            and libisev(s) == pikk]  # nädala tulemust ei võrrelda 4 nädala keskmisega
    if k['url'] in unikaalsed:
        for j, s in kand:
            if s['url'] == k['url']:
                return j
    for paevi in (3, 45):
        for j, s in kand:
            if lahedal(x, paev(s['lopp']), paevi) and sama_arvud(k, s):
                return j
    if not k['avaldamine']:
        for j, s in kand:
            if lahedal(x, paev(s['lopp']), 3):
                return j
    return None


def koosta(tabelid_, pohi, rist, alates, kuni, lisa_puuduvad):
    kirjed = [dict(k) for k in tabelid_[pohi]]
    for k in kirjed:
        k['markus'] = []
        k['allikas'] = LEHED[TABELID[pohi][0]][0] + 'wiki'
    # väljajätmised
    valja = []
    for t, f, a, l, tekst, pohjus in VALJAJAETUD:
        if t != pohi:
            continue
        leitud = [k for k in kirjed if k['firma'] == f and k['algus'] == a and k['lopp'] == l
                  and tekst in k['firma_tekst']]
        if len(leitud) != 1:
            print('HOIATUS: väljajätmine ei leidnud rida:', t, f, l, tekst, file=sys.stderr)
        valja += leitud
    kirjed = [k for k in kirjed if not any(k is v for v in valja)]
    # parandused
    for t, f, l, vali, uus, pohjus in PARANDUSED:
        if t != pohi:
            continue
        leitud = [k for k in kirjed if k['firma'] == f and k['lopp'] == l]
        if len(leitud) != 1:
            print('HOIATUS: parandus ei leidnud rida:', t, f, l, vali, file=sys.stderr)
            continue
        k = leitud[0]
        k['markus'].append('parandatud %s: %s -> %s (%s)' % (vali, k[vali], uus, pohjus))
        k[vali] = uus
    # ristkontroll
    if rist:
        rviki = LEHED[TABELID[rist][0]][0]
        teised = [dict(k) for k in tabelid_[rist]]
        urlid = Counter(k['url'] for k in kirjed) + Counter(k['url'] for k in teised)
        unikaalsed = {u for u, n in urlid.items() if u and n == 2
                      and sum(1 for k in kirjed if k['url'] == u) == 1}
        kasutatud = set()
        for k in kirjed:
            j = leia_paar(k, teised, kasutatud, unikaalsed)
            if j is None:
                if k['avaldamine']:
                    k['markus'].append('lopp on avaldamise kuupäev, välitöö aeg teadmata')
                continue
            kasutatud.add(j)
            s = teised[j]
            if k['avaldamine']:
                if len(s['algus']) == 10:
                    k['markus'].append('%s-viki andis avaldamise kuupäeva %s; välitöö aeg %s-vikist (%s)'
                                       % (LEHED[TABELID[pohi][0]][0], k['lopp'], rviki, s['kuupaev_tekst']))
                    k['algus'], k['lopp'] = s['algus'], s['lopp']
                    k['avaldamine'] = False
                    k['allikas'] += '+%swiki' % rviki
                else:
                    k['markus'].append('lopp on avaldamise kuupäev; %s-vikis "%s"' % (rviki, s['kuupaev_tekst']))
            elif s['algus'] and k['algus']:
                erinevus = max(abs((paev(k['algus']) - paev(s['algus'])).days),
                               abs((paev(k['lopp']) - paev(s['lopp'])).days))
                if erinevus > 2 and (paev(s['lopp']) - paev(s['algus'])).days < 20:
                    k['markus'].append('%s-vikis välitöö %s' % (rviki, s['kuupaev_tekst']))
            if k['valim'] and s['valim'] and k['valim'] != s['valim'] and not (k['valim_tekst'] or s['valim_tekst']):
                k['markus'].append('valim %s-vikis %s' % (rviki, s['valim']))
            if not sama_arvud(k, s):
                erinev = ['%s %s' % (p, s[p] or '-') for p in ERAKONNAD if p != 'MUUD' and s[p] != k[p]
                          and p in s['veerud'] and p in k['veerud']
                          and not (s[p] and k[p] and abs(float(s[p]) - float(k[p])) < 0.051)]
                k['markus'].append('%s-vikis erinev: %s' % (rviki, ' '.join(erinev)))
        if lisa_puuduvad:
            for j, s in enumerate(teised):
                if j in kasutatud:
                    continue
                if (rist, s['firma'], s['lopp']) not in LISATUD:
                    print('ei lisatud (ainult %s-vikis, ei ole loendis LISATUD): %s %s %s'
                          % (rviki, s['firma'], s['algus'], s['lopp']), file=sys.stderr)
                    continue
                s['markus'] = ['ainult %s-vikis' % rviki]
                if rist == 'etnext':
                    s['markus'].append('et-vikis puuduvad veerud KOOS, ERK ja Muud')
                s['allikas'] = rviki + 'wiki'
                print('lisatud %s-vikist: %s %s %s' % (rviki, s['firma'], s['algus'], s['lopp']), file=sys.stderr)
                kirjed.append(s)
    for t, f, l, tekst in MARKUSED:
        if t == pohi:
            for k in kirjed:
                if k['firma'] == f and k['lopp'] == l:
                    k['markus'].append(tekst)
    # tüüp ja alus
    for k in kirjed:
        k['tyyp'] = ''
        k['alus'] = ''
        k['lopp_liik'] = 'avaldamine' if k['avaldamine'] else ''
        if k['kuu_vahemik']:
            k['markus'].append('vikis "%s" (terve kalendrikuu), salvestatud kuuna' % k['kuupaev_tekst'])
        if k['firma'] == 'Norstat' and k['algus']:
            paevi = (paev(k['lopp']) - paev(k['algus'])).days + 1
            k['tyyp'] = 'libisev_4n' if paevi >= 21 else 'nadal'
            if k['tuletatud']:
                k['tyyp'] = 'nadal_tuletatud'
                k['markus'].append('Vikipeedia arvutas nädala tulemuse 4 nädala libisevatest keskmistest')
        if k['firma'] == 'Faktum & Ariko':
            k['tyyp'] = 'prognoos'
        if summa(k) < 85:
            k['alus'] = 'koik_vastajad'
    # ajavahemik
    a, b = paev(alates), paev(kuni)
    valjas = [k for k in kirjed if not (a < paev(k['lopp']) < b)]
    if valjas:
        print('HOIATUS: %d rida väljaspool valimistevahelist aega:' % len(valjas),
              [(k['firma'], k['lopp']) for k in valjas], file=sys.stderr)
    kirjed = [k for k in kirjed if a < paev(k['lopp']) < b]
    kirjed.sort(key=lambda k: (k['lopp'], k['algus'], k['firma']), reverse=True)
    return kirjed


def kirjuta(fail, kirjed):
    with open(fail, 'w', encoding='utf-8', newline='') as f:
        w = csv.writer(f, lineterminator='\n')
        w.writerow(VEERUD)
        for k in kirjed:
            rida = dict(k)
            rida['kusitleja'] = k['firma']
            rida['markus'] = '; '.join(k['markus'])
            w.writerow([rida.get(v, '') for v in VEERUD])

# ---------------------------------------------------------------------------
# Tulemused


TULEMUSE_KOODID = [
    (r'Reform', 'REF'), (r'Centre Party', 'KESK'), (r'Conservative People', 'EKRE'),
    (r'Pro Patria and Res Publica|^\[\[Isamaa', 'ISAMAA'), (r'Social Democratic', 'SDE'),
    (r'Estonia 200', 'E200'), (r'Free Party', 'VABA'), (r'Greens', 'ROH'), (r'Parempoolsed', 'PP'),
    (r'United Left', 'EUVP'), (r"People's Union", 'RAHVALIIT'), (r'Russian Party', 'VENE'),
    (r'Christian Democrats', 'KRISTDEM'), (r'Independence Party', 'ISESEISVUS'),
    (r"People's Unity", 'RAHVAYHTSUS'), (r'Biodiversity', 'ELURIKKUS'), (r'Independents', 'YKSIK'),
]


def tulemused(tekst):
    m = re.search(r'\{\{Election results(.*?)\n\}\}', tekst, re.S)
    if not m:
        m = re.search(r'\{\{Election results(.*?)\}\}<section end', tekst, re.S)
    sisu = m.group(1)
    param = {}
    for tykk in jaota(sisu, '|'):
        if '=' in tykk:
            k, v = tykk.split('=', 1)
            param[k.strip()] = v.strip()
    read = []
    i = 1
    while 'party%d' % i in param:
        nimi = param['party%d' % i]
        kood = next((c for mu, c in TULEMUSE_KOODID if re.search(mu, nimi)), None)
        if kood is None:
            raise ValueError('tundmatu erakond tulemustes: %r' % nimi)
        read.append((kood, int(param['votes%d' % i].replace(',', ''))))
        i += 1
    return read


def kontrolli_2023(tulemused_2023, juur):
    fail = os.path.join(juur, '..', 'rk2023', 'tulemus.json')
    if not os.path.exists(fail):
        return
    with open(fail, encoding='utf-8') as f:
        d = json.load(f)
    kehtivaid = sum(d['kehtivaidHääli'].values())
    nimed = {'Eesti Reformierakond': 'REF', 'Eesti Konservatiivne Rahvaerakond': 'EKRE',
             'Eesti Keskerakond': 'KESK', 'Erakond Eesti 200': 'E200', 'Sotsiaaldemokraatlik Erakond': 'SDE',
             'ISAMAA Erakond': 'ISAMAA', 'EESTIMAA ÜHENDATUD VASAKPARTEI': 'EUVP',
             'Erakond Parempoolsed': 'PP', 'Üksikkandidaadid': 'YKSIK', 'Erakond Eestimaa Rohelised': 'ROH'}
    c = Counter()
    for k in d['kandidaadid']:
        c[nimed[k['erakond']]] += k['hääli']
    viki = dict(tulemused_2023)
    erinevus = {p: (viki.get(p), c[p]) for p in set(c) | set(viki) if viki.get(p) != c[p]}
    print('2023 tulemused vs tulemus.json: kehtivaid %d, erinevusi %s' % (kehtivaid, erinevus or 'pole'))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--uusim', action='store_true')
    ap.add_argument('--vahemalu')
    ap.add_argument('--valjund', default=os.path.dirname(os.path.abspath(__file__)))
    a = ap.parse_args()
    tekstid = {}
    for key in LEHED:
        revid, tekstid[key] = lae(key, a.uusim, a.vahemalu)
        print('%-12s revid %s  %s' % (key, revid, LEHED[key][1]))
    tab = {t: loe_tabel(tekstid[leht], muster, aasta) for t, (leht, muster, aasta) in TABELID.items()}
    for fail, (pohi, rist, alates, kuni, lisa) in VALJUNDID.items():
        kirjed = koosta(tab, pohi, rist, alates, kuni, lisa)
        kirjuta(os.path.join(a.valjund, fail), kirjed)
        print('%s: %d küsitlust' % (fail, len(kirjed)))
    with open(os.path.join(a.valjund, 'tulemused.csv'), 'w', encoding='utf-8', newline='') as f:
        w = csv.writer(f, lineterminator='\n')
        w.writerow(['aasta', 'erakond', 'protsent', 'haali'])
        for aasta in (2011, 2015, 2019, 2023):
            read = tulemused(tekstid['en_rk%d' % aasta])
            kehtivaid = sum(h for _, h in read)
            for kood, h in read:
                w.writerow([aasta, kood, '%.2f' % (100 * h / kehtivaid), h])
            print('tulemused %d: %d rida, kehtivaid hääli %d' % (aasta, len(read), kehtivaid))
            if aasta == 2023:
                kontrolli_2023(read, a.valjund)


if __name__ == '__main__':
    main()
