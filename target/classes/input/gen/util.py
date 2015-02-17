import urllib2 
import subprocess
import selenium.webdriver
from selenium.webdriver.support.color import Color
from selenium.common.exceptions import WebDriverException
import os
import time
import Image
import ImageChops
import re
import cv2
import numpy as np

def get_source(url):
  response = urllib2.urlopen(url)
  html = response.read()
  return html

def wget_source(url):
  commandline = 'wget --adjust-extension --convert-links --no-directories --quiet'
  commandline += ' ' + url
  print commandline
  return_code = subprocess.call(commandline, shell=True)

def save_file(content, filename):
  html_file = open(filename, 'w')
  html_file.write(content)
  html_file.close()

def read_file(filename):
  html_file = open(filename, 'r')
  content = html_file.read()
  html_file.close()
  return content

def screenshot(htmlfile, pngfile):
  cwd = os.path.dirname(os.path.realpath(__file__))
  driver = None
  while True:
    try:
      driver = selenium.webdriver.Firefox()
      #driver = selenium.webdriver.PhantomJS()
      break
    except:
      time.sleep(1)
  driver.get('file://' + cwd + '/' + htmlfile)
  #driver.set_window_size(800, 600)
  #driver.maximize_window();
  driver.save_screenshot(pngfile)
  driver.quit()

def patch_xpath(xpath):
  return re.sub(r'(/table(\[\d+\])?)', r'\1/tbody[1]', xpath)

def get_default_value(htmlfile, xpath, name, css=True):
  cwd = os.path.dirname(os.path.realpath(__file__))
  driver = None
  while True:
    try:
      driver = selenium.webdriver.Firefox()
      #driver = selenium.webdriver.PhantomJS()
      break
    except:
      time.sleep(1)
  driver.get('file://' + cwd + '/' + htmlfile)
  #driver.set_window_size(800, 600)
  #driver.maximize_window();
  element = driver.find_element_by_xpath(patch_xpath(xpath))
  if css:
    value = element.value_of_css_property(name)
  else:
    value = element.get_attribute(name)
  if not value:
    value = ''
  if '-moz-' in value:
    value = value.replace('-moz-', '')
  if 'color' in name:
    try:
      value = Color.from_string(value).hex
    except:
      pass
  driver.quit()
  time.sleep(1)
  return value

def diff_image(imagefile1, imagefile2):
  image1 = Image.open(imagefile1)
  image2 = Image.open(imagefile2)
  diff = ImageChops.difference(image1, image2)
  return diff.convert('RGB')

def match_image(imagefile, subimagefile):
  if not os.path.isfile(imagefile):
    return (None, None)
  if not os.path.isfile(subimagefile):
    return (None, None)
  image = cv2.imread(imagefile)
  if image is None:
    return (None, None)
  template = cv2.imread(subimagefile)
  if template is None:
    return (None, None)
  result = cv2.matchTemplate(image, template, cv2.TM_SQDIFF)
  # print np.unravel_index(result.argmin(), result.shape)
  minVal, maxVal, minLoc, maxLoc = cv2.minMaxLoc(result)
  return (minVal, minLoc)

def list_equal(a, b):
  return a[0] == b[0] and a[1] == b[1] and a[2] == b[2]

def match_crop_image(testimage, rect, oracleimage):
  if not (rect[0] < rect[2] and rect[1] < rect[3]):
    return
  if not os.path.isfile(testimage):
    return
  if not os.path.isfile(oracleimage):
    return
  image = cv2.imread(oracleimage)
  if image is None:
    return
  template = cv2.imread(testimage)
  if rect[2] > template.shape[0] or rect[3] > template.shape[1]:
    return
  template = template[rect[1]:rect[3], rect[0]:rect[2]]
  if template is None:
    return
  result = cv2.matchTemplate(image, template, cv2.TM_SQDIFF)
  minVal, maxVal, minLoc, maxLoc = cv2.minMaxLoc(result)
  t = 0
  for x in range(rect[3] - rect[1]):
    for y in range(rect[2] - rect[0]):
      if not list_equal(template[x, y], image[minLoc[1] + x, minLoc[0] + y]):
        t += 1
  if t == 0:
    return minLoc
  else:
    return


if __name__ == "__main__":
  patch_xpath('/bookstore/book[1]/table/title')
  patch_xpath('/bookstore/book[1]/table[2]/title')
  patch_xpath('/bookstore/table/book[1]/table[2]/title')
  patch_xpath('/bookstore/table/book[1]/table[2]')
  patch_xpath('/bookstore/table/book[1]/table')
