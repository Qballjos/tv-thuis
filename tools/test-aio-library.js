const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const scripts = Array.from(fs.readFileSync('web/index.html', 'utf8').matchAll(/<script src="([^"]+)"><\/script>/g), match => match[1]);
assert(scripts.indexOf('fork-aio.js') < scripts.indexOf('data.js') && scripts.includes('fork-aio.js'), 'Load the isolated AIO module before its integration');
const source = scripts.filter(path => path === 'fork-aio.js' || path === 'data.js')
  .map(path => fs.readFileSync('web/' + path, 'utf8')).join('\n');
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

// Exercise the fork module without data.js, its storage globals, or the UI.
const directPending = [];
class DirectRequest {
  constructor() { this.headers = {}; }
  open(method, url) { this.method = method; this.url = url; }
  setRequestHeader(name, value) { this.headers[name] = value; }
  send(body) { this.body = body; directPending.push(this); }
}
const isolated = vm.createContext({XMLHttpRequest: DirectRequest});
vm.runInContext(fs.readFileSync('web/fork-aio.js', 'utf8'), isolated);
let session = '', directPicks = [];
const direct = isolated.ForkAio.create({
  prefs: {aioUrl: () => 'https://manager.invalid', aioUser: () => 'viewer', aioPassword: () => 'fixture',
    aioToken: () => session, set: (key, value) => { assert.equal(key, 'aioToken'); session = value; },
    cards: () => 20, topContent: () => 'aio'},
  translate: text => text, changed: () => {}, keyOf: p => p.kind + ':' + (p.itemId || p.id),
  sliceHour: list => list, jellyfinImage: () => '', fetchResume: done => done([]),
  picks: () => directPicks, setPicks: rows => { directPicks = rows; }
});
let authCalls = 0;
direct.ensureAio(error => { assert.equal(error, null); authCalls++; });
const login = directPending.shift();
assert.equal(login.method, 'POST');
assert.equal(login.url, 'https://manager.invalid/api/auth/login');
assert.equal(login.headers.Authorization, undefined, 'Login must not carry an old bearer token');
assert.deepEqual(JSON.parse(login.body), {username: 'viewer', password: 'fixture'});
login.status = 200; login.readyState = 4; login.responseText = '{"access_token":"session-fixture"}';
login.onreadystatechange(); login.onerror();
assert.equal(authCalls, 1, 'A late network event cannot complete login twice');
assert.equal(session, 'session-fixture');
direct.ensureAio(error => { assert.equal(error, null); authCalls++; });
assert.equal(directPending.length, 0, 'An existing session avoids another login');
let refused = 0;
direct.aioCall('GET', '/api/homepage', null, error => { assert.match(error.message, /refused the login/); refused++; });
const request = directPending.shift();
assert.equal(request.headers.Authorization, 'Bearer session-fixture');
assert.equal(request.timeout, 15000);
request.status = 401; request.readyState = 4; request.responseText = '{}';
request.onreadystatechange(); request.ontimeout();
assert.equal(session, '', 'Rejected authentication clears the stored token');
assert.equal(refused, 1);
const converted = direct.aioPick({id: uuid(303), title: 'Library', mediaType: 'Series'}, true);
assert.equal(converted.itemId, uuid(303));
assert.equal(converted.kind, 'tv');
assert.match(converted.poster, /^https:\/\/manager.invalid\/api\/homepage\/art\?.*image=Primary/);
assert.match(converted.backdrop, /image=Backdrop&fillWidth=1280&fillHeight=720/);
assert.equal(direct.sizedImage('/api/homepage/art?item_id=test&image=Primary&fillWidth=1', '', true),
  'https://manager.invalid/api/homepage/art?item_id=test&image=Backdrop&fillWidth=1280&fillHeight=720');
console.log('Standalone AIO: login/session headers, single completion, rejected-token clearing, stable UUIDs and artwork sizing passed.');
session = 'session-fixture';
let requested = false;
direct.requestTitle('tv', 456, error => { assert.equal(error, null); requested = true; });
const titleRequest = directPending.shift();
assert.equal(titleRequest.url, 'https://manager.invalid/api/homepage/request');
assert.deepEqual(JSON.parse(titleRequest.body), {mediaType: 'tv', mediaId: 456, seasons: 'all'});
titleRequest.status = 200; titleRequest.readyState = 4; titleRequest.responseText = '{"ok":true}';
titleRequest.onreadystatechange();
assert(requested);
let searchRows;
direct.searchTitles('a & b', (error, rows) => { assert.equal(error, null); searchRows = rows; });
const search = directPending.shift();
assert.match(search.url, /q=a%20%26%20b$/);
search.status = 200; search.readyState = 4;
search.responseText = JSON.stringify({results: Array.from({length: 15}, (_, n) => ({id: n + 1001, title: 'Result ' + n, can_request: true}))});
search.onreadystatechange();
assert.equal(searchRows.length, 12);
assert.equal(searchRows[0].app, 'request');
assert.equal(searchRows[0].recent, false);
console.log('Standalone AIO: TV requests preserve all-season body and searches encode queries and cap results.');
