const https = require('https');

function fetchUrl(url, headers = {}) {
  return new Promise((resolve) => {
    try {
      const u = new URL(url);
      const req = https.request({
        hostname: u.hostname,
        path: u.pathname + u.search,
        method: 'GET',
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
          'Referer': 'https://doramasflix.in/',
          ...headers
        }
      }, res => {
        let body = '';
        res.on('data', c => body += c);
        res.on('end', () => {
          resolve({ status: res.statusCode, headers: res.headers, body: body.substring(0, 1000) });
        });
      });
      req.on('error', (e) => resolve({ error: e.message }));
      req.end();
    } catch (e) {
      resolve({ error: e.message });
    }
  });
}

async function testServers() {
  console.log('--- Testing Primeload Ep 9 ---');
  const primeEp9 = await fetchUrl('https://primeload.co/embed/cFOwcMExQk7Z', { 'Referer': 'https://doramasflix.in/' });
  console.log('Primeload Ep 9 status:', primeEp9.status, 'len:', primeEp9.body?.length);
  if (primeEp9.body) {
    const iframe = primeEp9.body.match(/<iframe[^>]+src=["']([^"']+)["']/i);
    console.log('Primeload iframe src:', iframe ? iframe[1] : 'No iframe found');
  }

  console.log('\n--- Testing Primeload Ep 4 ---');
  const primeEp4 = await fetchUrl('https://primeload.co/embed/j2O4HbGVjFnG', { 'Referer': 'https://doramasflix.in/' });
  console.log('Primeload Ep 4 status:', primeEp4.status, 'len:', primeEp4.body?.length);
  if (primeEp4.body) {
    const iframe = primeEp4.body.match(/<iframe[^>]+src=["']([^"']+)["']/i);
    console.log('Primeload Ep 4 iframe src:', iframe ? iframe[1] : 'No iframe found');
  }

  console.log('\n--- Testing Primeload Ep 3 ---');
  const primeEp3 = await fetchUrl('https://primeload.co/embed/NNTm4plCyvN6', { 'Referer': 'https://doramasflix.in/' });
  console.log('Primeload Ep 3 status:', primeEp3.status, 'len:', primeEp3.body?.length);
  if (primeEp3.body) {
    const iframe = primeEp3.body.match(/<iframe[^>]+src=["']([^"']+)["']/i);
    console.log('Primeload Ep 3 iframe src:', iframe ? iframe[1] : 'No iframe found');
  }
}
testServers();
