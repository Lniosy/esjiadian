package com.lniosy.usedappliance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lniosy.usedappliance.common.BizException;
import com.lniosy.usedappliance.dto.order.CartItemDto;
import com.lniosy.usedappliance.dto.order.CartItemUpdateRequest;
import com.lniosy.usedappliance.dto.order.OrderCreateRequest;
import com.lniosy.usedappliance.dto.order.OrderDto;
import com.lniosy.usedappliance.dto.order.ShipRequest;
import com.lniosy.usedappliance.dto.product.ProductDto;
import com.lniosy.usedappliance.entity.Cart;
import com.lniosy.usedappliance.entity.OrderInfo;
import com.lniosy.usedappliance.entity.OrderItem;
import com.lniosy.usedappliance.entity.Product;
import com.lniosy.usedappliance.entity.UserAddress;
import com.lniosy.usedappliance.mapper.CartMapper;
import com.lniosy.usedappliance.mapper.OrderInfoMapper;
import com.lniosy.usedappliance.mapper.OrderItemMapper;
import com.lniosy.usedappliance.mapper.ProductMapper;
import com.lniosy.usedappliance.mapper.UserAddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class OrderService {
    private final CartMapper cartMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final UserAddressMapper userAddressMapper;
    private final ProductService productService;
    private final NotificationService notificationService;
    private final RecommendTraceService recommendTraceService;

    public OrderService(CartMapper cartMapper, OrderInfoMapper orderInfoMapper, OrderItemMapper orderItemMapper,
                        ProductMapper productMapper, UserAddressMapper userAddressMapper,
                        ProductService productService, NotificationService notificationService,
                        RecommendTraceService recommendTraceService) {
        this.cartMapper = cartMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.userAddressMapper = userAddressMapper;
        this.productService = productService;
        this.notificationService = notificationService;
        this.recommendTraceService = recommendTraceService;
    }

    public List<CartItemDto> listCart(Long userId) {
        return cartMapper.selectList(new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .orderByDesc(Cart::getId))
                .stream().map(this::toCartItemDto).toList();
    }

    public void addCart(Long userId, Long productId) {
        productService.detail(productId);
        Product p = productMapper.selectById(productId);
        if (p == null || Boolean.TRUE.equals(p.getSold()) || !"ON_SHELF".equals(p.getStatus())) {
            throw new BizException(400, "商品已下架或售出，无法加入购物车");
        }
        Long count = cartMapper.selectCount(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        if (count >= 20) {
            throw new BizException(400, "购物车最多20件");
        }
        Cart exists = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId)
                .last("limit 1"));
        if (exists == null) {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(1);
            cart.setSelected(true);
            cartMapper.insert(cart);
        }
        recommendTraceService.recordEvent(userId, productId, "CART", 2.5);
    }

    public void removeCart(Long userId, Long productId) {
        cartMapper.delete(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, productId));
    }

    public void updateCartItem(Long userId, CartItemUpdateRequest req) {
        Cart exists = cartMapper.selectOne(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getProductId, req.productId())
                .last("limit 1"));
        if (exists == null) {
            throw new BizException(404, "购物车商品不存在");
        }
        if (req.quantity() != null && req.quantity() < 1) {
            throw new BizException(400, "数量必须大于0");
        }
        cartMapper.update(null, new LambdaUpdateWrapper<Cart>()
                .eq(Cart::getId, exists.getId())
                .set(req.quantity() != null, Cart::getQuantity, req.quantity())
                .set(req.selected() != null, Cart::getSelected, req.selected()));
    }

    @Transactional
    public OrderDto createOrder(Long buyerId, OrderCreateRequest req) {
        ProductDto product = productService.detail(req.productId());
        if (!"ON_SHELF".equals(product.status())) {
            throw new BizException(400, "商品不可购买");
        }
        int locked = productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, req.productId())
                .eq(Product::getStatus, "ON_SHELF")
                .eq(Product::getSold, false)
                .set(Product::getStatus, "OFF_SHELF"));
        if (locked == 0) {
            throw new BizException(400, "商品已被下单，请选择其他商品");
        }
        UserAddress address = userAddressMapper.selectById(req.addressId());
        if (address == null || !address.getUserId().equals(buyerId)) {
            throw new BizException(400, "收货地址无效");
        }

        String orderNo = "UA" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + buyerId;
        BigDecimal freight = BigDecimal.ZERO;
        BigDecimal paid = product.price().add(freight);

        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setBuyerId(buyerId);
        order.setSellerId(product.sellerId());
        order.setAddressSnapshot(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetailAddress());
        order.setOrderAmount(product.price());
        order.setFreightAmount(freight);
        order.setPaidAmount(paid);
        order.setTradeMethod(req.tradeMethod());
        order.setBuyerNote(req.buyerNote());
        order.setStatus("PENDING_PAYMENT");
        orderInfoMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(req.productId());
        item.setTitleSnapshot(product.title());
        item.setUnitPrice(product.price());
        item.setQuantity(1);
        orderItemMapper.insert(item);

        removeCart(buyerId, req.productId());
        recommendTraceService.recordEvent(buyerId, req.productId(), "ORDER_CREATE", 3.0);
        notificationService.create(product.sellerId(), "ORDER", "新订单待支付", "订单号: " + orderNo);
        return toDto(order, req.productId());
    }

    public List<OrderDto> listOrders(Long userId, String view) {
        LambdaQueryWrapper<OrderInfo> qw = new LambdaQueryWrapper<OrderInfo>().orderByDesc(OrderInfo::getId);
        if ("seller".equalsIgnoreCase(view)) {
            qw.eq(OrderInfo::getSellerId, userId);
        } else if ("all".equalsIgnoreCase(view)) {
            qw.and(w -> w.eq(OrderInfo::getBuyerId, userId).or().eq(OrderInfo::getSellerId, userId));
        } else {
            qw.eq(OrderInfo::getBuyerId, userId);
        }
        List<OrderInfo> list = orderInfoMapper.selectList(qw);
        return list.stream().map(o -> toDto(o, getOrderProductId(o.getId()))).toList();
    }

    public OrderDto getOrder(Long orderId, Long requester) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new BizException(404, "订单不存在");
        }
        if (!order.getBuyerId().equals(requester) && !order.getSellerId().equals(requester)) {
            throw new BizException(403, "无权查看");
        }
        Long productId = getOrderProductId(orderId);
        return toDto(order, productId);
    }

    public void cancel(Long userId, Long orderId) {
        OrderInfo o = orderInfoMapper.selectById(orderId);
        if (o == null || !o.getBuyerId().equals(userId) || !"PENDING_PAYMENT".equals(o.getStatus())) {
            throw new BizException(400, "当前状态不可取消");
        }
        orderInfoMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                .eq(OrderInfo::getId, orderId)
                .set(OrderInfo::getStatus, "CANCELLED"));
        releaseProductLock(getOrderProductId(orderId));
        notificationService.create(o.getSellerId(), "ORDER", "订单已取消", "订单号: " + o.getOrderNo());
    }

    public void markPaid(Long orderId) {
        OrderInfo o = orderInfoMapper.selectById(orderId);
        if (o == null || !"PENDING_PAYMENT".equals(o.getStatus())) {
            throw new BizException(400, "订单状态错误");
        }
        orderInfoMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                .eq(OrderInfo::getId, orderId)
                .set(OrderInfo::getStatus, "PENDING_SHIPMENT")
                .set(OrderInfo::getPaidAt, LocalDateTime.now()));
        Long productId = getOrderProductId(orderId);
        markProductSold(productId);
        clearProductFromAllCarts(productId);
        notificationService.create(o.getSellerId(), "ORDER", "订单已支付待发货", "订单号: " + o.getOrderNo());
    }

    public void ship(Long sellerId, Long orderId, ShipRequest req) {
        OrderInfo o = orderInfoMapper.selectById(orderId);
        if (o == null || !o.getSellerId().equals(sellerId) || !"PENDING_SHIPMENT".equals(o.getStatus())) {
            throw new BizException(400, "订单状态错误");
        }
        orderInfoMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                .eq(OrderInfo::getId, orderId)
                .set(OrderInfo::getStatus, "PENDING_RECEIPT")
                .set(OrderInfo::getShippedAt, LocalDateTime.now()));
        notificationService.create(o.getBuyerId(), "ORDER", "订单已发货", "订单号: " + o.getOrderNo());
    }

    public void confirmReceipt(Long buyerId, Long orderId) {
        OrderInfo o = orderInfoMapper.selectById(orderId);
        if (o == null || !o.getBuyerId().equals(buyerId) || !"PENDING_RECEIPT".equals(o.getStatus())) {
            throw new BizException(400, "订单状态错误");
        }
        orderInfoMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                .eq(OrderInfo::getId, orderId)
                .set(OrderInfo::getStatus, "PENDING_REVIEW")
                .set(OrderInfo::getReceivedAt, LocalDateTime.now()));
        notificationService.create(o.getSellerId(), "ORDER", "买家已确认收货", "订单号: " + o.getOrderNo());
    }

    public void completeAfterReview(Long orderId) {
        orderInfoMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                .eq(OrderInfo::getId, orderId)
                .eq(OrderInfo::getStatus, "PENDING_REVIEW")
                .set(OrderInfo::getStatus, "COMPLETED"));
    }

    public void autoCancelUnpaidOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(30);
        List<OrderInfo> timeout = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getStatus, "PENDING_PAYMENT")
                .lt(OrderInfo::getCreatedAt, deadline));
        for (OrderInfo order : timeout) {
            orderInfoMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                    .eq(OrderInfo::getId, order.getId())
                    .eq(OrderInfo::getStatus, "PENDING_PAYMENT")
                    .set(OrderInfo::getStatus, "CANCELLED"));
            releaseProductLock(getOrderProductId(order.getId()));
            notificationService.create(order.getBuyerId(), "ORDER", "订单超时取消", "订单号: " + order.getOrderNo());
            notificationService.create(order.getSellerId(), "ORDER", "订单超时取消", "订单号: " + order.getOrderNo());
        }
    }

    public void autoConfirmReceiptOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);
        List<OrderInfo> timeout = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getStatus, "PENDING_RECEIPT")
                .lt(OrderInfo::getShippedAt, deadline));
        for (OrderInfo order : timeout) {
            orderInfoMapper.update(null, new LambdaUpdateWrapper<OrderInfo>()
                    .eq(OrderInfo::getId, order.getId())
                    .eq(OrderInfo::getStatus, "PENDING_RECEIPT")
                    .set(OrderInfo::getStatus, "PENDING_REVIEW")
                    .set(OrderInfo::getReceivedAt, LocalDateTime.now()));
            notificationService.create(order.getBuyerId(), "ORDER", "系统已自动确认收货", "订单号: " + order.getOrderNo());
        }
    }

    public OrderInfo getOrderEntity(Long orderId) {
        return orderInfoMapper.selectById(orderId);
    }

    private void releaseProductLock(Long productId) {
        if (productId == null || productId <= 0) {
            return;
        }
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getSold, false)
                .set(Product::getStatus, "ON_SHELF"));
    }

    private void markProductSold(Long productId) {
        if (productId == null || productId <= 0) {
            return;
        }
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .set(Product::getSold, true)
                .set(Product::getStatus, "SOLD")
                .setSql("sales_count = COALESCE(sales_count, 0) + 1"));
    }

    private void clearProductFromAllCarts(Long productId) {
        if (productId == null || productId <= 0) {
            return;
        }
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getProductId, productId));
    }

    private CartItemDto toCartItemDto(Cart cart) {
        Product p = productMapper.selectById(cart.getProductId());
        if (p == null) {
            return new CartItemDto(
                    cart.getProductId(),
                    "商品不存在",
                    java.math.BigDecimal.ZERO,
                    cart.getQuantity(),
                    cart.getSelected(),
                    "NOT_FOUND",
                    false,
                    "商品已删除"
            );
        }
        boolean valid = !Boolean.TRUE.equals(p.getSold()) && "ON_SHELF".equals(p.getStatus());
        String reason = valid ? "" : ("SOLD".equals(p.getStatus()) || Boolean.TRUE.equals(p.getSold()) ? "商品已售出" : "商品已下架");
        return new CartItemDto(
                p.getId(),
                p.getTitle(),
                p.getPrice(),
                cart.getQuantity(),
                cart.getSelected(),
                p.getStatus(),
                valid,
                reason
        );
    }

    private Long getOrderProductId(Long orderId) {
        OrderItem item = orderItemMapper.selectOne(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .last("limit 1"));
        return item == null ? 0L : item.getProductId();
    }

    private OrderDto toDto(OrderInfo order, Long productId) {
        return new OrderDto(order.getId(), order.getOrderNo(), productId,
                order.getBuyerId(), order.getSellerId(), order.getPaidAmount(), order.getStatus());
    }
}
