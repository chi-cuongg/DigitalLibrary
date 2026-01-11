# Vấn đề với Service Account và Giải pháp

## Vấn đề

Khi sử dụng Service Account để upload file lên Google Drive, bạn gặp lỗi:

```
403 Forbidden
"Service Accounts do not have storage quota. Leverage shared drives or use OAuth delegation instead."
"reason": "storageQuotaExceeded"
```

**Nguyên nhân:** Service Account không có storage quota riêng và không thể tạo file mới trong folder của user, ngay cả khi folder đó được share với Service Account.

## Giải pháp

Có 3 giải pháp có thể:

### Giải pháp 1: OAuth 2.0 (Khuyến nghị) ✅

Sử dụng OAuth 2.0 để user authorize ứng dụng upload vào Drive của họ. Đây là giải pháp phù hợp nhất.

**Ưu điểm:**
- Hoạt động với Google Drive personal account
- Không cần Google Workspace
- File được upload vào Drive của user
- Không có hạn chế storage quota

**Nhược điểm:**
- Cần user authorize lần đầu
- Cần implement OAuth flow

### Giải pháp 2: Shared Drives (Google Workspace)

Sử dụng Shared Drives nếu bạn có Google Workspace account.

**Yêu cầu:**
- Cần Google Workspace account
- Folder phải là Shared Drive (không phải folder trong My Drive)

### Giải pháp 3: Domain-wide Delegation (Google Workspace)

Chỉ hoạt động với Google Workspace và cần admin setup.

## Khuyến nghị

**Để tiếp tục sử dụng Service Account**, bạn có 2 lựa chọn:

1. **Chấp nhận giới hạn:** Tiếp tục lưu file local, không upload lên Drive
2. **Chuyển sang OAuth 2.0:** Implement OAuth 2.0 để upload vào Drive của user

Hiện tại code đã được cấu hình để fallback về local storage khi Drive upload fails, nên ứng dụng vẫn hoạt động bình thường, chỉ là file không được lưu lên Drive.
