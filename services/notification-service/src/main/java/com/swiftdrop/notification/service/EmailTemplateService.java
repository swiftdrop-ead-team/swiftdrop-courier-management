package com.swiftdrop.notification.service;

import org.springframework.stereotype.Service;

/**
 * Service responsible for generating clean, responsive HTML email templates
 * for SwiftDrop notification milestones.
 */
@Service
public class EmailTemplateService {

    private String getBaseEmailLayout(String title, String heading, String bodyContent) {
        return "<!DOCTYPE html>"
                + "<html lang=\"en\">"
                + "<head><meta charset=\"UTF-8\"><title>" + title + "</title>"
                + "<style>"
                + "  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px; color: #1e293b; }"
                + "  .container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 8px; border: 1px solid #e2e8f0; overflow: hidden; }"
                + "  .header { background: #0f172a; padding: 24px; text-align: center; }"
                + "  .header h1 { margin: 0; color: #38bdf8; font-size: 22px; font-weight: 700; letter-spacing: 0.5px; }"
                + "  .header p { margin: 4px 0 0; color: #94a3b8; font-size: 13px; }"
                + "  .content { padding: 32px 24px; }"
                + "  .heading { font-size: 18px; font-weight: 600; color: #0f172a; margin-bottom: 16px; }"
                + "  .badge { display: inline-block; padding: 6px 12px; border-radius: 9999px; font-size: 13px; font-weight: 600; margin-bottom: 16px; }"
                + "  .badge-info { background: #e0f2fe; color: #0369a1; }"
                + "  .badge-success { background: #dcfce7; color: #15803d; }"
                + "  .badge-warning { background: #fef3c7; color: #b45309; }"
                + "  .badge-danger { background: #fee2e2; color: #b91c1c; }"
                + "  .details-box { background: #f1f5f9; border-radius: 6px; padding: 16px; margin: 20px 0; font-size: 14px; line-height: 1.6; }"
                + "  .details-row { display: flex; justify-content: space-between; margin-bottom: 8px; }"
                + "  .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 20px; text-align: center; font-size: 12px; color: #64748b; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "  <div class=\"container\">"
                + "    <div class=\"header\">"
                + "      <h1>SwiftDrop Logistics</h1>"
                + "      <p>Delivering trust, every mile.</p>"
                + "    </div>"
                + "    <div class=\"content\">"
                + "      <div class=\"heading\">" + heading + "</div>"
                +        bodyContent
                + "    </div>"
                + "    <div class=\"footer\">"
                + "      <p>&copy; 2026 SwiftDrop Logistics Platform. All rights reserved.</p>"
                + "      <p>Need help? Contact support@swiftdrop.io</p>"
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }

    public String buildOrderConfirmedEmail(String recipientName, String parcelId, String trackingNumber) {
        String safeName = (recipientName != null && !recipientName.isBlank()) ? recipientName : "Valued Customer";
        String content = "<span class=\"badge badge-info\">Order Booked</span>"
                + "<p>Hi " + safeName + ",</p>"
                + "<p>Thank you for choosing SwiftDrop! Your delivery order has been confirmed and registered in our system.</p>"
                + "<div class=\"details-box\">"
                + "  <p><strong>Parcel ID:</strong> " + parcelId + "</p>"
                + "  <p><strong>Tracking Number:</strong> " + trackingNumber + "</p>"
                + "  <p><strong>Current Status:</strong> Created / Awaiting Pickup</p>"
                + "</div>"
                + "<p>You will receive updates when your parcel is on the way to your destination.</p>";
        return getBaseEmailLayout("SwiftDrop — Order Confirmed", "Your delivery booking is confirmed!", content);
    }

    public String buildCourierAssignedEmail(String courierName, String parcelId, String trackingNumber,
                                          String pickupAddress, String deliveryAddress,
                                          String recipientName, String recipientPhone) {
        String safeCourier = (courierName != null && !courierName.isBlank()) ? courierName : "Field Courier";
        String content = "<span class=\"badge badge-info\">New Assignment</span>"
                + "<p>Hi " + safeCourier + ",</p>"
                + "<p>You have been assigned a new parcel delivery for your route today.</p>"
                + "<div class=\"details-box\">"
                + "  <p><strong>Parcel ID:</strong> " + parcelId + "</p>"
                + "  <p><strong>Tracking Number:</strong> " + trackingNumber + "</p>"
                + "  <p><strong>Pickup Location:</strong> " + (pickupAddress != null ? pickupAddress : "Central Hub") + "</p>"
                + "  <p><strong>Delivery Destination:</strong> " + (deliveryAddress != null ? deliveryAddress : "Customer Destination") + "</p>"
                + "  <p><strong>Recipient Name:</strong> " + (recipientName != null ? recipientName : "N/A") + "</p>"
                + "  <p><strong>Recipient Phone:</strong> " + (recipientPhone != null ? recipientPhone : "N/A") + "</p>"
                + "</div>"
                + "<p>Please open your SwiftDrop Courier console to view your route and mark pickup.</p>";
        return getBaseEmailLayout("SwiftDrop — New Delivery Assignment", "New Parcel Assignment Assigned", content);
    }

    public String buildOutForDeliveryEmail(String recipientName, String parcelId, String trackingNumber, String courierName) {
        String safeName = (recipientName != null && !recipientName.isBlank()) ? recipientName : "Valued Customer";
        String safeCourier = (courierName != null && !courierName.isBlank()) ? courierName : "our courier driver";
        String content = "<span class=\"badge badge-warning\">Arriving Today</span>"
                + "<p>Hi " + safeName + ",</p>"
                + "<p>Great news! Your package is currently out for delivery with <strong>" + safeCourier + "</strong>.</p>"
                + "<div class=\"details-box\">"
                + "  <p><strong>Tracking Number:</strong> " + trackingNumber + "</p>"
                + "  <p><strong>Status:</strong> OUT FOR DELIVERY</p>"
                + "  <p><strong>Estimated Arrival:</strong> Today</p>"
                + "</div>"
                + "<p>Please keep your phone reachable so the driver can contact you upon arrival.</p>";
        return getBaseEmailLayout("SwiftDrop — Out for Delivery", "Your parcel is arriving today!", content);
    }

    public String buildDeliveredEmail(String recipientName, String parcelId, String trackingNumber) {
        String safeName = (recipientName != null && !recipientName.isBlank()) ? recipientName : "Valued Customer";
        String content = "<span class=\"badge badge-success\">Delivered Successfully</span>"
                + "<p>Hi " + safeName + ",</p>"
                + "<p>Your parcel has been successfully delivered and handed over at the destination.</p>"
                + "<div class=\"details-box\">"
                + "  <p><strong>Tracking Number:</strong> " + trackingNumber + "</p>"
                + "  <p><strong>Status:</strong> DELIVERED</p>"
                + "</div>"
                + "<p>Thank you for using SwiftDrop Courier Management System!</p>";
        return getBaseEmailLayout("SwiftDrop — Package Delivered", "Your package has been delivered!", content);
    }

    public String buildDeliveryFailedEmail(String recipientName, String parcelId, String trackingNumber, String failureReason) {
        String safeName = (recipientName != null && !recipientName.isBlank()) ? recipientName : "Valued Customer";
        String safeReason = (failureReason != null && !failureReason.isBlank()) ? failureReason : "Recipient unavailable at address";
        String content = "<span class=\"badge badge-danger\">Delivery Attempt Failed</span>"
                + "<p>Hi " + safeName + ",</p>"
                + "<p>We attempted to deliver your package, but the delivery could not be completed.</p>"
                + "<div class=\"details-box\">"
                + "  <p><strong>Tracking Number:</strong> " + trackingNumber + "</p>"
                + "  <p><strong>Failure Reason:</strong> " + safeReason + "</p>"
                + "  <p><strong>Next Step:</strong> Our dispatch team will schedule a re-attempt on the next business day.</p>"
                + "</div>"
                + "<p>If you need to update your delivery address or contact number, please reach out to customer support.</p>";
        return getBaseEmailLayout("SwiftDrop — Delivery Attempt Failed", "Action Required: Delivery Attempt Failed", content);
    }
}

