package com.webjpa.shopping.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_order")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal payAmount;

    @Column(nullable = false, length = 200)
    private String shippingAddress;

    @Column(length = 50)
    private String couponCode;

    @Column(length = 100)
    private String couponName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryStatus deliveryStatus;

    @Column(length = 100)
    private String trackingNumber;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    private LocalDateTime shippedAt;

    private LocalDateTime deliveredAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    protected PurchaseOrder() {
    }

    private PurchaseOrder(Member member, String shippingAddress) {
        this.member = member;
        this.shippingAddress = shippingAddress;
        this.status = OrderStatus.CREATED;
        this.totalAmount = BigDecimal.ZERO;
        this.discountAmount = BigDecimal.ZERO;
        this.payAmount = BigDecimal.ZERO;
        this.deliveryStatus = DeliveryStatus.READY;
        this.orderedAt = LocalDateTime.now();
    }

    public static PurchaseOrder create(Member member, String shippingAddress) {
        return new PurchaseOrder(member, shippingAddress);
    }

    public void addItem(Product product, int quantity, BigDecimal unitPrice) {
        OrderItem orderItem = OrderItem.create(this, product.getId(), product.getName(), quantity, unitPrice);
        items.add(orderItem);
        totalAmount = totalAmount.add(orderItem.getLineTotal());
    }

    public void attachPayment(Payment payment) {
        this.payment = payment;
    }

    public void applyCoupon(String couponCode, String couponName, BigDecimal discountAmount) {
        this.couponCode = couponCode;
        this.couponName = couponName;
        this.discountAmount = discountAmount;
        this.payAmount = totalAmount.subtract(discountAmount);
    }

    public void markPaid() {
        this.status = OrderStatus.PAID;
    }

    public void markPaymentFailed() {
        this.status = OrderStatus.PAYMENT_FAILED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    public void prepareDelivery() {
        this.deliveryStatus = DeliveryStatus.PREPARING;
    }

    public void ship(String trackingNumber) {
        this.deliveryStatus = DeliveryStatus.SHIPPED;
        this.trackingNumber = trackingNumber;
        this.shippedAt = LocalDateTime.now();
    }

    public void deliver() {
        this.deliveryStatus = DeliveryStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getPayAmount() {
        return payAmount;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public String getCouponName() {
        return couponName;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public LocalDateTime getShippedAt() {
        return shippedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public Payment getPayment() {
        return payment;
    }
}
