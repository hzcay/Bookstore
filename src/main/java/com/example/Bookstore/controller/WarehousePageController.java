package com.example.Bookstore.controller;

import com.example.Bookstore.dto.warehouse.CreateReceiptRequest;
import com.example.Bookstore.entity.Inventory;
import com.example.Bookstore.repository.BookRepository;
import com.example.Bookstore.repository.AuthorRepository;
import com.example.Bookstore.repository.PublisherRepository;
import com.example.Bookstore.entity.Book;
import com.example.Bookstore.entity.Author;
import com.example.Bookstore.entity.Category;
import com.example.Bookstore.entity.Publisher;
import com.example.Bookstore.repository.CategoryRepository;
import com.example.Bookstore.service.InventoryService;
import com.example.Bookstore.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    model.addAttribute("categories", categoryRepository.findAll());
    model.addAttribute("authors", authorRepository.findAll());
    model.addAttribute("publishers", publisherRepository.findAll());
    model.addAttribute("category", category);
    model.addAttribute("keyword", keyword);

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

  
	
  @GetMapping("/stock/low")
  public String low(Model model, @RequestParam(defaultValue="10") int threshold) {
    var data = inventoryService.lowStock(threshold);
    model.addAttribute("data", data);
    return "warehouse/report";
  }
	
  @GetMapping("/receipts")
  public String receipts(Model model) {
    var list = inventoryService.listReceipts(null, null, PageRequest.of(0, 50));
    model.addAttribute("receipts", list.getContent());
    
    // Thêm dữ liệu cho dropdown
    model.addAttribute("books", bookRepository.findAll());
    model.addAttribute("suppliers", supplierService.findAll());
    
    // Thống kê
    model.addAttribute("totalReceipts", list.getTotalElements());
    model.addAttribute("totalBooks", list.getContent().stream().mapToInt(Inventory::getQuantity).sum());
    model.addAttribute("totalSuppliers", supplierService.findAll().size());
    model.addAttribute("lowStockCount", inventoryService.lowStock(10).size());
    
    model.addAttribute("pageTitle", "Quản lý nhập sách");
    model.addAttribute("active", "receipts");
    return "warehouse/receipts";
  }
	
  @PostMapping("/receipts")
  public String createReceipt(@Valid CreateReceiptRequest form) {
    inventoryService.createReceipt(form);
    return "redirect:/warehouse/receipts";
  }
	
  @GetMapping("/suppliers")
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
}

