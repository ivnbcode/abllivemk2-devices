# Compilador de Max for Live Audio Effect (.amxd) + Paquete ZIP para SPATIAL-LAB 3D MkII

import os
import struct
import zipfile

script_dir = os.path.dirname(os.path.abspath(__file__))
maxpat_path = os.path.join(script_dir, "SPATIAL-LAB_3D_MkII.maxpat")
amxd_path = os.path.join(script_dir, "SPATIAL-LAB_3D_MkII.amxd")
zip_path = os.path.join(script_dir, "SPATIAL-LAB_3D_MkII_Ableton_M4L.zip")

with open(maxpat_path, "rb") as f:
    json_bytes = f.read()

# Tipo 'aaaa' = Max for Live Audio Effect
payload = json_bytes + b"\x00"
header = b""
header += b"mmmm" + struct.pack("<II", 4, 1)
header += b"aaaa" + struct.pack("<I", 4) + b"aaaa"
header += b"meta" + struct.pack("<II", 4, 0)
header += b"ptch" + struct.pack("<I", len(payload))

with open(amxd_path, "wb") as out:
    out.write(header + payload)

print(f"Generado: {amxd_path} ({os.path.getsize(amxd_path)} bytes)")

files_to_pack = ["SPATIAL-LAB_3D_MkII.amxd", "spatiallab_m4l.js", "SPATIAL-LAB_3D_MkII.maxpat", "README.md"]
with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
    for fname in files_to_pack:
        fpath = os.path.join(script_dir, fname)
        if os.path.exists(fpath):
            zf.write(fpath, arcname=f"SPATIAL-LAB_3D_MkII/{fname}")

print(f"Paquete ZIP generado: {zip_path} ({os.path.getsize(zip_path)} bytes)")
