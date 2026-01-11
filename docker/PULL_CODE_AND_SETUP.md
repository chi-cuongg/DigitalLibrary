# Pull code mới và setup MySQL password

## 1. ✅ Pull code mới trên VPS

Nếu bạn vừa push code mới lên Git, cần pull về VPS:

```bash
# SSH vào VPS
ssh root@your-vps-ip

# Vào thư mục project
cd /opt/DigitalLibrary

# Pull code mới
git pull

# Kiểm tra code đã cập nhật
git status
```

**Lưu ý**: Nếu có conflict hoặc thay đổi local, có thể cần:
```bash
git stash  # Lưu thay đổi local (nếu có)
git pull
```

---

## 2. 🔐 MySQL Password - Bạn TỰ ĐẶT!

**Quan trọng**: MySQL password **KHÔNG PHẢI** là password có sẵn! Bạn **TỰ ĐẶT** password trong file `.env`.

Khi Docker Compose chạy, nó sẽ:
1. Đọc password từ file `.env`
2. Tạo MySQL container với password đó
3. Tạo database và user với password đó

### Cách tạo file .env với password:

```bash
cd /opt/DigitalLibrary/docker
nano .env
```

Dán nội dung này (⚠️ **BẠN TỰ ĐẶT PASSWORD**):

```env
# Database Configuration
# ⚠️ ĐỔI các giá trị "your_password_here" thành password mạnh của bạn!
MYSQL_ROOT_PASSWORD=MySecureRootPass123!
MYSQL_DATABASE=DigitalLibrary
MYSQL_USER=digitallibrary
MYSQL_PASSWORD=MySecureDbPass123!

# Application Configuration
SPRING_PROFILES_ACTIVE=prod

# Google OAuth 2.0
GOOGLE_OAUTH2_CLIENT_ID=509081543880-ssjdjuvpt9tbja3oo1lpucnhtihkrqd2.apps.googleusercontent.com
GOOGLE_OAUTH2_CLIENT_SECRET=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
GOOGLE_OAUTH2_REDIRECT_URI=https://digilibrary.online/oauth2/callback

# Domain
DOMAIN=digilibrary.online
```

**Lưu ý về password:**
- ✅ **Tự đặt** password mạnh (ít nhất 8 ký tự, có chữ hoa, số, ký tự đặc biệt)
- ✅ **Nhớ** password này (hoặc lưu lại an toàn)
- ✅ Không dùng password yếu như: `123`, `password`, `root`
- ✅ Ví dụ password tốt: `MySecureRootPass123!`, `DigiLib2024#Secure`

**Sau khi điền password:**
- Nhấn `Ctrl + O` để Save
- Nhấn `Enter` để xác nhận
- Nhấn `Ctrl + X` để Exit

---

## 3. 📋 Checklist trước khi build

```bash
cd /opt/DigitalLibrary

# 1. Code đã pull mới nhất
git status

# 2. Kiểm tra pom.xml đã có MySQL (không phải SQL Server)
grep -A 2 "mysql-connector\|mssql-jdbc" demo/pom.xml

# Kết quả mong đợi: Thấy "mysql-connector-j", KHÔNG thấy "mssql-jdbc"

# 3. File .env đã tạo
ls -la docker/.env

# 4. Kiểm tra .env có password (không phải placeholder)
grep "PASSWORD" docker/.env
```

---

## 4. 🚀 Build và chạy Docker

Sau khi đã:
- ✅ Pull code mới
- ✅ Tạo file `.env` với password

Tiếp tục build:

```bash
cd /opt/DigitalLibrary/docker

# Build và chạy (lần đầu)
docker-compose up -d --build

# Xem logs
docker-compose logs -f
```

**Đợi đến khi:**
- MySQL: `ready for connections`
- App: `Started DemoApplication`
- Không có lỗi đỏ

---

## 5. 🔍 Kiểm tra MySQL đã chạy

```bash
# Kiểm tra container
docker-compose ps mysql

# Kiểm tra logs
docker-compose logs mysql | tail -20

# Test kết nối MySQL (sử dụng password từ .env)
docker exec -it digitallibrary_mysql mysql -uroot -p

# Nhập password: [password bạn đã đặt trong .env]
# Sau đó gõ: exit
```

---

## ⚠️ Nếu quên password MySQL

Nếu bạn quên password đã đặt:

1. **Xem trong file .env:**
```bash
cat docker/.env | grep PASSWORD
```

2. **Hoặc reset password:**
```bash
# Stop containers
docker-compose down

# Xóa volume MySQL (⚠️ MẤT DỮ LIỆU!)
docker volume rm docker_mysql_data

# Cập nhật password mới trong .env
nano docker/.env

# Start lại
docker-compose up -d
```

---

## 📝 Tóm tắt các bước

1. ✅ **Pull code mới**: `cd /opt/DigitalLibrary && git pull`
2. ✅ **Tạo file .env**: `cd docker && nano .env` (tự đặt password)
3. ✅ **Kiểm tra pom.xml**: Đảm bảo có MySQL dependency
4. ✅ **Build Docker**: `docker-compose up -d --build`
5. ✅ **Kiểm tra**: `docker-compose ps` và logs

---

## 🎯 Quy trình đầy đủ

```bash
# 1. Pull code
cd /opt/DigitalLibrary
git pull

# 2. Tạo .env với password (nếu chưa có)
cd docker
nano .env  # Đặt password của bạn

# 3. Kiểm tra pom.xml
cd ..
grep mysql-connector demo/pom.xml  # Phải thấy mysql-connector-j

# 4. Build và chạy
cd docker
docker-compose up -d --build

# 5. Xem logs
docker-compose logs -f
```
