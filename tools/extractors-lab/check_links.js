const https = require('https');

function checkEp(slug) {
  return new Promise((resolve) => {
    https.get('https://doramasflix.in/episodios/' + slug, {
      headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' }
    }, res => {
      let html = '';
      res.on('data', c => html += c);
      res.on('end', () => {
        const idx = html.indexOf('<script id="__NEXT_DATA__"');
        if (idx !== -1) {
          const start = html.indexOf('>', idx) + 1;
          const end = html.indexOf('</script>', start);
          const data = JSON.parse(html.substring(start, end));
          const ep = data.props?.pageProps?.episode;
          console.log('=== SLUG:', slug, '===');
          if (ep) {
            console.log('links_online count:', ep.links_online?.length);
            ep.links_online?.forEach(l => {
              console.log('  server:', l.server, 'lang:', l.lang, 'link:', l.link);
            });
          }
        }
        resolve();
      });
    });
  });
}

(async () => {
  await checkEp('kill-me-love-me-1x5');
  await checkEp('kill-me-love-me-1x8');
  await checkEp('kill-me-love-me-1x9');
})();
