import React, { useState } from 'react';
import { MessageSquare, PhoneIncoming, Send, CheckCheck, User, Plus, X, Phone, PhoneOff, Smartphone } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

export interface WhatsAppMessageItem {
  notifId: string;
  senderName: string;
  body: string;
  timestamp: string;
  isOutgoing: boolean;
  isConfirmed?: boolean;
}

interface WhatsAppModalProps {
  isOpen: boolean;
  onClose: () => void;
  messages: WhatsAppMessageItem[];
  onSendMessage: (notifId: string, text: string) => void;
  onStartNewChat: (phone: string, text: string) => void;
  onSimulateIncomingCall: (callerName: string) => void;
  onSimulateIncomingMessage: (sender: string, text: string) => void;
  onConfirmReply: (notifId: string) => void;
}

export function WhatsAppModal({
  isOpen,
  onClose,
  messages,
  onSendMessage,
  onStartNewChat,
  onSimulateIncomingCall,
  onSimulateIncomingMessage,
  onConfirmReply
}: WhatsAppModalProps) {
  const [inputText, setInputText] = useState('');
  const [simSender, setSimSender] = useState('Karim Bennani');
  const [simText, setSimText] = useState('Bonjour ! Est-ce que le Curve 9300 reçoit bien ce message ?');
  const [newChatPhone, setNewChatPhone] = useState('+212612345678');
  const [newChatMsg, setNewChatMsg] = useState('Bonjour via WhatsApp BlackBerry');
  const [activeTab, setActiveTab] = useState<'chat' | 'simulate' | 'newchat'>('chat');

  if (!isOpen) return null;

  const handleSend = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!inputText.trim()) return;
    onSendMessage('wa_active', inputText.trim());
    setInputText('');
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div 
        className="w-full max-w-2xl bg-[#0B141A] border border-[#232F3E] rounded-3xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]"
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div className="bg-[#128C7E] px-4 py-3 flex items-center justify-between text-white shadow-md">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-white/20 flex items-center justify-center border border-white/30">
              <MessageSquare className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="font-bold text-sm sm:text-base leading-tight">Module WhatsApp Curve 9300</h3>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-mono bg-emerald-900/60 text-emerald-200 border border-emerald-400/40">
                  RIM OS 6.0 SPP
                </span>
              </div>
              <p className="text-xs text-emerald-100 flex items-center gap-1">
                <span>Passerelle Android RFCOMM &amp; Audio 16 kHz</span>
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-full hover:bg-black/20 text-white/80 hover:text-white transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Sub-Tabs */}
        <div className="flex border-b border-[#202C33] bg-[#111B21] px-4 pt-2 gap-2">
          <button
            onClick={() => setActiveTab('chat')}
            className={`px-3 py-2 text-xs font-semibold rounded-t-xl transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'chat'
                ? 'bg-[#0B141A] text-emerald-400 border-t-2 border-emerald-400 font-bold'
                : 'text-gray-400 hover:text-gray-200'
            }`}
          >
            <MessageSquare className="w-3.5 h-3.5" />
            Discussion en direct ({messages.length})
          </button>
          <button
            onClick={() => setActiveTab('simulate')}
            className={`px-3 py-2 text-xs font-semibold rounded-t-xl transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'simulate'
                ? 'bg-[#0B141A] text-emerald-400 border-t-2 border-emerald-400 font-bold'
                : 'text-gray-400 hover:text-gray-200'
            }`}
          >
            <Smartphone className="w-3.5 h-3.5" />
            Simulateur Android (Appels &amp; Messages)
          </button>
          <button
            onClick={() => setActiveTab('newchat')}
            className={`px-3 py-2 text-xs font-semibold rounded-t-xl transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'newchat'
                ? 'bg-[#0B141A] text-emerald-400 border-t-2 border-emerald-400 font-bold'
                : 'text-gray-400 hover:text-gray-200'
            }`}
          >
            <Plus className="w-3.5 h-3.5" />
            Nouveau Chat
          </button>
        </div>

        {/* Tab 1: Live Chat View */}
        {activeTab === 'chat' && (
          <div className="flex-1 flex flex-col min-h-0 bg-[#0B141A]">
            {/* Messages Scroll Area */}
            <div className="flex-1 p-4 overflow-y-auto space-y-3 min-h-[260px] max-h-[380px]">
              {messages.length === 0 ? (
                <div className="text-center py-12 text-gray-500 text-xs">
                  Aucun message WhatsApp pour le moment.
                </div>
              ) : (
                messages.map((m, idx) => (
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
                              <span className="text-[9px]">Transmis Android</span>
                            </>
                          ) : (
                            <button
                              onClick={() => onConfirmReply(m.notifId)}
                              className="text-[9px] hover:underline text-cyan-200 cursor-pointer"
                            >
                              Confirmer (WHATSAPP_REPLY_OK)
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Quick Reply Bar */}
            <form onSubmit={handleSend} className="p-3 bg-[#1F2C34] border-t border-[#2A3942] flex items-center gap-2">
              <input
                type="text"
                value={inputText}
                onChange={e => setInputText(e.target.value)}
                placeholder="Répondre via le clavier physique BlackBerry Curve 9300..."
                className="flex-1 bg-[#2A3942] text-white text-xs sm:text-sm px-4 py-2.5 rounded-full border border-gray-700 focus:outline-none focus:border-emerald-500"
              />
              <button
                type="submit"
                className="px-4 py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-full font-bold text-xs flex items-center gap-1.5 transition-colors cursor-pointer shadow-lg shadow-emerald-700/20"
              >
                <Send className="w-3.5 h-3.5" />
                <span className="hidden sm:inline">Envoyer</span>
              </button>
            </form>
          </div>
        )}

        {/* Tab 2: Android Simulator (Triggers incoming packets) */}
        {activeTab === 'simulate' && (
          <div className="p-4 sm:p-6 space-y-5 bg-[#0B141A] overflow-y-auto">
            {/* 1. Simulate Incoming WhatsApp Call */}
            <div className="p-4 rounded-2xl bg-[#111B21] border border-[#202C33] space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-white font-bold text-sm">
                  <PhoneIncoming className="w-4 h-4 text-emerald-400" />
                  <span>1. Simuler un Appel WhatsApp Entrant</span>
                </div>
                <span className="text-[10px] font-mono text-emerald-400 bg-emerald-950/60 px-2 py-0.5 rounded border border-emerald-500/30">
                  WHATSAPP_CALL_INCOMING
                </span>
              </div>
              <p className="text-xs text-gray-400">
                Envoie le paquet Bluetooth à BlackBerry. Le Curve 9300 déclenche la vibration continue, fait clignoter la LED verte et affiche <code className="text-emerald-300">WhatsAppIncomingCallScreen</code> en plein écran.
              </p>
              <div className="flex flex-wrap items-center gap-2">
                <button
                  onClick={() => {
                    onSimulateIncomingCall('Karim Bennani');
                    onClose();
                  }}
                  className="px-3 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl flex items-center gap-1.5 transition-colors cursor-pointer shadow-md shadow-emerald-600/20"
                >
                  <PhoneIncoming className="w-3.5 h-3.5" />
                  Appel de Karim Bennani
                </button>
                <button
                  onClick={() => {
                    onSimulateIncomingCall('Dr. Karim Lahlou');
                    onClose();
                  }}
                  className="px-3 py-2 bg-[#202C33] hover:bg-[#2A3942] text-emerald-300 text-xs font-semibold rounded-xl flex items-center gap-1.5 transition-colors cursor-pointer border border-emerald-500/30"
                >
                  <PhoneIncoming className="w-3.5 h-3.5" />
                  Appel du Dr. Lahlou
                </button>
                <button
                  onClick={() => {
                    onSimulateIncomingCall('Amina Mansouri');
                    onClose();
                  }}
                  className="px-3 py-2 bg-[#202C33] hover:bg-[#2A3942] text-cyan-300 text-xs font-semibold rounded-xl flex items-center gap-1.5 transition-colors cursor-pointer border border-cyan-500/30"
                >
                  <PhoneIncoming className="w-3.5 h-3.5" />
                  Appel d'Amina
                </button>
              </div>
            </div>

            {/* 2. Simulate Incoming WhatsApp Message */}
            <div className="p-4 rounded-2xl bg-[#111B21] border border-[#202C33] space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-white font-bold text-sm">
                  <MessageSquare className="w-4 h-4 text-emerald-400" />
                  <span>2. Simuler un Message WhatsApp Entrant</span>
                </div>
                <span className="text-[10px] font-mono text-emerald-400 bg-emerald-950/60 px-2 py-0.5 rounded border border-emerald-500/30">
                  WHATSAPP_MSG
                </span>
              </div>
              <p className="text-xs text-gray-400">
                Encode le corps du texte en Base64 UTF-8. Le Curve 9300 émet un bip doux, vibre brièvement, allume la LED verte et met à jour <code className="text-emerald-300">WhatsAppChatScreen</code>.
              </p>
              <div className="space-y-2">
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                  <input
                    type="text"
                    value={simSender}
                    onChange={e => setSimSender(e.target.value)}
                    placeholder="Nom contact (ex: Karim Bennani)"
                    className="bg-[#202C33] text-white text-xs px-3 py-2 rounded-xl border border-gray-700"
                  />
                  <input
                    type="text"
                    value={simText}
                    onChange={e => setSimText(e.target.value)}
                    placeholder="Texte du message WhatsApp..."
                    className="sm:col-span-2 bg-[#202C33] text-white text-xs px-3 py-2 rounded-xl border border-gray-700"
                  />
                </div>
                <div className="flex justify-end">
                  <button
                    onClick={() => {
                      if (!simSender.trim() || !simText.trim()) return;
                      onSimulateIncomingMessage(simSender.trim(), simText.trim());
                      setActiveTab('chat');
                    }}
                    className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl flex items-center gap-1.5 transition-colors cursor-pointer shadow-md shadow-emerald-600/20"
                  >
                    <Send className="w-3.5 h-3.5" />
                    Transférer Message vers BlackBerry (Base64)
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab 3: Start New WhatsApp Chat */}
        {activeTab === 'newchat' && (
          <div className="p-4 sm:p-6 space-y-4 bg-[#0B141A]">
            <div className="p-4 rounded-2xl bg-[#111B21] border border-[#202C33] space-y-3">
              <div className="flex items-center justify-between">
                <h4 className="text-white font-bold text-sm">Démarrer un nouveau chat WhatsApp</h4>
                <span className="text-[10px] font-mono text-cyan-400 bg-cyan-950/60 px-2 py-0.5 rounded border border-cyan-500/30">
                  WHATSAPP_START_CHAT
                </span>
              </div>
              <p className="text-xs text-gray-400">
                Commande émise depuis le BlackBerry Curve 9300 vers l'application Android pour ouvrir la conversation et envoyer le texte encodé en Base64.
              </p>
              <div className="space-y-3">
                <div>
                  <label className="text-xs text-gray-300 block mb-1 font-medium">Numéro de téléphone :</label>
                  <input
                    type="text"
                    value={newChatPhone}
                    onChange={e => setNewChatPhone(e.target.value)}
                    className="w-full bg-[#202C33] text-white text-xs px-3 py-2 rounded-xl border border-gray-700"
                  />
                </div>
                <div>
                  <label className="text-xs text-gray-300 block mb-1 font-medium">Message initial :</label>
                  <textarea
                    rows={3}
                    value={newChatMsg}
                    onChange={e => setNewChatMsg(e.target.value)}
                    className="w-full bg-[#202C33] text-white text-xs px-3 py-2 rounded-xl border border-gray-700"
                  />
                </div>
                <div className="flex justify-end gap-2">
                  <button
                    onClick={() => {
                      if (!newChatPhone.trim()) return;
                      onStartNewChat(newChatPhone.trim(), newChatMsg.trim());
                      setActiveTab('chat');
                    }}
                    className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold rounded-xl flex items-center gap-1.5 transition-colors cursor-pointer"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    Lancer le Chat WhatsApp
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Footer info bar */}
        <div className="px-4 py-2.5 bg-[#111B21] border-t border-[#202C33] flex flex-wrap items-center justify-between text-[11px] text-gray-400 gap-2">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>Format paquets : <code className="text-emerald-300">WHATSAPP_CALL_*</code> | <code className="text-emerald-300">WHATSAPP_MSG</code> | <code className="text-emerald-300">WHATSAPP_REPLY</code></span>
          </div>
          <span className="font-mono text-gray-500 text-[10px]">
            Curve 9300 (320x240) • FullScreen Call &amp; MainScreen Chat
          </span>
        </div>
      </div>
    </div>
  );
}
