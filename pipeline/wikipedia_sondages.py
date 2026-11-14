"""Sondages publiés sur Wikipédia : 2e source du robot, quand 2027 et moi n'a pas encore saisi un sondage.

Page lue : « Liste de sondages sur l'élection présidentielle française de 2027 » (Wikipédia en français,
CC BY-SA 4.0). Les tableaux du 1er tour donnent une ligne par hypothèse ; ceux du 2nd tour, un tableau par duel.
Chaque ligne renvoie à la notice ou au rapport de l'institut : ce lien devient la source du sondage.

Une ligne incohérente (date, échantillon, total des scores) est écartée et signalée, jamais corrigée.
Rien ici ne touche au réseau, sauf telecharger_page().
"""

from __future__ import annotations

import datetime as dt
import hashlib
import json
import re
import unicodedata
import urllib.request
from html.parser import HTMLParser

PAGE = "Liste de sondages sur l'élection présidentielle française de 2027"
PAGE_URL = "https://fr.wikipedia.org/wiki/Liste_de_sondages_sur_l%27%C3%A9lection_pr%C3%A9sidentielle_fran%C3%A7aise_de_2027"
API = (
    "https://fr.wikipedia.org/w/api.php?action=parse&prop=text&formatversion=2&format=json&page="
    "Liste_de_sondages_sur_l%27%C3%A9lection_pr%C3%A9sidentielle_fran%C3%A7aise_de_2027"
)
LICENCE = "CC BY-SA 4.0"
ORIGINE = "Wikipédia"
# Sondages plus anciens : hors de la période suivie par l'appli (2027 et moi commence à la mi-août 2026).
DEPUIS = dt.date(2026, 8, 1)

MOIS = {
    "janvier": 1, "janv": 1, "février": 2, "fevrier": 2, "févr": 2, "fevr": 2, "fév": 2, "mars": 3, "avril": 4,
    "avr": 4, "mai": 5, "juin": 6, "juillet": 7, "juil": 7, "août": 8, "aout": 8, "septembre": 9, "sept": 9,
    "sep": 9, "octobre": 10, "oct": 10, "novembre": 11, "nov": 11, "décembre": 12, "decembre": 12, "déc": 12, "dec": 12,
}

# Familles d'instituts : « Harris » sur Wikipédia = « Toluna Harris Interactive » chez 2027 et moi, etc.
FAMILLES = [
    ("harris", "harris"), ("toluna", "harris"), ("ipsos", "ipsos"), ("bva", "ipsos"), ("ifop", "ifop"),
    ("opinionway", "opinionway"), ("elabe", "elabe"), ("odoxa", "odoxa"), ("csa", "csa"), ("yougov", "yougov"),
    ("cluster17", "cluster17"), ("viavoice", "viavoice"), ("verian", "verian"), ("kantar", "verian"),
]


def telecharger_page(user_agent: str) -> str:
    req = urllib.request.Request(API, headers={"User-Agent": user_agent})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.loads(r.read().decode("utf-8"))["parse"]["text"]


# ---------------------------------------------------------------- lecture du HTML


class Cellule:
    def __init__(self, entete: bool, rowspan: int, colspan: int):
        self.entete = entete
        self.rowspan = rowspan
        self.colspan = colspan
        self.texte = ""
        self.liens: list[dict] = []  # {"href", "title", "texte"}


class _Lecteur(HTMLParser):
    """Relève les titres de section et les tableaux « wikitable », cellule par cellule.
    Les appels de note (« [1] ») et les blocs cachés sont ignorés."""

    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.titres: dict[int, str] = {}
        self.tableaux: list[tuple[list[str], list[list[Cellule]]]] = []
        self._titre_niveau = 0
        self._titre_texte = ""
        self._table_profondeur = 0
        self._lignes: list[list[Cellule]] | None = None
        self._cellule: Cellule | None = None
        self._lien: dict | None = None
        self._ignore = 0  # profondeur dans <sup class=reference> ou <style>
        self._ignore_tag = ""

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        if self._ignore:
            if tag == self._ignore_tag:
                self._ignore += 1
            return
        if tag == "style" or (tag == "sup" and "reference" in (a.get("class") or "")):
            self._ignore, self._ignore_tag = 1, tag
            return
        if tag in ("h2", "h3", "h4", "h5", "h6"):
            self._titre_niveau, self._titre_texte = int(tag[1]), ""
        elif tag == "table":
            self._table_profondeur += 1
            if self._table_profondeur == 1 and "wikitable" in (a.get("class") or ""):
                self._lignes = []
        elif self._lignes is not None and self._table_profondeur == 1:
            if tag == "tr":
                self._lignes.append([])
            elif tag in ("td", "th") and self._lignes:
                self._cellule = Cellule(tag == "th", _entier(a.get("rowspan"), 1), _entier(a.get("colspan"), 1))
                self._lignes[-1].append(self._cellule)
            elif tag == "a" and self._cellule is not None:
                self._lien = {"href": a.get("href") or "", "title": a.get("title") or "", "texte": ""}
            elif tag == "br" and self._cellule is not None:
                self._cellule.texte += " "

    def handle_endtag(self, tag):
        if self._ignore:
            if tag == self._ignore_tag:
                self._ignore -= 1
            return
        if tag in ("h2", "h3", "h4", "h5", "h6") and self._titre_niveau:
            n = self._titre_niveau
            self.titres = {k: v for k, v in self.titres.items() if k < n}
            self.titres[n] = self._titre_texte.strip()
            self._titre_niveau = 0
        elif tag == "table":
            if self._table_profondeur == 1 and self._lignes is not None:
                self.tableaux.append(([self.titres[k] for k in sorted(self.titres)], self._lignes))
                self._lignes = None
            self._table_profondeur -= 1
        elif tag == "a" and self._lien is not None and self._cellule is not None:
            self._cellule.liens.append(self._lien)
            self._lien = None
        elif tag in ("td", "th"):
            self._cellule = None

    def handle_data(self, data):
        if self._ignore:
            return
        if self._titre_niveau:
            self._titre_texte += data
        if self._cellule is not None:
            self._cellule.texte += data
            if self._lien is not None:
                self._lien["texte"] += data


def _entier(v, defaut):
    try:
        return max(1, int(str(v).strip()))
    except (TypeError, ValueError):
        return defaut


def grille(lignes: list[list[Cellule]]) -> list[list[Cellule | None]]:
    """Déplie les cellules fusionnées : une même cellule (même objet) occupe toutes ses cases."""
    sortie: list[list[Cellule | None]] = []
    attente: dict[tuple[int, int], Cellule] = {}
    for i, ligne in enumerate(lignes):
        rangee: list[Cellule | None] = []
        j = 0
        cellules = iter(ligne)
        while True:
            if (i, j) in attente:
                rangee.append(attente.pop((i, j)))
                j += 1
                continue
            c = next(cellules, None)
            if c is None:
                break
            for dj in range(c.colspan):
                rangee.append(c)
                for di in range(1, c.rowspan):
                    attente[(i + di, j + dj)] = c
            j += c.colspan
        while (i, j) in attente:
            rangee.append(attente.pop((i, j)))
            j += 1
        sortie.append(rangee)
    return sortie


def lire_tableaux(html: str):
    lecteur = _Lecteur()
    lecteur.feed(html)
    return [(titres, grille(lignes)) for titres, lignes in lecteur.tableaux]


# ---------------------------------------------------------------- valeurs


def sans_accent(s: str) -> str:
    return "".join(ch for ch in unicodedata.normalize("NFD", s) if unicodedata.category(ch) != "Mn")


def cle_nom(s: str) -> str:
    return re.sub(r"[^a-z]", "", sans_accent(s).lower())


def famille(institut: str) -> str:
    k = cle_nom(institut.replace("17", "dixsept")).replace("dixsept", "17")
    for mot, fam in FAMILLES:
        if mot in k:
            return fam
    return k


def nombre(texte: str) -> float | None:
    t = texte.replace("\xa0", " ").strip()
    m = re.fullmatch(r"<?\s*(\d{1,3}(?:[,.]\d+)?)\s*%?", t)
    return float(m.group(1).replace(",", ".")) if m else None


def echantillon(texte: str) -> int | None:
    chiffres = re.sub(r"[^\d]", "", texte)
    return int(chiffres) if chiffres and 100 <= int(chiffres) <= 100_000 else None


_MOT_MOIS = r"([a-zéèûô]+)\.?"


def dates(texte: str, annee_section: int | None) -> tuple[dt.date, dt.date] | None:
    """« 12-13 novembre », « 28 octobre-2 novembre 2026 », « 1er-3 déc. 2026 », « 14 novembre 2026 »."""
    t = (texte.lower().replace("\xa0", " ").replace("1er", "1").replace("–", "-").replace(" au ", "-"))
    m = re.search(r"(\d{1,2})\s*(?:" + _MOT_MOIS + r")?\s*-\s*(\d{1,2})\s*" + _MOT_MOIS + r"\s*(\d{4})?", t)
    if m:
        j1, mois1, j2, mois2, annee = m.groups()
    else:
        m = re.search(r"(\d{1,2})\s*" + _MOT_MOIS + r"\s*(\d{4})?", t)
        if not m:
            return None
        j1, mois1, annee = m.groups()
        j2, mois2 = j1, mois1
    m2 = MOIS.get(mois2 or "")
    m1 = MOIS.get(mois1) if mois1 else m2
    a = int(annee) if annee else annee_section
    if not (m1 and m2 and a):
        return None
    try:
        fin = dt.date(a, m2, int(j2))
        debut = dt.date(a - 1 if m1 > m2 else a, m1, int(j1))
    except ValueError:
        return None
    return (debut, fin) if debut <= fin else None


def annee_de(titres: list[str]) -> int | None:
    for t in reversed(titres):
        m = re.search(r"(20\d\d)", t)
        if m:
            return int(m.group(1))
    return None


def personne(lien: dict) -> str | None:
    """Nom d'une personnalité d'après un lien interne (« Jean-Luc Mélenchon »), sans précision entre parenthèses."""
    if not lien["href"].startswith("/wiki/") or ":" in lien["href"][6:]:
        return None
    nom = re.sub(r"\s*\(.*\)$", "", lien["title"]).strip()
    return nom or None


def lien_externe(c: Cellule | None) -> dict | None:
    if c is None:
        return None
    for l in c.liens:
        if l["href"].startswith("http"):
            return l
    return None


# ---------------------------------------------------------------- noms → identifiants


class Annuaire:
    """Rattache un nom de Wikipédia à un identifiant de l'appli : candidat connu, nom déjà cité dans un sondage,
    sinon nouvel identifiant « nom de famille » ajouté aux noms hors liste."""

    def __init__(self, candidats: dict[str, str], hors_liste: dict[str, str]):
        self.par_cle = {cle_nom(nom): cid for cid, nom in {**hors_liste, **candidats}.items()}
        self.nouveaux: dict[str, str] = {}
        self.ids = set(candidats) | set(hors_liste)

    def id(self, nom: str) -> str:
        k = cle_nom(nom)
        if k in self.par_cle:
            return self.par_cle[k]
        mots = sans_accent(nom).lower().replace("'", " ").split()
        mots = mots[1:] or mots  # sans le prénom ; « de Villepin » → « villepin », « Le Pen » → « le-pen »
        while len(mots) > 1 and mots[0] in ("de", "du", "d"):
            mots = mots[1:]
        base = "-".join(mots)
        base = re.sub(r"[^a-z-]", "", base) or k
        cid = base
        n = 2
        while cid in self.ids:
            cid, n = f"{base}-{n}", n + 1
        self.ids.add(cid)
        self.par_cle[k] = cid
        self.nouveaux[cid] = nom
        return cid


# ---------------------------------------------------------------- tableaux → sondages


def _colonnes_noms(g) -> tuple[int, dict[int, str | None], int | None] | None:
    """Trouve la ligne des noms : celle où des cases d'en-tête renvoient à des personnalités.
    Renvoie (indice de la première ligne de données, {colonne: nom}, colonne « Autres »).
    Une colonne « Candidat RN » vaut None : le nom de la personnalité testée est dans chaque case."""
    for i, rangee in enumerate(g[:5]):
        noms: dict[int, str | None] = {}
        autres = None
        for j, c in enumerate(rangee):
            if c is None or not c.entete or j < 3:
                continue
            texte = c.texte.strip().lower()
            if texte.startswith("autres"):
                autres = j
            elif texte.startswith("candidat"):
                noms[j] = None
            else:
                p = personne(c.liens[0]) if c.liens else None
                if p:
                    noms[j] = p
        if sum(1 for n in noms.values() if n) >= 2:
            k = i + 1
            while k < len(g) and lien_externe(g[k][0] if g[k] else None) is None:
                k += 1
            return k, noms, autres
    return None


def _chiffres_et_noms(c: Cellule) -> tuple[list[float], list[str]]:
    """« 2,5 Villepin » → ([2.5], ["Dominique de Villepin"]) : nom du lien s'il y en a un, sinon le texte."""
    valeurs = [float(x.replace(",", ".")) for x in re.findall(r"\d{1,2}(?:,\d+)?", c.texte)]
    gens = [p for p in (personne(l) for l in c.liens) if p]
    if not gens:
        gens = [m.strip() for m in re.findall(r"[^\d,.\s—–-][^\d]*", c.texte) if m.strip()]
    return valeurs, gens


def _scores_ligne(rangee, noms: dict[int, str | None], autres: int | None, annuaire: Annuaire):
    """Scores nommés d'une ligne, et total de tous les chiffres lus (y compris « Autres » sans nom).
    Une case fusionnée sur plusieurs colonnes n'est comptée qu'une fois."""
    scores, total = {}, 0.0
    vues: list[Cellule] = []
    for j in sorted(noms):
        c = rangee[j] if j < len(rangee) else None
        if c is None or any(c is v for v in vues):
            continue
        vues.append(c)
        if noms[j] is None:
            valeurs, gens = _chiffres_et_noms(c)
            total += sum(valeurs)
            if len(valeurs) == 1 and len(gens) == 1:
                scores[annuaire.id(gens[0])] = valeurs[0]
            continue
        v = nombre(c.texte)
        if v is not None:
            total += v
            cid = annuaire.id(noms[j])
            if cid in scores:  # deux cases distinctes sous un même nom : ligne ambiguë, le total la fera écarter
                total += 100
            scores[cid] = v
    if autres is not None and autres < len(rangee) and rangee[autres] is not None:
        valeurs, gens = _chiffres_et_noms(rangee[autres])
        total += sum(valeurs)
        # « 2,5 Villepin » : un chiffre par personnalité nommée, dans l'ordre ; sinon on ne nomme personne.
        if gens and len(gens) == len(valeurs):
            for p, v in zip(gens, valeurs):
                scores[annuaire.id(p)] = v
    return scores, total


def _id(tour: int, fam: str, debut: dt.date, fin: dt.date) -> str:
    return hashlib.sha1(f"wikipedia|{tour}|{fam}|{debut}|{fin}".encode("utf-8")).hexdigest()[:12]


def _sondage(tour, sondeur: dict, d, ech) -> dict:
    debut, fin = d
    return {
        "id": _id(tour, famille(sondeur["texte"]), debut, fin),
        "institut": sondeur["texte"].strip(),
        "commanditaire": "",
        "terrain_debut": debut.isoformat(),
        "terrain_fin": fin.isoformat(),
        "echantillon": ech,
        "source": {"titre": f"Lien cité par Wikipédia ({sondeur['texte'].strip()})", "url": sondeur["href"], "date": ""},
        "origine": ORIGINE,
        "hypotheses": [],
    }


def sondages_wikipedia(html: str, candidats: dict[str, str], hors_liste: dict[str, str]):
    """Renvoie (1er tour, 2nd tour, nouveaux noms hors liste, lignes écartées)."""
    annuaire = Annuaire(candidats, hors_liste)
    premier: dict[str, dict] = {}
    second: dict[str, dict] = {}
    ecartees: list[str] = []
    for titres, g in lire_tableaux(html):
        texte_titres = " ".join(titres).lower()
        tour = 2 if "second tour" in texte_titres else 1 if "premier tour" in texte_titres else 0
        # « Autres » : variantes (vote blanc compté) et sondages de l'élection précédente, hors sujet ici.
        if not tour or titres[-1].strip().lower() == "autres":
            continue
        lu = _colonnes_noms(g)
        if lu is None:
            continue
        debut_donnees, noms, autres = lu
        if tour == 2 and len(noms) != 2:
            continue
        annee = annee_de(titres)
        for rangee in g[debut_donnees:]:
            if len(rangee) < 3:
                continue
            sondeur = lien_externe(rangee[0])
            if sondeur is None or not sondeur["texte"].strip():
                continue
            ou = f"{sondeur['texte'].strip()} « {rangee[1].texte.strip() if rangee[1] else ''} »"
            d = dates(rangee[1].texte if rangee[1] else "", annee)
            if d is None:
                ecartees.append(f"{ou} : dates illisibles")
                continue
            if d[1] < DEPUIS:
                continue
            ech = echantillon(rangee[2].texte if rangee[2] else "")
            scores, total = _scores_ligne(rangee, noms, autres if tour == 1 else None, annuaire)
            if tour == 1:
                if len(scores) < 5 or not 90 <= total <= 105:
                    ecartees.append(f"{ou} : 1er tour incomplet ({len(scores)} scores, total {total:g})")
                    continue
                s = premier.setdefault(_id(1, famille(sondeur["texte"]), *d), _sondage(1, sondeur, d, ech))
                s["hypotheses"].append({"label": f"Hypothèse {len(s['hypotheses']) + 1}", "scores": scores})
            else:
                if len(scores) != 2 or not 98 <= total <= 102:
                    ecartees.append(f"{ou} : 2nd tour incohérent (total {total:g})")
                    continue
                ids = list(scores)
                libelle = " – ".join(annuaire_nom(annuaire, candidats, hors_liste, i) for i in ids)
                s = second.setdefault(_id(2, famille(sondeur["texte"]), *d), _sondage(2, sondeur, d, ech))
                if all(h["label"] != libelle for h in s["hypotheses"]):
                    s["hypotheses"].append({"label": libelle, "scores": scores})
    return list(premier.values()), list(second.values()), annuaire.nouveaux, ecartees


def annuaire_nom(annuaire: Annuaire, candidats: dict, hors_liste: dict, cid: str) -> str:
    """Nom de famille pour le titre d'un duel (« Philippe – Le Pen »)."""
    nom = candidats.get(cid) or hors_liste.get(cid) or annuaire.nouveaux.get(cid) or cid
    mots = nom.split()
    return " ".join(mots[1:]) if len(mots) > 1 else nom
