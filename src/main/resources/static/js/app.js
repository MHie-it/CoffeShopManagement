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
        if (response.status === 401) {
          throw new Error("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
        }
        return response
          .json()
          .catch(function () {
            return {};
          })
          .then(function (errorBody) {
            var message = errorBody.message || errorBody.error || "Có lỗi xảy ra. Vui lòng thử lại.";
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
    var ingredientCreateForm = document.getElementById("ingredientCreateForm");
    var ingredientCreateMessage = document.getElementById("ingredientCreateMessage");
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

    if (ingredientCreateForm) {
      ingredientCreateForm.addEventListener("submit", function (event) {
        event.preventDefault();
        setInlineMessage(ingredientCreateMessage, "", false);

        apiFetch("/api/manager/inventory/ingredients", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            name: (document.getElementById("ingredientName") || {}).value || "",
            unit: (document.getElementById("ingredientUnit") || {}).value || "",
            stockQuantity: Number((document.getElementById("ingredientStockQuantity") || {}).value || 0),
            minThreshold: Number((document.getElementById("ingredientMinThreshold") || {}).value || 0)
          })
        })
          .then(function () {
            setInlineMessage(ingredientCreateMessage, "Đã thêm nguyên liệu mới.", false);
            ingredientCreateForm.reset();
            closeModal(document.getElementById("ingredientCreateModal"));
            return Promise.all([loadIngredients(), loadLowStock()]);
          })
          .catch(function (error) {
            setInlineMessage(ingredientCreateMessage, getErrorMessage(error, "Không thể tạo nguyên liệu."), true);
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
    var updateForm = document.getElementById("employeeUpdateForm");
    var updateMessage = document.getElementById("employeeUpdateMessage");
    var employeesMap = {};

    function loadEmployees() {
      return apiFetch("/api/manager/employees")
        .then(function (employees) {
          employeesMap = {};
          if (!employees.length) {
            employeeTableBody.innerHTML = '<tr><td colspan="6" class="muted">Chưa có nhân viên.</td></tr>';
            return;
          }
          employeeTableBody.innerHTML = employees
            .map(function (employee) {
              employeesMap[String(employee.id)] = employee;
              var shift = employee.shiftCode || "-";
              var rate = employee.hourlyRate != null ? formatCurrency(employee.hourlyRate) : "-";
              var active = employee.isActive === false ? "<span style='color:var(--danger);font-weight:600'>Ngừng hoạt động</span>" : "<span style='color:var(--success);font-weight:600'>Đang hoạt động</span>";
              return (
                "<tr>" +
                "<td>" + employee.fullName + "</td>" +
                "<td>" + employee.phone + "</td>" +
                "<td>" + shift + "</td>" +
                "<td>" + rate + "</td>" +
                "<td>" + active + "</td>" +
                "<td><button type='button' class='btn btn-ghost btn-xs' data-employee-edit='" + employee.id + "'>Sửa</button></td>" +
                "</tr>"
              );
            })
            .join("");
        })
        .catch(function () {
          employeeTableBody.innerHTML = '<tr><td colspan="6" class="muted">Không tải được danh sách nhân viên.</td></tr>';
        });
    }

    if (employeeTableBody) {
      employeeTableBody.addEventListener("click", function (event) {
        var editButton = event.target.closest("[data-employee-edit]");
        if (!editButton) return;
        var employeeId = editButton.getAttribute("data-employee-edit");
        var employee = employeesMap[String(employeeId)];
        if (!employee) return;

        var setValue = function (id, value) {
          var el = document.getElementById(id);
          if (el) el.value = value == null ? "" : value;
        };
        setValue("employeeUpdateId", employee.id);
        setValue("employeeUpdateFullName", employee.fullName);
        setValue("employeeUpdatePhone", employee.phone);
        setValue("employeeUpdatePosition", employee.position);
        setValue("employeeUpdateShiftCode", employee.shiftCode);
        setValue("employeeUpdateHourlyRate", employee.hourlyRate);
        setValue("employeeUpdateIsActive", employee.isActive === false ? "false" : "true");
        setInlineMessage(updateMessage, "", false);
        openModalById("employeeUpdateModal");
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

    if (updateForm) {
      updateForm.addEventListener("submit", function (event) {
        event.preventDefault();
        setInlineMessage(updateMessage, "", false);
        var employeeId = Number((document.getElementById("employeeUpdateId") || {}).value || 0);
        if (!employeeId) {
          setInlineMessage(updateMessage, "Không xác định được nhân viên cần cập nhật.", true);
          return;
        }

        apiFetch("/api/manager/employees/" + employeeId, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            fullName: (document.getElementById("employeeUpdateFullName") || {}).value || null,
            phone: (document.getElementById("employeeUpdatePhone") || {}).value || null,
            position: (document.getElementById("employeeUpdatePosition") || {}).value || null,
            shiftCode: (document.getElementById("employeeUpdateShiftCode") || {}).value || null,
            hourlyRate: (function () {
              var raw = (document.getElementById("employeeUpdateHourlyRate") || {}).value;
              return raw === "" || raw == null ? null : Number(raw);
            })(),
            isActive: ((document.getElementById("employeeUpdateIsActive") || {}).value || "true") === "true"
          })
        })
          .then(function () {
            setInlineMessage(updateMessage, "Đã cập nhật nhân viên.", false);
            closeModal(document.getElementById("employeeUpdateModal"));
            return loadEmployees();
          })
          .catch(function (error) {
            setInlineMessage(updateMessage, getErrorMessage(error, "Không thể cập nhật nhân viên."), true);
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
    var menuActionMessage = document.getElementById("menuActionMessage");
    var menuItemUpdateForm = document.getElementById("menuItemUpdateForm");
    var menuItemUpdateMessage = document.getElementById("menuItemUpdateMessage");
    var menuItemUpdateCategory = document.getElementById("menuItemUpdateCategory");
    var categoriesCache = [];
    var menuItemsMap = {};
    var fallbackMenuImages = {
      coffee: "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=1200&q=80",
      tea: "https://images.unsplash.com/photo-1558160074-4d7d8bdf4256?auto=format&fit=crop&w=1200&q=80",
      bakery: "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=1200&q=80",
      default: "https://images.unsplash.com/photo-1499636136210-6f4ee915583e?auto=format&fit=crop&w=1200&q=80"
    };

    function normalizeImageUrl(value) {
      var raw = value == null ? "" : String(value).trim();
      return raw || null;
    }

    function getFallbackMenuImage(item) {
      var categoryName = (item && item.category && item.category.name ? item.category.name : "").toLowerCase();
      if (categoryName.indexOf("coffee") >= 0) return fallbackMenuImages.coffee;
      if (categoryName.indexOf("tea") >= 0) return fallbackMenuImages.tea;
      if (categoryName.indexOf("bakery") >= 0) return fallbackMenuImages.bakery;
      return fallbackMenuImages.default;
    }

    function loadCategories() {
      return apiFetch("/api/manager/categories")
        .then(function (categories) {
          categoriesCache = categories || [];
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

          if (menuItemUpdateCategory) {
            if (!categories.length) {
              menuItemUpdateCategory.innerHTML = "<option value=''>Chưa có danh mục</option>";
            } else {
              menuItemUpdateCategory.innerHTML = categories
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
          if (menuItemUpdateCategory) {
            menuItemUpdateCategory.innerHTML = "<option value=''>Không tải được danh mục</option>";
          }
          return [];
        });
    }

    function loadMenuItems() {
      return apiFetch("/api/manager/menu-items")
        .then(function (items) {
          menuItemsMap = {};
          if (!items.length) {
            menuCards.innerHTML = "<div class='card'><p class='muted'>Chưa có món nào.</p></div>";
            return;
          }

          menuCards.innerHTML = items
            .map(function (item) {
              menuItemsMap[String(item.id)] = item;
              var description = item.description || (item.category && item.category.name) || "";
              var fallbackImage = getFallbackMenuImage(item);
              var imageUrl = normalizeImageUrl(item.imageUrl) || fallbackImage;
              var image = "<img src='" + imageUrl + "' data-fallback-src='" + fallbackImage + "' alt='" + item.name + "' loading='lazy' referrerpolicy='no-referrer' onerror=\"this.onerror=null;this.src=this.getAttribute('data-fallback-src');\" style='width:100%;aspect-ratio:4/3;object-fit:cover;border-radius:10px;margin-bottom:0.85rem;border:1px solid var(--border)' />";
              var status = item.isAvailable === false ? "Tạm ngưng" : "Đang bán";

              return (
                "<div class='card'>" +
                image +
                "<h3 style='margin:0 0 0.25rem;font-size:1rem'>" + item.name + "</h3>" +
                "<p class='muted' style='font-size:0.85rem;margin:0 0 0.5rem'>" + description + "</p>" +
                "<p style='margin:0 0 0.35rem;font-weight:700;color:var(--accent)'>" + formatCurrency(item.price) + "</p>" +
                "<p class='muted' style='margin:0 0 0.75rem;font-size:0.82rem'>Trạng thái: " + status + "</p>" +
                "<div style='display:flex;gap:0.45rem;flex-wrap:wrap'>" +
                "<button type='button' class='btn btn-ghost btn-xs' data-menu-edit='" + item.id + "'>Sửa</button>" +
                "<button type='button' class='btn btn-ghost btn-xs' data-menu-delete='" + item.id + "'>Xóa</button>" +
                "</div>" +
                "</div>"
              );
            })
            .join("");
        })
        .catch(function (error) {
          var msg = getErrorMessage(error, "Không tải được danh sách món.");
          if (msg === "Unauthorized") {
            msg = "Bạn không có quyền xem dữ liệu menu hoặc phiên đăng nhập đã hết hạn.";
          }
          menuCards.innerHTML = "<div class='card'><p class='muted'>" + msg + "</p></div>";
        });
    }

    if (menuCards) {
      menuCards.addEventListener("click", function (event) {
        var editBtn = event.target.closest("[data-menu-edit]");
        if (editBtn) {
          var item = menuItemsMap[String(editBtn.getAttribute("data-menu-edit"))];
          if (!item) return;
          var setValue = function (id, value) {
            var el = document.getElementById(id);
            if (el) el.value = value == null ? "" : value;
          };
          setValue("menuItemUpdateId", item.id);
          setValue("menuItemUpdateName", item.name);
          setValue("menuItemUpdatePrice", item.price);
          setValue("menuItemUpdateDescription", item.description);
          setValue("menuItemUpdateImage", item.imageUrl);
          setValue("menuItemUpdateIsAvailable", item.isAvailable === false ? "false" : "true");
          if (menuItemUpdateCategory) {
            menuItemUpdateCategory.value = item.category && item.category.id ? String(item.category.id) : "";
          }
          setInlineMessage(menuItemUpdateMessage, "", false);
          openModalById("menuItemUpdateModal");
          return;
        }

        var deleteBtn = event.target.closest("[data-menu-delete]");
        if (deleteBtn) {
          var deleteId = Number(deleteBtn.getAttribute("data-menu-delete") || 0);
          if (!deleteId) return;
          if (!window.confirm("Xóa món này khỏi menu?")) return;
          apiFetch("/api/manager/menu-items/" + deleteId, { method: "DELETE" })
            .then(function () {
              setInlineMessage(menuActionMessage, "Đã xóa món khỏi menu.", false);
              return loadMenuItems();
            })
            .catch(function (error) {
              var msg = getErrorMessage(error, "Không thể xóa món.");
              if (msg === "Cannot delete menu item that is already used in orders") {
                msg = "Món đã phát sinh trong đơn hàng, không thể xóa.";
              }
              setInlineMessage(menuActionMessage, msg, true);
            });
        }
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
            var msg = getErrorMessage(error, "Không thể thêm danh mục.");
            if (msg === "Category already exists") {
              msg = "Danh mục đã tồn tại.";
            }
            setInlineMessage(categoryMessage, msg, true);
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
            imageUrl: normalizeImageUrl((document.getElementById("menuItemImage") || {}).value),
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

    if (menuItemUpdateForm) {
      menuItemUpdateForm.addEventListener("submit", function (event) {
        event.preventDefault();
        setInlineMessage(menuItemUpdateMessage, "", false);
        var itemId = Number((document.getElementById("menuItemUpdateId") || {}).value || 0);
        var categoryId = Number((menuItemUpdateCategory || {}).value || 0);
        if (!itemId || !categoryId) {
          setInlineMessage(menuItemUpdateMessage, "Vui lòng chọn đúng món và danh mục.", true);
          return;
        }

        apiFetch("/api/manager/menu-items/" + itemId, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            categoryId: categoryId,
            name: (document.getElementById("menuItemUpdateName") || {}).value || "",
            description: (document.getElementById("menuItemUpdateDescription") || {}).value || "",
            price: Number((document.getElementById("menuItemUpdatePrice") || {}).value || 0),
            imageUrl: normalizeImageUrl((document.getElementById("menuItemUpdateImage") || {}).value),
            isAvailable: ((document.getElementById("menuItemUpdateIsAvailable") || {}).value || "true") === "true"
          })
        })
          .then(function () {
            closeModal(document.getElementById("menuItemUpdateModal"));
            setInlineMessage(menuActionMessage, "Đã cập nhật món.", false);
            return loadMenuItems();
          })
          .catch(function (error) {
            setInlineMessage(menuItemUpdateMessage, getErrorMessage(error, "Không thể cập nhật món."), true);
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
    var tableBody = document.getElementById("managerTableBody");
    var tableMessage = document.getElementById("managerTableMessage");
    var tablesCache = [];

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
          tablesCache = tables || [];
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

          if (!tableBody) return;
          if (!tables.length) {
            tableBody.innerHTML = "<tr><td colspan='4' class='muted'>Chưa có bàn.</td></tr>";
            return;
          }
          tableBody.innerHTML = tables
            .map(function (table) {
              return (
                "<tr>" +
                "<td>" + table.name + "</td>" +
                "<td>" + (table.capacity == null ? "-" : table.capacity) + "</td>" +
                "<td>" +
                  "<select data-table-status='" + table.id + "'>" +
                    "<option value='empty'" + (table.status === "empty" ? " selected" : "") + ">Trống</option>" +
                    "<option value='occupied'" + (table.status === "occupied" ? " selected" : "") + ">Đang phục vụ</option>" +
                    "<option value='reserved'" + (table.status === "reserved" ? " selected" : "") + ">Đã đặt</option>" +
                  "</select>" +
                "</td>" +
                "<td style='white-space:nowrap'>" +
                  "<button type='button' class='btn btn-ghost btn-xs' data-table-update='" + table.id + "'>Lưu</button> " +
                  "<button type='button' class='btn btn-ghost btn-xs' data-table-delete='" + table.id + "'>Xóa</button>" +
                "</td>" +
                "</tr>"
              );
            })
            .join("");
        })
        .catch(function () {
          tableFloorGrid.innerHTML = "<div class='table-tile'>Lỗi<span>Không tải được bàn</span></div>";
          if (tableBody) {
            tableBody.innerHTML = "<tr><td colspan='4' class='muted'>Không tải được danh sách bàn.</td></tr>";
          }
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

    if (tableBody) {
      tableBody.addEventListener("click", function (event) {
        var updateBtn = event.target.closest("[data-table-update]");
        if (updateBtn) {
          var updateId = Number(updateBtn.getAttribute("data-table-update") || 0);
          var statusSelect = tableBody.querySelector("[data-table-status='" + updateId + "']");
          if (!updateId || !statusSelect) return;
          setInlineMessage(tableMessage, "", false);
          apiFetch("/api/manager/tables/" + updateId + "/status", {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ status: statusSelect.value || "empty" })
          })
            .then(function () {
              setInlineMessage(tableMessage, "Đã cập nhật trạng thái bàn.", false);
              return loadTables();
            })
            .catch(function (error) {
              setInlineMessage(tableMessage, getErrorMessage(error, "Không thể cập nhật trạng thái."), true);
            });
          return;
        }

        var deleteBtn = event.target.closest("[data-table-delete]");
        if (deleteBtn) {
          var deleteId = Number(deleteBtn.getAttribute("data-table-delete") || 0);
          if (!deleteId) return;
          if (!window.confirm("Xóa bàn này?")) return;
          setInlineMessage(tableMessage, "", false);
          apiFetch("/api/manager/tables/" + deleteId, { method: "DELETE" })
            .then(function () {
              setInlineMessage(tableMessage, "Đã xóa bàn.", false);
              return loadTables();
            })
            .catch(function (error) {
              setInlineMessage(tableMessage, getErrorMessage(error, "Không thể xóa bàn."), true);
            });
        }
      });
    }

    loadTables();
  }

  function initManagerAttendancePage() {
    var reportBody = document.getElementById("attendanceReportBody");
    if (!reportBody) return;

    var payrollBody = document.getElementById("attendancePayrollBody");
    var filterForm = document.getElementById("attendanceFilterForm");
    var fromInput = document.getElementById("attendanceFromDate");
    var toInput = document.getElementById("attendanceToDate");
    var phoneInput = document.getElementById("attendancePhoneFilter");
    var todayBtn = document.getElementById("attendanceTodayBtn");
    var messageEl = document.getElementById("attendanceFilterMessage");
    var totalHoursEl = document.getElementById("attendanceTotalHours");
    var recordCountEl = document.getElementById("attendanceRecordCount");
    var payrollTotalEl = document.getElementById("attendancePayrollTotal");

    function toIsoDate(date) {
      return date.toISOString().slice(0, 10);
    }

    function formatDate(value) {
      if (!value) return "-";
      return new Date(value).toLocaleDateString("vi-VN");
    }

    function formatDateTime(value) {
      if (!value) return "-";
      return new Date(value).toLocaleString("vi-VN");
    }

    function renderSummary(reportRows, payrollRows) {
      var totalHours = 0;
      (reportRows || []).forEach(function (row) {
        totalHours += Number(row.hoursWorked || 0);
      });
      var totalPayroll = 0;
      (payrollRows || []).forEach(function (row) {
        totalPayroll += Number(row.salaryAmount || 0);
      });

      if (recordCountEl) recordCountEl.textContent = String((reportRows || []).length);
      if (totalHoursEl) totalHoursEl.textContent = formatNumber(totalHours) + "h";
      if (payrollTotalEl) payrollTotalEl.textContent = formatCurrency(totalPayroll);
    }

    function renderReport(reportRows) {
      if (!reportRows || !reportRows.length) {
        reportBody.innerHTML = "<tr><td colspan='5' class='muted'>Không có dữ liệu chấm công.</td></tr>";
        return;
      }
      reportBody.innerHTML = reportRows
        .map(function (row) {
          return (
            "<tr>" +
              "<td>" + (row.employeeName || "-") + "</td>" +
              "<td>" + formatDate(row.workDate) + "</td>" +
              "<td>" + formatDateTime(row.checkIn) + "</td>" +
              "<td>" + formatDateTime(row.checkOut) + "</td>" +
              "<td>" + (row.hoursWorked != null ? formatNumber(row.hoursWorked) + "h" : "-") + "</td>" +
            "</tr>"
          );
        })
        .join("");
    }

    function renderPayroll(payrollRows) {
      if (!payrollRows || !payrollRows.length) {
        payrollBody.innerHTML = "<tr><td colspan='4' class='muted'>Không có dữ liệu lương.</td></tr>";
        return;
      }
      payrollBody.innerHTML = payrollRows
        .map(function (row) {
          return (
            "<tr>" +
              "<td>" + (row.fullName || "-") + "</td>" +
              "<td>" + formatNumber(row.totalHours || 0) + "h</td>" +
              "<td>" + formatCurrency(row.hourlyRate || 0) + "</td>" +
              "<td>" + formatCurrency(row.salaryAmount || 0) + "</td>" +
            "</tr>"
          );
        })
        .join("");
    }

    function loadByFilter(fromDate, toDate, phone) {
      var reportUrl = "/api/manager/attendance/report?from=" + encodeURIComponent(fromDate) + "&to=" + encodeURIComponent(toDate);
      if (phone) {
        reportUrl += "&phone=" + encodeURIComponent(phone);
      }
      var payrollUrl = "/api/manager/payroll/summary?from=" + encodeURIComponent(fromDate) + "&to=" + encodeURIComponent(toDate);

      setInlineMessage(messageEl, "", false);
      return Promise.all([apiFetch(reportUrl), apiFetch(payrollUrl)])
        .then(function (results) {
          var reportRows = results[0] || [];
          var payrollRows = results[1] || [];
          renderReport(reportRows);
          renderPayroll(payrollRows);
          renderSummary(reportRows, payrollRows);
          setInlineMessage(messageEl, "Đã cập nhật báo cáo chấm công.", false);
        })
        .catch(function (error) {
          reportBody.innerHTML = "<tr><td colspan='5' class='muted'>Không tải được báo cáo chấm công.</td></tr>";
          payrollBody.innerHTML = "<tr><td colspan='4' class='muted'>Không tải được bảng lương.</td></tr>";
          renderSummary([], []);
          setInlineMessage(messageEl, getErrorMessage(error, "Không thể tải báo cáo chấm công."), true);
        });
    }

    function loadToday() {
      setInlineMessage(messageEl, "", false);
      return apiFetch("/api/manager/attendance/today")
        .then(function (rows) {
          renderReport(rows || []);
          if (recordCountEl) recordCountEl.textContent = String((rows || []).length);
          var totalHours = 0;
          (rows || []).forEach(function (row) {
            totalHours += Number(row.hoursWorked || 0);
          });
          if (totalHoursEl) totalHoursEl.textContent = formatNumber(totalHours) + "h";
          setInlineMessage(messageEl, "Đã tải dữ liệu chấm công hôm nay.", false);
        })
        .catch(function (error) {
          reportBody.innerHTML = "<tr><td colspan='5' class='muted'>Không tải được dữ liệu hôm nay.</td></tr>";
          setInlineMessage(messageEl, getErrorMessage(error, "Không thể tải dữ liệu hôm nay."), true);
        });
    }

    if (filterForm) {
      filterForm.addEventListener("submit", function (event) {
        event.preventDefault();
        var fromDate = fromInput ? fromInput.value : "";
        var toDate = toInput ? toInput.value : "";
        if (!fromDate || !toDate) {
          setInlineMessage(messageEl, "Vui lòng chọn đủ từ ngày và đến ngày.", true);
          return;
        }
        if (new Date(fromDate) > new Date(toDate)) {
          setInlineMessage(messageEl, "Từ ngày phải nhỏ hơn hoặc bằng đến ngày.", true);
          return;
        }
        loadByFilter(fromDate, toDate, phoneInput ? phoneInput.value.trim().replace(/\s+/g, "") : "");
      });
    }

    if (todayBtn) {
      todayBtn.addEventListener("click", function () {
        loadToday();
      });
    }

    var now = new Date();
    var startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    if (fromInput) fromInput.value = toIsoDate(startOfMonth);
    if (toInput) toInput.value = toIsoDate(now);

    loadByFilter(fromInput ? fromInput.value : toIsoDate(startOfMonth), toInput ? toInput.value : toIsoDate(now), "");
  }

  function initAdminDashboardPage() {
    var filterForm = document.getElementById("adminReportFilterForm");
    if (!filterForm) return;

    var fromInput = document.getElementById("adminReportFromDate");
    var toInput = document.getElementById("adminReportToDate");
    var monthInput = document.getElementById("adminReportMonth");
    var exportCsvBtn = document.getElementById("adminExportCsvBtn");
    var messageEl = document.getElementById("adminReportMessage");

    var todayRevenueEl = document.getElementById("adminTodayRevenue");
    var todayDiscountEl = document.getElementById("adminTodayDiscount");
    var todayOrdersEl = document.getElementById("adminTodayOrders");
    var monthRevenueEl = document.getElementById("adminMonthRevenue");
    var monthOrdersEl = document.getElementById("adminMonthOrders");
    var monthAvgOrderEl = document.getElementById("adminMonthAvgOrder");
    var topItemsBody = document.getElementById("adminTopItemsBody");

    function toIsoDate(date) {
      return date.toISOString().slice(0, 10);
    }

    function loadReports() {
      var fromDate = fromInput ? fromInput.value : "";
      var toDate = toInput ? toInput.value : "";
      var monthValue = monthInput ? monthInput.value : "";
      if (!fromDate || !toDate || !monthValue) {
        setInlineMessage(messageEl, "Vui lòng nhập đủ bộ lọc ngày/tháng.", true);
        return;
      }

      var monthParts = monthValue.split("-");
      var year = Number(monthParts[0] || 0);
      var month = Number(monthParts[1] || 0);
      var dailyUrl = "/api/admin/reports/sales/daily?date=" + encodeURIComponent(toDate);
      var monthlyUrl = "/api/admin/reports/sales/monthly?year=" + year + "&month=" + month;
      var topItemsUrl = "/api/admin/reports/top-menu-items?fromDate=" + encodeURIComponent(fromDate) + "&toDate=" + encodeURIComponent(toDate) + "&limit=10";

      setInlineMessage(messageEl, "", false);
      Promise.all([apiFetch(dailyUrl), apiFetch(monthlyUrl), apiFetch(topItemsUrl)])
        .then(function (results) {
          var daily = results[0] || {};
          var monthly = results[1] || {};
          var topItems = results[2] || [];

          if (todayRevenueEl) todayRevenueEl.textContent = formatCurrency(daily.revenue || 0);
          if (todayDiscountEl) todayDiscountEl.textContent = "Giảm giá: " + formatCurrency(daily.discount || 0);
          if (todayOrdersEl) todayOrdersEl.textContent = String(daily.ordersCount || 0);

          if (monthRevenueEl) monthRevenueEl.textContent = formatCurrency(monthly.revenue || 0);
          if (monthOrdersEl) monthOrdersEl.textContent = String(monthly.ordersCount || 0) + " đơn trong tháng";
          if (monthAvgOrderEl) monthAvgOrderEl.textContent = formatCurrency(monthly.avgOrderValue || 0);

          if (topItemsBody) {
            topItemsBody.innerHTML = topItems.length
              ? topItems
                  .map(function (item) {
                    return (
                      "<tr>" +
                      "<td>" + (item.name || "-") + "</td>" +
                      "<td>" + formatNumber(item.quantitySold || 0) + "</td>" +
                      "<td>" + formatCurrency(item.revenue || 0) + "</td>" +
                      "</tr>"
                    );
                  })
                  .join("")
              : "<tr><td colspan='3' class='muted'>Không có dữ liệu top món.</td></tr>";
          }

          setInlineMessage(messageEl, "Đã cập nhật dữ liệu doanh thu.", false);
        })
        .catch(function (error) {
          if (topItemsBody) {
            topItemsBody.innerHTML = "<tr><td colspan='3' class='muted'>Không tải được dữ liệu.</td></tr>";
          }
          setInlineMessage(messageEl, getErrorMessage(error, "Không thể tải dữ liệu báo cáo."), true);
        });
    }

    filterForm.addEventListener("submit", function (event) {
      event.preventDefault();
      loadReports();
    });

    if (exportCsvBtn) {
      exportCsvBtn.addEventListener("click", function () {
        var fromDate = fromInput ? fromInput.value : "";
        var toDate = toInput ? toInput.value : "";
        if (!fromDate || !toDate) {
          setInlineMessage(messageEl, "Vui lòng chọn from/to date để xuất CSV.", true);
          return;
        }
        fetch("/api/admin/reports/export/sales.csv?fromDate=" + encodeURIComponent(fromDate) + "&toDate=" + encodeURIComponent(toDate))
          .then(function (response) {
            if (!response.ok) throw new Error("Không thể xuất CSV.");
            return response.blob();
          })
          .then(function (blob) {
            var url = window.URL.createObjectURL(blob);
            var link = document.createElement("a");
            link.href = url;
            link.download = "sales-report.csv";
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
          })
          .catch(function (error) {
            setInlineMessage(messageEl, getErrorMessage(error, "Không thể xuất CSV."), true);
          });
      });
    }

    var now = new Date();
    var startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    if (fromInput) fromInput.value = toIsoDate(startOfMonth);
    if (toInput) toInput.value = toIsoDate(now);
    if (monthInput) monthInput.value = now.getFullYear() + "-" + String(now.getMonth() + 1).padStart(2, "0");
    loadReports();
  }

  function initManagerDashboardPage() {
    var servingOrdersEl = document.getElementById("managerServingOrdersValue");
    if (!servingOrdersEl) return;
    var activeEmployeesEl = document.getElementById("managerActiveEmployeesValue");
    var topMenuItemEl = document.getElementById("managerTopMenuItemValue");
    var lowStockEl = document.getElementById("managerLowStockValue");

    apiFetch("/api/manager/dashboard/summary")
      .then(function (summary) {
        servingOrdersEl.textContent = String(summary.servingOrdersCount == null ? 0 : summary.servingOrdersCount);
        if (activeEmployeesEl) {
          activeEmployeesEl.textContent = String(summary.activeEmployeesCount == null ? 0 : summary.activeEmployeesCount);
        }
        if (topMenuItemEl) {
          topMenuItemEl.textContent = summary.topMenuItemName || "Chưa có";
        }
        if (lowStockEl) {
          lowStockEl.textContent = String(summary.lowStockCount == null ? 0 : summary.lowStockCount);
        }
      })
      .catch(function () {
        servingOrdersEl.textContent = "0";
        if (activeEmployeesEl) activeEmployeesEl.textContent = "0";
        if (topMenuItemEl) topMenuItemEl.textContent = "Chưa có";
        if (lowStockEl) lowStockEl.textContent = "0";
      });
  }

  function initAdminUsersPage() {
    var usersBody = document.getElementById("adminUsersBody");
    if (!usersBody) return;

    var usersMessage = document.getElementById("adminUsersMessage");
    var roleCreateForm = document.getElementById("adminRoleCreateForm");
    var roleCreateMessage = document.getElementById("adminRoleCreateMessage");
    var userCreateForm = document.getElementById("adminUserCreateForm");
    var userCreateMessage = document.getElementById("adminUserCreateMessage");
    var userCreateRoleSelect = document.getElementById("adminUserCreateRole");

    var roles = [];
    var usersMap = {};

    function renderUsers(users) {
      usersMap = {};
      if (!users || !users.length) {
        usersBody.innerHTML = "<tr><td colspan='6' class='muted'>Chưa có tài khoản nào.</td></tr>";
        return;
      }
      usersBody.innerHTML = users
        .map(function (user) {
          usersMap[String(user.id)] = user;
          var rolesText = (user.roles || []).join(", ") || "-";
          var statusHtml = user.active
            ? "<span style='color:var(--success);font-weight:600'>Hoạt động</span>"
            : "<span style='color:var(--danger);font-weight:600'>Đã khóa</span>";
          var roleOptions = roles
            .map(function (role) {
              return "<option value='" + role.name + "'>" + role.name + "</option>";
            })
            .join("");
          return (
            "<tr>" +
              "<td>" + user.username + "</td>" +
              "<td>" + (user.fullName || "-") + "</td>" +
              "<td>" + (user.email || "-") + "</td>" +
              "<td>" + rolesText + "</td>" +
              "<td>" + statusHtml + "</td>" +
              "<td style='white-space:nowrap'>" +
                "<select data-user-role-select='" + user.id + "'>" + roleOptions + "</select> " +
                "<button type='button' class='btn btn-ghost btn-xs' data-user-assign-role='" + user.id + "'>Gán role</button> " +
                "<button type='button' class='btn btn-ghost btn-xs' data-user-toggle-active='" + user.id + "'>" + (user.active ? "Khóa" : "Mở") + "</button>" +
              "</td>" +
            "</tr>"
          );
        })
        .join("");
    }

    function syncRoleOptions() {
      if (!userCreateRoleSelect) return;
      if (!roles.length) {
        userCreateRoleSelect.innerHTML = "<option value='ROLE_STAFF'>ROLE_STAFF</option>";
        return;
      }
      userCreateRoleSelect.innerHTML = roles
        .map(function (role) {
          var selected = role.name === "ROLE_STAFF" ? " selected" : "";
          return "<option value='" + role.name + "'" + selected + ">" + role.name + "</option>";
        })
        .join("");
    }

    function loadRoles() {
      return apiFetch("/api/admin/roles")
        .then(function (data) {
          roles = data || [];
          syncRoleOptions();
        })
        .catch(function () {
          roles = [];
          syncRoleOptions();
        });
    }

    function loadUsers() {
      return apiFetch("/api/admin/users")
        .then(function (users) {
          renderUsers(users || []);
        })
        .catch(function (error) {
          usersBody.innerHTML = "<tr><td colspan='6' class='muted'>Không tải được danh sách tài khoản.</td></tr>";
          setInlineMessage(usersMessage, getErrorMessage(error, "Không thể tải danh sách tài khoản."), true);
        });
    }

    if (usersBody) {
      usersBody.addEventListener("click", function (event) {
        var assignRoleBtn = event.target.closest("[data-user-assign-role]");
        if (assignRoleBtn) {
          var userId = Number(assignRoleBtn.getAttribute("data-user-assign-role") || 0);
          var roleSelect = usersBody.querySelector("[data-user-role-select='" + userId + "']");
          if (!userId || !roleSelect) return;
          setInlineMessage(usersMessage, "", false);
          apiFetch("/api/admin/users/" + userId + "/roles", {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ roleName: roleSelect.value })
          })
            .then(function () {
              setInlineMessage(usersMessage, "Đã gán role cho tài khoản.", false);
              return loadUsers();
            })
            .catch(function (error) {
              setInlineMessage(usersMessage, getErrorMessage(error, "Không thể gán role."), true);
            });
          return;
        }

        var toggleBtn = event.target.closest("[data-user-toggle-active]");
        if (toggleBtn) {
          var toggleUserId = Number(toggleBtn.getAttribute("data-user-toggle-active") || 0);
          var user = usersMap[String(toggleUserId)];
          if (!toggleUserId || !user) return;
          setInlineMessage(usersMessage, "", false);
          apiFetch("/api/admin/users/" + toggleUserId + "/active", {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ active: !user.active })
          })
            .then(function () {
              setInlineMessage(usersMessage, "Đã cập nhật trạng thái tài khoản.", false);
              return loadUsers();
            })
            .catch(function (error) {
              setInlineMessage(usersMessage, getErrorMessage(error, "Không thể cập nhật trạng thái."), true);
            });
        }
      });
    }

    if (roleCreateForm) {
      roleCreateForm.addEventListener("submit", function (event) {
        event.preventDefault();
        setInlineMessage(roleCreateMessage, "", false);
        apiFetch("/api/admin/roles", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            name: (document.getElementById("adminRoleName") || {}).value || ""
          })
        })
          .then(function () {
            roleCreateForm.reset();
            closeModal(document.getElementById("adminRoleCreateModal"));
            setInlineMessage(usersMessage, "Đã tạo role mới.", false);
            return Promise.all([loadRoles(), loadUsers()]);
          })
          .catch(function (error) {
            setInlineMessage(roleCreateMessage, getErrorMessage(error, "Không thể tạo role."), true);
          });
      });
    }

    if (userCreateForm) {
      userCreateForm.addEventListener("submit", function (event) {
        event.preventDefault();
        setInlineMessage(userCreateMessage, "", false);

        var username = (document.getElementById("adminUserCreateUsername") || {}).value || "";
        var password = (document.getElementById("adminUserCreatePassword") || {}).value || "";
        var expectedRole = (userCreateRoleSelect && userCreateRoleSelect.value) || "ROLE_STAFF";

        apiFetch("/api/auth/register", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            username: username,
            password: password,
            confirmPassword: password,
            fullName: (document.getElementById("adminUserCreateFullName") || {}).value || "",
            email: (document.getElementById("adminUserCreateEmail") || {}).value || "",
            phone: (document.getElementById("adminUserCreatePhone") || {}).value || ""
          })
        })
          .then(function () {
            return apiFetch("/api/admin/users").then(function (users) {
              var createdUser = (users || []).find(function (u) {
                return u.username === username;
              });
              if (!createdUser || expectedRole === "ROLE_STAFF") {
                return null;
              }
              return apiFetch("/api/admin/users/" + createdUser.id + "/roles", {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ roleName: expectedRole })
              });
            });
          })
          .then(function () {
            closeModal(document.getElementById("adminUserCreateModal"));
            userCreateForm.reset();
            setInlineMessage(usersMessage, "Đã tạo tài khoản mới.", false);
            return loadUsers();
          })
          .catch(function (error) {
            setInlineMessage(userCreateMessage, getErrorMessage(error, "Không thể tạo tài khoản."), true);
          });
      });
    }

    Promise.all([loadRoles(), loadUsers()]);
  }

  function initAdminAuditPage() {
    var filterForm = document.getElementById("adminAuditFilterForm");
    if (!filterForm) return;

    var body = document.getElementById("adminAuditBody");
    var messageEl = document.getElementById("adminAuditMessage");
    var fromInput = document.getElementById("adminAuditFromDate");
    var toInput = document.getElementById("adminAuditToDate");
    var actionInput = document.getElementById("adminAuditAction");
    var usernameInput = document.getElementById("adminAuditUsername");
    var clearBtn = document.getElementById("adminAuditClearBtn");

    function toInstantStart(dateValue) {
      return dateValue + "T00:00:00.000Z";
    }

    function toInstantEnd(dateValue) {
      return dateValue + "T23:59:59.999Z";
    }

    function renderRows(rows) {
      if (!rows || !rows.length) {
        body.innerHTML = "<tr><td colspan='5' class='muted'>Không có nhật ký phù hợp.</td></tr>";
        return;
      }
      body.innerHTML = rows
        .map(function (log) {
          var details = log.detail ? JSON.stringify(log.detail) : "-";
          return (
            "<tr>" +
              "<td>" + (log.createdAt ? new Date(log.createdAt).toLocaleString("vi-VN") : "-") + "</td>" +
              "<td>" + (log.username || "-") + "</td>" +
              "<td>" + (log.action || "-") + "</td>" +
              "<td>" + (log.tableName || "-") + "</td>" +
              "<td><code style='font-size:0.75rem'>" + details + "</code></td>" +
            "</tr>"
          );
        })
        .join("");
    }

    function loadLogs() {
      var query = [];
      var fromValue = fromInput ? fromInput.value : "";
      var toValue = toInput ? toInput.value : "";
      var actionValue = actionInput ? actionInput.value.trim() : "";
      var usernameValue = usernameInput ? usernameInput.value.trim() : "";
      if (fromValue) query.push("from=" + encodeURIComponent(toInstantStart(fromValue)));
      if (toValue) query.push("to=" + encodeURIComponent(toInstantEnd(toValue)));
      if (actionValue) query.push("action=" + encodeURIComponent(actionValue));
      if (usernameValue) query.push("username=" + encodeURIComponent(usernameValue));
      var url = "/api/admin/audit-logs" + (query.length ? "?" + query.join("&") : "");
      setInlineMessage(messageEl, "", false);
      apiFetch(url)
        .then(function (rows) {
          renderRows(rows || []);
          setInlineMessage(messageEl, "Đã cập nhật nhật ký hệ thống.", false);
        })
        .catch(function (error) {
          body.innerHTML = "<tr><td colspan='5' class='muted'>Không tải được nhật ký.</td></tr>";
          setInlineMessage(messageEl, getErrorMessage(error, "Không thể tải nhật ký."), true);
        });
    }

    filterForm.addEventListener("submit", function (event) {
      event.preventDefault();
      loadLogs();
    });

    if (clearBtn) {
      clearBtn.addEventListener("click", function () {
        if (fromInput) fromInput.value = "";
        if (toInput) toInput.value = "";
        if (actionInput) actionInput.value = "";
        if (usernameInput) usernameInput.value = "";
        loadLogs();
      });
    }

    loadLogs();
  }

  function initStaffClockPage() {
    var form = document.getElementById("staffClockForm");
    if (!form) return;

    var nowEl = document.getElementById("staffClockNow");
    var dateEl = document.getElementById("staffClockDate");
    var phoneInput = document.getElementById("staffClockPhone");
    var checkInBtn = document.getElementById("staffCheckInBtn");
    var checkOutBtn = document.getElementById("staffCheckOutBtn");
    var historyEl = document.getElementById("staffClockHistory");
    var messageEl = document.getElementById("staffClockMessage");

    function pad(number) {
      return String(number).padStart(2, "0");
    }

    function updateClock() {
      var now = new Date();
      if (nowEl) {
        nowEl.textContent = pad(now.getHours()) + ":" + pad(now.getMinutes()) + ":" + pad(now.getSeconds());
      }
      if (dateEl) {
        dateEl.textContent = now.toLocaleDateString("vi-VN", {
          weekday: "long",
          year: "numeric",
          month: "2-digit",
          day: "2-digit"
        });
      }
    }

    function formatDateTime(value) {
      if (!value) return "—";
      return new Date(value).toLocaleString("vi-VN");
    }

    function renderHistory(attendance) {
      if (!historyEl) return;
      var checkInTime = formatDateTime(attendance ? attendance.checkIn : null);
      var checkOutTime = formatDateTime(attendance ? attendance.checkOut : null);
      var hoursWorked = attendance && attendance.hoursWorked != null ? String(attendance.hoursWorked) + "h" : "—";
      historyEl.innerHTML =
        "<li style='display:flex;justify-content:space-between;border-bottom:1px solid var(--border);padding:0.5rem 0'>" +
        "<span>Nhân viên</span><span style='color:var(--ink);font-weight:600'>" + ((attendance && attendance.employeeName) || "—") + "</span>" +
        "</li>" +
        "<li style='display:flex;justify-content:space-between;border-bottom:1px solid var(--border);padding:0.5rem 0'>" +
        "<span>Vào ca</span><span style='color:var(--ink);font-weight:600'>" + checkInTime + "</span>" +
        "</li>" +
        "<li style='display:flex;justify-content:space-between;border-bottom:1px solid var(--border);padding:0.5rem 0'>" +
        "<span>Ra ca</span><span style='color:var(--ink);font-weight:600'>" + checkOutTime + "</span>" +
        "</li>" +
        "<li style='display:flex;justify-content:space-between;padding:0.5rem 0'>" +
        "<span>Giờ làm</span><span style='color:var(--ink);font-weight:600'>" + hoursWorked + "</span>" +
        "</li>";
    }

    function submitClockAction(endpoint) {
      var phone = (phoneInput && phoneInput.value ? phoneInput.value : "").trim().replace(/\s+/g, "");
      if (!phone) {
        setInlineMessage(messageEl, "Vui lòng nhập số điện thoại nhân viên.", true);
        return;
      }
      setInlineMessage(messageEl, "", false);
      apiFetch(endpoint, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ phone: phone })
      })
        .then(function (attendance) {
          renderHistory(attendance);
          setInlineMessage(messageEl, "Cập nhật chấm công thành công.", false);
        })
        .catch(function (error) {
          var msg = getErrorMessage(error, "Không thể chấm công.");
          if (msg === "Employee not found") {
            msg = "Không tìm thấy nhân viên theo số điện thoại này.";
          } else if (msg === "Employee is inactive") {
            msg = "Nhân viên đang bị khóa, không thể chấm công.";
          } else if (msg === "Employee has not checked in today"
            || msg === "Bạn cần check-in trước khi check-out.") {
            msg = "Bạn cần check-in trước khi check-out.";
          }
          setInlineMessage(messageEl, msg, true);
        });
    }

    if (checkInBtn) {
      checkInBtn.addEventListener("click", function () {
        submitClockAction("/api/staff/attendance/check-in");
      });
    }
    if (checkOutBtn) {
      checkOutBtn.addEventListener("click", function () {
        submitClockAction("/api/staff/attendance/check-out");
      });
    }

    updateClock();
    window.setInterval(updateClock, 1000);
    renderHistory(null);
  }

  function initStaffOrdersPage() {
    var tablesGrid = document.getElementById("staffTablesGrid");
    if (!tablesGrid) return;

    var orderListBody = document.getElementById("staffOrdersListBody");
    var orderItemsBody = document.getElementById("staffOrderItemsBody");
    var orderTitle = document.getElementById("staffOrderDetailTitle");
    var orderStatusBadge = document.getElementById("staffOrderStatusBadge");
    var subtotalEl = document.getElementById("staffOrderSubtotal");
    var discountEl = document.getElementById("staffOrderDiscount");
    var totalEl = document.getElementById("staffOrderTotal");
    var messageEl = document.getElementById("staffOrderMessage");
    var checkoutMessageEl = document.getElementById("staffCheckoutMessage");

    var createBtn = document.getElementById("orderCreateBtn");
    var addItemBtn = document.getElementById("orderAddItemBtn");
    var checkoutBtn = document.getElementById("orderCheckoutBtn");
    var cancelBtn = document.getElementById("orderCancelBtn");
    var invoiceBtn = document.getElementById("orderInvoiceBtn");

    var createForm = document.getElementById("staffOrderCreateForm");
    var tableSelect = document.getElementById("staffOrderTableSelect");
    var customerIdInput = document.getElementById("staffOrderCustomerId");
    var createMenuSelect = document.getElementById("staffOrderCreateMenuSelect");
    var createLineQtyInput = document.getElementById("staffOrderCreateLineQty");
    var createLineNoteInput = document.getElementById("staffOrderCreateLineNote");
    var createAddLineBtn = document.getElementById("staffOrderCreateAddLineBtn");
    var createLinesBody = document.getElementById("staffOrderCreateLinesBody");
    var createPreviewTotalEl = document.getElementById("staffOrderCreatePreviewTotal");
    var createMessageEl = document.getElementById("staffOrderCreateMessage");

    var itemForm = document.getElementById("staffOrderItemForm");
    var menuItemSelect = document.getElementById("staffOrderMenuItemSelect");
    var itemQtyInput = document.getElementById("staffOrderItemQuantity");
    var itemNoteInput = document.getElementById("staffOrderItemNote");

    var checkoutForm = document.getElementById("staffOrderCheckoutForm");
    var paymentMethodInput = document.getElementById("staffCheckoutPaymentMethod");
    var voucherCodeInput = document.getElementById("staffCheckoutVoucherCode");
    var momoRefInput = document.getElementById("staffCheckoutMomoRef");

    var tables = [];
    var menuItems = [];
    var orders = [];
    var currentOrder = null;
    var createDraftLines = [];

    function getOrderStatusLabel(status) {
      if (status === "pending") return "Chờ phục vụ";
      if (status === "serving") return "Đang phục vụ";
      if (status === "done") return "Hoàn tất";
      if (status === "cancelled") return "Đã hủy";
      return status || "-";
    }

    function getTableStatusLabel(status) {
      if (status === "empty") return "Trống";
      if (status === "occupied") return "Đang phục vụ";
      if (status === "reserved") return "Đã đặt";
      return status || "Không rõ";
    }

    function getTableTileClass(status) {
      if (status === "empty") return "free";
      if (status === "occupied") return "busy";
      return "";
    }

    function setDetail(order) {
      currentOrder = order || null;
      if (!currentOrder) {
        if (orderTitle) orderTitle.textContent = "Chọn đơn để xem chi tiết";
        if (orderStatusBadge) orderStatusBadge.textContent = "-";
        if (orderItemsBody) orderItemsBody.innerHTML = "<tr><td colspan='4' class='muted'>Chưa chọn đơn.</td></tr>";
        if (subtotalEl) subtotalEl.textContent = formatCurrency(0);
        if (discountEl) discountEl.textContent = formatCurrency(0);
        if (totalEl) totalEl.textContent = formatCurrency(0);
        return;
      }

      if (orderTitle) {
        orderTitle.textContent = "Đơn #" + currentOrder.id + " - " + (currentOrder.tableName || ("Bàn " + currentOrder.tableId));
      }
      if (orderStatusBadge) {
        orderStatusBadge.textContent = getOrderStatusLabel(currentOrder.status);
      }
      if (subtotalEl) subtotalEl.textContent = formatCurrency(currentOrder.subtotal);
      if (discountEl) discountEl.textContent = formatCurrency(currentOrder.discount);
      if (totalEl) totalEl.textContent = formatCurrency(currentOrder.total);

      if (!orderItemsBody) return;
      if (!currentOrder.items || !currentOrder.items.length) {
        orderItemsBody.innerHTML = "<tr><td colspan='4' class='muted'>Đơn chưa có món.</td></tr>";
        return;
      }
      orderItemsBody.innerHTML = currentOrder.items
        .map(function (item) {
          return (
            "<tr>" +
            "<td>" + item.menuItemName + "</td>" +
            "<td>" + item.quantity + "</td>" +
            "<td>" + formatCurrency(item.lineTotal) + "</td>" +
            "<td style='white-space:nowrap'>" +
            "<button type='button' class='btn btn-ghost btn-xs' data-order-item-edit='" + item.id + "'>Sửa</button> " +
            "<button type='button' class='btn btn-ghost btn-xs' data-order-item-remove='" + item.id + "'>Xóa</button>" +
            "</td>" +
            "</tr>"
          );
        })
        .join("");
    }

    function renderTables() {
      if (!tablesGrid) return;
      if (!tables.length) {
        tablesGrid.innerHTML = "<div class='table-tile'>Trống<span>Chưa có bàn</span></div>";
        return;
      }
      tablesGrid.innerHTML = tables
        .map(function (table) {
          return (
            "<div class='table-tile " + getTableTileClass(table.status) + "'>" +
            table.name +
            "<span>" + getTableStatusLabel(table.status) + "</span>" +
            "</div>"
          );
        })
        .join("");
    }

    function tableHasOpenOrder(tableId) {
      var tid = Number(tableId);
      return (orders || []).some(function (o) {
        return Number(o.tableId) === tid && (o.status === "pending" || o.status === "serving");
      });
    }

    function canCreateOrderOnTable(table) {
      if (!table) return false;
      return !tableHasOpenOrder(table.id);
    }

    function renderOrderList() {
      if (!orderListBody) return;
      if (!orders.length) {
        orderListBody.innerHTML = "<tr><td colspan='5' class='muted'>Chưa có đơn.</td></tr>";
        return;
      }
      orderListBody.innerHTML = orders
        .map(function (order) {
          return (
            "<tr>" +
            "<td>#"+ order.id + "</td>" +
            "<td>" + (order.tableName || ("Bàn " + order.tableId)) + "</td>" +
            "<td>" + getOrderStatusLabel(order.status) + "</td>" +
            "<td>" + formatCurrency(order.total) + "</td>" +
            "<td><button type='button' class='btn btn-ghost btn-xs' data-order-view='" + order.id + "'>Xem</button></td>" +
            "</tr>"
          );
        })
        .join("");
    }

    function syncCreateTableOptions() {
      if (!tableSelect) return;
      if (!tables.length) {
        tableSelect.innerHTML = "<option value=''>Không có bàn</option>";
        if (createBtn) createBtn.disabled = true;
        return;
      }
      var optionsHtml = tables
        .map(function (table) {
          var blocked = tableHasOpenOrder(table.id);
          var hint = blocked
            ? " — đã có đơn mở"
            : table.status !== "empty"
              ? " — đồng bộ trạng thái khi tạo"
              : "";
          return (
            "<option value='" +
            table.id +
            "'" +
            (blocked ? " disabled" : "") +
            ">" +
            table.name +
            " (" +
            getTableStatusLabel(table.status) +
            ")" +
            hint +
            "</option>"
          );
        })
        .join("");
      tableSelect.innerHTML = optionsHtml;
      var firstPick = tables.find(canCreateOrderOnTable);
      if (firstPick) {
        tableSelect.value = String(firstPick.id);
      }
      if (createBtn) createBtn.disabled = !firstPick;
    }

    function syncMenuItemOptions() {
      if (!menuItemSelect) return;
      if (!menuItems.length) {
        menuItemSelect.innerHTML = "<option value=''>Không có món</option>";
        return;
      }
      menuItemSelect.innerHTML = menuItems
        .map(function (item) {
          return "<option value='" + item.id + "'>" + item.name + " - " + formatCurrency(item.price) + "</option>";
        })
        .join("");
    }

    function syncCreateMenuOptions() {
      if (!createMenuSelect) return;
      if (!menuItems.length) {
        createMenuSelect.innerHTML = "<option value=''>Không có món</option>";
        return;
      }
      createMenuSelect.innerHTML = menuItems
        .map(function (item) {
          return "<option value='" + item.id + "'>" + item.name + " - " + formatCurrency(item.price) + "</option>";
        })
        .join("");
    }

    function findMenuMeta(menuItemId) {
      return menuItems.find(function (m) {
        return Number(m.id) === Number(menuItemId);
      });
    }

    function renderCreateDraftLines() {
      if (!createLinesBody) return;
      if (!createDraftLines.length) {
        createLinesBody.innerHTML =
          "<tr><td colspan='4' class='muted'>Chưa có món. Thêm ít nhất 1 món trước khi tạo đơn.</td></tr>";
      } else {
        createLinesBody.innerHTML = createDraftLines
          .map(function (line, index) {
            var lineTotal = Number(line.unitPrice || 0) * Number(line.quantity || 0);
            return (
              "<tr>" +
              "<td>" + (line.name || ("#" + line.menuItemId)) + "</td>" +
              "<td>" + line.quantity + "</td>" +
              "<td>" + formatCurrency(lineTotal) + "</td>" +
              "<td><button type='button' class='btn btn-ghost btn-xs' data-create-line-remove='" + index + "'>Xóa</button></td>" +
              "</tr>"
            );
          })
          .join("");
      }
      var sum = createDraftLines.reduce(function (acc, line) {
        return acc + Number(line.unitPrice || 0) * Number(line.quantity || 0);
      }, 0);
      if (createPreviewTotalEl) createPreviewTotalEl.textContent = formatCurrency(sum);
    }

    function resetCreateOrderDraft() {
      createDraftLines = [];
      if (createLineQtyInput) createLineQtyInput.value = "1";
      if (createLineNoteInput) createLineNoteInput.value = "";
      renderCreateDraftLines();
    }

    function loadTables() {
      return apiFetch("/api/staff/tables")
        .then(function (data) {
          tables = data || [];
          renderTables();
          syncCreateTableOptions();
        })
        .catch(function (error) {
          tables = [];
          renderTables();
          setInlineMessage(messageEl, getErrorMessage(error, "Không tải được danh sách bàn."), true);
        });
    }

    function loadMenuItems() {
      return apiFetch("/api/staff/menu-items")
        .then(function (items) {
          menuItems = (items || []).filter(function (item) {
            return item.isAvailable !== false;
          });
          syncMenuItemOptions();
          syncCreateMenuOptions();
        })
        .catch(function () {
          menuItems = [];
          syncMenuItemOptions();
          syncCreateMenuOptions();
        });
    }

    function loadOrders() {
      return apiFetch("/api/staff/orders")
        .then(function (data) {
          orders = data || [];
          renderOrderList();
          syncCreateTableOptions();
          if (currentOrder) {
            var same = orders.find(function (x) {
              return x.id === currentOrder.id;
            });
            if (same) {
              return loadOrderDetail(same.id);
            }
          }
          return null;
        })
        .catch(function (error) {
          orders = [];
          renderOrderList();
          syncCreateTableOptions();
          setInlineMessage(messageEl, getErrorMessage(error, "Không tải được danh sách đơn."), true);
        });
    }

    function loadOrderDetail(orderId) {
      return apiFetch("/api/staff/orders/" + orderId)
        .then(function (order) {
          setDetail(order);
          return order;
        })
        .catch(function (error) {
          setInlineMessage(messageEl, getErrorMessage(error, "Không tải được chi tiết đơn."), true);
        });
    }

    if (createBtn) {
      createBtn.addEventListener("click", function () {
        Promise.all([loadTables(), loadMenuItems(), loadOrders()]).then(function () {
          resetCreateOrderDraft();
          if (createMessageEl) setInlineMessage(createMessageEl, "", false);
          openModalById("staffOrderCreateModal");
          if (!tables.some(canCreateOrderOnTable)) {
            setInlineMessage(messageEl, "Tất cả bàn đều đã có đơn đang mở (chờ/đang phục vụ).", true);
          }
        });
      });
    }

    if (createAddLineBtn) {
      createAddLineBtn.addEventListener("click", function () {
        if (createMessageEl) setInlineMessage(createMessageEl, "", false);
        var menuItemId = Number((createMenuSelect && createMenuSelect.value) || 0);
        var qty = Number((createLineQtyInput && createLineQtyInput.value) || 0);
        var note = (createLineNoteInput && createLineNoteInput.value ? createLineNoteInput.value : "").trim();
        if (!menuItemId) {
          if (createMessageEl) setInlineMessage(createMessageEl, "Vui lòng chọn món.", true);
          return;
        }
        if (!qty || qty < 1 || !Number.isInteger(qty)) {
          if (createMessageEl) setInlineMessage(createMessageEl, "Số lượng phải là số nguyên ≥ 1.", true);
          return;
        }
        var meta = findMenuMeta(menuItemId);
        if (!meta) {
          if (createMessageEl) setInlineMessage(createMessageEl, "Món không tồn tại hoặc đã ngừng bán.", true);
          return;
        }
        var merged = false;
        for (var i = 0; i < createDraftLines.length; i++) {
          if (
            Number(createDraftLines[i].menuItemId) === menuItemId &&
            (createDraftLines[i].note || "") === note
          ) {
            createDraftLines[i].quantity += qty;
            merged = true;
            break;
          }
        }
        if (!merged) {
          createDraftLines.push({
            menuItemId: menuItemId,
            quantity: qty,
            note: note || null,
            name: meta.name,
            unitPrice: Number(meta.price) || 0
          });
        }
        if (createLineQtyInput) createLineQtyInput.value = "1";
        if (createLineNoteInput) createLineNoteInput.value = "";
        renderCreateDraftLines();
      });
    }

    if (createLinesBody) {
      createLinesBody.addEventListener("click", function (event) {
        var removeBtn = event.target.closest("[data-create-line-remove]");
        if (!removeBtn) return;
        var idx = Number(removeBtn.getAttribute("data-create-line-remove"));
        if (Number.isNaN(idx)) return;
        createDraftLines.splice(idx, 1);
        renderCreateDraftLines();
      });
    }

    if (createForm) {
      createForm.addEventListener("submit", function (event) {
        event.preventDefault();
        var submitBtn = createForm.querySelector("button[type='submit']");
        if (submitBtn && submitBtn.disabled) return;
        if (createMessageEl) setInlineMessage(createMessageEl, "", false);
        if (!createDraftLines.length) {
          if (createMessageEl) setInlineMessage(createMessageEl, "Vui lòng thêm ít nhất một món vào đơn.", true);
          else setInlineMessage(messageEl, "Vui lòng thêm ít nhất một món vào đơn.", true);
          return;
        }
        var tableId = Number((tableSelect && tableSelect.value) || 0);
        if (!tableId) {
          setInlineMessage(messageEl, "Vui lòng chọn bàn.", true);
          return;
        }
        var selectedTable = tables.find(function (table) {
          return Number(table.id) === tableId;
        });
        if (tableHasOpenOrder(tableId)) {
          if (createMessageEl) setInlineMessage(createMessageEl, "Bàn này đã có đơn mở. Vui lòng chọn bàn khác.", true);
          else setInlineMessage(messageEl, "Bàn này đã có đơn mở. Vui lòng chọn bàn khác.", true);
          return;
        }
        var customerIdRaw = (customerIdInput && customerIdInput.value ? customerIdInput.value : "").trim();
        var payload = {
          tableId: tableId,
          items: createDraftLines.map(function (line) {
            return {
              menuItemId: line.menuItemId,
              quantity: line.quantity,
              note: line.note || undefined
            };
          })
        };
        if (customerIdRaw) {
          payload.customerId = Number(customerIdRaw);
          if (!Number.isInteger(payload.customerId) || payload.customerId < 1) {
            setInlineMessage(messageEl, "Mã khách hàng không hợp lệ.", true);
            return;
          }
        }
        if (submitBtn) submitBtn.disabled = true;
        apiFetch("/api/staff/orders", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload)
        })
          .then(function (order) {
            closeModal(document.getElementById("staffOrderCreateModal"));
            if (createForm) createForm.reset();
            resetCreateOrderDraft();
            setInlineMessage(messageEl, "Đã tạo đơn mới kèm " + (order.items ? order.items.length : 0) + " món.", false);
            return Promise.all([loadTables(), loadOrders()]).then(function () {
              return loadOrderDetail(order.id);
            });
          })
          .catch(function (error) {
            var msg = getErrorMessage(error, "Không thể tạo đơn.");
            if (msg === "Table is not available for new order" || msg === "Table already has an open order") {
              msg = "Bàn đã có khách hoặc đã có đơn mở. Vui lòng chọn bàn khác.";
            } else if (msg === "Menu item not found") {
              msg = "Không tìm thấy món trong thực đơn.";
            } else if (msg === "Menu item is not available") {
              msg = "Món đã chọn hiện không bán. Vui lòng bỏ hoặc đổi món.";
            } else if (msg === "Quantity must be at least 1" || msg === "Menu item id is required on order line") {
              msg = "Dữ liệu món không hợp lệ. Vui lòng thử lại.";
            }
            if (createMessageEl) setInlineMessage(createMessageEl, msg, true);
            else setInlineMessage(messageEl, msg, true);
          })
          .finally(function () {
            if (submitBtn) submitBtn.disabled = false;
          });
      });
    }

    if (orderListBody) {
      orderListBody.addEventListener("click", function (event) {
        var viewBtn = event.target.closest("[data-order-view]");
        if (!viewBtn) return;
        var orderId = Number(viewBtn.getAttribute("data-order-view"));
        if (!orderId) return;
        loadOrderDetail(orderId);
      });
    }

    if (orderItemsBody) {
      orderItemsBody.addEventListener("click", function (event) {
        var editBtn = event.target.closest("[data-order-item-edit]");
        if (editBtn && currentOrder) {
          var editItemId = Number(editBtn.getAttribute("data-order-item-edit"));
          var currentItem = (currentOrder.items || []).find(function (item) {
            return item.id === editItemId;
          });
          if (!currentItem) return;
          var quantityRaw = window.prompt("Số lượng mới", String(currentItem.quantity));
          if (!quantityRaw) return;
          var quantity = Number(quantityRaw);
          if (!quantity || quantity < 1) {
            setInlineMessage(messageEl, "Số lượng phải lớn hơn 0.", true);
            return;
          }
          var note = window.prompt("Ghi chú", currentItem.note || "") || "";
          apiFetch("/api/staff/orders/" + currentOrder.id + "/items/" + currentItem.id, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ quantity: quantity, note: note })
          })
            .then(function (order) {
              setDetail(order);
              loadOrders();
              setInlineMessage(messageEl, "Đã cập nhật món.", false);
            })
            .catch(function (error) {
              setInlineMessage(messageEl, getErrorMessage(error, "Không thể cập nhật món."), true);
            });
          return;
        }

        var removeBtn = event.target.closest("[data-order-item-remove]");
        if (removeBtn && currentOrder) {
          var removeItemId = Number(removeBtn.getAttribute("data-order-item-remove"));
          if (!window.confirm("Xóa món này khỏi đơn?")) return;
          apiFetch("/api/staff/orders/" + currentOrder.id + "/items/" + removeItemId, {
            method: "DELETE"
          })
            .then(function (order) {
              setDetail(order);
              loadOrders();
              setInlineMessage(messageEl, "Đã xóa món khỏi đơn.", false);
            })
            .catch(function (error) {
              setInlineMessage(messageEl, getErrorMessage(error, "Không thể xóa món."), true);
            });
        }
      });
    }

    if (addItemBtn) {
      addItemBtn.addEventListener("click", function () {
        if (!currentOrder) {
          setInlineMessage(messageEl, "Vui lòng chọn đơn trước khi thêm món.", true);
          return;
        }
        openModalById("staffOrderItemModal");
      });
    }

    if (itemForm) {
      itemForm.addEventListener("submit", function (event) {
        event.preventDefault();
        if (!currentOrder) {
          setInlineMessage(messageEl, "Đơn hiện tại không hợp lệ.", true);
          return;
        }
        var menuItemId = Number((menuItemSelect && menuItemSelect.value) || 0);
        var quantity = Number((itemQtyInput && itemQtyInput.value) || 0);
        if (!menuItemId || !quantity || quantity < 1) {
          setInlineMessage(messageEl, "Vui lòng chọn món và số lượng hợp lệ.", true);
          return;
        }
        apiFetch("/api/staff/orders/" + currentOrder.id + "/items", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            menuItemId: menuItemId,
            quantity: quantity,
            note: (itemNoteInput && itemNoteInput.value ? itemNoteInput.value : "").trim()
          })
        })
          .then(function (order) {
            setDetail(order);
            closeModal(document.getElementById("staffOrderItemModal"));
            if (itemForm) itemForm.reset();
            if (itemQtyInput) itemQtyInput.value = "1";
            loadOrders();
            setInlineMessage(messageEl, "Đã thêm món vào đơn.", false);
          })
          .catch(function (error) {
            setInlineMessage(messageEl, getErrorMessage(error, "Không thể thêm món."), true);
          });
      });
    }

    if (checkoutBtn) {
      checkoutBtn.addEventListener("click", function () {
        if (!currentOrder) {
          setInlineMessage(messageEl, "Vui lòng chọn đơn cần thanh toán.", true);
          return;
        }
        setInlineMessage(checkoutMessageEl, "", false);
        openModalById("staffOrderCheckoutModal");
      });
    }

    if (checkoutForm) {
      checkoutForm.addEventListener("submit", function (event) {
        event.preventDefault();
        if (!currentOrder) {
          setInlineMessage(checkoutMessageEl, "Vui lòng chọn đơn.", true);
          return;
        }
        apiFetch("/api/staff/orders/" + currentOrder.id + "/checkout", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            paymentMethod: paymentMethodInput ? paymentMethodInput.value : "cash",
            voucherCode: voucherCodeInput ? voucherCodeInput.value.trim() : "",
            momoRef: momoRefInput ? momoRefInput.value.trim() : ""
          })
        })
          .then(function (payment) {
            setInlineMessage(checkoutMessageEl, "Thanh toán thành công. Mã giao dịch #" + payment.paymentId, false);
            setInlineMessage(messageEl, "Đơn đã được thanh toán.", false);
            return loadOrders().then(function () {
              return loadOrderDetail(currentOrder.id);
            });
          })
          .catch(function (error) {
            setInlineMessage(checkoutMessageEl, getErrorMessage(error, "Không thể thanh toán đơn."), true);
          });
      });
    }

    if (cancelBtn) {
      cancelBtn.addEventListener("click", function () {
        if (!currentOrder) {
          setInlineMessage(messageEl, "Vui lòng chọn đơn cần hủy.", true);
          return;
        }
        var reason = window.prompt("Nhập lý do hủy đơn");
        if (!reason) return;
        apiFetch("/api/staff/orders/" + currentOrder.id + "/cancel", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ reason: reason })
        })
          .then(function (order) {
            setDetail(order);
            loadOrders();
            setInlineMessage(messageEl, "Đã hủy đơn.", false);
          })
          .catch(function (error) {
            setInlineMessage(messageEl, getErrorMessage(error, "Không thể hủy đơn."), true);
          });
      });
    }

    if (invoiceBtn) {
      invoiceBtn.addEventListener("click", function () {
        if (!currentOrder) {
          setInlineMessage(messageEl, "Vui lòng chọn đơn để in hóa đơn.", true);
          return;
        }
        fetch("/api/staff/orders/" + currentOrder.id + "/invoice/pdf")
          .then(function (response) {
            if (!response.ok) {
              throw new Error("Không thể tạo file PDF.");
            }
            return response.blob();
          })
          .then(function (blob) {
            var url = window.URL.createObjectURL(blob);
            var link = document.createElement("a");
            link.href = url;
            link.download = "invoice-" + currentOrder.id + ".pdf";
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
          })
          .catch(function (error) {
            setInlineMessage(messageEl, getErrorMessage(error, "Không thể in hóa đơn."), true);
          });
      });
    }

    Promise.all([loadTables(), loadMenuItems(), loadOrders()]).then(function () {
      setDetail(null);
    });
  }

  function initStaffMembersPage() {
    var searchForm = document.getElementById("staffMemberSearchForm");
    if (!searchForm) return;

    var phoneInput = document.getElementById("staffMemberPhone");
    var messageEl = document.getElementById("staffMemberMessage");
    var memberNameEl = document.getElementById("staffMemberName");
    var memberPointsEl = document.getElementById("staffMemberPoints");
    var vouchersBody = document.getElementById("staffMemberVouchersBody");
    var pointsBody = document.getElementById("staffMemberPointsBody");

    var openCreateBtn = document.getElementById("staffMemberOpenCreateBtn");
    var openRedeemBtn = document.getElementById("staffMemberOpenRedeemBtn");
    var reloadBtn = document.getElementById("staffMemberReloadBtn");

    var createForm = document.getElementById("staffMemberCreateForm");
    var createNameInput = document.getElementById("staffMemberCreateName");
    var createPhoneInput = document.getElementById("staffMemberCreatePhone");
    var createMessageEl = document.getElementById("staffMemberCreateMessage");

    var redeemForm = document.getElementById("staffMemberRedeemForm");
    var redeemCodeInput = document.getElementById("staffRedeemCode");
    var redeemTypeInput = document.getElementById("staffRedeemType");
    var redeemValueInput = document.getElementById("staffRedeemValue");
    var redeemPointsInput = document.getElementById("staffRedeemPointsRequired");
    var redeemMessageEl = document.getElementById("staffMemberRedeemMessage");

    var currentCustomer = null;

    function formatTime(value) {
      if (!value) return "-";
      return new Date(value).toLocaleString("vi-VN");
    }

    function renderCustomerSummary(customer) {
      currentCustomer = customer || null;
      if (memberNameEl) memberNameEl.textContent = customer ? customer.fullName : "-";
      if (memberPointsEl) memberPointsEl.textContent = customer ? String(customer.points || 0) : "0";
      if (!customer) {
        if (vouchersBody) vouchersBody.innerHTML = "<tr><td colspan='5' class='muted'>Chưa chọn khách.</td></tr>";
        if (pointsBody) pointsBody.innerHTML = "<tr><td colspan='3' class='muted'>Chưa chọn khách.</td></tr>";
      }
    }

    function loadCustomerByPhone(phone) {
      return apiFetch("/api/staff/customers/by-phone?phone=" + encodeURIComponent(phone))
        .then(function (customer) {
          renderCustomerSummary(customer);
          setInlineMessage(messageEl, "Đã tải thông tin khách hàng.", false);
          return Promise.all([
            apiFetch("/api/staff/customers/" + customer.id + "/vouchers"),
            apiFetch("/api/staff/customers/" + customer.id + "/points/history")
          ]).then(function (results) {
            var vouchers = results[0] || [];
            var pointLogs = results[1] || [];

            if (vouchersBody) {
              vouchersBody.innerHTML = vouchers.length
                ? vouchers
                    .map(function (voucher) {
                      return (
                        "<tr>" +
                        "<td>" + voucher.code + "</td>" +
                        "<td>" + voucher.type + "</td>" +
                        "<td>" + formatNumber(voucher.value) + "</td>" +
                        "<td>" + (voucher.isUsed ? "Đã dùng" : "Chưa dùng") + "</td>" +
                        "<td>" + formatTime(voucher.expiresAt) + "</td>" +
                        "</tr>"
                      );
                    })
                    .join("")
                : "<tr><td colspan='5' class='muted'>Khách chưa có voucher.</td></tr>";
            }

            if (pointsBody) {
              pointsBody.innerHTML = pointLogs.length
                ? pointLogs
                    .map(function (log) {
                      var color = Number(log.delta) >= 0 ? "var(--success)" : "var(--danger)";
                      var sign = Number(log.delta) > 0 ? "+" : "";
                      return (
                        "<tr>" +
                        "<td>" + formatTime(log.createdAt) + "</td>" +
                        "<td style='font-weight:600;color:" + color + "'>" + sign + log.delta + "</td>" +
                        "<td>" + (log.description || "-") + "</td>" +
                        "</tr>"
                      );
                    })
                    .join("")
                : "<tr><td colspan='3' class='muted'>Chưa có lịch sử điểm.</td></tr>";
            }
          });
        })
        .catch(function (error) {
          renderCustomerSummary(null);
          setInlineMessage(messageEl, getErrorMessage(error, "Không tìm thấy khách hàng."), true);
        });
    }

    searchForm.addEventListener("submit", function (event) {
      event.preventDefault();
      var phone = (phoneInput && phoneInput.value ? phoneInput.value : "").trim();
      if (!phone) {
        setInlineMessage(messageEl, "Vui lòng nhập số điện thoại.", true);
        return;
      }
      loadCustomerByPhone(phone);
    });

    if (reloadBtn) {
      reloadBtn.addEventListener("click", function () {
        if (!currentCustomer) {
          setInlineMessage(messageEl, "Vui lòng tra cứu khách hàng trước.", true);
          return;
        }
        loadCustomerByPhone(currentCustomer.phone);
      });
    }

    if (openCreateBtn) {
      openCreateBtn.addEventListener("click", function () {
        setInlineMessage(createMessageEl, "", false);
        openModalById("staffMemberCreateModal");
      });
    }

    if (createForm) {
      createForm.addEventListener("submit", function (event) {
        event.preventDefault();
        apiFetch("/api/staff/customers", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            fullName: createNameInput ? createNameInput.value.trim() : "",
            phone: createPhoneInput ? createPhoneInput.value.trim() : ""
          })
        })
          .then(function (customer) {
            setInlineMessage(createMessageEl, "Đã tạo khách hàng thành công.", false);
            if (phoneInput) phoneInput.value = customer.phone || "";
            closeModal(document.getElementById("staffMemberCreateModal"));
            if (createForm) createForm.reset();
            return loadCustomerByPhone(customer.phone);
          })
          .catch(function (error) {
            setInlineMessage(createMessageEl, getErrorMessage(error, "Không thể tạo khách hàng."), true);
          });
      });
    }

    if (openRedeemBtn) {
      openRedeemBtn.addEventListener("click", function () {
        if (!currentCustomer) {
          setInlineMessage(messageEl, "Vui lòng chọn khách trước khi đổi voucher.", true);
          return;
        }
        setInlineMessage(redeemMessageEl, "", false);
        openModalById("staffMemberRedeemModal");
      });
    }

    if (redeemForm) {
      redeemForm.addEventListener("submit", function (event) {
        event.preventDefault();
        if (!currentCustomer) {
          setInlineMessage(redeemMessageEl, "Vui lòng chọn khách trước.", true);
          return;
        }
        apiFetch("/api/staff/customers/" + currentCustomer.id + "/vouchers/redeem", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            code: redeemCodeInput ? redeemCodeInput.value.trim() : "",
            type: redeemTypeInput ? redeemTypeInput.value : "fixed",
            value: Number(redeemValueInput ? redeemValueInput.value : 0),
            pointsRequired: Number(redeemPointsInput ? redeemPointsInput.value : 0)
          })
        })
          .then(function () {
            setInlineMessage(redeemMessageEl, "Đổi voucher thành công.", false);
            closeModal(document.getElementById("staffMemberRedeemModal"));
            if (redeemForm) redeemForm.reset();
            return loadCustomerByPhone(currentCustomer.phone);
          })
          .catch(function (error) {
            setInlineMessage(redeemMessageEl, getErrorMessage(error, "Không thể đổi voucher."), true);
          });
      });
    }

    renderCustomerSummary(null);
  }

  function initStaffMomoPaymentPage() {
    var orderSelect = document.getElementById("staffMomoOrderSelect");
    if (!orderSelect) return;

    var orderSummary = document.getElementById("staffMomoOrderSummary");
    var qrBox = document.getElementById("staffMomoQrBox");
    var generateBtn = document.getElementById("staffMomoGenerateBtn");
    var confirmBtn = document.getElementById("staffMomoConfirmBtn");
    var cancelBtn = document.getElementById("staffMomoCancelBtn");
    var statusBadge = document.getElementById("staffMomoStatusBadge");
    var refEl = document.getElementById("staffMomoRef");
    var messageEl = document.getElementById("staffMomoMessage");

    var openOrders = [];
    var selectedOrder = null;
    var currentMomoRef = "";

    function updateStatus(text, isSuccess) {
      if (!statusBadge) return;
      statusBadge.textContent = text;
      if (isSuccess) {
        statusBadge.style.background = "rgba(46,125,50,0.16)";
        statusBadge.style.color = "var(--success)";
        return;
      }
      statusBadge.style.background = "rgba(188,108,37,0.15)";
      statusBadge.style.color = "var(--warning)";
    }

    function resetQr() {
      currentMomoRef = "";
      if (refEl) refEl.textContent = "-";
      if (qrBox) {
        qrBox.innerHTML = "QR MoMo<br><span style='font-size:0.75rem;margin-top:0.35rem;display:block'>Bấm \"Tạo / làm mới mã\"</span>";
      }
      updateStatus("Chưa tạo mã", false);
    }

    function renderOrderSummary(order) {
      if (!orderSummary) return;
      if (!order) {
        orderSummary.textContent = "Chưa chọn đơn.";
        return;
      }
      orderSummary.textContent = "Đơn #" + order.id + " - Bàn " + (order.tableName || order.tableId) + " · Số tiền " + formatCurrency(order.total || 0);
    }

    function syncOrderOptions() {
      if (!openOrders.length) {
        orderSelect.innerHTML = "<option value=''>Không có đơn mở cần thanh toán</option>";
        orderSelect.disabled = true;
        selectedOrder = null;
        renderOrderSummary(null);
        resetQr();
        return;
      }
      orderSelect.disabled = false;
      orderSelect.innerHTML = openOrders
        .map(function (order) {
          return "<option value='" + order.id + "'>#" + order.id + " - " + (order.tableName || ("Bàn " + order.tableId)) + " (" + formatCurrency(order.total || 0) + ")</option>";
        })
        .join("");
      selectedOrder = openOrders[0];
      orderSelect.value = String(selectedOrder.id);
      renderOrderSummary(selectedOrder);
      resetQr();
    }

    function loadOpenOrders() {
      setInlineMessage(messageEl, "", false);
      return Promise.all([
        apiFetch("/api/staff/orders?status=pending"),
        apiFetch("/api/staff/orders?status=serving")
      ])
        .then(function (results) {
          openOrders = (results[0] || []).concat(results[1] || []);
          syncOrderOptions();
        })
        .catch(function (error) {
          openOrders = [];
          syncOrderOptions();
          setInlineMessage(messageEl, getErrorMessage(error, "Không tải được danh sách đơn mở."), true);
        });
    }

    if (orderSelect) {
      orderSelect.addEventListener("change", function () {
        var orderId = Number(orderSelect.value || 0);
        selectedOrder = openOrders.find(function (order) { return order.id === orderId; }) || null;
        renderOrderSummary(selectedOrder);
        resetQr();
      });
    }

    if (generateBtn) {
      generateBtn.addEventListener("click", function () {
        if (!selectedOrder) {
          setInlineMessage(messageEl, "Vui lòng chọn đơn cần thanh toán.", true);
          return;
        }
        if (Number(selectedOrder.total || 0) <= 0) {
          setInlineMessage(messageEl, "Đơn chưa có tiền cần thanh toán.", true);
          return;
        }
        currentMomoRef = "MOMO-" + selectedOrder.id + "-" + Date.now();
        if (refEl) refEl.textContent = currentMomoRef;
        var qrPayload = "ORDER:" + selectedOrder.id + "|AMOUNT:" + Number(selectedOrder.total || 0) + "|REF:" + currentMomoRef;
        var qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=" + encodeURIComponent(qrPayload);
        if (qrBox) {
          qrBox.innerHTML = "<img src='" + qrUrl + "' alt='QR MoMo' style='max-width:100%;border-radius:12px' />";
        }
        updateStatus("Đang chờ khách quét", false);
        setInlineMessage(messageEl, "Đã tạo mã MoMo cho đơn #" + selectedOrder.id + ".", false);
      });
    }

    if (cancelBtn) {
      cancelBtn.addEventListener("click", function () {
        resetQr();
        setInlineMessage(messageEl, "Đã hủy giao dịch MoMo hiện tại.", false);
      });
    }

    if (confirmBtn) {
      confirmBtn.addEventListener("click", function () {
        if (!selectedOrder) {
          setInlineMessage(messageEl, "Vui lòng chọn đơn cần thanh toán.", true);
          return;
        }
        if (!currentMomoRef) {
          setInlineMessage(messageEl, "Vui lòng tạo mã MoMo trước khi xác nhận.", true);
          return;
        }
        if (confirmBtn.disabled) return;
        confirmBtn.disabled = true;
        apiFetch("/api/staff/orders/" + selectedOrder.id + "/checkout", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            paymentMethod: "momo",
            voucherCode: "",
            momoRef: currentMomoRef
          })
        })
          .then(function (payment) {
            updateStatus("Thanh toán thành công", true);
            setInlineMessage(messageEl, "Đã xác nhận thanh toán MoMo. Mã giao dịch #" + payment.paymentId, false);
            return loadOpenOrders();
          })
          .catch(function (error) {
            setInlineMessage(messageEl, getErrorMessage(error, "Không thể xác nhận thanh toán MoMo."), true);
          })
          .finally(function () {
            confirmBtn.disabled = false;
          });
      });
    }

    loadOpenOrders();
  }

  initRegisterValidation();
  initInventoryPage();
  initEmployeesPage();
  initMenuPage();
  initTablesPage();
  initManagerAttendancePage();
  initManagerDashboardPage();
  initAdminDashboardPage();
  initAdminUsersPage();
  initAdminAuditPage();
  initStaffClockPage();
  initStaffOrdersPage();
  initStaffMembersPage();
  initStaffMomoPaymentPage();
})();
