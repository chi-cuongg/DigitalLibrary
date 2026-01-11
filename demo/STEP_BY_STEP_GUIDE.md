# 📚 HƯỚNG DẪN TỪNG BƯỚC - Setup Google Drive API

## 🎯 MỤC TIÊU
Upload sách lên Google Drive thay vì lưu local trên máy.

---

## 📝 BƯỚC 1: Tạo Project trên Google Cloud Console

1. **Mở trình duyệt** và truy cập: https://console.cloud.google.com/
2. **Đăng nhập** bằng tài khoản Google của bạn
3. Nếu chưa có project:
   - Click nút **"Select a project"** ở đầu trang (góc trên bên trái)
   - Click **"NEW PROJECT"**
   - Điền tên project: `DigitalLibrary` (hoặc tên bất kỳ)
   - Click **"CREATE"**
   - Đợi vài giây cho project được tạo

4. **Chọn project vừa tạo** (nếu chưa được chọn)

---

## 🔌 BƯỚC 2: Bật Google Drive API

1. Trong Google Cloud Console, click vào menu **☰** (3 gạch ngang) ở góc trên bên trái
2. Chọn **"APIs & Services"** > **"Library"**
3. Trong ô tìm kiếm, gõ: `Google Drive API`
4. Click vào **"Google Drive API"** trong kết quả
5. Click nút **"ENABLE"** (màu xanh)
6. Đợi vài giây cho API được bật (sẽ hiện "API enabled")

---

## 🔑 BƯỚC 3: Tạo Service Account

1. Vẫn trong Google Cloud Console, click menu **☰** > **"APIs & Services"** > **"Credentials"**
2. Ở đầu trang, click nút **"+ CREATE CREDENTIALS"**
3. Chọn **"Service Account"** trong dropdown
4. Điền form:
   - **Service account name**: `digital-library-drive` (hoặc tên bất kỳ)
   - **Service account ID**: Sẽ tự động điền (giống tên trên)
   - **Description** (tùy chọn): `Service account for uploading books to Google Drive`
5. Click **"CREATE AND CONTINUE"**
6. Ở bước "Grant this service account access to project" - **BỎ QUA** (không cần làm gì)
7. Click **"DONE"**

✅ Service Account đã được tạo! Bạn sẽ thấy nó trong danh sách.

---

## 📥 BƯỚC 4: Tạo và Tải Key (JSON)

1. Trong danh sách Service Accounts, **click vào tên** service account vừa tạo (ví dụ: `digital-library-drive@...`)
2. Vào tab **"KEYS"** (ở trên cùng, bên cạnh "DETAILS")
3. Click nút **"+ ADD KEY"**
4. Chọn **"Create new key"**
5. Trong popup:
   - Chọn format: **JSON** (đã được chọn mặc định)
   - Click **"CREATE"**
6. ⚠️ File JSON sẽ **TỰ ĐỘNG TẢI XUỐNG** về máy tính của bạn
   - Tên file sẽ là: `[project-name]-[random-hash].json`
   - Ví dụ: `digital-library-1234567890-abcdef.json`
   - File thường được lưu vào thư mục **Downloads**

---

## 📧 BƯỚC 5: Lấy Email của Service Account

1. **Mở file JSON vừa tải về** bằng Notepad hoặc trình soạn thảo bất kỳ
2. Tìm dòng có `"client_email"`, ví dụ:
   ```json
   "client_email": "digital-library-drive@my-project-123456.iam.gserviceaccount.com",
   ```
3. **Copy toàn bộ email** đó (bao gồm cả phần @...)
   - Ví dụ: `digital-library-drive@my-project-123456.iam.gserviceaccount.com`

💾 **Lưu email này lại** để dùng ở bước tiếp theo!

---

## 🔗 BƯỚC 6: Chia sẻ Google Drive Folder với Service Account

1. **Mở Google Drive** trong trình duyệt: https://drive.google.com/
2. Mở folder cần upload sách vào: https://drive.google.com/drive/u/0/folders/1D9yGQ_xBZWe9ouRAiVYVbjC80XXFOjVO
3. Click nút **"Chia sẻ"** (hoặc **"Share"**) ở góc trên bên phải
4. Trong popup "Chia sẻ với người và nhóm":
   - Paste **email của Service Account** (đã copy ở bước 5) vào ô "Thêm người và nhóm"
   - Bấm **Tab** hoặc **Enter**
   - Ở cột "Vai trò", chọn **"Người chỉnh sửa"** (hoặc **"Editor"**)
5. **BỎ CHỌN** checkbox "Thông báo cho người dùng" (không cần)
6. Click **"Gửi"** (hoặc **"Send"**)

✅ Folder đã được chia sẻ với Service Account!

---

## 📁 BƯỚC 7: Đặt file credentials.json vào project

1. **Đổi tên file JSON** vừa tải về thành: `credentials.json`
   - Click phải vào file > **Rename**
   - Xóa phần tên dài, chỉ giữ: `credentials.json`

2. **Copy file** `credentials.json` vào một trong các vị trí sau:

   **Option 1 (Khuyến nghị):**
   - Đường dẫn: `D:\code\DigitalLibrary\demo\src\main\resources\credentials.json`
   - Tạo thư mục `resources` nếu chưa có

   **Option 2:**
   - Đường dẫn: `D:\code\DigitalLibrary\demo\credentials.json`
   - Đặt ở thư mục gốc của project (cùng cấp với file `pom.xml`)

3. ✅ **Kiểm tra** file đã được đặt đúng:
   - Mở File Explorer
   - Điều hướng đến `D:\code\DigitalLibrary\demo\src\main\resources\`
   - Xác nhận có file `credentials.json` ở đó

---

## 🚀 BƯỚC 8: Restart ứng dụng

1. **Dừng ứng dụng** (nếu đang chạy):
   - Mở terminal/PowerShell
   - Nhấn **Ctrl + C** để dừng

2. **Khởi động lại**:
   ```powershell
   cd D:\code\DigitalLibrary\demo
   .\mvnw.cmd spring-boot:run
   ```

3. **Quan sát log** khi ứng dụng khởi động:
   - Tìm dòng: `"Google Drive service initialized successfully with folder ID: 1D9yGQ_xBZWe9ouRAiVYVbjC80XXFOjVO"`
   - ✅ Nếu thấy dòng này = **THÀNH CÔNG!**
   - ❌ Nếu thấy: `"Google Drive credentials file NOT FOUND!"` = Kiểm tra lại bước 7

---

## 🧪 BƯỚC 9: Test upload lên Drive

1. **Mở trình duyệt**, truy cập: http://localhost:8080/admin/crawler

2. **Đăng nhập** (nếu chưa)

3. **Crawl một cuốn sách**:
   - URL: `https://dtv-ebook.com.vn/luoc-su-tuong-lai_25762.html`
   - Số lượng sách tối đa: `1`
   - ✅ Đánh dấu: **"Tải file sách về máy"**
   - Click **"Bắt đầu Crawl"**

4. **Quan sát log** trong terminal:
   - Tìm dòng: `"File uploaded to Google Drive: [tên file] (ID: [file-id])"`
   - ✅ Nếu thấy = Upload thành công!

5. **Kiểm tra trên Google Drive**:
   - Mở: https://drive.google.com/drive/u/0/folders/1D9yGQ_xBZWe9ouRAiVYVbjC80XXFOjVO
   - ✅ **File PDF sẽ xuất hiện** trong folder này!

---

## ❓ TROUBLESHOOTING (Xử lý lỗi)

### ❌ Lỗi: "Google Drive credentials file NOT FOUND"
**Nguyên nhân**: File `credentials.json` chưa được đặt đúng vị trí

**Giải pháp**:
1. Kiểm tra file có tên đúng là `credentials.json` (không có số, không có ký tự đặc biệt)
2. Kiểm tra đường dẫn: `D:\code\DigitalLibrary\demo\src\main\resources\credentials.json`
3. Đảm bảo file không bị ẩn (Hidden)
4. Restart ứng dụng sau khi di chuyển file

---

### ❌ Lỗi: "Permission denied" hoặc "Access denied"
**Nguyên nhân**: Service Account chưa được chia sẻ với Google Drive folder

**Giải pháp**:
1. Kiểm tra lại bước 6
2. Đảm bảo email Service Account đã được thêm vào folder
3. Đảm bảo quyền là **"Người chỉnh sửa"** (Editor) hoặc **"Content Manager"**
4. Thử xóa và share lại folder với Service Account

---

### ❌ Lỗi: "Google Drive API is not enabled"
**Nguyên nhân**: Chưa bật Google Drive API

**Giải pháp**:
1. Quay lại bước 2
2. Vào Google Cloud Console > APIs & Services > Library
3. Tìm "Google Drive API" và bật nó

---

### ❌ Vẫn lưu local thay vì Drive
**Nguyên nhân**: Có thể credentials.json chưa đúng format hoặc thiếu quyền

**Giải pháp**:
1. Kiểm tra log khi ứng dụng khởi động
2. Tìm dòng: `"Loading credentials from..."` để xác nhận file được tìm thấy
3. Tìm dòng: `"Google Drive service initialized successfully"` để xác nhận khởi tạo thành công
4. Nếu không thấy, kiểm tra lại file credentials.json có đúng format JSON không

---

## ✅ CHECKLIST HOÀN THÀNH

Sau khi làm xong, bạn nên có:

- [ ] Google Cloud Project đã tạo
- [ ] Google Drive API đã được bật
- [ ] Service Account đã được tạo
- [ ] File JSON key đã được tải về
- [ ] File đã được đổi tên thành `credentials.json`
- [ ] File `credentials.json` đã được đặt trong `src/main/resources/`
- [ ] Google Drive folder đã được share với Service Account email
- [ ] Ứng dụng đã được restart
- [ ] Log hiển thị: "Google Drive service initialized successfully"
- [ ] Test crawl sách thành công
- [ ] File xuất hiện trong Google Drive folder

---

## 🎉 HOÀN TẤT!

Nếu bạn đã hoàn thành tất cả các bước trên, hệ thống sẽ tự động upload sách lên Google Drive mỗi khi bạn crawl!

**Lưu ý quan trọng**:
- ⚠️ **KHÔNG** commit file `credentials.json` lên Git (đã được thêm vào `.gitignore`)
- 📁 Sách đã crawl trước đó vẫn ở local (trong thư mục `uploads/`)
- 🆕 Chỉ có sách mới crawl sau khi setup credentials mới được upload lên Drive
