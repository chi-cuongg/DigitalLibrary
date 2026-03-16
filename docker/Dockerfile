# Sử dụng JRE nhẹ để chạy ứng dụng
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Cài đặt curl để phục vụ việc kiểm tra nếu cần
RUN apk add --no-cache curl

# Tạo các thư mục lưu trữ
RUN mkdir -p uploads logs

# Copy file .jar từ máy EC2 vào trong image 
# (Vì bạn đang đứng ở DigitalLibrary/docker nên dùng ../demo/target/...)
COPY ../demo/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Phân quyền cho user spring để bảo mật (tùy chọn, có thể bỏ qua nếu muốn chạy quyền root cho nhanh)
#RUN addgroup -S spring && adduser -S spring -G spring
#RUN chown -R spring:spring /app
#USER spring:spring

EXPOSE 8080

# Cấu hình RAM "siêu tiết kiệm" cho máy 2GB
ENTRYPOINT ["java", "-Xms128m", "-Xmx512m", "-XX:+UseSerialGC", "-jar", "app.jar"]
