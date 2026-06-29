import { useState, useEffect } from 'react';
import { User, Sparkles, UserMinus, Calendar, Clock, X, CheckCircle } from 'lucide-react';

interface RetirementRisk {
  engineerId: string;
  email: string;
  monthsToRetirement: number;
  yearsExperience: number;
  expertiseAreas: string[];
  compositeRiskScore: number;
  priorityRating: 'CRITICAL' | 'ELEVATED';
}

export default function KnowledgeLossHeatmap() {
  const [risks, setRisks] = useState<RetirementRisk[]>([]);
  const [isSchedulerOpen, setIsSchedulerOpen] = useState(false);
  const [interviews, setInterviews] = useState<any[]>([
    {
      id: "1",
      email: "engineer@tacitiq.com",
      topic: "Shaft Alignment & Vibration Baselines",
      date: "2026-07-15",
      time: "10:00 AM",
      duration: "60 mins",
      status: "CONFIRMED"
    }
  ]);

  // Form states
  const [formEmail, setFormEmail] = useState('engineer@tacitiq.com');
  const [formTopic, setFormTopic] = useState('Rotating Equipment Overhaul Procedures');
  const [formDate, setFormDate] = useState('2026-07-20');
  const [formTime, setFormTime] = useState('14:00');
  const [formDuration, setFormDuration] = useState('60 mins');

  useEffect(() => {
    const token = localStorage.getItem('tacitiq_token');
    fetch('/api/agents/retirement-risk', {
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
          setRisks(data);
        }
      })
      .catch(() => loadMockRisks());
  }, []);

  const loadMockRisks = () => {
    setRisks([
      {
        engineerId: "a0000000-0000-0000-0000-000000000003",
        email: "engineer@tacitiq.com",
        monthsToRetirement: 9,
        yearsExperience: 35,
        expertiseAreas: ["Rotating Equipment", "Shaft Alignment", "Vibration Analysis"],
        compositeRiskScore: 0.88,
        priorityRating: "CRITICAL"
      },
      {
        engineerId: "a0000000-0000-0000-0000-000000000002",
        email: "manager@tacitiq.com",
        monthsToRetirement: 24,
        yearsExperience: 20,
        expertiseAreas: ["Management", "Operations", "SOP Outlines"],
        compositeRiskScore: 0.54,
        priorityRating: "ELEVATED"
      }
    ]);
  };

  const handleScheduleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const newSession = {
      id: Math.random().toString(),
      email: formEmail,
      topic: formTopic,
      date: formDate,
      time: formTime.includes(':') ? formatTimeToAMPM(formTime) : formTime,
      duration: formDuration,
      status: "SCHEDULED"
    };
    setInterviews(prev => [...prev, newSession]);
    setIsSchedulerOpen(false);
  };

  const formatTimeToAMPM = (time24: string) => {
    const [hoursStr, minutesStr] = time24.split(':');
    const hours = parseInt(hoursStr, 10);
    const ampm = hours >= 12 ? 'PM' : 'AM';
    const displayHours = hours % 12 || 12;
    return `${displayHours}:${minutesStr} ${ampm}`;
  };

  return (
    <div className="h-full p-6 overflow-y-auto space-y-6 relative bg-transparent text-slate-800">
      <div className="flex justify-between items-center">
        <div>
          <h3 className="text-xs font-extrabold uppercase tracking-wider text-slate-700">
            Workforce Planning & Knowledge Capture
          </h3>
          <p className="text-[11px] text-slate-500 font-semibold mt-1">
            Capture queues indicating critical expertise areas at risk of departure within 12–36 months.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {risks.map(r => (
          <div
            key={r.engineerId}
            className={`p-5 rounded-2xl border flex flex-col justify-between transition-all duration-200 hover:shadow-premium hover:-translate-y-0.5 ${
              r.priorityRating === 'CRITICAL'
                ? 'border-red-200 bg-red-50/50 shadow-sm'
                : 'glass-panel'
            }`}
          >
            {/* Risk badge */}
            <div className="flex justify-between items-start mb-4">
              <div className="flex items-center gap-3">
                <div className="p-2.5 bg-slate-100 rounded-xl border border-slate-200">
                  <User className="h-4.5 w-4.5 text-slate-600" />
                </div>
                <div>
                  <h4 className="text-xs font-bold text-slate-800">{r.email}</h4>
                  <p className="text-[10px] text-slate-500 font-semibold mt-0.5">
                    Experience: {r.yearsExperience} Years
                  </p>
                </div>
              </div>
              <span className={`text-[9px] font-bold px-2 py-0.5 rounded-lg border uppercase tracking-wider ${
                r.priorityRating === 'CRITICAL'
                  ? 'bg-red-100 text-brandRed border-red-200'
                  : 'bg-amber-100 text-brandAmber border-amber-200'
              }`}>
                {r.priorityRating} RISK
              </span>
            </div>

            {/* Expertise Areas */}
            <div className="mb-6">
              <p className="text-[9px] text-slate-400 font-extrabold uppercase tracking-wider mb-2">Primary Domain Expertise</p>
              <div className="flex flex-wrap gap-1.5">
                {r.expertiseAreas.map(exp => (
                  <span
                    key={exp}
                    className="text-[9px] font-bold bg-slate-100 px-2 py-0.5 rounded-lg text-slate-600 border border-slate-200/50"
                  >
                    {exp}
                  </span>
                ))}
              </div>
            </div>

            {/* Retiring metric */}
            <div className="pt-4 border-t border-slate-200/50 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <UserMinus className="h-4 w-4 text-brandRed" />
                <span className="text-[10px] font-bold text-slate-700">
                  Retires in {r.monthsToRetirement} Months
                </span>
              </div>
              <div className="text-right">
                <p className="text-[8px] text-slate-400 font-extrabold uppercase tracking-wider">Exposure Score</p>
                <p className={`text-sm font-extrabold ${
                  r.priorityRating === 'CRITICAL' ? 'text-brandRed' : 'text-brandAmber'
                }`}>
                  {Math.round(r.compositeRiskScore * 100)}%
                </p>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Scheduled Sessions List Section */}
      <div className="p-5 rounded-2xl glass-panel shadow-premium">
        <h4 className="text-xs font-extrabold uppercase tracking-wider text-slate-700 mb-4 flex items-center gap-2">
          <Calendar className="h-4 w-4 text-brandEmerald" />
          Active Knowledge Capture Sessions
        </h4>
        <div className="space-y-3">
          {interviews.map(session => (
            <div key={session.id} className="p-4 rounded-xl bg-white/60 border border-slate-250/50 flex items-center justify-between transition-all duration-200 hover:scale-[1.005] hover:shadow-sm">
              <div className="flex items-center gap-4">
                <div className="h-8 w-8 rounded-xl bg-emerald-50 flex items-center justify-center border border-emerald-100">
                  <CheckCircle className="h-4.5 w-4.5 text-brandEmerald" />
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-800">{session.topic}</p>
                  <p className="text-[10px] text-slate-500 font-semibold mt-0.5">Expert: {session.email}</p>
                </div>
              </div>
              <div className="flex items-center gap-6">
                <div className="text-right">
                  <p className="text-[10px] text-slate-700 font-bold font-mono flex items-center justify-end gap-1">
                    <Calendar className="h-3 w-3 text-slate-400" /> {session.date}
                  </p>
                  <p className="text-[10px] text-slate-500 font-semibold font-mono flex items-center justify-end gap-1 mt-0.5">
                    <Clock className="h-3 w-3 text-slate-400" /> {session.time} ({session.duration})
                  </p>
                </div>
                <span className="text-[9px] font-bold px-2 py-0.5 rounded-lg bg-emerald-100 text-brandEmerald border border-emerald-250 uppercase tracking-widest">
                  {session.status}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Action panel */}
      <div className="p-5 rounded-2xl glass-panel shadow-premium flex flex-col sm:flex-row items-center justify-between gap-6">
        <div className="flex items-center gap-4">
          <div className="h-10 w-10 rounded-xl bg-emerald-50 border border-emerald-100 flex items-center justify-center">
            <Sparkles className="h-5 w-5 text-brandEmerald" />
          </div>
          <div>
            <h4 className="text-xs font-bold text-slate-800">Trigger Knowledge Extraction Interview</h4>
            <p className="text-[11px] text-slate-500 font-semibold mt-0.5">
              Schedule automated transcription sessions for retiring experts to record troubleshooting steps.
            </p>
          </div>
        </div>
        <button
          onClick={() => setIsSchedulerOpen(true)}
          className="py-2 px-4 bg-gradient-to-r from-brandEmerald to-brandAzure hover:opacity-95 text-white font-bold text-xs rounded-xl shadow-sm hover:shadow-hover-glow cursor-pointer transition-all duration-200"
        >
          Open Interview Scheduler
        </button>
      </div>

      {/* Interview Scheduler Modal */}
      {isSchedulerOpen && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="w-full max-w-lg rounded-2xl glass-panel shadow-2xl relative z-50 p-6">
            <button
              onClick={() => setIsSchedulerOpen(false)}
              className="absolute top-4 right-4 p-1.5 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-500 hover:text-slate-800 border border-slate-200 cursor-pointer transition-all duration-200"
            >
              <X className="h-4 w-4" />
            </button>

            <div className="mb-5 border-b border-slate-200/50 pb-4">
              <h3 className="text-sm font-extrabold text-slate-800 flex items-center gap-2">
                <Calendar className="h-4.5 w-4.5 text-brandEmerald" />
                Schedule Expert Interview
              </h3>
              <p className="text-[11px] text-slate-500 font-semibold mt-1">
                Configure a structured knowledge extraction slot to capture undocumented industrial procedures.
              </p>
            </div>

            <form onSubmit={handleScheduleSubmit} className="space-y-4">
              <div>
                <label className="block text-[9px] uppercase font-bold tracking-widest text-slate-400 mb-1.5">
                  Target Engineer / Expert
                </label>
                <select
                  value={formEmail}
                  onChange={e => setFormEmail(e.target.value)}
                  className="w-full bg-white border border-slate-200 rounded-xl py-2 px-3 focus:outline-none focus:border-brandEmerald text-xs font-bold text-slate-800 cursor-pointer"
                >
                  <option value="engineer@tacitiq.com">engineer@tacitiq.com (Rotating Equipment Expert)</option>
                  <option value="manager@tacitiq.com">manager@tacitiq.com (Operations Manager)</option>
                </select>
              </div>

              <div>
                <label className="block text-[9px] uppercase font-bold tracking-widest text-slate-400 mb-1.5">
                  Knowledge Topic / Focus Area
                </label>
                <input
                  type="text"
                  required
                  value={formTopic}
                  onChange={e => setFormTopic(e.target.value)}
                  placeholder="e.g. Pump P-101 shaft alignment troubleshooting guide"
                  className="w-full bg-white border border-slate-200 rounded-xl py-2.5 px-3 focus:outline-none focus:border-brandEmerald text-xs font-semibold text-slate-800 placeholder-slate-400"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-[9px] uppercase font-bold tracking-widest text-slate-400 mb-1.5">
                    Proposed Date
                  </label>
                  <input
                    type="date"
                    required
                    value={formDate}
                    onChange={e => setFormDate(e.target.value)}
                    className="w-full bg-white border border-slate-200 rounded-xl py-2 px-3 focus:outline-none focus:border-brandEmerald text-xs font-semibold text-slate-800 cursor-pointer"
                  />
                </div>
                <div>
                  <label className="block text-[9px] uppercase font-bold tracking-widest text-slate-400 mb-1.5">
                    Proposed Time (24h)
                  </label>
                  <input
                    type="time"
                    required
                    value={formTime}
                    onChange={e => setFormTime(e.target.value)}
                    className="w-full bg-white border border-slate-200 rounded-xl py-2 px-3 focus:outline-none focus:border-brandEmerald text-xs font-semibold text-slate-800 cursor-pointer"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[9px] uppercase font-bold tracking-widest text-slate-400 mb-1.5">
                  Session Duration
                </label>
                <select
                  value={formDuration}
                  onChange={e => setFormDuration(e.target.value)}
                  className="w-full bg-white border border-slate-200 rounded-xl py-2 px-3 focus:outline-none focus:border-brandEmerald text-xs font-bold text-slate-800 cursor-pointer"
                >
                  <option value="30 mins">30 mins (Brief review)</option>
                  <option value="60 mins">60 mins (Standard capture)</option>
                  <option value="90 mins">90 mins (Detailed walkthrough)</option>
                </select>
              </div>

              <div className="pt-4 border-t border-slate-200/50 flex justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsSchedulerOpen(false)}
                  className="py-2 px-4 bg-slate-100 hover:bg-slate-200 border border-slate-200 text-slate-600 hover:text-slate-800 rounded-xl text-xs font-bold transition-all duration-200 cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="py-2 px-4 bg-brandEmerald text-white font-bold rounded-xl text-xs hover:opacity-95 transition-all duration-200 cursor-pointer shadow-sm hover:shadow-hover-glow"
                >
                  Register Session
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
