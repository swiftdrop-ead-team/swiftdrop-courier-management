package com.swiftdrop.dispatch.security;
import com.swiftdrop.dispatch.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
@Component("dispatchAccess")
@RequiredArgsConstructor
public class DispatchAccess {
 private final CourierRepository couriers;
 private final DispatchAssignmentRepository assignments;
 private String userId() {
  var auth = SecurityContextHolder.getContext().getAuthentication();
  return auth != null && auth.getPrincipal() instanceof UserPrincipal p ? p.getId().toString() : "";
 }
 public boolean user(String id) { return userId().equals(id); }
 public boolean courier(Long id) { return couriers.findById(id).map(c -> user(c.getUserId())).orElse(false); }
 public boolean assignment(Long id) { return assignments.findById(id).map(a -> user(a.getCourier().getUserId())).orElse(false); }
 public boolean tracking(String number) { return assignments.findByTrackingNumber(number).map(a -> user(a.getCourier().getUserId())).orElse(false); }
}
