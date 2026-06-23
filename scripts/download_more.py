"""Download additional PhET sims for the expanded catalog."""
import json, os, urllib.request
from pathlib import Path

BASE = "https://phet.colorado.edu/sims/html"
ASSETS = Path(__file__).parent.parent / "android" / "app" / "src" / "main" / "assets" / "experiments" / "phet"

new_sims = [
    ("battery-voltage", "电池电压"),
    ("beers-law-lab", "比尔定律"),
    ("build-a-molecule", "搭建分子"),
    ("charges-and-fields", "电荷与电场"),
    ("coulomb-force", "库仑力"),
    ("friction", "摩擦力"),
    ("gravity-force-lab", "万有引力实验室"),
    ("hookes-law", "胡克定律"),
    ("molecule-shapes", "分子形状"),
    ("ohms-law", "欧姆定律"),
    ("resistance-in-a-wire", "导线电阻"),
    ("states-of-matter", "物质状态"),
]

for sim_id, title in new_sims:
    sim_dir = ASSETS / sim_id
    sim_dir.mkdir(parents=True, exist_ok=True)

    downloaded = False
    for lang in ["zh_CN", "en"]:
        html_url = f"{BASE}/{sim_id}/latest/{sim_id}_{lang}.html"
        html_file = sim_dir / f"{sim_id}_{lang}.html"
        try:
            req = urllib.request.Request(html_url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, timeout=30) as r:
                data = r.read()
                with open(html_file, "wb") as f:
                    f.write(data)
            kb = len(data) // 1024
            print(f"OK {sim_id:40s} {lang:5s} {kb:>4d}KB")
            downloaded = True
            break
        except urllib.error.HTTPError as e:
            if e.code == 404:
                continue
            print(f"ERR {sim_id}: {e}")
            break
        except Exception as e:
            print(f"ERR {sim_id}: {e}")
            break

    if not downloaded:
        print(f"SKIP {sim_id}: not found on PhET")
        import shutil
        shutil.rmtree(sim_dir, ignore_errors=True)

# Update catalog.json with all sims
catalog_file = ASSETS.parent / "catalog.json"
with open(catalog_file, "r", encoding="utf-8") as f:
    catalog = json.load(f)

# Map of all known sims to their details
all_sims = {
    # 化学
    "acid-base-solutions": ("酸碱溶液", "探究酸碱 pH 值与浓度的关系", 1),
    "balancing-chemical-equations": ("化学方程式配平", "练习配平化学方程式", 2),
    "battery-voltage": ("电池电压", "探究电池串联与电压的关系", 1),
    "beers-law-lab": ("比尔定律", "探究溶液浓度与吸光度的关系", 3),
    "build-a-molecule": ("搭建分子", "用原子搭建分子模型", 1),
    "concentration": ("溶液浓度", "改变溶质溶剂的量观察浓度变化", 1),
    "molarity": ("摩尔浓度", "探究摩尔浓度的概念", 2),
    "molecule-shapes": ("分子形状", "探究 VSEPR 理论与分子几何结构", 2),
    "ph-scale": ("pH 标度", "测量常见液体的 pH 值", 1),
    "reactants-products-and-leftovers": ("反应物与生成物", "探究化学反应中的质量守恒", 1),
    "states-of-matter": ("物质状态", "观察固液气三态变化", 1),
    "sugar-and-salt-solutions": ("糖与盐的溶解（英文）", "观察离子分子在溶液中的行为", 2),
    # 物理
    "bending-light": ("光的折射", "探究光在不同介质中的传播", 2),
    "charges-and-fields": ("电荷与电场", "观察电场线和电势分布", 2),
    "circuit-construction-kit-dc": ("电路搭建（直流）", "搭建串联和并联电路", 1),
    "circuit-construction-kit-dc-virtual-lab": ("电路虚拟实验室", "用万用表测量电压电流", 2),
    "coulomb-force": ("库仑力", "探究电荷间的作用力", 2),
    "energy-skate-park-basics": ("能量滑板公园", "探究动能与势能的转化", 1),
    "faradays-law": ("法拉第电磁感应", "探究磁通量变化产生电流", 2),
    "forces-and-motion-basics": ("力与运动基础", "探究力和加速度的关系", 1),
    "friction": ("摩擦力", "探究摩擦力的影响因素", 1),
    "gravity-force-lab": ("万有引力实验室", "探究质量与距离对引力的影响", 1),
    "hookes-law": ("胡克定律", "探究弹簧弹力与形变的关系", 1),
    "magnet-and-compass": ("磁铁与指南针（英文）", "观察磁场方向", 1),
    "ohms-law": ("欧姆定律", "探究电压电流电阻的关系", 1),
    "pendulum-lab": ("单摆实验", "探究摆长质量对周期的影响", 2),
    "projectile-motion": ("抛体运动", "调整角度和速度观察抛射轨迹", 2),
    "resistance-in-a-wire": ("导线电阻", "探究导线长度粗细对电阻的影响", 1),
    "wave-on-a-string": ("绳波", "观察横波的传播和反射", 1),
    # 生物
    "gene-expression-essentials": ("基因表达基础", "探究基因如何表达蛋白质", 3),
    "natural-selection": ("自然选择", "模拟环境对物种选择的影响", 2),
    "neuron": ("神经元", "观察神经信号的传导", 2),
    # 科学
    "gravity-and-orbits": ("引力与轨道", "探究天体运动规律", 2),
    "greenhouse-effect": ("温室效应（英文）", "探究大气成分对温度的影响", 1),
    "my-solar-system": ("我的太阳系（英文）", "模拟行星绕太阳运行", 2),
    "under-pressure": ("液体压强", "探究液体深度与压强的关系", 2),
}

categories = [
    ("化学", "🧪", ["acid-base-solutions","balancing-chemical-equations","battery-voltage","beers-law-lab","build-a-molecule","concentration","molarity","molecule-shapes","ph-scale","reactants-products-and-leftovers","states-of-matter","sugar-and-salt-solutions"]),
    ("物理", "⚡", ["bending-light","charges-and-fields","circuit-construction-kit-dc","circuit-construction-kit-dc-virtual-lab","coulomb-force","energy-skate-park-basics","faradays-law","forces-and-motion-basics","friction","gravity-force-lab","hookes-law","magnet-and-compass","ohms-law","pendulum-lab","projectile-motion","resistance-in-a-wire","wave-on-a-string"]),
    ("生物", "🧬", ["gene-expression-essentials","natural-selection","neuron"]),
    ("科学", "🔭", ["gravity-and-orbits","greenhouse-effect","my-solar-system","under-pressure"]),
]

new_catalog = {"version": 2, "categories": []}
for cat_name, cat_icon, sim_ids in categories:
    exps = []
    for sid in sim_ids:
        if sid in all_sims:
            title, desc, diff = all_sims[sid]
            exps.append({"id": sid, "title": title, "desc": desc, "difficulty": diff})
    new_catalog["categories"].append({"name": cat_name, "icon": cat_icon, "experiments": exps})

with open(catalog_file, "w", encoding="utf-8") as f:
    json.dump(new_catalog, f, ensure_ascii=False, indent=2)

print(f"\nCatalog updated: {sum(len(c['experiments']) for c in new_catalog['categories'])} experiments total")

# Check final sizes
total_size = 0
total_count = 0
print("\nFinal offline experiments:")
for sim_dir in sorted(ASSETS.iterdir()):
    if sim_dir.is_dir():
        size = sum(f.stat().st_size for f in sim_dir.rglob("*") if f.is_file())
        if size > 0:
            total_size += size
            total_count += 1
            html = list(sim_dir.glob("*.html"))
            lang = "zh_CN" if any("zh_CN" in h.name for h in html) else "en" if html else "?"
            print(f"  {sim_dir.name:45s} {lang:5s} {size//1024:>4d}KB")
print(f"\nTotal: {total_count} sims, {total_size//1024//1024}MB")
