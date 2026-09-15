from PIL import Image, ImageDraw, ImageFont, ImageOps
import os

os.makedirs("app/src/main/res/drawable", exist_ok=True)

# Helper to save and crop
def save_asset(img, filename):
    # Crop to bounding box with small padding
    bbox = img.getbbox()
    if bbox:
        pad = 8
        left = max(0, bbox[0] - pad)
        top = max(0, bbox[1] - pad)
        right = min(img.width, bbox[2] + pad)
        bottom = min(img.height, bbox[3] + pad)
        img = img.crop((left, top, right, bottom))
    img.save(f"app/src/main/res/drawable/{filename}")
    print(f"Generated {filename} with size {img.size}")

# 1. WARNER BROS. (Shield with WB)
def create_warner_bros():
    width, height = 500, 250
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Draw Shield
    shield_pts = [(100, 20), (200, 20), (180, 140), (150, 220), (120, 140), (100, 20)]
    # Outer gold border / inner blue field
    draw.polygon([(x+2, y+2) for x, y in shield_pts], fill=(212, 175, 55, 255)) # Gold border
    draw.polygon(shield_pts, fill=(15, 45, 95, 255)) # Deep Warner Blue

    # Inner gold border stripe
    inner_pts = [(108, 30), (192, 30), (174, 135), (150, 205), (126, 135), (108, 30)]
    draw.polygon(inner_pts, outline=(212, 175, 55, 255), width=3)

    # WB Letters
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 65)
    except:
        font = ImageFont.load_default()

    draw.text((118, 55), "WB", fill=(212, 175, 55, 255), font=font)

    # Wordmark "WARNER BROS." to the right
    try:
        font_sm = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 36)
    except:
        font_sm = font

    draw.text((220, 85), "WARNER BROS.", fill=(255, 255, 255, 255), font=font_sm)
    save_asset(img, "ic_brand_warner_bros.png")

# 2. COLUMBIA PICTURES (Torch Lady concept + Wordmark)
def create_columbia():
    width, height = 700, 200
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Torch icon representation (Flame + torch handle)
    # Flame
    draw.ellipse([30, 40, 70, 100], fill=(255, 180, 50, 255))
    draw.ellipse([40, 20, 60, 70], fill=(255, 255, 200, 255))
    # Handle / Silhouette pedestal
    draw.rectangle([45, 100, 55, 160], fill=(212, 175, 55, 255))
    draw.rectangle([35, 150, 65, 165], fill=(212, 175, 55, 255))

    try:
        font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 44)
    except:
        font = ImageFont.load_default()

    draw.text((90, 70), "COLUMBIA PICTURES", fill=(255, 255, 255, 255), font=font)
    save_asset(img, "ic_brand_columbia.png")

# 3. UNIVERSAL (Globe + Wordmark)
def create_universal():
    width, height = 650, 220
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Globe circle
    draw.ellipse([20, 35, 160, 175], outline=(212, 175, 55, 255), width=4)
    draw.ellipse([30, 45, 150, 165], fill=(20, 90, 160, 255)) # Blue Earth
    # Grid lines on globe
    draw.arc([30, 45, 150, 165], 0, 360, fill=(212, 175, 55, 200), width=2)
    draw.line([(90, 45), (90, 165)], fill=(212, 175, 55, 200), width=2)
    draw.line([(30, 105), (150, 105)], fill=(212, 175, 55, 200), width=2)

    try:
        font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 52)
    except:
        font = ImageFont.load_default()

    draw.text((180, 80), "UNIVERSAL", fill=(255, 255, 255, 255), font=font)
    save_asset(img, "ic_brand_universal.png")

# 4. BLUMHOUSE (House Monogram + Wordmark)
def create_blumhouse():
    width, height = 600, 200
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Stylized minimalist house monogram
    # Roof triangle
    draw.polygon([(70, 90), (110, 45), (150, 90)], fill=(220, 40, 40, 255))
    # House body
    draw.rectangle([78, 90, 142, 150], fill=(255, 255, 255, 255))
    # Door cutout
    draw.rectangle([103, 120, 117, 150], fill=(20, 20, 20, 255))

    try:
        font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 52)
    except:
        font = ImageFont.load_default()

    draw.text((170, 75), "BLUMHOUSE", fill=(255, 255, 255, 255), font=font)
    save_asset(img, "ic_brand_blumhouse.png")

# 5. HULU (Iconic green Hulu wordmark)
def create_hulu():
    width, height = 400, 200
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    try:
        font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 90)
    except:
        font = ImageFont.load_default()

    # Hulu vibrant green #1CE783
    draw.text((30, 50), "hulu", fill=(28, 231, 131, 255), font=font)
    save_asset(img, "ic_brand_hulu.png")

# 6. ITV (Connected vibrant ITV wordmark)
def create_itv():
    width, height = 400, 200
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    try:
        font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 95)
    except:
        font = ImageFont.load_default()

    # ITV brand bright yellow/white/cyan styling
    draw.text((40, 50), "itv", fill=(255, 220, 0, 255), font=font)
    save_asset(img, "ic_brand_itv.png")

# 7. MGM+ (MGM + Red/Gold Plus)
def create_mgm_plus():
    width, height = 450, 200
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    try:
        font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 75)
    except:
        font = ImageFont.load_default()

    draw.text((30, 60), "MGM", fill=(255, 255, 255, 255), font=font)
    draw.text((230, 50), "+", fill=(225, 30, 30, 255), font=font)
    save_asset(img, "ic_brand_mgm_plus.png")

if __name__ == "__main__":
    create_warner_bros()
    create_columbia()
    create_universal()
    create_blumhouse()
    create_hulu()
    create_itv()
    create_mgm_plus()
    print("All 7 refined brand assets generated successfully.")
