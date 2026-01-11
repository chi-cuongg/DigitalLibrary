# Hướng dẫn Setup OAuth 2.0 cho Google Drive - Step by Step

## ✅ Bước 1: Đã hoàn thành - Cấu hình Credentials

Bạn đã có:
- ✅ Client ID: `509081543880-ssjdjuvpt9tbja3oo1lpucnhtihkrqd2.apps.googleusercontent.com`
- ✅ Client Secret: `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`
- ✅ Đã cấu hình trong `application.properties`

## ⚠️ Bước 2: Kiểm tra OAuth Consent Screen (QUAN TRỌNG)

1. Truy cập [Google Cloud Console](https://console.cloud.google.com/)
2. Chọn project của bạn
3. Vào **APIs & Services** > **OAuth consent screen**
4. Đảm bảo:
   - **User Type**: External (cho personal account)
   - **App name**: Đã điền tên ứng dụng
   - **User support email**: Email của bạn
   - **Developer contact information**: Email của bạn
   - **Scopes**: Phải có `https://www.googleapis.com/auth/drive`
   - **Test users**: Nếu app ở chế độ Testing, phải thêm email Google của bạn vào danh sách test users

5. Nếu chưa có scope `https://www.googleapis.com/auth/drive`:
   - Click **ADD OR REMOVE SCOPES**
   - Tìm và chọn: `.../auth/drive` (Google Drive API)
   - Click **UPDATE** và **SAVE AND CONTINUE**

## ⚠️ Bước 3: Kiểm tra Authorized Redirect URIs

1. Vào **APIs & Services** > **Credentials**
2. Click vào OAuth 2.0 Client ID của bạn
3. Kiểm tra **Authorized redirect URIs** có:
   - `http://localhost:8080/oauth2/callback`
4. Nếu chưa có, click **ADD URI** và thêm:
   - `http://localhost:8080/oauth2/callback`
5. Click **SAVE**

## 🚀 Bước 4: Khởi động ứng dụng

1. Mở terminal/command prompt
2. Di chuyển đến thư mục project:
   ```bash
   cd demo
   ```
3. Khởi động ứng dụng:
   ```bash
   mvn spring-boot:run
   ```
   Hoặc nếu bạn dùng IDE, chạy `DemoApplication.java`

4. Đợi ứng dụng khởi động (thường mất 30-60 giây)
5. Mở trình duyệt và truy cập: `http://localhost:8080`

## 🔐 Bước 5: Đăng nhập và Authorize Google Drive

1. Đăng nhập vào ứng dụng với tài khoản admin
2. Sau khi đăng nhập, bạn sẽ thấy trang **Dashboard**
3. Trong phần **"Kết nối Google Drive"**:
   - Nếu thấy "⚠️ Google Drive chưa được kết nối"
   - Click nút **"Kết nối Google Drive"**

4. Trình duyệt sẽ chuyển hướng đến trang Google OAuth:
   - Chọn tài khoản Google của bạn
   - Click **"Cho phép"** (Allow) để cấp quyền truy cập Google Drive

5. Sau khi authorize thành công:
   - Trình duyệt sẽ tự động quay lại ứng dụng
   - Bạn sẽ thấy thông báo: **"✅ Google Drive đã được kết nối thành công!"**
   - Status sẽ thay đổi thành **"Đã kết nối"**

## ✅ Bước 6: Kiểm tra và Test

1. Vào trang **Dashboard**, kiểm tra:
   - Google Drive status phải hiển thị **"Đã kết nối"**
   - Card Google Drive có màu xanh (success)

2. Test upload file:
   - Vào **Crawler** (`/admin/crawler`)
   - Crawl một vài sách
   - Kiểm tra logs để xem file có được upload lên Drive không
   - Kiểm tra Google Drive folder: https://drive.google.com/drive/u/0/folders/1D9yGQ_xBZWe9ouRAiVYVbjC80XXFOjVO

## 🔧 Troubleshooting

### Lỗi "redirect_uri_mismatch"
- **Nguyên nhân**: Redirect URI trong Google Cloud Console không khớp
- **Giải pháp**: 
  - Vào Google Cloud Console > Credentials > OAuth 2.0 Client ID
  - Đảm bảo có: `http://localhost:8080/oauth2/callback` (chính xác, không có dấu / ở cuối)
  - Click **SAVE**

### Lỗi "access_denied"
- **Nguyên nhân**: OAuth consent screen chưa được cấu hình hoặc email chưa được thêm vào test users
- **Giải pháp**:
  - Vào OAuth consent screen
  - Thêm email Google của bạn vào **Test users** (nếu app ở chế độ Testing)
  - Đảm bảo scope `.../auth/drive` đã được thêm

### Lỗi "invalid_client"
- **Nguyên nhân**: Client ID hoặc Client Secret sai
- **Giải pháp**: Kiểm tra lại `application.properties` đã điền đúng Client ID và Client Secret chưa

### Token expired
- **Nguyên nhân**: Refresh token đã hết hạn
- **Giải pháp**: Click **"Kết nối lại Google Drive"** để authorize lại

### Không thấy file trong Drive sau khi crawl
- Kiểm tra logs trong `demo/logs/google-drive.log`
- Kiểm tra folder ID có đúng không: `1D9yGQ_xBZWe9ouRAiVYVbjC80XXFOjVO`
- Đảm bảo đã authorize thành công (status "Đã kết nối")

## 📝 Lưu ý quan trọng

1. **OAuth Consent Screen**: Phải được cấu hình đầy đủ, đặc biệt là scope `.../auth/drive`
2. **Test Users**: Nếu app ở chế độ Testing, email của bạn phải được thêm vào test users
3. **Redirect URI**: Phải chính xác: `http://localhost:8080/oauth2/callback`
4. **Refresh Token**: Được lưu trong database (bảng `oauth_tokens`), có thể dùng lại cho đến khi expire
5. **Folder ID**: File sẽ được upload vào folder: `1D9yGQ_xBZWe9ouRAiVYVbjC80XXFOjVO`

## 🎉 Hoàn thành!

Sau khi hoàn thành các bước trên, bạn có thể:
- ✅ Crawl sách và file sẽ tự động được lưu lên Google Drive
- ✅ Upload sách qua admin panel và file sẽ được lưu lên Google Drive
- ✅ File được lưu trong folder Google Drive của bạn thay vì chỉ lưu local
