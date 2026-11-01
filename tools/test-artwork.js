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
