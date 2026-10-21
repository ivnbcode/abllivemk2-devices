# Compilador de Max for Live MIDI Effect + Instrumento Sonic Strings (.amxd)
# Genera:
# 1. OMNI-LAB_MkII_MIDI.amxd (Efecto MIDI para poner delante de cualquier sintetizador de Ableton o VST)
# 2. OMNI-LAB_MkII_Synth.amxd (Instrumento Max for Live con motor de síntesis Sonic Strings + Chorus integrado)

import os
import json
import struct

script_dir = os.path.dirname(os.path.abspath(__file__))
maxpat_path = os.path.join(script_dir, "OMNI-LAB_MkII.maxpat")
midi_amxd_path = os.path.join(script_dir, "OMNI-LAB_MkII.amxd")

with open(maxpat_path, "rb") as f:
    midi_json_bytes = f.read()

def write_amxd(path, device_type_code, patch_bytes):
    payload = patch_bytes + b"\x00"
    header = b""
    header += b"mmmm" + struct.pack("<II", 4, 1)
    header += b"aaaa" + struct.pack("<I", 4) + device_type_code
    header += b"meta" + struct.pack("<II", 4, 0)
    header += b"ptch" + struct.pack("<I", len(payload))
    with open(path, "wb") as out:
        out.write(header + payload)
    print(f"Generado: {path} ({os.path.getsize(path)} bytes)")

# 1. Guardar Efecto MIDI ('mddf')
write_amxd(midi_amxd_path, b"mddf", midi_json_bytes)

# 2. Empaquetar ZIP listo para descargar desde GitHub Pages (.amxd + .js + .maxpat + README)
import zipfile

zip_path = os.path.join(script_dir, "OMNI-LAB_MkII_Ableton_M4L.zip")
files_to_pack = ["OMNI-LAB_MkII.amxd", "omnilab_m4l.js", "OMNI-LAB_MkII.maxpat", "README.md"]

with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
    for fname in files_to_pack:
        fpath = os.path.join(script_dir, fname)
        if os.path.exists(fpath):
            zf.write(fpath, arcname=f"OMNI-LAB_MkII/{fname}")

print(f"Paquete ZIP generado: {zip_path} ({os.path.getsize(zip_path)} bytes)")

