"""
Download PhET simulations offline into Android assets.
Usage: python scripts/download_phet.py
"""
import os, re, json, urllib.request, sys
from pathlib import Path

BASE_URL = "https://phet.colorado.edu/sims/html"
ASSETS_DIR = Path(__file__).parent.parent / "android" / "app" / "src" / "main" / "assets" / "experiments"

def ensure_dir(path):
    os.makedirs(path, exist_ok=True)

def download_file(url, dest):
    """Download a file, skip if exists."""
    if dest.exists() and dest.stat().st_size > 0:
        return False  # already exists
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = resp.read()
            with open(dest, 'wb') as f:
                f.write(data)
        print(f"  OK {dest.name} ({len(data)//1024}KB)")
        return True
    except Exception as e:
        print(f"  FAIL {dest.name}: {e}")
        return False

def download_sim(sim_id):
    """Download a PhET simulation completely."""
    url = f"{BASE_URL}/{sim_id}/latest/"
    sim_dir = ASSETS_DIR / "phet" / sim_id
    ensure_dir(sim_dir)

    # Download the HTML
    html_file = sim_dir / f"{sim_id}_zh_CN.html"
    html_url = f"{url}{sim_id}_zh_CN.html"
    if not download_file(html_url, html_file) and not html_file.exists():
        # Try English fallback
        html_url_en = f"{url}{sim_id}_en.html"
        html_file_en = sim_dir / f"{sim_id}_en.html"
        download_file(html_url_en, html_file_en)
        html_file = html_file_en

    if not html_file.exists():
        print(f"  SKIP {sim_id}: no HTML found")
        return False

    # Parse HTML for local resources
    with open(html_file, 'r', encoding='utf-8', errors='ignore') as f:
        html = f.read()

    # Find all resource URLs (relative paths)
    patterns = [
        r'src="([^"]+)"',
        r'href="([^"]+)"',
        r'data-main="([^"]+)"',
    ]

    resources = set()
    for pat in patterns:
        for m in re.finditer(pat, html):
            res = m.group(1)
            if res.startswith("http") or res.startswith("//"):
                continue
            if res.startswith("data:"):
                continue
            resources.add(res)

    # Download each resource
    for res in sorted(resources):
        res_url = url + res
        res_path = sim_dir / res
        ensure_dir(res_path.parent)
        download_file(res_url, res_path)

    print(f"  Downloaded {len(resources)} resources for {sim_id}")
    return True

def main():
    # Load catalog
    catalog_file = ASSETS_DIR / "catalog.json"
    with open(catalog_file, 'r', encoding='utf-8') as f:
        catalog = json.load(f)

    all_sims = []
    for cat in catalog["categories"]:
        for exp in cat["experiments"]:
            all_sims.append(exp["id"])

    print(f"Total sims to download: {len(all_sims)}")
    print()

    for sim_id in all_sims:
        print(f"Downloading {sim_id}...")
        download_sim(sim_id)
        print()

    # Generate local catalog
    local_catalog = {
        "version": catalog["version"],
        "categories": [
            {
                "name": cat["name"],
                "icon": cat["icon"],
                "experiments": [
                    {"id": e["id"], "title": e["title"], "desc": e["desc"], "difficulty": e["difficulty"], "local": True}
                    for e in cat["experiments"]
                ]
            }
            for cat in catalog["categories"]
        ]
    }
    with open(ASSETS_DIR / "catalog_local.json", 'w', encoding='utf-8') as f:
        json.dump(local_catalog, f, ensure_ascii=False, indent=2)

    print("Done! Local catalog saved to catalog_local.json")
    print(f"Total size: {sum(os.path.getsize(f) for f in ASSETS_DIR.rglob('*') if f.is_file()) // 1024 // 1024}MB")

if __name__ == "__main__":
    main()
