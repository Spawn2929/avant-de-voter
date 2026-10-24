"""Copie les données publiées par le robot dans l'APK (android/app/src/main/assets/snapshot/),
pour que l'appli affiche tout dès l'installation, même sans réseau.

    python pipeline/copie_donnees.py [adresse]
"""

import json
import shutil
import sys
import urllib.request
from pathlib import Path

BASE = (sys.argv[1] if len(sys.argv) > 1 else "https://spawn2929.github.io/avant-de-voter").rstrip("/")
DEST = Path(__file__).resolve().parents[1] / "android" / "app" / "src" / "main" / "assets" / "snapshot"


def lire(chemin: str) -> bytes:
    with urllib.request.urlopen(f"{BASE}/{chemin}", timeout=30) as r:
        return r.read()


def main():
    if DEST.exists():
        shutil.rmtree(DEST)
    (DEST / "photos").mkdir(parents=True)
    for f in ("manifest.json", "app-data.json", "changes.json"):
        (DEST / f).write_bytes(lire(f))
    app = json.loads((DEST / "app-data.json").read_text(encoding="utf-8"))
    photos = [c["photo"]["fichier"] for c in app["candidats"] if c.get("photo")]
    for p in photos:
        (DEST / p).write_bytes(lire(p))
    print(f"Copie des données du {app['source']['mise_a_jour']} : {len(app['candidats'])} personnalités, {len(photos)} photos.")


if __name__ == "__main__":
    main()
