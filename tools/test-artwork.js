const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('web/home.js', 'utf8');
const art = source.slice(source.indexOf('  function paintArt('), source.indexOf('  // As HomeView\'s bgFollow:'));
const requested = [], images = [], revoked = [];
class Image {
  set src(value) { this.url = value; images.push(this); }
}
const context = vm.createContext({Image, URL: {revokeObjectURL: url => revoked.push(url)}, Data: {
  image: path => path || '', needsAuth: url => url.startsWith('private:'),
  loadImage: (url, done) => requested.push({url, done})
}});
vm.runInContext(art, context);
const node = {style: {}};
context.paintArt(node, 'landscape', 'w1280', 'poster');
assert.equal(images[0].url, 'landscape');
images.shift().onerror();
assert.equal(images[0].url, 'poster');
images.shift().onload();
assert.equal(node.style.backgroundImage, 'url(poster)', 'Failed landscape falls back to a decoded poster');
context.paintArt(node, 'private:landscape', 'w1280', 'private:poster');
requested.shift().done('blob:invalid');
images.shift().onerror();
assert(revoked.includes('blob:invalid'));
requested.shift().done('blob:poster');
images.shift().onload();
assert.equal(node.style.backgroundImage, 'url(blob:poster)', 'Authenticated undecodable art also falls back');
context.paintArt(node, 'private:old', 'w1280', 'old-poster');
context.paintArt(node, 'new', 'w1280', 'new-poster');
images.shift().onload();
requested.shift().done('blob:stale');
assert.equal(node.style.backgroundImage, 'url(new)', 'Late responses cannot replace the current title');
assert(revoked.includes('blob:stale') && revoked.includes('blob:poster'), 'Release stale and replaced image objects');
console.log('Artwork: failed/invalid landscape fallback, authenticated posters and stale-response isolation passed.');

// The upstream title-link launch and reordered inputs must coexist with fork AIO cards.
const activated = [], notices = [];
const integration = vm.createContext({
  Z: {TOP: 1, CARDS: 2, SHELF: 3}, st: {zone: 2, card: 0}, cards: [],
  Data: {requestTitle: (kind, id, done) => { activated.push(['request', kind, id]); done(null); }, refresh: () => activated.push(['refresh'])},
  tr: text => text, toast: text => notices.push(text),
  playerApp: () => ({id: 'player', title: 'Wholphin'}),
  launcher: id => () => activated.push(['launch', id]),
  pickTarget: p => ({app: {id: p.app}}), openTarget: (p, target) => activated.push(['target', p.id, target.app.id]),
  tabCount: () => 5, openDrawer: () => {}, firstPort: () => 3, updateFocus: () => {}
});
vm.runInContext(source.slice(source.indexOf('  function activate() {'), source.indexOf('  function selectTab(')), integration);
integration.cards = [{id: 123, app: 'prime'}];
integration.activate();
assert.deepEqual(activated.shift(), ['target', 123, 'prime'], 'Ordinary picks use upstream title-link launching');
integration.cards = [{aio: true, canRequest: true, kind: 'tv', mediaId: 456}];
integration.activate();
assert.deepEqual(activated.shift(), ['request', 'tv', 456]);
assert.deepEqual(activated.shift(), ['refresh']);
assert.equal(integration.cards[0].canRequest, false);
integration.cards = [{aio: true, canRequest: false}];
integration.activate();
assert.deepEqual(activated.shift(), ['launch', 'player'], 'Library cards retain the fork player launch');
integration.st.zone = integration.Z.TOP; integration.st.top = 6;
integration.activate();
assert.equal(integration.st.drawer, 3, 'HDMI button uses upstream input ordering after five fork tabs');
console.log('Merged controls: title links, AIO requests/player and reordered HDMI focus passed.');
