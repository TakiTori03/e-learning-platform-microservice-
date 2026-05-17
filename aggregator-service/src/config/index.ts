import dotenv from 'dotenv';

dotenv.config();

export const config = {
  port: parseInt(process.env.PORT || '4000', 10),
  gatewayUrl: process.env.GATEWAY_URL || 'http://localhost:8080',
  nodeEnv: process.env.NODE_ENV || 'development'
};
