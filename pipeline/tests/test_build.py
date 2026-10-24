"""Tests du robot quotidien. Lancer : python -m pytest pipeline/tests"""

import datetime as dt
import io
import json
import os
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import build  # noqa: E402

MAINTENANT = dt.datetime(2026, 10, 24, 4, 0, tzinfo=dt.timezone.utc)  # 6 h à Paris


def ecrire(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False), encoding="utf-8")


def source_min(tmp: Path, **modifs) -> Path:
    """Petit dossier data/ au format de 2027 et moi."""
    src = tmp / "data"
    candidats = modifs.get(
        "candidats",
        [
            {"id": "zemmour", "nom": "Éric Zemmour", "parti": "Reconquête", "bloc": "droite", "statut": "declare",
             "propositions": ["P1"], "liens": {"campagne": "https://z.fr", "x": "https://x.com/z"}},
            {"id": "arthaud", "nom": "Nathalie Arthaud", "parti": "LO", "bloc": "gauche", "statut": "declare",
             "propositions": [], "liens": {}},
            {"id": "le-pen", "nom": "Marine Le Pen", "parti": "RN", "bloc": "droite", "statut": "declare",
             "propositions": [], "liens": {}},
            {"id": "faure", "nom": "Olivier Faure", "parti": "PS", "bloc": "gauche", "statut": "renonce"},
            {"id": "hollande", "nom": "François Hollande", "parti": "PS", "bloc": "gauche", "statut": "pressenti"},
        ],
    )
    ecrire(src / "meta.json", {"publication": "2026-10-22", "donnees": "2026-10-22"})
    ecrire(src / "candidats.json", {"mise_a_jour": "2026-10-19", "candidats": candidats})
    mesures = modifs.get(
        "mesures",
        [
            {"theme": "economie", "texte": "Rétablir l'ISF.", "nature": "programme",
             "source": {"titre": "Programme", "url": "https://z.fr/p.pdf", "date": "2025-01"}},
            {"theme": "sante", "texte": "100 % Sécu.", "nature": "declaration",
             "source": {"titre": "TF1", "url": "https://tf1.fr/a", "date": "2026-05-03"}},
        ],
    )
    ecrire(src / "programmes" / "zemmour.json", {"id": "zemmour", "programme": {"etat": "partiel", "titre": "T", "url": "https://z.fr", "note": "N"}, "mesures": mesures})
    ecrire(src / "programmes" / "arthaud.json", {"id": "arthaud", "programme": {"etat": "aucun", "titre": "", "url": "", "note": ""}, "mesures": []})
    ecrire(src / "biographies" / "zemmour.json", {
        "id": "zemmour",
        "photo": {"url": "https://upload.wikimedia.org/z.jpg", "auteur": "A", "licence": "CC BY 4.0", "page": "https://commons/z"},
        "presentation": {"texte": "Présentation.", "sources": [{"titre": "W", "url": "https://w", "date": "2026-09-22"}]},
        "affaires": [{"titre": "Affaire", "texte": "Texte", "etat": "relaxe", "etat_detail": "", "date_etat": "2020-01-01", "sources": []}],
    })
    ecrire(src / "sondages.json", {"mise_a_jour": "2026-10-20", "noms_hors_liste": {"bonnal": "Sébastien Bonnal"}, "sondages": modifs.get("sondages", [
        {"institut": "Ifop", "commanditaire": "LCI", "terrain_debut": "2026-10-17", "terrain_fin": "2026-10-19", "echantillon": 1000,
         "source": {"titre": "Notice", "url": "https://cds/1.pdf", "date": "2026-10-20"},
         "hypotheses": [{"label": "H1", "scores": {"le-pen": 36, "zemmour": 4}}, {"label": "H2", "scores": {"le-pen": 38}}]},
        {"institut": "Elabe", "commanditaire": "BFM", "terrain_debut": "2026-10-01", "terrain_fin": "2026-10-02", "echantillon": 1500,
         "source": {"titre": "Notice", "url": "https://cds/2.pdf", "date": "2026-10-03"},
         "hypotheses": [{"label": "H", "scores": {"le-pen": 34, "zemmour": 5, "bonnal": 0.5}}]},
        {"institut": "Ipsos", "commanditaire": "Le Monde", "terrain_debut": "2026-09-20", "terrain_fin": "2026-09-24", "echantillon": 1500,
         "source": {"titre": "Notice", "url": "https://cds/3.pdf", "date": "2026-09-25"},
         "hypotheses": [{"label": "H", "scores": {"le-pen": 30}}]},
    ])})
    ecrire(src / "calendrier.json", {"etapes": [
        {"date": "2027-04-11", "titre": "1er tour", "statut": "estimee", "source": {"titre": "S", "url": "https://s", "date": "2026-01-01"}},
        {"date": "2026-12-10", "fin": "2026-12-13", "titre": "Référendum", "statut": "prevue"},
    ]})
    if modifs.get("votes", True):
        ecrire(src / "votes.json", {"mise_a_jour": "2026-09-29", "mandats": {"le-pen": [
            {"chambre": "AN", "detail": "Députée", "debut": "2017-06-21", "fin": "", "source": {"titre": "AN", "url": "https://an.fr", "date": "2026-09-25"}}]},
            "scrutins": [
                {"id": "an-1", "chambre": "AN", "date": "2025-01-10", "titre": "Loi A", "type": "ensemble", "resultat": "adopté", "url": "https://an.fr/1", "votes": {"le-pen": "pour"}},
                {"id": "an-2", "chambre": "AN", "date": "2026-03-01", "titre": "Loi B", "type": "ensemble", "resultat": "rejeté", "url": "https://an.fr/2", "votes": {"le-pen": "absent", "zemmour": "contre"}},
                {"id": "an-3", "chambre": "AN", "date": "2026-03-02", "titre": "Sans vote", "votes": {}},
            ]})
    return src


def fausse_photo(compteur=None):
    from PIL import Image

    def telecharge(url):
        if compteur is not None:
            compteur.append(url)
        img = Image.new("RGB", (500, 700), (120, 130, 140))
        b = io.BytesIO()
        img.save(b, "JPEG")
        return b.getvalue()

    return telecharge


def lancer(tmp, src, precedent=None, nom="out", maintenant=MAINTENANT, telecharge=None):
    out = tmp / nom
    rapport = build.executer(src, precedent, out, maintenant, telecharge or fausse_photo())
    return out, rapport, json.loads((out / "app-data.json").read_text(encoding="utf-8"))


# ---------------------------------------------------------------- contenu


def test_ordre_alphabetique_par_nom_de_famille(tmp_path):
    _, _, app = lancer(tmp_path, source_min(tmp_path))
    assert [c["id"] for c in app["candidats"]] == ["arthaud", "faure", "hollande", "le-pen", "zemmour"]


def test_texte_des_mesures_recopie_tel_quel(tmp_path):
    _, _, app = lancer(tmp_path, source_min(tmp_path))
    z = next(c for c in app["candidats"] if c["id"] == "zemmour")
    assert [m["texte"] for m in z["mesures"]] == ["Rétablir l'ISF.", "100 % Sécu."]
    assert z["mesures"][1]["nature"] == "declaration"
    assert z["mesures"][0]["source"] == {"titre": "Programme", "url": "https://z.fr/p.pdf", "date": "2025-01"}
    assert z["liens"] == {"campagne": "https://z.fr"}  # seulement campagne et programme


def test_identifiant_de_mesure_stable(tmp_path):
    _, _, a = lancer(tmp_path, source_min(tmp_path), nom="a")
    _, _, b = lancer(tmp_path, source_min(tmp_path), nom="b")
    assert [m["id"] for c in a["candidats"] for m in c["mesures"]] == [m["id"] for c in b["candidats"] for m in c["mesures"]]


def test_candidat_sans_programme_ni_photo(tmp_path):
    _, _, app = lancer(tmp_path, source_min(tmp_path))
    faure = next(c for c in app["candidats"] if c["id"] == "faure")
    assert faure["programme"] is None and faure["mesures"] == [] and faure["photo"] is None


def test_les_13_domaines_dans_l_ordre(tmp_path):
    _, _, app = lancer(tmp_path, source_min(tmp_path))
    assert [t["label"] for t in app["themes"]][:3] == ["Économie", "Travail et pouvoir d'achat", "Retraites"]
    assert len(app["themes"]) == 13 and app["themes"][-1]["id"] == "societe"


def test_date_de_la_source(tmp_path):
    _, _, app = lancer(tmp_path, source_min(tmp_path))
    assert app["source"]["mise_a_jour"] == "2026-10-22"


def test_votes_au_parlement(tmp_path):
    _, _, app = lancer(tmp_path, source_min(tmp_path))
    assert [v["id"] for v in app["scrutins"]] == ["an-2", "an-1"]  # du plus récent au plus ancien, scrutins sans vote retirés
    assert app["scrutins"][0]["votes"] == {"le-pen": "absent", "zemmour": "contre"}
    assert app["mandats"]["le-pen"][0]["detail"] == "Députée"


def test_sans_fichier_de_votes(tmp_path):
    _, _, app = lancer(tmp_path, source_min(tmp_path, votes=False))
    assert app["scrutins"] == [] and app["mandats"] == {}


# ---------------------------------------------------------------- moyenne


def test_moyenne_30_jours_et_hypotheses():
    s = [build.sondage_app(x) for x in json.loads(json.dumps([
        {"institut": "A", "terrain_fin": "2026-10-19", "source": {"url": "u1"},
         "hypotheses": [{"scores": {"x": 30, "y": 10}}, {"scores": {"x": 34}}]},
        {"institut": "B", "terrain_fin": "2026-09-25", "source": {"url": "u2"}, "hypotheses": [{"scores": {"x": 20, "y": 12}}]},
        {"institut": "C", "terrain_fin": "2026-09-24", "source": {"url": "u3"}, "hypotheses": [{"scores": {"x": 99}}]},  # 30 jours pile : exclu
    ]))]
    m = build.moyenne(s, dt.date(2026, 10, 24))
    assert m["nb_sondages"] == 2
    # x : sondage A = (30+34)/2 = 32, sondage B = 20 → 26 ; y : (10+12)/2 = 11
    assert m["scores"] == [{"id": "x", "score": 26.0, "nb_sondages": 2}, {"id": "y", "score": 11.0, "nb_sondages": 2}]


def test_moyenne_sans_sondage_recent():
    m = build.moyenne([], dt.date(2026, 10, 24))
    assert m["nb_sondages"] == 0 and m["scores"] == []


def test_sondages_deja_vus_conserves(tmp_path):
    out1, _, _ = lancer(tmp_path, source_min(tmp_path / "j1"), nom="j1out")
    # Le lendemain, la source ne garde plus que le sondage Ifop.
    src2 = source_min(tmp_path / "j2", sondages=[json.loads((tmp_path / "j1" / "data" / "sondages.json").read_text(encoding="utf-8"))["sondages"][0]])
    _, _, app = lancer(tmp_path, src2, precedent=out1, nom="j2out")
    assert [s["institut"] for s in app["sondages"]] == ["Ifop", "Elabe", "Ipsos"]
    assert app["noms_hors_liste"] == {"bonnal": "Sébastien Bonnal"}


# ---------------------------------------------------------------- photos


def test_photo_reduite_a_300_px_avec_credit(tmp_path):
    out, _, app = lancer(tmp_path, source_min(tmp_path))
    from PIL import Image

    z = next(c for c in app["candidats"] if c["id"] == "zemmour")
    assert z["photo"]["auteur"] == "A" and z["photo"]["licence"] == "CC BY 4.0"
    img = Image.open(out / z["photo"]["fichier"])
    assert img.size[0] == 300


def test_photo_recopiee_une_seule_fois(tmp_path):
    appels = []
    src = source_min(tmp_path)
    out1, _, _ = lancer(tmp_path, src, nom="a", telecharge=fausse_photo(appels))
    lancer(tmp_path, src, precedent=out1, nom="b", telecharge=fausse_photo(appels))
    assert len(appels) == 1


def test_photo_injoignable_donne_les_initiales(tmp_path):
    def panne(url):
        raise OSError("réseau")

    _, rapport, app = lancer(tmp_path, source_min(tmp_path), telecharge=panne)
    assert next(c for c in app["candidats"] if c["id"] == "zemmour")["photo"] is None
    assert rapport["avertissements"]


# ---------------------------------------------------------------- changements et publication


def test_premiere_publication_sans_changement(tmp_path):
    out, rapport, _ = lancer(tmp_path, source_min(tmp_path))
    assert rapport["change"] is True
    assert json.loads((out / "changes.json").read_text(encoding="utf-8"))["changements"] == []


def test_rien_de_neuf_manifeste_identique(tmp_path):
    src = source_min(tmp_path)
    out1, _, _ = lancer(tmp_path, src, nom="a")
    demain = MAINTENANT + dt.timedelta(days=1)
    out2 = tmp_path / "b"
    rapport = build.executer(src, out1, out2, demain, fausse_photo())
    assert rapport["change"] is False
    assert (out1 / "manifest.json").read_bytes() == (out2 / "manifest.json").read_bytes()


def test_changements_detectes(tmp_path):
    out1, _, _ = lancer(tmp_path, source_min(tmp_path / "j1"), nom="j1out")
    src2 = source_min(
        tmp_path / "j2",
        candidats=[
            {"id": "zemmour", "nom": "Éric Zemmour", "parti": "Reconquête", "statut": "declare"},
            {"id": "arthaud", "nom": "Nathalie Arthaud", "parti": "LO", "statut": "declare"},
            {"id": "le-pen", "nom": "Marine Le Pen", "parti": "RN", "statut": "declare"},
            {"id": "faure", "nom": "Olivier Faure", "parti": "PS", "statut": "renonce"},
            {"id": "hollande", "nom": "François Hollande", "parti": "PS", "statut": "declare"},
            {"id": "ruffin", "nom": "François Ruffin", "parti": "Debout", "statut": "declare"},
        ],
        mesures=[
            {"theme": "economie", "texte": "Rétablir l'ISF.", "nature": "programme", "source": {"titre": "P", "url": "https://z.fr/p.pdf", "date": "2025-01"}},
            {"theme": "logement", "texte": "Nouvelle mesure.", "nature": "presse", "source": {"titre": "Le Monde", "url": "https://lemonde.fr/a", "date": "2026-10-23"}},
        ],
    )
    out2, _, _ = lancer(tmp_path, src2, precedent=out1, nom="j2out", maintenant=MAINTENANT + dt.timedelta(days=1))
    ch = json.loads((out2 / "changes.json").read_text(encoding="utf-8"))["changements"]
    types = sorted((c["type"], c.get("candidat")) for c in ch)
    assert types == [
        ("mesure_ajoutee", "zemmour"),
        ("mesure_retiree", "zemmour"),
        ("nouveau_candidat", "ruffin"),
        ("statut_candidat", "hollande"),
    ]
    ajout = next(c for c in ch if c["type"] == "mesure_ajoutee")
    assert ajout["libelle"] == "Mesure ajoutée — Logement" and ajout["date"] == "2026-10-25"
    assert next(c for c in ch if c["type"] == "statut_candidat")["libelle"] == "Statut : pressenti → déclaré"


def test_historique_90_jours():
    vieux = [{"date": "2026-07-01", "type": "nouveau_sondage"}, {"date": "2026-09-01", "type": "nouveau_sondage"}]
    h = build.historique(vieux, [{"date": "2026-10-24", "type": "nouveau_sondage"}], dt.date(2026, 10, 24))
    assert [c["date"] for c in h] == ["2026-10-24", "2026-09-01"]


def test_donnee_invalide_bloque_la_publication(tmp_path):
    src = source_min(tmp_path, mesures=[{"theme": "economie", "texte": "X", "nature": "rumeur", "source": {"url": "https://a"}}])
    with pytest.raises(build.Invalide, match="nature inconnue"):
        build.executer(src, None, tmp_path / "out", MAINTENANT, fausse_photo())
    assert build.main(["--source", str(src), "--out", str(tmp_path / "out2")]) == 1


def test_mesure_sans_source_refusee(tmp_path):
    src = source_min(tmp_path, mesures=[{"theme": "economie", "texte": "X", "nature": "programme"}])
    with pytest.raises(build.Invalide, match="sans source"):
        build.executer(src, None, tmp_path / "out", MAINTENANT, fausse_photo())


def test_nouveau_domaine_de_la_source_accepte(tmp_path):
    src = source_min(tmp_path, mesures=[{"theme": "culture", "texte": "X", "nature": "programme", "source": {"url": "https://a"}}])
    _, _, app = lancer(tmp_path, src)
    assert app["themes"][-1] == {"id": "culture", "label": "Culture"}


# ---------------------------------------------------------------- vraies données (si présentes)


VRAIE_SOURCE = os.environ.get("SOURCE_2027")


@pytest.mark.skipif(not VRAIE_SOURCE, reason="SOURCE_2027 non défini")
def test_vraies_donnees(tmp_path):
    out, rapport, app = lancer(tmp_path, Path(VRAIE_SOURCE))
    assert rapport["change"]
    assert len(app["themes"]) == 13
    assert sum(len(c["mesures"]) for c in app["candidats"]) > 500
    assert (out / "app-data.json").stat().st_size < 1_000_000
