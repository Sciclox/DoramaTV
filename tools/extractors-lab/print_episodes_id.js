async function run() {
    const epGql = `query EpisodesPagination($page: Int!, $serie_id: ID!, $season_number: Int!, $limit: Int!) {
      paginationEpisode(page: $page, limit: $limit, filter: { serie_id: $serie_id, season_number: $season_number }, sort: NUMBER_ASC) {
        items { _id episode_number name slug }
      }
    }`;
    const epRes = await fetch('https://user-api.seriesapi.co/graphql', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query: epGql, variables: { page: 1, limit: 10, serie_id: '670b74efa35a6c33cfba1425', season_number: 1 } })
    }).then(r => r.json());

    const episodes = epRes?.data?.paginationEpisode?.items || [];
    episodes.forEach(e => {
        console.log(`Ep ${e.episode_number} (${e.name}): ID=${e._id}, slug=${e.slug}`);
    });
}
run();
