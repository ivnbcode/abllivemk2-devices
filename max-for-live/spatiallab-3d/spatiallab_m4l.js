/*
 * SPATIAL-LAB 3D MkII — Max for Live 3D Binaural Spatializer Core (spatiallab_m4l.js)
 * Diseñado para Ableton Live + Arturia MiniLab MkII
 *
 * Calcula en tiempo real desde coordenadas 3D (X, Y, Z) y trayectorias orbitales:
 *   - ITD (Interaural Time Difference): micro-retardo en ms para oído izquierdo y derecho (0.0 a 0.72 ms)
 *   - ILD (Interaural Level Difference): ganancia binaural L/R con sombra acústica de cabeza
 *   - Filtro de Cabeza / Pinna (Shadow + Back/Front + Elevation): frecuencia de corte para oído L y R
 *   - Atenuación por distancia (Ley inversa suave) y envío de reflexiones de sala (Reverb Send)
 *
 * Entradas (Inlets):
 *   - "pos_x" <-1.0..1.0>        (Izquierda <-> Derecha)
 *   - "pos_y" <-1.0..1.0>        (Abajo <-> Arriba / Elevación)
 *   - "pos_z" <-1.0..1.0>        (Adelante <-> Atrás)
 *   - "orbit_speed" <0.0..5.0>   (Velocidad de órbita 3D en Hz)
 *   - "orbit_depth" <0.0..1.0>   (Radio de órbita automática)
 *   - "room_mix" <0.0..1.0>      (Nivel de ambiente/reflexiones de sala)
 *   - "cc1" <0..127>             (Tira táctil Modulación MiniLab MkII -> barrido orbital / distancia)
 *   - "pad" <0..7>               (Pads 1-8 MiniLab MkII -> Posiciones y trayectorias 3D instantáneas)
 *   - "note" <pitch> <vel>       (Notas 36..43 disparan los 8 presets espaciales)
 *   - "tick"                     (Reloj de animación desde [metro 25] para órbitas suaves)
 *
 * Salidas (Outlets):
 *   Outlet 0: Lista DSP [delayL_ms, delayR_ms, gainL, gainR, cutoffL_hz, cutoffR_hz, reverbSend]
 *   Outlet 1: Mensajes de UI [preset_name, x, y, z, azimuth_deg, distance_m]
 */

autowatch = 1;
inlets = 1;
outlets = 2;

var baseX = 0.0;
var baseY = 0.2;
var baseZ = 0.55; // Enfrente del oyente por defecto

var orbitSpeed = 0.0;
var orbitDepth = 0.0;
var orbitPhase = 0.0;
var orbitMode = 'circle'; // 'circle' | 'figure8' | 'spiral' | 'elevation_bounce'
var roomMix = 0.30;
var cc1PhaseOffset = 0.0;

// 8 Presets Espaciales para los 8 Pads del Arturia MiniLab MkII (Notas 36-43)
var SPATIAL_PADS = [
  { name: 'PAD 1: Frente Cercano',  x: 0.0,   y: 0.1,  z: 0.45, speed: 0.0,  depth: 0.0,  mode: 'circle' },
  { name: 'PAD 2: Detras Nuca',     x: 0.0,   y: -0.1, z: -0.7, speed: 0.0,  depth: 0.0,  mode: 'circle' },
  { name: 'PAD 3: Izquierda Lejos', x: -0.85, y: 0.15, z: 0.2,  speed: 0.0,  depth: 0.0,  mode: 'circle' },
  { name: 'PAD 4: Derecha Lejos',   x: 0.85,  y: 0.15, z: 0.2,  speed: 0.0,  depth: 0.0,  mode: 'circle' },
  { name: 'PAD 5: Cenit (Arriba)',  x: 0.0,   y: 0.95, z: 0.1,  speed: 0.25, depth: 0.2,  mode: 'circle' },
  { name: 'PAD 6: Orbita 360',      x: 0.0,   y: 0.15, z: 0.0,  speed: 0.65, depth: 0.85, mode: 'circle' },
  { name: 'PAD 7: Infinito 8',      x: 0.0,   y: 0.2,  z: 0.0,  speed: 0.8,  depth: 0.9,  mode: 'figure8' },
  { name: 'PAD 8: Espiral Domo',    x: 0.0,   y: 0.0,  z: 0.0,  speed: 1.1,  depth: 0.95, mode: 'spiral' }
];

var activePresetName = SPATIAL_PADS[0].name;

function loadbang() {
  computeSpatialDSP();
}

function pos_x(v) {
  baseX = Math.max(-1.0, Math.min(1.0, v));
  computeSpatialDSP();
}

function pos_y(v) {
  baseY = Math.max(-1.0, Math.min(1.0, v));
  computeSpatialDSP();
}

function pos_z(v) {
  baseZ = Math.max(-1.0, Math.min(1.0, v));
  computeSpatialDSP();
}

function orbit_speed(v) {
  orbitSpeed = Math.max(0.0, Math.min(5.0, v));
  computeSpatialDSP();
}

function orbit_depth(v) {
  orbitDepth = Math.max(0.0, Math.min(1.0, v));
  computeSpatialDSP();
}

function room_mix(v) {
  roomMix = Math.max(0.0, Math.min(1.0, v));
  computeSpatialDSP();
}

function cc1(val) {
  // La tira táctil de Modulación del Arturia MiniLab MkII permite girar manualmente la fuente 360° alrededor de tu cabeza
  var norm = Math.max(0, Math.min(127, val)) / 127.0;
  cc1PhaseOffset = norm * Math.PI * 2.0;
  if (orbitDepth < 0.15) {
    // Si no tenía órbita activa, usar radio de 0.75 alrededor de la cabeza al deslizar la tira táctil
    baseX = Math.sin(cc1PhaseOffset) * 0.78;
    baseZ = Math.cos(cc1PhaseOffset) * 0.78;
  }
  computeSpatialDSP();
}

function pad(idx) {
  if (idx < 0 || idx >= SPATIAL_PADS.length) return;
  var p = SPATIAL_PADS[idx];
  baseX = p.x;
  baseY = p.y;
  baseZ = p.z;
  orbitSpeed = p.speed;
  orbitDepth = p.depth;
  orbitMode = p.mode;
  activePresetName = p.name;
  computeSpatialDSP();
}

function note(pitch, vel) {
  if (vel > 0 && pitch >= 36 && pitch <= 43) {
    pad(pitch - 36);
  }
}

function tick() {
  if (orbitSpeed > 0.001 && orbitDepth > 0.001) {
    orbitPhase += (orbitSpeed * 0.025 * Math.PI * 2.0);
    if (orbitPhase > Math.PI * 200) orbitPhase -= Math.PI * 200;
    computeSpatialDSP();
  }
}

function computeSpatialDSP() {
  var x = baseX;
  var y = baseY;
  var z = baseZ;

  var ph = orbitPhase + cc1PhaseOffset;

  if (orbitDepth > 0.001) {
    if (orbitMode === 'figure8') {
      x = Math.max(-1, Math.min(1, baseX + Math.sin(ph) * orbitDepth));
      z = Math.max(-1, Math.min(1, baseZ + Math.sin(ph * 2.0) * orbitDepth * 0.8));
      y = Math.max(-1, Math.min(1, baseY + Math.cos(ph) * orbitDepth * 0.4));
    } else if (orbitMode === 'spiral') {
      var r = (0.35 + 0.65 * (0.5 + 0.5 * Math.sin(ph * 0.35))) * orbitDepth;
      x = Math.max(-1, Math.min(1, Math.sin(ph) * r));
      z = Math.max(-1, Math.min(1, Math.cos(ph) * r));
      y = Math.max(-1, Math.min(1, Math.sin(ph * 0.5) * 0.85));
    } else {
      x = Math.max(-1, Math.min(1, baseX + Math.sin(ph) * orbitDepth));
      z = Math.max(-1, Math.min(1, baseZ + Math.cos(ph) * orbitDepth));
    }
  }

  // Distancia euclidiana en metros virtuales (0.25m a ~3.5m)
  var rawDist = Math.sqrt(x * x + y * y + z * z);
  var distMeters = 0.3 + rawDist * 2.4;

  // Ángulo horizontal Azimuth (-180 a +180 grados; 0 = frente, +90 = derecha, -90 = izquierda)
  var azimuthRad = Math.atan2(x, z === 0 ? 0.0001 : z);
  var azimuthDeg = Math.round(azimuthRad * (180.0 / Math.PI));

  // 1. ITD (Interaural Time Difference): hasta 0.68 ms según el ángulo lateral
  var lateral = Math.max(-1.0, Math.min(1.0, x));
  var maxItdMs = 0.68;
  var delayL = lateral > 0 ? lateral * maxItdMs : 0.0;
  var delayR = lateral < 0 ? (-lateral) * maxItdMs : 0.0;

  // 2. Atenuación por distancia + ILD (Interaural Level Difference)
  var distAtten = 1.0 / (1.0 + rawDist * 0.85);
  var panAngle = (lateral + 1.0) * 0.25 * Math.PI; // Equal-power base
  var gainL = Math.cos(panAngle) * distAtten;
  var gainR = Math.sin(panAngle) * distAtten;

  // 3. Filtro HRTF simplificado de sombra de cabeza + diferenciación Frente/Atrás (Z) + Elevación (Y)
  // Cuando Z < 0 (detrás de la cabeza), el pabellón auricular atenúa agudos
  var backFactor = z < 0 ? (1.0 + z * 0.55) : 1.0;
  // Elevación alta abre presencia en 7-12kHz; elevación baja oscurece ligeramente
  var elevFactor = 1.0 + (y * 0.25);

  var shadowL = lateral > 0 ? (1.0 - lateral * 0.58) : 1.0;
  var shadowR = lateral < 0 ? (1.0 - (-lateral) * 0.58) : 1.0;

  var cutoffL = Math.max(650.0, Math.min(18000.0, 13500.0 * backFactor * elevFactor * shadowL));
  var cutoffR = Math.max(650.0, Math.min(18000.0, 13500.0 * backFactor * elevFactor * shadowR));

  // 4. Envío a reflexiones de sala (aumenta con la distancia para sensación de profundidad 3D)
  var reverbSend = Math.min(0.95, roomMix * (0.45 + rawDist * 0.55));

  outlet(0, [delayL, delayR, gainL, gainR, cutoffL, cutoffR, reverbSend]);
  outlet(1, ['status', activePresetName, x.toFixed(2), y.toFixed(2), z.toFixed(2), azimuthDeg, distMeters.toFixed(2)]);
}
