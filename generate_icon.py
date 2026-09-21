import zlib, struct, math

WIDTH = 48
HEIGHT = 48

def make_png(width, height, rgba_data):
    def chunk(tag, data):
        return struct.pack('>I', len(data)) + tag + data + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    
    ihdr = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    raw = bytearray()
    for y in range(height):
        raw.append(0) # filter type 0
        raw.extend(rgba_data[y*width*4 : (y+1)*width*4])
    idat = zlib.compress(bytes(raw), 9)
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', b'')

# Create 48x48 buffer
img = [[(0, 0, 0, 0) for _ in range(WIDTH)] for _ in range(HEIGHT)]

def blend(c_back, c_fore):
    # c_fore: (r, g, b, a_float 0..1)
    # c_back: (r, g, b, a_float 0..1)
    br, bg, bb, ba = c_back
    fr, fg, fb, fa = c_fore
    out_a = fa + ba * (1.0 - fa)
    if out_a == 0:
        return (0, 0, 0, 0)
    out_r = (fr * fa + br * ba * (1.0 - fa)) / out_a
    out_g = (fg * fa + bg * ba * (1.0 - fa)) / out_a
    out_b = (fb * fa + bb * ba * (1.0 - fa)) / out_a
    return (int(out_r), int(out_g), int(out_b), out_a)

# Draw rounded squircle base (from x=2 to 45, y=2 to 45)
# Corner radius 9
rx, ry, rw, rh = 2.0, 2.0, 44.0, 44.0
radius = 10.0

for y in range(HEIGHT):
    for x in range(WIDTH):
        # Calculate signed distance to rounded rectangle
        cx = max(rx + radius, min(float(x) + 0.5, rx + rw - radius))
        cy = max(ry + radius, min(float(y) + 0.5, ry + rh - radius))
        dist = math.sqrt((x + 0.5 - cx)**2 + (y + 0.5 - cy)**2)
        
        # Inside box check
        in_core_x = (x + 0.5 >= rx) and (x + 0.5 <= rx + rw)
        in_core_y = (y + 0.5 >= ry) and (y + 0.5 <= ry + rh)
        
        if (x + 0.5 < rx + radius or x + 0.5 > rx + rw - radius) and (y + 0.5 < ry + radius or y + 0.5 > ry + rh - radius):
            d = dist - radius
        else:
            dx = max(rx - (x + 0.5), (x + 0.5) - (rx + rw))
            dy = max(ry - (y + 0.5), (y + 0.5) - (ry + rh))
            d = max(dx, dy)
        
        # Anti-aliasing alpha
        alpha = max(0.0, min(1.0, 0.5 - d))
        if alpha <= 0:
            continue
        
        # Gradient from top #1A2840 to bottom #0A111E
        t = (y - ry) / rh
        t = max(0.0, min(1.0, t))
        r = int(24 * (1 - t) + 8 * t)
        g = int(42 * (1 - t) + 14 * t)
        b = int(72 * (1 - t) + 26 * t)
        
        # Top inner glow/rim
        if d >= -1.5 and d <= 0.0 and y < 24:
            r = min(255, r + 70)
            g = min(255, g + 100)
            b = min(255, b + 150)
        
        img[y][x] = (r, g, b, alpha)

# Draw stylized Bluetooth / SmartBridge Icon in the center (center = 24, 24)
# We will draw:
# 1. Outer wireless arcs: left cyan arc, right green arc (Companion bridge)
# 2. Central Bluetooth symbol: vertical line, angled wings, intersecting cross

def draw_line(x0, y0, x1, y1, width, color):
    # color: (r, g, b, a_float)
    length = math.hypot(x1 - x0, y1 - y0)
    if length == 0: return
    for y in range(HEIGHT):
        for x in range(WIDTH):
            px, py = x + 0.5, y + 0.5
            # project (px, py) onto segment
            u = ((px - x0)*(x1 - x0) + (py - y0)*(y1 - y0)) / (length * length)
            u = max(0.0, min(1.0, u))
            proj_x = x0 + u * (x1 - x0)
            proj_y = y0 + u * (y1 - y0)
            dist = math.hypot(px - proj_x, py - proj_y)
            d = dist - (width / 2.0)
            a = max(0.0, min(1.0, 0.5 - d)) * color[3]
            if a > 0:
                img[y][x] = blend(img[y][x], (color[0], color[1], color[2], a))

def draw_circle(cx, cy, r_inner, r_outer, a_start, a_end, color):
    for y in range(HEIGHT):
        for x in range(WIDTH):
            px, py = x + 0.5, y + 0.5
            dist = math.hypot(px - cx, py - cy)
            ang = math.atan2(py - cy, px - cx)
            # Normalize ang to 0..2pi or -pi..pi
            if dist >= r_inner - 1.0 and dist <= r_outer + 1.0:
                if a_start <= ang <= a_end or (a_start > a_end and (ang >= a_start or ang <= a_end)):
                    d_edge = max(r_inner - dist, dist - r_outer)
                    a = max(0.0, min(1.0, 0.5 - d_edge)) * color[3]
                    if a > 0:
                        img[y][x] = blend(img[y][x], (color[0], color[1], color[2], a))

# Bridge arcs:
# Left arc (BlackBerry blue: #00A2E8)
draw_circle(24, 24, 15.0, 17.5, math.radians(120), math.radians(240), (0, 162, 232, 0.85))
draw_circle(24, 24, 18.5, 20.0, math.radians(135), math.radians(225), (0, 162, 232, 0.45))

# Right arc (Android green: #00E676)
draw_circle(24, 24, 15.0, 17.5, math.radians(-60), math.radians(60), (0, 230, 118, 0.85))
draw_circle(24, 24, 18.5, 20.0, math.radians(-45), math.radians(45), (0, 230, 118, 0.45))

# Bluetooth central symbol:
# Vertical center spine (24, 10) to (24, 38)
# Wings:
# (18, 16) -> (30, 28) -> (24, 34) -> (24, 14) -> (30, 20) -> (18, 32)
# With cyan glow behind it
draw_line(24, 11, 24, 37, 4.5, (0, 160, 255, 0.35))
draw_line(17, 17, 30, 30, 4.5, (0, 160, 255, 0.35))
draw_line(30, 30, 24, 36, 4.5, (0, 160, 255, 0.35))
draw_line(24, 12, 30, 18, 4.5, (0, 160, 255, 0.35))
draw_line(30, 18, 17, 31, 4.5, (0, 160, 255, 0.35))

# Crisp core lines (Pure white with slight cyan #F0F8FF)
CORE_W = 2.4
CORE_COL = (255, 255, 255, 0.98)
draw_line(24, 11, 24, 37, CORE_W, CORE_COL)
draw_line(18, 17, 30, 29, CORE_W, CORE_COL)
draw_line(30, 29, 24, 35, CORE_W, CORE_COL)
draw_line(24, 13, 30, 19, CORE_W, CORE_COL)
draw_line(30, 19, 18, 31, CORE_W, CORE_COL)

# Two bridge connection dots (dots at the ends):
def draw_dot(cx, cy, r, col):
    for y in range(HEIGHT):
        for x in range(WIDTH):
            dist = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            a = max(0.0, min(1.0, 0.5 - (dist - r))) * col[3]
            if a > 0:
                img[y][x] = blend(img[y][x], (col[0], col[1], col[2], a))

# Cyan dot at left, Emerald dot at right
draw_dot(17, 17, 2.0, (0, 210, 255, 1.0))
draw_dot(17, 31, 2.0, (0, 210, 255, 1.0))
draw_dot(30, 19, 2.0, (0, 230, 118, 1.0))
draw_dot(30, 29, 2.0, (0, 230, 118, 1.0))

# Convert to bytearray
raw_rgba = bytearray(WIDTH * HEIGHT * 4)
for y in range(HEIGHT):
    for x in range(WIDTH):
        r, g, b, a = img[y][x]
        idx = (y * WIDTH + x) * 4
        raw_rgba[idx] = r
        raw_rgba[idx+1] = g
        raw_rgba[idx+2] = b
        raw_rgba[idx+3] = int(a * 255)

png_data = make_png(WIDTH, HEIGHT, raw_rgba)
with open("blackberry/icon.png", "wb") as f:
    f.write(png_data)

print(f"Generated blackberry/icon.png: {len(png_data)} bytes successfully!")
