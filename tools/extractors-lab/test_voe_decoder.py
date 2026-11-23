import requests, re, base64

def rot13(s):
    res = []
    for c in s:
        if 'a' <= c <= 'z': res.append(chr((ord(c) - ord('a') + 13) % 26 + ord('a')))
        elif 'A' <= c <= 'Z': res.append(chr((ord(c) - ord('A') + 13) % 26 + ord('A')))
        else: res.append(c)
    return ''.join(res)

r = requests.get('https://goofy-banana.com/e/hkjihuqugbf7', headers={'User-Agent': 'Mozilla/5.0'})
# Find script 15
m = re.search(r'\[\"([A-Za-z0-9@\$%\?\*\~!\#\^\&]+)\"\]', r.text)
if m:
    raw = m.group(1)
    markers = ['@$', '^^', '~@', '%?', '*~', '!!', '#&']
    r_dec = rot13(raw)
    for mk in markers:
        r_dec = r_dec.replace(mk, '')
    pad = len(r_dec) % 4
    if pad: r_dec += '=' * (4 - pad)
    b1 = base64.b64decode(r_dec)
    b2 = bytes([b - 1 for b in b1])
    res_str = b2.decode('utf-8', errors='ignore')
    res_final = res_str[::-1]
    p2 = len(res_final) % 4
    if p2: res_final += '=' * (4 - p2)
    final_json = base64.b64decode(res_final).decode('utf-8', errors='ignore')
    print('DECODED VOE JSON:')
    print(final_json[:300])
else:
    print('No match for payload')
