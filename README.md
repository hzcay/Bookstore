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

## API Endpoints

### Books

- `GET /api/v1/books` - Lấy danh sách sách
- `GET /api/v1/books/search` - Tìm kiếm sách
- `GET /api/v1/books/{id}` - Lấy chi tiết sách
- `POST /api/v1/books` - Tạo sách mới
- `PUT /api/v1/books/{id}` - Cập nhật sách
- `DELETE /api/v1/books/{id}` - Xóa sách

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

### Reports

- `GET /api/v1/reports/sales` - Báo cáo doanh thu
- `GET /api/v1/reports/inventory` - Báo cáo tồn kho
- `GET /api/v1/reports/suppliers-debt` - Báo cáo công nợ nhà cung cấp

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
