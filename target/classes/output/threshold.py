import sys
import re
import os
import argparse

import common

def parse_sa(filename, expected_vp):
  if not os.path.isfile(filename):
    return (None, None)
  with open(filename) as f:
    linenumber = 0
    vp = ''
    initial_dp = ''
    for line in f:
      linenumber += 1
      line = line.strip()
      if linenumber == 2:
        xpath = line
      pattern1 = r'\d+ of \d+. (\S+) -> (\S+) \((\S+) category\)'
      match = re.match(pattern1, line)
      if match:
        vp = match.group(1)
        vp_type = match.group(2)
        vp_cate = match.group(3)
        final_dp = 1E10
      pattern2 = r'.*difference pixels size = (\d+)'
      match = re.match(pattern2, line)
      if match:
        initial_dp = match.group(1)
      pattern3 = r'next \(state, energy\) = \(\S+, (\d+)\)'
      match = re.match(pattern3, line)
      if match and 'numeric' in vp_cate and final_dp > (float)(match.group(1)):
        final_dp = (float)(match.group(1))
      pattern4 = r'Constant rate of change\. Hence, terminating\.'
      match = re.match(pattern4, line)
      if match:
        if vp == expected_vp and 'numeric' in vp_cate:
          return (initial_dp, final_dp)
  return (None, None)

def parse_ga(filename, expected_vp):
  if not os.path.isfile(filename):
    return (None, None)
  with open(filename) as f:
    linenumber = 0
    vp = ''
    initial_dp = ''
    for line in f:
      linenumber += 1
      line = line.strip()
      if linenumber == 2:
        xpath = line
      pattern1 = r'\d+ of \d+. (\S+) -> (\S+) \((\S+) category\)'
      match = re.match(pattern1, line)
      if match:
        vp = match.group(1)
        vp_type = match.group(2)
        vp_cate = match.group(3)
        final_dp = 1E10
      pattern2 = r'.*original DP = (\d+)'
      match = re.match(pattern2, line)
      if match:
        initial_dp = match.group(1)
      pattern3 = r'value \(GA\) = \d+, DP = (\S+)'
      match = re.match(pattern3, line)
      if match and 'numeric' in vp_cate and final_dp > (float)(match.group(1)):
        final_dp = (float)(match.group(1))
      pattern4 = r'Constant rate of change\. Hence, terminating\.'
      match = re.match(pattern4, line)
      if match:
        if vp == expected_vp and 'numeric' in vp_cate:
          return (initial_dp, final_dp)
  return (None, None)

def process(testpath):
  with open(testpath + '/description.txt') as f:
    for line in f:
      split = line.strip().split('\t')
      expected_type = split[3].lower()
      expected_vp = split[2].lower()
      expected_value = split[5].lower()
      path = split[8]
      expected_xpath = common.patch_xpath(split[7])
      (initial_dp, final_dp) = parse_sa(
          testpath + '/' + path + '/RCA_details.txt', expected_vp)
      if initial_dp:
        print testpath, path, expected_vp, initial_dp, final_dp

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
