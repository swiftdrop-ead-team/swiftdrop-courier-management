package com.swiftdrop.notification.controller;
import com.swiftdrop.notification.entity.EmailPreference;
import com.swiftdrop.notification.repository.EmailPreferenceRepository;
import com.swiftdrop.notification.security.UserPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import java.util.Map;
@RestController @RequestMapping("/api/v1/notifications/preferences") @RequiredArgsConstructor
public class EmailPreferenceController {
 private final EmailPreferenceRepository preferences;
 @GetMapping public Map<String,Boolean> get(@AuthenticationPrincipal UserPrincipal p){
  return Map.of("enabled",preferences.findById(p.getEmail().toLowerCase(java.util.Locale.ROOT)).map(EmailPreference::isEnabled).orElse(true));
 }
 @PutMapping public Map<String,Boolean> save(@AuthenticationPrincipal UserPrincipal p,@RequestBody Map<String,Boolean> body){
  var preference=new EmailPreference();preference.setEmail(p.getEmail().toLowerCase(java.util.Locale.ROOT));
  preference.setEnabled(Boolean.TRUE.equals(body.get("enabled")));preferences.save(preference);return Map.of("enabled",preference.isEnabled());
 }
}
