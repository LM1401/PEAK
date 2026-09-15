from PIL import Image, ImageDraw, ImageFont
import os

os.makedirs("app/src/main/res/drawable", exist_ok=True)

def save_tight_asset(img, filename):
    # Crop tightly to bounding box with 2px padding
    bbox = img.getbbox()
    if bbox:
        pad = 2
        left = max(0, bbox[0] - pad)
        top = max(0, bbox[1] - pad)
        right = min(img.width, bbox[2] + pad)
        bottom = min(img.height, bbox[3] + pad)
        img = img.crop((left, top, right, bottom))
    img.save(f"app/src/main/res/drawable/{filename}")
    print(f"Generated {filename} with size {img.size}")

# 1. APPLE TV: Clean Apple logo + "tv" matching reference
def create_apple_tv():
    img = Image.new("RGBA", (400, 160), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 90)
    except:
        font = ImageFont.load_default()

    # Apple shape representation
    draw.ellipse([20, 45, 95, 130], fill=(255, 255, 255, 255))
    draw.ellipse([70, 55, 105, 95], fill=(0, 0, 0, 0)) # Bite
    draw.arc([60, 25, 88, 55], 180, 360, fill=(255, 255, 255, 255), width=6)

    draw.text((120, 35), "tv", fill=(255, 255, 255, 255), font=font)
    save_tight_asset(img, "ic_brand_apple_tv.png")

# 2. PARAMOUNT+: Blue script Paramount +
def create_paramount_plus():
    img = Image.new("RGBA", (550, 160), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    try:
        font_script = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 85)
        font_plus = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 85)
    except:
        font_script = ImageFont.load_default()
        font_plus = font_script

    blue = (20, 110, 240, 255)
    draw.text((10, 30), "Paramount", fill=blue, font=font_script)
    draw.text((395, 25), "+", fill=blue, font=font_plus)
    save_tight_asset(img, "ic_brand_paramount_plus.png")

# 3. WARNER BROS.: Large prominent Shield + wordmark
def create_warner_bros():
    img = Image.new("RGBA", (600, 200), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Shield
    shield = [(350, 10), (550, 10), (520, 120), (450, 190), (380, 120), (350, 10)]
    draw.polygon([(x+3, y+3) for x, y in shield], fill=(230, 190, 50, 255))
    draw.polygon(shield, fill=(20, 55, 120, 255))

    try:
        font_wb = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 60)
        font_text = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 35)
    except:
        font_wb = ImageFont.load_default()
        font_text = font_wb

    draw.text((405, 45), "WB", fill=(230, 190, 50, 255), font=font_wb)
    draw.text((30, 130), "WARNER BROS.", fill=(255, 255, 255, 255), font=font_text)
    save_tight_asset(img, "ic_brand_warner_bros.png")

# 4. UNIVERSAL: Large globe + prominent UNIVERSAL wordmark
def create_universal():
    img = Image.new("RGBA", (650, 200), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Globe
    draw.ellipse([10, 10, 190, 190], outline=(230, 190, 60, 255), width=6)
    draw.ellipse([18, 18, 182, 182], fill=(10, 70, 140, 255))
    draw.arc([18, 18, 182, 182], 0, 360, fill=(230, 190, 60, 220), width=4)
    draw.line([(100, 18), (100, 182)], fill=(230, 190, 60, 220), width=4)
    draw.line([(18, 100), (182, 100)], fill=(230, 190, 60, 220), width=4)

    try:
        font = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 65)
    except:
        font = ImageFont.load_default()

    draw.text((215, 65), "UNIVERSAL", fill=(255, 255, 255, 255), font=font)
    save_tight_asset(img, "ic_brand_universal.png")

# 5. BLUMHOUSE: House monogram + wordmark
def create_blumhouse():
    img = Image.new("RGBA", (550, 160), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    draw.polygon([(40, 75), (80, 25), (120, 75)], fill=(220, 40, 40, 255))
    draw.rectangle([48, 75, 112, 135], fill=(255, 255, 255, 255))
    draw.rectangle([72, 105, 88, 135], fill=(15, 15, 15, 255))

    try:
        font = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 55)
    except:
        font = ImageFont.load_default()

    draw.text((145, 58), "BLUMHOUSE", fill=(255, 255, 255, 255), font=font)
    save_tight_asset(img, "ic_brand_blumhouse.png")

# 6. MGM+: Gold/bronze MGM +
def create_mgm_plus():
    img = Image.new("RGBA", (400, 160), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 75)
    except:
        font = ImageFont.load_default()

    gold = (220, 180, 70, 255)
    draw.text((20, 35), "MGM", fill=gold, font=font)
    draw.text((210, 30), "+", fill=gold, font=font)
    save_tight_asset(img, "ic_brand_mgm_plus.png")

# 7. HULU: Bright green hulu wordmark
def create_hulu():
    img = Image.new("RGBA", (350, 160), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 90)
    except:
        font = ImageFont.load_default()

    green = (28, 231, 131, 255)
    draw.text((20, 30), "hulu", fill=green, font=font)
    save_tight_asset(img, "ic_brand_hulu.png")

if __name__ == "__main__":
    create_apple_tv()
    create_paramount_plus()
    create_warner_bros()
    create_universal()
    create_blumhouse()
    create_mgm_plus()
    create_hulu()
    print("All final consolidated branding assets generated successfully.")
