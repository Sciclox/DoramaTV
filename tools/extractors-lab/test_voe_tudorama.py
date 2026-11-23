import requests, re, base64

u = "https://goofy-banana.com/e/hkjihuqugbf7"
headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36'}
r = requests.get(u, headers=headers)
html = r.text

m1 = re.search(r"var\s+sources\s*=\s*\{['\"]hls['\"]\s*:\s*['\"]([^'\"]+)['\"]", html)
m2 = re.search(r"['\"]hls['\"]\s*:\s*['\"]([^'\"]+)['\"]", html)
m3 = re.search(r"(?:const|let|var)\s+[a-zA-Z0-9_]+\s*=\s*'([A-Za-z0-9+/=]{40,})'", html)
print("m1:", m1.group(1) if m1 else None)
print("m2:", m2.group(1) if m2 else None)
if m3:
    b64 = m3.group(1)
    print("Found b64 payload, len:", len(b64))
    try:
        dec = base64.b64decode(b64).decode('utf-8', errors='ignore')
        print("Decoded snippet:", dec[:100])
        m_hls = re.search(r'https?://[^\s\'"]+\.m3u8[^\s\'"]*', dec)
        print("m3u8 inside b64:", m_hls.group(0) if m_hls else None)
    except Exception as e:
        print("b64 err:", e)
