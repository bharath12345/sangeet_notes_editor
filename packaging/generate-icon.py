#!/usr/bin/env python3
"""Generate a sitar-themed app icon for Sangeet Notes Editor."""

from PIL import Image, ImageDraw, ImageFont
import math
import os

def draw_sitar_icon(size=1024):
    """Draw a stylized sitar icon with musical notation elements."""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    cx, cy = size // 2, size // 2
    s = size / 1024.0  # scale factor

    # Background circle — warm saffron/amber gradient feel
    # Outer ring
    draw.ellipse([int(20*s), int(20*s), int(1004*s), int(1004*s)],
                 fill=(180, 60, 20), outline=(120, 40, 10), width=int(8*s))
    # Inner circle — warm cream/parchment
    draw.ellipse([int(60*s), int(60*s), int(964*s), int(964*s)],
                 fill=(255, 240, 210))

    # Sitar body — tumba (gourd) at bottom
    tumba_cx = int(512*s)
    tumba_cy = int(720*s)
    tumba_rx = int(200*s)
    tumba_ry = int(160*s)
    draw.ellipse([tumba_cx - tumba_rx, tumba_cy - tumba_ry,
                  tumba_cx + tumba_rx, tumba_cy + tumba_ry],
                 fill=(160, 90, 30), outline=(100, 55, 15), width=int(4*s))

    # Decorative ring on tumba
    draw.ellipse([tumba_cx - int(140*s), tumba_cy - int(100*s),
                  tumba_cx + int(140*s), tumba_cy + int(100*s)],
                 outline=(200, 150, 60), width=int(3*s))

    # Sitar neck (dand) — long diagonal
    neck_top_x = int(380*s)
    neck_top_y = int(160*s)
    neck_bot_x = int(512*s)
    neck_bot_y = int(600*s)
    neck_width = int(35*s)

    # Draw neck as polygon
    dx = int(neck_width * 0.5)
    draw.polygon([
        (neck_top_x - dx, neck_top_y),
        (neck_top_x + dx, neck_top_y),
        (neck_bot_x + dx + int(5*s), neck_bot_y),
        (neck_bot_x - dx - int(5*s), neck_bot_y),
    ], fill=(140, 80, 25), outline=(100, 55, 15), width=int(2*s))

    # Frets on neck
    for i in range(8):
        t = (i + 1) / 9.0
        fx = int(neck_top_x + (neck_bot_x - neck_top_x) * t)
        fy = int(neck_top_y + (neck_bot_y - neck_top_y) * t)
        fw = int((neck_width * 0.5) + t * 8 * s)
        draw.line([(fx - fw, fy), (fx + fw, fy)],
                  fill=(200, 170, 100), width=int(3*s))

    # Strings on neck
    for offset in [-int(8*s), 0, int(8*s)]:
        draw.line([
            (neck_top_x + offset, neck_top_y),
            (neck_bot_x + offset, neck_bot_y + int(80*s))
        ], fill=(220, 200, 150), width=int(2*s))

    # Tuning pegs at top
    for i in range(4):
        peg_x = neck_top_x - int(30*s) + i * int(20*s)
        peg_y = neck_top_y - int(20*s) + (i % 2) * int(15*s)
        draw.ellipse([peg_x - int(8*s), peg_y - int(8*s),
                      peg_x + int(8*s), peg_y + int(8*s)],
                     fill=(100, 55, 15), outline=(60, 30, 5), width=int(2*s))

    # Bridge on tumba
    draw.rectangle([tumba_cx - int(60*s), tumba_cy - int(15*s),
                    tumba_cx + int(60*s), tumba_cy + int(5*s)],
                   fill=(240, 220, 170), outline=(180, 150, 80), width=int(2*s))

    # Musical note — Devanagari "सा" (Sa) on the right side
    try:
        # Try to find a Devanagari font
        font_paths = [
            "/System/Library/Fonts/Supplemental/Devanagari MT.ttc",
            "/Library/Fonts/NotoSansDevanagari-Regular.ttf",
            "/System/Library/Fonts/Kohinoor.ttc",
        ]
        font = None
        for fp in font_paths:
            if os.path.exists(fp):
                font = ImageFont.truetype(fp, int(140*s))
                break
        if font:
            draw.text((int(620*s), int(200*s)), "सा", fill=(180, 60, 20), font=font)
    except Exception:
        pass

    # Small staff lines (notation reference) — bottom right
    staff_x = int(620*s)
    staff_y = int(420*s)
    for i in range(3):
        y = staff_y + i * int(20*s)
        draw.line([(staff_x, y), (staff_x + int(180*s), y)],
                  fill=(180, 60, 20, 150), width=int(2*s))

    # Small note dots on staff
    for i, x_off in enumerate([30, 80, 130, 160]):
        dot_x = staff_x + int(x_off * s)
        dot_y = staff_y + int((i % 3) * 20 * s)
        r = int(6*s)
        draw.ellipse([dot_x - r, dot_y - r, dot_x + r, dot_y + r],
                     fill=(180, 60, 20))

    return img


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    icons_dir = os.path.join(script_dir, "icons")
    os.makedirs(icons_dir, exist_ok=True)

    print("Generating sitar icon...")
    icon = draw_sitar_icon(1024)

    # Save master PNG
    master_path = os.path.join(icons_dir, "sangeet-icon-1024.png")
    icon.save(master_path, "PNG")
    print(f"  Saved: {master_path}")

    # Generate sizes needed for various platforms
    sizes = [16, 32, 48, 64, 128, 256, 512, 1024]
    for sz in sizes:
        resized = icon.resize((sz, sz), Image.LANCZOS)
        path = os.path.join(icons_dir, f"sangeet-icon-{sz}.png")
        resized.save(path, "PNG")
        print(f"  Saved: {path} ({sz}x{sz})")

    # Generate .ico for Windows (multi-size)
    ico_sizes = [16, 32, 48, 64, 128, 256]
    ico_images = [icon.resize((sz, sz), Image.LANCZOS) for sz in ico_sizes]
    ico_path = os.path.join(icons_dir, "sangeet-icon.ico")
    ico_images[0].save(ico_path, format='ICO', sizes=[(sz, sz) for sz in ico_sizes],
                       append_images=ico_images[1:])
    print(f"  Saved: {ico_path} (Windows ICO)")

    # Generate .icns for macOS using iconutil
    iconset_dir = os.path.join(icons_dir, "sangeet-icon.iconset")
    os.makedirs(iconset_dir, exist_ok=True)

    icns_sizes = {
        "icon_16x16.png": 16,
        "icon_16x16@2x.png": 32,
        "icon_32x32.png": 32,
        "icon_32x32@2x.png": 64,
        "icon_128x128.png": 128,
        "icon_128x128@2x.png": 256,
        "icon_256x256.png": 256,
        "icon_256x256@2x.png": 512,
        "icon_512x512.png": 512,
        "icon_512x512@2x.png": 1024,
    }
    for name, sz in icns_sizes.items():
        resized = icon.resize((sz, sz), Image.LANCZOS)
        resized.save(os.path.join(iconset_dir, name), "PNG")

    os.system(f"iconutil -c icns '{iconset_dir}' -o '{os.path.join(icons_dir, 'sangeet-icon.icns')}'")
    print(f"  Saved: {os.path.join(icons_dir, 'sangeet-icon.icns')} (macOS ICNS)")

    # Clean up iconset directory
    import shutil
    shutil.rmtree(iconset_dir)

    print("\nDone! Icons ready for packaging.")


if __name__ == "__main__":
    main()
