package com.swiftdrop.identity.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
@Service @RequiredArgsConstructor
public class PasswordResetMailer {
 private final JavaMailSender mail;
 @Value("${swiftdrop.security.frontend-url}") private String frontend;
 @Value("${NOTIFICATION_MAIL_FROM:no-reply@swiftdrop.io}") private String from;
 public void send(String email,String token) {
  var message=new SimpleMailMessage();message.setFrom(from);message.setTo(email);
  message.setSubject("Reset your SwiftDrop password");
  message.setText("Use this link within one hour to reset your password:\n"+frontend+"/reset-password?token="+token+"\nIf you did not request this, ignore this email.");
  mail.send(message);
 }
}
