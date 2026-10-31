const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

function pick(title, recent, extra) {
  return Object.assign({title, recent, aio: true, backdrop: '/test-image'}, extra);
}

function showcase(picks) {
  const cache = JSON.stringify({picks});
  const context = vm.createContext({
    window: {CONFIG: {aioUrl: 'https://media.example.invalid'}},
    TV: {zone: () => 'UTC'}, I18N: {}, navigator: {language: 'en-US'},
    localStorage: {getItem: key => key === 'fmtv.recs' ? cache : null}
  });
  vm.runInContext(fs.readFileSync('web/data.js', 'utf8'), context);
  return JSON.parse(JSON.stringify(context.Data.showcase()));
}

const alpha = pick('Alpha', false), beta = pick('Beta', false);
const library = pick('Shared - newly added', true), delta = pick('Delta', true);
const source = [alpha, pick('Shared: trending', false), beta, pick('Alpha: sequel', false),
  library, delta, pick('Delta', true), pick('Continue watching only', false, {resume: true}),
  pick('Other source', false, {aio: false}), pick('Missing art', true, {backdrop: ''})];
assert.deepEqual(showcase(source), [alpha, library, beta, delta]);
assert.equal(source.length, 10);
assert.deepEqual(showcase([alpha, beta, delta]), [alpha, delta, beta]);
assert.deepEqual(showcase([library, delta]), [library, delta]);
assert.deepEqual(showcase([alpha, beta]), [alpha, beta]);
assert.deepEqual(showcase([]), []);
console.log('Web AIO slides: alternation, recent duplicates, uneven/empty sources and missing art passed.');
