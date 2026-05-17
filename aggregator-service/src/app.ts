import express from "express";
import helmet from "helmet";
import morgan from "morgan";
import routes from "./routes/index.js";
import { requestContextMiddleware } from "./middlewares/authMiddleware.js";
import { errorHandlerMiddleware } from "./middlewares/errorMiddleware.js";

const app = express();

// 1. Request Context & Authorization forwarding middleware
app.use(requestContextMiddleware);

// 2. Standard security and logging middlewares
app.use(helmet());

app.use(express.json());
app.use(morgan("dev"));

// 3. Centralized API Router
app.use("/api/v1/bff", routes);

// 4. Global 404 Not Found Handler
app.use((req, res) => {
  res.status(404).json({
    success: false,
    message: `Cannot ${req.method} ${req.url}`,
  });
});

// 5. Global Centralized Error Handler
app.use(errorHandlerMiddleware);

export default app;
