# Ứng Dụng Quản Lý Và Bán Sách Trên Nền Web

## Mô tả dự án

Hệ thống quản lý và bán sách được xây dựng bằng Spring Boot + SQL Server, hỗ trợ quản lý toàn bộ hoạt động kinh doanh của cửa hàng sách.

## Công nghệ sử dụng

- **Backend**: Spring Boot 3.5.6
- **Database**: SQL Server
- **ORM**: Spring Data JPA + Hibernate
- **Security**: Spring Security
- **Build Tool**: Maven
- **Java Version**: 17

## Cấu trúc dự án

```
src/main/java/com/example/Bookstore/
├── entity/          # Các Entity classes
├── repository/      # Repository interfaces
├── service/         # Service interfaces và implementations
├── controller/      # REST Controllers
├── dto/            # Data Transfer Objects
├── config/         # Configuration classes
├── util/           # Utility classes
└── BookstoreApplication.java
```

## Cài đặt và chạy

### Yêu cầu hệ thống

- Java 17+
- Maven 3.6+
- SQL Server 2019+
- Docker >= 24.x
- Docker Compose >= 2.x

## Cách chạy

```
docker compose up -d --build
```

## Kiểm tra container

```
docker ps
```

## Dừng container

```
docker compose down
```

## Xem log

```
docker compose logs -f
```

## Khởi động lại

```
docker compose restart
```

Ứng dụng sẽ chạy tại: `http://localhost:8080`

## API Docs (Swagger / OpenAPI)

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Đã cấu hình với springdoc-openapi:

- Dependency: `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0`
- Mở quyền trong `SecurityConfig` cho: `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`

Khắc phục sự cố phổ biến:

- 403 khi truy cập Swagger: đảm bảo `SecurityConfig` đã permitAll các đường dẫn trên.
- 500 khi load `/v3/api-docs`: chạy `mvn clean install`, dùng springdoc ≥ 2.7.0 (tương thích Spring Boot 3.5.x).

## Giao diện Admin (Thymeleaf)

Truy cập giao diện quản trị tại: `http://localhost:8080/admin/dashboard`

### Công nghệ giao diện

- **Thymeleaf**: Template engine server-side rendering
- **Bootstrap 5**: CSS framework
- **Bootstrap Icons**: Icon library
- **Spring MVC**: Xử lý form và routing

### Các trang quản lý

- **Dashboard** (`/admin/dashboard`): Tổng quan hệ thống với thống kê nhanh
  - Doanh thu tháng hiện tại
  - Số đơn hàng
  - Tổng khách hàng
  - Cảnh báo sách tồn kho thấp
  - Danh sách nhà cung cấp có công nợ

- **Quản lý Sách** (`/admin/books`): 
  - Danh sách sách với phân trang
  - Thêm/Sửa/Xóa sách
  - Tìm kiếm sách
  - Form validation

- **Quản lý Khách hàng** (`/admin/customers`): 
  - CRUD khách hàng
  - Quản lý điểm tích lũy
  - Quản lý trạng thái xác thực

- **Quản lý NCC** (`/admin/suppliers`): 
  - CRUD nhà cung cấp
  - Theo dõi công nợ
  - Cập nhật công nợ

- **Quản lý Nhân viên** (`/admin/employees`): 
  - CRUD nhân viên
  - Phân quyền theo vai trò (ADMIN, MANAGER, CASHIER, WAREHOUSE, SHIPPER)
  - Quản lý trạng thái

- **Báo cáo Thống kê** (`/admin/reports`): 
  - Tổng doanh thu theo khoảng thời gian
  - Số đơn hàng
  - Giá trị trung bình đơn hàng
  - Báo cáo tồn kho
  - Báo cáo công nợ nhà cung cấp

### Ưu điểm của Thymeleaf

- ✅ **Server-side rendering**: Logic xử lý tập trung ở backend
- ✅ **Bảo mật**: Không expose API endpoints cho client
- ✅ **SEO-friendly**: HTML được render sẵn
- ✅ **Tích hợp tốt với Spring**: Dễ dàng binding form, validation
- ✅ **Không cần JavaScript framework**: Đơn giản hơn SPA
- ✅ **Session management**: Quản lý phiên đăng nhập dễ dàng

## API Endpoints

### Books

- `GET /api/v1/books` - Lấy danh sách sách (có phân trang)
- `GET /api/v1/books/search` - Tìm kiếm sách
- `GET /api/v1/books/{id}` - Lấy chi tiết sách
- `POST /api/v1/books` - Tạo sách mới
- `PUT /api/v1/books/{id}` - Cập nhật sách
- `DELETE /api/v1/books/{id}` - Xóa sách
- `GET /api/v1/books/low-stock` - Lấy sách tồn kho thấp

### Orders

- `GET /api/v1/orders` - Lấy danh sách đơn hàng
- `GET /api/v1/orders/{id}` - Lấy chi tiết đơn hàng
- `POST /api/v1/orders` - Tạo đơn hàng mới
- `PUT /api/v1/orders/{id}` - Cập nhật đơn hàng
- `POST /api/v1/orders/{id}/confirm` - Xác nhận đơn hàng
- `POST /api/v1/orders/{id}/cancel` - Hủy đơn hàng

### Customers

- `GET /api/v1/customers` - Lấy danh sách khách hàng
- `GET /api/v1/customers/{id}` - Lấy chi tiết khách hàng
- `POST /api/v1/customers` - Tạo khách hàng mới
- `PUT /api/v1/customers/{id}` - Cập nhật khách hàng
- `DELETE /api/v1/customers/{id}` - Xóa khách hàng
- `POST /api/v1/customers/register` - Đăng ký khách hàng
- `POST /api/v1/customers/verify-otp` - Xác thực OTP

### Employees

- `GET /api/v1/employees` - Lấy danh sách nhân viên
- `GET /api/v1/employees/{id}` - Lấy chi tiết nhân viên
- `POST /api/v1/employees` - Tạo nhân viên mới
- `PUT /api/v1/employees/{id}` - Cập nhật nhân viên
- `DELETE /api/v1/employees/{id}` - Xóa nhân viên
- `GET /api/v1/employees/active` - Lấy nhân viên đang hoạt động

### Suppliers

- `GET /api/v1/suppliers` - Lấy danh sách nhà cung cấp
- `GET /api/v1/suppliers/{id}` - Lấy chi tiết nhà cung cấp
- `POST /api/v1/suppliers` - Tạo nhà cung cấp mới
- `PUT /api/v1/suppliers/{id}` - Cập nhật nhà cung cấp
- `DELETE /api/v1/suppliers/{id}` - Xóa nhà cung cấp
- `PUT /api/v1/suppliers/{id}/debt` - Cập nhật công nợ

### Reports

- `GET /api/v1/reports/sales` - Báo cáo doanh thu
- `GET /api/v1/reports/inventory` - Báo cáo tồn kho
- `GET /api/v1/reports/suppliers-debt` - Báo cáo công nợ nhà cung cấp
- `GET /api/v1/reports/revenue` - Tính tổng doanh thu
- `GET /api/v1/reports/orders-count` - Đếm số đơn hàng
- `GET /api/v1/reports/average-order-value` - Giá trị trung bình đơn hàng

## Các tính năng chính

### Quản lý sách

- Thêm, sửa, xóa thông tin sách
- Tìm kiếm sách theo nhiều tiêu chí
- Quản lý tồn kho
- Cảnh báo sách sắp hết hàng

### Quản lý đơn hàng

- Tạo đơn hàng online và tại quầy
- Theo dõi trạng thái đơn hàng
- Tính toán chiết khấu và phí giao hàng
- Quản lý thanh toán

### Quản lý khách hàng

- Lưu trữ thông tin khách hàng
- Tích lũy điểm thưởng
- Lịch sử mua hàng

### Báo cáo

- Báo cáo doanh thu theo thời gian
- Báo cáo tồn kho
- Báo cáo công nợ nhà cung cấp

## Phân quyền người dùng

- **Khách hàng**: Xem sách, đặt hàng, theo dõi đơn hàng
- **Nhân viên**: Bán hàng, quản lý đơn hàng, nhập sách
- **Quản lý kho**: Quản lý tồn kho, nhà cung cấp
- **Shipper**: Quản lý giao hàng
- **Chủ cửa hàng**: Toàn quyền quản trị

## Database Schema

Hệ thống sử dụng 12 bảng chính:

- Books, Categories, Authors, Publishers
- Customers, Employees, Suppliers
- Orders, OrderItems, Inventory
- Shipments, Promotions

## Liên hệ

Dự án được phát triển cho mục đích học tập và nghiên cứu.
