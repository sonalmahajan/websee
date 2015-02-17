import os
import Image
import ImageChops
import lxml.html
import selenium.webdriver
from selenium.common.exceptions import WebDriverException
import timeit
import time

import util
import classify

def get_feature(imagefile):
  image = Image.open(imagefile)
  colors = image.getcolors(256 * 256)
  (width, height) = image.size
  return (len(colors), width, height)

def crop_to_file(imagefile, rect, filename):
  if not (rect[0] < rect[2] and rect[1] < rect[3]):
    return False
  if not os.path.isfile(imagefile):
    return False
  image = Image.open(imagefile)
  if rect[2] > image.size[0] or rect[3] > image.size[1]:
    return False
  image.crop(rect).save(filename)
  return True

def get_value(htmlfile, xpath):
  driver = None
  while True:
    try:
      driver = selenium.webdriver.PhantomJS()
      break
    except WebDriverException:
      time.sleep(1)
  driver.set_window_size(800, 600)
  # driver = selenium.webdriver.Firefox()
  # driver.maximize_window();
  cwd = os.path.dirname(os.path.realpath(__file__))
  driver.get('file://' + cwd + '/' + htmlfile)
  element = driver.find_element_by_xpath(xpath)
  # print element.location
  # print element.size
  rect = (element.location['x'],
    element.location['y'],
    element.location['x'] + element.size['width'],
    element.location['y'] + element.size['height'])
  driver.quit()
  return rect

def image_equal(imagefile1, imagefile2):
  image1 = Image.open(imagefile1)
  image2 = Image.open(imagefile2)
  return ImageChops.difference(image1, image2).getbbox() is None

home_dir = 'gmail'
with open(home_dir + '/description.txt') as f:
  for line in f:
    split = line.strip().split('\t')
#    print split[8]
    start = timeit.default_timer()
    if int(split[8][4:]) > 10:
      print split[8]
      test_dir = home_dir + '/' + split[8]
      rect1 = get_value(test_dir + '/test.html', split[7])
      if rect1[0] == rect1[2] and rect1[1] == rect1[3]:
        continue
      if not crop_to_file(test_dir + '/test.png',
        rect1, test_dir + '/element1.png'):
        continue
      (minVal, minLoc) = util.match_image(test_dir + '/element1.png',
        test_dir + '/oracle.png')
      rect2 = (minLoc[0], minLoc[1], minLoc[0] + rect1[2] - rect1[0], \
        minLoc[1] + rect1[3] - rect1[1])
      crop_to_file(test_dir + '/oracle.png', rect2, test_dir + '/element2.png')
      exact = image_equal(test_dir + '/element1.png',
        test_dir + '/element2.png')
      print exact, minVal, rect1, rect2
      if exact and minVal != 0:
        print util.match_image(test_dir + '/element1.png',
          test_dir + '/element2.png')
      # print split[2], split[5], split[6]
    stop = timeit.default_timer()
    # print stop - start
