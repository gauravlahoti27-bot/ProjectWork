
let totalCalories = 0;
let mealHistory = [];

// ===============================
// 📌 DOM ELEMENTS
// ===============================
const imageInput = document.getElementById("foodImage");
const preview = document.getElementById("preview");
const scanContainer = document.querySelector(".scan-container");
const scanLine = document.querySelector(".scan-line");
const btn = document.getElementById("analyzeBtn");
const result = document.getElementById("result");
const daily = document.getElementById("dailyCalories");
const card = document.querySelector(".card");

// ===============================
// 📸 IMAGE PREVIEW
// ===============================
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

// ===============================
// 🤖 ANALYZE BUTTON
// ===============================
btn.addEventListener("click", function () {

  const file = imageInput.files[0];

  if (!file) {
    result.innerText = "❌ Please upload a food image first.";
    return;
  }

  // start animation
  card.classList.add("pulse");
  scanLine.style.display = "block";
  result.innerText = "🤖 AI scanning food image...";

  const formData = new FormData();
  formData.append("image", file);

  fetch("http://127.0.0.1:5000/analyze", {
    method: "POST",
    body: formData
  })
  .then(res => {
    if (!res.ok) throw new Error("Server error");
    return res.json();
  })
  .then(data => {

    totalCalories += data.calories;

    // save history
    mealHistory.push({
      food: data.food,
      calories: data.calories,
      time: new Date().toLocaleTimeString()
    });

    localStorage.setItem(
      "mealHistory",
      JSON.stringify(mealHistory)
    );

    // show result
    result.innerHTML = `
      🍕 Detected Food: <b>${data.food}</b><br>
      🔥 Calories: <b>${data.calories} kcal</b><br>
      🥗 Carbs: <b>${data.carbs}g</b><br>
      🍗 Protein: <b>${data.protein}g</b><br>
      🧈 Fat: <b>${data.fat}g</b>
    `;

    daily.innerText =
      `Calories Today: ${totalCalories} kcal`;

    // stop animation
    card.classList.remove("pulse");
    scanLine.style.display = "none";
  })
  .catch(error => {

    console.error(error);

    result.innerText =
      "❌ Failed to connect to AI backend.";

    card.classList.remove("pulse");
    scanLine.style.display = "none";
  });

});

// ===============================
// 📊 LOAD HISTORY ON START
// ===============================
window.addEventListener("load", function () {

  const saved =
    localStorage.getItem("mealHistory");

  if (saved) {
    mealHistory = JSON.parse(saved);

    totalCalories = mealHistory
      .reduce((sum, meal) =>
        sum + meal.calories, 0);

    daily.innerText =
      `Calories Today: ${totalCalories} kcal`;
  }

});