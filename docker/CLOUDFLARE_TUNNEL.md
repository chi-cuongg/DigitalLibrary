# Hướng dẫn sử dụng Cloudflare Tunnel

Cloudflare Tunnel là giải pháp tốt hơn Nginx vì:
- ✅ Không cần mở port 80/443 trên VPS (bảo mật hơn)
- ✅ SSL tự động từ Cloudflare (miễn phí)
- ✅ DDoS protection tích hợp
- ✅ Dễ setup hơn, không cần cấu hình phức tạp

## 📋 Yêu cầu

1. Domain đã được thêm vào Cloudflare
2. Đã tạo Cloudflare Tunnel (bạn đã làm xong bước này)
3. Tunnel ID: `ea0c511c-5d3f-4114-93d3-afc3ab621052`
4. Tunnel name: `digilibrary`

## 🚀 Bước 1: Cài đặt cloudflared trên VPS

```bash
# Tải cloudflared
wget https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb

# Cài đặt
dpkg -i cloudflared-linux-amd64.deb

# Kiểm tra
cloudflared --version
```

## 🔧 Bước 2: Cấu hình Tunnel

### 2.1. Tạo file config

```bash
mkdir -p /root/.cloudflared
nano /root/.cloudflared/config.yml
```

Nội dung file `config.yml`:
```yaml
tunnel: ea0c511c-5d3f-4114-93d3-afc3ab621052
credentials-file: /root/.cloudflared/ea0c511c-5d3f-4114-93d3-afc3ab621052.json

ingress:
  # Route tất cả traffic đến Spring Boot app
  - hostname: your-domain.com
    service: http://localhost:8080
  
  # Route www subdomain
  - hostname: www.your-domain.com
    service: http://localhost:8080
  
  # Catch-all rule (phải đặt cuối cùng)
  - service: http_status:404
```

**Lưu ý**: Thay `your-domain.com` bằng domain thực tế của bạn.

### 2.2. Verify config

```bash
cloudflared tunnel ingress validate
```

## 🌐 Bước 3: Route DNS trong Cloudflare Dashboard

1. Vào Cloudflare Dashboard → DNS → Records
2. Thêm/tạo CNAME record:
   - **Type**: CNAME
   - **Name**: `@` (hoặc `www`)
   - **Target**: `ea0c511c-5d3f-4114-93d3-afc3ab621052.cfargotunnel.com`
   - **Proxy**: ON (orange cloud)
   - **TTL**: Auto

## 🐳 Bước 4: Tích hợp vào Docker Compose

### Option 1: Chạy cloudflared riêng (Khuyến nghị)

Thêm service vào `docker-compose.yml`:

```yaml
  cloudflared:
    image: cloudflare/cloudflared:latest
    container_name: digitallibrary_cloudflared
    restart: unless-stopped
    command: tunnel run
    volumes:
      - /root/.cloudflared:/etc/cloudflared:ro
    network_mode: host
    depends_on:
      - app
```

**Lưu ý**: Cần copy file config và credentials vào `/root/.cloudflared/` trước.

### Option 2: Chạy cloudflared như systemd service

```bash
# Tạo service file
nano /etc/systemd/system/cloudflared.service
```

Nội dung:
```ini
[Unit]
Description=Cloudflare Tunnel
After=network.target

[Service]
Type=simple
User=root
ExecStart=/usr/local/bin/cloudflared tunnel --config /root/.cloudflared/config.yml run
Restart=on-failure
RestartSec=5s

[Install]
WantedBy=multi-user.target
```

Khởi động service:
```bash
systemctl daemon-reload
systemctl enable cloudflared
systemctl start cloudflared
systemctl status cloudflared
```

## 🔄 Bước 5: Cập nhật docker-compose.yml (loại bỏ Nginx)

Nếu dùng Cloudflare Tunnel, bạn **KHÔNG CẦN** Nginx service. 

Có thể xóa Nginx service khỏi `docker-compose.yml`, hoặc giữ lại nhưng không expose port 80/443.

### docker-compose.yml đơn giản hơn:

```yaml
version: '3.8'

services:
  mysql:
    # ... (giữ nguyên)

  app:
    # ... (giữ nguyên)
    # Chỉ cần expose port 8080 internal, không cần public
    expose:
      - "8080"
    # XÓA dòng ports nếu không cần access trực tiếp từ VPS

  # XÓA nginx service nếu dùng Cloudflare Tunnel
```

## ✅ Bước 6: Kiểm tra

1. **Kiểm tra tunnel đang chạy**:
```bash
# Nếu dùng systemd
systemctl status cloudflared

# Hoặc kiểm tra process
ps aux | grep cloudflared
```

2. **Kiểm tra logs**:
```bash
# Nếu dùng systemd
journalctl -u cloudflared -f

# Hoặc nếu chạy manual
cloudflared tunnel run --loglevel debug
```

3. **Truy cập domain**:
   - Mở browser và truy cập `https://your-domain.com`
   - Nên tự động redirect HTTPS và hoạt động

## 🔐 Bước 7: Cập nhật Google OAuth Redirect URI

Trong Google Cloud Console:
1. Vào OAuth 2.0 Client IDs
2. Cập nhật **Authorized redirect URIs**:
   - Thêm: `https://your-domain.com/oauth2/callback`
   - Xóa: `http://localhost:8080/oauth2/callback` (nếu không dùng nữa)

3. Cập nhật `application-prod.properties`:
```properties
google.oauth2.redirect.uri=https://your-domain.com/oauth2/callback
```

## 🛡️ Bước 8: Bảo mật (Tùy chọn)

### 8.1. Đóng port 80/443 trên firewall

Vì dùng Cloudflare Tunnel, bạn không cần mở port 80/443:

```bash
# Chỉ mở port 22 (SSH) và 8080 (nếu cần access trực tiếp)
ufw allow 22/tcp
ufw allow 8080/tcp  # Chỉ nếu cần debug
ufw enable
```

### 8.2. Access control trong Cloudflare

Trong Cloudflare Dashboard:
- **Security** → **WAF**: Bật các rules cơ bản
- **Security** → **Access**: Tạo Access policies (nếu cần)

## 🐛 Troubleshooting

### Tunnel không kết nối

```bash
# Kiểm tra config
cloudflared tunnel ingress validate

# Test tunnel connection
cloudflared tunnel info

# Xem logs chi tiết
cloudflared tunnel run --loglevel debug
```

### DNS không resolve

- Kiểm tra CNAME record trong Cloudflare Dashboard
- Đảm bảo Proxy status là ON (orange cloud)
- Đợi 1-2 phút để DNS propagate

### App không accessible

- Kiểm tra Spring Boot app đang chạy: `docker-compose ps`
- Kiểm tra app logs: `docker-compose logs app`
- Kiểm tra app có listen trên port 8080: `netstat -tulpn | grep 8080`

## 📊 So sánh: Cloudflare Tunnel vs Nginx

| Feature | Cloudflare Tunnel | Nginx + Let's Encrypt |
|---------|------------------|----------------------|
| Setup | ⭐⭐⭐⭐⭐ Rất dễ | ⭐⭐⭐ Phức tạp hơn |
| SSL | ✅ Tự động | ✅ Cần cấu hình |
| Bảo mật | ✅ Không expose port | ⚠️ Cần mở port |
| DDoS | ✅ Tích hợp sẵn | ❌ Cần cấu hình riêng |
| Performance | ✅ CDN tích hợp | ⚠️ Tùy cấu hình |
| Cost | ✅ Miễn phí | ✅ Miễn phí |

## ✅ Checklist

- [ ] Đã cài cloudflared
- [ ] Đã tạo config.yml
- [ ] Đã route DNS trong Cloudflare
- [ ] Đã khởi động tunnel
- [ ] Đã cập nhật Google OAuth redirect URI
- [ ] Đã test truy cập domain
- [ ] Đã đóng port 80/443 (nếu không dùng Nginx)
