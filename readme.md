# 🛒 E-Commerce Microservices Platform

A production-grade e-commerce backend built with **Java 17**, **Spring Boot 3**, and **Microservices Architecture** — featuring event-driven notifications, JWT security, Razorpay payments and atomic inventory management.

---

## 📦 Microservices

| Service                   | Description            | Port     |
|---------------------------|------------------------|----------|
| `frontent-vercel`         | React | [link](https://eccomerce-frontent1.vercel.app/) |
| `eccomerce_spring`        | Auth, Products, Users  | 8080     |
| `eccomerce_orderhandling` | Cart, Orders, Payments | 8082     |
| `eccomerce_notification`  | Email Notifications    | 8081     ||

---

## 🏗️ Architecture

```
Client (Web / Mobile)
        ↓ REST API + JWT
┌─────────────────────┐
│  eccomerce_spring   │  ← Auth, Products
│  (Render)           │
└─────────────────────┘
        ↓ HTTP (Internal Service Key)
┌──────────────────────────┐
│ eccomerce_orderhandling  │  ← Cart, Orders, Payments
│ (Render)                 │
└──────────────────────────┘
        ↓ Kafka Events
┌──────────────────────────┐
│ eccomerce_notification   │  ← Email Notifications
│ (Render)                 │
└──────────────────────────┘

Shared Infrastructure:
→ MongoDB Atlas  (Database)
→ Apache Kafka   (Event Streaming)
→ Redis Cloud    (OTP Storage)
→ Razorpay       (Payment Gateway)
```

---

## ✨ Features

### 🔐 Security
- JWT authentication with **access tokens (15 min)** and **refresh token rotation (7 days)**
- Role-Based Access Control (**ADMIN / USER**)
- Microservice-to-microservice auth via **Internal Service Key**
- API rate limiting using **Bucket4j Token Bucket** algorithm
- **Redis OTP** system with TTL, brute force protection and resend cooldown

### 📦 Order Management
- Complete order lifecycle: **Cart → Payment → Confirmation → Shipping → Delivery**
- **Atomic stock management** using MongoDB `findAndModify` preventing overselling
- **Idempotency keys** preventing duplicate orders on double-click
- **Compensating transactions** restoring inventory on payment failures
- Full order timeline tracking with status history

### 💳 Payment
- **Razorpay** payment gateway integration
- **HMAC-SHA256** webhook signature verification preventing fake payment attacks
- Automatic refund initiation on order cancellation
- Test mode support for development

### 📧 Notification System
- **Event-driven** async email notifications via Apache Kafka
- **Dead Letter Queue (DLQ)** with exponential backoff retry (2s → 4s → 8s)
- **Thymeleaf HTML** email templates for all notification types
- Bulk campaign email system for promotional offers

### 🚀 Performance
- MongoDB **compound indexes** for 100x faster queries
- **Parallel stock checks** using Java parallel streams
- Database connection pooling
- Pagination on all list endpoints

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Security | Spring Security 6, JWT (JJWT) |
| Database | MongoDB Atlas |
| Cache | Redis Cloud |
| Messaging | Apache Kafka (RedPanda Cloud) |
| Payment | Razorpay |
| Email | SendGrid / Resend |
| Containerization | Docker (Multi-stage builds) |
| Deployment | Render |
| Build Tool | Maven |

---

## 📡 API Endpoints

### Auth Service (`eccomerce_spring`)
```
POST /api/auth/register     → Register user
POST /api/auth/login        → Login with password
POST /api/auth/send-otp     → Send OTP to email
POST /api/auth/verify-otp   → Verify OTP + get token
POST /api/auth/refresh      → Refresh access token
POST /api/auth/logout       → Logout
POST /api/auth/make-admin   → Promote user to ADMIN
```

### Product Service (`eccomerce_spring`)
```
GET  /api/products          → Get all products
GET  /api/products/:id      → Get product by ID
GET  /api/products/category/:name → Get by category
POST /api/products          → Add product (ADMIN)
PUT  /api/products/:id      → Update product (ADMIN)
DELETE /api/products/:id    → Delete product (ADMIN)
```

### Cart Service (`eccomerce_orderhandling`)
```
GET    /api/cart            → Get user cart
POST   /api/cart/add        → Add item to cart
PUT    /api/cart/update     → Update item quantity
DELETE /api/cart/clear      → Clear cart
```

### Order Service (`eccomerce_orderhandling`)
```
POST /api/orders/place              → Place order
GET  /api/orders                    → Get my orders
GET  /api/orders/:id                → Get order details
PUT  /api/orders/:id/cancel         → Cancel order
GET  /api/orders/admin/all          → All orders (ADMIN)
PUT  /api/orders/admin/:id/status   → Update status (ADMIN)
```

### Payment Service (`eccomerce_orderhandling`)
```
POST /api/payment/verify    → Verify payment
POST /api/payment/webhook   → Razorpay webhook
POST /api/payment/refund/:id → Initiate refund
```

---

## 🔔 Notification Types

| Event | Trigger | Email |
|---|---|---|
| `ORDER_PLACED` | Order confirmed after payment | Order confirmation with items |
| `ORDER_SHIPPED` | Admin marks as shipped | Shipping notification with tracking |
| `ORDER_DELIVERED` | Admin marks as delivered | Delivery confirmation |
| `ORDER_CANCELLED` | User/system cancels | Cancellation with reason |
| `WELCOME` | New user registered | Welcome email |
| `BIG_OFFER` | Admin sends campaign | Promotional offer email |
| `CAMPAIGN_BLAST` | Bulk email to users | Custom HTML campaign |

---

## 🚀 Getting Started

### Prerequisites
```
Java 17+
Maven 3.8+
Docker
MongoDB Atlas account
RedPanda Cloud account (Kafka)
Redis Cloud account
Razorpay account (test mode)
```

### Clone Repositories
```bash
git clone https://github.com/SubhamBhowmik/eccomerce_spring.git
git clone https://github.com/SubhamBhowmik/eccomerce_orderhandling.git
git clone https://github.com/SubhamBhowmik/eccomerce_notification.git
```

### Environment Variables

Create `.env` file in each project (never commit to GitHub):

**eccomerce_spring:**
```properties
MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/ecommerce
JWT_SECRET=your_secret_min_32_chars
JWT_ACCESS_TOKEN_EXPIRY=900000
JWT_REFRESH_TOKEN_EXPIRY=604800000
INTERNAL_SERVICE_KEY=your_internal_key
KAFKA_BOOTSTRAP_SERVERS=your_kafka_url
KAFKA_SASL_JAAS_CONFIG=your_jaas_config
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your_app_password
REDIS_HOST=your_redis_host
REDIS_PORT=your_redis_port
REDIS_PASSWORD=your_redis_password
REDIS_SSL=true
```

**eccomerce_orderhandling:**
```properties
MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/orderdb
JWT_SECRET=your_secret_min_32_chars
INTERNAL_SERVICE_KEY=your_internal_key
KAFKA_BOOTSTRAP_SERVERS=your_kafka_url
KAFKA_SASL_JAAS_CONFIG=your_jaas_config
RAZORPAY_KEY_ID=rzp_test_xxx
RAZORPAY_KEY_SECRET=your_secret
ECOMMERCE_SERVICE_URL=http://localhost:8080
```

**eccomerce_notification:**
```properties
MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/ecommerce
KAFKA_BOOTSTRAP_SERVERS=your_kafka_url
KAFKA_SASL_JAAS_CONFIG=your_jaas_config
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your_app_password
```

### Run Locally

```bash
# Terminal 1 - Main service
cd eccomerce_spring
mvn spring-boot:run

# Terminal 2 - Order service
cd eccomerce_orderhandling
mvn spring-boot:run

# Terminal 3 - Notification service
cd eccomerce_notification
mvn spring-boot:run
```

### Run with Docker

```bash
# Build image
docker build -t eccomerce-spring .

# Run container
docker run -p 8080:8080 \
  --env-file .env \
  eccomerce-spring
```

---

## 🧪 Testing

### Health Checks
```bash
curl https://eccomerce-spring.onrender.com/ping
curl https://eccomerce-orderhandling.onrender.com/ping
curl https://eccomerce-notification.onrender.com/ping
```

### Register & Login
```bash
# Register
curl -X POST https://eccomerce-spring.onrender.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"John","email":"john@gmail.com","password":"Test@1234"}'

# Login
curl -X POST https://eccomerce-spring.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@gmail.com","password":"Test@1234"}'
```

### Place Order Flow
```bash
# 1. Add to cart
curl -X POST https://eccomerce-orderhandling.onrender.com/api/cart/add \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":"xxx","productName":"iPhone 14","quantity":1,"price":69999}'

# 2. Place order
curl -X POST https://eccomerce-orderhandling.onrender.com/api/orders/place \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"username":"John","shippingAddress":"Mumbai","idempotencyKey":"order-001"}'
```

---

## 🔒 Security Architecture

```
Rate Limiting (Bucket4j):
→ Login:    5 requests/min  per IP
→ Register: 3 requests/hour per IP
→ Orders:   5 requests/min  per user
→ Cart:     30 requests/min per user
→ General:  100 requests/min per user

JWT Flow:
→ Access Token:  15 minutes (in-memory)
→ Refresh Token: 7 days (MongoDB + rotation)
→ Logout:        deletes refresh token

Service Auth:
→ X-Internal-Service-Key header
→ ROLE_SERVICE granted on match
→ Prevents unauthorized stock access
```

---

## 📊 Scalability

| Orders/Day | Infrastructure | Cost |
|---|---|---|
| 0 - 500 | Current free setup | $0/mo |
| 500 - 1000 | + SendGrid email | $20/mo |
| 1000 - 5000 | + MongoDB M2 + Redis | $30/mo |
| 5000 - 10000 | + Multiple instances + LB | $140/mo |
| 10000+ | AWS/GCP + Auto scaling | $300+/mo |

---

## 🗂️ Project Structure

```
eccomerce_spring/
├── config/         → JWT, Security, Redis, Rate Limiter
├── controller/     → Auth, Product, Health
├── service/        → Auth, Product, OTP, Publisher
├── model/          → User, Product, RefreshToken
├── repository/     → MongoDB repositories
└── event/          → NotificationEvent, NotificationType

eccomerce_orderhandling/
├── config/         → JWT, Security, Razorpay
├── controller/     → Cart, Order, Payment, Health
├── service/        → Cart, Order, Payment, Publisher
├── model/          → Order, Cart, OrderItem
├── repository/     → MongoDB repositories
└── exception/      → Global exception handler

eccomerce_notification/
├── config/         → Kafka config
├── consumer/       → NotificationConsumer
├── service/        → EmailService, Router, Logger
├── model/          → NotificationLog
├── event/          → NotificationEvent
└── templates/      → Thymeleaf email templates
```

---

## 👨‍💻 Author

**Subham Bhowmik**
- GitHub: [@SubhamBhowmik](https://github.com/SubhamBhowmik)
