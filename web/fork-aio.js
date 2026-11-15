/* Fork-owned AIO transport, artwork, rails and paged library. ES5 for older TVs.
 * create receives preferences plus callbacks into the shared recommendation host.
 * No DOM or persistent cache ownership: the host supplies and saves its picks.
 */
var ForkAio = (function () {
  function create(options) {
    var Prefs = options.prefs, changed = options.changed, trSafe = options.translate;
    var keyOf = options.keyOf, sliceHour = options.sliceHour;
    var jellyfinImage = options.jellyfinImage, fetchResume = options.fetchResume;
    var libraries = {}, libraryStamp = '';

    function aioOn() { return !!Prefs.aioUrl(); }

    function topAio() { return aioOn() && Prefs.topContent() === 'aio'; }

    function libraryIdentity() { return aioBase() + ':' + Prefs.aioUser() + ':' + Prefs.aioToken(); }

    function libraryItems(previous, next, kind) {
      var out = [], seen = {}, list = previous.concat(next);
      for (var i = 0; i < list.length; i++) {
        var p = list[i], id = 'id:' + keyOf(p);
        if (p.aio && p.recent && !p.resume && p.kind === kind && !seen[id]) { seen[id] = true; out.push(p); }
      }
      return out;
    }

    function libraryState(kind) {
      var identity = libraryIdentity();
      if (identity !== libraryStamp) { libraries = {}; libraryStamp = identity; }
      if (!libraries[kind]) libraries[kind] = {items: [], page: 0, loading: false, end: false, failedAt: 0};
      return libraries[kind];
    }

    function libraryRows(kind) {
      var state = libraryState(kind);
      if (!state.page) moreLibrary(kind);
      return state.items;
    }

    function moreLibrary(kind) {
      if (!aioOn() || (kind !== 'movie' && kind !== 'tv')) return;
      var state = libraryState(kind), identity = libraryStamp, sinceFail = Date.now() - state.failedAt;
      if (state.loading || state.end || (state.failedAt && sinceFail >= 0 && sinceFail < 30000)) return;
      state.loading = true;
      ensureAio(function (error) {
        if (error) { finish(error); return; }
        aioCall('GET', '/api/homepage/recent?mediaType=' + kind + '&page=' + (state.page + 1), null, finish);
      });
      function finish(error, data) {
        state.loading = false;
        if (libraries[kind] !== state || identity !== libraryIdentity()) { changed(); return; }
        var rows = data && data.items || [], next = [];
        if (error || (data && data.error && !rows.length)) { state.failedAt = Date.now(); changed(); return; }
        for (var i = 0; i < rows.length; i++) if (rows[i].title) next.push(aioPick(rows[i], true));
        state.items = libraryItems(state.page ? state.items : [], next, kind);
        state.page++;
        state.end = rows.length < 20 || state.page >= 500;
        state.failedAt = 0;
        changed();
      }
    }

    function aioRows(kind) {
      var picks = options.picks(), list = [];
      for (var a = 0; a < picks.length; a++) {
        var item = picks[a];
        if (!item.aio) continue;
        if (kind === 'resume') { if (item.resume) list.push(item); }
        else if (kind === 'recent' || kind === null) { if (item.recent && !item.resume) list.push(item); }
        else if (!item.recent && !item.resume && (kind === 'trending' || item.kind === kind)) list.push(item);
      }
      if (kind === 'resume') return list.slice(0, Prefs.cards());
      if (kind === 'recent' || kind === null) return newest(list).slice(0, Prefs.cards());
      return sliceHour(list);
    }

    function additionTime(raw) {
      var text = String(raw || ''), n;
      if (/^[0-9]{9,16}$/.test(text)) { n = Number(text); return n < 100000000000 ? n * 1000 : n; }
      if (/^\d{4}-\d\d-\d\dT\d\d:\d\d(?::\d\d(?:\.\d+)?)?$/.test(text)) text += 'Z';
      n = Date.parse(text);
      return isNaN(n) ? 0 : n;
    }

    function newest(list) {
      var indexed = [], i;
      for (i = 0; i < list.length; i++) indexed.push({item: list[i], index: i});
      indexed.sort(function (a, b) { return (b.item.addedAt || 0) - (a.item.addedAt || 0) || a.index - b.index; });
      var out = [];
      for (i = 0; i < indexed.length; i++) out.push(indexed[i].item);
      return out;
    }

    function aioBase() { return Prefs.aioUrl(); }

    function aioCall(method, path, body, done, authed) {
      var x = new XMLHttpRequest();
      var over = false;
      x.open(method, aioBase() + path, true);
      x.timeout = 15000;
      x.setRequestHeader('Accept', 'application/json');
      if (authed !== false && Prefs.aioToken()) x.setRequestHeader('Authorization', 'Bearer ' + Prefs.aioToken());
      if (body) x.setRequestHeader('Content-Type', 'application/json');
      x.onreadystatechange = function () {
        if (x.readyState !== 4 || over) return;
        over = true;
        var json = null;
        try { json = JSON.parse(x.responseText || '{}'); } catch (e) { json = null; }
        if (x.status === 401) { Prefs.set('aioToken', ''); done(new Error(trSafe('The media manager refused the login.'))); return; }
        if (x.status < 200 || x.status >= 300) {
          var detail = json && json.detail ? json.detail : ('HTTP ' + x.status);
          done(new Error(detail));
          return;
        }
        done(null, json || {});
      };
      x.ontimeout = x.onerror = function () { if (!over) { over = true; done(new Error('no connection')); } };
      x.send(body ? JSON.stringify(body) : null);
    }

    function ensureAio(done) {
      if (!aioOn()) { done(new Error(trSafe('Fill in the server, username and password.'))); return; }
      if (Prefs.aioToken()) { done(null); return; }
      if (!Prefs.aioUser() || !Prefs.aioPassword()) { done(new Error(trSafe('Fill in the server, username and password.'))); return; }
      aioCall('POST', '/api/auth/login', {username: Prefs.aioUser(), password: Prefs.aioPassword()}, function (err, res) {
        if (err) { done(err); return; }
        if (!res.access_token) { done(new Error(trSafe('The media manager did not return a session.'))); return; }
        Prefs.set('aioToken', res.access_token);
        done(null);
      }, false);
    }

    function artUrl(itemId, wide) {
      if (!itemId) return '';
      if (aioBase()) {
        return aioBase() + '/api/homepage/art?source=jellyfin&item_id=' + itemId
          + (wide ? '&image=Backdrop&fillWidth=1280&fillHeight=720' : '&image=Primary&fillWidth=780&fillHeight=1170');
      }
      return jellyfinImage(itemId, wide ? 'Backdrop' : 'Primary', wide ? 'maxWidth=1280' : 'maxHeight=780');
    }

    function artQuery(url, wide) {
      var q = url.indexOf('?');
      var base = q >= 0 ? url.slice(0, q) : url;
      var query = q >= 0 ? url.slice(q + 1) : '';
      var parts = query ? query.split('&') : [];
      var kept = [];
      for (var i = 0; i < parts.length; i++) {
        if (!parts[i] || parts[i].indexOf('image=') === 0 || parts[i].indexOf('fillWidth=') === 0 || parts[i].indexOf('fillHeight=') === 0) continue;
        kept.push(parts[i]);
      }
      kept.push('image=' + (wide ? 'Backdrop' : 'Primary'));
      kept.push(wide ? 'fillWidth=1280' : 'fillWidth=780');
      kept.push(wide ? 'fillHeight=720' : 'fillHeight=1170');
      return base + '?' + kept.join('&');
    }

    /** Prefer supplied artwork; invent Jellyfin artwork only through the manager. */
    function sizedImage(raw, itemId, wide) {
      if (!raw) return artUrl(itemId, wide);
      var url = raw;
      if (url.charAt(0) === '/') url = aioBase() + url;
      if (url.indexOf('/api/homepage/art') >= 0) return artQuery(url, wide);
      var size = wide ? '/w1280/' : '/w780/';
      return url.replace('/w92/', size).replace('/w154/', size).replace('/w185/', size)
        .replace('/w342/', size).replace('/w500/', size).replace('/w780/', size).replace('/original/', size);
    }

    function isUuid(id) {
      return /^[0-9a-fA-F]{32}$/.test(String(id || '').replace(/-/g, ''));
    }

    function libraryId(row) {
      var id = row.id == null ? '' : String(row.id);
      if (!isUuid(id)) {
        var url = String(row.url || ''), q = url.indexOf('id=');
        if (q >= 0) {
          id = url.slice(q + 3);
          var cut = id.search(/[&#]/);
          if (cut >= 0) id = id.slice(0, cut);
        }
      }
      return isUuid(id) ? id : '';
    }

    function aioPick(row, recent) {
      var kind = (row.mediaType || 'movie').toLowerCase();
      if (kind === 'series' || kind === 'show') kind = 'tv';
      if (kind !== 'tv') kind = 'movie';
      var itemId = libraryId(row);
      var id = row.mediaId || (itemId ? 0 : row.id) || 0;
      var portrait = sizedImage(row.poster || '', itemId, false);
      var landscape = sizedImage(row.backdrop || '', itemId, true);
      return {
        aio: true, recent: recent, kind: kind, id: id, itemId: itemId,
        addedAt: additionTime(row.when || row.DateCreated || row.addedAt),
        title: row.title || '',
        meta: row.detail || row.status || '',
        poster: portrait || landscape,
        backdrop: landscape || portrait,
        canRequest: !recent && !!(row.can_request || row.canRequest),
        mediaType: kind, mediaId: row.mediaId || id,
        detailed: true, app: recent || !(row.can_request || row.canRequest) ? 'library' : 'request'
      };
    }

    function fetchAio(done) {
      ensureAio(function (err) {
        if (err) { done(err); return; }
        aioCall('GET', '/api/homepage', null, function (e, home) {
          if (e) { done(e); return; }
          var list = [], seen = {};
          function addRows(rows, recent) {
            for (var i = 0; rows && i < rows.length; i++) {
              if (!rows[i].title) continue;
              var p = aioPick(rows[i], recent);
              var key = (recent ? 'r:' : 't:') + (p.itemId || (p.id > 1000 ? p.kind + ':' + p.id : p.kind + ':' + p.title));
              if (seen[key]) continue;
              seen[key] = true;
              list.push(p);
            }
          }
          function countKind(kind, recent) {
            var n = 0;
            for (var i = 0; i < list.length; i++) if (list[i].recent === recent && list[i].kind === kind) n++;
            return n;
          }
          addRows(home.trending, false);
          addRows(home.recent, true);
          var page = 2;
          function moreTrending() {
            if (page > 8 || (countKind('movie', false) >= 40 && countKind('tv', false) >= 40)) { moreRecent(1); return; }
            aioCall('GET', '/api/homepage/trending?page=' + page, null, function (err2, more) {
              var got = !err2 && more && more.items ? more.items.length : 0;
              if (got) addRows(more.items, false);
              if (err2 || !got) { moreRecent(1); return; }
              page++;
              moreTrending();
            });
          }
          function moreRecent(rp) {
            var n = 0;
            for (var i = 0; i < list.length; i++) if (list[i].recent) n++;
            if (rp > 4 || n >= 40) { finish(); return; }
            aioCall('GET', '/api/homepage/recent?page=' + rp, null, function (err3, more) {
              var got = !err3 && more && more.items ? more.items.length : 0;
              if (got) addRows(more.items, true);
              if (err3 || !got) { finish(); return; }
              moreRecent(rp + 1);
            });
          }
          function finish() {
            fetchResume(function (rows) {
              if (rows) for (var r = 0; r < rows.length; r++) list.push(rows[r]);
              if (!list.length) { done(new Error(trSafe('The media manager returned no titles.'))); return; }
              options.setPicks(list);
              libraries = {};
              done(null);
            });
          }
          moreTrending();
        });
      });
    }

    function requestTitle(kind, id, done) {
      ensureAio(function (err) {
        if (err) { done(err); return; }
        var body = {mediaType: kind === 'tv' ? 'tv' : 'movie', mediaId: id};
        if (body.mediaType === 'tv') body.seasons = 'all';
        aioCall('POST', '/api/homepage/request', body, function (e, res) {
          if (e) { done(e); return; }
          if (res && res.ok === false) { done(new Error(res.detail || 'refused')); return; }
          done(null);
        });
      });
    }

    function ping(done) {
      Prefs.set('aioToken', '');
      ensureAio(function (err) {
        if (err) { done(err); return; }
        aioCall('GET', '/api/homepage', null, function (e, home) {
          if (e) done(e);
          else done(null, (home.trending || []).length, (home.recent || []).length);
        });
      });
    }

    function searchTitles(query, done) {
      ensureAio(function (err) {
        if (err) { done(err); return; }
        aioCall('GET', '/api/homepage/search?provider=seerr&q=' + encodeURIComponent(query), null, function (e, res) {
          if (e) { done(e); return; }
          var rows = (res && res.results) || [];
          var out = [];
          for (var i = 0; i < rows.length && out.length < 12; i++) out.push(aioPick(rows[i], false));
          done(null, out);
        });
      });
    }

    return {
      requestTitle: requestTitle,
      ping: ping,
      searchTitles: searchTitles,
      aioOn: aioOn,
      topAio: topAio,
      libraryRows: libraryRows,
      moreLibrary: moreLibrary,
      aioRows: aioRows,
      aioBase: aioBase,
      aioCall: aioCall,
      ensureAio: ensureAio,
      sizedImage: sizedImage,
      isUuid: isUuid,
      aioPick: aioPick,
      fetchAio: fetchAio
    };
  }
  return {create: create};
})();
