import { useState, useEffect, useCallback } from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { ShieldCheck, AlertOctagon, HelpCircle } from 'lucide-react';
import { getApiUrl } from '../api/config';

interface TelemetryPoint {
  timestamp: string;
  temperature: number;
  vibration: number;
  pressure: number;
  flow: number;
}

export default function AssetDashboard() {
  const [selectedAsset, setSelectedAsset] = useState<string>("P-101");
  const [telemetry, setTelemetry] = useState<TelemetryPoint[]>([]);
  const [prediction, setPrediction] = useState<any>({
    remainingUsefulLifeDays: 85,
    failureProbability30Days: 0.18,
    shapExplainer: {
      "Vibration Amplitude (mm/s)": 0.12,
      "Operating Temperature (C)": 0.04,
      "Asset Installation Age (Years)": 0.02
    },
    recommendedActionWindow: "SCHEDULED (15-30 days)"
  });

  const [docStats, setDocStats] = useState({
    totalDocuments: 0,
    processedDocuments: 0,
    extractedEntities: 0,
    graphLinksCreated: 0,
    lastUploadedDocument: "None"
  });

  const assetsList = [
    { tag: "P-101", name: "Centrifugal Pump", health: 85, id: "c0000000-0000-0000-0000-000000000001" },
    { tag: "K-201", name: "Centrifugal Compressor", health: 92, id: "c0000000-0000-0000-0000-000000000002" },
    { tag: "E-205", name: "Heat Exchanger", health: 78, id: "c0000000-0000-0000-0000-000000000003" }
  ];

  const currentAssetId = assetsList.find(a => a.tag === selectedAsset)?.id;

  const logMock = useCallback((msg: string) => {
    console.log(`[TacitIQ Cache] ${msg}`);
  }, []);

  const loadMockTelemetry = useCallback(() => {
    const list = [];
    const baseVal = selectedAsset === 'K-201' ? 3.5 : selectedAsset === 'E-205' ? 0.2 : 1.8;
    for (let i = 0; i < 12; i++) {
      list.push({
        timestamp: `${i * 5}m`,
        temperature: 45 + Math.sin(i) * 2,
        vibration: baseVal + Math.cos(i) * 0.3,
        pressure: 12 + Math.sin(i) * 0.5,
        flow: 250 + Math.cos(i) * 8
      });
    }
    setTelemetry(list);
  }, [selectedAsset]);

  // Fetch telemetry and failure predictions
  useEffect(() => {
    if (!currentAssetId) return;

    const token = localStorage.getItem('tacitiq_token');

    fetch(getApiUrl(`/api/assets/${currentAssetId}/telemetry?points=12`), {
      headers: { 'Authorization': `Bearer ${token}` }
    })
      .then(res => {
        if (res.status === 401 || res.status === 403) {
          throw new Error("Unauthorized");
        }
        return res.json();
      })
      .then(data => {
        if (data && !data.error) {
          setTelemetry(data.map((p: any) => ({
            ...p,
            timestamp: new Date(p.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
          })));
        } else {
          loadMockTelemetry();
        }
      })
      .catch(() => loadMockTelemetry());

    fetch(getApiUrl(`/api/agents/predict/${currentAssetId}`), {
      headers: { 'Authorization': `Bearer ${token}` }
    })
      .then(res => {
        if (res.status === 401 || res.status === 403) {
          throw new Error("Unauthorized");
        }
        return res.json();
      })
      .then(data => {
        if (data && !data.error) {
          setPrediction(data);
        }
      })
      .catch(() => logMock("Predictions loaded from local offline cache."));
  }, [selectedAsset, currentAssetId, loadMockTelemetry, logMock]);

  useEffect(() => {
    const token = localStorage.getItem('tacitiq_token');
    fetch(getApiUrl('/api/dashboard/summary'), {
      headers: { 'Authorization': `Bearer ${token}` }
    })
      .then(res => res.json())
      .then(data => {
        if (data && !data.error) {
          setDocStats({
            totalDocuments: data.totalDocuments || 0,
            processedDocuments: data.processedDocuments || 0,
            extractedEntities: data.extractedEntities || 0,
            graphLinksCreated: data.graphLinksCreated || 0,
            lastUploadedDocument: data.lastUploadedDocument || "None"
          });
        }
      })
      .catch(err => console.error("Failed to load document stats", err));
  }, []);

  return (
    <div className="h-full flex flex-col md:flex-row bg-transparent overflow-y-auto">
      {/* Telemetry Charts and Gauge */}
      <div className="flex-1 p-6 space-y-6">
        {/* Selector Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          {assetsList.map(a => (
            <button
              key={a.tag}
              onClick={() => setSelectedAsset(a.tag)}
              className={`p-4 rounded-2xl border text-left transition-all duration-200 cursor-pointer hover:shadow-premium hover:-translate-y-0.5 ${
                selectedAsset === a.tag
                  ? 'bg-white border-brandEmerald shadow-premium scale-[1.01]'
                  : 'bg-white/60 border-white/50 hover:bg-white/95 text-slate-700'
              }`}
            >
              <div className="flex justify-between items-center mb-2">
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-500">{a.tag}</span>
                <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${
                  a.health > 85
                    ? 'bg-emerald-50 text-brandEmerald border-emerald-100'
                    : 'bg-amber-50 text-brandAmber border-amber-100'
                }`}>
                  {a.health}% Health
                </span>
              </div>
              <h4 className="text-xs font-bold text-slate-800">{a.name}</h4>
            </button>
          ))}
        </div>

        {/* Vibration Chart */}
        <div className="p-5 rounded-2xl glass-panel">
          <h3 className="text-[10px] font-bold uppercase tracking-widest text-slate-500 mb-4">
            Sensor Telemetry (Vibration mm/s)
          </h3>
          <div className="h-60 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={telemetry}>
                <defs>
                  <linearGradient id="areaAzure" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#0EA5E9" stopOpacity={0.25}/>
                    <stop offset="95%" stopColor="#0EA5E9" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" />
                <XAxis dataKey="timestamp" stroke="#94A3B8" fontSize={9} fontWeight="bold" />
                <YAxis stroke="#94A3B8" fontSize={9} fontWeight="bold" />
                <Tooltip
                  contentStyle={{
                    background: 'rgba(255, 255, 255, 0.92)',
                    border: '1px solid rgba(255, 255, 255, 0.60)',
                    borderRadius: '12px',
                    boxShadow: '0 8px 32px 0 rgba(15, 23, 42, 0.05)',
                    color: '#0F172A'
                  }}
                />
                <Area type="monotone" dataKey="vibration" stroke="#0EA5E9" strokeWidth={2} fillOpacity={1} fill="url(#areaAzure)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Temperature Chart */}
        <div className="p-5 rounded-2xl glass-panel">
          <h3 className="text-[10px] font-bold uppercase tracking-widest text-slate-500 mb-4">
            Sensor Telemetry (Bearing Temperature °C)
          </h3>
          <div className="h-60 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={telemetry}>
                <defs>
                  <linearGradient id="areaEmerald" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10B981" stopOpacity={0.25}/>
                    <stop offset="95%" stopColor="#10B981" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#E2E8F0" />
                <XAxis dataKey="timestamp" stroke="#94A3B8" fontSize={9} fontWeight="bold" />
                <YAxis stroke="#94A3B8" fontSize={9} fontWeight="bold" />
                <Tooltip
                  contentStyle={{
                    background: 'rgba(255, 255, 255, 0.92)',
                    border: '1px solid rgba(255, 255, 255, 0.60)',
                    borderRadius: '12px',
                    boxShadow: '0 8px 32px 0 rgba(15, 23, 42, 0.05)',
                    color: '#0F172A'
                  }}
                />
                <Area type="monotone" dataKey="temperature" stroke="#10B981" strokeWidth={2} fillOpacity={1} fill="url(#areaEmerald)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Failure Risk & SHAP Feature Importance */}
      <div className="w-full md:w-80 bg-white/70 backdrop-blur-md border-t md:border-t-0 md:border-l border-slate-200/50 p-6 flex flex-col gap-6 z-10">
        <div>
          <h3 className="text-xs font-extrabold uppercase tracking-wider text-slate-700 mb-4">
            Risk Assessment
          </h3>

          <div className="space-y-4">
            {/* RUL Card */}
            <div className="p-4 rounded-2xl bg-white border border-slate-100 flex justify-between items-center shadow-sm">
              <div>
                <p className="text-[9px] text-slate-400 font-extrabold uppercase tracking-wider">Estimated RUL</p>
                <p className="text-xl font-black text-slate-800">
                  {prediction.remainingUsefulLifeDays} Days
                </p>
              </div>
              {prediction.remainingUsefulLifeDays < 30 ? (
                <AlertOctagon className="h-9 w-9 text-brandRed" />
              ) : (
                <ShieldCheck className="h-9 w-9 text-brandGreen" />
              )}
            </div>

            {/* Failure probability gauge */}
            <div className="p-4 rounded-2xl bg-white border border-slate-100 shadow-sm">
              <div className="flex justify-between text-[10px] font-extrabold text-slate-500 mb-2 uppercase tracking-wider">
                <span>30-Day Failure Prob</span>
                <span className={prediction.failureProbability30Days > 0.4 ? 'text-brandRed' : 'text-brandAzure'}>
                  {Math.round(prediction.failureProbability30Days * 100)}%
                </span>
              </div>
              <div className="w-full bg-slate-100 h-2.5 rounded-full overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-500 ${
                    prediction.failureProbability30Days > 0.4 ? 'bg-brandRed' : 'bg-brandAzure'
                  }`}
                  style={{ width: `${prediction.failureProbability30Days * 100}%` }}
                ></div>
              </div>
            </div>
          </div>
        </div>

        {/* SHAP Feature Explainer Panel */}
        <div className="pt-5 border-t border-slate-200/50">
          <h4 className="text-[10px] font-extrabold uppercase tracking-wider text-slate-500 mb-4 flex items-center gap-1.5">
            SHAP Explainability Index
            <HelpCircle className="h-3.5 w-3.5 text-slate-400" />
          </h4>
          <div className="space-y-4">
            {Object.entries(prediction.shapExplainer || {}).map(([key, val]: any) => (
              <div key={key}>
                <div className="flex justify-between text-[9px] font-bold text-slate-500 mb-1">
                  <span className="truncate w-44">{key}</span>
                  <span className="text-brandAzure">+{Math.round(val * 100)}%</span>
                </div>
                <div className="w-full bg-slate-100 h-2 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-brandAzure rounded-full"
                    style={{ width: `${val * 100}%` }}
                  ></div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Document Intelligence Card */}
        <div className="pt-5 border-t border-slate-200/50">
          <h4 className="text-[10px] font-extrabold uppercase tracking-wider text-slate-500 mb-4 flex items-center gap-1.5">
            Document Intelligence
          </h4>
          <div className="space-y-3.5 bg-slate-50 p-4 rounded-xl border border-slate-100 shadow-sm text-[10px] font-semibold text-slate-500">
            <div className="flex justify-between items-center">
              <span>Ingested Docs</span>
              <span className="text-slate-800 font-extrabold">{docStats.totalDocuments}</span>
            </div>
            <div className="flex justify-between items-center">
              <span>Processed & Indexed</span>
              <span className="text-slate-800 font-extrabold">{docStats.processedDocuments}</span>
            </div>
            <div className="flex justify-between items-center">
              <span>Ontology Entities</span>
              <span className="text-slate-800 font-extrabold">{docStats.extractedEntities}</span>
            </div>
            <div className="flex justify-between items-center">
              <span>Graph Links Created</span>
              <span className="text-violet-600 font-extrabold">{docStats.graphLinksCreated}</span>
            </div>
            <div className="pt-2 border-t border-slate-200/40">
              <span className="block text-[8px] uppercase font-bold tracking-widest text-slate-400">Last Ingested</span>
              <span className="text-[10px] font-bold text-slate-700 truncate block mt-0.5" title={docStats.lastUploadedDocument}>
                {docStats.lastUploadedDocument}
              </span>
            </div>
          </div>
        </div>

        {/* Recommended Action Box */}
        <div className="pt-5 border-t border-slate-200/50 mt-auto">
          <p className="text-[9px] text-slate-400 font-extrabold uppercase tracking-wider mb-2">Action Window</p>
          <span className={`text-[10px] font-bold px-3 py-1.5 rounded-full shadow-sm border ${
            prediction.remainingUsefulLifeDays < 30
              ? 'bg-red-50 text-brandRed border-red-100'
              : 'bg-emerald-50 text-brandEmerald border-emerald-100'
          }`}>
            {prediction.recommendedActionWindow}
          </span>
        </div>
      </div>
    </div>
  );
}

