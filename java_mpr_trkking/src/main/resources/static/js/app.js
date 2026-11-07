/**
 * TrekMate - Core Interactive Client-Side Logic
 * Pure Vanilla JavaScript (No frameworks)
 */

document.addEventListener("DOMContentLoaded", function () {
    initPlanCalculator();
    initFlashAlertDismiss();
});

function initFlashAlertDismiss() {
    const alerts = document.querySelectorAll(".alert");
    alerts.forEach(function (alert) {
        setTimeout(function () {
            alert.style.transition = "opacity 0.5s ease";
            alert.style.opacity = "0";
            setTimeout(function () {
                if (alert.parentNode) {
                    alert.parentNode.removeChild(alert);
                }
            }, 500);
        }, 5000);
    });
}

function initPlanCalculator() {
    const planForm = document.getElementById("trekPlanForm");
    if (!planForm) return;

    const groupSizeInput = document.getElementById("groupSizeInput");
    const budgetInput = document.getElementById("budgetInput");
    const trekIdInput = document.getElementById("trekIdInput");
    const activityCheckboxes = document.querySelectorAll(".activity-checkbox");

    function getSelectedActivityIds() {
        const ids = [];
        activityCheckboxes.forEach(function (cb) {
            if (cb.checked) {
                ids.push(parseInt(cb.value, 10));
            }
        });
        return ids;
    }

    function recalculate() {
        const trekId = parseInt(trekIdInput.value, 10);
        const groupSize = parseInt(groupSizeInput.value, 10) || 1;
        const budgetVal = parseFloat(budgetInput.value) || 0;
        const selectedIds = getSelectedActivityIds();

        const payload = {
            trekId: trekId,
            groupSize: groupSize,
            userBudget: budgetVal,
            selectedActivityIds: selectedIds
        };

        fetch("/api/calculate-cost", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(payload)
        })
        .then(function (res) {
            if (!res.ok) throw new Error("Calculation error");
            return res.json();
        })
        .then(function (data) {
            updateSummaryUI(data, groupSize);
        })
        .catch(function (err) {
            console.error("Calculation failed:", err);
        });
    }

    function updateSummaryUI(data, groupSize) {
        // Format Currency Helper
        const formatInr = function (num) {
            return "₹" + Number(num).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        };

        // Update Summary Figures
        const baseCostEl = document.getElementById("summaryBaseCost");
        const activitiesCostEl = document.getElementById("summaryActivitiesCost");
        const facilitiesCostEl = document.getElementById("summaryFacilitiesCost");
        const totalCostEl = document.getElementById("summaryTotalCost");
        const perPersonCostEl = document.getElementById("summaryPerPersonCost");
        const groupSizeLabel = document.getElementById("summaryGroupCount");

        if (baseCostEl) baseCostEl.textContent = formatInr(data.baseCost);
        if (activitiesCostEl) activitiesCostEl.textContent = formatInr(data.activitiesCost);
        if (facilitiesCostEl) facilitiesCostEl.textContent = formatInr(data.facilitiesCost);
        if (totalCostEl) totalCostEl.textContent = formatInr(data.totalCost);
        if (perPersonCostEl) perPersonCostEl.textContent = formatInr(data.perPersonCost);
        if (groupSizeLabel) groupSizeLabel.textContent = groupSize + (groupSize === 1 ? " trekker" : " trekkers");

        // Update Budget Comparison Card
        const budgetBox = document.getElementById("budgetComparisonBox");
        const budgetText = document.getElementById("budgetComparisonText");
        const suggestionsBox = document.getElementById("suggestionsContainer");
        const suggestionsList = document.getElementById("suggestionsList");
        const altTreksBox = document.getElementById("altTreksBox");
        const altTreksList = document.getElementById("altTreksList");

        if (budgetBox && budgetText) {
            if (data.userBudget > 0) {
                budgetBox.style.display = "block";
                if (data.withinBudget) {
                    budgetBox.className = "budget-status-card within";
                    budgetText.innerHTML = "<strong>✅ Within Your Budget:</strong> You have <strong>" +
                        formatInr(data.budgetDiff) + "</strong> remaining out of your budget of " + formatInr(data.userBudget) + "!";
                } else {
                    budgetBox.className = "budget-status-card exceeded";
                    const deficit = Math.abs(data.budgetDiff);
                    budgetText.innerHTML = "<strong>⚠️ Budget Exceeded by " + formatInr(deficit) + ":</strong> Total cost is " +
                        formatInr(data.totalCost) + " against your budget of " + formatInr(data.userBudget) + ".";
                }
            } else {
                budgetBox.style.display = "none";
            }
        }

        // Render Suggestions
        if (suggestionsBox && suggestionsList) {
            if (data.suggestions && data.suggestions.length > 0 && !data.withinBudget) {
                suggestionsBox.style.display = "block";
                suggestionsList.innerHTML = "";
                data.suggestions.forEach(function (sug) {
                    const li = document.createElement("li");
                    li.textContent = sug;
                    suggestionsList.appendChild(li);
                });
            } else {
                suggestionsBox.style.display = "none";
            }
        }

        // Render Alternative Treks
        if (altTreksBox && altTreksList) {
            if (data.alternativeTreks && data.alternativeTreks.length > 0 && !data.withinBudget) {
                altTreksBox.style.display = "block";
                altTreksList.innerHTML = "";
                data.alternativeTreks.forEach(function (alt) {
                    const div = document.createElement("div");
                    div.style.padding = "8px 0";
                    div.style.borderBottom = "1px solid #fee2e2";
                    div.innerHTML = "<a href='/plan/" + alt.id + "?groupSize=" + groupSize + "&budget=" + data.userBudget +
                        "' style='font-weight:600; color:#b91c1c;'>" + alt.title + "</a> (" + alt.destination + ") &bull; " +
                        formatInr(alt.totalForGroup) + " (Save " + formatInr(alt.savings) + ")";
                    altTreksList.appendChild(div);
                });
            } else {
                altTreksBox.style.display = "none";
            }
        }

        // Wallet Check
        checkWalletBalance(data.totalCost);
    }

    function checkWalletBalance(totalCost) {
        fetch("/api/wallet-status?totalCost=" + totalCost)
            .then(function (res) { return res.json(); })
            .then(function (status) {
                const walletWarn = document.getElementById("walletWarningBox");
                const submitBtn = document.getElementById("submitBookingBtn");
                if (!walletWarn) return;

                if (status.loggedIn) {
                    if (!status.sufficient) {
                        walletWarn.style.display = "block";
                        const def = Number(status.deficit).toLocaleString("en-IN", { minimumFractionDigits: 2 });
                        walletWarn.innerHTML = "<strong>⚠️ Insufficient Virtual Credits:</strong> Your wallet balance is ₹" +
                            Number(status.balance).toLocaleString("en-IN", { minimumFractionDigits: 2 }) +
                            ". You need <strong>₹" + def + "</strong> more credits. " +
                            "<a href='/wallet' target='_blank' style='text-decoration:underline; font-weight:700;'>Top-Up Wallet Here</a>.";
                    } else {
                        walletWarn.style.display = "none";
                    }
                }
            })
            .catch(function (err) {
                console.error("Wallet check failed:", err);
            });
    }

    // Attach Event Listeners
    groupSizeInput.addEventListener("input", recalculate);
    budgetInput.addEventListener("input", recalculate);
    activityCheckboxes.forEach(function (cb) {
        cb.addEventListener("change", function () {
            const parentItem = cb.closest(".activity-item");
            if (parentItem) {
                if (cb.checked) {
                    parentItem.classList.add("selected");
                } else {
                    parentItem.classList.remove("selected");
                }
            }
            recalculate();
        });
    });

    // Initial run
    recalculate();
}

