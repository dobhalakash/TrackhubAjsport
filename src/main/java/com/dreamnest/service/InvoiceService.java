package com.dreamnest.service;

import com.dreamnest.entity.Address;
import com.dreamnest.entity.Order;
import com.dreamnest.entity.OrderItem;
import com.dreamnest.entity.Payment;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Generates PDF invoices for orders and emails them to the customer once an
 * order is delivered. PDFs are rendered on demand from a simple HTML/CSS
 * template (via openhtmltopdf) rather than stored, since an order's data
 * doesn't change after delivery and re-rendering is cheap.
 */
@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final EmailService emailService;

    public InvoiceService(EmailService emailService) {
        this.emailService = emailService;
    }

    /** Renders the invoice for the given order as PDF bytes. */
    public byte[] generatePdf(Order order, Payment payment) {
        String html = buildHtml(order, payment);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render invoice PDF for order {}", order.getOrderNumber(), e);
            throw new IllegalStateException("Could not generate invoice", e);
        }
    }

    /**
     * Emails the invoice to the customer, once. Safe to call repeatedly
     * (e.g. every time a webhook reports DELIVERED) - it only actually
     * sends the first time, tracked via {@code Order.invoiceEmailSent}.
     * Returns true if an email was sent during this call.
     */
    public boolean sendInvoiceEmailIfNeeded(Order order, Payment payment) {
        if (order.isInvoiceEmailSent()) {
            return false;
        }
        if (order.getUser() == null || order.getUser().getEmail() == null) {
            return false;
        }

        byte[] pdf = generatePdf(order, payment);
        String subject = "Your TrackHub invoice - Order " + order.getOrderNumber();
        String body = "Hi " + safeName(order) + ",\n\n"
                + "Your order " + order.getOrderNumber() + " has been delivered. Your invoice is attached.\n\n"
                + "Thanks for shopping with TrackHub!\n\n"
                + "- The TrackHub Team";

        boolean sent = emailService.sendEmailWithPdfAttachment(
                order.getUser().getEmail(), subject, body, pdf,
                "Invoice-" + order.getOrderNumber() + ".pdf");

        if (sent) {
            order.setInvoiceEmailSent(true);
        }
        return sent;
    }

    private String safeName(Order order) {
        if (order.getUser() == null) return "there";
        String first = order.getUser().getFirstName();
        return (first == null || first.isBlank()) ? "there" : first;
    }

    private String buildHtml(Order order, Payment payment) {
        Address addr = order.getAddress();
        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            itemsHtml.append("<tr>")
                    .append("<td>").append(escape(item.getProductName())).append(item.getSize() != null ? " (" + item.getSize() + ")" : "").append("</td>")
                    .append("<td class='num'>").append(item.getQuantity()).append("</td>")
                    .append("<td class='num'>").append(formatAmount(item.getUnitPrice())).append("</td>")
                    .append("<td class='num'>").append(formatAmount(item.getTotalPrice())).append("</td>")
                    .append("</tr>");
        }

        String paymentMethod = payment != null ? payment.getPaymentMethod().name() : "-";
        String paymentStatus = payment != null ? payment.getStatus().name() : "-";
        String invoiceDate = order.getUpdatedAt() != null ? order.getUpdatedAt().format(DATE_FORMAT) : "";

        return "<html><head><style>"
                + "body { font-family: Helvetica, Arial, sans-serif; color: #14181F; font-size: 12px; }"
                + ".header { width: 100%; margin-bottom: 24px; overflow: hidden; }"
                + ".header .left { float: left; width: 60%; }"
                + ".header .right { float: right; width: 38%; text-align: right; }"
                + ".brand { font-size: 22px; font-weight: bold; }"
                + ".brand .accent { color: #C8102E; }"
                + ".invoice-title { font-size: 16px; font-weight: bold; text-transform: uppercase; margin-top: 4px; }"
                + ".muted { color: #6b7280; }"
                + "table { width: 100%; border-collapse: collapse; margin-top: 18px; }"
                + "th { background: #14181F; color: #fff; text-align: left; padding: 8px; font-size: 11px; text-transform: uppercase; }"
                + "td { padding: 8px; border-bottom: 1px solid #e5e7eb; font-size: 12px; }"
                + ".num { text-align: right; }"
                + ".totals { width: 280px; margin-left: auto; margin-top: 12px; }"
                + ".totals td { border: none; padding: 4px 8px; }"
                + ".totals .grand { font-weight: bold; font-size: 14px; border-top: 2px solid #14181F; }"
                + ".addr-block { width: 100%; margin-top: 20px; overflow: hidden; }"
                + ".addr-block .col { float: left; width: 47%; }"
                + ".addr-block .col.second { float: right; }"
                + "h4 { margin-bottom: 4px; text-transform: uppercase; font-size: 11px; color: #6b7280; }"
                + "</style></head><body>"
                + "<div class='header'>"
                + "<div class='left'><div class='brand'>Track<span class='accent'>Hub</span></div><div class='muted'>Premium Jerseys, Teamwear &amp; Sports Apparel</div></div>"
                + "<div class='right'><div class='invoice-title'>Invoice</div>"
                + "<div class='muted'>Order: " + escape(order.getOrderNumber()) + "</div>"
                + "<div class='muted'>Date: " + invoiceDate + "</div></div>"
                + "</div>"

                + "<div class='addr-block'>"
                + "<div class='col'><h4>Billed To</h4>"
                + (addr != null ? escape(addr.getFullName()) + "<br/>" + escape(addr.getAddressLine1())
                + (addr.getAddressLine2() != null && !addr.getAddressLine2().isBlank() ? "<br/>" + escape(addr.getAddressLine2()) : "")
                + "<br/>" + escape(addr.getCity()) + ", " + escape(addr.getState()) + " " + escape(addr.getPincode())
                + "<br/>" + escape(addr.getPhone()) : "-")
                + "</div>"
                + "<div class='col second'><h4>Payment</h4>"
                + "Method: " + escape(paymentMethod) + "<br/>"
                + "Status: " + escape(paymentStatus) + "<br/>"
                + (payment != null && payment.getTransactionId() != null ? "Ref: " + escape(payment.getTransactionId()) : "")
                + "</div>"
                + "</div>"

                + "<table><thead><tr><th>Item</th><th class='num'>Qty</th><th class='num'>Unit Price</th><th class='num'>Total</th></tr></thead>"
                + "<tbody>" + itemsHtml + "</tbody></table>"

                + "<table class='totals'>"
                + "<tr><td>Subtotal</td><td class='num'>" + formatAmount(order.getSubtotal()) + "</td></tr>"
                + "<tr><td>Tax</td><td class='num'>" + formatAmount(order.getTaxAmount()) + "</td></tr>"
                + "<tr><td>Shipping</td><td class='num'>" + formatAmount(order.getShippingAmount()) + "</td></tr>"
                + (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0
                ? "<tr><td>Discount</td><td class='num'>-" + formatAmount(order.getDiscountAmount()) + "</td></tr>" : "")
                + "<tr class='grand'><td>Grand Total</td><td class='num'>" + formatAmount(order.getGrandTotal()) + "</td></tr>"
                + "</table>"

                + "<p class='muted' style='margin-top:40px;'>This is a system-generated invoice from TrackHub. For questions, contact support@trackhub.com.</p>"
                + "</body></html>";
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        return "Rs. " + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
