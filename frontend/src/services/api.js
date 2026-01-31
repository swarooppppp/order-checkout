import axios from 'axios';

const API_BASE_URL = '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Order API
export const orderApi = {
  getAll: () => api.get('/orders'),
  getById: (id) => api.get(`/orders/${id}`),
  create: (order) => api.post('/orders', order),
  update: (id, order) => api.put(`/orders/${id}`, order),
  updateStatus: (id, status) => api.patch(`/orders/${id}/status?status=${status}`),
  delete: (id) => api.delete(`/orders/${id}`),
  getByCustomerId: (customerId) => api.get(`/orders/customer/${customerId}`),
  getByStatus: (status) => api.get(`/orders/status/${status}`),
};

// Coupon API
export const couponApi = {
  getAll: () => api.get('/coupons'),
  getById: (id) => api.get(`/coupons/${id}`),
  getByCode: (code) => api.get(`/coupons/code/${code}`),
  create: (coupon) => api.post('/coupons', coupon),
  update: (id, coupon) => api.put(`/coupons/${id}`, coupon),
  delete: (id) => api.delete(`/coupons/${id}`),
  deactivate: (id) => api.patch(`/coupons/${id}/deactivate`),
  getActive: () => api.get('/coupons/active'),
  getValid: () => api.get('/coupons/valid'),
  calculateDiscount: (code, orderAmount) => 
    api.post(`/coupons/calculate-discount?code=${code}&orderAmount=${orderAmount}`),
  useCoupon: (code) => api.patch(`/coupons/code/${code}/use`),
};

export default api;
