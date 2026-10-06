"""Generate A/B palettes with the existing pixel-art generators and unchanged geometry.

Only palette constants and output names change before drawing. Red PNGs are never
opened or overwritten, and amber fruit/door details retain their original colors.
"""
import ast
import colorsys
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def palette(rgb, hue):
    h, s, v = colorsys.rgb_to_hsv(*(x / 255 for x in rgb))
    if s > .1 and (h > .85 or h < .08):
        return tuple(round(x * 255) for x in colorsys.hsv_to_rgb((h + hue - .97) % 1, s, v))
    return rgb


class VariantPalette(ast.NodeTransformer):
    def __init__(self, color, hue):
        self.color, self.hue = color, hue

    def visit_Constant(self, node):
        if isinstance(node.value, str):
            value = node.value
            if re.fullmatch(r'#[0-9a-fA-F]{6}', value):
                rgb = palette(tuple(int(value[i:i+2], 16) for i in (1, 3, 5)), self.hue)
                value = '#' + ''.join(f'{x:02x}' for x in rgb)
            else:
                value = re.sub(r'(?<![a-z_])((?:fruiting_)?collatz_[a-z_]+)',
                               lambda m: self.color + '_' + m[1], value)
                value = value.replace('collatz-red-texture-preview', 'collatz-' + self.color + '-texture-preview')
                value = value.replace('collatz-wood-texture-preview', 'collatz-' + self.color + '-wood-texture-preview')
            return ast.copy_location(ast.Constant(value=value), node)
        return node

    def visit_Tuple(self, node):
        # The wood generator stores its four RGB swatches as numeric tuples.
        if len(node.elts) == 3 and all(isinstance(n, ast.Constant) and type(n.value) is int for n in node.elts):
            rgb = tuple(n.value for n in node.elts)
            if all(0 <= x <= 255 for x in rgb) and rgb[0] > 70 and rgb[1] < rgb[2]:
                return ast.copy_location(ast.Tuple(elts=[ast.Constant(x) for x in palette(rgb, self.hue)], ctx=ast.Load()), node)
        return self.generic_visit(node)


def main():
    for color, hue in [('purple', .77), ('blue', .62)]:
        for filename, entrypoint in [('create_collatz_tree_textures.py', 'generate'),
                                     ('create_collatz_wood_textures.py', 'main')]:
            source = ROOT / 'tools/textures' / filename
            tree = ast.fix_missing_locations(VariantPalette(color, hue).visit(ast.parse(source.read_text(encoding='utf-8'))))
            namespace = {'__file__': str(source), '__name__': 'collatz_' + color}
            exec(compile(tree, str(source), 'exec'), namespace)
            namespace[entrypoint]()
        print('Generated', color, 'Collatz tree and wood textures')


if __name__ == '__main__':
    main()
