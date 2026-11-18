const https = require('https');
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
            res.on('end', () => resolve(JSON.parse(data)));
        });
        req.on('error', reject);
        req.write(payload);
        req.end();
    });
}

(async () => {
    const epsRes = await postGql('query Eps($id: ID!) { paginationEpisode(page: 1, limit: 35, filter: { serie_id: $id, season_number: 1 }, sort: NUMBER_ASC) { items { _id episode_number slug } } }', { id: '670b74efa35a6c33cfba1425' });
    const ep9 = epsRes.data.paginationEpisode.items.find(e => e.episode_number === 9);
    const linksRes = await postGql('query Links($id: ID!) { getEpisodeLinks(id: $id) { links_online { server lang link is_recommended } } }', { id: ep9._id });
    console.log(JSON.stringify(linksRes, null, 2));
})();
