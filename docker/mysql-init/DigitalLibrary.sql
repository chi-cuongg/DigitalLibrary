-- Sử dụng Database đã được Docker tạo sẵn
USE DigitalLibrary;

-- Xóa bảng cũ nếu tồn tại để tránh lỗi (theo thứ tự khóa ngoại)
DROP TABLE IF EXISTS book_review;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS book_file;
DROP TABLE IF EXISTS book_category;
DROP TABLE IF EXISTS category;
DROP TABLE IF EXISTS book;

-- Tạo bảng book
CREATE TABLE book (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255),
    description TEXT,
    image_url VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Tạo bảng category
CREATE TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

-- Tạo bảng book_category
CREATE TABLE book_category (
    book_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (book_id, category_id),
    FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE CASCADE
);

-- Tạo bảng book_file
CREATE TABLE book_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE
);

-- Tạo bảng users
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    full_name VARCHAR(255),
    enabled TINYINT(1) DEFAULT 1
);

-- Tạo bảng roles
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL
);

-- Tạo bảng user_roles
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Tạo bảng book_review
CREATE TABLE book_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_book_user UNIQUE (book_id, user_id),
    FOREIGN KEY (book_id) REFERENCES book(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- INSERT DỮ LIỆU
INSERT INTO book (title, author, description, image_url) VALUES
('Lập trình Java cơ bản', 'Nguyễn Văn A', 'Java cho người mới', '/images/java.jpg'),
('Spring Boot từ A đến Z', 'Trần Văn B', 'Spring Boot + Thymeleaf', '/images/spring.jpg'),
('Cấu trúc dữ liệu', 'Lê Văn C', 'DSA căn bản', '/images/dsa.jpg');

INSERT INTO category (name) VALUES
('Java'), ('Spring Boot'), ('Lập trình Web'), ('Cấu trúc dữ liệu');

INSERT INTO book_category (book_id, category_id)
SELECT b.id, c.id FROM book b, category c 
WHERE b.title = 'Lập trình Java cơ bản' AND c.name = 'Java';

INSERT INTO book_category (book_id, category_id)
SELECT b.id, c.id FROM book b, category c 
WHERE b.title = 'Spring Boot từ A đến Z' AND c.name IN ('Java', 'Spring Boot');

INSERT INTO users (username, password, email, full_name) VALUES
('admin', 'admin123', 'admin@gmail.com', 'Quản trị viên'),
('user1', 'user123', 'user1@gmail.com', 'Người dùng 1'),
('user2', 'user123', 'user2@gmail.com', 'Người dùng 2');

INSERT INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_USER');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON u.username = 'admin' WHERE r.name IN ('ROLE_ADMIN', 'ROLE_USER');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON u.username IN ('user1', 'user2') WHERE r.name = 'ROLE_USER';

INSERT INTO book_review (book_id, user_id, rating, comment)
SELECT b.id, u.id, 5, 'Sách rất hay, dễ hiểu' FROM book b, users u 
WHERE b.title = 'Lập trình Java cơ bản' AND u.username = 'user1';
