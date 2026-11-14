package com.swiftdrop.merchant.service;
import com.swiftdrop.merchant.dto.MerchantPrincipal;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;
@Service
public class MerchantServiceToken {
 private final SecretKey key;
 public MerchantServiceToken(@Value("${swiftdrop.security.jwt.secret}") String secret) {key=Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));}
 public String forMerchant(MerchantPrincipal principal) {
  return Jwts.builder().subject(principal.userId().toString()).issuer("swiftdrop-merchant")
   .claim("email",principal.email()).claim("fullName",principal.email()).claim("roles",List.of("ROLE_MERCHANT"))
   .issuedAt(Date.from(Instant.now())).expiration(Date.from(Instant.now().plusSeconds(60))).signWith(key).compact();
 }
}
