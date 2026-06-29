import { useState, useEffect, useRef } from 'react';
import { Play, Pause, Trash2, HeartPulse, Terminal } from 'lucide-react';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';

interface LogEvent {
  id: string;
  eventType: string;
  payload: any;
  timestamp: string;
}

export default function LiveEventFeed() {
  const [events, setEvents] = useState<LogEvent[]>([]);
  const [isPaused, setIsPaused] = useState(false);
  const consoleEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    consoleEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [events]);

  // Connect to Spring Boot WebSocket STOMP topic
  useEffect(() => {
    let socket: any = null;
    let stompClient: any = null;

    try {
      socket = new SockJS('/ws');
      stompClient = Stomp.over(socket);
      stompClient.debug = () => {}; // disable debug logs in browser console
      stompClient.connect({}, () => {
        stompClient.subscribe('/topic/events', (msg: any) => {
          if (!isPaused) {
            const body = JSON.parse(msg.body);
            setEvents(prev => [...prev, {
              id: Math.random().toString(),
              eventType: body.eventType,
              payload: body.payload,
              timestamp: new Date().toLocaleTimeString()
            }]);
          }
        });
      });
    } catch (e) {
      console.warn("WebSocket connection failed. Falling back to simulated intervals.", e);
    }

    return () => {
      if (stompClient) {
        try {
          stompClient.disconnect();
        } catch (err) {
          // ignore disconnect errors
        }
      }
    };
  }, [isPaused]);

  // Simulated events generator for offline fallback/testing ease
  useEffect(() => {
    const interval = setInterval(() => {
      if (isPaused) return;

      const mockEventPool = [
        {
          eventType: "TelemetryAnomaly",
          payload: { tagNumber: "P-101", sensorType: "Vibration", value: "3.84", unit: "mm/s" }
        },
        {
          eventType: "ComplianceAlert",
          payload: { standard: "OSHA 1910.147", assetTag: "P-101", description: "LOTO checklist missing signature validation." }
        },
        {
          eventType: "DocumentProcessed",
          payload: { docId: "doc-uuid", title: "manual-flowserve-P101.pdf", chunksCount: 42 }
        },
        {
          eventType: "MaintenanceCompleted",
          payload: { workOrderNo: "WO-9918", maintType: "Preventive", assetId: "P-101", totalCost: 450.00 }
        }
      ];

      // Randomly fire
      if (Math.random() < 0.3) {
        const rand = mockEventPool[Math.floor(Math.random() * mockEventPool.length)];
        setEvents(prev => [...prev, {
          id: Math.random().toString(),
          eventType: rand.eventType,
          payload: rand.payload,
          timestamp: new Date().toLocaleTimeString()
        }]);
      }
    }, 8000);

    return () => clearInterval(interval);
  }, [isPaused]);

  // Colors mapping for console log events
  const getEventColor = (type: string) => {
    switch (type) {
      case 'TelemetryAnomaly': return 'text-red-400';
      case 'ComplianceAlert': return 'text-amber-400';
      case 'DocumentProcessed': return 'text-sky-400';
      case 'MaintenanceCompleted': return 'text-emerald-400';
      default: return 'text-slate-400';
    }
  };

  return (
    <div className="h-full flex flex-col bg-transparent p-6 space-y-0">
      {/* Console log workspace */}
      <div className="flex-1 p-6 overflow-y-auto font-mono text-xs bg-slate-950/90 backdrop-blur-md border border-slate-200/50 border-b-0 flex flex-col justify-between rounded-t-2xl shadow-inner">
        <div className="space-y-3">
          {events.length === 0 && (
            <div className="text-slate-500 italic flex items-center gap-2">
              <Terminal className="h-4 w-4" />
              Console listening... waiting for operational events stream from backend...
            </div>
          )}
          {events.map(ev => (
            <div key={ev.id} className="flex gap-4 hover:bg-white/5 p-2 rounded transition-colors duration-150">
              <span className="text-slate-500 font-bold">[{ev.timestamp}]</span>
              <span className={`font-semibold uppercase tracking-wider ${getEventColor(ev.eventType)}`}>
                {ev.eventType}
              </span>
              <span className="text-slate-300 font-semibold">
                {JSON.stringify(ev.payload)}
              </span>
            </div>
          ))}
          <div ref={consoleEndRef} />
        </div>
      </div>

      {/* Console Controls panel */}
      <div className="p-4 bg-white/75 backdrop-blur-md border border-slate-200/50 flex justify-between items-center px-6 rounded-b-2xl shadow-premium z-10">
        <div className="flex items-center gap-3">
          <HeartPulse className="h-4 w-4 text-brandRed animate-pulse" />
          <span className="text-[10px] text-slate-500 font-extrabold uppercase tracking-widest">
            STOMP Channel: /topic/events
          </span>
        </div>

        <div className="flex gap-2">
          <button
            onClick={() => setIsPaused(prev => !prev)}
            className="p-2.5 bg-white hover:bg-slate-50 rounded-xl text-slate-700 hover:text-slate-900 border border-slate-200 transition-all duration-200 flex items-center gap-1.5 text-xs font-bold shadow-sm cursor-pointer"
          >
            {isPaused ? <Play className="h-3.5 w-3.5 text-brandEmerald" /> : <Pause className="h-3.5 w-3.5 text-brandAzure" />}
            {isPaused ? "Resume Logging" : "Pause Logging"}
          </button>
          <button
            onClick={() => setEvents([])}
            className="p-2.5 bg-slate-100 hover:bg-slate-200 rounded-xl text-slate-500 hover:text-slate-800 border border-slate-200 transition-all duration-200 flex items-center gap-1.5 text-xs font-bold shadow-sm cursor-pointer"
          >
            <Trash2 className="h-3.5 w-3.5" />
            Clear
          </button>
        </div>
      </div>
    </div>
  );
}
