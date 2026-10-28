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
    page = get("https://kenney.nl/assets/ui-pack-sci-fi").decode("utf-8", "replace")
    zips = sorted(set(re.findall(r"https://kenney\.nl/media/pages/assets/[^\"'\s]+?\.zip", page)))
    log("kenney zips:", zips)
    if not zips:
        raise RuntimeError("no zip link on the asset page")
    z = zipfile.ZipFile(io.BytesIO(get(zips[0], 180)))
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
CDN3D = "https://3dicons.sgp1.cdn.digitaloceanspaces.com/v1"
STYLES3D = ["color", "clay", "gradient", "premium"]


def threedicons():
    html = get("https://3dicons.co").decode("utf-8", "replace")
    urls = set(re.findall(r"https?://[^\"'\s\\)]+?\.(?:png|webp|jpg)", html))
    log("3dicons home: bytes", len(html), "image urls", len(urls), sorted(urls)[:15])
    names = set(re.findall(r"/v1/(?:dynamic|front|iso)/\w+/([a-z0-9\-]+?)-(?:dynamic|front|iso)-\w+\.png", html))
    # Next.js pages keep their data in script chunks: look there too.
    for js in sorted(set(re.findall(r"/_next/static/[^\"'\s]+?\.js", html)))[:60]:
        try:
            src = get("https://3dicons.co" + js).decode("utf-8", "replace")
        except Exception as e:
            continue
        names |= set(re.findall(r"/v1/(?:dynamic|front|iso)/\w+/([a-z0-9\-]+?)-(?:dynamic|front|iso)-\w+\.png", src))
        found = re.findall(r'"?(?:name|slug|title)"?\s*:\s*"([a-z0-9\- ]{2,40})"', src)
        if found:
            log("3dicons chunk", js, "name-like strings", len(found), found[:10])
    nd = re.search(r'<script id="__NEXT_DATA__"[^>]*>(.*?)</script>', html, re.S)
    if nd:
        data = nd.group(1)
        log("3dicons __NEXT_DATA__ bytes", len(data), data[:400])
        names |= set(re.findall(r'"(?:name|slug)"\s*:\s*"([a-z0-9\-]{2,40})"', data))
    log("3dicons candidate names:", len(names), sorted(names)[:40])
    items = []
    for n in sorted(names):
        ok = 0
        for s in STYLES3D:
            rel = f"3dicons/{s}/{n}.png"
            try:
                save_png(get(f"{CDN3D}/dynamic/{s}/{n}-dynamic-{s}.png"), os.path.join(OUT, rel), 256)
                ok += 1
            except Exception as e:
                if s == "color":
                    log("3dicons miss", n, e)
                    break
        if ok:
            items.append({"n": n, "s": ok})
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


os.makedirs(os.path.join(OUT, "catalog"), exist_ok=True)
for f in (fluent, kenney, threedicons):
    try:
        f()
    except Exception as e:
        log("FAILED", f.__name__, repr(e))
open(os.path.join(OUT, "log.txt"), "w").write("\n".join(LOG) + "\n")
