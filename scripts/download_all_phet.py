"""
Download ALL PhET HTML5 simulations with Chinese translation.
Updates both Android and Robot catalog files.

Usage: python scripts/download_all_phet.py
"""
import json, os, urllib.request, time, re
from pathlib import Path

# Paths
ANDROID_EXP = Path(__file__).parent.parent / "android" / "app" / "src" / "main" / "assets" / "experiments" / "phet"
ROBOT_EXP = Path(__file__).parent.parent.parent / "child-learning-robot" / "src" / "main" / "resources" / "static" / "experiments" / "phet"
ANDROID_CATALOG = ANDROID_EXP.parent / "catalog.json"
ROBOT_CATALOG = ROBOT_EXP.parent / "catalog.json"

BASE_URL = "https://phet.colorado.edu/sims/html"

def get_phet_catalog():
    """Get full list of PhET simulations from API."""
    req = urllib.request.Request(
        'https://phet.colorado.edu/services/metadata/1.0/simulations?format=json',
        headers={'User-Agent': 'Mozilla/5.0'}
    )
    resp = urllib.request.urlopen(req, timeout=30)
    data = json.loads(resp.read().decode('utf-8'))

    # Build deduplicated list of projects with HTML sims
    projects = {}
    for proj in data['projects']:
        proj_name = proj.get('name')
        if not proj_name:
            continue

        # Check all simulations for locale data
        for sim in proj.get('simulations', []):
            sim_type = sim.get('locale', '')

            # Check if it's an HTML sim
            is_html = any(
                cat == 'html' or 'html' in str(cat)
                for cat in [sim.get('type', '')]
            )

            # Get localized name
            zh_name = None
            for ls in sim.get('localizedSimulations', []):
                if ls.get('locale') == 'zh_CN':
                    zh_name = ls.get('name') or proj_name
                    break

            # Get categories
            cat_ids = sim.get('categoryIds', [])
            levels = sim.get('highGradeLevel', '')

            if proj_name not in projects:
                projects[proj_name] = {
                    'id': proj_name,
                    'zh_name': zh_name,
                    'has_zh': zh_name is not None,
                    'categories': []
                }

    # Return only projects that have actual sim data
    return [v for v in projects.values() if v['id']]

def download_sim(sim_id, dest_dir):
    """Download a PhET simulation HTML + dependencies."""
    sim_dir = dest_dir / sim_id
    sim_dir.mkdir(parents=True, exist_ok=True)

    # Try Chinese first, then English
    downloaded = False
    for lang in ['zh_CN', 'en']:
        html_url = f"{BASE_URL}/{sim_id}/latest/{sim_id}_{lang}.html"
        html_file = sim_dir / f"{sim_id}_{lang}.html"
        try:
            req = urllib.request.Request(html_url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req, timeout=30) as resp:
                data = resp.read()
                with open(html_file, 'wb') as f:
                    f.write(data)
            kb = len(data) // 1024
            return (lang, kb)
        except urllib.error.HTTPError as e:
            if e.code == 404:
                continue
            return (None, 0)
        except:
            continue

    return (None, 0)

def main():
    print("Getting PhET simulation list from API...")
    sims = get_phet_catalog()
    print(f"Found {len(sims)} projects")

    # Download each sim
    success_zh = 0
    success_en = 0
    failed = []

    for i, sim in enumerate(sims):
        sim_id = sim['id']
        print(f"[{i+1}/{len(sims)}] {sim_id}...", end=" ", flush=True)

        # Try downloading to both directories
        for dest in [ANDROID_EXP, ROBOT_EXP]:
            dest.mkdir(parents=True, exist_ok=True)
            lang, kb = download_sim(sim_id, dest)
            if lang:
                if dest == ANDROID_EXP:
                    print(f"{'CN' if lang=='zh_CN' else 'EN'} {kb}KB", end=" ")
                    if lang == 'zh_CN':
                        success_zh += 1
                    else:
                        success_en += 1

        # Small delay to be nice to the server
        time.sleep(0.5)

    print(f"\n\nResults: {success_zh} Chinese + {success_en} English = {success_zh + success_en} total")
    if failed:
        print(f"Failed: {failed}")

    # Check total size
    total_mb = sum(f.stat().st_size for f in ANDROID_EXP.rglob('*') if f.is_file()) // 1024 // 1024
    print(f"Total size: {total_mb}MB")

if __name__ == "__main__":
    main()
