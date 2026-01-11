# 🔍 DEBUG: Tại sao vẫn lưu local thay vì Google Drive?

## ✅ BƯỚC 1: Xác nhận file credentials.json đã có

1. Mở File Explorer
2. Điều hướng đến: `D:\code\DigitalLibrary\demo\src\main\resources\`
3. Kiểm tra xem có file `credentials.json` không
4. **Nếu không có**: Xem lại file `STEP_BY_STEP_GUIDE.md` từ bước 7

---

## 🔄 BƯỚC 2: RESTART ứng dụng (QUAN TRỌNG!)

**Bạn PHẢI restart ứng dụng sau khi đặt credentials.json!**

1. **Dừng ứng dụng đang chạy**:
   - Mở terminal/PowerShell nơi ứng dụng đang chạy
   - Nhấn **Ctrl + C** để dừng

2. **Khởi động lại**:
   ```powershell
   cd D:\code\DigitalLibrary\demo
   .\mvnw.cmd spring-boot:run
   ```

3. **Quan sát LOG khi khởi động** - Tìm các dòng sau:

   **✅ THÀNH CÔNG** - Nếu thấy:
   ```
   ✅ Loading credentials from classpath: credentials.json
   📝 Loading Google credentials from stream...
   ✅ Credentials loaded successfully
   🔧 Building Drive service...
   ✅✅✅ Google Drive service initialized successfully! ✅✅✅
   📁 Target folder ID: 1D9yGQ_xBZWe9ouRAiVYVbjC80XXFOjVO
   ```

   **❌ THẤT BẠI** - Nếu thấy:
   ```
   ❌ Google Drive credentials file NOT FOUND!
   ⚠️ Google Drive integration is DISABLED. Files will be saved locally.
   ```

   **Hoặc lỗi khác**:
   ```
   ❌❌❌ ERROR initializing Google Drive service: [thông báo lỗi]
   ```

---

## 🧪 BƯỚC 3: Test crawl và xem log

1. Mở trình duyệt: http://localhost:8080/admin/crawler
2. Crawl một cuốn sách:
   - URL: `https://dtv-ebook.com.vn/luoc-su-tuong-lai_25762.html`
   - Số lượng: `1`
   - ✅ Đánh dấu: "Tải file sách về máy"
   - Click "Bắt đầu Crawl"

3. **Xem LOG trong terminal** - Tìm các dòng:

   **✅ Nếu upload lên Drive THÀNH CÔNG:**
   ```
   🔍 Google Drive availability check: ✅ AVAILABLE
   🚀 Attempting to upload file from URL to Google Drive: [URL]
   ✅✅✅ File uploaded to Google Drive successfully! ✅✅✅
   📎 Drive File ID: [một chuỗi dài như: 1ABC...XYZ]
   📊 File size: [số] bytes
   ```

   **❌ Nếu Drive KHÔNG khả dụng:**
   ```
   🔍 Google Drive availability check: ❌ NOT AVAILABLE
   ⚠️ Google Drive is not available, using local storage
   📥 Downloading file to local storage: [URL]
   ✅ File saved locally as: [tên file]
   ```

   **❌ Nếu upload Drive THẤT BẠI:**
   ```
   🔍 Google Drive availability check: ✅ AVAILABLE
   🚀 Attempting to upload file from URL to Google Drive: [URL]
   ❌❌❌ Google Drive upload FAILED! ❌❌❌
   Error message: [thông báo lỗi]
   🔄 Falling back to LOCAL STORAGE...
   ```

---

## 🐛 CÁC LỖI THƯỜNG GẶP VÀ CÁCH XỬ LÝ

### Lỗi 1: "Google Drive credentials file NOT FOUND"

**Nguyên nhân**: File `credentials.json` không được tìm thấy

**Giải pháp**:
1. Kiểm tra file có tên đúng là `credentials.json` (không có số, không có extension khác)
2. Đặt file tại: `D:\code\DigitalLibrary\demo\src\main\resources\credentials.json`
3. **Restart ứng dụng** sau khi di chuyển file

---

### Lỗi 2: "403 Forbidden" hoặc "Permission denied"

**Nguyên nhân**: Service Account chưa được chia sẻ với Google Drive folder

**Giải pháp**:
1. Mở file `credentials.json` bằng Notepad
2. Tìm dòng `"client_email"`, copy email (ví dụ: `digital-library-drive@...`)
3. Mở Google Drive folder: https://drive.google.com/drive/u/0/folders/1D9yGQ_xBZWe9ouRAiVYVbjC80XXFOjVO
4. Click "Chia sẻ"
5. Paste email Service Account vào
6. Chọn quyền: **"Người chỉnh sửa"** (Editor)
7. Click "Gửi"

---

### Lỗi 3: "401 Unauthorized" hoặc "Invalid credentials"

**Nguyên nhân**: File `credentials.json` sai format hoặc không hợp lệ

**Giải pháp**:
1. Tải lại file JSON key từ Google Cloud Console
2. Đảm bảo file là JSON hợp lệ (mở bằng Notepad, kiểm tra có đủ `{}`, `[]`, `,`)
3. Đổi tên thành `credentials.json`
4. Đặt vào `src/main/resources/`
5. **Restart ứng dụng**

---

### Lỗi 4: "File is not a PDF" hoặc "Content-Type: text/html"

**Nguyên nhân**: Link download không phải là file PDF thực sự

**Giải pháp**: Đây là lỗi của website nguồn, không phải lỗi cấu hình. Hệ thống sẽ tự động fallback về local storage.

---

### Lỗi 5: Ứng dụng chạy nhưng vẫn lưu local

**Nguyên nhân**: 
- Ứng dụng chưa được restart sau khi đặt credentials.json
- Hoặc có exception khi khởi tạo Drive service nhưng không được log ra

**Giải pháp**:
1. **Restart ứng dụng** (Ctrl+C rồi chạy lại)
2. Xem log khi khởi động để xác nhận Drive service đã được khởi tạo
3. Xem log khi crawl để biết lý do fallback về local

---

## 📋 CHECKLIST KIỂM TRA

Trước khi test lại, hãy đảm bảo:

- [ ] File `credentials.json` có tại `src/main/resources/credentials.json`
- [ ] File `credentials.json` là JSON hợp lệ (có thể mở bằng Notepad)
- [ ] File có chứa field `"client_email"` và `"private_key"`
- [ ] Google Drive folder đã được share với Service Account email
- [ ] Ứng dụng đã được **RESTART** sau khi đặt credentials.json
- [ ] Log khi khởi động hiển thị: "Google Drive service initialized successfully"
- [ ] Log khi crawl hiển thị: "Google Drive availability check: ✅ AVAILABLE"

---

## 🔍 LẤY LOG ĐỂ DEBUG

Nếu vẫn không hoạt động, hãy copy toàn bộ log từ:
1. **Khi ứng dụng khởi động** - từ dòng "Starting DemoApplication" đến khi khởi động xong
2. **Khi crawl sách** - từ dòng "Starting crawl" đến khi hoàn thành

Gửi log cho tôi để phân tích!

---

## 💡 MẸO

1. **Luôn restart** ứng dụng sau khi thay đổi `credentials.json`
2. **Kiểm tra log** để biết chính xác điều gì đang xảy ra
3. Nếu thấy log "Google Drive service initialized successfully" nhưng vẫn lưu local, thì có thể là lỗi khi upload (xem log chi tiết)
