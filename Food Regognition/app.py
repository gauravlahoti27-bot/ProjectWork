from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
import torch
import open_clip
import io
import pandas as pd

app = FastAPI()

# ✅ CORS (REQUIRED)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Load nutrition data
nutrition_df = pd.read_csv("nutrition.csv")
FOOD_CLASSES = nutrition_df["food"].tolist()

FOOD_PROMPTS = {
    "pizza": "a photo of a cheese pizza slice",
    "burger": "a photo of a burger with bun and patty",
    "salad": "a photo of vegetable salad in a bowl",
    "pasta": "a photo of pasta with sauce",
    "rice": "a photo of plain cooked rice",
    "fried_rice": "a photo of fried rice with vegetables",
    "biryani": "a photo of Indian biryani rice dish",
    "dosa": "a photo of South Indian dosa on a plate",
    "idli": "a photo of idli with chutney",
    "chapati": "a photo of Indian chapati or roti",
    "naan": "a photo of naan bread",
    "paneer": "a photo of paneer curry",
    "dal": "a photo of Indian dal lentil curry",
    "samosa": "a photo of samosa snack",
    "poha": "a photo of poha breakfast dish",
    "upma": "a photo of upma breakfast dish",
    "omelette": "a photo of egg omelette",
    "boiled_eggs": "a photo of boiled eggs",
    "grilled_chicken": "a photo of grilled chicken",
    "chicken_curry": "a photo of chicken curry"
}

# Load OpenCLIP model
device = "cpu"
model, _, preprocess = open_clip.create_model_and_transforms(
    model_name="ViT-B-32",
    pretrained="openai"
)
model.eval().to(device)

tokenizer = open_clip.get_tokenizer("ViT-B-32")


@app.post("/analyze-food/") # ✅ MATCH FRONTEND
async def analyze_food(file: UploadFile = File(...)):
    image_bytes = await file.read()
    image = Image.open(io.BytesIO(image_bytes)).convert("RGB")

    image_input = preprocess(image).unsqueeze(0).to(device)
    text_inputs = tokenizer(
        [FOOD_PROMPTS.get(food, f"a photo of {food}") for food in FOOD_CLASSES]
    ).to(device)

    with torch.no_grad():
        image_features = model.encode_image(image_input)
        text_features = model.encode_text(text_inputs)

        image_features /= image_features.norm(dim=-1, keepdim=True)
        text_features /= text_features.norm(dim=-1, keepdim=True)

        similarity = (image_features @ text_features.T).softmax(dim=-1)

    best_idx = similarity.argmax().item()
    confidence = similarity[0][best_idx].item()

    detected_food = FOOD_CLASSES[best_idx]
    row = nutrition_df[nutrition_df["food"] == detected_food].iloc[0]

    return {
        "food": detected_food,
        "calories": int(row["calories"]),
        "protein": int(row["protein"]),
        "fat": int(row["fat"]),
        "carbs": int(row["carbs"]),
        "confidence": round(confidence, 3)
    }