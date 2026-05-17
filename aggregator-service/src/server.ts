import app from './app.js';
import { config } from './config/index.js';

const server = app.listen(config.port, () => {
  console.log(`=========================================`);
  console.log(`🚀 Aggregator Service (BFF) is running!`);
  console.log(`🔊 Listening on Port: ${config.port}`);
  console.log(`⚙️  Environment: ${config.nodeEnv}`);
  console.log(`🔗 Target Gateway URL: ${config.gatewayUrl}`);
  console.log(`=========================================`);
});

process.on('SIGTERM', () => {
  console.log('SIGTERM signal received: closing HTTP server');
  server.close(() => {
    console.log('HTTP server closed');
  });
});
