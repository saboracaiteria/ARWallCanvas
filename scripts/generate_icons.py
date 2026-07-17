#!/usr/bin/env python3
"""
Gera ícones PNG placeholder para o ARWallCanvas.
Usa apenas a biblioteca padrão (struct + zlib).
"""

import struct, zlib, os

def create_png(w, h, r, g, b):
    sig = b'\x89PNG\r\n\x1a\n'
    ihdr_data = struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0)
    ihdr = struct.pack('>I', 13) + b'IHDR' + ihdr_data + struct.pack('>I', zlib.crc32(b'IHDR' + ihdr_data) & 0xFFFFFFFF)
    raw = b''
    cx, cy = w/2, h/2
    rad = min(w, h)/2 - 2
    for y in range(h):
        raw += b'\x00'
        for x in range(w):
            d = ((x-cx)**2 + (y-cy)**2)**0.5
            raw += (struct.pack('BBB', r, g, b) if d < rad else
                    struct.pack('BBB', 255, 255, 255) if d < rad + 1.5 else
                    struct.pack('BBB', 200, 200, 200))
    comp = zlib.compress(raw)
    idat = struct.pack('>I', len(comp)) + b'IDAT' + comp + struct.pack('>I', zlib.crc32(b'IDAT' + comp) & 0xFFFFFFFF)
    iend = struct.pack('>I', 0) + b'IEND' + struct.pack('>I', zlib.crc32(b'IEND') & 0xFFFFFFFF)
    return sig + ihdr + idat + iend

def main():
    sizes = {'mipmap-mdpi': 48, 'mipmap-hdpi': 72, 'mipmap-xhdpi': 96,
             'mipmap-xxhdpi': 144, 'mipmap-xxxhdpi': 192}
    base = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'app', 'src', 'main', 'res')
    for folder, size in sizes.items():
        p = os.path.join(base, folder)
        os.makedirs(p, exist_ok=True)
        for name in ('ic_launcher.png', 'ic_launcher_round.png'):
            with open(os.path.join(p, name), 'wb') as f:
                f.write(create_png(size, size, 33, 150, 243))
            print(f'  OK  {folder}/{name} ({size}x{size})')

if __name__ == '__main__':
    main()
