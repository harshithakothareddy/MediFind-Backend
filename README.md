# MediFind – Backend API

MediFind Backend is the server-side application for the MediFind digital healthcare platform. It provides REST APIs for user authentication, medicine discovery, pharmacy operations, inventory management, and administration.

## Project Links

* **Frontend Website:** https://medifind-frontend-khaki.vercel.app
* **Swagger API Documentation:** https://medifind-backend-6f8q.onrender.com/api/swagger-ui/index.html
* **Frontend Repository:** https://github.com/harshithakothareddy/MediFind-Frontend
* **Backend Repository:** https://github.com/harshithakothareddy/MediFind-Backend

## Features

* User registration and authentication.
* JWT-based authentication.
* Medicine and pharmacy data management.
* Pharmacy inventory management.
* Administrative operations.
* MySQL database integration.
* REST APIs for frontend communication.

## Technologies Used

* Java 21
* Spring Boot
* Spring Data JPA
* Hibernate
* MySQL
* Maven
* REST APIs
* JWT authentication
* Docker

## Project Structure

The backend includes modules for:

* `entity` – application entities and data models.
* `controller` – REST API controllers.
* `admin` – administrative functionality.
* `pharmacy` – pharmacy-related operations.
* `user` – user-related operations.
* `public` – public-facing functionality.

## Getting Started

### Prerequisites

* Java 21
* MySQL
* Git
* Maven or the included Maven Wrapper

### Installation

1. Clone the repository:

   ```bash
   git clone https://github.com/harshithakothareddy/MediFind-Backend.git
   ```

2. Navigate to the project folder:

   ```bash
   cd MediFind-Backend
   ```

3. Configure the required environment variables for your database and authentication.

| Variable                 | Purpose                                    |
| ------------------------ | ------------------------------------------ |
| `DB_URL`                 | MySQL JDBC connection URL                  |
| `DB_USERNAME`            | Database username                          |
| `DB_PASSWORD`            | Database password                          |
| `JWT_SECRET`             | JWT signing secret                         |
| `FRONTEND_URL`           | Frontend origin for CORS                   |
| `SEED_DEMO_USERS`        | Demo-user seeding configuration            |
| `DEMO_ADMIN_PASSWORD`    | Demo administrator password, if configured |
| `DEMO_PHARMACY_PASSWORD` | Demo pharmacy password, if configured      |
| `DEMO_USER_PASSWORD`     | Demo user password, if configured          |

Never publish production credentials or secrets.

4. Run the application on Windows:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   Alternatively, if Maven is installed:

   ```bash
   mvn spring-boot:run
   ```

The application is configured to use port `8080` and the `/api` context path.

## Docker

The repository includes a Dockerfile for containerized deployment.

Build the Docker image:

```bash
docker build -t medifind-backend .
```

Run the container:

```bash
docker run -p 8080:8080 medifind-backend
```

Configure the required environment variables and database connection before running the container.

## API Documentation

Swagger UI is intended to provide an interactive view of the available API endpoints:

https://medifind-backend-6f8q.onrender.com/api/swagger-ui/index.html

Access to individual endpoints may require authentication. Confirm that the documentation link is publicly accessible before submitting it.

## Database and Deployment

* **Database:** MySQL hosted on Aiven.
* **Backend hosting:** Render.
* **Frontend hosting:** Vercel.

## Project Objective

MediFind aims to improve medicine accessibility and pharmacy coordination through a centralized digital healthcare platform.

---

**Security:** Never commit database passwords, JWT secrets, private keys, or real user information to GitHub.
