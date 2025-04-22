import React, { useState, useEffect } from 'react';
import { GoogleOAuthProvider, GoogleLogin } from '@react-oauth/google';
import jwt_decode from 'jwt-decode';

const backendUrl = process.env.REACT_APP_BACKEND_URL;
const googleClientId = process.env.REACT_APP_GOOGLE_CLIENT_ID;
const githubClientId = process.env.REACT_APP_GITHUB_CLIENT_ID;
const githubRedirectUri = 'http://localhost:3000/github/callback';

function App() {
  const [user, setUser] = useState(null);
  const [accessToken, setAccessToken] = useState(null);
  const [max, setMax] = useState('');
  const [primes, setPrimes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [authMode, setAuthMode] = useState('login');
  const [formData, setFormData] = useState({ name: '', email: '', password: '' });

  // GitHub OAuth callback
  useEffect(() => {
    const code = new URLSearchParams(window.location.search).get('code');
    if (window.location.pathname === '/github/callback' && code) {
      setLoading(true);
      fetch(`${backendUrl}/auth/github`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code }),
      })
        .then((res) => res.json())
        .then((data) => {
          setUser({ name: data.name || data.login, email: data.email });
          setAccessToken(data.token || null);
          window.history.replaceState({}, document.title, '/');
        })
        .catch((err) => console.error('GitHub login failed', err))
        .finally(() => setLoading(false));
    }
  }, []);

  // 👇 Google login success
  const handleGoogleSuccess = (credentialResponse) => {
    const decoded = jwt_decode(credentialResponse.credential);
    setUser({ name: decoded.name, email: decoded.email });
    setAccessToken(credentialResponse.credential);
  };

  const handleGitHubLogin = () => {
    const githubAuthUrl = `https://github.com/login/oauth/authorize?client_id=${githubClientId}&redirect_uri=${githubRedirectUri}&scope=read:user user:email`;
    window.location.href = githubAuthUrl;
  };  

  // 👇 Call secure Spring Boot endpoint
  const fetchPrimes = () => {
    if (!accessToken) return alert('Login required');
    fetch(`${backendUrl}/api/primes?max=${max}`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    })
      .then((res) => res.json())
      .then((data) => setPrimes(data))
      .catch((err) => console.error('Error fetching primes', err));
  };  

  const logout = () => {
    setUser(null);
    setAccessToken(null);
    setPrimes([]);
    window.history.replaceState({}, document.title, '/');
    //window.open("https://github.com/logout", "_blank");
  };

  const handleInputChange = (e) => {
    setFormData(prev => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleAuthSubmit = () => {
    const endpoint = authMode === 'signup' ? 'signup' : 'signin';
    fetch(`${backendUrl}/auth/${endpoint}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData),
    })
      .then(res => {
        if (!res.ok) throw new Error('Auth failed');
        return res.json();
      })
      .then(data => {
        setUser({ name: data.name, email: data.email });
        setAccessToken(data.token);
      })
      .catch(err => alert(err.message));
  };

  if (loading) {
    return <div style={{ padding: '20px' }}><h3>Logging you in with GitHub...</h3></div>;
  }

  return (
    
      <div style={{ padding: '20px', maxWidth: '500px', margin: 'auto' }}>
        {user ? (
          <>
            <h2>Welcome, {user.name}</h2>
            <button onClick={logout}>🔒Logout</button>
            <div style={{ marginTop: '20px' }}>
              <label>Enter Max Number: </label>
              <input
                type="number"
                value={max}
                onChange={(e) => setMax(e.target.value)}
              />
              <button onClick={fetchPrimes}>Get Primes</button>
            </div>
            {primes.length > 0 && (
              <div style={{ marginTop: '20px' }}>
                <strong>Primes:</strong> {primes.join(', ')}
              </div>
            )}
            <br/><br/>
          </>
        ) : (
          <>
            <h2>Please sign in</h2>
            <div style={{ marginBottom: '20px' }}>
              {authMode === 'signup' && (
                <input
                  name="name"
                  placeholder="Name"
                  value={formData.name}
                  onChange={handleInputChange}
                  style={{ display: 'block', marginBottom: '10px', width: '100%' }}
                />
              )}
              <input
                name="email"
                placeholder="Email"
                value={formData.email}
                onChange={handleInputChange}
                style={{ display: 'block', marginBottom: '10px', width: '100%' }}
              />
              <input
                name="password"
                type="password"
                placeholder="Password"
                value={formData.password}
                autoComplete={authMode === 'signup' ? 'new-password' : 'current-password'}
                onChange={handleInputChange}
                style={{ display: 'block', marginBottom: '10px', width: '100%' }}
              />
              <button 
                onClick={handleAuthSubmit}
                disabled={!formData.email || !formData.password}
                >
                {authMode === 'signup' ? 'Sign Up' : 'Sign In'}
              </button>
              <button onClick={() => setAuthMode(authMode === 'signup' ? 'login' : 'signup')} style={{ marginLeft: '10px' }}>
                Switch to {authMode === 'signup' ? 'Sign In' : 'Sign Up'}
              </button>
            </div>
            <br />
            <GoogleOAuthProvider clientId={googleClientId}>
              <GoogleLogin
                onSuccess={handleGoogleSuccess}
                onError={() => console.log('Google Login Failed')}
                ux_mode="popup"
                theme="outline"
                width="250"
              />
            </GoogleOAuthProvider>
            <br />
            <button
              onClick={handleGitHubLogin}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                border: '1px solid #ccc',
                borderRadius: '6px',
                padding: '8px 12px',
                background: '#fff',
                cursor: 'pointer',
                width: '250px',
                boxShadow: '0 1px 2px rgba(0,0,0,0.1)',
                fontSize: '14px',
                fontWeight: '500'
              }}
            >
              <img
                src="/github-logo.png"
                alt="GitHub"
                style={{ width: '20px', height: '20px' }}
              />
              <span>Sign in with GitHub</span>
            </button>
          </>
        )}
      </div>
  );
}

export default App;
