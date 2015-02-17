from StringIO import StringIO
import lxml.html
import lxml.etree
import random
import filecmp
import shutil
import os
import urlparse

import util
import const

def get_index(element):
  return len([sibling for sibling in element.itersiblings(
    tag=element.tag, preceding=True)]) + 1
  
def get_xpath(element):
  xpath = ''
  while element.tag != 'html' :
    index = get_index(element)
    xpath = '/' + element.tag + '[' + str(index) + ']' + xpath
    element = element.getparent()
  return '/html[1]' + xpath

def get_oracle(url):
  source = util.get_source(url)
  parser = lxml.etree.HTMLParser()
  etree = lxml.etree.parse(StringIO(source), parser)
  html = lxml.html.document_fromstring(source)
  html.make_links_absolute(url, resolve_base_href=True)
  html.doctype = etree.docinfo.doctype
  return html

def seed_css_fault(html, elements, prop, value):
  while elements:
    e = random.choice(elements)
    elements.remove(e)
    original_style = None
    if 'style' in e.attrib:
      original_style = e.attrib['style']
      e.attrib['style'] += ';{0}:{1};'.format(prop, value)
    else:
      e.attrib['style'] = '{0}:{1};'.format(prop, value)
    util.save_file(lxml.html.tostring(html, doctype=html.doctype), 'test.html')
    util.screenshot('test.html', 'test.png')
    if original_style is not None:
      e.attrib['style'] = original_style
    else:
      del e.attrib['style']
    if not filecmp.cmp('oracle.png', 'test.png'):
      xpath = get_xpath(e)
      default_value = util.get_default_value('oracle.html', xpath, prop)
      return (xpath, default_value, value)
      break
  return (None, None, None)

def seed_css_numeric(html, elements, prop):
  value = str(random.randint(1, 10)) + 'px'
  return seed_css_fault(html, elements, prop, value)

def seed_css_color(html, elements, prop):
  value = '#{0:02x}{1:02x}{2:02x}'.format(random.randint(0, 255), random.randint(0, 255), random.randint(0, 255))
  return seed_css_fault(html, elements, prop, value)

def seed_css_predefined(html, elements, prop):
  value = random.choice(const.css_predefined[prop])
  return seed_css_fault(html, elements, prop, value)

def process_css(test_dir, oracle_html, total):
  for cate in const.css:
    for prop in const.css[cate]:
      prop_total = 0
      elements = oracle_html.xpath("//*")
      filter_list = ['html', 'link', 'head', 'meta', 'title', 'style', 'script',
        'body']
      elements = [e for e in elements if e.tag not in filter_list]
      while elements:  
        if 'color' in cate:
          (xpath, default_value, value) = seed_css_color(oracle_html, 
            elements, prop)
        elif 'numeric' in cate:
          (xpath, default_value, value) = seed_css_numeric(oracle_html,
            elements, prop)
        elif 'pre-defined' in cate:
          (xpath, default_value, value) = seed_css_predefined(oracle_html, 
            elements, prop)
        if xpath:
          diff = util.diff_image('oracle.png', 'test.png')
          diff.save('diff.png', 'PNG')
          total += 1
          prop_total += 1
          testcase = 'test' + str(total)
          testcase_dir = test_dir + '/' + testcase
          os.mkdir(testcase_dir)
          shutil.copyfile('oracle.html', testcase_dir + '/oracle.html')
          shutil.copyfile('test.html', testcase_dir + '/test.html')
          shutil.copyfile('oracle.png', testcase_dir + '/oracle.png')
          shutil.copyfile('test.png', testcase_dir + '/test.png')
          shutil.copyfile('diff.png', testcase_dir + '/diff.png')
          write_description(test_dir, url, prop, default_value, value, xpath, testcase, 'css')
        if prop_total == 1:
          break
  return total

def seed_html_fault(html, elements, prop, value):
  while elements:
    e = random.choice(elements)
    elements.remove(e)
    original_style = None
    if prop in e.attrib:
      original_value = e.attrib[prop]
      e.attrib[prop] = value
      util.save_file(lxml.html.tostring(html, doctype=html.doctype), 'test.html')
      util.screenshot('test.html', 'test.png')
      e.attrib[prop] = original_value
    else:
      e.attrib[prop] = value
      util.save_file(lxml.html.tostring(html, doctype=html.doctype), 'test.html')
      util.screenshot('test.html', 'test.png')
      del e.attrib[prop]
    if not filecmp.cmp('oracle.png', 'test.png'):
      xpath = get_xpath(e)
      default_value = util.get_default_value('oracle.html', xpath, prop, 
        css=False)
      return (xpath, default_value, value)
      break
  return (None, None, None)

def seed_html_boolean(html, elements, prop):
  return seed_html_fault(html, elements, prop, '')

def seed_html_numeric(html, elements, prop):
  value = str(random.randint(1, 10))
  return seed_html_fault(html, elements, prop, value)

def seed_html_predefined(html, elements, prop):
  value = random.choice(const.html_predefined[prop])
  return seed_html_fault(html, elements, prop, value)

def process_html(test_dir, oracle_html, total):
  for cate in const.html:
    for prop in const.html[cate]:
      prop_total = 0
      elements = oracle_html.xpath("//*")
      filter_list = ['html', 'link', 'head', 'meta', 'title', 'style', 'script',
        'body']
      elements = [e for e in elements if e.tag not in filter_list]
      if len(const.html_elements[prop]):
        elements = [e for e in elements if e.tag in const.html_elements[prop]]
      while elements:  
        if 'boolean' in cate:
          (xpath, default_value, value) = seed_html_boolean(oracle_html, 
            elements, prop)
        elif 'numeric' in cate:
          (xpath, default_value, value) = seed_html_numeric(oracle_html, 
            elements, prop)
        elif 'pre-defined' in cate:
          (xpath, default_value, value) = seed_html_predefined(oracle_html,
            elements, prop)
        if xpath:
          diff = util.diff_image('oracle.png', 'test.png')
          diff.save('diff.png', 'PNG')
          total += 1
          prop_total += 1
          testcase = 'test' + str(total)
          testcase_dir = test_dir + '/' + testcase
          os.mkdir(testcase_dir)
          shutil.copyfile('oracle.html', testcase_dir + '/oracle.html')
          shutil.copyfile('test.html', testcase_dir + '/test.html')
          shutil.copyfile('oracle.png', testcase_dir + '/oracle.png')
          shutil.copyfile('test.png', testcase_dir + '/test.png')
          shutil.copyfile('diff.png', testcase_dir + '/diff.png')
          write_description(test_dir, url, prop, default_value, value, xpath, testcase, 'html')
          print total
        if prop_total == 1:
          break
  return total

def hide_advertisement(html, xpath):
  adv_elements = html.xpath(xpath)
  for element in adv_elements:
    if 'style' in element.attrib:
      element.attrib['style'] += ';visibility:hidden;'
    else:
      element.attrib['style'] = 'visibility:hidden;'
  return html

def process(url, ads_xpath):
  test_dir = urlparse.urlparse(url).hostname
  if os.path.isdir(test_dir):
    shutil.rmtree(test_dir)
  os.mkdir(test_dir)
  if os.path.isfile(test_dir + 'description.txt'):
    os.remove(test_dir + 'description.txt')
  oracle_html = get_oracle(url)
  for xpath in ads_xpath:
    oracle_html = hide_advertisement(oracle_html, xpath)
  util.save_file(lxml.html.tostring(oracle_html, doctype=oracle_html.doctype), 'oracle.html')
  util.screenshot('oracle.html', 'oracle.png')
  total = 0
  total = process_css(test_dir, oracle_html, total)
  total = process_html(test_dir, oracle_html, total)

def write_description(test_dir, url, prop, default_value, value, xpath, testcase, prop_type):
  dfile = open(test_dir + '/description.txt', 'a')
  dfile.write("{0}\t\t{1}\t{6}\tnew\t{2}\t{3}\t{4}\t{5}\n".format(
    url, prop, default_value, value, xpath, testcase, prop_type))
  dfile.close()

if __name__ == '__main__':
  urls = {
    #'http://www.gmail.com',
    #'http://www.cs.usc.edu/research/research-areas-labs.htm' : [],
    'http://losangeles.craigslist.org' : [],
    #'http://docs.oracle.com/javase/tutorial/essential/io/summary.html',
    'http://www.virginamerica.com/' : ['//*[@id="hero_scroll_image"]']
    }
  for url in urls.keys():
    process(url, urls[url])
