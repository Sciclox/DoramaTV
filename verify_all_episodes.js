const https = require("https");
const http = require("http");

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function pureB64Decode(clean) {
  clean = clean.replace(/[\r\n=]/g, '').trim();
  const isUrl = clean.includes('-') || clean.includes('_');
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  const alphabet = isUrl ? chars + "-_" : chars + "+/";
  const fallback = isUrl ? chars + "+/" : chars + "-_";
  const out = [];
  let buffer = 0, bitsLeft = 0;
  for (let i = 0; i < clean.length; i++) {
    const c = clean[i];
    let idx = alphabet.indexOf(c);
    if (idx < 0) idx = fallback.indexOf(c);
    if (idx < 0) continue;
    buffer = (buffer << 6) | idx;
    bitsLeft += 6;
    if (bitsLeft >= 8) {
      bitsLeft -= 8;
      out.push((buffer >> bitsLeft) & 0xFF);
    }
  }
  return Buffer.from(out).toString('utf8');
}

function decodeJwt(token) {
  try {
    if (!token.includes('/e/')) return token;
    const jwt = token.split('/e/')[1].split('&')[0];
    const parts = jwt.split('.');
    if (parts.length < 2) return token;
    const jsonStr = pureB64Decode(parts[1]);
    const parsed = JSON.parse(jsonStr);
    if (parsed.link) {
      return pureB64Decode(parsed.link);
    }
    return token;
  } catch (e) {
    return token;
  }
}

async function postGraphQL(query, variables, opName, retries = 5) {
  for (let attempt = 1; attempt <= retries; attempt++) {
    try {
      const res = await new Promise((resolve, reject) => {
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
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
          }
        }, (resp) => {
          let data = "";
          resp.on("data", c => data += c);
          resp.on("end", () => {
            try {
              resolve(JSON.parse(data));
            } catch (e) {
              resolve({ error: data });
            }
          });
        });
        req.on("error", reject);
        req.setTimeout(8000, () => {
          req.destroy();
          reject(new Error("Timeout"));
        });
        req.write(body);
        req.end();
      });

      if (res.errors && JSON.stringify(res.errors).includes("ThrottlerException")) {
        // Throttled: back off with jitter
        const waitTime = attempt * 2000;
        await sleep(waitTime);
        continue;
      }
      return res;
    } catch (err) {
      if (attempt === retries) throw err;
      await sleep(1500);
    }
  }
  return null;
}

function fetchUrl(url, headers = {}, maxRedirects = 3) {
  return new Promise((resolve) => {
    try {
      const parsedUrl = new URL(url);
      const isHttps = parsedUrl.protocol === "https:";
      const client = isHttps ? https : http;

      const defaultHeaders = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Referer": "https://doramasflix.in/",
        ...headers
      };

      const req = client.request({
        hostname: parsedUrl.hostname,
        port: parsedUrl.port || (isHttps ? 443 : 80),
        path: parsedUrl.pathname + parsedUrl.search,
        method: "GET",
        headers: defaultHeaders
      }, (res) => {
        if ([301, 302, 303, 307, 308].includes(res.statusCode) && res.headers.location && maxRedirects > 0) {
          const redirectUrl = new URL(res.headers.location, url).toString();
          return resolve(fetchUrl(redirectUrl, headers, maxRedirects - 1));
        }

        let body = "";
        res.on("data", chunk => {
          if (body.length < 250000) body += chunk;
        });
        res.on("end", () => {
          resolve({ statusCode: res.statusCode, body, headers: res.headers });
        });
      });

      req.on("error", (err) => resolve({ error: err.message, statusCode: 0 }));
      req.setTimeout(7000, () => {
        req.destroy();
        resolve({ error: "timeout", statusCode: 0 });
      });
      req.end();
    } catch (e) {
      resolve({ error: e.message, statusCode: 0 });
    }
  });
}

function unescapeHtml(input) {
  return input
    .replace(/&quot;/g, '"')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&#39;/g, "'")
    .replace(/&apos;/g, "'")
    .replace(/&#34;/g, '"')
    .replace(/&#38;/g, '&')
    .replace(/&#60;/g, '<')
    .replace(/&#62;/g, '>');
}

async function verifyOkRu(url) {
  try {
    const res = await fetchUrl(url, { Referer: "https://doramasflix.in/" });
    if (!res || res.statusCode !== 200 || !res.body) return false;
    if (res.body.includes("video_not_found") || res.body.includes("Video has been blocked") || res.body.includes("not available")) {
      return false;
    }
    const match = res.body.match(/data-options="([^"]+)"/);
    if (!match) return false;
    const json = JSON.parse(unescapeHtml(match[1]));
    const metadata = json.flashvars?.metadata;
    if (!metadata) return false;
    const metaObj = typeof metadata === "string" ? JSON.parse(metadata) : metadata;
    return !!(metaObj.hlsManifestUrl || (metaObj.videos && metaObj.videos.length > 0));
  } catch (e) {
    return false;
  }
}

async function verifyVoe(url) {
  try {
    let res = await fetchUrl(url, { Referer: "https://voe.sx/" });
    if (!res || !res.body) return false;
    if (res.statusCode === 404 || res.body.includes("File not found") || res.body.includes("404 Not Found") || res.body.includes("deleted")) {
      return false;
    }
    // Check redirect
    const redir = res.body.match(/window\.location(?:\.href)?\s*=\s*['"](https?:\/\/[^'"]+)['"]/);
    if (redir) {
      res = await fetchUrl(redir[1], { Referer: "https://voe.sx/" });
      if (!res || !res.body) return false;
    }
    return !res.body.includes("File not found") && !res.body.includes("404") && (res.body.includes(".m3u8") || res.body.includes("sources") || res.body.includes("player"));
  } catch (e) {
    return false;
  }
}

async function verifyGenericEmbed(url) {
  try {
    const res = await fetchUrl(url, { Referer: "https://doramasflix.in/" });
    if (!res || !res.body) return false;
    if (res.statusCode >= 400) return false;
    const lower = res.body.toLowerCase();
    if (lower.includes("file was deleted") || lower.includes("file not found") || lower.includes("404 not found") || lower.includes("video not found") || lower.includes("has been removed")) {
      return false;
    }
    return true;
  } catch (e) {
    return false;
  }
}

async function verifyServer(serverName, url) {
  if (serverName === "Ok.ru") {
    return await verifyOkRu(url);
  } else if (serverName === "VOE") {
    return await verifyVoe(url);
  } else {
    return await verifyGenericEmbed(url);
  }
}

async function main() {
  const doramaId = "670b74efa35a6c33cfba1425";

  console.log("=== INICIANDO AUDITORÍA DE CAPÍTULOS DE 'KILL ME LOVE ME' ===");

  const epQuery = `
    query EpisodesPagination($page: Int!, $serie_id: ID!, $season_number: Int!, $limit: Int!) {
      paginationEpisode(page: $page, limit: $limit, filter: { serie_id: $serie_id, season_number: $season_number }, sort: NUMBER_ASC) {
        count
        items {
          _id
          episode_number
          name
        }
      }
    }
  `;
  const eps = await postGraphQL(epQuery, { serie_id: doramaId, season_number: 1, page: 1, limit: 35 }, "EpisodesPagination");
  const items = eps.data?.paginationEpisode?.items || [];
  console.log(`Capítulos encontrados en base de datos: ${items.length}\n`);

  const results = [];

  for (let i = 0; i < items.length; i++) {
    const ep = items[i];
    
    // Polite delay between episodes to avoid GraphQL throttling
    await sleep(1200);

    const qWeb = `query EpisodeLinksOnlineWeb($episode_id: ID!) { getEpisodeLinks(id: $episode_id) { links_online { server link lang } } }`;
    const resWeb = await postGraphQL(qWeb, { episode_id: ep._id }, "EpisodeLinksOnlineWeb");
    const webLinks = resWeb?.data?.getEpisodeLinks?.links_online || [];

    await sleep(800);

    const qApp = `query EpisodeLinksOnlineApp($episode_id: ID!) { getEpisodeLinks(id: $episode_id, app: "com.asiapp.doramasgo") { links_online { server link lang } } }`;
    const resApp = await postGraphQL(qApp, { episode_id: ep._id }, "EpisodeLinksOnlineApp");
    const appLinks = resApp?.data?.getEpisodeLinks?.links_online || [];

    const rawAll = [...webLinks, ...appLinks];
    
    // Process and deduplicate URLs
    const processedLinks = [];
    for (const l of rawAll) {
      const decoded = decodeJwt(l.link);
      if (!decoded || decoded.includes("embedshortener.co")) continue;
      
      let sName = "Otro";
      if (l.server === "1113" || decoded.includes("ok.ru")) sName = "Ok.ru";
      else if (l.server === "1230" || decoded.includes("voe.sx") || decoded.includes("arianathankspecial")) sName = "VOE";
      else if (l.server === "958695" || decoded.includes("byse") || decoded.includes("filemoon")) sName = "Filemoon";
      else if (l.server === "8309" || decoded.includes("streamtape")) sName = "Streamtape";
      else if (l.server === "4721" || decoded.includes("primeload")) sName = "Primeload";
      else if (l.server === "576857" || decoded.includes("vidhide") || decoded.includes("callistanise")) sName = "Vidhide";
      else if (l.server === "38585" || decoded.includes("flaswish")) sName = "Flashwish";
      else if (l.server === "7286" || decoded.includes("dood") || decoded.includes("do7go")) sName = "Doodstream";

      if (!processedLinks.some(p => p.url === decoded)) {
        processedLinks.push({ serverName: sName, url: decoded, lang: l.lang });
      }
    }

    // Now test which servers are functional (video online and playable)
    const functionalServers = [];
    for (const pl of processedLinks) {
      const isWorking = await verifyServer(pl.serverName, pl.url);
      if (isWorking) {
        functionalServers.push(pl.serverName);
      }
    }

    const uniqueWorking = [...new Set(functionalServers)];
    const isFunctional = uniqueWorking.length > 0;

    results.push({
      cap: ep.episode_number,
      title: ep.name,
      totalLinks: processedLinks.length,
      workingServers: uniqueWorking,
      functional: isFunctional
    });

    console.log(`Cap ${ep.episode_number.toString().padStart(2, ' ')}: ${isFunctional ? '✅ FUNCIONAL' : '❌ NO FUNCIONAL'} | Servidores activos: [${uniqueWorking.join(", ")}] (Total enlaces: ${processedLinks.length})`);
  }

  const functionalCount = results.filter(r => r.functional).length;
  console.log(`\n==============================================`);
  console.log(`RESUMEN FINAL:`);
  console.log(`Total de capítulos analizados: ${results.length}`);
  console.log(`Capítulos funcionales (con streaming operativo): ${functionalCount} de ${results.length}`);
  console.log(`==============================================`);
}

main().catch(console.error);
