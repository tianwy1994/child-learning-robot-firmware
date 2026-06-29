"""Pre-generate TTS audio for all experiment sections.
Saves PCM files that can be loaded instantly by the Android app and web server.

Usage: python scripts/precache_tts.py
"""
import json, os, urllib.request, time
from pathlib import Path

TTS_URL = "http://192.168.31.117:8080/api/hardware/tts/speak"
ANDROID_AUDIO_DIR = Path(__file__).parent.parent / "android" / "app" / "src" / "main" / "assets" / "experiments" / "audio"
ROBOT_AUDIO_DIR = Path(__file__).parent.parent.parent / "child-learning-robot" / "src" / "main" / "resources" / "static" / "experiments" / "audio"

# Load catalog from Android assets
CATALOG_PATH = Path(__file__).parent.parent / "android" / "app" / "src" / "main" / "assets" / "experiments" / "catalog.json"

with open(CATALOG_PATH, "r", encoding="utf-8") as f:
    catalog = json.load(f)

# Collect all voice sections
sections = []
for cat in catalog["categories"]:
    for exp in cat["experiments"]:
        vi = exp.get("voiceIntro", "")
        if not vi:
            continue
        # Parse sections
        for i, line in enumerate(vi.strip().split("\n")):
            text = line.strip()
            if not text:
                continue
            # Remove emoji prefix
            clean = text[1:].strip() if text and text[0] in "🔬📋✨🎯🏠" else text
            sections.append({
                "exp_id": exp["id"],
                "exp_title": exp["title"],
                "section_idx": i,
                "text": clean[:490]  # Keep under 500 char limit
            })

print(f"Total sections to pre-cache: {len(sections)}")

# Create output dirs
for d in [ANDROID_AUDIO_DIR, ROBOT_AUDIO_DIR]:
    d.mkdir(parents=True, exist_ok=True)

# Generate manifest
manifest = {}

for i, sec in enumerate(sections):
    exp_id = sec["exp_id"]
    idx = sec["section_idx"]
    text = sec["text"]
    filename = f"{exp_id}_{idx}.pcm"
    filepath = f"experiments/audio/{filename}"

    # Initialize manifest entry
    if exp_id not in manifest:
        manifest[exp_id] = []
    manifest[exp_id].append({
        "file": filepath,
        "section": idx,
        "text_preview": text[:30]
    })

    # Check if already cached
    cached = (ANDROID_AUDIO_DIR / filename).exists() and (ROBOT_AUDIO_DIR / filename).exists()
    if cached and (ANDROID_AUDIO_DIR / filename).stat().st_size > 1000:
        print(f"[SKIP {i+1}/{len(sections)}] {filename} already cached")
        continue

    print(f"[{i+1}/{len(sections)}] Synthesizing {exp_id}[{idx}]: {text[:40]}...", end=" ", flush=True)

    try:
        req = urllib.request.Request(TTS_URL,
            data=json.dumps({"text": text, "speed": "normal"}).encode("utf-8"),
            headers={"Content-Type": "application/json"})
        start = time.time()
        with urllib.request.urlopen(req, timeout=300) as resp:
            audio = resp.read()
        elapsed = time.time() - start

        if len(audio) > 100:
            # Save to both directories
            for d in [ANDROID_AUDIO_DIR, ROBOT_AUDIO_DIR]:
                with open(d / filename, "wb") as f:
                    f.write(audio)
            print(f"OK {len(audio)//1024}KB in {elapsed:.0f}s")
        else:
            print(f"FAIL (empty response)")
    except Exception as e:
        print(f"FAIL ({e})")

# Save manifest to both locations
for d in [ANDROID_AUDIO_DIR.parent, ROBOT_AUDIO_DIR.parent]:
    with open(d / "audio_manifest.json", "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)

print(f"\nDone! Manifest saved to audio_manifest.json")
# Count total size
total_size = sum(f.stat().st_size for f in ANDROID_AUDIO_DIR.glob("*.pcm")) if ANDROID_AUDIO_DIR.exists() else 0
print(f"Total audio size: {total_size // 1024 // 1024}MB")
