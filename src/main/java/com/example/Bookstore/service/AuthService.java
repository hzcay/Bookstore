package com.example.Bookstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;
import java.util.Random;

import com.example.Bookstore.util.IDGenerator;

@Service
public class AuthService {
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private String generateOTP() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    private SimpleMailMessage createOTPEmail(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Mã OTP để đăng ký tài khoản");
        message.setText("Mã OTP của bạn là: " + otp);
        return message;
    }
    
    public void sendOTP(String email) {
        String otp = generateOTP(); 
        redisTemplate.opsForValue().set("otp:" + email, otp, 5, TimeUnit.MINUTES);
        

        mailSender.send(createOTPEmail(email, otp));
    }
    
    public boolean verifyOTP(String email, String otp) {
        String key = "otp:" + email;
        String storedOTP = redisTemplate.opsForValue().get(key);
        boolean ok = otp.equals(storedOTP);
        if (ok) {
            redisTemplate.delete(key);
        }
        return ok;
    }
}