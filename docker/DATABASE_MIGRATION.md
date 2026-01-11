# Hướng dẫn chuyển đổi Database từ SQL Server sang MySQL

## 📋 Lưu ý quan trọng

Để deploy lên VPS với cấu hình 2GB RAM, **bắt buộc phải chuyển từ SQL Server sang MySQL** vì:
- SQL Server cần tối thiểu 1GB RAM
- MySQL chỉ cần ~200-300MB RAM
- VPS 2GB RAM không đủ cho SQL Server + Spring Boot app

## 🔄 Bước 1: Cập nhật pom.xml

Mở file `demo/pom.xml` và thay đổi dependency:

### XÓA dòng này (dòng 51-55):
```xml
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <scope>runtime</scope>
</dependency>
```

### THÊM dòng này:
```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

## 🔄 Bước 2: Cập nhật application.properties

File `application-prod.properties` đã được tạo sẵn với cấu hình MySQL.

Nếu muốn test local với MySQL, thêm vào `application.properties`:

```properties
# MySQL Configuration (for local testing)
spring.datasource.url=jdbc:mysql://localhost:3306/DigitalLibrary?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

## 📊 Bước 3: Migration dữ liệu (nếu có dữ liệu cũ)

Nếu bạn đã có dữ liệu trong SQL Server và muốn chuyển sang MySQL:

### 3.1. Export từ SQL Server

```bash
# Sử dụng SQL Server Management Studio
# Tools > Data Migration Assistant
# Hoặc dùng mysqldump equivalent cho SQL Server
```

### 3.2. Import vào MySQL

```bash
# Sau khi deploy Docker, import vào MySQL container
docker exec -i digitallibrary_mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD DigitalLibrary < backup.sql
```

## ✅ Bước 4: Kiểm tra

1. **Chạy ứng dụng local** (nếu test):
   - Đảm bảo MySQL đang chạy
   - Run application với profile `prod`
   - Kiểm tra kết nối database

2. **Deploy lên VPS**:
   - Docker Compose sẽ tự động tạo database và schema
   - Hibernate sẽ tự động tạo tables với `ddl-auto=update`

## 🔍 So sánh cấu hình

| Feature | SQL Server | MySQL |
|---------|-----------|-------|
| RAM yêu cầu | ~1GB | ~200-300MB |
| Port mặc định | 1433 | 3306 |
| Driver class | `com.microsoft.sqlserver.jdbc.SQLServerDriver` | `com.mysql.cj.jdbc.Driver` |
| Dialect | `SQLServerDialect` | `MySQL8Dialect` |
| URL format | `jdbc:sqlserver://host:port;databaseName=db` | `jdbc:mysql://host:port/db?params` |

## ⚠️ Lưu ý về dữ liệu

- **NVARCHAR(MAX)** trong SQL Server → **TEXT** hoặc **LONGTEXT** trong MySQL
- Hibernate sẽ tự động xử lý phần lớn migration
- Kiểm tra lại các column có `NVARCHAR(MAX)` và đảm bảo entity đúng

## 🚀 Sau khi chuyển đổi

1. Build lại project: `mvn clean package`
2. Test local (nếu cần)
3. Deploy lên VPS với Docker Compose
4. Kiểm tra logs để đảm bảo không có lỗi
