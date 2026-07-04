package com.dreamnest.entity;

import com.dreamnest.enums.ShipmentStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Tracking/shipping details for an order - either created automatically via
 * the Shiprocket API, or entered manually by the business admin (e.g. when
 * they ship through a courier directly rather than via Shiprocket).
 */
@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ShipmentStatus status = ShipmentStatus.PENDING;

    @Column(name = "courier_name", length = 100)
    private String courierName;

    @Column(name = "awb_number", length = 100)
    private String awbNumber;

    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;

    /** "SHIPROCKET" if created via the API, "MANUAL" if entered by the business admin. */
    @Column(name = "source", length = 20, nullable = false)
    private String source = "MANUAL";

    @Column(name = "shiprocket_order_id", length = 50)
    private String shiprocketOrderId;

    @Column(name = "shiprocket_shipment_id", length = 50)
    private String shiprocketShipmentId;

    @Column(name = "last_tracking_note", length = 500)
    private String lastTrackingNote;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Shipment() {
    }

    public Shipment(Order order) {
        this.order = order;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public String getCourierName() {
        return courierName;
    }

    public void setCourierName(String courierName) {
        this.courierName = courierName;
    }

    public String getAwbNumber() {
        return awbNumber;
    }

    public void setAwbNumber(String awbNumber) {
        this.awbNumber = awbNumber;
    }

    public String getTrackingUrl() {
        return trackingUrl;
    }

    public void setTrackingUrl(String trackingUrl) {
        this.trackingUrl = trackingUrl;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getShiprocketOrderId() {
        return shiprocketOrderId;
    }

    public void setShiprocketOrderId(String shiprocketOrderId) {
        this.shiprocketOrderId = shiprocketOrderId;
    }

    public String getShiprocketShipmentId() {
        return shiprocketShipmentId;
    }

    public void setShiprocketShipmentId(String shiprocketShipmentId) {
        this.shiprocketShipmentId = shiprocketShipmentId;
    }

    public String getLastTrackingNote() {
        return lastTrackingNote;
    }

    public void setLastTrackingNote(String lastTrackingNote) {
        this.lastTrackingNote = lastTrackingNote;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(LocalDateTime shippedAt) {
        this.shippedAt = shippedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
