import requests
import json
import base64

def decode_jwt(raw):
    token = raw
    if "embedshortener.co/e/" in raw:
        token = raw.split("embedshortener.co/e/")[1].split("?")[0]
    try:
        parts = token.split('.')
        if len(parts) >= 2:
            padded = parts[1] + '=' * (4 - len(parts[1]) % 4)
            data = base64.urlsafe_b64decode(padded).decode('utf-8')
            payload = json.loads(data)
            b64_link = payload.get('link', '')
            try:
                b64_padded = b64_link + '=' * (4 - len(b64_link) % 4)
                decoded_link = base64.b64decode(b64_padded).decode('utf-8')
                return decoded_link
            except Exception:
                return b64_link
    except Exception as e:
        pass
    return raw

def test():
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
        'Content-Type': 'application/json'
    }
    ep_id = "670b752ba35a6c33cfba1440"
    
    partitions = ["com.asiapp.doramasgo", None, "doramasflix"]
    for p in partitions:
        print(f"=== Partition: {p} ===")
        if p is None:
            query = """
            query EpisodeLinksOnlineWeb($id: ID!) {
              getEpisodeLinks(id: $id) {
                links_online {
                  server
                  lang
                  link
                  _id
                  is_recommended
                }
              }
            }
            """
            op = "EpisodeLinksOnlineWeb"
        else:
            query = f"""
            query EpisodeLinksOnlineApp($id: ID!) {{
              getEpisodeLinks(id: $id, app: "{p}") {{
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
            op = "EpisodeLinksOnlineApp"
        
        r = requests.post("https://user-api.seriesapi.co/graphql", json={"query": query, "variables": {"id": ep_id}}, headers=headers)
        res = r.json()
        links = res.get("data", {}).get("getEpisodeLinks", {}).get("links_online", [])
        print(f"Found {len(links)} links")
        for l in links:
            raw = l.get("link", "")
            dec = decode_jwt(raw)
            print(f"Server {l.get('server')}: {dec}")

if __name__ == "__main__":
    test()
