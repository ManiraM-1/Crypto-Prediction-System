# -----------------------------------------------------------------------------
# STAGE 1: Build the React App
# -----------------------------------------------------------------------------
FROM node:18-alpine as build

WORKDIR /app

# Copy dependency definitions
COPY package*.json ./

# Install dependencies (clean install)
RUN npm ci --legacy-peer-deps || npm install --legacy-peer-deps

# Copy the rest of the code
COPY . .

# Build the app
# (This creates a 'dist' folder for Vite or 'build' for Create React App)
RUN npm run build

# -----------------------------------------------------------------------------
# STAGE 2: Serve with Nginx
# -----------------------------------------------------------------------------
FROM nginx:alpine

# Copy the build output to Nginx's serving directory
# NOTE: If you use 'Create React App', change '/app/dist' to '/app/build' below!
COPY --from=build /app/dist /usr/share/nginx/html

# Configure Nginx for Port 7860 (Hugging Face Requirement)
RUN echo 'server { \
    listen 7860; \
    server_name localhost; \
    location / { \
        root /usr/share/nginx/html; \
        index index.html index.htm; \
        try_files $uri $uri/ /index.html; \
    } \
}' > /etc/nginx/conf.d/default.conf

# Fix permissions for Hugging Face user
RUN chmod -R 777 /var/log/nginx /var/cache/nginx /var/run \
    && chmod -R 777 /etc/nginx/*

# Expose the port
EXPOSE 7860

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]