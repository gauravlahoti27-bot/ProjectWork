-- ===================================================================
-- TrekMate - MySQL Database Schema & Sample Data
-- For 2nd Year Mini Project (MPR)
-- Only MySQL syntax used
-- ===================================================================

CREATE DATABASE IF NOT EXISTS `trekmate_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `trekmate_db`;

-- 1. Users Table (Trekker & Host accounts)
CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `full_name` VARCHAR(100) NOT NULL,
    `email` VARCHAR(120) NOT NULL UNIQUE,
    `password` VARCHAR(100) NOT NULL,
    `phone` VARCHAR(20),
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER', -- 'USER' or 'HOST'
    `wallet_balance` DECIMAL(12,2) NOT NULL DEFAULT 25000.00,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Treks Table (Trek packages and destinations)
CREATE TABLE IF NOT EXISTS `treks` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(120) NOT NULL,
    `destination` VARCHAR(100) NOT NULL,
    `state` VARCHAR(60),
    `difficulty` VARCHAR(30) NOT NULL, -- 'Easy', 'Moderate', 'Challenging', 'Difficult'
    `duration_days` INT NOT NULL,
    `duration_nights` INT NOT NULL,
    `base_price` DECIMAL(10,2) NOT NULL,
    `description` TEXT,
    `altitude_meters` INT,
    `fitness_level` VARCHAR(50),
    `best_season` VARCHAR(100),
    `image_url` VARCHAR(300),
    `active` BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- 3. Trek Dates Table (Host-managed batch schedules and slot capacity)
CREATE TABLE IF NOT EXISTS `trek_dates` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `trek_id` BIGINT NOT NULL,
    `start_date` DATE NOT NULL,
    `end_date` DATE NOT NULL,
    `total_slots` INT NOT NULL DEFAULT 20,
    `booked_slots` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN', -- 'OPEN', 'FULL', 'CANCELLED'
    CONSTRAINT `fk_trek_dates_trek` FOREIGN KEY (`trek_id`) REFERENCES `treks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. Activities & Facilities Catalogue
CREATE TABLE IF NOT EXISTS `activities` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `category` VARCHAR(30) NOT NULL, -- 'ACTIVITY' or 'FACILITY'
    `description` TEXT,
    `price` DECIMAL(10,2) NOT NULL,
    `pricing_type` VARCHAR(20) NOT NULL, -- 'PER_PERSON' or 'PER_GROUP'
    `icon_name` VARCHAR(50),
    `active` BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- 5. Bookings Table (Submitted trek requests)
CREATE TABLE IF NOT EXISTS `bookings` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `booking_code` VARCHAR(30) NOT NULL UNIQUE,
    `user_id` BIGINT NOT NULL,
    `trek_id` BIGINT NOT NULL,
    `trek_date_id` BIGINT NOT NULL,
    `group_size` INT NOT NULL DEFAULT 1,
    `base_cost` DECIMAL(10,2) NOT NULL,
    `activities_cost` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `facilities_cost` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `total_cost` DECIMAL(10,2) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'
    `payment_status` VARCHAR(30) DEFAULT 'PAID_VIA_WALLET',
    `special_requests` TEXT,
    `host_notes` TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_bookings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_bookings_trek` FOREIGN KEY (`trek_id`) REFERENCES `treks` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_bookings_date` FOREIGN KEY (`trek_date_id`) REFERENCES `trek_dates` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. Booking Line Items (Customized activities & facilities in each booking)
CREATE TABLE IF NOT EXISTS `booking_items` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `booking_id` BIGINT NOT NULL,
    `activity_id` BIGINT NOT NULL,
    `unit_price` DECIMAL(10,2) NOT NULL,
    `quantity` INT NOT NULL,
    `subtotal` DECIMAL(10,2) NOT NULL,
    CONSTRAINT `fk_booking_items_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_booking_items_activity` FOREIGN KEY (`activity_id`) REFERENCES `activities` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 7. Virtual Wallet Transactions Table
CREATE TABLE IF NOT EXISTS `wallet_transactions` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `transaction_type` VARCHAR(20) NOT NULL, -- 'CREDIT' or 'DEBIT'
    `amount` DECIMAL(12,2) NOT NULL,
    `balance_after` DECIMAL(12,2) NOT NULL,
    `description` VARCHAR(255) NOT NULL,
    `reference_code` VARCHAR(50),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_wallet_tx_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ===================================================================
-- Initial Seed Data
-- ===================================================================

-- Seed Users
INSERT INTO `users` (`id`, `full_name`, `email`, `password`, `phone`, `role`, `wallet_balance`) VALUES
(1, 'Aryan Sharma', 'trekker@example.com', 'pass123', '+91 9876543210', 'USER', 45000.00),
(2, 'Captain Vikram (Expedition Leader)', 'host@example.com', 'admin123', '+91 9123456789', 'HOST', 0.00)
ON DUPLICATE KEY UPDATE `email`=`email`;

-- Seed Activities & Facilities
INSERT INTO `activities` (`id`, `name`, `category`, `description`, `price`, `pricing_type`, `icon_name`, `active`) VALUES
(1, 'Bungee Jumping', 'ACTIVITY', 'Adrenaline-pumping 83m jump from India\'s highest cantilever platform.', 3500.00, 'PER_PERSON', 'fa-bolt', 1),
(2, 'White Water River Rafting', 'ACTIVITY', 'Grade III-IV river rapids with safety kayak escorts and wetsuits.', 1800.00, 'PER_PERSON', 'fa-water', 1),
(3, 'Tandem Paragliding', 'ACTIVITY', 'Fly with a licensed pilot over majestic Himalayan alpine meadows.', 2800.00, 'PER_PERSON', 'fa-wind', 1),
(4, 'Cliff Zip-Lining', 'ACTIVITY', 'High-speed aerial cable ride across deep mountain gorges.', 1200.00, 'PER_PERSON', 'fa-mountain', 1),
(5, 'Rock Climbing & Rappelling', 'ACTIVITY', 'Learn technical ascending and descending on natural granite rock faces.', 1500.00, 'PER_PERSON', 'fa-hiking', 1),
(6, 'Campfire & Acoustic Night', 'ACTIVITY', 'Starry night acoustic music, barbecue snacks, and constellation storytelling.', 850.00, 'PER_PERSON', 'fa-fire', 1),
(7, 'Emergency Transport & Heli-Evac Insurance', 'FACILITY', 'Comprehensive altitude sickness evacuation guarantee with ambulance standby.', 1200.00, 'PER_PERSON', 'fa-ambulance', 1),
(8, 'Certified Wilderness First-Aid Guide', 'FACILITY', 'Dedicated mountaineering certified lead guide carrying pulse-oximeter and oxygen cylinder.', 3000.00, 'PER_GROUP', 'fa-user-shield', 1),
(9, 'Trekker High-Nutrition Food Plan', 'FACILITY', '4 hot meals daily: soup, carb-rich energy meals, trail nut mixes, and herbal tea.', 1600.00, 'PER_PERSON', 'fa-utensils', 1),
(10, 'Hygiene, Sanitization & Bio-Toilet Kit', 'FACILITY', 'Eco-friendly wet wipes, sanitizers, dry wash shampoo, and portable bio-toilet tent access.', 600.00, 'PER_PERSON', 'fa-pump-soap', 1),
(11, 'Porter / Mule Baggage Service', 'FACILITY', 'Dedicated porter or mule support to carry up to 10kg rucksack per group.', 2200.00, 'PER_GROUP', 'fa-horse', 1),
(12, 'High-Altitude Cold Gear Kit Rental', 'FACILITY', '-10C sleeping bag liner, microspikes/crampons, gaiters, and carbon trekking poles.', 950.00, 'PER_PERSON', 'fa-mitten', 1)
ON DUPLICATE KEY UPDATE `id`=`id`;

