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
async function run() {
    const res = await postGql('query Links($id: ID!) { getEpisodeLinks(id: $id) { links_online { server lang link is_recommended _id } } }', { id: '670b752ba35a6c33cfba145b' });
    console.log(JSON.stringify(res, null, 2));
}
run();
