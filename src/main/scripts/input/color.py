import os
import Image
import ImageChops
import lxml.html
import selenium.webdriver
from selenium.common.exceptions import WebDriverException
import timeit

import util
import classify

def get_feature(imagefile):
  image = Image.open(imagefile)
  colors = image.getcolors(256 * 256)
  (width, height) = image.size
  return (len(colors), width, height)
#  print image.histogram()
#  print len(image.histogram())

def get_diff_feature(imagefile1, imagefile2):
  image1 = Image.open(imagefile1)
  image2 = Image.open(imagefile2)
  diff_pixel = 0
  diff = ImageChops.difference(image1, image2)
  if diff.getbbox():
    left, upper, right, lower = diff.getbbox()
  else:
    return 0
  for x in range(left, right):
    for y in range(upper, lower):
      if diff.load()[x, y] != (0, 0, 0, 0):
        diff_pixel += 1
  return diff_pixel

def get_value(htmlfile, xpath):
  driver = None
  while True:
    try:
      driver = selenium.webdriver.PhantomJS()
      break
    except WebDriverException:
      time.sleep(1)
  driver.set_window_size(800, 600)
  #driver = selenium.webdriver.Firefox()
  #driver.maximize_window();
  cwd = os.path.dirname(os.path.realpath(__file__))
  driver.get('file://' + cwd + '/' + htmlfile)
  element = driver.find_element_by_xpath(xpath)
  print element.value_of_css_property('padding-top')
  print element.value_of_css_property('margin-top')
  print element.value_of_css_property('height')
  print element.value_of_css_property('width')
  print element.value_of_css_property('top')
  print element.value_of_css_property('left')
  print element.location
  print element.size
  driver.quit()

def change_property(htmlfile, xpath, prop, value):
  html = lxml.html.parse(htmlfile)
  elements = html.xpath(xpath)
  for e in elements:
    if 'style' in e.attrib:
      original_style = e.attrib['style']
      e.attrib['style'] += ';{0}:{1};'.format(prop, value)
    else:
      e.attrib['style'] = '{0}:{1};'.format(prop, value)
    util.save_file(lxml.html.tostring(html), 'temp.html')
    util.screenshot('temp.html', 'temp.png')

def fine_grain(base_dir, xpath, original_diff):
  for prop in classify.category['color']:
    change_property(base_dir + '/test.html', xpath, prop, '#123456')
    if get_diff_feature(base_dir + '/test.png', 'temp.png'):
      diff_pixel = get_diff_feature(base_dir + '/oracle.png', 'temp.png')
      if diff_pixel == original_diff:
        return prop
      elif prop == 'color' and (original_diff / 100) > abs(original_diff - diff_pixel):
        return prop

test_dir = 'gmail'
with open(test_dir + '/description.txt') as f:
  for line in f:
    split = line.strip().split('\t')
#    print split[8]
    start = timeit.default_timer()
    o = get_feature(test_dir + '/' + split[8] + '/oracle.png')
    t = get_feature(test_dir + '/' + split[8] + '/test.png')
    d = get_diff_feature(test_dir + '/' + split[8] + '/oracle.png',
      test_dir + '/' + split[8] + '/test.png')
    #get_value('test.html', split[7])
    if (o[0] < t[0] and o[1] == t[1] and o[2] == t[2]):
#      print 'Color Category'
      print fine_grain(test_dir +'/' + split[8], split[7], d)
    stop = timeit.default_timer()
    print stop - start
