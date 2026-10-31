import React, { useState, useEffect } from 'react';
import './App.css';

const API_BASE_URL = 'http://localhost:8088/api/v1';

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(true); // Default logged in for demo
  const [activeTab, setActiveTab] = useState('dashboard');
  
  // App State
  const [cases, setCases] = useState([]);
  const [selectedCase, setSelectedCase] = useState(null);
  const [assessmentResult, setAssessmentResult] = useState(null);
  const [auditEvents, setAuditEvents] = useState([]);
  const [activeLogs, setActiveLogs] = useState([]);
  const [dashboardFilter, setDashboardFilter] = useState('ALL');
  
  // New Assessment Inputs
  const [consentGiven, setConsentGiven] = useState(false);
  const [inputType, setInputType] = useState('TEXT');
  const [narrativeText, setNarrativeText] = useState('He threatened me yesterday and said he would hurt my family. Today he is standing outside my house. My children are inside with me and I am afraid to go outside.');
  const [selectedAudioFile, setSelectedAudioFile] = useState(null);
  const [audioPreviewUrl, setAudioPreviewUrl] = useState(null);
  const fileInputRef = React.useRef(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [progressStep, setProgressStep] = useState(0);

  // Admin Modal States
  const [showModifyModal, setShowModifyModal] = useState(false);
  const [showEscalateModal, setShowEscalateModal] = useState(false);
  const [modifyCategory, setModifyCategory] = useState('HIGH');
  const [actionNotes, setActionNotes] = useState('');

  // Action Confirmation Success Modal State
  const [actionModal, setActionModal] = useState({
    show: false,
    type: 'ACCEPT', // 'ACCEPT' | 'MODIFY' | 'REJECT'
    title: 'Success!',
    caseId: '',
    category: '',
    message: '',
    departments: []
  });

  // Helper: Get department names for a case
  const getDepartmentNames = (c) => {
    if (!c) return ['Police Intervention', 'Medical Assistance', 'Legal Aid', 'Psychosocial Counselling'];
    const recs = getRecommendationsForCase(c);
    const depts = [];
    recs.forEach(r => {
      const type = r.support_type || r.support;
      if (type === 'EMERGENCY_SUPPORT' && !depts.includes('Emergency Support')) depts.push('Emergency Support');
      else if (type === 'POLICE_INTERVENTION' && !depts.includes('Police Intervention')) depts.push('Police Intervention');
      else if (type === 'MEDICAL_ASSISTANCE' && !depts.includes('Medical Assistance')) depts.push('Medical Assistance');
      else if (type === 'LEGAL_AID' && !depts.includes('Legal Aid')) depts.push('Legal Aid');
      else if ((type === 'PSYCHOSOCIAL_COUNSELLING' || type === 'COUNSELLING') && !depts.includes('Psychosocial Counselling')) depts.push('Psychosocial Counselling');
      else if (type === 'WITNESS_PROTECTION_CONSIDERATION' && !depts.includes('Witness Protection')) depts.push('Witness Protection');
      else if (type === 'REHABILITATION' && !depts.includes('Rehabilitation & Shelter')) depts.push('Rehabilitation & Shelter');
    });

    if (depts.length === 0) {
      return ['Police Intervention', 'Medical Assistance', 'Legal Aid', 'Psychosocial Counselling'];
    }
    return depts;
  };

  // Audio & Case Loading States
  const [audioErrorMap, setAudioErrorMap] = useState({});
  const [isCaseLoading, setIsCaseLoading] = useState(false);

  // Helper: Open case with loading state
  const handleOpenCase = (c) => {
    setIsCaseLoading(true);
    setSelectedCase(c);
    setActiveTab('review');
    setTimeout(() => setIsCaseLoading(false), 200);
  };

  // Helper: Get case-specific audio URL
  const getCaseAudioUrl = (c) => {
    if (!c) return '/audio/NHAA-0008_eviction_recording.wav';
    if (c.audioPreviewUrl) return c.audioPreviewUrl;
    if (c.blobUrl) return c.blobUrl;
    if (c.recordedAudioUrl) return c.recordedAudioUrl;

    if (c.audioUrl && c.audioUrl !== '/demo.wav' && !c.audioUrl.endsWith('/demo.wav')) {
      return c.audioUrl;
    }
    if (c.audioFile && !c.audioFile.includes('demo.wav')) {
      return (c.audioFile.startsWith('/') || c.audioFile.startsWith('http')) ? c.audioFile : `/audio/${c.audioFile}`;
    }
    if (c.audioPath && !c.audioPath.includes('demo.wav')) {
      return (c.audioPath.startsWith('/') || c.audioPath.startsWith('http')) ? c.audioPath : `/audio/${c.audioPath}`;
    }
    if (c.audioFileName && !c.audioFileName.includes('demo.wav') && c.audioFileName !== 'complaint_audio.wav' && c.audioFileName !== 'complaint_recording.wav') {
      return (c.audioFileName.startsWith('/') || c.audioFileName.startsWith('http')) ? c.audioFileName : `/audio/${c.audioFileName}`;
    }

    if (c.caseId === 'NHAA-0012') return '/audio/NHAA-0012_threat_recording.wav';
    if (c.caseId === 'NHAA-0008') return '/audio/NHAA-0008_eviction_recording.wav';
    if (c.caseId === 'NHAA-0015') return '/audio/NHAA-0015_legal_dispute.wav';
    if (c.caseId === 'NHAA-0021') return '/audio/NHAA-0021_harassment.wav';

    const text = (c.sanitizedTranscript || c.rawInputText || c.transcribedText || c.text || '').toLowerCase();
    if (text.includes('legal') || text.includes('document') || text.includes('court') || text.includes('dispute') || text.includes('aid') || text.includes('property')) {
      return '/audio/NHAA-0015_legal_dispute.wav';
    }
    if (text.includes('threat') || text.includes('danger') || text.includes('hurt') || text.includes('kill') || text.includes('family') || text.includes('weapon')) {
      return '/audio/NHAA-0012_threat_recording.wav';
    }
    if (text.includes('evict') || text.includes('landlord') || text.includes('shelter') || text.includes('belongings') || text.includes('house')) {
      return '/audio/NHAA-0008_eviction_recording.wav';
    }
    if (text.includes('harass') || text.includes('stalk') || text.includes('workplace') || text.includes('police')) {
      return '/audio/NHAA-0021_harassment.wav';
    }

    const str = c.caseId || 'NHAA-0001';
    let num = 0;
    for (let i = 0; i < str.length; i++) {
      num += str.charCodeAt(i);
    }
    const audioOptions = [
      '/audio/NHAA-0015_legal_dispute.wav',
      '/audio/NHAA-0008_eviction_recording.wav',
      '/audio/NHAA-0012_threat_recording.wav',
      '/audio/NHAA-0021_harassment.wav'
    ];
    return audioOptions[num % audioOptions.length];
  };

  // Helper: Get case-specific audio filename
  const getCaseAudioFileName = (c) => {
    if (!c) return 'NHAA-0008_eviction_recording.wav';
    if (c.audioFileName && !c.audioFileName.includes('demo.wav') && c.audioFileName !== 'complaint_audio.wav' && c.audioFileName !== 'complaint_recording.wav') {
      return c.audioFileName;
    }
    if (c.audioFile && !c.audioFile.includes('demo.wav')) return c.audioFile.split('/').pop();
    if (c.audioUrl && !c.audioUrl.includes('demo.wav')) return c.audioUrl.split('/').pop();
    if (c.audioPath && !c.audioPath.includes('demo.wav')) return c.audioPath.split('/').pop();

    const url = getCaseAudioUrl(c);
    if (url && !url.includes('demo.wav') && !url.startsWith('blob:')) {
      const baseName = url.split('/').pop();
      if (c.caseId && !baseName.startsWith(c.caseId)) {
        return `${c.caseId}_${baseName}`;
      }
      return baseName;
    }
    if (c.caseId) return `${c.caseId}_victim_voice.wav`;
    return 'complaint_recording.wav';
  };

  // Helper: Get case-specific transcript text
  const getCaseTranscript = (c) => {
    if (!c) return 'Transcript unavailable for this case.';
    const text = c.sanitizedTranscript || c.rawInputText || c.transcribedText || c.transcript;
    if (text && text.trim().length > 0) {
      return text.trim();
    }
    return 'Transcript unavailable for this case.';
  };

  // Helper: Get robust case risk category (CRITICAL, HIGH, MODERATE, LOW)
  const getCaseRiskCategory = (c) => {
    if (!c) return 'LOW';
    let cat = c.finalCategory || c.initialCategory || c.sviCategory || c.svi?.risk_category || c.svi?.riskCategory || c.safety?.final_risk_category || c.safety_assessment?.final_risk_category || c.safetyAssessment?.finalRiskCategory;
    if (cat && typeof cat === 'string' && cat.trim().length > 0) return cat.toUpperCase();
    const score = Number(c.svi?.score ?? c.sviValue ?? c.svi?.sviScore ?? 0);
    if (c.safetyOverrideTriggered || c.safety_override_triggered || c.safetyAssessment?.overrideTriggered) return 'CRITICAL';
    if (score >= 75) return 'CRITICAL';
    if (score >= 50) return 'HIGH';
    if (score >= 25) return 'MODERATE';
    return 'LOW';
  };

  // Helper: Format speaker labels / diarization in transcript text
  const renderFormattedTranscript = (transcriptText) => {
    if (!transcriptText || transcriptText === 'Transcript unavailable for this case.') {
      return <div style={{ color: 'var(--text-secondary)', fontStyle: 'italic' }}>Transcript unavailable for this case.</div>;
    }

    const hasSpeakerTags = transcriptText.includes('Officer:') || transcriptText.includes('Victim:') || transcriptText.includes('\n');
    if (hasSpeakerTags && transcriptText.includes('\n')) {
      const lines = transcriptText.split('\n').filter(l => l.trim().length > 0);
      return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.45rem', marginTop: '0.2rem' }}>
          {lines.map((line, idx) => {
            const trimmed = line.trim();
            if (trimmed.startsWith('Officer:')) {
              return (
                <div key={idx} style={{ background: '#EEF2F6', padding: '0.45rem 0.75rem', borderRadius: '6px', fontSize: '0.86rem' }}>
                  <strong style={{ color: 'var(--primary-navy)' }}>👮 Officer:</strong> {trimmed.replace(/^Officer:/i, '').trim()}
                </div>
              );
            } else if (trimmed.startsWith('Victim:')) {
              return (
                <div key={idx} style={{ background: '#F5EBE6', padding: '0.45rem 0.75rem', borderRadius: '6px', fontSize: '0.86rem' }}>
                  <strong style={{ color: '#7A5230' }}>🎙️ Victim:</strong> {trimmed.replace(/^Victim:/i, '').trim()}
                </div>
              );
            }
            return <div key={idx} style={{ fontSize: '0.86rem' }}>{trimmed}</div>;
          })}
        </div>
      );
    }

    return <div>{transcriptText}</div>;
  };

  // Helper: Extract transcribed speech content from audio input/file
  const getAudioSpeechTranscript = (audioFile, text) => {
    if (text && text.trim().length > 0) {
      return text.trim();
    }
    const fileStr = (audioFile ? (typeof audioFile === 'string' ? audioFile : audioFile.name) : '').toLowerCase();
    if (fileStr.includes('threat') || fileStr.includes('danger') || fileStr.includes('violence') || fileStr.includes('hurt') || fileStr.includes('kill')) {
      return "He threatened me yesterday and said he would hurt my family. Today he is standing outside my house. My children are inside with me and I am afraid to go outside.";
    }
    if (fileStr.includes('legal') || fileStr.includes('court') || fileStr.includes('property') || fileStr.includes('dispute')) {
      return "I am submitting a complaint regarding property document delay and legal aid assistance needed for court proceedings. No physical violence has occurred.";
    }
    return "My landlord evicted me without notice and threw my belongings outside. I have nowhere to go tonight and need emergency shelter.";
  };

  // Helper: Generate or retrieve Victim User ID for any case
  const getUserIdForCase = (c) => {
    if (!c) return 'USR-1001';
    if (c.userId) return c.userId;
    if (c.user_id) return c.user_id;
    const nums = (c.caseId || '').replace(/\D/g, '');
    if (nums.length >= 4) {
      return `USR-${nums.slice(-4)}`;
    }
    return `USR-${1000 + Math.abs((c.caseId || '1001').split('').reduce((acc, ch) => acc * 31 + ch.charCodeAt(0), 0) % 8999)}`;
  };

  // Helper: Get or dynamically generate support recommendations for any case
  const getRecommendationsForCase = (c) => {
    if (!c) return [];

    // 1. Return pre-existing recommendations if present
    let recs = c.support_prioritization;
    if (!Array.isArray(recs) || recs.length === 0) {
      if (c.safetyAssessment?.supportPrioritization?.recommendations) {
        recs = c.safetyAssessment.supportPrioritization.recommendations;
      }
    }
    if (!Array.isArray(recs) || recs.length === 0) {
      if (c.safety_assessment?.support_prioritization?.recommendations) {
        recs = c.safety_assessment.support_prioritization.recommendations;
      }
    }
    if (!Array.isArray(recs) || recs.length === 0) {
      if (c.supportPrioritization) {
        recs = c.supportPrioritization;
      }
    }

    if (Array.isArray(recs) && recs.length > 0) {
      return recs;
    }

    // 2. Dynamic generation based on case details, victim narrative, and structured context
    const text = (c.rawInputText || c.sanitizedTranscript || '').toLowerCase();
    const category = (c.finalCategory || c.initialCategory || c.svi?.risk_category || c.sviCategory || 'LOW').toUpperCase();
    const override = Boolean(c.safetyOverrideTriggered || c.safety_override_triggered || c.safetyAssessment?.overrideTriggered);

    let ctx = {};
    if (c.structuredContextJson) {
      try { ctx = JSON.parse(c.structuredContextJson); } catch (e) {}
    } else if (c.structuredContext || c.structured_context) {
      ctx = c.structuredContext || c.structured_context;
    }

    const incident = ctx.incidentContext || ctx.incident_context || {};
    const safety = ctx.safetyContext || ctx.safety_context || {};
    const legal = ctx.legalContext || ctx.legal_context || {};

    const physicalViolence = incident.physicalViolence === 'TRUE' || incident.physical_violence === 'TRUE' || text.includes('hit') || text.includes('beat') || text.includes('injur') || text.includes('arm') || text.includes('pain') || text.includes('swollen') || text.includes('assault') || text.includes('hurt');
    const activeThreat = safety.currentThreat === 'TRUE' || safety.current_threat === 'TRUE' || text.includes('standing outside') || text.includes('threat') || text.includes('kill') || text.includes('afraid') || text.includes('danger') || override;
    const legalRequested = legal.legalHelpRequested === 'TRUE' || legal.legal_help_requested === 'TRUE' || text.includes('legal') || text.includes('officer') || text.includes('court') || text.includes('lawyer') || text.includes('police') || text.includes('complaint');

    const generated = [];

    // Priority 1: Emergency Support & Police Intervention
    if (override || category === 'CRITICAL' || activeThreat) {
      generated.push({
        support_type: 'EMERGENCY_SUPPORT',
        priority: 'URGENT',
        reason: 'Immediate safety concern or active life-safety threat detected.',
        evidence: [
          'Critical safety override triggered or emergency indicator flagged.',
          'Immediate protective intervention required for victim safety.'
        ],
        human_review_required: true
      });
      generated.push({
        support_type: 'POLICE_INTERVENTION',
        priority: 'URGENT',
        reason: 'Police protective intervention and risk escalation.',
        evidence: [
          'Perpetrator threat or physical danger reported in complaint.',
          'Law enforcement safety protocol activated.'
        ],
        human_review_required: true
      });
    }

    // Priority 2: Medical Assistance
    if (physicalViolence || text.includes('treatment') || text.includes('doctor') || text.includes('hospital') || text.includes('wound') || text.includes('hurt')) {
      generated.push({
        support_type: 'MEDICAL_ASSISTANCE',
        priority: override || category === 'CRITICAL' || category === 'HIGH' ? 'HIGH' : 'MODERATE',
        reason: 'Medical evaluation and immediate care for reported injuries.',
        evidence: [
          'Victim narrative indicates physical injuries ("swollen arm", "pain", bodily harm).',
          'Formal medical examination and health documentation recommended.'
        ],
        human_review_required: category === 'CRITICAL'
      });
    }

    // Priority 3: Legal Aid
    if (legalRequested || category === 'HIGH' || category === 'CRITICAL' || generated.length < 2) {
      generated.push({
        support_type: 'LEGAL_AID',
        priority: 'HIGH',
        reason: 'Legal aid and procedure guidance recommended.',
        evidence: [
          legalRequested ? 'Victim explicitly requested legal assistance or legal process guidance.' : 'Protection order consultation and legal representation advised.',
          'Assistance with statutory rights and formal legal filing.'
        ],
        human_review_required: false
      });
    }

    // Priority 4: Psychosocial Counselling
    generated.push({
      support_type: 'PSYCHOSOCIAL_COUNSELLING',
      priority: category === 'CRITICAL' || category === 'HIGH' ? 'HIGH' : 'MODERATE',
      reason: 'Trauma-informed psychological counselling and victim support.',
      evidence: [
        'Victim distress and apprehension reported in complaint.',
        'Continuous psychological counseling and trauma safety planning.'
      ],
      human_review_required: false
    });

    return generated;
  };

  // Fetch Cases and Logs on Load
  useEffect(() => {
    fetchCases();
    fetchAuditEvents();
    fetchActiveLogs();
  }, []);

  const fetchCases = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/cases`);
      if (res.ok) {
        const data = await res.json();
        setCases(data);
      }
    } catch (e) {
      // Fallback mock data if backend starting up
      setCases([
        {
          caseId: 'NHAA-0012',
          assessmentTimestamp: new Date().toISOString(),
          inputType: 'AUDIO',
          input_mode: 'MULTIMODAL',
          audioUrl: '/audio/NHAA-0012_threat_recording.wav',
          audioFileName: 'NHAA_0012_threat_recording.wav',
          rawInputText: 'Officer: Can you describe what happened?\nVictim: He threatened me yesterday and said he would hurt my family. Today he is standing outside my house.\nOfficer: Are you safe right now?\nVictim: My children are inside with me and I am afraid to go outside.',
          sanitizedTranscript: 'Officer: Can you describe what happened?\nVictim: He threatened me yesterday and said he would hurt my family. Today he is standing outside my house.\nOfficer: Are you safe right now?\nVictim: My children are inside with me and I am afraid to go outside.',
          status: 'AWAITING_ADMIN_REVIEW',
          textRiskScore: 78.5,
          voiceRiskScore: 72.0,
          contextRiskScore: 85.0,
          sviValue: 78.5,
          initialCategory: 'CRITICAL',
          finalCategory: 'CRITICAL',
          safetyOverrideTriggered: true,
          adminId: 'ADMIN_01'
        },
        {
          caseId: 'NHAA-0008',
          assessmentTimestamp: new Date(Date.now() - 86400000).toISOString(),
          inputType: 'AUDIO',
          input_mode: 'MULTIMODAL',
          audioUrl: '/audio/NHAA-0008_eviction_recording.wav',
          audioFileName: 'NHAA_0008_eviction_recording.wav',
          rawInputText: 'My landlord evicted me without notice and threw my belongings outside. I have nowhere to go tonight and need emergency shelter.',
          sanitizedTranscript: 'My landlord evicted me without notice and threw my belongings outside. I have nowhere to go tonight and need emergency shelter.',
          status: 'ACCEPTED',
          textRiskScore: 58.0,
          voiceRiskScore: 48.0,
          contextRiskScore: 55.0,
          sviValue: 54.1,
          initialCategory: 'MODERATE',
          finalCategory: 'MODERATE',
          safetyOverrideTriggered: false,
          adminId: 'ADMIN_01'
        },
        {
          caseId: 'NHAA-0015',
          assessmentTimestamp: new Date(Date.now() - 172800000).toISOString(),
          inputType: 'AUDIO',
          input_mode: 'MULTIMODAL',
          audioUrl: '/audio/NHAA-0015_legal_dispute.wav',
          audioFileName: 'NHAA_0015_legal_dispute.wav',
          rawInputText: 'I am submitting a complaint regarding property document delay and legal aid assistance needed for court proceedings. No physical violence has occurred.',
          sanitizedTranscript: 'I am submitting a complaint regarding property document delay and legal aid assistance needed for court proceedings. No physical violence has occurred.',
          status: 'AWAITING_ADMIN_REVIEW',
          textRiskScore: 28.0,
          voiceRiskScore: 22.0,
          contextRiskScore: 25.0,
          sviValue: 25.3,
          initialCategory: 'LOW',
          finalCategory: 'LOW',
          safetyOverrideTriggered: false,
          adminId: 'ADMIN_01'
        },
        {
          caseId: 'NHAA-0021',
          assessmentTimestamp: new Date(Date.now() - 259200000).toISOString(),
          inputType: 'AUDIO',
          input_mode: 'MULTIMODAL',
          audioUrl: '/audio/NHAA-0021_harassment.wav',
          audioFileName: 'NHAA_0021_harassment.wav',
          rawInputText: 'Officer: What is the primary issue?\nVictim: I am being repeatedly harassed and stalked at my workplace. I requested police protection.',
          sanitizedTranscript: 'Officer: What is the primary issue?\nVictim: I am being repeatedly harassed and stalked at my workplace. I requested police protection.',
          status: 'AWAITING_ADMIN_REVIEW',
          textRiskScore: 62.0,
          voiceRiskScore: 58.0,
          contextRiskScore: 65.0,
          sviValue: 61.7,
          initialCategory: 'HIGH',
          finalCategory: 'HIGH',
          safetyOverrideTriggered: false,
          adminId: 'ADMIN_01'
        }
      ]);
    }
  };

  const fetchAuditEvents = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/audit`);
      if (res.ok) {
        const data = await res.json();
        setAuditEvents(data);
      }
    } catch (e) {
      setAuditEvents([
        {
          id: 1,
          caseId: 'NHAA-0012',
          timestamp: new Date().toISOString(),
          actor: 'System',
          role: 'AI_SYSTEM',
          action: 'SVI_CALCULATED',
          previousState: 'PROCESSING',
          newState: 'HIGH',
          reason: 'Continuous SVI score generated: 74.3'
        },
        {
          id: 2,
          caseId: 'NHAA-0012',
          timestamp: new Date().toISOString(),
          actor: 'System',
          role: 'SAFETY_LAYER',
          action: 'SAFETY_OVERRIDE_TRIGGERED',
          previousState: 'HIGH',
          newState: 'CRITICAL',
          reason: 'Immediate safety concern detected in structured context.'
        }
      ]);
    }
  };

  const fetchActiveLogs = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/logs`);
      if (res.ok) {
        const data = await res.json();
        setActiveLogs(data);
      }
    } catch (e) {
      setActiveLogs([
        { id: 1, caseId: 'NHAA-0012', timestamp: '10:31:02', stage: 'STT', status: 'COMPLETED', actorService: 'SarvamSTT', eventMessage: 'Victim speech transcribed', severity: 'INFO' },
        { id: 2, caseId: 'NHAA-0012', timestamp: '10:31:05', stage: 'PII_MASKING', status: 'COMPLETED', actorService: 'PiiService', eventMessage: 'PII masked retaining semantics', severity: 'INFO' },
        { id: 3, caseId: 'NHAA-0012', timestamp: '10:31:08', stage: 'TEXT_ML', status: 'COMPLETED', actorService: 'MuRILService', eventMessage: '10 Indicators predicted. Text Risk Score: 68.5', severity: 'INFO' },
        { id: 4, caseId: 'NHAA-0012', timestamp: '10:31:11', stage: 'SAFETY_OVERRIDE', status: 'WARNING', actorService: 'SafetyLayer', eventMessage: 'Safety Override triggered! Escalated to CRITICAL', severity: 'WARN' }
      ]);
    }
  };

  const handleStartAssessment = async () => {
    if (!consentGiven) {
      alert('Please confirm informed consent before proceeding.');
      return;
    }
    setIsProcessing(true);
    setProgressStep(1);
    setActiveTab('progress');

    const isTextOnlyMode = inputType === 'TEXT';

    // Step sequence for UI animation based on mode
    const steps = isTextOnlyMode ? [
      { step: 1, label: 'PII Masking & Privacy Processing...' },
      { step: 2, label: 'Sarvam LLM Multilingual Structured Context Extraction...' },
      { step: 3, label: 'MuRIL Text ML (10 Risk Indicators)...' },
      { step: 4, label: 'Context Risk & Text-Only SVI Calculation (70% Text / 30% Context)...' },
      { step: 5, label: 'Monotonic Safety Validation & Safety Override Check...' },
      { step: 6, label: 'Support Prioritization Engine Mapping...' }
    ] : [
      { step: 1, label: 'Exotel Audio Splitter & PII Sanitization...' },
      { step: 2, label: 'Sarvam LLM Multilingual Structured Context Extraction...' },
      { step: 3, label: 'MuRIL Text ML (10 Risk Indicators)...' },
      { step: 4, label: 'Voice ML Acoustic Analysis (7 Evidence Metrics)...' },
      { step: 5, label: 'Three-Input Multimodal SVI Calculator (0-100 Score)...' },
      { step: 6, label: 'Monotonic Safety Validation & Safety Override Check...' },
      { step: 7, label: 'Support Prioritization Engine Mapping...' }
    ];

    for (let i = 0; i < steps.length; i++) {
      await new Promise(r => setTimeout(r, 500));
      setProgressStep(i + 1);
    }

    try {
      const res = await fetch(`${API_BASE_URL}/assessment`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          inputType: isTextOnlyMode ? 'TEXT_ONLY' : 'AUDIO',
          text: isTextOnlyMode ? narrativeText : (selectedAudioFile ? selectedAudioFile.name : 'Audio complaint'),
          audioPath: isTextOnlyMode ? null : (selectedAudioFile ? selectedAudioFile.name : 'demo.wav')
        })
      });
      if (res.ok) {
        const data = await res.json();
        setAssessmentResult(data);
        fetchCases();
        fetchAuditEvents();
        fetchActiveLogs();
      } else {
        throw new Error('API request failed');
      }
    } catch (e) {
      // Dynamic client-side fallback if backend API is unreachable
      const inputText = (inputType === 'TEXT' ? narrativeText : (selectedAudioFile ? selectedAudioFile.name : 'audio')).toLowerCase();
      
      let textScore = 8.0;
      let voiceScore = isTextOnlyMode ? null : 10.0;
      let contextScore = 0.0;
      let isCritical = false;
      let category = 'LOW';
      let recs = [];

      const isSafe = inputText.includes('argued') || inputText.includes('resolved') || inputText.includes('no one threatened') || inputText.includes('safe') || inputText.includes('no.') || inputText.includes('resolved the issue');
      const isThreat = (inputText.includes('threat') || inputText.includes('danger') || inputText.includes('beat') || inputText.includes('kill')) && !isSafe;

      if (isThreat) {
        textScore = 78.5;
        if (!isTextOnlyMode) voiceScore = 72.0;
        contextScore = 85.0;
        isCritical = true;
        category = 'CRITICAL';
        recs = [
          { support_type: 'EMERGENCY_SUPPORT', priority: 'URGENT', reason: 'Immediate danger or active life-safety threat detected.', evidence: ['Active threat / danger detected'], human_review_required: true },
          { support_type: 'POLICE_INTERVENTION', priority: 'URGENT', reason: 'Active threat or protection concern detected.', evidence: ['Protection review required'], human_review_required: true }
        ];
      } else if (inputText.includes('evict') || inputText.includes('homeless') || inputText.includes('displaced')) {
        textScore = 58.0;
        if (!isTextOnlyMode) voiceScore = 48.0;
        contextScore = 55.0;
        category = 'MODERATE';
      } else if (inputText.includes('legal') || inputText.includes('property') || inputText.includes('court')) {
        textScore = 28.0;
        if (!isTextOnlyMode) voiceScore = 22.0;
        contextScore = 25.0;
        category = 'LOW';
        recs = [{ support_type: 'LEGAL_AID', priority: 'HIGH', reason: 'Legal aid requested.', evidence: ['Victim requested legal help'], human_review_required: false }];
      } else {
        textScore = 12.0;
        contextScore = 0.0;
        category = 'LOW';
      }

      const rawSvi = isTextOnlyMode
        ? (textScore * 0.70) + (contextScore * 0.30)
        : (textScore * 0.40) + ((voiceScore || 0) * 0.30) + (contextScore * 0.30);
      const sviScore = Math.round(rawSvi * 100) / 100;

      if (!isCritical) {
        if (sviScore >= 75) category = 'CRITICAL';
        else if (sviScore >= 50) category = 'HIGH';
        else if (sviScore >= 25) category = 'MODERATE';
        else category = 'LOW';
      }

      const extractedAudioTranscript = isTextOnlyMode
        ? (narrativeText || 'Victim narrative text submitted for assessment.')
        : getAudioSpeechTranscript(selectedAudioFile, narrativeText);

      const mockResult = {
        caseId: 'NHAA-' + Math.floor(1000 + Math.random() * 9000),
        status: 'AWAITING_ADMIN_REVIEW',
        input_mode: isTextOnlyMode ? 'TEXT_ONLY' : 'MULTIMODAL',
        inputType: isTextOnlyMode ? 'TEXT_ONLY' : 'AUDIO',
        audioUrl: isTextOnlyMode ? null : (audioPreviewUrl || null),
        audioFileName: isTextOnlyMode ? null : (selectedAudioFile ? selectedAudioFile.name : null),
        textRiskScore: textScore,
        voiceRiskScore: voiceScore,
        contextRiskScore: contextScore,
        text_ml: { risk_score: textScore },
        structured_context: { risk_score: contextScore },
        sanitizedTranscript: extractedAudioTranscript,
        rawInputText: extractedAudioTranscript,
        initialCategory: category,
        finalCategory: category,
        sviCategory: category,
        sviValue: sviScore,
        svi: {
          score: sviScore,
          sviScore: sviScore,
          risk_category: category,
          riskCategory: category,
          text_ml_weight: isTextOnlyMode ? 0.70 : 0.40,
          voice_ml_weight: isTextOnlyMode ? 0.0 : 0.30,
          structured_context_weight: isTextOnlyMode ? 0.30 : 0.30
        },
        safety: {
          override_triggered: isCritical,
          priority: isCritical ? 'CRITICAL' : 'ROUTINE',
          human_review_required: isCritical,
          priority_alert: isCritical,
          reasons: isCritical ? ['Critical immediate safety concern detected.'] : [],
          final_risk_category: category,
          safety_escalated: isCritical
        },
        safety_assessment: {
          override_triggered: isCritical,
          human_review_required: isCritical,
          priority_alert: isCritical,
          override_reason: isCritical ? 'Critical immediate safety concern detected.' : null,
          reasons: isCritical ? ['Critical immediate safety concern detected.'] : [],
          final_risk_category: category,
          safety_escalated: isCritical
        },
        safetyAssessment: {
          overrideTriggered: isCritical,
          humanReviewRequired: isCritical,
          priorityAlert: isCritical,
          overrideReason: isCritical ? 'Critical immediate safety concern detected.' : null,
          finalRiskCategory: category,
          safetyEscalated: isCritical
        },
        support_prioritization: recs
      };
      setAssessmentResult(mockResult);
      setCases(prev => [mockResult, ...prev]);
    }

    setIsProcessing(false);
    setActiveTab('result');
  };

  const handleAdminAction = async (caseId, action, category, notes) => {
    const targetCase = selectedCase || cases.find(c => c.caseId === caseId) || assessmentResult;
    const depts = getDepartmentNames(targetCase);

    try {
      let endpoint = `${API_BASE_URL}/cases/${caseId}/` + action.toLowerCase();
      await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ adminId: 'ADMIN_01', finalCategory: category, notes, reason: notes })
      });
      fetchCases();
      fetchAuditEvents();
    } catch (e) {
      console.warn(`Demo mode action: ${action} for case ${caseId}`);
    }

    // Local state update for smooth demo interaction
    const updatedStatus = action === 'ACCEPT' ? 'ACCEPTED' : action === 'MODIFY' ? 'MODIFIED' : 'REJECTED';
    setCases(prev => prev.map(c => {
      if (c.caseId === caseId) {
        return {
          ...c,
          status: updatedStatus,
          finalCategory: category || c.finalCategory
        };
      }
      return c;
    }));

    if (selectedCase && selectedCase.caseId === caseId) {
      setSelectedCase(prev => prev ? {
        ...prev,
        status: updatedStatus,
        finalCategory: category || prev.finalCategory
      } : null);
    }
    if (assessmentResult && assessmentResult.caseId === caseId) {
      setAssessmentResult(prev => prev ? {
        ...prev,
        status: updatedStatus,
        finalCategory: category || prev.finalCategory
      } : null);
    }

    // Add entry to Audit Events list
    const newAudit = {
      id: Date.now(),
      caseId: caseId,
      timestamp: new Date().toISOString(),
      actor: 'Administrator',
      role: 'NHAA_AUTHORITY',
      action: `DECISION_${action}`,
      previousState: targetCase ? (targetCase.status || 'AWAITING_ADMIN_REVIEW') : 'AWAITING_ADMIN_REVIEW',
      newState: updatedStatus,
      reason: notes || `Administrator executed ${action} decision.`
    };
    setAuditEvents(prev => [newAudit, ...prev]);

    setShowModifyModal(false);
    setShowEscalateModal(false);

    let title = 'Success!';
    let message = '';

    if (action === 'ACCEPT') {
      title = 'Success!';
      message = `Case ${caseId} has been successfully ACCEPTED. This case is redirected to the respective department(s) for immediate response.`;
    } else if (action === 'MODIFY') {
      title = 'Assessment Modified!';
      message = `Case ${caseId} risk category has been modified to ${category}. This case is redirected to the respective department(s) for updated handling.`;
    } else if (action === 'REJECT') {
      title = 'Case Rejected!';
      message = `Case ${caseId} decision has been logged as REJECTED and archived. Administrative notes logged into the immutable audit trail.`;
    }

    setActionModal({
      show: true,
      type: action,
      title,
      caseId,
      category,
      message,
      departments: depts
    });
  };

  return (
    <div className="app-wrapper">
      {/* Official Government Top Header Bar */}
      <div className="gov-top-header">
        <div className="gov-top-header-content">
          <div className="gov-top-header-left">
            <span className="gov-flag-icon">🇮🇳</span>
            <span>Government of India | Ministry of Social Justice & Empowerment</span>
          </div>
          <div>
            <span>Department of Social Justice & Empowerment — National Helpline Against Atrocities (NHAA)</span>
          </div>
        </div>
      </div>

      <div className="app-container">
        {/* Sidebar - Redesigned Deep Navy #0B2942 */}
        <aside className="sidebar">
          <div className="sidebar-header">
            <img
              src="/prism_icon_transparent.png"
              alt="Prism AI Logo"
              style={{
                height: '46px',
                width: 'auto',
                objectFit: 'contain'
              }}
            />
            <div>
              <div className="sidebar-title">Prism AI</div>
              <div className="sidebar-subtitle">AI Decision-Support System</div>
            </div>
          </div>

          <nav className="nav-menu">
            <div className={`nav-item ${activeTab === 'dashboard' ? 'active' : ''}`} onClick={() => setActiveTab('dashboard')}>
              📊 Dashboard
            </div>
            <div className={`nav-item ${activeTab === 'consent' ? 'active' : ''}`} onClick={() => setActiveTab('consent')}>
              📝 New Assessment
            </div>
            <div className={`nav-item ${activeTab === 'history' ? 'active' : ''}`} onClick={() => setActiveTab('history')}>
              📂 Case Review / History
            </div>
            <div className={`nav-item ${activeTab === 'audit' ? 'active' : ''}`} onClick={() => setActiveTab('audit')}>
              📜 Immutable Audit Trail
            </div>
            <div className={`nav-item ${activeTab === 'logs' ? 'active' : ''}`} onClick={() => setActiveTab('logs')}>
              ⚡ Real-Time Processing Logs
            </div>
            <div className={`nav-item ${activeTab === 'settings' ? 'active' : ''}`} onClick={() => setActiveTab('settings')}>
              ⚙️ System Configuration
            </div>
          </nav>

          <div className="sidebar-footer">
            <div>NHAA 14566 Prototype v1.0</div>
            <div>Authorized Personnel Only</div>
          </div>
        </aside>

        {/* Main Content */}
        <main className="main-content">
          <header className="top-navbar">
            <h1 className="page-title">
              {activeTab === 'dashboard' && 'Administrator Dashboard'}
              {activeTab === 'consent' && (inputType === 'TEXT' ? 'Incident Description' : 'Voice Input Assessment')}
              {activeTab === 'progress' && 'AI Pipeline Processing Stream'}
              {activeTab === 'result' && 'Assessment Results'}
              {activeTab === 'review' && 'Case Review & Human Decisions'}
              {activeTab === 'history' && 'Case Review / History'}
              {activeTab === 'audit' && 'System Audit Trail'}
              {activeTab === 'logs' && 'Active Real-Time Processing Logs'}
              {activeTab === 'settings' && 'System Configuration'}
            </h1>
            <div className="user-profile">
              <div className="user-avatar">A</div>
              <div style={{ textAlign: 'left' }}>
                <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--primary-navy)', lineHeight: 1.1 }}>Administrator</div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-secondary)', fontWeight: 600 }}>NHAA_AUTHORITY</div>
              </div>
            </div>
          </header>

          <div className="content-body">
            {/* PAGE 2: DASHBOARD */}
            {activeTab === 'dashboard' && (
              <div>
                <div style={{ marginBottom: '1.25rem' }}>
                  <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                    Overview of current victim assessments, safety indicators, and review activity.
                  </p>
                </div>

                {/* 4 Interactive Enterprise Summary KPI Tiles */}
                <div className="stats-grid">
                  <div
                    className={`stat-card ${dashboardFilter === 'ALL' ? 'active-filter' : ''}`}
                    style={{ borderTop: '3px solid var(--primary-brand)' }}
                    onClick={() => setDashboardFilter('ALL')}
                    title="Click to view all cases"
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span className="stat-kicker">TOTAL ASSESSMENTS</span>
                      {dashboardFilter === 'ALL' && <span className="badge badge-low" style={{ fontSize: '0.65rem' }}>ACTIVE</span>}
                    </div>
                    <div className="stat-value">{cases.length}</div>
                    <div className="stat-desc">Registered system cases</div>
                  </div>

                  <div
                    className={`stat-card ${dashboardFilter === 'HIGH_CRITICAL' ? 'active-filter' : ''}`}
                    style={{ borderTop: '3px solid var(--risk-critical)' }}
                    onClick={() => setDashboardFilter('HIGH_CRITICAL')}
                    title="Click to filter High & Critical cases"
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span className="stat-kicker" style={{ color: 'var(--risk-critical)' }}>HIGH / CRITICAL CASES</span>
                      {dashboardFilter === 'HIGH_CRITICAL' && <span className="badge badge-critical" style={{ fontSize: '0.65rem' }}>ACTIVE</span>}
                    </div>
                    <div className="stat-value" style={{ color: 'var(--risk-critical)' }}>
                      {cases.filter(c => c.finalCategory === 'CRITICAL' || c.finalCategory === 'HIGH').length}
                    </div>
                    <div className="stat-desc" style={{ color: 'var(--risk-critical)' }}>Requires immediate review</div>
                  </div>

                  <div
                    className={`stat-card ${dashboardFilter === 'AWAITING_REVIEW' ? 'active-filter' : ''}`}
                    style={{ borderTop: '3px solid var(--risk-moderate)' }}
                    onClick={() => setDashboardFilter('AWAITING_REVIEW')}
                    title="Click to filter cases awaiting review"
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span className="stat-kicker" style={{ color: 'var(--risk-moderate)' }}>AWAITING REVIEW</span>
                      {dashboardFilter === 'AWAITING_REVIEW' && <span className="badge badge-moderate" style={{ fontSize: '0.65rem' }}>ACTIVE</span>}
                    </div>
                    <div className="stat-value" style={{ color: 'var(--risk-moderate)' }}>
                      {cases.filter(c => c.status === 'AWAITING_ADMIN_REVIEW' || c.status === 'PROCESSING').length}
                    </div>
                    <div className="stat-desc">Pending officer sign-off</div>
                  </div>

                  <div
                    className={`stat-card ${dashboardFilter === 'ACCEPTED_RESOLVED' ? 'active-filter' : ''}`}
                    style={{ borderTop: '3px solid var(--risk-low)' }}
                    onClick={() => setDashboardFilter('ACCEPTED_RESOLVED')}
                    title="Click to filter accepted & resolved cases"
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span className="stat-kicker" style={{ color: 'var(--risk-low)' }}>ACCEPTED / RESOLVED</span>
                      {dashboardFilter === 'ACCEPTED_RESOLVED' && <span className="badge badge-low" style={{ fontSize: '0.65rem' }}>ACTIVE</span>}
                    </div>
                    <div className="stat-value" style={{ color: 'var(--risk-low)' }}>
                      {cases.filter(c => c.status === 'ACCEPTED' || c.status === 'RESOLVED').length}
                    </div>
                    <div className="stat-desc">Action plan approved</div>
                  </div>
                </div>

                {/* Recent Victim Assessments Table with Active Filter Handling */}
                <div className="card">
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', flexWrap: 'wrap', gap: '0.75rem' }}>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                        <h3 style={{ fontSize: '1.1rem', fontWeight: 800, color: 'var(--primary-navy)' }}>Recent Victim Assessments</h3>
                        {dashboardFilter !== 'ALL' && (
                          <button
                            className="filter-pill active"
                            onClick={() => setDashboardFilter('ALL')}
                            style={{ fontSize: '0.75rem', padding: '0.25rem 0.75rem' }}
                          >
                            Filter: {dashboardFilter === 'HIGH_CRITICAL' ? 'High / Critical' : dashboardFilter === 'AWAITING_REVIEW' ? 'Awaiting Review' : 'Accepted / Resolved'} ✕
                          </button>
                        )}
                      </div>
                      <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
                        {dashboardFilter === 'ALL' && 'Showing all registered victim assessments'}
                        {dashboardFilter === 'HIGH_CRITICAL' && 'Filtered: Showing High & Critical cases only'}
                        {dashboardFilter === 'AWAITING_REVIEW' && 'Filtered: Showing cases awaiting review only'}
                        {dashboardFilter === 'ACCEPTED_RESOLVED' && 'Filtered: Showing accepted & resolved cases only'}
                      </p>
                    </div>
                    <button className="btn btn-primary" onClick={() => setActiveTab('consent')}>+ New Assessment</button>
                  </div>

                  <div className="table-container">
                    <table>
                      <thead>
                        <tr>
                          <th>Case ID</th>
                          <th>User ID</th>
                          <th>Timestamp</th>
                          <th>Input Type</th>
                          <th>SVI Score</th>
                          <th>Final Risk Category</th>
                          <th>Safety Override</th>
                          <th>Status</th>
                          <th>Action</th>
                        </tr>
                      </thead>
                      <tbody>
                        {(() => {
                          const filteredCases = cases.filter(c => {
                            if (dashboardFilter === 'HIGH_CRITICAL') return c.finalCategory === 'CRITICAL' || c.finalCategory === 'HIGH';
                            if (dashboardFilter === 'AWAITING_REVIEW') return c.status === 'AWAITING_ADMIN_REVIEW' || c.status === 'PROCESSING';
                            if (dashboardFilter === 'ACCEPTED_RESOLVED') return c.status === 'ACCEPTED' || c.status === 'RESOLVED';
                            return true;
                          });

                          if (filteredCases.length === 0) {
                            return (
                              <tr>
                                <td colSpan={9} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)' }}>
                                  No cases found matching the active card filter.
                                </td>
                              </tr>
                            );
                          }

                          return filteredCases.map(c => (
                            <tr key={c.caseId}>
                              <td style={{ fontWeight: 800, color: 'var(--primary-navy)' }}>{c.caseId}</td>
                              <td><span className="badge" style={{ backgroundColor: '#F3ECE2', color: '#7A5230', border: '1px solid #E2D4C3', fontWeight: 700, fontSize: '0.8rem' }}>{getUserIdForCase(c)}</span></td>
                              <td>{new Date(c.assessmentTimestamp).toLocaleString()}</td>
                              <td>
                                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem', fontSize: '0.82rem', fontWeight: 600 }}>
                                  {c.inputType === 'AUDIO' || c.inputType === 'MULTIMODAL' ? '🎙️ Audio' : '📝 Text'}
                                </span>
                              </td>
                              <td><strong style={{ fontSize: '0.95rem', color: 'var(--primary-navy)' }}>{c.sviValue || 0.0}</strong> / 100</td>
                              <td>
                                <span className={`badge badge-${(c.finalCategory || 'LOW').toLowerCase()}`}>
                                  {c.finalCategory || 'LOW'}
                                </span>
                              </td>
                              <td>
                                {c.safetyOverrideTriggered ? (
                                  <span className="badge badge-critical" style={{ fontSize: '0.7rem' }}>⚠️ OVERRIDE</span>
                                ) : (
                                  <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>NO</span>
                                )}
                              </td>
                              <td>
                                <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-secondary)' }}>{c.status}</span>
                              </td>
                              <td>
                                <button className="btn btn-outline" style={{ padding: '0.35rem 0.85rem', fontSize: '0.8rem' }} onClick={() => handleOpenCase(c)}>
                                  Inspect →
                                </button>
                              </td>
                            </tr>
                          ));
                        })()}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            )}

            {/* PAGE 3, 4, 5: NEW SAFETY ASSESSMENT */}
            {activeTab === 'consent' && (
              <div>
                {/* 2-Column Side-by-Side Layout */}
                <div style={{ display: 'grid', gridTemplateColumns: '320px 1fr', gap: '1.5rem', alignItems: 'start' }}>
                  
                  {/* LEFT COLUMN: Vertically Stacked Input Mode Cards & Privacy Note */}
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                    {/* TEXT INPUT CARD */}
                    <div
                      className="card"
                      style={{
                        border: inputType === 'TEXT' ? '2px solid var(--primary-brand)' : '1px solid var(--border-color)',
                        backgroundColor: inputType === 'TEXT' ? '#FAF7F2' : '#ffffff',
                        cursor: 'pointer',
                        transition: 'all 0.2s ease',
                        margin: 0
                      }}
                      onClick={() => setInputType('TEXT')}
                    >
                      <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>📄</div>
                      <h3 style={{ fontSize: '1.05rem', fontWeight: 800, color: 'var(--primary-navy)', marginBottom: '0.25rem' }}>Text Input</h3>
                      <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', lineHeight: '1.4', marginBottom: '1rem' }}>
                        Manually enter incident information and complaint narratives.
                      </p>
                      <button className={`btn ${inputType === 'TEXT' ? 'btn-primary' : 'btn-outline'}`} style={{ width: '100%', fontSize: '0.82rem', padding: '0.5rem 1rem' }}>
                        {inputType === 'TEXT' ? 'Active Mode ✓' : 'Select Text →'}
                      </button>
                    </div>

                    {/* VOICE INPUT CARD */}
                    <div
                      className="card"
                      style={{
                        border: inputType === 'AUDIO' ? '2px solid var(--primary-brand)' : '1px solid var(--border-color)',
                        backgroundColor: inputType === 'AUDIO' ? '#FAF7F2' : '#ffffff',
                        cursor: 'pointer',
                        transition: 'all 0.2s ease',
                        margin: 0
                      }}
                      onClick={() => setInputType('AUDIO')}
                    >
                      <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>🎙️</div>
                      <h3 style={{ fontSize: '1.05rem', fontWeight: 800, color: 'var(--primary-navy)', marginBottom: '0.25rem' }}>Voice Input</h3>
                      <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', lineHeight: '1.4', marginBottom: '1rem' }}>
                        Record or upload audio with automatic transcription.
                      </p>
                      <button className={`btn ${inputType === 'AUDIO' ? 'btn-primary' : 'btn-outline'}`} style={{ width: '100%', fontSize: '0.82rem', padding: '0.5rem 1rem' }}>
                        {inputType === 'AUDIO' ? 'Active Mode ✓' : 'Select Voice →'}
                      </button>
                    </div>

                    {/* Bottom Privacy Panel */}
                    <div style={{ padding: '1rem', background: '#ffffff', border: '1px solid var(--border-color)', borderRadius: '12px', display: 'flex', gap: '0.75rem', alignItems: 'flex-start', fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                      <span style={{ fontSize: '1.2rem', lineHeight: 1 }}>🔒</span>
                      <div>
                        <strong style={{ color: 'var(--primary-navy)' }}>Data Security:</strong> All data handled securely per government privacy guidelines.
                      </div>
                    </div>
                  </div>

                  {/* RIGHT COLUMN: Incident Description Form Box */}
                  <div className="card" style={{ margin: 0 }}>
                    {inputType === 'TEXT' && (
                      <div>
                        <div style={{ marginBottom: '1.25rem' }}>
                          <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--primary-navy)' }}>Incident Description</h3>
                          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Provide details about the situation.</p>
                        </div>

                        <div style={{ marginBottom: '1.25rem' }}>
                          <label style={{ display: 'block', fontWeight: 700, fontSize: '0.88rem', color: 'var(--text-primary)', marginBottom: '0.5rem' }}>
                            Incident Narrative Text
                          </label>
                          <textarea
                            rows={7}
                            style={{ width: '100%', padding: '0.85rem', borderRadius: '8px', border: '1px solid var(--border-color)', fontFamily: 'inherit', fontSize: '0.95rem', lineHeight: '1.5' }}
                            value={narrativeText}
                            onChange={e => setNarrativeText(e.target.value)}
                            placeholder="Type victim complaint or officer notes here..."
                          />
                          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '0.35rem', fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                            <span>Character count: {narrativeText.length} characters</span>
                            <span>Auto-detect language supported</span>
                          </div>
                        </div>

                        <div style={{ backgroundColor: '#F8FAFC', padding: '0.85rem 1rem', borderRadius: '8px', border: '1px solid var(--border-color)', marginBottom: '1.25rem', fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
                          ℹ️ Entered text is processed via Google MuRIL ML & Sarvam LLM for structured fact detection. PII is automatically sanitized.
                        </div>
                      </div>
                    )}

                    {inputType === 'AUDIO' && (
                      <div>
                        <div style={{ marginBottom: '1.25rem' }}>
                          <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--primary-navy)' }}>Voice Input Assessment</h3>
                          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Record or upload audio and provide additional context.</p>
                        </div>

                        <input
                          type="file"
                          ref={fileInputRef}
                          style={{ display: 'none' }}
                          accept="audio/*,.wav,.ogg,.mp3,.mp4,.m4a,.aac"
                          onChange={(e) => {
                            const file = e.target.files[0];
                            if (file) {
                              setSelectedAudioFile(file);
                              setAudioPreviewUrl(URL.createObjectURL(file));
                            }
                          }}
                        />

                        <div
                          style={{
                            marginBottom: '1.25rem',
                            border: selectedAudioFile ? '2px solid var(--primary-brand)' : '2px dashed var(--border-color)',
                            backgroundColor: selectedAudioFile ? '#FAF7F2' : '#FAF7F2',
                            padding: '1.5rem',
                            textAlign: 'center',
                            borderRadius: '12px',
                            cursor: 'pointer',
                            transition: 'all 0.2s ease'
                          }}
                          onClick={() => fileInputRef.current && fileInputRef.current.click()}
                        >
                          <div style={{ fontSize: '2.2rem', marginBottom: '0.4rem' }}>🎙️</div>
                          <div style={{ fontWeight: 800, fontSize: '1.05rem', color: 'var(--primary-navy)' }}>
                            {selectedAudioFile ? `Selected File: ${selectedAudioFile.name}` : 'Audio Record or Upload Area'}
                          </div>
                          
                          <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '4px', height: '32px', margin: '0.85rem 0' }}>
                            {[40, 65, 30, 85, 50, 95, 70, 45, 80, 60, 35, 90, 55, 75, 40].map((h, idx) => (
                              <span
                                key={idx}
                                style={{
                                  width: '4px',
                                  height: `${h}%`,
                                  backgroundColor: selectedAudioFile ? 'var(--primary-brand)' : 'var(--border-color)',
                                  borderRadius: '2px',
                                  transition: 'height 0.2s ease'
                                }}
                              />
                            ))}
                          </div>

                          <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
                            {selectedAudioFile
                              ? `Size: ${(selectedAudioFile.size / 1024).toFixed(1)} KB | Format: ${selectedAudioFile.type || 'Audio file'}`
                              : 'Supported formats: WAV, OGG, MP3 (Exotel dual-channel stereo auto-isolated)'}
                          </div>

                          {audioPreviewUrl && (
                            <div style={{ marginTop: '1rem' }} onClick={(e) => e.stopPropagation()}>
                              <div style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--primary-brand)', marginBottom: '0.3rem' }}>
                                🔊 Audio Playback Preview:
                              </div>
                              <audio controls src={audioPreviewUrl} style={{ width: '100%', maxWidth: '450px' }} />
                            </div>
                          )}

                          <button
                            type="button"
                            className="btn btn-primary"
                            style={{ marginTop: '1rem', fontSize: '0.82rem', padding: '0.5rem 1rem' }}
                            onClick={(e) => {
                              e.stopPropagation();
                              if (fileInputRef.current) fileInputRef.current.click();
                            }}
                          >
                            {selectedAudioFile ? '📁 Choose Different Audio File' : '📂 Select Audio File from Device'}
                          </button>
                        </div>

                        <div style={{ marginBottom: '1.25rem' }}>
                          <label style={{ display: 'block', fontWeight: 700, fontSize: '0.88rem', color: 'var(--text-primary)', marginBottom: '0.4rem' }}>
                            Additional Notes (Optional)
                          </label>
                          <textarea
                            rows={3}
                            style={{ width: '100%', padding: '0.75rem', borderRadius: '8px', border: '1px solid var(--border-color)', fontFamily: 'inherit', fontSize: '0.9rem' }}
                            value={narrativeText}
                            onChange={e => setNarrativeText(e.target.value)}
                            placeholder="Add supplementary contextual notes..."
                          />
                        </div>
                      </div>
                    )}

                    {/* Informed Consent Checkboxes */}
                    <div style={{ backgroundColor: '#F8FAFC', padding: '1.1rem 1.25rem', borderRadius: '8px', border: '1px solid var(--border-color)', marginBottom: '1.5rem', fontSize: '0.85rem' }}>
                      <p style={{ marginBottom: '0.6rem', fontWeight: 700, color: 'var(--primary-navy)' }}>Informed Consent & Privacy Confirmation:</p>
                      <label style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-start', cursor: 'pointer', marginBottom: '0.4rem' }}>
                        <input type="checkbox" checked={consentGiven} onChange={e => setConsentGiven(e.target.checked)} style={{ marginTop: '0.2rem' }} />
                        <span>I confirm consent to process complaint text/audio via AI decision-support algorithms for indicator detection and priority review.</span>
                      </label>
                      <label style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-start', cursor: 'pointer' }}>
                        <input type="checkbox" checked={consentGiven} onChange={e => setConsentGiven(e.target.checked)} style={{ marginTop: '0.2rem' }} />
                        <span>I understand PII will be sanitized and that final decisions require authorized human administrator sign-off.</span>
                      </label>
                    </div>

                    <div style={{ display: 'flex', gap: '1rem' }}>
                      {inputType === 'TEXT' && (
                        <button className="btn btn-outline" onClick={() => setNarrativeText('')}>
                          Clear Text
                        </button>
                      )}
                      <button className="btn btn-primary" style={{ flex: 1, padding: '0.8rem' }} onClick={handleStartAssessment}>
                        🚀 Run AI Risk Assessment
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* PAGE 5: ANALYSIS PROGRESS */}
            {activeTab === 'progress' && (
              <div className="card" style={{ maxWidth: '700px', margin: '0 auto', textAlign: 'center' }}>
                <h2 style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--primary-navy)', marginBottom: '1.5rem' }}>Executing Real-Time Assessment Pipeline</h2>
                
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem', textAlign: 'left', marginBottom: '2rem' }}>
                  {inputType === 'TEXT' ? (
                    <>
                      <div style={{ padding: '0.75rem 1rem', borderRadius: '8px', background: progressStep >= 1 ? 'var(--risk-low-bg)' : '#F8FAFC', border: '1px solid var(--border-color)', fontWeight: 600, fontSize: '0.88rem' }}>
                        {progressStep >= 1 ? '✅' : '⏳'} 1. PII Masking & Privacy Processing
                      </div>
                      <div style={{ padding: '0.75rem 1rem', borderRadius: '8px', background: progressStep >= 2 ? 'var(--risk-low-bg)' : '#F8FAFC', border: '1px solid var(--border-color)', fontWeight: 600, fontSize: '0.88rem' }}>
                        {progressStep >= 2 ? '✅' : '⏳'} 2. Sarvam LLM Multilingual Structured Context Extraction
                      </div>
                      <div style={{ padding: '0.75rem 1rem', borderRadius: '8px', background: progressStep >= 3 ? 'var(--risk-low-bg)' : '#F8FAFC', border: '1px solid var(--border-color)', fontWeight: 600, fontSize: '0.88rem' }}>
                        {progressStep >= 3 ? '✅' : '⏳'} 3. Google MuRIL Text ML (10 Multi-Label Risk Indicators)
                      </div>
                      <div style={{ padding: '0.75rem 1rem', borderRadius: '8px', background: progressStep >= 4 ? 'var(--risk-low-bg)' : '#F8FAFC', border: '1px solid var(--border-color)', fontWeight: 600, fontSize: '0.88rem' }}>
                        {progressStep >= 4 ? '✅' : '⏳'} 4. Context Risk & Text-Only SVI Calculation (70% Text / 30% Context)
                      </div>
                      <div style={{ padding: '0.75rem 1rem', borderRadius: '8px', background: progressStep >= 5 ? 'var(--risk-low-bg)' : '#F8FAFC', border: '1px solid var(--border-color)', fontWeight: 600, fontSize: '0.88rem' }}>
                        {progressStep >= 5 ? '✅' : '⏳'} 5. Monotonic Safety Validation & Safety Override Check
                      </div>
                      <div style={{ padding: '0.75rem 1rem', borderRadius: '8px', background: progressStep >= 6 ? 'var(--risk-low-bg)' : '#F8FAFC', border: '1px solid var(--border-color)', fontWeight: 600, fontSize: '0.88rem' }}>
                        {progressStep >= 6 ? '✅' : '⏳'} 6. Support Prioritization Engine Mapping
                      </div>
                    </>
                  ) : (
                    <>
                      <div style={{ padding: '0.75rem 1rem', borderRadius: '8px', background: progressStep >= 1 ? 'var(--risk-low-bg)' : '#F8FAFC', border: '1px solid var(--border-color)', fontWeight: 600, fontSize: '0.88rem' }}>
                        {progressStep >= 1 ? '✅' : '⏳'} 1. Exotel Audio Splitter & PII Sanitization
                      </div>
                      <div style={{ padding: '0.75rem 1rem', borderRadius: '8px', background: progressStep >= 2 ? 'var(--risk-low-bg)' : '#F8FAFC', border: '1px solid var(--border-color)', fontWeight: 600, fontSize: '0.88rem' }}>
                        {progressStep >= 2 ? '✅' : '⏳'} 2. Sarvam LLM Multilingual Structured Context Extraction
                      </div>
                      <div style={{ padding: '0.75rem 1rem', borderRadius: '8px', background: progressStep >= 3 ? 'var(--risk-low-bg)' : '#F8FAFC', border: '1px solid var(--border-color)', fontWeight: 600, fontSize: '0.88rem' }}>
                        {progressStep >= 3 ? '✅' : '⏳'} 3. Google MuRIL Text ML (10 Multi-Label Risk Indicators)
                      </div>
                      <div style={{ padding: '0.75rem 1rem', borderRadius: '8px', background: progressStep >= 4 ? 'var(--risk-low-bg)' : '#F8FAFC', border: '1px solid var(--border-color)', fontWeight: 600, fontSize: '0.88rem' }}>
                        {progressStep >= 4 ? '✅' : '⏳'} 4. Voice ML Acoustic Analysis (7 Evidence Metrics)
                      </div>
                      <div style={{ padding: '0.75rem 1rem', borderRadius: '8px', background: progressStep >= 5 ? 'var(--risk-low-bg)' : '#F8FAFC', border: '1px solid var(--border-color)', fontWeight: 600, fontSize: '0.88rem' }}>
                        {progressStep >= 5 ? '✅' : '⏳'} 5. Three-Input Multimodal SVI Calculator (0-100 Score)
                      </div>
                      <div style={{ padding: '0.75rem 1rem', borderRadius: '8px', background: progressStep >= 6 ? 'var(--risk-low-bg)' : '#F8FAFC', border: '1px solid var(--border-color)', fontWeight: 600, fontSize: '0.88rem' }}>
                        {progressStep >= 6 ? '✅' : '⏳'} 6. Monotonic Safety Validation & Safety Override Check
                      </div>
                      <div style={{ padding: '0.75rem 1rem', borderRadius: '8px', background: progressStep >= 7 ? 'var(--risk-low-bg)' : '#F8FAFC', border: '1px solid var(--border-color)', fontWeight: 600, fontSize: '0.88rem' }}>
                        {progressStep >= 7 ? '✅' : '⏳'} 7. Deterministic Support Prioritization
                      </div>
                    </>
                  )}
                </div>

                <div style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Please wait while the decision-support pipeline finishes...</div>
              </div>
            )}

            {/* PAGE 6: ASSESSMENT RESULTS & PAGE 7: SUPPORT PRIORITIZATION */}
            {activeTab === 'result' && assessmentResult && (
              <div>
                {/* Header Info Bar */}
                <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.5rem' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.25rem' }}>
                      <span style={{ fontSize: '0.88rem', color: 'var(--text-secondary)' }}>Case Identification:</span>
                      <strong style={{ fontSize: '1.4rem', color: 'var(--primary-navy)' }}>{assessmentResult.caseId}</strong>
                      <span className="badge" style={{ backgroundColor: 'var(--soft-blue)', color: 'var(--primary-blue)', fontWeight: 700, fontSize: '0.75rem' }}>
                        MODE: {assessmentResult.input_mode || assessmentResult.inputType || 'TEXT_ONLY'}
                      </span>
                    </div>
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                      Status: <strong style={{ color: 'var(--text-primary)' }}>{assessmentResult.status}</strong>
                    </div>
                  </div>
                  <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', textAlign: 'right' }}>
                    AI-Assisted Decision-Support Prototype | Human Review Required for High Risk Cases
                  </div>
                </div>

                {/* REFERENCE SCREEN #6: SVI SCORE CIRCLE & SAFETY CARDS */}
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginBottom: '1.5rem' }}>
                  {/* CARD A: NUMERICAL SVI CIRCULAR SCORE GAUGE */}
                  <div className="card" style={{ margin: 0, textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
                    <div style={{ fontSize: '0.85rem', fontWeight: 800, color: 'var(--primary-navy)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '1rem' }}>
                      SVI / RISK SCORE
                    </div>

                    {/* Circular Score Gauge */}
                    {(() => {
                      const scoreVal = assessmentResult.svi?.score !== undefined ? assessmentResult.svi.score : (assessmentResult.sviValue || 0.0);
                      const cat = getCaseRiskCategory(assessmentResult);
                      const strokeColor = cat === 'CRITICAL' ? '#991B1B' : cat === 'HIGH' ? '#C2410C' : cat === 'MODERATE' ? '#B45309' : '#15803D';
                      const strokeDash = (scoreVal / 100) * 283;

                      return (
                        <div style={{ position: 'relative', width: '150px', height: '150px', margin: '0.5rem auto' }}>
                          <svg width="150" height="150" viewBox="0 0 100 100">
                            <circle cx="50" cy="50" r="45" fill="none" stroke="#E2E8F0" strokeWidth="9" />
                            <circle
                              cx="50"
                              cy="50"
                              r="45"
                              fill="none"
                              stroke={strokeColor}
                              strokeWidth="9"
                              strokeDasharray={`${strokeDash} 283`}
                              strokeLinecap="round"
                              transform="rotate(-90 50 50)"
                            />
                          </svg>
                          <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
                            <div style={{ fontSize: '2rem', fontWeight: 900, color: strokeColor, lineHeight: 1 }}>
                              {scoreVal.toFixed(2)}
                            </div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontWeight: 600 }}>/ 100</div>
                          </div>
                        </div>
                      );
                    })()}

                    <div style={{ marginTop: '0.85rem' }}>
                      <span className={`badge badge-${getCaseRiskCategory(assessmentResult).toLowerCase()}`} style={{ fontSize: '0.9rem', padding: '0.35rem 0.9rem' }}>
                        {getCaseRiskCategory(assessmentResult)}
                      </span>
                    </div>

                    {/* Key Findings derived dynamically from existing data */}
                    <div style={{ marginTop: '1.25rem', width: '100%', textAlign: 'left', background: '#F8FAFC', padding: '0.85rem 1rem', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
                      <div style={{ fontSize: '0.82rem', fontWeight: 700, color: 'var(--primary-navy)', marginBottom: '0.4rem' }}>Key Findings:</div>
                      {(() => {
                        const isOverride = (assessmentResult.safety?.override_triggered || assessmentResult.safety_assessment?.human_review_required || assessmentResult.safetyAssessment?.humanReviewRequired);
                        if (isOverride) {
                          return (
                            <ul style={{ margin: 0, paddingLeft: '1.2rem', fontSize: '0.82rem', color: 'var(--risk-critical)' }}>
                              <li>⚠️ Critical immediate safety concern flagged</li>
                              <li>🚨 Priority human review mandatory</li>
                              <li>🛡️ Emergency support intervention required</li>
                            </ul>
                          );
                        } else {
                          return (
                            <ul style={{ margin: 0, paddingLeft: '1.2rem', fontSize: '0.82rem', color: 'var(--risk-low)' }}>
                              <li>✓ No immediate danger override detected</li>
                              <li>✓ Low to moderate emotional distress score</li>
                              <li>✓ Standard support action plan available</li>
                            </ul>
                          );
                        }
                      })()}
                    </div>
                  </div>

                  {/* CARD B: SAFETY OVERRIDE & STATUS CHIPS */}
                  <div className="card" style={{ margin: 0, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                    <div>
                      <div style={{ fontSize: '0.85rem', fontWeight: 800, color: 'var(--primary-navy)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '1rem' }}>
                        🛡️ Safety Assessment & Status
                      </div>

                      {(assessmentResult.safety?.override_triggered || assessmentResult.safety_assessment?.human_review_required || assessmentResult.safetyAssessment?.humanReviewRequired) ? (
                        <div>
                          <div style={{ marginBottom: '0.85rem' }}>
                            <span className="badge badge-critical" style={{ fontSize: '0.85rem' }}>CRITICAL SAFETY CONCERN DETECTED</span>
                          </div>
                          <div style={{ fontSize: '0.88rem', color: 'var(--risk-critical)', fontWeight: 700, marginBottom: '0.5rem' }}>
                            Human Review Required: YES | Priority Alert: YES
                          </div>
                          <div style={{ fontSize: '0.85rem', color: '#450a0a', background: '#FDF2F2', padding: '0.75rem 1rem', borderRadius: '8px', border: '1px solid #F8C4C4', marginBottom: '1rem' }}>
                            <strong>Escalation Reason:</strong> {assessmentResult.safety?.reasons?.[0] || assessmentResult.safety_assessment?.override_reason || assessmentResult.safetyAssessment?.overrideReason || 'Immediate safety indicator detected requiring human review.'}
                          </div>
                        </div>
                      ) : (
                        <div>
                          <div style={{ fontSize: '0.95rem', color: 'var(--risk-low)', fontWeight: 800, marginBottom: '0.5rem' }}>
                            No Critical Safety Override Triggered
                          </div>
                          <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.85rem' }}>
                            Human Review Required: <strong>NO</strong> | Priority Alert: <strong>NO</strong>
                          </div>
                          <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', background: '#F8FAFC', padding: '0.75rem', borderRadius: '8px', border: '1px solid var(--border-color)', marginBottom: '1rem' }}>
                            Standard assessment protocol applies based on numerical SVI score.
                          </div>
                        </div>
                      )}

                      {/* Status Chips */}
                      <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', marginTop: '0.75rem' }}>
                        <span className="badge" style={{ backgroundColor: '#EEF2F6', color: 'var(--primary-navy)' }}>
                          Safety: {(assessmentResult.safety?.override_triggered || assessmentResult.safety_assessment?.human_review_required) ? 'HIGH ALERT' : 'SECURE'}
                        </span>
                        <span className="badge" style={{ backgroundColor: '#EEF2F6', color: 'var(--primary-navy)' }}>
                          Urgency: {(assessmentResult.safety?.override_triggered || assessmentResult.safety_assessment?.human_review_required) ? 'URGENT' : 'ROUTINE'}
                        </span>
                        <span className="badge" style={{ backgroundColor: '#EEF2F6', color: 'var(--primary-navy)' }}>
                          Priority: {assessmentResult.svi?.risk_category || assessmentResult.sviCategory || 'LOW'}
                        </span>
                      </div>
                    </div>

                    <div style={{ marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid var(--border-color)', fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
                      "This assessment is AI-assisted and should be reviewed according to applicable organizational protocols."
                    </div>
                  </div>
                </div>

                {/* REFERENCE SCREEN #7: SUPPORT PRIORITIZATION RANKED CARDS */}
                <div className="card" style={{ marginBottom: '1.5rem' }}>
                  <div style={{ marginBottom: '1.25rem' }}>
                    <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--primary-navy)' }}>Recommended Support Actions</h3>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                      Support actions based on the existing assessment output. Ranked by priority and factual evidence.
                    </p>
                  </div>

                  {(() => {
                    let recs = getRecommendationsForCase(assessmentResult);

                    if (Array.isArray(recs) && recs.length > 0) {
                      return (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                          {recs.map((rec, idx) => {
                            const supportType = rec.support_type || rec.support;
                            const evidenceList = rec.evidence || [];
                            const cardNum = String(idx + 1).padStart(2, '0');

                            return (
                              <div
                                key={idx}
                                style={{
                                  padding: '1.1rem 1.35rem',
                                  background: '#ffffff',
                                  border: '1px solid var(--border-color)',
                                  borderRadius: '10px',
                                  borderLeft: rec.priority === 'URGENT' ? '5px solid var(--risk-critical)' : rec.priority === 'HIGH' ? '5px solid var(--risk-high)' : '5px solid var(--primary-blue)',
                                  display: 'flex',
                                  gap: '1.25rem',
                                  alignItems: 'flex-start'
                                }}
                              >
                                <div style={{ fontSize: '1.5rem', fontWeight: 900, color: 'var(--border-color)', lineHeight: 1 }}>
                                  {cardNum}
                                </div>
                                <div style={{ flex: 1 }}>
                                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.4rem' }}>
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                      <strong style={{ fontSize: '1.05rem', color: 'var(--primary-navy)' }}>
                                        {supportType === 'EMERGENCY_SUPPORT' && '🚨 Emergency Support'}
                                        {supportType === 'POLICE_INTERVENTION' && '🛡️ Police Intervention'}
                                        {supportType === 'MEDICAL_ASSISTANCE' && '🏥 Medical Assistance'}
                                        {supportType === 'LEGAL_AID' && '⚖️ Legal Aid'}
                                        {supportType === 'PSYCHOSOCIAL_COUNSELLING' && '🧠 Psychosocial Counselling'}
                                        {supportType === 'COUNSELLING' && '🧠 Psychosocial Counselling'}
                                        {supportType === 'WITNESS_PROTECTION_CONSIDERATION' && '🔒 Witness Protection Consideration'}
                                        {!['EMERGENCY_SUPPORT', 'POLICE_INTERVENTION', 'MEDICAL_ASSISTANCE', 'LEGAL_AID', 'PSYCHOSOCIAL_COUNSELLING', 'COUNSELLING', 'WITNESS_PROTECTION_CONSIDERATION'].includes(supportType) && supportType}
                                      </strong>
                                      {rec.human_review_required && (
                                        <span style={{ fontSize: '0.7rem', backgroundColor: 'var(--risk-critical-bg)', color: 'var(--risk-critical)', padding: '0.15rem 0.5rem', borderRadius: '4px', fontWeight: 700 }}>
                                          HUMAN REVIEW REQUIRED
                                        </span>
                                      )}
                                    </div>

                                    <span className={`badge ${rec.priority === 'URGENT' ? 'badge-critical' : rec.priority === 'HIGH' ? 'badge-high' : 'badge-moderate'}`}>
                                      Priority: {rec.priority}
                                    </span>
                                  </div>

                                  <div style={{ fontSize: '0.88rem', color: 'var(--text-secondary)', marginBottom: evidenceList.length > 0 ? '0.5rem' : 0 }}>
                                    <strong>Description:</strong> {rec.reason}
                                  </div>

                                  {evidenceList.length > 0 && (
                                    <div style={{ background: '#F8FAFC', padding: '0.65rem 0.85rem', borderRadius: '6px', border: '1px solid var(--border-color)', fontSize: '0.82rem' }}>
                                      <div style={{ fontWeight: 700, color: 'var(--primary-navy)', marginBottom: '0.2rem' }}>Supporting Evidence:</div>
                                      <ul style={{ margin: 0, paddingLeft: '1.2rem', color: 'var(--text-secondary)' }}>
                                        {evidenceList.map((item, evIdx) => (
                                          <li key={evIdx}>{item}</li>
                                        ))}
                                      </ul>
                                    </div>
                                  )}
                                </div>
                              </div>
                            );
                          })}
                        </div>
                      );
                    }

                    return (
                      <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                        No support recommendations generated.
                      </div>
                    );
                  })()}
                  </div>

                <div style={{ display: 'flex', gap: '1rem', justifyContent: 'flex-end' }}>
                  <button className="btn btn-outline" onClick={() => setActiveTab('dashboard')}>Back to Dashboard</button>
                  <button className="btn btn-primary" onClick={() => handleOpenCase(assessmentResult)}>Proceed to Case Review →</button>
                </div>
              </div>
            )}

            {/* PAGE 7 / PAGE 11: CASE REVIEW & TIMELINE HISTORY */}
            {(activeTab === 'review' || activeTab === 'history') && (
              <div>
                {activeTab === 'history' && !selectedCase ? (
                  <div className="card">
                    <div style={{ marginBottom: '1.25rem' }}>
                      <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--primary-navy)' }}>Case Review / History</h3>
                      <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Select a case from the archive to view history timeline.</p>
                    </div>

                    <div className="table-container">
                      <table>
                        <thead>
                          <tr>
                            <th>Case ID</th>
                            <th>User ID</th>
                            <th>Timestamp</th>
                            <th>SVI Score</th>
                            <th>Final Risk Category</th>
                            <th>Administrator</th>
                            <th>Status</th>
                            <th>Action</th>
                          </tr>
                        </thead>
                        <tbody>
                          {cases.map(c => (
                            <tr key={c.caseId}>
                              <td style={{ fontWeight: 800, color: 'var(--primary-navy)' }}>{c.caseId}</td>
                              <td><span className="badge" style={{ backgroundColor: '#FEF3C7', color: '#B45309', border: '1px solid #FDE68A', fontWeight: 700, fontSize: '0.8rem' }}>{getUserIdForCase(c)}</span></td>
                              <td>{new Date(c.assessmentTimestamp).toLocaleString()}</td>
                              <td><strong>{c.sviValue || 0.0}</strong> / 100</td>
                              <td><span className={`badge badge-${(c.finalCategory || 'LOW').toLowerCase()}`}>{c.finalCategory}</span></td>
                              <td>{c.adminId || 'System'}</td>
                              <td>{c.status}</td>
                              <td>
                                <button className="btn btn-outline" style={{ padding: '0.35rem 0.85rem', fontSize: '0.8rem' }} onClick={() => handleOpenCase(c)}>
                                  View History →
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                ) : (
                  <div>
                    {isCaseLoading ? (
                      <div className="card" style={{ padding: '3.5rem 2rem', textAlign: 'center', margin: 0 }}>
                        <div style={{ fontSize: '2.2rem', marginBottom: '0.75rem' }}>⏳</div>
                        <div style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--primary-navy)', marginBottom: '0.35rem' }}>
                          Loading case audio and transcript...
                        </div>
                        <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                          Retrieving case details and acoustic audio recording for inspection.
                        </div>
                      </div>
                    ) : (() => {
                      const targetCase = selectedCase || assessmentResult || cases[0];
                      if (!targetCase) return null;

                      const isVoice = targetCase.inputType === 'AUDIO' || targetCase.inputType === 'MULTIMODAL' || targetCase.input_mode === 'MULTIMODAL' || Boolean(targetCase.audioUrl || targetCase.audioFileName || targetCase.audioFile);
                      const textScore = targetCase.textRiskScore ?? targetCase.text_ml?.risk_score ?? 0;
                      const voiceScore = targetCase.voiceRiskScore ?? 0;
                      const contextScore = targetCase.contextRiskScore ?? targetCase.structured_context?.risk_score ?? 0;
                      const sviScore = targetCase.svi?.score ?? targetCase.sviValue ?? 0;

                      return (
                        <div>
                          {/* Selected Case Header Information */}
                          <div className="card" style={{ marginBottom: '1.5rem' }}>
                            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.25rem' }}>
                              <div>
                                <h2 style={{ fontSize: '1.35rem', fontWeight: 800, color: 'var(--primary-navy)' }}>
                                  Case Inspection: #{targetCase.caseId}
                                </h2>
                                <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                                  Victim User ID: <strong style={{ color: 'var(--primary-blue)' }}>{getUserIdForCase(targetCase)}</strong> | Input Mode: <strong style={{ color: 'var(--primary-blue)' }}>{isVoice ? 'MULTIMODAL (VOICE + TEXT)' : 'TEXT ONLY'}</strong> | Created: {new Date(targetCase.assessmentTimestamp || Date.now()).toLocaleString()} | Status: <strong>{targetCase.status}</strong>
                                </div>
                              </div>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                                <span className={`badge badge-${getCaseRiskCategory(targetCase).toLowerCase()}`} style={{ fontSize: '0.85rem', padding: '0.4rem 1rem' }}>
                                  Risk Category: {getCaseRiskCategory(targetCase)}
                                </span>
                                <span className="badge" style={{ backgroundColor: 'var(--soft-blue)', color: 'var(--primary-navy)', fontSize: '0.85rem' }}>
                                  SVI Score: {Number(sviScore).toFixed(2)} / 100
                                </span>
                              </div>
                            </div>

                            {/* 2. SVI SCORE CIRCLE & SAFETY ASSESSMENT OVERRIDE CARDS */}
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginBottom: '1.5rem' }}>
                              {/* SVI CIRCULAR SCORE GAUGE */}
                              <div style={{ background: '#ffffff', border: '1px solid var(--border-color)', borderRadius: '10px', padding: '1.25rem', textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
                                <div style={{ fontSize: '0.85rem', fontWeight: 800, color: 'var(--primary-navy)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.75rem' }}>
                                  SVI / RISK SCORE
                                </div>

                                {(() => {
                                  const scoreVal = Number(sviScore);
                                  const cat = getCaseRiskCategory(targetCase);
                                  const strokeColor = cat === 'CRITICAL' ? '#991B1B' : cat === 'HIGH' ? '#C2410C' : cat === 'MODERATE' ? '#B45309' : '#15803D';
                                  const strokeDash = (scoreVal / 100) * 283;

                                  return (
                                    <div style={{ position: 'relative', width: '130px', height: '130px', margin: '0.25rem auto' }}>
                                      <svg width="130" height="130" viewBox="0 0 100 100">
                                        <circle cx="50" cy="50" r="45" fill="none" stroke="#E2E8F0" strokeWidth="9" />
                                        <circle
                                          cx="50"
                                          cy="50"
                                          r="45"
                                          fill="none"
                                          stroke={strokeColor}
                                          strokeWidth="9"
                                          strokeDasharray={`${strokeDash} 283`}
                                          strokeLinecap="round"
                                          transform="rotate(-90 50 50)"
                                        />
                                      </svg>
                                      <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
                                        <div style={{ fontSize: '1.75rem', fontWeight: 900, color: strokeColor, lineHeight: 1 }}>
                                          {scoreVal.toFixed(2)}
                                        </div>
                                        <div style={{ fontSize: '0.72rem', color: 'var(--text-secondary)', fontWeight: 600 }}>/ 100</div>
                                      </div>
                                    </div>
                                  );
                                })()}

                                <div style={{ marginTop: '0.75rem' }}>
                                  <span className={`badge badge-${getCaseRiskCategory(targetCase).toLowerCase()}`} style={{ fontSize: '0.85rem', padding: '0.3rem 0.8rem' }}>
                                    {getCaseRiskCategory(targetCase)}
                                  </span>
                                </div>

                                <div style={{ marginTop: '1rem', width: '100%', textAlign: 'left', background: '#F8FAFC', padding: '0.75rem 0.85rem', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
                                  <div style={{ fontSize: '0.8rem', fontWeight: 700, color: 'var(--primary-navy)', marginBottom: '0.3rem' }}>Key Findings:</div>
                                  {targetCase.safetyOverrideTriggered ? (
                                    <ul style={{ margin: 0, paddingLeft: '1.1rem', fontSize: '0.78rem', color: 'var(--risk-critical)' }}>
                                      <li>⚠️ Critical immediate safety concern flagged</li>
                                      <li>🚨 Priority human review mandatory</li>
                                      <li>🛡️ Emergency support intervention required</li>
                                    </ul>
                                  ) : (
                                    <ul style={{ margin: 0, paddingLeft: '1.1rem', fontSize: '0.78rem', color: 'var(--risk-low)' }}>
                                      <li>✓ No immediate danger override detected</li>
                                      <li>✓ Low to moderate emotional distress score</li>
                                      <li>✓ Standard support action plan available</li>
                                    </ul>
                                  )}
                                </div>
                              </div>

                              {/* SAFETY ASSESSMENT OVERRIDE CARD */}
                              <div style={{ background: '#ffffff', border: '1px solid var(--border-color)', borderRadius: '10px', padding: '1.25rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
                                <div>
                                  <div style={{ fontSize: '0.85rem', fontWeight: 800, color: 'var(--primary-navy)', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.75rem' }}>
                                    🛡️ Safety Assessment & Status
                                  </div>

                                  {targetCase.safetyOverrideTriggered ? (
                                    <div>
                                      <div style={{ marginBottom: '0.75rem' }}>
                                        <span className="badge badge-critical" style={{ fontSize: '0.8rem' }}>CRITICAL SAFETY CONCERN DETECTED</span>
                                      </div>
                                      <div style={{ fontSize: '0.85rem', color: 'var(--risk-critical)', fontWeight: 700, marginBottom: '0.4rem' }}>
                                        Human Review Required: YES | Priority Alert: YES
                                      </div>
                                      <div style={{ fontSize: '0.82rem', color: '#450a0a', background: '#FDF2F2', padding: '0.65rem 0.85rem', borderRadius: '6px', border: '1px solid #F8C4C4', marginBottom: '0.85rem' }}>
                                        <strong>Escalation Reason:</strong> {targetCase.safety?.reasons?.[0] || targetCase.safety_assessment?.override_reason || targetCase.safetyAssessment?.overrideReason || 'Immediate safety indicator detected.'}
                                      </div>
                                    </div>
                                  ) : (
                                    <div>
                                      <div style={{ fontSize: '0.9rem', color: 'var(--risk-low)', fontWeight: 800, marginBottom: '0.4rem' }}>
                                        No Critical Safety Override Triggered
                                      </div>
                                      <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', marginBottom: '0.75rem' }}>
                                        Human Review Required: <strong>NO</strong> | Priority Alert: <strong>NO</strong>
                                      </div>
                                      <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', background: '#F8FAFC', padding: '0.65rem', borderRadius: '6px', border: '1px solid var(--border-color)', marginBottom: '0.85rem' }}>
                                        Standard assessment protocol applies based on numerical SVI score.
                                      </div>
                                    </div>
                                  )}

                                  <div style={{ display: 'flex', gap: '0.4rem', flexWrap: 'wrap' }}>
                                    <span className="badge" style={{ backgroundColor: '#EEF2F6', color: 'var(--primary-navy)' }}>
                                      Safety: {targetCase.safetyOverrideTriggered ? 'HIGH ALERT' : 'SECURE'}
                                    </span>
                                    <span className="badge" style={{ backgroundColor: '#EEF2F6', color: 'var(--primary-navy)' }}>
                                      Urgency: {targetCase.safetyOverrideTriggered ? 'URGENT' : 'ROUTINE'}
                                    </span>
                                    <span className={`badge badge-${getCaseRiskCategory(targetCase).toLowerCase()}`}>
                                      Priority: {getCaseRiskCategory(targetCase)}
                                    </span>
                                  </div>
                                </div>

                                <div style={{ marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border-color)', fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                                  "This assessment is AI-assisted and should be reviewed according to applicable organizational protocols."
                                </div>
                              </div>
                            </div>

                            {/* 3. INPUT NARRATIVE / AUDIO PLAYER & STRUCTURED FACT EXTRACTION */}
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginBottom: '1.5rem' }}>
                              <div>
                                <h4 style={{ fontSize: '0.9rem', fontWeight: 700, marginBottom: '0.5rem', color: 'var(--primary-navy)' }}>
                                  {isVoice ? '🎙️ Voice Audio & Transcribed Text' : '📝 Victim Narrative Text'}
                                </h4>

                                {isVoice && (
                                  <div style={{ padding: '0.85rem', background: '#F3ECE2', border: '1px solid #E2D4C3', borderRadius: '8px', marginBottom: '0.85rem' }}>
                                    <div style={{ fontSize: '0.8rem', fontWeight: 700, color: '#7A5230', marginBottom: '0.35rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                                      🔊 Voice Audio Playback:
                                    </div>
                                    {audioErrorMap[targetCase.caseId] ? (
                                      <div style={{ fontSize: '0.82rem', color: '#B91C1C', padding: '0.55rem 0.75rem', background: '#FDF2F2', borderRadius: '6px', border: '1px solid #F8C4C4', fontWeight: 600 }}>
                                        ⚠️ Audio recording unavailable for this case.
                                      </div>
                                    ) : (
                                      <audio
                                        controls
                                        key={targetCase.caseId + '_' + getCaseAudioUrl(targetCase)}
                                        src={getCaseAudioUrl(targetCase)}
                                        onError={(e) => {
                                           const fallbackUrl = getCaseAudioUrl({ ...targetCase, audioPreviewUrl: null, blobUrl: null, recordedAudioUrl: null });
                                           if (e.target.src !== window.location.origin + fallbackUrl && e.target.src !== fallbackUrl) {
                                             e.target.src = fallbackUrl;
                                           } else {
                                             setAudioErrorMap(prev => ({ ...prev, [targetCase.caseId]: true }));
                                           }
                                         }}
                                        style={{ width: '100%' }}
                                      />
                                    )}
                                    <div style={{ fontSize: '0.75rem', color: '#8B3A2B', marginTop: '0.35rem', fontWeight: 600 }}>
                                      Case Audio File: {getCaseAudioFileName(targetCase)}
                                    </div>
                                  </div>
                                )}

                                <div style={{ padding: '0.85rem 1rem', background: '#F8FAFC', border: '1px solid var(--border-color)', borderRadius: '8px', fontSize: '0.88rem', lineHeight: '1.5' }}>
                                  <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-secondary)', marginBottom: '0.3rem', textTransform: 'uppercase' }}>
                                    {isVoice ? 'TRANSCRIBED AUDIO SPEECH TEXT (PII MASKED):' : 'Victim Narrative (PII Masked):'}
                                  </div>
                                  {renderFormattedTranscript(getCaseTranscript(targetCase))}
                                </div>
                              </div>

                              <div>
                                <h4 style={{ fontSize: '0.9rem', fontWeight: 700, marginBottom: '0.5rem', color: 'var(--primary-navy)' }}>📋 Structured Fact Extraction</h4>
                                {(() => {
                                  let ctx = {};
                                  if (targetCase.structuredContextJson) {
                                    try { ctx = JSON.parse(targetCase.structuredContextJson); } catch (e) {}
                                  } else if (targetCase.structuredContext) {
                                    ctx = targetCase.structuredContext;
                                  }
                                  const safety = ctx.safetyContext || {};
                                  const incident = ctx.incidentContext || {};
                                  const family = ctx.familyContext || {};
                                  const property = ctx.propertyContext || {};
                                  const legal = ctx.legalContext || {};
                                  const text = (targetCase.sanitizedTranscript || targetCase.rawInputText || '').toLowerCase();

                                  return (
                                    <div style={{ padding: '0.85rem 1rem', background: '#F8FAFC', border: '1px solid var(--border-color)', borderRadius: '8px', fontSize: '0.82rem', display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
                                      <div><strong>Active Threat:</strong> {safety.currentThreat === 'TRUE' || text.includes('threat') ? 'TRUE (Active threat reported)' : 'FALSE'}</div>
                                      <div><strong>Immediate Danger Concern:</strong> {safety.immediateSafetyConcern === 'TRUE' || text.includes('danger') || text.includes('scared') ? 'HIGH' : 'LOW'}</div>
                                      <div><strong>Physical Violence Reported:</strong> {incident.physicalViolence === 'TRUE' || text.includes('beat') || text.includes('hit') ? 'TRUE' : 'NOT REPORTED'}</div>
                                      <div><strong>Weapon Involved:</strong> {incident.weaponUsage === 'TRUE' || text.includes('knife') || text.includes('weapon') ? 'TRUE' : 'FALSE'}</div>
                                      <div><strong>Family / Children Involvement:</strong> {family.familyInvolved || text.includes('family') || text.includes('children') ? 'TRUE' : 'FALSE'}</div>
                                      <div><strong>Displacement / Eviction:</strong> {property.propertyDispossession || text.includes('evict') || text.includes('homeless') ? 'TRUE' : 'FALSE'}</div>
                                      <div><strong>Legal Assistance Requested:</strong> {legal.legalHelpRequested || text.includes('legal') || text.includes('court') ? 'TRUE' : 'FALSE'}</div>
                                    </div>
                                  );
                                })()}
                              </div>
                            </div>

                            {/* 4. RISK COMPONENT BREAKDOWN */}
                            <div style={{ marginBottom: '1.5rem', background: '#ffffff', padding: '1.25rem', borderRadius: '10px', border: '1px solid var(--border-color)' }}>
                              <h4 style={{ fontSize: '0.95rem', fontWeight: 800, color: 'var(--primary-navy)', marginBottom: '0.75rem' }}>
                                📊 Risk Component Breakdown
                              </h4>

                              {isVoice ? (
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', alignItems: 'center' }}>
                                  <div style={{ padding: '0.85rem', background: '#F8FAFC', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
                                    <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Text ML Risk Score</div>
                                    <div style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--primary-navy)' }}>{Number(textScore).toFixed(1)}</div>
                                    <div style={{ fontSize: '0.75rem', color: 'var(--primary-brand)', fontWeight: 600 }}>× 40% Weight</div>
                                  </div>

                                  <div style={{ padding: '0.85rem', background: '#F8FAFC', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
                                    <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Voice ML Risk Score</div>
                                    <div style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--primary-navy)' }}>{Number(voiceScore).toFixed(1)}</div>
                                    <div style={{ fontSize: '0.75rem', color: 'var(--primary-brand)', fontWeight: 600 }}>× 30% Weight</div>
                                  </div>

                                  <div style={{ padding: '0.85rem', background: '#F8FAFC', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
                                    <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Structured Context Score</div>
                                    <div style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--primary-navy)' }}>{Number(contextScore).toFixed(1)}</div>
                                    <div style={{ fontSize: '0.75rem', color: 'var(--primary-brand)', fontWeight: 600 }}>× 30% Weight</div>
                                  </div>

                                  <div style={{ padding: '0.85rem', background: '#F3ECE2', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
                                    <div style={{ fontSize: '0.78rem', color: 'var(--primary-navy)', fontWeight: 700 }}>Multimodal SVI</div>
                                    <div style={{ fontSize: '1.5rem', fontWeight: 900, color: 'var(--primary-navy)' }}>{Number(sviScore).toFixed(2)}</div>
                                    <div style={{ fontSize: '0.72rem', color: 'var(--text-secondary)' }}>Normalized range</div>
                                  </div>
                                </div>
                              ) : (
                                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem', alignItems: 'center' }}>
                                  <div style={{ padding: '0.85rem', background: '#F8FAFC', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
                                    <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Text ML Risk Score</div>
                                    <div style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--primary-navy)' }}>{Number(textScore).toFixed(1)}</div>
                                    <div style={{ fontSize: '0.75rem', color: 'var(--primary-brand)', fontWeight: 600 }}>× 70% Weight</div>
                                  </div>

                                  <div style={{ padding: '0.85rem', background: '#F8FAFC', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
                                    <div style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', fontWeight: 600 }}>Structured Context Score</div>
                                    <div style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--primary-navy)' }}>{Number(contextScore).toFixed(1)}</div>
                                    <div style={{ fontSize: '0.75rem', color: 'var(--primary-brand)', fontWeight: 600 }}>× 30% Weight</div>
                                  </div>

                                  <div style={{ padding: '0.85rem', background: '#F3ECE2', borderRadius: '8px', border: '1px solid var(--border-color)' }}>
                                    <div style={{ fontSize: '0.78rem', color: 'var(--primary-navy)', fontWeight: 700 }}>Text-Only SVI</div>
                                    <div style={{ fontSize: '1.5rem', fontWeight: 900, color: 'var(--primary-navy)' }}>{Number(sviScore).toFixed(2)}</div>
                                    <div style={{ fontSize: '0.72rem', color: 'var(--text-secondary)' }}>Normalized range</div>
                                  </div>
                                </div>
                              )}
                            </div>

                            {/* 5. RECOMMENDED SUPPORT ACTIONS ENGINE */}
                            <div style={{ marginBottom: '1.5rem', background: '#ffffff', padding: '1.25rem', borderRadius: '10px', border: '1px solid var(--border-color)' }}>
                              <div style={{ marginBottom: '1rem' }}>
                                <h4 style={{ fontSize: '1rem', fontWeight: 800, color: 'var(--primary-navy)' }}>Recommended Support Actions</h4>
                                <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
                                  Service recommendations derived from text indicators, structured facts, and victim requests.
                                </p>
                              </div>

                              {(() => {
                                let recs = getRecommendationsForCase(targetCase);

                                if (Array.isArray(recs) && recs.length > 0) {
                                  return (
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem' }}>
                                      {recs.map((rec, idx) => {
                                        const supportType = rec.support_type || rec.support;
                                        const evidenceList = rec.evidence || [];
                                        const cardNum = String(idx + 1).padStart(2, '0');

                                        return (
                                          <div
                                            key={idx}
                                            style={{
                                              padding: '1rem 1.15rem',
                                              background: '#F8FAFC',
                                              border: '1px solid var(--border-color)',
                                              borderRadius: '8px',
                                              borderLeft: rec.priority === 'URGENT' ? '5px solid var(--risk-critical)' : rec.priority === 'HIGH' ? '5px solid var(--risk-high)' : '5px solid var(--primary-brand)',
                                              display: 'flex',
                                              gap: '1rem',
                                              alignItems: 'flex-start'
                                            }}
                                          >
                                            <div style={{ fontSize: '1.3rem', fontWeight: 900, color: 'var(--border-color)', lineHeight: 1 }}>
                                              {cardNum}
                                            </div>
                                            <div style={{ flex: 1 }}>
                                              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.35rem' }}>
                                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                                  <strong style={{ fontSize: '0.98rem', color: 'var(--primary-navy)' }}>
                                                    {supportType === 'EMERGENCY_SUPPORT' && '🚨 Emergency Support'}
                                                    {supportType === 'POLICE_INTERVENTION' && '🛡️ Police Intervention'}
                                                    {supportType === 'MEDICAL_ASSISTANCE' && '🏥 Medical Assistance'}
                                                    {supportType === 'LEGAL_AID' && '⚖️ Legal Aid'}
                                                    {supportType === 'PSYCHOSOCIAL_COUNSELLING' && '🧠 Psychosocial Counselling'}
                                                    {supportType === 'COUNSELLING' && '🧠 Psychosocial Counselling'}
                                                    {supportType === 'WITNESS_PROTECTION_CONSIDERATION' && '🔒 Witness Protection Consideration'}
                                                    {!['EMERGENCY_SUPPORT', 'POLICE_INTERVENTION', 'MEDICAL_ASSISTANCE', 'LEGAL_AID', 'PSYCHOSOCIAL_COUNSELLING', 'COUNSELLING', 'WITNESS_PROTECTION_CONSIDERATION'].includes(supportType) && supportType}
                                                  </strong>
                                                  {rec.human_review_required && (
                                                    <span style={{ fontSize: '0.68rem', backgroundColor: 'var(--risk-critical-bg)', color: 'var(--risk-critical)', padding: '0.15rem 0.4rem', borderRadius: '4px', fontWeight: 700 }}>
                                                      HUMAN REVIEW REQUIRED
                                                    </span>
                                                  )}
                                                </div>

                                                <span className={`badge ${rec.priority === 'URGENT' ? 'badge-critical' : rec.priority === 'HIGH' ? 'badge-high' : 'badge-moderate'}`} style={{ fontSize: '0.75rem', padding: '0.25rem 0.65rem' }}>
                                                  Priority: {rec.priority}
                                                </span>
                                              </div>

                                              <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: evidenceList.length > 0 ? '0.4rem' : 0 }}>
                                                <strong>Description:</strong> {rec.reason}
                                              </div>

                                              {evidenceList.length > 0 && (
                                                <div style={{ background: '#ffffff', padding: '0.55rem 0.75rem', borderRadius: '6px', border: '1px solid var(--border-color)', fontSize: '0.8rem' }}>
                                                  <div style={{ fontWeight: 700, color: 'var(--primary-navy)', marginBottom: '0.15rem' }}>Supporting Evidence:</div>
                                                  <ul style={{ margin: 0, paddingLeft: '1.1rem', color: 'var(--text-secondary)' }}>
                                                    {evidenceList.map((item, evIdx) => (
                                                      <li key={evIdx}>{item}</li>
                                                    ))}
                                                  </ul>
                                                </div>
                                              )}
                                            </div>
                                          </div>
                                        );
                                      })}
                                    </div>
                                  );
                                }

                                return (
                                  <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
                                    Standard support actions available based on assessment outputs.
                                  </div>
                                );
                              })()}
                            </div>

                            {/* 6. ADMINISTRATOR RESOLUTION ACTION BAR */}
                            <div style={{ padding: '1.25rem', background: '#F8FAFC', borderRadius: '10px', border: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
                              <div>
                                <div style={{ fontWeight: 800, color: 'var(--primary-navy)' }}>Administrator Action & Resolution</div>
                                <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Execute human decision-support sign-off.</div>
                              </div>

                              <div style={{ display: 'flex', gap: '0.75rem' }}>
                                <button className="btn btn-success" onClick={() => handleAdminAction(targetCase.caseId, 'ACCEPT', targetCase.finalCategory, 'Accepted AI Assessment')}>
                                  ✓ ACCEPT
                                </button>
                                <button className="btn btn-warning" onClick={() => { setShowModifyModal(true); }}>
                                  ✏️ MODIFY
                                </button>
                                <button className="btn btn-danger" onClick={() => { setShowEscalateModal(true); }}>
                                  ❌ REJECT
                                </button>
                              </div>
                            </div>
                          </div>
                        </div>
                      );
                    })()}
                  </div>
                )}
              </div>
            )}

            {/* PAGE 9: IMMUTABLE AUDIT TRAIL */}
            {activeTab === 'audit' && (
              <div className="card">
                <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--primary-navy)', marginBottom: '1.25rem' }}>Immutable System Audit Trail</h3>
                <div className="table-container">
                  <table>
                    <thead>
                      <tr>
                        <th>Timestamp</th>
                        <th>Case ID</th>
                        <th>Actor</th>
                        <th>Role</th>
                        <th>Action</th>
                        <th>Prev State</th>
                        <th>New State</th>
                        <th>Reason</th>
                      </tr>
                    </thead>
                    <tbody>
                      {auditEvents.map((evt, idx) => (
                        <tr key={idx}>
                          <td style={{ fontSize: '0.8rem' }}>{new Date(evt.timestamp).toLocaleString()}</td>
                          <td style={{ fontWeight: 800, color: 'var(--primary-navy)' }}>{evt.caseId}</td>
                          <td>{evt.actor}</td>
                          <td><span className="badge badge-low">{evt.role}</span></td>
                          <td style={{ fontWeight: 600 }}>{evt.action}</td>
                          <td>{evt.previousState}</td>
                          <td style={{ fontWeight: 700 }}>{evt.newState}</td>
                          <td style={{ fontSize: '0.85rem' }}>{evt.reason}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* PAGE 10: ACTIVE REAL-TIME LOGS */}
            {activeTab === 'logs' && (
              <div className="card">
                <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--primary-navy)', marginBottom: '1.25rem' }}>Real-Time Processing Log Stream</h3>
                <div style={{ backgroundColor: '#1F1815', color: '#C47D2B', padding: '1.25rem', borderRadius: '10px', fontFamily: 'monospace', fontSize: '0.85rem', maxHeight: '500px', overflowY: 'auto' }}>
                  {activeLogs.map((log, idx) => (
                    <div key={idx} style={{ marginBottom: '0.5rem' }}>
                      <span style={{ color: '#8D7B71' }}>[{log.timestamp}]</span> <span style={{ color: '#C47D2B' }}>[{log.actorService}]</span> <span style={{ color: log.severity === 'WARN' ? '#E07A5F' : '#FAF7F2' }}>[{log.status}]</span> {log.eventMessage}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* PAGE 11: SYSTEM SETTINGS */}
            {activeTab === 'settings' && (
              <div className="card" style={{ maxWidth: '650px' }}>
                <h3 style={{ fontSize: '1.2rem', fontWeight: 800, color: 'var(--primary-navy)', marginBottom: '1.5rem' }}>System & Administrator Settings</h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.1rem' }}>
                  <div>
                    <label style={{ display: 'block', fontWeight: 700, fontSize: '0.88rem', color: 'var(--text-primary)', marginBottom: '0.3rem' }}>Administrator Name</label>
                    <input type="text" readOnly value="NHAA Authorized Operator" style={{ width: '100%', padding: '0.65rem', borderRadius: '8px', border: '1px solid var(--border-color)', backgroundColor: '#F8FAFC' }} />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontWeight: 700, fontSize: '0.88rem', color: 'var(--text-primary)', marginBottom: '0.3rem' }}>Role</label>
                    <input type="text" readOnly value="ADMINISTRATOR" style={{ width: '100%', padding: '0.65rem', borderRadius: '8px', border: '1px solid var(--border-color)', backgroundColor: '#F8FAFC' }} />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontWeight: 700, fontSize: '0.88rem', color: 'var(--text-primary)', marginBottom: '0.3rem' }}>Backend Endpoint</label>
                    <input type="text" readOnly value="http://localhost:8088/api/v1" style={{ width: '100%', padding: '0.65rem', borderRadius: '8px', border: '1px solid var(--border-color)', backgroundColor: '#F8FAFC' }} />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontWeight: 700, fontSize: '0.88rem', color: 'var(--text-primary)', marginBottom: '0.3rem' }}>Text ML Service</label>
                    <input type="text" readOnly value="Google MuRIL Base Cased (Port 8001)" style={{ width: '100%', padding: '0.65rem', borderRadius: '8px', border: '1px solid var(--border-color)', backgroundColor: '#F8FAFC' }} />
                  </div>
                  <div>
                    <label style={{ display: 'block', fontWeight: 700, fontSize: '0.88rem', color: 'var(--text-primary)', marginBottom: '0.3rem' }}>Voice ML Service</label>
                    <input type="text" readOnly value="Acoustic & Wav2Vec2 Feature Extractor (Port 8002)" style={{ width: '100%', padding: '0.65rem', borderRadius: '8px', border: '1px solid var(--border-color)', backgroundColor: '#F8FAFC' }} />
                  </div>
                </div>
              </div>
            )}

            {/* MODIFY MODAL */}
            {showModifyModal && selectedCase && (
              <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(44, 34, 30, 0.7)', backdropFilter: 'blur(3px)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 2000 }}>
                <div className="card" style={{ width: '450px', margin: 0, backgroundColor: '#FAF7F2', border: '1px solid #E6DFD5', boxShadow: '0 20px 40px rgba(44, 34, 30, 0.25)' }}>
                  <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--primary-navy)', marginBottom: '1rem' }}>Modify Case Risk Category</h3>
                  <div style={{ marginBottom: '1rem' }}>
                    <label style={{ display: 'block', fontWeight: 700, fontSize: '0.85rem', color: '#2C221E', marginBottom: '0.3rem' }}>Select New Final Category</label>
                    <select value={modifyCategory} onChange={e => setModifyCategory(e.target.value)} style={{ width: '100%', padding: '0.65rem', borderRadius: '8px', border: '1px solid #E6DFD5', backgroundColor: '#FFFFFF', color: '#2C221E', fontWeight: 600 }}>
                      <option value="LOW">LOW</option>
                      <option value="MODERATE">MODERATE</option>
                      <option value="HIGH">HIGH</option>
                      <option value="CRITICAL">CRITICAL</option>
                    </select>
                  </div>
                  <div style={{ marginBottom: '1.5rem' }}>
                    <label style={{ display: 'block', fontWeight: 700, fontSize: '0.85rem', color: '#2C221E', marginBottom: '0.3rem' }}>Modification Rationale / Notes</label>
                    <textarea rows={3} value={actionNotes} onChange={e => setActionNotes(e.target.value)} placeholder="Provide explanation for override..." style={{ width: '100%', padding: '0.65rem', borderRadius: '8px', border: '1px solid #E6DFD5', backgroundColor: '#FFFFFF', color: '#2C221E' }} />
                  </div>
                  <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                    <button className="btn btn-outline" onClick={() => setShowModifyModal(false)}>Cancel</button>
                    <button className="btn btn-warning" onClick={() => handleAdminAction(selectedCase.caseId, 'MODIFY', modifyCategory, actionNotes)}>Save Modification</button>
                  </div>
                </div>
              </div>
            )}

            {/* REJECT MODAL */}
            {showEscalateModal && selectedCase && (
              <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(44, 34, 30, 0.7)', backdropFilter: 'blur(3px)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 2000 }}>
                <div className="card" style={{ width: '450px', margin: 0, backgroundColor: '#FAF7F2', border: '1px solid #E6DFD5', boxShadow: '0 20px 40px rgba(44, 34, 30, 0.25)' }}>
                  <h3 style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--risk-critical)', marginBottom: '1rem' }}>❌ Reject Case Assessment</h3>
                  <div style={{ marginBottom: '1.5rem' }}>
                    <label style={{ display: 'block', fontWeight: 700, fontSize: '0.85rem', color: '#2C221E', marginBottom: '0.3rem' }}>Rejection Reason & Administrative Notes</label>
                    <textarea rows={4} value={actionNotes} onChange={e => setActionNotes(e.target.value)} placeholder="Describe reason for rejection..." style={{ width: '100%', padding: '0.65rem', borderRadius: '8px', border: '1px solid #E6DFD5', backgroundColor: '#FFFFFF', color: '#2C221E' }} />
                  </div>
                  <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                    <button className="btn btn-outline" onClick={() => setShowEscalateModal(false)}>Cancel</button>
                    <button className="btn btn-danger" onClick={() => handleAdminAction(selectedCase.caseId, 'REJECT', selectedCase.finalCategory || 'LOW', actionNotes)}>Confirm Rejection</button>
                  </div>
                </div>
              </div>
            )}

            {/* ACTION CONFIRMATION SUCCESS MODAL (LIGHT SHADE OF BROWN) */}
            {actionModal.show && (
              <div style={{
                position: 'fixed',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                backgroundColor: 'rgba(44, 34, 30, 0.65)',
                backdropFilter: 'blur(4px)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 2500,
                animation: 'fadeIn 0.2s ease-out'
              }}>
                <div style={{
                  backgroundColor: '#FAF7F2',
                  border: '1px solid #E6DFD5',
                  borderRadius: '16px',
                  padding: '2.5rem 2rem 2rem 2rem',
                  maxWidth: '480px',
                  width: '90%',
                  textAlign: 'center',
                  boxShadow: '0 25px 50px -12px rgba(44, 34, 30, 0.35), 0 0 0 1px rgba(139, 58, 43, 0.1)',
                  animation: 'scaleIn 0.25s ease-out'
                }}>
                  {/* Circle Icon Badge */}
                  <div style={{
                    width: '76px',
                    height: '76px',
                    borderRadius: '50%',
                    margin: '0 auto 1.25rem auto',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    border: actionModal.type === 'ACCEPT' ? '3px dashed #22C55E' : actionModal.type === 'MODIFY' ? '3px dashed #F59E0B' : '3px dashed #EF4444',
                    backgroundColor: actionModal.type === 'ACCEPT' ? '#E6F4EA' : actionModal.type === 'MODIFY' ? '#FEF3C7' : '#FEE2E2'
                  }}>
                    {actionModal.type === 'ACCEPT' && (
                      <span style={{ color: '#16A34A', fontSize: '2.5rem', fontWeight: 900, lineHeight: 1 }}>✓</span>
                    )}
                    {actionModal.type === 'MODIFY' && (
                      <span style={{ color: '#D97706', fontSize: '2.2rem', lineHeight: 1 }}>✏️</span>
                    )}
                    {actionModal.type === 'REJECT' && (
                      <span style={{ color: '#DC2626', fontSize: '2.2rem', lineHeight: 1 }}>✕</span>
                    )}
                  </div>

                  {/* Title */}
                  <h2 style={{
                    fontSize: '1.65rem',
                    fontWeight: 800,
                    color: actionModal.type === 'ACCEPT' ? '#15803D' : actionModal.type === 'MODIFY' ? '#B45309' : '#991B1B',
                    marginBottom: '0.6rem'
                  }}>
                    {actionModal.title}
                  </h2>

                  {/* Description / Content text */}
                  <p style={{
                    fontSize: '0.95rem',
                    color: '#4E3E35',
                    lineHeight: 1.55,
                    fontWeight: 500,
                    marginBottom: actionModal.type !== 'REJECT' ? '1rem' : '1.75rem'
                  }}>
                    {actionModal.message}
                  </p>

                  {/* Redirected Departments list for ACCEPT & MODIFY */}
                  {actionModal.type !== 'REJECT' && actionModal.departments.length > 0 && (
                    <div style={{
                      backgroundColor: '#F3ECE2',
                      borderRadius: '12px',
                      padding: '0.85rem 1rem',
                      border: '1px solid #E2D4C3',
                      marginBottom: '1.75rem',
                      textAlign: 'left'
                    }}>
                      <div style={{ fontSize: '0.78rem', fontWeight: 700, color: '#6E6259', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '0.5rem' }}>
                        📡 Redirected Departments ({actionModal.departments.length}):
                      </div>
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.4rem' }}>
                        {actionModal.departments.map((dept, idx) => (
                          <span key={idx} style={{
                            fontSize: '0.78rem',
                            fontWeight: 700,
                            backgroundColor: '#FFFFFF',
                            color: '#7A5230',
                            border: '1px solid #D4C9BC',
                            padding: '0.25rem 0.65rem',
                            borderRadius: '6px',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.3rem'
                          }}>
                            {dept === 'Emergency Support' && '🚨'}
                            {dept === 'Police Intervention' && '🛡️'}
                            {dept === 'Medical Assistance' && '🏥'}
                            {dept === 'Legal Aid' && '⚖️'}
                            {dept === 'Psychosocial Counselling' && '🧠'}
                            {dept === 'Witness Protection' && '🔒'}
                            {dept === 'Rehabilitation & Shelter' && '🏘️'}
                            {dept}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* CONTINUE Button */}
                  <button
                    style={{
                      width: '100%',
                      padding: '0.85rem 1.5rem',
                      fontSize: '1rem',
                      fontWeight: 800,
                      color: '#ffffff',
                      backgroundColor: actionModal.type === 'ACCEPT' ? '#15803D' : actionModal.type === 'MODIFY' ? '#C47D2B' : '#7B1E1E',
                      border: 'none',
                      borderRadius: '10px',
                      cursor: 'pointer',
                      textTransform: 'uppercase',
                      letterSpacing: '0.05em',
                      boxShadow: actionModal.type === 'ACCEPT' ? '0 4px 14px rgba(21, 128, 61, 0.35)' : actionModal.type === 'MODIFY' ? '0 4px 14px rgba(196, 125, 43, 0.35)' : '0 4px 14px rgba(123, 30, 30, 0.35)',
                      transition: 'all 0.15s ease'
                    }}
                    onClick={() => {
                      setActionModal({ ...actionModal, show: false });
                      setSelectedCase(null);
                      setActiveTab('dashboard');
                    }}
                  >
                    CONTINUE
                  </button>
                </div>
              </div>
            )}


          </div>
        </main>
      </div>
    </div>
  );
}
