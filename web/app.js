/* ═══════════════════════════════════════════════════════════════════════
   UniLink — Frontend SPA
   All API calls, page routing, and UI logic in one self-contained file.
   ═══════════════════════════════════════════════════════════════════════ */

const API = '';   // same origin — empty prefix

// ── Session state ─────────────────────────────────────────────────────────
let token    = sessionStorage.getItem('unilink_token')    || null;
let username = sessionStorage.getItem('unilink_username') || null;
let userRole = sessionStorage.getItem('unilink_role')     || null;
let currentConvPartner  = null;
let convRefreshInterval = null;
let contactsRefreshInterval = null;
let notifyInterval      = null;
let currentSection      = 'profile';
let currentGroupId      = null;
let groupChatInterval   = null;

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
  currentSection = name;

  // Start/stop contacts auto-refresh based on active section
  if (name === 'messages') {
    clearInterval(contactsRefreshInterval);
    contactsRefreshInterval = setInterval(loadContacts, 5000);
  } else {
    clearInterval(contactsRefreshInterval);
  }
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
  sessionStorage.setItem('unilink_token',    token);
  sessionStorage.setItem('unilink_username', username);
  sessionStorage.setItem('unilink_role',     userRole);
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
  sessionStorage.removeItem('unilink_token');
  sessionStorage.removeItem('unilink_username');
  sessionStorage.removeItem('unilink_role');
  currentConvPartner = null;
  currentGroupId = null;
  clearInterval(convRefreshInterval);
  clearInterval(contactsRefreshInterval);
  clearInterval(notifyInterval);
  clearInterval(groupChatInterval);
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

  // Date of Birth
  const dobRow = document.getElementById('pv-dob-row');
  if (p.dateOfBirth) {
    document.getElementById('pv-dob').textContent = p.dateOfBirth;
    dobRow.classList.remove('hidden');
  } else {
    dobRow.classList.add('hidden');
  }

  // Gender
  const genderRow = document.getElementById('pv-gender-row');
  if (p.gender) {
    document.getElementById('pv-gender').textContent = p.gender;
    genderRow.classList.remove('hidden');
  } else {
    genderRow.classList.add('hidden');
  }

  // Introduction
  const introRow = document.getElementById('pv-intro-row');
  if (p.introduction) {
    document.getElementById('pv-intro').textContent = p.introduction;
    introRow.classList.remove('hidden');
  } else {
    introRow.classList.add('hidden');
  }

  // Hobbies
  const hobbiesEl = document.getElementById('pv-hobbies');
  hobbiesEl.innerHTML = p.hobbies && p.hobbies.length
    ? p.hobbies.map(h => `<span class="tag">${esc(h)}</span>`).join('')
    : '<span class="tag-empty">None listed</span>';

  // Pre-fill edit form
  document.getElementById('edit-major').value        = p.major || '';
  document.getElementById('edit-dob').value          = p.dateOfBirth || '';
  document.getElementById('edit-gender').value       = p.gender || '';
  document.getElementById('edit-strengths').value    = (p.strengths  || []).join(', ');
  document.getElementById('edit-weaknesses').value   = (p.weaknesses || []).join(', ');
  document.getElementById('edit-hobbies').value      = (p.hobbies    || []).join(', ');
  document.getElementById('edit-introduction').value = p.introduction || '';
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
  const major        = document.getElementById('edit-major').value.trim();
  const dateOfBirth  = document.getElementById('edit-dob').value.trim();
  const gender       = document.getElementById('edit-gender').value;
  const strengths    = document.getElementById('edit-strengths').value
                         .split(',').map(s => s.trim()).filter(Boolean);
  const weaknesses   = document.getElementById('edit-weaknesses').value
                         .split(',').map(s => s.trim()).filter(Boolean);
  const hobbies      = document.getElementById('edit-hobbies').value
                         .split(',').map(s => s.trim()).filter(Boolean);
  const introduction = document.getElementById('edit-introduction').value.trim();

  const r = await put('/api/profile', { major, dateOfBirth, gender, strengths, weaknesses, hobbies, introduction });
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
        <button class="btn btn-outline btn-sm" style="margin-top:6px" onclick="reportUser('${esc(p.username)}')">Report</button>
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
    const unreadBadge = c.unreadCount > 0
      ? `<span class="contact-unread-badge">${c.unreadCount}</span>`
      : '';
    li.innerHTML = `
      <div class="avatar" style="width:36px;height:36px;font-size:14px">${esc(c.username.charAt(0).toUpperCase())}</div>
      <div style="flex:1;min-width:0">
        <div class="contact-name">${esc(c.username)}</div>
        <div class="contact-major">${esc(c.major)}</div>
      </div>
      ${unreadBadge}
    `;
    li.addEventListener('click', () => openConversation(c.username));
    list.appendChild(li);
  });
}

async function openConversation(partner) {
  currentConvPartner = partner;

  // Highlight active contact and clear its unread badge immediately (optimistic update)
  document.querySelectorAll('.contact-item').forEach(li => {
    li.classList.toggle('active', li.dataset.username === partner);
    if (li.dataset.username === partner) {
      const badge = li.querySelector('.contact-unread-badge');
      if (badge) badge.remove();
    }
  });

  document.getElementById('no-conversation').classList.add('hidden');
  document.getElementById('conv-view').classList.remove('hidden');
  document.getElementById('conv-partner-name').textContent = partner;
  document.getElementById('report-conv-partner-btn').onclick = () => reportUser(partner);

  clearInterval(convRefreshInterval);
  await loadConversation(partner);
  convRefreshInterval = setInterval(() => loadConversation(partner), 3000);
}

async function loadConversation(partner) {
  const r = await get('/api/messages/conversation?with=' + encodeURIComponent(partner));
  if (!r.ok) return;

  const list = document.getElementById('messages-list');
  const wasAtBottom = list.scrollHeight - list.scrollTop <= list.clientHeight + 10;

  const msgs = r.data.messages || [];
  list.innerHTML = msgs.map(m => {
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
          ? `<span class="tag" style="background:var(--success-light);color:var(--success)">✓ Member</span>
             <button class="btn btn-primary btn-sm group-chat-btn" onclick="openGroupChat(${g.id}, '${esc(g.name)}')">💬 Chat</button>`
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

// ── Group Chat ────────────────────────────────────────────────────────────────

function openGroupChat(groupId, groupName) {
  currentGroupId = groupId;
  document.getElementById('group-chat-title').textContent = groupName + ' — Group Chat';
  document.getElementById('group-chat-messages').innerHTML = '';
  document.getElementById('group-chat-input').value = '';
  document.getElementById('modal-group-chat').classList.remove('hidden');

  clearInterval(groupChatInterval);
  loadGroupMessages(groupId);
  groupChatInterval = setInterval(() => loadGroupMessages(groupId), 3000);
}

async function loadGroupMessages(groupId) {
  const r = await get('/api/groups/chat?groupId=' + groupId);
  if (!r.ok) return;

  const container = document.getElementById('group-chat-messages');
  const wasAtBottom = container.scrollHeight - container.scrollTop <= container.clientHeight + 10;

  const msgs = r.data.messages || [];
  if (msgs.length === 0) {
    container.innerHTML = '<p class="no-group-msgs">No messages yet. Say hello!</p>';
    return;
  }

  container.innerHTML = msgs.map(m => {
    const isSent = m.sender === username;
    return `
      <div class="msg-bubble ${isSent ? 'sent' : 'received'}">
        ${!isSent ? `<div class="group-msg-sender">${esc(m.sender)}</div>` : ''}
        <div>${esc(m.content)}</div>
        <div class="msg-meta">${esc(m.timestamp)}</div>
      </div>
    `;
  }).join('');

  if (wasAtBottom || msgs.length <= 5) {
    container.scrollTop = container.scrollHeight;
  }
}

async function sendGroupMessage() {
  const input = document.getElementById('group-chat-input');
  const content = input.value.trim();
  if (!content || currentGroupId === null) return;

  const r = await post('/api/groups/chat', { groupId: currentGroupId, content });
  if (!r.ok) {
    toast('Send failed', r.data.error || 'Could not send message', 'error');
    return;
  }
  input.value = '';
  await loadGroupMessages(currentGroupId);
}

document.getElementById('group-chat-send').addEventListener('click', sendGroupMessage);
document.getElementById('group-chat-input').addEventListener('keydown', e => {
  if (e.key === 'Enter') sendGroupMessage();
});

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
    if (modalId) {
      document.getElementById(modalId).classList.add('hidden');
      if (modalId === 'modal-group-chat') {
        clearInterval(groupChatInterval);
        currentGroupId = null;
      }
    }
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

document.getElementById('refresh-users-btn').addEventListener('click', () => {
  loadAdminUsers();
  loadAdminReports();
});

// Admin sub-tabs (Users / Reports)
document.querySelectorAll('[data-admin-tab]').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('[data-admin-tab]').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    const panelId = btn.dataset.adminTab;
    document.getElementById('admin-users-panel').classList.toggle('hidden', panelId !== 'admin-users-panel');
    document.getElementById('admin-reports-panel').classList.toggle('hidden', panelId !== 'admin-reports-panel');
  });
});

// ════════════════════════════════════════════════════════════════ REPORTS ══

function reportUser(reportedUsername) {
  document.getElementById('report-target').value = reportedUsername;
  document.getElementById('report-reason').value = '';
  hideAlert('report-error');
  document.getElementById('modal-report').classList.remove('hidden');
}

document.getElementById('confirm-report').addEventListener('click', async () => {
  const reported = document.getElementById('report-target').value;
  const reason   = document.getElementById('report-reason').value.trim();
  if (!reason) {
    showAlert('report-error', 'Please describe the reason for the report.');
    return;
  }
  const r = await post('/api/reports/submit', { reported, reason });
  if (!r.ok) {
    showAlert('report-error', r.data.error || 'Could not submit report');
    return;
  }
  document.getElementById('modal-report').classList.add('hidden');
  toast('Report submitted', 'An administrator will review your report.', 'success');
});

async function loadAdminReports() {
  const r = await get('/api/reports');
  const msg = document.getElementById('admin-reports-message');
  if (!r.ok) {
    msg.textContent = r.data.error || 'Failed to load reports';
    msg.className = 'alert alert-error';
    return;
  }
  msg.className = 'alert hidden';

  const tbody = document.getElementById('reports-tbody');
  if (!r.data.reports || r.data.reports.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--text-muted);padding:24px">No reports yet.</td></tr>';
    return;
  }
  tbody.innerHTML = r.data.reports.map(rep => `
    <tr>
      <td>#${rep.id}</td>
      <td>${esc(rep.reporter)}</td>
      <td><strong>${esc(rep.reported)}</strong></td>
      <td style="max-width:200px;white-space:normal">${esc(rep.reason)}</td>
      <td>${esc(rep.timestamp)}</td>
      <td><span class="report-status ${esc(rep.status)}">${esc(rep.status)}</span></td>
      <td>
        ${rep.status === 'PENDING'
          ? `<button class="btn btn-outline btn-sm" onclick="adminDismissReport(${rep.id})">Dismiss</button>`
          : '<span style="color:var(--text-muted)">—</span>'}
      </td>
    </tr>
  `).join('');
}

async function adminDismissReport(id) {
  const r = await post('/api/reports/dismiss', { id });
  if (!r.ok) { toast('Error', r.data.error || 'Could not dismiss report', 'error'); return; }
  toast('Dismissed', 'Report #' + id + ' has been dismissed.', 'success');
  loadAdminReports();
}

// ════════════════════════════════════════════════════════════════ SETTINGS ══

// Settings sub-tabs
document.querySelectorAll('[data-settings-tab]').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('[data-settings-tab]').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    const panelId = btn.dataset.settingsTab;
    document.getElementById('settings-profile-panel').classList.toggle('hidden', panelId !== 'settings-profile-panel');
    document.getElementById('settings-security-panel').classList.toggle('hidden', panelId !== 'settings-security-panel');
  });
});

async function loadSettings() {
  const r = await get('/api/profile');
  if (!r.ok) return;
  document.getElementById('s-major').value     = r.data.major || '';
  document.getElementById('s-strengths').value  = (r.data.strengths  || []).join(', ');
  document.getElementById('s-weaknesses').value = (r.data.weaknesses || []).join(', ');
}

document.getElementById('save-settings-profile-btn').addEventListener('click', async () => {
  const major     = document.getElementById('s-major').value.trim();
  const strengths = document.getElementById('s-strengths').value.split(',').map(s => s.trim()).filter(Boolean);
  const weaknesses = document.getElementById('s-weaknesses').value.split(',').map(s => s.trim()).filter(Boolean);
  const r = await put('/api/profile', { major, strengths, weaknesses });
  if (!r.ok) {
    showAlert('settings-profile-msg', r.data.error || 'Update failed');
    document.getElementById('settings-profile-msg').className = 'alert alert-error';
    return;
  }
  showAlert('settings-profile-msg', 'Profile updated successfully!');
  document.getElementById('settings-profile-msg').className = 'alert alert-success';
  await loadProfile(); // refresh nav avatar / status badge
});

document.getElementById('change-pw-btn').addEventListener('click', async () => {
  const oldPw  = document.getElementById('s-old-pw').value;
  const newPw  = document.getElementById('s-new-pw').value;
  const confPw = document.getElementById('s-confirm-pw').value;
  const msgEl  = document.getElementById('settings-pw-msg');

  if (!oldPw || !newPw) {
    showAlert('settings-pw-msg', 'Please fill in all password fields.');
    msgEl.className = 'alert alert-error'; return;
  }
  if (newPw !== confPw) {
    showAlert('settings-pw-msg', 'New passwords do not match.');
    msgEl.className = 'alert alert-error'; return;
  }
  const r = await post('/api/settings/password', { oldPassword: oldPw, newPassword: newPw });
  if (!r.ok) {
    showAlert('settings-pw-msg', r.data.error || 'Could not change password');
    msgEl.className = 'alert alert-error'; return;
  }
  // Session invalidated server-side — redirect to login
  toast('Password changed', 'Please log in with your new password.', 'success');
  setTimeout(() => { clearSession(); showPage('page-login'); }, 1500);
});

document.getElementById('delete-account-btn').addEventListener('click', async () => {
  const pw    = document.getElementById('s-del-pw').value;
  const msgEl = document.getElementById('settings-del-msg');
  if (!pw) {
    showAlert('settings-del-msg', 'Please enter your password to confirm.');
    msgEl.className = 'alert alert-error'; return;
  }
  if (!confirm('Are you sure? This will permanently delete your account and cannot be undone.')) return;

  const r = await post('/api/settings/delete', { password: pw });
  if (!r.ok) {
    showAlert('settings-del-msg', r.data.error || 'Could not delete account');
    msgEl.className = 'alert alert-error'; return;
  }
  clearSession();
  showPage('page-login');
  showAlert('login-success', 'Your account has been deleted.');
});

// ═══════════════════════════════════════════════ OBSERVER PATTERN — POLLING ══

function startNotifyPolling() {
  clearInterval(notifyInterval);
  notifyInterval = setInterval(async () => {
    const r = await get('/api/notifications/poll');
    if (!r.ok || !r.data.notifications) return;
    r.data.notifications.forEach(n => {
      // Parse sender from "You have a new message from X!"
      const match = n.match(/new message from (.+)!$/);
      const sender = match ? match[1].trim() : null;

      if (sender) {
        // If this conversation is currently open → reload immediately
        if (currentSection === 'messages' && currentConvPartner === sender) {
          loadConversation(sender);
        }
        // Clickable toast → opens the conversation
        toastClickable('New Message', n, 'info', () => {
          showSection('messages');
          loadContacts().then(() => openConversation(sender));
        });
      } else {
        toast('Notification', n, 'info');
      }
    });
    if (r.data.notifications.length > 0) {
      updateUnreadBadge();
      if (currentSection === 'messages') loadContacts();
    }
  }, 5000);
}

function toastClickable(title, msg, type, onClick) {
  const el = document.createElement('div');
  el.className = 'toast ' + type;
  el.style.cursor = 'pointer';
  el.innerHTML = `<div class="toast-title">${esc(title)}</div><div class="toast-msg">${esc(msg)}</div>`;
  el.addEventListener('click', () => { onClick(); el.remove(); });
  document.getElementById('toast-container').appendChild(el);
  setTimeout(() => el.remove(), 6000);
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
    if (page === 'settings') await loadSettings();
    if (page === 'groups') await loadGroups();
    if (page === 'admin') {
      await loadAdminUsers();
      await loadAdminReports();
    }
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
    // Restore session if token exists in sessionStorage
    enterApp();
  } else {
    showPage('page-login');
  }
})();
