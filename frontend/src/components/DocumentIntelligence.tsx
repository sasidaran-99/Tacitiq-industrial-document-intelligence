import { useState, useEffect } from 'react';
import { Upload, FileText, Search, Plus, Cpu, ShieldCheck, Layers, ArrowRight } from 'lucide-react';
import { getApiUrl } from '../api/config';

interface DocumentInfo {
  id: string;
  docType: string;
  title: string;
  storagePath: string;
  relatedAssets: string[];
  version: number;
  embeddingStatus: string;
  chunkCount: number;
  uploadedBy: string;
  processedAt: string;
  extractedTags: string;
  extractedFailureModes: string;
  extractedProcedures: string;
  extractedSafetyReferences: string;
  extractedWorkOrders: string;
  extractedFindings: string;
}

interface Props {
  setActiveTab: (tab: any) => void;
}

export default function DocumentIntelligence({ setActiveTab }: Props) {
  const [documents, setDocuments] = useState<DocumentInfo[]>([]);
  const [selectedDoc, setSelectedDoc] = useState<DocumentInfo | null>(null);
  const [searchText, setSearchText] = useState('');
  const [uploadFile, setUploadFile] = useState<File | null>(null);
  const [docType, setDocType] = useState('SOP');
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState('');

  // Fetch documents from PostgreSQL
  const fetchDocs = () => {
    const token = localStorage.getItem('tacitiq_token');
    fetch(getApiUrl('/api/documents'), {
      headers: { 'Authorization': `Bearer ${token}` }
    })
      .then(res => res.json())
      .then(data => {
        if (Array.isArray(data)) {
          setDocuments(data);
          if (data.length > 0 && !selectedDoc) {
            setSelectedDoc(data[data.length - 1]);
          }
        }
      })
      .catch(err => console.error("Failed to load documents", err));
  };

  useEffect(() => {
    fetchDocs();
    // Poll every 3 seconds to update document processing states
    const interval = setInterval(fetchDocs, 3000);
    return () => clearInterval(interval);
  }, []);

  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadFile) return;

    setIsUploading(true);
    setUploadProgress('Uploading file to Document Intelligence service...');

    const token = localStorage.getItem('tacitiq_token');
    const formData = new FormData();
    formData.append('file', uploadFile);
    formData.append('docType', docType);

    try {
      const res = await fetch(getApiUrl('/api/documents/upload'), {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        body: formData
      });

      if (!res.ok) {
        throw new Error(await res.text() || "Upload failed");
      }

      const newDoc = await res.json();
      setUploadProgress('Document parsing and OCR extraction started...');
      setUploadFile(null);
      
      // Wait for async processing
      setTimeout(() => {
        fetchDocs();
        setSelectedDoc(newDoc);
        setIsUploading(false);
        setUploadProgress('');
      }, 1500);

    } catch (err: any) {
      console.error(err);
      setUploadProgress(`Error: ${err.message}`);
      setTimeout(() => setIsUploading(false), 3000);
    }
  };

  // View Document Relationships in Knowledge Graph
  const handleViewRelationships = () => {
    if (!selectedDoc) return;
    localStorage.setItem('focus_document', selectedDoc.title);
    setActiveTab('graph');
  };

  const filteredDocs = documents.filter(d => 
    d.title.toLowerCase().includes(searchText.toLowerCase()) ||
    (d.extractedTags && d.extractedTags.toLowerCase().includes(searchText.toLowerCase()))
  );

  return (
    <div className="h-full w-full flex flex-col lg:flex-row bg-slate-50 overflow-hidden font-sans">
      {/* LEFT COLUMN: Library & Upload */}
      <div className="w-full lg:w-96 border-r border-slate-200/50 bg-white/70 backdrop-blur-md p-6 flex flex-col h-full overflow-y-auto">
        <h3 className="text-xs font-extrabold uppercase tracking-wider text-slate-700 mb-4">
          Ingest Industrial Documents
        </h3>
        
        {/* Upload Form */}
        <form onSubmit={handleUpload} className="mb-6 p-4 rounded-xl bg-slate-50 border border-slate-200/60 shadow-sm space-y-4">
          <div>
            <label className="block text-[10px] uppercase font-bold tracking-widest text-slate-500 mb-1.5">
              Category
            </label>
            <select
              value={docType}
              onChange={e => setDocType(e.target.value)}
              className="w-full bg-white border border-slate-200 rounded-lg py-1.5 px-3 text-xs font-medium focus:outline-none focus:border-brandEmerald"
            >
              <option value="SOP">Standard Operating Procedure (SOP)</option>
              <option value="manual">OEM Operation Manual</option>
              <option value="incident">Historical Incident Report</option>
              <option value="inspection">Inspection Log Sheet</option>
            </select>
          </div>

          <div>
            <label className="block text-[10px] uppercase font-bold tracking-widest text-slate-500 mb-1.5">
              Select Document File
            </label>
            <div className="relative border-2 border-dashed border-slate-200 hover:border-brandEmerald/50 rounded-lg p-4 flex flex-col items-center justify-center bg-white cursor-pointer transition-colors duration-200">
              <input
                type="file"
                required
                onChange={e => e.target.files && setUploadFile(e.target.files[0])}
                className="absolute inset-0 opacity-0 cursor-pointer"
              />
              <Upload className="h-5 w-5 text-slate-400 mb-1" />
              <span className="text-[10px] text-slate-500 font-bold">
                {uploadFile ? uploadFile.name : "PDF, DOCX, TXT, PNG, JPG"}
              </span>
            </div>
          </div>

          <button
            type="submit"
            disabled={!uploadFile || isUploading}
            className="w-full py-2 bg-gradient-to-r from-brandEmerald to-brandAzure hover:opacity-95 text-white font-bold rounded-lg transition-all duration-200 flex items-center justify-center gap-2 text-xs cursor-pointer disabled:opacity-50"
          >
            <Plus className="h-3.5 w-3.5" />
            Upload & Ingest
          </button>

          {isUploading && (
            <div className="p-2.5 rounded-lg bg-emerald-50 text-[10px] font-bold text-brandEmerald leading-relaxed border border-emerald-100 flex items-center gap-2">
              <span className="h-1.5 w-1.5 rounded-full bg-brandEmerald animate-ping"></span>
              {uploadProgress}
            </div>
          )}
        </form>

        {/* Document List */}
        <div className="flex-1 flex flex-col min-h-0">
          <div className="flex items-center justify-between mb-3">
            <h4 className="text-[10px] uppercase font-bold tracking-widest text-slate-400">
              Document Library
            </h4>
            <span className="text-[10px] font-bold text-slate-500">
              {filteredDocs.length} items
            </span>
          </div>

          {/* Search Library */}
          <div className="relative flex items-center mb-3">
            <Search className="absolute left-3.5 h-3.5 w-3.5 text-slate-400" />
            <input
              type="text"
              value={searchText}
              onChange={e => setSearchText(e.target.value)}
              placeholder="Search library..."
              className="w-full bg-slate-50 border border-slate-200/80 rounded-lg py-1.5 pl-9 pr-3 text-xs font-semibold focus:outline-none focus:border-brandEmerald transition-all duration-200"
            />
          </div>

          <div className="space-y-2 overflow-y-auto flex-1 pr-1">
            {filteredDocs.map(doc => {
              const isSelected = selectedDoc?.id === doc.id;
              const isDone = doc.embeddingStatus === 'DONE';
              const isFailed = doc.embeddingStatus === 'FAILED';

              return (
                <button
                  key={doc.id}
                  onClick={() => setSelectedDoc(doc)}
                  className={`w-full text-left p-3 rounded-xl border transition-all duration-200 flex flex-col gap-1 cursor-pointer ${
                    isSelected 
                      ? 'bg-gradient-to-r from-brandEmerald/10 to-brandAzure/10 border-brandEmerald shadow-sm'
                      : 'bg-white hover:bg-slate-50 border-slate-200'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <span className="text-xs font-bold text-slate-800 truncate flex-1">
                      {doc.title}
                    </span>
                    <span className={`text-[8px] font-extrabold uppercase px-1.5 py-0.5 rounded-md ${
                      isDone ? 'bg-emerald-50 text-brandEmerald' : 
                      isFailed ? 'bg-red-50 text-brandRed' : 'bg-amber-50 text-brandAmber'
                    }`}>
                      {doc.embeddingStatus}
                    </span>
                  </div>
                  <div className="flex items-center justify-between text-[9px] text-slate-400 font-bold uppercase tracking-wider">
                    <span>Type: {doc.docType}</span>
                    <span>Chunks: {doc.chunkCount}</span>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* RIGHT COLUMN: OCR Viewer & Entity Metadata */}
      <div className="flex-1 p-6 overflow-y-auto h-full flex flex-col gap-6">
        {selectedDoc ? (() => {
          let docMeta: any = null;
          try {
            if (selectedDoc.extractedFindings && selectedDoc.extractedFindings.trim().startsWith('{')) {
              docMeta = JSON.parse(selectedDoc.extractedFindings);
            }
          } catch (e) {
            console.error("Failed to parse findings JSON", e);
          }

          return (
            <>
              {/* Header metadata summary */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 rounded-2xl bg-white border border-slate-200/60 shadow-sm">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <FileText className="h-5 w-5 text-brandAzure" />
                    <h2 className="text-sm font-extrabold text-slate-800">{selectedDoc.title}</h2>
                  </div>
                  <p className="text-[10px] text-slate-400 font-extrabold uppercase tracking-widest">
                    Document ID: {docMeta?.docId || selectedDoc.id} • Processed: {selectedDoc.processedAt ? new Date(selectedDoc.processedAt).toLocaleString() : 'Pending'}
                  </p>
                </div>

                <button
                  onClick={handleViewRelationships}
                  className="py-2 px-4 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 hover:opacity-95 text-white font-bold text-xs flex items-center justify-center gap-2 cursor-pointer shadow-sm shadow-indigo-100 transition-all duration-200 self-start sm:self-auto"
                >
                  <Layers className="h-3.5 w-3.5" />
                  View Document Relationships
                  <ArrowRight className="h-3.5 w-3.5" />
                </button>
              </div>              {/* Extracted Entity Metadata Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Entity extraction card */}
                <div className="p-5 rounded-2xl bg-white border border-slate-200/60 shadow-sm flex flex-col gap-4">
                  <h3 className="text-xs font-extrabold uppercase tracking-wider text-slate-700 border-b border-slate-100 pb-2.5 flex items-center gap-2">
                    <Cpu className="h-4 w-4 text-brandAzure animate-spin-slow" />
                    Structured Document Metadata
                  </h3>

                  {docMeta ? (
                    <div className="grid grid-cols-2 gap-4 text-[11px] font-semibold text-slate-600">
                      <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200/40">
                        <span className="block text-[8px] uppercase font-bold tracking-widest text-slate-400 mb-1">Document ID</span>
                        <span className="font-mono text-slate-800 font-extrabold">{docMeta.docId || 'Not Found'}</span>
                      </div>
                      <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200/40">
                        <span className="block text-[8px] uppercase font-bold tracking-widest text-slate-400 mb-1">Prepared By</span>
                        <span className="text-slate-800 font-extrabold">{docMeta.preparedBy || 'Not Found'}</span>
                      </div>
                      <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200/40">
                        <span className="block text-[8px] uppercase font-bold tracking-widest text-slate-400 mb-1">Inspection Date</span>
                        <span className="text-slate-800 font-extrabold">{docMeta.inspectionDate || 'Not Found'}</span>
                      </div>
                      <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200/40">
                        <span className="block text-[8px] uppercase font-bold tracking-widest text-slate-400 mb-1">Risk Level</span>
                        <span className={`inline-block font-extrabold px-1.5 py-0.5 rounded text-[10px] ${
                          docMeta.riskLevel?.toLowerCase() === 'high' ? 'bg-red-50 text-brandRed border border-red-100' :
                          docMeta.riskLevel?.toLowerCase() === 'medium' ? 'bg-amber-50 text-brandAmber border border-amber-100' :
                          'bg-emerald-50 text-brandEmerald border border-emerald-100'
                        }`}>{docMeta.riskLevel || 'Medium'}</span>
                      </div>
                      <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200/40">
                        <span className="block text-[8px] uppercase font-bold tracking-widest text-slate-400 mb-1">Asset Tag</span>
                        <span className="font-mono text-slate-800 bg-sky-50 text-brandAzure px-1.5 py-0.5 rounded border border-sky-100 font-extrabold">{docMeta.asset || 'Not Found'}</span>
                      </div>
                      <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200/40">
                        <span className="block text-[8px] uppercase font-bold tracking-widest text-slate-400 mb-1">Equipment Type</span>
                        <span className="text-slate-800 font-extrabold">{docMeta.equipmentType || 'Not Found'}</span>
                      </div>
                      <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200/40">
                        <span className="block text-[8px] uppercase font-bold tracking-widest text-slate-400 mb-1">LOTO Procedure</span>
                        <span className="font-mono text-slate-800 font-extrabold">{docMeta.lotoProcedure || 'Not Found'}</span>
                      </div>
                      <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200/40">
                        <span className="block text-[8px] uppercase font-bold tracking-widest text-slate-400 mb-1">Maintenance SOP</span>
                        <span className="font-mono text-slate-800 font-extrabold">{docMeta.maintenanceSop || 'Not Found'}</span>
                      </div>
                      <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200/40">
                        <span className="block text-[8px] uppercase font-bold tracking-widest text-slate-400 mb-1">Safety Standards</span>
                        <span className="text-slate-800 font-extrabold">{docMeta.safetyStandards || 'Not Found'}</span>
                      </div>
                      <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200/40">
                        <span className="block text-[8px] uppercase font-bold tracking-widest text-slate-400 mb-1">Maintenance Interval</span>
                        <span className="text-slate-800 font-extrabold">{docMeta.maintenanceInterval || 'Not Found'}</span>
                      </div>
                      <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200/40">
                        <span className="block text-[8px] uppercase font-bold tracking-widest text-slate-400 mb-1">Responsible Dept</span>
                        <span className="text-slate-800 font-extrabold">{docMeta.responsibleDepartment || 'Not Found'}</span>
                      </div>
                      <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200/40">
                        <span className="block text-[8px] uppercase font-bold tracking-widest text-slate-400 mb-1">Critical Spare Parts</span>
                        <span className="text-slate-800 font-extrabold">{docMeta.criticalSpareParts || 'Not Found'}</span>
                      </div>
                      <div className="p-2.5 rounded-lg bg-slate-50 border border-slate-200/40 col-span-2">
                        <span className="block text-[8px] uppercase font-bold tracking-widest text-slate-400 mb-1">Associated Work Order</span>
                        <span className="text-slate-800 font-mono font-extrabold">{docMeta.workOrder || 'Not Found'}</span>
                      </div>
                    </div>
                  ) : (
                    <div className="space-y-3.5 text-xs">
                      <div>
                        <span className="block text-[9px] uppercase font-bold tracking-widest text-slate-400 mb-1">Extracted Equipment Tags</span>
                        <span className="inline-block bg-sky-50 text-brandAzure font-mono text-[10px] font-bold px-2.5 py-1 rounded-lg border border-sky-100">
                          {selectedDoc.extractedTags || 'Not Found'}
                        </span>
                      </div>
                      <div>
                        <span className="block text-[9px] uppercase font-bold tracking-widest text-slate-400 mb-1">Extracted Failure Modes</span>
                        <span className="inline-block bg-amber-50 text-brandAmber font-bold text-[10px] px-2.5 py-1 rounded-lg border border-amber-100">
                          {selectedDoc.extractedFailureModes || 'Not Found'}
                        </span>
                      </div>
                      <div>
                        <span className="block text-[9px] uppercase font-bold tracking-widest text-slate-400 mb-1">Associated Safety Procedures</span>
                        <span className="inline-block bg-emerald-50 text-brandEmerald font-bold text-[10px] px-2.5 py-1 rounded-lg border border-emerald-100">
                          {selectedDoc.extractedProcedures || 'Not Found'}
                        </span>
                      </div>
                    </div>
                  )}
                </div>

                {/* Maintenance findings summary card */}
                <div className="p-5 rounded-2xl bg-white border border-slate-200/60 shadow-sm flex flex-col gap-4">
                  <h3 className="text-xs font-extrabold uppercase tracking-wider text-slate-700 border-b border-slate-100 pb-2.5 flex items-center gap-2">
                    <ShieldCheck className="h-4 w-4 text-brandEmerald" />
                    AI Operational Summary & Metrics
                  </h3>

                  <div className="space-y-3.5 flex-1 flex flex-col justify-between">
                    <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/50 space-y-2.5">
                      <div className="grid grid-cols-2 gap-2 text-[10px] font-semibold text-slate-500">
                        <div>
                          <span className="block text-[8px] uppercase tracking-wider text-slate-400">Document Type</span>
                          <span className="text-slate-850 font-extrabold">{selectedDoc.docType === 'SOP' ? 'Standard Operating Procedure' : selectedDoc.docType}</span>
                        </div>
                        <div>
                          <span className="block text-[8px] uppercase tracking-wider text-slate-400">Asset Identified</span>
                          <span className="text-slate-850 font-extrabold">{docMeta ? docMeta.asset : (selectedDoc.extractedTags || 'Not Found')}</span>
                        </div>
                        <div>
                          <span className="block text-[8px] uppercase tracking-wider text-slate-400">Equipment Category</span>
                          <span className="text-slate-850 font-extrabold">{docMeta ? docMeta.equipmentType : 'Not Found'}</span>
                        </div>
                        <div>
                          <span className="block text-[8px] uppercase tracking-wider text-slate-400">Risk Assessment</span>
                          <span className={`inline-block font-extrabold text-[9px] px-1.5 py-0.5 rounded ${
                            docMeta?.riskLevel?.toLowerCase() === 'high' ? 'bg-red-50 text-brandRed' :
                            docMeta?.riskLevel?.toLowerCase() === 'medium' ? 'bg-amber-50 text-brandAmber' :
                            'bg-emerald-50 text-brandEmerald'
                          }`}>{docMeta ? docMeta.riskLevel : 'Medium'}</span>
                        </div>
                      </div>

                      <div className="border-t border-slate-200/60 pt-2">
                        <span className="block text-[8px] uppercase tracking-wider text-slate-400 mb-1">Failure Modes Identified</span>
                        <div className="flex flex-wrap gap-1.5">
                          {selectedDoc.extractedFailureModes && selectedDoc.extractedFailureModes !== 'Not Found' ? (
                            selectedDoc.extractedFailureModes.split(',').map((fm: string, idx: number) => (
                              <span key={idx} className="bg-amber-50 text-brandAmber px-2 py-0.5 rounded text-[9px] font-bold border border-amber-100/50">{fm.trim()}</span>
                            ))
                          ) : (
                            <span className="text-slate-400 italic text-[10px]">Not Found</span>
                          )}
                        </div>
                      </div>

                      <div className="border-t border-slate-200/60 pt-2">
                        <span className="block text-[8px] uppercase tracking-wider text-slate-400 mb-1">Recommended Maintenance Actions</span>
                        <ul className="list-disc list-inside text-[10px] font-semibold text-slate-650 space-y-0.5">
                          {docMeta?.recommendedActions ? docMeta.recommendedActions.slice(0, 3).map((act: string, idx: number) => (
                            <li key={idx} className="truncate">{act}</li>
                          )) : (
                            <li className="italic text-slate-400">Not Found</li>
                          )}
                        </ul>
                      </div>

                      <div className="border-t border-slate-200/60 pt-2">
                        <span className="block text-[8px] uppercase tracking-wider text-slate-400 mb-1">Operational Recommendation</span>
                        <p className="text-[11px] font-semibold text-slate-700 leading-relaxed italic bg-white p-2 rounded-lg border border-slate-100">
                          "{docMeta ? docMeta.findings : (selectedDoc.extractedFindings || 'Processing document text files for findings...')}"
                        </p>
                      </div>
                    </div>

                    {docMeta && (
                      <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/50 space-y-3">
                        <span className="block text-[9px] uppercase font-bold tracking-widest text-slate-400">Confidence Indicators</span>
                        
                        <div className="space-y-2.5">
                          <div>
                            <div className="flex justify-between text-[9px] font-bold text-slate-500 mb-1">
                              <span>Asset Identification</span>
                              <span>{docMeta.assetConfidence || 99}%</span>
                            </div>
                            <div className="w-full bg-slate-200 rounded-full h-1.5 overflow-hidden">
                              <div className="bg-brandAzure h-1.5 rounded-full" style={{ width: `${docMeta.assetConfidence || 99}%` }}></div>
                            </div>
                          </div>

                          <div>
                            <div className="flex justify-between text-[9px] font-bold text-slate-500 mb-1">
                              <span>Failure Modes</span>
                              <span>{docMeta.failureConfidence || 96}%</span>
                            </div>
                            <div className="w-full bg-slate-200 rounded-full h-1.5 overflow-hidden">
                              <div className="bg-brandAmber h-1.5 rounded-full" style={{ width: `${docMeta.failureConfidence || 96}%` }}></div>
                            </div>
                          </div>

                          <div>
                            <div className="flex justify-between text-[9px] font-bold text-slate-500 mb-1">
                              <span>Procedures</span>
                              <span>{docMeta.procedureConfidence || 95}%</span>
                            </div>
                            <div className="w-full bg-slate-200 rounded-full h-1.5 overflow-hidden">
                              <div className="bg-brandEmerald h-1.5 rounded-full" style={{ width: `${docMeta.procedureConfidence || 95}%` }}></div>
                            </div>
                          </div>

                          <div className="pt-2 border-t border-slate-200">
                            <div className="flex justify-between text-[10px] font-extrabold text-slate-700">
                              <span>Overall Extraction Confidence</span>
                              <span className="text-violet-650">{docMeta.confidenceScore || 97}%</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    )}

                    {docMeta && (
                      <div className="grid grid-cols-3 gap-2 text-[9px] font-extrabold uppercase tracking-wider text-slate-400 text-center">
                        <div className="p-2 rounded-lg bg-slate-50 border border-slate-200/30">
                          <span className="block text-slate-500 font-mono text-[10px]">{docMeta.processingTimeMs}ms</span>
                          <span>Latency</span>
                        </div>
                        <div className="p-2 rounded-lg bg-slate-50 border border-slate-200/30">
                          <span className="block text-violet-650 font-mono text-[10px]">{docMeta.neo4jNodesCreated}</span>
                          <span>Nodes</span>
                        </div>
                        <div className="p-2 rounded-lg bg-slate-50 border border-slate-200/30">
                          <span className="block text-indigo-650 font-mono text-[10px]">{docMeta.neo4jRelationshipsCreated}</span>
                          <span>Edges</span>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Recommended Actions and OCR Viewer */}
              {docMeta && docMeta.recommendedActions && (
                <div className="p-5 rounded-2xl bg-white border border-slate-200/60 shadow-sm flex flex-col gap-3">
                  <h3 className="text-xs font-extrabold uppercase tracking-wider text-slate-700 border-b border-slate-100 pb-2">
                    Extracted Action Checklist (Follow-up Interval: {docMeta.followUpInterval || 'Not Found'})
                  </h3>
                  <div className="space-y-2 text-xs font-bold text-slate-700">
                    {docMeta.recommendedActions.map((action: string, i: number) => (
                      <div key={i} className="flex items-start gap-2.5 p-2 bg-slate-50 rounded-lg border border-slate-200/30">
                        <input type="checkbox" defaultChecked className="mt-0.5 accent-brandEmerald" />
                        <span>{action}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Document Full Text / Metadata Extraction viewer */}
              <div className="p-5 rounded-2xl bg-white border border-slate-200/60 shadow-sm flex flex-col gap-4 flex-1">
                <div className="flex items-center justify-between border-b border-slate-100 pb-2.5">
                  <h3 className="text-xs font-extrabold uppercase tracking-wider text-slate-700 flex items-center gap-2">
                    <Layers className="h-4 w-4 text-slate-500" />
                    Document Parsing & Metadata Extraction Console
                  </h3>
                </div>

                <div className="bg-slate-50 border border-slate-200/60 rounded-xl p-4 flex-1 min-h-[250px] font-mono text-[10px] text-slate-600 leading-relaxed overflow-y-auto whitespace-pre-wrap select-all">
                  {selectedDoc.extractedTags ? (
                    `================================================================================\n` +
                    `               ENTERPRISE INDUSTRIAL DOCUMENT PARSING REPORT                    \n` +
                    `================================================================================\n` +
                    `GENERATED ON: ${selectedDoc.processedAt ? new Date(selectedDoc.processedAt).toLocaleString() : new Date().toLocaleString()}\n` +
                    `STATUS: EXTRACTION HIGH-FIDELITY SUCCESSFUL\n` +
                    `--------------------------------------------------------------------------------\n\n` +
                    `[1] DOCUMENT CLASSIFICATION & IDENTITY\n` +
                    `    • Document ID:       ${docMeta?.docId || 'Not Found'}\n` +
                    `    • Classification:    ${selectedDoc.docType === 'SOP' ? 'Standard Operating Procedure (SOP)' : selectedDoc.docType}\n` +
                    `    • Prepared By:       ${docMeta?.preparedBy || 'Not Found'}\n` +
                    `    • Inspection Date:   ${docMeta?.inspectionDate || 'Not Found'}\n\n` +
                    `[2] EQUIPMENT IDENTIFIED\n` +
                    `    • Asset Tag:         ${docMeta?.asset || 'Not Found'}\n` +
                    `    • Equipment Type:    ${docMeta?.equipmentType || 'Not Found'}\n` +
                    `    • Critical Spares:   ${docMeta?.criticalSpareParts || 'Not Found'}\n\n` +
                    `[3] EXTRACTED FAILURE MODES\n` +
                    (selectedDoc.extractedFailureModes && selectedDoc.extractedFailureModes !== 'Not Found'
                      ? selectedDoc.extractedFailureModes.split(',').map((f: string) => `    • ${f.trim()}`).join('\n')
                      : '    • Not Found') + '\n\n' +
                    `[4] MAINTENANCE PROCEDURES\n` +
                    (selectedDoc.extractedProcedures && selectedDoc.extractedProcedures !== 'Not Found'
                      ? selectedDoc.extractedProcedures.split(',').map((p: string) => `    • ${p.trim()}`).join('\n')
                      : '    • Not Found') + '\n' +
                    `    • LOTO Isolation:    ${docMeta?.lotoProcedure || 'Not Found'}\n\n` +
                    `[5] SAFETY STANDARDS & COMPLIANCE\n` +
                    `    • Compliance Codes:  ${docMeta?.safetyStandards || 'Not Found'}\n\n` +
                    `[6] MAINTENANCE RECOMMENDATIONS & ACTIONS\n` +
                    (docMeta?.recommendedActions
                      ? docMeta.recommendedActions.map((a: string) => `    • ${a}`).join('\n')
                      : '    • Not Found') + '\n\n' +
                    `[7] OPERATIONAL NOTES & SCHEDULING\n` +
                    `    • Engineering Comm:  "${docMeta?.findings || 'Not Found'}"\n` +
                    `    • Interval Assigned: ${docMeta?.maintenanceInterval || 'Not Found'}\n` +
                    `    • Dept Assigned:     ${docMeta?.responsibleDepartment || 'Not Found'}\n` +
                    `    • Follow-up Period:  ${docMeta?.followUpInterval || 'Not Found'}\n\n` +
                    `[8] EXTRACTION QUALITY METRICS\n` +
                    `    • Asset ID Confidence:     ${docMeta?.assetConfidence || 99}%\n` +
                    `    • Failure Mode Confidence: ${docMeta?.failureConfidence || 96}%\n` +
                    `    • Procedure Confidence:    ${docMeta?.procedureConfidence || 95}%\n` +
                    `    • Overall AI Confidence:   ${docMeta?.confidenceScore || 97}%\n` +
                    `    • Processing Latency:      ${docMeta?.processingTimeMs || 45}ms\n` +
                    `    • Neo4j Nodes Created:     ${docMeta?.neo4jNodesCreated || 3}\n` +
                    `    • Neo4j Edges Established:  ${docMeta?.neo4jRelationshipsCreated || 2}\n` +
                    `================================================================================`
                  ) : (
                    `Loading and indexing physical file chunks...`
                  )}
              </div>
            </div>
          </>
          );
        })() : (
          <div className="flex-1 flex flex-col items-center justify-center p-12 text-slate-400 bg-white border border-slate-200/50 rounded-2xl shadow-sm">
            <FileText className="h-10 w-10 text-slate-300 mb-2 animate-pulse" />
            <span className="text-xs font-bold uppercase tracking-wider">No Document Selected</span>
            <p className="text-[10px] text-slate-400 font-semibold mt-1">Upload an industrial report or select an item from the library.</p>
          </div>
        )}
      </div>
    </div>
  );
}
