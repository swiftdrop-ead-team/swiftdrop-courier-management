package com.swiftdrop.pricing.controller;
import com.swiftdrop.pricing.entity.*;
import com.swiftdrop.pricing.repository.PricingRuleRepository;
import com.swiftdrop.pricing.strategy.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/pricing")
@RequiredArgsConstructor
public class PricingController {
 private final PricingRuleRepository rules;
 private final PricingStrategyRegistry strategies;
 public record Calculation(@NotNull @DecimalMin(value="0",inclusive=false) BigDecimal weightKg,
  @NotNull @DecimalMin(value="0",inclusive=false) BigDecimal lengthCm,
  @NotNull @DecimalMin(value="0",inclusive=false) BigDecimal widthCm,
  @NotNull @DecimalMin(value="0",inclusive=false) BigDecimal heightCm,
  @NotNull ServiceType deliveryType, boolean isFragile) {}
 public record Rate(@NotNull @DecimalMin("0") BigDecimal baseFare,
  @NotNull @DecimalMin("0") BigDecimal perKgRate,
  @NotNull @DecimalMin("0") BigDecimal perKmRate,
  @NotNull @DecimalMin("0.01") BigDecimal baseWeightKg,
  @NotNull @Min(1) Integer volumetricDivisor,
  @NotNull @DecimalMin("1") BigDecimal urgencyMultiplier, boolean active) {}
 @PostMapping("/calculate")
 public Map<String,Object> calculate(@Valid @RequestBody Calculation request) {
  var rule=rules.findByServiceTypeAndIsActiveTrue(request.deliveryType())
    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,"This delivery service is unavailable"));
  var context=PricingContext.builder().physicalWeightKg(request.weightKg()).lengthCm(request.lengthCm())
   .widthCm(request.widthCm()).heightCm(request.heightCm()).distanceKm(BigDecimal.ZERO).build();
  var result=strategies.getStrategy(request.deliveryType()).calculate(context,rule);
  var fee=result.getSubtotalFare().add(request.isFragile()?new BigDecimal("75"):BigDecimal.ZERO).setScale(2,RoundingMode.HALF_UP);
  return Map.of("totalFee",fee,"currency","LKR");
 }
 @GetMapping("/rules") @PreAuthorize("hasRole('ADMIN')")
 public List<PricingRule> rules() { return rules.findAll(); }
 @PutMapping("/rules/{tier}") @PreAuthorize("hasRole('ADMIN')") @Transactional
 public PricingRule save(@PathVariable ServiceType tier,@Valid @RequestBody Rate rate) {
  var rule=rules.findByServiceType(tier).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  rule.setBaseFare(rate.baseFare());rule.setPerKgRate(rate.perKgRate());rule.setPerKmRate(rate.perKmRate());
  rule.setBaseWeightKg(rate.baseWeightKg());rule.setVolumetricDivisor(rate.volumetricDivisor());
  rule.setUrgencyMultiplier(rate.urgencyMultiplier());rule.setIsActive(rate.active());return rules.save(rule);
 }
}
