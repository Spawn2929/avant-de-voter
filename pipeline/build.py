"""Robot quotidien d'« Avant de voter ».

Lit le dossier data/ de 2027 et moi, prépare les fichiers lus par l'appli et les écrit dans un dossier
de sortie, à partir de la version publiée la veille :

    python build.py --source source/data --previous site --out out

- manifest.json : date de la source et empreinte de chaque fichier (identique si rien n'a changé) ;
- app-data.json : candidats, mesures, sondages, moyenne sur 30 jours, calendrier ;
- changes.json  : changements des 90 derniers jours ;
- photos/       : photos recopiées depuis Wikimedia Commons, 300 px.

Le texte des mesures est recopié tel quel : rien n'est résumé ni reformulé.
Code de sortie : 0 = publié ou rien à publier, 1 = données invalides (rien n'est écrit).
"""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import io
import json
import shutil
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path
from zoneinfo import ZoneInfo

PARIS = ZoneInfo("Europe/Paris")
FORMAT = 1  # version du format des fichiers publiés ; l'appli refuse un format plus récent qu'elle
PHOTO_PX = 300
MOYENNE_JOURS = 30
CHANGES_JOURS = 90
USER_AGENT = "AvantDeVoter/1.0 (https://github.com/Spawn2929/avant-de-voter)"

# Les 13 domaines, dans l'ordre des puces de l'appli. Identifiants repris de la source.
THEMES = [
    ("economie", "Économie"),
    ("travail", "Travail et pouvoir d'achat"),
    ("retraites", "Retraites"),
    ("sante", "Santé"),
    ("education", "Éducation"),
    ("ecologie", "Écologie"),
    ("immigration", "Immigration"),
    ("securite", "Sécurité et justice"),
    ("international", "International et défense"),
    ("institutions", "Institutions"),
    ("logement", "Logement"),
    ("territoires", "Territoires"),
    ("societe", "Société"),
]
NATURES = {"programme", "declaration", "presse"}
ETATS_PROGRAMME = {"complet", "partiel", "aucun"}
STATUTS = {"declare", "pressenti", "renonce"}
REGLE_MOYENNE = (
    "Moyenne des sondages dont la fin de terrain date de moins de 30 jours. "
    "Quand un sondage teste plusieurs hypothèses, on retient la moyenne des hypothèses où le candidat figure. "
    "Puis moyenne simple entre sondages, arrondie au dixième."
)


class Invalide(Exception):
    """Donnée de la source inutilisable : rien ne doit être publié."""


# ---------------------------------------------------------------- lecture


def lire_json(path: Path):
    with path.open(encoding="utf-8") as f:
        return json.load(f)


def lire_source(source: Path) -> dict:
    """Charge les fichiers utiles du dossier data/ de 2027 et moi."""
    for nom in ("meta.json", "candidats.json", "sondages.json", "calendrier.json"):
        if not (source / nom).exists():
            raise Invalide(f"fichier manquant dans la source : {nom}")
    data = {
        "meta": lire_json(source / "meta.json"),
        "candidats": lire_json(source / "candidats.json"),
        "sondages": lire_json(source / "sondages.json"),
        "calendrier": lire_json(source / "calendrier.json"),
        "programmes": {},
        "biographies": {},
    }
    # Votes au Parlement : facultatifs (affichés sur la fiche candidat s'ils existent).
    data["votes"] = lire_json(source / "votes.json") if (source / "votes.json").exists() else {}
    for sous, cle in (("programmes", "programmes"), ("biographies", "biographies")):
        dossier = source / sous
        if dossier.is_dir():
            for f in sorted(dossier.glob("*.json")):
                data[cle][f.stem] = lire_json(f)
    return data


# ---------------------------------------------------------------- transformation


def id_mesure(candidat: str, texte: str) -> str:
    return hashlib.sha1(f"{candidat}|{texte}".encode("utf-8")).hexdigest()[:12]


def id_sondage(s: dict) -> str:
    cle = (s.get("source") or {}).get("url") or f"{s.get('institut')}|{s.get('terrain_debut')}|{s.get('terrain_fin')}"
    return hashlib.sha1(cle.encode("utf-8")).hexdigest()[:12]


def source_courte(src: dict | None) -> dict | None:
    if not src:
        return None
    return {"titre": src.get("titre") or "", "url": src.get("url") or "", "date": src.get("date") or ""}


def entrees_datees(liste) -> list:
    """Parcours politique / professionnel : période, texte, source."""
    return [
        {"periode": e.get("periode") or "", "texte": e["texte"], "source": source_courte(e.get("source"))}
        for e in (liste or [])
        if e.get("texte")
    ]


def candidat_app(c: dict, programme: dict | None, bio: dict | None, themes_connus: set) -> dict:
    cid = c["id"]
    mesures, vus = [], set()
    for m in (programme or {}).get("mesures", []):
        mid = id_mesure(cid, m["texte"])
        if mid in vus:  # même texte recopié deux fois dans la source
            continue
        vus.add(mid)
        themes_connus.add(m["theme"])
        mesures.append(
            {
                "id": mid,
                "theme": m["theme"],
                "texte": m["texte"],
                "nature": m["nature"],
                "source": source_courte(m.get("source")),
            }
        )
    prog = (programme or {}).get("programme")
    bio = bio or {}
    photo = bio.get("photo") if (bio.get("photo") or {}).get("url") else None
    presentation = bio.get("presentation") or {}
    return {
        "id": cid,
        "tri": cid,  # identifiant = nom de famille sans accent (« le-pen », « villepin ») : ordre alphabétique
        "nom": c["nom"],
        "parti": c.get("parti") or "",
        "bloc": c.get("bloc") or "",
        "statut": c["statut"],
        "statut_detail": c.get("statut_detail") or "",
        "statut_source": source_courte(c.get("source")),
        "propositions_phares": list(c.get("propositions") or []),
        "liens": {k: v for k, v in (c.get("liens") or {}).items() if k in ("campagne", "programme") and v},
        "photo": (
            {
                "source_url": photo["url"],
                "fichier": None,  # rempli après la copie de la photo
                "page": photo.get("page") or "",
                "auteur": photo.get("auteur") or "",
                "licence": photo.get("licence") or "",
                "legende": photo.get("legende") or "",
            }
            if photo
            else None
        ),
        "presentation": presentation.get("texte") or "",
        "presentation_sources": [source_courte(s) for s in presentation.get("sources") or []],
        "parcours_politique": entrees_datees(bio.get("parcours_politique")),
        "parcours_professionnel": entrees_datees(bio.get("parcours_professionnel")),
        "affaires": [
            {
                "titre": a.get("titre") or "",
                "texte": a.get("texte") or "",
                "etat": a.get("etat") or "",
                "etat_detail": a.get("etat_detail") or "",
                "date_etat": a.get("date_etat") or "",
                "sources": [source_courte(s) for s in a.get("sources") or []],
            }
            for a in bio.get("affaires") or []
        ],
        "programme": (
            {
                "etat": prog.get("etat") or "aucun",
                "titre": prog.get("titre") or "",
                "url": prog.get("url") or "",
                "note": prog.get("note") or "",
            }
            if prog
            else None
        ),
        "mesures": mesures,
    }


def sondage_app(s: dict) -> dict:
    return {
        "id": id_sondage(s),
        "institut": s["institut"],
        "commanditaire": s.get("commanditaire") or "",
        "terrain_debut": s.get("terrain_debut") or "",
        "terrain_fin": s["terrain_fin"],
        "echantillon": s.get("echantillon"),
        "source": source_courte(s.get("source")),
        "hypotheses": [
            {"label": h.get("label") or "", "scores": {k: float(v) for k, v in h["scores"].items() if v is not None}}
            for h in s.get("hypotheses") or []
        ],
    }


def fusion_sondages(nouveaux: list, anciens: list) -> list:
    """Garde tous les sondages déjà vus : 2027 et moi n'en publie qu'une fenêtre de quelques semaines.
    Un sondage encore présent dans la source remplace sa copie de la veille."""
    par_id = {s["id"]: s for s in anciens}
    for s in nouveaux:
        par_id[s["id"]] = s
    return sorted(par_id.values(), key=lambda s: (s["terrain_fin"], s["institut"], s["id"]), reverse=True)


def moyenne(sondages: list, aujourd_hui: dt.date) -> dict:
    debut = aujourd_hui - dt.timedelta(days=MOYENNE_JOURS)
    retenus = [s for s in sondages if debut < dt.date.fromisoformat(s["terrain_fin"]) <= aujourd_hui]
    par_candidat: dict[str, list[float]] = {}
    for s in retenus:
        vus: dict[str, list[float]] = {}
        for h in s["hypotheses"]:
            for cid, score in h["scores"].items():
                vus.setdefault(cid, []).append(score)
        for cid, scores in vus.items():
            par_candidat.setdefault(cid, []).append(sum(scores) / len(scores))
    scores = [
        {"id": cid, "score": round(sum(v) / len(v), 1), "nb_sondages": len(v)} for cid, v in par_candidat.items()
    ]
    scores.sort(key=lambda x: (-x["score"], x["id"]))
    # Pas de date du jour ici : sinon le fichier changerait chaque matin et l'appli retéléchargerait tout.
    return {
        "fenetre_jours": MOYENNE_JOURS,
        "nb_sondages": len(retenus),
        "regle": REGLE_MOYENNE,
        "scores": scores,
    }


def construire(data: dict, precedent: dict | None, aujourd_hui: dt.date) -> dict:
    """Assemble app-data.json (sans les photos, ajoutées ensuite)."""
    themes_connus: set = set()
    candidats = [
        candidat_app(c, data["programmes"].get(c["id"]), data["biographies"].get(c["id"]), themes_connus)
        for c in data["candidats"]["candidats"]
    ]
    candidats.sort(key=lambda c: c["tri"])

    themes = [{"id": i, "label": l} for i, l in THEMES]
    # Domaine apparu dans la source après la fabrication du robot : affiché à la fin, avec son identifiant.
    for t in sorted(themes_connus - {i for i, _ in THEMES}):
        themes.append({"id": t, "label": t.replace("_", " ").capitalize()})

    nouveaux = [sondage_app(s) for s in data["sondages"]["sondages"]]
    sondages = fusion_sondages(nouveaux, (precedent or {}).get("sondages", []))

    # Noms cités dans un sondage sans fiche candidat : on garde aussi ceux des sondages archivés.
    noms_hors_liste = dict((precedent or {}).get("noms_hors_liste") or {})
    noms_hors_liste.update(data["sondages"].get("noms_hors_liste") or {})

    votes = data.get("votes") or {}
    scrutins = sorted(
        (
            {
                "id": v["id"],
                "chambre": v.get("chambre") or "",
                "date": v["date"],
                "titre": v.get("titre") or "",
                "type": v.get("type") or "",
                "resultat": v.get("resultat") or "",
                "url": v.get("url") or "",
                "votes": {k: p for k, p in (v.get("votes") or {}).items() if p},
            }
            for v in votes.get("scrutins") or []
            if v.get("votes")
        ),
        key=lambda v: (v["date"], v["id"]),
        reverse=True,
    )
    mandats = {
        cid: [
            {
                "chambre": m.get("chambre") or "",
                "detail": m.get("detail") or "",
                "debut": m.get("debut") or "",
                "fin": m.get("fin") or "",
                "source": source_courte(m.get("source")),
            }
            for m in liste
        ]
        for cid, liste in sorted((votes.get("mandats") or {}).items())
    }

    meta = data["meta"]
    return {
        "format": FORMAT,
        "source": {
            "nom": "2027 et moi",
            "site": "https://2027etmoi.fr",
            "depot": "https://github.com/2027etmoi/2027etmoi",
            "licence": "Licence Ouverte 2.0",
            "mise_a_jour": meta.get("donnees") or meta.get("publication") or "",
        },
        "themes": themes,
        "candidats": candidats,
        "sondages": sondages,
        "noms_hors_liste": dict(sorted(noms_hors_liste.items())),
        "moyenne": moyenne(sondages, aujourd_hui),
        "calendrier": [
            {
                "date": e["date"],
                "fin": e.get("fin") or "",
                "titre": e["titre"],
                "statut": e.get("statut") or "",
                "source": source_courte(e.get("source")),
            }
            for e in sorted(data["calendrier"].get("etapes") or [], key=lambda e: e["date"])
        ],
        "scrutins": scrutins,
        "mandats": mandats,
    }


# ---------------------------------------------------------------- photos


def telecharger(url: str) -> bytes:
    """Wikimedia refuse les rafales (HTTP 429) : une seconde entre deux photos, et on réessaie après attente."""
    for essai in range(4):
        time.sleep(1 + 5 * essai)
        req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        try:
            with urllib.request.urlopen(req, timeout=30) as r:
                return r.read()
        except urllib.error.HTTPError as e:
            if e.code != 429 or essai == 3:
                raise
    raise AssertionError("inatteignable")


def reduire(contenu: bytes) -> bytes:
    from PIL import Image  # importé ici : les tests sans photo n'en ont pas besoin

    img = Image.open(io.BytesIO(contenu))
    img = img.convert("RGB")
    img.thumbnail((PHOTO_PX, PHOTO_PX * 2))  # 300 px de large au plus, proportions gardées
    sortie = io.BytesIO()
    img.save(sortie, "JPEG", quality=85, optimize=True)
    return sortie.getvalue()


def copier_photos(app: dict, precedent: dict | None, dossier_precedent: Path | None, out: Path, telecharge=telecharger):
    """Recopie chaque photo une seule fois : si l'adresse d'origine n'a pas changé, on reprend celle de la veille.
    Une photo impossible à télécharger reprend l'ancienne, sinon le candidat s'affiche avec ses initiales."""
    anciennes = {
        c["id"]: c["photo"] for c in (precedent or {}).get("candidats", []) if c.get("photo") and c["photo"].get("fichier")
    }
    (out / "photos").mkdir(parents=True, exist_ok=True)
    avertissements = []
    for c in app["candidats"]:
        p = c["photo"]
        if not p:
            continue
        ancienne = anciennes.get(c["id"])
        contenu = None
        if ancienne and ancienne["source_url"] == p["source_url"] and dossier_precedent:
            f = dossier_precedent / ancienne["fichier"]
            if f.exists():
                contenu = f.read_bytes()
        if contenu is None:
            try:
                contenu = reduire(telecharge(p["source_url"]))
            except Exception as e:  # réseau, image illisible
                avertissements.append(f"photo de {c['id']} non téléchargée : {e}")
                if ancienne and dossier_precedent and (dossier_precedent / ancienne["fichier"]).exists():
                    contenu = (dossier_precedent / ancienne["fichier"]).read_bytes()
                    p.update({k: ancienne[k] for k in ("source_url", "page", "auteur", "licence", "legende")})
        if contenu is None:
            c["photo"] = None
            continue
        nom = f"photos/{c['id']}-{hashlib.sha1(contenu).hexdigest()[:8]}.jpg"
        (out / nom).write_bytes(contenu)
        p["fichier"] = nom
    return avertissements


# ---------------------------------------------------------------- changements


def libelle_theme(themes: list, tid: str) -> str:
    return next((t["label"] for t in themes if t["id"] == tid), tid)


def changements(precedent: dict | None, app: dict, jour: str) -> list:
    """Ce qui a changé depuis la publication précédente. Rien à la toute première publication."""
    if not precedent:
        return []
    out = []
    avant = {c["id"]: c for c in precedent.get("candidats", [])}
    noms_statut = {"declare": "déclaré", "pressenti": "pressenti", "renonce": "a renoncé"}
    for c in app["candidats"]:
        a = avant.get(c["id"])
        if a is None:
            out.append({"date": jour, "type": "nouveau_candidat", "candidat": c["id"], "libelle": "Nouvelle personnalité suivie"})
            continue
        if a["statut"] != c["statut"]:
            out.append(
                {
                    "date": jour,
                    "type": "statut_candidat",
                    "candidat": c["id"],
                    "libelle": f"Statut : {noms_statut.get(a['statut'], a['statut'])} → {noms_statut.get(c['statut'], c['statut'])}",
                }
            )
        etat_a = (a.get("programme") or {}).get("etat") or "aucun"
        etat_c = (c.get("programme") or {}).get("etat") or "aucun"
        if etat_a != etat_c:
            out.append(
                {"date": jour, "type": "programme_etat", "candidat": c["id"], "libelle": f"Programme : {etat_a} → {etat_c}"}
            )
        ids_a = {m["id"]: m for m in a.get("mesures", [])}
        ids_c = {m["id"]: m for m in c.get("mesures", [])}
        for mid, m in ids_c.items():
            if mid not in ids_a:
                out.append(
                    {
                        "date": jour,
                        "type": "mesure_ajoutee",
                        "candidat": c["id"],
                        "theme": m["theme"],
                        "mesure": mid,
                        "libelle": f"Mesure ajoutée — {libelle_theme(app['themes'], m['theme'])}",
                        "texte": m["texte"],
                    }
                )
        for mid, m in ids_a.items():
            if mid not in ids_c:
                out.append(
                    {
                        "date": jour,
                        "type": "mesure_retiree",
                        "candidat": c["id"],
                        "theme": m["theme"],
                        "mesure": mid,
                        "libelle": f"Mesure retirée — {libelle_theme(app['themes'], m['theme'])}",
                        "texte": m["texte"],
                    }
                )
    deja = {s["id"] for s in precedent.get("sondages", [])}
    for s in app["sondages"]:
        if s["id"] not in deja:
            out.append({"date": jour, "type": "nouveau_sondage", "sondage": s["id"], "libelle": f"Nouveau sondage {s['institut']}"})
    return out


def historique(anciens: list, nouveaux: list, aujourd_hui: dt.date) -> list:
    limite = (aujourd_hui - dt.timedelta(days=CHANGES_JOURS)).isoformat()
    tout = nouveaux + [c for c in anciens if c["date"] >= limite]
    return sorted(tout, key=lambda c: c["date"], reverse=True)  # tri stable : l'ordre du jour est gardé


# ---------------------------------------------------------------- validation


def valider(app: dict, changes: dict):
    """Contrôle tout ce que l'appli suppose. Une erreur bloque la publication."""
    erreurs = []

    def exige(cond, msg):
        if not cond:
            erreurs.append(msg)

    exige(app.get("format") == FORMAT, "format inattendu")
    exige(bool(app["source"]["mise_a_jour"]), "date de mise à jour de la source absente")
    themes = {t["id"] for t in app["themes"]}
    exige(all(i in themes for i, _ in THEMES), "un des 13 domaines manque")
    exige(len(app["candidats"]) >= 5, f"trop peu de candidats ({len(app['candidats'])})")
    exige(len({c["id"] for c in app["candidats"]}) == len(app["candidats"]), "identifiant de candidat en double")
    exige(any(c["statut"] == "declare" for c in app["candidats"]), "aucun candidat déclaré")
    for c in app["candidats"]:
        ou = f"candidat {c.get('id')}"
        exige(bool(c["id"]) and bool(c["nom"]), f"{ou} : identifiant ou nom vide")
        exige(c["statut"] in STATUTS, f"{ou} : statut inconnu {c['statut']!r}")
        if c["programme"]:
            exige(c["programme"]["etat"] in ETATS_PROGRAMME, f"{ou} : état de programme inconnu {c['programme']['etat']!r}")
        if c["photo"]:
            exige(bool(c["photo"]["fichier"]), f"{ou} : photo sans fichier")
            exige(bool(c["photo"]["auteur"]) and bool(c["photo"]["licence"]), f"{ou} : photo sans auteur ou licence")
        for m in c["mesures"]:
            exige(m["theme"] in themes, f"{ou} : domaine inconnu {m['theme']!r}")
            exige(m["nature"] in NATURES, f"{ou} : nature inconnue {m['nature']!r}")
            exige(bool(m["texte"].strip()), f"{ou} : mesure vide")
            exige(bool(m["source"] and m["source"]["url"].startswith("http")), f"{ou} : mesure sans source ({m['texte'][:40]})")
    for s in app["sondages"]:
        ou = f"sondage {s['institut']} {s['terrain_fin']}"
        try:
            dt.date.fromisoformat(s["terrain_fin"])
        except ValueError:
            erreurs.append(f"{ou} : date de fin de terrain illisible")
        exige(bool(s["hypotheses"]), f"{ou} : aucune hypothèse")
        for h in s["hypotheses"]:
            exige(all(0 <= v <= 100 for v in h["scores"].values()), f"{ou} : score hors de 0-100")
    for v in app.get("scrutins", []):
        # Une position inconnue (nouvelle dans la source) s'affichera telle quelle : on vérifie seulement le format.
        exige(all(isinstance(p, str) for p in v["votes"].values()), f"scrutin {v['id']} : position de vote illisible")
    for ch in changes["changements"]:
        exige(ch["type"] in {"mesure_ajoutee", "mesure_retiree", "programme_etat", "statut_candidat", "nouveau_candidat", "nouveau_sondage"},
              f"type de changement inconnu {ch['type']!r}")
    taille = len(json.dumps(app, ensure_ascii=False).encode("utf-8"))
    exige(taille < 3_000_000, f"app-data.json trop gros ({taille} octets)")
    if erreurs:
        raise Invalide("\n".join(erreurs[:30]))


# ---------------------------------------------------------------- écriture


def octets_json(obj) -> bytes:
    return (json.dumps(obj, ensure_ascii=False, indent=None, separators=(",", ":"), sort_keys=False) + "\n").encode("utf-8")


def empreinte(contenu: bytes) -> str:
    return hashlib.sha256(contenu).hexdigest()


def executer(source: Path, dossier_precedent: Path | None, out: Path, maintenant: dt.datetime, telecharge=telecharger) -> dict:
    """Fabrique les fichiers dans out/. Renvoie un compte rendu ; lève Invalide si la source est inutilisable."""
    aujourd_hui = maintenant.astimezone(PARIS).date()
    precedent = None
    changes_anciens = []
    if dossier_precedent and (dossier_precedent / "app-data.json").exists():
        precedent = lire_json(dossier_precedent / "app-data.json")
        if (dossier_precedent / "changes.json").exists():
            changes_anciens = lire_json(dossier_precedent / "changes.json").get("changements", [])

    data = lire_source(source)
    app = construire(data, precedent, aujourd_hui)

    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True)
    avertissements = copier_photos(app, precedent, dossier_precedent, out, telecharge)

    nouveaux = changements(precedent, app, aujourd_hui.isoformat())
    changes = {"format": FORMAT, "changements": historique(changes_anciens, nouveaux, aujourd_hui)}
    valider(app, changes)

    app_octets = octets_json(app)
    ancien_manifeste = lire_json(dossier_precedent / "manifest.json") if precedent and (dossier_precedent / "manifest.json").exists() else None
    if ancien_manifeste and ancien_manifeste["fichiers"]["app-data.json"]["sha256"] == empreinte(app_octets) and not nouveaux:
        # Rien de neuf : on republie à l'identique, le manifeste ne change pas, l'appli ne retélécharge rien.
        shutil.rmtree(out)
        shutil.copytree(dossier_precedent, out, ignore=shutil.ignore_patterns(".git"))
        return {"change": False, "avertissements": avertissements, "nouveaux": 0}

    changes_octets = octets_json(changes)
    (out / "app-data.json").write_bytes(app_octets)
    (out / "changes.json").write_bytes(changes_octets)
    manifeste = {
        "format": FORMAT,
        "source_mise_a_jour": app["source"]["mise_a_jour"],
        "publie_le": maintenant.astimezone(PARIS).isoformat(timespec="seconds"),
        "fichiers": {
            "app-data.json": {"sha256": empreinte(app_octets), "taille": len(app_octets)},
            "changes.json": {"sha256": empreinte(changes_octets), "taille": len(changes_octets)},
        },
        "photos": sum(1 for c in app["candidats"] if c["photo"]),
    }
    (out / "manifest.json").write_bytes((json.dumps(manifeste, ensure_ascii=False, indent=2) + "\n").encode("utf-8"))
    (out / ".nojekyll").write_text("")
    (out / "index.html").write_text(
        "<!doctype html><meta charset=utf-8><title>Avant de voter</title>"
        "<p>Données de l'appli Android « Avant de voter ». "
        "Source : <a href='https://2027etmoi.fr'>2027 et moi</a>, Licence Ouverte 2.0.</p>",
        encoding="utf-8",
    )
    return {"change": True, "avertissements": avertissements, "nouveaux": len(nouveaux), "manifeste": manifeste}


def main(argv=None) -> int:
    p = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    p.add_argument("--source", type=Path, required=True, help="dossier data/ de 2027 et moi")
    p.add_argument("--previous", type=Path, help="dernière version publiée (branche gh-pages)")
    p.add_argument("--out", type=Path, required=True)
    a = p.parse_args(argv)
    try:
        rapport = executer(a.source, a.previous, a.out, dt.datetime.now(dt.timezone.utc))
    except (Invalide, KeyError, ValueError, TypeError) as e:
        print(f"::error::Données invalides, rien n'est publié.\n{type(e).__name__}: {e}")
        return 1
    for w in rapport["avertissements"]:
        print(f"::warning::{w}")
    if rapport["change"]:
        m = rapport["manifeste"]
        print(f"Publication : source du {m['source_mise_a_jour']}, {rapport['nouveaux']} changement(s), {m['photos']} photos.")
    else:
        print("Rien de neuf depuis la dernière publication.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
