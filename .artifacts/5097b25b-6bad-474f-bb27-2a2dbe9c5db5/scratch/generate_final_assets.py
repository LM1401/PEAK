from PIL import Image, ImageDraw, ImageFont
import os

os.makedirs("app/src/main/res/drawable", exist_ok=True)

def save_tight_asset(img, filename):
    # Crop to bounding box with small professional padding
    bbox = img.getbbox()
    if bbox:
        pad = 6
        left = max(0, bbox[0] - pad)
        top = max(0, bbox[1] - pad)
        right = min(img.width, bbox[2] + pad)
        bottom = min(img.height, bbox[3] + pad)
        img = img.crop((left, top, right, bottom))
    img.save(f"app/src/main/res/drawable/{filename}")
    print(f"Generated {filename} with size {img.size}")

# 1. PARAMOUNT+ (Distinctive Blue Script Paramount+)
def create_paramount_plus():
    width, height = 700, 220
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    try:
        font_script = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 75)
        font_plus = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 65)
    except:
        font_script = ImageFont.load_default()
        font_plus = font_script

    # Paramount blue signature color
    blue_color = (20, 100, 230, 255)

    draw.text((40, 60), "Paramount", fill=blue_color, font=font_script)
    draw.text((445, 55), "+", fill=blue_color, font=font_plus)

    save_tight_asset(img, "ic_brand_paramount_plus.png")

# 2. APPLE TV (Apple Symbol / Text + "tv")
def create_apple_tv():
    width, height = 500, 200
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    try:
        font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 70)
    except:
        font = ImageFont.load_default()

    # Draw a clean Apple-like symbol representation (or stylized Apple shape) + "tv"
    # For Apple logo silhouette: circle + bite + leaf or clean text "Apple tv" / "tv"
    # The prompt requests: "Apple symbol + 'tv'"
    # Let's draw an apple silhouette (circle body + stem/leaf)

    # Apple body circle
    draw.ellipse([40, 65, 110, 140], fill=(255, 255, 255, 255))
    # Bite out of right side
    draw.ellipse([90, 75, 120, 115], fill=(0, 0, 0, 0))
    # Leaf
    draw.arc([80, 45, 105, 75], 180, 360, fill=(255, 255, 255, 255), width=5)

    draw.text((135, 58), "tv", fill=(255, 255, 255, 255), font=font)

    save_tight_asset(img, "ic_brand_apple_tv.png")

# 3. MGM+ (Clean premium MGM + Red Plus)
def create_mgm_plus():
    width, height = 500, 200
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    try:
        font_mgm = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 75)
        font_plus = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 75)
    except:
        font_mgm = ImageFont.load_default()
        font_plus = font_mgm

    draw.text((40, 55), "MGM", fill=(255, 255, 255, 255), font=font_mgm)
    draw.text((230, 50), "+", fill=({230, 40, 40, 255} if isinstance(230, int) else (230, 40, 40, 255)), font=font_plus) # Vibrant red plus

    save_tight_asset(img, "ic_brand_mgm_plus.png")

# 4. UNIVERSAL (Well-balanced Globe + Bold Wordmark)
def create_universal():
    width, height = 650, 220
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Larger, prominent globe
    draw.ellipse([20, 20, 180, 180], outline=(230, 190, 60, 255), width=5) # Gold ring
    draw.ellipse([28, 28, 172, 172], fill=(15, 80, 150, 255)) # Deep blue globe
    # Latitude / Longitude lines
    draw.arc([28, 28, 172, 172], 0, 360, fill=(230, 190, 60, 220), width=3)
    draw.line([(100, 28), (100, 172)], fill=(230, 190, 60, 220), width=3)
    draw.line([(28, 100), (172, 100)], fill=(230, 190, 60, 220), width=3)

    try:
        font = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 60)
    except:
        font = ImageFont.load_default()

    draw.text((205, 72), "UNIVERSAL", fill=(255, 255, 255, 255), font=font)

    save_tight_asset(img, "ic_brand_universal.png")

# 5. BLUMHOUSE (Distinctive House Monogram + Wordmark)
def create_blumhouse():
    width, height = 650, 200
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Sleek stylized house icon with cinematic dark/red styling
    # Roof
    draw.polygon([(65, 95), (110, 40), (155, 95)], fill=(220, 35, 35, 255))
    # House base
    draw.rectangle([75, 95, 145, 160], fill=(255, 255, 255, 255))
    # Door
    draw.rectangle([102, 125, 118, 160], fill=(20, 20, 20, 255))

    try:
        font = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 55)
    except:
        font = ImageFont.load_default()

    draw.text((185, 75), "BLUMHOUSE", fill=(255, 255, 255, 255), font=font)

    save_tight_asset(img, "ic_brand_blumhouse.png")

if __name__ == "__main__":
    create_paramount_plus()
    create_apple_tv()
    create_mgm_plus()
    create_universal()
    create_blumhouse()
    print("All canonical and refined branding assets generated successfully.")
