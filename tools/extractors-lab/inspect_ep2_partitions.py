import requests, json, base64

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

headers = {'User-Agent': 'Mozilla/5.0', 'Content-Type': 'application/json'}
ep_id = "670b752ba35a6c33cfba1440"

partitions = ["com.asiapp.doramasgo", "doramasflix", "playdoramas", "doramasyt", None]
all_links = {}

for p in partitions:
    label = p if p else "WEB (null)"
    q = f"""query {{ getEpisodeLinks(id: "{ep_id}"{f', app: "{p}"' if p else ''}) {{ links_online {{ server link is_recommended }} }} }}"""
    r = requests.post("https://user-api.seriesapi.co/graphql", json={"query": q}, headers=headers)
    links = r.json().get("data", {}).get("getEpisodeLinks", {}).get("links_online", [])
    print(f"\n=== Partition: {label} (Found {len(links)}) ===")
    for l in links:
        s = l.get("server")
        raw = l.get("link", "")
        dec = decode_jwt(raw)
        print(f"  Server {s}: {dec}")
