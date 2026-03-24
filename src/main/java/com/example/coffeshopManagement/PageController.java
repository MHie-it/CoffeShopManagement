package com.example.coffeshopManagement;

import com.example.coffeshopManagement.dto.auth.AuthRegisterRequest;
import com.example.coffeshopManagement.exception.BadRequestException;
import com.example.coffeshopManagement.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class PageController {
	private final AuthService authService;
	private final AuthenticationManager authenticationManager;

	public PageController(AuthService authService, AuthenticationManager authenticationManager) {
		this.authService = authService;
		this.authenticationManager = authenticationManager;
	}

	@GetMapping("/")
	public String home(Authentication authentication, Model model) {
		boolean isAuthenticated = authentication != null && authentication.isAuthenticated()
				&& authentication.getAuthorities().stream().noneMatch(a -> "ROLE_ANONYMOUS".equals(a.getAuthority()));
		model.addAttribute("isAuthenticated", isAuthenticated);
		if (isAuthenticated) {
			model.addAttribute("currentUsername", authentication.getName());
			model.addAttribute("homeRoute", resolveHomeRoute(authentication));
		}
		return "home";
	}

	@GetMapping("/login")
	public String login(Authentication authentication) {
		if (isAuthenticated(authentication)) {
			return "redirect:" + resolveHomeRoute(authentication);
		}
		return "login";
	}

	@GetMapping("/register")
	public String register(Authentication authentication, Model model) {
		if (isAuthenticated(authentication)) {
			return "redirect:" + resolveHomeRoute(authentication);
		}
		if (!model.containsAttribute("registerRequest")) {
			model.addAttribute("registerRequest", new AuthRegisterRequest());
		}
		return "register";
	}

	@PostMapping("/register")
	public String registerSubmit(
			@Valid @ModelAttribute("registerRequest") AuthRegisterRequest registerRequest,
			BindingResult bindingResult,
			HttpServletRequest request,
			Model model) {
		if (bindingResult.hasErrors()) {
			return "register";
		}

		try {
			authService.register(registerRequest);

			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(registerRequest.getUsername(), registerRequest.getPassword()));
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
			request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

			return "redirect:" + resolveHomeRoute(authentication);
		} catch (BadRequestException ex) {
			String message = ex.getMessage();
			if ("Username is already taken".equals(message)) {
				bindingResult.addError(new FieldError("registerRequest", "username", message));
			} else if ("Email is already taken".equals(message)) {
				bindingResult.addError(new FieldError("registerRequest", "email", message));
			} else if ("Phone number is already taken".equals(message)) {
				bindingResult.addError(new FieldError("registerRequest", "phone", message));
			} else if ("Password confirmation does not match".equals(message)) {
				bindingResult.addError(new FieldError("registerRequest", "confirmPassword", message));
			} else {
				model.addAttribute("formError", message);
			}
			return "register";
		}
	}

	private static boolean isAuthenticated(Authentication authentication) {
		return authentication != null && authentication.isAuthenticated()
				&& authentication.getAuthorities().stream().noneMatch(a -> "ROLE_ANONYMOUS".equals(a.getAuthority()));
	}

	private static String resolveHomeRoute(Authentication authentication) {
		if (authentication == null) {
			return "/";
		}

		for (GrantedAuthority authority : authentication.getAuthorities()) {
			String role = authority.getAuthority();
			if ("ROLE_ADMIN".equals(role)) {
				return "/admin";
			}
			if ("ROLE_MANAGER".equals(role)) {
				return "/manager/inventory";
			}
			if ("ROLE_STAFF".equals(role)) {
				return "/staff/clock";
			}
		}

		return "/";
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
