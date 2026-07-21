"""Erzeugt aus dem Design-Spec ein PDF.

Baugleich zu /root/gen_pdf.py (OtakuPulse-Projektdoku) — fpdf2 verträgt weder
verschachtelte Inline-Tags in Tabellenzellen noch nicht registrierte Schriften,
deshalb werden <code>/<em> vorher entfernt.
"""
import os
import re
import sys

import markdown
from fpdf import FPDF

SRC = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
    os.path.dirname(__file__), "superpowers/specs/2026-07-21-otakupulse-companion-design.md"
)
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(
    os.path.dirname(__file__), "OtakuPulse-Companion-Plan.pdf"
)
FONTS = "/root/fonts"

with open(SRC, encoding="utf-8") as f:
    md = f.read()

html = markdown.markdown(md, extensions=["tables", "fenced_code", "sane_lists"])


def strip_inline_in_cells(h: str) -> str:
    """fpdf2 stolpert über verschachtelte Inline-Tags in <td>/<th>."""
    def repl(m):
        return re.sub(r"</?(code|strong|em|b|i)>", "", m.group(0))
    return re.sub(r"<t[dh][^>]*>.*?</t[dh]>", repl, h, flags=re.DOTALL)


def unwrap_code_blocks(h: str) -> str:
    """Codeblöcke als normalen Text setzen.

    write_html verlangt für <pre> die Kernschrift "courier", die kein Unicode kann —
    schon ein Gedankenstrich lässt sie scheitern, und überschreiben lässt sie sich nicht.
    Also Zeilenumbrüche und Einrückung von Hand erhalten und in DejaVu setzen.
    """
    def repl(m):
        body = m.group(1)
        body = body.replace("&nbsp;", " ")
        lines = body.strip("\n").split("\n")
        kept = [ln.replace(" ", "&nbsp;") for ln in lines]
        return "<p>" + "<br>".join(kept) + "</p>"

    h = re.sub(r"<pre><code[^>]*>(.*?)</code></pre>", repl, h, flags=re.DOTALL)
    return re.sub(r"<pre[^>]*>(.*?)</pre>", repl, h, flags=re.DOTALL)


html = strip_inline_in_cells(html)
html = unwrap_code_blocks(html)
html = re.sub(r"</?(code|em|i)>", "", html)


class PDF(FPDF):
    def footer(self):
        self.set_y(-15)
        self.set_font("DejaVu", "", 8)
        self.set_text_color(140, 140, 140)
        self.cell(
            0, 10,
            f"OtakuPulse Companion — Plan   ·   Seite {self.page_no()}/{{nb}}",
            align="C",
        )


pdf = PDF(format="A4")
pdf.set_auto_page_break(auto=True, margin=18)
pdf.set_margins(18, 16, 18)
pdf.add_font("DejaVu", "", f"{FONTS}/DejaVuSans.ttf")
pdf.add_font("DejaVu", "B", f"{FONTS}/DejaVuSans-Bold.ttf")
pdf.alias_nb_pages()
pdf.add_page()
pdf.set_font("DejaVu", size=10.5)
pdf.write_html(html, table_line_separators=True)
pdf.output(OUT)

print("OK", OUT, os.path.getsize(OUT), "Bytes, Seiten:", pdf.page_no())
