# Smart Stocks 📈

[![Github Pages](https://github.com/amolsr/smart-stocks/actions/workflows/angular.yml/badge.svg)](https://github.com/amolsr/smart-stocks/actions/workflows/angular.yml)

A comprehensive stock market analysis and prediction platform built with Angular, Spring Boot, and Machine Learning. Smart Stocks provides real-time market data, AI-powered price predictions, educational resources, and portfolio management tools.

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

2. **Frontend Setup**
   ```bash
   cd client
   npm install
   npm start
   ```
   Frontend will be available at `http://localhost:4200`

3. **Backend Setup**
   ```bash
   cd server
   mvn clean install
   mvn spring-boot:run
   ```
   Backend API will be available at `http://localhost:8080`

4. **ML Service Setup**
   ```bash
   cd model
   pip install -r requirements.txt
   python app.py
   ```
   ML service will be available at `http://localhost:8080`

### Environment Configuration

Update the database configuration in `/server/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: "your-postgresql-url"
    username: "your-username"
    password: "your-password"
```

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

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

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