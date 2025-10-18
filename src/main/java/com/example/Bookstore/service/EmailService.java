package com.example.Bookstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    /**
     * Gửi email đơn giản - dùng logic giống AuthService
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            // Dùng cùng logic như AuthService.sendOTP()
            mailSender.send(message);
            System.out.println("✅ Email sent successfully to: " + to);
            System.out.println("📧 Subject: " + subject);
            System.out.println("📝 Content: " + text);
            
        } catch (Exception e) {
            System.err.println("❌ Error sending email: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gửi email với file đính kèm
     */
    public void sendEmailWithAttachment(String to, String subject, String text, 
                                      byte[] attachment, String attachmentName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true); // HTML content
            
            if (attachment != null && attachmentName != null) {
                helper.addAttachment(attachmentName, () -> new java.io.ByteArrayInputStream(attachment));
            }
            
            mailSender.send(message);
            System.out.println("✅ Email with attachment sent successfully to: " + to);
            
        } catch (MessagingException e) {
            System.err.println("❌ Error sending email with attachment: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
