import sys
import re
import os
import argparse

import common
import numpy

def stats(element_list):
  arr = numpy.array(element_list)
  return numpy.mean(arr), numpy.median(arr), numpy.std(arr)

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
      pattern1 = r'Problem in (\S*) in attribute ([a-z\-]*)\. New value should be (.*) => (\S*) root cause found! Number of difference pixels reduced from (\d+) to (\d+)'
      match = re.match(pattern1, line)
      if match:
        xpath = match.group(1)
        attribute = match.group(2)
        value = match.group(3)
        if match.group(4) == 'exact':
          fix = 2
        if match.group(4) == 'acceptable':
          fix = 1
      pattern2 = r'Total Time = (\S*) sec'
      match = re.match(pattern2, line)
      if match:
        time = match.group(1)
  return (fix, time)

def process(testpath):
  with open(testpath + '/description.txt') as f:
    result = []
    total = 0
    fail = 0
    skip = 0
    total_acccept = 0
    total_exact = 0
    total_time = 0
    all_time = []
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
        (rc, time) = parse_file(testpath +'/' + path + '/RCA_results.txt')
        #sys.stdout.write("Actual: {0}\t{1}\t{2}\n".format(actual_xpath, actual_attribute, actual_value))
        sys.stdout.write("Root Cause (0: wrong; 1:acceptable; 2:exact): {0}\n".format(rc))
        sys.stdout.write("Time: {0} seconds\n".format(time))

        total_time += float(time)
        if float(time) > 300:
          print 'too long!'
        all_time.append(float(time))
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

    sys.stdout.write('\n')
    sys.stdout.write('Acceptable Fix: {0}\n'.format(total_acccept))
    sys.stdout.write('Exact Fix: {0}\n'.format(total_exact))
    sys.stdout.write('Skipped Test Case: {0}\n'.format(skip))
    sys.stdout.write('Failed Test Case: {0}\n'.format(fail))
    sys.stdout.write('Total Test Case: {0}\n'.format(total))
    count = total - fail
    if count == 0:
      count = -1
    sys.stdout.write('Average Running Time: {0:.2f} seconds\n'.format(total_time/count))
    accrate = total_acccept * 1.0 / count
    exactrate = total_exact * 1.0 / count 
    sys.stdout.write('Acceptable Rate: {0} / {1} = {2:.2f} \n'.format(total_acccept, count, accrate))
    sys.stdout.write('Exact Rate: {0} / {1} = {2:.2f} \n'.format(total_exact, count, exactrate))
    return (total_acccept, total_exact, total_time, count, all_time)

def print_info(result):
  sys.stdout.write('\n')
  sys.stdout.write('Category     A%  E%  T%  A  E  T Total  Time\n')
  items = result['www.gmail.com']
  sys.stdout.write('Gmail       {0:3.0f} {1:3.0f} {2:3.0f} {3:2} {4:2} {5:2} {6:5} {7:5.2f} \n'.format(
    items[0] * 100.0 / items[3], items[1] * 100.0 / items[3], (items[0] + items[1]) * 100.0 / items[3],
    items[0], items[1], items[0] + items[1], items[3], items[2] / items[3]))
  items = result['www.cs.usc.edu']
  sys.stdout.write('USC         {0:3.0f} {1:3.0f} {2:3.0f} {3:2} {4:2} {5:2} {6:5} {7:5.2f} \n'.format(
    items[0] * 100.0 / items[3], items[1] * 100.0 / items[3], (items[0] + items[1]) * 100.0 / items[3],
    items[0], items[1], items[0] + items[1], items[3], items[2] / items[3]))
  items = result['losangeles.craigslist.org']
  sys.stdout.write('Craigs      {0:3.0f} {1:3.0f} {2:3.0f} {3:2} {4:2} {5:2} {6:5} {7:5.2f} \n'.format(
    items[0] * 100.0 / items[3], items[1] * 100.0 / items[3], (items[0] + items[1]) * 100.0 / items[3],
    items[0], items[1], items[0] + items[1], items[3], items[2] / items[3]))
  items = result['docs.oracle.com']
  sys.stdout.write('Java        {0:3.0f} {1:3.0f} {2:3.0f} {3:2} {4:2} {5:2} {6:5} {7:5.2f} \n'.format(
    items[0] * 100.0 / items[3], items[1] * 100.0 / items[3], (items[0] + items[1]) * 100.0 / items[3],
    items[0], items[1], items[0] + items[1], items[3], items[2] / items[3]))
  items = result['www.virginamerica.com']
  sys.stdout.write('Virgin      {0:3.0f} {1:3.0f} {2:3.0f} {3:2} {4:2} {5:2} {6:5} {7:5.2f} \n'.format(
    items[0] * 100.0 / items[3], items[1] * 100.0 / items[3], (items[0] + items[1]) * 100.0 / items[3],
    items[0], items[1], items[0] + items[1], items[3], items[2] / items[3]))

def print_latex(result):
  sys.stdout.write('Gmail')
  items = result['www.gmail.com']
  sys.stdout.write(' & {0} & {1} & {2} & {3} & {4} \\\\\n'.format(items[0], items[1], items[2], items[3], items[4]))
  sys.stdout.write('USC CS Research')
  items = result['www.cs.usc.edu']
  sys.stdout.write(' & {0} & {1} & {2} & {3} & {4} \\\\\n'.format(items[0], items[1], items[2], items[3], items[4]))
  sys.stdout.write('Craigslist LA')
  items = result['losangeles.craigslist.org']
  sys.stdout.write(' & {0} & {1} & {2} & {3} & {4} \\\\\n'.format(items[0], items[1], items[2], items[3], items[4]))
  sys.stdout.write('Java Tutorial')
  items = result['docs.oracle.com']
  sys.stdout.write(' & {0} & {1} & {2} & {3} & {4} \\\\\n'.format(items[0], items[1], items[2], items[3], items[4]))
  sys.stdout.write('Virgin America')
  items = result['www.virginamerica.com']
  sys.stdout.write(' & {0} & {1} & {2} & {3} & {4} \\\\\n'.format(items[0], items[1], items[2], items[3], items[4]))

if __name__ == '__main__':
  parser = argparse.ArgumentParser()
  parser.add_argument('-d', dest='input_directory',
                    default='.',
                    help='Input directory')
  parser.add_argument('-o', dest='output_file',
                    help='Output to a file')
  args = parser.parse_args()
  result = {}
  all_time = []
  for d in os.listdir(args.input_directory):
    sys.stdout.write(d + '\n')
    dir = os.path.join(args.input_directory, d)
    if os.path.isdir(dir):
      result[d] = process(dir)
      all_time.extend(result[d][4])
  print_info(result)
  #print_latex(result)
