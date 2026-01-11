# Hướng dẫn nhanh: Copy code lên VPS

## 🚀 Cách nhanh nhất: Git Clone (5 phút)

### Bước 1: Push code lên GitHub (nếu chưa có)

**Trên máy Windows của bạn:**

```powershell
# Mở PowerShell hoặc Git Bash
cd D:\Github\DigitalLibrary

# Nếu chưa có Git repo
git init
git add .
git commit -m "Initial commit for deployment"

# Tạo repo mới trên GitHub (vào github.com → New repository)
# Sau đó push:
git remote add origin https://github.com/YOUR_USERNAME/DigitalLibrary.git
git branch -M main
git push -u origin main
```

### Bước 2: Clone trên VPS

```bash
# SSH vào VPS
ssh root@your-vps-ip

# Clone project
cd /opt
git clone https://github.com/YOUR_USERNAME/DigitalLibrary.git
cd DigitalLibrary

# Done! Code đã có trên VPS
ls -la
```

---

## 📦 Cách 2: Zip + Upload (Nếu không dùng Git)

### Bước 1: Tạo file zip (trên Windows)

**PowerShell:**
```powershell
cd D:\Github\DigitalLibrary
# Tạo zip, bỏ qua target, .git
Compress-Archive -Path demo,docker,README.md,DEPLOYMENT_GUIDE.md -DestinationPath DigitalLibrary.zip
```

**Hoặc dùng WinRAR/7-Zip:**
- Chọn các thư mục: `demo`, `docker`, `README.md`, `DEPLOYMENT_GUIDE.md`
- Bỏ qua: `target`, `.git`, `uploads`, `logs`
- Nén thành `DigitalLibrary.zip`

### Bước 2: Upload lên VPS

**Dùng WinSCP (GUI - Dễ nhất):**
1. Download WinSCP: https://winscp.net/
2. Kết nối VPS:
   - Protocol: SFTP
   - Host: IP của VPS
   - Username: root
   - Password: password VPS
3. Upload file `DigitalLibrary.zip` vào `/opt/`
4. Right-click file → Extract here

**Hoặc dùng SCP (Command line):**
```bash
# Trên Windows (Git Bash hoặc PowerShell)
scp DigitalLibrary.zip root@your-vps-ip:/opt/
```

### Bước 3: Giải nén trên VPS

```bash
# SSH vào VPS
ssh root@your-vps-ip

# Giải nén
cd /opt
unzip DigitalLibrary.zip -d DigitalLibrary
cd DigitalLibrary

# Xóa file zip
rm DigitalLibrary.zip

# Done!
ls -la
```

---

## ✅ Sau khi code đã trên VPS

Tiếp tục các bước deploy:

```bash
cd /opt/DigitalLibrary

# 1. Kiểm tra code
ls -la
ls docker/

# 2. Tiếp tục theo DEPLOY_STEPS.md
cd docker
# ... (xem DEPLOY_STEPS.md)
```

---

## 🎯 Checklist

- [ ] Code đã có trên VPS tại `/opt/DigitalLibrary`
- [ ] Cấu trúc thư mục đúng (có `demo/`, `docker/`)
- [ ] File `docker/docker-compose.yml` tồn tại
- [ ] File `demo/pom.xml` tồn tại
- [ ] Sẵn sàng tiếp tục deploy
