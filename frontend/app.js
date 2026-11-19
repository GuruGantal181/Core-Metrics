// Swap these two lines once Keshav's Spring Boot server is running
const IS_MOCK_MODE = true;
const API_BASE_URL = "http://localhost:8080/api";

let rmChartInstance = null;

async function loadDashboard() {
  const endpoint = IS_MOCK_MODE
    ? "fake_api_response.json"
    : `${API_BASE_URL}/dashboard`;

  try {
    const response = await fetch(endpoint);
    if (!response.ok) {
      throw new Error(`HTTP error! Status: ${response.status}`);
    }

    const data = await response.json();
    renderBMR(data.user);
    renderWorkoutTable(data.workouts);
    render1RMChart(data.workouts);
  } catch (error) {
    console.error("Failed to load dashboard data:", error);
  }
}

function renderBMR(user) {
  const bmrEl = document.getElementById("bmr-display");
  if (bmrEl) {
    bmrEl.textContent = user.bmr_kcal;
  }
}

function renderWorkoutTable(workouts) {
  const tbody = document.getElementById("workout-table-body");
  if (!tbody) return;

  tbody.innerHTML = "";
  workouts.forEach((log) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${log.date}</td>
      <td>${log.exercise}</td>
      <td>${log.weight_kg}</td>
      <td>${log.reps}</td>
      <td>${log.estimated_1rm}</td>
    `;
    tbody.appendChild(tr);
  });
}

function render1RMChart(workouts) {
  const canvas = document.getElementById("rm-chart");
  if (!canvas) return;

  if (rmChartInstance) {
    rmChartInstance.destroy();
  }

  rmChartInstance = new Chart(canvas, {
    type: "line",
    data: {
      labels: workouts.map((w) => w.date),
      datasets: [
        {
          label: "Estimated 1RM (kg)",
          data: workouts.map((w) => w.estimated_1rm),
          borderColor: "#0d6efd",
          tension: 0.2
        }
      ]
    }
  });
}

function setupWorkoutForm() {
  const form = document.getElementById("log-workout-form");
  if (!form) return;

  form.addEventListener("submit", async (e) => {
    e.preventDefault();

    const payload = {
      exercise: document.getElementById("exercise-input").value,
      weight_kg: parseFloat(document.getElementById("weight-input").value),
      reps: parseInt(document.getElementById("reps-input").value, 10)
    };

    if (IS_MOCK_MODE) {
      console.log("Mock POST payload ready for Spring Boot:", payload);
      form.reset();
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/workouts`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
      if (!response.ok) throw new Error(`POST failed: ${response.status}`);
      form.reset();
      loadDashboard();
    } catch (error) {
      console.error("Failed to log workout:", error);
    }
  });
}

loadDashboard();
setupWorkoutForm();