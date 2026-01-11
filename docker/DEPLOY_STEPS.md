# Các bước deploy sau khi đã cấu hình Cloudflare Tunnel

## ✅ Checklist đã hoàn thành
- [x] Domain: digilibrary.online
- [x] Docker đã cài trên VPS
- [x] Cloudflare Tunnel config.yml đã tạo
- [ ] DNS Records đã route trong Cloudflare
- [ ] Docker containers đã build và chạy
- [ ] Cloudflared tunnel đã chạy
- [ ] App accessible qua domain

## 🚀 Các bước tiếp theo

### Bước 1: Route DNS trong Cloudflare Dashboard

1. Vào [Cloudflare Dashboard](https://dash.cloudflare.com)
2. Chọn domain `digilibrary.online`
3. Vào **DNS** → **Records**
4. Tạo 2 CNAME records:

   **Record 1 (root domain):**
   - Type: `CNAME`
   - Name: `@`
   - Target: `ea0c511c-5d3f-4114-93d3-afc3ab621052.cfargotunnel.com`
   - Proxy status: **ON** (orange cloud ☁️)
   - TTL: Auto

   **Record 2 (www subdomain):**
   - Type: `CNAME`
   - Name: `www`
   - Target: `ea0c511c-5d3f-4114-93d3-afc3ab621052.cfargotunnel.com`
   - Proxy status: **ON** (orange cloud ☁️)
   - TTL: Auto

### Bước 2: Kiểm tra và cập nhật pom.xml (QUAN TRỌNG!)

Đảm bảo đã chuyển từ SQL Server sang MySQL:

```bash
cd /opt/DigitalLibrary  # hoặc đường dẫn project của bạn
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

**XÓA** và **THAY** bằng:
```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Bước 3: Tạo file .env

```bash
cd /opt/DigitalLibrary/docker
cp .env.example .env
nano .env
```

Cập nhật các giá trị (QUAN TRỌNG - đổi mật khẩu!):
```env
# Database
MYSQL_ROOT_PASSWORD=your_secure_root_password_here
MYSQL_DATABASE=DigitalLibrary
MYSQL_USER=digitallibrary
MYSQL_PASSWORD=your_secure_db_password_here

# Application
SPRING_PROFILES_ACTIVE=prod
GOOGLE_OAUTH2_CLIENT_ID=your_google_client_id
GOOGLE_OAUTH2_CLIENT_SECRET=your_google_client_secret
GOOGLE_OAUTH2_REDIRECT_URI=https://digilibrary.online/oauth2/callback

# Domain
DOMAIN=digilibrary.online
```

**Lưu ý**: 
- Đổi tất cả mật khẩu thành mật khẩu mạnh
- Cập nhật Google OAuth credentials

### Bước 4: Build và chạy Docker containers

```bash
cd /opt/DigitalLibrary/docker

# Build và chạy (lần đầu)
docker-compose up -d --build

# Xem logs để đảm bảo không có lỗi
docker-compose logs -f
```

**Đợi khoảng 1-2 phút** để:
- MySQL khởi động
- App build và khởi động
- Database được tạo

### Bước 5: Kiểm tra app đang chạy

```bash
# Kiểm tra containers đang chạy
docker-compose ps

# Kiểm tra app có listen trên port 8080
ss -lntp | grep 8080
# Hoặc
netstat -tulpn | grep 8080

# Test app
curl -I http://localhost:8080
# Hoặc
curl http://localhost:8080
```

**Kết quả mong đợi**: 
- Containers status: `Up`
- Port 8080 đang được listen
- curl trả về HTTP 200 hoặc HTML content

### Bước 6: Khởi động Cloudflare Tunnel

Nếu chưa chạy cloudflared:

```bash
# Kiểm tra cloudflared có đang chạy
systemctl status cloudflared
ps aux | grep cloudflared

# Nếu chưa chạy, start service
systemctl start cloudflared
systemctl enable cloudflared  # Tự động start khi reboot

# Xem logs
journalctl -u cloudflared -f
```

### Bước 7: Test truy cập qua domain

1. Mở browser
2. Truy cập: `https://digilibrary.online`
3. Kiểm tra:
   - ✅ SSL certificate (HTTPS)
   - ✅ Website load được
   - ✅ Không có lỗi

### Bước 8: Cập nhật Google OAuth (QUAN TRỌNG!)

1. Vào [Google Cloud Console](https://console.cloud.google.com)
2. APIs & Services → Credentials
3. Mở OAuth 2.0 Client ID của bạn
4. Thêm **Authorized redirect URIs**:
   - `https://digilibrary.online/oauth2/callback`
   - `https://www.digilibrary.online/oauth2/callback`

## 🐛 Troubleshooting

### Lỗi: "connection refused" trên port 8080

**Nguyên nhân**: App chưa chạy hoặc đang build

**Giải pháp**:
```bash
# Xem logs của app
docker-compose logs app

# Kiểm tra app có đang build không
docker-compose ps

# Restart app
docker-compose restart app

# Xem logs real-time
docker-compose logs -f app
```

### Lỗi: Database connection error

**Nguyên nhân**: MySQL chưa sẵn sàng

**Giải pháp**:
```bash
# Kiểm tra MySQL logs
docker-compose logs mysql

# Kiểm tra MySQL đang chạy
docker-compose ps mysql

# Đợi MySQL khởi động xong (có thể mất 30-60 giây)
docker-compose logs -f mysql
```

### Lỗi: Build failed

**Nguyên nhân**: Code lỗi hoặc dependency issues

**Giải pháp**:
```bash
# Xem logs build chi tiết
docker-compose build --no-cache app

# Kiểm tra pom.xml đã đúng chưa
cat demo/pom.xml | grep mysql-connector

# Clean và rebuild
cd /opt/DigitalLibrary/docker
docker-compose down
docker-compose build --no-cache
docker-compose up -d
```

### Cloudflared không kết nối

**Nguyên nhân**: Config sai hoặc DNS chưa route

**Giải pháp**:
```bash
# Validate config
cloudflared tunnel ingress validate

# Kiểm tra DNS trong Cloudflare Dashboard
# Đảm bảo CNAME record đúng và Proxy = ON

# Test tunnel connection
cloudflared tunnel info
```

## 📊 Kiểm tra tổng thể

```bash
# 1. Containers status
docker-compose ps

# 2. App logs
docker-compose logs --tail=50 app

# 3. MySQL logs
docker-compose logs --tail=50 mysql

# 4. Port 8080
ss -lntp | grep 8080

# 5. Cloudflared status
systemctl status cloudflared

# 6. Test local
curl -I http://localhost:8080

# 7. Test domain
curl -I https://digilibrary.online
```

## ✅ Khi nào là thành công?

- ✅ `docker-compose ps` hiển thị tất cả containers `Up`
- ✅ `ss -lntp | grep 8080` hiển thị port đang listen
- ✅ `curl http://localhost:8080` trả về HTTP 200
- ✅ `systemctl status cloudflared` hiển thị `active (running)`
- ✅ Truy cập `https://digilibrary.online` thấy website
- ✅ SSL certificate hợp lệ (HTTPS lock icon)
