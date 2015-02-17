import os
import Image
import ImageChops
import selenium.webdriver
from selenium.common.exceptions import WebDriverException
import cv2
import math
import time
import re
import argparse
import sys

class Feature(object):

  def __init__(self, oracle_image, test_page, element_xpath):
    self.oracle_image_file = oracle_image
    self.test_page_file = test_page
    self.element_xpath = element_xpath
    self.test_image_file = 'test.png'
    self.extract()

  def screenshot(self, htmlfile, pngfile):
    driver = None
    while True:
      try:
        driver = selenium.webdriver.PhantomJS()
        break
      except WebDriverException:
        time.sleep(1)
    cwd = os.path.dirname(os.path.realpath(__file__))
    driver.set_window_size(800, 600)
    driver.set_page_load_timeout(30)
    while True:
      try:
        driver.get('file://' + cwd + '/' + htmlfile)
        break
      except:
        time.sleep(1)
    driver.save_screenshot(pngfile)
    driver.quit()

  def patch_xpath(self, xpath):
    return re.sub(r'(/table(\[\d+\])?)', r'\1/tbody[1]', xpath)

  def get_value(self):
    driver = None
    while True:
      try:
        driver = selenium.webdriver.PhantomJS()
        break
      except WebDriverException:
        time.sleep(1)
    # cwd = os.path.dirname(os.path.realpath(__file__))
    cwd = os.getcwd()
    driver.get('file://' + cwd + '/' + self.test_page_file)
    driver.set_window_size(800, 600)
    element = driver.find_element_by_xpath(self.patch_xpath(self.element_xpath))
    self.element_rect = (int(math.ceil(element.location['x'])),
      int(math.ceil(element.location['y'])),
      int(math.floor(element.location['x'] + element.size['width'])),
      int(math.floor(element.location['y'] + element.size['height'])))
    self.element_is_displayed = element.is_displayed
    self.element_text = element.text
    self.element_tag = element.tag_name
    driver.quit()

  def list_equal(self, a, b):
    return a[0] == b[0] and a[1] == b[1] and a[2] == b[2]
  
  def exact_match(self, oracle_image_file, test_image_file, rect):
    oracle_image = cv2.imread(oracle_image_file)
    height, width, depth = oracle_image.shape
    if rect[3] > height or rect[2] > width:
      return False
    test_image = cv2.imread(test_image_file)
    height, width, depth = test_image.shape
    if rect[3] > height or rect[2] > width:
      return False
    for x in range(rect[1], rect[3]):
      for y in range(rect[0], rect[2]):
        if not self.list_equal(oracle_image[x, y], test_image[x, y]):
          return False
    return True

  def extract(self):
    oracle_image = Image.open(self.oracle_image_file)
    self.oracle_colors = oracle_image.getcolors(256 * 256)
    self.oracle_size = oracle_image.size
    self.screenshot(self.test_page_file, self.test_image_file)
    test_image = Image.open(self.test_image_file)
    self.test_colors = test_image.getcolors(256 * 256)
    self.test_size = test_image.size
    diff_image = ImageChops.difference(oracle_image, test_image)
    self.different_pixels = 0
    if diff_image.getbbox():
      left, upper, right, lower = diff_image.getbbox()
      for x in range(left, right):
        for y in range(upper, lower):
          if diff_image.load()[x, y] != (0, 0, 0, 0):
            self.different_pixels += 1
    self.get_value()
    self.exact_match = self.exact_match(self.oracle_image_file, \
      self.test_image_file, self.element_rect)
    
  def color_set(self, colors):
    color_list = []
    for color in colors:
      color_list.append(color[1])
    return set(color_list)

  def process(self):
    self.same_size = self.oracle_size[0] == self.test_size[0] and \
      self.oracle_size[1] == self.test_size[1]
    self.same_number_of_colors = len(self.oracle_colors) == len(self.test_colors)
    self.color_added = len(self.color_set(self.test_colors) - self.color_set(self.oracle_colors)) > 0
    self.color_removed = len(self.color_set(self.oracle_colors) - self.color_set(self.test_colors)) > 0
    self.small_difference = self.different_pixels < 100
    self.contain_text = len(self.element_text) > 0
    self.image_node = self.element_tag == 'img'

  def output(self):
    output_dict = {}
    output_dict['same_size'] = self.same_size
    output_dict['same_number_of_colors'] = self.same_number_of_colors
    output_dict['color_added'] = self.color_added
    output_dict['color_removed'] = self.color_removed
    output_dict['small_difference'] = self.small_difference
    output_dict['contain_text'] = self.contain_text
    output_dict['image_node'] = self.image_node
    output_dict['exact_match'] = self.exact_match
    return output_dict

  def output_binary(self):
    out = self.output().values()
    return [int(x == True) for x in out]

if __name__ == '__main__':
  parser = argparse.ArgumentParser()
  parser.add_argument('-o', dest='oracle_image',
                    default='data2/test1/oracle.png',
                    help='Oracle Image')
  parser.add_argument('-t', dest='test_page',
                    default='data2/test1/test.html',
                    help='Test web page')
  parser.add_argument('-x', dest='xpath',
                    default='/html[1]/body[1]/div[3]/div[1]/div[1]/div[2]/div[2]/div[1]/div[1]/ul[1]/li[5]/a[1]',
                    help='XPath of the element')
  args = parser.parse_args()
  feature = Feature(args.oracle_image, args.test_page, args.xpath)
  feature.process()
  print feature.output()
  print feature.output_binary()
