import axios from "axios";
import { AsyncLocalStorage } from "async_hooks";

// AsyncLocalStorage holds the client's Authorization token context per request thread
export const authContext = new AsyncLocalStorage<string>();

// Intercept outbound Axios requests and automatically propagate the client's Authorization header
axios.interceptors.request.use((config) => {
  const authHeader = authContext.getStore();
  if (authHeader) {
    config.headers = config.headers || {};
    config.headers["Authorization"] = authHeader;
  }
  return config;
});
