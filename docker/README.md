# Docker Deployment cho Digital Library

## 📁 Cấu trúc thư mục

```
docker/
├── docker-compose.yml          # Cấu hình Docker Compose
├── Dockerfile                  # Build image cho Spring Boot app
├── .env.example                # Template file cho biến môi trường
├── DATABASE_MIGRATION.md       # Hướng dẫn chuyển SQL Server → MySQL
├── QUICK_START.md              # Hướng dẫn deploy nhanh
└── nginx/
    ├── nginx.conf              # Cấu hình Nginx chính
    └── conf.d/
        └── default.conf        # Cấu hình virtual host
```

## 🚀 Quick Start

```bash
# 1. Copy file environment
cp .env.example .env

# 2. Sửa file .env với thông tin của bạn
nano .env

# 3. Cập nhật pom.xml (chuyển SQL Server → MySQL)
# Xem DATABASE_MIGRATION.md

# 4. Build và chạy
docker-compose up -d --build

# 5. Xem logs
docker-compose logs -f
```

## 📋 Services

### 1. MySQL Database
- **Port**: 3306
- **Data**: Persisted trong volume `mysql_data`
- **Healthcheck**: Tự động kiểm tra

### 2. Spring Boot Application
- **Port**: 8080 (internal)
- **Uploads**: Persisted trong volume `app_uploads`
- **Logs**: Persisted trong volume `app_logs`
- **Healthcheck**: Tự động kiểm tra

### 3. Nginx Reverse Proxy
- **Ports**: 80 (HTTP), 443 (HTTPS)
- **SSL**: Cần cấu hình với Let's Encrypt
- **Static files**: Cache enabled

## 🔧 Environment Variables

Xem file `.env.example` để biết các biến môi trường cần thiết.

## 📖 Tài liệu

- **[DEPLOYMENT_GUIDE.md](../DEPLOYMENT_GUIDE.md)**: Hướng dẫn deploy chi tiết
- **[QUICK_START.md](./QUICK_START.md)**: Hướng dẫn deploy nhanh
- **[DATABASE_MIGRATION.md](./DATABASE_MIGRATION.md)**: Chuyển đổi database

## 🐛 Troubleshooting

Xem [DEPLOYMENT_GUIDE.md](../DEPLOYMENT_GUIDE.md#-troubleshooting)
