-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: eduvibe
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `comment_likes`
--

DROP TABLE IF EXISTS `comment_likes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment_likes` (
  `comment_id` int NOT NULL,
  `user_id` int NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`comment_id`,`user_id`),
  KEY `idx_comment_likes_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `comment_likes`
--

LOCK TABLES `comment_likes` WRITE;
/*!40000 ALTER TABLE `comment_likes` DISABLE KEYS */;
INSERT INTO `comment_likes` VALUES (6,4,'2026-11-17 07:43:23'),(14,4,'2026-11-17 06:30:43');
/*!40000 ALTER TABLE `comment_likes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `comment_replies`
--

DROP TABLE IF EXISTS `comment_replies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comment_replies` (
  `id` int NOT NULL AUTO_INCREMENT,
  `comment_id` int NOT NULL,
  `user_id` int NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comment_replies_comment` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `comment_replies`
--

LOCK TABLES `comment_replies` WRITE;
/*!40000 ALTER TABLE `comment_replies` DISABLE KEYS */;
/*!40000 ALTER TABLE `comment_replies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `comments`
--

DROP TABLE IF EXISTS `comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `comments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `post_id` int NOT NULL,
  `user_id` int NOT NULL,
  `content` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `post_id` (`post_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `comments_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `comments_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `comments`
--

LOCK TABLES `comments` WRITE;
/*!40000 ALTER TABLE `comments` DISABLE KEYS */;
INSERT INTO `comments` VALUES (1,1,3,'black','2026-11-05 16:32:41'),(2,1,4,'waow','2026-11-05 16:39:42'),(3,1,4,'naysu','2026-11-05 16:59:16'),(4,2,3,'im pissed','2026-11-05 17:27:43'),(5,2,4,'hehehehehehe','2026-11-10 02:58:27'),(6,2,4,'gfhf','2026-11-10 11:05:37'),(8,7,7,'grabe','2026-11-17 04:17:21'),(9,7,7,'palo','2026-11-17 04:17:24'),(10,7,7,'sven','2026-11-17 04:17:29'),(11,7,7,'pogi points','2026-11-17 04:17:52'),(12,3,7,'palo','2026-11-17 04:18:58'),(13,8,8,'steven universe','2026-11-17 06:20:37'),(14,5,4,'meow','2026-11-17 06:30:38'),(16,13,11,'WOW','2026-11-17 07:30:12'),(17,12,11,'MALAS','2026-11-17 07:31:08'),(18,6,4,'i love 67','2026-11-21 01:24:13'),(19,6,4,'i love niggs','2026-11-21 01:24:23'),(20,18,4,'wahhaha','2026-11-24 02:24:13'),(21,15,4,'wazzuppp lkjhgfdsa','2026-11-24 02:30:41');
/*!40000 ALTER TABLE `comments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `follows`
--

DROP TABLE IF EXISTS `follows`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `follows` (
  `id` int NOT NULL AUTO_INCREMENT,
  `follower_id` int NOT NULL,
  `following_id` int NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_follow` (`follower_id`,`following_id`),
  KEY `following_id` (`following_id`),
  CONSTRAINT `follows_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `follows_ibfk_2` FOREIGN KEY (`following_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `follows`
--

LOCK TABLES `follows` WRITE;
/*!40000 ALTER TABLE `follows` DISABLE KEYS */;
INSERT INTO `follows` VALUES (3,4,2,'2026-11-08 01:16:32'),(4,4,3,'2026-11-08 01:17:15'),(6,4,1,'2026-11-12 04:54:03'),(7,7,6,'2026-11-17 04:19:18'),(8,10,8,'2026-11-17 07:00:31'),(9,10,3,'2026-11-17 07:00:32'),(10,10,5,'2026-11-17 07:00:34'),(11,10,9,'2026-11-17 07:00:35'),(12,10,2,'2026-11-17 07:00:36'),(13,10,4,'2026-11-17 07:00:38'),(14,10,7,'2026-11-17 07:00:40'),(15,10,6,'2026-11-17 07:00:41'),(16,10,1,'2026-11-17 07:00:45');
/*!40000 ALTER TABLE `follows` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `moderation_logs`
--

DROP TABLE IF EXISTS `moderation_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `moderation_logs` (
  `id` int NOT NULL AUTO_INCREMENT,
  `post_id` int DEFAULT NULL,
  `admin_id` int DEFAULT NULL,
  `action` enum('flagged','approved','removed','edited_by_owner','deleted_by_owner','user_suspended','user_banned','role_promoted','role_demoted','report_reviewed','report_dismissed','user_activated','points_adjusted') COLLATE utf8mb4_unicode_ci NOT NULL,
  `note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `post_id` (`post_id`),
  KEY `admin_id` (`admin_id`),
  CONSTRAINT `moderation_logs_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE SET NULL,
  CONSTRAINT `moderation_logs_ibfk_2` FOREIGN KEY (`admin_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `moderation_logs`
--

LOCK TABLES `moderation_logs` WRITE;
/*!40000 ALTER TABLE `moderation_logs` DISABLE KEYS */;
INSERT INTO `moderation_logs` VALUES (1,NULL,4,'points_adjusted','Admin adjusted @stvn (User ID: 4) by +1000 points. New balance: 1030. Reason: needed','2026-11-12 09:59:39'),(2,NULL,4,'points_adjusted','Admin adjusted @stvn (User ID: 4) by +10000 points. New balance: 10030. Reason: needed','2026-11-12 12:24:45'),(3,2,4,'edited_by_owner','Owner edited post ID 2','2026-11-18 04:07:24'),(4,2,4,'report_reviewed','Admin 4 reviewed report ID 1','2026-11-18 04:07:41'),(5,NULL,4,'points_adjusted','Admin adjusted @stvn (User ID: 4) by +200000000 points. New balance: 200007855. Reason: nah','2026-11-21 01:31:58'),(6,NULL,4,'points_adjusted','Admin adjusted @lee (User ID: 5) by +200000 points. New balance: 200020. Reason: nah','2026-11-24 02:21:52');
/*!40000 ALTER TABLE `moderation_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `type` enum('like','comment','reward','system','reminder') COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`,`is_read`),
  CONSTRAINT `notifications_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=303 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` VALUES (1,3,'like','Someone liked your post.',1,'2026-11-05 16:39:36'),(2,3,'comment','Someone commented on your post.',1,'2026-11-05 16:39:42'),(3,3,'like','@stvn liked your post.',0,'2026-11-05 16:52:57'),(4,3,'like','@stvn liked your post.',0,'2026-11-05 16:59:10'),(5,3,'comment','@stvn commented on your post.',1,'2026-11-05 16:59:16'),(6,4,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-05 17:14:52'),(7,3,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-05 17:27:25'),(8,4,'like','@eduvibe_test2026 liked your post.',1,'2026-11-05 17:27:28'),(9,4,'comment','@eduvibe_test2026 commented on your post.',1,'2026-11-05 17:27:43'),(10,4,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-08 00:11:02'),(11,4,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-08 18:41:47'),(12,4,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-09 16:35:35'),(13,1,'system','@stvn claimed the +20 daily login reward.',0,'2026-11-09 16:35:35'),(14,4,'system','@stvn claimed the +20 daily login reward.',1,'2026-11-09 16:35:35'),(15,4,'reward','You won 10 points from the daily spin!',1,'2026-11-09 16:36:22'),(16,1,'system','@stvn published a new ACADEMIC post. Post ID: 3',0,'2026-11-10 02:57:30'),(17,4,'system','@stvn published a new ACADEMIC post. Post ID: 3',1,'2026-11-10 02:57:30'),(18,1,'system','@stvn commented on a post. Post ID: 2',0,'2026-11-10 02:58:27'),(19,4,'system','@stvn commented on a post. Post ID: 2',1,'2026-11-10 02:58:27'),(20,1,'system','@stvn liked a post. Post ID: 2',0,'2026-11-10 03:00:10'),(21,4,'system','@stvn liked a post. Post ID: 2',1,'2026-11-10 03:00:10'),(22,4,'reward','You won \"Blue Ring Frame\" from the Mystery Reward Roll!',1,'2026-11-10 10:56:30'),(23,4,'reward','You won \"Starry Banner\" from the Mystery Reward Roll!',1,'2026-11-10 10:56:49'),(24,1,'system','@stvn commented on a post. Post ID: 2',0,'2026-11-10 11:05:37'),(25,4,'system','@stvn commented on a post. Post ID: 2',1,'2026-11-10 11:05:37'),(26,4,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-11 05:55:49'),(27,1,'system','@stvn claimed the +20 daily login reward.',0,'2026-11-11 05:55:49'),(28,4,'system','@stvn claimed the +20 daily login reward.',1,'2026-11-11 05:55:49'),(29,4,'reward','You won 10 points from the daily spin!',1,'2026-11-11 05:57:15'),(30,1,'system','@stvn published a new ENTERTAINMENT reel. Post ID: 4',0,'2026-11-11 06:07:24'),(31,4,'system','@stvn published a new ENTERTAINMENT reel. Post ID: 4',1,'2026-11-11 06:07:24'),(32,1,'system','@stvn liked a post. Post ID: 4',0,'2026-11-11 06:09:00'),(33,4,'system','@stvn liked a post. Post ID: 4',1,'2026-11-11 06:09:00'),(34,4,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-12 00:08:35'),(35,1,'system','@stvn claimed the +20 daily login reward.',0,'2026-11-12 00:08:35'),(36,4,'system','@stvn claimed the +20 daily login reward.',1,'2026-11-12 00:08:35'),(37,4,'reward','You won 25 points from the daily spin!',1,'2026-11-12 00:09:09'),(38,4,'reward','You won \"Neon Purple Theme\" from the Mystery Reward Roll!',1,'2026-11-12 00:09:34'),(39,1,'system','New user registered: @lee.',0,'2026-11-12 01:23:27'),(40,4,'system','New user registered: @lee.',1,'2026-11-12 01:23:27'),(41,5,'reward','Daily Login Reward! You earned 20 points for logging in today.',0,'2026-11-12 01:23:37'),(42,1,'system','@lee claimed the +20 daily login reward.',0,'2026-11-12 01:23:37'),(43,4,'system','@lee claimed the +20 daily login reward.',1,'2026-11-12 01:23:37'),(44,4,'like','@lee liked your post.',1,'2026-11-12 01:25:51'),(45,1,'system','@lee liked a post. Post ID: 3',0,'2026-11-12 01:25:51'),(46,4,'system','@lee liked a post. Post ID: 3',1,'2026-11-12 01:25:51'),(47,1,'system','@stvn started following @admin.',0,'2026-11-12 04:54:03'),(48,4,'system','@stvn started following @admin.',1,'2026-11-12 04:54:03'),(49,1,'system','Admin adjusted @stvn\'s points by +1000. New balance: 1030. Reason: needed',0,'2026-11-12 09:59:39'),(50,4,'system','Admin adjusted @stvn\'s points by +1000. New balance: 1030. Reason: needed',1,'2026-11-12 09:59:39'),(51,4,'reward','You won \"Ember Frame\" from the Mystery Reward Roll!',1,'2026-11-12 09:59:50'),(52,4,'reward','You won \"Pencil Frame\" from the Mystery Reward Roll!',1,'2026-11-12 10:00:01'),(53,4,'reward','You won \"Midnight Theme\" from the Mystery Reward Roll!',1,'2026-11-12 10:02:03'),(54,4,'reward','You won \"Ocean Theme\" from the Mystery Reward Roll!',1,'2026-11-12 10:02:10'),(55,4,'reward','You won \"Northern Lights Banner\" from the Mystery Reward Roll!',1,'2026-11-12 10:02:17'),(56,4,'reward','You won \"Volcano Banner\" from the Mystery Reward Roll!',1,'2026-11-12 10:02:24'),(57,4,'reward','You won \"Leaf Frame\" from the Mystery Reward Roll!',1,'2026-11-12 10:02:29'),(58,4,'reward','You won \"Campus Morning Banner\" from the Mystery Reward Roll!',1,'2026-11-12 10:02:33'),(59,4,'reward','You won \"Silver Ring\" from the Mystery Reward Roll!',1,'2026-11-12 10:02:38'),(60,4,'reward','You won \"Green Field Banner\" from the Mystery Reward Roll!',1,'2026-11-12 10:02:42'),(61,1,'system','Admin adjusted @stvn\'s points by +10000. New balance: 10030. Reason: needed',0,'2026-11-12 12:24:45'),(62,4,'system','Admin adjusted @stvn\'s points by +10000. New balance: 10030. Reason: needed',1,'2026-11-12 12:24:45'),(63,4,'reward','You won \"Amethyst Theme\" from the Mystery Reward Roll!',1,'2026-11-12 12:25:14'),(64,4,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-13 01:19:20'),(65,1,'system','@stvn claimed the +20 daily login reward.',0,'2026-11-13 01:19:20'),(66,4,'system','@stvn claimed the +20 daily login reward.',1,'2026-11-13 01:19:20'),(67,4,'reward','You won 15 points from the daily spin!',1,'2026-11-13 02:07:31'),(68,4,'reward','You won \"Classic Light Theme\" from the Mystery Reward Roll!',1,'2026-11-13 02:07:40'),(69,4,'reward','You won \"Cloud Frame\" from the Mystery Reward Roll!',1,'2026-11-13 02:07:47'),(70,4,'reward','You won \"Mint Theme\" from the Mystery Reward Roll!',1,'2026-11-13 02:07:53'),(71,4,'reward','You won \"Cherry Blossom Banner\" from the Mystery Reward Roll!',1,'2026-11-13 02:07:58'),(72,4,'reward','You won \"Notebook Banner\" from the Mystery Reward Roll!',1,'2026-11-13 02:08:03'),(73,4,'reward','You won \"Sky Ring\" from the Mystery Reward Roll!',1,'2026-11-13 02:08:07'),(74,4,'reward','You won \"Rainy Day Banner\" from the Mystery Reward Roll!',1,'2026-11-13 02:08:11'),(75,4,'reward','You won \"Sky Theme\" from the Mystery Reward Roll!',1,'2026-11-13 02:08:15'),(76,4,'reward','You won \"Twilight Theme\" from the Mystery Reward Roll!',1,'2026-11-13 02:08:19'),(77,4,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-15 05:36:00'),(78,1,'system','@stvn claimed the +20 daily login reward.',0,'2026-11-15 05:36:00'),(79,4,'system','@stvn claimed the +20 daily login reward.',1,'2026-11-15 05:36:00'),(80,4,'reward','You won 5 points from the daily spin!',1,'2026-11-15 06:17:10'),(81,4,'reward','You won \"Emerald Frame\" from the Mystery Reward Roll!',1,'2026-11-15 06:17:33'),(82,4,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-15 18:07:14'),(83,1,'system','@stvn claimed the +20 daily login reward.',0,'2026-11-15 18:07:14'),(84,4,'system','@stvn claimed the +20 daily login reward.',1,'2026-11-15 18:07:14'),(85,1,'system','@stvn commented on a post. Post ID: 2',0,'2026-11-15 18:10:22'),(86,4,'system','@stvn commented on a post. Post ID: 2',1,'2026-11-15 18:10:22'),(87,4,'reward','You won 15 points from the daily spin!',1,'2026-11-15 18:11:39'),(88,4,'reward','You won \"Solar Flare Frame\" from the Mystery Reward Roll!',1,'2026-11-15 18:12:06'),(89,1,'system','New user registered: @jjba.',0,'2026-11-15 19:17:08'),(90,4,'system','New user registered: @jjba.',1,'2026-11-15 19:17:08'),(91,6,'reward','Daily Login Reward! You earned 20 points for logging in today.',0,'2026-11-15 19:17:15'),(92,1,'system','@jjba claimed the +20 daily login reward.',0,'2026-11-15 19:17:15'),(93,4,'system','@jjba claimed the +20 daily login reward.',1,'2026-11-15 19:17:15'),(94,1,'system','@jjba published a new ENTERTAINMENT reel. Post ID: 5',0,'2026-11-15 19:18:40'),(95,4,'system','@jjba published a new ENTERTAINMENT reel. Post ID: 5',1,'2026-11-15 19:18:40'),(96,1,'system','@jjba published a new ACADEMIC reel. Post ID: 6',0,'2026-11-15 19:22:14'),(97,4,'system','@jjba published a new ACADEMIC reel. Post ID: 6',1,'2026-11-15 19:22:14'),(98,6,'like','@stvn liked your post.',0,'2026-11-16 12:47:59'),(99,1,'system','@stvn liked a post. Post ID: 5',0,'2026-11-16 12:47:59'),(100,4,'system','@stvn liked a post. Post ID: 5',1,'2026-11-16 12:47:59'),(101,4,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-16 23:12:44'),(102,1,'system','@stvn claimed the +20 daily login reward.',0,'2026-11-16 23:12:44'),(103,4,'system','@stvn claimed the +20 daily login reward.',1,'2026-11-16 23:12:44'),(104,1,'system','New user registered: @attag.',0,'2026-11-17 04:15:14'),(105,4,'system','New user registered: @attag.',1,'2026-11-17 04:15:14'),(106,7,'reward','Daily Login Reward! You earned 20 points for logging in today.',0,'2026-11-17 04:15:22'),(107,1,'system','@attag claimed the +20 daily login reward.',0,'2026-11-17 04:15:22'),(108,4,'system','@attag claimed the +20 daily login reward.',1,'2026-11-17 04:15:22'),(109,1,'system','@attag published a new ACADEMIC post. Post ID: 7',0,'2026-11-17 04:16:57'),(110,4,'system','@attag published a new ACADEMIC post. Post ID: 7',1,'2026-11-17 04:16:57'),(111,1,'system','@attag commented on a post. Post ID: 7',0,'2026-11-17 04:17:21'),(112,4,'system','@attag commented on a post. Post ID: 7',1,'2026-11-17 04:17:21'),(113,1,'system','@attag commented on a post. Post ID: 7',0,'2026-11-17 04:17:24'),(114,4,'system','@attag commented on a post. Post ID: 7',1,'2026-11-17 04:17:24'),(115,1,'system','@attag commented on a post. Post ID: 7',0,'2026-11-17 04:17:29'),(116,4,'system','@attag commented on a post. Post ID: 7',1,'2026-11-17 04:17:29'),(117,1,'system','@attag saved a post. Post ID: 7',0,'2026-11-17 04:17:41'),(118,4,'system','@attag saved a post. Post ID: 7',1,'2026-11-17 04:17:41'),(119,1,'system','@attag liked a post. Post ID: 7',0,'2026-11-17 04:17:43'),(120,4,'system','@attag liked a post. Post ID: 7',1,'2026-11-17 04:17:43'),(121,1,'system','@attag commented on a post. Post ID: 7',0,'2026-11-17 04:17:52'),(122,4,'system','@attag commented on a post. Post ID: 7',1,'2026-11-17 04:17:52'),(123,4,'comment','@attag commented on your post.',1,'2026-11-17 04:18:58'),(124,1,'system','@attag commented on a post. Post ID: 3',0,'2026-11-17 04:18:58'),(125,4,'system','@attag commented on a post. Post ID: 3',1,'2026-11-17 04:18:58'),(126,1,'system','@attag started following @jjba.',0,'2026-11-17 04:19:18'),(127,4,'system','@attag started following @jjba.',1,'2026-11-17 04:19:18'),(128,1,'system','A post has received a new user report. Post ID: 2',0,'2026-11-17 04:20:06'),(129,4,'system','A post has received a new user report. Post ID: 2',1,'2026-11-17 04:20:06'),(130,7,'reward','You won 15 points from the daily spin!',0,'2026-11-17 04:20:22'),(131,1,'system','@attag published a new ENTERTAINMENT post. Post ID: 8',0,'2026-11-17 04:23:09'),(132,4,'system','@attag published a new ENTERTAINMENT post. Post ID: 8',1,'2026-11-17 04:23:09'),(133,4,'reward','You won \"Sand Theme\" from the Mystery Reward Roll!',1,'2026-11-17 04:26:10'),(134,4,'reward','You won \"Royal Violet Frame\" from the Mystery Reward Roll!',1,'2026-11-17 04:26:15'),(135,4,'reward','You won \"Aurora Frame\" from the Mystery Reward Roll!',1,'2026-11-17 04:26:19'),(136,4,'reward','You won \"Library Banner\" from the Mystery Reward Roll!',1,'2026-11-17 04:26:23'),(137,4,'reward','You won \"Hall of Fame Banner\" from the Mystery Reward Roll!',1,'2026-11-17 04:26:27'),(138,4,'reward','You won \"Rose Theme\" from the Mystery Reward Roll!',1,'2026-11-17 05:11:23'),(139,7,'like','@stvn liked your post.',0,'2026-11-17 05:12:55'),(140,1,'system','@stvn liked a post. Post ID: 7',0,'2026-11-17 05:12:55'),(141,4,'system','@stvn liked a post. Post ID: 7',1,'2026-11-17 05:12:55'),(142,1,'system','New user registered: @harleetot.',0,'2026-11-17 06:16:40'),(143,4,'system','New user registered: @harleetot.',1,'2026-11-17 06:16:40'),(144,8,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-17 06:16:52'),(145,1,'system','@harleetot claimed the +20 daily login reward.',0,'2026-11-17 06:16:52'),(146,4,'system','@harleetot claimed the +20 daily login reward.',1,'2026-11-17 06:16:52'),(147,1,'system','@harleetot published a new ACADEMIC post. Post ID: 9',0,'2026-11-17 06:20:17'),(148,4,'system','@harleetot published a new ACADEMIC post. Post ID: 9',1,'2026-11-17 06:20:17'),(149,7,'comment','@harleetot commented on your post.',0,'2026-11-17 06:20:37'),(150,1,'system','@harleetot commented on a post. Post ID: 8',0,'2026-11-17 06:20:37'),(151,4,'system','@harleetot commented on a post. Post ID: 8',1,'2026-11-17 06:20:37'),(152,8,'reward','You won the Nebula Frame from the daily spin! Find it in Customize.',1,'2026-11-17 06:21:33'),(153,1,'system','@stvn saved a post. Post ID: 5',0,'2026-11-17 06:30:28'),(154,4,'system','@stvn saved a post. Post ID: 5',1,'2026-11-17 06:30:28'),(155,6,'comment','@stvn commented on your post.',0,'2026-11-17 06:30:38'),(156,1,'system','@stvn commented on a post. Post ID: 5',0,'2026-11-17 06:30:38'),(157,4,'system','@stvn commented on a post. Post ID: 5',1,'2026-11-17 06:30:38'),(158,4,'reward','You won the Galaxy Banner from the daily spin! Find it in Customize.',1,'2026-11-17 06:30:54'),(159,4,'reward','You won \"Jade Theme\" from the Mystery Reward Roll!',1,'2026-11-17 06:31:06'),(160,4,'reward','You won \"Thunderstorm Banner\" from the Mystery Reward Roll!',1,'2026-11-17 06:31:14'),(161,4,'reward','You won \"Golden Hour Banner\" from the Mystery Reward Roll!',1,'2026-11-17 06:31:19'),(162,4,'reward','You won \"Ice Crystal Frame\" from the Mystery Reward Roll!',1,'2026-11-17 06:31:26'),(163,4,'reward','You won \"Honor Roll Banner\" from the Mystery Reward Roll!',1,'2026-11-17 06:31:31'),(164,1,'system','New user registered: @Renzzz.',0,'2026-11-17 06:37:15'),(165,4,'system','New user registered: @Renzzz.',1,'2026-11-17 06:37:15'),(166,9,'reward','Daily Login Reward! You earned 20 points for logging in today.',0,'2026-11-17 06:37:29'),(167,1,'system','@Renzzz claimed the +20 daily login reward.',0,'2026-11-17 06:37:29'),(168,4,'system','@Renzzz claimed the +20 daily login reward.',1,'2026-11-17 06:37:29'),(169,1,'system','@Renzzz published a new ACADEMIC post. Post ID: 10',0,'2026-11-17 06:38:34'),(170,4,'system','@Renzzz published a new ACADEMIC post. Post ID: 10',1,'2026-11-17 06:38:34'),(171,1,'system','@Renzzz published a new ENTERTAINMENT post. Post ID: 11',0,'2026-11-17 06:40:21'),(172,4,'system','@Renzzz published a new ENTERTAINMENT post. Post ID: 11',1,'2026-11-17 06:40:21'),(173,9,'reward','You won 5 points from the daily spin!',0,'2026-11-17 06:40:40'),(174,6,'like','@Renzzz liked your post.',0,'2026-11-17 06:44:47'),(175,1,'system','@Renzzz liked a post. Post ID: 5',0,'2026-11-17 06:44:47'),(176,4,'system','@Renzzz liked a post. Post ID: 5',1,'2026-11-17 06:44:47'),(177,6,'comment','@Renzzz commented on your post.',0,'2026-11-17 06:44:56'),(178,1,'system','@Renzzz commented on a post. Post ID: 5',0,'2026-11-17 06:44:56'),(179,4,'system','@Renzzz commented on a post. Post ID: 5',1,'2026-11-17 06:44:56'),(180,1,'system','New user registered: @hahahaniks.',0,'2026-11-17 06:50:48'),(181,4,'system','New user registered: @hahahaniks.',1,'2026-11-17 06:50:48'),(182,10,'reward','Daily Login Reward! You earned 20 points for logging in today.',0,'2026-11-17 06:51:07'),(183,1,'system','@hahahaniks claimed the +20 daily login reward.',0,'2026-11-17 06:51:07'),(184,4,'system','@hahahaniks claimed the +20 daily login reward.',1,'2026-11-17 06:51:07'),(185,1,'system','@hahahaniks published a new ACADEMIC post. Post ID: 12',0,'2026-11-17 06:56:33'),(186,4,'system','@hahahaniks published a new ACADEMIC post. Post ID: 12',1,'2026-11-17 06:56:33'),(187,10,'reward','You won 5 points from the daily spin!',0,'2026-11-17 06:56:57'),(188,1,'system','@hahahaniks purchased the reward \"Golden Owl Sticker\". Reward ID: 4',0,'2026-11-17 06:58:39'),(189,4,'system','@hahahaniks purchased the reward \"Golden Owl Sticker\". Reward ID: 4',1,'2026-11-17 06:58:39'),(190,1,'system','@hahahaniks updated their profile information.',0,'2026-11-17 06:59:45'),(191,4,'system','@hahahaniks updated their profile information.',1,'2026-11-17 06:59:45'),(192,1,'system','@hahahaniks started following @harleetot.',0,'2026-11-17 07:00:31'),(193,4,'system','@hahahaniks started following @harleetot.',1,'2026-11-17 07:00:31'),(194,1,'system','@hahahaniks started following @eduvibe_test2026.',0,'2026-11-17 07:00:32'),(195,4,'system','@hahahaniks started following @eduvibe_test2026.',1,'2026-11-17 07:00:32'),(196,1,'system','@hahahaniks started following @lee.',0,'2026-11-17 07:00:34'),(197,4,'system','@hahahaniks started following @lee.',1,'2026-11-17 07:00:34'),(198,1,'system','@hahahaniks started following @Renzzz.',0,'2026-11-17 07:00:35'),(199,4,'system','@hahahaniks started following @Renzzz.',1,'2026-11-17 07:00:35'),(200,1,'system','@hahahaniks started following @sven.',0,'2026-11-17 07:00:36'),(201,4,'system','@hahahaniks started following @sven.',1,'2026-11-17 07:00:36'),(202,1,'system','@hahahaniks started following @stvn.',0,'2026-11-17 07:00:38'),(203,4,'system','@hahahaniks started following @stvn.',1,'2026-11-17 07:00:38'),(204,1,'system','@hahahaniks started following @attag.',0,'2026-11-17 07:00:40'),(205,4,'system','@hahahaniks started following @attag.',1,'2026-11-17 07:00:40'),(206,1,'system','@hahahaniks started following @jjba.',0,'2026-11-17 07:00:41'),(207,4,'system','@hahahaniks started following @jjba.',1,'2026-11-17 07:00:41'),(208,1,'system','@hahahaniks started following @admin.',0,'2026-11-17 07:00:45'),(209,4,'system','@hahahaniks started following @admin.',1,'2026-11-17 07:00:45'),(210,1,'system','New user registered: @SaRaph123.',0,'2026-11-17 07:26:41'),(211,4,'system','New user registered: @SaRaph123.',1,'2026-11-17 07:26:41'),(212,11,'reward','Daily Login Reward! You earned 20 points for logging in today.',0,'2026-11-17 07:26:58'),(213,1,'system','@SaRaph123 claimed the +20 daily login reward.',0,'2026-11-17 07:26:58'),(214,4,'system','@SaRaph123 claimed the +20 daily login reward.',1,'2026-11-17 07:26:58'),(215,1,'system','@SaRaph123 published a new ENTERTAINMENT post. Post ID: 13',0,'2026-11-17 07:29:46'),(216,4,'system','@SaRaph123 published a new ENTERTAINMENT post. Post ID: 13',1,'2026-11-17 07:29:46'),(217,1,'system','@SaRaph123 commented on a post. Post ID: 13',0,'2026-11-17 07:30:12'),(218,4,'system','@SaRaph123 commented on a post. Post ID: 13',1,'2026-11-17 07:30:12'),(219,10,'comment','@SaRaph123 commented on your post.',0,'2026-11-17 07:31:08'),(220,1,'system','@SaRaph123 commented on a post. Post ID: 12',0,'2026-11-17 07:31:08'),(221,4,'system','@SaRaph123 commented on a post. Post ID: 12',1,'2026-11-17 07:31:08'),(222,1,'system','New user registered: @jauno.',0,'2026-11-17 07:33:42'),(223,4,'system','New user registered: @jauno.',1,'2026-11-17 07:33:42'),(224,12,'reward','Daily Login Reward! You earned 20 points for logging in today.',0,'2026-11-17 07:33:50'),(225,1,'system','@jauno claimed the +20 daily login reward.',0,'2026-11-17 07:33:50'),(226,4,'system','@jauno claimed the +20 daily login reward.',1,'2026-11-17 07:33:50'),(227,1,'system','@jauno published a new ENTERTAINMENT post. Post ID: 14',0,'2026-11-17 07:34:44'),(228,4,'system','@jauno published a new ENTERTAINMENT post. Post ID: 14',1,'2026-11-17 07:34:44'),(229,12,'reward','You won 75 points from the daily spin!',0,'2026-11-17 07:35:36'),(230,12,'reward','You won \"Mint Theme\" from the Mystery Reward Roll!',0,'2026-11-17 07:36:17'),(231,1,'system','@jauno purchased the reward \"Golden Owl Sticker\". Reward ID: 4',0,'2026-11-17 07:36:47'),(232,4,'system','@jauno purchased the reward \"Golden Owl Sticker\". Reward ID: 4',1,'2026-11-17 07:36:47'),(233,1,'system','New user registered: @estellecutie.',0,'2026-11-17 07:46:49'),(234,4,'system','New user registered: @estellecutie.',1,'2026-11-17 07:46:49'),(235,13,'reward','Daily Login Reward! You earned 20 points for logging in today.',0,'2026-11-17 07:47:03'),(236,1,'system','@estellecutie claimed the +20 daily login reward.',0,'2026-11-17 07:47:03'),(237,4,'system','@estellecutie claimed the +20 daily login reward.',1,'2026-11-17 07:47:03'),(238,1,'system','@estellecutie published a new ENTERTAINMENT post. Post ID: 15',0,'2026-11-17 07:48:36'),(239,4,'system','@estellecutie published a new ENTERTAINMENT post. Post ID: 15',1,'2026-11-17 07:48:36'),(240,4,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-18 04:03:50'),(241,1,'system','@stvn claimed the +20 daily login reward.',0,'2026-11-18 04:03:50'),(242,4,'system','@stvn claimed the +20 daily login reward.',1,'2026-11-18 04:03:50'),(243,1,'system','A user edited their post. Owner edited post ID 2',0,'2026-11-18 04:07:24'),(244,4,'system','A user edited their post. Owner edited post ID 2',1,'2026-11-18 04:07:24'),(245,1,'system','@stvn published a new ENTERTAINMENT post. Post ID: 16',0,'2026-11-18 12:36:48'),(246,4,'system','@stvn published a new ENTERTAINMENT post. Post ID: 16',1,'2026-11-18 12:36:48'),(247,4,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-18 20:01:06'),(248,1,'system','@stvn claimed the +20 daily login reward.',0,'2026-11-18 20:01:06'),(249,4,'system','@stvn claimed the +20 daily login reward.',1,'2026-11-18 20:01:06'),(250,4,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-20 19:20:54'),(251,1,'system','@stvn claimed the +20 daily login reward.',0,'2026-11-20 19:20:54'),(252,4,'system','@stvn claimed the +20 daily login reward.',1,'2026-11-20 19:20:54'),(253,4,'reward','You won 25 points from the daily spin!',1,'2026-11-20 23:29:52'),(254,4,'reward','You won \"Golden Theme\" from the Mystery Reward Roll!',1,'2026-11-20 23:30:07'),(255,6,'like','@stvn liked your post.',0,'2026-11-21 01:23:57'),(256,1,'system','@stvn liked a post. Post ID: 6',0,'2026-11-21 01:23:57'),(257,4,'system','@stvn liked a post. Post ID: 6',1,'2026-11-21 01:23:57'),(258,6,'comment','@stvn commented on your post.',0,'2026-11-21 01:24:13'),(259,1,'system','@stvn commented on a post. Post ID: 6',0,'2026-11-21 01:24:13'),(260,4,'system','@stvn commented on a post. Post ID: 6',1,'2026-11-21 01:24:13'),(261,6,'comment','@stvn commented on your post.',0,'2026-11-21 01:24:23'),(262,1,'system','@stvn commented on a post. Post ID: 6',0,'2026-11-21 01:24:23'),(263,4,'system','@stvn commented on a post. Post ID: 6',1,'2026-11-21 01:24:23'),(264,1,'system','Admin adjusted @stvn\'s points by +200000000. New balance: 200007855. Reason: nah',0,'2026-11-21 01:31:58'),(265,4,'system','Admin adjusted @stvn\'s points by +200000000. New balance: 200007855. Reason: nah',1,'2026-11-21 01:31:58'),(266,4,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-23 11:56:59'),(267,1,'system','@stvn claimed the +20 daily login reward.',0,'2026-11-23 11:56:59'),(268,4,'system','@stvn claimed the +20 daily login reward.',1,'2026-11-23 11:56:59'),(269,1,'system','@stvn liked a post. Post ID: 4',0,'2026-11-23 12:20:56'),(270,4,'system','@stvn liked a post. Post ID: 4',1,'2026-11-23 12:20:56'),(271,1,'system','@stvn liked a post. Post ID: 4',0,'2026-11-23 12:20:58'),(272,4,'system','@stvn liked a post. Post ID: 4',1,'2026-11-23 12:20:58'),(273,4,'reward','Daily Login Reward! You earned 20 points for logging in today.',1,'2026-11-23 17:15:54'),(274,1,'system','@stvn claimed the +20 daily login reward.',0,'2026-11-23 17:15:54'),(275,4,'system','@stvn claimed the +20 daily login reward.',1,'2026-11-23 17:15:54'),(276,1,'system','@stvn published a new ACADEMIC reel. Post ID: 17',0,'2026-11-24 02:04:13'),(277,4,'system','@stvn published a new ACADEMIC reel. Post ID: 17',1,'2026-11-24 02:04:13'),(278,1,'system','New user registered: @harleee.',0,'2026-11-24 02:15:57'),(279,4,'system','New user registered: @harleee.',1,'2026-11-24 02:15:57'),(280,14,'reward','Daily Login Reward! You earned 20 points for logging in today.',0,'2026-11-24 02:16:41'),(281,1,'system','@harleee claimed the +20 daily login reward.',0,'2026-11-24 02:16:41'),(282,4,'system','@harleee claimed the +20 daily login reward.',1,'2026-11-24 02:16:41'),(283,4,'reward','You won \"Cosmic Dream Banner\" from the Mystery Reward Roll!',1,'2026-11-24 02:19:57'),(284,4,'reward','You won \"Diamond Halo Frame\" from the Mystery Reward Roll!',1,'2026-11-24 02:20:04'),(285,1,'system','Admin adjusted @lee\'s points by +200000. New balance: 200020. Reason: nah',0,'2026-11-24 02:21:52'),(286,4,'system','Admin adjusted @lee\'s points by +200000. New balance: 200020. Reason: nah',0,'2026-11-24 02:21:52'),(287,1,'system','@stvn published a new ACADEMIC reel. Post ID: 18',0,'2026-11-24 02:23:38'),(288,4,'system','@stvn published a new ACADEMIC reel. Post ID: 18',0,'2026-11-24 02:23:38'),(289,1,'system','@stvn commented on a post. Post ID: 18',0,'2026-11-24 02:24:13'),(290,4,'system','@stvn commented on a post. Post ID: 18',0,'2026-11-24 02:24:13'),(291,4,'reward','You won 20 points from the daily spin!',0,'2026-11-24 02:25:21'),(292,13,'comment','@stvn commented on your post.',0,'2026-11-24 02:30:41'),(293,1,'system','@stvn commented on a post. Post ID: 15',0,'2026-11-24 02:30:41'),(294,4,'system','@stvn commented on a post. Post ID: 15',0,'2026-11-24 02:30:41'),(295,13,'like','@stvn liked your post.',0,'2026-11-24 02:31:10'),(296,1,'system','@stvn liked a post. Post ID: 15',0,'2026-11-24 02:31:10'),(297,4,'system','@stvn liked a post. Post ID: 15',0,'2026-11-24 02:31:10'),(298,12,'like','@stvn liked your post.',0,'2026-11-24 02:31:13'),(299,1,'system','@stvn liked a post. Post ID: 14',0,'2026-11-24 02:31:13'),(300,4,'system','@stvn liked a post. Post ID: 14',0,'2026-11-24 02:31:13'),(301,1,'system','@stvn liked a post. Post ID: 3',0,'2026-11-24 02:31:26'),(302,4,'system','@stvn liked a post. Post ID: 3',0,'2026-11-24 02:31:26');
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `point_transactions`
--

DROP TABLE IF EXISTS `point_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `point_transactions` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `points` int NOT NULL,
  `action_type` enum('like','comment','post','daily_login','rng_spin','reward_purchase','admin_adjust') COLLATE utf8mb4_unicode_ci NOT NULL,
  `ref_key` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_user_ref` (`user_id`,`ref_key`),
  CONSTRAINT `point_transactions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=121 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `point_transactions`
--

LOCK TABLES `point_transactions` WRITE;
/*!40000 ALTER TABLE `point_transactions` DISABLE KEYS */;
INSERT INTO `point_transactions` VALUES (1,3,5,'like','like:1:4','2026-11-05 16:52:57'),(2,4,0,'reward_purchase','reward:1','2026-11-05 16:58:53'),(3,3,10,'comment','comment:3','2026-11-05 16:59:16'),(4,4,15,'post','post:2','2026-11-05 17:05:37'),(5,4,20,'daily_login','login:2026-11-06','2026-11-05 17:14:52'),(6,3,20,'daily_login','login:2026-11-06','2026-11-05 17:27:25'),(7,4,5,'like','like:2:3','2026-11-05 17:27:28'),(8,4,10,'comment','comment:4','2026-11-05 17:27:43'),(9,4,20,'daily_login','login:2026-11-08','2026-11-08 00:11:02'),(10,4,20,'daily_login','login:2026-11-09','2026-11-08 18:41:47'),(11,4,20,'daily_login','login:2026-11-10','2026-11-09 16:35:35'),(12,4,10,'rng_spin','spin:2026-11-10','2026-11-09 16:36:22'),(13,4,15,'post','post:3','2026-11-10 02:57:30'),(14,4,-50,'reward_purchase','roll:2','2026-11-10 10:56:30'),(15,4,-50,'reward_purchase','roll:3','2026-11-10 10:56:49'),(16,4,20,'daily_login','login:2026-11-11','2026-11-11 05:55:49'),(17,4,10,'rng_spin','spin:2026-11-11','2026-11-11 05:57:15'),(18,4,15,'post','post:4','2026-11-11 06:07:24'),(19,4,20,'daily_login','login:2026-11-12','2026-11-12 00:08:35'),(20,4,25,'rng_spin','spin:2026-11-12','2026-11-12 00:09:09'),(21,4,-100,'reward_purchase','roll:4','2026-11-12 00:09:34'),(22,5,20,'daily_login','login:2026-11-12','2026-11-12 01:23:37'),(23,4,5,'like','like:3:5','2026-11-12 01:25:51'),(24,4,-100,'reward_purchase','roll:5','2026-11-12 09:59:50'),(25,4,-100,'reward_purchase','roll:6','2026-11-12 10:00:01'),(26,4,-100,'reward_purchase','roll:7','2026-11-12 10:02:03'),(27,4,-100,'reward_purchase','roll:8','2026-11-12 10:02:10'),(28,4,-100,'reward_purchase','roll:9','2026-11-12 10:02:17'),(29,4,-100,'reward_purchase','roll:10','2026-11-12 10:02:24'),(30,4,-100,'reward_purchase','roll:11','2026-11-12 10:02:29'),(31,4,-100,'reward_purchase','roll:12','2026-11-12 10:02:33'),(32,4,-100,'reward_purchase','roll:13','2026-11-12 10:02:38'),(33,4,-100,'reward_purchase','roll:14','2026-11-12 10:02:42'),(34,4,-100,'reward_purchase','roll:15','2026-11-12 12:25:14'),(35,4,20,'daily_login','login:2026-11-13','2026-11-13 01:19:20'),(36,4,15,'rng_spin','spin:2026-11-13','2026-11-13 02:07:31'),(37,4,-100,'reward_purchase','roll:16','2026-11-13 02:07:40'),(38,4,-100,'reward_purchase','roll:17','2026-11-13 02:07:47'),(39,4,-100,'reward_purchase','roll:18','2026-11-13 02:07:53'),(40,4,-100,'reward_purchase','roll:19','2026-11-13 02:07:58'),(41,4,-100,'reward_purchase','roll:20','2026-11-13 02:08:03'),(42,4,-100,'reward_purchase','roll:21','2026-11-13 02:08:07'),(43,4,-100,'reward_purchase','roll:22','2026-11-13 02:08:11'),(44,4,-100,'reward_purchase','roll:23','2026-11-13 02:08:15'),(45,4,-100,'reward_purchase','roll:24','2026-11-13 02:08:19'),(46,4,20,'daily_login','login:2026-11-15','2026-11-15 05:36:00'),(47,4,5,'rng_spin','spin:2026-11-15','2026-11-15 06:17:10'),(48,4,-100,'reward_purchase','roll:25','2026-11-15 06:17:33'),(49,4,20,'daily_login','login:2026-11-16','2026-11-15 18:07:14'),(50,4,15,'rng_spin','spin:2026-11-16','2026-11-15 18:11:39'),(51,4,-100,'reward_purchase','roll:26','2026-11-15 18:12:06'),(52,6,20,'daily_login','login:2026-11-16','2026-11-15 19:17:15'),(53,6,15,'post','post:5','2026-11-15 19:18:40'),(54,6,15,'post','post:6','2026-11-15 19:22:14'),(55,6,5,'like','like:5:4','2026-11-16 12:47:59'),(56,4,20,'daily_login','login:2026-11-17','2026-11-16 23:12:44'),(57,7,20,'daily_login','login:2026-11-17','2026-11-17 04:15:22'),(58,7,15,'post','post:7','2026-11-17 04:16:57'),(59,4,10,'comment','comment:12','2026-11-17 04:18:58'),(60,7,15,'rng_spin','spin:2026-11-17','2026-11-17 04:20:22'),(61,7,15,'post','post:8','2026-11-17 04:23:09'),(62,4,-100,'reward_purchase','roll:27','2026-11-17 04:26:10'),(63,4,-100,'reward_purchase','roll:28','2026-11-17 04:26:15'),(64,4,-100,'reward_purchase','roll:29','2026-11-17 04:26:19'),(65,4,-100,'reward_purchase','roll:30','2026-11-17 04:26:23'),(66,4,-100,'reward_purchase','roll:31','2026-11-17 04:26:27'),(67,4,-100,'reward_purchase','roll:32','2026-11-17 05:11:23'),(68,7,5,'like','like:7:4','2026-11-17 05:12:55'),(69,8,20,'daily_login','login:2026-11-17','2026-11-17 06:16:52'),(70,8,15,'post','post:9','2026-11-17 06:20:17'),(71,7,10,'comment','comment:13','2026-11-17 06:20:37'),(72,8,0,'rng_spin','spin:2026-11-17','2026-11-17 06:21:33'),(73,6,10,'comment','comment:14','2026-11-17 06:30:38'),(74,4,0,'rng_spin','spin:2026-11-17','2026-11-17 06:30:54'),(75,4,-100,'reward_purchase','roll:35','2026-11-17 06:31:06'),(76,4,-100,'reward_purchase','roll:36','2026-11-17 06:31:14'),(77,4,-100,'reward_purchase','roll:37','2026-11-17 06:31:19'),(78,4,-100,'reward_purchase','roll:38','2026-11-17 06:31:26'),(79,4,-100,'reward_purchase','roll:39','2026-11-17 06:31:31'),(80,9,20,'daily_login','login:2026-11-17','2026-11-17 06:37:29'),(81,9,15,'post','post:10','2026-11-17 06:38:34'),(82,9,15,'post','post:11','2026-11-17 06:40:21'),(83,9,5,'rng_spin','spin:2026-11-17','2026-11-17 06:40:40'),(84,6,5,'like','like:5:9','2026-11-17 06:44:47'),(85,6,10,'comment','comment:15','2026-11-17 06:44:56'),(86,10,20,'daily_login','login:2026-11-17','2026-11-17 06:51:07'),(87,10,15,'post','post:12','2026-11-17 06:56:33'),(88,10,5,'rng_spin','spin:2026-11-17','2026-11-17 06:56:57'),(89,10,0,'reward_purchase','reward:40','2026-11-17 06:58:39'),(90,11,20,'daily_login','login:2026-11-17','2026-11-17 07:26:58'),(91,11,15,'post','post:13','2026-11-17 07:29:46'),(92,10,10,'comment','comment:17','2026-11-17 07:31:08'),(93,12,20,'daily_login','login:2026-11-17','2026-11-17 07:33:50'),(94,12,15,'post','post:14','2026-11-17 07:34:44'),(95,12,75,'rng_spin','spin:2026-11-17','2026-11-17 07:35:36'),(96,12,-100,'reward_purchase','roll:41','2026-11-17 07:36:17'),(97,12,0,'reward_purchase','reward:42','2026-11-17 07:36:47'),(98,13,20,'daily_login','login:2026-11-17','2026-11-17 07:47:03'),(99,13,15,'post','post:15','2026-11-17 07:48:36'),(100,4,20,'daily_login','login:2026-11-18','2026-11-18 04:03:50'),(101,4,15,'post','post:16','2026-11-18 12:36:48'),(102,4,20,'daily_login','login:2026-11-19','2026-11-18 20:01:06'),(103,4,20,'daily_login','login:2026-11-21','2026-11-20 19:20:54'),(104,4,25,'rng_spin','spin:2026-11-21','2026-11-20 23:29:52'),(105,4,-100,'reward_purchase','roll:43','2026-11-20 23:30:07'),(106,6,5,'like','like:6:4','2026-11-21 01:23:57'),(107,6,10,'comment','comment:18','2026-11-21 01:24:13'),(108,6,10,'comment','comment:19','2026-11-21 01:24:23'),(109,4,20,'daily_login','login:2026-11-23','2026-11-23 11:56:59'),(110,4,20,'daily_login','login:2026-11-24','2026-11-23 17:15:54'),(111,4,15,'post','post:17','2026-11-24 02:04:13'),(112,14,20,'daily_login','login:2026-11-24','2026-11-24 02:16:41'),(113,4,-100,'reward_purchase','roll:44','2026-11-24 02:19:57'),(114,4,-100,'reward_purchase','roll:45','2026-11-24 02:20:04'),(115,4,15,'post','post:18','2026-11-24 02:23:38'),(116,4,20,'rng_spin','spin:2026-11-24','2026-11-24 02:25:21'),(117,13,10,'comment','comment:21','2026-11-24 02:30:41'),(118,4,30,'rng_spin','goals:2026-11-24','2026-11-24 02:31:00'),(119,13,5,'like','like:15:4','2026-11-24 02:31:10'),(120,12,5,'like','like:14:4','2026-11-24 02:31:13');
/*!40000 ALTER TABLE `point_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `post_download_blocked`
--

DROP TABLE IF EXISTS `post_download_blocked`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_download_blocked` (
  `post_id` int NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `post_download_blocked`
--

LOCK TABLES `post_download_blocked` WRITE;
/*!40000 ALTER TABLE `post_download_blocked` DISABLE KEYS */;
/*!40000 ALTER TABLE `post_download_blocked` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `post_likes`
--

DROP TABLE IF EXISTS `post_likes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `post_likes` (
  `id` int NOT NULL AUTO_INCREMENT,
  `post_id` int NOT NULL,
  `user_id` int NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_like` (`post_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `post_likes_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `post_likes_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `post_likes`
--

LOCK TABLES `post_likes` WRITE;
/*!40000 ALTER TABLE `post_likes` DISABLE KEYS */;
INSERT INTO `post_likes` VALUES (4,1,4,'2026-11-05 16:59:10'),(5,2,3,'2026-11-05 17:27:28'),(6,2,4,'2026-11-10 03:00:10'),(8,3,5,'2026-11-12 01:25:51'),(10,7,7,'2026-11-17 04:17:43'),(11,7,4,'2026-11-17 05:12:55'),(12,5,9,'2026-11-17 06:44:47'),(13,6,4,'2026-11-21 01:23:57'),(16,15,4,'2026-11-24 02:31:10'),(17,14,4,'2026-11-24 02:31:13'),(18,3,4,'2026-11-24 02:31:26');
/*!40000 ALTER TABLE `post_likes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `posts`
--

DROP TABLE IF EXISTS `posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `posts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `category` enum('academic','entertainment') COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `media_type` enum('none','image','video') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'none',
  `media_path` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_reel` tinyint(1) NOT NULL DEFAULT '0',
  `moderation_status` enum('clean','flagged','approved','removed') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'clean',
  `flagged_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `idx_category` (`category`,`created_at`),
  CONSTRAINT `posts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `posts`
--

LOCK TABLES `posts` WRITE;
/*!40000 ALTER TABLE `posts` DISABLE KEYS */;
INSERT INTO `posts` VALUES (1,3,'entertainment','hallow','a wanderer insect name hallow knight','none',NULL,0,'approved',NULL,'2026-11-05 16:29:40','2026-11-08 01:33:53'),(2,4,'entertainment',NULL,'neon of valo','image','uploads/f063b9b7d4fe430b84eff36dccf938ea.jpg',0,'approved',NULL,'2026-11-05 17:05:37','2026-11-05 18:55:20'),(3,4,'academic','ms Elly da bes','try lang baka iperfect','none',NULL,0,'clean',NULL,'2026-11-10 02:57:30','2026-11-10 02:57:30'),(4,4,'entertainment','valorant duelist edit :>','check out this valorant vid edit','video','uploads/0ae937d616c94d7aa4437f795519ab23.mp4',1,'clean',NULL,'2026-11-11 06:07:24','2026-11-11 06:07:24'),(5,6,'entertainment','JJBA','BREH AHHAHAHHHA','video','uploads/d2d5a0e0f2c149a28b6bbbe0ff099158.mp4',1,'clean',NULL,'2026-11-15 19:18:40','2026-11-15 19:18:40'),(6,6,'academic','JJBA pose','just be better bro','video','uploads/85ed0aae7fb843f6a8b0c59b231cec25.mp4',1,'clean',NULL,'2026-11-15 19:22:14','2026-11-15 19:22:14'),(7,7,'academic','yes','ewan palo sven','image','uploads/25fb50124225468cad910488f84cae2e.png',0,'clean',NULL,'2026-11-17 04:16:57','2026-11-17 04:16:57'),(8,7,'entertainment','steven james sandoval','palo','image','uploads/900120ea5d4441b9bab23bdcbf8f2b4a.jpg',0,'clean',NULL,'2026-11-17 04:23:09','2026-11-17 04:23:09'),(9,8,'academic','No to Pax Silica','#Announcement NO TO POGO! NO TO PAX SILICA!\n\nThe Philippines is not for sale. Our land, resources, sovereignty, and future should never be compromised for the interests of foreign powers. We stand for transparency, accountability, and the protection of our national interests.\n\nDevelopment should empower Filipinos, not put our country\'s independence at risk. We deserve agreements that prioritize our people, protect our resources, and respect our sovereignty.\n\nOur country, our future, our decision.\n\nNO TO PAX SILICA WITHOUT TRANSPARENCY AND ACCOUNTABILITY!\n\n#NoToPaxSilica #ProtectPhilippineSovereignty #Philippines #NationalInterest #FilipinoPeople','image','uploads/f195e015131c44e69beb430888b406a5.jpg',0,'clean',NULL,'2026-11-17 06:20:17','2026-11-17 06:20:17'),(10,9,'academic',NULL,'No to pax silica #StudyTip #Announcement','none',NULL,0,'clean',NULL,'2026-11-17 06:38:34','2026-11-17 06:38:34'),(11,9,'entertainment',NULL,'have fun #Fun','none',NULL,0,'clean',NULL,'2026-11-17 06:40:21','2026-11-17 06:40:21'),(12,10,'academic','NO TO PAX SILICA','TRUMPOUT','image','uploads/3e30aa0956b8407e85a64ab894dd37f7.jpg',0,'clean',NULL,'2026-11-17 06:56:33','2026-11-17 06:56:33'),(13,11,'entertainment','HAHAHAHHAHAHAHA','BIG gUY','image','uploads/ea4347ef1aec4aaba0fa101bfc554b9d.jpg',0,'clean',NULL,'2026-11-17 07:29:46','2026-11-17 07:29:46'),(14,12,'entertainment','halo po!!!','greetings','none',NULL,0,'clean',NULL,'2026-11-17 07:34:44','2026-11-17 07:34:44'),(15,13,'entertainment','greetings lng','halo to everyone','none',NULL,0,'clean',NULL,'2026-11-17 07:48:36','2026-11-17 07:48:36'),(16,4,'academic','trumpy','peace silico','none',NULL,0,'clean',NULL,'2026-11-18 12:36:48','2026-11-18 12:37:03'),(17,4,'academic','SCIENCE','CHECK PUT THIS ASTRONOMY','video','uploads/4604a056605c45dba3ef56b2af2ba38f.mp4',1,'clean',NULL,'2026-11-24 02:04:13','2026-11-24 02:04:13'),(18,4,'academic','sciece','astro','video','uploads/e4b077008ea14b319fac6b7fc75e9282.mp4',1,'clean',NULL,'2026-11-24 02:23:38','2026-11-24 02:23:38');
/*!40000 ALTER TABLE `posts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reports`
--

DROP TABLE IF EXISTS `reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reports` (
  `id` int NOT NULL AUTO_INCREMENT,
  `post_id` int NOT NULL,
  `reporter_id` int DEFAULT NULL,
  `source` enum('auto','user') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user',
  `reason` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('pending','reviewed','dismissed') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `post_id` (`post_id`),
  KEY `reporter_id` (`reporter_id`),
  CONSTRAINT `reports_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `reports_ibfk_2` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reports`
--

LOCK TABLES `reports` WRITE;
/*!40000 ALTER TABLE `reports` DISABLE KEYS */;
INSERT INTO `reports` VALUES (1,2,7,'user','palo','reviewed','2026-11-17 04:20:06');
/*!40000 ALTER TABLE `reports` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rewards`
--

DROP TABLE IF EXISTS `rewards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rewards` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reward_type` enum('avatar_frame','banner','theme','sticker') COLLATE utf8mb4_unicode_ci NOT NULL,
  `rarity` enum('common','rare','epic','legendary') COLLATE utf8mb4_unicode_ci NOT NULL,
  `drop_weight` int NOT NULL DEFAULT '10',
  `point_cost` int DEFAULT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  CONSTRAINT `chk_weight` CHECK ((`drop_weight` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=53 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rewards`
--

LOCK TABLES `rewards` WRITE;
/*!40000 ALTER TABLE `rewards` DISABLE KEYS */;
INSERT INTO `rewards` VALUES (1,'Blue Ring Frame','A simple glowing avatar frame','avatar_frame','common',50,500,1,'2026-11-02 13:03:24'),(2,'Starry Banner','Night sky profile banner','banner','rare',30,800,1,'2026-11-02 13:03:24'),(3,'Neon Purple Theme','Purple accent profile theme','theme','epic',15,1000,1,'2026-11-02 13:03:24'),(4,'Golden Owl Sticker','Legendary study mascot','sticker','legendary',5,NULL,1,'2026-11-02 13:03:24'),(11,'Silver Ring','A clean silver ring around your avatar.','avatar_frame','common',10,60,1,'2026-11-12 01:31:25'),(12,'Sky Ring','A light-blue frame for a fresh look.','avatar_frame','common',10,60,1,'2026-11-12 01:31:25'),(13,'Leaf Frame','A green frame for nature lovers.','avatar_frame','common',10,60,1,'2026-11-12 01:31:25'),(14,'Cloud Frame','A soft white frame.','avatar_frame','common',10,60,1,'2026-11-12 01:31:25'),(15,'Pencil Frame','A yellow frame for note-takers.','avatar_frame','common',10,60,1,'2026-11-12 01:31:25'),(16,'Royal Violet Frame','A rich purple frame fit for a scholar.','avatar_frame','rare',10,380,1,'2026-11-12 01:31:25'),(17,'Ice Crystal Frame','A frosty blue-white frame.','avatar_frame','rare',10,380,1,'2026-11-12 01:31:25'),(18,'Ember Frame','A glowing orange-red frame.','avatar_frame','rare',10,380,1,'2026-11-12 01:31:25'),(19,'Emerald Frame','A shining green frame.','avatar_frame','rare',10,380,1,'2026-11-12 01:31:25'),(20,'Aurora Frame','Shifting colours like the northern lights.','avatar_frame','epic',10,800,1,'2026-11-12 01:31:25'),(21,'Nebula Frame','A frame of deep-space purple and pink.','avatar_frame','epic',10,800,1,'2026-11-12 01:31:25'),(22,'Solar Flare Frame','A fiery gold-orange frame.','avatar_frame','epic',10,800,1,'2026-11-12 01:31:25'),(23,'Golden Crown Frame','The gold frame reserved for top students.','avatar_frame','legendary',10,1800,1,'2026-11-12 01:31:25'),(24,'Diamond Halo Frame','A brilliant frame that shines above the rest.','avatar_frame','legendary',10,1800,1,'2026-11-12 01:31:25'),(25,'Campus Morning Banner','A calm blue banner for your profile.','banner','common',10,70,1,'2026-11-12 01:31:25'),(26,'Library Banner','A cozy banner for bookworms.','banner','common',10,70,1,'2026-11-12 01:31:25'),(27,'Notebook Banner','A clean banner inspired by notebook paper.','banner','common',10,70,1,'2026-11-12 01:31:25'),(28,'Green Field Banner','A fresh green banner.','banner','common',10,70,1,'2026-11-12 01:31:25'),(29,'Rainy Day Banner','A quiet grey-blue banner.','banner','common',10,70,1,'2026-11-12 01:31:25'),(30,'Galaxy Banner','A purple space banner for big thinkers.','banner','rare',10,420,1,'2026-11-12 01:31:25'),(31,'Cherry Blossom Banner','A soft pink banner full of spring colours.','banner','rare',10,420,1,'2026-11-12 01:31:25'),(32,'Northern Lights Banner','A green and violet glow.','banner','rare',10,420,1,'2026-11-12 01:31:25'),(33,'Volcano Banner','A hot red-orange banner.','banner','rare',10,420,1,'2026-11-12 01:31:25'),(34,'Golden Hour Banner','A glowing gold banner that grabs attention.','banner','epic',10,900,1,'2026-11-12 01:31:25'),(35,'Cosmic Dream Banner','A swirl of purple, pink and blue.','banner','epic',10,900,1,'2026-11-12 01:31:25'),(36,'Thunderstorm Banner','A dramatic dark-blue banner.','banner','epic',10,900,1,'2026-11-12 01:31:25'),(37,'Honor Roll Banner','The legendary banner for the best of the best.','banner','legendary',10,2000,1,'2026-11-12 01:31:25'),(38,'Hall of Fame Banner','A royal gold and crimson banner.','banner','legendary',10,2000,1,'2026-11-12 01:31:25'),(39,'Classic Light Theme','A bright, clean profile tint.','theme','common',10,75,1,'2026-11-12 01:31:25'),(40,'Ocean Theme','A cool blue profile tint.','theme','common',10,75,1,'2026-11-12 01:31:25'),(41,'Sand Theme','A warm neutral profile tint.','theme','common',10,75,1,'2026-11-12 01:31:25'),(42,'Sky Theme','A light sky-blue profile tint.','theme','common',10,75,1,'2026-11-12 01:31:25'),(43,'Mint Theme','A fresh mint profile tint.','theme','common',10,75,1,'2026-11-12 01:31:25'),(44,'Midnight Theme','A deep indigo profile tint.','theme','rare',10,400,1,'2026-11-12 01:31:25'),(45,'Rose Theme','An elegant pink profile tint.','theme','rare',10,400,1,'2026-11-12 01:31:25'),(46,'Jade Theme','A rich green profile tint.','theme','rare',10,400,1,'2026-11-12 01:31:25'),(47,'Crimson Theme','A bold red profile tint.','theme','rare',10,400,1,'2026-11-12 01:31:25'),(48,'Amethyst Theme','A rich violet profile tint.','theme','epic',10,850,1,'2026-11-12 01:31:25'),(49,'Sapphire Theme','A brilliant deep-blue profile tint.','theme','epic',10,850,1,'2026-11-12 01:31:25'),(50,'Twilight Theme','A purple-orange dusk profile tint.','theme','epic',10,850,1,'2026-11-12 01:31:25'),(51,'Golden Theme','A shining gold profile tint.','theme','legendary',10,1900,1,'2026-11-12 01:31:25'),(52,'Platinum Theme','A radiant silver-white profile tint.','theme','legendary',10,1900,1,'2026-11-12 01:31:25');
/*!40000 ALTER TABLE `rewards` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rng_history`
--

DROP TABLE IF EXISTS `rng_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rng_history` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `reward_id` int DEFAULT NULL,
  `points_spent` int NOT NULL,
  `rarity_result` enum('common','rare','epic','legendary') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `reward_id` (`reward_id`),
  CONSTRAINT `rng_history_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `rng_history_ibfk_2` FOREIGN KEY (`reward_id`) REFERENCES `rewards` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rng_history`
--

LOCK TABLES `rng_history` WRITE;
/*!40000 ALTER TABLE `rng_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `rng_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `saved_posts`
--

DROP TABLE IF EXISTS `saved_posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `saved_posts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `post_id` int NOT NULL,
  `saved_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_saved_post` (`user_id`,`post_id`),
  KEY `post_id` (`post_id`),
  CONSTRAINT `saved_posts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `saved_posts_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `saved_posts`
--

LOCK TABLES `saved_posts` WRITE;
/*!40000 ALTER TABLE `saved_posts` DISABLE KEYS */;
INSERT INTO `saved_posts` VALUES (2,7,7,'2026-11-17 04:17:41'),(3,4,5,'2026-11-17 06:30:28');
/*!40000 ALTER TABLE `saved_posts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_equipped`
--

DROP TABLE IF EXISTS `user_equipped`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_equipped` (
  `user_id` int NOT NULL,
  `slot` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reward_id` int NOT NULL,
  PRIMARY KEY (`user_id`,`slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_equipped`
--

LOCK TABLES `user_equipped` WRITE;
/*!40000 ALTER TABLE `user_equipped` DISABLE KEYS */;
INSERT INTO `user_equipped` VALUES (4,'badge',4),(4,'banner',31),(4,'frame',24),(4,'theme',45),(8,'frame',21),(10,'badge',4),(12,'theme',43);
/*!40000 ALTER TABLE `user_equipped` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_rewards`
--

DROP TABLE IF EXISTS `user_rewards`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_rewards` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `reward_id` int NOT NULL,
  `obtained_via` enum('rng','purchase') COLLATE utf8mb4_unicode_ci NOT NULL,
  `obtained_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `reward_id` (`reward_id`),
  CONSTRAINT `user_rewards_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `user_rewards_ibfk_2` FOREIGN KEY (`reward_id`) REFERENCES `rewards` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_rewards`
--

LOCK TABLES `user_rewards` WRITE;
/*!40000 ALTER TABLE `user_rewards` DISABLE KEYS */;
INSERT INTO `user_rewards` VALUES (1,4,4,'purchase','2026-11-05 16:58:53'),(2,4,1,'purchase','2026-11-10 10:56:30'),(3,4,2,'purchase','2026-11-10 10:56:49'),(4,4,3,'purchase','2026-11-12 00:09:34'),(5,4,18,'purchase','2026-11-12 09:59:50'),(6,4,15,'purchase','2026-11-12 10:00:01'),(7,4,44,'purchase','2026-11-12 10:02:03'),(8,4,40,'purchase','2026-11-12 10:02:10'),(9,4,32,'purchase','2026-11-12 10:02:17'),(10,4,33,'purchase','2026-11-12 10:02:24'),(11,4,13,'purchase','2026-11-12 10:02:29'),(12,4,25,'purchase','2026-11-12 10:02:33'),(13,4,11,'purchase','2026-11-12 10:02:38'),(14,4,28,'purchase','2026-11-12 10:02:42'),(15,4,48,'purchase','2026-11-12 12:25:14'),(16,4,39,'purchase','2026-11-13 02:07:40'),(17,4,14,'purchase','2026-11-13 02:07:47'),(18,4,43,'purchase','2026-11-13 02:07:53'),(19,4,31,'purchase','2026-11-13 02:07:58'),(20,4,27,'purchase','2026-11-13 02:08:03'),(21,4,12,'purchase','2026-11-13 02:08:07'),(22,4,29,'purchase','2026-11-13 02:08:11'),(23,4,42,'purchase','2026-11-13 02:08:15'),(24,4,50,'purchase','2026-11-13 02:08:19'),(25,4,19,'purchase','2026-11-15 06:17:33'),(26,4,22,'purchase','2026-11-15 18:12:06'),(27,4,41,'purchase','2026-11-17 04:26:10'),(28,4,16,'purchase','2026-11-17 04:26:15'),(29,4,20,'purchase','2026-11-17 04:26:19'),(30,4,26,'purchase','2026-11-17 04:26:23'),(31,4,38,'purchase','2026-11-17 04:26:27'),(32,4,45,'purchase','2026-11-17 05:11:23'),(33,8,21,'rng','2026-11-17 06:21:33'),(34,4,30,'rng','2026-11-17 06:30:54'),(35,4,46,'purchase','2026-11-17 06:31:06'),(36,4,36,'purchase','2026-11-17 06:31:14'),(37,4,34,'purchase','2026-11-17 06:31:19'),(38,4,17,'purchase','2026-11-17 06:31:26'),(39,4,37,'purchase','2026-11-17 06:31:31'),(40,10,4,'purchase','2026-11-17 06:58:39'),(41,12,43,'purchase','2026-11-17 07:36:17'),(42,12,4,'purchase','2026-11-17 07:36:47'),(43,4,51,'purchase','2026-11-20 23:30:07'),(44,4,35,'purchase','2026-11-24 02:19:57'),(45,4,24,'purchase','2026-11-24 02:20:04');
/*!40000 ALTER TABLE `user_rewards` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `full_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `username` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `bio` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT '',
  `profile_pic` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` enum('user','admin') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user',
  `status` enum('active','suspended','banned') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `points` int NOT NULL DEFAULT '0',
  `entertainment_limit_minutes` int NOT NULL DEFAULT '30',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  CONSTRAINT `chk_points` CHECK ((`points` >= 0))
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'EduVibe Admin','admin','admin@eduvibe.local','$2b$10$/CHU.VJVtebw/dK2GAfvBOCEl42b74TMyH2f2J03lgguA0rx5OzRy','',NULL,'admin','active',0,30,'2026-11-02 13:06:28'),(2,'sven','sven','sven29@gmail.com','$2b$10$X8qlrDjOLUKu/6Eat8EzVeOUcD9tGnV4rwib.JNM9smdw.UWJ8Qii','',NULL,'user','active',0,30,'2026-11-02 14:27:39'),(3,'EduVibe Tester','eduvibe_test2026','eduvibe_test2026@gmail.com','$2a$12$OvXTocvyMVGhXp/sYfDT8OmC7uIdt5gqGjybN2qafdi0PvjfnbdLO','',NULL,'user','active',35,30,'2026-11-05 15:41:14'),(4,'stvn123','stvn','stvn@gmail.com','$2a$12$O8sBadQIKY/Hw.lvDkqtRurzV7XqcmAGkAQhJk.qmMMY9Z3tqEbv6','sven','uploads/avatars/d6a4271f98d844e4bcf063b671499c0e.jpg','admin','active',200007775,15,'2026-11-05 16:39:17'),(5,'lee','lee','lee@gmail.com','$2a$12$Q1LF3gZK6e4MtunpKrZhF.CszcHvPfahtAn.cFPhmIoeDHOvXM1vK','',NULL,'user','active',200020,30,'2026-11-12 01:23:27'),(6,'jjba','jjba','jjba@gmail.com','$2a$12$6Ct9hA6yBxeAyjjd7DXzCOBg9PoVFx/l7v0o5HpbbVKpbKCOMVvHW','',NULL,'user','active',105,30,'2026-11-15 19:17:08'),(7,'ngab','attag','gabigli@gmail.com','$2a$12$qkYU2c1QSucv2tZ1LeV2FOEAcuAhNtpylQn/9JF2gtiAzgRraLYBC','','uploads/avatars/72cf09315aed4c268c76d5a92758bb8d.png','user','active',80,30,'2026-11-17 04:15:14'),(8,'Aj Soer','harleetot','25-61712@gmail.com','$2a$12$NY5c3cOT29LxRbQVlQrLbux2xUKbmrH4SiU3Is2yweePO00EyYULm','','uploads/avatars/eddeb994062548fcb7c59607127cca65.jpg','user','active',35,30,'2026-11-17 06:16:40'),(9,'Renz Olan','Renzzz','olanjoshuarenz12@gmail.com','$2a$12$hYQ4uNsi2NzopxFH8KjEhe4mIvLiWOA1UA8vxo4jkTbtUL3oojWQe','',NULL,'user','active',55,60,'2026-11-17 06:37:15'),(10,'franzel nikki','hahahaniks','niks@gmail.com','$2a$12$48zAHllCCAOTInO5TrOo5uAjmCwbodvwuYdbIXjAqJ5WMpjlY1t0G','HAHAHA',NULL,'user','active',50,30,'2026-11-17 06:50:48'),(11,'Raphael Antonio L. Almendras','SaRaph123','yhael.almendras@gmail.com','$2a$12$4MlJEk/91KRzymlMwOX4P.fjQf/8L0.RkGuGGEoVp2pasTCeftUKa','',NULL,'user','active',35,30,'2026-11-17 07:26:40'),(12,'JAPAU NAO','jauno','japaunao@gmail.com','$2a$12$7iUjjSLKx7CdNC2TxM2c1.K5OXinW8wS7Y.D9nXTF4Fou.8f7/CX6','',NULL,'user','active',15,30,'2026-11-17 07:33:42'),(13,'Estelle','estellecutie','chloegarcia@gmail.com','$2a$12$cxNsJOG90WjKWaoV.0Mmt.uDbiYu/zNa9DfTBdZ00zs3N/8qLrWA6','',NULL,'user','active',50,30,'2026-11-17 07:46:49'),(14,'Harlee Soer','harleee','25-61712@g.batstate-u.edu.ph','$2a$12$nuYPpMkqYS24aRB7f5.p1ui/1Pl8WXzb/abbmt7OGeFtydSD0xOG.','',NULL,'user','active',20,30,'2026-11-24 02:15:56');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-26 14:32:19
