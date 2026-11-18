const https = require("https");

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
    req.write(body);
    req.end();
  });
}

async function run() {
  const qWeb = `query EpisodeLinksOnlineWeb($episode_id: ID!) { getEpisodeLinks(id: $episode_id) { links_online { server link lang is_recommended } } }`;
  const resWeb = await postGraphQL(qWeb, { episode_id: "670b752ba35a6c33cfba143a" }, "EpisodeLinksOnlineWeb");
  const webLinks = resWeb?.data?.getEpisodeLinks?.links_online || [];

  console.log("Web links:");
  webLinks.forEach(l => {
    console.log(`Server: ${l.server}, Decoded: ${decodeJwt(l.link)}`);
  });

  await new Promise(r => setTimeout(r, 500));

  const qApp = `query EpisodeLinksOnlineApp($episode_id: ID!) { getEpisodeLinks(id: $episode_id, app: "com.asiapp.doramasgo") { links_online { server link lang is_recommended } } }`;
  const resApp = await postGraphQL(qApp, { episode_id: "670b752ba35a6c33cfba143a" }, "EpisodeLinksOnlineApp");
  const appLinks = resApp?.data?.getEpisodeLinks?.links_online || [];

  console.log("\nApp links:");
  appLinks.forEach(l => {
    console.log(`Server: ${l.server}, Decoded: ${decodeJwt(l.link)}`);
  });
}

run();
