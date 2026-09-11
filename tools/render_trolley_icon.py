from PIL import Image, ImageDraw

size = 256
bg = (255,255,255,0)
fill = (255,87,34,255)  # #FF5722
stroke = (0,0,0,255)

img = Image.new('RGBA', (size, size), bg)
d = ImageDraw.Draw(img)

# Draw basket (trapezoid)
basket = [(48,80),(196,80),(168,140),(80,140)]
d.polygon(basket, fill=fill)

# Draw handle
d.line([(56,80),(40,40),(88,40),(108,80)], fill=fill, width=12)

# Draw wheels
d.ellipse((60,160,100,200), fill=stroke)
d.ellipse((156,160,196,200), fill=stroke)

# Draw basket grid lines
d.line([(80,90),(168,90)], fill=stroke, width=2)
d.line([(80,105),(168,105)], fill=stroke, width=2)
d.line([(100,80),(100,140)], fill=stroke, width=2)
d.line([(132,80),(132,140)], fill=stroke, width=2)

# Save
img.save(r"C:\Users\marti\source\repos\copilot-worktrees\ShoppingList\finnegan0596-issue-7-updating-the-app-logo-to-a-shopping-trol-00218a\tools\trolley_render.png")
print('saved')
