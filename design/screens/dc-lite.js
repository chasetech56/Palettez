/*
 * dc-lite.js — a tiny stand-alone renderer for the Palettez design screens (*.dc.html).
 * Written for this project so the screens open in any browser without the design canvas.
 * Supports exactly what the screens use: {{holes}}, <sc-for>, <sc-if>, on* event holes,
 * <helmet> (moved into <head>), data-props defaults, and a DCLogic base class with setState.
 * MIT licensed, part of the Palettez repository.
 */
(function () {
  'use strict';

  class DCLogic {
    constructor(props) { this.props = props || {}; this.state = {}; }
    setState(update) {
      const next = typeof update === 'function' ? update(this.state, this.props) : update;
      this.state = Object.assign({}, this.state, next);
      schedule();
    }
    forceUpdate() { schedule(); }
  }

  let component = null, template = null, mount = null, pending = false;

  function schedule() {
    if (pending) return;
    pending = true;
    requestAnimationFrame(() => { pending = false; render(); });
  }

  function lookup(path, scopes) {
    path = path.trim();
    if (path === 'true') return true;
    if (path === 'false') return false;
    if (path === 'null') return null;
    if (/^-?\d+(\.\d+)?$/.test(path)) return Number(path);
    if (/^(['"]).*\1$/.test(path)) return path.slice(1, -1);
    const parts = path.split('.');
    for (let i = scopes.length - 1; i >= 0; i--) {
      const scope = scopes[i];
      if (scope && Object.prototype.hasOwnProperty.call(scope, parts[0])) {
        let value = scope[parts[0]];
        for (let k = 1; k < parts.length && value != null; k++) value = value[parts[k]];
        return value;
      }
    }
    return undefined;
  }

  const HOLE = /\{\{([^}]*)\}\}/g;
  const WHOLE = /^\s*\{\{([^}]*)\}\}\s*$/;
  const text = (s, scopes) => s.replace(HOLE, (_, p) => { const v = lookup(p, scopes); return v == null ? '' : String(v); });

  function build(node, scopes, out) {
    if (node.nodeType === Node.TEXT_NODE) {
      out.appendChild(document.createTextNode(node.nodeValue.includes('{{') ? text(node.nodeValue, scopes) : node.nodeValue));
      return;
    }
    if (node.nodeType !== Node.ELEMENT_NODE) return;
    const tag = node.localName;

    if (tag === 'sc-for') {
      const list = lookup((node.getAttribute('list') || '').replace(/^\s*\{\{|\}\}\s*$/g, ''), scopes) || [];
      const as = node.getAttribute('as') || 'item';
      Array.from(list).forEach((item, index) => {
        const scope = { $index: index }; scope[as] = item;
        node.childNodes.forEach(child => build(child, scopes.concat([scope]), out));
      });
      return;
    }
    if (tag === 'sc-if') {
      const value = lookup((node.getAttribute('value') || '').replace(/^\s*\{\{|\}\}\s*$/g, ''), scopes);
      if (value) node.childNodes.forEach(child => build(child, scopes, out));
      return;
    }
    if (tag === 'helmet') return;

    const el = node.cloneNode(false); // keeps the SVG namespace for <svg> children
    Array.from(node.attributes).forEach(attr => {
      const name = attr.name;
      if (name.startsWith('hint-')) { el.removeAttribute(name); return; }
      if (!attr.value.includes('{{')) return;
      const whole = attr.value.match(WHOLE);
      if (name.startsWith('on') && whole) {
        el.removeAttribute(name);
        const fn = lookup(whole[1], scopes);
        let type = name.slice(2).toLowerCase();
        if (type === 'change' && /^(input|textarea)$/i.test(tag)) type = 'input'; // React-style onChange
        if (typeof fn === 'function') el.addEventListener(type, fn);
        return;
      }
      const value = text(attr.value, scopes);
      el.setAttribute(name, value);
      if (name === 'value' && 'value' in el) el.value = value;
    });
    node.childNodes.forEach(child => build(child, scopes, el));
    out.appendChild(el);
  }

  function render() {
    const active = document.activeElement;
    const focusId = active && active.id;
    const selection = focusId && 'selectionStart' in active ? [active.selectionStart, active.selectionEnd] : null;
    const scrolls = Array.from(mount.querySelectorAll('*')).filter(e => e.scrollTop || e.scrollLeft).map(e => [pathOf(e), e.scrollTop, e.scrollLeft]);

    const fragment = document.createDocumentFragment();
    const vals = component.renderVals ? component.renderVals() : {};
    template.childNodes.forEach(child => build(child, [vals], fragment));
    mount.replaceChildren(fragment);

    scrolls.forEach(([path, top, left]) => { const e = byPath(path); if (e) { e.scrollTop = top; e.scrollLeft = left; } });
    if (focusId) {
      const again = document.getElementById(focusId);
      if (again) { again.focus(); if (selection) try { again.setSelectionRange(selection[0], selection[1]); } catch (e) { /* not a text field */ } }
    }
  }

  function pathOf(el) {
    const path = [];
    while (el && el !== mount) { path.unshift(Array.prototype.indexOf.call(el.parentNode.children, el)); el = el.parentNode; }
    return path;
  }
  function byPath(path) {
    let el = mount;
    for (const i of path) { el = el && el.children[i]; }
    return el;
  }

  function start() {
    const host = document.querySelector('x-dc');
    const script = document.querySelector('script[data-dc-script]');
    if (!host || !script) return;

    const helmet = host.querySelector('helmet');
    if (helmet) Array.from(helmet.children).forEach(child => document.head.appendChild(child.cloneNode(true)));

    template = document.createElement('template').content;
    Array.from(host.childNodes).forEach(child => { if (child !== helmet) template.appendChild(child); });
    mount = host;

    let defaults = {};
    try {
      const props = JSON.parse(script.getAttribute('data-props') || '{}');
      Object.keys(props).forEach(k => { if (k[0] !== '$' && props[k] && 'default' in props[k]) defaults[k] = props[k].default; });
    } catch (e) { /* no props */ }

    const Component = new Function('DCLogic', script.textContent + '\n;return Component;')(DCLogic);
    component = new Component(defaults);
    if (!component.state) component.state = {};
    render();
    if (component.componentDidMount) component.componentDidMount();
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start);
  else start();
})();
