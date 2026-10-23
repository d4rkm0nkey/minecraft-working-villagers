"""Minecraft markup for the wiki pages.

- ``:mc-emerald:`` an item or block icon with a Minecraft tooltip. ``any_axe``,
  ``any_log``, ... cycle through their members like in the recipe book.
- ``[[craft: _ any_axe _ / _ any_log _ -> chopping_block]]`` a crafting table GUI; rows
  are separated by ``/``, ``_`` is an empty slot. ``[[craft shapeless: paper ink_sac
  emerald -> work_contract]]`` fills the grid in order.
- ``[[workers]]`` the worker cards of the home page, with a card for the planned ones.
- ``[[planned_workers]]`` the cards of the workers planned next, ``[[later_workers]]`` a
  table of those planned for later.
- ``workers: [smith]`` in a page's front matter puts the infobox of that worker under the
  page title.

The pictures come from tools/generate_wiki_assets.py, the worker data from
data/workers.yml and data/planned.yml.
"""
import html
import pathlib
import re

import yaml
from mkdocs.exceptions import PluginError
from mkdocs.utils import get_relative_url

DOCS_ASSETS = "assets/mc"

GROUPS = {
    "any_axe": ("Any Axe", ["wooden_axe", "stone_axe", "iron_axe", "golden_axe", "diamond_axe", "netherite_axe"]),
    "any_hoe": ("Any Hoe", ["wooden_hoe", "stone_hoe", "iron_hoe", "diamond_hoe"]),
    "any_pickaxe": ("Any Pickaxe", ["wooden_pickaxe", "stone_pickaxe", "iron_pickaxe", "diamond_pickaxe"]),
    "any_log": ("Any Log", ["oak_log", "birch_log", "spruce_log", "dark_oak_log"]),
    "any_planks": ("Any Planks", ["oak_planks", "birch_planks", "spruce_planks"]),
    "any_wooden_slab": ("Any Wooden Slab", ["oak_slab", "spruce_slab"]),
}

NAMES = {
    "carpenter_bench": "Carpenter's Bench",
    "clock": "Clock",
    "farmland": "Farmland",
    "iron_axe_worn": "Iron Axe (worn)",
    "iron_axe_repaired": "Iron Axe (repaired)",
    "raw_iron": "Raw Iron",
}

ICON = re.compile(r":mc-([a-z0-9_]+):")
CRAFT = re.compile(r"^\[\[craft( shapeless)?:\s*(.+?)\s*->\s*([a-z0-9_]+)(?:\s+x(\d+))?\s*\]\]$", re.M)
WORKERS = re.compile(r"^\[\[workers\]\]$", re.M)
PLANNED = re.compile(r"^\[\[planned_workers\]\]$", re.M)
LATER = re.compile(r"^\[\[later_workers\]\]$", re.M)
PLANNED_PAGE = "planned.md"
FENCE = re.compile(r"^(```|~~~).*?^\1", re.M | re.S)

_state = {}


def on_config(config):
    root = pathlib.Path(config.config_file_path).parent
    docs = pathlib.Path(config.docs_dir)
    # Heads are named head_<profession>, the profession names clash with blocks.
    _state["pictures"] = {
        (f"head_{path.stem}" if folder == "heads" else path.stem): (folder, path.name)
        for folder in ("items", "blocks", "heads")
        for path in (docs / DOCS_ASSETS / folder).glob("*.png")
    }
    _state["workers"] = yaml.safe_load((root / "data/workers.yml").read_text())
    _state["planned"] = yaml.safe_load((root / "data/planned.yml").read_text())
    return config


def on_page_markdown(markdown, page, config, files):
    ctx = Context(page, files)
    parts, last = [], 0
    for fence in FENCE.finditer(markdown):
        parts.append(ctx.convert(markdown[last:fence.start()]))
        parts.append(fence.group(0))
        last = fence.end()
    parts.append(ctx.convert(markdown[last:]))
    markdown = "".join(parts)

    names = page.meta.get("workers") or []
    if names:
        boxes = "".join(ctx.infobox(name) for name in names)
        markdown = re.sub(r"^(# .*\n)", lambda m: m.group(1) + "\n" + boxes + "\n\n", markdown, count=1, flags=re.M)
    return markdown


def name_of(item):
    if item.startswith("head_"):
        return item[5:].title()
    if item in GROUPS:
        return GROUPS[item][0]
    return NAMES.get(item) or item.replace("_", " ").title()


class Context:
    def __init__(self, page, files):
        self.page = page
        self.files = files

    def url(self, path):
        return get_relative_url(path, self.page.url)

    def page_url(self, src):
        file = self.files.get_file_from_path(src)
        if file is None:
            raise PluginError(f"{self.page.file.src_path}: no page {src}")
        return get_relative_url(file.url, self.page.url)

    def image(self, item, css=""):
        picture = _state["pictures"].get(item)
        if picture is None:
            raise PluginError(f"{self.page.file.src_path}: no picture for '{item}'")
        folder, file_name = picture
        cls = f"mc-{folder[:-1]} {css}".strip()
        src = self.url(f"{DOCS_ASSETS}/{folder}/{file_name}")
        return f'<img class="{cls}" src="{src}" alt="{html.escape(name_of(item))}" loading="lazy">'

    def pictures(self, item):
        """The <img> tags of an item; groups get one per member, the first one shown."""
        if item in GROUPS:
            members = GROUPS[item][1]
            return "".join(self.image(m, "is-active" if i == 0 else "") for i, m in enumerate(members)), True
        return self.image(item), False

    def icon(self, item):
        pictures, cycles = self.pictures(item)
        cls = "mc-icon mc-cycle" if cycles else "mc-icon"
        return f'<span class="{cls}" data-tip="{html.escape(name_of(item))}">{pictures}</span>'

    def slot(self, item, result=False, count=None):
        cls = "mc-slot mc-slot--result" if result else "mc-slot"
        if item is None:
            return f'<span class="{cls}"></span>'
        pictures, cycles = self.pictures(item)
        if cycles:
            cls += " mc-cycle"
        number = f'<span class="mc-count">{count}</span>' if count and int(count) > 1 else ""
        return f'<span class="{cls}" data-tip="{html.escape(name_of(item))}">{pictures}{number}</span>'

    def craft(self, match):
        shapeless, ingredients, result, count = match.groups()
        if shapeless:
            items = ingredients.split()
            if len(items) > 9:
                raise PluginError(f"{self.page.file.src_path}: more than 9 ingredients")
            grid = items + ["_"] * (9 - len(items))
        else:
            rows = [row.split() for row in ingredients.split("/")]
            if len(rows) > 3 or any(len(row) > 3 for row in rows):
                raise PluginError(f"{self.page.file.src_path}: a crafting grid is 3x3")
            rows += [[]] * (3 - len(rows))
            grid = [cell for row in rows for cell in row + ["_"] * (3 - len(row))]
        slots = "".join(self.slot(None if cell == "_" else cell) for cell in grid)
        title = html.escape(name_of(result))
        note = '<span class="mc-recipe__note">Shapeless</span>' if shapeless else ""
        return (
            f'<figure class="mc-recipe" aria-label="Crafting recipe: {title}">'
            f'<div class="mc-gui"><div class="mc-gui__title">Crafting</div>'
            f'<div class="mc-gui__body"><div class="mc-grid">{slots}</div>'
            f'<span class="mc-arrow" aria-hidden="true"></span>{self.slot(result, True, count)}</div></div>'
            f'<figcaption>{title}{note}</figcaption></figure>'
        )

    def text(self, value):
        """Plain text with :mc-...: icons."""
        parts, last = [], 0
        for m in ICON.finditer(str(value)):
            parts.append(html.escape(str(value)[last:m.start()]))
            parts.append(self.icon(m.group(1)))
            last = m.end()
        parts.append(html.escape(str(value)[last:]))
        return "".join(parts)

    def worker(self, name):
        worker = _state["workers"].get(name)
        if worker is None:
            raise PluginError(f"{self.page.file.src_path}: no worker '{name}' in data/workers.yml")
        return worker

    def infobox(self, name):
        worker = self.worker(name)
        facts = "".join(f"<dt>{html.escape(k)}</dt><dd>{self.text(v)}</dd>" for k, v in worker["facts"].items())
        return (
            f'<aside class="wv-infobox" aria-label="{html.escape(worker["name"])} at a glance">'
            f'<div class="wv-infobox__title">{html.escape(worker["name"])}</div>'
            f'<div class="wv-infobox__art">{self.image("head_" + worker["head"], "wv-infobox__head")}'
            f'{self.image(worker["workstation"], "wv-infobox__station")}</div>'
            f'<dl>{facts}</dl></aside>'
        )

    def cards(self, _match):
        cards = []
        for worker in _state["workers"].values():
            cards.append(
                f'<a class="wv-card" href="{self.page_url(worker["page"])}">'
                f'<span class="wv-card__art">{self.image("head_" + worker["head"], "wv-card__head")}'
                f'{self.image(worker["workstation"], "wv-card__station")}</span>'
                f'<span class="wv-card__name">{html.escape(worker["name"])}</span>'
                f'<span class="wv-card__text">{html.escape(worker["summary"])}</span></a>'
            )
        cards.append(
            f'<a class="wv-card wv-card--planned" href="{self.page_url(PLANNED_PAGE)}#planned-workers">'
            f'<span class="wv-card__art">{self.image("head_villager", "wv-card__head")}'
            f'{self.image("scaffolding", "wv-card__station")}</span>'
            f'<span class="wv-card__name">More planned</span>'
            f'<span class="wv-card__text">Miner, mine logistician, merchant and more '
            f'are on their way. See what is coming.</span></a>'
        )
        return f'<div class="wv-cards">{"".join(cards)}</div>'

    def planned_cards(self, _match):
        cards = []
        for worker in _state["planned"]["next"].values():
            station = self.image(worker["workstation"], "wv-card__station") if "workstation" in worker else ""
            cards.append(
                f'<div class="wv-card wv-card--planned">'
                f'<span class="wv-card__art">{self.image(worker["icon"], "wv-card__symbol")}{station}</span>'
                f'<span class="wv-card__name">{html.escape(worker["name"])}</span>'
                f'<span class="wv-card__station-name">{self.text(worker["station"])}</span>'
                f'<span class="wv-card__text">{html.escape(worker["summary"])}</span></div>'
            )
        return f'<div class="wv-cards">{"".join(cards)}</div>'

    def later_table(self, _match):
        rows = []
        for worker in _state["planned"]["later"].values():
            station = worker.get("workstation")
            station = f'{self.icon(station)} {html.escape(name_of(station))}' if station else "open"
            rows.append(f'<tr><td>{html.escape(worker["name"])}</td><td>{station}</td><td>{self.text(worker["work"])}</td></tr>')
        return (
            '<table><thead><tr><th>Worker</th><th>Workstation</th><th>Work</th></tr></thead>'
            f'<tbody>{"".join(rows)}</tbody></table>'
        )

    def convert(self, text):
        text = CRAFT.sub(self.craft, text)
        text = WORKERS.sub(self.cards, text)
        text = PLANNED.sub(self.planned_cards, text)
        text = LATER.sub(self.later_table, text)
        return ICON.sub(lambda m: self.icon(m.group(1)), text)
