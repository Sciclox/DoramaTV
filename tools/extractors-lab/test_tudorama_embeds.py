import requests, re

urls = [
    ("VOE", "https://goofy-banana.com/e/hkjihuqugbf7"),
    ("Filemoon", "https://bysesukior.com/e/dbz2wo4skjew"),
    ("Abyss", "https://abyssplayer.com/CAN_7exUA"),
    ("Earnvids", "https://minochinos.com/embed/6ps6hpdt0rbo")
]

headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36'}

for name, u in urls:
    try:
        r = requests.get(u, headers=headers, timeout=5)
        print(f"[{name}] Status: {r.status_code} | Len: {len(r.text)} | Has .m3u8: {'.m3u8' in r.text} | 404: {'not found' in r.text.lower() or 'deleted' in r.text.lower()}")
    except Exception as e:
        print(f"[{name}] Error: {e}")
