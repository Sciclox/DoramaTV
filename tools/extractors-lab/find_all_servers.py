import re
import json

with open('tools/extractors-lab/kmlm_1x3.html', 'r', encoding='utf-8') as f:
    html = f.read()

# find all Server entries
matches = re.findall(r'\{[^{}]*?"__typename":"Server"[^{}]*?\}', html)
print('Found server raw matches:', len(matches))

unique_servers = {}
for m in matches:
    # unescape if needed
    clean = m.replace('\\"', '"')
    try:
        obj = json.loads(clean)
        if 'name' in obj:
            unique_servers[obj['name']] = obj.get('code_flix', obj.get('_id'))
    except:
        # regex extract name and code_flix
        name_m = re.search(r'"name":"([^"]+)"', clean)
        code_m = re.search(r'"code_flix":"([^"]+)"', clean)
        if name_m:
            unique_servers[name_m.group(1)] = code_m.group(1) if code_m else 'unknown'

print('\nDoramasflix All Registered Servers in HTML payload:')
for name, code in sorted(unique_servers.items()):
    print(f'  - Server Name: "{name}" -> code_flix: "{code}"')
