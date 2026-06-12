/* eslint-disable no-undef */
// Loads history.json from same directory and renders three time-series charts —
// one per role (code/tests/docs), with one line per module within each chart.
// The metric (loc, files, complexity) is switchable via the radio buttons in
// the header; charts re-render in-place.

const MODULE_COLORS = {
  'sangeet-core': '#8B1A1A', // deep red
  'sangeet-desktop': '#1F6B5C', // teal
  'sangeet-server': '#2E5D8A', // blue
  'sangeet-web': '#B07A3E', // amber
  e2e: '#6A3E80', // purple
  docs: '#4A6B2E', // olive
};

const ROLES = ['code', 'tests', 'docs'];

const charts = {}; // role → Chart instance

function fmt(n) {
  return new Intl.NumberFormat().format(n);
}

function buildDatasets(history, role, metric) {
  // For each module that has any non-zero value in this role across the history,
  // build a {x: ts, y: value} time series. Skip modules whose entire series is
  // zero — keeps the legend uncluttered for charts like "Code" where e2e/docs
  // are always 0.
  const modules = Object.keys(MODULE_COLORS).filter((m) =>
    history.snapshots.some((s) => (s.modules?.[m]?.[role]?.[metric] ?? 0) > 0),
  );

  return modules.map((m) => ({
    label: m,
    borderColor: MODULE_COLORS[m],
    backgroundColor: MODULE_COLORS[m],
    pointRadius: 2,
    pointHoverRadius: 4,
    tension: 0.15,
    data: history.snapshots.map((s) => ({
      x: s.timestamp,
      y: s.modules?.[m]?.[role]?.[metric] ?? 0,
    })),
  }));
}

function renderChart(role, history, metric) {
  const ctx = document.getElementById(`chart-${role}`).getContext('2d');
  const datasets = buildDatasets(history, role, metric);

  if (charts[role]) {
    charts[role].data.datasets = datasets;
    charts[role].update();
    return;
  }

  charts[role] = new Chart(ctx, {
    type: 'line',
    data: { datasets },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'nearest', intersect: false },
      scales: {
        x: {
          type: 'time',
          time: { tooltipFormat: 'yyyy-MM-dd HH:mm' },
          ticks: { maxRotation: 0, autoSkipPadding: 24, color: '#5a4d3f' },
          grid: { color: 'rgba(0,0,0,0.04)' },
        },
        y: {
          beginAtZero: true,
          ticks: { color: '#5a4d3f', callback: (v) => fmt(v) },
          grid: { color: 'rgba(0,0,0,0.06)' },
        },
      },
      plugins: {
        legend: { position: 'bottom', labels: { boxWidth: 12, font: { size: 11 } } },
        tooltip: {
          callbacks: {
            label: (ctx) => `${ctx.dataset.label}: ${fmt(ctx.parsed.y)}`,
          },
        },
      },
    },
  });
}

function renderLatest(history) {
  if (!history.snapshots.length) {
    document.getElementById('latest-meta').textContent = 'No snapshots yet.';
    return;
  }
  const latest = history.snapshots[history.snapshots.length - 1];
  document.getElementById('latest-meta').innerHTML =
    `<code>${latest.sha.slice(0, 8)}</code> · ${latest.timestamp} · ${escapeHtml(latest.subject ?? '')}`;

  const tbody = document.querySelector('#latest-table tbody');
  tbody.innerHTML = '';
  for (const m of Object.keys(MODULE_COLORS)) {
    const row = latest.modules?.[m];
    if (!row) continue;
    const tr = document.createElement('tr');
    tr.innerHTML =
      `<td>${m}</td>` +
      `<td>${fmt(row.code?.loc ?? 0)} loc / ${row.code?.files ?? 0} files</td>` +
      `<td>${fmt(row.tests?.loc ?? 0)} loc / ${row.tests?.files ?? 0} files</td>` +
      `<td>${fmt(row.docs?.loc ?? 0)} loc / ${row.docs?.files ?? 0} files</td>`;
    tbody.appendChild(tr);
  }
}

function escapeHtml(s) {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function selectedMetric() {
  return document.querySelector('input[name="metric"]:checked').value;
}

async function main() {
  let history;
  try {
    const res = await fetch('history.json', { cache: 'no-cache' });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    history = await res.json();
  } catch (err) {
    document.querySelector('main').innerHTML =
      `<section class="chart-card"><h2>Could not load history.json</h2>` +
      `<p class="muted">${escapeHtml(String(err))}</p></section>`;
    return;
  }

  for (const r of ROLES) renderChart(r, history, selectedMetric());
  renderLatest(history);

  for (const radio of document.querySelectorAll('input[name="metric"]')) {
    radio.addEventListener('change', () => {
      const m = selectedMetric();
      for (const r of ROLES) renderChart(r, history, m);
    });
  }
}

main();
