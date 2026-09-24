/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import { 
  Phone, 
  PhoneIncoming, 
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
  BatteryMedium
} from 'lucide-react';

interface Sim {
  name: string;
  slot: number;
}

export default function App() {
  // Bluetooth Link & Telephony State
  const [sims, setSims] = useState<Sim[]>([
    { name: 'inwi', slot: 0 },
    { name: 'Orange', slot: 1 }
  ]);
  const [selectedSimSlot, setSelectedSimSlot] = useState<number>(0);
  
  // Call State
  const [callState, setCallState] = useState<'idle' | 'incoming' | 'outbound_dialing' | 'active'>('idle');
  const [currentCaller, setCurrentCaller] = useState({
    id: 'call_1042',
    name: 'Amina',
    number: '+212634934134',
    simName: 'inwi'
  });
  const [speakerOn, setSpeakerOn] = useState<boolean>(false);
  const [simChoiceModalOpen, setSimChoiceModalOpen] = useState<boolean>(false);
  const [dialNumber, setDialNumber] = useState<string>('+212634934134');
  
  // Logs
  const [logs, setLogs] = useState<Array<{ time: string; dir: 'RX' | 'TX' | 'UI'; text: string }>>([
    { time: '14:20:01', dir: 'TX', text: 'HELLO|BSB/1|BLACKBERRY_9790' },
    { time: '14:20:01', dir: 'TX', text: 'GET_SIMS' },
    { time: '14:20:02', dir: 'RX', text: 'SIM_LIST|2|inwi|0|Orange|1' },
    { time: '14:20:02', dir: 'UI', text: 'Loaded 2 SIMs: [0] inwi, [1] Orange' },
    { time: '14:20:05', dir: 'RX', text: 'CELL_TELEMETRY|inwi|4G|4|ONLINE' },
  ]);

  const addLog = (dir: 'RX' | 'TX' | 'UI', text: string) => {
    const time = new Date().toTimeString().split(' ')[0];
    setLogs(prev => [ { time, dir, text }, ...prev.slice(0, 49) ]);
  };

  // Simulating incoming call from Android
  const triggerIncomingCall = (simIdx: number = 0) => {
    const chosenSim = sims[simIdx] || { name: 'inwi', slot: 0 };
    setCurrentCaller({
      id: 'call_' + Math.floor(1000 + Math.random() * 9000),
      name: 'Hamza H.',
      number: '+212611223344',
      simName: chosenSim.name
    });
    setCallState('incoming');
    setSpeakerOn(false);
    addLog('RX', `CALL_INCOMING|call_1042|Hamza H.|+212611223344|${chosenSim.name}`);
    addLog('UI', `BlackBerry: Vibreur actif + Sonnerie + Popup modale [sur ${chosenSim.name}]`);
  };

  // User answers on BlackBerry
  const handleAnswer = () => {
    setCallState('active');
    addLog('TX', `CALL_ANSWER|${currentCaller.id}`);
    addLog('RX', `CALL_ACTIVE|${currentCaller.id}`);
    addLog('UI', 'Communication active - Sonnerie coupée');
  };

  // User rejects on BlackBerry
  const handleReject = () => {
    setCallState('idle');
    addLog('TX', `CALL_REJECT|${currentCaller.id}`);
    addLog('UI', 'Appel rejeté - Popup fermée');
  };

  // Hangup from BlackBerry
  const handleHangup = () => {
    setCallState('idle');
    setSpeakerOn(false);
    addLog('TX', 'CALL_END');
    addLog('RX', `CALL_END|${currentCaller.id}`);
    addLog('UI', 'Fin d\'appel - Écran revenu à l\'accueil');
  };

  // Toggle speaker
  const handleToggleSpeaker = () => {
    const nextState = !speakerOn;
    addLog('TX', 'SPEAKER_TOGGLE');
    setSpeakerOn(nextState);
    addLog('RX', `SPEAKER_STATUS|${nextState ? 'ON' : 'OFF'}`);
    addLog('UI', `Haut-Parleur: ${nextState ? 'ACTIVÉ' : 'DÉSACTIVÉ'}`);
  };

  // User initiates outbound call from BlackBerry
  const startOutboundDial = () => {
    if (sims.length >= 2) {
      setSimChoiceModalOpen(true);
    } else {
      executeOutboundCall(sims[0]?.slot ?? 0, sims[0]?.name ?? 'Défaut');
    }
  };

  const executeOutboundCall = (slot: number, simName: string) => {
    setSimChoiceModalOpen(false);
    setCurrentCaller({
      id: 'out_' + Date.now(),
      name: 'Youssef B.',
      number: dialNumber,
      simName: simName
    });
    setCallState('outbound_dialing');
    setSpeakerOn(false);
    addLog('TX', `CALL_OUTBOUND|${dialNumber}|${slot}`);
    addLog('RX', `CALL_OUTBOUND_OK|${dialNumber}|${simName}|${slot}`);
    addLog('UI', `Écran appel: "Appel en cours sur ${simName}..."`);

    // Auto connect after 2s for demonstration
    setTimeout(() => {
      setCallState(curr => {
        if (curr === 'outbound_dialing') {
          addLog('RX', 'CALL_ACTIVE|out_call');
          addLog('UI', 'Communication établie');
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
          <div className="w-9 h-9 rounded-lg bg-cyan-600/20 border border-cyan-500/40 flex items-center justify-center text-cyan-400">
            <Radio className="w-5 h-5 animate-pulse" />
          </div>
          <div>
            <h1 className="text-lg sm:text-xl font-bold tracking-tight text-white flex items-center gap-2">
              BlackBerry SmartBridge <span className="text-xs px-2 py-0.5 rounded bg-cyan-500/20 text-cyan-400 border border-cyan-500/30">v1.3.0 Curve Double SIM</span>
            </h1>
            <p className="text-xs text-gray-400">
              Module Téléphonie RFCOMM / SPP – Gestion Double SIM, Appels sortants, Popups modales et Routage audio
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 text-xs">
          <span className="px-2.5 py-1 bg-green-500/10 text-green-400 border border-green-500/30 rounded-md font-mono flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-green-400 animate-ping"></span>
            SPP: CONNECTÉ
          </span>
          <span className="px-2.5 py-1 bg-blue-500/10 text-blue-400 border border-blue-500/30 rounded-md font-mono">
            BlackBerry Curve 9300 (OS 5.0)
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
              <div className="w-16 h-1 bg-gray-700 rounded-full"></div>
              <div className={`w-2.5 h-2.5 rounded-full ${callState === 'incoming' ? 'bg-red-500 animate-ping' : 'bg-green-500'}`}></div>
            </div>

            {/* CURVE SCREEN (320x240 Aspect Ratio) */}
            <div className="w-[320px] h-[240px] bg-black border-2 border-gray-800 rounded-md overflow-hidden relative font-mono text-xs select-none flex flex-col">
              
              {/* STATUS BAR */}
              <div className="h-6 bg-[#111] border-b border-gray-800 flex items-center justify-between px-2 text-[10px] text-gray-300">
                <span className="text-green-400 font-bold">[BT: ON]</span>
                <span className="text-cyan-400 font-semibold">[4G] inwi - Signal: [||||]</span>
                <span className="text-green-400">[BB: 88%]</span>
              </div>

              {/* SCREEN CONTENT BY CALL STATE */}
              {callState === 'idle' && (
                <div className="flex-1 flex flex-col justify-between p-2">
                  <div className="text-center pt-1">
                    <div className="text-2xl font-bold tracking-wider text-white">14:24</div>
                    <div className="text-[10px] text-gray-400">Jeudi 24 Septembre 2026</div>
                    <div className="text-[11px] text-cyan-400 mt-1">
                      Double SIM Active : [{sims.map(s => s.name).join(' | ')}]
                    </div>
                  </div>

                  {/* 3x3 Grid Buttons Preview */}
                  <div className="grid grid-cols-3 gap-1 px-1">
                    <button onClick={startOutboundDial} className="bg-[#222] hover:bg-[#333] border border-cyan-800 text-cyan-300 py-1 rounded text-[10px] font-bold text-center cursor-pointer">
                      📞 Calls
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
                    <button className="bg-[#222] border border-gray-700 text-gray-300 py-1 rounded text-[10px] text-center">
                      👤 VIP (10)
                    </button>
                    <button className="bg-[#222] border border-gray-700 text-gray-300 py-1 rounded text-[10px] text-center">
                      ⚙️ Réglages
                    </button>
                  </div>

                  <div className="text-[9px] text-gray-500 text-center">
                    Appuyez sur 'Calls' pour composer avec choix de SIM
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
                      Sonnerie en cours...
                    </div>
                  </div>

                  <div>
                    <div className="flex justify-center gap-2 mb-1">
                      <button 
                        onClick={handleAnswer} 
                        className="px-4 py-1.5 bg-green-700 hover:bg-green-600 text-white font-bold rounded-lg text-xs cursor-pointer border border-green-400"
                      >
                        Décrocher
                      </button>
                      <button 
                        onClick={handleReject} 
                        className="px-4 py-1.5 bg-red-700 hover:bg-red-600 text-white font-bold rounded-lg text-xs cursor-pointer border border-red-400"
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
              {(callState === 'outbound_dialing' || callState === 'active') && (
                <div className="flex-1 bg-black flex flex-col justify-between p-2 text-center">
                  <div>
                    <div className={`text-xs font-bold uppercase tracking-wider ${callState === 'active' ? 'text-green-400' : 'text-cyan-400'}`}>
                      {callState === 'active' ? 'COMMUNICATION ACTIVE' : 'APPEL SORTANT'}
                    </div>
                    <div className="text-xs font-bold text-yellow-400">
                      sur [{currentCaller.simName}]
                    </div>
                    <div className="w-full h-[1px] bg-gray-800 my-1"></div>
                  </div>

                  <div>
                    <div className="text-base font-bold text-white leading-tight">
                      {currentCaller.name}
                    </div>
                    <div className="text-xs text-gray-400">
                      {currentCaller.number}
                    </div>
                    <div className={`text-xs font-semibold mt-1 ${callState === 'active' ? 'text-green-400' : 'text-cyan-300'}`}>
                      {callState === 'active' ? '00:14 (En communication)' : 'Numérotation en cours...'}
                    </div>
                    <div className={`text-[10px] mt-1 ${speakerOn ? 'text-green-400 font-bold' : 'text-gray-500'}`}>
                      {speakerOn ? '🔊 [Haut-parleur : ACTIVÉ]' : '🔈 [Haut-parleur : ÉTEINT]'}
                    </div>
                  </div>

                  <div>
                    <div className="flex justify-center gap-2 mb-1">
                      <button 
                        onClick={handleHangup} 
                        className="px-3 py-1.5 bg-red-700 hover:bg-red-600 text-white font-bold rounded text-xs cursor-pointer border border-red-400"
                      >
                        Raccrocher
                      </button>
                      <button 
                        onClick={handleToggleSpeaker} 
                        className={`px-3 py-1.5 font-bold rounded text-xs cursor-pointer border ${speakerOn ? 'bg-cyan-800 border-cyan-400 text-white' : 'bg-gray-800 border-gray-600 text-gray-200'}`}
                      >
                        {speakerOn ? 'HP: ON' : 'Haut-Parleur'}
                      </button>
                    </div>
                    <div className="text-[8px] text-gray-400">
                      Menu BlackBerry: Option 'Haut-parleur ON/OFF'
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

            </div>

            {/* CURVE TRACKPAD & HARDWARE BUTTONS */}
            <div className="w-[320px] flex items-center justify-between px-4 mt-3">
              <button 
                onClick={() => {
                  if (callState === 'incoming') handleAnswer();
                  else if (callState === 'idle') startOutboundDial();
                }} 
                className="w-10 h-7 bg-green-700 hover:bg-green-600 rounded-lg text-white text-[11px] font-bold shadow flex items-center justify-center cursor-pointer"
                title="Touche Verte (Send)"
              >
                📞
              </button>
              <button className="w-8 h-7 bg-gray-700 rounded-lg text-gray-300 text-[10px] font-bold">
                MENU
              </button>
              {/* Optical Trackpad */}
              <div className="w-10 h-10 rounded-full bg-black border-2 border-gray-600 shadow-inner flex items-center justify-center">
                <div className="w-3.5 h-3.5 rounded-full bg-gray-500"></div>
              </div>
              <button className="w-8 h-7 bg-gray-700 rounded-lg text-gray-300 text-[10px] font-bold">
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
          
          {/* Card 1: Double SIM & Outbound Controls */}
          <div className="bg-[#161a22] border border-gray-800 rounded-xl p-4 shadow-sm">
            <h2 className="text-sm font-bold text-white flex items-center gap-2 mb-3">
              <SimIcon className="w-4 h-4 text-cyan-400" />
              1. Configuration Double SIM & Appels Sortants
            </h2>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-3 text-xs">
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
                <button
                  onClick={() => {
                    addLog('TX', 'GET_SIMS');
                    addLog('RX', 'SIM_LIST|2|inwi|0|Orange|1');
                  }}
                  className="mt-2 text-[10px] text-cyan-400 hover:underline cursor-pointer"
                >
                  Renvoyer trame SIM_LIST|2|inwi|0|Orange|1
                </button>
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
                  className="w-full mt-2 py-1.5 bg-cyan-700 hover:bg-cyan-600 text-white rounded font-bold text-xs flex items-center justify-center gap-1.5 cursor-pointer"
                >
                  <Phone className="w-3.5 h-3.5" />
                  Appeler (Déclenche sélecteur SIM)
                </button>
              </div>
            </div>
          </div>

          {/* Card 2: Incoming Call & Speaker Toggle Bench */}
          <div className="bg-[#161a22] border border-gray-800 rounded-xl p-4 shadow-sm">
            <h2 className="text-sm font-bold text-white flex items-center gap-2 mb-3">
              <PhoneIncoming className="w-4 h-4 text-green-400" />
              2. Simulation Événements Android vers BlackBerry
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
          </div>

          {/* Card 3: Live Protocol Log Trace */}
          <div className="bg-[#161a22] border border-gray-800 rounded-xl p-3 shadow-sm flex flex-col flex-1">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-bold text-gray-300 flex items-center gap-1.5">
                <Cpu className="w-3.5 h-3.5 text-cyan-400" />
                Trace Protocole Bluetooth SPP (Trame BSB)
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

      {/* Compiled Deliverables Footer */}
      <footer className="w-full max-w-6xl mt-6 p-4 bg-[#161a22] border border-gray-800 rounded-xl flex flex-wrap items-center justify-between gap-3 text-xs">
        <div className="flex items-center gap-3">
          <CheckCircle2 className="w-5 h-5 text-green-400" />
          <div>
            <div className="font-bold text-white">Binaires BlackBerry OS 5.0 générés avec succès</div>
            <div className="text-[11px] text-gray-400">
              Compilé avec BlackBerry RAPC & Preverified CLDC 1.1 pour Curve 9300
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <a
            href="/blackberry/BBSmartBridge.cod"
            download="BBSmartBridge.cod"
            className="px-3 py-1.5 bg-gray-800 hover:bg-gray-700 text-gray-200 rounded border border-gray-700 font-mono flex items-center gap-1.5"
          >
            <Download className="w-3.5 h-3.5 text-cyan-400" />
            BBSmartBridge.cod (73 Ko)
          </a>
          <a
            href="/blackberry/BBSmartBridge.jad"
            download="BBSmartBridge.jad"
            className="px-3 py-1.5 bg-gray-800 hover:bg-gray-700 text-gray-200 rounded border border-gray-700 font-mono flex items-center gap-1.5"
          >
            <Download className="w-3.5 h-3.5 text-yellow-400" />
            BBSmartBridge.jad
          </a>
          <a
            href="/blackberry/BBSmartBridge.alx"
            download="BBSmartBridge.alx"
            className="px-3 py-1.5 bg-gray-800 hover:bg-gray-700 text-gray-200 rounded border border-gray-700 font-mono flex items-center gap-1.5"
          >
            <Download className="w-3.5 h-3.5 text-green-400" />
            BBSmartBridge.alx
          </a>
        </div>
      </footer>
    </div>
  );
}
