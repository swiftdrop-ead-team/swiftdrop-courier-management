"""Merchant integration through the frontend proxy. Creates test orders and rotates test keys.
Use MERCHANT_TEST_LOCAL_WEBHOOK=1 only with the temporary local webhook Compose override.
"""
import json,os,uuid
from urllib.request import Request,urlopen
from urllib.error import HTTPError
from smoke_test import BASE,eventually

def request(method,path,data=None,token=None,key=None,reference=None,expected=200):
 headers={"Content-Type":"application/json"}
 if token:headers["Authorization"]="Bearer "+token
 if key:headers["X-API-Key"]=key
 if reference:headers["Idempotency-Key"]=reference
 try:
  with urlopen(Request(BASE+path,data=json.dumps(data).encode() if data is not None else None,headers=headers,method=method),timeout=30) as r:status,body=r.status,r.read()
 except HTTPError as e:status,body=e.code,e.read()
 assert status==expected,f"{method} {path}: expected {expected}, got {status}: {body[:300]!r}"
 return json.loads(body) if body else None

def main():
 approved=request("POST","/auth/login",{"email":"merchant.approved@swiftdrop.io","password":"Merchant@123!"})["accessToken"]
 pending=request("POST","/auth/login",{"email":"merchant.pending@swiftdrop.io","password":"Merchant@123!"})["accessToken"]
 profile=request("GET","/merchant/profile",token=approved)["data"]
 assert profile["approvalStatus"]=="APPROVED"
 assert request("GET","/merchant/profile",token=pending)["data"]["approvalStatus"]=="PENDING"
 request("POST","/merchant/api-keys",{"scopes":["orders:read"]},token=pending,expected=403)
 fields={k:profile[k] for k in ["businessName","contactPerson","phoneNumber","businessAddress"]}
 assert request("PUT","/merchant/profile",fields,token=approved)["data"]["businessName"]==fields["businessName"]
 print("PASS Identity approval synchronization, pending restrictions and business profile",flush=True)
 scopes={"scopes":["orders:read","orders:write","dashboard:read"]}
 old=request("POST","/merchant/api-keys/rotate",scopes,token=approved)["data"]["key"]
 key=request("POST","/merchant/api-keys/rotate",scopes,token=approved)["data"]["key"]
 request("GET","/merchant/orders",key=old,expected=401)
 request("GET","/merchant/orders",key=key)
 readonly=request("POST","/merchant/api-keys",{"scopes":["orders:read"]},token=approved)["data"]
 booking={"senderName":"Smoke Store","senderPhone":"0771234567","pickupAddress":"24 Store Road, Colombo","recipientName":"Merchant Smoke Customer","recipientPhone":"0772345678","deliveryAddress":"18 Main Road, Kandy","deliveryCity":"Kandy","weightKg":1,"lengthCm":20,"widthCm":15,"heightCm":10,"description":"Clothing","deliveryType":"STANDARD","isFragile":False,"specialInstructions":""}
 request("POST","/merchant/orders",booking,key=readonly["key"],reference=uuid.uuid4().hex,expected=403)
 request("POST","/merchant/orders",booking,token=pending,reference=uuid.uuid4().hex,expected=403)
 print("PASS API key rotation, revocation and scopes",flush=True)
 hook=None
 if os.getenv("MERCHANT_TEST_LOCAL_WEBHOOK"):
  hook=request("POST","/merchant/webhooks",{"targetUrl":"http://127.0.0.1:8083/api/v1/merchant/demo/webhook","eventTypes":["parcel.status.changed"]},token=approved)["data"]
  assert hook["signingSecret"]
  assert all("signingSecret" not in h for h in request("GET","/merchant/webhooks",token=approved)["data"])
 else:
  request("POST","/merchant/webhooks",{"targetUrl":"http://127.0.0.1:8083/api/v1/merchant/demo/webhook","eventTypes":["parcel.status.changed"]},token=approved,expected=409)
 estimate=request("POST","/parcels/estimate",booking,token=approved)
 payload={**booking,"estimateId":estimate["id"]};reference=uuid.uuid4().hex
 order=request("POST","/merchant/orders",payload,token=approved,reference=reference,expected=201)["data"]
 repeated=request("POST","/merchant/orders",payload,token=approved,reference=reference,expected=201)["data"]
 assert order["id"]==repeated["id"]
 request("POST","/merchant/orders",{**payload,"recipientName":"Changed"},token=approved,reference=reference,expected=409)
 parcel=request("GET","/parcels/"+order["parcelId"],token=approved)
 assert parcel["senderType"]=="MERCHANT" and parcel["currency"]=="LKR" and float(parcel["deliveryFee"])==float(estimate["amount"])
 api_order=request("POST","/merchant/orders",booking,key=key,reference=uuid.uuid4().hex,expected=201)["data"]
 assert request("GET","/parcels/"+api_order["parcelId"],token=approved)["senderType"]=="MERCHANT"
 print("PASS JWT and API key orders, real quotes, parcel ownership and duplicate protection",flush=True)
 for item in [order,api_order]:request("POST","/parcels/"+item["parcelId"]+"/cancel",{"reason":"Merchant smoke test completed"},token=approved)
 eventually(lambda:all(o["deliveryStatus"]=="CANCELLED" for o in request("GET","/merchant/orders",key=key)["data"] if o["id"] in [order["id"],api_order["id"]]),"merchant order cancellation synchronizes from Parcel")
 if hook:
  path=f"/merchant/webhooks/{hook['id']}/deliveries"
  eventually(lambda:len([d for d in request("GET",path,token=approved)["data"] if d["status"]=="DELIVERED" and d["trackingNumber"]==order["trackingNumber"] and d["parcelStatus"]=="CANCELLED"])>0,"signed webhook delivered and visible in merchant history",seconds=60)
  request("DELETE",f"/merchant/webhooks/{hook['id']}",token=approved)
 request("DELETE",f"/merchant/api-keys/{readonly['id']}",token=approved)
 assert request("GET","/merchant/orders/dashboard/summary",key=key)["data"]["totalOrders"]>=2
 print("PASS merchant dashboard summary and cleanup",flush=True)

if __name__=="__main__":main()
