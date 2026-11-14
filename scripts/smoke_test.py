"""Exercise the merged services through Nginx and the gateway. Creates test records.

Run after `docker compose up --build -d`: python scripts/smoke_test.py
Override SWIFTDROP_BASE_URL / SWIFTDROP_ADMIN_PASSWORD for another local stack.
Uses only Python's standard library and never prints authentication tokens.
"""
import json
import os
import re
import time
import uuid
from urllib.error import HTTPError
from urllib.request import Request, urlopen

BASE = os.getenv("SWIFTDROP_BASE_URL", "http://localhost:3000/api/v1").rstrip("/")


def request(method, path, data=None, token=None, expected=200):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = Request(BASE + path, data=json.dumps(data).encode() if data is not None else None,
                  headers=headers, method=method)
    try:
        with urlopen(req, timeout=20) as response:
            status, body = response.status, response.read()
    except HTTPError as error:
        status, body = error.code, error.read()
    assert status == expected, f"{method} {path}: expected {expected}, got {status}: {body[:400]!r}"
    return json.loads(body) if body else None


def eventually(check, description, seconds=40):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        try:
            if check():
                print("PASS", description, flush=True)
                return
        except (AssertionError, HTTPError):
            pass
        time.sleep(1)
    raise AssertionError("Timed out: " + description)


def main():
    run = uuid.uuid4().hex[:10]
    password = "SmokeTest@2026!"
    admin = request("POST", "/auth/login", {
        "email": "admin@swiftdrop.io",
        "password": os.getenv("SWIFTDROP_ADMIN_PASSWORD", "Admin@SwiftDrop2026!")})["accessToken"]
    courier_login = request("POST", "/auth/login", {
        "email": "courier@swiftdrop.io", "password": "Courier@123!"})
    courier, courier_user = courier_login["accessToken"], courier_login["user"]
    email = f"smoke-{run}@example.com"
    customer = request("POST", "/auth/register", {
        "fullName": "Smoke Test Customer", "email": email, "phoneNumber": "0771234567",
        "password": password, "confirmPassword": password, "acceptTerms": True}, expected=201)["accessToken"]
    stranger = request("POST", "/auth/register", {
        "fullName": "Other Test Customer", "email": f"other-{run}@example.com", "phoneNumber": "0771234568",
        "password": password, "confirmPassword": password, "acceptTerms": True}, expected=201)["accessToken"]
    print("PASS login and registration", flush=True)
    request("GET", "/dispatch/couriers", expected=401)
    request("GET", "/dispatch/couriers", token=customer, expected=403)
    request("GET", "/dispatch/couriers", token=courier, expected=403)
    request("POST", "/notifications/send", {"parcelId": "denied", "recipientEmail": email,
            "notificationType": "ORDER_CONFIRMED"}, token=customer, expected=403)
    print("PASS role boundaries", flush=True)

    profile = request("GET", "/auth/profile", token=customer)
    profile.update(fullName="Updated Smoke Customer", address="24 Main Road, Colombo")
    request("PUT", "/auth/profile", profile, customer)
    assert request("GET", "/auth/profile", token=customer)["fullName"] == "Updated Smoke Customer"
    address = {"id": str(uuid.uuid4()), "label": "Home", "address": "24 Main Road, Colombo"}
    request("PUT", "/auth/addresses", [address], customer)
    assert request("GET", "/auth/addresses", token=customer) == [address]
    request("PUT", "/notifications/preferences", {"enabled": False}, customer)
    assert request("GET", "/notifications/preferences", token=customer)["enabled"] is False
    request("PUT", "/notifications/preferences", {"enabled": True}, customer)
    print("PASS profile, saved addresses and email preferences", flush=True)
    customer_id = request("GET", "/auth/me", token=customer)["id"]
    admin_id = request("GET", "/auth/me", token=admin)["id"]
    request("PATCH", f"/admin/users/{customer_id}", {"roles": ["CUSTOMER"], "status": "ACTIVE"}, admin)
    request("PATCH", f"/admin/users/{admin_id}", {"roles": ["CUSTOMER"], "status": "INACTIVE"}, admin, expected=409)
    print("PASS admin access updates and self-protection", flush=True)

    rates = request("GET", "/pricing/rules", token=admin)
    assert any(r["serviceType"] == "STANDARD" and r["isActive"] for r in rates)
    standard = next(r for r in rates if r["serviceType"] == "STANDARD")
    request("PUT", "/pricing/rules/STANDARD", {**standard, "active": standard["isActive"]}, admin)
    request("GET", "/pricing/rules", token=customer, expected=403)
    couriers = request("GET", "/dispatch/couriers", token=admin)["data"]
    linked = next((c for c in couriers if c["userId"] == courier_user["id"]), None)
    if linked is None:
        linked = request("POST", "/dispatch/couriers", {
            "userId": courier_user["id"], "fullName": courier_user["fullName"],
            "phoneNumber": courier_user.get("phoneNumber") or "0771234567", "email": courier_user["email"],
            "vehicleType": "MOTORBIKE", "maxActiveDeliveries": 20}, admin, expected=201)["data"]
    request("PATCH", f"/dispatch/couriers/{linked['id']}/status", {"status": "AVAILABLE"}, courier)

    booking = {"senderName": "Smoke Customer", "senderPhone": "0771234567", "pickupAddress": "24 Main Road, Colombo",
               "recipientName": "Smoke Recipient", "recipientPhone": "0772345678", "deliveryAddress": "18 Lake Road, Kandy",
               "deliveryCity": "Kandy", "weightKg": 1, "lengthCm": 20, "widthCm": 15, "heightCm": 10,
               "description": "Smoke test parcel", "deliveryType": "STANDARD", "isFragile": False}

    approved_merchant = request("POST", "/auth/login", {
        "email": "merchant.approved@swiftdrop.io", "password": "Merchant@123!"})["accessToken"]
    pending_merchant = request("POST", "/auth/login", {
        "email": "merchant.pending@swiftdrop.io", "password": "Merchant@123!"})["accessToken"]
    assert request("GET", "/auth/profile", token=approved_merchant)["businessName"]
    request("POST", "/parcels/estimate", booking, pending_merchant, expected=403)
    request("POST", "/parcels", booking, pending_merchant, expected=403)
    merchant_estimate = request("POST", "/parcels/estimate", booking, approved_merchant)
    merchant_parcel = request("POST", "/parcels", {**booking, "estimateId": merchant_estimate["id"]}, approved_merchant, expected=201)
    assert merchant_parcel["senderType"] == "MERCHANT"
    request("POST", "/parcels/" + merchant_parcel["id"] + "/cancel", {"reason": "Smoke test completed"}, approved_merchant)
    print("PASS approved merchant booking and server-side pending-merchant restrictions", flush=True)

    def book():
        estimate = request("POST", "/parcels/estimate", booking, customer)
        assert estimate["currency"] == "LKR"
        payload = {**booking, "estimateId": estimate["id"]}
        parcel = request("POST", "/parcels", payload, customer, expected=201)
        assert float(parcel["deliveryFee"]) == float(estimate["amount"])
        request("POST", "/parcels", payload, customer, expected=409)
        return parcel

    def assign(parcel):
        return request("POST", "/dispatch/assignments", {
            "parcelId": parcel["id"], "courierId": linked["id"], "trackingNumber": parcel["trackingNumber"],
            "pickupAddress": parcel["pickupAddress"], "deliveryAddress": parcel["deliveryAddress"],
            "recipientName": parcel["recipientName"], "recipientPhone": parcel["recipientPhone"],
            "notificationEmail": email, "notificationName": "Smoke Customer"}, admin, expected=201)["data"]

    parcel = book()
    eventually(lambda: request("GET", "/tracking/" + parcel["trackingNumber"])["currentStatus"] == "CREATED", "tracking starts at booking")
    request("GET", "/parcels/" + parcel["id"], token=stranger, expected=403)
    assignment = assign(parcel)
    aid = assignment["id"]
    eventually(lambda: request("GET", "/parcels/" + parcel["id"], token=courier)["assignedCourierId"] == courier_user["id"], "assignment reaches parcel service")
    request("PATCH", f"/dispatch/assignments/{aid}/status", {"status": "DELIVERED"}, courier, expected=400)
    for status in ["PICKED_UP", "IN_TRANSIT", "OUT_FOR_DELIVERY"]:
        request("PATCH", f"/dispatch/assignments/{aid}/status", {"status": status}, courier)
    request("POST", f"/dispatch/assignments/{aid}/proof", {"recipientName": "Smoke Recipient", "deliveryNotes": "Received"}, courier, expected=201)
    eventually(lambda: request("GET", "/tracking/" + parcel["trackingNumber"])["currentStatus"] == "DELIVERED", "tracking reaches delivered")
    eventually(lambda: request("GET", "/parcels/" + parcel["id"], token=customer)["status"] == "DELIVERED", "customer sees delivered status")
    eventually(lambda: any(n["parcelId"] == parcel["id"] for n in request("GET", "/notifications/history", token=customer)["data"]), "notification history reaches owner")
    notices = request("GET", "/notifications/history", token=customer)["data"]
    assert all(n["recipientEmail"].lower() == email.lower() for n in notices)
    request("PATCH", f"/notifications/{notices[0]['id']}/read", {}, customer)
    assert request("GET", f"/notifications/{notices[0]['id']}", token=customer)["data"]["readAt"]
    request("GET", f"/notifications/{notices[0]['id']}", token=stranger, expected=403)
    print("PASS full booking, delivery, privacy and notification-read workflow", flush=True)

    cancelled = book()
    cancelled_assignment = assign(cancelled)
    request("POST", "/parcels/" + cancelled["id"] + "/cancel", {"reason": "Smoke test cancellation"}, customer)
    eventually(lambda: request("GET", f"/dispatch/assignments/{cancelled_assignment['id']}", token=admin)["data"]["status"] == "CANCELLED", "cancellation closes dispatch assignment")
    eventually(lambda: request("GET", "/tracking/" + cancelled["trackingNumber"])["currentStatus"] == "CANCELLED", "cancellation reaches public tracking")
    request("POST", "/auth/forgot-password", {"email": email})
    mailpit = os.getenv("SWIFTDROP_MAILPIT_URL", "http://localhost:8025").rstrip("/")
    reset = {}

    def find_reset_email():
        with urlopen(mailpit + "/api/v1/messages", timeout=15) as response:
            messages = json.load(response)["messages"]
        for message in messages:
            if message["Subject"] == "Reset your SwiftDrop password" and any(
                    recipient["Address"].lower() == email.lower() for recipient in message["To"]):
                with urlopen(mailpit + "/api/v1/message/" + message["ID"], timeout=15) as response:
                    text = json.load(response)["Text"]
                match = re.search(r"/reset-password\?token=([A-Za-z0-9-]+)", text)
                if match:
                    reset["token"] = match.group(1)
                    return True
        return False

    eventually(find_reset_email, "password reset email delivered to local SMTP inbox")
    new_password = "SmokeReset@2026!"
    reset_body = {"token": reset["token"], "newPassword": new_password, "confirmPassword": new_password}
    request("POST", "/auth/reset-password", reset_body)
    # Identity's established invalid-credentials response is HTTP 400.
    request("POST", "/auth/login", {"email": email, "password": password}, expected=400)
    assert request("POST", "/auth/login", {"email": email, "password": new_password})["accessToken"]
    request("POST", "/auth/reset-password", reset_body, expected=401)
    print("PASS emailed password reset, new login and single-use token", flush=True)
    print("All merged-service smoke checks passed. Test accounts and parcels remain for inspection.", flush=True)


if __name__ == "__main__":
    main()
