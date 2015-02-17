from StringIO import StringIO
import lxml.html
import lxml.etree
import random
import filecmp
import shutil
import os

import util
import classify

filtered_elements = []

def get_index(element):
  return len([sibling for sibling in element.itersiblings(tag=element.tag, preceding=True)]) + 1
  
def get_xpath(element):
  xpath = ''
  while element.tag != 'html' :
    index = get_index(element)
    xpath = '/' + element.tag + '[' + str(index) + ']' + xpath
    element = element.getparent()
  return '/html[1]' + xpath

def get_oracle(url):
  source = util.get_source(url)
  html = lxml.html.document_fromstring(source)
  html.make_links_absolute(url, resolve_base_href=True)
  util.save_file(lxml.html.tostring(html), 'oracle.html')
  util.screenshot('oracle.html', 'oracle.png')
  return html

def seed_error(html, elements, prop, value):
  while elements:
    e = random.choice(elements)
    elements.remove(e)
    #print 'Trying ', e
    original_style = None
    if 'style' in e.attrib:
      original_style = e.attrib['style']
      e.attrib['style'] += ';{0}:{1};'.format(prop, value)
    else:
      e.attrib['style'] = '{0}:{1};'.format(prop, value)
    util.save_file(lxml.html.tostring(html), 'test.html')
    util.screenshot('test.html', 'test.png')
    if original_style:
      e.attrib['style'] = original_style
    else:
      del e.attrib['style']
    if not filecmp.cmp('oracle.png', 'test.png'):
      xpath = get_xpath(e)
      default_value = util.get_default_value('oracle.html', xpath, prop)
      return (xpath, default_value, value)
      break
  return (None, None, None)

def seed_numeric(html, elements, prop):
  value = str(random.randint(1, 10)) + 'px'
  return seed_error(html, elements, prop, value)

def seed_color(html, elements, prop):
  value = '#{0:02x}{1:02x}{2:02x}'.format(random.randint(0, 255), random.randint(0, 255), random.randint(0, 255))
  return seed_error(html, elements, prop, value)

def seed_predefined(html, elements, prop):
  value = random.choice(classify.predefined[prop])
  return seed_error(html, elements, prop, value)

def process(url):
  if os.path.isfile('description.txt'):
    os.remove('description.txt')
  shutil.rmtree('test')
  os.mkdir('test')
  oracle_html = get_oracle(url)
  total = 0
  for cate in classify.category:
    for prop in classify.category[cate]:
      current = 0
      elements = oracle_html.xpath("//*")
      filter_list = ['html', 'link', 'head', 'meta', 'title', 'style', 'script', 'body']
      elements = [e for e in elements if e.tag not in filter_list]  
      while elements:
        if 'color' in cate:
          (xpath, default_value, value) = seed_color(oracle_html, elements, prop)
        elif 'numeric' in cate:
          (xpath, default_value, value) = seed_numeric(oracle_html, elements, prop)
        elif 'pre-defined' in cate:
          (xpath, default_value, value) = seed_predefined(oracle_html, elements, prop)
        if xpath:
          diff = util.diff_image('oracle.png', 'test.png')
          diff.save('diff.png', 'PNG')
          total += 1
	  current += 1
          path = 'test' + str(total)
          os.mkdir('test/' + path)
          shutil.copyfile('oracle.html', 'test/' + path + '/oracle.html')
          shutil.copyfile('test.html', 'test/' + path + '/test.html')
          shutil.copyfile('oracle.png', 'test/' + path + '/oracle.png')
          shutil.copyfile('test.png', 'test/' + path + '/test.png')
          shutil.copyfile('diff.png', 'test/' + path + '/diff.png')
          write_description(url, prop, default_value, value, xpath, path)
        if current == 20:
          break

def write_description(url, prop, default_value, value, xpath, path):
  dfile = open('description.txt', 'a')
  dfile.write("{0}\t\t{1}\tcss\tnew\t{2}\t{3}\t{4}\t{5}\n".format(url, prop, default_value, value, xpath, path))
  dfile.close()

# url = 'http://www.gmail.com'
# url = 'http://www.cs.usc.edu/research/research-areas-labs.htm'
# url = 'http://losangeles.craigslist.org'
# url = 'http://docs.oracle.com/javase/tutorial/essential/io/summary.html'
url = 'http://www.virginamerica.com/' 
process(url)
