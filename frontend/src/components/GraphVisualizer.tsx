import { useState, useEffect } from 'react';
import { Database, Play, AlertTriangle, FileText, Settings, ShieldCheck, FileSpreadsheet } from 'lucide-react';
import { getApiUrl } from '../api/config';

interface Node {
  id: string;
  label: string;
  type: 'Asset' | 'Incident' | 'FailureMode' | 'Procedure' | 'Document';
  x: number;
  y: number;
}

interface Edge {
  source: string;
  target: string;
  label: string;
}

export default function GraphVisualizer() {
  const [nodes, setNodes] = useState<Node[]>([]);
  const [edges, setEdges] = useState<Edge[]>([]);
  const [traversedPath, setTraversedPath] = useState<string[]>([]);
  const [hoveredNode, setHoveredNode] = useState<Node | null>(null);

  // Fetch full schema or load mock
  useEffect(() => {
    const token = localStorage.getItem('tacitiq_token');
    fetch(getApiUrl('/api/graph/nodes'), {
      headers: { 'Authorization': `Bearer ${token}` }
    })
      .then(res => {
        if (res.status === 401 || res.status === 403) {
          throw new Error("Unauthorized");
        }
        return res.json();
      })
      .then(data => {
        if (data && data.elements) {
          parseSchema(data.elements);
        } else {
          loadMockGraph();
        }
      })
      .catch(() => loadMockGraph());
  }, []);

  const loadMockGraph = () => {
    setNodes([
      { id: "P-101", label: "Pump P-101", type: "Asset", x: 150, y: 150 },
      { id: "K-201", label: "Compressor K-201", type: "Asset", x: 150, y: 350 },
      { id: "INC-01", label: "Why 1: Bearing Spike", type: "Incident", x: 300, y: 150 },
      { id: "INC-02", label: "Why 1: LOTO Slip", type: "Incident", x: 300, y: 350 },
      { id: "FM-01", label: "Why 2: Oil Starvation", type: "FailureMode", x: 450, y: 150 },
      { id: "FM-02", label: "Why 2: Arc Hazard", type: "FailureMode", x: 450, y: 350 },
      { id: "PR-01", label: "SOP: Lubrication", type: "Procedure", x: 600, y: 150 },
      { id: "PR-02", label: "SOP: Lockout Tagout", type: "Procedure", x: 600, y: 350 }
    ]);
    setEdges([
      { source: "P-101", target: "INC-01", label: "HAS_INCIDENT" },
      { source: "K-201", target: "INC-02", label: "HAS_INCIDENT" },
      { source: "INC-01", target: "FM-01", label: "CAUSED_BY" },
      { source: "INC-02", target: "FM-02", label: "CAUSED_BY" },
      { source: "FM-01", target: "PR-01", label: "MITIGATED_BY" },
      { source: "FM-02", target: "PR-02", label: "MITIGATED_BY" }
    ]);
  };

  const parseSchema = (elements: any[]) => {
    const listNodes: Node[] = [];
    const listEdges: Edge[] = [];
    let idx = 0;
    elements.forEach((el: any) => {
      if (el.data.source) {
        listEdges.push({
          source: el.data.source,
          target: el.data.target,
          label: el.data.label
        });
      } else {
        const type = el.data.type || 'Asset';
        let x = 150;
        const y = 150 + (idx * 60) % 300;
        if (type === 'Incident') x = 300;
        else if (type === 'FailureMode') x = 450;
        else if (type === 'Procedure') x = 600;
        else if (type === 'Document') x = 750;

        listNodes.push({
          id: el.data.id,
          label: el.data.label,
          type: type as any,
          x,
          y
        });
        idx++;
      }
    });
    setNodes(listNodes);
    setEdges(listEdges);
  };

  useEffect(() => {
    const focusDoc = localStorage.getItem('focus_document');
    if (focusDoc && nodes.length > 0) {
      localStorage.removeItem('focus_document');
      const docNode = nodes.find(n => n.label === focusDoc || n.id === focusDoc);
      if (docNode) {
        const edgesFromDoc = edges.filter(e => e.source === docNode.id || e.target === docNode.id);
        const relatedAssetId = edgesFromDoc.find(e => e.target !== docNode.id)?.target || "P-101";
        
        setTraversedPath([]);
        const path = [docNode.id, relatedAssetId, "INC-01", "FM-01", "PR-01"];
        path.forEach((nodeId, idx) => {
          setTimeout(() => {
            setTraversedPath(prev => [...prev, nodeId]);
          }, idx * 600);
        });
      }
    }
  }, [nodes, edges]);

  // Trigger 5-Why path animation
  const triggerRcaTraversal = (tag: string) => {
    setTraversedPath([]);

    const path = [tag, "INC-01", "FM-01", "PR-01"];
    path.forEach((nodeId, idx) => {
      setTimeout(() => {
        setTraversedPath(prev => [...prev, nodeId]);
      }, idx * 600);
    });
  };

  // Helper to color nodes based on category
  const getNodeColor = (type: string, isHighlighted: boolean) => {
    if (isHighlighted) return '#0EA5E9'; // Azure highlights
    switch (type) {
      case 'Asset': return '#0EA5E9'; // Azure
      case 'Incident': return '#EF4444'; // Red
      case 'FailureMode': return '#F59E0B'; // Orange
      case 'Procedure': return '#10B981'; // Emerald
      case 'Document': return '#8B5CF6'; // Purple
      default: return '#64748B';
    }
  };

  return (
    <div className="h-full w-full flex flex-col md:flex-row bg-transparent">
      {/* Interactive SVG Canvas */}
      <div className="flex-1 h-full min-h-[400px] relative p-6">
        <div className="absolute top-6 left-6 p-3 bg-white border border-slate-200 shadow-sm rounded-xl text-[9px] font-extrabold text-slate-500 uppercase tracking-widest pointer-events-none flex items-center gap-2">
          <Database className="h-4 w-4 text-brandEmerald" />
          Interactive Graph Traversal
        </div>

        <svg className="w-full h-full min-h-[450px]" style={{ background: 'transparent' }}>
          <defs>
            {/* Arrow Marker */}
            <marker id="arrow" viewBox="0 0 10 10" refX="22" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
              <path d="M 0 0 L 10 5 L 0 10 z" fill="#CBD5E1" />
            </marker>
            <marker id="arrow-active" viewBox="0 0 10 10" refX="22" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
              <path d="M 0 0 L 10 5 L 0 10 z" fill="#0EA5E9" />
            </marker>
          </defs>

          {/* Render Edges (Lines) */}
          {edges.map((edge, idx) => {
            const sourceNode = nodes.find(n => n.id === edge.source);
            const targetNode = nodes.find(n => n.id === edge.target);
            if (!sourceNode || !targetNode) return null;

            const isTraversed = traversedPath.includes(edge.source) && traversedPath.includes(edge.target);

            return (
              <g key={idx}>
                <line
                  x1={sourceNode.x}
                  y1={sourceNode.y}
                  x2={targetNode.x}
                  y2={targetNode.y}
                  stroke={isTraversed ? '#0EA5E9' : '#E2E8F0'}
                  strokeWidth={isTraversed ? 2.5 : 1.5}
                  markerEnd={isTraversed ? "url(#arrow-active)" : "url(#arrow)"}
                  style={{ transition: 'all 0.5s ease' }}
                />
              </g>
            );
          })}

          {/* Render Nodes */}
          {nodes.map((node, idx) => {
            const isHighlighted = traversedPath.includes(node.id);
            const color = getNodeColor(node.type, isHighlighted);

            return (
              <g
                key={idx}
                transform={`translate(${node.x}, ${node.y})`}
                className="cursor-pointer"
                onMouseEnter={() => setHoveredNode(node)}
                onMouseLeave={() => setHoveredNode(null)}
                onClick={() => node.type === 'Asset' && triggerRcaTraversal(node.id)}
              >
                <circle
                  r={isHighlighted ? 21 : 18}
                  fill="rgba(255, 255, 255, 0.95)"
                  stroke={color}
                  strokeWidth={isHighlighted ? 3 : 1.5}
                  className="transition-all duration-300 shadow-sm"
                />
                {/* Node Icons labels */}
                <text
                  textAnchor="middle"
                  dy=".3em"
                  fill="#0F172A"
                  fontSize="9"
                  fontWeight="extrabold"
                  className="pointer-events-none select-none font-mono"
                >
                  {node.id.substring(0, 5)}
                </text>
                {/* Floating Node Title */}
                <text
                  y="36"
                  textAnchor="middle"
                  fill="#475569"
                  fontSize="9"
                  fontWeight="bold"
                  className="pointer-events-none select-none"
                >
                  {node.label}
                </text>
              </g>
            );
          })}
        </svg>

        {/* Hover Attributes Card */}
        {hoveredNode && (
          <div className="absolute bottom-6 left-6 p-4 rounded-2xl bg-white/90 backdrop-blur-md border border-slate-200 shadow-premium w-64 pointer-events-none text-[11px] text-slate-700">
            <h4 className="font-extrabold uppercase tracking-wide mb-2 text-slate-800 border-b border-slate-100 pb-1.5 flex items-center gap-2">
              {hoveredNode.type === 'Asset' && <Settings className="h-4 w-4 text-brandAzure" />}
              {hoveredNode.type === 'Incident' && <AlertTriangle className="h-4 w-4 text-brandRed" />}
              {hoveredNode.type === 'FailureMode' && <FileText className="h-4 w-4 text-brandAmber" />}
              {hoveredNode.type === 'Procedure' && <ShieldCheck className="h-4 w-4 text-brandEmerald" />}
              {hoveredNode.type === 'Document' && <FileSpreadsheet className="h-4 w-4 text-violet-500" />}
              {hoveredNode.label}
            </h4>
            <p className="font-semibold">Node ID: <span className="font-mono text-slate-800">{hoveredNode.id}</span></p>
            <p className="font-semibold mt-0.5">Ontology Label: <span className="text-slate-800 capitalize">{hoveredNode.type}</span></p>
          </div>
        )}
      </div>

      {/* Control Actions Side panel */}
      <div className="w-full md:w-80 bg-white/70 backdrop-blur-md border-t md:border-t-0 md:border-l border-slate-200/50 p-6 flex flex-col justify-between z-10">
        <div>
          <h3 className="text-xs font-extrabold uppercase tracking-wider text-slate-700 mb-4">
            Graph Queries
          </h3>
          <p className="text-xs text-slate-500 font-semibold leading-relaxed mb-6">
            Select an Asset node in the network or trigger the multi-hop RCA query simulator below to visualize path traversals:
          </p>

          <div className="space-y-3">
            <button
              onClick={() => triggerRcaTraversal("P-101")}
              className="w-full py-2.5 px-4 rounded-xl bg-white hover:bg-slate-50 border border-slate-200 text-slate-700 hover:text-slate-900 shadow-sm flex items-center gap-3 font-bold text-xs transition-all duration-200 cursor-pointer"
            >
              <Play className="h-3.5 w-3.5 text-brandAzure" />
              Trace P-101 Bearing RCA
            </button>
            <button
              onClick={() => triggerRcaTraversal("K-201")}
              className="w-full py-2.5 px-4 rounded-xl bg-white hover:bg-slate-50 border border-slate-200 text-slate-700 hover:text-slate-900 shadow-sm flex items-center gap-3 font-bold text-xs transition-all duration-200 cursor-pointer"
            >
              <Play className="h-3.5 w-3.5 text-brandAzure" />
              Trace K-201 LOTO Breach
            </button>
            <button
              onClick={() => {
                const docNode = nodes.find(n => n.type === 'Document');
                if (docNode) {
                  const edgesFromDoc = edges.filter(e => e.source === docNode.id || e.target === docNode.id);
                  const relatedAssetId = edgesFromDoc.find(e => e.target !== docNode.id)?.target || "P-101";
                  setTraversedPath([]);
                  const path = [docNode.id, relatedAssetId, "INC-01", "FM-01", "PR-01"];
                  path.forEach((nodeId, idx) => {
                    setTimeout(() => {
                      setTraversedPath(prev => [...prev, nodeId]);
                    }, idx * 600);
                  });
                } else {
                  const mockDocTitle = "Pump_P101_Maintenance_Report.pdf";
                  setNodes(prev => {
                    if (prev.some(n => n.id === mockDocTitle)) return prev;
                    return [...prev, { id: mockDocTitle, label: mockDocTitle, type: "Document", x: 80, y: 250 }];
                  });
                  setEdges(prev => {
                    if (prev.some(e => e.source === mockDocTitle)) return prev;
                    return [...prev, { source: mockDocTitle, target: "P-101", label: "REFERENCES" }];
                  });
                  setTraversedPath([]);
                  const path = [mockDocTitle, "P-101", "INC-01", "FM-01", "PR-01"];
                  path.forEach((nodeId, idx) => {
                    setTimeout(() => {
                      setTraversedPath(prev => [...prev, nodeId]);
                    }, idx * 600);
                  });
                }
              }}
              className="w-full py-2.5 px-4 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 hover:opacity-95 text-white shadow-sm flex items-center gap-3 font-bold text-xs transition-all duration-200 cursor-pointer"
            >
              <FileSpreadsheet className="h-3.5 w-3.5 text-white" />
              Trace Document Relationships
            </button>
          </div>
        </div>

        {/* Legend */}
        <div className="pt-5 border-t border-slate-200/50 mt-6 text-[10px] text-slate-500 space-y-2.5">
          <p className="font-extrabold uppercase tracking-widest text-slate-400 mb-1.5">Legend</p>
          <div className="flex items-center gap-2 font-semibold">
            <span className="h-2.5 w-2.5 rounded-full bg-brandAzure"></span> Asset Node
          </div>
          <div className="flex items-center gap-2 font-semibold">
            <span className="h-2.5 w-2.5 rounded-full bg-brandRed"></span> Incident (Why 1)
          </div>
          <div className="flex items-center gap-2 font-semibold">
            <span className="h-2.5 w-2.5 rounded-full bg-brandAmber"></span> Failure Mode (Why 2-3)
          </div>
          <div className="flex items-center gap-2 font-semibold">
            <span className="h-2.5 w-2.5 rounded-full bg-brandEmerald"></span> Safety Procedure (Why 4-5)
          </div>
          <div className="flex items-center gap-2 font-semibold">
            <span className="h-2.5 w-2.5 rounded-full bg-violet-500"></span> Document Node
          </div>
        </div>
      </div>
    </div>
  );
}
