# Hướng dẫn thiết lập OAuth 2.0 cho Google Drive

## Bước 1: Tạo OAuth 2.0 Credentials trên Google Cloud Console

1. Truy cập [Google Cloud Console](https://console.cloud.google.com/)
2. Chọn project của bạn (hoặc tạo project mới)
3. Vào **APIs & Services** > **Credentials**
4. Click **+ CREATE CREDENTIALS** > **OAuth client ID**
5. Nếu lần đầu, bạn cần cấu hình OAuth consent screen:
   - Chọn **External** (cho personal account)
   - Điền thông tin ứng dụng
   - Thêm scopes: `https://www.googleapis.com/auth/drive`
   - Thêm test users (email của bạn) nếu ở chế độ Testing
   - Lưu và tiếp tục
6. Tạo OAuth Client ID:
   - **Application type**: Web application
   - **Name**: Digital Library OAuth Client
   - **Authorized redirect URIs**: 
     - `http://localhost:8080/oauth2/callback` (cho development)
     - `https://yourdomain.com/oauth2/callback` (cho production)
   - Click **Create**
7. Copy **Client ID** và **Client Secret**

## Bước 2: Cấu hình trong application.properties

Thêm các thông tin sau vào `application.properties`:

```properties
# Google OAuth 2.0 Configuration
google.oauth2.client.id=YOUR_CLIENT_ID_HERE
google.oauth2.client.secret=YOUR_CLIENT_SECRET_HERE
google.oauth2.redirect.uri=http://localhost:8080/oauth2/callback
```

Thay `YOUR_CLIENT_ID_HERE` và `YOUR_CLIENT_SECRET_HERE` bằng Client ID và Client Secret bạn đã copy.

## Bước 3: Authorize ứng dụng

1. Khởi động ứng dụng
2. Đăng nhập với tài khoản admin
3. Vào trang quản lý (có thể thêm link "Connect Google Drive" ở dashboard)
4. Click "Connect Google Drive" để bắt đầu OAuth flow
5. Đăng nhập Google và authorize ứng dụng
6. Sau khi authorize, refresh token sẽ được lưu và ứng dụng có thể upload file vào Drive của bạn

## Lưu ý

- Refresh token sẽ được lưu trong database (bảng `oauth_tokens`)
- Mỗi lần authorize, refresh token mới sẽ thay thế token cũ
- Refresh token có thể expire nếu không sử dụng trong thời gian dài
- Nếu token expire, bạn cần authorize lại

## Troubleshooting

- **Lỗi "redirect_uri_mismatch"**: Đảm bảo redirect URI trong Google Cloud Console khớp với redirect URI trong application.properties
- **Lỗi "access_denied"**: Kiểm tra OAuth consent screen đã được cấu hình đúng
- **Token expired**: Authorize lại ứng dụng
