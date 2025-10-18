package com.example.Bookstore.controller;

import com.example.Bookstore.dto.UpdateProfileRequest;
import com.example.Bookstore.entity.Customer;
import com.example.Bookstore.repository.CustomerRepository;
import com.example.Bookstore.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import static com.example.Bookstore.controller.AuthController.SESSION_UID;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final CustomerRepository customerRepository;
    private final CustomerService customerService;

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        String customerId = (String) session.getAttribute(SESSION_UID);
        if (customerId == null || customerId.isBlank()) return "redirect:/login";

        Customer me = customerRepository.findByCustomerIdAndStatus(customerId, 1)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        UpdateProfileRequest form = new UpdateProfileRequest();
        form.setName(me.getName());
        form.setEmail(me.getEmail());
        form.setPhone(me.getPhone());
        form.setAddress(me.getAddress());

        model.addAttribute("uri", "/profile");
        model.addAttribute("form", form);
        model.addAttribute("points", me.getPoints());
        return "profile";
    }

    @PostMapping("/profile")
    public String update(HttpSession session,
                         @Valid @ModelAttribute("form") UpdateProfileRequest form,
                         BindingResult br,
                         Model model) {
        String customerId = (String) session.getAttribute(SESSION_UID);
        if (customerId == null || customerId.isBlank()) return "redirect:/login";

        Customer me = customerRepository.findByCustomerIdAndStatus(customerId, 1)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!br.hasErrors()) {
            try {
                customerService.updateProfile(me.getCustomerId(), form);
                model.addAttribute("success", "Đã lưu thay đổi.");
            } catch (IllegalArgumentException ex) {
                model.addAttribute("error", ex.getMessage());
            }
        }

        model.addAttribute("uri", "/profile");
        model.addAttribute("points", me.getPoints());
        return "profile";
    }
}

