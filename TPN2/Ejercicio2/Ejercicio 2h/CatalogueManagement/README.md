# 📚 Catalogue Management

A multi-tier Spring Boot application for managing a book catalogue. It allows book collectors to create, update, view, and delete books in their personal library.

---

## 🚀 Project Overview

This project includes a Spring Boot API and a React frontend:

- **Backend**: REST API for managing books.
- **Frontend**: React app that uses the REST API.

---

## 🛠️ Tech Stack

- Java 17
- Spring Boot 3
- Maven
- H2 In-Memory Database
- Docker
- Swagger API Documentation 

---

## 📦 Features

- 📖 List all books
- ➕ Add new books
- 📝 Update existing books
- ❌ Delete books
- 📄 Swagger UI for API exploration

---

## ⚙️ How to Run

### Windows

Run `run.bat` from this folder. It opens the backend and frontend in separate consoles. On the first run it downloads Maven and installs the frontend dependencies.

### IntelliJ IDEA

1. Open the folder that contains `pom.xml`.
2. If IntelliJ asks to load Maven, select **Load**. Otherwise, right-click `pom.xml` and choose **Add as Maven Project**.
3. Set the Project SDK to Java 17 or newer.
4. Select **Run CatalogueManagement** in the run menu and press Run.

The shared run configuration is in `.run/Run CatalogueManagement.run.xml`.

### macOS / Linux

Run `./mvnw spring-boot:run` for the backend. In a second terminal, run `cd frontend && npm install && npm run dev`.

Java 17 or newer and Node.js 20 or newer are required. Vite proxies `/api` requests to the backend at `http://localhost:8080`.

## Accessing the API using Swagger
Open your browser and go to http://localhost:8080/swagger-ui/index.html#/

![img.png](img.png)
