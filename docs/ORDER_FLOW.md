# Order and Checkout Flow

This document describes how an order is created, paid for at checkout, fulfilled, cancelled, and how product stock is synchronized between the Order Service and the Product Service.

## 1. Overview

Orders are owned by the **Order Service**. Products are owned by the **Product Service**. The Order Service calls the Product Service through a Feign client to validate products and read current product data.

All requests enter through the API Gateway and are authenticated with JWT. Authorization rules are enforced in the security configuration and in the Order Service ownership checks.

```text
Angular frontend
      |
      v
 API Gateway
      |
      +-------------------+
      |                   |
      v                   v
Order Service        Product Service
      |                   ^
      |  Feign: getProduct|
      +-------------------+
      |
      | Kafka: order-stock-events
      +-------------------> Product Service
```

## 2. Actors and Permissions

| Actor | Description | Main order permissions |
|---|---|---|
| `CLIENT` | Buyer | Create orders, view own orders, cancel own `PENDING`/`CONFIRMED` orders, redo or delete own `CANCELLED`/`DELIVERED` orders |
| `SELLER` | Product owner | View orders containing own products, cancel `PENDING`/`CONFIRMED` orders, advance status |
| Guest | Not authenticated | Cannot create or view orders |

The frontend hides actions the current role cannot perform, but the backend always re-checks ownership and role.

## 3. Order Data Model

An order is stored in MongoDB in the `orders` collection.

```json
{
  "id": "order-id",
  "checkoutId": "shared-checkout-id",
  "buyerId": "buyer-user-id",
  "sellerId": "seller-user-id",
  "status": "PENDING",
  "items": [
    {
      "productId": "product-id",
      "sellerId": "seller-user-id",
      "name": "Product name",
      "imageUrl": "media-id",
      "price": 99.99,
      "quantity": 1
    }
  ],
  "total": 99.99,
  "address": {
    "fullName": "Ada Lovelace",
    "phone": "+1 555 0100",
    "line1": "1 Main St",
    "line2": null,
    "city": "Springfield",
    "state": "IL",
    "postalCode": "62701",
    "country": "USA"
  },
  "createdAt": "2026-01-01T10:00:00Z",
  "updatedAt": "2026-01-01T10:00:00Z"
}
```

Notes:

- An order always belongs to exactly one seller. A cart containing products from multiple sellers produces multiple orders.
- `checkoutId` groups all orders created by the same cart checkout. Buy Now orders have no `checkoutId`.
- `total` is calculated by the Order Service as `sum(item.price * item.quantity)` using the price stored on each item.
- Item price, name, and image are snapshots taken when the order is created.
- Item `sellerId` may be different from the order `sellerId` only for legacy data; new orders keep them consistent.

## 4. Delivery Address

Buy Now and cart checkout require a delivery address. The address is validated before the order is created.

Required fields:

| Field | Validation |
|---|---|
| `fullName` | Non-blank, max 120 characters |
| `phone` | Non-blank, max 30 characters |
| `line1` | Non-blank, max 200 characters |
| `city` | Non-blank, max 100 characters |
| `postalCode` | Non-blank, max 20 characters |
| `country` | Non-blank, max 100 characters |

Optional fields:

| Field | Validation |
|---|---|
| `line2` | Max 200 characters |
| `state` | Max 100 characters |

The frontend uses a reusable Material dialog:
`frontend/src/app/features/orders/dialogs/delivery-address-form/`.

## 5. Creating an Order

### 5.1 Buy Now

Endpoint: `POST /orders/buy`

1. The frontend opens the delivery address dialog.
2. The user submits a product ID, quantity, and address.
3. `OrderService.buyNow` validates the request.
4. The Order Service fetches the product from the Product Service using Feign.
5. It validates:
   - the product exists,
   - the product has a price,
   - the requested quantity is available,
   - the seller and product name are available.
6. The service creates one `PENDING` order with one item.
7. Creating the order immediately reserves its stock through Kafka.
8. The response is the created order. The frontend navigates to `/orders/:id`.

Buy Now requires the delivery address.

### 5.2 Cart Checkout

Endpoint: `POST /cart/checkout`

1. The frontend opens the delivery address dialog.
2. The request is sent with `{ "address": { ... } }`.
3. `CartService.checkoutItems` loads the user's cart.
4. Each cart item is re-validated against the current Product Service data.
5. If any issue is found, checkout stops before creating orders and returns:
   - `valid: false`
   - a list of issues
   - the updated cart
   - an empty `orders` list

Possible issues:

| Issue type | Behavior |
|---|---|
| `PRODUCT_UNAVAILABLE` | Product is removed from the cart |
| `INSUFFICIENT_STOCK` | Checkout is blocked |
| `PRICE_CHANGED` | Cart item price is updated, checkout is blocked until retried |
| `NAME_CHANGED` | Cart item name is updated, checkout is blocked until retried |
| `SELLER_CHANGED` | Cart item seller is updated, checkout is blocked until retried |
| `IMAGE_CHANGED` | Cart item image is updated, checkout is blocked until retried |

When there are no issues:

1. Cart items are grouped by `sellerId`.
2. One `PENDING` order is created per seller.
3. All orders from the same checkout share a generated `checkoutId`.
4. Creating each order immediately reserves its stock through Kafka.
5. The cart is cleared and saved.
5. The response returns `valid: true` and the created `orders`.

The frontend shows the created orders and links to the order list/detail pages.

### 5.3 Direct Order Creation

Endpoint: `POST /orders`

This existing endpoint accepts a `CreateOrderRequest` containing `checkoutId`, `sellerId`, items, and an optional address. All items must belong to the same seller. Cart checkout reuses the same internal `createOrderForBuyer` path.

## 6. Order Status Lifecycle

```text
                 seller confirms              seller ships              seller delivers
 PENDING ---------------------> CONFIRMED ---------------------> SHIPPED ---------------------> DELIVERED
    |                              |
    | buyer or seller cancels      | buyer or seller cancels
    v                              v
 CANCELLED <----------------------+
    |
    | buyer redoes
    v
 PENDING (new order)
```

Status values:

| Status | Meaning |
|---|---|
| `PENDING` | Order placed, waiting for the seller |
| `CONFIRMED` | Seller confirmed the order and is preparing it |
| `SHIPPED` | Order left the seller |
| `DELIVERED` | Order was delivered |
| `CANCELLED` | Order will not be fulfilled |

Allowed status transitions:

| Current | Next | Who |
|---|---|---|
| `PENDING` | `CONFIRMED` | Seller of the order |
| `CONFIRMED` | `SHIPPED` | Seller of the order |
| `SHIPPED` | `DELIVERED` | Seller of the order |

Any other forward transition is rejected with HTTP `409 Conflict`.

## 7. Buyer and Seller Actions

### Cancel Order

`POST /orders/{orderId}/cancel`

- Allowed for the buyer or the seller of the order.
- Allowed only when the order is `PENDING` or `CONFIRMED`.
- Sets the status to `CANCELLED`.
- Releases the stock reserved when the order was created.

### Advance Status

`PATCH /orders/{orderId}/status`

Request body:

```json
{ "status": "CONFIRMED" }
```

- Seller only.
- The seller must own the order or one of its items.
- Only the next status in the sequence is allowed.
- Status changes do not modify stock; stock is already reserved when the order is created.

### Redo Order

`POST /orders/{orderId}/redo`

- Buyer only.
- Allowed only for `CANCELLED` or `DELIVERED` orders.
- Creates a new `PENDING` order with copied items, address, total, and `checkoutId`.
- Reserves stock for the new order.
- The original order is kept unchanged.

### Delete Order

`DELETE /orders/{orderId}`

- Buyer only.
- Allowed only for `CANCELLED` or `DELIVERED` orders.
- Removes the order from the buyer's list.
- Does not change stock.
  - A `CANCELLED` order already released its reservation when it was cancelled.
  - A `DELIVERED` order already consumed its reserved stock.

### View Orders

| Endpoint | Description |
|---|---|
| `GET /orders/me` | Buyer's own orders |
| `GET /orders/seller` | Seller's orders; seller role required |
| `GET /orders/{orderId}` | Single order; buyer or seller of the order only |

List endpoints support:

- `search` (max 200 characters)
- `status`
- `from` / `to` dates in ISO format (`YYYY-MM-DD`)
- `page` (zero-based)
- `size` (1 to 50)

Results are sorted by `createdAt` descending.

## 8. Stock Synchronization with Kafka

The Order Service publishes stock events after the order state is persisted. The Product Service consumes them and adjusts product quantities atomically.

Topic: `order-stock-events`

Producer: Order Service

Consumer group: `product-service-group`

Event schema:

```json
{
  "orderId": "order-id",
  "action": "RESERVE",
  "items": [
    { "productId": "product-id", "quantity": 2 }
  ]
}
```

`action` is either `RESERVE` or `RELEASE`.

### Stock Rules

| Order event | Kafka action | Product stock effect |
|---|---|---|
| Order is created as `PENDING` | `RESERVE` | Decrease stock by item quantity |
| `PENDING` order is cancelled | `RELEASE` | Increase stock by item quantity |
| `CONFIRMED` order is cancelled | `RELEASE` | Increase stock by item quantity |
| Status changes to `CONFIRMED` | none | No change; stock was already reserved at order creation |
| `CANCELLED` order is deleted | none | No change; stock was already released on cancellation |
| `DELIVERED` order is deleted | none | No change; delivered stock is consumed |
| Order is redone | `RESERVE` | The new `PENDING` order reserves stock |

### Product Service Handling

1. `OrderStockListener` receives the event.
2. It applies the item changes sequentially.
3. `ProductRepositoryPort.adjustStock(productId, delta)` is used:
   - `RESERVE` passes a negative delta.
   - `RELEASE` passes a positive delta.
4. `MongoProductAdapter.adjustStock` uses an atomic MongoDB `findAndModify`:
   - For negative deltas, the query requires `quantity >= requested quantity`.
   - The update applies `$inc` to `quantity`.
   - If the condition does not match, no product is updated.
5. The listener logs each result and continues with the remaining items.

This design prevents overselling during concurrent reservations because the stock check and decrement happen in one atomic database operation.

## 9. API Summary

| Method | Endpoint | Auth | Purpose |
|---|---|---|---|
| `POST` | `/orders` | Buyer | Create an order directly |
| `POST` | `/orders/buy` | Buyer | Buy Now with address |
| `GET` | `/orders/me` | Buyer | List own orders |
| `GET` | `/orders/seller` | Seller | List seller orders |
| `GET` | `/orders/{orderId}` | Buyer/Seller | Get one order |
| `PATCH` | `/orders/{orderId}/status` | Seller | Advance status |
| `POST` | `/orders/{orderId}/cancel` | Buyer/Seller | Cancel order |
| `POST` | `/orders/{orderId}/redo` | Buyer | Redo cancelled/delivered order |
| `DELETE` | `/orders/{orderId}` | Buyer | Delete cancelled/delivered order |
| `POST` | `/cart/items` | Buyer | Add item to cart |
| `GET` | `/cart/items` | Buyer | Get cart |
| `DELETE` | `/cart/items/{productId}` | Buyer | Remove item from cart |
| `POST` | `/cart/checkout` | Buyer | Validate cart, create orders, clear cart |

## 10. Frontend Flow

Relevant routes:

| Route | Page |
|---|---|
| `/cart` | Cart and checkout |
| `/orders` | Buyer or seller order list |
| `/orders/:id` | Order detail |

Buy Now:

1. Product detail page checks authentication.
2. Guest is redirected to `/login`.
3. Authenticated buyer opens the delivery address dialog.
4. On submit, `OrderService.buyNow(productId, quantity, address)` is called.
5. Success navigates to the created order detail.

Cart checkout:

1. Cart page opens the delivery address dialog.
2. On submit, `CartService.checkout(address)` is called.
3. If `valid` is false, the page shows the returned issues.
4. If `valid` is true, the cart clears and the success message links to the orders.

Order list:

- `CLIENT` loads `GET /orders/me`.
- `SELLER` loads `GET /orders/seller`.
- Filters trigger a debounced reload.
- Available actions depend on role and status.

Order detail:

- Loads `GET /orders/{orderId}`.
- Shows status timeline, items, total, and delivery address.
- Sellers can advance the order status.
- Buyers and sellers can cancel when allowed.
- Buyers can redo or remove cancelled/delivered orders.

## 11. Error Handling

Order endpoints return a consistent error body:

```json
{
  "code": "ORDER_INVALID_STATE",
  "message": "Only pending or confirmed orders can be cancelled",
  "details": []
}
```

Common codes:

| HTTP status | Code | Meaning |
|---|---|---|
| `400` | `INVALID_ORDER` | Invalid order data |
| `400` | `VALIDATION_ERROR` | Bean validation failed |
| `400` | `INVALID_ORDER_QUERY` | Invalid pagination, date range, or search |
| `403` | `ORDER_ACCESS_DENIED` | Wrong role or not the order owner |
| `404` | `ORDER_NOT_FOUND` | Order does not exist |
| `409` | `ORDER_INVALID_STATE` | Status transition is not allowed |
| `500` | `INTERNAL_ERROR` | Unexpected server error |

## 12. Key Source Files

Backend:

- `backend/order-service/src/main/java/com/buy01/order/order/controller/OrderController.java`
- `backend/order-service/src/main/java/com/buy01/order/order/service/OrderService.java`
- `backend/order-service/src/main/java/com/buy01/order/cart/controller/CartController.java`
- `backend/order-service/src/main/java/com/buy01/order/cart/service/CartService.java`
- `backend/order-service/src/main/java/com/buy01/order/order/model/entity/Order.java`
- `backend/order-service/src/main/java/com/buy01/order/order/model/entity/DeliveryAddress.java`
- `backend/order-service/src/main/java/com/buy01/order/order/messaging/OrderStockEventPublisher.java`
- `backend/product-service/src/main/java/com/buy01/product/infrastructure/messaging/OrderStockListener.java`
- `backend/product-service/src/main/java/com/buy01/product/infrastructure/persistence/mongo/MongoProductAdapter.java`

Frontend:

- `frontend/src/app/core/services/order.service.ts`
- `frontend/src/app/core/services/cart.service.ts`
- `frontend/src/app/features/orders/order-list/`
- `frontend/src/app/features/orders/order-detail/`
- `frontend/src/app/features/orders/dialogs/delivery-address-form/`
- `frontend/src/app/features/products/product-detail/`
- `frontend/src/app/features/cart/cart-page/`

## 13. Verification

The flow is covered by:

- `OrderServiceSpec` for creation, status transitions, cancellation, redo, deletion, and stock-event calls.
- `CartServiceSpec` for cart validation and checkout grouping.
- `OrderStockListenerTest` for reserve/release handling.
- `MongoProductAdapterTest` for atomic stock adjustment.
- Frontend specs for Buy Now, cart checkout, address dialog, order list, and order detail.
