import sys, re, html
from html.parser import HTMLParser

class P(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.out = []
        self.pre = 0
        self.skip = 0
        self.cell = False
    def handle_starttag(self, tag, attrs):
        if tag in ('script', 'style'):
            self.skip += 1
        elif tag == 'pre':
            self.pre += 1
            self.out.append('\n')
        elif tag in ('h1', 'h2', 'h3', 'h4', 'h5', 'h6'):
            self.out.append('\n\n' + '#' * int(tag[1]) + ' ')
        elif tag in ('p', 'div', 'ul', 'ol', 'table', 'section', 'dl'):
            self.out.append('\n')
        elif tag == 'li':
            self.out.append('\n* ')
        elif tag == 'tr':
            self.out.append('\n')
        elif tag in ('td', 'th'):
            self.out.append(' | ')
        elif tag == 'br':
            self.out.append('\n')
    def handle_endtag(self, tag):
        if tag in ('script', 'style'):
            self.skip -= 1
        elif tag == 'pre':
            self.pre -= 1
            self.out.append('\n')
        elif tag in ('p', 'div', 'table', 'ul', 'ol'):
            self.out.append('\n')
    def handle_data(self, data):
        if self.skip:
            return
        if self.pre:
            self.out.append(data)
        else:
            self.out.append(re.sub(r'\s+', ' ', data))

p = P()
p.feed(open(sys.argv[1], encoding='utf-8').read())
t = ''.join(p.out)
t = re.sub(r'[ \t]+\n', '\n', t)
t = re.sub(r'\n{3,}', '\n\n', t)
open(sys.argv[2], 'w', encoding='utf-8', newline='\n').write(t)
print(len(t), t.count('\n'))
