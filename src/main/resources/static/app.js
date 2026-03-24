const API_BASE = "";
const TOKEN_KEY = "access_token";
const USER_KEY = "auth_user";

const loginView = document.getElementById("login-view");
const registerView = document.getElementById("register-view");
const dashboardView = document.getElementById("dashboard-view");

const loginForm = document.getElementById("login-form");
const registerForm = document.getElementById("register-form");
const loginError = document.getElementById("login-error");
const registerError = document.getElementById("register-error");
const registerSuccess = document.getElementById("register-success");

const showRegisterLink = document.getElementById("show-register-link");
const showLoginLink = document.getElementById("show-login-link");

const welcomeText = document.getElementById("welcome-text");
const menuNav = document.getElementById("menu-nav");
const logoutBtn = document.getElementById("logout-btn");

const pagePos = document.getElementById("page-pos");
const pageDynamic = document.getElementById("page-dynamic");
const pagePlaceholder = document.getElementById("page-placeholder");
const dynamicPageTitle = document.getElementById("dynamic-page-title");
const dynamicPageDescription = document.getElementById("dynamic-page-description");
const dynamicPageContent = document.getElementById("dynamic-page-content");
const placeholderTitle = document.getElementById("placeholder-title");
const placeholderDescription = document.getElementById("placeholder-description");
const tableList = document.getElementById("table-list");
const menuItemList = document.getElementById("menu-item-list");

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

function getUser() {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch (error) {
    return null;
  }
}

function clearAuth() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

function showAuthMode(mode) {
  dashboardView.classList.add("hidden");
  if (mode === "register") {
    loginView.classList.add("hidden");
    registerView.classList.remove("hidden");
  } else {
    registerView.classList.add("hidden");
    loginView.classList.remove("hidden");
  }
}

function hideAllPages() {
  pagePos.classList.add("hidden");
  pageDynamic.classList.add("hidden");
  pagePlaceholder.classList.add("hidden");
}

function renderResultList(items, formatter) {
  const ul = document.createElement("ul");
  ul.className = "result-list";
  if (!items || items.length === 0) {
    const li = document.createElement("li");
    li.className = "muted";
    li.textContent = "Khong co du lieu";
    ul.appendChild(li);
    return ul;
  }
  items.forEach((item) => {
    const li = document.createElement("li");
    li.textContent = formatter(item);
    ul.appendChild(li);
  });
  return ul;
}

function fillExistingList(target, items, formatter) {
  target.innerHTML = "";
  if (!items || items.length === 0) {
    const li = document.createElement("li");
    li.className = "muted";
    li.textContent = "Khong co du lieu";
    target.appendChild(li);
    return;
  }
  items.forEach((item) => {
    const li = document.createElement("li");
    li.textContent = formatter(item);
    target.appendChild(li);
  });
}

async function request(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  const token = getToken();
  const isAuthEndpoint = path.startsWith("/api/auth/");
  if (token && !isAuthEndpoint) {
    headers.Authorization = `Bearer ${token}`;
  }
  if (options.body && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let body = null;
    try {
      body = await response.json();
    } catch (error) {
      body = null;
    }
    const detailMessage =
      body?.details && typeof body.details === "object"
        ? Object.entries(body.details)
            .map(([field, msg]) => `${field}: ${msg}`)
            .join(", ")
        : "";
    const message = detailMessage || body?.error || body?.message || "Request failed";
    throw new Error(message);
  }

  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    return response.json();
  }
  if (contentType.includes("application/pdf") || contentType.includes("text/csv")) {
    return response.blob();
  }
  return response.text();
}

async function login(payload) {
  return request("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

async function register(payload) {
  return request("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

function resolveMenus(roles) {
  const isAdmin = roles.includes("ROLE_ADMIN");
  const isManager = roles.includes("ROLE_MANAGER");
  const isStaff = roles.includes("ROLE_STAFF") || isManager || isAdmin;

  const menus = [];
  if (isStaff) {
    menus.push(
      { id: "pos", title: "POS", description: "Ban va menu cho nhan vien" },
      { id: "customers", title: "Customers", description: "Quan ly thanh vien va diem" },
      { id: "attendance", title: "Attendance", description: "Check-in / Check-out" },
    );
  }
  if (isManager || isAdmin) {
    menus.push(
      { id: "tables", title: "Tables", description: "Danh sach ban va tao ban moi" },
      { id: "menu", title: "Menu", description: "Danh muc va mon an" },
      { id: "inventory", title: "Inventory", description: "Kho nguyen lieu va log dieu chinh" },
      { id: "employees", title: "Employees", description: "Quan ly nhan vien" },
      { id: "payroll", title: "Payroll", description: "Tong hop luong theo thoi gian" },
    );
  }
  if (isAdmin) {
    menus.push(
      { id: "users", title: "Users", description: "Danh sach user va gan role" },
      { id: "roles", title: "Roles", description: "Tao role moi" },
      { id: "reports", title: "Reports", description: "Bao cao doanh thu, top mon, export CSV" },
      { id: "audit-logs", title: "Audit Logs", description: "Lich su thao tac he thong" },
    );
  }
  return menus;
}

function setActiveMenu(activeId) {
  Array.from(menuNav.querySelectorAll("a")).forEach((anchor) => {
    anchor.classList.toggle("active", anchor.dataset.page === activeId);
  });
}

function renderDynamicLayout(title, description) {
  hideAllPages();
  pageDynamic.classList.remove("hidden");
  dynamicPageTitle.textContent = title;
  dynamicPageDescription.textContent = description;
  dynamicPageContent.innerHTML = "";
}

function buildActionCard(title, description) {
  const card = document.createElement("div");
  card.className = "card";
  const heading = document.createElement("h4");
  heading.textContent = title;
  const hint = document.createElement("p");
  hint.className = "muted";
  hint.textContent = description;
  const msg = document.createElement("p");
  msg.className = "error hidden";
  card.appendChild(heading);
  card.appendChild(hint);
  return { card, msg };
}

function appendJsonPre(container, data) {
  const pre = document.createElement("pre");
  pre.className = "json-box";
  pre.textContent = JSON.stringify(data, null, 2);
  container.appendChild(pre);
}

async function renderPos() {
  hideAllPages();
  pagePos.classList.remove("hidden");
  tableList.innerHTML = "<li>Dang tai...</li>";
  menuItemList.innerHTML = "<li>Dang tai...</li>";
  try {
    const [tables, menuItems] = await Promise.all([
      request("/api/staff/tables"),
      request("/api/staff/menu-items"),
    ]);
    fillExistingList(tableList, tables, (table) => `${table.name} - ${table.status} (capacity: ${table.capacity ?? "-"})`);
    fillExistingList(
      menuItemList,
      menuItems,
      (item) =>
        `${item.name} - ${Number(item.price || 0).toLocaleString("vi-VN")} VND - ${item.isAvailable ? "Available" : "Unavailable"}`,
    );
  } catch (error) {
    tableList.innerHTML = `<li class="error">${error.message}</li>`;
    menuItemList.innerHTML = `<li class="error">${error.message}</li>`;
  }
}

async function renderCustomersPage() {
  renderDynamicLayout("Customers", "Tao khach hang, tim theo phone, xem voucher va lich su diem");
  const grid = document.createElement("div");
  grid.className = "grid";

  const create = buildActionCard("Tao khach hang", "POST /api/staff/customers");
  const createForm = document.createElement("form");
  createForm.innerHTML = `
    <label>Ho va ten</label><input name="fullName" required />
    <label>So dien thoai</label><input name="phone" required />
    <button type="submit">Tao</button>
  `;
  createForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    create.msg.classList.add("hidden");
    const fd = new FormData(createForm);
    try {
      const res = await request("/api/staff/customers", {
        method: "POST",
        body: JSON.stringify({ fullName: fd.get("fullName"), phone: fd.get("phone") }),
      });
      appendJsonPre(create.card, res);
    } catch (error) {
      create.msg.textContent = error.message;
      create.msg.classList.remove("hidden");
    }
  });
  create.card.appendChild(createForm);
  create.card.appendChild(create.msg);

  const search = buildActionCard("Tim theo phone", "GET /api/staff/customers/by-phone");
  const searchForm = document.createElement("form");
  searchForm.innerHTML = `
    <label>So dien thoai</label><input name="phone" required />
    <button type="submit">Tim</button>
  `;
  searchForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    search.msg.classList.add("hidden");
    const phone = new FormData(searchForm).get("phone");
    try {
      const res = await request(`/api/staff/customers/by-phone?phone=${encodeURIComponent(String(phone || ""))}`);
      appendJsonPre(search.card, res);
    } catch (error) {
      search.msg.textContent = error.message;
      search.msg.classList.remove("hidden");
    }
  });
  search.card.appendChild(searchForm);
  search.card.appendChild(search.msg);

  grid.appendChild(create.card);
  grid.appendChild(search.card);
  dynamicPageContent.appendChild(grid);
}

async function renderAttendancePage() {
  renderDynamicLayout("Attendance", "Check-in / Check-out theo so dien thoai");
  const grid = document.createElement("div");
  grid.className = "grid";
  ["check-in", "check-out"].forEach((action) => {
    const box = buildActionCard(action === "check-in" ? "Check-in" : "Check-out", `POST /api/staff/attendance/${action}`);
    const form = document.createElement("form");
    form.innerHTML = `
      <label>So dien thoai nhan vien</label><input name="phone" required />
      <button type="submit">${action === "check-in" ? "Check-in" : "Check-out"}</button>
    `;
    form.addEventListener("submit", async (event) => {
      event.preventDefault();
      box.msg.classList.add("hidden");
      const phone = new FormData(form).get("phone");
      try {
        const res = await request(`/api/staff/attendance/${action}`, {
          method: "POST",
          body: JSON.stringify({ phone }),
        });
        appendJsonPre(box.card, res);
      } catch (error) {
        box.msg.textContent = error.message;
        box.msg.classList.remove("hidden");
      }
    });
    box.card.appendChild(form);
    box.card.appendChild(box.msg);
    grid.appendChild(box.card);
  });
  dynamicPageContent.appendChild(grid);
}

async function renderTablesPage() {
  renderDynamicLayout("Tables", "Danh sach ban va tao ban moi");
  const list = await request("/api/staff/tables");
  dynamicPageContent.appendChild(renderResultList(list, (t) => `${t.id}. ${t.name} - ${t.status} - ${t.capacity ?? "-"}`));

  const box = buildActionCard("Tao ban", "POST /api/manager/tables");
  const form = document.createElement("form");
  form.innerHTML = `
    <label>Ten ban</label><input name="name" required />
    <label>Suc chua</label><input name="capacity" type="number" min="1" />
    <label>Trang thai</label>
    <select name="status">
      <option value="empty">empty</option>
      <option value="occupied">occupied</option>
      <option value="reserved">reserved</option>
    </select>
    <label>Ghi chu</label><input name="note" />
    <button type="submit">Tao ban</button>
  `;
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    box.msg.classList.add("hidden");
    const fd = new FormData(form);
    const payload = {
      name: fd.get("name"),
      capacity: fd.get("capacity") ? Number(fd.get("capacity")) : null,
      status: fd.get("status"),
      note: fd.get("note"),
    };
    try {
      const res = await request("/api/manager/tables", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      appendJsonPre(box.card, res);
    } catch (error) {
      box.msg.textContent = error.message;
      box.msg.classList.remove("hidden");
    }
  });
  box.card.appendChild(form);
  box.card.appendChild(box.msg);
  dynamicPageContent.appendChild(box.card);
}

async function renderMenuPage() {
  renderDynamicLayout("Menu", "Category va Menu Item");
  const [categories, items] = await Promise.all([request("/api/staff/categories"), request("/api/staff/menu-items")]);
  const grid = document.createElement("div");
  grid.className = "grid";
  const cate = document.createElement("div");
  cate.className = "card";
  cate.innerHTML = "<h4>Categories</h4>";
  cate.appendChild(renderResultList(categories, (c) => `${c.id}. ${c.name}`));
  const menu = document.createElement("div");
  menu.className = "card";
  menu.innerHTML = "<h4>Menu Items</h4>";
  menu.appendChild(renderResultList(items, (m) => `${m.id}. ${m.name} - ${m.price}`));
  grid.appendChild(cate);
  grid.appendChild(menu);
  dynamicPageContent.appendChild(grid);
}

async function renderInventoryPage() {
  renderDynamicLayout("Inventory", "Nguyen lieu, low-stock va nhat ky");
  const [ingredients, lowStock] = await Promise.all([
    request("/api/manager/inventory/ingredients"),
    request("/api/manager/inventory/ingredients/low-stock"),
  ]);
  const grid = document.createElement("div");
  grid.className = "grid";
  const allCard = document.createElement("div");
  allCard.className = "card";
  allCard.innerHTML = "<h4>All Ingredients</h4>";
  allCard.appendChild(renderResultList(ingredients, (i) => `${i.name}: ${i.stockQuantity} ${i.unit} (min ${i.minThreshold})`));
  const lowCard = document.createElement("div");
  lowCard.className = "card";
  lowCard.innerHTML = "<h4>Low Stock</h4>";
  lowCard.appendChild(renderResultList(lowStock, (i) => `${i.name}: ${i.stockQuantity} ${i.unit}`));
  grid.appendChild(allCard);
  grid.appendChild(lowCard);
  dynamicPageContent.appendChild(grid);
}

async function renderEmployeesPage() {
  renderDynamicLayout("Employees", "Danh sach nhan vien va attendance hom nay");
  const [employees, today] = await Promise.all([
    request("/api/manager/employees"),
    request("/api/manager/attendance/today"),
  ]);
  const grid = document.createElement("div");
  grid.className = "grid";
  const emp = document.createElement("div");
  emp.className = "card";
  emp.innerHTML = "<h4>Employees</h4>";
  emp.appendChild(renderResultList(employees, (e) => `${e.fullName} - ${e.phone} - ${e.position || "-"}`));
  const att = document.createElement("div");
  att.className = "card";
  att.innerHTML = "<h4>Attendance Today</h4>";
  att.appendChild(renderResultList(today, (a) => `${a.employeeName || a.phone} - in: ${a.checkInTime || "-"} out: ${a.checkOutTime || "-"}`));
  grid.appendChild(emp);
  grid.appendChild(att);
  dynamicPageContent.appendChild(grid);
}

async function renderPayrollPage() {
  renderDynamicLayout("Payroll", "Tong hop luong theo khoang ngay");
  const card = buildActionCard("Payroll Summary", "GET /api/manager/payroll/summary?from&to");
  const form = document.createElement("form");
  form.innerHTML = `
    <label>From</label><input type="date" name="from" required />
    <label>To</label><input type="date" name="to" required />
    <button type="submit">Xem tong hop</button>
  `;
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    card.msg.classList.add("hidden");
    const fd = new FormData(form);
    const from = fd.get("from");
    const to = fd.get("to");
    try {
      const res = await request(`/api/manager/payroll/summary?from=${from}&to=${to}`);
      appendJsonPre(card.card, res);
    } catch (error) {
      card.msg.textContent = error.message;
      card.msg.classList.remove("hidden");
    }
  });
  card.card.appendChild(form);
  card.card.appendChild(card.msg);
  dynamicPageContent.appendChild(card.card);
}

async function renderUsersPage() {
  renderDynamicLayout("Users", "Danh sach user va role hien tai");
  const users = await request("/api/admin/users");
  dynamicPageContent.appendChild(renderResultList(users, (u) => `${u.id}. ${u.username} - ${(u.roles || []).join(", ")}`));
}

async function renderRolesPage() {
  renderDynamicLayout("Roles", "Danh sach role va tao role moi");
  const roles = await request("/api/admin/roles");
  dynamicPageContent.appendChild(renderResultList(roles, (r) => `${r.id}. ${r.name}`));
}

async function renderReportsPage() {
  renderDynamicLayout("Reports", "Bao cao doanh thu theo ngay/thang va top menu");
  const card = buildActionCard("Daily Sales", "GET /api/admin/reports/sales/daily?date");
  const form = document.createElement("form");
  form.innerHTML = `
    <label>Ngay</label><input type="date" name="date" required />
    <button type="submit">Xem doanh thu ngay</button>
  `;
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    card.msg.classList.add("hidden");
    const date = new FormData(form).get("date");
    try {
      const res = await request(`/api/admin/reports/sales/daily?date=${date}`);
      appendJsonPre(card.card, res);
    } catch (error) {
      card.msg.textContent = error.message;
      card.msg.classList.remove("hidden");
    }
  });
  card.card.appendChild(form);
  card.card.appendChild(card.msg);
  dynamicPageContent.appendChild(card.card);
}

async function renderAuditLogsPage() {
  renderDynamicLayout("Audit Logs", "Lich su hanh dong tu backend");
  const data = await request("/api/admin/audit-logs");
  dynamicPageContent.appendChild(
    renderResultList(data, (a) => `${a.createdAt || a.timestamp || "-"} - ${a.username || "-"} - ${a.action || "-"}`),
  );
}

function renderPlaceholder(menu) {
  hideAllPages();
  pagePlaceholder.classList.remove("hidden");
  placeholderTitle.textContent = menu.title;
  placeholderDescription.textContent = `${menu.description}.`;
}

async function renderPage(menu) {
  try {
    if (menu.id === "pos") return await renderPos();
    if (menu.id === "customers") return await renderCustomersPage();
    if (menu.id === "attendance") return await renderAttendancePage();
    if (menu.id === "tables") return await renderTablesPage();
    if (menu.id === "menu") return await renderMenuPage();
    if (menu.id === "inventory") return await renderInventoryPage();
    if (menu.id === "employees") return await renderEmployeesPage();
    if (menu.id === "payroll") return await renderPayrollPage();
    if (menu.id === "users") return await renderUsersPage();
    if (menu.id === "roles") return await renderRolesPage();
    if (menu.id === "reports") return await renderReportsPage();
    if (menu.id === "audit-logs") return await renderAuditLogsPage();
    renderPlaceholder(menu);
  } catch (error) {
    renderDynamicLayout(menu.title, menu.description);
    const err = document.createElement("p");
    err.className = "error";
    err.textContent = error.message || "Khong the tai du lieu";
    dynamicPageContent.appendChild(err);
  }
}

function showDashboard(user) {
  loginView.classList.add("hidden");
  registerView.classList.add("hidden");
  dashboardView.classList.remove("hidden");
  welcomeText.textContent = `Xin chao ${user.username} (${(user.roles || []).join(", ")})`;

  const menus = resolveMenus(user.roles || []);
  menuNav.innerHTML = "";
  menus.forEach((menu) => {
    const link = document.createElement("a");
    link.href = "#";
    link.dataset.page = menu.id;
    link.textContent = menu.title;
    link.addEventListener("click", async (event) => {
      event.preventDefault();
      setActiveMenu(menu.id);
      await renderPage(menu);
    });
    menuNav.appendChild(link);
  });

  if (menus.length > 0) {
    setActiveMenu(menus[0].id);
    renderPage(menus[0]);
  }
}

loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  loginError.classList.add("hidden");
  const formData = new FormData(loginForm);
  const payload = {
    usernameOrEmail: String(formData.get("usernameOrEmail") || "").trim(),
    password: String(formData.get("password") || ""),
  };
  try {
    const auth = await login(payload);
    setToken(auth.token);
    setUser(auth);
    showDashboard(auth);
  } catch (error) {
    loginError.textContent = error.message || "Dang nhap that bai";
    loginError.classList.remove("hidden");
  }
});

registerForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  registerError.classList.add("hidden");
  registerSuccess.classList.add("hidden");
  const formData = new FormData(registerForm);
  const payload = {
    username: String(formData.get("username") || "").trim(),
    fullName: String(formData.get("fullName") || "").trim(),
    email: String(formData.get("email") || "").trim(),
    password: String(formData.get("password") || ""),
  };
  try {
    await register(payload);
    registerSuccess.textContent = "Dang ky thanh cong. Vui long dang nhap.";
    registerSuccess.classList.remove("hidden");
    registerForm.reset();
  } catch (error) {
    registerError.textContent = error.message || "Dang ky that bai";
    registerError.classList.remove("hidden");
  }
});

showRegisterLink.addEventListener("click", (event) => {
  event.preventDefault();
  showAuthMode("register");
});

showLoginLink.addEventListener("click", (event) => {
  event.preventDefault();
  showAuthMode("login");
});

logoutBtn.addEventListener("click", () => {
  clearAuth();
  showAuthMode("login");
});

const storedUser = getUser();
if (storedUser && getToken()) {
  showDashboard(storedUser);
} else {
  showAuthMode("login");
}
