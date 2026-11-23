const API_BASE_URL = "http://localhost:8080/api";

let currentUserId = null;
let usersCache = [];
let exercisesCache = [];
let rmChartInstance = null;
let statusTimeout = null;

const USER_REQUIRED_FORM_IDS = [
  "edit-weight-form",
  "log-workout-form",
  "log-cardio-form",
  "log-diet-form",
  "create-routine-form",
  "add-routine-exercise-form",
  "add-exercise-form"
];

function showStatus(message, type = "success") {
  const element = document.getElementById("status-alert");
  if (!element) return;

  element.className = `alert alert-${type}`;
  element.textContent = message;
  if (statusTimeout) clearTimeout(statusTimeout);
  statusTimeout = setTimeout(() => element.classList.add("d-none"), 4000);
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  })[character]);
}

async function apiRequest(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(options.headers || {})
    }
  });

  const contentType = response.headers.get("content-type") || "";
  const data = contentType.includes("application/json")
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    const message = data && typeof data === "object" && data.error
      ? data.error
      : (typeof data === "string" && data.trim() ? data : `Request failed (${response.status}).`);
    throw new Error(message);
  }

  return data;
}

function calculate1RM(weightKg, reps) {
  if (reps <= 1) return Number(weightKg) || 0;
  return Number(weightKg) * (1 + Number(reps) / 30.0);
}

function getExerciseName(id) {
  const found = exercisesCache.find((exercise) => Number(exercise.exerciseId) === Number(id));
  return found ? found.name : `Exercise #${id}`;
}

function getActiveUser() {
  return usersCache.find((user) => Number(user.userId) === Number(currentUserId)) || null;
}

function hideModal(id) {
  const modalElement = document.getElementById(id);
  if (modalElement && window.bootstrap?.Modal) {
    bootstrap.Modal.getOrCreateInstance(modalElement).hide();
  }
}

function setUserDependentControlsEnabled(enabled) {
  USER_REQUIRED_FORM_IDS.forEach((id) => {
    const form = document.getElementById(id);
    if (!form) return;
    form.querySelectorAll("input, select, textarea, button[type='submit']").forEach((control) => {
      control.disabled = !enabled;
    });
  });

  const editButton = document.querySelector('[data-bs-target="#editWeightModal"]');
  const deleteButton = document.getElementById("delete-user-btn");
  if (editButton) editButton.disabled = !enabled;
  if (deleteButton) deleteButton.disabled = !enabled;
  const switcher = document.getElementById("user-switcher");
  if (switcher) switcher.disabled = usersCache.length === 0;
}

function updateActiveUserBanner() {
  const user = getActiveUser();
  const banner = document.getElementById("active-user-banner");
  if (!banner) return;

  if (!user) {
    banner.textContent = "No active user selected. Create a profile to start tracking.";
    setUserDependentControlsEnabled(false);
    return;
  }

  banner.textContent = `Active Profile: User #${user.userId} — ${user.weightKg} kg | ${user.heightCm} cm | Age ${user.age} (${user.gender})`;

  const profileInputs = {
    "edit-weight-input": user.weightKg,
    "edit-height-input": user.heightCm,
    "edit-age-input": user.age,
    "edit-gender-input": user.gender
  };
  Object.entries(profileInputs).forEach(([id, value]) => {
    const input = document.getElementById(id);
    if (input) input.value = value;
  });

  setUserDependentControlsEnabled(true);
}

// ---------------------------------------------------------------------------
// Initial loading and profile/exercise selectors
// ---------------------------------------------------------------------------

async function initApp() {
  try {
    await Promise.all([loadUsers(), loadExercises()]);
    if (currentUserId !== null) {
      await loadAllUserData();
    } else {
      clearUserDashboard();
    }
  } catch (error) {
    console.error("Application initialization failed:", error);
    showStatus(error.message || "Unable to initialize Core Metrics.", "danger");
  }
}

async function loadUsers() {
  const users = await apiRequest("/users");
  usersCache = Array.isArray(users) ? users : [];

  const switcher = document.getElementById("user-switcher");
  switcher.replaceChildren();

  if (usersCache.length === 0) {
    currentUserId = null;
    const option = document.createElement("option");
    option.value = "";
    option.textContent = "No users found";
    switcher.appendChild(option);
    updateActiveUserBanner();
    clearUserDashboard();
    return;
  }

  if (!usersCache.some((user) => Number(user.userId) === Number(currentUserId))) {
    currentUserId = Number(usersCache[0].userId);
  }

  usersCache.forEach((user) => {
    const option = document.createElement("option");
    option.value = String(user.userId);
    option.textContent = `User #${user.userId} (${user.weightKg} kg, ${user.heightCm} cm, ${user.age} y, ${user.gender})`;
    option.selected = Number(user.userId) === Number(currentUserId);
    switcher.appendChild(option);
  });

  updateActiveUserBanner();
  updateCardioEstimate();
}

function populateSelect(select, exercises, placeholder) {
  if (!select) return;
  select.replaceChildren();

  const placeholderOption = document.createElement("option");
  placeholderOption.value = "";
  placeholderOption.textContent = placeholder;
  placeholderOption.disabled = true;
  placeholderOption.selected = true;
  select.appendChild(placeholderOption);

  exercises.forEach((exercise) => {
    const option = document.createElement("option");
    option.value = String(exercise.exerciseId);
    option.textContent = `${exercise.name} (MET ${exercise.metValue})`;
    select.appendChild(option);
  });
  select.disabled = exercises.length === 0 || currentUserId === null;
}

async function loadExercises() {
  const exercises = await apiRequest("/exercises");
  exercisesCache = Array.isArray(exercises) ? exercises : [];

  populateSelect(
    document.getElementById("exercise-input"),
    exercisesCache.filter((exercise) => exercise.type === "strength"),
    "Select a strength exercise"
  );
  populateSelect(
    document.getElementById("cardio-exercise-input"),
    exercisesCache.filter((exercise) => exercise.type === "cardio"),
    "Select a cardio exercise"
  );
  populateSelect(
    document.getElementById("routine-exercise-select"),
    exercisesCache,
    "Select an exercise"
  );

  renderExerciseDictionary();
}

function renderExerciseDictionary() {
  const body = document.getElementById("exercise-dict-body");
  body.replaceChildren();

  if (exercisesCache.length === 0) {
    body.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No exercises found in exercise_dict.</td></tr>';
    return;
  }

  exercisesCache.forEach((exercise) => {
    const row = document.createElement("tr");
    const scope = exercise.createdByUid == null ? "Global" : `User #${exercise.createdByUid}`;
    row.innerHTML = `
      <td>${escapeHtml(exercise.exerciseId)}</td>
      <td>${escapeHtml(exercise.name)}</td>
      <td><span class="badge ${exercise.type === "strength" ? "bg-primary" : "bg-warning text-dark"}">${escapeHtml(exercise.type)}</span></td>
      <td>${escapeHtml(exercise.metValue)}</td>
      <td>${escapeHtml(scope)}</td>`;
    body.appendChild(row);
  });
}

// ---------------------------------------------------------------------------
// Dashboard loading and renderers
// ---------------------------------------------------------------------------

async function loadAllUserData() {
  if (currentUserId === null) {
    clearUserDashboard();
    return;
  }

  const days = document.getElementById("analytics-days").value;
  const minCal = document.getElementById("high-cal-threshold").value || "1800";
  const userId = currentUserId;

  try {
    const [analytics, workouts, summary, cardio, diet, aboveAvg, highCalDays, routines] = await Promise.all([
      apiRequest(`/analytics/${userId}?days=${encodeURIComponent(days)}`),
      apiRequest(`/workouts/${userId}`),
      apiRequest(`/exercises/${userId}/summary`),
      apiRequest(`/cardio/${userId}`),
      apiRequest(`/diet/${userId}`),
      apiRequest(`/diet/${userId}/above-average`),
      apiRequest(`/diet/${userId}/high-calorie-days?min=${encodeURIComponent(minCal)}`),
      apiRequest(`/routines/${userId}`)
    ]);

    // Ignore a response that belongs to the previous user after a fast profile switch.
    if (Number(userId) !== Number(currentUserId)) return;

    let sqlInsights = null;
    try {
      sqlInsights = await apiRequest(`/sql-insights/${userId}`);
    } catch (insightError) {
      console.error("SQL insight loading failed:", insightError);
    }
    if (Number(userId) !== Number(currentUserId)) return;

    renderAnalytics(analytics);
    renderWorkoutTable(workouts);
    render1RMChart(workouts);
    renderExerciseSummary(summary);
    renderCardioTable(cardio);
    renderDietTable(diet);
    renderDietInsights(aboveAvg, highCalDays);
    renderSqlInsights(sqlInsights);
    await renderRoutines(routines, userId);
  } catch (error) {
    console.error("Dashboard load error:", error);
    showStatus(error.message || "Failed to load dashboard data. Check the Spring Boot API and MySQL connection.", "danger");
  }
}

function clearUserDashboard() {
  const values = [
    "bmr-display",
    "intake-display",
    "cardio-burn-display",
    "net-calories-display",
    "deficit-display"
  ];
  values.forEach((id) => {
    const element = document.getElementById(id);
    if (element) element.textContent = "--";
  });

  renderWorkoutTable([]);
  renderCardioTable([]);
  renderDietTable([]);
  renderExerciseSummary([]);
  renderDietInsights([], []);
  ["sql-bmr-display", "weekly-calories-in", "weekly-cardio-out", "weekly-strength-sets", "strength-volume-display"].forEach((id) => {
    const element = document.getElementById(id);
    if (element) element.textContent = "--";
  });
  const nutritionViewBody = document.getElementById("nutrition-view-body");
  if (nutritionViewBody) nutritionViewBody.innerHTML = '<tr><td colspan="3" class="text-center text-muted">No active user.</td></tr>';

  const routineContainer = document.getElementById("routines-container");
  const routineSelect = document.getElementById("routine-select");
  if (routineContainer) routineContainer.innerHTML = '<p class="text-muted">Create a user profile to manage routines.</p>';
  if (routineSelect) {
    routineSelect.innerHTML = '<option value="">No active user</option>';
    routineSelect.disabled = true;
  }
  if (rmChartInstance) {
    rmChartInstance.destroy();
    rmChartInstance = null;
  }
}

function renderAnalytics(analytics) {
  document.getElementById("bmr-display").textContent = Math.round(analytics.bmr ?? 0);
  document.getElementById("intake-display").textContent = Math.round(analytics.totalIntake ?? 0);
  document.getElementById("cardio-burn-display").textContent = Math.round(analytics.totalCardioBurn ?? 0);
  document.getElementById("net-calories-display").textContent = Math.round(analytics.netCalories ?? 0);
  document.getElementById("deficit-display").textContent = Math.round(analytics.caloricDeficit ?? 0);
}

function renderWorkoutTable(workouts) {
  const body = document.getElementById("workout-table-body");
  body.replaceChildren();

  if (!workouts || workouts.length === 0) {
    body.innerHTML = '<tr><td colspan="4" class="text-center text-muted">No workouts logged yet.</td></tr>';
    return;
  }

  workouts.forEach((log) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${escapeHtml(getExerciseName(log.exerciseId))}</td>
      <td>${escapeHtml(log.weightKg)}</td>
      <td>${escapeHtml(log.reps)}</td>
      <td>${calculate1RM(log.weightKg, log.reps).toFixed(1)}</td>`;
    body.appendChild(row);
  });
}

function render1RMChart(workouts) {
  const canvas = document.getElementById("rm-chart");
  if (!canvas || typeof window.Chart !== "function") return;

  if (rmChartInstance) {
    rmChartInstance.destroy();
    rmChartInstance = null;
  }

  if (!workouts || workouts.length === 0) return;

  const sorted = [...workouts].sort((first, second) => {
    const firstTime = first.loggedAt ? new Date(first.loggedAt).getTime() : 0;
    const secondTime = second.loggedAt ? new Date(second.loggedAt).getTime() : 0;
    return firstTime - secondTime || Number(first.logId) - Number(second.logId);
  });

  const labels = sorted.map((workout, index) => {
    const date = workout.loggedAt ? new Date(workout.loggedAt) : null;
    const label = date && !Number.isNaN(date.getTime()) ? date.toLocaleString() : `Set ${index + 1}`;
    return `${label} (#${workout.logId ?? index + 1})`;
  });

  const exerciseIds = [...new Set(sorted.map((workout) => Number(workout.exerciseId)))];
  const palette = ["#0d6efd", "#198754", "#dc3545", "#fd7e14", "#6f42c1", "#20c997", "#d63384", "#6c757d"];
  const datasets = exerciseIds.map((exerciseId, index) => ({
    label: getExerciseName(exerciseId),
    data: sorted.map((workout) => Number(workout.exerciseId) === exerciseId
      ? Number(calculate1RM(workout.weightKg, workout.reps).toFixed(1))
      : null),
    borderColor: palette[index % palette.length],
    backgroundColor: palette[index % palette.length],
    tension: 0.2,
    spanGaps: false
  }));

  rmChartInstance = new Chart(canvas, {
    type: "line",
    data: { labels, datasets },
    options: {
      responsive: true,
      maintainAspectRatio: true,
      interaction: { mode: "nearest", intersect: false },
      plugins: { legend: { display: datasets.length > 1 } },
      scales: {
        y: { beginAtZero: false, title: { display: true, text: "Estimated 1RM (kg)" } },
        x: { title: { display: true, text: "Workout date" } }
      }
    }
  });
}

function renderExerciseSummary(summary) {
  const list = document.getElementById("exercise-summary-list");
  list.replaceChildren();

  if (!summary || summary.length === 0) {
    list.innerHTML = '<li class="list-group-item text-muted small">No strength exercises found.</li>';
    return;
  }

  summary.forEach((exercise) => {
    const item = document.createElement("li");
    item.className = "list-group-item small d-flex justify-content-between gap-2";
    const hasLogs = exercise.maxWeightKg !== null && exercise.maxWeightKg !== undefined;
    const max = Number(exercise.maxWeightKg ?? 0);
    item.innerHTML = `<span>${escapeHtml(exercise.name)}</span><strong>${hasLogs ? `${max.toFixed(1)} kg` : "No logs"}</strong>`;
    list.appendChild(item);
  });
}

function renderCardioTable(logs) {
  const body = document.getElementById("cardio-table-body");
  body.replaceChildren();

  if (!logs || logs.length === 0) {
    body.innerHTML = '<tr><td colspan="4" class="text-center text-muted">No cardio sessions logged yet.</td></tr>';
    return;
  }

  logs.forEach((log) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${escapeHtml(getExerciseName(log.exerciseId))}</td>
      <td>${escapeHtml(log.inclinePct ?? 0)}</td>
      <td>${escapeHtml(log.durationM ?? 0)}</td>
      <td>${Number(log.caloriesOut ?? 0).toFixed(1)}</td>`;
    body.appendChild(row);
  });
}

function renderDietTable(logs) {
  const body = document.getElementById("diet-table-body");
  body.replaceChildren();

  if (!logs || logs.length === 0) {
    body.innerHTML = '<tr><td colspan="3" class="text-center text-muted">No meals logged yet.</td></tr>';
    return;
  }

  logs.forEach((log) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${escapeHtml(log.foodItem)}</td>
      <td>${escapeHtml(log.calories)}</td>
      <td>${escapeHtml(log.proteinG)}</td>`;
    body.appendChild(row);
  });
}

function renderSqlInsights(insights) {
  const fields = {
    "sql-bmr-display": insights?.sqlBmr == null ? "--" : `${Math.round(insights.sqlBmr)} kcal/day`,
    "weekly-calories-in": insights?.weeklySummary?.weeklyCaloriesIn == null ? "--" : `${Math.round(insights.weeklySummary.weeklyCaloriesIn)} kcal`,
    "weekly-cardio-out": insights?.weeklySummary?.weeklyCardioOut == null ? "--" : `${Math.round(insights.weeklySummary.weeklyCardioOut)} kcal`,
    "weekly-strength-sets": insights?.weeklySummary?.weeklyStrengthSets == null ? "--" : insights.weeklySummary.weeklyStrengthSets,
    "strength-volume-display": insights?.totalStrengthVolume == null ? "--" : `${Number(insights.totalStrengthVolume).toFixed(1)} kg·reps`
  };
  Object.entries(fields).forEach(([id, value]) => {
    const element = document.getElementById(id);
    if (element) element.textContent = value;
  });

  const body = document.getElementById("nutrition-view-body");
  body.replaceChildren();
  const dailyRows = insights?.dailyNutrition || [];
  if (dailyRows.length === 0) {
    body.innerHTML = '<tr><td colspan="3" class="text-center text-muted">No nutrition data in the SQL view yet.</td></tr>';
    return;
  }
  dailyRows.forEach((day) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${escapeHtml(day.logDate)}</td>
      <td>${escapeHtml(day.totalCaloriesIn)}</td>
      <td>${Number(day.totalProteinG ?? 0).toFixed(1)}</td>`;
    body.appendChild(row);
  });
}

function renderDietInsights(aboveAverageMeals, highCalorieDays) {
  const averageList = document.getElementById("above-avg-diet-list");
  averageList.replaceChildren();
  if (!aboveAverageMeals || aboveAverageMeals.length === 0) {
    averageList.innerHTML = '<li class="list-group-item text-muted small">No meals currently exceed your average meal calories.</li>';
  } else {
    aboveAverageMeals.forEach((meal) => {
      const item = document.createElement("li");
      item.className = "list-group-item small";
      item.innerHTML = `${escapeHtml(meal.foodItem)} — <strong>${escapeHtml(meal.calories)} kcal</strong> (${escapeHtml(meal.proteinG)} g protein)`;
      averageList.appendChild(item);
    });
  }

  const daysList = document.getElementById("high-cal-days-list");
  daysList.replaceChildren();
  if (!highCalorieDays || highCalorieDays.length === 0) {
    daysList.innerHTML = '<li class="list-group-item text-muted small">No days meet this calorie threshold.</li>';
  } else {
    highCalorieDays.forEach((day) => {
      const item = document.createElement("li");
      item.className = "list-group-item small";
      item.textContent = day;
      daysList.appendChild(item);
    });
  }
}

async function renderRoutines(routines, requestedUserId = currentUserId) {
  const select = document.getElementById("routine-select");
  const container = document.getElementById("routines-container");
  select.replaceChildren();
  container.replaceChildren();

  if (!routines || routines.length === 0) {
    select.innerHTML = '<option value="">Create a routine first</option>';
    container.innerHTML = '<p class="text-muted">No routines created yet.</p>';
    return;
  }

  select.disabled = false;
  const routineDetails = await Promise.all(routines.map(async (routine) => ({
    routine,
    exercises: await apiRequest(`/routines/${routine.routineId}/exercises`)
  })));

  // Prevent a slow routine-detail response from a previous profile switch
  // from overwriting the currently selected user's routines.
  if (Number(requestedUserId) !== Number(currentUserId)) return;

  routineDetails.forEach(({ routine, exercises }) => {
    const option = document.createElement("option");
    option.value = String(routine.routineId);
    option.textContent = routine.routineName;
    select.appendChild(option);

    const card = document.createElement("div");
    card.className = "card mb-3 border";
    const header = document.createElement("div");
    header.className = "card-header d-flex justify-content-between align-items-center gap-2";

    const title = document.createElement("strong");
    title.textContent = routine.routineName;
    const deleteButton = document.createElement("button");
    deleteButton.type = "button";
    deleteButton.className = "btn btn-sm btn-outline-danger";
    deleteButton.textContent = "Delete";
    deleteButton.addEventListener("click", () => deleteRoutine(routine.routineId));
    header.append(title, deleteButton);
    card.appendChild(header);

    const list = document.createElement("ul");
    list.className = "list-group list-group-flush";
    if (!exercises || exercises.length === 0) {
      list.innerHTML = '<li class="list-group-item text-muted small">No exercises mapped yet.</li>';
    } else {
      exercises.forEach((exercise) => {
        const item = document.createElement("li");
        item.className = "list-group-item small";
        if (exercise.type === "cardio") {
          item.textContent = `${exercise.name} [cardio] — ${exercise.defaultDur} min @ ${exercise.defaultIncl}% incline`;
        } else {
          item.textContent = `${exercise.name} [strength] — ${exercise.defaultSets} sets × ${exercise.defaultReps} reps`;
        }
        list.appendChild(item);
      });
    }
    card.appendChild(list);
    container.appendChild(card);
  });
}

// ---------------------------------------------------------------------------
// Cardio calorie estimate
// ---------------------------------------------------------------------------

function updateCardioEstimate() {
  const user = getActiveUser();
  const exerciseId = Number(document.getElementById("cardio-exercise-input").value);
  const duration = Number(document.getElementById("cardio-duration-input").value);
  const exercise = exercisesCache.find((item) => Number(item.exerciseId) === exerciseId);
  const output = document.getElementById("cardio-calories-input");

  if (!user || !exercise || !Number.isFinite(duration) || duration <= 0) {
    if (output) output.value = "";
    return;
  }

  // The same MET × body-weight × duration formula is also used by the backend.
  output.value = (Number(exercise.metValue) * Number(user.weightKg) * (duration / 60)).toFixed(1);
}

// ---------------------------------------------------------------------------
// Form handlers
// ---------------------------------------------------------------------------

function registerForm(id, handler) {
  const form = document.getElementById(id);
  if (!form) return;
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
      await handler(event, form);
    } catch (error) {
      console.error(`${id} failed:`, error);
      showStatus(error.message || "The request failed. Check the backend console.", "danger");
    }
  });
}

function registerEventHandlers() {
  document.getElementById("user-switcher").addEventListener("change", async (event) => {
    const nextId = Number.parseInt(event.target.value, 10);
    currentUserId = Number.isInteger(nextId) && nextId > 0 ? nextId : null;
    updateActiveUserBanner();
    updateCardioEstimate();
    await loadAllUserData();
  });

  document.getElementById("analytics-days").addEventListener("change", loadAllUserData);
  document.getElementById("refresh-high-cal-btn").addEventListener("click", loadAllUserData);
  document.getElementById("cardio-exercise-input").addEventListener("change", updateCardioEstimate);
  document.getElementById("cardio-duration-input").addEventListener("input", updateCardioEstimate);

  registerForm("add-user-form", async (event, form) => {
    const payload = {
      weightKg: Number(document.getElementById("new-user-weight").value),
      heightCm: Number(document.getElementById("new-user-height").value),
      age: Number.parseInt(document.getElementById("new-user-age").value, 10),
      gender: document.getElementById("new-user-gender").value
    };
    const result = await apiRequest("/users", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    currentUserId = Number(result.userId);
    form.reset();
    hideModal("addUserModal");
    await loadUsers();
    await loadAllUserData();
    showStatus(`Created User #${result.userId} (BMR: ${Math.round(result.bmr)} kcal/day).`);
  });

  registerForm("edit-weight-form", async (event, form) => {
    if (currentUserId === null) throw new Error("Create or select a user first.");
    const payload = {
      weightKg: Number(document.getElementById("edit-weight-input").value),
      heightCm: Number(document.getElementById("edit-height-input").value),
      age: Number.parseInt(document.getElementById("edit-age-input").value, 10),
      gender: document.getElementById("edit-gender-input").value
    };
    await apiRequest(`/users/${currentUserId}`, {
      method: "PUT",
      body: JSON.stringify(payload)
    });
    hideModal("editWeightModal");
    await loadUsers();
    await loadAllUserData();
    showStatus(`Updated User #${currentUserId} profile.`);
  });

  document.getElementById("delete-user-btn").addEventListener("click", async () => {
    if (currentUserId === null) return;
    if (!window.confirm(`Delete User #${currentUserId} and all their logs?`)) return;
    try {
      const deletedId = currentUserId;
      await apiRequest(`/users/${deletedId}`, { method: "DELETE" });
      currentUserId = null;
      await loadUsers();
      if (currentUserId !== null) await loadAllUserData();
      showStatus(`User #${deletedId} deleted.`, "warning");
    } catch (error) {
      showStatus(error.message || "Unable to delete this user.", "danger");
    }
  });

  registerForm("log-workout-form", async (event, form) => {
    if (currentUserId === null) throw new Error("Create or select a user first.");
    const payload = {
      userId: currentUserId,
      exerciseId: Number.parseInt(document.getElementById("exercise-input").value, 10),
      weightKg: Number(document.getElementById("weight-input").value),
      reps: Number.parseInt(document.getElementById("reps-input").value, 10)
    };
    await apiRequest("/workouts", { method: "POST", body: JSON.stringify(payload) });
    form.reset();
    await loadAllUserData();
    showStatus("Strength workout saved.");
  });

  registerForm("log-cardio-form", async (event, form) => {
    if (currentUserId === null) throw new Error("Create or select a user first.");
    const payload = {
      userId: currentUserId,
      exerciseId: Number.parseInt(document.getElementById("cardio-exercise-input").value, 10),
      inclinePct: Number(document.getElementById("cardio-incline-input").value),
      durationM: Number.parseInt(document.getElementById("cardio-duration-input").value, 10),
      caloriesOut: Number(document.getElementById("cardio-calories-input").value || 0)
    };
    await apiRequest("/cardio", { method: "POST", body: JSON.stringify(payload) });
    form.reset();
    document.getElementById("cardio-incline-input").value = "0";
    await loadAllUserData();
    showStatus("Cardio session saved.");
  });

  registerForm("log-diet-form", async (event, form) => {
    if (currentUserId === null) throw new Error("Create or select a user first.");
    const payload = {
      userId: currentUserId,
      foodItem: document.getElementById("diet-food-input").value.trim(),
      calories: Number.parseInt(document.getElementById("diet-calories-input").value, 10),
      proteinG: Number(document.getElementById("diet-protein-input").value)
    };
    await apiRequest("/diet", { method: "POST", body: JSON.stringify(payload) });
    form.reset();
    await loadAllUserData();
    showStatus("Meal logged.");
  });

  registerForm("create-routine-form", async (event, form) => {
    if (currentUserId === null) throw new Error("Create or select a user first.");
    const payload = {
      userId: currentUserId,
      routineName: document.getElementById("routine-name-input").value.trim()
    };
    await apiRequest("/routines", { method: "POST", body: JSON.stringify(payload) });
    form.reset();
    await loadAllUserData();
    showStatus("Routine created.");
  });

  registerForm("add-routine-exercise-form", async () => {
    if (currentUserId === null) throw new Error("Create or select a user first.");
    const routineId = Number.parseInt(document.getElementById("routine-select").value, 10);
    if (!Number.isInteger(routineId) || routineId <= 0) throw new Error("Create or select a routine first.");
    const payload = {
      exerciseId: Number.parseInt(document.getElementById("routine-exercise-select").value, 10),
      defaultSets: Number.parseInt(document.getElementById("routine-sets").value, 10) || 0,
      defaultReps: Number.parseInt(document.getElementById("routine-reps").value, 10) || 0,
      defaultIncl: Number(document.getElementById("routine-incline").value) || 0,
      defaultDur: Number.parseInt(document.getElementById("routine-duration").value, 10) || 0
    };
    await apiRequest(`/routines/${routineId}/exercises`, {
      method: "POST",
      body: JSON.stringify(payload)
    });
    await loadAllUserData();
    showStatus("Exercise added to routine.");
  });

  registerForm("add-exercise-form", async (event, form) => {
    if (currentUserId === null) throw new Error("Create or select a user first.");
    const payload = {
      name: document.getElementById("custom-ex-name").value.trim(),
      type: document.getElementById("custom-ex-type").value,
      metValue: Number(document.getElementById("custom-ex-met").value),
      createdByUid: currentUserId
    };
    await apiRequest("/exercises", { method: "POST", body: JSON.stringify(payload) });
    form.reset();
    await loadExercises();
    await loadAllUserData();
    showStatus("Custom exercise added to the dictionary.");
  });
}

async function deleteRoutine(routineId) {
  if (!window.confirm("Delete this routine and all its exercise mappings?")) return;
  try {
    await apiRequest(`/routines/${routineId}`, { method: "DELETE" });
    await loadAllUserData();
    showStatus("Routine deleted.", "warning");
  } catch (error) {
    showStatus(error.message || "Unable to delete routine.", "danger");
  }
}

registerEventHandlers();
initApp();
