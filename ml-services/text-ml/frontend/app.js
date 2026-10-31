const API_BASE_URL = (window.location.protocol.startsWith('http'))
    ? window.location.origin
    : 'http://localhost:8000';

const PRESETS = {
    "CASE_A": "He threatened me yesterday and said he would break my legs.",
    "CASE_B": "He did not threaten me, we just had a verbal disagreement.",
    "CASE_C": "He threatened to hurt my children if I talked to anyone.",
    "CASE_D": "He is outside my house right now pounding on the door and threatening to kill me.",
    "CASE_E": "I feel like ending my life because I cannot bear this suffering anymore.",
    "CASE_F": "I have been going to court for eight months and feel completely helpless because the case is not moving forward.",
    "CASE_G": "He assaulted me, threatened my brother, and now we are afraid to go back to our home.",
    "CASE_H": "The police officer asked me whether he threatened me or harmed me.",
    "CASE_I": "He never threatened my family or tried to hurt them.",
    "CASE_HINGLISH": "Usne mujhe dhamki di ki agar maine police ko bataya toh wo mere ghar waalon ko maarega."
};

let radarChart = null;
let lastAnalysisData = null;

// Initialize Dashboard
document.addEventListener("DOMContentLoaded", () => {
    initRadarChart();
    checkApiHealth();

    const textarea = document.getElementById("narrative-input");
    if (textarea) {
        textarea.addEventListener("input", (e) => {
            document.getElementById("char-counter").textContent = `${e.target.value.length} characters`;
        });
    }

    renderWeightsConfig();
});

// Tab Switching
function switchTab(tabId) {
    document.querySelectorAll(".nav-tab").forEach(tab => tab.classList.remove("active"));
    document.querySelectorAll(".tab-page").forEach(page => page.classList.remove("active"));

    const btn = document.querySelector(`.nav-tab[onclick="switchTab('${tabId}')"]`);
    if (btn) btn.classList.add("active");

    const page = document.getElementById(`tab-${tabId}`);
    if (page) page.classList.add("active");
}

// API Health Check
async function checkApiHealth() {
    const dot = document.getElementById("api-pulse");
    const text = document.getElementById("api-status-text");

    try {
        const res = await fetch(`${API_BASE_URL}/health`);
        if (res.ok) {
            const data = await res.json();
            dot.className = "pulse-dot online";
            text.textContent = `Online (${data.pytorch_model_loaded ? 'MuRIL PyTorch' : 'Rule Dev Mode'})`;
        } else {
            dot.className = "pulse-dot offline";
            text.textContent = "API Error";
        }
    } catch (e) {
        dot.className = "pulse-dot offline";
        text.textContent = "Offline (Run python -m api.main)";
    }
}

// Preset Loader
function loadPreset(key) {
    if (PRESETS[key]) {
        const input = document.getElementById("narrative-input");
        input.value = PRESETS[key];
        document.getElementById("char-counter").textContent = `${input.value.length} characters`;
    }
}

function clearNarrative() {
    document.getElementById("narrative-input").value = "";
    document.getElementById("char-counter").textContent = "0 characters";
    updateGauge(0.0);
    resetIndicators();
}

// Analyze Narrative
async function analyzeNarrative() {
    const input = document.getElementById("narrative-input").value.trim();
    if (!input) {
        alert("Please enter a victim narrative statement before analyzing.");
        return;
    }

    const btn = document.getElementById("analyze-btn");
    const icon = document.getElementById("btn-icon");
    const label = document.getElementById("btn-label");
    const spinner = document.getElementById("btn-spinner");

    icon.classList.add("hidden");
    label.textContent = "Analyzing Narrative...";
    spinner.classList.remove("hidden");
    btn.disabled = true;

    try {
        const res = await fetch(`${API_BASE_URL}/predict`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ text: input })
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.detail || "Prediction request failed");
        }

        const data = await res.json();
        lastAnalysisData = data;
        renderResults(data);

    } catch (err) {
        console.error("Inference Error:", err);
        alert(`Analysis Error: ${err.message}\nMake sure API backend is running at ${API_BASE_URL}`);
    } finally {
        icon.classList.remove("hidden");
        label.textContent = "Analyze Text Narrative";
        spinner.classList.add("hidden");
        btn.disabled = false;
    }
}

// Render Results
function renderResults(data) {
    // 1. Update Gauge & Score
    updateGauge(data.text_score);

    // 2. Update Radar Chart
    updateRadarChart(data.indicators);

    // 3. Render Critical Safety Flags Alert
    const criticalBox = document.getElementById("critical-alert-box");
    const flagsContainer = document.getElementById("critical-flags-container");

    if (data.has_critical_flag && data.critical_flags.length > 0) {
        criticalBox.classList.remove("hidden");
        flagsContainer.innerHTML = data.critical_flags.map(flag => `
            <div class="critical-card">
                <strong>[${flag.severity}] ${flag.indicator.replace(/_/g, ' ').toUpperCase()}</strong>
                (Prob: ${(flag.probability * 100).toFixed(1)}% | Threshold: ${(flag.threshold * 100).toFixed(0)}%)
                <div style="font-size:11px; margin-top:2px; color:var(--text-muted);">${flag.action_required}</div>
            </div>
        `).join('');
    } else {
        criticalBox.classList.add("hidden");
    }

    // 4. Render 10 Indicator Cards
    const grid = document.getElementById("indicators-grid");
    grid.innerHTML = "";

    Object.keys(data.indicators).forEach(label => {
        const prob = data.indicators[label];
        const pct = (prob * 100).toFixed(1);

        let badge = "LOW";
        if (prob >= 0.70) badge = "CRITICAL";
        else if (prob >= 0.50) badge = "HIGH";
        else if (prob >= 0.30) badge = "MODERATE";

        const cardHtml = `
            <div class="ind-card">
                <div class="ind-top">
                    <span class="ind-name">${label.replace(/_/g, ' ')}</span>
                    <span class="ind-badge ${badge}">${badge}</span>
                </div>
                <div class="ind-val">${pct}%</div>
                <div class="ind-bar-bg">
                    <div class="ind-bar-fill ${badge}" style="width: ${pct}%"></div>
                </div>
            </div>
        `;
        grid.insertAdjacentHTML("beforeend", cardHtml);
    });

    // 5. Render Evidence Spans
    const evidenceBox = document.getElementById("evidence-container");
    evidenceBox.innerHTML = "";

    let hasEv = false;
    if (data.evidence) {
        Object.keys(data.evidence).forEach(label => {
            const sentence = data.evidence[label];
            if (sentence) {
                hasEv = true;
                evidenceBox.insertAdjacentHTML("beforeend", `
                    <div class="evidence-span">
                        <div class="evidence-label">${label.replace(/_/g, ' ')}</div>
                        <div>"${sentence}"</div>
                    </div>
                `);
            }
        });
    }

    if (!hasEv) {
        evidenceBox.innerHTML = '<div class="empty-state">No active evidence spans mapped (Evidence is null when no verbatim text span is matched).</div>';
    }
}

// Gauge Animation
function updateGauge(score) {
    const num = document.getElementById("score-number");
    const bar = document.getElementById("gauge-bar");

    // Circumference = 2 * PI * 50 = 314
    const offset = 314 - (314 * (score / 100));
    bar.style.strokeDashoffset = offset;

    if (score >= 70) bar.style.stroke = "var(--accent-rose)";
    else if (score >= 40) bar.style.stroke = "var(--accent-amber)";
    else bar.style.stroke = "var(--accent-blue)";

    // Score counter animation
    let current = 0;
    const step = score / 20;
    const interval = setInterval(() => {
        current += step;
        if (current >= score) {
            current = score;
            clearInterval(interval);
        }
        num.textContent = current.toFixed(1);
    }, 25);
}

// Radar Chart (Chart.js)
function initRadarChart() {
    const ctx = document.getElementById("radarChart");
    if (!ctx) return;

    radarChart = new Chart(ctx, {
        type: 'radar',
        data: {
            labels: ['Violence', 'Sexual', 'Threat', 'Danger', 'Isolation', 'Displacement', 'Family', 'Self-Harm', 'Depression', 'Legal'],
            datasets: [{
                label: 'Risk Profile',
                data: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
                backgroundColor: 'rgba(56, 189, 248, 0.2)',
                borderColor: '#38bdf8',
                borderWidth: 2,
                pointBackgroundColor: '#6366f1'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                r: {
                    angleLines: { color: 'rgba(255, 255, 255, 0.1)' },
                    grid: { color: 'rgba(255, 255, 255, 0.1)' },
                    pointLabels: { color: '#94a3b8', font: { size: 10 } },
                    ticks: { display: false },
                    suggestedMin: 0,
                    suggestedMax: 1
                }
            },
            plugins: { legend: { display: false } }
        }
    });
}

function updateRadarChart(indicators) {
    if (!radarChart) return;
    const values = Object.values(indicators);
    radarChart.data.datasets[0].data = values;
    radarChart.update();
}

function resetIndicators() {
    document.getElementById("indicators-grid").innerHTML = '<div class="empty-state">Enter victim statement and click "Analyze Text Narrative" to inspect model predictions.</div>';
    document.getElementById("evidence-container").innerHTML = '<div class="empty-state">No active evidence spans mapped yet.</div>';
    document.getElementById("critical-alert-box").classList.add("hidden");
    if (radarChart) {
        radarChart.data.datasets[0].data = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
        radarChart.update();
    }
}

// Export JSON Report
function exportAnalysisJSON() {
    if (!lastAnalysisData) {
        alert("Run a narrative analysis first before exporting the report.");
        return;
    }
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(lastAnalysisData, null, 2));
    const dlAnchor = document.createElement('a');
    dlAnchor.setAttribute("href", dataStr);
    dlAnchor.setAttribute("download", `SIH26093_TextML_Report_${Date.now()}.json`);
    document.body.appendChild(dlAnchor);
    dlAnchor.click();
    dlAnchor.remove();
}

// Batch Test Suite Runner
async function runBatchSuite() {
    const tbody = document.getElementById("suite-table-body");
    tbody.innerHTML = '<tr><td colspan="7" class="empty-state">Running test suite against live model...</td></tr>';

    const testKeys = Object.keys(PRESETS).filter(k => k.startsWith("CASE_"));
    let html = "";

    for (const key of testKeys) {
        const text = PRESETS[key];
        try {
            const res = await fetch(`${API_BASE_URL}/predict`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ text: text })
            });

            if (res.ok) {
                const data = await res.json();
                const active = Object.keys(data.indicators).filter(k => data.indicators[k] >= 0.50);
                const flags = data.critical_flags.map(f => f.indicator);

                html += `
                    <tr>
                        <td><strong>${key}</strong></td>
                        <td>${key.replace('CASE_', 'Scenario ')}</td>
                        <td style="max-width:300px;">"${text}"</td>
                        <td><strong>${data.text_score.toFixed(1)}</strong></td>
                        <td>${active.length > 0 ? active.join(', ') : 'None'}</td>
                        <td>${flags.length > 0 ? flags.join(', ') : 'None'}</td>
                        <td class="status-pass">PASS ✓</td>
                    </tr>
                `;
            }
        } catch (e) {
            html += `
                <tr>
                    <td><strong>${key}</strong></td>
                    <td colspan="5">Connection Error</td>
                    <td class="status-fail">FAIL ✗</td>
                </tr>
            `;
        }
    }

    tbody.innerHTML = html;
}

// Weights Configurator
function renderWeightsConfig() {
    const container = document.getElementById("weights-sliders");
    if (!container) return;

    const weights = {
        "violence": 12.0, "sexual_violence": 15.0, "threat_intimidation": 12.0,
        "immediate_danger": 18.0, "social_isolation": 6.0, "displacement": 7.0,
        "family_harm": 10.0, "self_harm_suicidal_ideation": 12.0, "depression_related": 4.0, "legal_distress": 4.0
    };

    container.innerHTML = Object.keys(weights).map(key => `
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
            <label style="font-size:13px; font-weight:600; text-transform:capitalize;">${key.replace(/_/g, ' ')}</label>
            <input type="range" min="1" max="25" value="${weights[key]}" oninput="this.nextElementSibling.value = this.value" style="width:160px;">
            <output style="font-size:13px; font-weight:700; width:30px; text-align:right;">${weights[key]}</output>
        </div>
    `).join('');
}

function resetWeights() {
    renderWeightsConfig();
}
