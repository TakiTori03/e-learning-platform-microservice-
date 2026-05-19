# 🎓 E-Learning Platform - Hệ Thống Microservices Đào Tạo Trực Tuyến

> **Hệ thống phân tán hiệu năng cao tích hợp Trí tuệ Nhân tạo (AI RAG), Bảo vệ bản quyền video DRM HLS, Giao dịch phân tán Saga bền vững và cổng thanh toán VNPay.**

---

## 🏛️ 1. Bản Đồ Kiến Trúc Hệ Thống (System Architecture Map)

Hệ thống được thiết kế theo kiến trúc **Microservices** phân rã hoàn toàn để tối ưu khả năng mở rộng (Scalability), giao tiếp đồng bộ hiệu năng cao qua **REST / OpenFeign** và giao tiếp bất đồng bộ phi trạng thái qua **Apache Kafka**. Bảo mật tập trung được đảm bảo bằng **Keycloak (OAuth2/OIDC)** và **API Gateway**, đi kèm với dịch vụ bóc tách dữ liệu nâng cao sử dụng **FastAPI (Python)** và **Whisper.cpp (C++)**.

## 📂 2. Danh Sách Các Microservices & Công Nghệ Sử Dụng

Hệ thống bao gồm **12 mô-đun nghiệp vụ chính** hoạt động độc lập và bổ trợ lẫn nhau:

| Mô-đun (Service)           | Cổng (Port) | Cơ sở dữ liệu (Database)    | Công nghệ & Nhiệm vụ cốt lõi                                                                                                                                                                                           |
| :------------------------- | :---------: | :-------------------------- | :--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`api-gateway`**          |   `8080`    | _Không_                     | **Spring Cloud Gateway**. Định tuyến, Rate Limiting, CORS, phân quyền JWT tập trung.                                                                                                                                   |
| **`discovery-service`**    |   `8761`    | _Không_                     | **Netflix Eureka Server**. Đăng ký và phát hiện dịch vụ tự động trong môi trường phân tán.                                                                                                                             |
| **`aggregator-service`**   |   `3000`    | _Không_                     | **BFF (Backend For Frontend) - Node.js, TypeScript, Express**. Tổng hợp dữ liệu phân tán (Data Aggregation) từ nhiều microservice hạ nguồn song song (`Promise.all`) nhằm tăng tối đa tốc độ hiển thị Dashboard Admin. |
| **`common-library`**       | _Thư viện_  | _Không_                     | **Shared Core**. Chứa AOP Custom Aspects (`@CheckCourseOwner`, `@CustomCache`, `@TrackPerformance`), Exception Handler tập trung và các DTO dùng chung.                                                                |
| **`identity-service`**     |   `9000`    | PostgreSQL (`identity_db`)  | **Keycloak, Redis**. Xác thực người dùng, phân quyền RBAC, kiểm tra danh sách đen logout token trên Redis (`RedisBlacklistJwtValidator`).                                                                              |
| **`course-service`**       |   `9001`    | MongoDB (`course_db`)       | Quản lý danh mục khóa học, chương học, bài giảng đa phương tiện hỗ trợ CRUD hiệu năng cao.                                                                                                                             |
| **`media-service`**        |   `9002`    | MongoDB (`media_db`)        | **MinIO S3**. Quản lý tài nguyên media, cấp phát Presigned URL để upload tệp siêu tốc bỏ qua Gateway tránh nghẽn RAM, xác thực quyền xem khóa giải mã video.                                                           |
| **`order-service`**        |   `9004`    | PostgreSQL (`order_db`)     | **VNPay, Transactional Outbox**. Xử lý giỏ hàng, đơn hàng, IPN Webhook VNPay tin cậy cao và đẩy sự kiện Outbox lên Kafka qua Scheduler Poller.                                                                         |
| **`learning-service`**     |   `9005`    | MongoDB (`learning_db`)     | Đăng ký học viên, theo dõi tiến độ xem video bài học từng giây, tự động tính % hoàn thành khóa học để cấp chứng chỉ.                                                                                                   |
| **`interaction-service`**  |   `9003`    | MongoDB (`interaction_db`)  | Quản lý Hỏi đáp bài học (Discussion), Đánh giá khóa học (Reviews) và Ticket hỗ trợ (Feedback).                                                                                                                         |
| **`notification-service`** |   `9006`    | MongoDB (`notification_db`) | Gửi Email chào mừng, xác nhận thanh toán bất đồng bộ tiêu thụ sự kiện từ Kafka.                                                                                                                                        |
| **`assessment-service`**   |   `9007`    | MongoDB (`assessment_db`)   | Quản lý ngân hàng câu hỏi, bài tập lớn, chấm điểm trắc nghiệm tự động.                                                                                                                                                 |
| **`worker-service`**       |   `9088`    | _Không_                     | **FFmpeg, Whisper.cpp**. Offload các tác vụ nặng: Nén nạp video HLS Stream, trích xuất ảnh thu nhỏ, tách nhạc WAV mono 16kHz, chạy bóc băng giọng nói offline.                                                         |
| **`pdf-parser-service`**   |   `8090`    | _Không_                     | **Python, FastAPI, fitz (PyMuPDF), PaddleOCR**. OCR PDF tài liệu học tập tăng tốc bằng **GPU CUDA** (hỗ trợ fallback CPU), chuyển đổi bảng biểu sang Markdown, cắt phân đoạn thông minh kèm tiêu đề phân cấp.          |
| **`ai-service`**           |   `8099`    | PostgreSQL (`pgvector`)     | **Spring AI, Gemini API, RAG**. Trợ lý ảo AI Tutor RAG trả lời thông minh dựa trên giáo trình khóa học.                                                                                                                |

---

## 💎 3. Các Điểm Sáng Kiến Trúc Kỹ Thuật Độc Đáo (Enterprise Architectural Highlights)

### 🔄 3.1. Giao Dịch Phân Tán Saga Choreography & Transactional Outbox Pattern

Để đảm bảo tính nhất quán dữ liệu tuyệt đối (**Eventual Consistency**) giữa dịch vụ thanh toán (`order-service`) và dịch vụ ghi danh (`learning-service`), hệ thống triển khai mô hình giao dịch phân tán không đồng bộ:

1. **Tránh lỗi Dual-write:** Khi VNPay gọi IPN Webhook thành công, `order-service` cập nhật trạng thái đơn hàng thành `PAYMENT_SUCCESS` và ghi một bản ghi sự kiện `OrderPaidEvent` vào bảng `outbox_event` dưới **cùng một Database Transaction**.
2. **Đảm bảo phân phát tin cậy (At-Least-Once Delivery):** Một `@Scheduled` `OutboxPoller` chạy ngầm quét các sự kiện `PENDING`, gửi sự kiện lên Kafka đồng bộ (`.get()`) đảm bảo tin nhắn được ghi nhận trên Broker trước khi chuyển trạng thái sự kiện thành `PROCESSED`.
3. **Saga Orchestration & Compensating Transaction:** `learning-service` lắng nghe sự kiện, tiến hành ghi danh với cơ chế **Tự động thử lại (3 attempts)**. Nếu thành công, nó gửi sự kiện `SUCCESS` để `order-service` hoàn tất đơn hàng (`COMPLETED`). Nếu thất bại hoàn toàn sau 3 lần thử lại, sự kiện `FAILED` được phát ra để kích hoạt **Giao dịch bù (Compensating Transaction)** đưa đơn hàng về trạng thái `FAILED` phục vụ xử lý hoàn tiền tự động hoặc thủ công.

### 🎥 3.2. Asynchronous Video Transcoding HLS DRM & Local Speech-To-Text Pipeline

- **Upload tối ưu:** Hệ thống sinh **S3 Presigned PUT URLs** từ `media-service` cho phép Client tải video trực tiếp lên MinIO, bỏ qua API Gateway để loại bỏ rủi ro nghẽn RAM do truyền luồng tệp lớn.
- **Bảo vệ bản quyền DRM:** `worker-service` tải video thô từ MinIO về máy cục bộ, sử dụng **FFmpeg** cắt lát thành định dạng **Adaptive HLS (phân đoạn `.ts`) mã hóa cứng AES-128**. Khóa giải mã chỉ có thể truy cập qua một API nội bộ bảo vệ chéo bằng Feign Client kết nối tới `learning-service` xác thực quyền sở hữu khóa học của học viên.
- **Offline Speech-To-Text (Whisper.cpp):** Tách âm thanh từ video sang chuẩn WAV mono 16kHz PCM. Gọi tiến trình thực thi **Whisper.cpp** cục bộ kết hợp mô hình neural `ggml-base.bin` để bóc băng tiếng Việt/tiếng Anh offline hoàn chỉnh ra tệp WebVTT, tiết kiệm 100% chi phí API đám mây.
- **Smart Semantic Chunking & Citation Alignment:** Phân mảnh file WebVTT thành các khối văn bản tối ưu khoảng 120 từ (~1-1.5 phút giảng dạy), đính kèm mốc thời gian bắt đầu chính xác làm nguồn trích dẫn (**Citation**), phát sự kiện `RawTextIngestedEvent` lên Kafka để lưu vào Vector Database.

### 🐍 3.3. GPU-Accelerated AI PDF Parser & Structural Chunking (Python FastAPI)

Hệ thống tích hợp một bộ phân tích tài liệu PDF nâng cao viết bằng Python FastAPI:

- **Nhận dạng lai (Hybrid OCR):** Trích xuất văn bản phẳng cấu trúc phân cấp bằng thuật toán hình học của `fitz` (PyMuPDF) để định dạng tiêu đề Markdown `#` hoặc `##` dựa trên font size/bold và nhận diện bảng biểu thành **Markdown Tables**. Tự động kích hoạt **PaddleOCR tăng tốc GPU CUDA** (dự phòng CPU và tự động liên kết DLL) khi phát hiện trang quét dạng ảnh (< 50 ký tự).
- **Sliding Window & Contextual Prefixing:** Để tối ưu hóa độ chính xác cho Vector Search trong RAG, tài liệu được cắt nhỏ theo trang với độ đè dịch (overlap 200 ký tự). Mỗi phân đoạn được chèn tiền tố tiêu đề phân cấp động dạng `### [Chủ đề: Active Heading] (Trang X)` giúp mô hình ngôn ngữ lớn (LLM) luôn giữ vững ngữ cảnh phân cấp khi truy xuất.

### 🛡️ 3.4. Hệ Thống Spring AOP Enterprise Framework (`common-library`)

- **`@CheckCourseOwner` (Distributed Ownership Guard):** Sử dụng AspectJ và SpEL để phân tích cú pháp tham số đầu vào động. Kết hợp với Spring `ApplicationContext` gọi bean `CourseIdResolver` tương ứng để truy vấn ngược từ ID tài nguyên ra Course ID, đối chiếu quyền sở hữu thời gian thực trên **Shared Redis** nhằm loại bỏ triệt để tấn công leo thang đặc quyền ngang (**Horizontal Privilege Escalation**).
- **`@CustomCache` (Resilient Cache with Circuit-Breaker):** Tự động hóa việc lưu và đọc bộ đệm Redis. Các thao tác Redis được bao bọc bằng khối try-catch an toàn; nếu cụm Redis gặp sự cố kết nối, Aspect sẽ tự động ghi log cảnh báo và **chuyển hướng truy vấn trực tiếp xuống Database gốc mượt mà**, triệt tiêu lỗi chuỗi (Cascading Failure).
- **`@TrackPerformance` (SLA Speed Monitor):** AspectJ giám sát tốc độ xử lý phương thức qua Spring `StopWatch` với độ chính xác micro-giây, chủ động đưa ra cảnh báo `SLOW METHOD WARNING` khi tốc độ thực thi vượt quá ngưỡng cam kết dịch vụ (SLA Threshold).

---

## 🚀 4. Hướng Dẫn Cài Đặt Và Khởi Chạy (Quick Start Guide)

### 📋 4.1. Yêu Cầu Hệ Thống (Prerequisites)

- Java JDK 17 trở lên.
- Docker và Docker Desktop.
- Maven 3.8+.
- FFmpeg (đã cài đặt và cấu hình biến môi trường Path).
- Python 3.10+ (nếu muốn khởi chạy `pdf-parser-service` cục bộ).

### 🐳 4.2. Khởi Chạy Hạ Tầng Bằng Docker Compose

Chạy các dịch vụ cơ sở dữ liệu và trung gian (PostgreSQL, MongoDB, Kafka, Redis, MinIO, Keycloak) từ thư mục gốc:

```powershell
docker-compose -f docker/docker-compose.yml up -d
```

### 🛠️ 4.3. Biên Dịch Toàn Bộ Dự Án (Maven Multi-Module)

Biên dịch dự án từ thư mục gốc để tải và cài đặt toàn bộ dependencies cũng như cài đặt `common-library`:

```powershell
.\mvnw clean compile
```

### 🏃 4.4. Trình Tự Khởi Chạy Các Dịch Vụ (Startup Order)

Để hệ thống khởi chạy ổn định, hãy tuân thủ trình tự sau:

1. **Dịch vụ Đăng Ký:** Khởi chạy `discovery-service` (Eureka Server) trước.
2. **Dịch vụ Cổng & Xác thực:** Khởi chạy `api-gateway` và `identity-service` (đảm bảo Keycloak đã chạy).
3. **Các Dịch vụ Nghiệp vụ:** Chạy các dịch vụ `course-service`, `media-service`, `interaction-service`, `order-service`, `learning-service`, `notification-service`, `assessment-service`.
4. **Các Dịch vụ Xử lý Nền & AI:** Khởi chạy `worker-service`, `pdf-parser-service` (Python), và `ai-service`.

Bạn có thể chạy trực tiếp từng dịch vụ bằng Maven từ thư mục con của dịch vụ đó:

```powershell
.\mvnw spring-boot:run
```

---

## 🔒 5. Bảo Mật & Xác Thực (Security Framework)

- **Xác thực tập trung:** Hệ thống áp dụng chuẩn bảo mật **OAuth2** kết hợp máy chủ định danh **Keycloak**.
- **Phân quyền người dùng:** Gồm 3 nhóm quyền chính: `STUDENT` (Học viên), `INSTRUCTOR` (Giảng viên sở hữu khóa học), và `ADMIN` (Quản trị viên hệ thống).
- **Phân quyền tài nguyên:** Sử dụng Custom annotation `@CheckCourseOwner` đi kèm các Component Resolvers linh hoạt giúp xác minh quyền sở hữu tài nguyên khóa học theo thời gian thực (Real-time Ownership Verification) trên môi trường phân tán.
- **Immediate Revocation:** Khi người dùng nhấn Đăng xuất, Access Token sẽ được đưa vào danh sách đen lưu trên Redis với thời gian hết hạn khớp với TTL của token thông qua `RedisBlacklistJwtValidator` tích hợp trong bộ lọc bảo mật (`SecurityConfig`).
