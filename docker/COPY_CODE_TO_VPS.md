# Hướng dẫn copy code lên VPS

## Có 2 cách chính:

### Cách 1: Clone từ Git (Khuyến nghị - Dễ nhất) ⭐

Nếu bạn đã push code lên GitHub/GitLab/Bitbucket:

#### Bước 1: Tạo Git repository (nếu chưa có)

```bash
# Trên máy local (Windows)
cd D:\Github\DigitalLibrary
git init
git add .
git commit -m "Initial commit"

# Push lên GitHub
# Tạo repository mới trên GitHub, sau đó:
git remote add origin https://github.com/your-username/DigitalLibrary.git
git push -u origin main
```

#### Bước 2: Clone trên VPS

```bash
# SSH vào VPS
ssh root@your-vps-ip

# Clone project
cd /opt
git clone https://github.com/your-username/DigitalLibrary.git
cd DigitalLibrary

# Kiểm tra
ls -la
```

---

### Cách 2: Upload code qua SCP/SFTP

Nếu chưa dùng Git hoặc muốn upload trực tiếp:

#### Option A: Dùng WinSCP (Windows - GUI)

1. Download WinSCP: https://winscp.net/
2. Cài đặt và mở WinSCP
3. Kết nối VPS:
   - **Protocol**: SFTP
   - **Host name**: IP của VPS
   - **User name**: root
   - **Password**: password của VPS
   - Click **Login**
4. Upload code:
   - Bên trái: Máy local (Windows) - tìm thư mục `D:\Github\DigitalLibrary`
   - Bên phải: VPS - điều hướng đến `/opt`
   - Chọn toàn bộ thư mục `DigitalLibrary` (bỏ qua `target/`, `.git/` nếu có)
   - Drag & drop hoặc right-click → Upload
5. Di chuyển vào thư mục:
```bash
cd /opt/DigitalLibrary
```

#### Option B: Dùng SCP (Command line)

```bash
# Trên máy Windows (PowerShell hoặc Git Bash)
cd D:\Github\DigitalLibrary

# Upload toàn bộ thư mục (trừ .git, target nếu có)
scp -r -o "StrictHostKeyChecking=no" . root@your-vps-ip:/opt/DigitalLibrary

# Hoặc nếu muốn exclude một số thư mục, dùng rsync (cần cài trên Windows)
# Hoặc zip trước rồi upload
```

#### Option C: Dùng Zip + SCP (Đơn giản nhất)

**Bước 1: Zip code trên máy local**

```bash
# Trên Windows PowerShell
cd D:\Github\DigitalLibrary
# Bỏ qua các thư mục không cần thiết
Compress-Archive -Path * -DestinationPath DigitalLibrary.zip -Exclude target,*.git
```

**Bước 2: Upload file zip**

```bash
# Upload zip file
scp DigitalLibrary.zip root@your-vps-ip:/opt/
```

**Bước 3: Giải nén trên VPS**

```bash
# SSH vào VPS
ssh root@your-vps-ip

# Giải nén
cd /opt
unzip DigitalLibrary.zip -d DigitalLibrary
cd DigitalLibrary

# Xóa file zip
rm DigitalLibrary.zip
```

---

## 📋 Checklist sau khi copy code

Sau khi code đã có trên VPS, kiểm tra:

```bash
cd /opt/DigitalLibrary

# 1. Kiểm tra cấu trúc thư mục
ls -la
ls -la demo/
ls -la docker/

# 2. Kiểm tra file quan trọng
cat demo/pom.xml | grep mysql-connector
cat docker/docker-compose.yml | head -20

# 3. Kiểm tra file .env (nếu có)
ls -la docker/.env
```

---

## ⚠️ Lưu ý quan trọng

1. **Không upload các thư mục không cần thiết**:
   - `target/` (build artifacts)
   - `.git/` (nếu không dùng Git trên VPS)
   - `uploads/` (sẽ được tạo tự động)
   - `logs/` (sẽ được tạo tự động)
   - `*.iml`, `.idea/`, `.vscode/` (IDE files)

2. **Đảm bảo file permissions**:
```bash
cd /opt/DigitalLibrary
chmod +x demo/mvnw  # Nếu có mvnw
```

3. **Nếu dùng Git, đảm bảo đã push code mới nhất**:
```bash
# Trên máy local
git add .
git commit -m "Prepare for deployment"
git push
```

---

## 🎯 Sau khi code đã trên VPS

Tiếp tục các bước trong `DEPLOY_STEPS.md`:
1. Kiểm tra và cập nhật pom.xml (MySQL dependency)
2. Tạo file .env
3. Build và chạy Docker containers
4. Route DNS trong Cloudflare
5. Test truy cập
