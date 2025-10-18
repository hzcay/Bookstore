package com.example.Bookstore.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.example.Bookstore.entity.Promotion;
import com.example.Bookstore.repository.PromotionRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PromotionController {
    private final PromotionRepository promoRepo;

    @GetMapping("/active")
    public List<Promotion> active(@RequestParam(required = false) Double subtotal) {
        var now = LocalDateTime.now();
        var all = promoRepo.findByStatusAndExpireDateAfterOrderByExpireDateAsc(1, now);
        if (subtotal == null)
            return all;
        return all.stream()
                .filter(p -> subtotal >= (p.getMinValue() == null ? 0d : p.getMinValue()))
                .toList();
    }

    @GetMapping("/validate")
    public java.util.Map<String, Object> validate(
            @RequestParam String code,
            @RequestParam(required = false) Double subtotal) {

        var now = LocalDateTime.now();
        var opt = promoRepo.findByCodeIgnoreCaseAndStatusAndExpireDateAfter(code, 1, now);

        var resp = new java.util.HashMap<String, Object>();
        if (opt.isEmpty()) {
            resp.put("valid", false);
            resp.put("code", code);
            resp.put("discount", 0);
            resp.put("minValue", 0);
            resp.put("message", "Mã không tồn tại hoặc đã hết hạn");
            return resp;
        }

        var p = opt.get();
        double min = p.getMinValue() == null ? 0d : p.getMinValue();
        boolean ok = subtotal == null || subtotal >= min;

        resp.put("valid", ok);
        resp.put("code", p.getCode());
        resp.put("discount", p.getDiscount() == null ? 0d : p.getDiscount());
        resp.put("minValue", min);
        resp.put("expireDate", p.getExpireDate());
        if (!ok)
            resp.put("message", "Chưa đạt giá trị tối thiểu");

        return resp;
    }
}

