import requests, json

headers = {'User-Agent': 'Mozilla/5.0', 'Content-Type': 'application/json'}
q_detail = """query DetailDoramaSlug($slug: String!) { detailDorama(filter: { slug: $slug }) { _id name } }"""
r = requests.post("https://user-api.seriesapi.co/graphql", json={"query": q_detail, "variables": {"slug": "kill-me-love-me"}}, headers=headers)
serie_id = r.json().get("data", {}).get("detailDorama", {}).get("_id")
print("Serie ID:", serie_id)

q_eps = """query EpisodesPagination($page: Int!, $serie_id: ID!, $season_number: Int!, $limit: Int!) {
  paginationEpisode(page: $page, limit: $limit, filter: { serie_id: $serie_id, season_number: $season_number }, sort: NUMBER_ASC) {
    items { _id episode_number }
  }
}"""
r = requests.post("https://user-api.seriesapi.co/graphql", json={"query": q_eps, "variables": {"page": 1, "limit": 10, "serie_id": serie_id, "season_number": 1}}, headers=headers)
eps = r.json().get("data", {}).get("paginationEpisode", {}).get("items", [])
print(f"Got {len(eps)} episodes")

for ep in eps[:5]:
    num = ep["episode_number"]
    eid = ep["_id"]
    print(f"\n--- Episode {num} ({eid}) ---")
    for p in ["com.asiapp.doramasgo", "doramasflix", None]:
        gql = f"""query {{ getEpisodeLinks(id: "{eid}"{f', app: "{p}"' if p else ''}) {{ links_online {{ server }} }} }}"""
        r2 = requests.post("https://user-api.seriesapi.co/graphql", json={"query": gql}, headers=headers)
        data = r2.json().get("data", {}).get("getEpisodeLinks", {})
        links = data.get("links_online") if data else []
        servers = [l.get("server") for l in (links or [])]
        print(f"  Partition {p or 'web'}: {servers}")
