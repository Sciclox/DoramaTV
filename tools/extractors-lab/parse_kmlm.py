with open('tools/extractors-lab/kmlm_1x3.html', 'r', encoding='utf-8') as f:
    html = f.read()

import re
matches = list(re.finditer(r'Primeload', html))
print("Regex matches count:", len(matches))
for i, m in enumerate(matches):
    print(f"Match {i} at pos {m.start()}:")
    script_start = html.rfind('<script', 0, m.start())
    script_end = html.find('</script>', m.end())
    print("  script tags:", script_start, script_end)
    print("  tag start:", repr(html[script_start:script_start+50]))
    with open(f'tools/extractors-lab/primeload_match_{i}.txt', 'w', encoding='utf-8') as out:
        out.write(html[script_start:script_end+9])
print("Done writing match files.")
