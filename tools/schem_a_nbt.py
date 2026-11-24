"""
Convierte schematics de WorldEdit (.schem, Sponge v2/v3) a estructuras vanilla (.nbt) que se cargan con
la Structure API de Bukkit. Sin aire ni structure_void (al pegar, esos bloques no tocan el mundo).
Guarda el DataVersion del schematic: Paper actualiza bloques, items y spawners al cargarlo.

Uso: python tools/schem_a_nbt.py <carpeta con .schem> src/main/resources/beginning
"""
import gzip
import io
import os
import struct
import sys


def read_nbt(data):
    p = [0]

    def u(fmt):
        v = struct.unpack_from('>' + fmt, data, p[0])[0]
        p[0] += struct.calcsize('>' + fmt)
        return v

    def s():
        n = u('H')
        v = data[p[0]:p[0] + n].decode('utf8')
        p[0] += n
        return v

    def payload(t):
        if t in (1, 2, 3, 4, 5, 6):
            return u('bhiqfd'[t - 1])
        if t == 7:
            n = u('i'); v = data[p[0]:p[0] + n]; p[0] += n; return v
        if t == 8:
            return s()
        if t == 9:
            et = u('b'); n = u('i'); return (et, [payload(et) for _ in range(n)])
        if t == 10:
            o = {}
            while True:
                tt = u('b')
                if tt == 0:
                    return o
                k = s()
                o[k] = (tt, payload(tt))
        if t == 11:
            n = u('i'); return [u('i') for _ in range(n)]
        if t == 12:
            n = u('i'); return [u('q') for _ in range(n)]
        raise ValueError(t)

    t = u('b'); s()
    return payload(t)


def write_nbt(root):
    out = io.BytesIO()

    def w(fmt, v):
        out.write(struct.pack('>' + fmt, v))

    def ws(v):
        b = v.encode('utf8'); w('H', len(b)); out.write(b)

    def payload(t, v):
        if t in (1, 2, 3, 4, 5, 6):
            w('bhiqfd'[t - 1], v)
        elif t == 7:
            w('i', len(v)); out.write(v)
        elif t == 8:
            ws(v)
        elif t == 9:
            et, items = v
            w('b', et if items else 0); w('i', len(items))
            for i in items:
                payload(et, i)
        elif t == 10:
            for k, (tt, vv) in v.items():
                w('b', tt); ws(k); payload(tt, vv)
            w('b', 0)
        elif t == 11:
            w('i', len(v)); [w('i', i) for i in v]
        elif t == 12:
            w('i', len(v)); [w('q', i) for i in v]

    w('b', 10); ws('')
    payload(10, root)
    return gzip.compress(out.getvalue())


def varints(b):
    out, i = [], 0
    while i < len(b):
        v = sh = 0
        while True:
            x = b[i]; i += 1; v |= (x & 127) << sh; sh += 7
            if not x & 128:
                break
        out.append(v)
    return out


# Cambios propios a los spawners del original: el de cerdos de encima del zepelin de la Ytic saca ghasts
SPAWNER_MOBS = {'minecraft:pig': 'minecraft:ghast'}


def set_spawner_mob(nbt):
    entities = []
    if 'SpawnData' in nbt:
        entities.append(nbt['SpawnData'][1]['entity'][1])
    for p in nbt.get('SpawnPotentials', (9, (10, [])))[1][1]:
        entities.append(p['data'][1]['entity'][1])
    for ent in entities:
        if ent['id'][1] in SPAWNER_MOBS:
            ent['id'] = (8, SPAWNER_MOBS[ent['id'][1]])


def convert(path):
    r = read_nbt(gzip.open(path).read())
    if 'Schematic' in r:
        r = r['Schematic'][1]
    W, H, L = r['Width'][1], r['Height'][1], r['Length'][1]
    if 'Blocks' in r:  # v3
        blocks = r['Blocks'][1]
        pal, data = blocks['Palette'][1], blocks['Data'][1]
        ents = blocks.get('BlockEntities', (10, []))[1][1]
    else:  # v2
        pal, data = r['Palette'][1], r['BlockData'][1]
        ents = r.get('BlockEntities', (9, (10, [])))[1][1]
    names = {v[1]: k for k, v in pal.items()}

    entity_nbt = {}
    for e in ents:
        e = dict(e)
        pos = tuple(e.pop('Pos')[1])
        bid = e.pop('Id')[1]
        if 'Data' in e:  # v3 guarda los campos dentro de Data
            e = dict(e['Data'][1])
        e['id'] = (8, bid)
        if 'spawner' in bid:
            set_spawner_mob(e)
        entity_nbt[pos] = e

    palette, index, out_blocks = [], {}, []
    for i, sid in enumerate(varints(data)):
        name = names[sid]
        if name in ('minecraft:air', 'minecraft:structure_void'):
            continue
        if name not in index:
            index[name] = len(palette)
            base, _, props = name.partition('[')
            entry = {'Name': (8, base)}
            if props:
                entry['Properties'] = (10, {k: (8, v) for k, v in
                                            (kv.split('=') for kv in props.rstrip(']').split(','))})
            palette.append(entry)
        x, z, y = i % W, (i // W) % L, i // (W * L)
        b = {'pos': (9, (3, [x, y, z])), 'state': (3, index[name])}
        if (x, y, z) in entity_nbt:
            b['nbt'] = (10, entity_nbt[(x, y, z)])
        out_blocks.append(b)

    return {
        'DataVersion': (3, r['DataVersion'][1]),
        'size': (9, (3, [W, H, L])),
        'palette': (9, (10, palette)),
        'blocks': (9, (10, out_blocks)),
        'entities': (9, (10, [])),
    }


if __name__ == '__main__':
    src, dst = sys.argv[1], sys.argv[2]
    os.makedirs(dst, exist_ok=True)
    for f in sorted(os.listdir(src)):
        if f.endswith('.schem'):
            data = write_nbt(convert(os.path.join(src, f)))
            # Comprobacion: lo escrito se vuelve a leer igual
            assert read_nbt(gzip.decompress(data))['size'][1][1]
            out = os.path.join(dst, f[:-6] + '.nbt')
            open(out, 'wb').write(data)
            print(out, len(data), 'bytes')
