package com.example.Bookstore.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.Bookstore.dto.warehouse.CreateReceiptRequest;
import com.example.Bookstore.entity.Author;
import com.example.Bookstore.entity.Book;
import com.example.Bookstore.entity.Category;
import com.example.Bookstore.entity.Publisher;
import com.example.Bookstore.repository.AuthorRepository;
import com.example.Bookstore.repository.BookRepository;
import com.example.Bookstore.repository.CategoryRepository;
import com.example.Bookstore.repository.EmployeeRepository;
import com.example.Bookstore.repository.PublisherRepository;
import com.example.Bookstore.service.InventoryService;
import com.example.Bookstore.service.SupplierService;

import lombok.RequiredArgsConstructor;
	
	@Controller
	@RequestMapping("/warehouse")
	@RequiredArgsConstructor
	public class WarehousePageController {
	
  private final InventoryService inventoryService;
  private final SupplierService supplierService;
  private final CategoryRepository categoryRepository;
  private final BookRepository bookRepository;
  private final AuthorRepository authorRepository;
  private final PublisherRepository publisherRepository;
  private final EmployeeRepository employeeRepository;
	  	
  @GetMapping("/stock")
  public String stock(@RequestParam(required=false) String keyword,
                      @RequestParam(required=false) String category,
                      @RequestParam(defaultValue="0") int page,
                      @RequestParam(defaultValue="10") int size,
                      Model model) {
	
    var pageable = PageRequest.of(page, size);
	    var vm = inventoryService.listStockView(keyword, category, pageable);
	
	    model.addAttribute("data", vm.content());
	    model.addAttribute("page", page);
	    model.addAttribute("last", vm.last());
    model.addAttribute("size", size);
	
	    model.addAttribute("totalBooks", vm.totalBooks());
	    model.addAttribute("totalQuantity", vm.totalQuantity());
	    model.addAttribute("lowStockCount", vm.lowStockCount());
	    model.addAttribute("inventoryValueFmt", vm.inventoryValueFmt());
	    
	    // Badge so sánh với tháng trước (giá trị mô phỏng cho đẹp)
	    model.addAttribute("deltaBooksText", "+12% so với tháng trước");
	    model.addAttribute("deltaQtyText", "+8% so với tháng trước");
	
	    // <-- dùng repo trực tiếp
	    model.addAttribute("categories", categoryRepository.findAll());
    model.addAttribute("authors", authorRepository.findAll());
    model.addAttribute("publishers", publisherRepository.findAll());
	    model.addAttribute("category", category);
	    model.addAttribute("keyword", keyword);
	    
	    // TODO: Sau này check role thật từ Spring Security
	    // Tạm thời hardcode: true = admin có quyền sửa, false = nhân viên chỉ xem
	    boolean isAdmin = true; // Đổi thành false để test quyền nhân viên
	    model.addAttribute("isAdmin", isAdmin);
	
	    model.addAttribute("pageTitle","Quản lý tồn kho");
	    model.addAttribute("active","stock");
	    return "warehouse/stock";
	  }

  @PostMapping("/stock/books")
  public String createBook(@RequestParam String title,
                           @RequestParam String categoryId,
                           @RequestParam String authorId,
                           @RequestParam String publisherId,
                           @RequestParam(required=false) Double importPrice,
                           @RequestParam(required=false) Double salePrice) {
    Category c = categoryRepository.findById(categoryId)
      .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    Author a = authorRepository.findById(authorId)
      .orElseThrow(() -> new IllegalArgumentException("Author not found"));
    Publisher p = publisherRepository.findById(publisherId)
      .orElseThrow(() -> new IllegalArgumentException("Publisher not found"));

    Book b = new Book();
    b.setTitle(title);
    b.setCategory(c);
    b.setAuthor(a);
    b.setPublisher(p);
    b.setImportPrice(importPrice);
    b.setSalePrice(salePrice);
    b.setQuantity(0);
    b.setStatus(1);
    bookRepository.save(b);

    return "redirect:/warehouse/stock";
  }

  

  @PostMapping("/stock/categories")
  public String createCategory(@RequestParam String name) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("Category name required");
    Category c = new Category();
    c.setName(name);
    c.setStatus(1);
    categoryRepository.save(c);
    return "redirect:/warehouse/stock";
  }

  @PostMapping("/stock/authors")
  public String createAuthor(@RequestParam String name,
                             @RequestParam(required=false) String description) {
    if (name == null || name.isBlank()) throw new IllegalArgumentException("Author name required");
    Author a = new Author();
    a.setName(name);
    a.setDescription(description);
    a.setStatus(1);
    authorRepository.save(a);
    return "redirect:/warehouse/stock";
  }

  @GetMapping("/stock/view/{id}")
  public String viewBook(@PathVariable String id, Model model) {
    Book book = bookRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách với ID: " + id));
    
    model.addAttribute("book", book);
    
    // TODO: Sau này check role thật từ Spring Security
    boolean isAdmin = true; // Đổi thành false để test quyền nhân viên
    model.addAttribute("isAdmin", isAdmin);
    
    model.addAttribute("pageTitle", "Chi tiết sách");
    model.addAttribute("active", "stock");
    return "warehouse/stock-view";
  }
  
  @PostMapping("/stock/update-quantity/{id}")
  public String updateQuantity(@PathVariable String id, 
                               @RequestParam Integer quantity) {
    // TODO: Check admin role từ Spring Security
    Book book = bookRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách với ID: " + id));
    
    if (quantity == null || quantity < 0) {
      throw new IllegalArgumentException("Số lượng không hợp lệ");
    }
    
    book.setQuantity(quantity);
    bookRepository.save(book);
    
    // Redirect về trang view để thấy kết quả ngay
    return "redirect:/warehouse/stock/view/" + id;
  }

  @GetMapping("/stock/edit/{id}")
  public String editBookForm(@PathVariable String id, Model model) {
    Book book = bookRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách với ID: " + id));
    
    model.addAttribute("book", book);
    model.addAttribute("categories", categoryRepository.findAll());
    model.addAttribute("authors", authorRepository.findAll());
    model.addAttribute("publishers", publisherRepository.findAll());
    model.addAttribute("pageTitle", "Sửa thông tin sách");
    model.addAttribute("active", "stock");
    return "warehouse/stock-edit";
  }

  @PostMapping("/stock/edit/{id}")
  public String updateBook(@PathVariable String id,
                          @RequestParam String title,
                          @RequestParam String categoryId,
                          @RequestParam String authorId,
                          @RequestParam String publisherId,
                          @RequestParam(required=false) Double importPrice,
                          @RequestParam(required=false) Double salePrice,
                          @RequestParam(required=false) Integer quantity) {
    Book book = bookRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách với ID: " + id));
    
    Category c = categoryRepository.findById(categoryId)
      .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    Author a = authorRepository.findById(authorId)
      .orElseThrow(() -> new IllegalArgumentException("Author not found"));
    Publisher p = publisherRepository.findById(publisherId)
      .orElseThrow(() -> new IllegalArgumentException("Publisher not found"));

    book.setTitle(title);
    book.setCategory(c);
    book.setAuthor(a);
    book.setPublisher(p);
    book.setImportPrice(importPrice);
    book.setSalePrice(salePrice);
    if (quantity != null) {
      book.setQuantity(quantity);
    }
    bookRepository.save(book);

    return "redirect:/warehouse/stock";
  }

  @PostMapping("/stock/delete/{id}")
  public String deleteBook(@PathVariable String id) {
    Book book = bookRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách với ID: " + id));
    
    // Soft delete: chỉ đổi status = 0
    book.setStatus(0);
    bookRepository.save(book);
    
    return "redirect:/warehouse/stock";
  }

  
	
	  @GetMapping("/stock/low")
	  public String low(Model model, @RequestParam(defaultValue="10") int threshold) {
	    var data = inventoryService.lowStock(threshold);
	    model.addAttribute("data", data);
	    return "warehouse/report";
	  }
	
  @GetMapping("/receipts")
  public String receipts(@RequestParam(required = false) Integer month,
                        @RequestParam(required = false) Integer year,
                        Model model) {
    // Mặc định hiển thị tháng hiện tại nếu không có tham số
    java.time.LocalDate now = java.time.LocalDate.now();
    int selectedMonth = (month != null) ? month : now.getMonthValue();
    int selectedYear = (year != null) ? year : now.getYear();
    
    // Tính từ ngày và đến ngày cho tháng được chọn
    java.time.LocalDateTime fromDate = java.time.LocalDateTime.of(selectedYear, selectedMonth, 1, 0, 0);
    java.time.LocalDateTime toDate = fromDate.plusMonths(1);
    
    var list = inventoryService.listReceipts(fromDate, toDate, PageRequest.of(0, 500));
    model.addAttribute("receipts", list.getContent());
    
    // Thêm dữ liệu cho dropdown
    model.addAttribute("books", bookRepository.findAll());
    model.addAttribute("suppliers", supplierService.findAll());
    
    // Thêm thông tin tháng/năm được chọn
    model.addAttribute("selectedMonth", selectedMonth);
    model.addAttribute("selectedYear", selectedYear);
    
    // Tạo danh sách tháng và năm cho dropdown
    java.util.List<Integer> months = java.util.stream.IntStream.rangeClosed(1, 12).boxed().toList();
    java.util.List<Integer> years = java.util.stream.IntStream.rangeClosed(2020, now.getYear()).boxed()
      .sorted(java.util.Comparator.reverseOrder()).toList();
    model.addAttribute("months", months);
    model.addAttribute("years", years);
    
    // Thống kê - ĐỒNG BỘ VỚI TỒN KHO
    // 1. Tổng số đầu sách (từ Book)
    long totalBooks = bookRepository.count();
    
    // 2. Tổng số lượng sách trong kho (từ Book.quantity)
    Long totalQuantity = bookRepository.sumAllQuantity();
    
    // 3. Số nhà cung cấp hoạt động (status = 1)
    long totalSuppliers = supplierService.findAll().stream()
      .filter(s -> s.getStatus() != null && s.getStatus() == 1)
      .count();
    
    // 4. Số sách sắp hết (quantity <= 10)
    long lowStockCount = bookRepository.findAll().stream()
      .filter(b -> b.getQuantity() != null && b.getQuantity() <= 10)
      .count();
    
    model.addAttribute("totalBooks", totalBooks);
    model.addAttribute("totalQuantity", totalQuantity != null ? totalQuantity : 0L);
    model.addAttribute("totalSuppliers", totalSuppliers);
    model.addAttribute("lowStockCount", lowStockCount);
    
    model.addAttribute("pageTitle", "Quản lý nhập sách");
    model.addAttribute("active", "receipts");
    return "warehouse/receipts";
  }
	
  @PostMapping("/receipts")
  public String createReceipt(@RequestParam String bookId,
                              @RequestParam String supplierId,
                              @RequestParam int quantity,
                              @RequestParam Double importPrice) {
    CreateReceiptRequest request = new CreateReceiptRequest(bookId, supplierId, quantity, importPrice);
    inventoryService.createReceipt(request);
    return "redirect:/warehouse/receipts";
  }  @GetMapping("/suppliers")
  public String suppliers(@RequestParam(value="q", required=false) String q,
                          Model model) {
	    var page = supplierService.list(q, org.springframework.data.domain.PageRequest.of(0, 200));
	
	    model.addAttribute("pageTitle", "Quản lý NCC");
	    model.addAttribute("active", "suppliers");
	
	    model.addAttribute("suppliers", page != null ? page.getContent() : java.util.List.of());
	    model.addAttribute("q", q);
	
	    // BẮT BUỘC cho th:object
	    model.addAttribute("supplierForm", new com.example.Bookstore.entity.Supplier());
	
	  return "warehouse/suppliers";
	}

	@PostMapping("/suppliers")
	public String createSupplier(@RequestParam String name,
	                             @RequestParam(required=false) String address,
	                             @RequestParam(required=false) String phone) {
	  supplierService.create(name, address, phone);
	  return "redirect:/warehouse/suppliers";
	}

	@PostMapping("/suppliers/{id}")
	public String updateSupplier(@PathVariable String id,
	                            @RequestParam String name,
	                            @RequestParam(required=false) String address,
	                            @RequestParam(required=false) String phone) {
	  supplierService.update(id, name, address, phone);
	  return "redirect:/warehouse/suppliers";
	}

	@GetMapping("/suppliers/{id}/edit")
	public String editSupplier(@PathVariable String id, Model model) {
	  var supplier = supplierService.findById(id);
	  model.addAttribute("supplierForm", supplier);
	  model.addAttribute("pageTitle", "Sửa nhà cung cấp");
	  model.addAttribute("active", "suppliers");
	  return "warehouse/suppliers";
	}

	@PostMapping("/suppliers/{id}/delete")
	public String deleteSupplier(@PathVariable String id) {
	  supplierService.delete(id);
	  return "redirect:/warehouse/suppliers";
	}
	
	// ===== QUẢN LÝ CÔNG NỢ =====
	
	@GetMapping("/debt-report")
	public String debtReport(Model model) {
	  var suppliersWithDebt = supplierService.findAll().stream()
	    .filter(s -> s.getDebt() != null && s.getDebt() > 0)
	    .toList();
	  
	  double totalDebt = suppliersWithDebt.stream()
	    .mapToDouble(s -> s.getDebt() != null ? s.getDebt() : 0.0)
	    .sum();
	  
	  long paymentCount = supplierService.getAllPaymentHistoryForExport().size();
	  
	  java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
	  
	  model.addAttribute("suppliersWithDebtList", suppliersWithDebt);
	  model.addAttribute("totalDebt", nf.format(totalDebt));
	  model.addAttribute("suppliersWithDebt", suppliersWithDebt.size());
	  model.addAttribute("paymentHistoryCount", paymentCount);
	  model.addAttribute("pageTitle", "Báo cáo công nợ");
	  model.addAttribute("active", "debt-report");
	  
	  return "warehouse/debt-report";
	}
	
	@GetMapping("/debt-report/pay")
	public String showPayForm(@RequestParam String supplierId,
	                          @RequestParam String supplierName,
	                          @RequestParam Double debt,
	                          Model model) {
	  model.addAttribute("supplierId", supplierId);
	  model.addAttribute("supplierName", supplierName);
	  model.addAttribute("debt", debt);
	  model.addAttribute("employees", employeeRepository.findByStatus(1));
	  model.addAttribute("pageTitle", "Thanh toán công nợ");
	  model.addAttribute("active", "debt-report");
	  return "warehouse/debt-pay";
	}
	
	@PostMapping("/debt-report/pay")
	public String payDebt(@RequestParam String supplierId,
	                      @RequestParam Double paymentAmount,
	                      @RequestParam(required=false) String paymentMethod,
	                      @RequestParam(required=false) String note,
	                      @RequestParam(required=false) String employeeName,
	                      RedirectAttributes redirectAttributes) {
	  try {
	    com.example.Bookstore.dto.warehouse.PayDebtRequest request = 
	      new com.example.Bookstore.dto.warehouse.PayDebtRequest(
	        supplierId, paymentAmount, paymentMethod, note, employeeName
	      );
	    supplierService.payDebt(request);
	    redirectAttributes.addFlashAttribute("successMessage", "Thanh toán thành công!");
	  } catch (IllegalArgumentException e) {
	    redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
	  } catch (Exception e) {
	    redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
	    e.printStackTrace();
	  }
	  return "redirect:/warehouse/debt-report";
	}
	
	@GetMapping("/debt-report/history/{supplierId}")
	public String paymentHistory(@PathVariable String supplierId,
	                             @RequestParam(required = false) Integer month,
	                             @RequestParam(required = false) Integer year,
	                             Model model) {
	  // Lấy thông tin nhà cung cấp
	  var supplier = supplierService.findById(supplierId);
	  model.addAttribute("supplier", supplier);
	  
	  // Mặc định hiển thị tháng hiện tại nếu không có tham số
	  java.time.LocalDate now = java.time.LocalDate.now();
	  int selectedMonth = (month != null) ? month : now.getMonthValue();
	  int selectedYear = (year != null) ? year : now.getYear();
	  
	  // Tạo danh sách tháng và năm cho dropdown
	  java.util.List<Integer> months = java.util.stream.IntStream.rangeClosed(1, 12).boxed().toList();
	  java.util.List<Integer> years = java.util.stream.IntStream.rangeClosed(2020, now.getYear()).boxed()
	    .sorted(java.util.Comparator.reverseOrder()).toList();
	  model.addAttribute("months", months);
	  model.addAttribute("years", years);
	  model.addAttribute("selectedMonth", selectedMonth);
	  model.addAttribute("selectedYear", selectedYear);
	  
	  // Lấy lịch sử thanh toán theo tháng
	  var paymentHistory = supplierService.getPaymentHistory(supplierId, PageRequest.of(0, 500));
	  
	  // Lọc theo tháng/năm được chọn
	  java.time.LocalDateTime fromDate = java.time.LocalDateTime.of(selectedYear, selectedMonth, 1, 0, 0);
	  java.time.LocalDateTime toDate = fromDate.plusMonths(1);
	  
	  var filteredHistory = paymentHistory.getContent().stream()
	    .filter(p -> p.getPaymentDate() != null && 
	                 !p.getPaymentDate().isBefore(fromDate) && 
	                 p.getPaymentDate().isBefore(toDate))
	    .toList();
	  
	  model.addAttribute("paymentHistory", filteredHistory);
	  
	  // Tính tổng tiền đã thanh toán (tất cả các lần thanh toán)
	  double totalPaid = paymentHistory.getContent().stream()
	    .mapToDouble(p -> p.getPaymentAmount() != null ? p.getPaymentAmount() : 0.0)
	    .sum();
	  
	  java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
	  model.addAttribute("totalPaidFmt", nf.format(totalPaid));
	  
	  model.addAttribute("pageTitle", "Lịch sử thanh toán - " + supplier.getName());
	  model.addAttribute("active", "debt-report");
	  
	  return "warehouse/payment-history";
	}
	}
