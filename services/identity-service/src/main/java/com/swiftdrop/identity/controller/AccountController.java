package com.swiftdrop.identity.controller;
import com.swiftdrop.identity.entity.*;
import com.swiftdrop.identity.repository.*;
import com.swiftdrop.identity.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
@RestController @RequestMapping("/api/v1/auth") @RequiredArgsConstructor
public class AccountController {
 private final UserRepository users;
 private final AccountSettingsRepository settings;
 private final ObjectMapper mapper;
 public record Profile(@NotBlank @Size(min=2,max=150) String fullName, String email,
  @Size(max=30) String phone, String businessName, @Size(max=500) String address, boolean emailNotifications) {}
 public record Address(@NotBlank @Size(max=100) String id,@NotBlank @Size(max=60) String label,
  @NotBlank @Size(min=5,max=500) String address) {}
 @GetMapping("/business-profile") @Transactional(readOnly=true)
 @org.springframework.security.access.prepost.PreAuthorize("hasRole('MERCHANT')")
 public Map<String,Object> business(@AuthenticationPrincipal UserPrincipal p) {
  var u=users.findById(p.getId()).orElseThrow();var r=u.getMerchantRecord();
  if(r==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Business registration not found");
  return Map.of("userId",u.getId(),"email",u.getEmail(),"businessName",r.getBusinessName(),
   "contactPerson",r.getContactPerson(),"phoneNumber",r.getPhoneNumber()==null?"":r.getPhoneNumber(),
   "businessAddress",r.getBusinessAddress(),"approvalStatus",r.getStatus().name());
 }
 private AccountSettings preferences(UUID id) {
  return settings.findById(id).orElseGet(() -> {var s=new AccountSettings();s.setUserId(id);return s;});
 }
 @GetMapping("/profile") @Transactional(readOnly=true)
 public Profile profile(@AuthenticationPrincipal UserPrincipal p) {
  var u=users.findById(p.getId()).orElseThrow();var s=preferences(p.getId());
  return new Profile(u.getFullName(),u.getEmail(),u.getPhoneNumber(),
   u.getMerchantRecord()==null?"":u.getMerchantRecord().getBusinessName(),s.getAddress(),s.isEmailNotifications());
 }
 @PutMapping("/profile") @Transactional
 public Profile save(@AuthenticationPrincipal UserPrincipal p,@Valid @RequestBody Profile profile) {
  var u=users.findById(p.getId()).orElseThrow();
  if(!u.getEmail().equalsIgnoreCase(profile.email())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Email changes require verification. Keep your current sign-in email.");
  u.setFullName(profile.fullName().trim());u.setPhoneNumber(profile.phone());users.save(u);
  var s=preferences(p.getId());s.setAddress(profile.address());s.setEmailNotifications(profile.emailNotifications());settings.save(s);
  return profile(p);
 }
 @GetMapping("/addresses")
 public List<Address> addresses(@AuthenticationPrincipal UserPrincipal p) throws Exception {
  return mapper.readValue(preferences(p.getId()).getAddressesJson(),new TypeReference<List<Address>>(){});
 }
 @PutMapping("/addresses") @Transactional
 public List<Address> saveAddresses(@AuthenticationPrincipal UserPrincipal p,@Valid @RequestBody @Size(max=20) List<@Valid Address> addresses) throws Exception {
  if(addresses.size()>20 || addresses.stream().map(Address::id).distinct().count()!=addresses.size())
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use at most 20 unique saved addresses");
  var s=preferences(p.getId());s.setAddressesJson(mapper.writeValueAsString(addresses));settings.save(s);return addresses;
 }
}
