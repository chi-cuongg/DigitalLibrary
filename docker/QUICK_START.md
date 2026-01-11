# Quick Start Guide - Deploy Digital Library

## 🎯 Chọn phương án deployment

Có 2 phương án:
1. **Cloudflare Tunnel** (⭐ Khuyến nghị - Dễ hơn, bảo mật hơn)
2. **Nginx + Let's Encrypt** (Truyền thống)

👉 **Nếu đã setup Cloudflare Tunnel**, xem [CLOUDFLARE_TUNNEL.md](./CLOUDFLARE_TUNNEL.md)

## ⚡ Deploy nhanh trong 5 phút

### Bước 1: Chuẩn bị VPS

```bash
# Cài Docker và Docker Compose
curl -fsSL https://get.docker.com -o get-docker.sh && sh get-docker.sh
apt install docker-compose git -y
```

### Bước 2: Clone project

```bash
cd /opt
git clone https://github.com/your-username/DigitalLibrary.git
cd DigitalLibrary
```

### Bước 3: Cấu hình

```bash
cd docker
cp .env.example .env
nano .env  # Sửa mật khẩu và thông tin
```

### Bước 4: Cập nhật pom.xml (chuyển từ SQL Server sang MySQL)

Mở file `demo/pom.xml` và thay thế:

```xml
<!-- XÓA -->
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- THÊM -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Bước 5: Deploy

```bash
docker-compose up -d --build
```

### Bước 6: Kiểm tra

```bash
# Xem logs
docker-compose logs -f app

# Kiểm tra status
docker-compose ps

# Truy cập ứng dụng
curl http://localhost:8080
```

### Bước 7: Cấu hình Domain (Tùy chọn)

Nếu có domain:

1. **Cloudflare**:
   - Thêm domain vào Cloudflare
   - Thêm A record trỏ về IP VPS
   - Bật Proxy (orange cloud)

2. **SSL Certificate**:
```bash
# Cài certbot
apt install certbot python3-certbot-nginx -y

# Lấy certificate
certbot --nginx -d your-domain.com
```

## 🔄 Update application

```bash
cd /opt/DigitalLibrary
git pull
cd docker
docker-compose up -d --build
```

## 📊 Quản lý

```bash
# Xem logs
docker-compose logs -f

# Restart
docker-compose restart

# Stop
docker-compose stop

# Start
docker-compose start

# Xem tài nguyên
docker stats
```

## 🐛 Troubleshooting

```bash
# Container không start?
docker-compose logs app

# Database error?
docker-compose logs mysql

# Port bị chiếm?
netstat -tulpn | grep :8080
```

## 📝 Lưu ý quan trọng

1. **Đổi mật khẩu**: Đảm bảo đổi tất cả mật khẩu trong file `.env`
2. **Google OAuth**: Cập nhật redirect URI trong Google Cloud Console
3. **Firewall**: Mở port 80, 443, 22
4. **Backup**: Thiết lập backup database tự động
