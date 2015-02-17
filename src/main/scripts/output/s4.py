import sys
import re
import os
import argparse

import common

def parse_file(filename):
  filter = ['html:target', 'css:cursor', 'html:value' ,
    'css:border-top-right-radius', 'css:border-bottom-right-radius',
    'css:border-top-left-radius', 'css:border-bottom-left-radius',
    'css:background-image', 'css:background-attachement',
    'css:border-style', 'css:max-width', 'html:spellcheck', 'html:placeholder',
    'css:z-index', 'css:min-width', 'css:min-height', 'html:novalidate',
    'html:action', 'html:method']
  rank = []
  time = '0.0'
  if not os.path.isfile(filename):
    return (rank, time)
  with open(filename) as f:
    linenumber = 0
    for line in f:
      linenumber += 1
      line = line.strip()
      if linenumber == 2:
        xpath = line
      pattern1 = r'\S+ visual properties: \[(.*)\]'
      match = re.match(pattern1, line)
      if match:
        rank = [x.strip() for x in match.group(1).split(',')]
        for f in filter:
          if f in rank:
            rank.remove(f)
      pattern2 = r'Total Time = (\S*) sec'
      match = re.match(pattern2, line)
      if match:
        time = match.group(1)
  return (rank, time)

def process(testpath):
  with open(testpath + '/description.txt') as f:
    total = 0
    fail = 0
    skip = 0
    total_time = 0
    total_rank = 0
    total_vp = 0
    for line in f:
      total += 1
      split = line.strip().split('\t')
      eid = split[1]
      expected_type = split[3].lower()
      expected_attribute = split[2].lower()
      expected_value = split[5].lower()
      path = split[8]
      expected_xpath = common.patch_xpath(split[7])
      sys.stdout.write("Test Case: {0}\n".format(path))
      sys.stdout.write("Expected: {0}\t{1}\n".format(expected_type, expected_attribute))
      if eid == 'no seeding':
        sys.stdout.write('Skip.\n')
        skip += 1
      else:
        (rank, time) = parse_file(
          testpath + '/' + path + '/RCA_details.txt')
        sys.stdout.write("Time: {0} seconds\n".format(time))
        sys.stdout.write(str(rank) + '\n')
        vp = expected_type + ':' + expected_attribute
        if vp in rank:
          index = rank.index(vp) + 1
        else:
          index = len(rank) + 1
        sys.stdout.write("Rank: {0}\n".format(index))
        sys.stdout.write("Length: {0}\n".format(len(rank)))
        if time:
          total_time += float(time)
        total_rank += float(index)
        total_vp += float(len(rank))
      sys.stdout.write('\n')
    sys.stdout.write('\n')
    sys.stdout.write('Failed Test Case: {0}\n'.format(fail))
    count = total - fail - skip
    sys.stdout.write('Total Test Case: {0}\n'.format(count))
    if count == 0:
      count = -1
    sys.stdout.write('Average Rank: {0:.2f}\n'.format(total_rank / count))
    sys.stdout.write('Average VP: {0:.2f}\n'.format(total_vp / count))
    sys.stdout.write('Average Running Time: {0:.2f} seconds\n'.format(total_time / count))
    return (total, total_rank / count, total_vp / count, total_time / count)

def print_info(result):
  sys.stdout.write('\n')
  sys.stdout.write('Category Total RANK    VP Time\n')
  items = result['www.gmail.com']
  sys.stdout.write('Gmail    {0:5} {1:.2f} {2:5.2f} {3:5.2f}\n'.format(
    items[0], items[1], items[2], items[3]))
  items = result['www.cs.usc.edu']
  sys.stdout.write('USC      {0:5} {1:.2f} {2:5.2f} {3:5.2f}\n'.format(
    items[0], items[1], items[2], items[3]))
  items = result['losangeles.craigslist.org']
  sys.stdout.write('Craigs   {0:5} {1:.2f} {2:5.2f} {3:5.2f}\n'.format(
    items[0], items[1], items[2], items[3]))
  items = result['docs.oracle.com']
  sys.stdout.write('Java     {0:5} {1:.2f} {2:5.2f} {3:5.2f}\n'.format(
    items[0], items[1], items[2], items[3]))      
  items = result['www.virginamerica.com']
  sys.stdout.write('Virgin   {0:5} {1:.2f} {2:5.2f} {3:5.2f}\n'.format(
    items[0], items[1], items[2], items[3]))
  
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
  for d in os.listdir(args.input_directory):
    sys.stdout.write(d + '\n')
    dir = os.path.join(args.input_directory, d)
    if os.path.isdir(dir):
      result[d] = process(dir)
  print_info(result)
