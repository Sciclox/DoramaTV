const https = require('https');
const fs = require('fs');

function postGql(query, variables) {
    return new Promise((resolve, reject) => {
        const payload = JSON.stringify({ query, variables });
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
            res.on('end', () => {
                try {
                    resolve(JSON.parse(data));
                } catch(e) {
                    resolve({});
                }
            });
        });
        req.on('error', reject);
        req.write(payload);
        req.end();
    });
}

function decodeB64(str) {
    return Buffer.from(str.replace(/-/g, '+').replace(/_/g, '/'), 'base64').toString('utf8');
}

function decodeJwt(jwtUrl) {
    if (!jwtUrl || !jwtUrl.includes('/e/')) return jwtUrl;
    const token = jwtUrl.split('/e/')[1].split('&')[0];
    const parts = token.split('.');
    if (parts.length < 2) return null;
    try {
        const payload = JSON.parse(decodeB64(parts[1]));
        if (payload.link) return decodeB64(payload.link);
    } catch(e) {}
    return null;
}

async function run() {
    const epsRes = await postGql('query Eps($id: ID!) { paginationEpisode(page: 1, limit: 35, filter: { serie_id: $id, season_number: 1 }, sort: NUMBER_ASC) { items { _id episode_number slug } } }', { id: '670b74efa35a6c33cfba1425' });
    const items = epsRes.data?.paginationEpisode?.items || [];
    console.log('Got', items.length, 'episodes');
    items.sort((a,b) => a.episode_number - b.episode_number);
    const result = [];
    for (let i = 0; i < items.length; i++) {
        const ep = items[i];
        await new Promise(r => setTimeout(r, 600));
        const webLinks = await postGql('query Links($id: ID!) { getEpisodeLinks(id: $id) { links_online { server lang link } } }', { id: ep._id });
        await new Promise(r => setTimeout(r, 600));
        const appLinks = await postGql('query LinksApp($id: ID!) { getEpisodeLinks(id: $id, app: "com.asiapp.doramasgo") { links_online { server lang link } } }', { id: ep._id });
        
        const all = [
            ...(webLinks.data?.getEpisodeLinks?.links_online || []),
            ...(appLinks.data?.getEpisodeLinks?.links_online || [])
        ];
        const srvs = all.map(l => ({ server: l.server, url: decodeJwt(l.link) }));
        const summary = {
            ep: ep.episode_number,
            hasOkRu: srvs.some(s => s.server === '1113' || (s.url && s.url.includes('ok.ru'))),
            hasVoe: srvs.some(s => s.server === '1230' || (s.url && s.url.includes('voe.sx'))),
            hasFilemoon: srvs.some(s => s.server === '958695' || (s.url && (s.url.includes('filemoon') || s.url.includes('byse')))),
            hasStreamtape: srvs.some(s => s.server === '8309' || (s.url && s.url.includes('streamtape'))),
            hasDood: srvs.some(s => s.server === '7286' || (s.url && (s.url.includes('dood') || s.url.includes('do7go')))),
            hasPrimeload: srvs.some(s => s.server === '4721' || (s.url && s.url.includes('primeload'))),
            hasVidhide: srvs.some(s => s.server === '576857' || (s.url && s.url.includes('callistanise')))
        };
        console.log(`Cap ${ep.episode_number}: Ok.ru=${summary.hasOkRu}, VOE=${summary.hasVoe}, Streamtape=${summary.hasStreamtape}, Filemoon=${summary.hasFilemoon}`);
        result.push(summary);
    }
    fs.writeFileSync('kill_me_love_me_servers.json', JSON.stringify(result, null, 2));
    console.log('DONE');
}
run();
