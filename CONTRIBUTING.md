# Contributing to Smart Stocks 📈

Welcome, contributors 👋

We’re excited to have you help **Smart Stocks**, an open-source AI-powered platform built with Angular, Spring Boot, and Machine Learning designed to deliver real-time insights, price forecasts, and portfolio management tools for investors and learners alike.

Your contributions — whether in code, ideas, or documentation — help empower smarter investing through innovation and collaboration. 📈🤝

## Table of Contents

- [🚀 Quick Start](#🚀-quick-start)
- [🏗️ Project Structure](#🏗️-project-structure)
- [🛠️ Development Setup](#🛠️-development-setup)
  - [Prerequisites](#prerequisites)
  - [Environment Setup](#environment-setup)
- [📋 Coding Standards](#📋-coding-standards)
  - [Frontend (Angular/TypeScript)](#frontend-angulartypescript)
  - [Backend (Spring Boot/Java)](#backend-spring-bootjava)
  - [ML Service (Python)](#ml-service-python)
- [🧪 Testing Guidelines](#🧪-testing-guidelines)
  - [Frontend Testing](#frontend-testing)
  - [Backend Testing](#backend-testing)
  - [ML Service Testing](#ml-service-testing)
- [📝 Documentation](#📝-documentation)
- [🔧 API Guidelines](#🔧-api-guidelines)
  - [Naming Conventions](#naming-conventions)
  - [Example Endpoints](#example-endpoints)
  - [Response Format](#response-format)
- [🐛 Bug Reports](#🐛-bug-reports)
- [✨ Feature Requests](#✨-feature-requests)
- [🔍 Code Review Process](#🔍-code-review-process)
- [🚀 Deployment](#🚀-deployment)
  - [Production Build](#production-build)
  - [Environment Variables](#environment-variables)
- [🤝 Community Guidelines](#🤝-community-guidelines)
- [📞 Getting Help](#📞-getting-help)
- [🏆 Recognition](#🏆-recognition)
- [📄 License](#📄-license)

## 🚀 Quick Start

1. **Fork the repository**
   ```bash
   git clone https://github.com/amolsr/smart-stocks.git
   cd smart-stocks
   ```

2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make your changes** following our coding standards

4. **Test your changes** and update documentation

5. **Submit a pull request**

## 🏗️ Project Structure

Understanding the project architecture will help you contribute effectively:

### Frontend (`/client/`)
- **Framework**: Angular 12 with TypeScript
- **UI Library**: Angular Material + Bootstrap
- **Charts**: CanvasJS, Chart.js, ng2-charts
- **Features**: Responsive design, real-time updates, interactive components

### Backend (`/server/`)
- **Framework**: Spring Boot 2.5.3 with Java 11
- **Database**: PostgreSQL with JPA/Hibernate
- **Security**: JWT-based authentication
- **API**: RESTful APIs with OpenAPI documentation

### ML Service (`/model/`)
- **Framework**: Flask with TensorFlow/Keras
- **ML Libraries**: TensorFlow, scikit-learn, pandas, numpy
- **Features**: LSTM neural networks, price prediction, data scraping

## 🛠️ Development Setup

### Prerequisites
- Node.js 14.x
- Java 11
- Python 3.8+
- PostgreSQL
- Maven 3.6+

### Environment Setup

1. **Database Setup**
   - Install PostgreSQL
   - Create database: `smart_stocks`
   - Update connection details in `server/src/main/resources/application.yml`

2. **Backend Setup**
   ```bash
   cd server
   mvn clean install
   mvn spring-boot:run
   ```
   Backend API: `http://localhost:8080`

3. **ML Service Setup**
   ```bash
   cd model
   pip install -r requirements.txt
   python app.py
   ```
   ML service: `http://localhost:5000`

4. **Frontend Setup**
   ```bash
   cd client
   npm install
   npm start
   ```
   Frontend: `http://localhost:4200`

## 📋 Coding Standards

### Frontend (Angular/TypeScript)
- Follow [Angular Style Guide](https://angular.io/guide/styleguide)
- Use TypeScript strict mode
- Write unit tests for components and services
- Use Angular Material components consistently
- Follow responsive design principles

### Backend (Spring Boot/Java)
- Follow Spring Boot conventions
- Use Java 11 features appropriately
- Write JUnit tests for all services and controllers
- Follow RESTful API design principles
- Use proper exception handling

### ML Service (Python)
- Follow PEP 8 style guide
- Use type hints for function parameters and return types
- Write pytest tests for ML functions
- Document model architecture and parameters
- Use virtual environments for dependency management

## 🧪 Testing Guidelines

### Frontend Testing
```bash
cd client
npm test                    # Run unit tests
npm run e2e                # Run end-to-end tests
npm run lint               # Run linting
```

### Backend Testing
```bash
cd server
mvn test                   # Run unit tests
mvn integration-test       # Run integration tests
```

### ML Service Testing
```bash
cd model
pytest                     # Run tests
python -m pytest --cov    # Run with coverage
```

## 📝 Documentation

When contributing, please:

- Update relevant documentation
- Add comments for complex logic
- Update API documentation for new endpoints
- Include examples in your documentation
- Update README.md if adding new features

## 🔧 API Guidelines

When adding new API endpoints:

### Naming Conventions
- Use RESTful conventions
- Use plural nouns for resources
- Use HTTP methods appropriately (GET, POST, PUT, DELETE)

### Example Endpoints
```
GET /api/stocks/gainers     # Get top gaining stocks
POST /api/user/watchlist    # Add to watchlist
PUT /api/user/profile       # Update profile
DELETE /api/stocks/{id}     # Remove stock
```

### Response Format
```json
{
  "success": true,
  "data": {},
  "message": "Success message",
  "timestamp": "2023-01-01T00:00:00Z"
}
```

## 🐛 Bug Reports

When reporting bugs, please include:

- Clear description of the issue
- Steps to reproduce
- Expected vs actual behavior
- Environment details (OS, browser, versions)
- Screenshots if applicable
- Error logs or console output

## ✨ Feature Requests

For new features:

- Describe the feature and its benefits
- Provide use cases
- Consider impact on existing functionality
- Discuss implementation approach
- Include mockups or wireframes if applicable

## 🔍 Code Review Process

All contributions go through code review:

1. **Automated Checks**: CI/CD pipeline runs tests and linting
2. **Peer Review**: At least one maintainer reviews the code
3. **Testing**: Verify functionality works as expected
4. **Documentation**: Ensure documentation is updated
5. **Merge**: Approved changes are merged to main branch

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

### Environment Variables
- `DATABASE_URL`: PostgreSQL connection string
- `JWT_SECRET`: JWT signing key
- `ML_SERVICE_URL`: ML service endpoint

## 🤝 Community Guidelines

- Be respectful and inclusive
- Help others learn and grow
- Provide constructive feedback
- Follow the code of conduct
- Ask questions if you're unsure

## 📞 Getting Help

- Create an issue for bugs or feature requests
- Join discussions in existing issues
- Email: support@vishwakraft.com
- Check existing documentation and README

## 🏆 Recognition

Contributors are recognized in:
- README.md contributors section
- Release notes for significant contributions
- GitHub contributor graphs

## 📄 License

By contributing, you agree that your contributions will be licensed under the **MIT License**.

---

### Thank you for contributing to Smart Stocks! 💫

We truly appreciate your time, effort, and passion — your contributions make this project smarter, stronger, and better for everyone.  
Whether you're fixing a bug, improving docs, adding features, or sharing ideas, you're helping build a platform that empowers investors and learners around the world.  

Thank you for being part of the journey — happy contributing! 🚀✨

