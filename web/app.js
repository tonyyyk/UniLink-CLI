/* ═══════════════════════════════════════════════════════════════════════
   UniLink — Frontend SPA
   All API calls, page routing, and UI logic in one self-contained file.
   ═══════════════════════════════════════════════════════════════════════ */

const API = '';   // same origin — empty prefix

// ── Session state ─────────────────────────────────────────────────────────
let token    = localStorage.getItem('unilink_token')    || null;
let username = localStorage.getItem('unilink_username') || null;
let userRole = localStorage.getItem('unilink_role')     || null;
let currentConvPartner = null;
let convRefreshInterval = null;
let notifyInterval      = null;

// ══════════════════════════════════════════════════════════════ API HELPERS ══

async function apiCall(method, path, body) {
  const opts = {
    method,
    headers: { 'Content-Type': 'application/json' }
  };
  if (token) opts.headers['Authorization'] = 'Bearer ' + token;
  if (body)  opts.body = JSON.stringify(body);
  try {
    const res = await fetch(API + path, opts);
    const data = await res.json().catch(() => ({}));
    return { ok: res.ok, status: res.status, data };
  } catch (err) {
    return { ok: false, status: 0, data: { error: 'Network error' } };
  }
}

const get  = (path)        => apiCall('GET',  path);
const post = (path, body)  => apiCall('POST', path, body);
const put  = (path, body)  => apiCall('PUT',  path, body);

// ══════════════════════════════════════════════════════════════════ TOASTS ══

function toast(title, msg, type = 'info') {
  const el = document.createElement('div');
  el.className = 'toast ' + type;
  el.innerHTML = `<div class="toast-title">${esc(title)}</div><div class="toast-msg">${esc(msg)}</div>`;
  document.getElementById('toast-container').appendChild(el);
  setTimeout(() => el.remove(), 4000);
}

function esc(s) {
  return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

// ════════════════════════════════════════════════════════════════ ROUTING ══

function showPage(id) {
  ['page-login','page-register','page-app'].forEach(p => {
    document.getElementById(p).classList.toggle('hidden', p !== id);
  });
}

function showSection(name) {
  document.querySelectorAll('.page-section').forEach(s => s.classList.add('hidden'));
  document.getElementById('section-' + name).classList.remove('hidden');
  document.querySelectorAll('.nav-link').forEach(l => {
    l.classList.toggle('active', l.dataset.page === name);
  });
}

// ════════════════════════════════════════════════════════════════════ AUTH ══

document.getElementById('login-form').addEventListener('submit', async e => {
  e.preventDefault();
  const user = document.getElementById('login-username').value.trim();
  const pass = document.getElementById('login-password').value;
  const r = await post('/api/auth/login', { username: user, password: pass });
  if (!r.ok) {
    showAlert('login-error', r.data.error || 'Login failed');
    return;
  }
  token    = r.data.token;
  username = r.data.username;
  userRole = r.data.role;
  localStorage.setItem('unilink_token',    token);
  localStorage.setItem('unilink_username', username);
  localStorage.setItem('unilink_role',     userRole);
  enterApp();
});

document.getElementById('register-form').addEventListener('submit', async e => {
  e.preventDefault();
  const user  = document.getElementById('reg-username').value.trim();
  const pass  = document.getElementById('reg-password').value;
  const major = document.getElementById('reg-major').value.trim();
  const r = await post('/api/auth/register', { username: user, password: pass, major });
  if (!r.ok) {
    showAlert('register-error', r.data.error || 'Registration failed');
    return;
  }
  hideAlert('register-error');
  document.getElementById('register-form').reset();
  showPage('page-login');
  showAlert('login-success', 'Account created! Please log in.');
});

document.getElementById('goto-register').addEventListener('click', e => {
  e.preventDefault();
  showPage('page-register');
});
document.getElementById('goto-login').addEventListener('click', e => {
  e.preventDefault();
  showPage('page-login');
});

document.getElementById('logout-btn').addEventListener('click', async () => {
  await post('/api/auth/logout');
  clearSession();
  showPage('page-login');
});

function clearSession() {
  token = username = userRole = null;
  localStorage.removeItem('unilink_token');
  localStorage.removeItem('unilink_username');
  localStorage.removeItem('unilink_role');
  currentConvPartner = null;
  clearInterval(convRefreshInterval);
  clearInterval(notifyInterval);
}

// ════════════════════════════════════════════════════════════════ ENTER APP ══

async function enterApp() {
  showPage('page-app');

  // Set up nav
  const adminNav = document.getElementById('admin-nav-item');
  adminNav.classList.toggle('hidden', userRole !== 'ADMIN');

  // Load profile section first
  await loadProfile();
  showSection('profile');

  // Start notification polling
  startNotifyPolling();
}

// ════════════════════════════════════════════════════════════════ PROFILE ══

async function loadProfile() {
  const r = await get('/api/profile');
  if (!r.ok) return;
  const p = r.data;

  // Update nav
  document.getElementById('nav-username').textContent = p.username;
  document.getElementById('nav-avatar').textContent   = p.username.charAt(0).toUpperCase();
  const statusBadge = document.getElementById('nav-status');
  statusBadge.textContent = p.status;
  statusBadge.className   = 'status-badge ' + p.status;

  // Update profile view
  document.getElementById('profile-avatar-big').textContent = p.username.charAt(0).toUpperCase();
  document.getElementById('pv-username').textContent = p.username;
  document.getElementById('pv-major').textContent    = p.major || '—';
  document.getElementById('pv-role').textContent     = p.role;
  const pvStatus = document.getElementById('pv-status');
  pvStatus.textContent = p.status;
  pvStatus.className   = 'status-badge ' + p.status;

  // Strengths
  const strengthsEl = document.getElementById('pv-strengths');
  strengthsEl.innerHTML = p.strengths && p.strengths.length
    ? p.strengths.map(s => `<span class="tag">${esc(s)}</span>`).join('')
    : '<span class="tag-empty">None listed</span>';

  // Weaknesses
  const weaknessesEl = document.getElementById('pv-weaknesses');
  weaknessesEl.innerHTML = p.weaknesses && p.weaknesses.length
    ? p.weaknesses.map(w => `<span class="tag">${esc(w)}</span>`).join('')
    : '<span class="tag-empty">None listed</span>';

  // Capabilities
  const caps = [
    { label: 'Can send messages',    ok: p.canSendMessage },
    { label: 'Appears in searches',  ok: p.canAppearInSearch },
    { label: 'Can update profile',   ok: true }
  ];
  document.getElementById('pv-capabilities').innerHTML = caps.map(c =>
    `<div class="capability-item">
       <span class="cap-dot ${c.ok ? 'yes' : 'no'}"></span>
       <span>${c.label}</span>
     </div>`
  ).join('');

  // Pre-fill edit form
  document.getElementById('edit-major').value     = p.major || '';
  document.getElementById('edit-strengths').value  = (p.strengths  || []).join(', ');
  document.getElementById('edit-weaknesses').value = (p.weaknesses || []).join(', ');
}

// Edit / Save Profile
document.getElementById('edit-profile-btn').addEventListener('click', () => {
  document.getElementById('profile-view').classList.add('hidden');
  document.getElementById('profile-edit').classList.remove('hidden');
  document.getElementById('edit-profile-btn').classList.add('hidden');
});

document.getElementById('cancel-edit-btn').addEventListener('click', () => {
  document.getElementById('profile-view').classList.remove('hidden');
  document.getElementById('profile-edit').classList.add('hidden');
  document.getElementById('edit-profile-btn').classList.remove('hidden');
  hideAlert('profile-edit-error');
});

document.getElementById('save-profile-btn').addEventListener('click', async () => {
  const major     = document.getElementById('edit-major').value.trim();
  const strengths = document.getElementById('edit-strengths').value
                      .split(',').map(s => s.trim()).filter(Boolean);
  const weaknesses = document.getElementById('edit-weaknesses').value
                       .split(',').map(s => s.trim()).filter(Boolean);

  const r = await put('/api/profile', { major, strengths, weaknesses });
  if (!r.ok) {
    showAlert('profile-edit-error', r.data.error || 'Update failed');
    return;
  }
  hideAlert('profile-edit-error');
  document.getElementById('profile-view').classList.remove('hidden');
  document.getElementById('profile-edit').classList.add('hidden');
  document.getElementById('edit-profile-btn').classList.remove('hidden');
  await loadProfile();
  toast('Profile updated', 'Your changes have been saved.', 'success');
});

// ════════════════════════════════════════════════════════════ FIND PARTNERS ══

document.getElementById('search-partners-btn').addEventListener('click', loadPartners);

async function loadPartners() {
  const strategy = document.querySelector('input[name="strategy"]:checked').value;
  const btn = document.getElementById('search-partners-btn');
  btn.textContent = 'Searching...';
  btn.disabled = true;

  const r = await get('/api/match?strategy=' + strategy);
  btn.textContent = 'Search Partners';
  btn.disabled = false;

  const container = document.getElementById('partners-results');
  if (!r.ok) {
    container.innerHTML = `<div class="alert alert-error">${esc(r.data.error)}</div>`;
    return;
  }

  const { results } = r.data;
  if (!results || results.length === 0) {
    container.innerHTML = '<p style="color:var(--text-muted);margin-top:16px">No partners found. Try updating your profile with strengths and weaknesses.</p>';
    return;
  }

  container.innerHTML = results.map(p => `
    <div class="partner-card">
      <div>
        <div style="display:flex;align-items:center;gap:10px;margin-bottom:6px">
          <div class="avatar" style="width:40px;height:40px;font-size:16px">${esc(p.username.charAt(0).toUpperCase())}</div>
          <div>
            <div class="partner-info"><h4>${esc(p.username)}</h4></div>
            <div class="partner-meta">
              <span class="tag tag-major">${esc(p.major)}</span>
            </div>
          </div>
        </div>
        <div class="partner-skills">
          <span class="partner-skills-label">Strengths</span>
          <div class="tag-list">
            ${p.strengths.length ? p.strengths.map(s => `<span class="tag">${esc(s)}</span>`).join('') : '<span class="tag-empty">—</span>'}
          </div>
        </div>
        <div class="partner-skills" style="margin-top:8px">
          <span class="partner-skills-label">Weaknesses</span>
          <div class="tag-list">
            ${p.weaknesses.length ? p.weaknesses.map(w => `<span class="tag">${esc(w)}</span>`).join('') : '<span class="tag-empty">—</span>'}
          </div>
        </div>
      </div>
      <div style="text-align:center;min-width:100px">
        <div class="score-label" style="font-size:24px;margin-bottom:4px">${p.score}%</div>
        <div class="score-bar-wrap" style="flex-direction:column;gap:4px">
          <div class="score-bar-track" style="width:100px">
            <div class="score-bar-fill" style="width:${p.score}%"></div>
          </div>
          <span style="font-size:11px;color:var(--text-muted)">Match Score</span>
        </div>
        <button class="btn btn-primary btn-sm" style="margin-top:12px" onclick="goToMessageWith('${esc(p.username)}')">Message</button>
      </div>
    </div>
  `).join('');
}

function goToMessageWith(partnerName) {
  showSection('messages');
  loadContacts().then(() => openConversation(partnerName));
}

// ═════════════════════════════════════════════════════════════════ MESSAGES ══

async function loadContacts() {
  const r = await get('/api/messages/contacts');
  if (!r.ok) return;

  const list = document.getElementById('contacts-list');
  list.innerHTML = '';

  if (!r.data.contacts || r.data.contacts.length === 0) {
    list.innerHTML = '<li style="padding:16px;color:var(--text-muted);font-size:13px">No contacts yet.</li>';
    return;
  }

  r.data.contacts.forEach(c => {
    const li = document.createElement('li');
    li.className = 'contact-item';
    li.dataset.username = c.username;
    li.innerHTML = `
      <div class="avatar" style="width:36px;height:36px;font-size:14px">${esc(c.username.charAt(0).toUpperCase())}</div>
      <div>
        <div class="contact-name">${esc(c.username)}</div>
        <div class="contact-major">${esc(c.major)}</div>
      </div>
    `;
    li.addEventListener('click', () => openConversation(c.username));
    list.appendChild(li);
  });
}

async function openConversation(partner) {
  currentConvPartner = partner;

  // Highlight active contact
  document.querySelectorAll('.contact-item').forEach(li => {
    li.classList.toggle('active', li.dataset.username === partner);
  });

  document.getElementById('no-conversation').classList.add('hidden');
  document.getElementById('conv-view').classList.remove('hidden');
  document.getElementById('conv-partner-name').textContent = partner;

  clearInterval(convRefreshInterval);
  await loadConversation(partner);
  convRefreshInterval = setInterval(() => loadConversation(partner), 3000);
}

async function loadConversation(partner) {
  const r = await get('/api/messages/conversation?with=' + encodeURIComponent(partner));
  if (!r.ok) return;

  const list = document.getElementById('messages-list');
  const wasAtBottom = list.scrollHeight - list.scrollTop <= list.clientHeight + 10;

  list.innerHTML = r.data.messages.map(m => {
    const isSent = m.sender === username;
    return `
      <div class="msg-bubble ${isSent ? 'sent' : 'received'}">
        <div>${esc(m.content)}</div>
        <div class="msg-meta">${esc(m.timestamp)}</div>
      </div>
    `;
  }).join('');

  if (wasAtBottom || r.data.messages.length <= 5) {
    list.scrollTop = list.scrollHeight;
  }

  // Update unread badge
  updateUnreadBadge();
}

document.getElementById('send-msg-btn').addEventListener('click', sendMessage);
document.getElementById('msg-input').addEventListener('keydown', e => {
  if (e.key === 'Enter') sendMessage();
});

async function sendMessage() {
  const input = document.getElementById('msg-input');
  const content = input.value.trim();
  if (!content || !currentConvPartner) return;

  const r = await post('/api/messages/send', { to: currentConvPartner, content });
  if (!r.ok) {
    toast('Send failed', r.data.error || 'Could not send message', 'error');
    return;
  }
  input.value = '';
  await loadConversation(currentConvPartner);
}

// Contact search filter
document.getElementById('contact-search').addEventListener('input', e => {
  const q = e.target.value.toLowerCase();
  document.querySelectorAll('.contact-item').forEach(li => {
    const name = li.dataset.username || '';
    li.style.display = name.toLowerCase().includes(q) ? '' : 'none';
  });
});

async function updateUnreadBadge() {
  const r = await get('/api/messages/unread-count');
  const badge = document.getElementById('unread-badge');
  if (r.ok && r.data.count > 0) {
    badge.textContent = r.data.count;
    badge.classList.remove('hidden');
  } else {
    badge.classList.add('hidden');
  }
}

// ═══════════════════════════════════════════════════════════════════ GROUPS ══

async function loadGroups() {
  const [allR, mineR] = await Promise.all([
    get('/api/groups'),
    get('/api/groups/mine')
  ]);

  renderGroups('all-groups', allR.ok ? allR.data.groups : []);
  renderGroups('my-groups',  mineR.ok ? mineR.data.groups : []);
}

function renderGroups(containerId, groups) {
  const container = document.getElementById(containerId);
  if (!groups || groups.length === 0) {
    container.innerHTML = '<p style="color:var(--text-muted);padding:16px">No groups found.</p>';
    return;
  }
  container.innerHTML = groups.map(g => `
    <div class="group-card">
      <h4>${esc(g.name)}</h4>
      <div class="group-topic">${esc(g.topic)}</div>
      <div class="group-meta">Created by <strong>${esc(g.creator)}</strong> · ID #${g.id}</div>
      <div class="group-footer">
        <span class="group-members">👥 ${g.members.length} member${g.members.length !== 1 ? 's' : ''}</span>
        ${g.isMember
          ? '<span class="tag" style="background:var(--success-light);color:var(--success)">✓ Member</span>'
          : `<button class="btn btn-primary btn-sm" onclick="joinGroup(${g.id})">Join</button>`}
      </div>
    </div>
  `).join('');
}

async function joinGroup(groupId) {
  const r = await post('/api/groups/join', { groupId });
  if (!r.ok) {
    toast('Error', r.data.error || 'Could not join group', 'error');
    return;
  }
  toast('Joined!', 'You have joined the group.', 'success');
  loadGroups();
}

// Tabs
document.querySelectorAll('.tab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    const tabId = btn.dataset.tab;
    document.querySelectorAll('.groups-grid').forEach(g => g.classList.add('hidden'));
    document.getElementById(tabId).classList.remove('hidden');
  });
});

// Create Group Modal
document.getElementById('create-group-btn').addEventListener('click', () => {
  document.getElementById('modal-create-group').classList.remove('hidden');
});

document.querySelectorAll('.modal-close').forEach(btn => {
  btn.addEventListener('click', () => {
    const modalId = btn.dataset.modal;
    if (modalId) document.getElementById(modalId).classList.add('hidden');
  });
});

document.getElementById('confirm-create-group').addEventListener('click', async () => {
  const name  = document.getElementById('group-name').value.trim();
  const topic = document.getElementById('group-topic').value.trim();
  if (!name) {
    showAlert('create-group-error', 'Group name is required');
    return;
  }
  const r = await post('/api/groups/create', { name, topic });
  if (!r.ok) {
    showAlert('create-group-error', r.data.error || 'Could not create group');
    return;
  }
  document.getElementById('modal-create-group').classList.add('hidden');
  document.getElementById('group-name').value  = '';
  document.getElementById('group-topic').value = '';
  hideAlert('create-group-error');
  toast('Group created!', 'Your study group is ready.', 'success');
  loadGroups();
});

// ════════════════════════════════════════════════════════════════ ADMIN ══

async function loadAdminUsers() {
  const r = await get('/api/admin/users');
  const msg = document.getElementById('admin-message');
  if (!r.ok) {
    msg.textContent = r.data.error || 'Failed to load users';
    msg.className = 'alert alert-error';
    return;
  }
  msg.className = 'alert hidden';

  const tbody = document.getElementById('users-tbody');
  tbody.innerHTML = r.data.users.map(u => `
    <tr>
      <td><strong>${esc(u.username)}</strong></td>
      <td>${esc(u.major)}</td>
      <td><span class="tag">${esc(u.role)}</span></td>
      <td><span class="status-badge ${esc(u.status)}">${esc(u.status)}</span></td>
      <td>
        ${u.role !== 'ADMIN' ? (
          u.status === 'NORMAL'
            ? `<button class="btn btn-danger btn-sm" onclick="adminSuspend('${esc(u.username)}')">Suspend</button>`
            : `<button class="btn btn-success btn-sm" onclick="adminReinstate('${esc(u.username)}')">Reinstate</button>`
        ) : '<span style="color:var(--text-muted)">—</span>'}
      </td>
    </tr>
  `).join('');
}

async function adminSuspend(uname) {
  const r = await post('/api/admin/suspend', { username: uname });
  if (!r.ok) { toast('Error', r.data.error, 'error'); return; }
  toast('Suspended', uname + ' has been suspended.', 'success');
  loadAdminUsers();
}

async function adminReinstate(uname) {
  const r = await post('/api/admin/reinstate', { username: uname });
  if (!r.ok) { toast('Error', r.data.error, 'error'); return; }
  toast('Reinstated', uname + ' has been reinstated.', 'success');
  loadAdminUsers();
}

document.getElementById('refresh-users-btn').addEventListener('click', loadAdminUsers);

// ═══════════════════════════════════════════════ OBSERVER PATTERN — POLLING ══

function startNotifyPolling() {
  clearInterval(notifyInterval);
  notifyInterval = setInterval(async () => {
    const r = await get('/api/notifications/poll');
    if (!r.ok || !r.data.notifications) return;
    r.data.notifications.forEach(n => {
      toast('New Notification', n, 'info');
    });
    // Also refresh unread badge whenever notifications arrive
    if (r.data.notifications.length > 0) updateUnreadBadge();
  }, 5000);
}

// ══════════════════════════════════════════════════════════ NAV LINK CLICKS ══

document.querySelectorAll('.nav-link').forEach(link => {
  link.addEventListener('click', async e => {
    e.preventDefault();
    const page = link.dataset.page;
    showSection(page);

    if (page === 'profile')  await loadProfile();
    if (page === 'partners') { /* user clicks search manually */ }
    if (page === 'messages') {
      await loadContacts();
      await updateUnreadBadge();
      // Stop conv refresh if switching away
    }
    if (page === 'groups') await loadGroups();
    if (page === 'admin')  await loadAdminUsers();
  });
});

// ════════════════════════════════════════════════════════════════ ALERT HELPERS ══

function showAlert(id, msg) {
  const el = document.getElementById(id);
  if (el) { el.textContent = msg; el.classList.remove('hidden'); }
}

function hideAlert(id) {
  const el = document.getElementById(id);
  if (el) el.classList.add('hidden');
}

// ═══════════════════════════════════════════════════════════════════ STARTUP ══

(function init() {
  if (token && username) {
    // Restore session if token exists in localStorage
    enterApp();
  } else {
    showPage('page-login');
  }
})();
