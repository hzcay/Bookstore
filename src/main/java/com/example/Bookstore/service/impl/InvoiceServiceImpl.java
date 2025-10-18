package com.example.Bookstore.service.impl;

import com.example.Bookstore.entity.Order;
import com.example.Bookstore.service.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Override
    public byte[] generateInvoicePDF(Order order) throws Exception {
        // 1. Tạo HTML từ template
        String html = generateInvoiceHTML(order);
        
        // 2. Chuyển HTML sang PDF
        return convertHTMLToPDF(html);
    }
    
    private String generateInvoiceHTML(Order order) {
        Context context = new Context();
        
        // Thêm data vào context
        context.setVariable("order", order);
        context.setVariable("orderItems", order.getOrderItems());
        
        // Tính tạm tính = tổng (giá × số lượng) của các sản phẩm
        double subtotal = order.getOrderItems().stream()
            .mapToDouble(item -> item.getPrice() * item.getQuantity())
            .sum();
        context.setVariable("subtotal", subtotal);
        
        // Format ngày tháng
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String formattedDate = order.getCreateAt() != null ? 
            order.getCreateAt().format(formatter) : "";
        context.setVariable("formattedDate", formattedDate);
        
        // Xử lý template
        return templateEngine.process("NhanVienBanHang/invoice-template", context);
    }
    
    private byte[] convertHTMLToPDF(String html) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(outputStream);
        outputStream.close();
        
        return outputStream.toByteArray();
    }
}
