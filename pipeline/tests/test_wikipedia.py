import datetime as dt
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import build  # noqa: E402
import wikipedia_sondages as w  # noqa: E402
from test_build import MAINTENANT, fausse_photo, source_min  # noqa: E402


def entete(nom, parti, colspan=1):
    return f'<th colspan="{colspan}"><a href="/wiki/{nom.replace(" ", "_")}" title="{nom}">{nom.split()[-1]}</a><br><small>(<a href="/wiki/{parti}" title="{parti}">{parti}</a>)</small></th>'


def page(lignes_t1: str, lignes_t2: str = "", autres: str = "") -> str:
    """Page au format de Wikipédia : en-têtes sur trois lignes, sondeur/date/échantillon fusionnés sur les hypothèses."""
    return f"""
<div class="mw-heading mw-heading2"><h2 id="p">Sondages concernant le premier tour</h2></div>
<div class="mw-heading mw-heading5"><h5 id="o">Octobre-décembre 2026</h5></div>
<table class="wikitable"><tbody>
<tr><th rowspan="3">Sondeur</th><th rowspan="3">Date</th><th rowspan="3">Échantillon</th>
<th><img alt="a"></th><th><img alt="b"></th><th colspan="2"><img alt="c"></th><th><img alt="d"></th><th><img alt="e"></th><th><img alt="f"></th>
<th rowspan="2">Autres</th></tr>
<tr>{entete("Nathalie Arthaud", "LO")}{entete("Jean-Luc Mélenchon", "LFI")}{entete("Raphaël Glucksmann", "PP", 2)}
{entete("Édouard Philippe", "HOR")}{entete("Marine Le Pen", "RN")}{entete("Éric Zemmour", "REC")}</tr>
<tr><td></td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
{lignes_t1}
</tbody></table>
<div class="mw-heading mw-heading3"><h3 id="a">Autres</h3></div>
<table class="wikitable"><tbody>
<tr><th>Sondeur</th><th>Dates</th><th>Échantillon</th>{entete("Nathalie Arthaud", "LO")}{entete("Marine Le Pen", "RN")}</tr>
{autres}
</tbody></table>
<div class="mw-heading mw-heading2"><h2 id="s">Sondages concernant le second tour</h2></div>
<div class="mw-heading mw-heading4"><h4 id="h">Hypothèse Philippe – Le Pen</h4></div>
<table class="wikitable"><tbody>
<tr><th rowspan="3">Sondeur</th><th rowspan="3">Dates</th><th rowspan="3">Échantillon</th><th><img alt="p"></th><th><img alt="l"></th></tr>
<tr>{entete("Édouard Philippe", "HOR")}{entete("Marine Le Pen", "RN")}</tr>
<tr><td></td><td></td></tr>
{lignes_t2}
</tbody></table>
"""


ELABE = """
<tr><td rowspan="2"><a class="external" href="https://elabe.fr/n.pdf">Elabe</a></td><td rowspan="2">12-13 novembre</td><td rowspan="2">1&#160;500</td>
<td>0,5</td><td><b>17</b></td><td colspan="2">13,5</td><td>15</td><td><b>47</b></td><td>4,5</td>
<td>2,5<br><small><b><a href="/wiki/Dominique_de_Villepin" title="Dominique de Villepin">Villepin</a></b></small><sup class="reference">[1]</sup></td></tr>
<tr><td>1</td><td>17,5</td><td colspan="2">14</td><td>—</td><td>48</td><td>19,5</td><td>—</td></tr>
"""
IFOP_DOUBLON = """
<tr><td><a class="external" href="https://cds/1.pdf">Ifop</a></td><td>17-19 octobre</td><td>2081</td>
<td>1</td><td>16</td><td colspan="2">12</td><td>16</td><td>51</td><td>4</td><td>—</td></tr>
"""
INCOMPLET = """
<tr><td><a class="external" href="https://x.fr/n.pdf">OpinionWay</a></td><td>10-12 novembre</td><td>964</td>
<td>1</td><td>16</td><td colspan="2">12</td><td>—</td><td>—</td><td>—</td><td>—</td></tr>
"""
DUELS = """
<tr><td><a class="external" href="https://elabe.fr/n.pdf">Elabe</a></td><td>12-13 novembre 2026</td><td>1500</td><td>45</td><td><b>55</b></td></tr>
<tr><td><a class="external" href="https://h.fr/n.pdf">Harris</a></td><td>3-5 novembre 2026</td><td>2101</td><td>42</td><td>58</td></tr>
<tr><td><a class="external" href="https://h.fr/x.pdf">Harris</a></td><td>3-5 novembre 2026</td><td>2101</td><td>42</td><td>50</td></tr>
"""
VOTE_BLANC = """
<tr><td><a class="external" href="https://cds/vb.pdf">Ifop</a></td><td>9-11 novembre</td><td>1548</td><td>40</td><td>60</td></tr>
"""
CANDIDATS = {"arthaud": "Nathalie Arthaud", "melenchon": "Jean-Luc Mélenchon", "le-pen": "Marine Le Pen", "zemmour": "Éric Zemmour"}


def lire(html):
    return w.sondages_wikipedia(html, CANDIDATS, {"glucksmann": "Raphaël Glucksmann"})


def test_premier_tour_une_ligne_par_hypothese():
    premier, _, nouveaux, ecartees = lire(page(ELABE))
    assert len(premier) == 1
    s = premier[0]
    assert (s["institut"], s["terrain_debut"], s["terrain_fin"], s["echantillon"]) == ("Elabe", "2026-11-12", "2026-11-13", 1500)
    assert s["origine"] == "Wikipédia" and s["source"]["url"] == "https://elabe.fr/n.pdf"
    h1, h2 = s["hypotheses"]
    assert h1["label"] == "Hypothèse 1"
    # Case fusionnée sur deux colonnes comptée une fois ; « 2,5 Villepin » rattaché à Villepin ; appel de note ignoré.
    assert h1["scores"] == {"arthaud": 0.5, "melenchon": 17, "glucksmann": 13.5, "philippe": 15, "le-pen": 47, "zemmour": 4.5, "villepin": 2.5}
    assert "philippe" not in h2["scores"] and h2["scores"]["le-pen"] == 48
    assert nouveaux == {"philippe": "Édouard Philippe", "villepin": "Dominique de Villepin"}
    assert ecartees == []


def test_ligne_incomplete_ecartee_et_section_autres_ignoree():
    premier, _, _, ecartees = lire(page(ELABE + INCOMPLET, autres=VOTE_BLANC))
    assert [s["institut"] for s in premier] == ["Elabe"]
    assert len(ecartees) == 1 and "OpinionWay" in ecartees[0]


def test_second_tour_un_duel_par_tableau():
    _, second, _, ecartees = lire(page(ELABE, DUELS))
    assert [s["institut"] for s in second] == ["Elabe", "Harris"]  # la 2e ligne Harris (total 92) est écartée
    s = second[0]
    assert (s["institut"], s["terrain_fin"]) == ("Elabe", "2026-11-13")
    assert s["hypotheses"] == [{"label": "Philippe – Le Pen", "scores": {"philippe": 45, "le-pen": 55}}]
    assert len(ecartees) == 1 and "Harris" in ecartees[0]


def test_dates():
    d = w.dates
    assert d("12-13 novembre", 2026) == (dt.date(2026, 11, 12), dt.date(2026, 11, 13))
    assert d("28 octobre-2 novembre 2026", None) == (dt.date(2026, 10, 28), dt.date(2026, 11, 2))
    assert d("1er-3 déc. 2026", None) == (dt.date(2026, 12, 1), dt.date(2026, 12, 3))
    assert d("30 décembre-2 janvier 2027", None) == (dt.date(2026, 12, 30), dt.date(2027, 1, 2))
    assert d("14 novembre 2026", None) == (dt.date(2026, 11, 14), dt.date(2026, 11, 14))
    assert d("fin novembre", 2026) is None


def test_familles_d_instituts():
    assert w.famille("Toluna Harris Interactive") == w.famille("Harris")
    assert w.famille("Ipsos bva") == w.famille("Ipsos")
    assert w.famille("Ifop-Fiducial") == w.famille("Ifop")
    assert w.famille("Cluster17") != w.famille("Elabe")


# ---------------------------------------------------------------- dans le robot


def lancer(tmp, src, html, precedent=None, nom="out"):
    out = tmp / nom
    rapport = build.executer(src, precedent, out, MAINTENANT + dt.timedelta(days=21), fausse_photo(), page_wikipedia=html)
    return out, rapport, json.loads((out / "app-data.json").read_text(encoding="utf-8"))


def test_wikipedia_complete_2027_et_moi(tmp_path):
    src = source_min(tmp_path)
    _, rapport, app = lancer(tmp_path, src, page(ELABE + IFOP_DOUBLON, DUELS))
    origines = [(s["institut"], s["terrain_fin"], s["origine"]) for s in app["sondages"]]
    # L'Ifop du 19 octobre est déjà chez 2027 et moi : sa ligne Wikipédia n'est pas reprise.
    assert origines[0] == ("Elabe", "2026-11-13", "Wikipédia")
    assert sum(1 for i, f, _ in origines if i.startswith("Ifop") and f == "2026-10-19") == 1
    assert ("Ifop", "2026-10-19", "2027 et moi") in origines
    assert app["second_tour"][0]["hypotheses"][0]["label"] == "Philippe – Le Pen"
    assert app["sources_sondages"][0]["licence"] == "CC BY-SA 4.0"
    assert app["noms_hors_liste"]["villepin"] == "Dominique de Villepin"
    assert "Wikipédia" in app["moyenne"]["regle"]
    # Moyenne au 14 novembre : Elabe (Wikipédia) et Ifop du 19 octobre (2027 et moi).
    assert app["moyenne"]["nb_sondages"] == 2
    assert any("Wikipédia, ligne écartée" in a for a in rapport["avertissements"])


def test_nouveaux_sondages_dans_les_nouveautes(tmp_path):
    src = source_min(tmp_path)
    veille, _, _ = lancer(tmp_path, src, None, nom="veille")
    out, _, _ = lancer(tmp_path, src, page(ELABE, DUELS), precedent=veille)
    libelles = [c["libelle"] for c in json.loads((out / "changes.json").read_text(encoding="utf-8"))["changements"]]
    assert "Nouveau sondage Elabe" in libelles
    assert "Nouveau sondage du 2nd tour Elabe" in libelles


def test_page_absente_ou_tronquee_garde_la_veille(tmp_path):
    src = source_min(tmp_path)
    veille, _, avant = lancer(tmp_path, src, page(ELABE, DUELS), nom="veille")
    _, _, sans_page = lancer(tmp_path, src, None, precedent=veille, nom="sans")
    assert [s["id"] for s in sans_page["second_tour"]] == [s["id"] for s in avant["second_tour"]]
    assert any(s["origine"] == "Wikipédia" for s in sans_page["sondages"])
    _, rapport, tronquee = lancer(tmp_path, src, "<p>page vide</p>", precedent=veille, nom="vide")
    assert [s["id"] for s in tronquee["second_tour"]] == [s["id"] for s in avant["second_tour"]]
    assert any("la veille" in a for a in rapport["avertissements"])
