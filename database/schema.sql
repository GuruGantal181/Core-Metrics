-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: core_metrics
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `diet_logs`
--
CREATE DATABASE IF NOT EXISTS core_metrics;
USE core_metrics;
DROP TABLE IF EXISTS `diet_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `diet_logs` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `food_item` varchar(100) NOT NULL,
  `calories` int NOT NULL,
  `log_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `diet_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_profiles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `diet_logs`
--

LOCK TABLES `diet_logs` WRITE;
/*!40000 ALTER TABLE `diet_logs` DISABLE KEYS */;
INSERT INTO `diet_logs` VALUES (1,1,'eggs',400,'2026-11-12 14:17:48'),(2,1,'chicken',400,'2026-11-12 14:17:48'),(3,2,'fish',500,'2026-11-12 14:17:48'),(4,3,'chicken',450,'2026-11-12 14:25:21'),(5,4,'eggs',300,'2026-11-12 14:29:25');
/*!40000 ALTER TABLE `diet_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `strength_logs`
--

DROP TABLE IF EXISTS `strength_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `strength_logs` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int NOT NULL,
  `exercise_name` varchar(100) NOT NULL,
  `weight_lifted` decimal(5,2) NOT NULL,
  `reps` int NOT NULL,
  `sets` int NOT NULL,
  `log_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `strength_logs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_profiles` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `strength_logs`
--

LOCK TABLES `strength_logs` WRITE;
/*!40000 ALTER TABLE `strength_logs` DISABLE KEYS */;
INSERT INTO `strength_logs` VALUES (1,1,'benchpress',20.00,10,3,'2026-11-12 14:17:38'),(2,1,'pullups',70.00,10,3,'2026-11-12 14:17:38'),(3,2,'pushups',60.00,10,5,'2026-11-12 14:17:38'),(4,3,'curls',60.00,10,5,'2026-11-12 14:25:12'),(5,4,'benchpress',60.00,10,3,'2026-11-12 14:29:18');
/*!40000 ALTER TABLE `strength_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_profiles`
--

DROP TABLE IF EXISTS `user_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_profiles` (
  `id` int NOT NULL AUTO_INCREMENT,
  `weight_kg` decimal(5,2) NOT NULL,
  `height_cm` decimal(5,2) NOT NULL,
  `age` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_profiles`
--

LOCK TABLES `user_profiles` WRITE;
/*!40000 ALTER TABLE `user_profiles` DISABLE KEYS */;
INSERT INTO `user_profiles` VALUES (1,75.50,180.00,19),(2,60.00,175.00,19),(3,90.00,190.00,22),(4,75.00,170.00,18);
/*!40000 ALTER TABLE `user_profiles` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-20  0:21:10
