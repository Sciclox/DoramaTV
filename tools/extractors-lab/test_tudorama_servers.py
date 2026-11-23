import requests, re, json

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
    'Referer': 'https://tudorama.com/'
}

url = "https://tudorama.com/ver/love-in-disguise-s1x10/"
r = requests.get(url, headers=headers)
print("Page status:", r.status_code)

data_id = re.search(r'data-id=["\'](\d+)["\']', r.text)
data_nonce = re.search(r'data-nonce=["\']([a-zA-Z0-9]+)["\']', r.text)

print("data-id:", data_id.group(1) if data_id else None)
print("data-nonce:", data_nonce.group(1) if data_nonce else None)

if data_id and data_nonce:
    data = {
        'action': 'corvus_get_servers',
        'post_id': data_id.group(1),
        'nonce': data_nonce.group(1)
    }
    ajax_headers = headers.copy()
    ajax_headers['X-Requested-With'] = 'XMLHttpRequest'
    r_ajax = requests.post('https://tudorama.com/wp-admin/admin-ajax.php', data=data, headers=ajax_headers)
    print("Ajax status:", r_ajax.status_code)
    try:
        servers = r_ajax.json()
        print(f"Total servers: {len(servers)}")
        for s in servers:
            cdn_url = s.get("url")
            print(f"Server: {s.get('name')} | Lang: {s.get('lang')} | URL: {cdn_url}")
            if cdn_url:
                r_cdn = requests.get(cdn_url, headers=headers)
                iframe = re.search(r'iframe\.src\s*=\s*["\']([^"\']+)["\']', r_cdn.text)
                print(f"  -> Real URL: {iframe.group(1) if iframe else 'Direct or none'}")
    except Exception as e:
        print("Json error:", e, r_ajax.text[:200])
