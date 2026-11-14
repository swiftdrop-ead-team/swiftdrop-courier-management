package com.swiftdrop.merchant.service;
import com.swiftdrop.merchant.dto.MerchantPrincipal;
import com.swiftdrop.merchant.entity.*;
import com.swiftdrop.merchant.repository.MerchantProfileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@Service
public class IdentityProfileSync {
 private final MerchantProfileRepository profiles;
 private final MerchantServiceToken tokens;
 private final RestClient client;
 private final boolean enabled;
 public IdentityProfileSync(MerchantProfileRepository profiles,MerchantServiceToken tokens,RestClient.Builder builder,
  @Value("${IDENTITY_SERVICE_URL:http://localhost:8081}") String url,@Value("${merchant.identity-sync.enabled:true}") boolean enabled) {
  var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();factory.setConnectTimeout(3000);factory.setReadTimeout(5000);
  this.profiles=profiles;this.tokens=tokens;this.client=builder.requestFactory(factory).baseUrl(url).build();this.enabled=enabled;
 }
 @Transactional
 public void sync(MerchantPrincipal principal) {
  if(!enabled)return;
  Map<?,?> source;
  try {source=client.get().uri("/api/v1/auth/business-profile").header("Authorization","Bearer "+tokens.forMerchant(principal)).retrieve().body(Map.class);}
  catch(RestClientResponseException e){throw new ResponseStatusException(e.getStatusCode().is4xxClientError()?HttpStatus.FORBIDDEN:HttpStatus.SERVICE_UNAVAILABLE,"Business approval could not be verified with Identity");}
  catch(RestClientException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Identity is temporarily unavailable");}
  if(source==null || !principal.userId().toString().equals(source.get("userId")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Business identity mismatch");
  var profile=profiles.findByUserId(principal.userId()).orElseGet(()->MerchantProfile.builder().userId(principal.userId())
   .businessName((String)source.get("businessName")).contactPerson((String)source.get("contactPerson"))
   .phoneNumber((String)source.get("phoneNumber")).businessAddress((String)source.get("businessAddress")).build());
  var approval=MerchantApprovalStatus.valueOf((String)source.get("approvalStatus"));
  boolean changed=profile.getId()==null || profile.getApprovalStatus()!=approval || !Objects.equals(profile.getEmail(),source.get("email"));
  profile.setApprovalStatus(approval);profile.setEmail((String)source.get("email"));
  if(changed)profiles.save(profile);
 }
}
