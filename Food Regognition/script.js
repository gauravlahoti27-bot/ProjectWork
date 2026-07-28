let totalCalories = 0;

const BACKEND_URL = "http://10.110.80.15:8000";

const imageInput = document.getElementById("foodImage");
const preview = document.getElementById("preview");
const scanContainer = document.querySelector(".scan-container");
const scanLine = document.querySelector(".scan-line");
const btn = document.getElementById("analyzeBtn");
const result = document.getElementById("result");
const daily = document.getElementById("dailyCalories");
const card = document.querySelector(".card");

// ========================
// IMAGE PREVIEW
// ========================
imageInput.addEventListener("change", function () {

  const file = imageInput.files[0];
  if (!file) return;

  const reader = new FileReader();

  reader.onload = function () {
    preview.src = reader.result;
    scanContainer.style.display = "block";
  };

  reader.readAsDataURL(file);
});

// ========================
// ANALYZE FOOD
// ========================
btn.addEventListener("click", async function () {

  if (!imageInput.files[0]) {
    result.innerText = "❌ Upload food image first.";
    return;
  }

  card.classList.add("pulse");
  scanLine.style.display = "block";

  result.innerText = "🤖 AI analyzing food...";

  const formData = new FormData();
  formData.append("file", imageInput.files[0]);

  try {

    const response = await fetch(`${BACKEND_URL}/analyze-food/`
, {
      method: "POST",
      body: formData
    });

    const data = await response.json();

    totalCalories += data.calories;

    result.innerHTML = `
      🍕 Food: <b>${data.food}</b><br>
      🔥 Calories: <b>${data.calories}</b><br>
      🥗 Carbs: <b>${data.carbs}g</b><br>
      🍗 Protein: <b>${data.protein}g</b><br>
      🧈 Fat: <b>${data.fat}g</b>
    `;

    daily.innerText = `Calories Today: ${totalCalories} kcal`;

    saveMeal(
      data.food,
      data.calories,
      data.carbs,
      data.protein,
      data.fat
    );

  } catch {
    result.innerText = "❌ Backend not reachable.";
  }

  card.classList.remove("pulse");
  scanLine.style.display = "none";

});

// ========================
// NAVIGATE TO DASHBOARD
// ========================
function goDashboard() {
  window.location.href = "dashboard.html";
}

// ========================
// SAVE DATA FOR DASHBOARD
// ========================
function saveMeal(food, calories, carbs, protein, fat) {

  const meals = JSON.parse(localStorage.getItem("meals")) || [];

  meals.push({
    name: food,
    calories: calories,
    carbs: carbs,
    protein: protein,
    fat: fat,
    date: new Date().toISOString().split("T")[0]
  });

  localStorage.setItem("meals", JSON.stringify(meals));
}
