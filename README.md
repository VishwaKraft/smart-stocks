
# Smart Stocks 📈 #

[![Github Pages](https://github.com/amolsr/smart-stocks/actions/workflows/angular.yml/badge.svg)](https://github.com/amolsr/smart-stocks/actions/workflows/angular.yml)

**A comprehensive stock market analysis and prediction platform built with Angular, Spring Boot, and Machine Learning. Smart Stocks provides real-time market data, AI-powered price predictions, educational resources, and portfolio management tools.**

## 📚 Table of Contents

-  [🎯 Key Objectives](#🎯-key-objectives)
-  [🚀 Features](#🚀-features) 
-  [🏗️ Architecture](#🏗️-architecture)
-  [📁 Project Structure](#📁-project-structure)
-  [🛠️ Technology Stack](#🛠️-technology-stack)
-  [🚀 Getting Started](#🚀-getting-started)
    - [Prerequisites](#prerequisites)   
    - [Installation](#installation)
-  [📱 Usage](#📱-usage)
-  [🔧 API Endpoints](#🔧-api-endpoints)
      - [API Details & Examples](#api-details--examples)
- [📚 Documentation & Guides](#📚-documentation)
- [🤝 Contributing](#🤝-contributing)
- [🚀 Deployment](#🚀-deployment)
- [🛠️ Troubleshooting](#🛠️-troubleshooting)
- [🧩 Environment Variables](#🧩-environment-variables)
- [📄 License & Contributors](#📄-license)

 ## 🎯 Key Objectives

- Real-time market data dashboard (live prices, top gainers/losers, news)
- AI-powered 20-day price predictions (LSTM model served via Flask)
- Detailed stock analytics and historical charts per symbol
- Watchlist and portfolio management with simulated buy/sell and transaction history
- Responsive Angular UI using Material Design and charting libraries
- Secure Spring Boot backend with JWT auth and PostgreSQL persistence
- Clear component separation: `client/`, `server/`, `model/` for easier development
- Docker-friendly and CI-ready structure for repeatable deployment


## 🚀 Features
### 📊 **Real-time Market Data**
- Live stock prices and market trends
- Top gainers and losers tracking
- Interactive charts and visualizations
- Market news and updates

### 🤖 **AI-Powered Predictions**
- LSTM-based stock price prediction model
- 20-day future price forecasting
- Machine learning-powered trend analysis
- Historical data analysis with outlier detection

### 📚 **Educational Resources**
- Stock market learning modules
- Investment strategies and tips
- Educational content for beginners
- University-level financial education

### 💼 **Portfolio Management**
- Watchlist functionality
- Buy/sell stock simulation
- Portfolio tracking and analytics
- Payment history and transaction logs

### 🎯 **User Experience**
- Modern, responsive Angular UI
- Material Design components
- Interactive dashboards
- Mobile-friendly interface

## 🏗️ Architecture

This project follows a microservices architecture with three main components:

### Frontend (Angular 12)
- **Location**: `/client/`
- **Framework**: Angular 12 with TypeScript
- **UI Library**: Angular Material + Bootstrap
- **Charts**: CanvasJS, Chart.js, ng2-charts
- **Features**: Responsive design, real-time updates, interactive components

### Backend (Spring Boot)
- **Location**: `/server/`
- **Framework**: Spring Boot 2.5.3 with Java 11
- **Database**: PostgreSQL with JPA/Hibernate
- **Security**: JWT-based authentication
- **API**: RESTful APIs with OpenAPI documentation
- **Features**: User management, portfolio tracking, data persistence

### ML Service (Python/Flask)
- **Location**: `/model/`
- **Framework**: Flask with TensorFlow/Keras
- **ML Libraries**: TensorFlow, scikit-learn, pandas, numpy
- **Features**: LSTM neural networks, price prediction, data scraping

## 📁 Project Structure

```
smart-stocks/
├── 📁 client/               # Angular frontend
│   ├── src/
│   │   ├── app/            # Angular components
│   │   ├── assets/         # Static assets
│   │   └── environments/   # Environment configs
│   ├── package.json        # Node dependencies
│   └── angular.json        # Angular configuration
├── 📁 server/              # Spring Boot backend
│   ├── src/
│   │   ├── main/java/      # Java source code
│   │   └── main/resources/ # Configuration files
│   ├── pom.xml            # Maven dependencies
│   └── Dockerfile         # Docker configuration
├── 📁 model/               # ML service
│   ├── app.py             # Flask application
│   ├── requirements.txt   # Python dependencies
│   └── Model.ipynb        # Jupyter notebook
├── 📁 .github/            # CI/CD workflows
│   └── workflows/         # GitHub Actions
├── 📁 docs/               # Documentation
└── README.md              # This file
```

### 🔧 System Components

| Component | Description |
|-----------|-------------|
| **Frontend Module** | Angular-based UI for real-time market data visualization and user interaction |
| **API Gateway Module** | Spring Boot REST API handling authentication, stock data, and user management |
| **ML Prediction Module** | LSTM neural network for 20-day stock price forecasting using TensorFlow |
| **Data Processing Module** | Real-time stock data scraping and historical data analysis |
| **Portfolio Management Module** | Watchlist tracking, buy/sell simulation, and performance analytics |
| **Authentication Module** | JWT-based secure user authentication and authorization |
| **Database Module** | PostgreSQL persistence for user data, portfolios, and transaction history |

## 🛠️ Technology Stack

### Frontend
- Angular 12
- TypeScript
- Angular Material
- Bootstrap 5
- CanvasJS
- Chart.js
- RxJS

### Backend
- Spring Boot 2.5.3
- Java 11
- PostgreSQL
- JPA/Hibernate
- Spring Security
- JWT
- Maven

### Machine Learning
- Python 3.x
- TensorFlow 2.6.0
- Keras
- scikit-learn
- pandas
- numpy
- Flask

### DevOps
- Docker
- GitHub Actions
- Nginx

## 🚀 Getting Started

### Prerequisites
- Node.js 14.x
- Java 11
- Python 3.x
- PostgreSQL
- Maven 3.6+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/amolsr/smart-stocks.git
   cd smart-stocks
   ```

2. **Database Setup**
   - Install PostgreSQL
   - Create database: `smart_stocks`
   - Update connection details in `server/src/main/resources/application.yml`

3. **Backend Setup**
   ```bash
   cd server
   mvn clean install
   mvn spring-boot:run
   ```
   Backend API: `http://localhost:8080`

4. **ML Service Setup**
   ```bash
   cd model
   pip install -r requirements.txt
   python app.py
   ```
   ML service: `http://localhost:5000`

5. **Frontend Setup**
   ```bash
   cd client
   npm install
   npm start
   ```
   Frontend: `http://localhost:4200`

### Environment Configuration

**Database Configuration** (`server/src/main/resources/application.yml`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smart_stocks
    username: your_username
    password: your_password
  jpa:
    hibernate:
      ddl-auto: update
```

**Environment Variables**:
- `DB_URL`: Database connection URL
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `JWT_SECRET`: JWT signing secret


## 📱 Usage

1. **Dashboard**: View market overview, top gainers/losers, and market news
2. **Stock Details**: Analyze individual stocks with charts and predictions
3. **Predictions**: Get AI-powered price forecasts for any stock symbol
4. **Portfolio**: Manage your watchlist and track investments
5. **Education**: Access learning materials and tutorials
6. **Profile**: Manage your account and view transaction history


## 🔧 API Endpoints

### Stock Data
- `GET /api/stocks/gainers` - Get top gaining stocks
- `GET /api/stocks/losers` - Get top losing stocks
- `GET /api/stocks/search` - Search for stocks
- `GET /api/stocks/{symbol}` - Get stock details

### Predictions
- `GET /prediction?symbol={SYMBOL}` - Get price predictions
- `GET /data?type={gainers|losers}` - Get market data

### User Management
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `GET /api/user/profile` - Get user profile

### API Details & Examples

Authentication
- The backend uses JWT for protected endpoints. Obtain a token via `POST /api/auth/login` and include it in requests:

```
Authorization: Bearer <JWT_TOKEN>
```

1) Get top gainers
- Request:

```bash
curl -X GET "http://localhost:8080/api/stocks/gainers"
```

- Example response (HTTP 200):

```json
[
   { "symbol": "ABC", "price": 123.45, "change": 5.2, "percent": 4.41 },
   { "symbol": "XYZ", "price": 67.89, "change": 3.1, "percent": 4.78 }
]
```

2) Get stock details
- Request:

```bash
curl -X GET "http://localhost:8080/api/stocks/RELIANCE"
```

- Example response (HTTP 200):

```json
{
   "symbol": "RELIANCE",
   "companyName": "Reliance Industries Ltd",
   "price": 2500.12,
   "history": [ { "date": "2025-10-10", "close": 2480.50 }, ... ]
}
```

3) Prediction endpoint (ML service)
- The application proxies or calls the ML service. Example endpoints:

- Backend prediction (via server):

```bash
curl -X GET "http://localhost:8080/prediction?symbol=RELIANCE"
```

- Direct ML service (if running separately):

```bash
curl -X GET "http://localhost:5000/predict?symbol=RELIANCE"
```

- Example response (HTTP 200):

```json
{
   "symbol": "RELIANCE",
   "predictions": [ 2505.3, 2510.1, 2498.7, ... ],
   "dates": [ "2025-10-14", "2025-10-15", ... ]
}
```

4) Authenticated user endpoints
- Example: get profile (requires Authorization header)

```bash
curl -H "Authorization: Bearer <JWT>" "http://localhost:8080/api/user/profile"
```

- Response (HTTP 200):

```json
{
   "id": 42,
   "username": "jane.doe",
   "email": "jane@example.com",
   "watchlist": [ "RELIANCE", "TCS" ]
}
```

Error responses
- Common error format:

```json
{ "status": "error", "message": "Description of the problem" }
```

Notes
- Confirm exact paths and parameter names in `server/src/main/java` controllers if you need exact request/response schemas.
- If the ML service is behind the backend, `ML_SERVICE_URL` in environment variables points to the ML service; otherwise the frontend may directly call it.


## 📚 Documentation & Guides



### Developer Guide
- **Frontend**: Angular 12, TypeScript, Material Design
- **Backend**: Spring Boot, PostgreSQL, JWT authentication
- **ML Service**: Python Flask, TensorFlow, LSTM models

### API Endpoints
- `GET /api/stocks/gainers` - Top gaining stocks
- `GET /api/stocks/losers` - Top losing stocks
- `GET /prediction?symbol={SYMBOL}` - Price predictions
- `POST /api/auth/login` - User authentication

## 🤝 Contributing

### Quick Start
1. Fork repository
2. Create feature branch: `git checkout -b feature/name`
3. Follow coding standards (TypeScript, Java 11, Python 3.8+)
4. Add tests and update docs
5. Submit pull request

### Standards
- **Frontend**: Angular style guide, unit tests
- **Backend**: Spring Boot conventions, JUnit tests
- **ML**: PEP 8, type hints, pytest

## 🚀 Deployment
### Production Build
```bash
# Frontend
cd client && ng build --prod

# Backend
cd server && mvn clean package

# ML Service
cd model && pip install -r requirements.txt
```

### Docker Deployment
```bash
docker-compose up -d
```

## 🔧 Troubleshooting

### Common Issues

**Frontend Issues:**
- **Port 4200 in use**: Run `ng serve --port 4201`
- **Node version**: Ensure Node.js 14.x is installed
- **Dependencies**: Delete `node_modules` and run `npm install`

**Backend Issues:**
- **Port 8080 in use**: Change port in `application.yml`
- **Database connection**: Verify PostgreSQL is running
- **Java version**: Ensure Java 11 is installed

**ML Service Issues:**
- **Python dependencies**: Use virtual environment
- **TensorFlow errors**: Check Python version compatibility
- **Port conflicts**: ML service uses port 5000 by default

### Performance Tips
- Use production builds for deployment
- Configure database connection pooling
- Enable caching for API responses
- Optimize ML model loading

## 🧩 Environment Variables
- `DATABASE_URL`: PostgreSQL connection string
- `JWT_SECRET`: JWT signing key
- `ML_SERVICE_URL`: ML service endpoint


## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Contributors

| Username | Name | Role |
|----------|------|------|
| [@amolsr](https://github.com/amolsr) | Amol Saini | Full Stack Developer |
| [@jeevith15](https://github.com/jeevith15) | Jeevith R | Full Stack Developer |

## 🙏 Acknowledgments

- Yahoo Finance API for stock data
- Groww.in for market data scraping
- TensorFlow team for ML libraries
- Angular and Spring Boot communities

## 📞 Support

For support, email support@vishwakraft.com or create an issue in this repository.

---

**Note**: This is a demo project for educational purposes. Please do not use for actual trading without proper risk assessment and financial advice.