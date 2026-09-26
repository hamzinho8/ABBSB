import React, { useState, useMemo } from 'react';
import { 
  PhoneIncoming, 
  PhoneOutgoing, 
  Clock, 
  Search, 
  Phone, 
  X, 
  History, 
  CreditCard as SimIcon,
  Filter,
  CheckCircle,
  PhoneCall
} from 'lucide-react';

export interface CallRecord {
  id: string;
  callerName: string;
  phoneNumber: string;
  timestamp: string;
  direction: 'incoming' | 'outgoing';
  duration?: string;
  simName?: string;
  status?: 'answered' | 'missed' | 'rejected';
}

interface CallHistoryModalProps {
  isOpen: boolean;
  onClose: () => void;
  callRecords: CallRecord[];
  onSelectNumberToCall?: (number: string, name: string) => void;
  onClearHistory?: () => void;
}

export const CallHistoryModal: React.FC<CallHistoryModalProps> = ({
  isOpen,
  onClose,
  callRecords,
  onSelectNumberToCall,
  onClearHistory
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterDirection, setFilterDirection] = useState<'all' | 'incoming' | 'outgoing'>('all');

  // Filter and search
  const filteredRecords = useMemo(() => {
    return callRecords.filter(record => {
      // Direction filter
      if (filterDirection === 'incoming' && record.direction !== 'incoming') return false;
      if (filterDirection === 'outgoing' && record.direction !== 'outgoing') return false;

      // Query filter
      if (searchQuery.trim() !== '') {
        const query = searchQuery.toLowerCase();
        const matchesName = record.callerName.toLowerCase().includes(query);
        const matchesNumber = record.phoneNumber.toLowerCase().includes(query);
        const matchesSim = record.simName?.toLowerCase().includes(query);
        return matchesName || matchesNumber || matchesSim;
      }

      return true;
    });
  }, [callRecords, filterDirection, searchQuery]);

  if (!isOpen) return null;

  return (
    <div 
      className="fixed inset-0 bg-black/80 backdrop-blur-xs flex items-center justify-center p-3 sm:p-4 z-50 animate-in fade-in duration-150"
      onClick={onClose}
    >
      <div 
        className="w-full max-w-2xl bg-[#161a22] border border-cyan-500/40 rounded-2xl shadow-2xl flex flex-col max-h-[85vh] overflow-hidden"
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-gray-800 bg-[#0d1117]/80">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-cyan-600/20 border border-cyan-500/40 flex items-center justify-center text-cyan-400">
              <History className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-white tracking-tight">
                  Historique des Appels Récents
                </h2>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-cyan-500/15 text-cyan-400 border border-cyan-500/30">
                  {callRecords.length} appels
                </span>
              </div>
              <p className="text-xs text-gray-400">
                Journal synchronisé Bluetooth HFP (BlackBerry Curve 9300 &amp; Smartphone Android)
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg bg-gray-800/80 hover:bg-gray-700 text-gray-400 hover:text-white flex items-center justify-center transition-colors cursor-pointer"
            title="Fermer (Échap)"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Search & Filter Toolbar */}
        <div className="px-5 py-3 border-b border-gray-800 bg-[#12161f] flex flex-col sm:flex-row items-center gap-3 justify-between">
          {/* Search box */}
          <div className="relative w-full sm:w-64">
            <Search className="w-3.5 h-3.5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Rechercher par nom ou numéro..."
              className="w-full bg-[#0d1117] border border-gray-700/80 rounded-lg pl-9 pr-3 py-1.5 text-xs text-white placeholder-gray-500 focus:outline-none focus:border-cyan-500/80 transition-colors"
            />
            {searchQuery && (
              <button 
                onClick={() => setSearchQuery('')}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-white text-xs"
              >
                ✕
              </button>
            )}
          </div>

          {/* Direction filters */}
          <div className="flex items-center gap-1.5 w-full sm:w-auto justify-end">
            <div className="inline-flex p-0.5 rounded-lg bg-[#0d1117] border border-gray-800">
              <button
                onClick={() => setFilterDirection('all')}
                className={`px-2.5 py-1 text-xs rounded-md font-medium transition-colors cursor-pointer ${
                  filterDirection === 'all' 
                    ? 'bg-cyan-600/30 text-cyan-300 font-semibold' 
                    : 'text-gray-400 hover:text-gray-200'
                }`}
              >
                Tous ({callRecords.length})
              </button>
              <button
                onClick={() => setFilterDirection('incoming')}
                className={`px-2.5 py-1 text-xs rounded-md font-medium flex items-center gap-1 transition-colors cursor-pointer ${
                  filterDirection === 'incoming' 
                    ? 'bg-green-600/30 text-green-300 font-semibold' 
                    : 'text-gray-400 hover:text-gray-200'
                }`}
              >
                <PhoneIncoming className="w-3 h-3 text-green-400" />
                <span>Entrants</span>
              </button>
              <button
                onClick={() => setFilterDirection('outgoing')}
                className={`px-2.5 py-1 text-xs rounded-md font-medium flex items-center gap-1 transition-colors cursor-pointer ${
                  filterDirection === 'outgoing' 
                    ? 'bg-cyan-600/30 text-cyan-300 font-semibold' 
                    : 'text-gray-400 hover:text-gray-200'
                }`}
              >
                <PhoneOutgoing className="w-3 h-3 text-cyan-400" />
                <span>Sortants</span>
              </button>
            </div>
          </div>
        </div>

        {/* Scrollable Call List */}
        <div className="flex-1 overflow-y-auto p-4 space-y-2.5 divide-y divide-transparent">
          {filteredRecords.length === 0 ? (
            <div className="text-center py-12 px-4">
              <div className="w-12 h-12 rounded-full bg-gray-800/60 mx-auto flex items-center justify-center text-gray-500 mb-3">
                <History className="w-6 h-6" />
              </div>
              <h3 className="text-sm font-semibold text-gray-300">Aucun appel trouvé</h3>
              <p className="text-xs text-gray-500 mt-1 max-w-sm mx-auto">
                {searchQuery 
                  ? `Aucun résultat pour la recherche "${searchQuery}". Essayez un autre terme.`
                  : "Aucun appel enregistré pour ce filtre."}
              </p>
            </div>
          ) : (
            filteredRecords.map((record) => {
              const isIncoming = record.direction === 'incoming';

              return (
                <div
                  key={record.id}
                  className="bg-[#0d1117] hover:bg-[#12161f] border border-gray-800/80 hover:border-gray-700/80 rounded-xl p-3.5 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3 group"
                >
                  {/* Left: Direction Icon + Caller Info */}
                  <div className="flex items-center gap-3">
                    <div 
                      className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 border ${
                        isIncoming 
                          ? 'bg-green-500/10 border-green-500/30 text-green-400' 
                          : 'bg-cyan-500/10 border-cyan-500/30 text-cyan-400'
                      }`}
                      title={isIncoming ? 'Appel entrant' : 'Appel sortant'}
                    >
                      {isIncoming ? (
                        <PhoneIncoming className="w-5 h-5" />
                      ) : (
                        <PhoneOutgoing className="w-5 h-5" />
                      )}
                    </div>

                    <div className="flex flex-col min-w-0">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="text-sm font-semibold text-white truncate group-hover:text-cyan-200 transition-colors">
                          {record.callerName}
                        </span>
                        
                        {/* Call Direction Badge */}
                        <span 
                          className={`text-[10px] px-2 py-0.5 rounded-full font-medium flex items-center gap-1 ${
                            isIncoming 
                              ? 'bg-green-500/15 text-green-400 border border-green-500/30' 
                              : 'bg-cyan-500/15 text-cyan-400 border border-cyan-500/30'
                          }`}
                        >
                          {isIncoming ? 'Entrant' : 'Sortant'}
                        </span>

                        {/* SIM Badge if present */}
                        {record.simName && (
                          <span className="text-[10px] px-1.5 py-0.5 rounded bg-gray-800 text-yellow-400 font-mono border border-gray-700/60 flex items-center gap-1">
                            <SimIcon className="w-2.5 h-2.5" />
                            {record.simName}
                          </span>
                        )}
                      </div>

                      <div className="flex items-center gap-3 text-xs text-gray-400 mt-0.5 flex-wrap">
                        <span className="font-mono text-gray-300">{record.phoneNumber}</span>
                        {record.duration && (
                          <span className="text-[11px] text-gray-500 flex items-center gap-1">
                            <span>•</span>
                            <span>{record.duration}</span>
                          </span>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Right: Timestamp & Action */}
                  <div className="flex items-center justify-between sm:justify-end gap-3 pt-2 sm:pt-0 border-t sm:border-t-0 border-gray-800/60">
                    <div className="text-right">
                      <div className="flex items-center gap-1 text-xs text-gray-300 font-medium justify-end">
                        <Clock className="w-3 h-3 text-gray-400" />
                        <span>{record.timestamp}</span>
                      </div>
                      <span className="text-[10px] text-gray-500">
                        {isIncoming ? 'Reçu sur Curve 9300' : 'Émis depuis Curve 9300'}
                      </span>
                    </div>

                    {onSelectNumberToCall && (
                      <button
                        onClick={() => onSelectNumberToCall(record.phoneNumber, record.callerName)}
                        className="px-3 py-1.5 bg-cyan-700/80 hover:bg-cyan-600 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer shadow-sm ml-2 shrink-0"
                        title={`Rappeler ${record.callerName} (${record.phoneNumber})`}
                      >
                        <PhoneCall className="w-3.5 h-3.5" />
                        <span className="hidden sm:inline">Rappeler</span>
                      </button>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Footer */}
        <div className="px-5 py-3 border-t border-gray-800 bg-[#0d1117]/90 flex items-center justify-between text-xs text-gray-400">
          <div className="flex items-center gap-2">
            <span className="w-2 h-2 rounded-full bg-cyan-400 animate-pulse"></span>
            <span>Synchronisation automatique active via protocole BSB</span>
          </div>

          <div className="flex items-center gap-2">
            {onClearHistory && callRecords.length > 0 && (
              <button
                onClick={onClearHistory}
                className="text-[11px] text-gray-500 hover:text-red-400 transition-colors cursor-pointer mr-2"
              >
                Effacer historique
              </button>
            )}
            <button
              onClick={onClose}
              className="px-3.5 py-1.5 bg-gray-800 hover:bg-gray-700 text-white rounded-lg text-xs font-medium transition-colors cursor-pointer"
            >
              Fermer
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
