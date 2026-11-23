const API_BASE_URL = "http://localhost:8080/api";
const CURRENT_USER_ID = 1;

// Matches starter seed IDs in database/schema.sql
const EXERCISE_NAMES = {
  1: "Bench Press",
  2: "Pullups",
  3: "Treadmill Run",
  4: "Cycling"
};

let rmChartInstance = null;

function calculate1RM(weightKg, reps) {
  if (reps <= 1) return weightKg;
  return weightKg * (1 + reps / 30.0);
}

async function loadDashboard() {
  try {
    const [analyticsRes, workoutsRes] = await Promise.all([
      fetch(`${API_BASE_URL}/analytics/${CURRENT_USER_ID}`),
      fetch(`${API_BASE_URL}/workouts/${CURRENT_USER_ID}`)
    ]);

    if (!analyticsRes.ok) throw new Error(`Analytics error: ${analyticsRes.status}`);
    if (!workoutsRes.ok) throw new Error(`Workouts error: ${workoutsRes.status}`);

    const analytics = await analyticsRes.json();
    const workouts = await workoutsRes.json();

    renderBMR(analytics.bmr);
    renderWorkoutTable(workouts);
    render1RMChart(workouts);
  } catch (error) {
    console.error("Failed to load live dashboard data:", error);
  }
}

function renderBMR(bmr) {
  const bmrEl = document.getElementById("bmr-display");
  if (bmrEl && bmr != null) {
    bmrEl.textContent = Math.round(bmr);
  }
}

function renderWorkoutTable(workouts) {
  const tbody = document.getElementById("workout-table-body");
  if (!tbody) return;

  tbody.innerHTML = "";

  if (workouts.length === 0) {
    tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted">No workouts logged yet.</td></tr>`;
    return;
  }

  workouts.forEach((log) => {
    const exerciseName = EXERCISE_NAMES[log.exerciseId] || `Exercise #${log.exerciseId}`;
    const est1RM = calculate1RM(log.weightKg, log.reps).toFixed(1);

    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${exerciseName}</td>
      <td>${log.weightKg}</td>
      <td>${log.reps}</td>
      <td>${est1RM}</td>
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

  const labels = workouts.map((w, idx) =>
    w.loggedAt ? new Date(w.loggedAt).toLocaleDateString() : `Set #${idx + 1}`
  );
  const dataPoints = workouts.map((w) => Number(calculate1RM(w.weightKg, w.reps).toFixed(1)));

  rmChartInstance = new Chart(canvas, {
    type: "line",
    data: {
      labels: labels,
      datasets: [
        {
          label: "Estimated 1RM (kg)",
          data: dataPoints,
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
      userId: CURRENT_USER_ID,
      exerciseId: parseInt(document.getElementById("exercise-input").value, 10),
      weightKg: parseFloat(document.getElementById("weight-input").value),
      reps: parseInt(document.getElementById("reps-input").value, 10)
    };

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
      console.error("Failed to save workout:", error);
    }
  });
}

loadDashboard();
setupWorkoutForm();