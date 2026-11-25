// ---- Panel layout engine (shared by the Layouts artboards) ----
// A layout is a tree of cuts. Each cut runs edge to edge across a four-sided panel,
// so every panel stays a quadrilateral, even when cuts are slanted.
var FAMILIES = [
  { key: 'classic', name: 'Classic', rule: 'Straight tiers that read left to right, top to bottom. The safe default for steady pacing.' },
  { key: 'stacks', name: 'Stacks', rule: 'Columns lead. Tall panels give height, falls and full-figure shots room.' },
  { key: 'slants', name: 'Slants', rule: 'Tilted cuts add speed and tension. Keep each tilt small and alternate direction.' },
  { key: 'insets', name: 'Insets', rule: 'One floating panel breaks the grid for the key moment, ringed by a gutter.' },
  { key: 'mosaic', name: 'Mosaic', rule: 'Nested splits at golden and off-grid ratios. One big panel anchors the page.' }
];
var TEMPLATES = [
  { name: 'Tiers', fam: 'classic', node: { d: 'h', r: [1.1, 1, 0.9], c: [{ d: 'v', r: [1, 1] }, { d: 'v', r: [0.65, 1.35] }, null] } },
  { name: 'Hero base', fam: 'classic', node: { d: 'h', r: [0.75, 0.75, 1.5], c: [{ d: 'v', r: [2, 1] }, { d: 'v', r: [1.3, 0.75, 0.75] }, null] } },
  { name: 'Brick', fam: 'classic', node: { d: 'h', r: [1, 1, 1], c: [{ d: 'v', r: [1.1, 1] }, { d: 'v', r: [0.6, 1.4] }, { d: 'v', r: [1.1, 0.9] }] } },
  { name: 'Tall left', fam: 'stacks', node: { d: 'v', r: [1, 1], c: [null, { d: 'h', r: [1, 1.25] }] } },
  { name: 'Triptych', fam: 'stacks', node: { d: 'h', r: [0.55, 1.45], c: [{ d: 'v', r: [0.6, 1.4] }, { d: 'v', r: [1.2, 0.8, 0.8] }] } },
  { name: 'Ladder', fam: 'stacks', node: { d: 'v', r: [1.1, 0.9], c: [{ d: 'h', r: [0.8, 0.6, 1] }, null] } },
  { name: 'Tilt', fam: 'slants', node: { d: 'h', r: [0.8, 1, 1, 1.1], s: [0.05, -0.06, 0.08], c: [null, { d: 'v', r: [1, 1], s: [0.12] }, null, null] } },
  { name: 'Wedge', fam: 'slants', node: { d: 'h', r: [1, 1.5], s: [0.14], c: [{ d: 'v', r: [1, 1], s: [-0.08] }, null] } },
  { name: 'Rush', fam: 'slants', node: { d: 'h', r: [1, 1.2], s: [-0.1], c: [{ d: 'v', r: [1, 1, 1], s: [0.1, 0.1] }, { d: 'v', r: [1, 1.3], s: [-0.14] }] } },
  { name: 'Spotlight', fam: 'insets', node: { inset: [0.22, 0.5, 0.56, 0.34], base: { d: 'h', r: [0.55, 1.45], c: [null, { d: 'v', r: [1, 1] }] } } },
  { name: 'Corner pop', fam: 'insets', node: { inset: [0.52, 0.05, 0.43, 0.3], base: { d: 'h', r: [1, 1], c: [{ d: 'v', r: [1, 1] }, null] } } },
  { name: 'Center stage', fam: 'insets', node: { inset: [0.17, 0.31, 0.66, 0.38], base: { d: 'h', r: [1, 1, 1], c: [{ d: 'v', r: [1, 1] }, null, { d: 'v', r: [1, 1, 1] }] } } },
  { name: 'Spiral', fam: 'mosaic', node: { d: 'h', r: [1.618, 1], c: [null, { d: 'v', r: [1.618, 1], c: [null, { d: 'h', r: [1, 1.618], c: [null, { d: 'v', r: [1, 1.618] }] }] }] } },
  { name: 'Off-grid', fam: 'mosaic', node: { d: 'v', r: [1, 1.4], c: [{ d: 'h', r: [1, 0.6, 1] }, { d: 'h', r: [0.7, 1.3], c: [null, { d: 'v', r: [1, 1] }] }] } },
  { name: 'Window', fam: 'mosaic', node: { d: 'h', r: [0.5, 1.6, 0.5], c: [{ d: 'v', r: [1.6, 1] }, { d: 'v', r: [0.5, 1.6, 0.5], c: [null, null, { d: 'h', r: [1, 1] }] }, { d: 'v', r: [1, 1.6] }] } }
];
var PALETTE = ['#E4572E', '#F2A65A', '#8C5383', '#1B998B', '#F6E7CB', '#2E4057', '#AFCBEA', '#A7D86E'];
var STYLES = {
  paper: { bg: '#D7CFC2', panel: function () { return '#FFFFFF'; } },
  ink: { bg: '#151515', panel: function () { return '#FBF8F3'; } },
  palette: { bg: '#151515', panel: function (i) { return PALETTE[i % PALETTE.length]; } }
};

function lerp(a, b, t) { return { x: a.x + (b.x - a.x) * t, y: a.y + (b.y - a.y) * t }; }
function dist(a, b) { return Math.max(1e-6, Math.hypot(b.x - a.x, b.y - a.y)); }
function rect(x, y, w, h) { return [{ x: x, y: y }, { x: x + w, y: y }, { x: x + w, y: y + h }, { x: x, y: y + h }]; }

function splitNode(node, q, g, sm, out) {
  if (!node) { out.push({ pts: q, kind: 'panel' }); return; }
  if (node.inset) {
    splitNode(node.base, q, g, sm, out);
    var xs = q.map(function (p) { return p.x; }), ys = q.map(function (p) { return p.y; });
    var x0 = Math.min.apply(null, xs), y0 = Math.min.apply(null, ys), bw = Math.max.apply(null, xs) - x0, bh = Math.max.apply(null, ys) - y0;
    var i = node.inset, x = x0 + i[0] * bw, y = y0 + i[1] * bh, w = i[2] * bw, h = i[3] * bh;
    out.push({ pts: rect(x - g, y - g, w + 2 * g, h + 2 * g), kind: 'ring' });
    out.push({ pts: rect(x, y, w, h), kind: 'panel' });
    return;
  }
  var r = node.r, n = r.length, tot = 0, acc = 0, cuts = [];
  r.forEach(function (v) { tot += v; });
  for (var k = 0; k < n - 1; k++) {
    acc += r[k];
    var c = acc / tot, s = ((node.s && node.s[k]) || 0) * sm;
    cuts.push([c - s / 2, c + s / 2]);
  }
  var tl = q[0], tr = q[1], br = q[2], bl = q[3];
  var e1a, e1b, e2a, e2b;
  if (node.d === 'v') { e1a = tl; e1b = tr; e2a = bl; e2b = br; } else { e1a = tl; e1b = bl; e2a = tr; e2b = br; }
  var d1 = g / 2 / dist(e1a, e1b), d2 = g / 2 / dist(e2a, e2b);
  for (var j = 0; j < n; j++) {
    var s1 = j === 0 ? 0 : cuts[j - 1][0] + d1, s2 = j === 0 ? 0 : cuts[j - 1][1] + d2;
    var t1 = j === n - 1 ? 1 : cuts[j][0] - d1, t2 = j === n - 1 ? 1 : cuts[j][1] - d2;
    var A = lerp(e1a, e1b, s1), B = lerp(e1a, e1b, t1), C = lerp(e2a, e2b, t2), D = lerp(e2a, e2b, s2);
    splitNode(node.c ? node.c[j] : null, node.d === 'v' ? [A, B, C, D] : [A, D, C, B], g, sm, out);
  }
}

// Returns positioned layers for a w×h px box. Page units: 1000 wide.
function renderLayout(node, w, h, o) {
  o = o || {};
  var PW = 1000, PH = 1000 * h / w, sc = w / PW;
  var g = o.gutter == null ? 24 : o.gutter, m = o.margin == null ? 40 : o.margin, sm = o.slant == null ? 1 : o.slant;
  var st = STYLES[o.style || 'paper'], out = [];
  splitNode(node, rect(m, m, PW - 2 * m, PH - 2 * m), g, sm, out);
  var idx = 0;
  var items = out.map(function (it) {
    var pts = it.pts.map(function (p) {
      var x = o.mirrorX ? PW - p.x : p.x, y = o.mirrorY ? PH - p.y : p.y;
      return (Math.round(x * sc * 10) / 10) + 'px ' + (Math.round(y * sc * 10) / 10) + 'px';
    }).join(', ');
    var color = it.kind === 'ring' ? st.bg : st.panel(idx++);
    return 'position: absolute; inset: 0; clip-path: polygon(' + pts + '); background: ' + color;
  });
  return { items: items, count: idx, boxStyle: 'position: relative; overflow: hidden; flex: none; width: ' + w + 'px; height: ' + h + 'px; background: ' + st.bg };
}

function mulberry(seed) { return function () { seed |= 0; seed = seed + 0x6D2B79F5 | 0; var t = Math.imul(seed ^ seed >>> 15, 1 | seed); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; }; }
function countPanels(node) {
  if (!node) return 1;
  if (node.inset) return countPanels(node.base) + 1;
  var n = 0; for (var i = 0; i < node.r.length; i++) n += countPanels(node.c ? node.c[i] : null); return n;
}
// Builds a new layout from the same rules: 2–3 cuts per level, alternating direction,
// ratios between 0.6 and 1.5, optional small slants and an occasional inset.
function generateLayout(seed, slantChance) {
  for (var tries = 0; tries < 30; tries++) {
    var rand = mulberry(seed * 7919 + tries);
    var grow = function (depth, dir) {
      if (depth > 0 && (depth >= 3 || rand() < 0.25 + depth * 0.2)) return null;
      var n = 2 + Math.floor(rand() * (depth === 0 ? 3 : 2)), r = [], s = [], c = [];
      for (var i = 0; i < n; i++) r.push(Math.round((0.6 + rand() * 0.9) * 100) / 100);
      for (var k = 0; k < n - 1; k++) s.push(rand() < slantChance ? Math.round((rand() - 0.5) * 0.24 * 100) / 100 : 0);
      for (var j = 0; j < n; j++) c.push(grow(depth + 1, dir === 'h' ? 'v' : 'h'));
      return { d: dir, r: r, s: s, c: c };
    };
    var node = grow(0, rand() < 0.75 ? 'h' : 'v');
    if (rand() < 0.2) node = { inset: [0.15 + rand() * 0.3, 0.2 + rand() * 0.4, 0.4, 0.28], base: node };
    var n = countPanels(node);
    if (n >= 4 && n <= 9) return node;
  }
  return TEMPLATES[0].node;
}
// ---- end engine ----
