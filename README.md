# 🌌 Shadow Dimensions: The Ethereal Nexus

![Shadow Dimensions Banner](C:\Users\khadi\.gemini\antigravity\brain\6010af48-338c-44b3-86fb-74ab28928992\shadow_dimensions_banner_1778140025940.png)

## 🎭 Project Overview
**Shadow Dimensions** is a high-end JavaFX desktop application designed to bridge the gap between reality and the mystical. It serves as an all-in-one platform for managing a dark fantasy ecosystem, featuring a premium marketplace, an interactive artwork gallery, immersive event management, and AI-driven interactions.

Built with a focus on **immersive UX** and **modern architecture**, Shadow Dimensions leverages state-of-the-art AI (Gemini, OpenRouter) to provide a dynamic experience that evolves with the user.

---

## ✨ Core Modules & Features

### 🔐 1. The Soul Vault (User Management)
The gateway to the dimensions. Secure, flexible, and integrated.
- **Social Integration**: Seamless "Continue with Google" OAuth 2.0 flow.
- **Security First**: Password hashing using **BCrypt** and mandatory email verification.
- **Dynamic Profiles**: Manage user roles and preferences within the "Vault."
- **Persistence**: Robust MySQL backend for storing user souls (accounts).

### 🛒 2. Shadow Marketplace
A premium commerce experience for mystical artifacts and products.
- **AI-Powered Curations**: Automated product descriptions generated via **Gemini API**.
- **Global Economy**: Real-time currency conversion using the **ExchangeRate API**.
- **Inventory Ledger**: Advanced stock alerts and dynamic availability updates.
- **Master Ledgers**: Professional PDF reports for orders and transactions (OpenPDF/PDFBox).

### 🎨 3. Ethereal Gallery (Artworks)
A sanctuary for visual expressions and mystical creations.
- **Reservations System**: Real-time booking of exclusive artworks with automated email/SMS notifications (Jakarta Mail & Twilio).
- **Vision Capture**: Integrated webcam support for capturing reality within the app.
- **AI Imagery**: Integration with **Pollinations** for generating ethereal visuals.

### 📅 4. Rift Events
Manage and explore events across the dimensions.
- **AI Event Assistant**: A dedicated assistant powered by **OpenRouter** to help users navigate the rift.
- **Dynamic Environment**: Real-time weather integration (Open-Meteo) to sync event conditions with reality.
- **Reclamation System**: Track and resolve event-related issues with a robust status workflow.

### 🏛️ 5. Dark Forum & Tutorials
- **Knowledge Base**: Interactive tutorials to master the dimensions.
- **Community Hub**: A dedicated forum for souls to interact and share lore.

---

## 🛠️ Tech Stack & Ecosystem

- **Language**: Java 17
- **UI Framework**: JavaFX 21 (FXML, CSS, Custom Animations)
- **Database**: MySQL 8.0
- **Build Tool**: Maven
- **AI Engines**: Google Gemini, OpenRouter, Pollinations
- **Communication**: Jakarta Mail, Twilio (SMS)
- **Utilities**: OpenCV (Image Processing), Gson, Dotenv (Config)

---

## 🚀 Getting Started

### 1. Prerequisites
- **Java JDK 17** or higher.
- **Maven** installed.
- **MySQL Server** running.

### 2. Environment Setup
The application uses a `.env` file for secrets. **Do not commit this file.**

1. Copy `.env.example` to `.env`.
2. Fill in the required keys:
   ```env
   DB_URL=jdbc:mysql://localhost:3306/shadow_dimensions
   GEMINI_API_KEY=your_key_here
   OPENROUTER_API_KEY=your_key_here
   GOOGLE_CLIENT_ID=your_id_here
   MAIL_USERNAME=your_email
   MAIL_PASSWORD=your_app_password
   ```

### 3. Database Initialization
Run the following SQL snippet to ensure Google OAuth compatibility:
```sql
ALTER TABLE `user` ADD COLUMN `google_id` VARCHAR(255) NULL UNIQUE;
```

### 4. Installation & Run
```powershell
# Clone the repository
git clone [repository-url]

# Navigate to the project
cd shadow_dimension_java

# Build and run with Maven
mvn clean javafx:run
```

---

## 🎨 Visual Identity
The "Shadow Dimensions" design system follows a **Gothic-Futurism** aesthetic, utilizing deep purples, ethereal teals, and sleek dark modes. Every interaction is designed to feel "alive," with micro-animations and responsive layouts.

---

## 🤝 Contribution
The shadows are always expanding. If you wish to contribute:
1. Fork the Project.
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3. Commit your Changes (`git commit -m 'Add AmazingFeature'`).
4. Push to the Branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📜 License
Distributed under the MIT License. See `LICENSE` for more information.

---
*Created with 💜 by the Shadow Dimensions Team.*