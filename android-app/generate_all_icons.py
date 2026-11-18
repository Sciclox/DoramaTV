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

def create_tv_banner(width=640, height=360, bebas_path="", inter_path=""):
    img = Image.new('RGBA', (width, height), (6, 8, 12, 255))
    
    # Atmospheric red & blue glow
    glow = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    cx, cy = width // 2, int(height * 0.44)
    rx, ry = int(width * 0.36), int(height * 0.32)
    glow_draw.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=(229, 9, 20, 80))
    
    rx_b, ry_b = int(width * 0.46), int(height * 0.40)
    glow_draw.ellipse([cx - rx_b, cy - ry_b + 18, cx + rx_b, cy + ry_b + 18], fill=(30, 36, 51, 60))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=int(height * 0.15)))
    img = Image.alpha_composite(img, glow)
    
    # Fonts
    font_tv_size = int(height * 0.26)
    font_pro_size = int(height * 0.32)
    font_sub_size = int(height * 0.046)
    
    font_tv = ImageFont.truetype(bebas_path, font_tv_size)
    font_pro = ImageFont.truetype(bebas_path, font_pro_size)
    font_sub = ImageFont.truetype(inter_path, font_sub_size)
    
    bbox_tv = font_tv.getbbox("TV")
    tv_w = bbox_tv[2] - bbox_tv[0]
    tv_h = bbox_tv[3] - bbox_tv[1]
    
    pad_h = int(height * 0.055)
    pad_v = int(height * 0.024)
    badge_w = tv_w + pad_h * 2
    badge_h = tv_h + pad_v * 2
    
    bbox_pro = font_pro.getbbox("PRO")
    pro_w = bbox_pro[2] - bbox_pro[0]
    pro_h = bbox_pro[3] - bbox_pro[1]
    
    spacing = int(height * 0.048)
    total_brand_w = badge_w + spacing + pro_w
    
    brand_start_x = (width - total_brand_w) // 2
    brand_center_y = int(height * 0.41)
    
    badge_x0 = brand_start_x
    badge_y0 = brand_center_y - (badge_h // 2)
    
    # Badge with gradient and border
    badge_img = Image.new('RGBA', (badge_w, badge_h), (0, 0, 0, 0))
    corner_radius = int(height * 0.045)
    draw_linear_gradient(badge_img, (0, 0, badge_w, badge_h), (229, 9, 20, 255), (139, 0, 0, 255), vertical=True)
    
    mask = Image.new('L', (badge_w, badge_h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, badge_w, badge_h], radius=corner_radius, fill=255)
    badge_img.putalpha(mask)
    
    border_img = Image.new('RGBA', (badge_w, badge_h), (0, 0, 0, 0))
    ImageDraw.Draw(border_img).rounded_rectangle([0, 0, badge_w - 1, badge_h - 1], radius=corner_radius, outline=(255, 255, 255, 90), width=2)
    badge_img = Image.alpha_composite(badge_img, border_img)
    
    tv_text_x = (badge_w - tv_w) // 2 - bbox_tv[0]
    tv_text_y = (badge_h - tv_h) // 2 - bbox_tv[1]
    ImageDraw.Draw(badge_img).text((tv_text_x, tv_text_y), "TV", font=font_tv, fill=(255, 255, 255, 255))
    img.paste(badge_img, (badge_x0, badge_y0), badge_img)
    
    # "PRO" text
    pro_x = badge_x0 + badge_w + spacing - bbox_pro[0]
    pro_y = brand_center_y - (pro_h // 2) - bbox_pro[1]
    ImageDraw.Draw(img).text((pro_x, pro_y), "PRO", font=font_pro, fill=(245, 242, 235, 255))
    
    # Subtitle: "EXPERIENCIA CINEMATOGRÁFICA"
    sub_text = "E X P E R I E N C I A   C I N E M A T O G R Á F I C A"
    bbox_sub = font_sub.getbbox(sub_text)
    sub_w = bbox_sub[2] - bbox_sub[0]
    sub_h = bbox_sub[3] - bbox_sub[1]
    
    sub_y = badge_y0 + badge_h + int(height * 0.085)
    sub_x = (width - sub_w) // 2
    
    line_w = int(width * 0.065)
    line_y = sub_y + sub_h // 2 + 1
    draw = ImageDraw.Draw(img)
    draw.line([(sub_x - int(width * 0.02) - line_w, line_y), (sub_x - int(width * 0.02), line_y)], fill=(255, 255, 255, 50), width=1)
    draw.line([(sub_x + sub_w + int(width * 0.02), line_y), (sub_x + sub_w + int(width * 0.02) + line_w, line_y)], fill=(255, 255, 255, 50), width=1)
    draw.text((sub_x, sub_y), sub_text, font=font_sub, fill=(156, 163, 175, 230))
    
    # Accent bar
    bar_w = int(width * 0.32)
    bar_h = 3
    bar_y = sub_y + sub_h + int(height * 0.065)
    bar_x0 = (width - bar_w) // 2
    draw_linear_gradient(img, (bar_x0, bar_y, bar_x0 + bar_w, bar_y + bar_h), (229, 9, 20, 220), (245, 242, 235, 120), vertical=False)
    
    return img

def create_tv_icon(size=512, bebas_path="", inter_path="", round_icon=False, for_foreground=False):
    width, height = size, size
    
    if for_foreground:
        img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    else:
        img = Image.new('RGBA', (width, height), (6, 8, 12, 255))
        
    glow = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    
    # Scale factor for foreground (safe zone is ~66% of adaptive icon size)
    scale = 0.68 if for_foreground else 1.0
    
    cx, cy = width // 2, int(height * 0.44)
    rx, ry = int(width * 0.42 * scale), int(height * 0.38 * scale)
    glow_draw.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=(229, 9, 20, 95))
    glow = glow.filter(ImageFilter.GaussianBlur(radius=int(height * 0.16 * scale)))
    img = Image.alpha_composite(img, glow)
    
    font_tv_size = int(height * 0.22 * scale)
    font_pro_size = int(height * 0.28 * scale)
    font_sub_size = int(height * 0.034 * scale)
    
    font_tv = ImageFont.truetype(bebas_path, font_tv_size)
    font_pro = ImageFont.truetype(bebas_path, font_pro_size)
    font_sub = ImageFont.truetype(inter_path, font_sub_size)
    
    bbox_tv = font_tv.getbbox("TV")
    tv_w = bbox_tv[2] - bbox_tv[0]
    tv_h = bbox_tv[3] - bbox_tv[1]
    
    pad_h = int(height * 0.045 * scale)
    pad_v = int(height * 0.02 * scale)
    badge_w = tv_w + pad_h * 2
    badge_h = tv_h + pad_v * 2
    
    bbox_pro = font_pro.getbbox("PRO")
    pro_w = bbox_pro[2] - bbox_pro[0]
    pro_h = bbox_pro[3] - bbox_pro[1]
    
    spacing = int(height * 0.035 * scale)
    total_brand_w = badge_w + spacing + pro_w
    brand_start_x = (width - total_brand_w) // 2
    brand_center_y = int(height * 0.42)
    
    badge_x0 = brand_start_x
    badge_y0 = brand_center_y - (badge_h // 2)
    
    badge_img = Image.new('RGBA', (badge_w, badge_h), (0, 0, 0, 0))
    corner_radius = max(2, int(height * 0.04 * scale))
    draw_linear_gradient(badge_img, (0, 0, badge_w, badge_h), (229, 9, 20, 255), (139, 0, 0, 255), vertical=True)
    
    mask = Image.new('L', (badge_w, badge_h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, badge_w, badge_h], radius=corner_radius, fill=255)
    badge_img.putalpha(mask)
    
    border_img = Image.new('RGBA', (badge_w, badge_h), (0, 0, 0, 0))
    ImageDraw.Draw(border_img).rounded_rectangle([0, 0, badge_w - 1, badge_h - 1], radius=corner_radius, outline=(255, 255, 255, 90), width=max(1, int(2 * scale)))
    badge_img = Image.alpha_composite(badge_img, border_img)
    
    tv_text_x = (badge_w - tv_w) // 2 - bbox_tv[0]
    tv_text_y = (badge_h - tv_h) // 2 - bbox_tv[1]
    ImageDraw.Draw(badge_img).text((tv_text_x, tv_text_y), "TV", font=font_tv, fill=(255, 255, 255, 255))
    img.paste(badge_img, (badge_x0, badge_y0), badge_img)
    
    pro_x = badge_x0 + badge_w + spacing - bbox_pro[0]
    pro_y = brand_center_y - (pro_h // 2) - bbox_pro[1]
    ImageDraw.Draw(img).text((pro_x, pro_y), "PRO", font=font_pro, fill=(245, 242, 235, 255))
    
    sub_text = "E X P E R I E N C I A   C I N E M A T O G R Á F I C A"
    bbox_sub = font_sub.getbbox(sub_text)
    sub_w = bbox_sub[2] - bbox_sub[0]
    sub_h = bbox_sub[3] - bbox_sub[1]
    sub_y = badge_y0 + badge_h + int(height * 0.08 * scale)
    sub_x = (width - sub_w) // 2
    
    draw = ImageDraw.Draw(img)
    line_w = int(width * 0.05 * scale)
    line_y = sub_y + sub_h // 2 + 1
    draw.line([(sub_x - int(width * 0.02 * scale) - line_w, line_y), (sub_x - int(width * 0.02 * scale), line_y)], fill=(255, 255, 255, 55), width=1)
    draw.line([(sub_x + sub_w + int(width * 0.02 * scale), line_y), (sub_x + sub_w + int(width * 0.02 * scale) + line_w, line_y)], fill=(255, 255, 255, 55), width=1)
    draw.text((sub_x, sub_y), sub_text, font=font_sub, fill=(156, 163, 175, 230))
    
    bar_w = int(width * 0.36 * scale)
    bar_h = max(2, int(3 * scale))
    bar_y = sub_y + sub_h + int(height * 0.065 * scale)
    bar_x0 = (width - bar_w) // 2
    draw_linear_gradient(img, (bar_x0, bar_y, bar_x0 + bar_w, bar_y + bar_h), (229, 9, 20, 230), (245, 242, 235, 120), vertical=False)
    
    if round_icon and not for_foreground:
        circle_mask = Image.new('L', (width, height), 0)
        ImageDraw.Draw(circle_mask).ellipse([0, 0, width, height], fill=255)
        border = Image.new('RGBA', (width, height), (0, 0, 0, 0))
        ImageDraw.Draw(border).ellipse([1, 1, width - 2, height - 2], outline=(255, 255, 255, 35), width=max(1, int(2 * scale)))
        img.putalpha(circle_mask)
        img = Image.alpha_composite(img, border)
        
    return img

def main():
    root = r"c:\Users\Lenovo\OneDrive\Documentos\Proyectos\APK-TVpro\android-app\app\src\main\res"
    bebas = os.path.join(root, "font", "bebas_neue.ttf")
    inter = os.path.join(root, "font", "inter.ttf")
    
    # 1. Base Master Images (High Res)
    banner_high = create_tv_banner(960, 540, bebas, inter)
    banner_med = create_tv_banner(640, 360, bebas, inter)
    banner_std = create_tv_banner(320, 180, bebas, inter)
    
    icon_master = create_tv_icon(512, bebas, inter, round_icon=False)
    icon_round_master = create_tv_icon(512, bebas, inter, round_icon=True)
    fg_master = create_tv_icon(512, bebas, inter, for_foreground=True)
    
    # 2. Save Banners
    os.makedirs(os.path.join(root, "drawable"), exist_ok=True)
    os.makedirs(os.path.join(root, "drawable-xhdpi"), exist_ok=True)
    os.makedirs(os.path.join(root, "drawable-xxhdpi"), exist_ok=True)
    os.makedirs(os.path.join(root, "drawable-xxxhdpi"), exist_ok=True)
    
    banner_med.save(os.path.join(root, "drawable", "ic_banner.png"), "PNG")
    banner_std.save(os.path.join(root, "drawable-xhdpi", "ic_banner.png"), "PNG")
    banner_med.save(os.path.join(root, "drawable-xxhdpi", "ic_banner.png"), "PNG")
    banner_high.save(os.path.join(root, "drawable-xxxhdpi", "ic_banner.png"), "PNG")
    
    # Save standard drawable/ic_launcher.png (fallback)
    icon_master.save(os.path.join(root, "drawable", "ic_launcher.png"), "PNG")
    
    # 3. Save Mipmaps
    densities = {
        "mipmap-mdpi": (48, 108),
        "mipmap-hdpi": (72, 162),
        "mipmap-xhdpi": (96, 216),
        "mipmap-xxhdpi": (144, 324),
        "mipmap-xxxhdpi": (192, 432),
    }
    
    for folder, (icon_sz, fg_sz) in densities.items():
        folder_path = os.path.join(root, folder)
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
        
    print("All image assets created successfully in res directories!")

if __name__ == "__main__":
    main()
