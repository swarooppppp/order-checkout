# Order Management Application

A Spring Boot application for managing orders with H2 in-memory database.

## Tech Stack

- **Java**: 17
- **Spring Boot**: 3.2.2
- **Database**: H2 (In-Memory)
- **Build Tool**: Maven

## Project Structure

```
src/main/java/com/example/ordermanagement/
├── OrderManagementApplication.java    # Main application class
├── controller/
│   └── OrderController.java           # REST API endpoints
├── entity/
│   ├── Order.java                     # Order entity
│   └── OrderStatus.java               # Order status enum
├── exception/
│   ├── GlobalExceptionHandler.java    # Global exception handling
│   └── ResourceNotFoundException.java # Custom exception
├── repository/
│   └── OrderRepository.java           # Data access layer
└── service/
    └── OrderService.java              # Business logic layer
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Running the Application

1. Clone the repository
2. Navigate to the project directory
3. Run the application:

```bash
mvn spring-boot:run
```

Or build and run:

```bash
mvn clean package
java -jar target/order-management-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## API Endpoints

### Orders

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/orders` | Get all orders |
| GET | `/api/orders/{id}` | Get order by ID |
| POST | `/api/orders` | Create a new order |
| PUT | `/api/orders/{id}` | Update an order |
| PATCH | `/api/orders/{id}/status?status=STATUS` | Update order status |
| DELETE | `/api/orders/{id}` | Delete an order |
| GET | `/api/orders/customer?name=NAME` | Get orders by customer name |
| GET | `/api/orders/status/{status}` | Get orders by status |

### Order Status Values

- `PENDING`
- `CONFIRMED`
- `PROCESSING`
- `SHIPPED`
- `DELIVERED`
- `CANCELLED`

## Sample Request

### Create Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "John Doe",
    "productName": "Laptop",
    "quantity": 1,
    "price": 999.99
  }'
```

### Response

```json
{
  "id": 1,
  "customerName": "John Doe",
  "productName": "Laptop",
  "quantity": 1,
  "price": 999.99,
  "status": "PENDING",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

## H2 Console

Access the H2 database console at: `http://localhost:8080/h2-console`

**Connection Details:**
- JDBC URL: `jdbc:h2:mem:orderdb`
- Username: `sa`
- Password: (leave empty)

## Configuration

Application properties can be modified in `src/main/resources/application.properties`

## License

This project is open source.
