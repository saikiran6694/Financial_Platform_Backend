# 💰 Arthium - AI-Powered Finance Platform

An **intelligent finance management platform** that helps users track, analyze, and automate personal or business transactions. Users can create transactions manually or upload receipts, which are analyzed with **Google Gemini AI** to extract details such as date, amount, merchant, and category.

## ✨ Features

### 🧠 AI Receipt Analysis

- Upload image or PDF receipts and extract transaction details with Google Gemini AI
- Map extracted dates, amounts, merchants, and categories to transaction fields
- Store receipt and profile media securely through Cloudinary

### 📊 Financial Insights & Visualization

- Track income, expenses, balances, savings rates, and expense ratios
- Analyze monthly spending trends and category breakdowns
- Query summaries, charts, expense breakdowns, and period comparisons through the REST API

### 🔁 Automation & Scheduled Reports

- Generate financial reports on user-defined schedules
- Schedule recurring transactions and budget checks
- Deliver periodic reports by email through Resend

### 🔐 Secure Authentication & Authorization

- JWT-based authentication with Bearer tokens
- Argon2id password hashing through Spring Security
- Password reset with email OTP verification
- Configurable CORS and protected API routes

### ☁️ Cloud Integration & Deployment

- MongoDB Atlas or a compatible MongoDB deployment
- Cloudinary file storage
- Docker containerization
- Render deployment configuration included

### 💬 AI Chat Support

- Ask natural-language questions about financial data
- Groq-powered chat using the OpenAI-compatible API
- Tool-assisted queries for summaries, transactions, budgets, recurring items, and trends
- Current default model: `openai/gpt-oss-20b`

## 🧩 Tech Stack

### Backend

- ☕ **Java 25**
- 🚀 **Spring Boot 4.1.1** and Spring MVC
- 💾 **Spring Data MongoDB** and `MongoTemplate`
- 🤖 **Google Gemini AI** through REST for receipt analysis
- 💬 **Groq** through its OpenAI-compatible REST API for chat
- ☁️ **Cloudinary** for file storage
- 📧 **Resend** for transactional email
- ⏰ Spring task scheduling with dynamic cron jobs
- 🔐 Spring Security, JWT, and Argon2id

### Frontend

The backend exposes the HTTP contract consumed by the existing React/TypeScript client.

### DevOps & Infrastructure

- 🐳 Docker
- MongoDB Atlas
- Render deployment configuration

## 📁 Project Structure

```text
src/main/java/com/arthium/finance/
├── ai/           Gemini service and prompts
├── analytics/    Financial analytics and date range resolution
├── auth/         Registration, login, refresh tokens, and password reset
├── budget/       Budgets, status, and spend calculations
├── chat/         Groq chat, prompts, tools, and chat queries
├── common/       Exceptions, JSON, money, dates, and health checks
├── config/       Application, MongoDB, security, REST, and scheduler config
├── cron/         Dynamic scheduled jobs and report/budget automation
├── mail/         Resend integration and email templates
├── report/       Report generation and report settings
├── security/     JWT services and authentication filters
├── storage/      Cloudinary integration
├── transaction/  Transaction CRUD, bulk operations, and receipt scanning
└── user/         User profiles and schedules
```

## ⚙️ Environment Variables

Create a `.env` file in the project root. Spring imports it automatically through `application.yml`.

```env
# Database
MONGO_URI=mongodb+srv://username:password@cluster.mongodb.net/
MONGO_DATABASE_NAME=arthium_finance

# Authentication
SECRET_KEY=your-secret-key-here
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=60
REFRESH_TOKEN_EXPIRE_MINUTES=10080

# CORS
ALLOWED_ORIGINS=http://localhost:3000

# Cloudinary
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret

# Google Gemini
GOOGLE_GEMINI_API_KEY=your-gemini-api-key
GOOGLE_GEMINI_AI_MODEL=gemini-2.0-flash

# Groq chat
GROQ_API_KEY=your-groq-api-key
GROQ_MODEL_NAME=openai/gpt-oss-20b

# Resend
RESEND_API_KEY=your-resend-api-key
RESEND_MAILER_SENDER=noreply@yourdomain.com
RESEND_MAILER_SENDER_NAME=Arthium
```

Never commit `.env` or expose production credentials. Use `.env.example` as the template.

## 🚀 Getting Started

### Prerequisites

- **Java 25**
- **Maven 3.9+**
- **MongoDB Atlas** or a MongoDB replica set
- API keys for Google Gemini, Groq, Cloudinary, and Resend
- **Git** and optionally **Docker**

### Installation & Setup

1. **Clone the repository**

   ```bash
   git clone https://github.com/yourusername/financial-platform-springboot.git
   cd financial-platform-springboot
   ```

2. **Configure environment variables**

   ```bash
   cp .env.example .env
   # Edit .env with your credentials
   ```

3. **Build the application**

   ```bash
   mvn clean package
   ```

4. **Run the server locally**

   ```bash
   java -jar target/financial-platform-1.0.0.jar
   ```

   The API is available at `http://localhost:8080`.

### Docker

```bash
docker build -t arthium .
docker run --env-file .env -p 8080:8080 arthium
```

### Health Check

```bash
curl http://localhost:8080/health
curl http://localhost:8080/actuator/health
```

## 📡 API Endpoints

All protected endpoints require an `Authorization: Bearer <token>` header.

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a user |
| POST | `/api/auth/login` | Login and receive JWT tokens |
| POST | `/api/auth/refresh-token` | Refresh an access token |
| POST | `/api/auth/forgot-password/send-otp` | Request a password reset OTP |
| POST | `/api/auth/forgot-password/verify-otp` | Verify a password reset OTP |
| POST | `/api/auth/forgot-password/reset` | Reset a password |

### Users, Transactions, and Budgets

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/user/me` | Get the current user |
| PUT | `/api/user/update` | Update profile or profile picture |
| POST | `/api/transaction/create` | Create a transaction |
| POST | `/api/transaction/bulk-transaction` | Create transactions in bulk |
| POST | `/api/transaction/scan-receipt` | Scan an image or PDF receipt |
| GET | `/api/transaction/all` | List transactions |
| PUT | `/api/transaction/{id}` | Update a transaction |
| DELETE | `/api/transaction/{id}` | Delete a transaction |
| POST | `/api/budget/create` | Create a budget |
| GET | `/api/budget/all` | List budgets |

### Analytics, Reports, and Chat

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/analytics/summary` | Get financial summary |
| GET | `/api/analytics/chart` | Get chart data |
| GET | `/api/analytics/expense-breakdown` | Get category expense data |
| GET | `/api/report/all` | List reports |
| GET | `/api/report/generate` | Generate a report |
| PUT | `/api/report/update-setting` | Update report settings |
| POST | `/api/chat` | Send a question to the AI assistant |

## 🏗️ Architecture Overview

```text
Client
  ↓
Spring MVC controller and validation
  ↓
Authentication and authorization filters
  ↓
Domain service
  ├── MongoDB through Spring Data MongoDB
  ├── Gemini for receipt analysis
  ├── Groq for financial chat
  ├── Cloudinary for media storage
  └── Resend for email delivery
```

Receipt processing uploads the file to Cloudinary, sends it to Gemini for extraction, maps the result to a transaction, and persists it in MongoDB. Chat requests use Groq query tools to retrieve user-specific data before generating a response.

## 🔒 Security Features

- ✅ Argon2id password hashing
- ✅ Expiring HS256 access and refresh tokens
- ✅ Bearer-token authentication for protected routes
- ✅ Configurable CORS
- ✅ Environment-based secrets
- ✅ Authenticated MongoDB and Cloudinary integrations

## ⚠️ Deployment Notes

- MongoDB transactions require Atlas, a replica set, or a sharded cluster.
- Scheduled jobs are loaded from MongoDB at startup.
- Use a MongoDB-backed lock before scaling scheduled workers horizontally.
- Do not commit `.env` or API credentials.

## 📚 Additional Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/)
- [Spring Data MongoDB Documentation](https://docs.spring.io/spring-data/mongodb/reference/)
- [MongoDB Documentation](https://www.mongodb.com/docs/)
- [Google Gemini API](https://ai.google.dev/)
- [Groq Documentation](https://console.groq.com/docs)
- [Cloudinary Documentation](https://cloudinary.com/documentation)

## 🤝 Contributing

Contributions are welcome. Please open an issue or submit a pull request with a clear description of the change.

## 📝 License

This project is licensed under the MIT License - see the `LICENSE` file for details.
