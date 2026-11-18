const https = require("https");

function decodeEmbedJwt(jwtUrl) {
  try {
    if (!jwtUrl.includes("/e/")) return jwtUrl;
    const token = jwtUrl.split("/e/")[1].split("&")[0];
    const parts = token.split(".");
    if (parts.length < 2) return null;
    let b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    while (b64.length % 4) b64 += '=';
    const jsonStr = Buffer.from(b64, 'base64').toString('utf-8');
    const json = JSON.parse(jsonStr);
    if (json.link) {
      let lB64 = json.link.replace(/-/g, '+').replace(/_/g, '/');
      while (lB64.length % 4) lB64 += '=';
      return Buffer.from(lB64, 'base64').toString('utf-8');
    }
    return null;
  } catch(e) {
    return null;
  }
}

function postGraphQL(query, variables, opName) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify({
      operationName: opName,
      query,
      variables
    });
    const req = https.request({
      hostname: "user-api.seriesapi.co",
      path: "/graphql",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Origin": "https://doramasflix.in",
        "Referer": "https://doramasflix.in/",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
      }
    }, (res) => {
      let data = "";
      res.on("data", c => data += c);
      res.on("end", () => {
        try {
          resolve(JSON.parse(data));
        } catch(e) {
          reject(e);
        }
      });
    });
    req.on("error", reject);
    req.write(body);
    req.end();
  });
}

async function main() {
  const doramaId = "670b74efa35a6c33cfba1425";

  const epQuery = `
    query EpisodesPagination($page: Int!, $serie_id: ID!, $season_number: Int!, $limit: Int!) {
      paginationEpisode(page: $page, limit: $limit, filter: { serie_id: $serie_id, season_number: $season_number }, sort: NUMBER_ASC) {
        count
        items {
          _id
          name
          slug
          episode_number
          season_number
        }
      }
    }
  `;
  const eps = await postGraphQL(epQuery, { serie_id: doramaId, season_number: 1, page: 1, limit: 35 }, "EpisodesPagination");
  const items = eps.data?.paginationEpisode?.items || [];
  
  for (const ep of items.slice(0, 5)) {
    console.log(`\n=================== Episode ${ep.episode_number} (_id: ${ep._id}) ===================`);
    const linksQuery = `
      query EpisodeLinksOnlineWeb($episode_id: ID!) {
        getEpisodeLinks(id: $episode_id) {
          links_online {
            server
            lang
            link
            _id
            is_recommended
          }
        }
      }
    `;
    const links = await postGraphQL(linksQuery, { episode_id: ep._id }, "EpisodeLinksOnlineWeb");
    const webLinks = links.data?.getEpisodeLinks?.links_online || [];
    console.log(`Web links (${webLinks.length}):`);
    for (const l of webLinks) {
      const decoded = decodeEmbedJwt(l.link) || l.link;
      console.log(`  [Server: ${l.server}] -> ${decoded}`);
    }

    await new Promise(r => setTimeout(r, 600));

    const appLinksQuery = `
      query EpisodeLinksOnlineApp($episode_id: ID!) {
        getEpisodeLinks(id: $episode_id, app: "com.asiapp.doramasgo") {
          links_online {
            server
            lang
            link
            _id
            is_recommended
          }
        }
      }
    `;
    const appLinks = await postGraphQL(appLinksQuery, { episode_id: ep._id }, "EpisodeLinksOnlineApp");
    const appL = appLinks.data?.getEpisodeLinks?.links_online || [];
    console.log(`App links (${appL.length}):`);
    for (const l of appL) {
      const decoded = decodeEmbedJwt(l.link) || l.link;
      console.log(`  [Server: ${l.server}] -> ${decoded}`);
    }
  }
}

main().catch(console.error);
