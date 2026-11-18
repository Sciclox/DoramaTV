const https = require('https');

const payload = JSON.stringify({
  query: `query DetailDoramaSlug($slug: String!) {
    detailDorama(filter: { slug: $slug }) {
      _id
      name
      slug
    }
  }`,
  variables: { slug: 'kill-me-love-me' }
});

const req = https.request('https://user-api.seriesapi.co/graphql', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Origin': 'https://doramasflix.in',
    'Referer': 'https://doramasflix.in/'
  }
}, res => {
  let data = '';
  res.on('data', c => data += c);
  res.on('end', () => console.log('Detail result:', data));
});
req.write(payload);
req.end();
