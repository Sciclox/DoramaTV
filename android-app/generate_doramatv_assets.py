import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def draw_linear_gradient(draw_image, rect, color1, color2, vertical=True):
    x0, y0, x1, y1 = rect
    w = max(1, x1 - x0)
    h = max(1, y1 - y0)
    gradient = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(gradient)
    steps = h if vertical else w
    for i in range(steps):
        t = i / float(steps - 1) if steps > 1 else 0
        r = int(color1[0] * (1 - t) + color2[0] * t)
        g = int(color1[1] * (1 - t) + color2[1] * t)
        b = int(color1[2] * (1 - t) + color2[2] * t)
        a = int(color1[3] * (1 - t) + color2[3] * t)
        if vertical:
            g_draw.line([(0, i), (w, i)], fill=(r, g, b, a))
        else:
            g_draw.line([(i, 0), (i, h)], fill=(r, g, b, a))
    draw_image.paste(gradient, (x0, y0), gradient)

def create_tv_banner(width=640, height=360, bebas_path="", pjs_path="", inter_path=""):
    img = Image.new('RGBA', (width, height), (16, 11, 24, 255))
    
    # 1. Vibrant cinematic atmospheric glow (Magenta + Violet)
    glow = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    
    cx, cy = width // 2, int(height * 0.44)
    # Magenta center glow
    rx, ry = int(width * 0.40), int(height * 0.38)
    glow_draw.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=(255, 46, 99, 90))
    
    # Violet secondary ambient glow
    rx2, ry2 = int(width * 0.52), int(height * 0.44)
    glow_draw.ellipse([cx - rx2, cy - ry2 + 15, cx + rx2, cy + ry2 + 15], fill=(138, 43, 226, 55))
    
    glow = glow.filter(ImageFilter.GaussianBlur(radius=int(height * 0.16)))
    img = Image.alpha_composite(img, glow)
    
    # Subtle inner border for TV launcher card
    border = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    b_draw = ImageDraw.Draw(border)
    b_draw.rectangle([0, 0, width - 1, height - 1], outline=(255, 117, 151, 35), width=2)
    img = Image.alpha_composite(img, border)
    
    # Typography & Badges
    font_dorama_size = int(height * 0.29)
    font_tv_size = int(height * 0.22)
    font_sub_size = int(height * 0.046)
    
    font_dorama = ImageFont.truetype(bebas_path, font_dorama_size)
    font_tv = ImageFont.truetype(bebas_path, font_tv_size)
    font_sub = ImageFont.truetype(inter_path, font_sub_size)
    
    # Measure "DORAMA"
    bbox_dorama = font_dorama.getbbox("DORAMA")
    dorama_w = bbox_dorama[2] - bbox_dorama[0]
    dorama_h = bbox_dorama[3] - bbox_dorama[1]
    
    # Measure "TV" badge
    bbox_tv = font_tv.getbbox("TV")
    tv_w = bbox_tv[2] - bbox_tv[0]
    tv_h = bbox_tv[3] - bbox_tv[1]
    
    pad_h = int(height * 0.055)
    pad_v = int(height * 0.025)
    badge_w = tv_w + pad_h * 2
    badge_h = tv_h + pad_v * 2
    
    spacing = int(height * 0.038)
    total_brand_w = dorama_w + spacing + badge_w
    brand_start_x = (width - total_brand_w) // 2
    brand_center_y = int(height * 0.40)
    
    # Draw "DORAMA" with soft shadow
    dorama_x = brand_start_x - bbox_dorama[0]
    dorama_y = brand_center_y - (dorama_h // 2) - bbox_dorama[1]
    
    shadow_img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    ImageDraw.Draw(shadow_img).text((dorama_x + 2, dorama_y + 4), "DORAMA", font=font_dorama, fill=(0, 0, 0, 180))
    img = Image.alpha_composite(img, shadow_img)
    
    # Draw "DORAMA" in crisp warm cream (#FFF0F5)
    ImageDraw.Draw(img).text((dorama_x, dorama_y), "DORAMA", font=font_dorama, fill=(255, 240, 245, 255))
    
    # Draw "TV" Badge
    badge_x0 = brand_start_x + dorama_w + spacing
    badge_y0 = brand_center_y - (badge_h // 2)
    
    badge_img = Image.new('RGBA', (badge_w, badge_h), (0, 0, 0, 0))
    corner_radius = int(height * 0.045)
    draw_linear_gradient(badge_img, (0, 0, badge_w, badge_h), (255, 46, 99, 255), (255, 117, 151, 255), vertical=True)
    
    mask = Image.new('L', (badge_w, badge_h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, badge_w, badge_h], radius=corner_radius, fill=255)
    badge_img.putalpha(mask)
    
    border_img = Image.new('RGBA', (badge_w, badge_h), (0, 0, 0, 0))
    ImageDraw.Draw(border_img).rounded_rectangle([0, 0, badge_w - 1, badge_h - 1], radius=corner_radius, outline=(255, 255, 255, 120), width=2)
    badge_img = Image.alpha_composite(badge_img, border_img)
    
    tv_text_x = (badge_w - tv_w) // 2 - bbox_tv[0]
    tv_text_y = (badge_h - tv_h) // 2 - bbox_tv[1]
    ImageDraw.Draw(badge_img).text((tv_text_x, tv_text_y), "TV", font=font_tv, fill=(255, 255, 255, 255))
    img.paste(badge_img, (badge_x0, badge_y0), badge_img)
    
    # Subtitle
    sub_text = "D O R A M A S   Y   P E L Í C U L A S   A S I Á T I C A S"
    bbox_sub = font_sub.getbbox(sub_text)
    sub_w = bbox_sub[2] - bbox_sub[0]
    sub_h = bbox_sub[3] - bbox_sub[1]
    
    sub_y = badge_y0 + badge_h + int(height * 0.09)
    sub_x = (width - sub_w) // 2
    
    line_w = int(width * 0.075)
    line_y = sub_y + sub_h // 2 + 1
    draw = ImageDraw.Draw(img)
    draw.line([(sub_x - int(width * 0.025) - line_w, line_y), (sub_x - int(width * 0.025), line_y)], fill=(255, 117, 151, 80), width=1)
    draw.line([(sub_x + sub_w + int(width * 0.025), line_y), (sub_x + sub_w + int(width * 0.025) + line_w, line_y)], fill=(255, 117, 151, 80), width=1)
    draw.text((sub_x, sub_y), sub_text, font=font_sub, fill=(200, 184, 202, 230))
    
    # Glowing Accent Bar
    bar_w = int(width * 0.36)
    bar_h = 3
    bar_y = sub_y + sub_h + int(height * 0.065)
    bar_x0 = (width - bar_w) // 2
    draw_linear_gradient(img, (bar_x0, bar_y, bar_x0 + bar_w, bar_y + bar_h), (255, 46, 99, 220), (255, 141, 161, 120), vertical=False)
    
    return img

def create_tv_icon(size=512, bebas_path="", pjs_path="", inter_path="", round_icon=False, for_foreground=False):
    width, height = size, size
    
    if for_foreground:
        img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    else:
        img = Image.new('RGBA', (width, height), (16, 11, 24, 255))
        
    scale = 0.72 if for_foreground else 1.0
    
    # 1. Central Ambient Glow
    glow = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    cx, cy = width // 2, int(height * 0.44)
    rx, ry = int(width * 0.42 * scale), int(height * 0.38 * scale)
    glow_draw.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=(255, 46, 99, 100))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=int(height * 0.16 * scale)))
    img = Image.alpha_composite(img, glow)
    
    # 2. TV Screen Emblem
    # Let's make the TV screen larger, sleek, and centered
    screen_w = int(width * 0.50 * scale)
    screen_h = int(height * 0.36 * scale)
    screen_x0 = (width - screen_w) // 2
    screen_y0 = int(height * 0.18 * scale) if not for_foreground else int(height * 0.20)
    
    screen_img = Image.new('RGBA', (screen_w, screen_h), (0, 0, 0, 0))
    corner_radius = max(6, int(18 * scale))
    draw_linear_gradient(screen_img, (0, 0, screen_w, screen_h), (255, 46, 99, 255), (138, 43, 226, 255), vertical=True)
    
    # Rounded mask for TV screen
    s_mask = Image.new('L', (screen_w, screen_h), 0)
    ImageDraw.Draw(s_mask).rounded_rectangle([0, 0, screen_w, screen_h], radius=corner_radius, fill=255)
    screen_img.putalpha(s_mask)
    
    # Screen luminous neon border
    s_border = Image.new('RGBA', (screen_w, screen_h), (0, 0, 0, 0))
    ImageDraw.Draw(s_border).rounded_rectangle([0, 0, screen_w - 1, screen_h - 1], radius=corner_radius, outline=(255, 255, 255, 170), width=max(2, int(2.5 * scale)))
    screen_img = Image.alpha_composite(screen_img, s_border)
    
    # Big Play Arrow inside TV Screen
    tri_draw = ImageDraw.Draw(screen_img)
    tcx, tcy = screen_w // 2, screen_h // 2
    ts = int(24 * scale)
    tri_points = [
        (tcx - int(ts * 0.6) + int(3 * scale), tcy - ts),
        (tcx + int(ts * 0.9), tcy),
        (tcx - int(ts * 0.6) + int(3 * scale), tcy + ts)
    ]
    # Triangle drop shadow
    tri_shadow = [
        (p[0] + 1, p[1] + 2) for p in tri_points
    ]
    tri_draw.polygon(tri_shadow, fill=(0, 0, 0, 90))
    tri_draw.polygon(tri_points, fill=(255, 255, 255, 255))
    
    # Paste TV Screen into main image
    img.paste(screen_img, (screen_x0, screen_y0), screen_img)
    
    # Small TV stand base
    stand_w = int(screen_w * 0.26)
    stand_h = max(2, int(4 * scale))
    stand_x = (width - stand_w) // 2
    stand_y = screen_y0 + screen_h + max(2, int(4 * scale))
    draw_linear_gradient(img, (stand_x, stand_y, stand_x + stand_w, stand_y + stand_h), (255, 117, 151, 230), (200, 184, 202, 140), vertical=False)
    
    # 3. Typography "DORAMA TV"
    font_dorama_size = int(height * 0.20 * scale)
    font_tv_size = int(height * 0.15 * scale)
    
    font_dorama = ImageFont.truetype(bebas_path, font_dorama_size)
    font_tv = ImageFont.truetype(bebas_path, font_tv_size)
    
    bbox_dorama = font_dorama.getbbox("DORAMA")
    dorama_w = bbox_dorama[2] - bbox_dorama[0]
    dorama_h = bbox_dorama[3] - bbox_dorama[1]
    
    bbox_tv = font_tv.getbbox("TV")
    tv_w = bbox_tv[2] - bbox_tv[0]
    tv_h = bbox_tv[3] - bbox_tv[1]
    
    pad_h = int(height * 0.035 * scale)
    pad_v = int(height * 0.016 * scale)
    badge_w = tv_w + pad_h * 2
    badge_h = tv_h + pad_v * 2
    
    spacing = int(height * 0.025 * scale)
    total_w = dorama_w + spacing + badge_w
    start_x = (width - total_w) // 2
    brand_center_y = stand_y + stand_h + int(height * 0.09 * scale)
    
    dorama_x = start_x - bbox_dorama[0]
    dorama_y = brand_center_y - (dorama_h // 2) - bbox_dorama[1]
    
    # Drop shadow
    shadow_img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    ImageDraw.Draw(shadow_img).text((dorama_x + 2, dorama_y + 3), "DORAMA", font=font_dorama, fill=(0, 0, 0, 190))
    img = Image.alpha_composite(img, shadow_img)
    
    # Draw "DORAMA"
    ImageDraw.Draw(img).text((dorama_x, dorama_y), "DORAMA", font=font_dorama, fill=(255, 240, 245, 255))
    
    # TV Badge
    badge_x0 = start_x + dorama_w + spacing
    badge_y0 = brand_center_y - (badge_h // 2)
    badge_img = Image.new('RGBA', (badge_w, badge_h), (0, 0, 0, 0))
    b_radius = max(3, int(9 * scale))
    draw_linear_gradient(badge_img, (0, 0, badge_w, badge_h), (255, 46, 99, 255), (255, 117, 151, 255), vertical=True)
    b_mask = Image.new('L', (badge_w, badge_h), 0)
    ImageDraw.Draw(b_mask).rounded_rectangle([0, 0, badge_w, badge_h], radius=b_radius, fill=255)
    badge_img.putalpha(b_mask)
    
    b_border = Image.new('RGBA', (badge_w, badge_h), (0, 0, 0, 0))
    ImageDraw.Draw(b_border).rounded_rectangle([0, 0, badge_w - 1, badge_h - 1], radius=b_radius, outline=(255, 255, 255, 140), width=max(1, int(1.5 * scale)))
    badge_img = Image.alpha_composite(badge_img, b_border)
    
    tv_tx = (badge_w - tv_w) // 2 - bbox_tv[0]
    tv_ty = (badge_h - tv_h) // 2 - bbox_tv[1]
    ImageDraw.Draw(badge_img).text((tv_tx, tv_ty), "TV", font=font_tv, fill=(255, 255, 255, 255))
    img.paste(badge_img, (badge_x0, badge_y0), badge_img)
    
    # Glowing accent bar at bottom
    bar_w = int(total_w * 0.95)
    bar_h = max(2, int(3 * scale))
    bar_y = brand_center_y + (dorama_h // 2) + int(height * 0.04 * scale)
    bar_x0 = (width - bar_w) // 2
    draw_linear_gradient(img, (bar_x0, bar_y, bar_x0 + bar_w, bar_y + bar_h), (255, 46, 99, 230), (255, 141, 161, 140), vertical=False)
    
    if round_icon and not for_foreground:
        circle_mask = Image.new('L', (width, height), 0)
        ImageDraw.Draw(circle_mask).ellipse([0, 0, width, height], fill=255)
        border = Image.new('RGBA', (width, height), (0, 0, 0, 0))
        ImageDraw.Draw(border).ellipse([1, 1, width - 2, height - 2], outline=(255, 117, 151, 80), width=max(1, int(2 * scale)))
        img.putalpha(circle_mask)
        img = Image.alpha_composite(img, border)
        
    return img

def main():
    root = r"c:\Users\Lenovo\OneDrive\Documentos\Proyectos\APK-DoramaTV\android-app\app\src\main\res"
    bebas = os.path.join(root, "font", "bebas_neue.ttf")
    pjs = os.path.join(root, "font", "plus_jakarta_sans.ttf")
    inter = os.path.join(root, "font", "inter.ttf")
    
    artifact_dir = r"C:\Users\Lenovo\.gemini\antigravity\brain\5a441a75-7d86-48aa-b59d-e7979f2b0e62"
    
    # 1. Base Master Images (High Res)
    banner_high = create_tv_banner(960, 540, bebas, pjs, inter)
    banner_med = create_tv_banner(640, 360, bebas, pjs, inter)
    banner_std = create_tv_banner(320, 180, bebas, pjs, inter)
    
    icon_master = create_tv_icon(512, bebas, pjs, inter, round_icon=False)
    icon_round_master = create_tv_icon(512, bebas, pjs, inter, round_icon=True)
    fg_master = create_tv_icon(512, bebas, pjs, inter, for_foreground=True)
    
    # Save preview in artifacts
    banner_med.save(os.path.join(artifact_dir, "preview_doramatv_banner.png"), "PNG")
    icon_master.save(os.path.join(artifact_dir, "preview_doramatv_icon.png"), "PNG")
    icon_round_master.save(os.path.join(artifact_dir, "preview_doramatv_icon_round.png"), "PNG")
    fg_master.save(os.path.join(artifact_dir, "preview_doramatv_fg.png"), "PNG")
    
    # 2. Save Banners in res (TV)
    os.makedirs(os.path.join(root, "drawable"), exist_ok=True)
    os.makedirs(os.path.join(root, "drawable-xhdpi"), exist_ok=True)
    os.makedirs(os.path.join(root, "drawable-xxhdpi"), exist_ok=True)
    os.makedirs(os.path.join(root, "drawable-xxxhdpi"), exist_ok=True)
    
    banner_med.save(os.path.join(root, "drawable", "ic_banner.png"), "PNG")
    banner_std.save(os.path.join(root, "drawable-xhdpi", "ic_banner.png"), "PNG")
    banner_med.save(os.path.join(root, "drawable-xxhdpi", "ic_banner.png"), "PNG")
    banner_high.save(os.path.join(root, "drawable-xxxhdpi", "ic_banner.png"), "PNG")
    
    # Target res directories for TV and Mobile
    mobile_root = r"c:\Users\Lenovo\OneDrive\Documentos\Proyectos\APK-DoramaTV\android-app\app-mobile\src\main\res"
    targets = [root, mobile_root]
    
    for res_dir in targets:
        # Save standard drawable/ic_launcher.png (fallback)
        os.makedirs(os.path.join(res_dir, "drawable"), exist_ok=True)
        icon_master.save(os.path.join(res_dir, "drawable", "ic_launcher.png"), "PNG")
        
        # 3. Save Mipmaps
        densities = {
            "mipmap-mdpi": (48, 108),
            "mipmap-hdpi": (72, 162),
            "mipmap-xhdpi": (96, 216),
            "mipmap-xxhdpi": (144, 324),
            "mipmap-xxxhdpi": (192, 432),
        }
        
        for folder, (icon_sz, fg_sz) in densities.items():
            folder_path = os.path.join(res_dir, folder)
            os.makedirs(folder_path, exist_ok=True)
            
            # ic_launcher.png
            icon_res = icon_master.resize((icon_sz, icon_sz), Image.Resampling.LANCZOS)
            icon_res.save(os.path.join(folder_path, "ic_launcher.png"), "PNG")
            
            # ic_launcher_round.png
            icon_round_res = icon_round_master.resize((icon_sz, icon_sz), Image.Resampling.LANCZOS)
            icon_round_res.save(os.path.join(folder_path, "ic_launcher_round.png"), "PNG")
            
            # ic_launcher_foreground.png
            fg_res = fg_master.resize((fg_sz, fg_sz), Image.Resampling.LANCZOS)
            fg_res.save(os.path.join(folder_path, "ic_launcher_foreground.png"), "PNG")
            
    print("All refined DoramaTV image assets created successfully for TV and Mobile!")

if __name__ == "__main__":
    main()
