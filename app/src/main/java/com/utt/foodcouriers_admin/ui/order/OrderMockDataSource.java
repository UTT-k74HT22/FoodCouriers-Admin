package com.utt.foodcouriers_admin.ui.order;

import com.utt.foodcouriers_admin.data.model.Order;
import com.utt.foodcouriers_admin.data.model.OrderItem;
import com.utt.foodcouriers_admin.data.model.OrderStatus;
import com.utt.foodcouriers_admin.data.model.Shipper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OrderMockDataSource {

    private static final List<Order> ORDERS = new ArrayList<>();
    private static final List<Shipper> SHIPPERS = new ArrayList<>();

    static {
        if (SHIPPERS.isEmpty()) {
            SHIPPERS.add(buildShipper("shipper-01", "Nguyễn Quốc Shipper", "0911111111"));
            SHIPPERS.add(buildShipper("shipper-02", "Trần Văn Giao", "0922222222"));
            SHIPPERS.add(buildShipper("shipper-03", "Lê Hoàng Ship", "0933333333"));
        }

        if (ORDERS.isEmpty()) {
            ORDERS.add(buildOrder("order-001", "ORD20260410001", OrderStatus.PENDING, "Cơm Tấm 79", "Nguyễn Minh Anh", "0901000001", 145000, "Ít đá", null));
            ORDERS.add(buildOrder("order-002", "ORD20260410002", OrderStatus.CONFIRMED, "Bún Bò Quận 1", "Trần Quốc Việt", "0901000002", 92000, "Gọi trước khi giao", null));
            ORDERS.add(buildOrder("order-003", "ORD20260410003", OrderStatus.PREPARING, "Phở Ngon 24h", "Lê Hà My", "0901000003", 168000, "", null));
            ORDERS.add(buildOrder("order-007", "ORD20260410007", OrderStatus.READY_FOR_PICKUP, "Bún Đậu Mắm Tôm", "Đặng Văn Lâm", "0901000007", 120000, "Nhiều mắm tôm", null));
            ORDERS.add(buildOrder("order-008", "ORD20260410008", OrderStatus.READY_FOR_PICKUP, "Gà Rán Popeyes", "Nguyễn Công Phượng", "0901000008", 250000, "", null));
            ORDERS.add(buildOrder("order-004", "ORD20260410004", OrderStatus.DELIVERING, "Cơm Gà Hải Nam", "Phạm Khánh", "0901000004", 110000, "", "shipper-01"));
            ORDERS.add(buildOrder("order-005", "ORD20260410005", OrderStatus.DELIVERED, "Mì Cay Sài Gòn", "Hoàng Thu", "0901000005", 76000, "", "shipper-02"));
            ORDERS.add(buildOrder("order-006", "ORD20260410006", OrderStatus.CANCELLED, "Bánh Mì Chảo 5 Sao", "Ngô Thảo", "0901000006", 54000, "", null));
        }
    }

    private OrderMockDataSource() {
    }

    public static List<Order> getOrders() {
        return deepCopyOrders(ORDERS);
    }

    public static Order getOrderById(String orderId) {
        for (Order order : ORDERS) {
            if (order.getId().equals(orderId)) {
                return cloneOrder(order);
            }
        }
        return null;
    }

    public static List<Shipper> getShippers() {
        return new ArrayList<>(SHIPPERS);
    }

    public static Order updateStatus(String orderId, OrderStatus status) {
        for (Order order : ORDERS) {
            if (order.getId().equals(orderId)) {
                order.setStatus(status.getValue());
                order.setUpdatedAt("2026-04-10T12:00:00");
                return cloneOrder(order);
            }
        }
        return null;
    }

    public static Order assignShipper(String orderId, Shipper shipper) {
        for (Order order : ORDERS) {
            if (order.getId().equals(orderId)) {
                if (shipper != null) {
                    order.setShipperId(shipper.getUserId());
                    Order.OrderUser shipperUser = new Order.OrderUser();
                    shipperUser.setId(shipper.getUserId());
                    shipperUser.setFullName(shipper.getFullName());
                    shipperUser.setPhone(shipper.getPhone());
                    order.setShipper(shipperUser);
                }
                return cloneOrder(order);
            }
        }
        return null;
    }

    private static List<Order> deepCopyOrders(List<Order> source) {
        List<Order> copied = new ArrayList<>();
        for (Order order : source) {
            copied.add(cloneOrder(order));
        }
        return copied;
    }

    private static Order cloneOrder(Order original) {
        Order copy = new Order();
        copy.setId(original.getId());
        copy.setOrderCode(original.getOrderCode());
        copy.setRestaurantId(original.getRestaurantId());
        copy.setUserId(original.getUserId());
        copy.setShipperId(original.getShipperId());
        copy.setDeliveryAddress(original.getDeliveryAddress());
        copy.setNote(original.getNote());
        copy.setSubtotal(original.getSubtotal());
        copy.setDeliveryFee(original.getDeliveryFee());
        copy.setDiscount(original.getDiscount());
        copy.setTotal(original.getTotal());
        copy.setPaymentMethod(original.getPaymentMethod());
        copy.setPaymentStatus(original.getPaymentStatus());
        copy.setStatus(original.getStatus());
        copy.setCreatedAt(original.getCreatedAt());
        copy.setUpdatedAt(original.getUpdatedAt());
        copy.setCancelledReason(original.getCancelledReason());
        copy.setUser(cloneUser(original.getUser()));
        copy.setRestaurant(cloneRestaurant(original.getRestaurant()));
        copy.setShipper(cloneUser(original.getShipper()));

        List<OrderItem> items = new ArrayList<>();
        for (OrderItem item : original.getItems()) {
            OrderItem copyItem = new OrderItem();
            copyItem.setId(item.getId());
            copyItem.setOrderId(item.getOrderId());
            copyItem.setMenuItemName(item.getMenuItemName());
            copyItem.setMenuItemPrice(item.getMenuItemPrice());
            copyItem.setQuantity(item.getQuantity());
            copyItem.setSubtotal(item.getSubtotal());
            copyItem.setNote(item.getNote());
            items.add(copyItem);
        }
        copy.setItems(items);
        return copy;
    }

    private static Order.OrderUser cloneUser(Order.OrderUser original) {
        if (original == null) {
            return null;
        }
        Order.OrderUser copy = new Order.OrderUser();
        copy.setId(original.getId());
        copy.setFullName(original.getFullName());
        copy.setPhone(original.getPhone());
        copy.setEmail(original.getEmail());
        return copy;
    }

    private static Order.OrderRestaurant cloneRestaurant(Order.OrderRestaurant original) {
        if (original == null) {
            return null;
        }
        Order.OrderRestaurant copy = new Order.OrderRestaurant();
        copy.setId(original.getId());
        copy.setName(original.getName());
        return copy;
    }

    private static Order buildOrder(String id,
                                    String code,
                                    OrderStatus status,
                                    String restaurantName,
                                    String customerName,
                                    String phone,
                                    int total,
                                    String note,
                                    String shipperId) {
        Order order = new Order();
        order.setId(id);
        order.setOrderCode(code);
        order.setRestaurantId("restaurant-" + id);
        order.setUserId("user-" + id);
        order.setShipperId(shipperId);
        order.setDeliveryAddress("123 Nguyen Trai, Quan 1, TP.HCM");
        order.setNote(note);
        order.setSubtotal(Math.max(total - 15000, 0));
        order.setDeliveryFee(15000);
        order.setDiscount(0);
        order.setTotal(total);
        order.setPaymentMethod("cod");
        order.setPaymentStatus("pending");
        order.setStatus(status.getValue());
        order.setCreatedAt(String.format(Locale.US, "2026-04-10T1%d:2%d:00", ORDERS.size(), ORDERS.size()));
        order.setUpdatedAt(order.getCreatedAt());
        if (status == OrderStatus.CANCELLED) {
            order.setCancelledReason("Nhà hàng quá tải");
        }

        Order.OrderUser user = new Order.OrderUser();
        user.setId("user-" + id);
        user.setFullName(customerName);
        user.setPhone(phone);
        user.setEmail("mock@example.com");
        order.setUser(user);

        Order.OrderRestaurant restaurant = new Order.OrderRestaurant();
        restaurant.setId("restaurant-" + id);
        restaurant.setName(restaurantName);
        order.setRestaurant(restaurant);

        if (shipperId != null) {
            for (Shipper shipper : SHIPPERS) {
                if (shipperId.equals(shipper.getId()) || shipperId.equals(shipper.getUserId())) {
                    Order.OrderUser shipperUser = new Order.OrderUser();
                    shipperUser.setId(shipper.getUserId());
                    shipperUser.setFullName(shipper.getFullName());
                    shipperUser.setPhone(shipper.getPhone());
                    order.setShipper(shipperUser);
                    break;
                }
            }
        }

        List<OrderItem> items = new ArrayList<>();
        items.add(buildItem(id + "-1", id, "Cơm sườn nướng", 45000, 2));
        items.add(buildItem(id + "-2", id, "Trà đào", 18000, 1));
        order.setItems(items);
        return order;
    }

    private static OrderItem buildItem(String id, String orderId, String name, int price, int quantity) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setOrderId(orderId);
        item.setMenuItemName(name);
        item.setMenuItemPrice(price);
        item.setQuantity(quantity);
        item.setSubtotal(price * quantity);
        return item;
    }

    private static Shipper buildShipper(String id, String name, String phone) {
        Shipper shipper = new Shipper();
        shipper.setId(id);
        shipper.setUserId(id);
        shipper.setFullName(name);
        shipper.setPhone(phone);
        shipper.setRestaurantId("restaurant-order-001");
        shipper.setRoleInRestaurant("shipper");
        shipper.setActive(true);
        return shipper;
    }
}
