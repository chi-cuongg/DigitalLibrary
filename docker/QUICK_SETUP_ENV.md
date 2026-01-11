# Hướng dẫn nhanh: Pull code và tạo .env

## 1. Pull code mới

```bash
cd /opt/DigitalLibrary
git pull
```

## 2. Tạo file .env với MySQL password

MySQL password là **BẠN TỰ ĐẶT**, không phải password có sẵn!

```bash
cd /opt/DigitalLibrary/docker
nano .env
```

Dán nội dung này và **THAY ĐỔI PASSWORD**:

```env
# ⚠️ QUAN TRỌNG: Đổi các password thành password mạnh của bạn!
MYSQL_ROOT_PASSWORD=YourRootPassword123!
MYSQL_DATABASE=DigitalLibrary
MYSQL_USER=digitallibrary
MYSQL_PASSWORD=YourDbPassword123!

SPRING_PROFILES_ACTIVE=prod

# Google OAuth 2.0 (Lấy từ Google Cloud Console)
GOOGLE_OAUTH2_CLIENT_ID=your_google_client_id_here
GOOGLE_OAUTH2_CLIENT_SECRET=your_google_client_secret_here
GOOGLE_OAUTH2_REDIRECT_URI=https://digilibrary.online/oauth2/callback

DOMAIN=digilibrary.online
```

**Thay đổi:**
- `YourRootPassword123!` → Password mạnh của bạn (cho root MySQL)
- `YourDbPassword123!` → Password mạnh của bạn (cho user digitallibrary)

Save: `Ctrl + O` → `Enter` → `Ctrl + X`

## 3. Build Docker

```bash
cd /opt/DigitalLibrary/docker
docker-compose up -d --build
```
