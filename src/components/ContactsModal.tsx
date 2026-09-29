import React, { useState } from 'react';
import { Search, Phone, Star, RefreshCw, X, User } from 'lucide-react';

export interface ContactItem {
  id: string;
  name: string;
  number: string;
  isVip: boolean;
  notes?: string;
}

interface ContactsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCallContact: (number: string, name: string) => void;
  onSyncContacts?: () => void;
}

export const ContactsModal: React.FC<ContactsModalProps> = ({
  isOpen,
  onClose,
  onCallContact,
  onSyncContacts
}) => {
  const [search, setSearch] = useState('');
  const [contacts] = useState<ContactItem[]>([
    { id: '1', name: 'Amina Mansouri', number: '+212634934134', isVip: true, notes: 'Famille / Mobile inwi' },
    { id: '2', name: 'Youssef Bennani', number: '+212655881230', isVip: true, notes: 'Bureau / Orange' },
    { id: '3', name: 'Hamza H.', number: '+212611223344', isVip: true, notes: 'Ingénieur BlackBerry Bridge' },
    { id: '4', name: 'Dr. Karim Lahlou', number: '+212672409918', isVip: true, notes: 'Clinique / Urgences' },
    { id: '5', name: 'Fatima Zahra', number: '+212698712345', isVip: true, notes: 'Mobile' },
    { id: '6', name: 'Sara Alami', number: '+212644332211', isVip: true, notes: 'Personnel' },
    { id: '7', name: 'Service Client inwi', number: '220', isVip: true, notes: 'Assistance Télécom inwi' },
    { id: '8', name: 'Service Client Orange', number: '121', isVip: true, notes: 'Assistance Télécom Orange' },
  ]);

  if (!isOpen) return null;

  const filtered = contacts.filter(c => 
    c.name.toLowerCase().includes(search.toLowerCase()) || 
    c.number.includes(search)
  );

  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-xs flex items-center justify-center p-3 sm:p-4 z-50 animate-in fade-in duration-150">
      <div className="w-full max-w-md bg-[#161a22] border border-cyan-500/50 rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="p-4 border-b border-gray-800 bg-[#0d1117] flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-cyan-500/10 border border-cyan-500/30 flex items-center justify-center text-cyan-400">
              <User className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-sm font-bold text-white flex items-center gap-1.5">
                Contacts VIP BlackBerry Curve
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-cyan-500/20 text-cyan-300 font-mono">
                  {contacts.length}
                </span>
              </h2>
              <p className="text-[11px] text-gray-400">
                Synchronisé avec le carnet du Curve 9300
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-7 h-7 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-400 hover:text-white flex items-center justify-center cursor-pointer transition-colors"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Search & Actions */}
        <div className="p-3 border-b border-gray-800 bg-[#161a22] flex items-center gap-2">
          <div className="relative flex-1">
            <Search className="w-3.5 h-3.5 absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
            <input
              type="text"
              placeholder="Rechercher par nom ou numéro..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full bg-[#0d1117] border border-gray-700/80 rounded-lg pl-8 pr-3 py-1.5 text-xs text-white placeholder-gray-500 focus:outline-none focus:border-cyan-500"
            />
          </div>
          {onSyncContacts && (
            <button
              onClick={onSyncContacts}
              className="px-2.5 py-1.5 bg-gray-800 hover:bg-gray-700 border border-gray-700 text-gray-300 hover:text-white rounded-lg text-xs font-medium flex items-center gap-1.5 shrink-0 cursor-pointer transition-colors"
              title="Synchroniser avec Android"
            >
              <RefreshCw className="w-3 h-3 text-cyan-400" />
              <span className="hidden sm:inline">Sync</span>
            </button>
          )}
        </div>

        {/* Contacts List */}
        <div className="flex-1 overflow-y-auto p-3 space-y-2">
          {filtered.length === 0 ? (
            <div className="text-center py-8 text-xs text-gray-500">
              Aucun contact trouvé pour cette recherche.
            </div>
          ) : (
            filtered.map((c) => (
              <div
                key={c.id}
                className="bg-[#0d1117] hover:bg-[#12161f] border border-gray-800/80 rounded-xl p-2.5 flex items-center justify-between gap-3 group transition-colors"
              >
                <div className="flex items-center gap-2.5 min-w-0">
                  <div className="w-8 h-8 rounded-lg bg-yellow-500/10 border border-yellow-500/30 flex items-center justify-center text-yellow-400 shrink-0">
                    <Star className="w-4 h-4 fill-yellow-400/20" />
                  </div>
                  <div className="min-w-0">
                    <div className="text-xs font-bold text-white truncate flex items-center gap-1.5">
                      <span>{c.name}</span>
                      <span className="text-[9px] px-1.5 py-0.2 rounded bg-yellow-500/20 text-yellow-300 border border-yellow-500/30 font-semibold">
                        VIP
                      </span>
                    </div>
                    <div className="text-[11px] text-gray-400 font-mono flex items-center gap-2 mt-0.5">
                      <span>{c.number}</span>
                      {c.notes && <span className="text-[10px] text-gray-500 truncate hidden sm:inline">• {c.notes}</span>}
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => onCallContact(c.number, c.name)}
                  className="px-3 py-1.5 bg-green-600/90 hover:bg-green-500 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shrink-0 cursor-pointer shadow-sm transition-transform active:scale-95"
                >
                  <Phone className="w-3 h-3" />
                  <span>Appeler</span>
                </button>
              </div>
            ))
          )}
        </div>

        {/* Footer */}
        <div className="p-3 border-t border-gray-800 bg-[#0d1117] flex items-center justify-between text-xs text-gray-400">
          <span className="text-[11px] text-gray-500">
            Raccourci physique Curve : Clic Trackpad / Touche Verte
          </span>
          <button
            onClick={onClose}
            className="px-3 py-1 bg-gray-800 hover:bg-gray-700 text-white rounded-lg text-xs font-medium cursor-pointer"
          >
            Fermer
          </button>
        </div>
      </div>
    </div>
  );
};
