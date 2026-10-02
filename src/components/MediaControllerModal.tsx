import React, { useState } from 'react';
import { Play, Pause, SkipBack, SkipForward, Volume2, VolumeX, Music, Disc3, X } from 'lucide-react';

interface MediaControllerModalProps {
  isOpen: boolean;
  onClose: () => void;
  onMediaAction?: (action: 'PLAY' | 'PAUSE' | 'NEXT' | 'PREVIOUS', volume?: number) => void;
}

export const MediaControllerModal: React.FC<MediaControllerModalProps> = ({
  isOpen,
  onClose,
  onMediaAction
}) => {
  const [isPlaying, setIsPlaying] = useState(true);
  const [title, setTitle] = useState('Flux Audio HD');
  const [artist, setArtist] = useState('Smartphone Android');
  const [progress, setProgress] = useState(42);
  const [volume, setVolume] = useState(85);
  const [muted, setMuted] = useState(false);

  if (!isOpen) return null;

  const togglePlay = () => {
    const next = !isPlaying;
    setIsPlaying(next);
    if (onMediaAction) onMediaAction(next ? 'PLAY' : 'PAUSE');
  };

  const handleNext = () => {
    setTitle('Audio Multimédia HD');
    setArtist('Application Android');
    setProgress(0);
    if (onMediaAction) onMediaAction('NEXT');
  };

  const handlePrev = () => {
    setTitle('Appel Vocal & Musique');
    setArtist('Audio Bluetooth');
    setProgress(0);
    if (onMediaAction) onMediaAction('PREVIOUS');
  };

  return (
    <div className="fixed inset-0 bg-black/80 backdrop-blur-xs flex items-center justify-center p-3 sm:p-4 z-50 animate-in fade-in duration-150">
      <div className="w-full max-w-md bg-[#161a22] border border-cyan-500/50 rounded-2xl shadow-2xl overflow-hidden flex flex-col">
        {/* Header */}
        <div className="p-4 border-b border-gray-800 bg-[#0d1117] flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-purple-500/10 border border-purple-500/30 flex items-center justify-center text-purple-400">
              <Music className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-sm font-bold text-white">
                Contrôle Média Smartphone
              </h2>
              <p className="text-[11px] text-gray-400">
                Synchronisé Spotify / YouTube / Android Audio
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

        {/* Media Player Card */}
        <div className="p-5 flex flex-col items-center text-center">
          {/* Rotating Vinyl Disc / Album art */}
          <div className="relative mb-4">
            <div className={`w-28 h-28 rounded-full bg-gradient-to-tr from-gray-900 via-[#1c1936] to-purple-900 border-4 border-gray-800 shadow-xl flex items-center justify-center ${isPlaying ? 'animate-[spin_6s_linear_infinite]' : ''}`}>
              <div className="w-10 h-10 rounded-full bg-[#11161B] border-2 border-purple-400/50 flex items-center justify-center">
                <Disc3 className="w-6 h-6 text-purple-400" />
              </div>
            </div>
            {/* Status badge */}
            <div className={`absolute -bottom-2 left-1/2 -translate-x-1/2 px-2.5 py-0.5 rounded-full text-[10px] font-bold border ${
              isPlaying 
                ? 'bg-green-500/20 text-green-300 border-green-500/40' 
                : 'bg-yellow-500/20 text-yellow-300 border-yellow-500/40'
            }`}>
              {isPlaying ? 'Lecture active' : 'En pause'}
            </div>
          </div>

          {/* Title & Artist */}
          <h3 className="text-base font-bold text-white mt-1 truncate max-w-xs">
            {title}
          </h3>
          <p className="text-xs text-cyan-400 font-medium mt-0.5">
            {artist}
          </p>
          <span className="text-[10px] text-gray-500 mt-1 font-mono">
            Routage : Flux audio numérique 500 ms vers Curve 9300
          </span>

          {/* Progress Bar */}
          <div className="w-full mt-4 space-y-1">
            <div className="w-full h-1.5 bg-gray-800 rounded-full overflow-hidden">
              <div 
                className="h-full bg-gradient-to-r from-cyan-500 to-purple-500 rounded-full transition-all duration-300"
                style={{ width: `${progress}%` }}
              ></div>
            </div>
            <div className="flex justify-between text-[10px] text-gray-500 font-mono">
              <span>01:28</span>
              <span>03:50</span>
            </div>
          </div>

          {/* Playback Controls */}
          <div className="flex items-center justify-center gap-4 mt-3">
            <button
              onClick={handlePrev}
              className="w-10 h-10 rounded-full bg-gray-800 hover:bg-gray-700 text-gray-200 flex items-center justify-center cursor-pointer transition-colors shadow"
              title="Piste précédente (Touche P)"
            >
              <SkipBack className="w-4 h-4" />
            </button>
            <button
              onClick={togglePlay}
              className="w-14 h-14 rounded-full bg-cyan-600 hover:bg-cyan-500 text-white flex items-center justify-center cursor-pointer transition-all shadow-lg active:scale-95"
              title="Lecture / Pause (Touche Espace)"
            >
              {isPlaying ? <Pause className="w-6 h-6" /> : <Play className="w-6 h-6 ml-0.5" />}
            </button>
            <button
              onClick={handleNext}
              className="w-10 h-10 rounded-full bg-gray-800 hover:bg-gray-700 text-gray-200 flex items-center justify-center cursor-pointer transition-colors shadow"
              title="Piste suivante (Touche N)"
            >
              <SkipForward className="w-4 h-4" />
            </button>
          </div>

          {/* Volume Control */}
          <div className="w-full mt-4 flex items-center gap-3 px-2 bg-[#0d1117] py-2 rounded-xl border border-gray-800">
            <button 
              onClick={() => setMuted(!muted)}
              className="text-gray-400 hover:text-white cursor-pointer"
            >
              {muted || volume === 0 ? <VolumeX className="w-4 h-4 text-red-400" /> : <Volume2 className="w-4 h-4 text-cyan-400" />}
            </button>
            <input
              type="range"
              min="0"
              max="100"
              value={muted ? 0 : volume}
              onChange={(e) => {
                setVolume(Number(e.target.value));
                setMuted(false);
              }}
              className="w-full accent-cyan-500 cursor-pointer h-1"
            />
            <span className="text-xs font-mono text-gray-400 w-8 text-right">
              {muted ? '0%' : `${volume}%`}
            </span>
          </div>
        </div>

        {/* Footer */}
        <div className="p-3 border-t border-gray-800 bg-[#0d1117] flex items-center justify-between text-xs text-gray-400">
          <span className="text-[11px] text-gray-500">
            Raccourcis BlackBerry : Espace = Play/Pause, N = Suiv, P = Prec
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
