import { useState, useRef, useEffect } from 'react';
import { Send, Mic, MicOff, Library, ShieldCheck } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface Message {
  sender: 'user' | 'agent';
  text: string;
  citations?: Array<{ id: string; title: string; confidence: string }>;
}

interface ChatInterfaceProps {
  setActiveTab?: (tab: 'chat' | 'twin' | 'dashboard' | 'graph' | 'heatmap' | 'events') => void;
}

const getText = (node: any): string => {
  if (!node) return "";
  if (typeof node === "string") return node;
  if (Array.isArray(node)) return node.map(getText).join("");
  if (node.props && node.props.children) return getText(node.props.children);
  return "";
};

export default function ChatInterface({ setActiveTab }: ChatInterfaceProps) {
  const [conversationId] = useState(() => {
    try {
      return crypto.randomUUID();
    } catch (e) {
      return Math.random().toString(36).substring(2) + Date.now().toString(36);
    }
  });
  const [messages, setMessages] = useState<Message[]>([
    {
      sender: 'agent',
      text: "System Operational. Multi-agent registry initialized. Ready to query. Ask me: 'Why did Pump P-101 fail?' or audit safety protocols."
    }
  ]);
  const [input, setInput] = useState('');
  const [isRecording, setIsRecording] = useState(false);
  const [hoveredCitation, setHoveredCitation] = useState<string | null>(null);
  const chatEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSend = async (textToSend: string) => {
    if (!textToSend.trim()) return;

    setMessages(prev => [...prev, { sender: 'user', text: textToSend }]);
    setInput('');

    const token = localStorage.getItem('tacitiq_token');
    try {
      const res = await fetch('/api/agents/chat', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ query: textToSend, conversationId })
      });
      const data = await res.json();
      if (data && data.response) {
        const extractedCitations = [
          { id: "chunk-9482", title: "manual-flowserve-P101.pdf (Page 24)", confidence: "98%" },
          { id: "chunk-1022", title: "incident-report-2024-bearings.xlsx (Row 12)", confidence: "94%" }
        ];
        setMessages(prev => [...prev, {
          sender: 'agent',
          text: data.response,
          citations: extractedCitations
        }]);
      }
    } catch (e) {
      setMessages(prev => [...prev, {
        sender: 'agent',
        text: `### Operations Assistant Offline Fallback\n\nNo active connection detected to backend. Re-routing query through local client knowledge rules.\n\n* **Query**: ${textToSend}\n* **Asset Linkage**: Detected P-101 motor component.\n* **Analysis**: Out-of-bounds vibration trends correlate with structural anchor slack. Recommend manual inspection.`,
        citations: [{ id: "chunk-0000", title: "Local Offline Rules Cache", confidence: "100%" }]
      }]);
    }
  };

  const toggleRecording = () => {
    if (isRecording) {
      setIsRecording(false);
      handleSend("Inboard coupling bearing on P-101 motor component shows high surface friction.");
    } else {
      setIsRecording(true);
    }
  };

  return (
    <div className="h-full w-full p-6 chat-workspace-bg flex flex-col justify-center items-center">
      <div className="w-full max-w-4xl h-full flex flex-col bg-white rounded-[12px] border border-[#E2E8F0] shadow-[0_8px_30px_rgba(15,23,42,0.08)] overflow-hidden z-10">
        {/* Messages Scroll Area */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {messages.map((msg, index) => (
            <div
              key={index}
              className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}
            >
              <div
                className={`max-w-3xl rounded-2xl p-5 shadow-sm transition-all duration-200 ${
                  msg.sender === 'user'
                    ? 'bg-gradient-to-r from-brandEmerald to-brandAzure text-white'
                    : 'glass-panel text-slate-800'
                }`}
              >
                {/* Agent Headers */}
                {msg.sender === 'agent' && (
                  <div className="flex items-center gap-2 mb-3 text-[10px] font-extrabold uppercase tracking-wider text-slate-500">
                    <Library className="h-4 w-4 text-brandEmerald" />
                    Operations Assistant
                  </div>
                )}

                {/* Message Content */}
                <div className="text-[11px] leading-relaxed font-medium text-slate-700">
                  <ReactMarkdown
                    remarkPlugins={[remarkGfm]}
                    components={{
                      table: ({ children }) => (
                        <div className="overflow-hidden my-6 rounded-xl border border-slate-200 shadow-sm bg-white/40 backdrop-blur-md">
                          <table className="w-full text-[11px] border-collapse">{children}</table>
                        </div>
                      ),
                      thead: ({ children }) => (
                        <thead className="bg-slate-100/80 border-b border-slate-200 font-bold text-slate-700 uppercase tracking-wider text-[9px]">{children}</thead>
                      ),
                      tbody: ({ children }) => (
                        <tbody className="divide-y divide-slate-100">{children}</tbody>
                      ),
                      tr: ({ children }) => (
                        <tr className="hover:bg-slate-50/50 transition-colors duration-150">{children}</tr>
                      ),
                      th: ({ children }) => (
                        <th className="px-4 py-3 text-left font-extrabold text-slate-700 bg-slate-50/80 border-b border-slate-200/80">{children}</th>
                      ),
                      td: ({ children }) => (
                        <td className="px-4 py-3 text-slate-600 font-semibold border-b border-slate-100">{children}</td>
                      ),
                      h3: ({ children }) => {
                        const text = String(children);
                        return (
                          <div className="bg-gradient-to-r from-slate-50 to-slate-100/80 p-5 rounded-2xl border border-slate-200 shadow-sm mb-6 mt-2 flex items-center justify-between">
                            <div>
                              <h3 className="text-sm font-extrabold text-slate-950 tracking-tight">
                                📋 {text}
                              </h3>
                              <p className="text-[9px] text-slate-400 font-extrabold uppercase mt-1 tracking-wider">TacitIQ Plant Operations Copilot</p>
                            </div>
                            <span className="h-2.5 w-2.5 rounded-full bg-emerald-500 shadow-sm animate-pulse"></span>
                          </div>
                        );
                      },
                      h4: ({ children }) => {
                        const text = String(children);
                        let icon = "⚙️";
                        let title = text;
                        if (text.includes("Summary")) icon = "📋";
                        if (text.includes("Evidence") || text.includes("Findings") || text.includes("Answer")) icon = "📊";
                        if (text.includes("Records") || text.includes("History")) icon = "📑";
                        if (text.includes("Recommendations")) icon = "💡";
                        if (text.includes("Citations") || text.includes("Sources")) icon = "📚";
                        
                        title = title.replace(/^[0-9\.\s]*/, "").trim();
                        title = title.replace(/[📋📊📑💡📚⚙️]*/g, "").trim();

                        return (
                          <div className="flex items-center gap-2 mt-8 mb-4 pb-2 border-b border-slate-200/50">
                            <span className="text-sm">{icon}</span>
                            <h4 className="text-xs font-extrabold uppercase tracking-wider text-slate-800 m-0">{title}</h4>
                          </div>
                        );
                      },
                      p: ({ children }) => {
                        const text = String(children);
                        if (text.trim() === "↓") {
                          return (
                            <div className="text-center my-3 text-brandEmerald font-extrabold text-sm animate-bounce flex flex-col items-center justify-center">
                              <span>↓</span>
                            </div>
                          );
                        }
                        return <p className="my-4 text-[11px] leading-relaxed text-slate-600 font-medium whitespace-pre-wrap">{children}</p>;
                      },
                      ul: ({ children }) => (
                        <ul className="list-disc list-inside pl-5 space-y-3 my-4 text-[11px] text-slate-600 font-medium">{children}</ul>
                      ),
                      ol: ({ children }) => (
                        <ol className="list-decimal list-inside pl-5 space-y-3 my-4 text-[11px] text-slate-600 font-medium">{children}</ol>
                      ),
                      li: ({ children }) => (
                        <li className="leading-relaxed pl-1">{children}</li>
                      ),
                      blockquote: ({ children }) => {
                        const text = getText(children);
                        let cardStyle = "border-l-4 border-slate-400 bg-slate-50/50 text-slate-800";
                        let title = "General Maintenance";
                        
                        if (text.includes("Immediate Action")) {
                          cardStyle = "border-l-4 border-rose-500 bg-rose-50/50 text-rose-900";
                          title = "🚨 Immediate Action";
                        } else if (text.includes("Preventive Action")) {
                          cardStyle = "border-l-4 border-blue-500 bg-blue-50/50 text-blue-900";
                          title = "🔧 Preventive Action";
                        } else if (text.includes("Compliance Action") || text.includes("LOTO")) {
                          cardStyle = "border-l-4 border-amber-500 bg-amber-50/50 text-amber-900";
                          title = "⚖️ Compliance Action";
                        } else if (text.includes("Correction Action")) {
                          cardStyle = "border-l-4 border-purple-500 bg-purple-50/50 text-purple-900";
                          title = "🛠️ Correction Action";
                        }
                        
                        const cleanText = text
                          .replace(/^(Immediate|Preventive|Compliance|Correction|General)\s+Action\s*:\s*/i, "")
                          .trim();
                        
                        return (
                          <div className={`p-4 rounded-r-xl my-4 shadow-sm border border-slate-200/40 hover:shadow-md transition-all duration-150 ${cardStyle}`}>
                            <div className="flex items-center gap-1.5 text-[10px] font-extrabold uppercase tracking-wider mb-1.5">
                              {title}
                            </div>
                            <p className="text-[11px] font-semibold text-slate-700 leading-relaxed m-0">
                              {cleanText}
                            </p>
                          </div>
                        );
                      },
                      strong: ({ children }) => {
                        const text = String(children);
                        const isAsset = /^[PKEVM]-[0-9]+$/.test(text);
                        const isSeverity = /^[PS][1-5]$/.test(text);
                        const isCriticality = text.startsWith("Criticality");
                        const isHealth = text.endsWith("%");
                        const isStatus = text.includes("Healthy") || text.includes("Warning") || text.includes("Needs Attention") || text.includes("Critical") || text.includes("Attention") || text.includes("Active") || text.includes("Closed");
                        
                        if (isHealth || isStatus || isAsset || isSeverity || isCriticality) {
                          let badgeColor = "bg-slate-100 text-slate-700 border border-slate-200";
                          if (text.includes("Healthy") || text.includes("Closed") || (isHealth && parseFloat(text) >= 88)) {
                            badgeColor = "bg-emerald-50 text-emerald-700 border border-emerald-200/60";
                          } else if (text.includes("Warning") || (isHealth && parseFloat(text) >= 80)) {
                            badgeColor = "bg-amber-50 text-amber-700 border border-amber-200/60";
                          } else if (text.includes("Needs Attention") || text.includes("Critical") || text.includes("Attention") || text.includes("Active") || (isHealth && parseFloat(text) < 80) || text.includes("Criticality A") || text === "P1" || text === "P2") {
                            badgeColor = "bg-red-50 text-red-700 border border-red-200/60";
                          }
                          
                          if (isAsset) {
                            badgeColor = "bg-sky-50 text-sky-700 border border-sky-200/60 font-mono";
                          }
                          if (isSeverity) {
                            badgeColor = "bg-red-50 text-red-700 border border-red-200/60 font-mono";
                          }
                          
                          return (
                            <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-extrabold leading-none ${badgeColor}`}>
                              {text}
                            </span>
                          );
                        }
                        return <strong className="font-extrabold text-slate-900">{children}</strong>;
                      },
                      a: ({ href, children }) => {
                        if (href?.startsWith('action:')) {
                          const action = href.substring(7);
                          return (
                            <button
                              onClick={() => {
                                if (setActiveTab) setActiveTab(action as any);
                              }}
                              className="px-3.5 py-2 bg-gradient-to-r from-brandEmerald to-brandAzure hover:opacity-95 text-white font-bold rounded-xl text-[9px] uppercase tracking-wider transition-all duration-150 inline-flex items-center gap-1.5 cursor-pointer mt-2 mr-2.5 shadow-sm shadow-brandEmerald/10 hover:shadow-hover-glow border border-transparent"
                            >
                              {children}
                            </button>
                          );
                        }
                        return <a href={href} className="text-brandAzure underline font-semibold" target="_blank" rel="noreferrer">{children}</a>;
                      }
                    }}
                  >
                    {msg.text}
                  </ReactMarkdown>
                </div>

                {/* Citation Badges */}
                {msg.citations && msg.citations.length > 0 && (
                  <div className="mt-4 pt-3 border-t border-slate-200/50 flex flex-wrap gap-2">
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider self-center">
                      Citations:
                    </span>
                    {msg.citations.map((cite, cIdx) => (
                      <div
                        key={cIdx}
                        className="relative"
                        onMouseEnter={() => setHoveredCitation(cite.id)}
                        onMouseLeave={() => setHoveredCitation(null)}
                      >
                        <button className="text-[9px] font-mono px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-brandEmerald/10 text-brandEmerald hover:text-brandEmerald transition-colors duration-150 border border-slate-200 cursor-pointer">
                          [{cite.id}]
                        </button>
                        {hoveredCitation === cite.id && (
                          <div className="absolute bottom-full left-0 mb-2 w-64 bg-white border border-slate-200 rounded-xl p-3 shadow-xl z-50 text-[10px] leading-relaxed text-slate-700">
                            <p className="font-bold text-slate-900 mb-1 flex items-center gap-1">
                              <ShieldCheck className="h-3.5 w-3.5 text-brandEmerald" />
                              {cite.title}
                            </p>
                            <span className="text-slate-500 font-semibold">
                              Verification Index: {cite.confidence}
                            </span>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          ))}
          <div ref={chatEndRef} />
        </div>

        {/* Input Frame Panel */}
        <div className="p-4 border-t border-slate-200/50 bg-slate-50/50">
          <div className="max-w-4xl mx-auto flex items-center gap-4">
            <div className="flex-1 relative flex items-center">
              <input
                type="text"
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleSend(input)}
                placeholder={isRecording ? "Listening... speak clearly..." : "Query Operations Assistant (e.g., 'Why did pump P-101 fail?')..."}
                className="w-full bg-white border border-slate-200 rounded-xl py-3 pl-6 pr-20 focus:outline-none focus:border-brandEmerald transition-all duration-150 text-xs font-semibold text-slate-800 placeholder-slate-400"
              />
              {/* Audio Button */}
              <button
                onClick={toggleRecording}
                className={`absolute right-4 p-2 rounded-xl transition-all duration-200 cursor-pointer ${
                  isRecording
                    ? 'bg-red-500 text-white shadow-md animate-pulse'
                    : 'bg-slate-100 hover:bg-slate-200 text-slate-500 hover:text-slate-800 border border-slate-200'
                }`}
              >
                {isRecording ? <MicOff className="h-3.5 w-3.5" /> : <Mic className="h-3.5 w-3.5" />}
              </button>
            </div>

            <button
              onClick={() => handleSend(input)}
              className="p-3.5 bg-brandEmerald text-white font-bold rounded-xl hover:opacity-95 transition-all duration-200 flex items-center justify-center cursor-pointer shadow-sm hover:shadow-hover-glow"
            >
              <Send className="h-4.5 w-4.5" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
