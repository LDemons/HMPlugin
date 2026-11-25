"""Genera el resource pack de HMPlugin: animacion de corazon roto (pixel art en lienzo de 48x48).

Uso: python resourcepack/generar_pack.py [carpeta_preview]
Salida: src/main/resources/HMPack.zip (va dentro del jar; el plugin lo sirve y lo envia al entrar)
y preview.gif en carpeta_preview si se pasa.
"""
import hashlib
import json
import math
import random
import sys
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

SIZE = 48
FONT_HEIGHT = 40   # alto en la pantalla (unidades de fuente; el titulo lo multiplica x4)
R = 12.0           # radio del corazon en pixeles a escala 1
CX, CY = 24.0, 25.4
FIRST_CHAR = 0xE000
NAMESPACE = "hmplugin"

OUTLINE = (55, 0, 8)
DARK = (150, 10, 25)
BASE = (215, 25, 45)
LIGHT = (255, 95, 105)
SPEC = (255, 225, 225)
CRACK = (35, 0, 5)
FLASH = (255, 240, 240)

# Grieta en coordenadas del corazon (u, v), de la muesca de arriba a la punta de abajo
CRACK_PATH = [(0, 1.0), (-0.16, 0.62), (0.15, 0.27), (-0.11, -0.08), (0.12, -0.45), (0, -1.0)]

# Grietas secundarias: (avance de la principal en que empiezan a salir, puntos desde la grieta hacia afuera)
BRANCHES = [
    (0.35, [(-0.16, 0.62), (-0.5, 0.78), (-0.78, 0.6)]),
    (0.35, [(0.15, 0.27), (0.48, 0.48), (0.8, 0.38)]),
    (0.6, [(-0.11, -0.08), (-0.42, -0.18), (-0.62, -0.02)]),
    (0.6, [(0.12, -0.45), (0.38, -0.5)]),
    (0.6, [(0.15, 0.27), (0.32, -0.02), (0.55, -0.1)]),
    (0.8, [(-0.5, 0.78), (-0.48, 1.0)]),
]


def inside(u, v):
    return (u * u + v * v - 1) ** 3 - u * u * v ** 3 <= 0


def crack_u(v):
    """u de la grieta a la altura v (interpolacion lineal del zigzag)."""
    for (u0, v0), (u1, v1) in zip(CRACK_PATH, CRACK_PATH[1:]):
        if v1 <= v <= v0:
            t = (v0 - v) / (v0 - v1)
            return u0 + (u1 - u0) * t
    return 0.0


def crack_dist(u, v, progress):
    """Distancia (en unidades u) a la grieta, si ya se dibujo hasta esa altura."""
    if v < 1.0 - 2.0 * progress:
        return 99
    return abs(u - crack_u(v))


def seg_dist(u, v, a, b):
    ax, ay = a
    dx, dy = b[0] - ax, b[1] - ay
    t = max(0.0, min(1.0, ((u - ax) * dx + (v - ay) * dy) / (dx * dx + dy * dy)))
    return math.hypot(u - ax - t * dx, v - ay - t * dy)


def branch_dist(u, v, progress):
    """Distancia a las grietas secundarias; cada una crece desde la principal segun el avance."""
    best = 99
    for start, pts in BRANCHES:
        f = (progress - start) / (1 - start)
        if f <= 0:
            continue
        segs = list(zip(pts, pts[1:]))
        n = min(1.0, f) * len(segs)
        for i, (a, b) in enumerate(segs):
            if i >= n:
                break
            k = min(1.0, n - i)
            best = min(best, seg_dist(u, v, a, (a[0] + (b[0] - a[0]) * k, a[1] + (b[1] - a[1]) * k)))
    return best


def shade(u, v):
    if (u + 0.55) ** 2 + (v - 0.72) ** 2 < 0.03:
        return SPEC
    s = -u * 0.4 + v * 0.5
    if s > 0.5:
        return LIGHT
    if s < -0.35:
        return DARK
    return BASE


def render_heart(img, scale=1.0, crack=0.0, main_crack=True, branches=False, flash=False, alpha=255, half=None, angle=0.0, dx=0.0, dy=0.0):
    """Dibuja el corazon (o una mitad, rotada sobre la punta de abajo) en img."""
    px = img.load()
    r = R * scale
    ca, sa = math.cos(-angle), math.sin(-angle)

    def to_heart(x, y):
        # pixel -> coords del corazon, deshaciendo traslacion y rotacion
        u = (x + 0.5 - CX - dx) / r
        v = -(y + 0.5 - CY - dy) / r
        pu, pv = u, v + 1.0          # pivote en la punta (0, -1)
        u = pu * ca - pv * sa
        v = pu * sa + pv * ca - 1.0
        return u, v

    def member(x, y):
        u, v = to_heart(x, y)
        if not inside(u, v):
            return False
        if half == "left":
            return u < crack_u(v)
        if half == "right":
            return u >= crack_u(v)
        return True

    for y in range(SIZE):
        for x in range(SIZE):
            if not member(x, y):
                continue
            edge = not all(member(x + ox, y + oy) for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            u, v = to_heart(x, y)
            if edge:
                c = OUTLINE
            elif crack > 0 and ((main_crack and crack_dist(u, v, crack) < 0.75 / r + 0.04)
                                or (branches and branch_dist(u, v, crack) < 0.065)):
                c = CRACK
            elif flash:
                c = FLASH
            else:
                c = shade(u, v)
            px[x, y] = c + (alpha,)


def make_frames(branches, shake=False):
    frames = []

    def new():
        return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))

    # Aparece, late dos veces, se agrieta, destello
    for sc in (0.3, 0.7, 1.12, 1.0, 1.0, 1.15, 1.05, 1.0, 1.15, 1.0):
        img = new()
        render_heart(img, scale=sc)
        frames.append(img)
    for cr in (0.35, 0.7, 1.0):
        img = new()
        render_heart(img, crack=cr, branches=branches)
        frames.append(img)
    if shake:
        # Tiembla antes de romperse (1 px hacia los lados)
        for sx, sy in ((1, 0), (-1, 0), (1, -1), (-1, 1), (1, 0), (-1, 0)):
            img = new()
            render_heart(img, crack=1.0, branches=branches, dx=sx, dy=sy)
            frames.append(img)
    img = new()
    render_heart(img, crack=1.0, branches=branches, flash=True)
    frames.append(img)

    # Se parte en dos mitades que caen y se desvanecen, con esquirlas
    rng = random.Random(7)
    shards = []
    for _ in range(12):
        v = rng.uniform(-0.8, 0.9)
        shards.append([CX + crack_u(v) * R, CY - v * R, rng.uniform(-1.6, 1.6), rng.uniform(-1.8, 0.2),
                       rng.choice((BASE, LIGHT, DARK)), rng.choice((1, 2))])
    for t in range(1, 7):
        img = new()
        alpha = int(255 * (1 - t / 7.5))
        ang = 0.11 * t
        render_heart(img, crack=1.0, main_crack=False, branches=branches, alpha=alpha, half="left", angle=ang, dx=-1.2 * t, dy=0.3 * t * t)
        render_heart(img, crack=1.0, main_crack=False, branches=branches, alpha=alpha, half="right", angle=-ang, dx=1.2 * t, dy=0.3 * t * t)
        px = img.load()
        for s in shards:
            s[0] += s[2]
            s[1] += s[3]
            s[3] += 0.35
            for oy in range(s[5]):
                for ox in range(s[5]):
                    x, y = int(s[0]) + ox, int(s[1]) + oy
                    if 0 <= x < SIZE and 0 <= y < SIZE:
                        px[x, y] = s[4] + (alpha,)
        frames.append(img)

    # Minecraft recorta las columnas transparentes de la derecha al medir el ancho del glifo y eso
    # descentra la imagen. Un pixel con alpha 1 (el shader de texto no dibuja alpha < 0.1) fija el ancho.
    for img in frames:
        img.putpixel((SIZE - 1, 0), (0, 0, 0, 1))
    return frames


def pale(img):
    """Version palida (ultima vida): mezcla cada color con un rosa grisaceo. El alpha no se toca."""
    out = img.copy()
    px = out.load()
    for y in range(SIZE):
        for x in range(SIZE):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (int(r * 0.5 + 225 * 0.5), int(g * 0.5 + 200 * 0.5), int(b * 0.5 + 205 * 0.5), a)
    return out


# Tragamonedas de eventos (EventSlot): se dibuja en el texto de una barra de jefe blanca (arriba, donde va el tab)
# 1 pixel = 1 unidad de la GUI. Caracteres: E100-E102 marco (luces A, luces B, todo encendido), E103 "?",
# E104... cabezas en el orden de SLOT_HEADS (debe coincidir con EventSlot.PLAYER_EVENTS)
SLOT_HEADS = ["Palgato", "Pelaoordinario", "Eledimins"]
SLOT_FIRST = 0xE100
SLOT_W, SLOT_H, SLOT_SYM = 100, 36, 24
SLOT_WINDOWS = (4, 36, 68)  # x de cada ventana de 28x28 (y = 4); el simbolo va 2 pixeles adentro
QUESTION = ["..XXXX..", ".XX..XX.", ".....XX.", "....XX..", "...XX...", "...XX...", "........", "...XX..."]


def slot_panel(lights):
    img = Image.new("RGBA", (SLOT_W, SLOT_H), (20, 5, 5, 255))
    px = img.load()
    for x in range(1, SLOT_W - 1):
        for y in range(1, SLOT_H - 1):
            px[x, y] = (200, 150, 40, 255) if x < 4 or x >= SLOT_W - 4 or y < 4 or y >= SLOT_H - 4 else (90, 15, 20, 255)
    for wx in SLOT_WINDOWS:
        for x in range(wx, wx + 28):
            for y in range(4, 32):
                if x in (wx, wx + 27) or y in (4, 31):
                    px[x, y] = (30, 20, 20, 255)
                else:
                    f = 1 - 0.35 * (abs(y - 17.5) / 13) ** 2  # mas oscuro arriba y abajo: parece un rodillo
                    px[x, y] = (int(235 * f), int(228 * f), int(210 * f), 255)
    for k, x in enumerate(range(3, SLOT_W - 4, 6)):
        on = lights == "all" or (k % 2 == 0) == (lights == "a")
        for y in (1, SLOT_H - 3):
            for dx in (0, 1):
                for dy in (0, 1):
                    px[x + dx, y + dy] = (255, 240, 150, 255) if on else (120, 80, 20, 255)
    return img


def slot_question():
    img = Image.new("RGBA", (SLOT_SYM, SLOT_SYM), (0, 0, 0, 0))
    px = img.load()
    filled = {(x, y) for y in range(SLOT_SYM) for x in range(SLOT_SYM) if QUESTION[y // 3][x // 3] == "X"}
    for x in range(SLOT_SYM):
        for y in range(SLOT_SYM):
            if (x, y) in filled:
                px[x, y] = (255, 200, 40, 255) if y % 12 > 1 else (255, 235, 140, 255)
            elif any((x + dx, y + dy) in filled for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                px[x, y] = (90, 50, 0, 255)
    px[SLOT_SYM - 1, 0] = (0, 0, 0, 1)  # Minecraft recorta columnas transparentes a la derecha: fija el ancho
    return img


def slot_head(skin_png):
    skin = Image.open(skin_png).convert("RGBA")
    face = skin.crop((8, 8, 16, 16))
    face.alpha_composite(skin.crop((40, 8, 48, 16)))  # capa del sombrero
    return face.resize((SLOT_SYM, SLOT_SYM), Image.NEAREST)


def add_slot(files, tex):
    images = [slot_panel("a"), slot_panel("b"), slot_panel("all"), slot_question()]
    images += [slot_head(tex / "cabezas" / f"{n}.png") for n in SLOT_HEADS]
    providers = []
    for i, img in enumerate(images):
        buf = BytesIO()
        img.save(buf, "PNG")
        files[f"assets/{NAMESPACE}/textures/font/slot_{i:02d}.png"] = buf.getvalue()
        # El texto de la barra empieza en y=3 de la pantalla: ascent 7 deja el marco desde ahi y los simbolos 6 mas abajo
        providers.append({"type": "bitmap", "file": f"{NAMESPACE}:font/slot_{i:02d}.png",
                          "ascent": 7 if i < 3 else 1, "height": img.height, "chars": [chr(SLOT_FIRST + i)]})
    # Espacios para volver al inicio del marco y saltar entre ventanas (avance de un bitmap = ancho + 1)
    providers.append({"type": "space", "advances": {"": -(SLOT_W + 1), "": 6, "": 7}})
    files[f"assets/{NAMESPACE}/font/slot.json"] = json.dumps({"providers": providers}, indent=2).encode()
    # La barra blanca queda invisible (solo la usa la tragamonedas; ni el plugin ni el juego usan blanca)
    buf = BytesIO()
    Image.new("RGBA", (182, 5), (0, 0, 0, 0)).save(buf, "PNG")
    for part in ("background", "progress"):
        files[f"assets/minecraft/textures/gui/sprites/boss_bar/white_{part}.png"] = buf.getvalue()
    return images


def build_pack(frames, out_zip):
    providers = []
    files = {}
    tex = Path(__file__).parent / "texturas"

    def put(path, data):
        files[path] = json.dumps(data, indent=2).encode() if isinstance(data, dict) else data

    # Bloque de Netherite Infernal (netherite_block.png del pack PERMADEATH V2.8). El plugin lo muestra con un
    # item display del tamano del bloque (item_model hmplugin:infernal_netherite) encima del spawner
    put(f"assets/{NAMESPACE}/textures/block/infernal_netherite.png", (tex / "infernal_netherite.png").read_bytes())
    put(f"assets/{NAMESPACE}/models/block/infernal_netherite.json",
        {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NAMESPACE}:block/infernal_netherite"}})
    put(f"assets/{NAMESPACE}/items/infernal_netherite.json",
        {"model": {"type": "minecraft:model", "model": f"{NAMESPACE}:block/infernal_netherite"}})

    # Items planos (texturas del pack PERMADEATH V2.8). Las piezas de armadura (V1.8) son grises y se tinen con el
    # color del cuero, igual que en el original: el plugin las tine de rojo
    for name in ["life_orb", "end_relic", "beginning_relic"] + [
            f"infernal_netherite_{p}" for p in ("helmet", "chestplate", "leggings", "boots")]:
        put(f"assets/{NAMESPACE}/textures/item/{name}.png", (tex / f"{name}.png").read_bytes())
        put(f"assets/{NAMESPACE}/models/item/{name}.json",
            {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NAMESPACE}:item/{name}"}})
        model = {"type": "minecraft:model", "model": f"{NAMESPACE}:item/{name}"}
        if name.startswith("infernal_netherite_"):
            model["tints"] = [{"type": "minecraft:dye", "default": -6265536}]
        put(f"assets/{NAMESPACE}/items/{name}.json", {"model": model})

    # Armadura puesta: equipamiento hmplugin:infernal_netherite (capas de cuero de la V1.8, tenidas con el color)
    dyeable = {"color_when_undyed": -6265536}
    put(f"assets/{NAMESPACE}/textures/entity/equipment/humanoid/infernal_netherite.png",
        (tex / "infernal_netherite_layer_1.png").read_bytes())
    put(f"assets/{NAMESPACE}/textures/entity/equipment/humanoid_leggings/infernal_netherite.png",
        (tex / "infernal_netherite_layer_2.png").read_bytes())
    put(f"assets/{NAMESPACE}/equipment/infernal_netherite.json", {"layers": {
        "humanoid": [{"texture": f"{NAMESPACE}:infernal_netherite", "dyeable": dyeable}],
        "humanoid_leggings": [{"texture": f"{NAMESPACE}:infernal_netherite", "dyeable": dyeable}]}})

    add_slot(files, tex)

    for i, img in enumerate(frames):
        name = f"heart_{i:02d}.png"
        buf = BytesIO()
        img.save(buf, "PNG")
        files[f"assets/{NAMESPACE}/textures/font/{name}"] = buf.getvalue()
        # ascent = alto/2 - 3 centra la imagen en la pantalla (el titulo se dibuja 10 unidades arriba del centro)
        providers.append({"type": "bitmap", "file": f"{NAMESPACE}:font/{name}",
                          "ascent": FONT_HEIGHT // 2 - 3, "height": FONT_HEIGHT, "chars": [chr(FIRST_CHAR + i)]})
    files[f"assets/{NAMESPACE}/font/heart.json"] = json.dumps({"providers": providers}, indent=2).encode()
    files["pack.mcmeta"] = json.dumps({"pack": {
        "description": "HMPlugin",
        "pack_format": 46, "supported_formats": [46, 999],
        "min_format": 46, "max_format": 999}}, indent=2).encode()

    # Fecha fija para que el zip (y su sha1) solo cambie si cambia el contenido
    with zipfile.ZipFile(out_zip, "w", zipfile.ZIP_DEFLATED) as z:
        for path in sorted(files):
            z.writestr(zipfile.ZipInfo(path, (2026, 1, 1, 0, 0, 0)), files[path])
    return hashlib.sha1(out_zip.read_bytes()).hexdigest()


def build_preview(frames, durations, out_gif):
    bg = (30, 30, 34, 255)
    imgs = []
    for f in frames:
        canvas = Image.new("RGBA", (SIZE, SIZE), bg)
        canvas.alpha_composite(f)
        imgs.append(canvas.resize((SIZE * 6, SIZE * 6), Image.NEAREST).convert("P"))
    imgs.append(Image.new("RGBA", (SIZE * 6, SIZE * 6), bg).convert("P"))
    imgs[0].save(out_gif, save_all=True, append_images=imgs[1:], duration=durations + [800], loop=0)


if __name__ == "__main__":
    here = Path(__file__).parent
    frames = make_frames(branches=False)
    assert len(frames) == 20, len(frames)
    # Ultima vida: palido, con grietas secundarias y 6 fotogramas de temblor antes del destello
    pale_frames = [pale(f) for f in make_frames(branches=True, shake=True)]
    assert len(pale_frames) == 26, len(pale_frames)
    # - normales, - palidos (ultima vida); HeartAnimation depende de este orden
    sha1 = build_pack(frames + pale_frames, here.parent / "src" / "main" / "resources" / "HMPack.zip")
    print(f"frames={len(frames)}+{len(pale_frames)} sha1={sha1}")
    if len(sys.argv) > 1:
        out = Path(sys.argv[1])
        build_preview(frames, [100] * 20, out / "preview.gif")
        # Ultima vida: los 3 fotogramas de grieta (10-12) duran 12 ticks en vez de 2, como en HeartAnimation
        build_preview(pale_frames, [600 if 10 <= i <= 12 else 100 for i in range(26)], out / "preview_ultima.gif")
