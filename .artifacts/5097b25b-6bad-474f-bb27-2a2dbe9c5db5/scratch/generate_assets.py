from PIL import Image, ImageDraw, ImageFont
import os

os.makedirs("app/src/main/res/drawable", exist_ok=True)

try:
    font_path = "C:/Windows/Fonts/arialbd.ttf"
    font_large = ImageFont.truetype(font_path, 80)
except Exception:
    font_large = ImageFont.load_default()

def create_wordmark(text, filename, width=600, height=180, font_size=75):
    img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", font_size)
    except:
        font = font_large

    # Get bounding box of text
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]

    x = (width - tw) / 2 - bbox[0]
    y = (height - th) / 2 - bbox[1]

    draw.text((x, y), text, fill=(255, 255, 255, 255), font=font)

    # Crop transparent padding
    bbox_img = img.getbbox()
    if bbox_img:
        # add a small padding
        pad = 10
        left = max(0, bbox_img[0] - pad)
        top = max(0, bbox_img[1] - pad)
        right = min(width, bbox_img[2] + pad)
        bottom = min(height, bbox_img[3] + pad)
        img = img.crop((left, top, right, bottom))

    img.save(f"app/src/main/res/drawable/{filename}")
    print(f"Generated {filename} with size {img.size}")

create_wordmark("ITV", "ic_brand_itv.png", font_size=90)
create_wordmark("COLUMBIA PICTURES", "ic_brand_columbia.png", font_size=50)
create_wordmark("WARNER BROS.", "ic_brand_warner_bros.png", font_size=60)
create_wordmark("UNIVERSAL", "ic_brand_universal.png", font_size=70)
create_wordmark("BLUMHOUSE", "ic_brand_blumhouse.png", font_size=65)
create_wordmark("hulu", "ic_brand_hulu.png", font_size=80)
create_wordmark("MGM+", "ic_brand_mgm_plus.png", font_size=80)
