import os
import selenium.webdriver
from selenium.common.exceptions import WebDriverException

def getinlinecss(htmlfile, xpath):
  driver = None
  while True:
    try:
      driver = selenium.webdriver.PhantomJS()
      break
    except WebDriverException:
      time.sleep(1)
  driver.set_window_size(800, 600)
  cwd = os.path.dirname(os.path.realpath(__file__))
  driver.get('file://' + cwd + '/' + htmlfile)
  with open('inlinecss.js', 'r') as jsfile:
    inlinecss = jsfile.read()
  value = driver.execute_script(inlinecss, xpath)
  print value


home_dir = 'gmail'
with open(home_dir + '/description.txt') as f:
  for line in f:
    split = line.strip().split('\t')
    test_dir = home_dir + '/' + split[8]
    getinlinecss(test_dir + '/test.html', split[7])
