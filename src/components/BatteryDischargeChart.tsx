import React, { useEffect, useRef, useState, useMemo } from 'react';
import * as d3 from 'd3';
import { BatteryMedium, Zap, TrendingDown, Clock, RefreshCw, AlertCircle } from 'lucide-react';

export interface BatteryDataPoint {
  time: Date;
  minutesAgo: number;
  level: number; // percentage 0-100
  event?: string;
}

interface BatteryDischargeChartProps {
  currentLevel?: number; // default 88
}

export const BatteryDischargeChart: React.FC<BatteryDischargeChartProps> = ({
  currentLevel = 88
}) => {
  const svgRef = useRef<SVGSVGElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  const [activeWindow, setActiveWindow] = useState<'60m' | '30m' | '15m'>('60m');
  const [hoveredPoint, setHoveredPoint] = useState<BatteryDataPoint | null>(null);

  // Generate realistic historical discharge data over the last 60 minutes
  const fullData: BatteryDataPoint[] = useMemo(() => {
    const now = new Date();
    const points: BatteryDataPoint[] = [];

    // Define discharge slope down to currentLevel (e.g., from 96% down to 88% over 60 mins)
    const startLevel = Math.min(100, currentLevel + 8);
    const intervals = [60, 55, 50, 45, 40, 35, 30, 25, 20, 15, 10, 5, 0];
    
    // Slight realistic drops during voice calls vs idle
    const levels = [
      startLevel,
      startLevel - 0.5,
      startLevel - 1.2,
      startLevel - 2.0,
      startLevel - 2.8,
      startLevel - 3.5,
      startLevel - 4.5,
      startLevel - 5.2,
      startLevel - 6.0,
      startLevel - 6.8,
      startLevel - 7.3,
      startLevel - 7.8,
      currentLevel
    ];

    const events = [
      'Veille Bluetooth',
      '',
      'Sync SDP HFP',
      '',
      'Appel entrant (4 min)',
      '',
      'Canal SCO actif',
      '',
      'Transfert données SPP',
      '',
      'Appel sortant (2 min)',
      '',
      'En veille connectée'
    ];

    for (let i = 0; i < intervals.length; i++) {
      const minAgo = intervals[i];
      const t = new Date(now.getTime() - minAgo * 60 * 1000);
      points.push({
        time: t,
        minutesAgo: minAgo,
        level: Math.round(levels[i] * 10) / 10,
        event: events[i] || undefined
      });
    }

    return points;
  }, [currentLevel]);

  // Filter based on active window
  const chartData = useMemo(() => {
    const maxMinutes = activeWindow === '60m' ? 60 : activeWindow === '30m' ? 30 : 15;
    return fullData.filter(d => d.minutesAgo <= maxMinutes);
  }, [fullData, activeWindow]);

  // D3 chart drawing
  useEffect(() => {
    if (!svgRef.current || !containerRef.current || chartData.length === 0) return;

    const svg = d3.select(svgRef.current);
    svg.selectAll('*').remove(); // Clear previous render

    const containerWidth = containerRef.current.clientWidth || 480;
    const height = 180;
    const margin = { top: 20, right: 25, bottom: 28, left: 38 };
    const width = containerWidth - margin.left - margin.right;
    const innerHeight = height - margin.top - margin.bottom;

    // SVG root setup
    svg
      .attr('viewBox', `0 0 ${containerWidth} ${height}`)
      .attr('width', '100%')
      .attr('height', height);

    // Definitions for gradients & glow filters
    const defs = svg.append('defs');

    // Area Gradient: Cyan/Green to transparent
    const areaGradient = defs
      .append('linearGradient')
      .attr('id', 'battery-area-gradient')
      .attr('x1', '0%')
      .attr('y1', '0%')
      .attr('x2', '0%')
      .attr('y2', '100%');

    areaGradient
      .append('stop')
      .attr('offset', '0%')
      .attr('stop-color', '#22c55e')
      .attr('stop-opacity', 0.35);

    areaGradient
      .append('stop')
      .attr('offset', '60%')
      .attr('stop-color', '#06b6d4')
      .attr('stop-opacity', 0.12);

    areaGradient
      .append('stop')
      .attr('offset', '100%')
      .attr('stop-color', '#06b6d4')
      .attr('stop-opacity', 0.0);

    // Stroke Gradient
    const strokeGradient = defs
      .append('linearGradient')
      .attr('id', 'battery-stroke-gradient')
      .attr('x1', '0%')
      .attr('y1', '0%')
      .attr('x2', '100%')
      .attr('y2', '0%');

    strokeGradient.append('stop').attr('offset', '0%').attr('stop-color', '#38bdf8');
    strokeGradient.append('stop').attr('offset', '100%').attr('stop-color', '#22c55e');

    // Glow filter
    const filter = defs.append('filter').attr('id', 'glow');
    filter
      .append('feGaussianBlur')
      .attr('stdDeviation', '2.5')
      .attr('result', 'coloredBlur');
    const feMerge = filter.append('feMerge');
    feMerge.append('feMergeNode').attr('in', 'coloredBlur');
    feMerge.append('feMergeNode').attr('in', 'SourceGraphic');

    const g = svg
      .append('g')
      .attr('transform', `translate(${margin.left},${margin.top})`);

    // Scales
    const xExtent = d3.extent<BatteryDataPoint, Date>(chartData, d => d.time);
    const validXExtent: [Date, Date] = [
      xExtent[0] || new Date(Date.now() - 3600000),
      xExtent[1] || new Date()
    ];
    const xScale = d3.scaleTime().domain(validXExtent).range([0, width]);

    const minLevelVal = d3.min<BatteryDataPoint, number>(chartData, d => d.level) ?? 80;
    const maxLevelVal = d3.max<BatteryDataPoint, number>(chartData, d => d.level) ?? 100;
    const minLevel = Math.max(0, Math.floor(minLevelVal - 2));
    const maxLevel = Math.min(100, Math.ceil(maxLevelVal + 2));
    const yScale = d3.scaleLinear().domain([minLevel, maxLevel]).range([innerHeight, 0]);

    // Grid Lines (horizontal)
    const yTicks = yScale.ticks(4);
    g.append('g')
      .attr('class', 'grid')
      .selectAll('line')
      .data(yTicks)
      .enter()
      .append('line')
      .attr('x1', 0)
      .attr('x2', width)
      .attr('y1', d => yScale(d))
      .attr('y2', d => yScale(d))
      .attr('stroke', '#1f2937')
      .attr('stroke-dasharray', '3,3')
      .attr('stroke-width', 1);

    // Axes
    const xAxis = d3
      .axisBottom(xScale)
      .ticks(activeWindow === '60m' ? 6 : 4)
      .tickFormat(d => d3.timeFormat('%H:%M')(d as Date))
      .tickSize(0)
      .tickPadding(8);

    const yAxis = d3
      .axisLeft(yScale)
      .ticks(4)
      .tickFormat(d => `${d}%`)
      .tickSize(0)
      .tickPadding(6);

    const gx = g
      .append('g')
      .attr('transform', `translate(0,${innerHeight})`)
      .call(xAxis);

    gx.select('.domain').attr('stroke', '#374151');
    gx.selectAll('text').attr('fill', '#9ca3af').attr('font-size', '10px').attr('font-family', 'monospace');

    const gy = g.append('g').call(yAxis);
    gy.select('.domain').remove();
    gy.selectAll('text').attr('fill', '#9ca3af').attr('font-size', '10px').attr('font-family', 'monospace');

    // Area Generator
    const area = d3
      .area<BatteryDataPoint>()
      .x(d => xScale(d.time))
      .y0(innerHeight)
      .y1(d => yScale(d.level))
      .curve(d3.curveMonotoneX);

    g.append('path')
      .datum(chartData)
      .attr('fill', 'url(#battery-area-gradient)')
      .attr('d', area);

    // Line Generator
    const line = d3
      .line<BatteryDataPoint>()
      .x(d => xScale(d.time))
      .y(d => yScale(d.level))
      .curve(d3.curveMonotoneX);

    // Line glow background
    g.append('path')
      .datum(chartData)
      .attr('fill', 'none')
      .attr('stroke', '#22c55e')
      .attr('stroke-width', 4)
      .attr('stroke-opacity', 0.2)
      .attr('filter', 'url(#glow)')
      .attr('d', line);

    // Main line path
    g.append('path')
      .datum(chartData)
      .attr('fill', 'none')
      .attr('stroke', 'url(#battery-stroke-gradient)')
      .attr('stroke-width', 2.2)
      .attr('d', line);

    // Data dots
    const dotsGroup = g.append('g').attr('class', 'dots');

    dotsGroup
      .selectAll<SVGCircleElement, BatteryDataPoint>('circle.data-point')
      .data(chartData)
      .enter()
      .append('circle')
      .attr('class', 'data-point')
      .attr('cx', (d: BatteryDataPoint) => xScale(d.time))
      .attr('cy', (d: BatteryDataPoint) => yScale(d.level))
      .attr('r', (_d: BatteryDataPoint, i: number) => (i === chartData.length - 1 ? 5 : 3))
      .attr('fill', (_d: BatteryDataPoint, i: number) => (i === chartData.length - 1 ? '#22c55e' : '#0e7490'))
      .attr('stroke', (_d: BatteryDataPoint, i: number) => (i === chartData.length - 1 ? '#ffffff' : '#38bdf8'))
      .attr('stroke-width', (_d: BatteryDataPoint, i: number) => (i === chartData.length - 1 ? 2 : 1.5))
      .style('cursor', 'pointer');

    // Pulse animation ring around the last (current) point
    const lastPoint = chartData[chartData.length - 1];
    if (lastPoint) {
      g.append('circle')
        .attr('cx', xScale(lastPoint.time))
        .attr('cy', yScale(lastPoint.level))
        .attr('r', 8)
        .attr('fill', 'none')
        .attr('stroke', '#22c55e')
        .attr('stroke-width', 1.5)
        .attr('opacity', 0.7)
        .append('animate')
        .attr('attributeName', 'r')
        .attr('values', '5;12;5')
        .attr('dur', '2s')
        .attr('repeatCount', 'indefinite');
    }

    // Interactive Overlay for Tooltips
    const overlay = g
      .append('rect')
      .attr('width', width)
      .attr('height', innerHeight)
      .attr('fill', 'transparent')
      .style('cursor', 'crosshair');

    const bisectTime = d3.bisector<BatteryDataPoint, Date>(d => d.time).center;

    overlay.on('mousemove', function (event) {
      const [mouseX] = d3.pointer(event);
      const x0 = xScale.invert(mouseX);
      const index = bisectTime(chartData, x0);
      const selected = chartData[index];
      if (selected) {
        setHoveredPoint(selected);
      }
    });

    overlay.on('mouseleave', function () {
      setHoveredPoint(null);
    });

  }, [chartData, activeWindow]);

  const dischargeDelta = useMemo(() => {
    if (chartData.length < 2) return 0;
    const first = chartData[0].level;
    const last = chartData[chartData.length - 1].level;
    return Math.round((first - last) * 10) / 10;
  }, [chartData]);

  return (
    <div className="bg-[#161a22] border border-cyan-800/60 rounded-xl p-4 shadow-sm relative overflow-hidden flex flex-col">
      {/* Top Card Badge */}
      <div className="absolute top-0 right-0 px-3 py-1 bg-cyan-600/20 text-cyan-400 text-[10px] font-mono rounded-bl-lg border-b border-l border-cyan-500/30 flex items-center gap-1.5">
        <Zap className="w-3 h-3 text-yellow-400 animate-pulse" />
        <span>D3.JS TÉLÉMÉTRIE MATÉRIELLE</span>
      </div>

      {/* Title & Description */}
      <div className="flex flex-wrap items-center justify-between gap-2 mb-2 pr-32">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-lg bg-green-500/20 border border-green-500/40 flex items-center justify-center text-green-400">
            <BatteryMedium className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              Décharge Batterie BlackBerry Curve 9300
            </h3>
            <p className="text-[11px] text-gray-400">
              Télémétrie de décharge en temps réel sur la dernière heure (Profil HFP/SCO)
            </p>
          </div>
        </div>
      </div>

      {/* KPI Stats Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 my-2.5">
        <div className="bg-[#0d1117] p-2 rounded-lg border border-gray-800 flex flex-col">
          <span className="text-[10px] text-gray-400 flex items-center gap-1">
            <BatteryMedium className="w-3 h-3 text-green-400" />
            Niveau Actuel
          </span>
          <div className="flex items-baseline gap-1.5 mt-0.5">
            <span className="text-lg font-bold font-mono text-green-400">{currentLevel}%</span>
            <span className="text-[9px] text-gray-500 font-mono">Curve 9300</span>
          </div>
        </div>

        <div className="bg-[#0d1117] p-2 rounded-lg border border-gray-800 flex flex-col">
          <span className="text-[10px] text-gray-400 flex items-center gap-1">
            <TrendingDown className="w-3 h-3 text-amber-400" />
            Décharge ({activeWindow})
          </span>
          <div className="flex items-baseline gap-1.5 mt-0.5">
            <span className="text-lg font-bold font-mono text-amber-400">-{dischargeDelta}%</span>
            <span className="text-[9px] text-gray-500 font-mono">~8%/h</span>
          </div>
        </div>

        <div className="bg-[#0d1117] p-2 rounded-lg border border-gray-800 flex flex-col">
          <span className="text-[10px] text-gray-400 flex items-center gap-1">
            <Clock className="w-3 h-3 text-cyan-400" />
            Autonomie Estimée
          </span>
          <div className="flex items-baseline gap-1.5 mt-0.5">
            <span className="text-lg font-bold font-mono text-cyan-300">~11h 00m</span>
            <span className="text-[9px] text-gray-500 font-mono">1150 mAh</span>
          </div>
        </div>

        <div className="bg-[#0d1117] p-2 rounded-lg border border-gray-800 flex flex-col justify-between">
          <span className="text-[10px] text-gray-400">Période d'Analyse</span>
          <div className="flex items-center gap-1 mt-1">
            {(['15m', '30m', '60m'] as const).map(win => (
              <button
                key={win}
                onClick={() => setActiveWindow(win)}
                className={`flex-1 py-0.5 text-[10px] font-mono rounded font-medium transition-colors cursor-pointer ${
                  activeWindow === win
                    ? 'bg-cyan-600/30 text-cyan-300 border border-cyan-500/50'
                    : 'bg-gray-800/80 text-gray-400 hover:text-white border border-transparent'
                }`}
              >
                {win}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* D3 SVG Chart Container */}
      <div ref={containerRef} className="w-full relative mt-1 bg-[#0d1117] rounded-lg border border-gray-800/80 p-1">
        <svg ref={svgRef} className="overflow-visible block w-full"></svg>

        {/* Hover Tooltip Overlay */}
        {hoveredPoint && (
          <div className="absolute top-2 right-3 pointer-events-none bg-[#161a22]/95 border border-cyan-500/50 rounded-lg p-2 text-xs shadow-xl backdrop-blur-xs flex items-center gap-2 animate-in fade-in duration-100 font-mono">
            <div className="w-2 h-2 rounded-full bg-green-400 animate-ping"></div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-gray-400">{d3.timeFormat('%H:%M:%S')(hoveredPoint.time)}</span>
                <span className="font-bold text-green-400">{hoveredPoint.level}%</span>
                <span className="text-[10px] text-gray-500">(-{hoveredPoint.minutesAgo} min)</span>
              </div>
              {hoveredPoint.event && (
                <div className="text-[10px] text-cyan-300 mt-0.5">
                  Événement : {hoveredPoint.event}
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Legend & Telemetry Status Footer */}
      <div className="mt-2.5 pt-2 border-t border-gray-800/70 flex flex-wrap items-center justify-between text-[11px] text-gray-400 gap-2">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-0.5 bg-gradient-to-r from-cyan-400 to-green-400 rounded-full"></span>
            <span className="text-gray-300">Courbe D3 Monotone</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-green-400"></span>
            <span className="text-gray-300">Point Actuel (BlackBerry OS)</span>
          </div>
        </div>

        <div className="flex items-center gap-1 text-[10px] text-cyan-400 font-mono">
          <Zap className="w-3 h-3 text-cyan-400" />
          <span>SCO Audio + SPP Bluetooth activés</span>
        </div>
      </div>
    </div>
  );
};
