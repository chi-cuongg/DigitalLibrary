# Hướng dẫn thiết lập Google Drive API - Bản rút gọn

## Bước 1: Tạo Service Account (5 phút)

1. Truy cập: https://console.cloud.google.com/
2. Chọn project hoặc tạo project mới
3. Vào **"APIs & Services"** > **"Library"**
4. Tìm **"Google Drive API"** và click **"ENABLE"**

## Bước 2: Tạo Service Account và tải Key

1. Vào **"APIs & Services"** > **"Credentials"**
2. Click **"CREATE CREDENTIALS"** > Chọn **"Service Account"**
3. Điền thông tin:
   - **Service account name**: `digital-library` (hoặc tên bất kỳ)
   - Click **"CREATE AND CONTINUE"**
   - Click **"DONE"** (skip bước 2 và 3)
4. Trong danh sách Service Accounts, click vào service account vừa tạo
5. Vào tab **"KEYS"**
6. Click **"ADD KEY"** > **"Create new key"**
7. Chọn format **JSON**
8. Click **"CREATE"** - File JSON sẽ tự động tải xuống (tên file sẽ là `[project-name]-[hash].json`)

## Bước 3: Chia sẻ Google Drive Folder với Service Account

1. Mở file JSON vừa tải về bằng Notepad hoặc text editor
2. Tìm dòng có `"client_email"` - ví dụ: `"client_email": "digital-library@my-project.iam.gserviceaccount.com"`
3. Copy email đó (ví dụ: `digital-library@my-project.iam.gserviceaccount.com`)
4. Mở Google Drive folder: https://drive.google.com/drive/u/0/folders/1D9yGQ_xBZWe9ouRAiVYVbjC80XXFOjVO
5. Click nút **"Chia sẻ"** (Share) ở góc trên bên phải
6. Paste email của Service Account vào ô "Thêm người và nhóm"
7. Chọn quyền **"Người chỉnh sửa"** (Editor)
8. Click **"Gửi"** (Send)

## Bước 4: Đặt file credentials vào project

1. Đổi tên file JSON vừa tải về thành `credentials.json`
2. Copy file vào một trong các vị trí sau:
   - `D:\code\DigitalLibrary\demo\src\main\resources\credentials.json` ✅ (Khuyến nghị)
   - HOẶC `D:\code\DigitalLibrary\demo\credentials.json` (ở thư mục gốc của project)

## Bước 5: Restart ứng dụng

1. Dừng ứng dụng (nếu đang chạy): Ctrl+C trong terminal
2. Khởi động lại:
   ```bash
   cd D:\code\DigitalLibrary\demo
   .\mvnw.cmd spring-boot:run
   ```

## Bước 6: Test

1. Vào trang crawler: http://localhost:8080/admin/crawler
2. Crawl một cuốn sách (ví dụ: `https://dtv-ebook.com.vn/luoc-su-tuong-lai_25762.html`)
3. Kiểm tra trong Google Drive folder: https://drive.google.com/drive/u/0/folders/1D9yGQ_xBZWe9ouRAiVYVbjC80XXFOjVO
4. File PDF sẽ xuất hiện trong folder đó!

## Troubleshooting

### Lỗi: "Google Drive credentials file not found"
- ✅ Kiểm tra file `credentials.json` đã được đặt đúng vị trí chưa
- ✅ Kiểm tra tên file phải đúng là `credentials.json` (không có số, không có ký tự đặc biệt)

### Lỗi: "Permission denied" hoặc "Access denied"
- ✅ Kiểm tra Service Account email đã được share với Google Drive folder chưa
- ✅ Kiểm tra quyền đã được set là "Editor" hoặc "Content Manager"

### Lỗi: "Google Drive API is not enabled"
- ✅ Vào Google Cloud Console và enable "Google Drive API"

### Vẫn lưu local thay vì Drive
- ✅ Kiểm tra log của ứng dụng, tìm dòng "Google Drive service initialized successfully"
- ✅ Nếu không thấy dòng đó, kiểm tra lại file credentials.json
- ✅ Restart ứng dụng sau khi thêm credentials.json

## Lưu ý quan trọng

⚠️ **KHÔNG commit file `credentials.json` lên Git!** File này chứa thông tin nhạy cảm.
- File đã được thêm vào `.gitignore` tự động

⚠️ Nếu bạn đã có sách được lưu local trước đây, chúng sẽ vẫn ở trong thư mục `uploads/`. 
- Chỉ có sách mới crawl sau khi setup credentials mới được upload lên Drive.
