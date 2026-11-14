package com.swiftdrop.merchant.service;
import com.swiftdrop.merchant.dto.*;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;
class ParcelClientIntegrationTest {
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void sendsAuthenticatedParcelContractAndReadsActualParcelId(){
  var builder=RestClient.builder().baseUrl("http://parcel");var server=MockRestServiceServer.bindTo(builder).build();
  var tokens=new MerchantServiceToken(Base64.getEncoder().encodeToString(new byte[32]));
  var principal=new MerchantPrincipal(UUID.randomUUID(),"merchant@example.test",true);
  SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,List.of()));
  server.expect(requestTo("http://parcel/api/v1/parcels")).andExpect(header("Idempotency-Key","store-1"))
   .andExpect(request->{assertThat(request.getHeaders().getFirst("Authorization")).startsWith("Bearer ");})
   .andExpect(content().json("{\"merchantOrderReference\":\"store-1\",\"recipientName\":\"Customer\",\"deliveryCity\":\"Kandy\",\"lengthCm\":20}",org.springframework.test.json.JsonCompareMode.LENIENT))
   .andRespond(withSuccess("{\"id\":\"parcel-1\",\"trackingNumber\":\"SD123\"}",MediaType.APPLICATION_JSON));
  var client=new ParcelClientImpl(builder.build(),"/api/v1/parcels",tokens);
  var response=client.createParcel("store-1",new ParcelOrderRequest("store-1","Customer","0771234567","Delivery","Pickup","Clothing",java.math.BigDecimal.ONE,Map.of("deliveryCity","Kandy","lengthCm",20)));
  assertThat(response.parcelId()).isEqualTo("parcel-1");server.verify();
 }
}
