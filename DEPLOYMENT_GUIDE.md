# Hướng dẫn Deploy Digital Library lên VPS

## 📋 Yêu cầu hệ thống

- **VPS**: Ubuntu 20.04+ (2 CPU, 2GB RAM, 30GB Disk)
- **Domain**: (tùy chọn, có thể dùng IP)
- **Cloudflare**: (tùy chọn, miễn phí cho CDN và SSL)

## 🎯 Phương án được đề xuất

### Option 1: Docker Compose + MySQL + Cloudflare Tunnel (⭐ Khuyến nghị)

**Tại sao chọn phương án này?**
- ✅ **Docker Compose**: Dễ quản lý, deploy nhanh
- ✅ **MySQL**: Nhẹ hơn SQL Server, phù hợp VPS 2GB RAM
- ✅ **Cloudflare Tunnel**: Không cần mở port, SSL tự động, bảo mật cao
- ✅ **Dễ setup hơn**: Không cần cấu hình Nginx phức tạp

👉 **Xem hướng dẫn chi tiết**: [docker/CLOUDFLARE_TUNNEL.md](./docker/CLOUDFLARE_TUNNEL.md)

### Option 2: Docker Compose + MySQL + Nginx + Let's Encrypt

**Khi nào dùng?**
- Không dùng Cloudflare
- Cần control hoàn toàn reverse proxy
- Muốn tự quản lý SSL

**Tại sao chọn phương án này?**
- ✅ **Docker Compose**: Dễ quản lý, deploy nhanh
- ✅ **MySQL**: Nhẹ hơn SQL Server, phù hợp VPS 2GB RAM
- ✅ **Nginx**: Reverse proxy, SSL, static files
- ✅ **Let's Encrypt**: SSL miễn phí

## 📦 Cấu trúc file deployment

```
DigitalLibrary/
├── docker/
│   ├── docker-compose.yml
│   ├── Dockerfile
│   └── nginx/
│       └── nginx.conf
├── demo/
│   └── src/main/resources/
│       └── application-prod.properties
└── DEPLOYMENT_GUIDE.md
```

---

## 🚀 Bước 1: Chuẩn bị VPS

### 1.1. Kết nối VPS

```bash
ssh root@your-vps-ip
```

### 1.2. Cập nhật hệ thống

```bash
apt update && apt upgrade -y
```

### 1.3. Cài đặt Docker và Docker Compose

```bash
# Cài đặt Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# Cài đặt Docker Compose
apt install docker-compose -y

# Khởi động Docker
systemctl start docker
systemctl enable docker

# Kiểm tra
docker --version
docker-compose --version
```

### 1.4. Cài đặt Git (nếu chưa có)

```bash
apt install git -y
```

---

## 🗄️ Bước 2: Chuyển đổi Database từ SQL Server sang MySQL

### 2.1. Cập nhật pom.xml

Thay đổi dependency trong `demo/pom.xml`:

```xml
<!-- XÓA dòng này -->
<!-- <dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <scope>runtime</scope>
</dependency> -->

<!-- THÊM dòng này -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 2.2. Tạo file application-prod.properties

File này sẽ được dùng khi chạy production với Docker.

---

## 📝 Bước 3: Tạo các file Docker

Xem các file đã được tạo ở bước tiếp theo.

---

## 🔧 Bước 4: Cấu hình Cloudflare (Tùy chọn)

### 4.1. Thêm domain vào Cloudflare

1. Đăng ký tại [cloudflare.com](https://cloudflare.com)
2. Thêm domain của bạn
3. Cập nhật nameservers theo hướng dẫn

### 4.2. Cấu hình DNS

Thêm A record:
- **Type**: A
- **Name**: @ (hoặc www)
- **Content**: IP của VPS
- **Proxy**: ON (orange cloud)

---

## 🚀 Bước 5: Deploy

### 5.1. Clone project lên VPS

```bash
cd /opt
git clone https://github.com/your-username/DigitalLibrary.git
cd DigitalLibrary
```

### 5.2. Cấu hình biến môi trường

Tạo file `.env`:

```bash
nano .env
```

Nội dung:
```env
# Database
MYSQL_ROOT_PASSWORD=your_secure_password_here
MYSQL_DATABASE=DigitalLibrary
MYSQL_USER=digitallibrary
MYSQL_PASSWORD=your_db_password_here

# Application
SPRING_PROFILES_ACTIVE=prod
GOOGLE_OAUTH2_REDIRECT_URI=https://your-domain.com/oauth2/callback

# Domain
DOMAIN=your-domain.com
```

### 5.3. Build và chạy

```bash
cd docker
docker-compose up -d --build
```

### 5.4. Kiểm tra logs

```bash
# Xem logs của tất cả services
docker-compose logs -f

# Xem logs của app
docker-compose logs -f app

# Xem logs của database
docker-compose logs -f mysql
```

---

## 🔐 Bước 6: Cấu hình SSL với Let's Encrypt

### 6.1. Cài đặt Certbot

```bash
apt install certbot python3-certbot-nginx -y
```

### 6.2. Lấy SSL certificate

```bash
certbot --nginx -d your-domain.com -d www.your-domain.com
```

### 6.3. Tự động renew

```bash
certbot renew --dry-run
```

---

## 📊 Bước 7: Quản lý và Monitoring

### 7.1. Xem trạng thái containers

```bash
docker-compose ps
```

### 7.2. Restart services

```bash
docker-compose restart
```

### 7.3. Stop services

```bash
docker-compose stop
```

### 7.4. Start services

```bash
docker-compose start
```

### 7.5. Xem tài nguyên sử dụng

```bash
docker stats
```

---

## 🔄 Bước 8: Update application

### 8.1. Pull code mới

```bash
cd /opt/DigitalLibrary
git pull
```

### 8.2. Rebuild và restart

```bash
cd docker
docker-compose up -d --build
```

---

## 🐛 Troubleshooting

### Vấn đề: Container không start

```bash
# Xem logs
docker-compose logs app

# Kiểm tra port đã được sử dụng chưa
netstat -tulpn | grep :8080
```

### Vấn đề: Database connection error

```bash
# Kiểm tra MySQL container
docker-compose logs mysql

# Kiểm tra network
docker network ls
docker network inspect docker_default
```

### Vấn đề: Out of memory

```bash
# Xem memory usage
free -h
docker stats

# Giảm memory cho MySQL trong docker-compose.yml
```

---

## 📈 Tối ưu hóa

### 1. Enable caching trong Nginx

Đã được cấu hình trong nginx.conf

### 2. Tối ưu MySQL

Các tham số đã được tối ưu trong docker-compose.yml

### 3. Giám sát với htop

```bash
apt install htop -y
htop
```

---

## 🔒 Bảo mật

### 1. Firewall (UFW)

```bash
# Cho phép SSH
ufw allow 22/tcp

# Cho phép HTTP/HTTPS
ufw allow 80/tcp
ufw allow 443/tcp

# Bật firewall
ufw enable
```

### 2. Đổi mật khẩu mặc định

Đảm bảo thay đổi tất cả mật khẩu trong file `.env`

### 3. Backup database

Tạo script backup tự động:

```bash
#!/bin/bash
docker exec docker_mysql_1 mysqldump -u root -p$MYSQL_ROOT_PASSWORD DigitalLibrary > backup_$(date +%Y%m%d_%H%M%S).sql
```

---

## 📞 Hỗ trợ

Nếu gặp vấn đề, kiểm tra:
1. Logs: `docker-compose logs -f`
2. Status: `docker-compose ps`
3. Resources: `docker stats`

---

## ✅ Checklist trước khi deploy

- [ ] Đã cài Docker và Docker Compose
- [ ] Đã cập nhật pom.xml (MySQL thay SQL Server)
- [ ] Đã tạo file application-prod.properties
- [ ] Đã tạo file .env với mật khẩu an toàn
- [ ] Đã cấu hình domain (nếu có)
- [ ] Đã cấu hình Cloudflare (nếu có)
- [ ] Đã cấu hình Google OAuth 2.0 redirect URI
- [ ] Đã test build locally
- [ ] Đã backup database (nếu có dữ liệu cũ)
