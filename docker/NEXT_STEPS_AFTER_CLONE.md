# Các bước tiếp theo sau khi clone code

## ✅ Đã hoàn thành
- [x] Code đã clone vào `/opt/DigitalLibrary`

## 🚀 Các bước tiếp theo

### Bước 1: Kiểm tra cấu trúc code

```bash
cd /opt/DigitalLibrary
ls -la
ls -la demo/
ls -la docker/
```

Đảm bảo có các thư mục: `demo/`, `docker/`, và các file cần thiết.

### Bước 2: Kiểm tra và cập nhật pom.xml (QUAN TRỌNG!)

**Đây là bước QUAN TRỌNG nhất!** Phải chuyển từ SQL Server sang MySQL:

```bash
cd /opt/DigitalLibrary
nano demo/pom.xml
```

Tìm dòng này (khoảng dòng 51-55):
```xml
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <scope>runtime</scope>
</dependency>
```

**XÓA** dòng đó và **THÊM** dòng này:
```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

Sau đó:
- Nhấn `Ctrl + O` để Save
- Nhấn `Enter` để xác nhận
- Nhấn `Ctrl + X` để Exit

**Kiểm tra lại:**
```bash
grep -A 2 "mysql-connector" demo/pom.xml
```

Kết quả mong đợi: Thấy `mysql-connector-j` thay vì `mssql-jdbc`

### Bước 3: Tạo file .env

```bash
cd /opt/DigitalLibrary/docker
nano .env
```

Dán nội dung này (⚠️ QUAN TRỌNG: Đổi mật khẩu!):

```env
# Database Configuration
MYSQL_ROOT_PASSWORD=your_secure_root_password_123
MYSQL_DATABASE=DigitalLibrary
MYSQL_USER=digitallibrary
MYSQL_PASSWORD=your_secure_db_password_123

# Application Configuration
SPRING_PROFILES_ACTIVE=prod

# Google OAuth 2.0 (cập nhật với giá trị thực tế của bạn)
GOOGLE_OAUTH2_CLIENT_ID=509081543880-ssjdjuvpt9tbja3oo1lpucnhtihkrqd2.apps.googleusercontent.com
GOOGLE_OAUTH2_CLIENT_SECRET=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
GOOGLE_OAUTH2_REDIRECT_URI=https://digilibrary.online/oauth2/callback

# Domain
DOMAIN=digilibrary.online
```

**Lưu ý:**
- ⚠️ **ĐỔI** `your_secure_root_password_123` và `your_secure_db_password_123` thành mật khẩu mạnh!
- Đảm bảo Google OAuth credentials đúng

Save: `Ctrl + O` → `Enter` → `Ctrl + X`

### Bước 4: Route DNS trong Cloudflare (nếu chưa làm)

1. Vào [Cloudflare Dashboard](https://dash.cloudflare.com)
2. Chọn domain `digilibrary.online`
3. Vào **DNS** → **Records**
4. Tạo 2 CNAME records:

   **Record 1:**
   - Type: `CNAME`
   - Name: `@`
   - Target: `ea0c511c-5d3f-4114-93d3-afc3ab621052.cfargotunnel.com`
   - Proxy: **ON** (orange cloud ☁️)
   - TTL: Auto
   - Save

   **Record 2:**
   - Type: `CNAME`
   - Name: `www`
   - Target: `ea0c511c-5d3f-4114-93d3-afc3ab621052.cfargotunnel.com`
   - Proxy: **ON** (orange cloud ☁️)
   - TTL: Auto
   - Save

### Bước 5: Build và chạy Docker containers

```bash
cd /opt/DigitalLibrary/docker
docker-compose up -d --build
```

**Lần đầu build có thể mất 5-10 phút** (download images, build app).

**Đợi và xem logs:**
```bash
# Xem logs real-time
docker-compose logs -f
```

**Đợi đến khi thấy:**
- MySQL: `ready for connections`
- App: `Started DemoApplication` (hoặc tương tự)
- Không có lỗi đỏ

Nhấn `Ctrl + C` để thoát logs.

### Bước 6: Kiểm tra containers đang chạy

```bash
docker-compose ps
```

Kết quả mong đợi: Tất cả containers có status `Up`

### Bước 7: Kiểm tra app đang chạy trên port 8080

```bash
# Kiểm tra port
ss -lntp | grep 8080

# Test app
curl -I http://localhost:8080
```

Kết quả mong đợi: HTTP 200 OK

### Bước 8: Kiểm tra Cloudflared

Cloudflared sẽ tự động kết nối khi app chạy. Kiểm tra:

```bash
# Nếu dùng systemd service
systemctl status cloudflared

# Xem logs
journalctl -u cloudflared -n 50
```

### Bước 9: Test truy cập domain

Mở browser và truy cập: `https://digilibrary.online`

Kiểm tra:
- ✅ SSL certificate (HTTPS lock icon)
- ✅ Website load được
- ✅ Không có lỗi 502/503/504

### Bước 10: Cập nhật Google OAuth Redirect URI

1. Vào [Google Cloud Console](https://console.cloud.google.com)
2. APIs & Services → Credentials
3. Mở OAuth 2.0 Client ID của bạn
4. Thêm **Authorized redirect URIs**:
   - `https://digilibrary.online/oauth2/callback`
   - `https://www.digilibrary.online/oauth2/callback`
5. Save

---

## 🐛 Troubleshooting

### Lỗi: Build failed - "mssql-jdbc" not found

**Nguyên nhân**: Chưa cập nhật pom.xml

**Giải pháp**: Làm lại Bước 2, đảm bảo đã thay `mssql-jdbc` bằng `mysql-connector-j`

### Lỗi: Database connection failed

**Giải pháp**:
```bash
# Kiểm tra MySQL logs
docker-compose logs mysql

# Đợi MySQL khởi động (có thể mất 30-60 giây)
docker-compose logs -f mysql
```

### Lỗi: Port 8080 already in use

**Giải pháp**:
```bash
# Tìm process đang dùng port 8080
ss -lntp | grep 8080

# Stop containers
docker-compose down

# Start lại
docker-compose up -d
```

### App không accessible qua domain

1. Kiểm tra DNS: Đảm bảo CNAME records đã đúng
2. Kiểm tra Cloudflared: `systemctl status cloudflared`
3. Đợi DNS propagate (có thể mất 1-2 phút)

---

## ✅ Checklist cuối cùng

- [ ] Code đã clone vào `/opt/DigitalLibrary`
- [ ] pom.xml đã cập nhật (MySQL thay SQL Server)
- [ ] File `.env` đã tạo với mật khẩu mạnh
- [ ] DNS đã route trong Cloudflare
- [ ] Docker containers đã build và chạy
- [ ] App đang chạy trên port 8080
- [ ] Cloudflared đang chạy
- [ ] Website accessible qua `https://digilibrary.online`
- [ ] Google OAuth redirect URI đã cập nhật

---

## 📞 Nếu gặp vấn đề

Xem logs chi tiết:
```bash
# App logs
docker-compose logs app | tail -100

# MySQL logs
docker-compose logs mysql | tail -100

# Tất cả logs
docker-compose logs | tail -100
```
