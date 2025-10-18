package com.example.Bookstore.controller;

import com.example.Bookstore.dto.*;
import com.example.Bookstore.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private BookService bookService;
    
    @Autowired
    private CustomerService customerService;
    
    @Autowired
    private EmployeeService employeeService;
    
    @Autowired
    private SupplierService supplierService;
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private AuthorService authorService;
    
    @Autowired
    private PublisherService publisherService;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private PromotionService promotionService;

    @GetMapping({"", "/", "/dashboard"})
    // @PreAuthorize("hasRole('ADMIN')")  // Disabled temporarily
    public String dashboard(Model model, HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(userRole)) {
            return "redirect:/admin/access-denied";
        }
        model.addAttribute("activePage", "dashboard");
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime firstDayOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime lastDayOfMonth = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);
            
            Double monthlyRevenue = reportService.calculateTotalRevenue(firstDayOfMonth, lastDayOfMonth);
            Long monthlyOrders = reportService.countTotalOrders(firstDayOfMonth, lastDayOfMonth);
            
            Page<CustomerDTO> customerPage = customerService.getAllCustomers(null, PageRequest.of(0, 1));
            Long totalCustomers = customerPage.getTotalElements();
            
            List<BookDTO> lowStockBooks = bookService.getLowStockBooks(10);
            
            model.addAttribute("monthlyRevenue", monthlyRevenue != null ? monthlyRevenue : 0.0);
            model.addAttribute("monthlyOrders", monthlyOrders != null ? monthlyOrders : 0L);
            model.addAttribute("totalCustomers", totalCustomers);
            model.addAttribute("lowStockCount", lowStockBooks != null ? lowStockBooks.size() : 0);
            model.addAttribute("lowStockBooks", lowStockBooks != null && !lowStockBooks.isEmpty() ? 
                lowStockBooks.subList(0, Math.min(5, lowStockBooks.size())) : List.of());
            
            try {
                ReportDTO.SupplierDebtReport debtReport = reportService.generateSupplierDebtReport();
                model.addAttribute("debtSuppliers", 
                    debtReport != null && debtReport.getSuppliers() != null && !debtReport.getSuppliers().isEmpty() ? 
                    debtReport.getSuppliers().subList(0, Math.min(5, debtReport.getSuppliers().size())) : 
                    List.of());
            } catch (Exception e) {
                model.addAttribute("debtSuppliers", List.of());
            }
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi tải dữ liệu dashboard: " + e.getMessage());
            model.addAttribute("monthlyRevenue", 0.0);
            model.addAttribute("monthlyOrders", 0L);
            model.addAttribute("totalCustomers", 0L);
            model.addAttribute("lowStockCount", 0);
            model.addAttribute("lowStockBooks", List.of());
            model.addAttribute("debtSuppliers", List.of());
        }
        
        return "admin/dashboard";
    }

    @GetMapping("/books")
    // @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")  // Disabled temporarily
    public String listBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Model model, HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(userRole) && !"WAREHOUSE".equals(userRole)) {
            return "redirect:/admin/access-denied";
        }
        
        model.addAttribute("activePage", "books");
        Pageable pageable = PageRequest.of(page, size, Sort.by("title").ascending());
        Page<BookDTO> books;
        if (search != null && !search.trim().isEmpty()) {
            books = bookService.searchBooks(search, null, null, null, null, null, pageable);
        } else {
            books = bookService.getAllBooks(pageable);
        }
        
        model.addAttribute("books", books);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        
        return "admin/books";
    }

    @GetMapping("/books/new")
    // @PreAuthorize("hasAnyRole('ADMIN', 'WAREHOUSE')")  // Disabled temporarily
    public String newBookForm(Model model, HttpSession session) {
        String userRole = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(userRole) && !"WAREHOUSE".equals(userRole)) {
            return "redirect:/admin/access-denied";
        }
        model.addAttribute("activePage", "books");
        model.addAttribute("book", new BookDTO());
        model.addAttribute("isEdit", false);
        model.addAttribute("categories", categoryService.getActiveCategories());
        model.addAttribute("authors", authorService.getActiveAuthors());
        model.addAttribute("publishers", publisherService.getActivePublishers());
        return "admin/book-form";
    }

    @GetMapping("/books/edit/{id}")
    public String editBookForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("activePage", "books");
        try {
            BookDTO book = bookService.getBookById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));
            model.addAttribute("book", book);
            model.addAttribute("isEdit", true);
            model.addAttribute("categories", categoryService.getActiveCategories());
            model.addAttribute("authors", authorService.getActiveAuthors());
            model.addAttribute("publishers", publisherService.getActivePublishers());
            return "admin/book-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy sách!");
            return "redirect:/admin/books";
        }
    }

    @PostMapping("/books/save")
    public String saveBook(@ModelAttribute("book") BookDTO bookDTO, 
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/book-form";
        }
        
        try {
            if (bookDTO.getBookId() != null && !bookDTO.getBookId().isEmpty()) {
                bookService.updateBook(bookDTO.getBookId(), bookDTO);
                redirectAttributes.addFlashAttribute("success", "Cập nhật sách thành công!");
            } else {
                bookService.createBook(bookDTO);
                redirectAttributes.addFlashAttribute("success", "Thêm sách mới thành công!");
            }
            return "redirect:/admin/books";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/books";
        }
    }

    @GetMapping("/books/delete/{id}")
    public String deleteBook(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            bookService.deleteBook(id);
            redirectAttributes.addFlashAttribute("success", "Xóa sách thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xóa sách: " + e.getMessage());
        }
        return "redirect:/admin/books";
    }

    @GetMapping("/customers")
    public String listCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Model model) {
        
        model.addAttribute("activePage", "customers");
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<CustomerDTO> customers = customerService.getAllCustomers(search, pageable);
        
        model.addAttribute("customers", customers);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        
        return "admin/customers";
    }

    @GetMapping("/customers/new")
    public String newCustomerForm(Model model) {
        model.addAttribute("activePage", "customers");
        model.addAttribute("customer", new CustomerDTO());
        model.addAttribute("isEdit", false);
        return "admin/customer-form";
    }

    @GetMapping("/customers/edit/{id}")
    public String editCustomerForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("activePage", "customers");
        try {
            CustomerDTO customer = customerService.getCustomerById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));
            model.addAttribute("customer", customer);
            model.addAttribute("isEdit", true);
            return "admin/customer-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy khách hàng!");
            return "redirect:/admin/customers";
        }
    }

    @PostMapping("/customers/save")
    public String saveCustomer(@ModelAttribute("customer") CustomerDTO customerDTO,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/customer-form";
        }
        
        try {
            if (customerDTO.getCustomerId() != null && !customerDTO.getCustomerId().isEmpty()) {
                customerService.updateCustomer(customerDTO.getCustomerId(), customerDTO);
                redirectAttributes.addFlashAttribute("success", "Cập nhật khách hàng thành công!");
            } else {
                customerDTO.setStatus(0);
                CustomerDTO created = customerService.createCustomer(customerDTO);
                if (created.getEmail() != null && !created.getEmail().isEmpty()) {
                    authService.sendOTP(created.getEmail());
                    redirectAttributes.addFlashAttribute("success", "Thêm khách hàng mới thành công! Mã OTP đã được gửi qua email.");
                } else {
                    redirectAttributes.addFlashAttribute("success", "Thêm khách hàng mới thành công!");
                }
            }
            return "redirect:/admin/customers";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/customers";
        }
    }

    @GetMapping("/customers/delete/{id}")
    public String deleteCustomer(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            customerService.deleteCustomer(id);
            redirectAttributes.addFlashAttribute("success", "Xóa khách hàng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xóa khách hàng: " + e.getMessage());
        }
        return "redirect:/admin/customers";
    }

    @GetMapping("/employees")
    public String listEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            Model model) {
        
        model.addAttribute("activePage", "employees");
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        
        Page<EmployeeDTO> employees;
        if (role != null && !role.trim().isEmpty()) {
            employees = employeeService.getAllEmployees(search, role, pageable);
        } else {
            employees = employeeService.getAllEmployees(search, pageable);
        }
        
        model.addAttribute("employees", employees);
        model.addAttribute("search", search);
        model.addAttribute("role", role);
        model.addAttribute("currentPage", page);
        
        // Thêm danh sách roles để hiển thị trong filter
        model.addAttribute("roles", List.of("ADMIN", "CASHIER", "WAREHOUSE", "SHIPPER"));
        
        return "admin/employees";
    }

    @GetMapping("/employees/new")
    public String newEmployeeForm(Model model) {
        model.addAttribute("activePage", "employees");
        model.addAttribute("employee", new EmployeeDTO());
        model.addAttribute("isEdit", false);
        return "admin/employee-form";
    }

    @GetMapping("/employees/edit/{id}")
    public String editEmployeeForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("activePage", "employees");
        try {
            EmployeeDTO employee = employeeService.getEmployeeById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
            model.addAttribute("employee", employee);
            model.addAttribute("isEdit", true);
            return "admin/employee-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy nhân viên!");
            return "redirect:/admin/employees";
        }
    }

    @PostMapping("/employees/save")
    public String saveEmployee(@ModelAttribute("employee") EmployeeDTO employeeDTO,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/employee-form";
        }
        
        try {
            if (employeeDTO.getEmployeeId() != null && !employeeDTO.getEmployeeId().isEmpty()) {
                employeeService.updateEmployee(employeeDTO.getEmployeeId(), employeeDTO);
                redirectAttributes.addFlashAttribute("success", "Cập nhật nhân viên thành công!");
            } else {
                employeeDTO.setStatus(0);
                EmployeeDTO created = employeeService.createEmployee(employeeDTO);
                if (created.getEmail() != null && !created.getEmail().isEmpty()) {
                    authService.sendOTP(created.getEmail());
                    redirectAttributes.addFlashAttribute("success", "Thêm nhân viên mới thành công! Mã OTP đã được gửi qua email.");
                } else {
                    redirectAttributes.addFlashAttribute("success", "Thêm nhân viên mới thành công!");
                }
            }
            return "redirect:/admin/employees";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/employees";
        }
    }

    @GetMapping("/employees/delete/{id}")
    public String deleteEmployee(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            employeeService.deleteEmployee(id);
            redirectAttributes.addFlashAttribute("success", "Xóa nhân viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xóa nhân viên: " + e.getMessage());
        }
        return "redirect:/admin/employees";
    }

    @GetMapping("/suppliers")
    public String listSuppliers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Model model) {
        
        model.addAttribute("activePage", "suppliers");
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<SupplierDTO> suppliers = supplierService.getAllSuppliers(search, pageable);
        
        model.addAttribute("suppliers", suppliers);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        
        return "admin/suppliers";
    }

    @GetMapping("/suppliers/new")
    public String newSupplierForm(Model model) {
        model.addAttribute("activePage", "suppliers");
        model.addAttribute("supplier", new SupplierDTO());
        model.addAttribute("isEdit", false);
        return "admin/supplier-form";
    }

    @GetMapping("/suppliers/edit/{id}")
    public String editSupplierForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("activePage", "suppliers");
        try {
            SupplierDTO supplier = supplierService.getSupplierById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà cung cấp"));
            model.addAttribute("supplier", supplier);
            model.addAttribute("isEdit", true);
            return "admin/supplier-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy nhà cung cấp!");
            return "redirect:/admin/suppliers";
        }
    }

    @PostMapping("/suppliers/save")
    public String saveSupplier(@ModelAttribute("supplier") SupplierDTO supplierDTO,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/supplier-form";
        }
        
        try {
            if (supplierDTO.getSupplierId() != null && !supplierDTO.getSupplierId().isEmpty()) {
                supplierService.updateSupplier(supplierDTO.getSupplierId(), supplierDTO);
                redirectAttributes.addFlashAttribute("success", "Cập nhật nhà cung cấp thành công!");
            } else {
                supplierService.createSupplier(supplierDTO);
                redirectAttributes.addFlashAttribute("success", "Thêm nhà cung cấp mới thành công!");
            }
            return "redirect:/admin/suppliers";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/suppliers";
        }
    }

    @GetMapping("/suppliers/delete/{id}")
    public String deleteSupplier(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            supplierService.deleteSupplier(id);
            redirectAttributes.addFlashAttribute("success", "Xóa nhà cung cấp thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xóa nhà cung cấp: " + e.getMessage());
        }
        return "redirect:/admin/suppliers";
    }

    @GetMapping("/reports")
    public String reports(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Model model) {
        
        model.addAttribute("activePage", "reports");
        try {
            LocalDateTime from;
            LocalDateTime to;
            
            if (fromDate != null && toDate != null) {
                from = LocalDateTime.parse(fromDate + "T00:00:00");
                to = LocalDateTime.parse(toDate + "T23:59:59");
            } else {
                LocalDateTime now = LocalDateTime.now();
                from = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                to = now.withHour(23).withMinute(59).withSecond(59);
            }
            
            Double totalRevenue = reportService.calculateTotalRevenue(from, to);
            Long totalOrders = reportService.countTotalOrders(from, to);
            Double avgOrderValue = reportService.calculateAverageOrderValue(from, to);
            
            ReportDTO.SalesReport salesReport = reportService.generateSalesReport(from, to, "day");
            ReportDTO.InventoryReport inventoryReport = reportService.generateInventoryReport();
            ReportDTO.SupplierDebtReport debtReport = reportService.generateSupplierDebtReport();
            
            model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("avgOrderValue", avgOrderValue != null ? avgOrderValue : 0.0);
            model.addAttribute("salesReport", salesReport);
            model.addAttribute("inventoryReport", inventoryReport);
            model.addAttribute("debtReport", debtReport);
            model.addAttribute("fromDate", fromDate);
            model.addAttribute("toDate", toDate);
            
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi tải báo cáo: " + e.getMessage());
        }
        
        return "admin/reports";
    }
    
    @GetMapping("/test-reports")
    public String testReports() {
        return "admin/test-reports";
    }
    

    @GetMapping("/categories")
    public String listCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Model model) {
        
        model.addAttribute("activePage", "categories");
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<CategoryDTO> categories = categoryService.getAllCategories(search, pageable);
        
        model.addAttribute("categories", categories);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        
        return "admin/categories";
    }

    @GetMapping("/categories/new")
    public String newCategoryForm(Model model) {
        model.addAttribute("activePage", "categories");
        model.addAttribute("category", new CategoryDTO());
        model.addAttribute("isEdit", false);
        return "admin/category-form";
    }

    @GetMapping("/categories/edit/{id}")
    public String editCategoryForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("activePage", "categories");
        try {
            CategoryDTO category = categoryService.getCategoryById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thể loại"));
            model.addAttribute("category", category);
            model.addAttribute("isEdit", true);
            return "admin/category-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thể loại!");
            return "redirect:/admin/categories";
        }
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute("category") CategoryDTO categoryDTO,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/category-form";
        }
        
        try {
            if (categoryDTO.getCategoryId() != null && !categoryDTO.getCategoryId().isEmpty()) {
                categoryService.updateCategory(categoryDTO.getCategoryId(), categoryDTO);
                redirectAttributes.addFlashAttribute("success", "Cập nhật thể loại thành công!");
            } else {
                categoryService.createCategory(categoryDTO);
                redirectAttributes.addFlashAttribute("success", "Thêm thể loại mới thành công!");
            }
            return "redirect:/admin/categories";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/categories";
        }
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Xóa thể loại thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xóa thể loại: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/authors")
    public String listAuthors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Model model) {
        
        model.addAttribute("activePage", "authors");
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<AuthorDTO> authors = authorService.getAllAuthors(search, pageable);
        
        model.addAttribute("authors", authors);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        
        return "admin/authors";
    }

    @GetMapping("/authors/new")
    public String newAuthorForm(Model model) {
        model.addAttribute("activePage", "authors");
        model.addAttribute("author", new AuthorDTO());
        model.addAttribute("isEdit", false);
        return "admin/author-form";
    }

    @GetMapping("/authors/edit/{id}")
    public String editAuthorForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("activePage", "authors");
        try {
            AuthorDTO author = authorService.getAuthorById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tác giả"));
            model.addAttribute("author", author);
            model.addAttribute("isEdit", true);
            return "admin/author-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy tác giả!");
            return "redirect:/admin/authors";
        }
    }

    @PostMapping("/authors/save")
    public String saveAuthor(@ModelAttribute("author") AuthorDTO authorDTO,
                            BindingResult result,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/author-form";
        }
        
        try {
            if (authorDTO.getAuthorId() != null && !authorDTO.getAuthorId().isEmpty()) {
                authorService.updateAuthor(authorDTO.getAuthorId(), authorDTO);
                redirectAttributes.addFlashAttribute("success", "Cập nhật tác giả thành công!");
            } else {
                authorService.createAuthor(authorDTO);
                redirectAttributes.addFlashAttribute("success", "Thêm tác giả mới thành công!");
            }
            return "redirect:/admin/authors";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/authors";
        }
    }

    @GetMapping("/authors/delete/{id}")
    public String deleteAuthor(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            authorService.deleteAuthor(id);
            redirectAttributes.addFlashAttribute("success", "Xóa tác giả thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xóa tác giả: " + e.getMessage());
        }
        return "redirect:/admin/authors";
    }

    @GetMapping("/publishers")
    public String listPublishers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Model model) {
        
        model.addAttribute("activePage", "publishers");
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<PublisherDTO> publishers = publisherService.getAllPublishers(search, pageable);
        
        model.addAttribute("publishers", publishers);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        
        return "admin/publishers";
    }

    @GetMapping("/publishers/new")
    public String newPublisherForm(Model model) {
        model.addAttribute("activePage", "publishers");
        model.addAttribute("publisher", new PublisherDTO());
        model.addAttribute("isEdit", false);
        return "admin/publisher-form";
    }

    @GetMapping("/publishers/edit/{id}")
    public String editPublisherForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("activePage", "publishers");
        try {
            PublisherDTO publisher = publisherService.getPublisherById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà xuất bản"));
            model.addAttribute("publisher", publisher);
            model.addAttribute("isEdit", true);
            return "admin/publisher-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy nhà xuất bản!");
            return "redirect:/admin/publishers";
        }
    }

    @PostMapping("/publishers/save")
    public String savePublisher(@ModelAttribute("publisher") PublisherDTO publisherDTO,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/publisher-form";
        }
        
        try {
            if (publisherDTO.getPublisherId() != null && !publisherDTO.getPublisherId().isEmpty()) {
                publisherService.updatePublisher(publisherDTO.getPublisherId(), publisherDTO);
                redirectAttributes.addFlashAttribute("success", "Cập nhật nhà xuất bản thành công!");
            } else {
                publisherService.createPublisher(publisherDTO);
                redirectAttributes.addFlashAttribute("success", "Thêm nhà xuất bản mới thành công!");
            }
            return "redirect:/admin/publishers";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/publishers";
        }
    }

    @GetMapping("/publishers/delete/{id}")
    public String deletePublisher(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            publisherService.deletePublisher(id);
            redirectAttributes.addFlashAttribute("success", "Xóa nhà xuất bản thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xóa nhà xuất bản: " + e.getMessage());
        }
        return "redirect:/admin/publishers";
    }

    @GetMapping("/promotions")
    public String listPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            Model model) {
        
        model.addAttribute("activePage", "promotions");
        Pageable pageable = PageRequest.of(page, size, Sort.by("expireDate").descending());
        Page<PromotionDTO> promotions = promotionService.getAllPromotions(search, pageable);
        
        model.addAttribute("promotions", promotions);
        model.addAttribute("search", search);
        model.addAttribute("currentPage", page);
        
        return "admin/promotions";
    }

    @GetMapping("/promotions/new")
    public String newPromotionForm(Model model) {
        model.addAttribute("activePage", "promotions");
        model.addAttribute("promotion", new PromotionDTO());
        model.addAttribute("isEdit", false);
        return "admin/promotion-form";
    }

    @GetMapping("/promotions/edit/{id}")
    public String editPromotionForm(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("activePage", "promotions");
        try {
            PromotionDTO promotion = promotionService.getPromotionById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã giảm giá"));
            model.addAttribute("promotion", promotion);
            model.addAttribute("isEdit", true);
            return "admin/promotion-form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy mã giảm giá!");
            return "redirect:/admin/promotions";
        }
    }

    @PostMapping("/promotions/save")
    public String savePromotion(@ModelAttribute("promotion") PromotionDTO promotionDTO,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/promotion-form";
        }
        
        try {
            if (promotionDTO.getPromoId() != null && !promotionDTO.getPromoId().isEmpty()) {
                promotionService.updatePromotion(promotionDTO.getPromoId(), promotionDTO);
                redirectAttributes.addFlashAttribute("success", "Cập nhật mã giảm giá thành công!");
            } else {
                promotionService.createPromotion(promotionDTO);
                redirectAttributes.addFlashAttribute("success", "Thêm mã giảm giá mới thành công!");
            }
            return "redirect:/admin/promotions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/promotions";
        }
    }

    @GetMapping("/promotions/delete/{id}")
    public String deletePromotion(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            promotionService.deletePromotion(id);
            redirectAttributes.addFlashAttribute("success", "Xóa mã giảm giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi xóa mã giảm giá: " + e.getMessage());
        }
        return "redirect:/admin/promotions";
    }
}

