const state = {
  selectedTaskId: null,
};

async function request(url, options = {}) {
  const response = await fetch(url, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
    ...options,
  });
  const data = await response.json();
  if (!response.ok || !data.success) {
    throw new Error(data.message || '请求失败');
  }
  return data.data;
}

function toast(message) {
  const node = document.createElement('div');
  node.className = 'toast';
  node.textContent = message;
  document.body.appendChild(node);
  setTimeout(() => node.remove(), 2400);
}

function summaryCard(label, value) {
  return `
    <article class="summary-card">
      <div class="eyebrow">${label}</div>
      <strong>${value}</strong>
    </article>
  `;
}

function renderSummary(summary) {
  document.getElementById('summaryCards').innerHTML = [
    summaryCard('热点总数', summary.hotspotCount),
    summaryCard('任务总数', summary.taskCount),
    summaryCard('待审核', summary.reviewPendingCount),
    summaryCard('已发布', summary.publishedCount),
  ].join('');
}

function renderHotspots(hotspots) {
  document.getElementById('hotspotList').innerHTML = hotspots.map((item) => `
    <article class="hotspot-card">
      <div class="badge">${item.source}</div>
      <h4>${item.title}</h4>
      <div class="meta">热度 ${item.score ?? '-'}</div>
      <p>${item.summary ?? ''}</p>
    </article>
  `).join('');
}

function renderTasks(tasks) {
  document.getElementById('taskList').innerHTML = tasks.map((task) => `
    <article class="task-card">
      <h4>${task.name}</h4>
      <div class="task-meta">话题：${task.selectedTopic ?? '-'}</div>
      <div class="task-meta">状态：${task.status} / 审核：${task.reviewStatus} / 发布：${task.publishStatus}</div>
      <div class="task-meta">平台：${(task.targetPlatforms || []).join(', ')}</div>
      <button class="action secondary inline" onclick="loadTaskDetail(${task.id})">查看详情</button>
    </article>
  `).join('');
}

function renderTaskDetail(detail) {
  state.selectedTaskId = detail.id;
  const assetHtml = (detail.assets || []).map((asset) => `
    <article class="asset-card">
      <div class="badge">${asset.assetType}</div>
      <h4>${asset.fileName}</h4>
      <div class="asset-meta">路径：${asset.fileUrl}</div>
      <div class="asset-meta">状态：${asset.status}</div>
    </article>
  `).join('');

  const publishHtml = (detail.publishRecords || []).length
    ? detail.publishRecords.map((record) => `
      <article class="publish-card">
        <div class="badge">${record.platform}</div>
        <h4>${record.status}</h4>
        <div class="asset-meta">内容ID：${record.platformContentId || '-'}</div>
        <p>${record.responseMessage || ''}</p>
      </article>
    `).join('')
    : '<div class="empty-state">暂无发布记录</div>';

  document.getElementById('taskDetail').innerHTML = `
    <div class="detail-layout">
      <div class="detail-grid">
        <section class="detail-card">
          <div class="badge">${detail.status}</div>
          <div class="badge">${detail.reviewStatus}</div>
          <div class="badge">${detail.publishStatus}</div>
          <h4>${detail.name}</h4>
          <p class="meta">批次：${detail.batchNo} | 当前步骤：${detail.currentStep}</p>
          <p>选题：${detail.selectedTopic || '-'}</p>
          <p>平台：${(detail.targetPlatforms || []).join(', ')}</p>
          <p>备注：${detail.errorMessage || '暂无'}</p>
          <div>
            <button class="action primary inline" onclick="approveTask()">审核通过</button>
            <button class="action secondary inline" onclick="rejectTask()">审核驳回</button>
            <button class="action secondary inline" onclick="publishTask()">触发发布</button>
          </div>
        </section>
        <section class="detail-card">
          <h4>脚本内容</h4>
          <p><strong>标题：</strong>${detail.script?.title || '-'}</p>
          <p><strong>开场：</strong>${detail.script?.introHook || '-'}</p>
          <ol class="segments">${(detail.script?.segments || []).map((segment) => `<li>${segment}</li>`).join('')}</ol>
          <p><strong>结尾：</strong>${detail.script?.closingCta || '-'}</p>
          <p><strong>标签：</strong>${(detail.script?.tags || []).join(' / ')}</p>
          <p><strong>时长：</strong>${detail.script?.estimatedDurationSec || '-'} 秒</p>
          <p><strong>音色：</strong>${detail.script?.voiceTone || '-'}</p>
          <p><strong>配图提示词：</strong>${detail.script?.imagePrompt || '-'}</p>
        </section>
      </div>
      <div class="detail-grid">
        <section class="detail-card">
          <h4>媒体资产</h4>
          <div class="assets-grid">${assetHtml || '<div class="empty-state">暂无资产</div>'}</div>
        </section>
        <section class="detail-card">
          <h4>发布记录</h4>
          <div class="publish-grid">${publishHtml}</div>
        </section>
      </div>
    </div>
  `;
}

async function loadSummary() {
  renderSummary(await request('/api/dashboard/summary'));
}

async function loadHotspots() {
  renderHotspots(await request('/api/hotspots'));
}

async function loadTasks() {
  const tasks = await request('/api/tasks');
  renderTasks(tasks);
  if (state.selectedTaskId) {
    const existing = tasks.find((item) => item.id === state.selectedTaskId);
    if (existing) {
      await loadTaskDetail(state.selectedTaskId);
    }
  }
}

async function loadTaskDetail(taskId) {
  const detail = await request(`/api/tasks/${taskId}`);
  renderTaskDetail(detail);
}

async function refreshAll() {
  await Promise.all([loadSummary(), loadHotspots(), loadTasks()]);
}

async function collectHotspots() {
  await request('/api/hotspots/collect', { method: 'POST' });
  toast('热点抓取完成');
  await refreshAll();
}

async function createTask(event) {
  event.preventDefault();
  const form = event.target;
  const platforms = [...form.querySelector('#targetPlatforms').selectedOptions].map((option) => option.value);
  const payload = {
    taskName: form.taskName.value,
    accountPositioning: form.accountPositioning.value,
    preferredTopic: form.preferredTopic.value,
    targetPlatforms: platforms.length ? platforms : ['LOCAL_SIMULATION'],
  };
  const detail = await request('/api/tasks', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  form.reset();
  toast('任务创建成功');
  await refreshAll();
  renderTaskDetail(detail);
}

async function approveTask() {
  if (!state.selectedTaskId) return;
  const detail = await request(`/api/tasks/${state.selectedTaskId}/approve`, {
    method: 'POST',
    body: JSON.stringify({ comment: '前端审核通过' }),
  });
  toast('任务已通过审核');
  renderTaskDetail(detail);
  await refreshAll();
}

async function rejectTask() {
  if (!state.selectedTaskId) return;
  const detail = await request(`/api/tasks/${state.selectedTaskId}/reject`, {
    method: 'POST',
    body: JSON.stringify({ comment: '前端审核驳回，请调整选题或脚本' }),
  });
  toast('任务已驳回');
  renderTaskDetail(detail);
  await refreshAll();
}

async function publishTask() {
  if (!state.selectedTaskId) return;
  const detail = await request(`/api/tasks/${state.selectedTaskId}/publish`, {
    method: 'POST',
    body: JSON.stringify({}),
  });
  toast('发布流程执行完成');
  renderTaskDetail(detail);
  await refreshAll();
}

document.getElementById('taskForm').addEventListener('submit', (event) => {
  createTask(event).catch((error) => toast(error.message));
});

document.getElementById('collectHotspotsBtn').addEventListener('click', () => {
  collectHotspots().catch((error) => toast(error.message));
});

document.getElementById('refreshBtn').addEventListener('click', () => {
  refreshAll().catch((error) => toast(error.message));
});

window.loadTaskDetail = (taskId) => {
  loadTaskDetail(taskId).catch((error) => toast(error.message));
};

window.approveTask = () => approveTask().catch((error) => toast(error.message));
window.rejectTask = () => rejectTask().catch((error) => toast(error.message));
window.publishTask = () => publishTask().catch((error) => toast(error.message));

refreshAll().catch((error) => toast(error.message));
