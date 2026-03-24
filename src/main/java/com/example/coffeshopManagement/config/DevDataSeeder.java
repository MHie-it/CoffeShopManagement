package com.example.coffeshopManagement.config;

import com.example.coffeshopManagement.entity.Attendance;
import com.example.coffeshopManagement.entity.Category;
import com.example.coffeshopManagement.entity.Customer;
import com.example.coffeshopManagement.entity.Employee;
import com.example.coffeshopManagement.entity.Ingredient;
import com.example.coffeshopManagement.entity.MenuItem;
import com.example.coffeshopManagement.entity.OrderItem;
import com.example.coffeshopManagement.entity.OrderStatus;
import com.example.coffeshopManagement.entity.Role;
import com.example.coffeshopManagement.entity.ShopOrder;
import com.example.coffeshopManagement.entity.TableEntity;
import com.example.coffeshopManagement.entity.TableStatus;
import com.example.coffeshopManagement.entity.User;
import com.example.coffeshopManagement.repository.AttendanceRepository;
import com.example.coffeshopManagement.repository.CategoryRepository;
import com.example.coffeshopManagement.repository.CustomerRepository;
import com.example.coffeshopManagement.repository.EmployeeRepository;
import com.example.coffeshopManagement.repository.IngredientRepository;
import com.example.coffeshopManagement.repository.MenuItemRepository;
import com.example.coffeshopManagement.repository.OrderItemRepository;
import com.example.coffeshopManagement.repository.RoleRepository;
import com.example.coffeshopManagement.repository.ShopOrderRepository;
import com.example.coffeshopManagement.repository.TableEntityRepository;
import com.example.coffeshopManagement.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DevDataSeeder implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final TableEntityRepository tableEntityRepository;
    private final IngredientRepository ingredientRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final AttendanceRepository attendanceRepository;
    private final ShopOrderRepository shopOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DevDataSeeder(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            MenuItemRepository menuItemRepository,
            TableEntityRepository tableEntityRepository,
            IngredientRepository ingredientRepository,
            EmployeeRepository employeeRepository,
            CustomerRepository customerRepository,
            AttendanceRepository attendanceRepository,
            ShopOrderRepository shopOrderRepository,
            OrderItemRepository orderItemRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.tableEntityRepository = tableEntityRepository;
        this.ingredientRepository = ingredientRepository;
        this.employeeRepository = employeeRepository;
        this.customerRepository = customerRepository;
        this.attendanceRepository = attendanceRepository;
        this.shopOrderRepository = shopOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, Role> roles = seedRoles();
        Map<String, User> users = seedUsers(roles);
        Map<String, Category> categories = seedCategories();
        Map<String, MenuItem> menuItems = seedMenuItems(categories);
        Map<String, TableEntity> tables = seedTables();
        seedIngredients();
        Map<String, Employee> employees = seedEmployees();
        Map<String, Customer> customers = seedCustomers();
        seedAttendance(employees, users);
        seedOrders(users, customers, tables, menuItems);
    }

    private Map<String, Role> seedRoles() {
        Map<String, Role> result = new HashMap<>();
        result.put("ROLE_ADMIN", getOrCreateRole("ROLE_ADMIN"));
        result.put("ROLE_MANAGER", getOrCreateRole("ROLE_MANAGER"));
        result.put("ROLE_STAFF", getOrCreateRole("ROLE_STAFF"));
        return result;
    }

    private Map<String, User> seedUsers(Map<String, Role> roles) {
        Map<String, User> result = new HashMap<>();
        result.put("admin", getOrCreateUser(
                "admin",
                "System Admin",
                "admin@coffeshop.local",
                "0900000001",
                roles.get("ROLE_ADMIN")));
        result.put("manager", getOrCreateUser(
                "manager",
                "Store Manager",
                "manager@coffeshop.local",
                "0900000002",
                roles.get("ROLE_MANAGER")));
        result.put("staff", getOrCreateUser(
                "staff",
                "Store Staff",
                "staff@coffeshop.local",
                "0900000003",
                roles.get("ROLE_STAFF")));
        return result;
    }

    private Map<String, Category> seedCategories() {
        Map<String, Category> result = new HashMap<>();
        result.put("Coffee", getOrCreateCategory("Coffee", 1));
        result.put("Tea", getOrCreateCategory("Tea", 2));
        result.put("Bakery", getOrCreateCategory("Bakery", 3));
        return result;
    }

    private Map<String, MenuItem> seedMenuItems(Map<String, Category> categories) {
        Map<String, MenuItem> result = new HashMap<>();
        result.put("Americano", getOrCreateMenuItem(
                "Americano",
                categories.get("Coffee"),
                new BigDecimal("39000"),
                "Espresso pha loang, đậm vị cà phê.",
                "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=1200&q=80"));
        result.put("Latte", getOrCreateMenuItem(
                "Latte",
                categories.get("Coffee"),
                new BigDecimal("49000"),
                "Espresso cùng sữa tươi, vị cân bằng.",
                "https://images.unsplash.com/photo-1570968915860-54d5c301fa9f?auto=format&fit=crop&w=1200&q=80"));
        result.put("Peach Tea", getOrCreateMenuItem(
                "Peach Tea",
                categories.get("Tea"),
                new BigDecimal("45000"),
                "Trà đào thanh mát, dễ uống.",
                "https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=1200&q=80"));
        result.put("Black Tea", getOrCreateMenuItem(
                "Black Tea",
                categories.get("Tea"),
                new BigDecimal("35000"),
                "Trà đen truyền thống, hậu vị rõ.",
                "https://images.unsplash.com/photo-1594631661960-6f6b6b7df6d8?auto=format&fit=crop&w=1200&q=80"));
        result.put("Croissant", getOrCreateMenuItem(
                "Croissant",
                categories.get("Bakery"),
                new BigDecimal("32000"),
                "Bánh sừng bò bơ thơm, vỏ giòn xốp.",
                "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=1200&q=80"));
        return result;
    }

    private Map<String, TableEntity> seedTables() {
        Map<String, TableEntity> result = new HashMap<>();
        result.put("B1", getOrCreateTable("B1", 2, TableStatus.empty));
        result.put("B2", getOrCreateTable("B2", 4, TableStatus.occupied));
        result.put("B3", getOrCreateTable("B3", 4, TableStatus.empty));
        result.put("B4", getOrCreateTable("B4", 6, TableStatus.reserved));
        result.put("B5", getOrCreateTable("B5", 2, TableStatus.empty));
        return result;
    }

    private void seedIngredients() {
        getOrCreateIngredient("Coffee Bean", "kg", new BigDecimal("12.50"), new BigDecimal("3.00"));
        getOrCreateIngredient("Milk", "liter", new BigDecimal("25.00"), new BigDecimal("8.00"));
        getOrCreateIngredient("Peach Syrup", "bottle", new BigDecimal("6.00"), new BigDecimal("2.00"));
        getOrCreateIngredient("Black Tea Leaf", "kg", new BigDecimal("4.00"), new BigDecimal("1.50"));
        getOrCreateIngredient("Butter", "kg", new BigDecimal("3.50"), new BigDecimal("1.00"));
    }

    private Map<String, Employee> seedEmployees() {
        Map<String, Employee> result = new HashMap<>();
        if (employeeRepository.count() > 0) {
            return result;
        }
        result.put("0901111111", getOrCreateEmployee("Le An", "0901111111", "Barista", "MORNING", new BigDecimal("28000")));
        result.put("0901111112", getOrCreateEmployee("Pham Hai", "0901111112", "Cashier", "EVENING", new BigDecimal("30000")));
        result.put("0901111113", getOrCreateEmployee("Tran Vy", "0901111113", "Server", "FULL", new BigDecimal("26000")));
        return result;
    }

    private Map<String, Customer> seedCustomers() {
        Map<String, Customer> result = new HashMap<>();
        result.put("0902222221", getOrCreateCustomer("Nguyen Ha", "0902222221", 320));
        result.put("0902222222", getOrCreateCustomer("Do Minh", "0902222222", 180));
        return result;
    }

    private void seedAttendance(Map<String, Employee> employees, Map<String, User> users) {
        if (employees.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        User staffUser = users.get("staff");
        for (Employee employee : employees.values()) {
            Optional<Attendance> existing = attendanceRepository.findByEmployeeIdAndWorkDate(employee.getId(), today);
            if (existing.isPresent()) {
                continue;
            }
            LocalDateTime checkIn = LocalDateTime.of(today.getYear(), today.getMonth(), today.getDayOfMonth(), 8, 0);
            LocalDateTime checkOut = LocalDateTime.of(today.getYear(), today.getMonth(), today.getDayOfMonth(), 16, 30);

            jdbcTemplate.update(
                    "INSERT INTO attendance (employee_id, user_id, work_date, check_in, check_out, note) "
                            + "VALUES (?, ?, ?, ?, ?, ?) "
                            + "ON DUPLICATE KEY UPDATE note = VALUES(note)",
                    employee.getId(),
                    staffUser.getId(),
                    java.sql.Date.valueOf(today),
                    java.sql.Timestamp.valueOf(checkIn),
                    java.sql.Timestamp.valueOf(checkOut),
                    "Seeded sample attendance");
        }
    }

    private void seedOrders(
            Map<String, User> users,
            Map<String, Customer> customers,
            Map<String, TableEntity> tables,
            Map<String, MenuItem> menuItems) {
        if (shopOrderRepository.count() > 0) {
            return;
        }

        User staff = users.get("staff");
        Customer customer = customers.get("0902222221");
        TableEntity tableB2 = tables.get("B2");

        ShopOrder order = new ShopOrder();
        order.setUser(staff);
        order.setCustomer(customer);
        order.setTable(tableB2);
        order.setStatus(OrderStatus.serving);
        order.setDiscount(BigDecimal.ZERO);
        order.setSubtotal(BigDecimal.ZERO);
        order.setTotal(BigDecimal.ZERO);
        ShopOrder savedOrder = shopOrderRepository.save(order);

        addOrderItem(savedOrder, menuItems.get("Americano"), 2);
        addOrderItem(savedOrder, menuItems.get("Croissant"), 1);
    }

    private void addOrderItem(ShopOrder order, MenuItem menuItem, int quantity) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setMenuItem(menuItem);
        item.setQuantity(quantity);
        item.setUnitPrice(menuItem.getPrice());
        item.setNote("Seeded item");
        orderItemRepository.save(item);

        BigDecimal lineTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(quantity));
        BigDecimal subtotal = order.getSubtotal() == null ? BigDecimal.ZERO : order.getSubtotal();
        order.setSubtotal(subtotal.add(lineTotal));
        order.setTotal(order.getSubtotal().subtract(order.getDiscount() == null ? BigDecimal.ZERO : order.getDiscount()));
        shopOrderRepository.save(order);
    }

    private Role getOrCreateRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(roleName);
                    return roleRepository.save(role);
                });
    }

    private User getOrCreateUser(String username, String fullName, String email, String phone, Role primaryRole) {
        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            User user = existing.get();
            ensurePrimaryRole(user, primaryRole);
            return userRepository.save(user);
        }

        User user = new User();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        String encoded = passwordEncoder.encode("password");
        user.setPassword(encoded);
        user.setLegacyPassword(encoded);
        user.setActive(true);
        user.setRoleId(primaryRole.getId());
        Set<Role> roles = new HashSet<>();
        roles.add(primaryRole);
        user.setRoles(roles);
        return userRepository.save(user);
    }

    private void ensurePrimaryRole(User user, Role primaryRole) {
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        }
        user.getRoles().add(primaryRole);
        user.setRoleId(primaryRole.getId());
    }

    private Category getOrCreateCategory(String name, int sortOrder) {
        List<Category> categories = categoryRepository.findAll();
        for (Category category : categories) {
            if (name.equalsIgnoreCase(category.getName())) {
                return category;
            }
        }
        Category category = new Category();
        category.setName(name);
        category.setSortOrder(sortOrder);
        category.setIsActive(true);
        return categoryRepository.save(category);
    }

    private MenuItem getOrCreateMenuItem(
            String name,
            Category category,
            BigDecimal price,
            String description,
            String imageUrl) {
        List<MenuItem> items = menuItemRepository.findAll();
        for (MenuItem item : items) {
            if (name.equalsIgnoreCase(item.getName())) {
                boolean changed = false;
                if (item.getCategory() == null || !item.getCategory().getId().equals(category.getId())) {
                    item.setCategory(category);
                    changed = true;
                }
                if (item.getPrice() == null || item.getPrice().compareTo(price) != 0) {
                    item.setPrice(price);
                    changed = true;
                }
                if (item.getDescription() == null || item.getDescription().isBlank()) {
                    item.setDescription(description);
                    changed = true;
                }
                if (item.getImageUrl() == null || item.getImageUrl().isBlank()) {
                    item.setImageUrl(imageUrl);
                    changed = true;
                }
                if (item.getIsAvailable() == null) {
                    item.setIsAvailable(true);
                    changed = true;
                }
                return changed ? menuItemRepository.save(item) : item;
            }
        }
        MenuItem item = new MenuItem();
        item.setName(name);
        item.setCategory(category);
        item.setDescription(description);
        item.setPrice(price);
        item.setImageUrl(imageUrl);
        item.setIsAvailable(true);
        return menuItemRepository.save(item);
    }

    private TableEntity getOrCreateTable(String name, int capacity, TableStatus status) {
        List<TableEntity> tables = tableEntityRepository.findAll();
        for (TableEntity table : tables) {
            if (name.equalsIgnoreCase(table.getName())) {
                return table;
            }
        }
        TableEntity table = new TableEntity();
        table.setName(name);
        table.setCapacity(capacity);
        table.setStatus(status);
        table.setNote("Seeded table");
        return tableEntityRepository.save(table);
    }

    private Ingredient getOrCreateIngredient(String name, String unit, BigDecimal stock, BigDecimal minThreshold) {
        List<Ingredient> ingredients = ingredientRepository.findAll();
        for (Ingredient ingredient : ingredients) {
            if (name.equalsIgnoreCase(ingredient.getName())) {
                return ingredient;
            }
        }
        Ingredient ingredient = new Ingredient();
        ingredient.setName(name);
        ingredient.setUnit(unit);
        ingredient.setStockQuantity(stock);
        ingredient.setMinThreshold(minThreshold);
        return ingredientRepository.save(ingredient);
    }

    private Employee getOrCreateEmployee(String fullName, String phone, String position, String shiftCode, BigDecimal hourlyRate) {
        return employeeRepository.findByPhone(phone)
                .orElseGet(() -> {
                    Employee employee = new Employee();
                    employee.setFullName(fullName);
                    employee.setPhone(phone);
                    employee.setPosition(position);
                    employee.setShiftCode(shiftCode);
                    employee.setHourlyRate(hourlyRate);
                    employee.setIsActive(true);
                    return employeeRepository.save(employee);
                });
    }

    private Customer getOrCreateCustomer(String fullName, String phone, int points) {
        return customerRepository.findByPhone(phone)
                .orElseGet(() -> {
                    Customer customer = new Customer();
                    customer.setFullName(fullName);
                    customer.setPhone(phone);
                    customer.setPoints(points);
                    return customerRepository.save(customer);
                });
    }
}
