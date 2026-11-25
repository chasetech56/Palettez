// ---- Color system engine (shared by the color artboards) ----
var MOODS = ['Earthy', 'Warm', 'Cool', 'Pastel', 'Bold', 'Neutral'];
var PRESETS = [
  { name: 'Sunset', mood: 'From image', mine: true, colors: ['#F6E7CB', '#E4572E', '#8C5383', '#F2A65A', '#2E4057', '#1B998B'] },
  { name: 'Terracotta', mood: 'Earthy', colors: ['#2F3E46', '#C8553D', '#F28F3B', '#588B8B', '#FFE8DC'] },
  { name: 'Clay & Moss', mood: 'Earthy', colors: ['#2E3A23', '#8C6A4F', '#6B7F4E', '#D9B48F', '#F3ECE0'] },
  { name: 'Savanna', mood: 'Earthy', colors: ['#1F2A22', '#C8643B', '#E59A55', '#3E5641', '#F4E6C9'] },
  { name: 'Ember', mood: 'Warm', colors: ['#3D0C11', '#D94F30', '#8E1B1B', '#F59E42', '#FCE8C8'] },
  { name: 'Market day', mood: 'Warm', colors: ['#16323F', '#D91A60', '#F25C05', '#1B8C6D', '#FFF4D6'] },
  { name: 'Lagoon', mood: 'Cool', colors: ['#0B3954', '#087E8B', '#5FB49C', '#F2A541', '#F5F1E8'] },
  { name: 'Night swim', mood: 'Cool', colors: ['#101A2B', '#45607F', '#86A0BE', '#E4C36B', '#E4E7EA'] },
  { name: 'Sorbet', mood: 'Pastel', colors: ['#3B3450', '#E7798F', '#C1F0DC', '#C9D7F8', '#FFF6E6'] },
  { name: 'Linen', mood: 'Pastel', colors: ['#4B5859', '#C9828C', '#AFC5B3', '#E9B4BC', '#F6EEE8'] },
  { name: 'Pop', mood: 'Bold', colors: ['#151515', '#FF3B6B', '#2979FF', '#FFC233', '#FFFFFF'] },
  { name: 'Riso', mood: 'Bold', colors: ['#1A1A40', '#FF48B0', '#0078BF', '#FFE800', '#F5F1E8'] },
  { name: 'Paper', mood: 'Neutral', colors: ['#151515', '#4A453E', '#9C958A', '#D7CFC2', '#FBF8F3'] },
  { name: 'Concrete', mood: 'Neutral', colors: ['#1F2124', '#3C4046', '#7D838C', '#C3C8CE', '#F2F3F5'] },
  { name: 'Palettez', mood: 'Bold', colors: ['#151515', '#6A58CC', '#E2603F', '#F4C542', '#A7D86E', '#EFE9DF'] }
];
var STEPS = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900];
// Tints mix toward white, shades toward black, in OKLab so steps look even.
var MIX = [[1, 0.92], [1, 0.82], [1, 0.64], [1, 0.44], [1, 0.22], [0, 0], [-1, 0.15], [-1, 0.32], [-1, 0.5], [-1, 0.66]];

function hexToRgb(h) { var n = parseInt(h.slice(1), 16); return [(n >> 16) & 255, (n >> 8) & 255, n & 255]; }
function rgbToHex(c) { return '#' + c.map(function (v) { v = Math.max(0, Math.min(255, Math.round(v))); return (v < 16 ? '0' : '') + v.toString(16); }).join('').toUpperCase(); }
function toLin(v) { v /= 255; return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }
function fromLin(v) { v = v <= 0.0031308 ? 12.92 * v : 1.055 * Math.pow(v, 1 / 2.4) - 0.055; return v * 255; }
function toOklab(hex) {
  var c = hexToRgb(hex).map(toLin), r = c[0], g = c[1], b = c[2];
  var l = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b);
  var m = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b);
  var s = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
  return [0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s, 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s, 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s];
}
function fromOklab(L) {
  var l = Math.pow(L[0] + 0.3963377774 * L[1] + 0.2158037573 * L[2], 3);
  var m = Math.pow(L[0] - 0.1055613458 * L[1] - 0.0638541728 * L[2], 3);
  var s = Math.pow(L[0] - 0.0894841775 * L[1] - 1.2914855480 * L[2], 3);
  return rgbToHex([4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s, -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s, -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s].map(fromLin));
}
function mixHex(a, b, t) { var A = toOklab(a), B = toOklab(b); return fromOklab([0, 1, 2].map(function (i) { return A[i] + (B[i] - A[i]) * t; })); }
function scaleOf(hex) {
  return STEPS.map(function (step, i) {
    var m = MIX[i], v = m[0] === 0 ? hex.toUpperCase() : mixHex(hex, m[0] > 0 ? '#FFFFFF' : '#000000', m[1]);
    return { step: step, hex: v };
  });
}
function lum(hex) { var c = hexToRgb(hex).map(toLin); return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]; }
function contrast(a, b) { var x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); }
function grade(r) { return r >= 7 ? 'AAA' : r >= 4.5 ? 'AA' : r >= 3 ? 'AA Large' : 'Fail'; }
function inkOn(hex) { return contrast(hex, '#151515') >= contrast(hex, '#FFFFFF') ? '#151515' : '#FFFFFF'; }
function hsl(hex) {
  var c = hexToRgb(hex).map(function (v) { return v / 255; }), mx = Math.max.apply(null, c), mn = Math.min.apply(null, c), h = 0, s = 0, l = (mx + mn) / 2;
  if (mx !== mn) { var d = mx - mn; s = l > 0.5 ? d / (2 - mx - mn) : d / (mx + mn); h = mx === c[0] ? (c[1] - c[2]) / d + (c[1] < c[2] ? 6 : 0) : mx === c[1] ? (c[2] - c[0]) / d + 2 : (c[0] - c[1]) / d + 4; h *= 60; }
  return [h, s, l];
}
function fromHsl(h, s, l) {
  h = ((h % 360) + 360) % 360;
  var f = function (n) { var k = (n + h / 30) % 12, a = s * Math.min(l, 1 - l); return 255 * (l - a * Math.max(-1, Math.min(k - 3, 9 - k, 1))); };
  return rgbToHex([f(0), f(8), f(4)]);
}
function harmonies(hex) {
  var c = hsl(hex), h = c[0], s = c[1], l = c[2];
  var at = function (d) { return fromHsl(h + d, s, l); };
  return [
    { name: 'Complementary', colors: [hex.toUpperCase(), at(180)] },
    { name: 'Analogous', colors: [at(-30), hex.toUpperCase(), at(30)] },
    { name: 'Triadic', colors: [hex.toUpperCase(), at(120), at(240)] },
    { name: 'Split', colors: [hex.toUpperCase(), at(150), at(210)] }
  ];
}
// Roles: background = lightest, text = darkest, primary = the most colorful color that is
// readable on the background, secondary = the next, accent = the hue furthest from primary.
function autoRoles(colors) {
  var byL = colors.slice().sort(function (a, b) { return lum(b) - lum(a); });
  var bg = byL[0], text = byL[byL.length - 1];
  if (contrast(bg, text) < 7) text = contrast(bg, '#151515') >= contrast(bg, '#FFFFFF') ? '#151515' : '#FFFFFF';
  var rest = colors.filter(function (c) { return c !== bg && c !== text; });
  if (!rest.length) rest = [text];
  var chroma = function (c) { var o = toOklab(c); return Math.hypot(o[1], o[2]); };
  var ranked = rest.slice().sort(function (a, b) { return (chroma(b) + (contrast(b, bg) >= 3 ? 1 : 0)) - (chroma(a) + (contrast(a, bg) >= 3 ? 1 : 0)); });
  var primary = ranked[0], secondary = ranked[1] || ranked[0];
  var hp = hsl(primary)[0];
  var pool = rest.filter(function (c) { return c !== primary && c !== secondary; });
  if (!pool.length) pool = rest.filter(function (c) { return c !== primary; });
  if (!pool.length) pool = [primary];
  var accent = pool.sort(function (a, b) {
    var da = Math.abs(((hsl(a)[0] - hp + 540) % 360) - 180), db = Math.abs(((hsl(b)[0] - hp + 540) % 360) - 180);
    return da - db;
  })[0];
  return { background: bg, text: text, primary: primary, secondary: secondary, accent: accent };
}
var ROLE_ORDER = ['background', 'text', 'primary', 'secondary', 'accent'];
var ROLE_NAMES = { background: 'Background', text: 'Text', primary: 'Primary', secondary: 'Secondary', accent: 'Accent' };
function slug(s) { return s.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, ''); }
function exportCode(kind, p, roles) {
  var names = p.colors.map(function (c, i) { return 'color-' + (i + 1); });
  if (kind === 'css') {
    var lines = [':root {'];
    ROLE_ORDER.forEach(function (r) { lines.push('  --' + r + ': ' + roles[r] + ';'); });
    p.colors.forEach(function (c, i) { scaleOf(c).forEach(function (s) { lines.push('  --' + names[i] + '-' + s.step + ': ' + s.hex + ';'); }); });
    lines.push('}'); return lines.join('\n');
  }
  if (kind === 'tailwind') {
    var o = {}; ROLE_ORDER.forEach(function (r) { o[r] = roles[r]; });
    p.colors.forEach(function (c, i) { var sc = {}; scaleOf(c).forEach(function (s) { sc[s.step] = s.hex; }); o[names[i]] = sc; });
    return '// tailwind.config.js\nmodule.exports = {\n  theme: { extend: { colors: ' + JSON.stringify(o, null, 2).replace(/\n/g, '\n  ') + ' } }\n};';
  }
  var t = { name: p.name, roles: roles, scales: {} };
  p.colors.forEach(function (c, i) { var sc = {}; scaleOf(c).forEach(function (s) { sc[s.step] = s.hex; }); t.scales[names[i]] = sc; });
  return JSON.stringify(t, null, 2);
}
// ---- end color engine ----
