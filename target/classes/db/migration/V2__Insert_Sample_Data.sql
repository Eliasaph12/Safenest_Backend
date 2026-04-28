-- Insert sample users
INSERT INTO users (email, password, first_name, last_name) VALUES
('admin@safenest.com', 'hashed_password_1', 'Admin', 'User'),
('john.doe@example.com', 'hashed_password_2', 'John', 'Doe'),
('jane.smith@example.com', 'hashed_password_3', 'Jane', 'Smith');

-- Insert sample products
INSERT INTO products (name, description, price, stock_quantity) VALUES
('Security Camera 1080p', 'High-definition home security camera with night vision', 99.99, 50),
('Door Lock Smart', 'WiFi-enabled smart door lock with app control', 149.99, 30),
('Motion Sensor', 'Advanced motion detection sensor for outdoor use', 49.99, 100),
('Alarm System', 'Multi-zone home alarm system with backup battery', 199.99, 25),
('Glass Break Detector', 'Acoustic glass break sensor for windows', 29.99, 75);
