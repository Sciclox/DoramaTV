import urllib.request
import json
import ssl
import base64

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Content-Type': 'application/json; charset=utf-8',
    'Referer': 'https://doramasflix.in/',
    'Origin': 'https://doramasflix.in'
}

def post_graphql(query, variables, op_name):
    url = 'https://user-api.seriesapi.co/graphql'
    payload = json.dumps({
        'operationName': op_name,
        'query': query,
        'variables': variables
    }).encode('utf-8')
    req = urllib.request.Request(url, data=payload, headers=headers)
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=10) as resp:
            return json.loads(resp.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        print(f"HTTP Error {op_name}: {e.code} {e.read().decode('utf-8')}")
        return None
    except Exception as e:
        print(f"Error {op_name}:", e)
        return None

def decode_embed_jwt(raw):
    try:
        if not raw or not raw.startswith("http"):
            return None
        parts = raw.split("/e/")
        if len(parts) < 2:
            return None
        jwt = parts[1].split(".")[1]
        padded = jwt + "=" * ((4 - len(jwt) % 4) % 4)
        decoded = base64.urlsafe_b64decode(padded).decode('utf-8')
        obj = json.loads(decoded)
        link = obj.get("link", "")
        # might be b64 encoded
        padded_link = link + "=" * ((4 - len(link) % 4) % 4)
        try:
            real_link = base64.b64decode(padded_link).decode('utf-8')
            return real_link
        except:
            return link
    except Exception as e:
        return f"Err: {e}"

dorama_id = "670b74efa35a6c33cfba1425"

ep_gql = """
query EpisodesPagination($page: Int!, $serie_id: ID!, $season_number: Int!, $limit: Int!) {
  paginationEpisode(page: $page, limit: $limit, filter: { serie_id: $serie_id, season_number: $season_number }, sort: NUMBER_ASC) {
    items {
      _id
      episode_number
      slug
      name
    }
  }
}
"""
episodes = post_graphql(ep_gql, {'page': 1, 'limit': 50, 'serie_id': dorama_id, 'season_number': 1}, 'EpisodesPagination')

ep3_id = None
if episodes and 'data' in episodes:
    for ep in episodes['data']['paginationEpisode']['items']:
        if ep['episode_number'] == 3:
            ep3_id = ep['_id']
            print(f"Found Ep 3: id={ep3_id}, slug={ep['slug']}, name={ep['name']}")
            break

if ep3_id:
    for op_name, app in [("EpisodeLinksOnlineWeb", None), ("EpisodeLinksOnlineApp", "com.asiapp.doramasgo"), ("EpisodeLinksOnlineFlix", "doramasflix")]:
        if app:
            q = f"""
            query {op_name}($episode_id: ID!) {{
              getEpisodeLinks(id: $episode_id, app: "{app}") {{
                links_online {{
                  server
                  lang
                  link
                  _id
                  is_recommended
                }}
              }}
            }}
            """
        else:
            q = f"""
            query {op_name}($episode_id: ID!) {{
              getEpisodeLinks(id: $episode_id) {{
                links_online {{
                  server
                  lang
                  link
                  _id
                  is_recommended
                }}
              }}
            }}
            """
        res = post_graphql(q, {'episode_id': ep3_id}, op_name)
        links = res.get('data', {}).get('getEpisodeLinks', {}).get('links_online', []) if res else []
        print(f"\n--- {op_name} (found {len(links)}) ---")
        for l in links:
            decoded = decode_embed_jwt(l.get('link', ''))
            print(f"  Server: {l.get('server')} | Lang: {l.get('lang')} | Rec: {l.get('is_recommended')} | Decoded: {decoded}")
