"""
Builds the editor's downloadable asset libraries into OUT (published to the `assets` branch).
Sources without a public API are mirrored here; the app fetches files from
raw.githubusercontent.com/<repo>/assets/... on demand. Every source is best-effort: a failure is
logged to OUT/log.txt and the other libraries are still built.
"""
import io, json, os, re, subprocess, sys, urllib.request, zipfile
from PIL import Image

OUT = sys.argv[1]
LOG = []
UA = {"User-Agent": "Mozilla/5.0 (PixelForge asset builder)"}


def log(*a):
    s = " ".join(str(x) for x in a)
    print(s, flush=True)
    LOG.append(s)


def get(url, timeout=60):
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=timeout) as r:
        return r.read()


def save_png(data, path, side):
    """Saves an image scaled to fit side x side (never upscaled) as an optimised PNG."""
    im = Image.open(io.BytesIO(data)).convert("RGBA")
    im.thumbnail((side, side), Image.LANCZOS)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path, optimize=True)
    return im.size


def words(name):
    """camelCase / snake / kebab name -> lower-case words."""
    name = re.sub(r"([a-z])([A-Z])", r"\1 \2", name)
    return [w for w in re.split(r"[\s_\-]+", name.lower()) if w]


# --- Kenney UI Pack: Sci-Fi (CC0) -------------------------------------------------------------
def kenney():
    # The official pack, uploaded once as a release asset (kenney.nl has no direct link).
    z = zipfile.ZipFile(io.BytesIO(get("https://github.com/ohavs/Pixel-Forge-Studio/releases/download/asset-sources/kenney-scifi.zip", 300)))
    pngs = [n for n in z.namelist() if n.lower().endswith(".png") and "/png/" in ("/" + n.lower()) and "preview" not in n.lower() and "sample" not in n.lower()]
    log("kenney pngs:", len(pngs), pngs[:12])
    groups = {}
    for n in pngs:
        base = os.path.splitext(os.path.basename(n))[0]
        # Packs ship "Double" (2x) copies too; keep the larger one when both exist.
        double = "double" in n.lower()
        key = base
        rel = f"kenney-scifi/{key}.png"
        dst = os.path.join(OUT, rel)
        if os.path.exists(dst) and not double:
            continue
        w, h = save_png(z.read(n), dst, 512)
        g = words(base)[0] if words(base) else "misc"
        groups.setdefault(g, {})[key] = {"n": key, "f": rel, "w": w, "h": h}
    cat = [{"g": g, "items": sorted(v.values(), key=lambda i: i["n"])} for g, v in sorted(groups.items())]
    json.dump(cat, open(os.path.join(OUT, "catalog/kenney-scifi.json"), "w"), separators=(",", ":"))
    log("kenney groups:", {c["g"]: len(c["items"]) for c in cat})


# --- 3dicons (CC0) ----------------------------------------------------------------------------
# Built from the official download pack, uploaded once as a release asset of this repo.
PACK3D = "https://github.com/ohavs/Pixel-Forge-Studio/releases/download/asset-sources/3dicons.zip"
STYLES3D = ["color", "clay", "gradient", "premium"]


def threedicons():
    z = zipfile.ZipFile(io.BytesIO(get(PACK3D, 600)))
    pngs = [n for n in z.namelist() if n.lower().endswith(".png") and "__macosx" not in n.lower()]
    log("3dicons pack pngs:", len(pngs), pngs[:10])
    names = {}
    for n in pngs:
        low = n.lower()
        style = next((s for s in STYLES3D if s in low), None)
        # Keep one angle (the "dynamic" 3/4 view) when the pack has several.
        if style is None or ("dynamic" not in low and any(a in low for a in ("front", "iso"))):
            continue
        base = os.path.splitext(os.path.basename(low))[0]
        name = re.sub(r"[-_ ]*(dynamic|front|iso|color|clay|gradient|premium|\d+x)[-_ ]*", "-", base).strip("-")
        dst = os.path.join(OUT, f"3dicons/{style}/{name}.png")
        if not os.path.exists(dst):
            save_png(z.read(n), dst, 256)
            names.setdefault(name, set()).add(style)
    items = [{"n": n} for n, st in sorted(names.items()) if "color" in st]
    json.dump({"styles": STYLES3D, "items": items}, open(os.path.join(OUT, "catalog/3dicons.json"), "w"), separators=(",", ":"))
    log("3dicons built:", len(items))


# --- Fluent Emoji 3D (MIT) --------------------------------------------------------------------
def fluent():
    d = "/tmp/fluent"
    if not os.path.exists(d):
        subprocess.run(["git", "clone", "-q", "--depth", "1", "--filter=blob:none", "--no-checkout",
                        "https://github.com/microsoft/fluentui-emoji.git", d], check=True)
    paths = subprocess.run(["git", "-C", d, "ls-tree", "-r", "--name-only", "HEAD", "assets"], check=True, capture_output=True, text=True).stdout.split("\n")
    metas = [p for p in paths if p.endswith("/metadata.json")]
    subprocess.run(["git", "-C", d, "checkout", "-q", "HEAD", "--"] + metas, check=True)
    threed = {}
    for p in paths:
        parts = p.split("/")
        # assets/<Name>/3D/x_3d.png  or  assets/<Name>/Default/3D/x_3d_default.png
        if p.endswith(".png") and "/3D/" in p and (len(parts) == 4 or (len(parts) == 5 and parts[2] == "Default")):
            threed[parts[1]] = p
    groups = {}
    for m in metas:
        name = m.split("/")[1]
        if name not in threed:
            continue
        meta = json.load(open(os.path.join(d, m)))
        g = meta.get("group", "Other")
        groups.setdefault(g, []).append({"n": name, "f": threed[name], "k": " ".join(meta.get("keywords", [])[:6])})
    cat = [{"g": g, "items": sorted(v, key=lambda i: i["n"])} for g, v in groups.items()]
    json.dump(cat, open(os.path.join(OUT, "catalog/fluent3d.json"), "w"), separators=(",", ":"))
    log("fluent3d groups:", {c["g"]: len(c["items"]) for c in cat})


# --- Iconify sets, grouped (the app draws them through api.iconify.design) ----------------------
def npm_json(pkg, path):
    """Reads one JSON file from an npm package (downloads the tarball into /tmp/npm)."""
    d = f"/tmp/npm/{pkg.replace('/', '_')}"
    if not os.path.exists(d):
        os.makedirs(d)
        tgz = subprocess.run(["npm", "pack", "-q", pkg], cwd=d, check=True, capture_output=True, text=True).stdout.strip().split("\n")[-1]
        subprocess.run(["tar", "xzf", tgz], cwd=d, check=True)
    return json.load(open(os.path.join(d, "package", path)))


def write(name, groups):
    cat = [{"g": g, "items": [{"n": n} for n in v]} for g, v in groups if v]
    json.dump(cat, open(os.path.join(OUT, f"catalog/{name}.json"), "w"), separators=(",", ":"))
    log(name, {c["g"]: len(c["items"]) for c in cat})


def keyword_groups(names, rules, rest):
    """Groups names by the first rule (title, words) whose word appears in the name."""
    out = {t: [] for t, _ in rules}
    out[rest] = []
    for n in names:
        for t, ws in rules:
            if any(w in n for w in ws):
                out[t].append(n)
                break
        else:
            out[rest].append(n)
    return list(out.items())


def tabler():
    meta = npm_json("@tabler/icons", "icons.json")
    names = set(npm_json("@iconify-json/tabler", "icons.json")["icons"])
    groups = {}
    for m in meta.values():
        c = m.get("category") or "Other"
        if m["name"] in names:
            groups.setdefault(c, []).append("tabler:" + m["name"])
        if "filled" in m.get("styles", {}) and m["name"] + "-filled" in names:
            groups.setdefault("Filled", []).append("tabler:" + m["name"] + "-filled")
    order = sorted(groups, key=lambda g: (g == "Brand", -len(groups[g])))
    write("tabler", [(g, sorted(groups[g])) for g in order])


def meteocons():
    names = sorted(npm_json("@iconify-json/meteocons", "icons.json")["icons"])
    rules = [("Sun", ["clear-day", "sun", "hot", "uv-index"]), ("Night", ["night", "moon", "star"]),
             ("Clouds", ["cloud", "overcast", "fog", "haze", "mist", "smoke", "dust"]), ("Rain", ["rain", "drizzle", "umbrella", "raindrop"]),
             ("Snow", ["snow", "sleet", "hail", "snowflake"]), ("Storm", ["thunder", "lightning", "tornado", "hurricane"]),
             ("Wind", ["wind", "windsock"]), ("Temperature", ["thermometer", "celsius", "fahrenheit", "barometer", "humidity", "pressure"])]
    fill = [f"meteocons:{n}" for n in names if n.endswith("-fill")]
    line = [f"meteocons:{n}" for n in names if not n.endswith("-fill")]
    write("meteocons", [(g, v) for g, v in keyword_groups(fill, rules, "More")] + [("Line", line)])


def vectors():
    """The open sets that stand in for SVG Repo: one group per set, one style each to keep it tidy."""
    sets = [("Phosphor", "ph", lambda n: n.endswith("-duotone")), ("Solar", "solar", lambda n: n.endswith("-bold-duotone")),
            ("Game icons", "game-icons", lambda n: True), ("Noto", "noto", lambda n: True),
            ("Streamline", "streamline-emojis", lambda n: True), ("Flat color", "flat-color-icons", lambda n: True),
            ("Fluent color", "fluent-color", lambda n: True), ("Flags", "circle-flags", lambda n: "-" not in n or len(n) <= 6),
            ("Material", "material-symbols", lambda n: n.endswith("-rounded") and "outline" not in n)]
    out = []
    for title, prefix, keep in sets:
        try:
            names = sorted(npm_json(f"@iconify-json/{prefix}", "icons.json")["icons"])
            out.append((title, [f"{prefix}:{n}" for n in names if keep(n)]))
        except Exception as e:
            log("vectors skip", prefix, repr(e))
    write("vectors", out)


os.makedirs(os.path.join(OUT, "catalog"), exist_ok=True)
for f in (tabler, meteocons, vectors, fluent, kenney, threedicons):
    try:
        f()
    except Exception as e:
        log("FAILED", f.__name__, repr(e))
open(os.path.join(OUT, "log.txt"), "w").write("\n".join(LOG) + "\n")
