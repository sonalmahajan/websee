import os
from selenium import webdriver
from selenium.webdriver.support.color import Color

def getDefaultValue(filepath, xpath, name, css=True):
  driver = webdriver.Firefox()
#  driver = webdriver.PhantomJS()
  driver.maximize_window()
  driver.get('file://' + filepath)
  element = driver.find_element_by_xpath(xpath)
  if css:
    value = element.value_of_css_property(name)
  else:
    value = element.get_attribute(name)
  if not value:
    value = ''
  if '-moz-' in value:
    value = value.replace('-moz-', '')
  if 'color' in name:
    value = Color.from_string(value).hex
  driver.quit()
  return value

def process(testpath):
  fout = open(testpath + '/description.txt~', 'w')
  with open(testpath + '/description.txt') as f:
    number = 0
    for line in f:
      number += 1
      split = line.strip().split('\t')
      element_id = split[1]
      attribute = split[2].lower()
      css = split[3]
      value = split[5].lower()
      path = split[8]
      xpath = split[7]
      default = getDefaultValue(testpath + '/' + path + '/oracle.html', xpath, attribute, css == 'css')
      split[5] = default
      fout.write('\t'.join(split) + '\n')
  fout.close()

for d in os.listdir('.'):
  if os.path.isdir(d):
    process(os.path.abspath(d))
