(function () {
  var toggle = document.querySelector(".menu-toggle");
  var sidebar = document.querySelector(".sidebar");
  var backdrop = document.querySelector(".sidebar-backdrop");

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

  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape") close();
  });
})();
