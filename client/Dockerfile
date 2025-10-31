# Use Node.js 16 LTS for compatibility with Angular 12 and OpenSSL
FROM node:16-alpine

# Set working directory
WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies with legacy peer deps to handle Angular version conflicts
RUN npm install --legacy-peer-deps

# Copy project files
COPY . .

# Expose port
EXPOSE 4200

# Start Angular development server
CMD ["npm", "start", "--", "--host", "0.0.0.0", "--disable-host-check"]

