# JashinColor Backend

A Spring Boot backend system for creating coloring books from user-uploaded images, with payment processing and PDF generation.

## Features

- **User Authentication**: Register, login, JWT-based authentication
- **Book Management**: Create coloring books from images, download as PDF
- **Payment Processing**: Manual PIX QR Code (free - no fees)
- **Image Processing**: Automatic image optimization and line detection
- **Cloud Storage**: Cloudinary integration for image storage

## Requirements

- Java 17+
- MySQL 8.0+
- Maven 3.8+

## Setup

1. **Clone the repository**
2. **Configure database** in `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/jashincolor
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

3. **Create database**:
   ```sql
   CREATE DATABASE jashincolor;
   ```

4. **Configure Cloudinary** (optional - using default values):
   ```properties
   cloudinary.cloud-name=your_cloud_name
   cloudinary.api-key=your_api_key
   cloudinary.api-secret=your_api_secret
   ```

5. **Configure PIX** (for payment QR codes):
   ```properties
   pix.key=YOUR_PIX_KEY
   pix.beneficiary-name=YOUR_NAME
   pix.city=YOUR_CITY
   ```

6. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

The server will start at `http://localhost:8080`

## API Endpoints

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login user |
| GET | `/api/auth/validate` | Validate JWT token |
| POST | `/api/auth/refresh` | Refresh JWT token |

#### Register Request
```json
{
  "email": "user@example.com",
  "password": "your_password"
}
```

#### Login Request
```json
{
  "email": "user@example.com",
  "password": "your_password"
}
```

#### Response
```json
{
  "token": "eyJhbGc...",
  "type": "Bearer",
  "userId": 1,
  "email": "user@example.com",
  "message": "Login realizado com sucesso"
}
```

---

### Books

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/books` | Create a new book with images |
| GET | `/api/books/{userId}` | Get all books for a user |
| GET | `/api/books/{bookId}/images` | Get images for a book |
| GET | `/api/books/{bookId}/download-url` | Download book as PDF |

#### Create Book
- **Endpoint**: POST `/api/books`
- **Content-Type**: multipart/form-data
- **Parameters**:
  - `userId` (Long): User ID
  - `files` (List<MultipartFile>): Image files

#### Get User Books
- **Endpoint**: GET `/api/books/{userId}`
- **Response**: List of books

#### Download PDF
- **Endpoint**: GET `/api/books/{bookId}/download-url`
- **Response**: PDF file

---

### Payments (Manual PIX)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/pix/generate` | Generate PIX QR Code |
| POST | `/api/payments/register` | Register a payment |
| POST | `/api/payments/{payId}/pay` | Confirm payment |
| GET | `/api/payments/book/{bookPayId}` | Get payment by book ID |

#### Generate PIX QR Code
- **Endpoint**: POST `/api/pix/generate`
- **Content-Type**: application/json

Generate a QR code that customers scan to pay manually via PIX.

```json
{
  "bookId": 1,
  "amount": 29.90
}
```

**Response:**
```json
{
  "bookId": 1,
  "amount": 29.90,
  "qrCodeImage": "data:image/png;base64,...",
  "message": "Escaneie o QR Code para realizar o pagamento"
}
```

**Payment Flow:**
1. Frontend calls `/api/pix/generate` to get QR code image
2. Display QR code to customer
3. Customer scans and pays manually via their bank app
4. You verify the payment manually in your bank
5. Call `/api/payments/{payId}/pay` to confirm and unlock download

This method has **no platform fees** and is completely free!

#### Register Payment
- **Endpoint**: POST `/api/payments/register`
- **Parameters**:
  - `bookPayId` (Long): Book ID to pay for
  - `transactionId` (String): Transaction ID from payment provider

#### Confirm Payment
- **Endpoint**: POST `/api/payments/{payId}/pay`
- Confirms payment and unlocks PDF download

---

## Configuration

Edit `src/main/resources/application.properties`:

| Property | Description | Default |
|----------|-------------|---------|
| `jwt.secret` | JWT secret key | (generated) |
| `jwt.expiration` | JWT expiration (ms) | 86400000 (24h) |
| `cors.allowed.origins` | CORS allowed origins | localhost:3000, localhost:5173 |
| `image.processing.max-width` | Max image width | 3000 |
| `image.processing.max-height` | Max image height | 3000 |
| `image.processing.min-dpi` | Minimum DPI | 300 |
| `pdf.page-size` | PDF page size | A4 |

### PIX Configuration

Configure your PIX key in `application.properties`:

```properties
pix.key=YOUR_PIX_KEY
pix.beneficiary-name=YOUR_NAME
pix.city=YOUR_CITY
```

- `pix.key`: Your PIX key (CPF, email, phone, or random key)
- `pix.beneficiary-name`: Your name or business name
- `pix.city`: Your city

## Security

- JWT-based authentication
- Passwords encrypted with BCrypt
- CORS configured for specific origins
- File validation for uploads

## Tech Stack

- Spring Boot 3.4.5
- Spring Security
- Spring Data JPA
- MySQL
- JWT (jjwt)
- Cloudinary
- iTextPDF
- BoofCV (image processing)
- ZXing (QR Code generation)
