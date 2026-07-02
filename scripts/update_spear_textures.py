from PIL import Image
from pathlib import Path

out_dir = Path(
    r"c:\Users\abdy2\Downloads\fabric-example-mod-1.21.11\fabric-example-mod-1.21.11"
    r"\src\main\resources\assets\modid\textures\item"
)
inv_src = Path(
    r"C:\Users\abdy2\.cursor\projects\c-Users-abdy2-Downloads-fabric-example-mod-1-21-11"
    r"\assets\c__Users_abdy2_AppData_Roaming_Cursor_User_workspaceStorage_empty-window_images"
    r"_pixil-frame-0__18_-90fb194f-9168-4c55-8226-03a5d84f0d14.png"
)
hand_src = Path(
    r"C:\Users\abdy2\.cursor\projects\c-Users-abdy2-Downloads-fabric-example-mod-1-21-11"
    r"\assets\c__Users_abdy2_AppData_Roaming_Cursor_User_workspaceStorage_empty-window_images"
    r"_pixil-frame-1-849af77c-2197-4f64-87fc-42a4306ebad6.png"
)

inv = Image.open(inv_src).convert("RGBA")
hand = Image.open(hand_src).convert("RGBA")
print("inventory", inv.size, "in_hand", hand.size)

inv.save(out_dir / "portal_spear_hilt.png")
hand.save(out_dir / "portal_spear_hilt_in_hand.png")
print("saved textures")
