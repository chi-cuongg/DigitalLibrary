# Hướng dẫn thiết lập Google Drive API

## Bước 1: Tạo Service Account trên Google Cloud Console

1. Truy cập [Google Cloud Console](https://console.cloud.google.com/)
2. Tạo một project mới hoặc chọn project hiện có
3. Bật Google Drive API:
   - Vào "APIs & Services" > "Library"
   - Tìm "Google Drive API" và bật nó

## Bước 2: Tạo Service Account

1. Vào "APIs & Services" > "Credentials"
2. Click "Create Credentials" > "Service Account"
3. Điền thông tin:
   - Service account name: `digital-library-drive`
   - Service account ID: tự động tạo
   - Description: `Service account for Digital Library file storage`
4. Click "Create and Continue"
5. Skip "Grant this service account access to project" (Optional)
6. Click "Done"

## Bước 3: Tạo và tải Key

1. Trong danh sách Service Accounts, click vào service account vừa tạo
2. Vào tab "Keys"
3. Click "Add Key" > "Create new key"
4. Chọn JSON format
5. Click "Create" - file JSON sẽ được tải xuống

## Bước 4: Chia sẻ Google Drive Folder với Service Account

1. Mở file JSON đã tải về, tìm trường `client_email` (ví dụ: `digital-library-drive@project-id.iam.gserviceaccount.com`)
2. Vào Google Drive, mở folder: https://drive.google.com/drive/u/0/folders/1D9yGQ_xBZWe9ouRAiVYVbjC80XXFOjVO
3. Click phải vào folder > "Share"
4. Paste email của service account vào ô "Add people and groups"
5. Chọn quyền "Editor" hoặc "Content Manager"
6. Click "Send"

## Bước 5: Đặt file credentials.json vào project

1. Đổi tên file JSON đã tải về thành `credentials.json`
2. Đặt file vào một trong các vị trí sau:
   - `src/main/resources/credentials.json` (khuyến nghị - trong classpath)
   - `credentials.json` ở thư mục gốc của project (cùng cấp với pom.xml)

## Bước 6: Cấu hình trong application.properties

File `application.properties` đã được cấu hình sẵn:
```properties
google.drive.folder.id=1D9yGQ_xBZWe9ouRAiVYVbjC80XXFOjVO
google.drive.credentials.path=credentials.json
```

Bạn có thể thay đổi các giá trị này nếu cần.

## Lưu ý

- File `credentials.json` chứa thông tin nhạy cảm, **KHÔNG** commit lên Git!
- Thêm `credentials.json` vào `.gitignore`
- Đảm bảo service account có quyền truy cập vào folder Google Drive
- Khi crawl sách, file sẽ được upload tự động lên Google Drive thay vì lưu local

## Kiểm tra

Sau khi setup xong, khởi động ứng dụng và thử crawl một cuốn sách. File sẽ được upload lên Google Drive folder đã cấu hình.
