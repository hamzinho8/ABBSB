import React, { useState, useEffect, useRef } from 'react';
import { 
  Phone, 
  PhoneCall, 
  PhoneIncoming, 
  PhoneOutgoing, 
  PhoneMissed, 
  PhoneOff, 
  Music, 
  Disc3, 
  Play, 
  Pause, 
  SkipBack, 
  SkipForward, 
  Volume2, 
  VolumeX, 
  Users, 
  User, 
  Clock, 
  Search, 
  History, 
  RefreshCw, 
  X, 
  Radio, 
  ArrowUpRight, 
  ArrowDownLeft, 
  Shield, 
  Smartphone, 
  Send, 
  Mic, 
  MicOff, 
  Wifi, 
  Battery, 
  Layers, 
  CheckCircle, 
  Sliders, 
  Plus, 
  ChevronRight, 
  Maximize2,
  MessageSquare,
  CheckCheck
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { CallHistoryModal, CallRecord } from './components/CallHistoryModal';
import { ContactsModal, ContactItem } from './components/ContactsModal';
import { MediaControllerModal } from './components/MediaControllerModal';
import { WhatsAppModal, WhatsAppMessageItem } from './components/WhatsAppModal';

// Initial realistic call history (Émis, Reçus, Manqués)
const INITIAL_CALL_RECORDS: CallRecord[] = [
  {
    id: 'call_1',
    callerName: 'Amina Mansouri',
    phoneNumber: '+212634934134',
    timestamp: 'Aujourd\'hui 14:28',
    direction: 'incoming',
    duration: '04:12',
    simName: 'inwi',
    status: 'answered'
  },
  {
    id: 'call_2',
    callerName: 'Youssef Bennani',
    phoneNumber: '+212655881230',
    timestamp: 'Aujourd\'hui 12:15',
    direction: 'outgoing',
    duration: '01:45',
    simName: 'Orange',
    status: 'answered'
  },
  {
    id: 'call_3',
    callerName: 'Hamza H.',
    phoneNumber: '+212611223344',
    timestamp: 'Aujourd\'hui 10:04',
    direction: 'incoming',
    duration: '08:30',
    simName: 'inwi',
    status: 'answered'
  },
  {
    id: 'call_4',
    callerName: 'Service Client inwi',
    phoneNumber: '220',
    timestamp: 'Hier 18:40',
    direction: 'outgoing',
    duration: '02:10',
    simName: 'inwi',
    status: 'answered'
  },
  {
    id: 'call_5',
    callerName: 'Dr. Karim Lahlou',
    phoneNumber: '+212672409918',
    timestamp: 'Hier 15:22',
    direction: 'incoming',
    duration: 'Manqué',
    simName: 'Orange',
    status: 'missed'
  },
  {
    id: 'call_6',
    callerName: 'Fatima Zahra',
    phoneNumber: '+212698712345',
    timestamp: '24 Sep 11:15',
    direction: 'incoming',
    duration: '05:20',
    simName: 'inwi',
    status: 'answered'
  },
  {
    id: 'call_7',
    callerName: 'Orange Recharges',
    phoneNumber: '121',
    timestamp: '24 Sep 09:30',
    direction: 'outgoing',
    duration: '03:05',
    simName: 'Orange',
    status: 'answered'
  },
  {
    id: 'call_8',
    callerName: 'Sara Alami',
    phoneNumber: '+212644332211',
    timestamp: '23 Sep 16:50',
    direction: 'incoming',
    duration: 'Manqué',
    simName: 'inwi',
    status: 'missed'
  }
];

const INITIAL_CONTACTS: ContactItem[] = [
  { id: '1', name: 'Amina Mansouri', number: '+212634934134', isVip: true, notes: 'Famille / Mobile inwi' },
  { id: '2', name: 'Youssef Bennani', number: '+212655881230', isVip: true, notes: 'Bureau / Orange' },
  { id: '3', name: 'Hamza H.', number: '+212611223344', isVip: true, notes: 'Ingénieur BlackBerry Bridge' },
  { id: '4', name: 'Dr. Karim Lahlou', number: '+212672409918', isVip: true, notes: 'Clinique / Urgences' },
  { id: '5', name: 'Fatima Zahra', number: '+212698712345', isVip: true, notes: 'Mobile' },
  { id: '6', name: 'Sara Alami', number: '+212644332211', isVip: true, notes: 'Personnel' },
  { id: '7', name: 'Service Client inwi', number: '220', isVip: true, notes: 'Assistance Télécom inwi' },
  { id: '8', name: 'Service Client Orange', number: '121', isVip: true, notes: 'Assistance Télécom Orange' }
];

export default function App() {
  // Navigation / Tabs
  const [activeTab, setActiveTab] = useState<'calls' | 'whatsapp' | 'media' | 'contacts' | 'logs'>('calls');
  
  // Modals state
  const [isMediaModalOpen, setIsMediaModalOpen] = useState(false);
  const [isContactsModalOpen, setIsContactsModalOpen] = useState(false);
  const [isCallHistoryModalOpen, setIsCallHistoryModalOpen] = useState(false);
  const [isWhatsAppModalOpen, setIsWhatsAppModalOpen] = useState(false);

  // WhatsApp Module State
  const [whatsAppCall, setWhatsAppCall] = useState<{
    id: string;
    name: string;
    state: 'ringing' | 'connected' | 'ended';
    duration: number;
  } | null>(null);

  const [whatsAppMessages, setWhatsAppMessages] = useState<WhatsAppMessageItem[]>([
    { notifId: 'wa_1', senderName: 'Karim Bennani', body: 'Salut Hamza, tu es disponible pour le point d\'avancement ?', timestamp: '15:20', isOutgoing: false },
    { notifId: 'wa_2', senderName: 'Dr. Lahlou', body: 'Les résultats sont prêts, rappelle-moi dès que tu peux.', timestamp: '14:45', isOutgoing: false },
    { notifId: 'wa_3', senderName: 'Moi', body: 'Message reçu ! Je prépare le dossier.', timestamp: 'Hier', isOutgoing: true, isConfirmed: true },
    { notifId: 'wa_4', senderName: 'Groupe Dev BlackBerry', body: 'Le build OS 6.0 pour Curve 9300 est validé !', timestamp: 'Hier', isOutgoing: false },
  ]);
  const [whatsAppReplyText, setWhatsAppReplyText] = useState('');

  // Call & History State
  const [callRecords, setCallRecords] = useState<CallRecord[]>(INITIAL_CALL_RECORDS);
  const [contacts, setContacts] = useState<ContactItem[]>(INITIAL_CONTACTS);
  const [historySearch, setHistorySearch] = useState('');
  const [historyFilter, setHistoryFilter] = useState<'all' | 'outgoing' | 'incoming' | 'missed'>('all');

  // Phone Dialer State
  const [dialerNumber, setDialerNumber] = useState('');
  const [selectedSim, setSelectedSim] = useState<'inwi' | 'Orange'>('inwi');

  // Active Call State
  const [activeCall, setActiveCall] = useState<{
    id: string;
    name: string;
    number: string;
    simName: string;
    isOutbound: boolean;
    state: 'ringing' | 'connected' | 'ended';
    duration: number;
    speakerOn: boolean;
    micMuted: boolean;
  } | null>(null);

  // Media Player State
  const [isPlaying, setIsPlaying] = useState(false);
  const [mediaTitle, setMediaTitle] = useState('Flux Audio HD');
  const [mediaArtist, setMediaArtist] = useState('Smartphone Android');
  const [mediaProgress, setMediaProgress] = useState(42);
  const [mediaVolume, setMediaVolume] = useState(85);
  const [isMuted, setIsMuted] = useState(false);
  const [audioStreamingActive, setAudioStreamingActive] = useState(true);
  const [chunksSentCount, setChunksSentCount] = useState(148);

  // Bluetooth Protocol Logs
  const [btLogs, setBtLogs] = useState<Array<{ id: number; time: string; text: string; type: 'tx' | 'rx' | 'sys' }>>([
    { id: 1, time: '14:28:10', text: 'RX: CALL_INCOMING|101|Amina Mansouri|+212634934134|inwi', type: 'rx' },
    { id: 2, time: '14:28:11', text: 'TX: CALL_ANSWER|101', type: 'tx' },
    { id: 3, time: '14:28:12', text: 'RX: CALL_ACTIVE|101|inwi', type: 'rx' },
    { id: 4, time: '14:32:22', text: 'RX: CALL_END|101', type: 'rx' },
    { id: 5, time: '14:32:23', text: 'SYS: Appel enregistré dans l\'historique (04:12)', type: 'sys' },
    { id: 6, time: '14:35:00', text: 'TX: AUDIO_START', type: 'tx' },
    { id: 7, time: '14:35:01', text: 'TX: AUDIO_CHUNK|UklGRs... [4044 bytes WAV - 250ms Zero-Lag]', type: 'tx' }
  ]);

  // Audio Chunk Simulator Timer (250 ms strict real-time chunks)
  useEffect(() => {
    if (!audioStreamingActive && !isPlaying) return;
    const interval = setInterval(() => {
      setChunksSentCount(prev => prev + 1);
      if (isPlaying) {
        setMediaProgress(prev => (prev >= 100 ? 0 : prev + 1));
      }
    }, 250); // 250ms audio chunks (4 packets per second)
    return () => clearInterval(interval);
  }, [audioStreamingActive, isPlaying]);

  // In-call duration timer
  useEffect(() => {
    let timer: NodeJS.Timeout | null = null;
    if (activeCall && activeCall.state === 'connected') {
      timer = setInterval(() => {
        setActiveCall(prev => {
          if (!prev) return null;
          return { ...prev, duration: prev.duration + 1 };
        });
      }, 1000);
    }
    return () => {
      if (timer) clearInterval(timer);
    };
  }, [activeCall?.state]);

  const addLog = (text: string, type: 'tx' | 'rx' | 'sys') => {
    const time = new Date().toTimeString().split(' ')[0];
    setBtLogs(prev => [...prev.slice(-30), { id: Date.now() + Math.random(), time, text, type }]);
  };

  // Dial / Initiate Outbound Call
  const startCall = (number: string, contactName?: string, simOverride?: 'inwi' | 'Orange') => {
    const targetNumber = number.trim();
    if (!targetNumber) return;
    const sim = simOverride || selectedSim;
    const contact = contacts.find(c => c.number === targetNumber);
    const displayName = contactName || contact?.name || targetNumber;

    const newCallId = 'call_' + Date.now();
    addLog(`TX: CALL_OUTBOUND|${targetNumber}|${sim === 'inwi' ? '0' : '1'}`, 'tx');

    setActiveCall({
      id: newCallId,
      name: displayName,
      number: targetNumber,
      simName: sim,
      isOutbound: true,
      state: 'connected', // Connect immediately for smooth simulation
      duration: 0,
      speakerOn: true,
      micMuted: false
    });

    addLog(`RX: CALL_OUTBOUND_OK|${targetNumber}|${sim}`, 'rx');
    addLog(`RX: CALL_ACTIVE|${newCallId}|${sim}`, 'rx');
    setDialerNumber('');
  };

  // Simulate Incoming Call
  const simulateIncomingCall = (name = 'Dr. Karim Lahlou', number = '+212672409918', sim: 'inwi' | 'Orange' = 'Orange') => {
    const newCallId = 'inc_' + Date.now();
    addLog(`RX: CALL_INCOMING|${newCallId}|${name}|${number}|${sim}`, 'rx');

    setActiveCall({
      id: newCallId,
      name,
      number,
      simName: sim,
      isOutbound: false,
      state: 'ringing',
      duration: 0,
      speakerOn: true,
      micMuted: false
    });
  };

  // Answer Call
  const answerCall = () => {
    if (!activeCall) return;
    addLog(`TX: CALL_ANSWER|${activeCall.id}`, 'tx');
    setActiveCall(prev => {
      if (!prev) return null;
      return { ...prev, state: 'connected', duration: 0 };
    });
    addLog(`RX: CALL_ACTIVE|${activeCall.id}|${activeCall.simName}`, 'rx');
  };

  // End / Reject Call & Save to REAL Call History
  const endCall = () => {
    if (!activeCall) return;
    const isMissed = !activeCall.isOutbound && activeCall.state === 'ringing';
    addLog(isMissed ? `TX: CALL_REJECT|${activeCall.id}` : `TX: CALL_END|${activeCall.id}`, 'tx');

    const durationStr = isMissed 
      ? 'Manqué' 
      : `${String(Math.floor(activeCall.duration / 60)).padStart(2, '0')}:${String(activeCall.duration % 60).padStart(2, '0')}`;

    const newRecord: CallRecord = {
      id: 'rec_' + Date.now(),
      callerName: activeCall.name,
      phoneNumber: activeCall.number,
      timestamp: 'À l\'instant',
      direction: activeCall.isOutbound ? 'outgoing' : 'incoming',
      duration: durationStr,
      simName: activeCall.simName,
      status: isMissed ? 'missed' : 'answered'
    };

    // Prepend to real call history!
    setCallRecords(prev => [newRecord, ...prev]);
    addLog(`SYS: Appel enregistré dans l'historique (${newRecord.direction === 'outgoing' ? 'Émis' : isMissed ? 'Manqué' : 'Reçu'} - ${durationStr})`, 'sys');

    setActiveCall(prev => prev ? { ...prev, state: 'ended' } : null);
    setTimeout(() => {
      setActiveCall(null);
    }, 1200);
  };

  // Toggle Mute / Speaker
  const toggleMute = () => {
    if (!activeCall) return;
    const next = !activeCall.micMuted;
    setActiveCall(prev => prev ? { ...prev, micMuted: next } : null);
    addLog(`TX: MUTE_TOGGLE|${next ? '1' : '0'}`, 'tx');
  };

  const toggleSpeaker = () => {
    if (!activeCall) return;
    const next = !activeCall.speakerOn;
    setActiveCall(prev => prev ? { ...prev, speakerOn: next } : null);
    addLog(`TX: SPEAKER_TOGGLE|${next ? '1' : '0'}`, 'tx');
  };

  // WhatsApp Call Duration Timer
  useEffect(() => {
    let timer: NodeJS.Timeout | null = null;
    if (whatsAppCall && whatsAppCall.state === 'connected') {
      timer = setInterval(() => {
        setWhatsAppCall(prev => prev ? { ...prev, duration: prev.duration + 1 } : null);
      }, 1000);
    }
    return () => {
      if (timer) clearInterval(timer);
    };
  }, [whatsAppCall?.state]);

  // WhatsApp Protocol Methods
  const simulateIncomingWhatsAppCall = (name = 'Karim Bennani') => {
    const callId = 'wa_' + Date.now();
    addLog(`RX: WHATSAPP_CALL_INCOMING|${callId}|${name}`, 'rx');
    setWhatsAppCall({
      id: callId,
      name,
      state: 'ringing',
      duration: 0
    });
  };

  const answerWhatsAppCall = () => {
    if (!whatsAppCall) return;
    addLog(`TX: WHATSAPP_CALL_ANSWER|${whatsAppCall.id}`, 'tx');
    setWhatsAppCall(prev => prev ? { ...prev, state: 'connected', duration: 0 } : null);
  };

  const rejectWhatsAppCall = () => {
    if (!whatsAppCall) return;
    addLog(`TX: WHATSAPP_CALL_REJECT|${whatsAppCall.id}`, 'tx');
    setWhatsAppCall(prev => prev ? { ...prev, state: 'ended' } : null);
    setTimeout(() => {
      setWhatsAppCall(null);
    }, 1000);
  };

  const handleSendWhatsAppReply = (notifId: string, text: string) => {
    if (!text.trim()) return;
    const cleanText = text.trim();
    let b64 = '';
    try {
      b64 = btoa(unescape(encodeURIComponent(cleanText)));
    } catch (e) {
      b64 = btoa(cleanText);
    }
    addLog(`TX: WHATSAPP_REPLY|${notifId}|${b64}`, 'tx');
    const newMsg: WhatsAppMessageItem = {
      notifId,
      senderName: 'Moi',
      body: cleanText,
      timestamp: new Date().toTimeString().slice(0, 5),
      isOutgoing: true,
      isConfirmed: false
    };
    setWhatsAppMessages(prev => [...prev, newMsg]);
    setWhatsAppReplyText('');

    // Simulated Android auto-confirmation via WHATSAPP_REPLY_OK
    setTimeout(() => {
      addLog(`RX: WHATSAPP_REPLY_OK|${notifId}`, 'rx');
      setWhatsAppMessages(prev => prev.map(m => m.notifId === notifId ? { ...m, isConfirmed: true } : m));
    }, 1200);
  };

  const handleStartWhatsAppChat = (phone: string, text: string) => {
    const cleanPhone = phone.trim();
    const cleanMsg = text.trim();
    let b64 = '';
    try {
      b64 = btoa(unescape(encodeURIComponent(cleanMsg)));
    } catch (e) {
      b64 = btoa(cleanMsg);
    }
    addLog(`TX: WHATSAPP_START_CHAT|${cleanPhone}|${b64}`, 'tx');
    const newMsg: WhatsAppMessageItem = {
      notifId: 'wa_' + Date.now(),
      senderName: cleanPhone,
      body: cleanMsg,
      timestamp: new Date().toTimeString().slice(0, 5),
      isOutgoing: true,
      isConfirmed: true
    };
    setWhatsAppMessages(prev => [...prev, newMsg]);
  };

  const handleIncomingWhatsAppMessage = (sender: string, text: string) => {
    const notifId = 'wa_' + Date.now();
    let b64 = '';
    try {
      b64 = btoa(unescape(encodeURIComponent(text)));
    } catch (e) {
      b64 = btoa(text);
    }
    const time = new Date().toTimeString().slice(0, 5);
    addLog(`RX: WHATSAPP_MSG|${notifId}|${sender}|${b64}|${time}`, 'rx');
    setWhatsAppMessages(prev => [...prev, {
      notifId,
      senderName: sender,
      body: text,
      timestamp: time,
      isOutgoing: false
    }]);
  };

  const handleConfirmReply = (notifId: string) => {
    addLog(`RX: WHATSAPP_REPLY_OK|${notifId}`, 'rx');
    setWhatsAppMessages(prev => prev.map(m => m.notifId === notifId ? { ...m, isConfirmed: true } : m));
  };

  // Filtered Call History
  const filteredHistory = callRecords.filter(record => {
    if (historyFilter === 'outgoing' && record.direction !== 'outgoing') return false;
    if (historyFilter === 'incoming' && (record.direction !== 'incoming' || record.status === 'missed')) return false;
    if (historyFilter === 'missed' && record.status !== 'missed') return false;
    if (historySearch.trim() !== '') {
      const q = historySearch.toLowerCase();
      return (
        record.callerName.toLowerCase().includes(q) ||
        record.phoneNumber.includes(q) ||
        record.simName?.toLowerCase().includes(q)
      );
    }
    return true;
  });

  return (
    <div className="min-h-screen bg-[#0A0E14] text-white font-sans flex flex-col items-center justify-start p-2 sm:p-6 select-none">
      
      {/* Top Main Navigation Bar */}
      <header className="w-full max-w-6xl bg-[#11161F] border border-[#232F3E] rounded-2xl p-3 sm:p-4 mb-4 sm:mb-6 shadow-xl flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="relative">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-cyan-600 to-blue-500 flex items-center justify-center shadow-lg shadow-cyan-500/20">
              <Smartphone className="w-5 h-5 text-white" />
            </div>
            <span className="absolute -top-1 -right-1 w-3.5 h-3.5 bg-green-500 border-2 border-[#11161F] rounded-full animate-pulse" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base sm:text-lg font-bold text-white tracking-wide">
                BlackBerry Curve 9300 <span className="text-cyan-400 font-normal">Bridge</span>
              </h1>
              <span className="hidden sm:inline-block px-2 py-0.5 rounded text-[10px] font-mono bg-blue-500/20 text-blue-300 border border-blue-500/40">
                RIM OS 5.0 - 7.1
              </span>
            </div>
            <p className="text-xs text-gray-400 flex items-center gap-2">
              <span>Pixel 7 Pro</span>
              <span className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
              <span className="text-green-400">Bluetooth RFCOMM &amp; Audio SCO Connecté</span>
            </p>
          </div>
        </div>

        {/* Quick Access Action Buttons (Media, Contacts, Call History) */}
        <div className="flex items-center gap-2 flex-wrap">
          {/* Media Button */}
          <button
            onClick={() => {
              setActiveTab('media');
              setIsMediaModalOpen(true);
            }}
            className="px-3 py-2 rounded-xl bg-[#1B2230] hover:bg-[#232C3D] border border-purple-500/40 text-purple-300 text-xs font-semibold flex items-center gap-2 transition-all hover:scale-[1.02] cursor-pointer"
          >
            <Music className="w-4 h-4 text-purple-400" />
            <span>Média</span>
            {isPlaying && (
              <span className="w-2 h-2 rounded-full bg-green-400 animate-ping" />
            )}
          </button>

          {/* Contacts Button */}
          <button
            onClick={() => {
              setActiveTab('contacts');
              setIsContactsModalOpen(true);
            }}
            className="px-3 py-2 rounded-xl bg-[#1B2230] hover:bg-[#232C3D] border border-cyan-500/40 text-cyan-300 text-xs font-semibold flex items-center gap-2 transition-all hover:scale-[1.02] cursor-pointer"
          >
            <Users className="w-4 h-4 text-cyan-400" />
            <span>Contacts</span>
            <span className="px-1.5 py-0.2 rounded-full bg-cyan-500/20 text-[10px] font-mono">
              {contacts.length}
            </span>
          </button>

          {/* Call History Modal Button */}
          <button
            onClick={() => {
              setActiveTab('calls');
              setIsCallHistoryModalOpen(true);
            }}
            className="px-3 py-2 rounded-xl bg-[#1B2230] hover:bg-[#232C3D] border border-yellow-500/40 text-yellow-300 text-xs font-semibold flex items-center gap-2 transition-all hover:scale-[1.02] cursor-pointer"
          >
            <History className="w-4 h-4 text-yellow-400" />
            <span>Journal</span>
            <span className="px-1.5 py-0.2 rounded-full bg-yellow-500/20 text-[10px] font-mono">
              {callRecords.length}
            </span>
          </button>

          {/* WhatsApp Modal Button */}
          <button
            onClick={() => {
              setActiveTab('whatsapp');
              setIsWhatsAppModalOpen(true);
            }}
            className="px-3 py-2 rounded-xl bg-[#1B2230] hover:bg-[#232C3D] border border-emerald-500/50 text-emerald-300 text-xs font-semibold flex items-center gap-2 transition-all hover:scale-[1.02] cursor-pointer shadow-lg shadow-emerald-900/20"
          >
            <MessageSquare className="w-4 h-4 text-emerald-400" />
            <span>WhatsApp</span>
            {whatsAppCall ? (
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
            ) : (
              <span className="px-1.5 py-0.2 rounded-full bg-emerald-500/20 text-[10px] font-mono text-emerald-300">
                {whatsAppMessages.length}
              </span>
            )}
          </button>

          {/* Simulate Call Button */}
          <button
            onClick={() => simulateIncomingCall()}
            className="px-3 py-2 rounded-xl bg-green-600 hover:bg-green-500 text-white text-xs font-semibold flex items-center gap-2 transition-all hover:scale-[1.02] cursor-pointer shadow-lg shadow-green-600/20"
          >
            <PhoneIncoming className="w-4 h-4 animate-bounce" />
            <span className="hidden sm:inline">Simuler Appel</span>
          </button>

          {/* Simulate WhatsApp Call Button */}
          <button
            onClick={() => simulateIncomingWhatsAppCall('Karim Bennani')}
            className="px-3 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold flex items-center gap-2 transition-all hover:scale-[1.02] cursor-pointer shadow-lg shadow-emerald-600/20"
          >
            <PhoneIncoming className="w-4 h-4" />
            <span className="hidden sm:inline">Appel WhatsApp</span>
          </button>
        </div>
      </header>

      {/* Main Grid: Left Column = BlackBerry Curve 9300 Hardware & Emulation, Right Column = Control Station */}
      <div className="w-full max-w-6xl grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        
        {/* =============================================================== */}
        {/* LEFT COLUMN: BlackBerry Curve 9300 Realistic Hardware Device   */}
        {/* =============================================================== */}
        <div className="lg:col-span-5 flex flex-col items-center">
          
          {/* Curve 9300 Chassis / Outer Case */}
          <div className="w-[360px] max-w-full bg-gradient-to-b from-[#1C1F26] via-[#12151B] to-[#0A0D12] p-4 rounded-[42px] border-[5px] border-[#2E3748] shadow-[0_20px_50px_rgba(0,0,0,0.8),inset_0_2px_4px_rgba(255,255,255,0.15)] flex flex-col items-center">
            
            {/* Top Earpiece Grill & Notification LED */}
            <div className="w-full flex items-center justify-between px-6 pt-1 pb-3">
              <div className="text-[10px] tracking-widest font-mono text-gray-500 uppercase font-black">
                BlackBerry
              </div>
              <div className="w-16 h-1.5 bg-[#080A0D] rounded-full border border-[#2D3748] shadow-inner" />
              <div className={`w-2.5 h-2.5 rounded-full ${activeCall ? 'bg-red-500 animate-ping' : isPlaying ? 'bg-purple-500 animate-pulse' : 'bg-green-500'} shadow-[0_0_8px_currentColor]`} />
            </div>

            {/* LCD Screen Bezel (Curve 9300 320x240 Aspect Ratio) */}
            <div className="w-[320px] h-[240px] bg-[#0A0E14] border-[3px] border-[#181D26] rounded-xl shadow-inner overflow-hidden flex flex-col relative font-sans">
              
              {/* Screen Top Status Bar (20px) */}
              <div className="h-5 bg-[#05070B] border-b border-[#1E293B] px-2 flex items-center justify-between text-[10px] text-gray-300 shrink-0">
                <div className="flex items-center gap-1.5">
                  <Wifi className="w-2.5 h-2.5 text-cyan-400" />
                  <span className="font-bold text-[9px] text-cyan-300">inwi | Orange</span>
                </div>
                <div className="flex items-center gap-2">
                  {audioStreamingActive && (
                    <span className="text-[8px] px-1 rounded bg-yellow-500/20 text-yellow-300 font-mono">SCO</span>
                  )}
                  <span className="text-[9px] font-mono text-gray-300">14:32</span>
                  <div className="flex items-center text-green-400 gap-0.5">
                    <span className="text-[9px]">88%</span>
                    <Battery className="w-3 h-3" />
                  </div>
                </div>
              </div>

              {/* LCD Screen Content Area */}
              <div className="flex-1 p-2 flex flex-col justify-between overflow-hidden">
                
                {/* Condition 0: WhatsApp Incoming Call Overlay (FullScreen WhatsAppIncomingCallScreen replica) */}
                {whatsAppCall ? (
                  <div className="flex-1 flex flex-col justify-between items-center text-center bg-[#0B141A] -m-2 p-2 animate-in fade-in duration-150">
                    <div className="w-full bg-[#128C7E] py-0.5 px-2 rounded text-[9px] font-bold text-white tracking-wider flex items-center justify-center gap-1 shadow-sm">
                      <MessageSquare className="w-3 h-3 text-emerald-300" />
                      <span>APPEL VOCAL WHATSAPP</span>
                    </div>

                    <div className="my-auto flex flex-col items-center py-1">
                      <div className="w-9 h-9 rounded-full bg-[#25D366] flex items-center justify-center text-white font-black text-xs shadow-md mb-1">
                        WA
                      </div>
                      <div className="text-sm font-bold text-white leading-tight truncate max-w-[260px]">
                        {whatsAppCall.name}
                      </div>
                      <div className="text-[10px] text-[#8696A0] mt-0.5 font-medium">
                        {whatsAppCall.state === 'ringing' ? 'Appel entrant via Android...' : 'Audio vocal connecté (SPP 16 kHz)'}
                      </div>
                      {whatsAppCall.state === 'connected' && (
                        <div className="text-base font-mono font-bold text-[#25D366] mt-1 bg-[#111B21] px-3 py-0.5 rounded border border-[#25D366]/40">
                          {`${String(Math.floor(whatsAppCall.duration / 60)).padStart(2, '0')}:${String(whatsAppCall.duration % 60).padStart(2, '0')}`}
                        </div>
                      )}
                    </div>

                    <div className="w-full border-t border-[#202C33] pt-1.5 flex items-center justify-between text-[9px] px-1">
                      {whatsAppCall.state === 'ringing' ? (
                        <>
                          <button
                            onClick={answerWhatsAppCall}
                            className="px-2 py-0.5 bg-emerald-700 hover:bg-emerald-600 text-white rounded font-bold cursor-pointer flex items-center gap-1 shadow"
                          >
                            [ Verte ] Décrocher
                          </button>
                          <button
                            onClick={rejectWhatsAppCall}
                            className="px-2 py-0.5 bg-red-700 hover:bg-red-600 text-white rounded font-bold cursor-pointer flex items-center gap-1 shadow"
                          >
                            [ Rouge ] Rejeter
                          </button>
                        </>
                      ) : (
                        <div className="w-full flex justify-center">
                          <button
                            onClick={rejectWhatsAppCall}
                            className="px-3 py-1 bg-red-700 hover:bg-red-600 text-white rounded font-bold cursor-pointer shadow text-[10px]"
                          >
                            [ Touche Rouge ] Raccrocher
                          </button>
                        </div>
                      )}
                    </div>
                  </div>
                ) : activeCall ? (
                  <div className="flex-1 flex flex-col justify-between items-center text-center animate-in fade-in duration-200">
                    <div>
                      <div className={`text-[10px] font-bold uppercase tracking-wider ${activeCall.state === 'ringing' ? 'text-green-400 animate-pulse' : 'text-cyan-400'}`}>
                        {activeCall.state === 'ringing' ? '🔔 APPEL ENTRANT' : '📞 APPEL EN COURS'} [{activeCall.simName}]
                      </div>
                      <div className="text-sm font-bold text-white mt-0.5 truncate max-w-[280px]">
                        {activeCall.name}
                      </div>
                      <div className="text-[11px] font-mono text-gray-400">
                        {activeCall.number}
                      </div>
                    </div>

                    {/* Timer / Ringing status */}
                    <div className="my-1 py-1 px-3 bg-[#11161F] border border-cyan-500/30 rounded-lg">
                      <span className="text-lg font-mono font-bold text-cyan-300">
                        {activeCall.state === 'ringing' 
                          ? 'Sonnerie...' 
                          : `${String(Math.floor(activeCall.duration / 60)).padStart(2, '0')}:${String(activeCall.duration % 60).padStart(2, '0')}`}
                      </span>
                    </div>

                    {/* Hardware Buttons on screen */}
                    <div className="flex items-center gap-2 w-full justify-center">
                      {activeCall.state === 'ringing' ? (
                        <>
                          <button
                            onClick={answerCall}
                            className="px-3 py-1 bg-green-700 hover:bg-green-600 text-white rounded text-[10px] font-bold flex items-center gap-1 cursor-pointer"
                          >
                            <Phone className="w-3 h-3" /> Décrocher
                          </button>
                          <button
                            onClick={endCall}
                            className="px-3 py-1 bg-red-700 hover:bg-red-600 text-white rounded text-[10px] font-bold flex items-center gap-1 cursor-pointer"
                          >
                            <PhoneOff className="w-3 h-3" /> Refuser
                          </button>
                        </>
                      ) : (
                        <>
                          <button
                            onClick={toggleMute}
                            className={`px-2 py-1 rounded text-[9px] font-medium border ${activeCall.micMuted ? 'bg-amber-600 text-white border-amber-400' : 'bg-gray-800 text-gray-300 border-gray-700'}`}
                          >
                            {activeCall.micMuted ? 'Micro Muté' : 'Micro Actif'}
                          </button>
                          <button
                            onClick={toggleSpeaker}
                            className={`px-2 py-1 rounded text-[9px] font-medium border ${activeCall.speakerOn ? 'bg-cyan-700 text-white border-cyan-400' : 'bg-gray-800 text-gray-300 border-gray-700'}`}
                          >
                            {activeCall.speakerOn ? 'HP Actif' : 'Écouteur'}
                          </button>
                          <button
                            onClick={endCall}
                            className="px-3 py-1 bg-red-700 hover:bg-red-600 text-white rounded text-[10px] font-bold cursor-pointer"
                          >
                            Raccrocher
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                ) : activeTab === 'whatsapp' ? (
                  /* Condition 2: WhatsApp Chat Screen (MainScreen WhatsAppChatScreen replica) */
                  <div className="flex-1 flex flex-col justify-between bg-[#0B141A] -m-2 p-1.5 overflow-hidden text-left animate-in fade-in duration-150">
                    <div className="bg-[#128C7E] px-2 py-1 rounded text-white flex items-center justify-between shadow-sm">
                      <div>
                        <div className="text-[10px] font-bold leading-tight">WHATSAPP MESSENGER</div>
                        <div className="text-[8px] text-emerald-100">Discussion active | Clavier physique actif</div>
                      </div>
                      <button 
                        onClick={() => simulateIncomingWhatsAppCall('Karim Bennani')}
                        className="text-[8px] bg-emerald-800 hover:bg-emerald-700 px-1.5 py-0.5 rounded text-white font-bold cursor-pointer shadow-sm"
                        title="Simuler un appel WhatsApp"
                      >
                        Appel WA
                      </button>
                    </div>

                    {/* Messages list (bubbles) */}
                    <div className="flex-1 overflow-y-auto space-y-1.5 my-1 px-0.5 max-h-[125px]">
                      {whatsAppMessages.slice(-4).map((m, idx) => (
                        <div key={idx} className={`flex ${m.isOutgoing ? 'justify-end' : 'justify-start'}`}>
                          <div className={`max-w-[85%] rounded-lg p-1.5 text-[9px] shadow-sm ${
                            m.isOutgoing 
                              ? 'bg-[#005C4B] text-white border border-[#128C7E]' 
                              : 'bg-[#202C33] text-gray-200 border border-[#2A3942]'
                          }`}>
                            <div className="flex items-center justify-between gap-1 text-[7px] text-emerald-300 font-bold mb-0.5">
                              <span>{m.senderName}</span>
                              <span className="text-gray-400 font-normal">{m.timestamp}</span>
                            </div>
                            <div className="leading-snug break-words">{m.body}</div>
                            {m.isOutgoing && (
                              <div className="text-right text-[7px] text-cyan-300 mt-0.5 font-bold">vv</div>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>

                    {/* Champ de réponse rapide */}
                    <form 
                      onSubmit={(e) => {
                        e.preventDefault();
                        if (whatsAppReplyText.trim()) {
                          handleSendWhatsAppReply('wa_active', whatsAppReplyText.trim());
                        }
                      }}
                      className="flex items-center gap-1 bg-[#1F2C34] p-1 rounded border border-[#2A3942]"
                    >
                      <input
                        type="text"
                        value={whatsAppReplyText}
                        onChange={(e) => setWhatsAppReplyText(e.target.value)}
                        placeholder="Répondre..."
                        className="flex-1 bg-[#2A3942] text-white text-[9px] px-2 py-0.5 rounded border border-gray-700 focus:outline-none"
                      />
                      <button 
                        type="submit"
                        className="px-2 py-0.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded text-[8px] font-bold cursor-pointer"
                      >
                        Envoyer
                      </button>
                    </form>
                    <div className="text-[7px] text-gray-500 text-center mt-0.5">
                      Entrée: Envoyer (WHATSAPP_REPLY) | Rouge: Quitter
                    </div>
                  </div>
                ) : (
                  /* Condition 2: Regular Curve 9300 Home Screen */
                  <div className="flex-1 flex flex-col justify-between">
                    {/* Clock & Status */}
                    <div className="text-center pt-1">
                      <div className="text-2xl font-bold font-mono tracking-tight text-white leading-none">
                        14:32
                      </div>
                      <div className="text-[10px] text-gray-400 mt-0.5">
                        Mardi 24 Septembre 2026
                      </div>
                      <div className="text-[9px] text-cyan-400 font-medium">
                        Double SIM Active : [inwi | Orange]
                      </div>
                    </div>

                    {/* 3x2 Grid Buttons (Exact replica of SmartBridgeScreen.java) */}
                    <div className="grid grid-cols-3 gap-1.5 px-1 py-1">
                      {/* Appels Button -> Opens Dialer & Call History */}
                      <button
                        onClick={() => setActiveTab('calls')}
                        className={`p-1.5 rounded-lg border text-center transition-colors cursor-pointer ${
                          activeTab === 'calls' 
                            ? 'bg-cyan-900/60 border-cyan-400 text-white' 
                            : 'bg-[#151D28] border-cyan-800/40 text-cyan-300 hover:bg-cyan-900/40'
                        }`}
                      >
                        <PhoneCall className="w-3.5 h-3.5 mx-auto mb-0.5 text-cyan-400" />
                        <span className="text-[9px] font-bold block leading-tight">Appels</span>
                        <span className="text-[7px] text-gray-400 block">&amp; Journal</span>
                      </button>

                      {/* Contacts Button */}
                      <button
                        onClick={() => {
                          setActiveTab('contacts');
                          setIsContactsModalOpen(true);
                        }}
                        className={`p-1.5 rounded-lg border text-center transition-colors cursor-pointer ${
                          activeTab === 'contacts' 
                            ? 'bg-cyan-900/60 border-cyan-400 text-white' 
                            : 'bg-[#151D28] border-cyan-800/40 text-cyan-300 hover:bg-cyan-900/40'
                        }`}
                      >
                        <User className="w-3.5 h-3.5 mx-auto mb-0.5 text-cyan-400" />
                        <span className="text-[9px] font-bold block leading-tight">Contacts</span>
                        <span className="text-[7px] text-cyan-400 font-mono block">VIP ({contacts.length})</span>
                      </button>

                      {/* Média Button */}
                      <button
                        onClick={() => {
                          setActiveTab('media');
                          setIsMediaModalOpen(true);
                        }}
                        className={`p-1.5 rounded-lg border text-center transition-colors cursor-pointer ${
                          activeTab === 'media' 
                            ? 'bg-purple-900/60 border-purple-400 text-white' 
                            : 'bg-[#151D28] border-purple-800/40 text-purple-300 hover:bg-purple-900/40'
                        }`}
                      >
                        <Music className="w-3.5 h-3.5 mx-auto mb-0.5 text-purple-400" />
                        <span className="text-[9px] font-bold block leading-tight">Média</span>
                        <span className="text-[7px] text-gray-400 block">{isPlaying ? 'Lecture' : 'Pause'}</span>
                      </button>

                      {/* Messages Button (Opens WhatsApp Chat Screen) */}
                      <button
                        onClick={() => {
                          setActiveTab('whatsapp');
                          addLog('TX: OPEN_APP|WhatsApp', 'tx');
                        }}
                        className={`p-1.5 rounded-lg border text-center transition-colors cursor-pointer ${
                          activeTab === 'whatsapp'
                            ? 'bg-emerald-900/60 border-emerald-400 text-white'
                            : 'bg-[#151D28] border-emerald-800/40 text-emerald-300 hover:bg-emerald-900/40'
                        }`}
                      >
                        <MessageSquare className="w-3.5 h-3.5 mx-auto mb-0.5 text-emerald-400" />
                        <span className="text-[9px] font-bold block leading-tight">Messages</span>
                        <span className="text-[7px] text-emerald-400 block font-mono">WhatsApp</span>
                      </button>

                      {/* Notifs Button */}
                      <button
                        onClick={() => addLog('TX: GET_NOTIFICATIONS', 'tx')}
                        className="p-1.5 rounded-lg bg-[#151D28] border border-gray-700 text-gray-300 hover:bg-gray-800 text-center cursor-pointer"
                      >
                        <Radio className="w-3.5 h-3.5 mx-auto mb-0.5 text-amber-400" />
                        <span className="text-[9px] font-medium block leading-tight">Notifs</span>
                        <span className="text-[7px] text-amber-400 block">2 actives</span>
                      </button>

                      {/* Audio Stream Button */}
                      <button
                        onClick={() => {
                          const next = !audioStreamingActive;
                          setAudioStreamingActive(next);
                          addLog(next ? 'TX: AUDIO_START' : 'TX: AUDIO_STOP', 'tx');
                        }}
                        className={`p-1.5 rounded-lg border text-center transition-colors cursor-pointer ${
                          audioStreamingActive 
                            ? 'bg-amber-900/40 border-amber-500/80 text-amber-200' 
                            : 'bg-[#151D28] border-gray-700 text-gray-400'
                        }`}
                      >
                        <Volume2 className="w-3.5 h-3.5 mx-auto mb-0.5 text-amber-400" />
                        <span className="text-[9px] font-bold block leading-tight">Audio SCO</span>
                        <span className="text-[7px] font-mono block">{audioStreamingActive ? '250ms ZeroLag' : 'Inactif'}</span>
                      </button>
                    </div>

                    {/* Bottom Hint */}
                    <div className="text-[8px] text-gray-500 text-center pb-0.5">
                      Touche Verte: Appels &amp; Journal | Menu: Options
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Hardware Keypad Row: Green Send, Menu, Optical Trackpad, Escape, Red End */}
            <div className="w-[320px] flex items-center justify-between px-2 pt-3 pb-2">
              {/* Green Send Key */}
              <button 
                onClick={() => {
                  if (whatsAppCall && whatsAppCall.state === 'ringing') answerWhatsAppCall();
                  else if (activeCall && activeCall.state === 'ringing') answerCall();
                  else if (activeTab === 'whatsapp' && whatsAppReplyText.trim()) handleSendWhatsAppReply('wa_active', whatsAppReplyText.trim());
                  else setActiveTab('calls');
                }}
                className="w-12 h-8 rounded-lg bg-[#112415] hover:bg-[#1a3a22] border border-green-600/70 text-green-400 flex items-center justify-center cursor-pointer shadow-md active:scale-95 transition-all"
                title="Touche Verte (Appels / Décrocher / Envoyer)"
              >
                <Phone className="w-4 h-4" />
              </button>

              {/* BlackBerry Menu Key */}
              <button 
                onClick={() => {
                  if (activeTab === 'whatsapp') setIsWhatsAppModalOpen(true);
                  else setIsCallHistoryModalOpen(true);
                }}
                className="w-10 h-8 rounded-lg bg-[#1A1F29] hover:bg-[#252C3A] border border-[#333E52] text-gray-300 flex items-center justify-center cursor-pointer shadow-md active:scale-95 transition-all"
                title="Touche Menu BlackBerry"
              >
                <Layers className="w-4 h-4 text-cyan-400" />
              </button>

              {/* Optical Trackpad */}
              <div 
                onClick={() => {
                  if (whatsAppCall) {
                    if (whatsAppCall.state === 'ringing') answerWhatsAppCall();
                    else rejectWhatsAppCall();
                  } else if (activeCall) {
                    toggleMute();
                  } else if (activeTab === 'whatsapp' && whatsAppReplyText.trim()) {
                    handleSendWhatsAppReply('wa_active', whatsAppReplyText.trim());
                  } else {
                    setIsMediaModalOpen(true);
                  }
                }}
                className="w-11 h-9 rounded-xl bg-gradient-to-b from-[#0F1318] to-[#040608] border-2 border-cyan-500/60 shadow-[0_0_10px_rgba(6,182,212,0.3)] flex items-center justify-center cursor-pointer active:scale-90 transition-transform"
                title="Trackpad Optique BlackBerry (Clic pour action)"
              >
                <div className="w-4 h-3 rounded-full bg-cyan-400/30 border border-cyan-400/50" />
              </div>

              {/* Escape / Back Key */}
              <button 
                onClick={() => {
                  if (whatsAppCall) rejectWhatsAppCall();
                  else if (activeCall) endCall();
                  else setActiveTab('calls');
                }}
                className="w-10 h-8 rounded-lg bg-[#1A1F29] hover:bg-[#252C3A] border border-[#333E52] text-gray-300 flex items-center justify-center cursor-pointer shadow-md active:scale-95 transition-all"
                title="Touche Retour / Échap"
              >
                <X className="w-4 h-4 text-gray-400" />
              </button>

              {/* Red End Call Key */}
              <button 
                onClick={() => {
                  if (whatsAppCall) rejectWhatsAppCall();
                  else if (activeCall) endCall();
                  else setActiveTab('calls');
                }}
                className="w-12 h-8 rounded-lg bg-[#2B1414] hover:bg-[#401C1C] border border-red-600/70 text-red-400 flex items-center justify-center cursor-pointer shadow-md active:scale-95 transition-all"
                title="Touche Rouge (Raccrocher / Refuser / Quitter)"
              >
                <PhoneOff className="w-4 h-4" />
              </button>
            </div>

            {/* Curve 9300 QWERTY Keyboard Accent Lines */}
            <div className="w-[310px] grid grid-cols-10 gap-1 pt-1 opacity-40">
              {['Q','W','E','R','T','Y','U','I','O','P'].map(k => (
                <div key={k} className="h-4 bg-[#181D26] rounded border border-gray-800 text-[8px] text-gray-500 flex items-center justify-center font-bold">
                  {k}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* =============================================================== */}
        {/* RIGHT COLUMN: Interactive Control Station (Tabs & Content)      */}
        {/* =============================================================== */}
        <div className="lg:col-span-7 flex flex-col gap-4">
          
          {/* Tab Selector Bar */}
          <div className="bg-[#11161F] border border-[#232F3E] rounded-xl p-1.5 flex items-center gap-1 overflow-x-auto">
            <button
              onClick={() => setActiveTab('calls')}
              className={`flex-1 py-2 px-3 rounded-lg text-xs font-bold flex items-center justify-center gap-2 transition-all cursor-pointer ${
                activeTab === 'calls'
                  ? 'bg-cyan-500 text-black shadow-lg shadow-cyan-500/25'
                  : 'text-gray-400 hover:text-white hover:bg-[#1A2230]'
              }`}
            >
              <PhoneCall className="w-3.5 h-3.5" />
              <span>Appels</span>
            </button>

            <button
              onClick={() => setActiveTab('whatsapp')}
              className={`flex-1 py-2 px-3 rounded-lg text-xs font-bold flex items-center justify-center gap-2 transition-all cursor-pointer ${
                activeTab === 'whatsapp'
                  ? 'bg-emerald-600 text-white shadow-lg shadow-emerald-600/25'
                  : 'text-gray-400 hover:text-white hover:bg-[#1A2230]'
              }`}
            >
              <MessageSquare className="w-3.5 h-3.5 text-emerald-400" />
              <span>WhatsApp ({whatsAppMessages.length})</span>
              {whatsAppCall && (
                <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
              )}
            </button>

            <button
              onClick={() => setActiveTab('media')}
              className={`flex-1 py-2 px-3 rounded-lg text-xs font-bold flex items-center justify-center gap-2 transition-all cursor-pointer ${
                activeTab === 'media'
                  ? 'bg-purple-600 text-white shadow-lg shadow-purple-600/25'
                  : 'text-gray-400 hover:text-white hover:bg-[#1A2230]'
              }`}
            >
              <Music className="w-3.5 h-3.5" />
              <span>Contrôle Média</span>
            </button>

            <button
              onClick={() => setActiveTab('contacts')}
              className={`flex-1 py-2 px-3 rounded-lg text-xs font-bold flex items-center justify-center gap-2 transition-all cursor-pointer ${
                activeTab === 'contacts'
                  ? 'bg-cyan-600 text-white shadow-lg shadow-cyan-600/25'
                  : 'text-gray-400 hover:text-white hover:bg-[#1A2230]'
              }`}
            >
              <Users className="w-3.5 h-3.5" />
              <span>Contacts VIP</span>
            </button>

            <button
              onClick={() => setActiveTab('logs')}
              className={`flex-1 py-2 px-3 rounded-lg text-xs font-bold flex items-center justify-center gap-2 transition-all cursor-pointer ${
                activeTab === 'logs'
                  ? 'bg-blue-600 text-white shadow-lg shadow-blue-600/25'
                  : 'text-gray-400 hover:text-white hover:bg-[#1A2230]'
              }`}
            >
              <Radio className="w-3.5 h-3.5" />
              <span>Trames BT</span>
            </button>
          </div>

          {/* TAB 1: APPELS ET VRAI HISTORIQUE (Restored & Enhanced) */}
          {activeTab === 'calls' && (
            <div className="flex flex-col gap-4">
              
              {/* Dialer & Outbound Call Box */}
              <div className="bg-[#11161F] border border-[#232F3E] rounded-2xl p-4 shadow-xl">
                <div className="flex items-center justify-between pb-3 border-b border-[#1E293B] mb-3">
                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-lg bg-green-500/10 border border-green-500/30 flex items-center justify-center text-green-400">
                      <PhoneCall className="w-3.5 h-3.5" />
                    </div>
                    <div>
                      <h3 className="text-xs sm:text-sm font-bold text-white">Compositeur &amp; Sélection SIM</h3>
                      <p className="text-[10px] text-gray-400">Routage audio HFP Bluetooth automatique</p>
                    </div>
                  </div>

                  {/* Dual SIM Selector */}
                  <div className="flex items-center gap-1.5 bg-[#0A0D14] p-1 rounded-lg border border-[#1E293B]">
                    <button
                      onClick={() => setSelectedSim('inwi')}
                      className={`px-2.5 py-1 rounded text-[10px] font-bold transition-colors cursor-pointer ${
                        selectedSim === 'inwi' ? 'bg-purple-700 text-white' : 'text-gray-400 hover:text-white'
                      }`}
                    >
                      SIM 1 [inwi]
                    </button>
                    <button
                      onClick={() => setSelectedSim('Orange')}
                      className={`px-2.5 py-1 rounded text-[10px] font-bold transition-colors cursor-pointer ${
                        selectedSim === 'Orange' ? 'bg-orange-600 text-white' : 'text-gray-400 hover:text-white'
                      }`}
                    >
                      SIM 2 [Orange]
                    </button>
                  </div>
                </div>

                {/* Input & Call Trigger */}
                <div className="flex items-center gap-2">
                  <div className="relative flex-1">
                    <input
                      type="text"
                      placeholder="Saisir un numéro ou sélectionner ci-dessous..."
                      value={dialerNumber}
                      onChange={e => setDialerNumber(e.target.value)}
                      onKeyDown={e => {
                        if (e.key === 'Enter') startCall(dialerNumber);
                      }}
                      className="w-full bg-[#0A0D14] border border-[#232F3E] focus:border-cyan-500 rounded-xl px-4 py-2.5 text-sm font-mono text-white placeholder-gray-500 focus:outline-none transition-colors"
                    />
                    {dialerNumber && (
                      <button
                        onClick={() => setDialerNumber('')}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    )}
                  </div>
                  <button
                    onClick={() => startCall(dialerNumber)}
                    disabled={!dialerNumber.trim()}
                    className="px-5 py-2.5 bg-green-600 hover:bg-green-500 disabled:opacity-40 disabled:hover:bg-green-600 text-white rounded-xl text-xs font-bold flex items-center gap-2 cursor-pointer shadow-lg shadow-green-600/20 transition-all"
                  >
                    <Phone className="w-4 h-4" />
                    <span>Appeler</span>
                  </button>
                </div>
              </div>

              {/* VRAI HISTORIQUE DES APPELS (Émis, Reçus, Manqués) */}
              <div className="bg-[#11161F] border border-[#232F3E] rounded-2xl p-4 shadow-xl">
                {/* Header with Search and Clear */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-[#1E293B]">
                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-lg bg-yellow-500/10 border border-yellow-500/30 flex items-center justify-center text-yellow-400">
                      <History className="w-3.5 h-3.5" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="text-xs sm:text-sm font-bold text-white">Vrai Historique des Appels</h3>
                        <span className="text-[10px] px-2 py-0.5 rounded-full bg-cyan-500/20 text-cyan-300 font-mono">
                          {filteredHistory.length} appels
                        </span>
                      </div>
                      <p className="text-[10px] text-gray-400">Journal en direct des appels émis et reçus</p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setIsCallHistoryModalOpen(true)}
                      className="px-2.5 py-1.5 rounded-lg bg-[#1A2230] hover:bg-[#232D40] text-gray-300 hover:text-white text-[11px] font-medium flex items-center gap-1.5 transition-colors cursor-pointer border border-[#2D394C]"
                      title="Plein écran"
                    >
                      <Maximize2 className="w-3 h-3 text-cyan-400" />
                      <span>Agrandir</span>
                    </button>
                    <button
                      onClick={() => setCallRecords([])}
                      className="px-2.5 py-1.5 rounded-lg bg-red-950/40 hover:bg-red-900/60 text-red-300 text-[11px] font-medium flex items-center gap-1.5 transition-colors cursor-pointer border border-red-800/40"
                    >
                      Effacer
                    </button>
                  </div>
                </div>

                {/* Filter and Search Bar */}
                <div className="flex flex-col sm:flex-row items-center gap-2 my-3">
                  {/* Direction Filters */}
                  <div className="flex items-center gap-1 w-full sm:w-auto bg-[#0A0D14] p-1 rounded-lg border border-[#1E293B]">
                    <button
                      onClick={() => setHistoryFilter('all')}
                      className={`flex-1 sm:flex-initial px-2.5 py-1 rounded text-[11px] font-medium transition-colors cursor-pointer ${
                        historyFilter === 'all' ? 'bg-[#232D3E] text-white' : 'text-gray-400 hover:text-white'
                      }`}
                    >
                      Tous
                    </button>
                    <button
                      onClick={() => setHistoryFilter('outgoing')}
                      className={`flex-1 sm:flex-initial px-2.5 py-1 rounded text-[11px] font-medium transition-colors cursor-pointer flex items-center gap-1 ${
                        historyFilter === 'outgoing' ? 'bg-cyan-900/60 text-cyan-300 border border-cyan-700/50' : 'text-gray-400 hover:text-white'
                      }`}
                    >
                      <ArrowUpRight className="w-3 h-3 text-cyan-400" />
                      <span>Émis</span>
                    </button>
                    <button
                      onClick={() => setHistoryFilter('incoming')}
                      className={`flex-1 sm:flex-initial px-2.5 py-1 rounded text-[11px] font-medium transition-colors cursor-pointer flex items-center gap-1 ${
                        historyFilter === 'incoming' ? 'bg-green-900/60 text-green-300 border border-green-700/50' : 'text-gray-400 hover:text-white'
                      }`}
                    >
                      <ArrowDownLeft className="w-3 h-3 text-green-400" />
                      <span>Reçus</span>
                    </button>
                    <button
                      onClick={() => setHistoryFilter('missed')}
                      className={`flex-1 sm:flex-initial px-2.5 py-1 rounded text-[11px] font-medium transition-colors cursor-pointer flex items-center gap-1 ${
                        historyFilter === 'missed' ? 'bg-red-900/60 text-red-300 border border-red-700/50' : 'text-gray-400 hover:text-white'
                      }`}
                    >
                      <PhoneMissed className="w-3 h-3 text-red-400" />
                      <span>Manqués</span>
                    </button>
                  </div>

                  {/* Search Input */}
                  <div className="relative flex-1 w-full">
                    <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
                    <input
                      type="text"
                      placeholder="Filtrer l'historique par nom ou numéro..."
                      value={historySearch}
                      onChange={e => setHistorySearch(e.target.value)}
                      className="w-full bg-[#0A0D14] border border-[#1E293B] rounded-lg pl-8 pr-3 py-1.5 text-xs text-white placeholder-gray-500 focus:outline-none focus:border-cyan-500"
                    />
                  </div>
                </div>

                {/* Call History Records List */}
                <div className="space-y-2 max-h-[360px] overflow-y-auto pr-1">
                  {filteredHistory.length === 0 ? (
                    <div className="text-center py-8 text-xs text-gray-500">
                      Aucun appel ne correspond à votre filtre.
                    </div>
                  ) : (
                    filteredHistory.map((item) => {
                      const isOutgoing = item.direction === 'outgoing';
                      const isMissed = item.status === 'missed';

                      return (
                        <div
                          key={item.id}
                          className="bg-[#0D121B] hover:bg-[#131A26] border border-[#1C2636] hover:border-cyan-500/30 rounded-xl p-3 flex items-center justify-between gap-3 transition-colors"
                        >
                          <div className="flex items-center gap-3 min-w-0">
                            {/* Direction Icon Badge */}
                            <div className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${
                              isMissed
                                ? 'bg-red-500/10 border border-red-500/30 text-red-400'
                                : isOutgoing
                                ? 'bg-cyan-500/10 border border-cyan-500/30 text-cyan-400'
                                : 'bg-green-500/10 border border-green-500/30 text-green-400'
                            }`}>
                              {isMissed ? (
                                <PhoneMissed className="w-4 h-4" />
                              ) : isOutgoing ? (
                                <ArrowUpRight className="w-4 h-4" />
                              ) : (
                                <ArrowDownLeft className="w-4 h-4" />
                              )}
                            </div>

                            {/* Contact Details */}
                            <div className="min-w-0">
                              <div className="flex items-center gap-2">
                                <span className="text-xs sm:text-sm font-bold text-white truncate">
                                  {item.callerName}
                                </span>
                                {item.simName && (
                                  <span className={`px-1.5 py-0.2 rounded text-[9px] font-mono font-bold ${
                                    item.simName.toLowerCase().includes('orange') 
                                      ? 'bg-orange-500/20 text-orange-300' 
                                      : 'bg-purple-500/20 text-purple-300'
                                  }`}>
                                    {item.simName}
                                  </span>
                                )}
                              </div>
                              <div className="flex items-center gap-2 text-[11px] text-gray-400 font-mono mt-0.5">
                                <span>{item.phoneNumber}</span>
                                <span>•</span>
                                <span className="text-gray-500">{item.timestamp}</span>
                              </div>
                            </div>
                          </div>

                          {/* Duration and Redial Button */}
                          <div className="flex items-center gap-3 shrink-0">
                            <div className="text-right">
                              <span className={`text-xs font-mono font-semibold block ${
                                isMissed ? 'text-red-400' : 'text-gray-300'
                              }`}>
                                {item.duration || '00:00'}
                              </span>
                              <span className="text-[9px] text-gray-500">
                                {isOutgoing ? 'Appel émis' : isMissed ? 'Manqué' : 'Appel reçu'}
                              </span>
                            </div>

                            {/* One-click Rappeler Button */}
                            <button
                              onClick={() => startCall(item.phoneNumber, item.callerName, item.simName as any)}
                              className="px-3 py-1.5 bg-cyan-600 hover:bg-cyan-500 text-black text-xs font-bold rounded-lg flex items-center gap-1.5 transition-all cursor-pointer shadow-md shadow-cyan-600/20"
                              title="Rappeler ce correspondant"
                            >
                              <Phone className="w-3 h-3" />
                              <span className="hidden sm:inline">Rappeler</span>
                            </button>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            </div>
          )}

          {/* TAB: WHATSAPP COMPANION & MESSAGERIE INSTANTANÉE */}
          {activeTab === 'whatsapp' && (
            <div className="flex flex-col gap-4">
              {/* Header Box */}
              <div className="bg-[#11161F] border border-[#232F3E] rounded-2xl p-4 sm:p-5 shadow-xl">
                <div className="flex flex-wrap items-center justify-between gap-3 pb-3 border-b border-[#1E293B]">
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shadow-md">
                      <MessageSquare className="w-5 h-5" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <h2 className="text-sm sm:text-base font-bold text-white">Module WhatsApp BlackBerry Curve 9300</h2>
                        <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-emerald-500/20 text-emerald-300 border border-emerald-500/40">
                          RIM OS 6.0
                        </span>
                      </div>
                      <p className="text-[11px] text-gray-400">
                        Passerelle Bluetooth SPP • Décrochage Touche Verte • Rejet Touche Rouge • Encodage Base64 UTF-8
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setIsWhatsAppModalOpen(true)}
                      className="px-3 py-1.5 rounded-lg bg-[#1A2230] hover:bg-[#232D40] text-emerald-300 text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer border border-emerald-500/40"
                    >
                      <Maximize2 className="w-3.5 h-3.5" />
                      <span>Mode Fenêtre</span>
                    </button>
                  </div>
                </div>

                {/* Quick Simulation Triggers */}
                <div className="mt-4 grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                  <button
                    onClick={() => simulateIncomingWhatsAppCall('Karim Bennani')}
                    className="p-3 rounded-xl bg-[#151D28] hover:bg-[#1E293B] border border-emerald-500/30 text-left transition-all cursor-pointer group"
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-bold text-emerald-400 flex items-center gap-1.5">
                        <PhoneIncoming className="w-3.5 h-3.5" />
                        Simuler Appel WA
                      </span>
                      <span className="text-[9px] font-mono text-gray-500">INCOMING</span>
                    </div>
                    <p className="text-[10px] text-gray-400">
                      Déclenche vibration continue + LED verte + plein écran d'appel
                    </p>
                  </button>

                  <button
                    onClick={() => handleIncomingWhatsAppMessage('Dr. Karim Lahlou', 'Résultats d\'analyses reçus via WhatsApp.')}
                    className="p-3 rounded-xl bg-[#151D28] hover:bg-[#1E293B] border border-emerald-500/30 text-left transition-all cursor-pointer group"
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-bold text-emerald-400 flex items-center gap-1.5">
                        <MessageSquare className="w-3.5 h-3.5" />
                        Simuler Message WA
                      </span>
                      <span className="text-[9px] font-mono text-gray-500">MSG</span>
                    </div>
                    <p className="text-[10px] text-gray-400">
                      Bip doux + vibration brève + bulle de dialogue
                    </p>
                  </button>

                  <button
                    onClick={() => handleStartWhatsAppChat('+212661223344', 'Bonjour Hamza, contact WhatsApp validé.')}
                    className="p-3 rounded-xl bg-[#151D28] hover:bg-[#1E293B] border border-cyan-500/30 text-left transition-all cursor-pointer group"
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-bold text-cyan-400 flex items-center gap-1.5">
                        <Plus className="w-3.5 h-3.5" />
                        Nouveau Chat WA
                      </span>
                      <span className="text-[9px] font-mono text-gray-500">START_CHAT</span>
                    </div>
                    <p className="text-[10px] text-gray-400">
                      Ouvre une conversation depuis le Curve 9300
                    </p>
                  </button>
                </div>
              </div>

              {/* Chat View & Input */}
              <div className="bg-[#11161F] border border-[#232F3E] rounded-2xl p-4 sm:p-5 shadow-xl flex flex-col gap-4">
                <div className="flex items-center justify-between pb-2 border-b border-[#1E293B]">
                  <div className="text-xs font-bold text-white flex items-center gap-2">
                    <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
                    <span>Fil de discussion WhatsApp Curve 9300 ({whatsAppMessages.length} messages)</span>
                  </div>
                  <span className="text-[10px] text-gray-400 font-mono">
                    Bulles #005C4B &amp; #202C33
                  </span>
                </div>

                {/* Messages Feed */}
                <div className="bg-[#0B141A] p-4 rounded-xl border border-[#202C33] space-y-3 max-h-[340px] overflow-y-auto">
                  {whatsAppMessages.map((m, idx) => (
                    <div
                      key={idx}
                      className={`flex ${m.isOutgoing ? 'justify-end' : 'justify-start'}`}
                    >
                      <div
                        className={`max-w-[80%] rounded-2xl p-3 shadow-md ${
                          m.isOutgoing
                            ? 'bg-[#005C4B] border border-[#128C7E] text-white rounded-br-none'
                            : 'bg-[#202C33] border border-[#2A3942] text-gray-100 rounded-bl-none'
                        }`}
                      >
                        <div className="flex items-center justify-between gap-3 mb-1">
                          <span className={`text-[11px] font-bold ${m.isOutgoing ? 'text-emerald-300' : 'text-emerald-400'}`}>
                            {m.senderName}
                          </span>
                          <span className="text-[10px] text-gray-400 font-mono">
                            {m.timestamp}
                          </span>
                        </div>
                        <p className="text-xs sm:text-sm whitespace-pre-wrap leading-relaxed">
                          {m.body}
                        </p>
                        {m.isOutgoing && (
                          <div className="flex items-center justify-end gap-1 mt-1 text-[10px] text-cyan-300">
                            {m.isConfirmed ? (
                              <>
                                <CheckCheck className="w-3.5 h-3.5 text-cyan-400" />
                                <span className="text-[9px]">Transmis Android (WHATSAPP_REPLY_OK)</span>
                              </>
                            ) : (
                              <button
                                onClick={() => handleConfirmReply(m.notifId)}
                                className="text-[9px] hover:underline text-cyan-200 cursor-pointer"
                              >
                                En attente d'accusé... Cliquez pour confirmer
                              </button>
                            )}
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>

                {/* Quick Reply Form */}
                <form
                  onSubmit={(e) => {
                    e.preventDefault();
                    if (whatsAppReplyText.trim()) {
                      handleSendWhatsAppReply('wa_active', whatsAppReplyText.trim());
                    }
                  }}
                  className="flex items-center gap-2 bg-[#1F2C34] p-2 rounded-xl border border-[#2A3942]"
                >
                  <input
                    type="text"
                    value={whatsAppReplyText}
                    onChange={(e) => setWhatsAppReplyText(e.target.value)}
                    placeholder="Saisir réponse rapide (Touche Entrée ou Clic Trackpad Curve 9300)..."
                    className="flex-1 bg-[#2A3942] text-white text-xs sm:text-sm px-4 py-2.5 rounded-lg border border-gray-700 focus:outline-none focus:border-emerald-500"
                  />
                  <button
                    type="submit"
                    className="px-4 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg font-bold text-xs flex items-center gap-1.5 transition-colors cursor-pointer shadow-md shadow-emerald-700/20"
                  >
                    <Send className="w-3.5 h-3.5" />
                    <span>Envoyer</span>
                  </button>
                </form>
              </div>
            </div>
          )}

          {/* TAB 2: CONTRÔLE MÉDIA (Restored & Enhanced) */}
          {activeTab === 'media' && (
            <div className="bg-[#11161F] border border-[#232F3E] rounded-2xl p-5 shadow-xl flex flex-col gap-6">
              
              {/* Header */}
              <div className="flex items-center justify-between pb-3 border-b border-[#1E293B]">
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-lg bg-purple-500/10 border border-purple-500/30 flex items-center justify-center text-purple-400">
                    <Music className="w-4 h-4" />
                  </div>
                  <div>
                    <h2 className="text-sm font-bold text-white">Contrôle Média Smartphone</h2>
                    <p className="text-[11px] text-gray-400">Diffusion audio temps réel vers BlackBerry Curve 9300 (WAV 250ms Zero-Lag)</p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setIsMediaModalOpen(true)}
                    className="px-3 py-1.5 rounded-lg bg-[#1A2230] hover:bg-[#232D40] text-gray-300 hover:text-white text-xs font-medium flex items-center gap-1.5 transition-colors cursor-pointer border border-[#2D394C]"
                  >
                    <Maximize2 className="w-3.5 h-3.5 text-purple-400" />
                    <span>Mode Fenêtre</span>
                  </button>
                </div>
              </div>

              {/* Player UI */}
              <div className="flex flex-col sm:flex-row items-center gap-6 bg-[#0B0E14] p-5 rounded-2xl border border-[#1E293B]">
                {/* Vinyl / Album Art */}
                <div className="relative shrink-0">
                  <div className={`w-32 h-32 rounded-full bg-gradient-to-tr from-gray-900 via-[#1C1838] to-purple-900 border-4 border-gray-800 shadow-2xl flex items-center justify-center ${
                    isPlaying ? 'animate-[spin_6s_linear_infinite]' : ''
                  }`}>
                    <div className="w-10 h-10 rounded-full bg-[#0B0E14] border-2 border-purple-400/50 flex items-center justify-center">
                      <Disc3 className="w-6 h-6 text-purple-400" />
                    </div>
                  </div>
                  <span className={`absolute -bottom-2 left-1/2 -translate-x-1/2 px-2 py-0.5 rounded-full text-[9px] font-bold border ${
                    isPlaying ? 'bg-green-500/20 text-green-300 border-green-500/40' : 'bg-gray-800 text-gray-400 border-gray-700'
                  }`}>
                    {isPlaying ? 'En cours' : 'En pause'}
                  </span>
                </div>

                {/* Track Details & Controls */}
                <div className="flex-1 w-full flex flex-col justify-center">
                  <h3 className="text-lg font-bold text-white">{mediaTitle}</h3>
                  <p className="text-xs text-cyan-400 font-medium">{mediaArtist}</p>
                  <p className="text-[10px] text-gray-500 font-mono mt-0.5">
                    Audio J2ME JSR-135 : 8000 Hz, 16-bit Mono (Blocs 250 ms, Zero-Lag Drop-if-Lagging, {chunksSentCount} paquets)
                  </p>

                  {/* Progress Bar */}
                  <div className="mt-4">
                    <div className="w-full bg-gray-800 h-1.5 rounded-full overflow-hidden">
                      <div 
                        className="bg-purple-500 h-full rounded-full transition-all duration-300"
                        style={{ width: `${mediaProgress}%` }}
                      />
                    </div>
                    <div className="flex justify-between text-[10px] font-mono text-gray-500 mt-1">
                      <span>01:42</span>
                      <span>03:50</span>
                    </div>
                  </div>

                  {/* Playback Buttons */}
                  <div className="flex items-center justify-center gap-4 mt-3">
                    <button
                      onClick={() => {
                        setMediaTitle('Appel & Audio Précédent');
                        addLog('TX: MEDIA_PREVIOUS', 'tx');
                      }}
                      className="p-2 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 hover:text-white cursor-pointer"
                    >
                      <SkipBack className="w-4 h-4" />
                    </button>

                    <button
                      onClick={() => {
                        const next = !isPlaying;
                        setIsPlaying(next);
                        addLog(next ? 'TX: MEDIA_PLAY' : 'TX: MEDIA_PAUSE', 'tx');
                      }}
                      className="p-3 rounded-full bg-purple-600 hover:bg-purple-500 text-white shadow-lg shadow-purple-600/30 cursor-pointer"
                    >
                      {isPlaying ? <Pause className="w-5 h-5" /> : <Play className="w-5 h-5 ml-0.5" />}
                    </button>

                    <button
                      onClick={() => {
                        setMediaTitle('Audio Multimédia Suivant');
                        addLog('TX: MEDIA_NEXT', 'tx');
                      }}
                      className="p-2 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 hover:text-white cursor-pointer"
                    >
                      <SkipForward className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>

              {/* Volume & Audio Route Settings */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="bg-[#0B0E14] p-4 rounded-xl border border-[#1E293B]">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs text-gray-300 font-medium">Volume Smartphone</span>
                    <span className="text-xs font-mono text-cyan-400">{isMuted ? '0%' : `${mediaVolume}%`}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <button
                      onClick={() => setIsMuted(!isMuted)}
                      className="text-gray-400 hover:text-white cursor-pointer"
                    >
                      {isMuted ? <VolumeX className="w-4 h-4 text-red-400" /> : <Volume2 className="w-4 h-4 text-purple-400" />}
                    </button>
                    <input
                      type="range"
                      min="0"
                      max="100"
                      value={isMuted ? 0 : mediaVolume}
                      onChange={e => {
                        setIsMuted(false);
                        setMediaVolume(Number(e.target.value));
                      }}
                      className="flex-1 accent-purple-500"
                    />
                  </div>
                </div>

                <div className="bg-[#0B0E14] p-4 rounded-xl border border-[#1E293B] flex flex-col justify-between">
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-gray-300 font-medium">Flux Audio Curve 9300</span>
                    <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                      audioStreamingActive ? 'bg-green-500/20 text-green-300' : 'bg-gray-800 text-gray-500'
                    }`}>
                      {audioStreamingActive ? 'SCO Connecté' : 'Désactivé'}
                    </span>
                  </div>
                  <button
                    onClick={() => {
                      const next = !audioStreamingActive;
                      setAudioStreamingActive(next);
                      addLog(next ? 'TX: AUDIO_START|8000|1|16|500' : 'TX: AUDIO_STOP', 'tx');
                    }}
                    className="w-full mt-2 py-1.5 bg-[#1B2230] hover:bg-[#232C3D] text-xs font-semibold rounded-lg text-gray-200 border border-[#2B3547] cursor-pointer"
                  >
                    {audioStreamingActive ? 'Arrêter le flux audio' : 'Activer le flux audio'}
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: CONTACTS VIP (Restored & Enhanced) */}
          {activeTab === 'contacts' && (
            <div className="bg-[#11161F] border border-[#232F3E] rounded-2xl p-5 shadow-xl flex flex-col gap-4">
              <div className="flex items-center justify-between pb-3 border-b border-[#1E293B]">
                <div className="flex items-center gap-2.5">
                  <div className="w-8 h-8 rounded-lg bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-400">
                    <Users className="w-4 h-4" />
                  </div>
                  <div>
                    <h2 className="text-sm font-bold text-white flex items-center gap-2">
                      Contacts VIP Synchronisés
                      <span className="text-[10px] px-2 py-0.2 rounded-full bg-cyan-500/20 text-cyan-300 font-mono">
                        {contacts.length}
                      </span>
                    </h2>
                    <p className="text-[11px] text-gray-400">Carnet d'adresses commun BlackBerry Curve 9300 &amp; Android</p>
                  </div>
                </div>

                <button
                  onClick={() => setIsContactsModalOpen(true)}
                  className="px-3 py-1.5 rounded-lg bg-[#1A2230] hover:bg-[#232D40] text-gray-300 hover:text-white text-xs font-medium flex items-center gap-1.5 transition-colors cursor-pointer border border-[#2D394C]"
                >
                  <Maximize2 className="w-3.5 h-3.5 text-cyan-400" />
                  <span>Voir Tout</span>
                </button>
              </div>

              {/* Contact Cards Grid */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-[400px] overflow-y-auto pr-1">
                {contacts.map((contact) => (
                  <div
                    key={contact.id}
                    className="bg-[#0B0E14] hover:bg-[#10141D] border border-[#1E293B] hover:border-cyan-500/40 rounded-xl p-3.5 flex flex-col justify-between gap-3 transition-colors"
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex items-center gap-2.5">
                        <div className="w-9 h-9 rounded-full bg-cyan-600/20 border border-cyan-500/30 text-cyan-400 flex items-center justify-center font-bold text-xs">
                          {contact.name.charAt(0)}
                        </div>
                        <div>
                          <h4 className="text-xs sm:text-sm font-bold text-white">{contact.name}</h4>
                          <p className="text-[11px] font-mono text-gray-400">{contact.number}</p>
                          {contact.notes && (
                            <p className="text-[10px] text-gray-500">{contact.notes}</p>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-2 pt-2 border-t border-gray-800/80">
                      <button
                        onClick={() => startCall(contact.number, contact.name, 'inwi')}
                        className="flex-1 py-1.5 px-2 bg-purple-900/50 hover:bg-purple-800 text-purple-200 border border-purple-700/50 rounded-lg text-[10px] font-bold flex items-center justify-center gap-1 cursor-pointer transition-colors"
                      >
                        <Phone className="w-3 h-3 text-purple-400" />
                        <span>Appel inwi</span>
                      </button>
                      <button
                        onClick={() => startCall(contact.number, contact.name, 'Orange')}
                        className="flex-1 py-1.5 px-2 bg-orange-900/50 hover:bg-orange-800 text-orange-200 border border-orange-700/50 rounded-lg text-[10px] font-bold flex items-center justify-center gap-1 cursor-pointer transition-colors"
                      >
                        <Phone className="w-3 h-3 text-orange-400" />
                        <span>Appel Orange</span>
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* TAB 4: PROTOCOLE ET LOGS BLUETOOTH */}
          {activeTab === 'logs' && (
            <div className="bg-[#11161F] border border-[#232F3E] rounded-2xl p-4 shadow-xl flex flex-col gap-3">
              <div className="flex items-center justify-between pb-2 border-b border-[#1E293B]">
                <div className="flex items-center gap-2">
                  <div className="w-7 h-7 rounded-lg bg-blue-500/10 border border-blue-500/30 flex items-center justify-center text-blue-400">
                    <Radio className="w-3.5 h-3.5" />
                  </div>
                  <div>
                    <h3 className="text-xs sm:text-sm font-bold text-white">Moniteur Protocole Bluetooth RFCOMM</h3>
                    <p className="text-[10px] text-gray-400">Échanges série bidirectionnels en temps réel</p>
                  </div>
                </div>
                <button
                  onClick={() => setBtLogs([])}
                  className="px-2.5 py-1 text-[10px] font-bold bg-[#1C2330] hover:bg-[#252E3E] text-gray-400 hover:text-white rounded border border-[#2D394C] cursor-pointer"
                >
                  Effacer
                </button>
              </div>

              {/* Logs terminal box */}
              <div className="bg-[#07090E] p-3 rounded-xl border border-[#1A2230] font-mono text-[10px] leading-relaxed max-h-[350px] overflow-y-auto space-y-1.5">
                {btLogs.map(log => (
                  <div key={log.id} className="flex items-start gap-2">
                    <span className="text-gray-500 shrink-0">{log.time}</span>
                    <span className={`shrink-0 font-bold ${
                      log.type === 'tx' ? 'text-green-400' : log.type === 'rx' ? 'text-cyan-400' : 'text-amber-400'
                    }`}>
                      [{log.type.toUpperCase()}]
                    </span>
                    <span className="text-gray-300 break-all">{log.text}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Standalone Modals (Media Controller, Contacts, and Call History) */}
      <MediaControllerModal
        isOpen={isMediaModalOpen}
        onClose={() => setIsMediaModalOpen(false)}
        onMediaAction={(action) => {
          if (action === 'PLAY') setIsPlaying(true);
          if (action === 'PAUSE') setIsPlaying(false);
          addLog(`TX: MEDIA_${action}`, 'tx');
        }}
      />

      <ContactsModal
        isOpen={isContactsModalOpen}
        onClose={() => setIsContactsModalOpen(false)}
        onCallContact={(number, name) => {
          setIsContactsModalOpen(false);
          startCall(number, name);
        }}
        onSyncContacts={() => {
          addLog('TX: GET_CONTACTS', 'tx');
          addLog('RX: CONTACTS_SYNC|8 contacts reçus', 'rx');
        }}
      />

      <CallHistoryModal
        isOpen={isCallHistoryModalOpen}
        onClose={() => setIsCallHistoryModalOpen(false)}
        callRecords={callRecords}
        onSelectNumberToCall={(number, name) => {
          setIsCallHistoryModalOpen(false);
          startCall(number, name);
        }}
        onClearHistory={() => setCallRecords([])}
      />

      <WhatsAppModal
        isOpen={isWhatsAppModalOpen}
        onClose={() => setIsWhatsAppModalOpen(false)}
        messages={whatsAppMessages}
        onSendMessage={handleSendWhatsAppReply}
        onStartNewChat={handleStartWhatsAppChat}
        onSimulateIncomingCall={simulateIncomingWhatsAppCall}
        onSimulateIncomingMessage={handleIncomingWhatsAppMessage}
        onConfirmReply={handleConfirmReply}
      />
    </div>
  );
}
