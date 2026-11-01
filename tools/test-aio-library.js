const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const source = fs.readFileSync('web/data.js', 'utf8');
const uuid = n => '00000000-0000-4000-8000-' + String(n).padStart(12, '0');
const row = n => ({id: uuid(n), title: 'Franchise: Part ' + n, mediaType: 'movie', poster: '/poster'});

function fixture(cached = []) {
  const pending = [];
  let time = 1_000_000;
  const stored = {'fmtv.recs': JSON.stringify({picks: cached}), 'fmtv.prefs': JSON.stringify({topContent: 'aio', aioUrl: 'https://media.example.invalid', aioToken: 'fixture-only'})};
  class Clock extends Date { static now() { return time; } }
  class Request {
    open(method, url) { this.url = url; }
    setRequestHeader() {}
    send() { pending.push(this); }
  }
  const context = vm.createContext({window: {}, TV: {zone: () => 'UTC'}, I18N: {}, navigator: {language: 'en-US'}, Date: Clock,
    XMLHttpRequest: Request, localStorage: {getItem: key => stored[key] || null, setItem: (key, value) => { stored[key] = value; }}});
  vm.runInContext(source, context);
  return {context, pending, advance: () => { time += 30_001; }, reply(items, status = 200) {
    assert(pending.length);
    const request = pending.shift();
    request.status = status; request.readyState = 4; request.responseText = JSON.stringify({items});
    request.onreadystatechange();
    return request.url;
  }};
}

const f = fixture();
assert.equal(f.context.Data.picks('movie').length, 0);
f.context.Data.picks('movie'); f.context.Data.moreLibrary('movie');
assert.equal(f.pending.length, 1, 'Only one request may be in flight per type');
assert.match(f.reply(Array.from({length: 20}, (_, i) => row(i))), /mediaType=movie&page=1/);
assert.equal(f.context.Data.picks('movie').length, 20);
assert.equal(f.pending.length, 0, 'Reading a loaded page must not fetch the entire catalogue');
f.context.Data.moreLibrary('movie');
f.reply(Array.from({length: 20}, (_, i) => row(i + 20)));
f.context.Data.moreLibrary('movie');
f.reply([row(39), row(40), row(41), row(42), row(43), row(44)]);
assert.equal(f.context.Data.picks('movie').length, 45, 'Keep sequels and append beyond 40; remove duplicate IDs');
f.context.Data.moreLibrary('movie');
assert.equal(f.pending.length, 0, 'Short final page ends pagination');
f.context.Data.picks('tv');
f.context.Prefs.set('aioUrl', 'https://other.example.invalid');
f.reply([{...row(100), mediaType: 'tv'}]);
assert.equal(f.context.Data.picks('tv').length, 0, 'A stale response cannot cross server identities');
f.reply([{...row(101), mediaType: 'tv'}]);
assert.equal(f.context.Data.picks('tv')[0].itemId, uuid(101));

const failed = fixture();
failed.context.Data.picks('movie'); failed.reply([], 503);
failed.context.Data.picks('movie'); failed.context.Data.moreLibrary('movie');
assert.equal(failed.pending.length, 0, 'Failed pages back off instead of looping on every render');
failed.advance(); failed.context.Data.moreLibrary('movie');
assert.equal(failed.pending.length, 1);
console.log('Web library: lazy pages, >40 titles, sequels, duplicate IDs, completion, identity changes and failure backoff passed.');

const recent = fixture([
  {aio: true, recent: true, title: 'Saga: Part 1', id: 1, kind: 'movie', addedAt: 1000},
  {aio: true, recent: true, title: 'Saga: Part 2', id: 2, kind: 'movie', addedAt: 3000},
  {aio: true, recent: true, title: 'Saga: Part 3', id: 3, kind: 'movie', addedAt: 2000},
  {aio: true, recent: true, title: 'Unknown', id: 4, kind: 'movie'}
]);
assert.deepEqual(Array.from(recent.context.Data.picks('recent'), p => p.id), [2, 3, 1, 4]);
recent.advance();
assert.deepEqual(Array.from(recent.context.Data.picks('recent'), p => p.id), [2, 3, 1, 4], 'Recent order never rotates or drops sequels');
assert.equal(recent.context.Data.image('/poster.jpg', 'w780'), 'https://image.tmdb.org/t/p/w780/poster.jpg');
assert.equal(recent.context.Data.image('/api/homepage/art?item_id=fixture', 'w1280').indexOf('https://media.example.invalid/api/'), 0);
console.log('Recent: newest first, unknown dates last, sequels retained, no hourly rotation; TMDB/AIO image origins passed.');

const dates = fixture();
dates.context.Data.picks('movie');
dates.reply([
  {...row(201), when: '2023-11-14T22:13:20'},
  {...row(202), when: '1700000000'},
  {...row(203), when: '2023-11-14T23:13:20+01:00'}
]);
assert.deepEqual(Array.from(dates.context.Data.picks('movie'), p => p.addedAt), [1700000000000, 1700000000000, 1700000000000]);
