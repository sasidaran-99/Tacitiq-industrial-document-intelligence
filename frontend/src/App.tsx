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
      await fetch('/api/auth/logout', { method: 'POST' });
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
            const res = await fetch('/api/auth/refresh', { method: 'POST' });
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
          const res = await fetch('/api/auth/refresh', { method: 'POST' });
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
            const refreshRes = await originalFetch('/api/auth/refresh', { method: 'POST' });
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
      const res = await fetch('/api/auth/login', {
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
      const res = await fetch('/api/auth/google', {
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

    fetch('/api/dashboard/summary', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
      .then(async res => {
        if (res.status === 401 || res.status === 403) {
          // Attempt silent refresh
          try {
            const refreshRes = await fetch('/api/auth/refresh', { method: 'POST' });
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
      <div className="flex items-center justify-center min-h-screen bg-slate-50 text-slate-800 overflow-hidden font-sans relative">
        {/* Glowing visual blobs for SaaS feel */}
        <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-brandAzure/10 rounded-full blur-[100px] pointer-events-none" />
        <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-brandEmerald/5 rounded-full blur-[100px] pointer-events-none" />
        
        <div className="w-full max-w-[490px] p-10 bg-white border border-slate-100/80 shadow-premium rounded-3xl relative z-10 transition-all duration-300">
          <div className="text-center mb-6">
            <div className="h-12 w-12 rounded-2xl bg-white border border-slate-100 flex items-center justify-center mx-auto mb-3 shadow-sm">
              <LogoIcon />
            </div>
            <h1 className="text-lg font-black text-slate-900 tracking-tight">
              TacitIQ Portal
            </h1>
            <p className="text-[10px] text-slate-400 mt-0.5 font-bold uppercase tracking-wider">
              Enterprise Industrial Intelligence Platform
            </p>
          </div>

          <div className="mb-5 text-center">
            <h2 className="text-base font-bold text-slate-800">Welcome Back</h2>
            <p className="text-xs text-slate-400 mt-0.5 font-medium">Sign in to continue to your dashboard</p>
          </div>

          {loginError && (
            <div className="p-3 mb-4 bg-brandRed/10 border border-brandRed/20 rounded-xl text-xs font-bold text-brandRed">
              {loginError}
            </div>
          )}

          {/* Primary Login: Google OAuth */}
          <div className="mb-4">
            <div className="w-full focus-within:ring-2 focus-within:ring-brandAzure focus-within:ring-offset-2 rounded-xl overflow-hidden flex justify-center">
              {isGoogleConfigured ? (
                <div className="w-full flex justify-center [&>div]:w-full">
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
                <div className="w-full p-4 bg-slate-50 border border-slate-200/50 rounded-2xl text-center">
                  <p className="text-xs font-bold text-slate-800">
                    Enterprise Sign-In Unavailable
                  </p>
                  <p className="text-[10.5px] text-slate-500 mt-1.5 font-semibold leading-relaxed">
                    Google authentication is currently unavailable.<br />
                    Please contact your administrator or try again later.
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Divider */}
          <div className="flex items-center my-5">
            <div className="flex-grow border-t border-slate-100"></div>
            <span className="px-3 text-[10px] text-slate-400 uppercase font-bold tracking-widest">OR</span>
            <div className="flex-grow border-t border-slate-100"></div>
          </div>

          {/* Traditional Password Form */}
          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <label className="block text-[10px] uppercase font-bold tracking-widest text-slate-400 mb-1.5">
                Email Address
              </label>
              <div className="relative flex items-center">
                <Mail className="absolute left-4 h-4 w-4 text-slate-400" />
                <input
                  type="email"
                  required
                  value={emailInput}
                  onChange={e => setEmailInput(e.target.value)}
                  placeholder="name@company.com"
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl py-2 pl-12 pr-4 focus:outline-none focus:ring-2 focus:ring-brandAzure focus:bg-white text-xs font-semibold transition-all duration-200"
                  aria-label="Email Address"
                />
              </div>
            </div>

            <div>
              <label className="block text-[10px] uppercase font-bold tracking-widest text-slate-400 mb-1.5">
                Password
              </label>
              <div className="relative flex items-center">
                <Lock className="absolute left-4 h-4 w-4 text-slate-400" />
                <input
                  type="password"
                  required
                  value={passwordInput}
                  onChange={e => setPasswordInput(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl py-2 pl-12 pr-4 focus:outline-none focus:ring-2 focus:ring-brandAzure focus:bg-white text-xs font-semibold transition-all duration-200"
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
                  className="rounded border-slate-200 text-brandAzure focus:ring-brandAzure w-4 h-4 cursor-pointer"
                />
                <span>Remember Me</span>
              </label>
              <a href="#forgot" className="text-brandAzure hover:text-brandAzure/80 font-bold hover:underline" onClick={e => { e.preventDefault(); alert("Please contact your system administrator to manage email password keys."); }}>
                Forgot Password?
              </a>
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3.5 bg-gradient-to-r from-brandEmerald to-brandAzure text-white font-bold rounded-xl hover:scale-[1.02] active:scale-[0.98] transition-all duration-200 hover:shadow-lg hover:shadow-brandEmerald/25 flex items-center justify-center gap-2 text-sm mt-4 cursor-pointer focus:outline-none focus:ring-2 focus:ring-brandAzure focus:ring-offset-2 disabled:opacity-50"
              aria-label="Sign In with email and password"
            >
              {isSubmitting ? "Connecting..." : "Sign In"}
            </button>
          </form>

          {/* Secure Footer */}
          <div className="mt-6 text-center border-t border-slate-100/80 pt-5">
            <p className="text-[10px] text-slate-400/50 font-bold tracking-wider uppercase">
              © 2026 TacitIQ • Secure Enterprise Platform
            </p>
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


