/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export default function App() {
  return (
    <div className="min-h-screen bg-[#1a1a1a] text-white font-sans flex flex-col items-center justify-center p-4 sm:p-8 overflow-hidden">
      <div className="w-full max-w-[800px] bg-[#2a2a2a] border-[8px] sm:border-[12px] border-[#333] rounded-[24px] sm:rounded-[40px] shadow-2xl p-4 sm:p-6 flex flex-col h-[700px]">
        
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 bg-[#000] rounded-t-lg border-b border-[#444]">
          <div className="flex items-center gap-4">
            <div className="w-3 h-3 rounded-full bg-blue-500 animate-pulse"></div>
            <span className="text-[10px] sm:text-xs font-bold tracking-widest uppercase truncate">BlackBerrySmartBridge v1.0.4</span>
          </div>
          <div className="hidden sm:flex gap-6 text-[10px] text-gray-400">
            <span>BT: RFCOMM/SPP</span>
            <span>SDK: 7.1.0</span>
          </div>
        </div>

        {/* Body */}
        <div className="flex flex-col sm:flex-row flex-1 overflow-hidden">
          {/* Sidebar */}
          <div className="w-full sm:w-1/3 bg-[#111] border-b sm:border-b-0 sm:border-r border-[#333] p-4 flex flex-col gap-4 overflow-y-auto">
            <div className="p-3 bg-[#1e1e1e] border-l-4 border-blue-600">
              <h3 className="text-[10px] uppercase text-gray-500 font-bold mb-1">Android Link</h3>
              <p className="text-sm text-blue-400 font-medium">Pixel 7 Pro ● Connected</p>
              <p className="text-[10px] text-gray-500 truncate">ID: 27b44265-43a7-4744...</p>
            </div>
            
            <div className="p-3 bg-[#1e1e1e] border-l-4 border-green-600">
              <h3 className="text-[10px] uppercase text-gray-500 font-bold mb-1">Status</h3>
              <p className="text-sm text-green-400">Battery: 78%</p>
              <p className="text-[10px] text-gray-500">Signal: Excellent (-64dBm)</p>
            </div>
            
            <div className="flex-1 overflow-hidden mt-2 hidden sm:block">
              <h3 className="text-[10px] uppercase text-gray-400 font-bold mb-2">Active Modules</h3>
              <ul className="text-[11px] space-y-2 text-gray-300">
                <li className="flex items-center gap-2"><span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>NotificationManager</li>
                <li className="flex items-center gap-2"><span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>CallRelayService</li>
                <li className="flex items-center gap-2"><span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>ContactSync</li>
                <li className="flex items-center gap-2"><span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>ProtocolHandler</li>
              </ul>
            </div>
          </div>

          {/* Main Content */}
          <div className="w-full sm:w-2/3 flex flex-col flex-1 overflow-hidden">
            <div className="p-4 bg-[#151515] flex-1 overflow-y-auto">
              <div className="flex justify-between items-end mb-4">
                <h2 className="text-xl sm:text-2xl font-light text-gray-200">Recent Activity</h2>
                <span className="text-[10px] text-gray-500 hidden sm:inline">480 x 360 Viewport Emulation</span>
              </div>
              
              <div className="space-y-3">
                <div className="bg-[#222] p-3 rounded border border-[#333] flex gap-3">
                  <div className="w-8 h-8 bg-green-700 rounded-full flex items-center justify-center text-xs shrink-0">W</div>
                  <div className="flex-1">
                    <div className="flex justify-between"><span className="text-xs font-bold text-gray-200">WhatsApp</span><span className="text-[9px] text-gray-500">14:02</span></div>
                    <p className="text-[11px] text-gray-400 mt-1">Mohamed: Salut ça va ? On se voit à 18h?</p>
                    <div className="mt-2 flex gap-2">
                      <button className="px-2 py-1 bg-[#333] hover:bg-[#444] text-[9px] rounded uppercase transition-colors cursor-pointer">Reply</button>
                      <button className="px-2 py-1 bg-[#333] hover:bg-[#444] text-[9px] rounded uppercase transition-colors cursor-pointer">Dismiss</button>
                    </div>
                  </div>
                </div>
                
                <div className="bg-[#222] p-3 rounded border border-[#333] flex gap-3">
                  <div className="w-8 h-8 bg-blue-700 rounded-full flex items-center justify-center text-xs shrink-0">P</div>
                  <div className="flex-1">
                    <div className="flex justify-between"><span className="text-xs font-bold text-gray-200">Call Incoming</span><span className="text-[9px] text-gray-500">Just Now</span></div>
                    <p className="text-[11px] text-gray-400 mt-1">Amina (06 12 34 56 78)</p>
                    <div className="mt-2 flex gap-2">
                      <button className="px-3 py-1 bg-green-700 hover:bg-green-600 text-[9px] rounded uppercase font-bold transition-colors cursor-pointer">Answer</button>
                      <button className="px-3 py-1 bg-red-700 hover:bg-red-600 text-[9px] rounded uppercase font-bold transition-colors cursor-pointer">Reject</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <div className="p-3 bg-[#000] border-t border-[#333] h-32 overflow-y-auto shrink-0">
              <h3 className="text-[9px] uppercase text-blue-500 font-bold mb-1">LogManager Trace</h3>
              <div className="font-mono text-[9px] text-gray-500 leading-relaxed">
                <p>02:14:52 [BT] RX: NOTIFICATION|152|WhatsApp|Mohamed|Salut</p>
                <p>02:14:52 [UI] Event: Notification displayed internally</p>
                <p>02:14:55 [BT] TX: PING</p>
                <p>02:14:55 [BT] RX: PONG (Latency: 22ms)</p>
                <p className="text-blue-400">02:15:01 [BT] RX: CALL_INCOMING|123|Amina|0612345678</p>
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="hidden sm:flex justify-between px-6 py-4 bg-[#222] border-t border-[#444] rounded-b-lg text-gray-400 shrink-0">
          <div className="flex gap-4 items-center">
            <span className="text-xs">MENU: Trackpad Select</span>
            <div className="w-[1px] h-3 bg-[#444]"></div>
            <span className="text-xs">QWERTY: Focused</span>
          </div>
          <div className="text-[10px] font-mono px-3 py-1 bg-[#111] rounded border border-[#333]">
            com.hamza.blackberrybridge.SmartBridgeApp
          </div>
        </div>
      </div>
    </div>
  );
}
