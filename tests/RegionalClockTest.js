'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
function fixture(device, language, clock, legacy) {
  const context = vm.createContext({window: {I18N_CATALOGUES: {nl: JSON.parse(fs.readFileSync('app/assets/i18n/nl.json', 'utf8'))}},
    navigator: {language: device}, document: {documentElement: {}}, Intl: legacy ? undefined : Intl});
  vm.runInContext(fs.readFileSync('web/strings.js', 'utf8'), context);
  context.I18N.set(language); context.I18N.setClock(clock);
  return context.I18N;
}
const at = new Date(2026, 10, 12, 19, 8).getTime();
let clock = fixture('en-GB', 'en', 'auto');
assert.equal(clock.regionalClock(at), '19:08', 'British region uses 24 hours even with the English catalogue');
assert.match(clock.regionalDate(at), /12 November/, 'British dates put day before month');
clock = fixture('en-US', 'en', 'auto');
assert.match(clock.regionalClock(at), /7:08.*PM/, 'US region uses 12 hours and day period');
assert.match(clock.regionalDate(at), /November 12/, 'US dates put month before day');
clock = fixture('nl-NL', 'en', 'auto');
assert.equal(clock.regionalClock(at), '19:08', 'English UI preserves the TV region');
clock = fixture('en-US', 'nl', 'auto');
assert.match(clock.regionalClock(at), /^7:08/, 'Dutch UI must preserve a US TV automatic 12-hour cycle');
clock = fixture('en-GB', 'en', '12');
assert.match(clock.regionalClock(at), /7:08.*pm/i, 'Explicit 12-hour choice wins over region');
clock = fixture('en-US', 'en', '24');
assert.equal(clock.regionalClock(at), '19:08', 'Explicit 24-hour choice wins over region');
clock = fixture('en-GB', 'en', 'auto', true);
assert.equal(clock.regionalClock(at), '19:08', 'Legacy TV without Intl preserves British time format');
clock = fixture('en-AU', 'en', 'auto', true);
assert.match(clock.regionalClock(at), /7:08.*PM/, 'Legacy Australian region keeps its 12-hour clock');
console.log('Regional clock: GB/US/NL regions, date order, manual overrides and legacy fallback passed.');
