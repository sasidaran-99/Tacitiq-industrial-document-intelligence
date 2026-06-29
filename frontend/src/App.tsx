import { useState, useEffect } from 'react';
import { Activity, ShieldAlert, Cpu, Heart, Database, MessageSquareCode, LogOut, Mail, Lock, FileText } from 'lucide-react';
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

export default function App() {
  const [token, setToken] = useState<string | null>(localStorage.getItem('tacitiq_token'));
  const [user, setUser] = useState<any>(JSON.parse(localStorage.getItem('tacitiq_user') || 'null'));
  const [emailInput, setEmailInput] = useState('admin@tacitiq.com');
  const [passwordInput, setPasswordInput] = useState('password');
  const [loginError, setLoginError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

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

  const handleLogout = () => {
    localStorage.removeItem('tacitiq_token');
    localStorage.removeItem('tacitiq_user');
    setToken(null);
    setUser(null);
  };

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
        throw new Error("Invalid username or password.");
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

  // Fetch aggregate status stats from backend
  useEffect(() => {
    if (!token) return;

    fetch('/api/dashboard/summary', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })
      .then(res => {
        if (res.status === 401 || res.status === 403) {
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

  // Render Login View if not authenticated
  if (!token) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-brandBg text-slate-800 overflow-hidden font-sans relative">
        {/* Layered glowing background blobs */}
        <div className="absolute top-1/4 left-1/4 w-[350px] h-[350px] bg-brandEmerald/10 rounded-full blur-[80px] pointer-events-none" />
        <div className="absolute bottom-1/4 right-1/4 w-[350px] h-[350px] bg-brandAzure/15 rounded-full blur-[80px] pointer-events-none" />
        
        <div className="w-full max-w-md p-8 glass-panel border border-white/55 shadow-premium rounded-2xl relative z-10 transition-all duration-300 hover:shadow-hover-glow">
          <div className="text-center mb-8">
            <div className="h-14 w-14 rounded-2xl bg-white border border-slate-100 flex items-center justify-center mx-auto mb-4 shadow-sm">
              <LogoIcon />
            </div>
            <h1 className="text-2xl font-extrabold bg-gradient-to-r from-brandEmerald to-brandAzure bg-clip-text text-transparent tracking-tight">
              TacitIQ Portal
            </h1>
            <p className="text-[10px] text-slate-500 mt-1.5 font-bold uppercase tracking-wider">
              Enterprise AI Platform
            </p>
          </div>

          {loginError && (
            <div className="p-3.5 mb-6 bg-brandRed/10 border border-brandRed/20 rounded-xl text-xs font-bold text-brandRed">
              {loginError}
            </div>
          )}

          <form onSubmit={handleLogin} className="space-y-5">
            <div>
              <label className="block text-[10px] uppercase font-bold tracking-widest text-slate-500 mb-1.5">
                Engineer Email
              </label>
              <div className="relative flex items-center">
                <Mail className="absolute left-4 h-4 w-4 text-slate-400" />
                <input
                  type="email"
                  required
                  value={emailInput}
                  onChange={e => setEmailInput(e.target.value)}
                  placeholder="name@company.com"
                  className="w-full bg-white/60 border border-slate-200 rounded-xl py-2.5 pl-12 pr-4 focus:outline-none focus:border-brandEmerald text-sm font-medium transition-all duration-200"
                />
              </div>
            </div>

            <div>
              <label className="block text-[10px] uppercase font-bold tracking-widest text-slate-500 mb-1.5">
                Security Key
              </label>
              <div className="relative flex items-center">
                <Lock className="absolute left-4 h-4 w-4 text-slate-400" />
                <input
                  type="password"
                  required
                  value={passwordInput}
                  onChange={e => setPasswordInput(e.target.value)}
                  placeholder="••••••••"
                  className="w-full bg-white/60 border border-slate-200 rounded-xl py-2.5 pl-12 pr-4 focus:outline-none focus:border-brandEmerald text-sm font-medium transition-all duration-200"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-2.5 bg-gradient-to-r from-brandEmerald to-brandAzure hover:opacity-95 text-white font-bold rounded-xl transition-all duration-200 flex items-center justify-center gap-2 text-sm mt-2 cursor-pointer shadow-sm hover:shadow-hover-glow disabled:opacity-50"
            >
              {isSubmitting ? "Connecting..." : "Connect to Platform"}
            </button>
          </form>

          {/* Quick credentials helper */}
          <div className="mt-8 pt-6 border-t border-slate-100 text-center">
            <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider mb-2">
              Assigned Credentials
            </p>
            <div className="inline-block bg-slate-50 border border-slate-100 rounded-xl px-4 py-2.5 text-[11px] font-mono text-slate-500 text-left">
              <div>Email: <span className="text-slate-800">admin@tacitiq.com</span></div>
              <div>Pswd: <span className="text-slate-800">password</span></div>
            </div>
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
      {/* Dynamic layered background mesh blobs */}
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
                Enterprise AI Platform
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
                  onClick={() => setActiveTab(item.id as any)}
                  className={`w-full flex items-center gap-3 px-4 py-2.5 rounded-xl text-xs font-semibold transition-all duration-200 cursor-pointer ${
                    isActive
                      ? 'bg-gradient-to-r from-brandEmerald/10 to-brandAzure/10 text-brandEmerald border-l-4 border-brandEmerald shadow-sm'
                      : 'text-slate-500 hover:bg-slate-100/50 hover:text-slate-800'
                  }`}
                >
                  <Icon className={`h-4.5 w-4.5 ${isActive ? 'text-brandEmerald' : 'text-slate-400'}`} />
                  {item.label}
                </button>
              );
            })}
          </nav>
        </div>

        {/* System Diagnostics & Logout Badge */}
        <div className="p-4 border-t border-slate-200/50 bg-white/40 space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="h-2 w-2 rounded-full bg-brandEmerald animate-pulse"></span>
              <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider">
                System Live
              </span>
            </div>
            <button
              onClick={handleLogout}
              title="Logout Session"
              className="p-1.5 rounded-lg bg-slate-100 hover:bg-brandRed/10 text-slate-400 hover:text-brandRed border border-slate-200 hover:border-brandRed/20 transition-all duration-200 cursor-pointer"
            >
              <LogOut className="h-3.5 w-3.5" />
            </button>
          </div>
          <div className="space-y-0.5">
            <p className="text-[10px] text-slate-700 truncate font-semibold">
              {user?.email}
            </p>
            <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">
              {user?.role}
            </p>
          </div>
        </div>
      </aside>

      {/* Main Workspace Frame */}
      <main className="flex-1 flex flex-col overflow-hidden z-10">
        {/* Top Header Panel */}
        <header className="h-16 bg-white/70 backdrop-blur-md border-b border-slate-200/50 px-8 flex items-center justify-between z-10">
          <h2 className="text-sm font-bold text-slate-800 flex items-center gap-2">
            {getHeaderTitle()}
          </h2>
          <div className="flex items-center gap-6">
            <div className="text-right">
              <span className="text-[9px] text-slate-400 uppercase tracking-widest font-bold">
                Health Index
              </span>
              <p className="text-xs font-bold text-brandEmerald">
                {stats.averageHealthScore} Avg
              </p>
            </div>
            <div className="text-right">
              <span className="text-[9px] text-slate-400 uppercase tracking-widest font-bold">
                Active Issues
              </span>
              <p className="text-xs font-bold text-brandRed">
                {stats.activeIncidentsCount} Incidents
              </p>
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


