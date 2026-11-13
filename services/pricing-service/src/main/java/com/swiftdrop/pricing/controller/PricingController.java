package com.swiftdrop.pricing.controller;

import com.swiftdrop.pricing.dto.ApiResponse;
import com.swiftdrop.pricing.dto.PricingRequestDto;
import com.swiftdrop.pricing.dto.PricingResponseDto;
import com.swiftdrop.pricing.service.PricingFacadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * REST controller exposing endpoints for price quote calculation and retrieval.
 * Delegates orchestration strictly to {@link PricingFacadeService}.
 */
@RestController
@RequestMapping("/api/v1/pricing")
@Tag(name = "Pricing Quotes", description = "Dynamic shipping rate calculation and price quote management")
@RequiredArgsConstructor
public class PricingController {

    private final PricingFacadeService pricingFacadeService;

    /**
     * Calculates a shipping fare and generates an immutable price quote with TTL validity.
     *
     * @param request the shipment parameters for pricing calculation
     * @return 201 Created with Location header and {@link ApiResponse} wrapping {@link PricingResponseDto}
     */
    @PostMapping("/quotes")
    @Operation(
            summary = "Calculate and create a new price quote",
            description = "Executes the dynamic pricing pipeline across delivery urgency tiers, destination zone multipliers, floating fuel surcharges, and discount rules. Returns a persisted, guaranteed price quote."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Price quote created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload or constraint validation failure"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Specified origin or destination zone not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Delivery tier restrictions violated or inactive zone/promo code"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal configuration or fuel index error")
    })
    public ResponseEntity<ApiResponse<PricingResponseDto>> createQuote(
            @Valid @RequestBody PricingRequestDto request) {

        // Step 7: Merchant tier is null (untrusted/no auth). Step 8 will extract authenticated merchant identity from JWT.
        PricingResponseDto quoteDto = pricingFacadeService.calculateAndCreateQuote(request, null);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(quoteDto.getId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(ApiResponse.ok("Price quote created successfully", quoteDto));
    }

    /**
     * Retrieves an existing price quote by ID regardless of expiration status.
     *
     * @param id the quote UUID
     * @return 200 OK with {@link ApiResponse} wrapping {@link PricingResponseDto}
     */
    @GetMapping("/quotes/{id}")
    @Operation(
            summary = "Retrieve a price quote by ID",
            description = "Fetches a price quote by its unique identifier regardless of expiration status (historical retrieval)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Price quote retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Price quote not found")
    })
    public ResponseEntity<ApiResponse<PricingResponseDto>> getQuote(
            @Parameter(description = "Unique identifier of the price quote", required = true)
            @PathVariable UUID id) {

        PricingResponseDto quoteDto = pricingFacadeService.getQuote(id);
        return ResponseEntity.ok(ApiResponse.ok(quoteDto));
    }

    /**
     * Retrieves an active price quote by ID, enforcing expiration status.
     *
     * @param id the quote UUID
     * @return 200 OK with {@link ApiResponse} wrapping {@link PricingResponseDto}
     */
    @GetMapping("/quotes/{id}/active")
    @Operation(
            summary = "Retrieve an active price quote by ID",
            description = "Fetches a price quote only if it has not yet expired. Returns 410 Gone if expired."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active price quote retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid UUID format"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Price quote not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "Price quote has expired")
    })
    public ResponseEntity<ApiResponse<PricingResponseDto>> getActiveQuote(
            @Parameter(description = "Unique identifier of the active price quote", required = true)
            @PathVariable UUID id) {

        PricingResponseDto quoteDto = pricingFacadeService.getActiveQuote(id);
        return ResponseEntity.ok(ApiResponse.ok(quoteDto));
    }
}
