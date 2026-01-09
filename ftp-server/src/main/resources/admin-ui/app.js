/* FTP Admin UI
 *
 * Uses the admin HTTP API served by ftp-server on :9090 under /api/*.
 * No authentication required.
 */

let offline = false;

const el = {
  view: document.getElementById("view"),
  banner: document.getElementById("banner"),
  modal: document.getElementById("modal"),
  modalTitle: document.getElementById("modalTitle"),
  modalBody: document.getElementById("modalBody"),
  modalActions: document.getElementById("modalActions"),
  navLinks: [...document.querySelectorAll(".navlink")]
};

function showBanner(type, msg) {
  el.banner.hidden = false;
  el.banner.className = "banner " + (type || "");
  el.banner.textContent = msg || "";
}

function clearBanner() {
  el.banner.hidden = true;
  el.banner.className = "banner";
  el.banner.textContent = "";
}

function ensureOfflineOverlay() {
  let ov = document.getElementById("offlineOverlay");
  if (ov) return ov;
  ov = document.createElement("div");
  ov.id = "offlineOverlay";
  ov.className = "offlineOverlay";
  ov.innerHTML = `
    <div class="offlineCard">
      <div class="offlineTitle">Server is offline</div>
      <div class="offlineText" id="offlineText">The server is not reachable. Start the server again to continue.</div>
      <div class="toolbar" style="justify-content:flex-end;">
        <button id="offlineRetry" class="btn btn-primary" type="button">Retry</button>
      </div>
    </div>
  `;
  document.body.appendChild(ov);
  ov.querySelector("#offlineRetry").addEventListener("click", async () => {
    await pingServer(true);
  });
  return ov;
}

function setOfflineState(isOffline, reason) {
  offline = !!isOffline;
  const ov = ensureOfflineOverlay();
  const text = ov.querySelector("#offlineText");
  if (text) text.textContent = reason || "The server is not reachable. Start the server again to continue.";
  ov.classList.toggle("show", offline);
}

async function pingServer(showOverlayOnFail) {
  try {
    await apiFetchNoAuth("/api/admin-token");
    if (offline) {
      setOfflineState(false);
      // After server returns, we must request token again for this session
      await bootstrapAuth();
    }
    return true;
  } catch (e) {
    if (showOverlayOnFail) setOfflineState(true, "The server is offline. Start it again to continue.");
    return false;
  }
}

function fmtBytes(n) {
  const v = Number(n || 0);
  if (v < 1024) return `${v} B`;
  const kb = v / 1024;
  if (kb < 1024) return `${kb.toFixed(1)} KB`;
  const mb = kb / 1024;
  if (mb < 1024) return `${mb.toFixed(1)} MB`;
  const gb = mb / 1024;
  return `${gb.toFixed(2)} GB`;
}

async function apiFetch(path, opts = {}) {
  if (offline) {
    throw new Error("Server is offline");
  }
  const headers = new Headers(opts.headers || {});
  // No token required - auth removed
  if (opts.json !== undefined) {
    headers.set("Content-Type", "application/json");
  }
  let resp;
  try {
    resp = await fetch(path, {
      method: opts.method || "GET",
      headers,
      body: opts.json !== undefined ? JSON.stringify(opts.json) : opts.body
    });
  } catch (e) {
    setOfflineState(true, "The server is offline. Start it again to continue.");
    throw new Error("Server is offline");
  }

  const ct = resp.headers.get("content-type") || "";
  const isJson = ct.includes("application/json");
  const text = isJson ? null : await resp.text().catch(() => "");
  const data = isJson ? await resp.json().catch(() => null) : null;

  if (!resp.ok) {
    const bodyMsg = isJson ? (data ? JSON.stringify(data) : "") : (text || "");
    const suffix = bodyMsg ? `: ${bodyMsg}` : "";
    throw new Error(`HTTP ${resp.status}${suffix}`);
  }
  return isJson ? data : text;
}

async function apiFetchNoAuth(path, opts = {}) {
  const headers = new Headers(opts.headers || {});
  if (opts.json !== undefined) headers.set("Content-Type", "application/json");
  let resp;
  try {
    resp = await fetch(path, {
      method: opts.method || "GET",
      headers,
      body: opts.json !== undefined ? JSON.stringify(opts.json) : opts.body
    });
  } catch (e) {
    setOfflineState(true, "The server is offline. Start it again to continue.");
    throw new Error("Server is offline");
  }
  const ct = resp.headers.get("content-type") || "";
  const isJson = ct.includes("application/json");
  const text = isJson ? null : await resp.text().catch(() => "");
  const data = isJson ? await resp.json().catch(() => null) : null;
  if (!resp.ok) {
    const bodyMsg = isJson ? (data ? JSON.stringify(data) : "") : (text || "");
    const suffix = bodyMsg ? `: ${bodyMsg}` : "";
    throw new Error(`HTTP ${resp.status}${suffix}`);
  }
  return isJson ? data : text;
}

// Token prompt removed - no auth required

function escapeHtml(s) {
  return String(s || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function route() {
  const hash = location.hash || "#/users";
  const r = hash.replace(/^#\//, "");
  const routeName = (r.split("?")[0] || "users").trim();
  for (const a of el.navLinks) {
    a.classList.toggle("active", a.dataset.route === routeName);
  }
  clearBanner();
  switch (routeName) {
    case "users": return renderUsers();
    case "limits": return renderLimits();
    case "permissions": return renderPermissions();
    case "root": return renderRoot();
    case "stats": return renderStats();
    default:
      location.hash = "#/users";
      return;
  }
}

async function bootstrapAuth() {
  // No auth required - just verify server is online
  try {
    await apiFetchNoAuth("/api/admin-token");
  } catch (e) {
    // Server might be offline, but that's handled by pingServer
  }
}

function setView(html) {
  el.view.innerHTML = html;
}

function qs(sel) {
  return el.view.querySelector(sel);
}

function qsa(sel) {
  return [...el.view.querySelectorAll(sel)];
}

function openModal({ title, bodyHtml, actionsHtml, onOpen }) {
  el.modalTitle.textContent = title || "Dialog";
  el.modalBody.innerHTML = bodyHtml || "";
  el.modalActions.innerHTML = actionsHtml || "";
  el.modal.showModal();
  if (onOpen) onOpen();
}

function closeModal() {
  try { el.modal.close(); } catch (_) {}
}

/* =========================
 * USERS
 * ========================= */

async function renderUsers() {
  setView(`
    <section class="card">
      <div class="cardhead">
        <div>
          <div class="cardtitle">Users</div>
          <div class="cardsub">Create, enable/disable, set per-user rate limit</div>
        </div>
        <div class="toolbar">
          <button id="createUserBtn" class="btn btn-primary" type="button">Create user</button>
          <button id="refreshUsersBtn" class="btn" type="button">Refresh</button>
        </div>
      </div>
      <div class="cardbody">
        <div class="block" id="usersHintBlock">
          <div class="hint" id="usersHint">Loading...</div>
        </div>
        <div class="block tablewrap">
          <table>
            <thead>
              <tr>
                <th style="width: 50%;">Username</th>
                <th>Enabled</th>
                <th style="width: 1%;">Actions</th>
              </tr>
            </thead>
            <tbody id="usersTbody"></tbody>
          </table>
        </div>
      </div>
    </section>
  `);

  qs("#createUserBtn").addEventListener("click", openCreateUser);
  qs("#refreshUsersBtn").addEventListener("click", loadUsers);
  await loadUsers();
}

async function loadUsers() {
  const hint = qs("#usersHint");
  const hintBlock = qs("#usersHintBlock");
  const tbody = qs("#usersTbody");
  hint.textContent = "Loading...";
  hintBlock.hidden = false;
  tbody.innerHTML = "";
  try {
    const users = await apiFetch("/api/users");
    hint.textContent = users.length ? "" : "No users";
    hintBlock.hidden = !hint.textContent;
    tbody.innerHTML = users.map(u => `
      <tr data-username="${escapeHtmlAttr(u.username)}">
        <td>${escapeHtml(u.username)}</td>
        <td>${u.enabled ? `true` : `false`}</td>
        <td class="cell-actions">
          <button class="btn btn-secondary" data-action="edit" type="button">Edit</button>
          <button class="btn btn-danger" data-action="delete" type="button">Delete</button>
        </td>
      </tr>
    `).join("");

    qsa("button[data-action='edit']").forEach(btn => btn.addEventListener("click", () => {
      const tr = btn.closest("tr");
      openEditUser(tr.dataset.username);
    }));
    qsa("button[data-action='delete']").forEach(btn => btn.addEventListener("click", () => {
      const tr = btn.closest("tr");
      deleteUser(tr.dataset.username);
    }));
  } catch (e) {
    hint.textContent = "No data";
    hintBlock.hidden = !hint.textContent;
    showBanner("err", `Failed to load users: ${e.message}`);
  }
}
                                   
function openCreateUser() {
  openModal({
    title: "Create user",
    bodyHtml: `
      <div class="grid2">
        <div>
          <label class="label" for="newUsername">Username</label>
          <input id="newUsername" class="field" type="text" placeholder="username" />
        </div>
        <div>
          <label class="label" for="newPassword">Password</label>
          <input id="newPassword" class="field" type="password" placeholder="password" />
        </div>
      </div>
    `,
    actionsHtml: `
      <button class="btn" type="button" onclick="closeModal()">Cancel</button>
      <button id="createUserConfirm" class="btn btn-primary" type="button">Create</button>
    `,
    onOpen: () => {
      const u = document.getElementById("newUsername");
      const p = document.getElementById("newPassword");
      document.getElementById("createUserConfirm").addEventListener("click", async () => {
        clearBanner();
        const username = (u.value || "").trim();
        const password = (p.value || "").trim();
        if (!username || !password) {
          showBanner("warn", "Username and password are required");
          return;
        }
        try {
          await apiFetch("/api/users", { method: "POST", json: { username, password } });
          closeModal();
          showBanner("ok", `User "${username}" created`);
          await loadUsers();
        } catch (e) {
          showBanner("err", `Failed to create user: ${e.message}`);
        }
      });
    }
  });
}

async function openEditUser(username) {
  openModal({
    title: `Edit user: ${username}`,
    bodyHtml: `
      <div class="grid2">
        <div>
          <label class="label" for="editEnabled">Enabled</label>
          <select id="editEnabled" class="field">
            <option value="true">true</option>
            <option value="false">false</option>
          </select>
        </div>
        <div>
          <label class="label" for="editRateLimit">Rate limit (bytes/sec, optional)</label>
          <input id="editRateLimit" class="field" type="number" min="0" placeholder="null = use server limit" />
          <div class="muted" style="margin-top:6px; color: #999;">Leave empty to use server limit</div>
        </div>
      </div>
    `,
    actionsHtml: `
      <button class="btn" type="button" onclick="closeModal()">Cancel</button>
      <button id="saveUserConfirm" class="btn btn-primary" type="button">Save</button>
    `,
    onOpen: async () => {
      const enabledSel = document.getElementById("editEnabled");
      const rateInp = document.getElementById("editRateLimit");
      const saveBtn = document.getElementById("saveUserConfirm");

      saveBtn.disabled = true;
      try {
        const detail = await apiFetch(`/api/users/${encodeURIComponent(username)}`);
        enabledSel.value = String(!!detail.enabled);
        rateInp.value = detail.rateLimit === null || detail.rateLimit === undefined ? "" : String(detail.rateLimit);
      } catch (e) {
        showBanner("warn", `Could not load user details: ${e.message}`);
      } finally {
        saveBtn.disabled = false;
      }

      saveBtn.addEventListener("click", async () => {
        clearBanner();
        const enabled = enabledSel.value === "true";
        const rateRaw = (rateInp.value || "").trim();
        const rateLimit = rateRaw === "" ? null : Number(rateRaw);
        if (rateLimit !== null && (!Number.isFinite(rateLimit) || rateLimit < 0)) {
          showBanner("warn", "Rate limit must be a non-negative number or empty");
          return;
        }
        try {
          await apiFetch(`/api/users/${encodeURIComponent(username)}`, {
            method: "PUT",
            json: rateLimit === null ? { enabled } : { enabled, rateLimit }
          });
          closeModal();
          showBanner("ok", `User "${username}" updated`);
          await loadUsers();
        } catch (e) {
          showBanner("err", `Failed to update user: ${e.message}`);
        }
      });
    }
  });
}

async function deleteUser(username) {
  clearBanner();
  if (!confirm(`Delete user "${username}"?`)) return;
  try {
    await apiFetch(`/api/users/${encodeURIComponent(username)}`, { method: "DELETE" });
    showBanner("ok", `User "${username}" deleted`);
    await loadUsers();
  } catch (e) {
    showBanner("err", `Failed to delete user: ${e.message}`);
  }
}

/* =========================
 * LIMITS
 * ========================= */

async function renderLimits() {
  setView(`
    <section class="card">
      <div class="cardhead">
        <div>
          <div class="cardtitle">Server limits</div>
          <div class="cardsub">Global max connections and rate limits</div>
        </div>
        <div class="toolbar">
          <button id="refreshLimitsBtn" class="btn" type="button">Refresh</button>
          <button id="saveLimitsBtn" class="btn btn-primary" type="button">Apply</button>
        </div>
      </div>
      <div class="cardbody">
        <div class="block">
          <div class="grid2">
            <div>
              <label class="label" for="maxConn">Global max connections</label>
              <input id="maxConn" class="field" type="number" min="1" />
            </div>
            <div>
              <label class="label" for="uploadLimit">Global upload limit (bytes/sec)</label>
              <input id="uploadLimit" class="field" type="number" min="0" placeholder="Optional" />
              <div id="uploadHint" class="hint" style="margin-top:6px;"></div>
            </div>
          </div>
          <div class="grid2" style="margin-top: 15px;">
            <div>
              <label class="label" for="downloadLimit">Global download limit (bytes/sec)</label>
              <input id="downloadLimit" class="field" type="number" min="0" placeholder="Optional" />
              <div id="downloadHint" class="hint" style="margin-top:6px;"></div>
            </div>
            <div></div>
          </div>
        </div>
      </div>
    </section>
  `);

  qs("#refreshLimitsBtn").addEventListener("click", loadLimits);
  qs("#saveLimitsBtn").addEventListener("click", saveLimits);
  await loadLimits();
}

async function loadLimits() {
  clearBanner();
  const maxConn = qs("#maxConn");
  const uploadLimit = qs("#uploadLimit");
  const downloadLimit = qs("#downloadLimit");
  const uploadHint = qs("#uploadHint");
  const downloadHint = qs("#downloadHint");
  uploadHint.textContent = "Loading...";
  downloadHint.textContent = "";
  try {
    const l = await apiFetch("/api/limits");
    maxConn.value = String(l.globalMaxConnections ?? "");
    uploadLimit.value = String(l.globalUploadLimit ?? "");
    downloadLimit.value = String(l.globalDownloadLimit ?? "");
    uploadHint.textContent = l.globalUploadLimit ? `~ ${fmtBytes(l.globalUploadLimit)}/s` : "";
    downloadHint.textContent = l.globalDownloadLimit ? `~ ${fmtBytes(l.globalDownloadLimit)}/s` : "";
  } catch (e) {
    uploadHint.textContent = "";
    downloadHint.textContent = "";
    showBanner("err", `Failed to load limits: ${e.message}`);
  }
}

async function saveLimits() {
  clearBanner();
  const maxConn = Number(qs("#maxConn").value);
  const uploadLimitRaw = qs("#uploadLimit").value.trim();
  const downloadLimitRaw = qs("#downloadLimit").value.trim();
  
  if (!Number.isFinite(maxConn) || maxConn < 1) {
    showBanner("warn", "Global max connections must be >= 1");
    return;
  }
  
  const uploadLimit = uploadLimitRaw === "" ? null : Number(uploadLimitRaw);
  const downloadLimit = downloadLimitRaw === "" ? null : Number(downloadLimitRaw);
  
  if (uploadLimit !== null && (!Number.isFinite(uploadLimit) || uploadLimit < 0)) {
    showBanner("warn", "Global upload limit must be >= 0 or empty");
    return;
  }
  if (downloadLimit !== null && (!Number.isFinite(downloadLimit) || downloadLimit < 0)) {
    showBanner("warn", "Global download limit must be >= 0 or empty");
    return;
  }
  
  try {
    const json = { globalMaxConnections: maxConn };
    if (uploadLimit !== null) json.globalUploadLimit = uploadLimit;
    if (downloadLimit !== null) json.globalDownloadLimit = downloadLimit;
    
    await apiFetch("/api/limits", {
      method: "PUT",
      json
    });
    showBanner("ok", "Limits applied (all active sessions will be disconnected to apply new limits)");
    await loadLimits();
  } catch (e) {
    showBanner("err", `Failed to apply limits: ${e.message}`);
  }
}

/* =========================
 * PERMISSIONS
 * ========================= */

async function renderPermissions() {
  setView(`
    <section class="card">
      <div class="cardhead">
        <div>
          <div class="cardtitle">Permissions</div>
          <div class="cardsub">Global permissions and shared folders (user-to-user)</div>
        </div>
        <div class="toolbar">
          <button id="permRefreshBtn" class="btn" type="button">Refresh</button>
          <button id="permSaveBtn" class="btn btn-primary" type="button">Save global</button>
        </div>
      </div>
      <div class="cardbody">
        <div class="block">
          <div class="sectiontitle">Global permissions</div>
          <div class="sectiondesc">Applies to all folders for the selected user.</div>
          <div class="sectionbody">
            <label class="label" for="permUser">User</label>
            <select id="permUser" class="field" style="width: 320px;"></select>
          </div>
          <div class="sectionbody">
            <div class="checks">
              <label class="check"><input type="checkbox" id="gR" /> R</label>
              <label class="check"><input type="checkbox" id="gW" /> W</label>
              <label class="check"><input type="checkbox" id="gE" /> E</label>
            </div>
          </div>
        </div>

        <div class="block">
          <div class="sectiontitle">Shared folders (user-to-user)</div>
          <div class="sectiondesc">All shared folders from the database.</div>
          <div class="sectionbody toolbar" style="justify-content:flex-start;">
            <button id="sharedRefreshBtn" class="btn" type="button">Refresh</button>
          </div>
          <div class="sectionbody">
            <div class="hint" id="sharedHint">Loading...</div>
          </div>
        </div>

        <div class="block tablewrap">
          <table>
            <thead>
              <tr>
                <th style="width: 15%;">Owner</th>
                <th style="width: 15%;">Shared with</th>
                <th style="width: 18%;">Name</th>
                <th style="width: 35%;">Path</th>
                <th>R</th>
                <th>W</th>
                <th>E</th>
                <th style="width: 1%;">Actions</th>
              </tr>
            </thead>
            <tbody id="sharedTbody"></tbody>
          </table>
        </div>
      </div>
    </section>
  `);

  qs("#permRefreshBtn").addEventListener("click", loadPermissionsForSelected);
  qs("#permSaveBtn").addEventListener("click", () => {
    if (confirm("Confirm and save global permissions?")) saveGlobalPermissions();
  });
  qs("#permUser").addEventListener("change", loadPermissionsForSelected);

  qs("#sharedRefreshBtn").addEventListener("click", loadAllSharedFolders);

  await loadUsersIntoPermSelect();
  await loadAllSharedFolders();
}

async function loadUsersIntoPermSelect() {
  const sel = qs("#permUser");
  sel.innerHTML = `<option value="">-- select --</option>`;
  try {
    const users = await apiFetch("/api/users");
    for (const u of users) {
      const opt = document.createElement("option");
      opt.value = u.username;
      opt.textContent = u.username;
      sel.appendChild(opt);
    }
  } catch (e) {
    showBanner("err", `Failed to load users: ${e.message}`);
  }
}

async function loadPermissionsForSelected() {
  clearBanner();
  const username = qs("#permUser").value;
  const sharedHint = qs("#sharedHint");
  const sharedTbody = qs("#sharedTbody");
  if (!username) {
    qs("#gR").checked = false;
    qs("#gW").checked = false;
    qs("#gE").checked = false;

    sharedHint.textContent = "Select user to load shared folders";
    sharedTbody.innerHTML = "";
    return;
  }

  try {
    const gp = await apiFetch(`/api/user-permissions?user=${encodeURIComponent(username)}`)
      .catch(() => ({ read: false, write: false, execute: false }));

    qs("#gR").checked = !!gp.read;
    qs("#gW").checked = !!gp.write;
    qs("#gE").checked = !!gp.execute;
  } catch (e) {
    showBanner("err", `Failed to load global permissions: ${e.message}`);
  }
  // Note: We now load all shared folders, not filtered by user
}

async function saveGlobalPermissions() {
  clearBanner();
  const username = qs("#permUser").value;
  if (!username) {
    showBanner("warn", "Select user first");
    return;
  }

  const gp = {
    username,
    read: qs("#gR").checked,
    write: qs("#gW").checked,
    execute: qs("#gE").checked
  };

  try {
    await apiFetch("/api/user-permissions", { method: "POST", json: gp });
    showBanner("ok", "Global permissions saved");
    await loadPermissionsForSelected();
  } catch (e) {
    showBanner("err", `Failed to save global permissions: ${e.message}`);
  }
}

/* =========================
 * SHARED FOLDERS (user-to-user)
 * ========================= */

async function loadAllSharedFolders() {
  clearBanner();
  const hint = qs("#sharedHint");
  const tbody = qs("#sharedTbody");
  if (!hint || !tbody) return;

  hint.textContent = "Loading...";
  tbody.innerHTML = "";

  try {
    const shared = await apiFetch("/api/shared-folders/all");
    hint.textContent = shared.length ? "" : "No shared folders in database.";
    tbody.innerHTML = shared.map((s, idx) => `
      <tr data-idx="${idx}" data-folder-id="${s.id || ""}" data-folder-path="${escapeHtmlAttr(s.folderPath || "")}">
        <td>${escapeHtml(s.ownerUsername || "")}</td>
        <td>${escapeHtml(s.userToShareUsername || "")}</td>
        <td>${escapeHtml(s.folderName || "")}</td>
        <td>${escapeHtml(s.folderPath || "")}</td>
        <td style="text-align:center;">${s.read ? "true" : "false"}</td>
        <td style="text-align:center;">${s.write ? "true" : "false"}</td>
        <td style="text-align:center;">${s.execute ? "true" : "false"}</td>
        <td class="cell-actions">
          <button class="btn btn-danger" data-action="delete-share" type="button">Delete</button>
        </td>
      </tr>
    `).join("");

    tbody._data = shared;
    qsa("button[data-action='delete-share']").forEach(btn => btn.addEventListener("click", () => {
      const tr = btn.closest("tr");
      const i = Number(tr?.dataset.idx);
      if (!Number.isFinite(i)) return;
      const row = tbody._data[i];
      deleteSharedFolder(row?.id, row?.folderPath);
    }));
  } catch (e) {
    hint.textContent = "No data";
    showBanner("err", `Failed to load shared folders: ${e.message}`);
  }
}

async function deleteSharedFolder(folderId, folderPath) {
  if (!folderId && !folderPath) return;
  clearBanner();
  const confirmMsg = folderId 
    ? `Delete shared folder entry (ID: ${folderId})?`
    : `Delete shared folder entries for path "${folderPath}"?`;
  if (!confirm(confirmMsg)) return;
  try {
    const url = folderId
      ? `/api/shared-folders/all/delete?id=${encodeURIComponent(folderId)}`
      : `/api/shared-folders/all/delete?folderPath=${encodeURIComponent(folderPath)}`;
    await apiFetch(url, { method: "DELETE" });
    showBanner("ok", "Shared folder deleted");
    await loadAllSharedFolders();
  } catch (e) {
    showBanner("err", `Failed to delete shared folder: ${e.message}`);
  }
}

/* =========================
 * STATS
 * ========================= */

async function renderStats() {
  setView(`
    <section class="card">
      <div class="cardhead">
        <div>
          <div class="cardtitle">Stats</div>
          <div class="cardsub">Live connections and traffic</div>
        </div>
        <div class="toolbar">
          <button id="refreshStatsBtn" class="btn" type="button">Refresh</button>
        </div>
      </div>
      <div class="cardbody">
        <div class="block">
          <div class="hint" id="statsHint">Loading...</div>
          <div class="hint" id="statsConnectedNow"></div>
        </div>
        <div class="block tablewrap">
          <table>
            <thead>
              <tr>
                <th style="width: 24%;">Username</th>
                <th>Connections now</th>
                <th>Uploaded</th>
                <th>Downloaded</th>
                <th style="width: 28%;">Last login</th>
              </tr>
            </thead>
            <tbody id="statsTbody"></tbody>
          </table>
        </div>
      </div>
    </section>
  `);
  qs("#refreshStatsBtn").addEventListener("click", loadStats);
  await loadStats();
}

async function loadStats() {
  clearBanner();
  const hint = qs("#statsHint");
  const connectedNow = qs("#statsConnectedNow");
  const tbody = qs("#statsTbody");
  hint.textContent = "Loading...";
  connectedNow.textContent = "";
  tbody.innerHTML = "";
  try {
    const live = await apiFetch("/api/stats/live");
    const users = live?.users || [];
    const connectedUsers = Number(live?.connectedUsers ?? 0);
    const totalConnections = Number(live?.totalConnections ?? 0);

    hint.textContent = users.length ? "" : "No data";
    connectedNow.textContent = `Connected now: ${connectedUsers} users (${totalConnections} connections)`;

    tbody.innerHTML = users.map(s => `
      <tr>
        <td>
          <span class="nick">
            <span class="dot ${s.connected ? "dot-green" : "dot-red"}"></span>
            <span>${escapeHtml(s.username)}</span>
          </span>
        </td>
        <td>${Number(s.connections || 0)}</td>
        <td>${fmtBytes(s.bytesUploaded || 0)}</td>
        <td>${fmtBytes(s.bytesDownloaded || 0)}</td>
        <td>${escapeHtml(s.lastLogin || "")}</td>
      </tr>
    `).join("");
  } catch (e) {
    hint.textContent = "No data";
    connectedNow.textContent = "";
    showBanner("err", `Failed to load stats: ${e.message}`);
  }
}

/* =========================
 * ROOT
 * ========================= */

async function renderRoot() {
  setView(`
    <section class="card">
      <div class="cardhead">
        <div>
          <div class="cardtitle">Root</div>
          <div class="cardsub">FTP server root directory management</div>
        </div>
        <div class="toolbar">
          <button id="refreshRootBtn" class="btn" type="button">Refresh</button>
        </div>
      </div>
      <div class="cardbody">
        <div class="block">
          <div class="sectiontitle">Current Root</div>
          <div class="sectiondesc">Current FTP server root directory and its contents.</div>
          <div class="sectionbody">
            <div id="rootInfo" class="hint">Loading...</div>
          </div>
        </div>

        <div class="block">
          <div class="sectiontitle">Set Root Path</div>
          <div class="sectiondesc">Change the root directory path. Requires server restart to take effect.</div>
          <div class="sectionbody">
            <label class="label" for="rootPathInput">Root path</label>
            <input id="rootPathInput" class="field" type="text" placeholder="e.g. D:\\ftp-root" style="width: 100%; max-width: 600px;" />
            <div class="toolbar" style="margin-top: 10px;">
              <button id="setRootBtn" class="btn btn-primary" type="button">Set root</button>
            </div>
          </div>
        </div>

        <div class="block">
          <div class="sectiontitle">Create New Root</div>
          <div class="sectiondesc">Create a new root directory with empty database and required folders.</div>
          <div class="sectionbody">
            <label class="label" for="newRootPathInput">New root path</label>
            <input id="newRootPathInput" class="field" type="text" placeholder="e.g. D:\\new-ftp-root" style="width: 100%; max-width: 600px;" />
            <div class="muted" style="margin-top: 6px; color: #999;">
              This will create: ftp.db (empty), /shared folder, /users folder
            </div>
            <div class="toolbar" style="margin-top: 10px;">
              <button id="createRootBtn" class="btn btn-primary" type="button">Create root</button>
            </div>
          </div>
        </div>
      </div>
    </section>
  `);

  qs("#refreshRootBtn").addEventListener("click", loadRootInfo);
  qs("#setRootBtn").addEventListener("click", setRootPath);
  qs("#createRootBtn").addEventListener("click", createRoot);
  await loadRootInfo();
}

async function loadRootInfo() {
  clearBanner();
  const rootInfo = qs("#rootInfo");
  rootInfo.textContent = "Loading...";
  try {
    const info = await apiFetch("/api/root");
    rootInfo.innerHTML = `
      <div style="line-height: 1.8;">
        <div><strong>Root path:</strong> <code>${escapeHtml(info.currentFtpRoot)}</code></div>
        <div><strong>Database:</strong> <code>${escapeHtml(info.currentDbPath)}</code> ${info.dbExists ? '<span style="color: green;">✓</span>' : '<span style="color: red;">✗</span>'}</div>
        <div><strong>Shared folder:</strong> <code>${escapeHtml(info.sharedPath)}</code> ${info.sharedExists ? '<span style="color: green;">✓</span>' : '<span style="color: red;">✗</span>'}</div>
        <div><strong>Users folder:</strong> <code>${escapeHtml(info.usersPath)}</code> ${info.usersExists ? '<span style="color: green;">✓</span>' : '<span style="color: red;">✗</span>'}</div>
      </div>
    `;
    qs("#rootPathInput").value = info.currentFtpRoot;
  } catch (e) {
    rootInfo.textContent = "Failed to load root info";
    showBanner("err", `Failed to load root info: ${e.message}`);
  }
}

async function setRootPath() {
  clearBanner();
  const rootPath = qs("#rootPathInput").value.trim();
  if (!rootPath) {
    showBanner("warn", "Root path is required");
    return;
  }

  if (!confirm(`Set root path to:\n${rootPath}\n\nServer restart is required for this change to take effect. Continue?`)) {
    return;
  }

  try {
    const result = await apiFetch("/api/root", { method: "PUT", json: { ftpRoot: rootPath } });
    showBanner("warn", "Root path saved. Please restart the server for changes to take effect.");
    await loadRootInfo();
  } catch (e) {
    showBanner("err", `Failed to set root path: ${e.message}`);
  }
}

async function createRoot() {
  clearBanner();
  const rootPath = qs("#newRootPathInput").value.trim();
  if (!rootPath) {
    showBanner("warn", "Root path is required");
    return;
  }

  if (!confirm(`Create new root at:\n${rootPath}\n\nThis will create:\n- Empty ftp.db with schema\n- /shared folder\n- /users folder\n\nServer restart is required. Continue?`)) {
    return;
  }

  try {
    const result = await apiFetch("/api/root/create", { method: "POST", json: { ftpRoot: rootPath } });
    showBanner("ok", `Root created successfully at:\n${result.ftpRoot}\n\nPlease restart the server to use the new root.`);
    qs("#newRootPathInput").value = "";
    await loadRootInfo();
  } catch (e) {
    showBanner("err", `Failed to create root: ${e.message}`);
  }
}

/* =========================
 * Helpers
 * ========================= */

function escapeHtml(s) {
  return String(s ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function escapeHtmlAttr(s) {
  return escapeHtml(s).replaceAll("`", "&#96;");
}

/* =========================
 * Boot
 * ========================= */

function init() {
  // No token required - auth removed

  window.addEventListener("hashchange", route);
  if (!location.hash) location.hash = "#/users";

  // On entry: just verify server is online
  bootstrapAuth().finally(() => route());

  // Keep checking server health; if it goes down, show offline overlay and block actions.
  setInterval(() => pingServer(true), 2000);
}

init();


