import sys
import re
import os
import argparse

import common

def parse_file(filename):
  fix = False
  xpath = None
  attribute = None
  value = None
  time = '0.0'
  if not os.path.isfile(filename):
    return (fix, xpath, attribute, value, time)
  with open(filename) as f:
    linenumber = 0
    for line in f:
      linenumber += 1
      line = line.strip()
      if linenumber == 2:
        xpath = line
      pattern1 = r'Problem in (\S*) in attribute (\S*). New value should be (\S*)'
      match = re.match(pattern1, line)
      if match:
        attribute = match.group(2)
        value = match.group(3)
        fix = True
      pattern2 = r'Problem in (\S*) in attribute (\S*). suggested fix is remove hidden'
      match = re.match(pattern2, line)
      if match:
        attribute = match.group(2)
        value = ''
        fix = True
      pattern3 = r'The best root cause was found to be (\S*) with value (\S*?)\.'
      match = re.match(pattern3, line)
      if match:
        attribute = match.group(1)
        value = match.group(2)
      pattern4 = r'Total Time = (\S*) sec'
      match = re.match(pattern4, line)
      if match:
        time = match.group(1)
  if value:
    value = value.replace('.0', '') 
  return (fix, xpath, attribute, value, time)

def equal(expected_attribute, expected_value, actual_attribute, actual_value):
  global equal_dict
  if expected_attribute == actual_attribute and expected_value == actual_value:
    return True
  if expected_attribute != expected_attribute:
    return False
  if expected_attribute not in equal_dict:
    return False
  for e in equal_dict[expected_attribute]:
    if expected_value in e and actual_value in e:
      return True
  return False

def equalelement(element1, element2):
  if not element1:
    return False
  if not element2:
    return False
  if element1 == element2:
    return True
  if element1.replace('[1]', '') == element2.replace('[1]', ''):
    return True
  return False

def process(testpath):
  with open(testpath + '/description.txt') as f:
    total = 0
    fail = 0
    skip = 0
    correct_element = 0
    correct_attribute = 0
    acceptable_number = 0
    fix_number = 0
    total_time = 0
    for line in f:
      total += 1
      split = line.strip().split('\t')
      eid = split[1]
      expected_attribute = split[2].lower()
      expected_value = split[5].lower()
      path = split[8]
      expected_xpath = split[7]
      sys.stdout.write("Test Case: {0}\n".format(path))
      sys.stdout.write("Expected: {0}\t{1}\t{2}\n".format(expected_xpath, expected_attribute, expected_value))
      if eid == 'no seeding':
        sys.stdout.write('Skip.\n')
        skip += 1
      else:
        (fix, actual_xpath, actual_attribute, actual_value, time) = parse_file(testpath + '/' + path + '/RCA_results.txt')
        sys.stdout.write("Actual: {0}\t{1}\t{2}\n".format(actual_xpath, actual_attribute, actual_value))
        sys.stdout.write("Same Image: {0}\n".format(fix))
        sys.stdout.write("Time: {0} seconds\n".format(time))
  
        if not actual_xpath:
          sys.stdout.write('Fail!\n')
          fail += 1
        if time:
          total_time += float(time)
        if fix:
          acceptable_number += 1
        if common.xpath_equal(actual_xpath, expected_xpath):
          correct_element += 1
          if actual_attribute == expected_attribute:
            correct_attribute += 1
          if fix and common.equal(expected_attribute, expected_value, actual_attribute, actual_value):
            fix_number += 1
            sys.stdout.write('Exact fix\n')
      sys.stdout.write('\n')
    sys.stdout.write('\n')
    sys.stdout.write('Correct Element: {0}\n'.format(correct_element))
    sys.stdout.write('Correct Attribute: {0}\n'.format(correct_attribute))
    sys.stdout.write('Acceptable Fix: {0}\n'.format(acceptable_number - fix_number))
    sys.stdout.write('Exact Fix: {0}\n'.format(fix_number))
    sys.stdout.write('Skipped Test Case: {0}\n'.format(skip))
    sys.stdout.write('Failed Test Case: {0}\n'.format(fail))
    sys.stdout.write('Total Test Case: {0}\n'.format(total))
    sys.stdout.write('Average Running Time: {0:.2f} seconds\n'.format(total_time / (total - fail - skip)))
    count = total - fail - skip
    elerate = correct_element * 1.0 / count
    attrate = correct_attribute * 1.0 / count
    accrate = (acceptable_number - fix_number) * 1.0 / count
    fixrate = fix_number * 1.0 / count 
    sys.stdout.write('Correct Element Rate: {0} / {1} = {2:.2f} \n'.format(correct_element, count, correct_element * 1.0 / count))
    sys.stdout.write('Correct Attribute Rate: {0} / {1} = {2:.2f} \n'.format(correct_attribute, count, correct_attribute * 1.0 / count))
    sys.stdout.write('Acceptable Rate: {0} / {1} = {2:.2f} \n'.format(acceptable_number - fix_number, count, (acceptable_number - fix_number) * 1.0 / count))
    sys.stdout.write('Exact Rate: {0} / {1} = {2:.2f} \n'.format(fix_number, count, fix_number * 1.0 / count))
    return (acceptable_number - fix_number, fix_number, (total - fail - skip), total_time / (total - fail - skip))

def print_info(result):
  sys.stdout.write('\n')
  sys.stdout.write('Category AS ES Total Time\n')
  items = result['www.gmail.com']
  sys.stdout.write('Gmail    {0:2} {1:2} {2:5} {3:.2f}\n'.format(items[0], items[1], items[2], items[3]))
  items = result['www.cs.usc.edu']
  sys.stdout.write('USC      {0:2} {1:2} {2:5} {3:.2f}\n'.format(items[0], items[1], items[2], items[3]))
  items = result['losangeles.craigslist.org']
  sys.stdout.write('Craigs   {0:2} {1:2} {2:5} {3:.2f}\n'.format(items[0], items[1], items[2], items[3]))
  items = result['docs.oracle.com']
  sys.stdout.write('Java     {0:2} {1:2} {2:5} {3:.2f}\n'.format(items[0], items[1], items[2], items[3]))      
  items = result['www.virginamerica.com']
  sys.stdout.write('Virgin   {0:2} {1:2} {2:5} {3:.2f}\n'.format(items[0], items[1], items[2], items[3]))
  
def print_latex(result):
  sys.stdout.write('\n')
  sys.stdout.write('Gmail')
  items = result['www.gmail.com']
  sys.stdout.write(' & {0} & {1} & {2} \\\\\n'.format(items[1], items[2], items[0]))
  sys.stdout.write('USC CS Research')
  items = result['www.cs.usc.edu']
  sys.stdout.write(' & {0} & {1} & {2} \\\\\n'.format(items[1], items[2], items[0]))
  sys.stdout.write('Craigslist LA')
  items = result['losangeles.craigslist.org']
  sys.stdout.write(' & {0} & {1} & {2} \\\\\n'.format(items[1], items[2], items[0]))
  sys.stdout.write('Java Tutorial')
  items = result['docs.oracle.com']
  sys.stdout.write(' & {0} & {1} & {2} \\\\\n'.format(items[1], items[2], items[0]))
  sys.stdout.write('Virgin America')
  items = result['www.virginamerica.com']
  sys.stdout.write(' & {0} & {1} & {2} \\\\\n'.format(items[1], items[2], items[0]))

if __name__ == '__main__':
  parser = argparse.ArgumentParser()
  parser.add_argument('-d', dest='input_directory',
                    default='.',
                    help='Input directory')
  parser.add_argument('-o', dest='output_file',
                    help='Output to a file')
  args = parser.parse_args()
  result = {}
  for d in os.listdir(args.input_directory):
    dir = os.path.join(args.input_directory, d)
    if os.path.isdir(dir):
      result[d] = process(dir)
  print_latex(result)      
  print_info(result)
