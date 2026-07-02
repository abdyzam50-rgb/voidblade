from PIL import Image
from pathlib import Path

sheet_path = Path(
    r"C:\Users\abdy2\.cursor\projects\c-Users-abdy2-Downloads-fabric-example-mod-1-21-11"
    r"\assets\c__Users_abdy2_AppData_Roaming_Cursor_User_workspaceStorage_7eb3ec3ec63f92f0650c7ac8772cd6f0"
    r"_images_pixilart-sprite__2_-107af777-ae4f-482b-92bf-eeda4221fbeb.png"
)
out_dir = Path(
    r"c:\Users\abdy2\Downloads\fabric-example-mod-1.21.11\fabric-example-mod-1.21.11"
    r"\src\main\resources\assets\modid\textures\item"
)
out_dir.mkdir(parents=True, exist_ok=True)

img = Image.open(sheet_path).convert("RGBA")
print("sheet", img.size)

if img.size == (64, 32):
    base = img.crop((0, 0, 32, 32))
    custom = img.crop((32, 0, 64, 32))
elif img.size == (32, 64):
    base = img.crop((0, 0, 32, 32))
    custom = img.crop((0, 32, 32, 64))
else:
    raise SystemExit(f"unexpected sheet size {img.size}")

custom.save(out_dir / "portal_spear_hilt_in_hand.png")
custom.resize((16, 16), Image.NEAREST).save(out_dir / "portal_spear_hilt.png")
print("wrote portal_spear_hilt_in_hand.png (32x32)")
print("wrote portal_spear_hilt.png (16x16)")

size = 32
opaque = [(c, r) for r in range(size) for c in range(size) if base.getpixel((c, r))[3] > 10]
by_row = {}
for c, r in opaque:
    by_row.setdefault(r, []).append(c)

head_rows = []
for r in range(size):
    cols = sorted(by_row.get(r, []))
    if len(cols) <= 2:
        continue
    head_rows.append((r, cols[0], cols[-1] + 1))

# Shaft is thin (1-2 px wide); head rows are wider.
print("base head rows (row, col_start, col_end):")
for row in head_rows:
    if row[0] > 12:
        break
    print(row)
