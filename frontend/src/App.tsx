import { useState, useEffect } from 'react';
import { Activity, ShieldAlert, Cpu, Heart, Database, MessageSquareCode, LogOut, Mail, Lock, FileText } from 'lucide-react';
import { GoogleLogin } from '@react-oauth/google';
import ChatInterface from './components/ChatInterface';
import DigitalTwin3D from './components/DigitalTwin3D';
import AssetDashboard from './components/AssetDashboard';
import GraphVisualizer from './components/GraphVisualizer';
import KnowledgeLossHeatmap from './components/KnowledgeLossHeatmap';
import LiveEventFeed from './components/LiveEventFeed';
import DocumentIntelligence from './components/DocumentIntelligence';
import { getApiUrl } from './api/config';

// Custom SVG Logo representing gear + AI networks inside a hexagon
const LogoIcon = () => (
  <svg className="h-7 w-7" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
      <linearGradient id="logoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stopColor="#10B981" /> {/* Emerald */}
        <stop offset="100%" stopColor="#0EA5E9" /> {/* Azure */}
      </linearGradient>
    </defs>
    {/* Hexagon Border */}
    <path d="M12 2L3.5 7v10L12 22l8.5-5V7L12 2z" stroke="url(#logoGrad)" strokeWidth="2" strokeLinejoin="round" />
    
    {/* Center Gear Ring */}
    <circle cx="12" cy="12" r="3" stroke="url(#logoGrad)" strokeWidth="1.5" strokeDasharray="3 2" />
    
    {/* Gear Teeth / Circuit nodes */}
    <path d="M12 5.5V7M12 17v1.5M5.5 12H7m10 0h1.5" stroke="url(#logoGrad)" strokeWidth="1.5" strokeLinecap="round" />
    <path d="M7.4 7.4l1.1 1.1m7 7l1.1 1.1M7.4 16.6l1.1-1.1m7-7l1.1-1.1" stroke="url(#logoGrad)" strokeWidth="1.5" strokeLinecap="round" />
    
    {/* Neural circuits points */}
    <circle cx="12" cy="5.5" r="1" fill="#10B981" />
    <circle cx="12" cy="18.5" r="1" fill="#0EA5E9" />
    <circle cx="5.5" cy="12" r="1" fill="#10B981" />
    <circle cx="18.5" cy="12" r="1" fill="#0EA5E9" />
    <circle cx="7.4" cy="7.4" r="1" fill="#10B981" />
    <circle cx="16.6" cy="16.6" r="1" fill="#0EA5E9" />
    <circle cx="7.4" cy="16.6" r="1" fill="#10B981" />
    <circle cx="16.6" cy="7.4" r="1" fill="#0EA5E9" />
    <circle cx="12" cy="12" r="1.5" fill="url(#logoGrad)" />
  </svg>
);

function isTokenExpired(tokenStr: string | null): boolean {
  if (!tokenStr) return true;
  try {
    const base64Url = tokenStr.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      window
        .atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    const payload = JSON.parse(jsonPayload);
    if (!payload.exp) return false;
    return payload.exp < Date.now() / 1000;
  } catch (e) {
    return true;
  }
}

export default function App() {
  const [token, setToken] = useState<string | null>(localStorage.getItem('tacitiq_token'));
  const [user, setUser] = useState<any>(JSON.parse(localStorage.getItem('tacitiq_user') || 'null'));
  const [emailInput, setEmailInput] = useState('');
  const [passwordInput, setPasswordInput] = useState('');
  const [rememberMe, setRememberMe] = useState(true);
  const [loginError, setLoginError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);

  const [activeTab, setActiveTab] = useState<'chat' | 'twin' | 'dashboard' | 'graph' | 'heatmap' | 'events' | 'documents'>('chat');
  const [stats, setStats] = useState<any>({
    totalAssets: 4,
    averageHealthScore: "0.88",
    activeIncidentsCount: 2,
    complianceAuditStatus: "ELEVATED_RISK",
    expertSubmissionsCount: 0,
    criticalAssetsCount: 3,
    totalDocuments: 0,
    processedDocuments: 0,
    extractedEntities: 0,
    graphLinksCreated: 0,
    lastUploadedDocument: "None"
  });

  // Load last active tab if authenticated
  useEffect(() => {
    if (token) {
      const savedTab = localStorage.getItem('tacitiq_active_tab');
      if (savedTab && ['chat', 'twin', 'dashboard', 'graph', 'heatmap', 'events', 'documents'].includes(savedTab)) {
        setActiveTab(savedTab as any);
      }
    }
  }, [token]);

  // Hoisted logout function to clean up credentials across state, cookies, and local storage
  async function handleLogout() {
    try {
      await fetch(getApiUrl('/api/auth/logout'), { method: 'POST' });
    } catch (err) {
      // ignore
    }
    localStorage.removeItem('tacitiq_token');
    localStorage.removeItem('tacitiq_user');
    localStorage.removeItem('tacitiq_active_tab');
    setToken(null);
    setUser(null);
    window.location.replace('/');
  }

  // Session persistence & route protection check: validate JWT expiry on startup
  useEffect(() => {
    const checkSessionAndRefresh = async () => {
      const currentToken = localStorage.getItem('tacitiq_token');
      if (currentToken) {
        if (isTokenExpired(currentToken)) {
          try {
            const res = await fetch(getApiUrl('/api/auth/refresh'), { method: 'POST' });
            if (res.ok) {
              const data = await res.json();
              if (data && data.accessToken) {
                localStorage.setItem('tacitiq_token', data.accessToken);
                localStorage.setItem('tacitiq_user', JSON.stringify(data.user));
                setToken(data.accessToken);
                setUser(data.user);
              } else {
                handleLogout();
              }
            } else {
              handleLogout();
            }
          } catch (err) {
            handleLogout();
          }
        }
      } else {
        try {
          const res = await fetch(getApiUrl('/api/auth/refresh'), { method: 'POST' });
          if (res.ok) {
            const data = await res.json();
            if (data && data.accessToken) {
              localStorage.setItem('tacitiq_token', data.accessToken);
              localStorage.setItem('tacitiq_user', JSON.stringify(data.user));
              setToken(data.accessToken);
              setUser(data.user);
            }
          }
        } catch (err) {
          // ignore
        }
      }
    };
    checkSessionAndRefresh();
  }, []);

  // Centralized HTTP fetch interceptor to handle token refresh / auto-logout globally
  useEffect(() => {
    const originalFetch = window.fetch;
    window.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
      let response = await originalFetch(input, init);
      if (response.status === 401 || response.status === 403) {
        const url = typeof input === 'string' ? input : (input instanceof Request ? input.url : input.toString());
        if (!url.includes('/api/auth/refresh') && !url.includes('/api/auth/logout') && !url.includes('/api/auth/login')) {
          try {
            const refreshRes = await originalFetch(getApiUrl('/api/auth/refresh'), { method: 'POST' });
            if (refreshRes.ok) {
              const data = await refreshRes.json();
              if (data && data.accessToken) {
                localStorage.setItem('tacitiq_token', data.accessToken);
                localStorage.setItem('tacitiq_user', JSON.stringify(data.user));
                setToken(data.accessToken);
                setUser(data.user);

                const newInit = { ...init };
                const newHeaders = new Headers(newInit.headers || {});
                newHeaders.set('Authorization', `Bearer ${data.accessToken}`);
                newInit.headers = newHeaders;
                response = await originalFetch(input, newInit);
              } else {
                handleLogout();
              }
            } else {
              handleLogout();
            }
          } catch (err) {
            handleLogout();
          }
        }
      }
      return response;
    };
    return () => {
      window.fetch = originalFetch;
    };
  }, []);

  // Back-forward cache (bfcache) check to prevent unauthorized view on back button
  useEffect(() => {
    const handlePageShow = () => {
      const currentToken = localStorage.getItem('tacitiq_token');
      if (!currentToken) {
        setToken(null);
        setUser(null);
      }
    };
    window.addEventListener('pageshow', handlePageShow);
    return () => window.removeEventListener('pageshow', handlePageShow);
  }, []);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError(null);
    setIsSubmitting(true);
    try {
      const res = await fetch(getApiUrl('/api/auth/login'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: emailInput, password: passwordInput })
      });
      if (!res.ok) {
        if (res.status === 401 || res.status === 403) {
          throw new Error("Invalid email or security key. OAuth-only accounts must login using Google.");
        } else {
          throw new Error(`Server error (${res.status}): Failed to connect to authentication service.`);
        }
      }
      const data = await res.json();
      if (data && data.accessToken) {
        localStorage.setItem('tacitiq_token', data.accessToken);
        localStorage.setItem('tacitiq_user', JSON.stringify(data.user));
        setToken(data.accessToken);
        setUser(data.user);
      } else {
        throw new Error("Invalid response format.");
      }
    } catch (err: any) {
      setLoginError(err.message || "Failed to authenticate.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleGoogleSuccess = async (idToken: string) => {
    setLoginError(null);
    setIsSubmitting(true);
    try {
      const res = await fetch(getApiUrl('/api/auth/google'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idToken })
      });
      if (!res.ok) {
        throw new Error("Google authentication failed at verification gateway.");
      }
      const data = await res.json();
      if (data && data.accessToken) {
        localStorage.setItem('tacitiq_token', data.accessToken);
        localStorage.setItem('tacitiq_user', JSON.stringify(data.user));
        setToken(data.accessToken);
        setUser(data.user);
      } else {
        throw new Error("Invalid response format from authorization backend.");
      }
    } catch (err: any) {
      setLoginError(err.message || "Failed to authenticate with Google.");
    } finally {
      setIsSubmitting(false);
    }
  };



  const handleTabChange = (tab: any) => {
    setActiveTab(tab);
    localStorage.setItem('tacitiq_active_tab', tab);
  };

  // Fetch aggregate status stats from backend
  useEffect(() => {
    if (!token) return;

    fetch(getApiUrl('/api/dashboard/summary'), {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
      .then(async res => {
        if (res.status === 401 || res.status === 403) {
          // Attempt silent refresh
          try {
            const refreshRes = await fetch(getApiUrl('/api/auth/refresh'), { method: 'POST' });
            if (refreshRes.ok) {
              const refreshData = await refreshRes.json();
              if (refreshData && refreshData.accessToken) {
                localStorage.setItem('tacitiq_token', refreshData.accessToken);
                localStorage.setItem('tacitiq_user', JSON.stringify(refreshData.user));
                setToken(refreshData.accessToken);
                setUser(refreshData.user);
                return;
              }
            }
          } catch (err) {
            // refresh failed
          }
          handleLogout();
          throw new Error("Session expired.");
        }
        return res.json();
      })
      .then(data => {
        if (data && !data.error) {
          setStats(data);
        }
      })
      .catch(() => logMock("System stats loaded from offline local cache."));
  }, [token]);

  const logMock = (msg: string) => {
    console.log(`[TacitIQ Cache] ${msg}`);
  };

  // Render Login View if not authenticated (Route Guard / Conditional Page Render)
  if (!token) {
    const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
    const isGoogleConfigured = googleClientId && googleClientId !== "mock_client_id" && googleClientId !== "YOUR_GOOGLE_CLIENT_ID";

    return (
      <div className="min-h-screen flex bg-slate-50 text-slate-800 font-sans relative overflow-hidden">
        {/* Left Panel: 55% Width (Hidden on Mobile/Tablet below lg) */}
        <div className="hidden lg:flex lg:w-[55%] bg-slate-950 text-white p-16 flex-col justify-between relative overflow-hidden border-r border-slate-800/60 z-20">
          {/* Blueprint Engineering Grid Pattern */}
          <div className="absolute inset-0 opacity-[0.035] bg-[linear-gradient(rgba(255,255,255,1)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,1)_1px,transparent_1px)] bg-[size:36px_36px] pointer-events-none" />
          
          {/* Subtle Glowing Mesh Blobs */}
          <div className="absolute top-[-10%] right-[-10%] w-[500px] h-[500px] bg-brandAzure/10 rounded-full blur-[120px] pointer-events-none" />
          <div className="absolute bottom-[-10%] left-[-10%] w-[500px] h-[500px] bg-brandEmerald/5 rounded-full blur-[120px] pointer-events-none" />

          {/* Animated Asset Topology Network (Vector Industrial Illustration) */}
          <div className="absolute inset-0 opacity-[0.18] pointer-events-none flex items-center justify-center">
            <svg className="w-full h-full p-8" viewBox="0 0 800 600" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="linkGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#10B981" stopOpacity="0.4" />
                  <stop offset="100%" stopColor="#0EA5E9" stopOpacity="0.4" />
                </linearGradient>
                <radialGradient id="nodeGlow" cx="50%" cy="50%" r="50%">
                  <stop offset="0%" stopColor="#10B981" stopOpacity="0.25" />
                  <stop offset="100%" stopColor="#10B981" stopOpacity="0" />
                </radialGradient>
              </defs>
              <path d="M 150,150 L 300,100 L 450,150 L 600,100 L 700,250 M 300,100 L 350,300 L 450,150 M 150,150 L 250,400 L 500,450 L 700,250 M 350,300 L 500,450 L 550,280 L 600,100" stroke="url(#linkGrad)" strokeWidth="1.5" strokeDasharray="5, 5">
                <animate attributeName="stroke-dashoffset" values="100;0" dur="20s" repeatCount="indefinite" />
              </path>
              
              <circle r="4" fill="#0EA5E9">
                <animateMotion dur="7s" repeatCount="indefinite" path="M 150,150 L 300,100 L 450,150 L 600,100 L 700,250" />
              </circle>
              <circle r="4" fill="#10B981">
                <animateMotion dur="9s" repeatCount="indefinite" path="M 150,150 L 250,400 L 500,450 L 700,250" />
              </circle>

              <g className="animate-pulse">
                <circle cx="150" cy="150" r="16" fill="url(#nodeGlow)" />
                <circle cx="150" cy="150" r="6" fill="#10B981" stroke="#fff" strokeWidth="2" />
                <text x="150" y="130" fill="#64748B" fontSize="10" fontWeight="bold" textAnchor="middle">FEED_PUMP_A</text>
              </g>
              <g className="animate-pulse" style={{ animationDelay: '1s' }}>
                <circle cx="300" cy="100" r="16" fill="url(#nodeGlow)" />
                <circle cx="300" cy="100" r="6" fill="#10B981" stroke="#fff" strokeWidth="2" />
                <text x="300" y="80" fill="#64748B" fontSize="10" fontWeight="bold" textAnchor="middle">VALVE_012</text>
              </g>
              <g className="animate-pulse" style={{ animationDelay: '0.5s' }}>
                <circle cx="450" cy="150" r="16" fill="url(#nodeGlow)" />
                <circle cx="450" cy="150" r="6" fill="#0EA5E9" stroke="#fff" strokeWidth="2" />
                <text x="450" y="130" fill="#64748B" fontSize="10" fontWeight="bold" textAnchor="middle">COMPRESSOR_B</text>
              </g>
              <g className="animate-pulse" style={{ animationDelay: '1.5s' }}>
                <circle cx="600" cy="100" r="16" fill="url(#nodeGlow)" />
                <circle cx="600" cy="100" r="6" fill="#10B981" stroke="#fff" strokeWidth="2" />
                <text x="600" y="80" fill="#64748B" fontSize="10" fontWeight="bold" textAnchor="middle">TEMP_SENS_04</text>
              </g>
              <g className="animate-pulse" style={{ animationDelay: '2s' }}>
                <circle cx="700" cy="250" r="16" fill="url(#nodeGlow)" />
                <circle cx="700" cy="250" r="6" fill="#0EA5E9" stroke="#fff" strokeWidth="2" />
                <text x="700" y="230" fill="#64748B" fontSize="10" fontWeight="bold" textAnchor="middle">DIST_UNIT_1</text>
              </g>
              <g className="animate-pulse" style={{ animationDelay: '0.7s' }}>
                <circle cx="350" cy="300" r="16" fill="url(#nodeGlow)" />
                <circle cx="350" cy="300" r="6" fill="#10B981" stroke="#fff" strokeWidth="2" />
                <text x="350" y="280" fill="#64748B" fontSize="10" fontWeight="bold" textAnchor="middle">FLOW_SENS_2</text>
              </g>
              <g className="animate-pulse" style={{ animationDelay: '1.2s' }}>
                <circle cx="250" cy="400" r="16" fill="url(#nodeGlow)" />
                <circle cx="250" cy="400" r="6" fill="#0EA5E9" stroke="#fff" strokeWidth="2" />
                <text x="250" y="380" fill="#64748B" fontSize="10" fontWeight="bold" textAnchor="middle">HEATER_03</text>
              </g>
              <g className="animate-pulse" style={{ animationDelay: '1.8s' }}>
                <circle cx="500" cy="450" r="16" fill="url(#nodeGlow)" />
                <circle cx="500" cy="450" r="6" fill="#10B981" stroke="#fff" strokeWidth="2" />
                <text x="500" y="430" fill="#64748B" fontSize="10" fontWeight="bold" textAnchor="middle">OUTLET_VALVE</text>
              </g>
              <g className="animate-pulse" style={{ animationDelay: '2.5s' }}>
                <circle cx="550" cy="280" r="16" fill="url(#nodeGlow)" />
                <circle cx="550" cy="280" r="6" fill="#0EA5E9" stroke="#fff" strokeWidth="2" />
                <text x="550" y="260" fill="#64748B" fontSize="10" fontWeight="bold" textAnchor="middle">PRESS_REG_12</text>
              </g>
            </svg>
          </div>

          {/* Top Content: Branding */}
          <div className="flex items-center gap-3.5 z-10 animate-in fade-in slide-in-from-top-4 duration-500 ease-out">
            <div className="h-11 w-11 rounded-xl bg-gradient-to-br from-brandEmerald to-brandAzure flex items-center justify-center shadow-md">
              <LogoIcon />
            </div>
            <div>
              <span className="font-extrabold text-lg text-white tracking-tight leading-none block">TacitIQ</span>
              <span className="block text-[10px] text-slate-400 font-semibold tracking-wider mt-1">Enterprise Industrial Intelligence Platform</span>
            </div>
          </div>

          {/* Center Content: Headline & Desc */}
          <div className="my-auto max-w-lg z-10 animate-in fade-in slide-in-from-bottom-6 duration-500 ease-out">
            <h1 className="text-3xl xl:text-4xl font-extrabold text-white tracking-tight leading-tight mb-4">
              Transform Enterprise Knowledge into Operational Intelligence
            </h1>
            <p className="text-slate-400 text-sm leading-relaxed mb-8 font-medium">
              TacitIQ unifies operational knowledge, digital twins, AI copilots, and industrial assets into one intelligent platform.
            </p>
            
            {/* Feature List */}
            <ul className="space-y-4">
              {[
                "AI Operations Assistant",
                "Digital Twin Intelligence",
                "Enterprise Knowledge Graph",
                "Predictive Maintenance",
                "Workforce Knowledge Capture"
              ].map((feature, idx) => (
                <li key={idx} className="flex items-center gap-3.5 text-slate-200 text-sm font-semibold">
                  <div className="h-5 w-5 rounded-full bg-brandEmerald/10 border border-brandEmerald/30 flex items-center justify-center text-brandEmerald">
                    <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="3">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                    </svg>
                  </div>
                  <span>{feature}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Bottom Content: Footer info */}
          <div className="flex items-center justify-between text-[10px] text-slate-500 border-t border-slate-800/80 pt-6 z-10">
            <span className="font-semibold tracking-wider">Version 2.4.0 (Enterprise)</span>
            <div className="flex items-center gap-4 font-semibold tracking-wider">
              <a href="#docs" className="hover:text-slate-300 transition-colors" onClick={e => e.preventDefault()}>Documentation</a>
              <a href="#support" className="hover:text-slate-300 transition-colors" onClick={e => e.preventDefault()}>Support</a>
              <a href="#privacy" className="hover:text-slate-300 transition-colors" onClick={e => e.preventDefault()}>Privacy</a>
            </div>
          </div>
        </div>

        {/* Right Panel: 45% Width (Vertically Centered Auth Form) */}
        <div className="w-full lg:w-[45%] flex flex-col justify-center items-center p-8 sm:p-16 bg-white relative z-10">
          {/* Subtle Blueprint grid overlay for auth side */}
          <div className="absolute inset-0 opacity-[0.012] bg-[linear-gradient(rgba(15,23,42,1)_1px,transparent_1px),linear-gradient(90deg,rgba(15,23,42,1)_1px,transparent_1px)] bg-[size:30px_30px] pointer-events-none" />

          <div className="max-w-[420px] w-full z-10 animate-in fade-in slide-in-from-bottom-8 duration-600 ease-out">
            {/* Branding Header visible ONLY on Mobile/Tablet */}
            <div className="lg:hidden flex items-center gap-3.5 mb-10">
              <div className="h-10 w-10 rounded-xl bg-gradient-to-br from-brandEmerald to-brandAzure flex items-center justify-center shadow-sm">
                <LogoIcon />
              </div>
              <div>
                <span className="font-extrabold text-base text-slate-900 tracking-tight block">TacitIQ</span>
                <span className="block text-[9px] text-slate-400 font-bold tracking-wider mt-0.5">Enterprise Industrial Intelligence Platform</span>
              </div>
            </div>

            {/* Headline and tagline */}
            <div className="mb-8">
              <h2 className="text-2xl font-extrabold text-slate-900 tracking-tight">Sign in</h2>
              <p className="text-xs text-slate-500 mt-1.5 font-semibold">Continue to your enterprise workspace</p>
            </div>

            {loginError && (
              <div className="p-3.5 mb-5 bg-brandRed/10 border border-brandRed/20 rounded-xl text-xs font-bold text-brandRed">
                {loginError}
              </div>
            )}

            {/* Primary Login: Google OAuth */}
            <div className="mb-5">
              <div className="w-full focus-within:ring-2 focus-within:ring-brandAzure focus-within:ring-offset-2 rounded-xl overflow-hidden flex justify-center">
                {isGoogleConfigured ? (
                  <div className="w-full flex justify-center [&>div]:w-full transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[0_4px_12px_rgba(15,23,42,0.04)] active:scale-[0.99] active:translate-y-0 rounded-xl overflow-hidden border border-slate-200 bg-white">
                    <GoogleLogin
                      onSuccess={async (credentialResponse) => {
                        if (credentialResponse.credential) {
                          await handleGoogleSuccess(credentialResponse.credential);
                        }
                      }}
                      onError={() => {
                        setLoginError("Google Sign-In failed.");
                      }}
                      text="continue_with"
                      theme="outline"
                      size="large"
                      shape="rectangular"
                    />
                  </div>
                ) : (
                  <div className="w-full p-4 bg-slate-50 border border-slate-200/50 rounded-xl text-center">
                    <p className="text-xs font-bold text-slate-800">
                      Enterprise Sign-In Unavailable
                    </p>
                    <p className="text-[10px] text-slate-500 mt-1.5 font-semibold leading-relaxed">
                      Google authentication is currently unavailable.<br />
                      Please contact your administrator or try again later.
                    </p>
                  </div>
                )}
              </div>
            </div>

            {/* Divider */}
            <div className="flex items-center my-6">
              <div className="flex-grow border-t border-slate-200/50"></div>
              <span className="px-4 text-[9px] text-slate-400 font-bold uppercase tracking-[0.2em]">or</span>
              <div className="flex-grow border-t border-slate-200/50"></div>
            </div>

            {/* Traditional Credentials Form */}
            <form onSubmit={handleLogin} className="space-y-5">
              <div>
                <label className="block text-[10px] uppercase font-bold tracking-[0.15em] text-slate-400 mb-2">
                  Email Address
                </label>
                <div className="relative flex items-center group">
                  <Mail className="absolute left-4 h-4.5 w-4.5 text-slate-400 group-focus-within:text-brandEmerald transition-colors duration-200" />
                  <input
                    type="email"
                    required
                    value={emailInput}
                    onChange={e => setEmailInput(e.target.value)}
                    placeholder="name@company.com"
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl py-3 pl-12 pr-4 focus:outline-none focus:border-brandEmerald focus:ring-4 focus:ring-brandEmerald/10 focus:bg-white text-xs font-semibold transition-all duration-200"
                    aria-label="Email Address"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[10px] uppercase font-bold tracking-[0.15em] text-slate-400 mb-2">
                  Password
                </label>
                <div className="relative flex items-center group">
                  <Lock className="absolute left-4 h-4.5 w-4.5 text-slate-400 group-focus-within:text-brandEmerald transition-colors duration-200" />
                  <input
                    type="password"
                    required
                    value={passwordInput}
                    onChange={e => setPasswordInput(e.target.value)}
                    placeholder="••••••••"
                    className="w-full bg-slate-50 border border-slate-200 rounded-xl py-3 pl-12 pr-4 focus:outline-none focus:border-brandEmerald focus:ring-4 focus:ring-brandEmerald/10 focus:bg-white text-xs font-semibold transition-all duration-200"
                    aria-label="Password"
                  />
                </div>
              </div>

              <div className="flex items-center justify-between text-xs pt-1">
                <label className="flex items-center gap-2 text-slate-500 font-semibold cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={rememberMe}
                    onChange={e => setRememberMe(e.target.checked)}
                    className="rounded border-slate-200 text-brandEmerald focus:ring-brandEmerald accent-brandEmerald w-4 h-4 cursor-pointer transition-colors duration-150"
                  />
                  <span>Remember Me</span>
                </label>
                <a 
                  href="#forgot" 
                  className="text-brandEmerald hover:text-brandEmerald/80 font-bold transition-colors duration-150 relative py-0.5 after:absolute after:bottom-0 after:left-0 after:w-full after:h-[1px] after:bg-brandEmerald/60 after:scale-x-0 hover:after:scale-x-100 after:transition-transform after:duration-250 after:origin-left" 
                  onClick={e => { e.preventDefault(); alert("Please contact your system administrator to manage email password keys."); }}
                >
                  Forgot Password?
                </a>
              </div>

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full py-3.5 bg-gradient-to-r from-brandEmerald to-brandAzure text-white font-bold rounded-xl hover:shadow-[0_4px_12px_rgba(16,185,129,0.12)] transition-all duration-200 flex items-center justify-center gap-2 text-sm mt-6 cursor-pointer focus:outline-none focus:ring-2 focus:ring-brandAzure focus:ring-offset-2 disabled:opacity-50"
                aria-label="Sign In with email and password"
              >
                {isSubmitting ? "Connecting..." : "Sign In"}
              </button>
            </form>
          </div>
        </div>
      </div>
    );
  }

  // Active view header title resolver
  const getHeaderTitle = () => {
    switch (activeTab) {
      case 'chat': return 'Operations Assistant';
      case 'twin': return '3D Digital Twin Simulation';
      case 'dashboard': return 'Asset Telemetry & Analytics';
      case 'graph': return 'Knowledge Graph RCA Traversal';
      case 'heatmap': return 'Workforce Planning & Capture';
      case 'events': return 'Live Event Feed Console';
      case 'documents': return 'Document Intelligence & Extraction';
      default: return 'Operations Dashboard';
    }
  };

  return (
    <div className="flex h-screen text-slate-800 overflow-hidden font-sans relative bg-brandBg">
      {/* Dynamic background mesh blobs */}
      <div className="absolute top-0 left-0 w-[400px] h-[400px] bg-brandEmerald/5 rounded-full blur-[100px] pointer-events-none" />
      <div className="absolute bottom-0 right-0 w-[500px] h-[500px] bg-brandAzure/10 rounded-full blur-[120px] pointer-events-none" />

      {/* Enterprise Sidebar */}
      <aside className="w-64 bg-white/70 backdrop-blur-md border-r border-slate-200/50 flex flex-col justify-between z-20">
        <div>
          <div className="p-6 flex items-center gap-3 border-b border-slate-200/50">
            <div className="h-9 w-9 rounded-xl bg-white border border-slate-100 flex items-center justify-center shadow-sm">
              <LogoIcon />
            </div>
            <div>
              <h1 className="text-lg font-bold tracking-tight bg-gradient-to-r from-brandEmerald to-brandAzure bg-clip-text text-transparent">
                TacitIQ
              </h1>
              <p className="text-[9px] text-slate-400 font-bold uppercase tracking-wider">
                Enterprise Industrial Intelligence Platform
              </p>
            </div>
          </div>

          <nav className="p-4 space-y-1">
            {[
              { id: 'chat', label: 'Operations Assistant', icon: MessageSquareCode },
              { id: 'twin', label: 'Digital Twin 3D', icon: Cpu },
              { id: 'dashboard', label: 'Asset Dashboard', icon: Activity },
              { id: 'graph', label: 'Knowledge Graph', icon: Database },
              { id: 'documents', label: 'Document Intelligence', icon: FileText },
              { id: 'heatmap', label: 'Workforce Planning', icon: ShieldAlert },
              { id: 'events', label: 'Live Event Console', icon: Heart }
            ].map(item => {
              const Icon = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  key={item.id}
                  onClick={() => handleTabChange(item.id as any)}
                  className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold transition-all duration-200 cursor-pointer ${
                    isActive
                      ? 'bg-gradient-to-r from-brandEmerald/10 to-brandAzure/10 text-brandEmerald border-l-4 border-brandEmerald shadow-sm'
                      : 'text-slate-500 hover:bg-slate-100/50 hover:text-slate-800'
                  }`}
                  aria-label={item.label}
                >
                  <Icon className={`h-4.5 w-4.5 ${isActive ? 'text-brandEmerald' : 'text-slate-400'}`} />
                  {item.label}
                </button>
              );
            })}
          </nav>
        </div>

        {/* System Diagnostics Badge */}
        <div className="p-4 border-t border-slate-200/50 bg-white/40">
          <div className="flex items-center gap-2">
            <span className="h-2 w-2 rounded-full bg-brandEmerald animate-pulse"></span>
            <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">
              System Operations Live
            </span>
          </div>
        </div>
      </aside>

      {/* Main Workspace Frame */}
      <main className="flex-1 flex flex-col overflow-hidden z-10">
        {/* Top Header Panel */}
        <header className="h-16 bg-white/70 backdrop-blur-md border-b border-slate-200/50 px-8 flex items-center justify-between relative z-30">
          <h2 className="text-sm font-bold text-slate-800 flex items-center gap-2">
            {getHeaderTitle()}
          </h2>
          <div className="flex items-center gap-4">
            {/* Health Index Badge (Compact) */}
            {stats.averageHealthScore && (
              <div className="hidden sm:flex items-center gap-1.5 px-3 py-1 bg-emerald-50 rounded-full border border-emerald-100/80">
                <span className="text-[10px] text-slate-500 uppercase tracking-wider font-bold">Health Index:</span>
                <span className="text-xs font-extrabold text-emerald-600">{stats.averageHealthScore}</span>
              </div>
            )}

            {/* Notifications Bell */}
            <button 
              className="p-2 text-slate-400 hover:text-slate-600 rounded-xl hover:bg-slate-100/50 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-brandAzure relative cursor-pointer"
              aria-label="Notifications"
              onClick={() => alert("No new notifications.")}
            >
              <span className="absolute top-1.5 right-1.5 h-1.5 w-1.5 bg-brandRed rounded-full"></span>
              <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
                <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
              </svg>
            </button>

            {/* Profile Dropdown Menu */}
            <div className="relative">
              <button
                onClick={() => setIsProfileMenuOpen(!isProfileMenuOpen)}
                className="flex items-center gap-1.5 p-1 rounded-full hover:bg-slate-50 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-brandAzure cursor-pointer"
                aria-label="User profile menu"
                aria-expanded={isProfileMenuOpen}
              >
                {user?.profilePicture ? (
                  <img
                    src={user.profilePicture}
                    alt={user.displayName || user.email}
                    className="w-9 h-9 rounded-full object-cover border border-slate-200"
                    onError={(e) => {
                      (e.target as HTMLImageElement).src = `https://api.dicebear.com/7.x/initials/svg?seed=${user.displayName || user.email}`;
                    }}
                  />
                ) : (
                  <div className="w-9 h-9 rounded-full bg-gradient-to-br from-brandEmerald to-brandAzure text-white flex items-center justify-center font-extrabold text-sm uppercase">
                    {(user?.displayName || user?.email || "?").charAt(0)}
                  </div>
                )}
                <svg className="w-4 h-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="6 9 12 15 18 9"></polyline>
                </svg>
              </button>

              {isProfileMenuOpen && (
                <>
                  <div
                    className="fixed inset-0 z-40"
                    onClick={() => setIsProfileMenuOpen(false)}
                  />
                  <div className="absolute right-0 mt-2.5 w-56 bg-white rounded-2xl border border-slate-100 shadow-premium p-3.5 z-50 animate-in fade-in slide-in-from-top-2 duration-150 text-slate-700">
                    <div className="px-2.5 pb-2.5 border-b border-slate-100 mb-2">
                      <div className="flex items-center gap-1.5 text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1.5">
                        <span>👤</span>
                        <span>Profile</span>
                      </div>
                      <div className="overflow-hidden">
                        <h4 className="text-xs font-black text-slate-900 truncate">
                          {user?.displayName || 'Engineer'}
                        </h4>
                        <p className="text-[10.5px] text-slate-400 truncate mt-0.5 font-medium">
                          {user?.email}
                        </p>
                        <span className="inline-block text-[9px] font-bold text-slate-500 uppercase tracking-wide bg-slate-50 border border-slate-100 px-1.5 py-0.5 rounded-md mt-1.5">
                          {user?.role ? user.role.replace('_', ' ') : 'USER'}
                        </span>
                      </div>
                    </div>

                    <div className="space-y-0.5">
                      <button
                        onClick={() => {
                          setIsProfileMenuOpen(false);
                          alert("Account settings are managed by your identity administrator.");
                        }}
                        className="w-full flex items-center gap-2 px-2.5 py-2 hover:bg-slate-50 text-slate-600 hover:text-slate-950 font-bold rounded-xl text-[11px] text-left transition-all duration-150 cursor-pointer"
                      >
                        ⚙️ Settings
                      </button>
                      <button
                        onClick={() => {
                          setIsProfileMenuOpen(false);
                          alert("Access the enterprise support portal at support.tacitiq.com.");
                        }}
                        className="w-full flex items-center gap-2 px-2.5 py-2 hover:bg-slate-50 text-slate-600 hover:text-slate-950 font-bold rounded-xl text-[11px] text-left transition-all duration-150 cursor-pointer"
                      >
                        ❓ Help
                      </button>
                    </div>

                    <div className="border-t border-slate-100 mt-2.5 pt-2">
                      <button
                        onClick={() => {
                          setIsProfileMenuOpen(false);
                          handleLogout();
                        }}
                        className="w-full flex items-center justify-center gap-2 py-2 px-3 bg-red-50 hover:bg-red-100 text-red-600 font-bold rounded-xl transition-all duration-150 text-[11px] border border-red-100/50 cursor-pointer"
                        aria-label="Logout"
                      >
                        <LogOut className="w-3.5 h-3.5" />
                        <span>Logout</span>
                      </button>
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>
        </header>

        {/* View Switcher Panel */}
        <div className="flex-1 overflow-hidden relative">
          {activeTab === 'chat' && <ChatInterface setActiveTab={setActiveTab} />}
          {activeTab === 'twin' && <DigitalTwin3D />}
          {activeTab === 'dashboard' && <AssetDashboard />}
          {activeTab === 'graph' && <GraphVisualizer />}
          {activeTab === 'documents' && <DocumentIntelligence setActiveTab={setActiveTab} />}
          {activeTab === 'heatmap' && <KnowledgeLossHeatmap />}
          {activeTab === 'events' && <LiveEventFeed />}
        </div>
      </main>
    </div>
  );
}


