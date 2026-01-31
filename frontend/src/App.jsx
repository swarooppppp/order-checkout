import { useState, useEffect } from 'react';
import { orderApi, couponApi } from './services/api';
import './App.css';

function App() {
  const [orders, setOrders] = useState([]);
  const [coupons, setCoupons] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('orders');

  // Checkout state
  const [checkoutForm, setCheckoutForm] = useState({
    name: '',
    originalAmount: '',
    customerId: '',
  });
  const [couponCode, setCouponCode] = useState('');
  const [discount, setDiscount] = useState(null);
  const [finalAmount, setFinalAmount] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const [ordersRes, couponsRes] = await Promise.all([
        orderApi.getAll(),
        couponApi.getAll(),
      ]);
      setOrders(ordersRes.data);
      setCoupons(couponsRes.data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch data. Make sure the backend is running.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleApplyCoupon = async () => {
    if (!couponCode || !checkoutForm.originalAmount) {
      setError('Please enter order amount and coupon code');
      return;
    }
    try {
      const response = await couponApi.calculateDiscount(
        couponCode,
        checkoutForm.originalAmount
      );
      setDiscount(response.data.discount);
      setFinalAmount(response.data.finalAmount);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Invalid coupon or coupon cannot be applied');
      setDiscount(null);
      setFinalAmount(null);
    }
  };

  const handleCreateOrder = async (e) => {
    e.preventDefault();
    try {
      const orderData = {
        name: checkoutForm.name,
        originalAmount: parseFloat(checkoutForm.originalAmount),
        finalAmount: finalAmount || parseFloat(checkoutForm.originalAmount),
        customerId: parseInt(checkoutForm.customerId),
      };
      await orderApi.create(orderData);
      
      // Increment coupon used count if a coupon was applied
      if (couponCode && discount !== null) {
        try {
          await couponApi.useCoupon(couponCode);
        } catch (couponErr) {
          console.error('Failed to update coupon usage:', couponErr);
        }
      }
      
      setCheckoutForm({ name: '', originalAmount: '', customerId: '' });
      setCouponCode('');
      setDiscount(null);
      setFinalAmount(null);
      fetchData();
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create order');
    }
  };

  const handleUpdateStatus = async (id, status) => {
    try {
      await orderApi.updateStatus(id, status);
      fetchData();
    } catch (err) {
      setError('Failed to update order status');
    }
  };

  const handleDeleteOrder = async (id) => {
    try {
      await orderApi.delete(id);
      fetchData();
    } catch (err) {
      setError('Failed to delete order');
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'CREATED': return '#3498db';
      case 'PAID': return '#27ae60';
      case 'CANCELLED': return '#e74c3c';
      default: return '#95a5a6';
    }
  };

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  return (
    <div className="app">
      <header className="header">
        <h1>Order Management System</h1>
      </header>

      {error && <div className="error-banner">{error}</div>}

      <nav className="tabs">
        <button
          className={activeTab === 'orders' ? 'active' : ''}
          onClick={() => setActiveTab('orders')}
        >
          Orders ({orders.length})
        </button>
        <button
          className={activeTab === 'checkout' ? 'active' : ''}
          onClick={() => setActiveTab('checkout')}
        >
          New Checkout
        </button>
        <button
          className={activeTab === 'coupons' ? 'active' : ''}
          onClick={() => setActiveTab('coupons')}
        >
          Coupons ({coupons.length})
        </button>
      </nav>

      <main className="content">
        {activeTab === 'orders' && (
          <div className="orders-section">
            <h2>All Orders</h2>
            {orders.length === 0 ? (
              <p className="empty-state">No orders yet. Create one from the Checkout tab.</p>
            ) : (
              <div className="orders-grid">
                {orders.map((order) => (
                  <div key={order.id} className="order-card">
                    <div className="order-header">
                      <span className="order-id">#{order.id}</span>
                      <span
                        className="order-status"
                        style={{ backgroundColor: getStatusColor(order.status) }}
                      >
                        {order.status}
                      </span>
                    </div>
                    <h3>{order.name}</h3>
                    <div className="order-details">
                      <p><strong>Customer ID:</strong> {order.customerId}</p>
                      <p><strong>Original:</strong> ${order.originalAmount?.toFixed(2)}</p>
                      <p><strong>Final:</strong> ${order.finalAmount?.toFixed(2)}</p>
                      {order.originalAmount !== order.finalAmount && (
                        <p className="savings">
                          You saved: ${(order.originalAmount - order.finalAmount).toFixed(2)}
                        </p>
                      )}
                    </div>
                    <div className="order-actions">
                      {order.status === 'CREATED' && (
                        <>
                          <button
                            className="btn btn-success"
                            onClick={() => handleUpdateStatus(order.id, 'PAID')}
                          >
                            Mark Paid
                          </button>
                          <button
                            className="btn btn-danger"
                            onClick={() => handleUpdateStatus(order.id, 'CANCELLED')}
                          >
                            Cancel
                          </button>
                        </>
                      )}
                      <button
                        className="btn btn-outline"
                        onClick={() => handleDeleteOrder(order.id)}
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 'checkout' && (
          <div className="checkout-section">
            <h2>Checkout</h2>
            <form onSubmit={handleCreateOrder} className="checkout-form">
              <div className="form-group">
                <label>Order Name</label>
                <input
                  type="text"
                  value={checkoutForm.name}
                  onChange={(e) => setCheckoutForm({ ...checkoutForm, name: e.target.value })}
                  placeholder="e.g., Premium Laptop"
                  required
                />
              </div>
              <div className="form-group">
                <label>Amount ($)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={checkoutForm.originalAmount}
                  onChange={(e) => {
                    setCheckoutForm({ ...checkoutForm, originalAmount: e.target.value });
                    setDiscount(null);
                    setFinalAmount(null);
                  }}
                  placeholder="e.g., 199.99"
                  required
                />
              </div>
              <div className="form-group">
                <label>Customer ID</label>
                <input
                  type="number"
                  value={checkoutForm.customerId}
                  onChange={(e) => setCheckoutForm({ ...checkoutForm, customerId: e.target.value })}
                  placeholder="e.g., 1001"
                  required
                />
              </div>

              <div className="coupon-section">
                <h3>Apply Coupon</h3>
                <div className="coupon-input">
                  <input
                    type="text"
                    value={couponCode}
                    onChange={(e) => setCouponCode(e.target.value.toUpperCase())}
                    placeholder="Enter coupon code"
                  />
                  <button type="button" className="btn btn-secondary" onClick={handleApplyCoupon}>
                    Apply
                  </button>
                </div>
                {discount !== null && (
                  <div className="discount-applied">
                    <p>Discount: -${discount.toFixed(2)}</p>
                    <p className="final-amount">Final Amount: ${finalAmount.toFixed(2)}</p>
                  </div>
                )}
              </div>

              <div className="order-summary">
                <h3>Order Summary</h3>
                <div className="summary-row">
                  <span>Subtotal:</span>
                  <span>${parseFloat(checkoutForm.originalAmount || 0).toFixed(2)}</span>
                </div>
                {discount !== null && (
                  <div className="summary-row discount">
                    <span>Discount:</span>
                    <span>-${discount.toFixed(2)}</span>
                  </div>
                )}
                <div className="summary-row total">
                  <span>Total:</span>
                  <span>${(finalAmount || parseFloat(checkoutForm.originalAmount || 0)).toFixed(2)}</span>
                </div>
              </div>

              <button type="submit" className="btn btn-primary btn-large">
                Place Order
              </button>
            </form>
          </div>
        )}

        {activeTab === 'coupons' && (
          <div className="coupons-section">
            <h2>Available Coupons</h2>
            {coupons.length === 0 ? (
              <p className="empty-state">No coupons available.</p>
            ) : (
              <div className="coupons-grid">
                {coupons.map((coupon) => (
                  <div key={coupon.id} className={`coupon-card ${!coupon.active ? 'inactive' : ''}`}>
                    <div className="coupon-code">{coupon.code}</div>
                    <div className="coupon-value">
                      {coupon.type === 'PERCENTAGE'
                        ? `${coupon.value}% OFF`
                        : `$${coupon.value} OFF`}
                    </div>
                    <div className="coupon-details">
                      <p><strong>Type:</strong> {coupon.type}</p>
                      {coupon.type === 'FIXED' && coupon.minOrderAmount > 0 && (
                        <p><strong>Min Order:</strong> ${coupon.minOrderAmount}</p>
                      )}
                      <p><strong>Uses:</strong> {coupon.usedCount}/{coupon.maxUses}</p>
                      <p><strong>Valid Until:</strong> {new Date(coupon.validUntil).toLocaleDateString()}</p>
                    </div>
                    <span className={`coupon-status ${coupon.active ? 'active' : 'inactive'}`}>
                      {coupon.active ? 'Active' : 'Inactive'}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  );
}

export default App;
