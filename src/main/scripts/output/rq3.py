import sys
import re
import os
import common
import argparse
#import numpy

def parse_test_report(filename):
  xpaths = []
  time = '0'
  if not os.path.isfile(filename):
    return (xpaths, time)
  with open(filename) as f:
    for line in f:
      line = line.strip()
      pattern1 = r'(\d+): (\S*) -> (.*)'
      match = re.match(pattern1, line)
      if match:
        xpaths.append(match.group(2))
      pattern2 = r'Time required to run this test case = (\S*) sec'
      match = re.match(pattern2, line)
      if match:
        time = match.group(1)
  return (xpaths, time)

def parse_file(filename):
  fix = 0
  xpath = None
  attribute = None
  value = None
  time = '0'
  if not os.path.isfile(filename):
    return (fix, time)
  with open(filename) as f:
    for line in f:
      line = line.strip()
      pattern1 = r'Final result is of element (\S*), the visual property is (\S*) with value (.*) => (\S*) root cause found!. Number of difference pixels reduced from (\d+) to (\d+)'
      match = re.match(pattern1, line)
      if match:
        xpath = match.group(1)
        attribute = match.group(2)
        value = match.group(3)
        if match.group(4) == 'exact':
          fix = 2
        if match.group(4) == 'acceptable':
          fix = 1
      pattern2 = r'Total time = (\S*) sec'
      match = re.match(pattern2, line)
      if match:
        time = match.group(1)
  return (fix, time)

#def stats(element_list):
#  arr = numpy.array(element_list)
#  return numpy.mean(arr), numpy.median(arr), numpy.std(arr)
  
def process(testpath):
  with open(testpath + '/description\'.txt') as f:
    result = []
    total = 0
    fail = 0
    skip = 0
    total_acccept = 0
    total_exact = 0
    total_time = 0
    for line in f:
      total += 1
      accept = 0
      exact = 0
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
        rc = 2
        time = 0
        skip += 1
      else:
        (rc, rca_time) = parse_file(testpath +'/' + path + '/RCA_results.txt')
        #sys.stdout.write("Actual: {0}\t{1}\t{2}\n".format(actual_xpath, actual_attribute, actual_value))
        sys.stdout.write("Root Cause (0: wrong; 1:exact; 2:acceptable): {0}\n".format(rc))
        sys.stdout.write("RCA Time: {0} seconds\n".format(rca_time))
        (xpaths, time) = parse_test_report(testpath + '/' + path + '/test_report.txt')
        sys.stdout.write("Time: {0} seconds\n".format(time))
        tried = False
        if xpaths:
          for xpath in xpaths:
            if common.xpath_equal(expected_xpath, xpath):
              tried = True
        sys.stdout.write('Tried element: {0}\n'.format(tried))

        total_time += float(time)
        if time == '0':
          sys.stdout.write('Fail!\n')
          rc = -1
          fail += 1
        if rc == 1:
          sys.stdout.write('Acceptable fix\n')
        elif rc == 2:
          sys.stdout.write('Exact fix\n')
      if rc == 1: 
        total_acccept += 1
      elif rc == 2:
        total_exact += 1
      sys.stdout.write('\n')
      result.append((rc, float(time), float(rca_time)))

    sys.stdout.write('\n')
    sys.stdout.write('Acceptable Fix: {0}\n'.format(total_acccept))
    sys.stdout.write('Exact Fix: {0}\n'.format(total_exact))
    sys.stdout.write('Skipped Test Case: {0}\n'.format(skip))
    sys.stdout.write('Failed Test Case: {0}\n'.format(fail))
    sys.stdout.write('Total Test Case: {0}\n'.format(total))
    count = total - fail
    sys.stdout.write('Average Running Time: {0:.2f} seconds\n'.format(total_time/count))
    accrate = total_acccept * 1.0 / count
    exactrate = total_exact * 1.0 / count 
    sys.stdout.write('Acceptable Rate: {0} / {1} = {2:.2f} \n'.format(total_acccept, count, accrate))
    sys.stdout.write('Exact Rate: {0} / {1} = {2:.2f} \n'.format(total_exact, count, exactrate))
    return result

def print_info(result):
  acc_rc = 0
  exact_rc = 0
  time = 0
  sys.stdout.write('\n')
  sys.stdout.write('Category     A%  E%  T%  A  E  T Total  Time\n')
  items = result['www.gmail.com']
  sys.stdout.write('Gmail       {0:3.0f} {1:3.0f} {2:3.0f} {3:2} {4:2} {5:2} {6:5} {7:5.2f} \n'.format(
    items[1] * 100.0 / items[0], items[2] * 100.0 / items[0], (items[1] + items[2]) * 100.0 / items[0],
    items[1], items[2], items[1] + items[2], items[0], items[3] / items[0]))
  acc_rc += items[1] * 100.0 / items[0]
  exact_rc += items[2] * 100.0 / items[0]
  time += items[3] / items[0]
  items = result['www.cs.usc.edu']
  sys.stdout.write('USC         {0:3.0f} {1:3.0f} {2:3.0f} {3:2} {4:2} {5:2} {6:5} {7:5.2f} \n'.format(
    items[1] * 100.0 / items[0], items[2] * 100.0 / items[0], (items[1] + items[2]) * 100.0 / items[0],
    items[1], items[2], items[1] + items[2], items[0], items[3] / items[0]))
  acc_rc += items[1] * 100.0 / items[0]
  exact_rc += items[2] * 100.0 / items[0]
  time += items[3] / items[0]
  items = result['losangeles.craigslist.org']
  sys.stdout.write('Craigs      {0:3.0f} {1:3.0f} {2:3.0f} {3:2} {4:2} {5:2} {6:5} {7:5.2f} \n'.format(
    items[1] * 100.0 / items[0], items[2] * 100.0 / items[0], (items[1] + items[2]) * 100.0 / items[0],
    items[1], items[2], items[1] + items[2], items[0], items[3] / items[0]))
  acc_rc += items[1] * 100.0 / items[0]
  exact_rc += items[2] * 100.0 / items[0]
  time += items[3] / items[0]
  items = result['docs.oracle.com']
  sys.stdout.write('Java        {0:3.0f} {1:3.0f} {2:3.0f} {3:2} {4:2} {5:2} {6:5} {7:5.2f} \n'.format(
    items[1] * 100.0 / items[0], items[2] * 100.0 / items[0], (items[1] + items[2]) * 100.0 / items[0],
    items[1], items[2], items[1] + items[2], items[0], items[3] / items[0]))
  acc_rc += items[1] * 100.0 / items[0]
  exact_rc += items[2] * 100.0 / items[0]
  time += items[3] / items[0]
  items = result['www.virginamerica.com']
  sys.stdout.write('Virgin      {0:3.0f} {1:3.0f} {2:3.0f} {3:2} {4:2} {5:2} {6:5} {7:5.2f} \n'.format(
    items[1] * 100.0 / items[0], items[2] * 100.0 / items[0], (items[1] + items[2]) * 100.0 / items[0],
    items[1], items[2], items[1] + items[2], items[0], items[3] / items[0]))
  acc_rc += items[1] * 100.0 / items[0]
  exact_rc += items[2] * 100.0 / items[0]
  time += items[3] / items[0]
  sys.stdout.write('Average     {0:3.0f} {1:3.0f} {2:3.0f}\n'.format(
    acc_rc/5, exact_rc/5, acc_rc/5 + exact_rc/5, time/5))

def print_latex(result):
  sys.stdout.write('Gmail')
  items = result['www.gmail.com']
  sys.stdout.write(' & 67 & {0:.0f} & 63 & {1:.0f} & 43 & {2:.0f} \\\\\n'.format(
    items[1] * 100.0 / items[0], items[2] * 100.0 / items[0], items[3] * 100.0 / items[0]))
  sys.stdout.write('USC CS Research')
  items = result['www.cs.usc.edu']
  sys.stdout.write(' & 67 & {0:.0f} & 67 & {1:.0f} & 63 & {2:.0f} \\\\\n'.format(
    items[1] * 100.0 / items[0], items[2] * 100.0 / items[0], items[3] * 100.0 / items[0]))
  sys.stdout.write('Craigslist LA')
  items = result['losangeles.craigslist.org']
  sys.stdout.write(' & 47 & {0:.0f} & 50 & {1:.0f} & 47 & {2:.0f} \\\\\n'.format(
    items[1] * 100.0 / items[0], items[2] * 100.0 / items[0], items[3] * 100.0 / items[0]))
  sys.stdout.write('Java Tutorial')
  items = result['docs.oracle.com']
  sys.stdout.write(' & 47 & {0:.0f} & 47 & {1:.0f} & 43 & {2:.0f} \\\\\n'.format(
    items[1] * 100.0 / items[0], items[2] * 100.0 / items[0], items[3] * 100.0 / items[0]))
  sys.stdout.write('Virgin America')
  items = result['www.virginamerica.com']
  sys.stdout.write(' & 50 & {0:.0f} & 47 & {1:.0f} & 47 & {2:.0f} \\\\\n'.format(
    items[1] * 100.0 / items[0], items[2] * 100.0 / items[0], items[3] * 100.0 / items[0]))

if __name__ == '__main__':
  parser = argparse.ArgumentParser()
  parser.add_argument('-d', dest='input_directory',
                    default='.',
                    help='Input directory')
  parser.add_argument('-o', dest='output_file',
                    help='Output to a file')
  args = parser.parse_args()
  interresult = process(args.input_directory)
  result = {
    'www.gmail.com' : [0, 0, 0, 0, 0],
    'www.cs.usc.edu' : [0, 0, 0, 0, 0],
    'losangeles.craigslist.org' : [0, 0, 0, 0, 0],
    'docs.oracle.com' : [0, 0, 0, 0, 0],
    'www.virginamerica.com' : [0, 0, 0, 0, 0],
  }
  total_time = {}
  rca_time = {}
  ws_time = {}
  for i in range(len(interresult)):
    if interresult[i][1] == -1:
      continue
    if i % 10 == 0 or i % 10 == 1:
      name = 'www.gmail.com'
    if i % 10 == 2 or i % 10 == 3:
      name = 'www.cs.usc.edu'
    if i % 10 == 4 or i % 10 == 5:
      name = 'losangeles.craigslist.org'
    if i % 10 == 6 or i % 10 == 7:
      name = 'docs.oracle.com'
    if i % 10 == 8 or i % 10 == 9:
      name = 'www.virginamerica.com'
    if interresult[i][0] != -1:
      result[name][0] += 1
    if interresult[i][0] == 1:
      result[name][1] += 1
    elif interresult[i][0] == 2:
      result[name][2] += 1
    result[name][3] += interresult[i][1]
  print_info(result)
