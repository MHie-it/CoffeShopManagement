(function () {
  var toggle = document.querySelector(".menu-toggle");
  var sidebar = document.querySelector(".sidebar");
  var backdrop = document.querySelector(".sidebar-backdrop");
  var openModals = [];
  var modalTransitionLock = false;

  function close() {
    if (sidebar) sidebar.classList.remove("open");
    if (backdrop) backdrop.classList.remove("show");
  }

  function open() {
    if (sidebar) sidebar.classList.add("open");
    if (backdrop) backdrop.classList.add("show");
  }

  if (toggle && sidebar) {
    toggle.addEventListener("click", function () {
      if (sidebar.classList.contains("open")) close();
      else open();
    });
  }

  if (backdrop) {
    backdrop.addEventListener("click", close);
  }

  function getErrorMessage(error, fallbackMessage) {
    if (error && error.message) return error.message;
    return fallbackMessage;
  }

  function setInlineMessage(el, message, isError) {
    if (!el) return;
    el.textContent = message || "";
    el.classList.remove("error", "success");
    if (!message) return;
    el.classList.add(isError ? "error" : "success");
  }

  function apiFetch(url, options) {
    return fetch(url, options).then(function (response) {
      if (!response.ok) {
        return response
          .json()
          .catch(function () {
            return {};
          })
          .then(function (errorBody) {
            var message = errorBody.message || "Có lỗi xảy ra. Vui lòng thử lại.";
            throw new Error(message);
          });
      }
      return response.json();
    });
  }

  function formatNumber(value) {
    var parsed = Number(value || 0);
    return new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 2 }).format(parsed);
  }

  function formatCurrency(value) {
    var parsed = Number(value || 0);
    return new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND", maximumFractionDigits: 0 }).format(parsed);
  }

  function mapInventoryType(type) {
    if (type === "import_") return "Nhập";
    if (type === "export") return "Xuất";
    return "Điều chỉnh";
  }

  function getVisibleModals() {
    return Array.prototype.slice.call(document.querySelectorAll(".modal")).filter(function (modal) {
      return !modal.hidden;
    });
  }

  function syncBodyModalClass() {
    if (getVisibleModals().length > 0) {
      document.body.classList.add("modal-open");
      return;
    }
    document.body.classList.remove("modal-open");
  }

  function resetModalStateOnBoot() {
    Array.prototype.slice.call(document.querySelectorAll(".modal")).forEach(function (modal) {
      modal.hidden = true;
    });
    openModals = [];
    syncBodyModalClass();
  }

  function openModalById(modalId) {
    if (modalTransitionLock) return;
    var modal = document.getElementById(modalId);
    if (!modal) return;
    modalTransitionLock = true;
    window.setTimeout(function () {
      modalTransitionLock = false;
    }, 120);

    closeAllModals();
    modal.hidden = false;
    if (openModals.indexOf(modal) === -1) {
      openModals.push(modal);
    }
    syncBodyModalClass();
    modal.dispatchEvent(new CustomEvent("modal:open"));
  }

  function closeModal(modal) {
    if (!modal) return;
    modal.hidden = true;
    openModals = openModals.filter(function (item) {
      return item !== modal;
    });
    syncBodyModalClass();
  }

  function closeAllModals() {
    getVisibleModals().forEach(function (modal) {
      modal.hidden = true;
    });
    openModals = [];
    syncBodyModalClass();
  }

  document.addEventListener("click", function (event) {
    var opener = event.target.closest("[data-open-modal]");
    if (opener) {
      openModalById(opener.getAttribute("data-open-modal"));
      return;
    }

    var closer = event.target.closest("[data-close-modal]");
    if (closer) {
      closeModal(closer.closest(".modal"));
      return;
    }
  });

  document.addEventListener("keydown", function (e) {
    if (e.key !== "Escape") return;
    var visibleModals = getVisibleModals();
    var currentModal = visibleModals[visibleModals.length - 1] || openModals[openModals.length - 1];
    if (currentModal) {
      closeModal(currentModal);
      return;
    }
    close();
  });

  resetModalStateOnBoot();

  function clearInvalidState(input) {
    if (input) input.classList.remove("invalid");
  }

  function markInvalid(input) {
    if (input) input.classList.add("invalid");
  }

  function initRegisterValidation() {
    var registerForm = document.querySelector("form[data-auth-register]");
    if (!registerForm) return;

    var emailInput = registerForm.querySelector("#email");
    var passwordInput = registerForm.querySelector("#password");
    var confirmPasswordInput = registerForm.querySelector("#confirmPassword");

    registerForm.addEventListener("submit", function (e) {
      var isValid = true;

      if (emailInput) {
        clearInvalidState(emailInput);
        var isEmailValid = emailInput.checkValidity();
        if (!isEmailValid) {
          markInvalid(emailInput);
          isValid = false;
        }
      }

      if (passwordInput && confirmPasswordInput) {
        clearInvalidState(passwordInput);
        clearInvalidState(confirmPasswordInput);
        if (passwordInput.value !== confirmPasswordInput.value) {
          markInvalid(passwordInput);
          markInvalid(confirmPasswordInput);
          isValid = false;
        }
      }

      if (!isValid) {
        e.preventDefault();
      }
    });
  }

  function initInventoryPage() {
    var inventoryTableBody = document.getElementById("inventoryTableBody");
    if (!inventoryTableBody) return;

    var lowStockSummary = document.getElementById("lowStockSummary");
    var receiptForm = document.getElementById("inventoryReceiptForm");
    var receiptIngredient = document.getElementById("receiptIngredient");
    var receiptType = document.getElementById("receiptType");
    var receiptQuantity = document.getElementById("receiptQuantity");
    var receiptNote = document.getElementById("receiptNote");
    var receiptMessage = document.getElementById("inventoryReceiptMessage");
    var historyBody = document.getElementById("inventoryHistoryBody");
    var historyModal = document.getElementById("inventoryHistoryModal");

    function loadIngredients() {
      return apiFetch("/api/manager/inventory/ingredients").then(function (ingredients) {
        if (!ingredients.length) {
          inventoryTableBody.innerHTML = '<tr><td colspan="5" class="muted">Chưa có nguyên liệu.</td></tr>';
          if (receiptIngredient) {
            receiptIngredient.innerHTML = "<option value=''>Chưa có nguyên liệu</option>";
          }
          return ingredients;
        }

        inventoryTableBody.innerHTML = ingredients
          .map(function (item) {
            var statusText = item.lowStock ? "Sắp hết" : "Ổn định";
            var statusColor = item.lowStock ? "var(--warning)" : "var(--success)";
            return (
              "<tr>" +
              "<td>" + item.name + "</td>" +
              "<td>" + item.unit + "</td>" +
              "<td>" + formatNumber(item.stockQuantity) + "</td>" +
              "<td>" + formatNumber(item.minThreshold) + "</td>" +
              "<td style='color:" + statusColor + ";font-weight:600'>" + statusText + "</td>" +
              "</tr>"
            );
          })
          .join("");

        if (receiptIngredient) {
          receiptIngredient.innerHTML = ingredients
            .map(function (item) {
              return "<option value='" + item.id + "'>" + item.name + " (" + item.unit + ")</option>";
            })
            .join("");
        }

        return ingredients;
      });
    }

    function loadLowStock() {
      return apiFetch("/api/manager/inventory/ingredients/low-stock")
        .then(function (items) {
          if (!lowStockSummary) return;
          if (!items.length) {
            lowStockSummary.textContent = "Không có nguyên liệu dưới ngưỡng tối thiểu.";
            return;
          }
          lowStockSummary.textContent = items.map(function (x) {
            return x.name;
          }).join(", ") + " dưới ngưỡng tối thiểu.";
        })
        .catch(function () {
          if (lowStockSummary) {
            lowStockSummary.textContent = "Không tải được dữ liệu cảnh báo tồn kho.";
          }
        });
    }

    function loadLogs() {
      return apiFetch("/api/manager/inventory/logs")
        .then(function (logs) {
          if (!historyBody) return;
          if (!logs.length) {
            historyBody.innerHTML = '<tr><td colspan="6" class="muted">Chưa có lịch sử nhập/xuất.</td></tr>';
            return;
          }
          historyBody.innerHTML = logs
            .slice(0, 30)
            .map(function (log) {
              var time = log.createdAt ? new Date(log.createdAt).toLocaleString("vi-VN") : "-";
              return (
                "<tr>" +
                "<td>" + time + "</td>" +
                "<td>" + (log.ingredientName || "-") + "</td>" +
                "<td>" + mapInventoryType(log.type) + "</td>" +
                "<td>" + formatNumber(log.quantity) + "</td>" +
                "<td>" + (log.username || "-") + "</td>" +
                "<td>" + (log.note || "-") + "</td>" +
                "</tr>"
              );
            })
            .join("");
        })
        .catch(function () {
          if (historyBody) {
            historyBody.innerHTML = '<tr><td colspan="6" class="muted">Không tải được lịch sử kho.</td></tr>';
          }
        });
    }

    if (historyModal) {
      historyModal.addEventListener("modal:open", function () {
        loadLogs();
      });
    }

    if (receiptForm) {
      receiptForm.addEventListener("submit", function (event) {
        event.preventDefault();
        setInlineMessage(receiptMessage, "", false);

        var ingredientId = Number(receiptIngredient ? receiptIngredient.value : 0);
        if (!ingredientId) {
          setInlineMessage(receiptMessage, "Vui lòng chọn nguyên liệu.", true);
          return;
        }

        apiFetch("/api/manager/inventory/ingredients/" + ingredientId + "/adjust", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            type: receiptType ? receiptType.value : "import_",
            quantity: Number(receiptQuantity ? receiptQuantity.value : 0),
            note: receiptNote ? receiptNote.value : ""
          })
        })
          .then(function () {
            setInlineMessage(receiptMessage, "Đã lưu phiếu kho thành công.", false);
            if (receiptForm) receiptForm.reset();
            if (receiptType) receiptType.value = "import_";
            return Promise.all([loadIngredients(), loadLowStock(), loadLogs()]);
          })
          .catch(function (error) {
            setInlineMessage(receiptMessage, getErrorMessage(error, "Không thể lưu phiếu kho."), true);
          });
      });
    }

    loadIngredients().then(loadLowStock).catch(function () {
      inventoryTableBody.innerHTML = '<tr><td colspan="5" class="muted">Không tải được dữ liệu tồn kho.</td></tr>';
      if (lowStockSummary) {
        lowStockSummary.textContent = "Không tải được dữ liệu cảnh báo tồn kho.";
      }
    });
  }

  function initEmployeesPage() {
    var employeeTableBody = document.getElementById("employeeTableBody");
    if (!employeeTableBody) return;

    var form = document.getElementById("employeeCreateForm");
    var message = document.getElementById("employeeCreateMessage");
    var modal = document.getElementById("employeeCreateModal");

    function loadEmployees() {
      return apiFetch("/api/manager/employees")
        .then(function (employees) {
          if (!employees.length) {
            employeeTableBody.innerHTML = '<tr><td colspan="5" class="muted">Chưa có nhân viên.</td></tr>';
            return;
          }
          employeeTableBody.innerHTML = employees
            .map(function (employee) {
              var shift = employee.shiftCode || "-";
              var rate = employee.hourlyRate != null ? formatCurrency(employee.hourlyRate) : "-";
              return (
                "<tr>" +
                "<td>" + employee.fullName + "</td>" +
                "<td>" + employee.phone + "</td>" +
                "<td>" + shift + "</td>" +
                "<td>" + rate + "</td>" +
                "<td><button type='button' class='btn btn-ghost btn-xs' data-employee-detail='" + employee.id + "'>Chi tiết</button></td>" +
                "</tr>"
              );
            })
            .join("");
        })
        .catch(function () {
          employeeTableBody.innerHTML = '<tr><td colspan="5" class="muted">Không tải được danh sách nhân viên.</td></tr>';
        });
    }

    if (employeeTableBody) {
      employeeTableBody.addEventListener("click", function (event) {
        var detailButton = event.target.closest("[data-employee-detail]");
        if (!detailButton) return;
        alert("Thông tin chi tiết đã được kích hoạt. Bạn có thể mở rộng modal chi tiết ở bước tiếp theo.");
      });
    }

    if (modal) {
      modal.addEventListener("modal:open", function () {
        setInlineMessage(message, "", false);
      });
    }

    if (form) {
      form.addEventListener("submit", function (event) {
        event.preventDefault();
        setInlineMessage(message, "", false);

        var payload = {
          fullName: (document.getElementById("employeeFullName") || {}).value || "",
          phone: (document.getElementById("employeePhone") || {}).value || "",
          position: (document.getElementById("employeePosition") || {}).value || null,
          shiftCode: (document.getElementById("employeeShiftCode") || {}).value || null,
          hourlyRate: (document.getElementById("employeeHourlyRate") || {}).value || null,
          isActive: true
        };

        apiFetch("/api/manager/employees", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload)
        })
          .then(function () {
            setInlineMessage(message, "Đã tạo nhân viên mới.", false);
            form.reset();
            return loadEmployees();
          })
          .catch(function (error) {
            setInlineMessage(message, getErrorMessage(error, "Không thể tạo nhân viên."), true);
          });
      });
    }

    loadEmployees();
  }

  function initMenuPage() {
    var menuCards = document.getElementById("menuCards");
    if (!menuCards) return;

    var categoryList = document.getElementById("categoryList");
    var categoryCreateForm = document.getElementById("categoryCreateForm");
    var categoryMessage = document.getElementById("categoryCreateMessage");
    var menuItemForm = document.getElementById("menuItemCreateForm");
    var menuItemMessage = document.getElementById("menuItemCreateMessage");
    var menuItemCategory = document.getElementById("menuItemCategory");

    function loadCategories() {
      return apiFetch("/api/staff/categories")
        .then(function (categories) {
          if (categoryList) {
            if (!categories.length) {
              categoryList.innerHTML = "<p class='muted'>Chưa có danh mục nào.</p>";
            } else {
              categoryList.innerHTML = categories
                .map(function (category) {
                  return "<div class='list-row'><strong>" + category.name + "</strong><span>#" + (category.sortOrder == null ? 0 : category.sortOrder) + "</span></div>";
                })
                .join("");
            }
          }

          if (menuItemCategory) {
            if (!categories.length) {
              menuItemCategory.innerHTML = "<option value=''>Chưa có danh mục</option>";
            } else {
              menuItemCategory.innerHTML = categories
                .map(function (category) {
                  return "<option value='" + category.id + "'>" + category.name + "</option>";
                })
                .join("");
            }
          }

          return categories;
        })
        .catch(function () {
          if (categoryList) {
            categoryList.innerHTML = "<p class='muted'>Không tải được danh mục.</p>";
          }
          if (menuItemCategory) {
            menuItemCategory.innerHTML = "<option value=''>Không tải được danh mục</option>";
          }
          return [];
        });
    }

    function loadMenuItems() {
      return apiFetch("/api/staff/menu-items")
        .then(function (items) {
          if (!items.length) {
            menuCards.innerHTML = "<div class='card'><p class='muted'>Chưa có món nào.</p></div>";
            return;
          }

          menuCards.innerHTML = items
            .map(function (item) {
              var description = item.description || (item.category && item.category.name) || "";
              var image = item.imageUrl
                ? "<img src='" + item.imageUrl + "' alt='" + item.name + "' style='width:100%;aspect-ratio:4/3;object-fit:cover;border-radius:10px;margin-bottom:0.85rem;border:1px solid var(--border)' />"
                : "<div style='aspect-ratio:4/3;background:var(--surface-2);border-radius:10px;margin-bottom:0.85rem;border:1px solid var(--border)'></div>";

              return (
                "<div class='card'>" +
                image +
                "<h3 style='margin:0 0 0.25rem;font-size:1rem'>" + item.name + "</h3>" +
                "<p class='muted' style='font-size:0.85rem;margin:0 0 0.5rem'>" + description + "</p>" +
                "<p style='margin:0;font-weight:700;color:var(--accent)'>" + formatCurrency(item.price) + "</p>" +
                "</div>"
              );
            })
            .join("");
        })
        .catch(function () {
          menuCards.innerHTML = "<div class='card'><p class='muted'>Không tải được danh sách món.</p></div>";
        });
    }

    if (categoryCreateForm) {
      categoryCreateForm.addEventListener("submit", function (event) {
        event.preventDefault();
        setInlineMessage(categoryMessage, "", false);

        apiFetch("/api/manager/categories", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            name: (document.getElementById("categoryName") || {}).value || "",
            sortOrder: Number((document.getElementById("categorySortOrder") || {}).value || 0),
            isActive: true
          })
        })
          .then(function () {
            setInlineMessage(categoryMessage, "Đã thêm danh mục mới.", false);
            categoryCreateForm.reset();
            return Promise.all([loadCategories(), loadMenuItems()]);
          })
          .catch(function (error) {
            setInlineMessage(categoryMessage, getErrorMessage(error, "Không thể thêm danh mục."), true);
          });
      });
    }

    if (menuItemForm) {
      menuItemForm.addEventListener("submit", function (event) {
        event.preventDefault();
        setInlineMessage(menuItemMessage, "", false);

        var categoryId = Number((menuItemCategory || {}).value || 0);
        if (!categoryId) {
          setInlineMessage(menuItemMessage, "Vui lòng chọn danh mục trước khi thêm món.", true);
          return;
        }

        apiFetch("/api/manager/menu-items", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            categoryId: categoryId,
            name: (document.getElementById("menuItemName") || {}).value || "",
            description: (document.getElementById("menuItemDescription") || {}).value || "",
            price: Number((document.getElementById("menuItemPrice") || {}).value || 0),
            imageUrl: (document.getElementById("menuItemImage") || {}).value || null,
            isAvailable: true
          })
        })
          .then(function () {
            setInlineMessage(menuItemMessage, "Đã tạo món mới thành công.", false);
            menuItemForm.reset();
            return loadMenuItems();
          })
          .catch(function (error) {
            setInlineMessage(menuItemMessage, getErrorMessage(error, "Không thể tạo món mới."), true);
          });
      });
    }

    Promise.all([loadCategories(), loadMenuItems()]);
  }

  function initTablesPage() {
    var tableFloorGrid = document.getElementById("tableFloorGrid");
    if (!tableFloorGrid) return;

    var form = document.getElementById("tableCreateForm");
    var message = document.getElementById("tableCreateMessage");

    function tableTileClass(status) {
      if (status === "empty") return "free";
      if (status === "occupied") return "busy";
      return "";
    }

    function tableStatusLabel(status) {
      if (status === "empty") return "Trống";
      if (status === "occupied") return "Đang phục vụ";
      if (status === "reserved") return "Đã đặt";
      return "Không rõ";
    }

    function loadTables() {
      return apiFetch("/api/staff/tables")
        .then(function (tables) {
          var tableCards = tables.map(function (table) {
            return (
              "<div class='table-tile " + tableTileClass(table.status) + "'>" +
              table.name +
              "<span>" + tableStatusLabel(table.status) + "</span>" +
              "</div>"
            );
          });

          tableCards.push(
            "<button type='button' class='table-tile table-tile-add' data-open-modal='tableCreateModal'>+<span>Thêm</span></button>"
          );

          tableFloorGrid.innerHTML = tableCards.join("");
        })
        .catch(function () {
          tableFloorGrid.innerHTML = "<div class='table-tile'>Lỗi<span>Không tải được bàn</span></div>";
        });
    }

    if (form) {
      form.addEventListener("submit", function (event) {
        event.preventDefault();
        setInlineMessage(message, "", false);

        apiFetch("/api/manager/tables", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            name: (document.getElementById("tableName") || {}).value || "",
            capacity: Number((document.getElementById("tableCapacity") || {}).value || 0) || null,
            status: (document.getElementById("tableStatus") || {}).value || "empty",
            note: (document.getElementById("tableNote") || {}).value || ""
          })
        })
          .then(function () {
            setInlineMessage(message, "Đã thêm bàn mới.", false);
            form.reset();
            var statusField = document.getElementById("tableStatus");
            if (statusField) statusField.value = "empty";
            return loadTables();
          })
          .catch(function (error) {
            setInlineMessage(message, getErrorMessage(error, "Không thể thêm bàn."), true);
          });
      });
    }

    loadTables();
  }

  initRegisterValidation();
  initInventoryPage();
  initEmployeesPage();
  initMenuPage();
  initTablesPage();
})();
