/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from 'react';
import { 
  Phone, 
  PhoneIncoming, 
  PhoneOutgoing,
  PhoneOff, 
  Volume2, 
  VolumeX, 
  CreditCard as SimIcon, 
  Radio, 
  Users, 
  Download, 
  Smartphone,
  Cpu,
  CheckCircle2,
  Send,
  Sliders,
  BatteryMedium,
  Mic,
  Headphones,
  Bluetooth,
  HelpCircle,
  Clock,
  History,
  PhoneCall
} from 'lucide-react';
import { CallHistoryModal, CallRecord } from './components/CallHistoryModal';
import { BatteryDischargeChart } from './components/BatteryDischargeChart';


interface Sim {
  name: string;
  slot: number;
}

type AudioRoute = 'BLUETOOTH' | 'SPEAKERPHONE' | 'EARPIECE';

export default function App() {
  // Bluetooth Link & Telephony State
  const [sims, setSims] = useState<Sim[]>([
    { name: 'inwi', slot: 0 },
    { name: 'Orange', slot: 1 }
  ]);
  
  // Call State
  const [callState, setCallState] = useState<'idle' | 'incoming' | 'outbound_dialing' | 'active' | 'ended'>('idle');
  const [currentCaller, setCurrentCaller] = useState({
    id: 'call_1042',
    name: 'Amina',
    number: '+212634934134',
    simName: 'inwi'
  });
  const [speakerOn, setSpeakerOn] = useState<boolean>(false);
  const [audioRoute, setAudioRoute] = useState<AudioRoute>('BLUETOOTH');
  const [localBbSpeaker, setLocalBbSpeaker] = useState<boolean>(false);
  const [simChoiceModalOpen, setSimChoiceModalOpen] = useState<boolean>(false);
  const [audioChoiceModalOpen, setAudioChoiceModalOpen] = useState<boolean>(false);
  const [hfpGuideModalOpen, setHfpGuideModalOpen] = useState<boolean>(false);
  const [callHistoryModalOpen, setCallHistoryModalOpen] = useState<boolean>(false);
  const [dialNumber, setDialNumber] = useState<string>('+212634934134');
  
  // Call History State
  const [callRecords, setCallRecords] = useState<CallRecord[]>([
    {
      id: 'rec_1',
      callerName: 'Amina Mansouri',
      phoneNumber: '+212634934134',
      timestamp: 'Aujourd\'hui, 14:28',
      direction: 'incoming',
      duration: '04:12',
      simName: 'inwi',
      status: 'answered'
    },
    {
      id: 'rec_2',
      callerName: 'Youssef Bennani',
      phoneNumber: '+212655881230',
      timestamp: 'Aujourd\'hui, 12:15',
      direction: 'outgoing',
      duration: '01:45',
      simName: 'Orange',
      status: 'answered'
    },
    {
      id: 'rec_3',
      callerName: 'Hamza H.',
      phoneNumber: '+212611223344',
      timestamp: 'Aujourd\'hui, 10:04',
      direction: 'incoming',
      duration: '08:30',
      simName: 'inwi',
      status: 'answered'
    },
    {
      id: 'rec_4',
      callerName: 'Service Client inwi',
      phoneNumber: '220',
      timestamp: 'Hier, 18:40',
      direction: 'outgoing',
      duration: '02:10',
      simName: 'inwi',
      status: 'answered'
    },
    {
      id: 'rec_5',
      callerName: 'Dr. Karim Lahlou',
      phoneNumber: '+212672409918',
      timestamp: 'Hier, 15:22',
      direction: 'incoming',
      duration: '00:54',
      simName: 'Orange',
      status: 'answered'
    },
    {
      id: 'rec_6',
      callerName: 'Fatima Zahra',
      phoneNumber: '+212698712345',
      timestamp: '24 Sep, 20:11',
      direction: 'incoming',
      duration: '05:20',
      simName: 'inwi',
      status: 'answered'
    },
    {
      id: 'rec_7',
      callerName: 'Orange Recharges & Info',
      phoneNumber: '121',
      timestamp: '24 Sep, 16:30',
      direction: 'outgoing',
      duration: '03:05',
      simName: 'Orange',
      status: 'answered'
    },
    {
      id: 'rec_8',
      callerName: 'Sara Alami',
      phoneNumber: '+212644332211',
      timestamp: '23 Sep, 11:05',
      direction: 'incoming',
      duration: '02:18',
      simName: 'inwi',
      status: 'answered'
    },
    {
      id: 'rec_9',
      callerName: 'Mehdi Chraibi',
      phoneNumber: '+212661908877',
      timestamp: '23 Sep, 09:44',
      direction: 'outgoing',
      duration: '00:42',
      simName: 'Orange',
      status: 'answered'
    },
    {
      id: 'rec_10',
      callerName: 'Omar Tazi',
      phoneNumber: '+212650123456',
      timestamp: '22 Sep, 17:19',
      direction: 'incoming',
      duration: '06:14',
      simName: 'inwi',
      status: 'answered'
    }
  ]);

  const addCallRecord = (
    name: string, 
    number: string, 
    dir: 'incoming' | 'outgoing', 
    durationStr: string, 
    sim: string, 
    status: 'answered' | 'missed' | 'rejected' = 'answered'
  ) => {
    const now = new Date();
    const timeFormatted = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const newRecord: CallRecord = {
      id: 'rec_' + Date.now(),
      callerName: name,
      phoneNumber: number,
      timestamp: `Aujourd'hui, ${timeFormatted}`,
      direction: dir,
      duration: durationStr,
      simName: sim,
      status: status
    };
    setCallRecords(prev => [newRecord, ...prev]);
  };
  
  // Call Duration Timer (00:00)
  const [durationSeconds, setDurationSeconds] = useState<number>(0);

  useEffect(() => {
    let interval: any = null;
    if (callState === 'active') {
      interval = setInterval(() => {
        setDurationSeconds(sec => sec + 1);
      }, 1000);
    } else if (callState === 'idle') {
      setDurationSeconds(0);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [callState]);

  const formatDuration = (sec: number) => {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m < 10 ? '0' : ''}${m}:${s < 10 ? '0' : ''}${s}`;
  };

  // Logs
  const [logs, setLogs] = useState<Array<{ time: string; dir: 'RX' | 'TX' | 'UI'; text: string }>>([
    { time: '14:40:01', dir: 'TX', text: 'HELLO|BSB/1|BLACKBERRY_9790' },
    { time: '14:40:01', dir: 'UI', text: 'SDP: Service 0x111E (Handsfree Unit) et 0x1108 (Headset) déclarés' },
    { time: '14:40:02', dir: 'RX', text: 'SIM_LIST|2|inwi|0|Orange|1' },
    { time: '14:40:02', dir: 'UI', text: 'Loaded 2 SIMs: [0] inwi, [1] Orange' },
    { time: '14:40:03', dir: 'RX', text: 'AUDIO_STATUS|BLUETOOTH' },
    { time: '14:40:03', dir: 'UI', text: 'Audio initial: Bluetooth / BlackBerry (Micro + Écouteur)' },
  ]);

  const addLog = (dir: 'RX' | 'TX' | 'UI', text: string) => {
    const time = new Date().toTimeString().split(' ')[0];
    setLogs(prev => [ { time, dir, text }, ...prev.slice(0, 49) ]);
  };

  // Simulating incoming call from Android
  const triggerIncomingCall = (simIdx: number = 0) => {
    const chosenSim = sims[simIdx] || { name: 'inwi', slot: 0 };
    const callId = 'call_' + Math.floor(1000 + Math.random() * 9000);
    setCurrentCaller({
      id: callId,
      name: 'Hamza H.',
      number: '+212611223344',
      simName: chosenSim.name
    });
    setCallState('incoming');
    setSpeakerOn(false);
    setAudioRoute('BLUETOOTH');
    setDurationSeconds(0);
    addLog('RX', `CALL_INCOMING|${callId}|Hamza H.|+212611223344|${chosenSim.name}`);
    addLog('UI', `BlackBerry: Vibreur actif + Sonnerie d'appel continue sur [${chosenSim.name}]`);
  };

  // User answers on BlackBerry
  const handleAnswer = () => {
    setCallState('active');
    setDurationSeconds(0);
    addLog('TX', `CALL_ANSWER|${currentCaller.id}`);
    addLog('RX', `CALL_ACTIVE|${currentCaller.id}|${currentCaller.simName}`);
    addLog('UI', 'Chronomètre d\'appel démarré (00:00) - Bip de connexion');
    addLog('UI', 'Canal vocal Bluetooth SCO ouvert vers le BlackBerry');
  };

  // User rejects on BlackBerry
  const handleReject = () => {
    setCallState('idle');
    addLog('TX', `CALL_REJECT|${currentCaller.id}`);
    addLog('UI', 'Appel rejeté - Popup fermée');
    addCallRecord(currentCaller.name, currentCaller.number, 'incoming', 'Refusé', currentCaller.simName, 'rejected');
  };

  // Hangup from BlackBerry
  const handleHangup = () => {
    setCallState('ended');
    addLog('TX', 'CALL_END');
    addLog('RX', `CALL_END|${currentCaller.id}`);
    addLog('UI', 'Fin d\'appel : Bip sonore BlackBerry (Alert.startAudio)');
    
    // Determine call direction
    const direction: 'incoming' | 'outgoing' = currentCaller.id.startsWith('out_') ? 'outgoing' : 'incoming';
    const finalDuration = formatDuration(durationSeconds);
    addCallRecord(currentCaller.name, currentCaller.number, direction, finalDuration, currentCaller.simName, 'answered');

    setTimeout(() => {
      setCallState('idle');
      setSpeakerOn(false);
      addLog('UI', 'Retour à l\'écran d\'accueil');
    }, 1400);
  };

  // Toggle smartphone speaker
  const handleToggleSpeaker = () => {
    const nextState = !speakerOn;
    addLog('TX', 'SPEAKER_TOGGLE');
    setSpeakerOn(nextState);
    addLog('RX', `SPEAKER_STATUS|${nextState ? 'ON' : 'OFF'}`);
    addLog('UI', `Haut-Parleur Smartphone: ${nextState ? 'ACTIVÉ' : 'DÉSACTIVÉ'}`);
  };

  // Change Audio Route
  const handleSelectAudioRoute = (route: AudioRoute) => {
    setAudioChoiceModalOpen(false);
    setAudioRoute(route);
    addLog('TX', `AUDIO_ROUTE|${route}`);
    addLog('RX', `AUDIO_STATUS|${route}`);
    if (route === 'BLUETOOTH') {
      addLog('UI', 'Audio commuté vers : BlackBerry (Microphone + Écouteur)');
    } else if (route === 'SPEAKERPHONE') {
      addLog('UI', 'Audio commuté vers : Haut-parleur du Smartphone Android');
    } else {
      addLog('UI', 'Audio commuté vers : Écouteur du Smartphone Android');
    }
  };

  // Toggle local BlackBerry speaker / handset
  const handleToggleLocalBbSpeaker = () => {
    const nextState = !localBbSpeaker;
    setLocalBbSpeaker(nextState);
    addLog('UI', `Audio local BlackBerry (AudioPathControl) : ${nextState ? 'Haut-Parleur' : 'Combiné/Écouteur'}`);
  };

  // User initiates outbound call from BlackBerry
  const startOutboundDial = () => {
    if (sims.length >= 2) {
      setSimChoiceModalOpen(true);
    } else {
      executeOutboundCall(sims[0]?.slot ?? 0, sims[0]?.name ?? 'inwi');
    }
  };

  const executeOutboundCall = (slot: number, simName: string) => {
    setSimChoiceModalOpen(false);
    const callId = 'out_' + Date.now();
    setCurrentCaller({
      id: callId,
      name: 'Youssef B.',
      number: dialNumber,
      simName: simName
    });
    setCallState('outbound_dialing');
    setSpeakerOn(false);
    setAudioRoute('BLUETOOTH');
    setDurationSeconds(0);
    addLog('TX', `CALL_OUTBOUND|${dialNumber}|${slot}`);
    addLog('RX', `CALL_OUTBOUND_OK|${dialNumber}|${simName}|${slot}`);
    addLog('UI', `Écran appel sortant : "Numérotation sur [${simName}]..."`);

    // Auto connect after 2.2s for demonstration
    setTimeout(() => {
      setCallState(curr => {
        if (curr === 'outbound_dialing') {
          addLog('RX', `CALL_ACTIVE|${callId}|${simName}`);
          addLog('UI', 'Communication établie - Chronomètre actif');
          return 'active';
        }
        return curr;
      });
    }, 2200);
  };

  return (
    <div className="min-h-screen bg-[#0d1117] text-gray-100 flex flex-col items-center justify-start p-3 sm:p-6 font-sans">
      
      {/* App Header */}
      <header className="w-full max-w-6xl flex flex-wrap items-center justify-between pb-4 mb-4 border-b border-gray-800 gap-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-cyan-600/20 border border-cyan-500/40 flex items-center justify-center text-cyan-400">
            <Bluetooth className="w-5 h-5 animate-pulse" />
          </div>
          <div>
            <h1 className="text-lg sm:text-xl font-bold tracking-tight text-white flex items-center gap-2">
              BlackBerry SmartBridge <span className="text-xs px-2.5 py-0.5 rounded-full bg-cyan-500/20 text-cyan-400 border border-cyan-500/30">v1.4.0 Audio HFP & Double SIM</span>
            </h1>
            <p className="text-xs text-gray-400">
              Profil Mains-Libres Bluetooth (HFP 0x111E), Micro/Écouteur BlackBerry, Double SIM et Routage Audio
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 text-xs flex-wrap">
          <button 
            onClick={() => setCallHistoryModalOpen(true)}
            className="px-2.5 py-1 bg-cyan-500/10 hover:bg-cyan-500/20 text-cyan-400 border border-cyan-500/30 rounded-md font-mono flex items-center gap-1.5 cursor-pointer transition-colors"
          >
            <History className="w-3.5 h-3.5" />
            Historique Appels ({callRecords.length})
          </button>
          <button 
            onClick={() => setHfpGuideModalOpen(true)}
            className="px-2.5 py-1 bg-yellow-500/10 hover:bg-yellow-500/20 text-yellow-400 border border-yellow-500/30 rounded-md font-mono flex items-center gap-1.5 cursor-pointer transition-colors"
          >
            <HelpCircle className="w-3.5 h-3.5" />
            Guide HFP / Audio
          </button>
          <span className="px-2.5 py-1 bg-cyan-500/10 text-cyan-400 border border-cyan-500/30 rounded-md font-mono flex items-center gap-1.5">
            <Headphones className="w-3.5 h-3.5 text-cyan-400" />
            HFP: SDP 0x111E
          </span>
          <span className="px-2.5 py-1 bg-green-500/10 text-green-400 border border-green-500/30 rounded-md font-mono flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-green-400 animate-ping"></span>
            SPP: CONNECTÉ
          </span>
        </div>
      </header>

      {/* Main Grid: BB Device Emulator + Android Companion Controls */}
      <div className="w-full max-w-6xl grid grid-cols-1 lg:grid-cols-12 gap-6">
        
        {/* LEFT COLUMN: BlackBerry Curve 9300 Screen Simulation (5 Cols) */}
        <div className="lg:col-span-5 flex flex-col items-center">
          <div className="w-full max-w-[360px] bg-[#161a22] border-4 border-[#30363d] rounded-[36px] p-4 shadow-2xl relative flex flex-col items-center">
            
            {/* Top Speaker Ear-piece & Status LED */}
            <div className="w-full flex items-center justify-between px-6 mb-3">
              <span className="text-[10px] tracking-widest text-gray-400 font-bold">BlackBerry</span>
              <div className="w-16 h-1.5 bg-gray-700 rounded-full flex items-center justify-center">
                <div className="w-6 h-0.5 bg-gray-900 rounded-full"></div>
              </div>
              <div className={`w-2.5 h-2.5 rounded-full ${callState === 'incoming' ? 'bg-red-500 animate-ping' : callState === 'active' ? 'bg-green-400' : 'bg-green-500'}`}></div>
            </div>

            {/* CURVE SCREEN (320x240 Aspect Ratio) */}
            <div className="w-[320px] h-[240px] bg-black border-2 border-gray-800 rounded-md overflow-hidden relative font-mono text-xs select-none flex flex-col">
              
              {/* STATUS BAR */}
              <div className="h-6 bg-[#111] border-b border-gray-800 flex items-center justify-between px-2 text-[10px] text-gray-300">
                <span className="text-cyan-400 font-bold flex items-center gap-1">
                  <Bluetooth className="w-3 h-3 text-cyan-400" />
                  [HFP/SPP]
                </span>
                <span className="text-yellow-400 font-semibold">[4G] inwi - Signal: [||||]</span>
                <span className="text-green-400">[BB: 88%]</span>
              </div>

              {/* SCREEN CONTENT BY CALL STATE */}
              {callState === 'idle' && (
                <div className="flex-1 flex flex-col justify-between p-2">
                  <div className="text-center pt-1">
                    <div className="text-2xl font-bold tracking-wider text-white">14:42</div>
                    <div className="text-[10px] text-gray-400">Jeudi 24 Septembre 2026</div>
                    <div className="text-[11px] text-cyan-400 mt-0.5">
                      Double SIM Active : [{sims.map(s => s.name).join(' | ')}]
                    </div>
                    <div className="text-[9px] text-green-400 flex items-center justify-center gap-1 mt-0.5">
                      <Mic className="w-2.5 h-2.5" />
                      Audio Mains-Libres Prêt (SCO)
                    </div>
                  </div>

                  {/* 3x3 Grid Buttons Preview */}
                  <div className="grid grid-cols-3 gap-1 px-1">
                    <button onClick={startOutboundDial} className="bg-[#222] hover:bg-[#333] border border-cyan-800 text-cyan-300 py-1 rounded text-[10px] font-bold text-center cursor-pointer">
                      📞 Calls
                    </button>
                    <button onClick={() => setCallHistoryModalOpen(true)} className="bg-[#222] hover:bg-[#333] border border-cyan-700 text-cyan-300 py-1 rounded text-[10px] font-bold text-center cursor-pointer">
                      📜 History
                    </button>
                    <button className="bg-[#222] border border-gray-700 text-gray-300 py-1 rounded text-[10px] text-center">
                      ✉️ Messages
                    </button>
                    <button className="bg-[#222] border border-gray-700 text-gray-300 py-1 rounded text-[10px] text-center">
                      🔔 Notifs (2)
                    </button>
                    <button className="bg-[#222] border border-gray-700 text-gray-300 py-1 rounded text-[10px] text-center">
                      💬 WhatsApp
                    </button>
                    <button onClick={() => setHfpGuideModalOpen(true)} className="bg-[#222] border border-yellow-700 text-yellow-300 py-1 rounded text-[10px] text-center cursor-pointer">
                      🎧 Audio HFP
                    </button>
                  </div>

                  <div className="text-[9px] text-gray-500 text-center">
                    Touche Verte pour composer | Menu pour options HFP
                  </div>
                </div>
              )}

              {/* INCOMING CALL MODAL POPUP */}
              {callState === 'incoming' && (
                <div className="flex-1 bg-black flex flex-col justify-between p-2 text-center animate-pulse">
                  <div>
                    <div className="text-sm font-bold text-green-400 uppercase tracking-wider">
                      APPEL ENTRANT...
                    </div>
                    <div className="text-xs font-bold text-yellow-400">
                      sur [{currentCaller.simName}]
                    </div>
                    <div className="w-full h-[1px] bg-gray-800 my-1"></div>
                  </div>

                  <div>
                    <div className="text-lg font-bold text-white leading-tight">
                      {currentCaller.name}
                    </div>
                    <div className="text-xs text-gray-400">
                      {currentCaller.number}
                    </div>
                    <div className="text-xs text-yellow-500 font-bold mt-1">
                      Sonnerie & Vibreur en cours...
                    </div>
                    <div className="text-[10px] text-cyan-400 mt-0.5">
                      [Audio : Micro & Écouteur BlackBerry]
                    </div>
                  </div>

                  <div>
                    <div className="flex justify-center gap-2 mb-1">
                      <button 
                        onClick={handleAnswer} 
                        className="px-4 py-1.5 bg-green-700 hover:bg-green-600 text-white font-bold rounded-lg text-xs cursor-pointer border border-green-400 shadow"
                      >
                        Décrocher
                      </button>
                      <button 
                        onClick={handleReject} 
                        className="px-4 py-1.5 bg-red-700 hover:bg-red-600 text-white font-bold rounded-lg text-xs cursor-pointer border border-red-400 shadow"
                      >
                        Refuser
                      </button>
                    </div>
                    <div className="text-[8px] text-gray-400">
                      Touche Verte: Décrocher | Rouge / Échap: Refuser
                    </div>
                  </div>
                </div>
              )}

              {/* OUTBOUND OR ACTIVE CALL SCREEN */}
              {(callState === 'outbound_dialing' || callState === 'active' || callState === 'ended') && (
                <div className="flex-1 bg-black flex flex-col justify-between p-2 text-center">
                  <div>
                    <div className={`text-xs font-bold uppercase tracking-wider ${
                      callState === 'ended' ? 'text-red-400' :
                      callState === 'active' ? 'text-green-400' : 'text-cyan-400'
                    }`}>
                      {callState === 'ended' ? 'APPEL TERMINÉ' :
                       callState === 'active' ? 'COMMUNICATION ACTIVE' : 'APPEL SORTANT'}
                    </div>
                    <div className="text-xs font-bold text-yellow-400">
                      sur [{currentCaller.simName}]
                    </div>
                    <div className="w-full h-[1px] bg-gray-800 my-0.5"></div>
                  </div>

                  <div>
                    <div className="text-base font-bold text-white leading-tight">
                      {currentCaller.name}
                    </div>
                    <div className="text-xs text-gray-400">
                      {currentCaller.number}
                    </div>

                    {/* DURATION OR STATUS */}
                    <div className={`text-xs font-bold mt-1 flex items-center justify-center gap-1.5 ${
                      callState === 'ended' ? 'text-red-400' :
                      callState === 'active' ? 'text-green-400' : 'text-cyan-300'
                    }`}>
                      {callState === 'active' ? (
                        <>
                          <Clock className="w-3 h-3 animate-pulse" />
                          <span>En communication ({formatDuration(durationSeconds)})</span>
                        </>
                      ) : callState === 'ended' ? (
                        <span>Durée : {formatDuration(durationSeconds)} - Terminé</span>
                      ) : (
                        <span>Numérotation en cours...</span>
                      )}
                    </div>

                    {/* AUDIO BADGE */}
                    <div className="mt-1 flex flex-col items-center gap-0.5">
                      <span className={`text-[10px] font-bold ${
                        audioRoute === 'BLUETOOTH' ? 'text-cyan-400' :
                        audioRoute === 'SPEAKERPHONE' ? 'text-amber-400' : 'text-gray-300'
                      }`}>
                        {audioRoute === 'BLUETOOTH' ? '[Audio: Bluetooth / BlackBerry]' :
                         audioRoute === 'SPEAKERPHONE' ? '[Audio: Haut-parleur Smartphone]' :
                         '[Audio: Écouteur Smartphone]'}
                      </span>
                      <div className="flex items-center gap-2 text-[9px]">
                        <span className={speakerOn ? 'text-green-400 font-bold' : 'text-gray-500'}>
                          [HP Tel: {speakerOn ? 'ON' : 'OFF'}]
                        </span>
                        <span className={localBbSpeaker ? 'text-green-400' : 'text-gray-400'}>
                          [Sortie BB: {localBbSpeaker ? 'HP' : 'Combiné'}]
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* ACTION BUTTONS */}
                  <div>
                    {callState !== 'ended' ? (
                      <div className="flex justify-center gap-1.5 mb-1">
                        <button 
                          onClick={handleHangup} 
                          className="px-2.5 py-1 bg-red-700 hover:bg-red-600 text-white font-bold rounded text-xs cursor-pointer border border-red-400 shadow"
                        >
                          Raccrocher
                        </button>
                        <button 
                          onClick={handleToggleSpeaker} 
                          className={`px-2 py-1 font-bold rounded text-xs cursor-pointer border ${speakerOn ? 'bg-cyan-800 border-cyan-400 text-white' : 'bg-gray-800 border-gray-600 text-gray-200'}`}
                        >
                          {speakerOn ? 'HP: ON' : 'HP Tel'}
                        </button>
                        <button 
                          onClick={() => setAudioChoiceModalOpen(true)} 
                          className="px-2 py-1 bg-cyan-900/60 hover:bg-cyan-800 text-cyan-200 font-bold rounded text-xs cursor-pointer border border-cyan-500/50"
                        >
                          Audio...
                        </button>
                      </div>
                    ) : (
                      <div className="text-[10px] text-red-300 py-1 font-bold">
                        Bip de fin... Fermeture automatique
                      </div>
                    )}
                    <div className="text-[8px] text-gray-400">
                      Menu BlackBerry : Option 'Haut-parleur ON/OFF' & 'Route Audio'
                    </div>
                  </div>
                </div>
              )}

              {/* POPUP SIM SELECTION DIALOG (CURVE MODAL) */}
              {simChoiceModalOpen && (
                <div className="absolute inset-0 bg-black/85 backdrop-blur-xs flex items-center justify-center p-3 z-20">
                  <div className="w-full bg-[#1e232b] border border-cyan-500/60 rounded-lg p-2.5 text-center shadow-2xl">
                    <div className="text-xs font-bold text-white mb-2">
                      Appeler {dialNumber} avec :
                    </div>
                    <div className="space-y-1.5">
                      {sims.map(sim => (
                        <button
                          key={sim.slot}
                          onClick={() => executeOutboundCall(sim.slot, sim.name)}
                          className="w-full py-1 px-2 bg-cyan-900/40 hover:bg-cyan-700 text-cyan-200 text-xs font-bold rounded border border-cyan-600/50 flex items-center justify-between cursor-pointer"
                        >
                          <span>Option {sim.slot + 1} :</span>
                          <span className="text-yellow-400 font-bold">{sim.name} (Slot {sim.slot})</span>
                        </button>
                      ))}
                      <button
                        onClick={() => setSimChoiceModalOpen(false)}
                        className="w-full py-1 text-[10px] text-gray-400 hover:text-white mt-1 cursor-pointer"
                      >
                        Annuler (Touche Échap)
                      </button>
                    </div>
                  </div>
                </div>
              )}

              {/* POPUP AUDIO ROUTE DIALOG (CURVE MODAL) */}
              {audioChoiceModalOpen && (
                <div className="absolute inset-0 bg-black/85 backdrop-blur-xs flex items-center justify-center p-2 z-20">
                  <div className="w-full bg-[#1e232b] border border-cyan-500/60 rounded-lg p-2 text-center shadow-2xl">
                    <div className="text-[11px] font-bold text-white mb-1.5">
                      Choisir la sortie audio :
                    </div>
                    <div className="space-y-1 text-[10px]">
                      <button
                        onClick={() => handleSelectAudioRoute('BLUETOOTH')}
                        className={`w-full py-1 px-2 text-left rounded border flex items-center justify-between cursor-pointer ${
                          audioRoute === 'BLUETOOTH' ? 'bg-cyan-700 text-white border-cyan-400' : 'bg-gray-800 text-gray-300 border-gray-700'
                        }`}
                      >
                        <span>1: Bluetooth / BlackBerry</span>
                        <Headphones className="w-3 h-3 text-cyan-300" />
                      </button>
                      <button
                        onClick={() => handleSelectAudioRoute('SPEAKERPHONE')}
                        className={`w-full py-1 px-2 text-left rounded border flex items-center justify-between cursor-pointer ${
                          audioRoute === 'SPEAKERPHONE' ? 'bg-amber-800 text-white border-amber-400' : 'bg-gray-800 text-gray-300 border-gray-700'
                        }`}
                      >
                        <span>2: Haut-parleur Smartphone</span>
                        <Volume2 className="w-3 h-3 text-amber-300" />
                      </button>
                      <button
                        onClick={() => handleSelectAudioRoute('EARPIECE')}
                        className={`w-full py-1 px-2 text-left rounded border flex items-center justify-between cursor-pointer ${
                          audioRoute === 'EARPIECE' ? 'bg-blue-800 text-white border-blue-400' : 'bg-gray-800 text-gray-300 border-gray-700'
                        }`}
                      >
                        <span>3: Écouteur Smartphone</span>
                        <Smartphone className="w-3 h-3 text-blue-300" />
                      </button>
                      <button
                        onClick={handleToggleLocalBbSpeaker}
                        className="w-full py-1 px-2 text-left bg-gray-900 text-green-300 rounded border border-green-800/60 flex items-center justify-between cursor-pointer"
                      >
                        <span>4: Basculer HP/Combiné BB</span>
                        <span className="text-[9px] font-mono">{localBbSpeaker ? 'HP' : 'Combiné'}</span>
                      </button>
                      <button
                        onClick={() => setAudioChoiceModalOpen(false)}
                        className="w-full py-0.5 text-[9px] text-gray-400 hover:text-white cursor-pointer"
                      >
                        Fermer (Touche Échap)
                      </button>
                    </div>
                  </div>
                </div>
              )}

            </div>

            {/* CURVE TRACKPAD & HARDWARE BUTTONS */}
            <div className="w-[320px] flex items-center justify-between px-4 mt-3">
              <button 
                onClick={() => {
                  if (callState === 'incoming') handleAnswer();
                  else if (callState === 'idle') startOutboundDial();
                  else if (callState === 'active') setAudioChoiceModalOpen(true);
                }} 
                className="w-10 h-7 bg-green-700 hover:bg-green-600 rounded-lg text-white text-[11px] font-bold shadow flex items-center justify-center cursor-pointer"
                title="Touche Verte (Send)"
              >
                📞
              </button>
              <button 
                onClick={() => setAudioChoiceModalOpen(true)}
                className="w-8 h-7 bg-gray-700 hover:bg-gray-600 rounded-lg text-gray-300 text-[10px] font-bold cursor-pointer"
                title="Touche Menu"
              >
                MENU
              </button>
              {/* Optical Trackpad */}
              <div 
                onClick={() => {
                  if (callState === 'active') setAudioChoiceModalOpen(true);
                }}
                className="w-10 h-10 rounded-full bg-black border-2 border-gray-600 shadow-inner flex items-center justify-center cursor-pointer hover:border-cyan-400"
                title="Trackpad optique Curve (Clic)"
              >
                <div className="w-3.5 h-3.5 rounded-full bg-gray-500"></div>
              </div>
              <button 
                onClick={() => {
                  setAudioChoiceModalOpen(false);
                  setSimChoiceModalOpen(false);
                }}
                className="w-8 h-7 bg-gray-700 hover:bg-gray-600 rounded-lg text-gray-300 text-[10px] font-bold cursor-pointer"
                title="Touche Échap / Retour"
              >
                ESC
              </button>
              <button 
                onClick={() => {
                  if (callState === 'incoming') handleReject();
                  else if (callState !== 'idle') handleHangup();
                }} 
                className="w-10 h-7 bg-red-700 hover:bg-red-600 rounded-lg text-white text-[11px] font-bold shadow flex items-center justify-center cursor-pointer"
                title="Touche Rouge (End / Hangup)"
              >
                🔴
              </button>
            </div>

            {/* Physical QWERTY Keyboard hint */}
            <div className="w-full flex justify-center mt-2">
              <span className="text-[9px] text-gray-500 font-mono">Clavier physique Curve 9300 actif</span>
            </div>
          </div>
        </div>

        {/* RIGHT COLUMN: Android Test Bench & Bluetooth Protocol Inspector (7 Cols) */}
        <div className="lg:col-span-7 flex flex-col gap-4">
          
          {/* Card 1: Audio Routing & Handsfree HFP Control */}
          <div className="bg-[#161a22] border border-cyan-800/60 rounded-xl p-4 shadow-sm relative overflow-hidden">
            <div className="absolute top-0 right-0 px-3 py-1 bg-cyan-600/20 text-cyan-400 text-[10px] font-mono rounded-bl-lg border-b border-l border-cyan-500/30">
              AUDIO VOIX (SCO / HFP)
            </div>

            <h2 className="text-sm font-bold text-white flex items-center gap-2 mb-2">
              <Headphones className="w-4 h-4 text-cyan-400" />
              1. Routage Audio Téléphonie & Micro/Écouteur
            </h2>
            <p className="text-xs text-gray-400 mb-3">
              Contrôlez où transite la voix en temps réel (BlackBerry Curve vs Haut-parleur / Écouteur Android).
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 text-xs mb-3">
              <button
                onClick={() => handleSelectAudioRoute('BLUETOOTH')}
                className={`p-2.5 rounded-lg border flex flex-col items-center gap-1.5 cursor-pointer text-center ${
                  audioRoute === 'BLUETOOTH' ? 'bg-cyan-950/80 border-cyan-400 text-cyan-200' : 'bg-gray-900 border-gray-800 text-gray-400 hover:bg-gray-800'
                }`}
              >
                <Headphones className="w-4 h-4 text-cyan-400" />
                <span className="font-bold text-[11px]">Audio Bluetooth / BB</span>
                <span className="text-[9px] text-gray-400">Micro & HP Curve</span>
              </button>

              <button
                onClick={() => handleSelectAudioRoute('SPEAKERPHONE')}
                className={`p-2.5 rounded-lg border flex flex-col items-center gap-1.5 cursor-pointer text-center ${
                  audioRoute === 'SPEAKERPHONE' ? 'bg-amber-950/80 border-amber-400 text-amber-200' : 'bg-gray-900 border-gray-800 text-gray-400 hover:bg-gray-800'
                }`}
              >
                <Volume2 className="w-4 h-4 text-amber-400" />
                <span className="font-bold text-[11px]">HP Smartphone</span>
                <span className="text-[9px] text-gray-400">Haut-parleur Android</span>
              </button>

              <button
                onClick={() => handleSelectAudioRoute('EARPIECE')}
                className={`p-2.5 rounded-lg border flex flex-col items-center gap-1.5 cursor-pointer text-center ${
                  audioRoute === 'EARPIECE' ? 'bg-blue-950/80 border-blue-400 text-blue-200' : 'bg-gray-900 border-gray-800 text-gray-400 hover:bg-gray-800'
                }`}
              >
                <Smartphone className="w-4 h-4 text-blue-400" />
                <span className="font-bold text-[11px]">Écouteur Smartphone</span>
                <span className="text-[9px] text-gray-400">Combiné Android</span>
              </button>
            </div>

            {/* Audio Signal Flow Diagram */}
            <div className="bg-[#0d1117] p-2.5 rounded-lg border border-gray-800 text-[11px] font-mono space-y-1">
              <div className="text-gray-400 font-bold flex items-center justify-between">
                <span>Flux Audio SCO Bidirectionnel :</span>
                <span className="text-cyan-400 font-bold">Profil HFP v1.5</span>
              </div>
              <div className="text-gray-300 flex items-center gap-1 text-[10px]">
                <Mic className="w-3 h-3 text-red-400" />
                <span>Micro BlackBerry ➔ Voix transmise à l'interlocuteur via SCO</span>
              </div>
              <div className="text-gray-300 flex items-center gap-1 text-[10px]">
                <Headphones className="w-3 h-3 text-cyan-400" />
                <span>Interlocuteur ➔ Entendu dans {audioRoute === 'BLUETOOTH' ? "l'écouteur du BlackBerry Curve" : audioRoute}</span>
              </div>
            </div>
          </div>

          {/* Card 2: Double SIM & Outbound Controls */}
          <div className="bg-[#161a22] border border-gray-800 rounded-xl p-4 shadow-sm">
            <h2 className="text-sm font-bold text-white flex items-center gap-2 mb-3">
              <SimIcon className="w-4 h-4 text-cyan-400" />
              2. Configuration Double SIM & Appels Sortants
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
              <div className="bg-[#0d1117] p-2.5 rounded-lg border border-gray-800">
                <span className="text-gray-400 block mb-1">Cartes SIM reçues via Bluetooth :</span>
                <div className="space-y-1.5">
                  {sims.map((sim, i) => (
                    <div key={sim.slot} className="flex items-center justify-between bg-gray-900 px-2.5 py-1.5 rounded border border-gray-800">
                      <span className="font-semibold text-yellow-400">SIM {i + 1} : {sim.name}</span>
                      <span className="text-[10px] px-1.5 py-0.5 rounded bg-gray-800 text-gray-300 font-mono">Slot {sim.slot}</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="bg-[#0d1117] p-2.5 rounded-lg border border-gray-800 flex flex-col justify-between">
                <div>
                  <span className="text-gray-400 block mb-1">Numéro à appeler depuis le BlackBerry :</span>
                  <input
                    type="text"
                    value={dialNumber}
                    onChange={(e) => setDialNumber(e.target.value)}
                    className="w-full bg-[#161a22] border border-gray-700 rounded px-2.5 py-1.5 text-xs text-white font-mono"
                    placeholder="+212634934134"
                  />
                </div>
                <button
                  onClick={startOutboundDial}
                  className="w-full mt-2 py-1.5 bg-cyan-700 hover:bg-cyan-600 text-white rounded font-bold text-xs flex items-center justify-center gap-1.5 cursor-pointer shadow"
                >
                  <Phone className="w-3.5 h-3.5" />
                  Appeler (Déclenche sélecteur SIM)
                </button>
              </div>
            </div>
          </div>

          {/* Card 3: Incoming Call Simulation */}
          <div className="bg-[#161a22] border border-gray-800 rounded-xl p-4 shadow-sm">
            <h2 className="text-sm font-bold text-white flex items-center gap-2 mb-2">
              <PhoneIncoming className="w-4 h-4 text-green-400" />
              3. Déclencheur d'Événements Android vers BlackBerry
            </h2>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs">
              <button
                onClick={() => triggerIncomingCall(0)}
                className="p-2 bg-green-950/40 hover:bg-green-900/60 border border-green-700/50 rounded text-green-300 font-semibold flex flex-col items-center gap-1 cursor-pointer"
              >
                <PhoneIncoming className="w-4 h-4" />
                <span>Entrant (SIM 1: inwi)</span>
              </button>

              <button
                onClick={() => triggerIncomingCall(1)}
                className="p-2 bg-yellow-950/40 hover:bg-yellow-900/60 border border-yellow-700/50 rounded text-yellow-300 font-semibold flex flex-col items-center gap-1 cursor-pointer"
              >
                <PhoneIncoming className="w-4 h-4" />
                <span>Entrant (SIM 2: Orange)</span>
              </button>

              <button
                onClick={handleToggleSpeaker}
                className="p-2 bg-blue-950/40 hover:bg-blue-900/60 border border-blue-700/50 rounded text-blue-300 font-semibold flex flex-col items-center gap-1 cursor-pointer"
              >
                {speakerOn ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
                <span>Bascule HP ({speakerOn ? 'ON' : 'OFF'})</span>
              </button>

              <button
                onClick={handleHangup}
                className="p-2 bg-red-950/40 hover:bg-red-900/60 border border-red-700/50 rounded text-red-300 font-semibold flex flex-col items-center gap-1 cursor-pointer"
              >
                <PhoneOff className="w-4 h-4" />
                <span>Fin d'appel (CALL_END)</span>
              </button>
            </div>

            <div className="mt-3 pt-2.5 border-t border-gray-800/80 flex flex-wrap items-center justify-between gap-2 text-xs">
              <span className="text-gray-400 flex items-center gap-1.5">
                <History className="w-3.5 h-3.5 text-cyan-400" />
                <span>Journal d'appels récents : <strong className="text-cyan-400 font-mono">{callRecords.length}</strong></span>
              </span>
              <button
                onClick={() => setCallHistoryModalOpen(true)}
                className="px-2.5 py-1 bg-cyan-900/40 hover:bg-cyan-800/60 border border-cyan-600/40 hover:border-cyan-500 rounded text-cyan-200 text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer"
              >
                <History className="w-3.5 h-3.5" />
                <span>Afficher le Journal Complet</span>
              </button>
            </div>
          </div>

          {/* Card 4: D3.js Battery Discharge Trend Line Chart */}
          <BatteryDischargeChart currentLevel={88} />

          {/* Card 5: Live Protocol Log Trace */}
          <div className="bg-[#161a22] border border-gray-800 rounded-xl p-3 shadow-sm flex flex-col flex-1">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-bold text-gray-300 flex items-center gap-1.5">
                <Cpu className="w-3.5 h-3.5 text-cyan-400" />
                Trace Protocole Bluetooth BSB & Audio HFP
              </span>
              <button 
                onClick={() => setLogs([])}
                className="text-[10px] text-gray-400 hover:text-white cursor-pointer"
              >
                Effacer logs
              </button>
            </div>

            <div className="h-44 bg-black/90 rounded border border-gray-800 p-2 overflow-y-auto font-mono text-[11px] space-y-1">
              {logs.map((lg, idx) => (
                <div key={idx} className="flex items-start gap-2">
                  <span className="text-gray-500 text-[9px] shrink-0">{lg.time}</span>
                  <span className={`px-1 py-0.2 rounded text-[9px] font-bold shrink-0 ${
                    lg.dir === 'RX' ? 'bg-purple-900/60 text-purple-300' :
                    lg.dir === 'TX' ? 'bg-cyan-900/60 text-cyan-300' :
                    'bg-green-900/60 text-green-300'
                  }`}>
                    {lg.dir}
                  </span>
                  <span className={`${
                    lg.dir === 'RX' ? 'text-purple-200' :
                    lg.dir === 'TX' ? 'text-cyan-200 font-semibold' :
                    'text-gray-400 italic'
                  }`}>
                    {lg.text}
                  </span>
                </div>
              ))}
            </div>
          </div>

        </div>
      </div>

      {/* Guide HFP Modal */}
      {hfpGuideModalOpen && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="max-w-md w-full bg-[#161a22] border border-cyan-500/50 rounded-2xl p-5 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-gray-800 pb-2">
              <div className="flex items-center gap-2 text-cyan-400 font-bold">
                <Headphones className="w-5 h-5" />
                <span>Guide Audio Mains-Libres Bluetooth (HFP)</span>
              </div>
              <button 
                onClick={() => setHfpGuideModalOpen(false)}
                className="text-gray-400 hover:text-white cursor-pointer text-sm"
              >
                ✕
              </button>
            </div>

            <div className="text-xs space-y-3 text-gray-300">
              <div className="bg-[#0d1117] p-3 rounded-lg border border-gray-800 space-y-1.5">
                <div className="font-bold text-yellow-400">1. Sur le BlackBerry Curve / Bold :</div>
                <p>
                  Allez dans <strong>Options &gt; Bluetooth &gt; Touche Menu &gt; Options Bluetooth</strong>.
                  Vérifiez que le profil <em>Passerelle audio mains libres</em> ou <em>Casque</em> est activé.
                  L'application SmartBridge v1.4.0 enregistre automatiquement l'enregistrement de service SDP (UUID 0x111E).
                </p>
              </div>

              <div className="bg-[#0d1117] p-3 rounded-lg border border-gray-800 space-y-1.5">
                <div className="font-bold text-yellow-400">2. Sur le smartphone Android :</div>
                <p>
                  Dans <strong>Paramètres &gt; Bluetooth &gt; Appareils appairés &gt; BlackBerry Curve</strong>.
                  Activez l'interrupteur <strong>« Appels audio »</strong> (Handsfree Profile).
                </p>
              </div>

              <div className="bg-[#0d1117] p-3 rounded-lg border border-gray-800 space-y-1.5">
                <div className="font-bold text-yellow-400">3. Lors d'un appel :</div>
                <p>
                  Parlez dans le microphone du BlackBerry et écoutez dans son écouteur !
                  Vous pouvez basculer le son vers le haut-parleur du BlackBerry ou celui du smartphone avec l'option <strong>Route Audio</strong>.
                </p>
              </div>
            </div>

            <button
              onClick={() => setHfpGuideModalOpen(false)}
              className="w-full py-2 bg-cyan-700 hover:bg-cyan-600 text-white rounded-lg font-bold text-xs cursor-pointer"
            >
              Compris
            </button>
          </div>
        </div>
      )}

      {/* Call History Modal */}
      <CallHistoryModal
        isOpen={callHistoryModalOpen}
        onClose={() => setCallHistoryModalOpen(false)}
        callRecords={callRecords}
        onSelectNumberToCall={(num, name) => {
          setDialNumber(num);
          setCallHistoryModalOpen(false);
          addLog('UI', `Sélection depuis l'historique : ${name} (${num})`);
          if (sims.length >= 2) {
            setSimChoiceModalOpen(true);
          } else {
            executeOutboundCall(sims[0]?.slot ?? 0, sims[0]?.name ?? 'inwi');
          }
        }}
        onClearHistory={() => {
          setCallRecords([]);
          addLog('UI', 'Historique des appels récents réinitialisé');
        }}
      />

      {/* Compiled Deliverables Footer */}
      <footer className="w-full max-w-6xl mt-6 p-4 bg-[#161a22] border border-gray-800 rounded-xl flex flex-wrap items-center justify-between gap-3 text-xs">
        <div className="flex items-center gap-3">
          <CheckCircle2 className="w-5 h-5 text-green-400" />
          <div>
            <div className="font-bold text-white">Binaires BlackBerry OS 5.0 (v1.4.0) générés avec succès</div>
            <div className="text-[11px] text-gray-400">
              Compilé avec BlackBerry RAPC, Preverified CLDC 1.1 pour Curve 9300 &amp; Bold
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <a
            href="/blackberry/BBSmartBridge.cod"
            download="BBSmartBridge.cod"
            className="px-3 py-1.5 bg-gray-800 hover:bg-gray-700 text-gray-200 rounded border border-gray-700 font-mono flex items-center gap-1.5 cursor-pointer"
          >
            <Download className="w-3.5 h-3.5 text-cyan-400" />
            BBSmartBridge.cod (84 Ko)
          </a>
          <a
            href="/blackberry/BBSmartBridge.jad"
            download="BBSmartBridge.jad"
            className="px-3 py-1.5 bg-gray-800 hover:bg-gray-700 text-gray-200 rounded border border-gray-700 font-mono flex items-center gap-1.5 cursor-pointer"
          >
            <Download className="w-3.5 h-3.5 text-yellow-400" />
            BBSmartBridge.jad
          </a>
          <a
            href="/blackberry/BBSmartBridge.alx"
            download="BBSmartBridge.alx"
            className="px-3 py-1.5 bg-gray-800 hover:bg-gray-700 text-gray-200 rounded border border-gray-700 font-mono flex items-center gap-1.5 cursor-pointer"
          >
            <Download className="w-3.5 h-3.5 text-green-400" />
            BBSmartBridge.alx
          </a>
        </div>
      </footer>
    </div>
  );
}
