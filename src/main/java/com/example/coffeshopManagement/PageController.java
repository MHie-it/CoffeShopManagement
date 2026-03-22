package com.example.coffeshopManagement;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PageController {

	@GetMapping("/")
	public String home() {
		return "home";
	}

	@GetMapping("/login")
	public String login() {
		return "login";
	}

	@PostMapping("/login")
	public String loginSubmit() {
		return "redirect:/";
	}

	@GetMapping("/admin")
	public String adminDashboard(Model model) {
		addShell(model, "ADMIN", "revenue", "Admin", "Thống kê doanh thu",
				"Biểu đồ doanh thu, xuất báo cáo — kết nối API sau.");
		return "admin/dashboard";
	}

	@GetMapping("/admin/users")
	public String adminUsers(Model model) {
		addShell(model, "ADMIN", "users", "Admin", "Tài khoản & phân quyền",
				"Tạo, khóa tài khoản và gán quyền RBAC.");
		return "admin/users";
	}

	@GetMapping("/admin/audit")
	public String adminAudit(Model model) {
		addShell(model, "ADMIN", "audit", "Admin", "Nhật ký hành động",
				"Theo dõi thao tác người dùng trên hệ thống.");
		return "admin/audit";
	}

	@GetMapping("/manager/inventory")
	public String managerInventory(Model model) {
		addShell(model, "MANAGER", "inventory", "Quản lý", "Nhập hàng & tồn kho",
				"Cảnh báo nguyên liệu sắp hết — dữ liệu demo.");
		return "manager/inventory";
	}

	@GetMapping("/manager/employees")
	public String managerEmployees(Model model) {
		addShell(model, "MANAGER", "employees", "Quản lý", "Nhân viên",
				"Hồ sơ, lương và ca làm việc.");
		return "manager/employees";
	}

	@GetMapping("/manager/attendance")
	public String managerAttendance(Model model) {
		addShell(model, "MANAGER", "attendance", "Quản lý", "Báo cáo chấm công",
				"Tổng hợp giờ làm theo tháng từ dữ liệu chấm công.");
		return "manager/attendance";
	}

	@GetMapping("/manager/menu")
	public String managerMenu(Model model) {
		addShell(model, "MANAGER", "menu", "Quản lý", "Quản lý menu",
				"CRUD món, danh mục, giá và ảnh.");
		return "manager/menu";
	}

	@GetMapping("/manager/tables")
	public String managerTables(Model model) {
		addShell(model, "MANAGER", "tables", "Quản lý", "Sơ đồ bàn",
				"Trạng thái bàn: trống / đang phục vụ / đã đặt.");
		return "manager/tables";
	}

	@GetMapping("/staff/clock")
	public String staffClock(Model model) {
		addShell(model, "STAFF", "clock", "Nhân viên", "Chấm công",
				"Check-in / check-out trong ngày làm việc.");
		return "staff/clock";
	}

	@GetMapping("/staff/orders")
	public String staffOrders(Model model) {
		addShell(model, "STAFF", "orders", "Nhân viên", "Đơn theo bàn",
				"Tạo đơn, sửa hoặc hủy trước khi thanh toán — in hóa đơn.");
		return "staff/orders";
	}

	@GetMapping("/staff/members")
	public String staffMembers(Model model) {
		addShell(model, "STAFF", "members", "Nhân viên", "Khách thành viên",
				"Tích điểm, đổi voucher.");
		return "staff/members";
	}

	@GetMapping("/staff/payment")
	public String staffPayment(Model model) {
		addShell(model, "STAFF", "payment", "Nhân viên", "Thanh toán MoMo",
				"Tạo mã QR và xác nhận giao dịch.");
		return "staff/payment";
	}

	private static void addShell(Model model, String role, String navActive, String roleLabel,
			String pageTitle, String pageSubtitle) {
		model.addAttribute("role", role);
		model.addAttribute("navActive", navActive);
		model.addAttribute("roleLabel", roleLabel);
		model.addAttribute("pageTitle", pageTitle);
		model.addAttribute("pageSubtitle", pageSubtitle);
	}
}
